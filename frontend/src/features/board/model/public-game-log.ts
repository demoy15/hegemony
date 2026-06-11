import type { ClassType, EventLogEntry, GameState, PlayerScoringBreakdown } from "@/types/game";

export type PublicLogSeverity = "INFO" | "WARNING" | "ERROR";
export type PublicLogCategory = "ACTION" | "PHASE" | "VOTE" | "PRODUCTION" | "SCORING" | "REJECTION" | "AUTOMA_DECISION";

export interface PublicGameLogEntry {
  id: string;
  round: number;
  turnNumber: number;
  actorClass: ClassType | "NONE";
  actorDisplayName: string;
  title: string;
  details: string[];
  severity: PublicLogSeverity;
  category: PublicLogCategory;
  eventIds: number[];
}

const CLASS_LABEL: Record<string, string> = {
  WORKER: "Рабочий класс",
  MIDDLE_CLASS: "Средний класс",
  CAPITALIST: "Капиталисты",
  STATE: "Государство",
  NONE: "Система",
};

const VOTING_CUBE_COLOR_LABEL: Record<string, string> = {
  WORKER: "\u043a\u0440\u0430\u0441\u043d\u044b\u0435",
  MIDDLE_CLASS: "\u0436\u0435\u043b\u0442\u044b\u0435",
  CAPITALIST: "\u0441\u0438\u043d\u0438\u0435",
};

const ACTION_LABEL: Record<string, string> = {
  BUILD_ENTERPRISE: "построить предприятие",
  MAKE_DEAL: "заключить сделку",
  SELL_TO_FOREIGN_MARKET: "продать на внешнем рынке",
  PROPOSE_BILL: "внести законопроект",
  ASSIGN_WORKERS: "назначить рабочих",
  PLACE_STRIKES: "забастовка",
  PLACE_DEMONSTRATION: "демонстрация",
};

const POLICY_LABEL: Record<string, string> = {
  POLICY_1_FISCAL: "фискальной политике",
  POLICY_2_LABOR_MARKET: "рынку труда",
  POLICY_3_TAXATION: "налоговой политике",
  POLICY_4_HEALTHCARE_AND_BENEFITS: "здравоохранению и льготам",
  POLICY_5_EDUCATION: "образованию",
  POLICY_6_FOREIGN_TRADE: "внешней торговле",
  POLICY_7_IMMIGRATION: "миграции",
};

const RESOURCE_LABEL: Record<string, string> = {
  food: "еды",
  FOOD: "еды",
  luxury: "роскоши",
  LUXURY: "роскоши",
  healthcare: "медицины",
  HEALTHCARE: "медицины",
  education: "образования",
  EDUCATION: "образования",
  influence: "влияния",
  INFLUENCE: "влияния",
  media_influence: "медиа-влияния",
  MEDIA_INFLUENCE: "медиа-влияния",
};

const STATIC_OBJECT_LABEL: Record<string, string> = {
  automobile_factory: "Автомобильный завод",
  vegetable_farm: "Овощная ферма",
  supermarket: "Супермаркет",
  mall: "Торговый центр",
  college: "Колледж",
  polyclinic: "Поликлиника",
  private_clinic: "Частная клиника",
  mini_market: "Минимаркет",
  state_hospital: "Государственная больница",
  state_university: "Государственный университет",
  state_media: "Государственные СМИ",
};

interface PendingAutomaDecision {
  actorClass: ClassType | "NONE";
  skippedActions: string[];
  skippedReasons: string[];
  eventId: number;
  message: string;
}

export function buildPublicGameLog(state: GameState): PublicGameLogEntry[] {
  const ordered = [...(Array.isArray(state.eventLog) ? state.eventLog : [])].sort((a, b) => a.id - b.id);
  const entries: PublicGameLogEntry[] = [];
  let pendingAutoma: PendingAutomaDecision | undefined;

  for (const event of ordered) {
    if (isNoopEvent(event)) {
      continue;
    }
    if (event.type === "BOT_TURN") {
      pendingAutoma = parseAutomaDecision(event);
      continue;
    }
    const mapped = mapEvent(event, state, pendingAutoma);
    if (!mapped) {
      continue;
    }
    if (pendingAutoma && mapped.actorClass === pendingAutoma.actorClass) {
      mapped.eventIds.unshift(pendingAutoma.eventId);
      mapped.details.push(...automaDetails(pendingAutoma));
      pendingAutoma = undefined;
    }
    entries.push(mapped);
  }
  if (pendingAutoma) {
    entries.push(standaloneAutomaEntry(pendingAutoma, state));
  }

  return entries.map((entry, index) => ({ ...entry, turnNumber: index + 1 })).reverse().slice(0, 40);
}

