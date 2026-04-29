import type { ClassType, EventLogEntry, GameState } from "@/types/game";

export type PublicLogSeverity = "INFO" | "WARNING" | "ERROR";
export type PublicLogCategory = "ACTION" | "PHASE" | "VOTE" | "PRODUCTION" | "REJECTION" | "AUTOMA_DECISION";

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

const ACTION_LABEL: Record<string, string> = {
  BUILD_ENTERPRISE: "Построить предприятие",
  ASSIGN_WORKERS: "Назначить работников",
  MAKE_DEAL: "Заключить сделку",
  SELL_TO_FOREIGN_MARKET: "Продать на внешнем рынке",
  PROPOSE_BILL: "Внести законопроект",
  APPLY_POLITICAL_PRESSURE: "Оказать политическое давление",
  LOBBY: "Лоббировать интересы",
  BUY_GOODS_AND_SERVICES: "Купить товары и услуги",
  CONSUME_HEALTHCARE: "Получить медицинскую помощь",
  CONSUME_EDUCATION: "Получить образование",
  CONSUME_LUXURY: "Использовать предметы роскоши",
  REPAY_LOAN: "Погасить заём",
  GET_BENEFITS: "Получить льготы",
  CHANGE_PRICES: "Изменить цены",
  BUY_STORAGE_IF_OVERFLOW: "Купить хранилище",
};

const POLICY_LABEL: Record<string, string> = {
  POLICY_1_FISCAL: "фискальной политике",
  POLICY_2_LABOR_MARKET: "политике на рынке труда",
  POLICY_3_TAXATION: "налоговой политике",
  POLICY_4_HEALTHCARE_AND_BENEFITS: "здравоохранению и льготам",
  POLICY_5_EDUCATION: "образованию",
  POLICY_6_FOREIGN_TRADE: "внешней торговле",
  POLICY_7_IMMIGRATION: "миграционной политике",
};

const RESOURCE_LABEL: Record<string, string> = {
  food: "продовольствие",
  FOOD: "продовольствие",
  luxury: "предметы роскоши",
  LUXURY: "предметы роскоши",
  healthcare: "медицинские услуги",
  HEALTHCARE: "медицинские услуги",
  education: "образовательные услуги",
  EDUCATION: "образовательные услуги",
  influence: "влияние",
  INFLUENCE: "влияние",
  money: "деньги",
  capital: "капитал",
  revenue: "выручка",
};

const REASON_LABEL: Record<string, string> = {
  NO_AVAILABLE_WORKERS_FOR_FUNCTIONING_ENTERPRISE: "нет подходящих работников, чтобы предприятие сразу заработало",
  POLICY_TAG_TARGET_NOT_AVAILABLE: "нет доступной политики для законопроекта",
  NO_EXPORT_OPERATION_EXECUTABLE: "нечего продать на внешнем рынке",
  NO_PENDING_PROPOSAL_FOR_POLICY: "по этой политике сейчас нет внесённого законопроекта",
  NOT_IN_VOTING_PHASE: "это действие доступно только во время голосования",
  NOT_CURRENT_PLAYER: "это действие может выполнить только текущий игрок",
  NOT_CURRENT_VOTING_STAGE: "сейчас другой этап голосования",
  CANNOT_RESOLVE_BEFORE_ALL_STANCES: "сначала все игроки должны объявить позицию",
  CANNOT_RESOLVE_BEFORE_ALL_INFLUENCE_COMMITS: "сначала все игроки должны подтвердить влияние",
  INFLUENCE_ALREADY_COMMITTED: "этот игрок уже подтвердил влияние",
  STANCE_ALREADY_SUBMITTED: "этот игрок уже объявил позицию",
  INSUFFICIENT_INFLUENCE_ADVANTAGE: "недостаточно преимущества по влиянию",
  INSUFFICIENT_BAG_CUBE_ADVANTAGE: "недостаточно преимущества по кубикам в мешочке",
  NOT_ENOUGH_MONEY: "недостаточно денег",
  NO_AVAILABLE_DEAL: "нет доступной сделки",
  NO_AVAILABLE_ENTERPRISE: "нет доступного предприятия",
  NO_VALID_TARGET: "нет подходящей цели",
  UNSUPPORTED_ACTION: "действие пока не реализовано в движке",
};