function mapEvent(event: EventLogEntry, state: GameState, automa?: PendingAutomaDecision): PublicGameLogEntry | undefined {
  const actorClass = detectLogFaction(event);
  const base = baseEntry(event, state, actorClass);
  const message = event.message ?? "";
  let conciseOnly = true;
  if (conciseOnly) {
    return conciseEvent(event, state, base, message);
  }

  const workerAssigned = message.match(/(?:worker\s+)?([a-z0-9_-]*worker-[a-z0-9_-]*\d+)\s+assigned to enterprise\s+([a-z0-9_-]+)\s+slot\s+([a-z0-9_-]+)(?:\s+and tied by labor contract)?/i);
  if (workerAssigned) {
    const tied = /tied by labor contract/i.test(message);
    return actionEntry(
      base,
      "WORKER",
      `Рабочий ${workerPublicLabel(workerAssigned[1])} назначен на предприятие ${objectLabel(workerAssigned[2], state)}.`,
      [`Место: ${slotPublicLabel(workerAssigned[3])}.${tied ? " Заключен трудовой договор." : ""}`],
    );
  }

  const migrationResolved = message.match(/Migration resolved for round (\d+):\s*(.*?):\s*([a-z0-9_-]+)\s*->\s*([A-Z_]+)\/([A-Z_]+)\s*\(([^)]+)\)/i);
  if (migrationResolved) {
    return {
      ...base,
      actorClass: "NONE",
      actorDisplayName: CLASS_LABEL.NONE,
      title: `Миграция в раунде ${migrationResolved[1]}: прибыл ${qualificationPublicLabel(migrationResolved[4])} рабочий.`,
      details: [`Тип рабочего: ${workerColorPublicLabel(migrationResolved[5])}. ${migrationReasonLabel(migrationResolved[2])}`],
      category: "PHASE",
    };
  }

  const wagePaid = message.match(/([a-z_]+) paid (\d+) wages to ([a-z_]+).*?for (\d+) worker\(s\) at ([a-z0-9_-]+).*enterprise wage total (\d+)/i);
  if (wagePaid) {
    const payerIsState = classFromRaw(wagePaid[1]) === "STATE";
    return {
      ...base,
      actorClass: classFromRaw(wagePaid[1]),
      actorDisplayName: playerLabel(wagePaid[1]),
      title: payerIsState
        ? `Государство выплатило из казны ${wagePaid[2]} монет зарплаты игроку ${playerLabel(wagePaid[3])}.`
        : `${playerLabel(wagePaid[1])} выплатили ${wagePaid[2]} монет зарплаты игроку ${playerLabel(wagePaid[3])}.`,
      details: [
        `Предприятие: ${objectLabel(wagePaid[5], state)}. Рабочих: ${wagePaid[4]}. Общая зарплата предприятия: ${wagePaid[6]}.`,
      ],
      category: "PRODUCTION",
    };
  }

  const produced = message.match(/([a-z_]+) produced (\d+) ([a-z_]+) from ([a-z0-9_-]+) into (.+)\.?/i);
  if (produced) {
    return {
      ...base,
      actorClass: classFromRaw(produced[1]),
      actorDisplayName: playerLabel(produced[1]),
      title: `${playerLabel(produced[1])} произвели ${produced[2]} ${resourceLabel(produced[3])}.`,
      details: [`Источник: ${objectLabel(produced[4], state)}. Куда положено: ${storageLabel(produced[5])}.`],
      category: "PRODUCTION",
    };
  }

  const resourcePurchase = message.match(/([a-z_]+) paid (\d+) to ([a-z_ ]+|treasury|external market) for (\d+) ([a-z_]+)/i);
  if (resourcePurchase) {
    const quantity = Number(resourcePurchase[4]);
    const cost = Number(resourcePurchase[2]);
    const price = quantity > 0 ? Math.round((cost / quantity) * 100) / 100 : cost;
    return {
      ...base,
      actorClass: classFromRaw(resourcePurchase[1]),
      actorDisplayName: playerLabel(resourcePurchase[1]),
      title: `${playerLabel(resourcePurchase[1])} купили ${quantity} ${resourceLabel(resourcePurchase[5])} за ${cost} монет.`,
      details: [`Продавец: ${recipientLabel(resourcePurchase[3])}. Цена за единицу: ${price}.`],
      category: event.type.includes("FOOD") ? "PRODUCTION" : "ACTION",
    };
  }

  const boughtTotal = message.match(/([a-z_]+) bought (\d+) ([a-z_]+) for total (\d+) \[(.*)]/i);
  if (boughtTotal) {
    return {
      ...base,
      actorClass: classFromRaw(boughtTotal[1]),
      actorDisplayName: playerLabel(boughtTotal[1]),
      title: `${playerLabel(boughtTotal[1])} завершили покупку: ${boughtTotal[2]} ${resourceLabel(boughtTotal[3])} за ${boughtTotal[4]} монет.`,
      details: boughtTotal[5] ? boughtTotal[5].split(";").map((line) => humanizeSupplierLine(line.trim(), boughtTotal[3])) : [],
      category: "ACTION",
    };
  }

  const consumed = message.match(/([a-z_]+) consumed (\d+) ([a-z_]+).*welfare increased from (\d+) to (\d+) \(\+(\d+) VP\)/i);
  if (consumed) {
    return {
      ...base,
      actorClass: classFromRaw(consumed[1]),
      actorDisplayName: playerLabel(consumed[1]),
      title: `${playerLabel(consumed[1])} потребили ${consumed[2]} ${resourceLabel(consumed[3])} и получили ${consumed[6]} ПО.`,
      details: [`Благосостояние выросло с ${consumed[4]} до ${consumed[5]}.`],
      category: "SCORING",
    };
  }

  const satisfiedFood = message.match(/([a-z_]+) satisfied (\d+) food need for (\d+)/i);
  if (satisfiedFood) {
    return {
      ...base,
      actorClass: classFromRaw(satisfiedFood[1]),
      actorDisplayName: playerLabel(satisfiedFood[1]),
      title: `${playerLabel(satisfiedFood[1])} закрыли потребность в еде: ${satisfiedFood[2]} ед. за ${satisfiedFood[3]} монет.`,
      details: [],
      category: "PRODUCTION",
    };
  }

  const taxPaid = message.match(/([a-z_]+) paid (\d+) .*tax(?:es)? to treasury:? ?(.+)?/i);
  if (taxPaid) {
    return {
      ...base,
      actorClass: classFromRaw(taxPaid[1]),
      actorDisplayName: playerLabel(taxPaid[1]),
      title: `${playerLabel(taxPaid[1])} заплатили ${taxPaid[2]} монет налогов в казну.`,
      details: taxPaid[3] ? [translateTaxFormula(taxPaid[3])] : [],
      category: "PRODUCTION",
    };
  }

  const strikePlaced = message.match(/([a-z_]+) placed a strike token on ([a-z0-9_-]+)/i);
  if (strikePlaced) {
    return actionEntry(base, classFromRaw(strikePlaced[1]), `${playerLabel(strikePlaced[1])} объявили забастовку на предприятии ${objectLabel(strikePlaced[2], state)}.`, []);
  }

  const strikeResolved = message.match(/([a-z0-9_-]+) skipped production and wages due to strike/i);
  if (strikeResolved) {
    return {
      ...base,
      actorClass: "WORKER",
      actorDisplayName: CLASS_LABEL.WORKER,
      title: `${objectLabel(strikeResolved[1], state)} пропустило производство из-за забастовки.`,
      details: ["Рабочий класс получил 1 влияние. Зарплата на этом предприятии не выплачивалась."],
      category: "PRODUCTION",
    };
  }

  const automaStrikeRaised = message.match(/([a-z_]+) reacted to strike on ([a-z0-9_-]+): revealed speech symbol and raised wage level from L(\d+) to L3/i);
  if (automaStrikeRaised) {
    return {
      ...base,
      actorClass: "CAPITALIST",
      actorDisplayName: CLASS_LABEL.CAPITALIST,
      title: `АК отреагировала на забастовку: ${objectLabel(automaStrikeRaised[2], state)} подняло зарплату до L3.`,
      details: [`Было L${automaStrikeRaised[3]}. Рабочие на предприятии связаны трудовым договором.`],
      category: "ACTION",
    };
  }

  const automaStrikeIgnored = message.match(/([a-z_]+) reacted to strike on ([a-z0-9_-]+): no speech symbol was revealed/i);
  if (automaStrikeIgnored) {
    return {
      ...base,
      actorClass: "CAPITALIST",
      actorDisplayName: CLASS_LABEL.CAPITALIST,
      title: `АК проверила реакцию на забастовку: ${objectLabel(automaStrikeIgnored[2], state)} оставило зарплату без изменений.`,
      details: ["На открытых картах не было символа речи."],
      category: "ACTION",
    };
  }

  const demonstrationLoss = message.match(/([a-z_]+) lost (\d+) VP due to demonstration/i);
  if (demonstrationLoss) {
    return {
      ...base,
      actorClass: classFromRaw(demonstrationLoss[1]),
      actorDisplayName: playerLabel(demonstrationLoss[1]),
      title: `${playerLabel(demonstrationLoss[1])} потеряли ${demonstrationLoss[2]} ПО из-за демонстрации.`,
      details: ["Штраф распределен рабочим классом с учетом лимита пустых клеток предприятий."],
      category: "SCORING",
    };
  }

  const demonstrationResolved = message.match(/VP penalty pool was (\d+)/i);
  if (event.type === "DEMONSTRATION_RESOLVED") {
    return {
      ...base,
      actorClass: "WORKER",
      actorDisplayName: CLASS_LABEL.WORKER,
      title: "Демонстрация дошла до производства.",
      details: [`Рабочий класс получил 1 влияние. Общий пул штрафа: ${demonstrationResolved?.[1] ?? "0"} ПО.`],
      category: "PRODUCTION",
    };
  }

  const built = message.match(/([a-z_]+) built enterprise ([a-z0-9_-]+) for (\d+)\.?(.*)/i);
  if (built) {
    return withAutoma(
      actionEntry(
        base,
        classFromRaw(built[1]),
        `${playerLabel(built[1])} построили предприятие ${objectLabel(built[2], state)} за ${built[3]} монет.`,
        built[4] ? [humanizeWorkerLog(built[4])] : [],
      ),
      automa,
    );
  }

  const soldEnterprise = message.match(/([a-z_]+) sold enterprise ([a-z0-9_-]+) for (\d+)/i);
  if (soldEnterprise) {
    return withAutoma(actionEntry(base, classFromRaw(soldEnterprise[1]), `${playerLabel(soldEnterprise[1])} продали предприятие ${objectLabel(soldEnterprise[2], state)} за ${soldEnterprise[3]} монет.`, []), automa);
  }

  const deal = message.match(/([a-z_]+) made deal ([a-z0-9_-]+) \(cost=(\d+), free_trade_zone=\{([^}]*)}, regular_storage=\{([^}]*)}/i);
  if (deal) {
    return withAutoma(
      actionEntry(
        base,
        classFromRaw(deal[1]),
        `${playerLabel(deal[1])} заключили сделку ${objectLabel(deal[2], state)} за ${deal[3]} монет.`,
        [
          ...formatStorageLine(deal[4], "в зоне свободной торговли"),
          ...formatStorageLine(deal[5], "в обычном складе"),
        ],
      ),
      automa,
    );
  }

  const exportMatch = message.match(/([a-z_]+) exported via ([a-z0-9_-]+): (.+)\./i);
  if (exportMatch) {
    const details = exportMatch[3].split(",").map((raw) => exportOperationDetail(raw.trim(), state));
    const total = details.reduce((sum, line) => sum + (Number(line.match(/выручка (\d+)/)?.[1] ?? 0)), 0);
    return withAutoma(
      actionEntry(
        base,
        classFromRaw(exportMatch[1]),
        `${playerLabel(exportMatch[1])} продали ресурсы на внешнем рынке${total > 0 ? ` за ${total} монет` : ""}.`,
        [`Карта: ${objectLabel(exportMatch[2], state)}.`, ...details],
      ),
      automa,
    );
  }

  const proposed = message.match(/([A-Z_]+|[a-z_]+) proposed bill on ([A-Z0-9_]+) from ([ABC]) to ([ABC])/i);
  if (proposed) {
    return withAutoma(
      {
        ...base,
        actorClass: classFromRaw(proposed[1]),
        actorDisplayName: playerLabel(proposed[1]),
        title: `${playerLabel(proposed[1])} внесли законопроект по ${policyLabel(proposed[2])}: курс ${proposed[3]} -> ${proposed[4]}.`,
        details: [],
        category: "VOTE",
      },
      automa,
    );
  }

  if (event.type === "SCORING_RESOLVED") {
    return {
      ...base,
      actorClass: "NONE",
      actorDisplayName: CLASS_LABEL.NONE,
      title: `Подсчет очков за раунд ${state.lastScoringSummary?.round ?? state.currentRound} завершен.`,
      details: scoringDetails(state.scoringBreakdown ?? state.lastScoringSummary?.players ?? []),
      category: "SCORING",
    };
  }

  if (event.type === "PRODUCTION_RESOLVED") {
    return {
      ...base,
      actorClass: "NONE",
      actorDisplayName: CLASS_LABEL.NONE,
      title: "Фаза производства завершена.",
      details: productionDetails(state, state.lastProductionSummary ?? state.productionPhaseState),
      category: "PRODUCTION",
    };
  }

  if (event.type === "HEALTHCARE_CONSUMED") {
    const match = message.match(/([a-z_]+) gained \+2 VP and new unskilled worker ([a-z0-9_-]+)/i);
    return {
      ...base,
      actorClass: classFromRaw(match?.[1] ?? "worker"),
      actorDisplayName: playerLabel(match?.[1] ?? "worker"),
      title: `${playerLabel(match?.[1] ?? "worker")} получили +2 ПО за медицину.`,
      details: match?.[2] ? [`Добавлен новый неквалифицированный рабочий: ${match[2]}.`] : [],
      category: "SCORING",
    };
  }

  if (event.type === "FINAL_WORKER_SCORING" || event.type === "FINAL_CAPITALIST_SCORING") {
    const finalScore = message.match(/([a-z_]+) gained (\d+) final VP: (.+)\./i);
    if (finalScore) {
      return {
        ...base,
        actorClass: classFromRaw(finalScore[1]),
        actorDisplayName: playerLabel(finalScore[1]),
        title: `${playerLabel(finalScore[1])} получили ${finalScore[2]} финальных ПО.`,
        details: [translateFinalScore(finalScore[3])],
        category: "SCORING",
      };
    }
  }

  if (/Advanced to production phase/i.test(message)) {
    return phaseEntry(base, "Фаза действий завершена. Начинается производство.");
  }
  if (/Advanced to scoring phase/i.test(message)) {
    return phaseEntry(base, "Производство завершено. Начинается подсчет очков.");
  }
  if (/Advanced to round (\d+)/i.test(message)) {
    const round = message.match(/Advanced to round (\d+)/i)?.[1];
    return phaseEntry(base, `Начался раунд ${round}.`);
  }
  if (/No pending proposals/i.test(message)) {
    return phaseEntry(base, "Голосование пропущено: внесенных законопроектов нет.");
  }

  const drawnVoteCubes = message.match(/Drawn (\d+) cubes for vote on ([A-Z0-9_]+)(?::\s*([^;]+);\s*votes\s*(.+))?\./i);
  if (drawnVoteCubes) {
    return {
      ...base,
      actorClass: "NONE",
      actorDisplayName: CLASS_LABEL.NONE,
      title: `Из мешка вытянули кубики для голосования: ${drawnVoteCubes[1]} шт. по ${policyLabel(drawnVoteCubes[2])}.`,
      details: [
        drawnVoteCubes[3] ? `Классы: ${formatVoteCubeSummary(drawnVoteCubes[3])}.` : "",
        drawnVoteCubes[4] ? `Итог кубиков: ${formatInterpretedVoteSummary(drawnVoteCubes[4])}.` : "",
      ].filter(Boolean),
      category: "VOTE",
    };
  }

  const votePreliminary = message.match(/Preliminary vote result before influence: FOR (\d+) \/ AGAINST (\d+)/i);
  if (votePreliminary) {
    return {
      ...base,
      actorClass: "NONE",
      actorDisplayName: CLASS_LABEL.NONE,
      title: `Предварительное голосование: за ${votePreliminary[1]}, против ${votePreliminary[2]}.`,
      details: ["После этого игроки могут вложить влияние."],
      category: "VOTE",
    };
  }

  const votingCubes = message.match(/([a-z_]+) added (\d+) .* voting cubes/i);
  if (votingCubes) {
    return {
      ...base,
      actorClass: classFromRaw(votingCubes[1]),
      actorDisplayName: playerLabel(votingCubes[1]),
      title: `${playerLabel(votingCubes[1])} добавили ${votingCubes[2]} кубика в мешок голосования.`,
      details: [],
      category: "VOTE",
    };
  }

  if (event.type === "ACTION_REJECTED" || event.type === "ACTION_FAILED") {
    return {
      ...base,
      title: publicRejectedMessage(message),
      details: [],
      severity: "WARNING",
      category: "REJECTION",
    };
  }

  return {
    ...base,
    title: translateFallback(message),
    details: [],
    category: event.type.includes("VOTE") ? "VOTE" : "ACTION",
  };
}

function baseEntry(event: EventLogEntry, state: GameState, actorClass: ClassType | "NONE"): Omit<PublicGameLogEntry, "title" | "details" | "category"> {
  return {
    id: `public-log-${event.id}`,
    round: state.currentRound,
    turnNumber: 0,
    actorClass,
    actorDisplayName: CLASS_LABEL[actorClass] ?? CLASS_LABEL.NONE,
    severity: "INFO",
    eventIds: [event.id],
  };
}

function actionEntry(
  base: Omit<PublicGameLogEntry, "title" | "details" | "category">,
  actorClass: ClassType | "NONE",
  title: string,
  details: string[],
): PublicGameLogEntry {
  return {
    ...base,
    actorClass,
    actorDisplayName: CLASS_LABEL[actorClass] ?? CLASS_LABEL.NONE,
    title,
    details,
    category: "ACTION",
  };
}

function standaloneAutomaEntry(automa: PendingAutomaDecision, state: GameState): PublicGameLogEntry {
  const base = baseEntry({ id: automa.eventId, type: "BOT_TURN", message: automa.message }, state, automa.actorClass);
  return {
    ...base,
    title: `${CLASS_LABEL[automa.actorClass] ?? CLASS_LABEL.NONE}: ход автомы.`,
    details: [translateFallback(automa.message), ...automaDetails(automa)].filter(Boolean),
    category: "AUTOMA_DECISION",
  };
}