const STATIC_OBJECT_LABEL: Record<string, string> = {
  business_deal_01: "сделку",
  "business-deal-01": "сделку",
  "export-card-01": "текущую карту экспорта",
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
  selectedAction?: string;
  snapVoteSkippedReasons: string[];
  eventId: number;
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
      pendingAutoma = undefined;
    }
    entries.push(mapped);
  }

  return entries.map((entry, index) => ({ ...entry, turnNumber: index + 1 })).reverse().slice(0, 12);
}

function mapEvent(event: EventLogEntry, state: GameState, automa?: PendingAutomaDecision): PublicGameLogEntry | undefined {
  const actorClass = detectLogFaction(event);
  const base = {
    id: `public-log-${event.id}`,
    round: state.currentRound,
    turnNumber: 0,
    actorClass,
    actorDisplayName: CLASS_LABEL[actorClass] ?? CLASS_LABEL.NONE,
    severity: "INFO" as PublicLogSeverity,
    eventIds: [event.id],
  };
  const message = event.message ?? "";

  const wagePaid = message.match(/([a-z_]+) paid (\d+) wages to ([a-z_]+).*?for (\d+) worker\(s\) at ([a-z0-9_-]+)/i);
  if (wagePaid) {
    return {
      ...base,
      title: `${playerLabel(wagePaid[1])} заплатил ${wagePaid[2]} монет игроку ${playerLabel(wagePaid[3])}.`,
      details: [`Зарплата за ${wagePaid[4]} рабоч(их) на предприятии «${objectLabel(wagePaid[5], state)}».`],
      category: "PRODUCTION",
    };
  }

  const produced = message.match(/([a-z_]+) produced (\d+) ([a-z_]+) from ([a-z0-9_-]+) into (.+)\.?/i);
  if (produced) {
    return {
      ...base,
      title: `${playerLabel(produced[1])} произвел ${produced[2]} ${resourceLabel(produced[3])}.`,
      details: [`Источник: «${objectLabel(produced[4], state)}». Куда: ${produced[5]}.`],
      category: "PRODUCTION",
    };
  }

  const taxPaid = message.match(/([a-z_]+) paid (\d+) .*taxes? to treasury:? ?(.+)?/i);
  if (taxPaid) {
    return {
      ...base,
      actorClass: classFromRaw(taxPaid[1]),
      actorDisplayName: CLASS_LABEL[classFromRaw(taxPaid[1])] ?? CLASS_LABEL.NONE,
      title: `${playerLabel(taxPaid[1])} заплатил ${taxPaid[2]} монет налогов в казну.`,
      details: taxPaid[3] ? [taxPaid[3]] : [],
      category: "PRODUCTION",
    };
  }

  const foodPaid = message.match(/([a-z_]+) paid (\d+) to ([a-z_ ]+) for (\d+) food at price (\d+)/i);
  if (foodPaid) {
    return {
      ...base,
      title: `${playerLabel(foodPaid[1])} заплатил ${foodPaid[2]} монет за еду.`,
      details: [`Получатель: ${playerLabel(foodPaid[3].trim())}. Куплено: ${foodPaid[4]} еды по цене ${foodPaid[5]}.`],
      category: "PRODUCTION",
    };
  }

  const resourceTransfer = message.match(/([a-z_]+) paid (\d+) to ([a-z_ ]+) for (\d+) ([a-z_]+)/i);
  if (resourceTransfer) {
    return {
      ...base,
      title: `${playerLabel(resourceTransfer[1])} заплатил ${resourceTransfer[2]} монет за ${resourceLabel(resourceTransfer[5])}.`,
      details: [`Получатель: ${playerLabel(resourceTransfer[3].trim())}. Передано ресурса: ${resourceTransfer[4]}.`],
      category: "ACTION",
    };
  }

  const preliminaryVote = message.match(/Preliminary vote result before influence: FOR (\d+) \/ AGAINST (\d+)/i);
  if (preliminaryVote) {
    return {
      ...base,
      actorClass: "NONE",
      actorDisplayName: CLASS_LABEL.NONE,
      title: `Предварительный результат голосования: за ${preliminaryVote[1]}, против ${preliminaryVote[2]}.`,
      details: ["Влияние вкладывается только после объявления этого результата."],
      category: "VOTE",
    };
  }

  const returnedWorker = message.match(/([a-z0-9_-]+) was returned to unemployed by ([a-z_]+)/i);
  if (returnedWorker) {
    return {
      ...base,
      actorClass: classFromRaw(returnedWorker[2]),
      actorDisplayName: CLASS_LABEL[classFromRaw(returnedWorker[2])] ?? CLASS_LABEL.NONE,
      title: `${playerLabel(returnedWorker[2])} вернул рабочего ${returnedWorker[1]} на клетку безработных.`,
      details: ["Рабочий не был связан трудовым договором."],
      category: "ACTION",
    };
  }

  if (event.type === "ACTION_REJECTED" || event.type === "ACTION_FAILED") {
    return {
      ...base,
      title: publicRejectedMessage(message),
      details: [],
      severity: "WARNING",
      category: "REJECTION",
      actorClass: actorClass === "NONE" ? "NONE" : actorClass,
    };
  }

  const built = message.match(/built enterprise ([a-z0-9_-]+) for (\d+)/i);
  if (built) {
    return withAutomaDetails(
      {
        ...base,
        actorClass: "CAPITALIST",
        actorDisplayName: CLASS_LABEL.CAPITALIST,
        title: `Капиталисты построили предприятие «${objectLabel(built[1], state)}» за ${built[2]}.`,
        details: ["Предприятие добавлено в частный сектор."],
        category: "ACTION",
      },
      automa,
    );
  }

  const deal = message.match(/made deal ([a-z0-9_-]+) \(cost=(\d+), free_trade_zone=\{([^}]*)}, regular_storage=\{([^}]*)}/i);
  if (deal) {
    const details = [
      ...formatStorageLine(deal[3], "отправили в зону свободной торговли"),
      ...formatStorageLine(deal[4], "положили в обычные хранилища"),
    ];
    return withAutomaDetails(
      {
        ...base,
        actorClass: "CAPITALIST",
        actorDisplayName: CLASS_LABEL.CAPITALIST,
        title: `Капиталисты заключили сделку за ${deal[2]}.`,
        details,
        category: "ACTION",
      },
      automa,
    );
  }

  const exportMatch = message.match(/exported via [a-z0-9_-]+: ([a-zA-Z_]+) x(\d+)/i);
  if (exportMatch) {
    return withAutomaDetails(
      {
        ...base,
        actorClass: "CAPITALIST",
        actorDisplayName: CLASS_LABEL.CAPITALIST,
        title: `Капиталисты продали на внешнем рынке ${exportMatch[2]} ${resourceLabel(exportMatch[1])}.`,
        details: [],
        category: "ACTION",
      },
      automa,
    );
  }

  const proposed = message.match(/([A-Z_]+|[a-z_]+) proposed bill on ([A-Z0-9_]+) from ([ABC]) to ([ABC])/i);
  if (proposed) {
    return withAutomaDetails(
      {
        ...base,
        actorClass: classFromRaw(proposed[1]),
        actorDisplayName: CLASS_LABEL[classFromRaw(proposed[1])] ?? CLASS_LABEL.NONE,
        title: `${CLASS_LABEL[classFromRaw(proposed[1])] ?? "Игрок"} внесли законопроект по ${policyLabel(proposed[2])}: курс ${proposed[3]} → ${proposed[4]}.`,
        details: snapVoteDetails(automa),
        category: "VOTE",
      },
      automa,
      false,
    );
  }

  if (/spent 1 influence and started an extraordinary vote/i.test(message)) {
    const policy = message.match(/on ([A-Z0-9_]+)/i)?.[1];
    return {
      ...base,
      title: `Началось внеочередное голосование${policy ? ` по ${policyLabel(policy)}` : ""}.`,
      details: ["Мешочек не пополняется: используются только кубики, которые уже лежат внутри."],
      category: "VOTE",
    };
  }

  const stance = message.match(/declared ([A-Z]+) for ([A-Z0-9_]+)/i);
  if (stance) {
    return {
      ...base,
      title: `${base.actorDisplayName} объявили позицию «${stance[1] === "FOR" ? "за" : "против"}» по ${policyLabel(stance[2])}.`,
      details: [],
      category: "VOTE",
    };
  }

  const commit = message.match(/committed (\d+) influence to vote on ([A-Z0-9_]+)/i);
  if (commit) {
    return {
      ...base,
      title: `${base.actorDisplayName} вложили ${commit[1]} влияния в голосование по ${policyLabel(commit[2])}.`,
      details: [],
      category: "VOTE",
    };
  }

  const voteResolved = message.match(/Vote .*?([A-Z0-9_]+).*?(PASSED|REJECTED).*?(\d+).*?(?:FOR|for).*?(\d+)/i);
  if (voteResolved) {
    return {
      ...base,
      actorClass: "NONE",
      actorDisplayName: CLASS_LABEL.NONE,
      title: `Голосование по ${policyLabel(voteResolved[1])}: ${voteResolved[2] === "PASSED" ? "законопроект принят" : "законопроект отклонён"}.`,
      details: [`Итог: ${voteResolved[3]} за, ${voteResolved[4]} против.`],
      category: "VOTE",
    };
  }

  if (/Extraordinary vote finished/i.test(message)) {
    return {
      ...base,
      actorClass: "NONE",
      actorDisplayName: CLASS_LABEL.NONE,
      title: "Внеочередное голосование завершено.",
      details: ["Фаза действий продолжается."],
      category: "VOTE",
    };
  }

  if (/Advanced to production phase/i.test(message)) {
    return phaseEntry(base, "Фаза действий завершена. Начинается фаза производства.");
  }
  if (/Advanced to scoring phase/i.test(message)) {
    return phaseEntry(base, "Фаза производства завершена. Начинается подсчёт очков.");
  }
  if (/Production phase resolved/i.test(message)) {
    return {
      ...base,
      actorClass: "NONE",
      actorDisplayName: CLASS_LABEL.NONE,
      title: "Фаза производства разрешена.",
      details: ["Зарплаты, производство, потребности и налоги обработаны движком."],
      category: "PRODUCTION",
    };
  }

  if (/added \d+ .* voting cubes/i.test(message)) {
    const amount = message.match(/added (\d+)/i)?.[1] ?? "0";
    return {
      ...base,
      title: `${base.actorDisplayName} добавили ${amount} кубика в мешочек голосования.`,
      details: [],
      category: "VOTE",
    };
  }

  return undefined;
}