function conciseEvent(
  event: EventLogEntry,
  state: GameState,
  base: Omit<PublicGameLogEntry, "title" | "details" | "category">,
  message: string,
): PublicGameLogEntry | undefined {
  if (event.type.includes("SKIPPED") || event.type.includes("PRELIMINARY") || event.type === "EXTRAORDINARY_VOTE_FINISHED") {
    return undefined;
  }

  const proposed = message.match(/([A-Z_]+|[a-z_]+) proposed bill on ([A-Z0-9_]+) from ([ABC]) to ([ABC])/i);
  if (proposed) {
    return {
      ...base,
      actorClass: classFromRaw(proposed[1]),
      actorDisplayName: playerLabel(proposed[1]),
      title: `${playerLabel(proposed[1])} предложил закон: ${policyLabelClean(proposed[2])} ${proposed[3]} -> ${proposed[4]}.`,
      details: [],
      category: "VOTE",
    };
  }

  const voteResolved = message.match(/Vote on ([A-Z0-9_]+) resolved as ([A-Z_]+) \((\d+) FOR vs (\d+) AGAINST\)/i);
  if (voteResolved) {
    const passed = voteResolved[2] === "PASSED";
    return {
      ...base,
      actorClass: "NONE",
      actorDisplayName: CLASS_LABEL.NONE,
      title: `${passed ? "Закон принят" : "Закон отклонен"}: ${policyLabelClean(voteResolved[1])}.`,
      details: [`Голоса: ${voteResolved[3]} за, ${voteResolved[4]} против.`],
      category: "VOTE",
    };
  }

  const cubesDrawn = message.match(/Drawn (\d+) cubes.*?(?:classes\s*([^;]+);\s*votes\s*(.+))?\./i);
  if (cubesDrawn) {
    return {
      ...base,
      actorClass: "NONE",
      actorDisplayName: CLASS_LABEL.NONE,
      title: `Из мешка достали кубики: ${cubesDrawn[1]} шт.`,
      details: [
        cubesDrawn[2] ? `Состав: ${formatVoteCubeSummary(cubesDrawn[2])}.` : "",
        cubesDrawn[3] ? `Итог: ${formatInterpretedVoteSummary(cubesDrawn[3])}.` : "",
      ].filter(Boolean),
      category: "VOTE",
    };
  }

  const influence = message.match(/([a-z_]+) committed (\d+) influence/i);
  if (influence) {
    const amount = Number(influence[2]);
    if (amount <= 0) {
      return undefined;
    }
    return actionEntry(base, classFromRaw(influence[1]), `${playerLabel(influence[1])} вложил ${amount} влияния в голосование.`, []);
  }

  const built = message.match(/([a-z_]+) built enterprise ([a-z0-9_-]+) for (\d+)/i);
  if (built) {
    return actionEntry(base, classFromRaw(built[1]), `${playerLabel(built[1])} построил предприятие: ${objectLabel(built[2], state)} за ${built[3]} монет.`, []);
  }

  const sold = message.match(/([a-z_]+) sold enterprise ([a-z0-9_-]+) for (\d+)/i);
  if (sold) {
    return actionEntry(base, classFromRaw(sold[1]), `${playerLabel(sold[1])} продал предприятие: ${objectLabel(sold[2], state)} за ${sold[3]} монет.`, []);
  }

  const exported = message.match(/([a-z_]+) exported via ([a-z0-9_-]+): (.+)\./i);
  if (exported) {
    return actionEntry(base, classFromRaw(exported[1]), `${playerLabel(exported[1])} продал ресурсы на внешнем рынке.`, []);
  }

  const manual = conciseManualEvent(base, message);
  if (manual) {
    return manual;
  }

  if (event.type === "WAGES_SYNCED") {
    return {
      ...base,
      actorClass: "NONE",
      actorDisplayName: CLASS_LABEL.NONE,
      title: message,
      details: [],
      category: "ACTION",
    };
  }

  if (event.type === "PRODUCTION_RESOLVED") {
    return phaseEntry(base, "Производство завершено.");
  }
  if (event.type === "SCORING_RESOLVED") {
    return {
      ...base,
      actorClass: "NONE",
      actorDisplayName: CLASS_LABEL.NONE,
      title: `\u041f\u043e\u0434\u0441\u0447\u0435\u0442 \u043e\u0447\u043a\u043e\u0432 \u0437\u0430 \u0440\u0430\u0443\u043d\u0434 ${state.lastScoringSummary?.round ?? state.currentRound} \u0437\u0430\u0432\u0435\u0440\u0448\u0435\u043d.`,
      details: scoringDetails(state.scoringBreakdown ?? state.lastScoringSummary?.players ?? []),
      category: "SCORING",
    };
  }
  if (event.type === "PREPARATION_RESOLVED") {
    return phaseEntry(base, "Подготовка раунда завершена.");
  }
  return undefined;
}