function withAutomaDetails<T extends PublicGameLogEntry>(entry: T, automa?: PendingAutomaDecision, includeSnapDetails = true): T {
  if (!automa) {
    return entry;
  }
  const skipped = automa.skippedActions.slice(0, 2);
  const reasons = automa.skippedReasons.slice(0, 2);
  if (skipped.length > 0) {
    const reasonText = skipped
      .map((action, index) =>
        action === "вариант действия"
          ? (reasons[index] ?? "вариант недоступен")
          : `${action.toLowerCase()} — нельзя: ${reasons[index] ?? "вариант недоступен"}`,
      )
      .join("; ");
    entry.details.push(`Автома выбрала это действие, потому что первые варианты были недоступны: ${reasonText}.`);
  }
  if (includeSnapDetails) {
    entry.details.push(...snapVoteDetails(automa));
  }
  return entry;
}

function snapVoteDetails(automa?: PendingAutomaDecision): string[] {
  if (!automa || automa.snapVoteSkippedReasons.length === 0) {
    return [];
  }
  return ["Внеочередное голосование не объявлено: у автора нет достаточного преимущества."];
}

function parseAutomaDecision(event: EventLogEntry): PendingAutomaDecision {
  const message = event.message ?? "";
  const skipped = [...message.matchAll(/Slot \d+ unavailable: ([A-Z0-9_]+)/g)];
  const selected = message.match(/Slot \d+: ([A-Z0-9_]+) executed/i)?.[1];
  const snapSkipped = message.match(/Snap vote skipped \(([^)]*)\)/i)?.[1] ?? "";
  return {
    actorClass: detectLogFaction(event),
    skippedActions: skipped.map((_, index) => {
      const slotAction = message.match(new RegExp(`Slot ${index + 1}: ([A-Z0-9_]+)`, "i"))?.[1];
      return ACTION_LABEL[slotAction ?? ""] ?? "вариант действия";
    }),
    skippedReasons: skipped.map((match) => REASON_LABEL[match[1]] ?? "вариант недоступен"),
    selectedAction: selected ? ACTION_LABEL[selected] : undefined,
    snapVoteSkippedReasons: snapSkipped.split(",").map((item) => item.trim()).filter(Boolean),
    eventId: event.id,
  };
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