function conciseManualEvent(
  base: Omit<PublicGameLogEntry, "title" | "details" | "category">,
  message: string,
): PublicGameLogEntry | undefined {
  let match = message.match(/Manual money transfer (\d+) from (.+) to (.+)\./i);
  if (match) {
    return actionEntry(base, "NONE", `Перенесены деньги: ${match[1]} монет.`, [`Откуда: ${shortPartyLabel(match[2])}. Куда: ${shortPartyLabel(match[3])}.`]);
  }
  match = message.match(/Manual resource transfer (\d+) ([a-z_]+) from (.+) to (.+)\./i);
  if (match) {
    return actionEntry(base, "NONE", `Перенесен ресурс: ${match[1]} ${resourceLabel(match[2])}.`, [`Откуда: ${shortPartyLabel(match[3])}. Куда: ${shortPartyLabel(match[4])}.`]);
  }
  match = message.match(/adjusted victory points for (.+) by (-?\d+)/i);
  if (match) {
    const delta = Number(match[2]);
    const sign = delta > 0 ? "+" : "";
    return actionEntry(base, classFromRaw(match[1]), `Очки победы изменены: ${playerLabel(match[1])} ${sign}${delta}.`, []);
  }
  match = message.match(/removed unemployed worker ([a-z0-9_-]+)/i);
  if (match) {
    return actionEntry(base, "NONE", `Рабочий убран с безработицы: #${workerNo(match[1])}.`, []);
  }
  match = message.match(/removed voting cubes: (.+)\./i);
  if (match) {
    return actionEntry(base, "NONE", `Из мешка удалены кубики: ${formatManualVotingCubeSummary(match[1])}.`, []);
  }
  match = message.match(/added voting cubes: (.+)\./i);
  if (match) {
    return actionEntry(base, "NONE", `В мешок добавлены кубики: ${formatManualVotingCubeSummary(match[1])}.`, []);
  }
  match = message.match(/removed (\d+) ([A-Z_]+) voting cube/i);
  if (match) {
    return actionEntry(base, "NONE", `Из мешка удалены кубики: ${cubeOwnerLabel(match[2])}, ${match[1]} шт.`, []);
  }
  match = message.match(/returned (\d+) drawn voting cube/i);
  if (match) {
    return actionEntry(base, "NONE", `Кубики возвращены в мешок: ${match[1]} шт.`, []);
  }
  match = message.match(/changed proposal target for ([A-Z0-9_]+) to ([ABC])/i);
  if (match) {
    return actionEntry(base, "NONE", `Предложенный курс изменен: ${policyLabelClean(match[1])} -> ${match[2]}.`, []);
  }
  return undefined;
}