function publicRejectedMessage(message: string): string {
  const codes = [...message.matchAll(/[A-Z][A-Z0-9_]+/g)].map((match) => match[0]);
  const translated = codes.map((code) => REASON_LABEL[code]).filter((line): line is string => Boolean(line));
  if (translated.length > 0) {
    return `Действие невозможно: ${translated.join(" ")}`;
  }
  if (/Policy has no pending proposal/i.test(message)) {
    return "Действие невозможно: по этой политике сейчас нет внесённого законопроекта.";
  }
  return "Действие невозможно: выбранный вариант сейчас недоступен.";
}

function isNoopEvent(event: EventLogEntry): boolean {
  const message = event.message ?? "";
  return /нет действий, влияющих на состояние/i.test(message) || /no actions affecting state/i.test(message);
}

function detectLogFaction(entry: { type: string; message: string }): ClassType | "NONE" {
  const haystack = `${entry.type} ${entry.message}`.toUpperCase();
  if (haystack.includes("MIDDLE_CLASS") || haystack.includes("MIDDLE CLASS")) {
    return "MIDDLE_CLASS";
  }
  if (haystack.includes("CAPITALIST") || haystack.includes("КАПИТАЛИСТ")) {
    return "CAPITALIST";
  }
  if (haystack.includes("WORKER") || haystack.includes("РАБОЧ")) {
    return "WORKER";
  }
  if (haystack.includes("STATE") || haystack.includes("ГОСУДАР")) {
    return "STATE";
  }
  return "NONE";
}

function classFromRaw(raw: string): ClassType | "NONE" {
  const upper = raw.toUpperCase();
  if (upper === "WORKER" || upper === "WORKING_CLASS") {
    return "WORKER";
  }
  if (upper === "MIDDLE_CLASS") {
    return "MIDDLE_CLASS";
  }
  if (upper === "CAPITALIST") {
    return "CAPITALIST";
  }
  if (upper === "STATE") {
    return "STATE";
  }
  return "NONE";
}

function objectLabel(id: string, state: GameState): string {
  const enterprise = state.enterprises.find((item) => item.id === id);
  if (enterprise?.name) {
    return enterprise.name;
  }
  const deal = state.businessDealCards.find((item) => item.id === id);
  if (deal?.title) {
    return deal.title;
  }
  const exportCard = state.exportCards.find((item) => item.cardId === id);
  if (exportCard?.title) {
    return exportCard.title;
  }
  return STATIC_OBJECT_LABEL[id] ?? "объект";
}

function policyLabel(policyId: string): string {
  return POLICY_LABEL[policyId] ?? "выбранной политике";
}

function resourceLabel(resourceId: string): string {
  return RESOURCE_LABEL[resourceId] ?? "ресурс";
}

function playerLabel(playerId: string): string {
  const classType = classFromRaw(playerId);
  return CLASS_LABEL[classType] ?? playerId;
}

function formatStorageLine(raw: string, verb: string): string[] {
  const parts = raw
    .split(",")
    .map((part) => part.trim())
    .filter(Boolean)
    .map((part) => {
      const [resource, amount] = part.split("=").map((value) => value.trim());
      return `${amount} ${resourceLabel(resource)}`;
    });
  if (parts.length === 0) {
    return [];
  }
  return [`${parts.join(" и ")} ${verb}.`];
}