function shortPartyLabel(raw: string): string {
  const value = raw.trim();
  if (value === "TREASURY") return "казна";
  if (value === "STATE_SERVICES") return "госуслуги";
  return playerLabel(value);
}

function cubeOwnerLabel(raw: string): string {
  if (raw === "WORKER") return "рабочие";
  if (raw === "MIDDLE_CLASS") return "средний класс";
  if (raw === "CAPITALIST") return "капиталисты";
  return raw;
}

function workerNo(workerId: string): string {
  const match = workerId.match(/(\d+)$/);
  return match ? match[1] : workerId;
}

function formatManualVotingCubeSummary(raw: string): string {
  return raw
    .split(",")
    .map((part) => {
      const [owner, amount] = part.trim().split("=");
      if (!owner || !amount) {
        return "";
      }
      return `${cubeOwnerLabel(owner.trim())} ${amount.trim()} шт.`;
    })
    .filter(Boolean)
    .join(", ");
}

function policyLabelClean(policyId: string): string {
  const clean: Record<string, string> = {
    POLICY_1_FISCAL: "бюджет",
    POLICY_2_LABOR_MARKET: "рынок труда",
    POLICY_3_TAXATION: "налоги",
    POLICY_4_HEALTHCARE_AND_BENEFITS: "здравоохранение и льготы",
    POLICY_5_EDUCATION: "образование",
    POLICY_6_FOREIGN_TRADE: "внешняя торговля",
    POLICY_7_IMMIGRATION: "иммиграция",
  };
  return clean[policyId] ?? policyId;
}

function phaseEntry(base: Omit<PublicGameLogEntry, "title" | "details" | "category">, title: string): PublicGameLogEntry {
  return {
    ...base,
    actorClass: "NONE",
    actorDisplayName: CLASS_LABEL.NONE,
    title,
    details: [],
    category: "PHASE",
  };
}

function withAutoma(entry: PublicGameLogEntry, automa?: PendingAutomaDecision): PublicGameLogEntry {
  if (!automa) {
    return entry;
  }
  entry.details.push(...automaDetails(automa));
  entry.eventIds.unshift(automa.eventId);
  return entry;
}

function automaDetails(automa: PendingAutomaDecision): string[] {
  if (automa.skippedActions.length === 0) {
    return [];
  }
  return [
    `Автома дошла до этого варианта после недоступных действий: ${automa.skippedActions
      .slice(0, 3)
      .map((action, index) => `${action} (${automa.skippedReasons[index] ?? "недоступно"})`)
      .join("; ")}.`,
  ];
}

function parseAutomaDecision(event: EventLogEntry): PendingAutomaDecision {
  const message = event.message ?? "";
  const skipped = [...message.matchAll(/Slot \d+ unavailable: ([A-Z0-9_]+)/g)].map((match) => match[1]);
  return {
    actorClass: detectLogFaction(event),
    skippedActions: skipped.map((action) => ACTION_LABEL[action] ?? action),
    skippedReasons: skipped.map((reason) => reasonLabel(reason)),
    eventId: event.id,
    message,
  };
}

function detectLogFaction(entry: { type: string; message: string }): ClassType | "NONE" {
  const haystack = `${entry.type} ${entry.message}`.toUpperCase();
  if (haystack.includes("MIDDLE_CLASS") || haystack.includes("MIDDLE CLASS")) return "MIDDLE_CLASS";
  if (haystack.includes("CAPITALIST")) return "CAPITALIST";
  if (haystack.includes("WORKER")) return "WORKER";
  if (haystack.includes("STATE")) return "STATE";
  return "NONE";
}

function classFromRaw(raw: string): ClassType | "NONE" {
  const upper = raw.toUpperCase().trim();
  if (upper === "WORKER" || upper === "WORKING_CLASS") return "WORKER";
  if (upper === "MIDDLE_CLASS" || upper === "MIDDLE CLASS") return "MIDDLE_CLASS";
  if (upper === "CAPITALIST") return "CAPITALIST";
  if (upper === "STATE") return "STATE";
  return "NONE";
}

function playerLabel(playerId: string): string {
  const classType = classFromRaw(playerId);
  return CLASS_LABEL[classType] ?? playerId;
}

function recipientLabel(raw: string): string {
  const normalized = raw.trim();
  if (normalized === "treasury") return "казна государства";
  if (normalized === "external market") return "внешний рынок";
  return playerLabel(normalized);
}

function objectLabel(id: string, state: GameState): string {
  const enterprise = state.enterprises.find((item) => item.id === id);
  if (enterprise?.name) return enterprise.name;
  const deal = state.businessDealCards.find((item) => item.id === id);
  if (deal?.title) return deal.title;
  const exportCard = state.exportCards.find((item) => item.cardId === id);
  if (exportCard?.title) return exportCard.title;
  return STATIC_OBJECT_LABEL[id] ?? id.replace(/[_-]+/g, " ");
}

function workerPublicLabel(id: string): string {
  const match = id.match(/(\d+)$/);
  return match ? `#${match[1]}` : id.replace(/[_-]+/g, " ");
}

function slotPublicLabel(id: string): string {
  const match = id.match(/slot-(\d+)$/i);
  return match ? `слот ${match[1]}` : id.replace(/[_-]+/g, " ");
}

function qualificationPublicLabel(raw: string): string {
  const normalized = raw.toUpperCase();
  if (normalized === "SKILLED") return "квалифицированный";
  return "неквалифицированный";
}

function workerColorPublicLabel(raw: string): string {
  const normalized = raw.toUpperCase();
  if (normalized === "GREEN" || normalized === "FOOD") return "продовольственный";
  if (normalized === "BLUE" || normalized === "LUXURY") return "работник роскоши";
  if (normalized === "ORANGE" || normalized === "EDUCATION") return "образовательный";
  if (normalized === "PURPLE" || normalized === "MEDIA" || normalized === "INFLUENCE" || normalized === "MEDIA_INFLUENCE") return "медиа";
  if (normalized === "WHITE" || normalized === "RED" || normalized === "HEALTHCARE") return "медицинский";
  return "серый неквалифицированный";
}

function migrationReasonLabel(raw: string): string {
  if (/before worker turn/i.test(raw)) return "Карта миграции открыта перед ходом рабочего класса.";
  return "Карта миграции обработана.";
}

function policyLabel(policyId: string): string {
  return POLICY_LABEL[policyId] ?? policyId;
}

function formatVoteCubeSummary(raw: string): string {
  return raw
    .split(",")
    .map((part) => {
      const [owner, count] = part.split("=").map((value) => value.trim());
      const color = VOTING_CUBE_COLOR_LABEL[owner];
      return `${CLASS_LABEL[owner] ?? owner}${color ? `, ${color}` : ""} - ${count}`;
    })
    .join(", ");
}

function formatInterpretedVoteSummary(raw: string): string {
  const label: Record<string, string> = {
    FOR: "за",
    AGAINST: "против",
    NEUTRAL: "нейтрально",
  };
  return raw
    .split(",")
    .map((part) => {
      const [vote, count] = part.split("=").map((value) => value.trim());
      return `${label[vote] ?? vote} - ${count}`;
    })
    .join(", ");
}

function resourceLabel(resourceId: string): string {
  return RESOURCE_LABEL[resourceId] ?? resourceId.replace(/[_-]+/g, " ");
}

function storageLabel(raw: string): string {
  if (/public services/i.test(raw)) return "запас государственных услуг";
  if (/goods\/services/i.test(raw)) return "зона товаров и услуг покупателя";
  if (/produced resource/i.test(raw)) return "склад произведенных ресурсов";
  return raw.replace(/\.$/, "");
}

function humanizeSupplierLine(raw: string, resourceId: string): string {
  const match = raw.match(/([A-Z_]+)(?:\(([^)]*)\))?: qty=(\d+), cost=(\d+)/i);
  if (!match) return raw;
  const supplier = match[2] ? recipientLabel(match[2]) : supplierTypeLabel(match[1]);
  const quantity = Number(match[3]);
  const cost = Number(match[4]);
  const price = quantity > 0 ? Math.round((cost / quantity) * 100) / 100 : cost;
  return `${supplier}: ${quantity} ${resourceLabel(resourceId)} за ${cost} монет, цена ${price}.`;
}

function supplierTypeLabel(raw: string): string {
  if (raw === "STATE") return "государство";
  if (raw === "EXTERNAL_MARKET") return "внешний рынок";
  if (raw === "CAPITALIST") return "капиталисты";
  if (raw === "MIDDLE_CLASS") return "средний класс";
  return raw;
}

function formatStorageLine(raw: string, place: string): string[] {
  const parts = raw
    .split(",")
    .map((part) => part.trim())
    .filter(Boolean)
    .map((part) => {
      const [resource, amount] = part.split("=").map((value) => value.trim());
      return `${amount} ${resourceLabel(resource)}`;
    });
  return parts.length > 0 ? [`Получено ${parts.join(", ")} ${place}.`] : [];
}

function exportOperationDetail(raw: string, state: GameState): string {
  const match = raw.match(/([a-z_]+) x(\d+)/i);
  if (!match) return raw;
  const resourceId = match[1];
  const quantity = Number(match[2]);
  const offer = state.activeExportCard?.offers?.find((candidate) => candidate.resourceId === resourceId && candidate.quantity === quantity)
    ?? state.exportCards.flatMap((card) => card.offers ?? []).find((candidate) => candidate.resourceId === resourceId && candidate.quantity === quantity);
  const revenue = offer?.revenue ?? 0;
  return `${quantity} ${resourceLabel(resourceId)}${revenue > 0 ? `, выручка ${revenue} монет` : ""}.`;
}

function productionDetails(state: GameState, production: GameState["lastProductionSummary"] | GameState["productionPhaseState"]): string[] {
  if (!production) return ["Детальная сводка производства пока недоступна."];
  const details: string[] = [];
  for (const result of production.enterpriseResults ?? []) {
    const produced = Object.entries(result.producedResources ?? {})
      .map(([resource, amount]) => `${amount} ${resourceLabel(resource)}`)
      .join(", ");
    const wages = Object.entries(result.wagesPaidByRecipient ?? {})
      .map(([playerId, amount]) => `${playerLabel(playerId)}: ${amount}`)
      .join(", ");
    if (produced || wages) {
      details.push(`${objectLabel(result.enterpriseId, state)}: ${produced || "без выпуска"}; зарплаты ${wages || "0"}.`);
    }
  }
  details.push(`Еда рабочего класса: нужно ${production.workerFoodRequired}, получено ${production.workerFoodConsumed}, нехватка ${production.workerFoodUnmet}.`);
  details.push(`Еда среднего класса: нужно ${production.middleClassFoodRequired}, получено ${production.middleClassFoodConsumed}, нехватка ${production.middleClassFoodUnmet}.`);
  details.push(`Налоги: рабочие ${production.workerTaxesPaid}, средний класс ${production.middleClassTaxesPaid}, капиталисты ${production.capitalistTaxesPaid}.`);
  return details;
}

function scoringDetails(rows: PlayerScoringBreakdown[]): string[] {
  if (!rows || rows.length === 0) return ["Подробная разбивка очков пока недоступна."];
  return rows.flatMap((row) => {
    const header = `${CLASS_LABEL[row.classType] ?? row.playerId}: было ${row.accumulatedBeforePhase}, +${row.gainedThisPhase}, стало ${row.totalAfterPhase} ПО.`;
    const sources = (row.sources ?? [])
      .filter((source) => source.vpDelta !== 0 || source.note)
      .map((source) => `  ${source.vpDelta >= 0 ? "+" : ""}${source.vpDelta} ПО: ${translateScoringSource(source.sourceId, source.note)}`);
    return [header, ...sources];
  });
}

function translateScoringSource(sourceId: string, note?: string): string {
  if (sourceId === "capitalist_wealth_track_base") return note?.replace("Capital", "Капитал").replace("gives", "дает").replace("VP on the wealth track.", "ПО по треку богатства.") ?? "трек богатства";
  if (sourceId === "capitalist_wealth_track_growth") return note?.replace("Wealth marker advanced", "Маркер богатства продвинулся на").replace("step(s) beyond previous maximum.", "шаг(а) выше прежнего максимума.") ?? "рост богатства";
  if (sourceId === "capitalist_revenue_to_capital") return note?.replace("Moved", "Перенесено").replace("money from revenue to capital before wealth scoring.", "монет из дохода в капитал перед подсчетом.") ?? "доход перенесен в капитал";
  if (sourceId === "persisted_vp_from_supported_actions") return "в этой фазе дополнительных ПО по текущему срезу правил нет";
  return note ?? sourceId;
}

function translateTaxFormula(raw: string): string {
  return raw
    .replace(/employment/gi, "налог за занятость")
    .replace(/profit/gi, "налог на прибыль")
    .replace(/income/gi, "подоходный налог")
    .replace(/functioning enterprise\(s\)/gi, "работающих предприятий")
    .replace(/worker\(s\) on other employers/gi, "рабочих у других работодателей")
    .replace(/multiplier/gi, "множитель")
    .replace(/cash covered/gi, "покрыто деньгами")
    .replace(/revenue base/gi, "база дохода")
    .replace(/rate/gi, "ставка");
}

function translateFinalScore(raw: string): string {
  return raw
    .replace(/policies/gi, "политики")
    .replace(/resources/gi, "ресурсы")
    .replace(/cash/gi, "деньги");
}

function humanizeWorkerLog(raw: string): string {
  return raw
    .replace(/worker-worker-(\d+)/gi, "рабочий #$1")
    .replace(/assigned worker/gi, "назначен рабочий")
    .replace(/assigned to enterprise/gi, "назначен на предприятие")
    .replace(/and tied by labor contract/gi, "и заключен трудовой договор")
    .replace(/slot/gi, "слот")
    .trim();
}

function reasonLabel(reason: string): string {
  return reason
    .replace(/_/g, " ")
    .toLowerCase();
}

function publicRejectedMessage(message: string): string {
  const codes = [...message.matchAll(/[A-Z][A-Z0-9_]+/g)].map((match) => reasonLabel(match[0]));
  return codes.length > 0
    ? `Действие невозможно: ${codes.join("; ")}.`
    : "Действие невозможно: выбранный вариант сейчас недоступен.";
}

function translateFallback(message: string): string {
  return message
    .replace(/Game initialized from setup-spec for (\d+) players\./i, "Игра подготовлена на $1 игроков.")
    .replace(/migration-card-\d+/gi, "карта миграции")
    .replace(/worker-worker-(\d+)/gi, "рабочий #$1")
    .replace(/UNSKILLED\/GRAY/gi, "неквалифицированный серый рабочий")
    .replace(/SKILLED\/GREEN/gi, "квалифицированный продовольственный рабочий")
    .replace(/SKILLED\/BLUE/gi, "квалифицированный работник роскоши")
    .replace(/SKILLED\/ORANGE/gi, "квалифицированный образовательный работник")
    .replace(/SKILLED\/PURPLE/gi, "квалифицированный медиа-работник")
    .replace(/SKILLED\/WHITE|SKILLED\/RED/gi, "квалифицированный медицинский работник")
    .replace(/Legacy demo actions are enabled only as explicitly marked legacy moves\./i, "Демо-действия доступны только как явно помеченные legacy-ходы.")
    .replace(/State paid (\d+) interest for (\d+) loan\(s\)\./i, "Государство выплатило $1 процентов по займам: $2.")
    .replace(/Worker class gained (\d+) influence from unions\./i, "Рабочий класс получил $1 влияния от профсоюзов.")
    .replace(/Released tied contracts after production: (\d+) workers\./i, "После производства сняты трудовые договоры: $1 рабочих.")
    .replace(/IMF intervention check skipped because loans are not modeled\./i, "Проверка МВФ пропущена: займы пока не моделируются.");
}

function isNoopEvent(event: EventLogEntry): boolean {
  const message = event.message ?? "";
  return /no actions affecting state/i.test(message);
}
