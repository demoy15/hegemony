import type { AvailableInteraction, BoardRenderable } from "@/features/board/model/types";
import type { ActionType, ComposerMetadata, GameState, LegalMove, PolicyId } from "@/types/game";

interface BuildAvailableInteractionsInput {
  selectedEntity?: BoardRenderable;
  selectedZoneId?: string;
  legalMoves: LegalMove[];
  composerMetadata: ComposerMetadata;
}

const ACTION_LABEL: Record<ActionType, string> = {
  ADVANCE_TO_VOTING: "К голосованию",
  ADVANCE_TO_PRODUCTION: "К производству",
  RESOLVE_PRODUCTION_PHASE: "Завершить производство",
  ADVANCE_TO_SCORING: "К подсчету",
  RESOLVE_SCORING_PHASE: "Завершить подсчет",
  ADVANCE_TO_NEXT_ROUND: "К следующему раунду",
  RESOLVE_PREPARATION_PHASE: "Завершить подготовку",
  ADVANCE_GAME_FLOW: "Следующий шаг",
  ADVANCE_ROUND: "Следующий раунд",
  DECLARE_VOTE_STANCE: "Позиция по голосованию",
  DRAW_VOTING_CUBES: "Достать кубики",
  COMMIT_VOTE_INFLUENCE: "Вложить влияние",
  PROPOSE_BILL: "Предложить закон",
  ADD_VOTING_CUBES: "Добавить кубики",
  CALL_EXTRAORDINARY_VOTE: "Внеочередное голосование",
  BUILD_ENTERPRISE: "Построить предприятие",
  SELL_ENTERPRISE: "Продать предприятие",
  SELL_ON_EXTERNAL_MARKET: "Продать на внешнем рынке",
  MAKE_BUSINESS_DEAL: "Заключить сделку",
  LOBBY_INTERESTS: "Лоббировать",
  CHANGE_PRICES: "Изменить цены",
  CHANGE_WAGES: "Изменить зарплаты",
  PAY_BONUS: "Выплатить бонус",
  BUY_STORAGE: "Купить хранилище",
  TAKE_STATE_BENEFITS: "Получить льготы",
  REPAY_LOAN: "Погасить заем",
  RESPOND_TO_EVENT: "Отреагировать на событие",
  MEET_DEPUTIES: "Встретиться с депутатами",
  INTRODUCE_EXTRA_TAX: "Дополнительный налог",
  RUN_CAMPAIGN: "Провести кампанию",
  ASSIGN_WORKERS: "Назначить рабочих",
  PLACE_STRIKES: "Забастовка",
  PLACE_DEMONSTRATION: "Демонстрация",
  BUY_GOODS_AND_SERVICES: "Покупка товаров",
  CONSUME_HEALTHCARE: "Потребить ресурс",
  CONSUME_EDUCATION: "Потребить ресурс",
  CONSUME_LUXURY: "Потребить ресурс",
  REFRESH_BUSINESS_DEALS: "Обновить сделки",
  START_TURN: "Начать ход",
  HIRE_WORKER: "Нанять рабочего",
  PRODUCE_GOODS: "Произвести товары",
  SELL_GOODS: "Продать товары",
  ADJUST_POLICY: "Сдвиг политики",
  PLAY_CARD: "Ручная корректировка",
  END_TURN: "Завершить ход",
};

const MANUAL_OVERRIDE_ACTIONS: ActionType[] = [
  "START_TURN",
  "HIRE_WORKER",
  "PRODUCE_GOODS",
  "SELL_GOODS",
  "ADJUST_POLICY",
  "PLAY_CARD",
  "ASSIGN_WORKERS",
  "PLACE_STRIKES",
  "PLACE_DEMONSTRATION",
  "BUY_GOODS_AND_SERVICES",
  "PROPOSE_BILL",
  "ADD_VOTING_CUBES",
  "CALL_EXTRAORDINARY_VOTE",
  "BUILD_ENTERPRISE",
  "SELL_ENTERPRISE",
  "SELL_ON_EXTERNAL_MARKET",
  "MAKE_BUSINESS_DEAL",
  "LOBBY_INTERESTS",
  "CHANGE_PRICES",
  "CHANGE_WAGES",
  "PAY_BONUS",
  "BUY_STORAGE",
  "TAKE_STATE_BENEFITS",
  "REPAY_LOAN",
  "RESPOND_TO_EVENT",
  "MEET_DEPUTIES",
  "INTRODUCE_EXTRA_TAX",
  "RUN_CAMPAIGN",
  "DECLARE_VOTE_STANCE",
  "DRAW_VOTING_CUBES",
  "COMMIT_VOTE_INFLUENCE",
  "CONSUME_HEALTHCARE",
  "CONSUME_EDUCATION",
  "CONSUME_LUXURY",
  "REFRESH_BUSINESS_DEALS",
];

const MANUAL_DISABLED_ACTIONS: ActionType[] = [
  "ADVANCE_GAME_FLOW",
  "ADVANCE_TO_VOTING",
  "ADVANCE_TO_PRODUCTION",
  "RESOLVE_PRODUCTION_PHASE",
  "ADVANCE_TO_SCORING",
  "RESOLVE_SCORING_PHASE",
  "ADVANCE_TO_NEXT_ROUND",
  "RESOLVE_PREPARATION_PHASE",
  "ADVANCE_ROUND",
  "END_TURN",
];

const CONSUME_ACTIONS: ActionType[] = ["CONSUME_LUXURY", "CONSUME_EDUCATION", "CONSUME_HEALTHCARE"];

const ENTITY_LABELS: Record<string, string> = {
  supermarket: "супермаркет",
  mall: "торговый центр",
  college: "колледж",
  polyclinic: "поликлиника",
  state_hospital: "гос. больница",
  state_university: "гос. университет",
  state_media: "гос. медиа",
  mini_market: "минимаркет",
  private_clinic: "частная клиника",
};

type StateServiceResourceType = "HEALTHCARE" | "EDUCATION" | "INFLUENCE";

function selectedStateServiceResource(selectedEntity?: BoardRenderable): StateServiceResourceType | undefined {
  const markerId = selectedEntity?.id?.startsWith("state-service-stock:")
    ? selectedEntity.id
    : selectedEntity?.sourceRef?.sourceId?.startsWith("state-service-stock:")
      ? selectedEntity.sourceRef.sourceId
      : undefined;

  if (!markerId) {
    return undefined;
  }

  const group = markerId.slice("state-service-stock:".length);
  if (group === "hospital") {
    return "HEALTHCARE";
  }
  if (group === "university") {
    return "EDUCATION";
  }
  if (group === "media") {
    return "INFLUENCE";
  }
  return undefined;
}

function compactEntityId(raw: string): string {
  return ENTITY_LABELS[raw] ?? raw.replace(/[_-]+/g, " ");
}

function compactSummary(summary: string): string {
  return summary
    .replace(/Assign up to (\d+) workers using assignment operations\.?/gi, "Назначьте до $1 рабочих через операции назначения.")
    .replace(/Propose bill on ([A-Z0-9_]+) to ([ABC])\.?/gi, "Предложите закон по $1 и переведите его на курс $2.")
    .replace(/Add 3 voting cubes for ([a-z_]+)\.?/gi, "Добавьте 3 кубика своего цвета в мешок.")
    .replace(/Call extraordinary vote on ([A-Z0-9_]+)\.?/gi, "Потратьте 1 влияние и проведите внеочередное голосование по $1.")
    .replace(/Buy ([A-Z_]+) from (\d+) supplier\(s\)\.?/gi, "Купите $1 у поставщиков: $2.")
    .replace(/Advance one safe lifecycle step\.?/gi, "Продвиньте один безопасный шаг жизненного цикла.")
    .replace(/Action exists in supported slice but is currently illegal in this state\.?/gi, "Действие поддерживается, но сейчас недоступно по правилам.")
    .replace(/Consume healthcare for population and increase welfare\.?/gi, "Потребите медицину для населения и повысьте благосостояние.")
    .replace(/Consume education for population and increase welfare\.?/gi, "Потребите образование для населения и повысьте благосостояние.")
    .replace(/Consume luxury for population and increase welfare\.?/gi, "Потребите роскошь для населения и повысьте благосостояние.")
    .replace(/\bFOOD\b/g, "еду")
    .replace(/\bHEALTHCARE\b/g, "медицину")
    .replace(/\bEDUCATION\b/g, "образование")
    .replace(/\bLUXURY\b/g, "роскошь")
    .replace(/\bworker-worker-(\d+)\b/gi, "рабочий #$1")
    .replace(/\b([a-z]+(?:_[a-z]+)*)-slot-(\d+)\b/gi, (_, enterpriseId, slotNo) => `${compactEntityId(String(enterpriseId))} слот ${slotNo}`)
    .replace(
      /\b(supermarket|mall|college|polyclinic|state_hospital|state_university|state_media|mini_market|private_clinic)\b/gi,
      (value) => compactEntityId(String(value).toLowerCase()),
    );
}

function actionPriority(actionType: ActionType): number {
  if (actionType === "CONSUME_LUXURY" || actionType === "CONSUME_EDUCATION" || actionType === "CONSUME_HEALTHCARE") {
    return 2;
  }
  if (actionType === "PROPOSE_BILL" || actionType === "CALL_EXTRAORDINARY_VOTE" || actionType === "ADD_VOTING_CUBES" || actionType === "ASSIGN_WORKERS") {
    return 1;
  }
  if (actionType === "PLACE_STRIKES" || actionType === "PLACE_DEMONSTRATION") {
    return 1;
  }
  if (actionType === "DECLARE_VOTE_STANCE" || actionType === "DRAW_VOTING_CUBES" || actionType === "COMMIT_VOTE_INFLUENCE") {
    return 2;
  }
  if (actionType.startsWith("ADVANCE_") || actionType.startsWith("RESOLVE_")) {
    return 3;
  }
  return 10;
}

function matchesSelectedContext(actionType: ActionType, selectedEntity?: BoardRenderable, selectedZoneId?: string): boolean {
  if (actionType === "PLAY_CARD") {
    return true;
  }

  const stateServiceResource = selectedStateServiceResource(selectedEntity);
  if (stateServiceResource) {
    return stateServiceResource !== "INFLUENCE" && actionType === "BUY_GOODS_AND_SERVICES";
  }

  if (!selectedEntity && !selectedZoneId) {
    return actionType !== "REFRESH_BUSINESS_DEALS";
  }

  if (selectedEntity?.sourceRef?.sourceType === "policy") {
    return actionType === "PROPOSE_BILL" || actionType === "CALL_EXTRAORDINARY_VOTE" || actionType === "DECLARE_VOTE_STANCE" || actionType === "DRAW_VOTING_CUBES" || actionType === "COMMIT_VOTE_INFLUENCE";
  }

  if (selectedEntity?.sourceRef?.sourceType === "businessDeal") {
    return actionType === "REFRESH_BUSINESS_DEALS";
  }

  if (selectedEntity?.sourceRef?.sourceType === "enterpriseMarket") {
    return actionType === "BUILD_ENTERPRISE";
  }

  if (selectedEntity?.sourceRef?.sourceType === "exportCard") {
    return false;
  }

  if (selectedEntity?.kind === "ENTERPRISE" && selectedEntity.sourceRef?.sourceType !== "enterprise") {
    return false;
  }

  if (selectedEntity?.sourceRef?.sourceType === "enterprise" || selectedEntity?.sourceRef?.sourceType === "worker") {
    return actionType === "ASSIGN_WORKERS";
  }

  if (selectedZoneId === "unemployed") {
    return actionType === "ASSIGN_WORKERS" || actionType === "PLACE_DEMONSTRATION";
  }

  if (selectedZoneId === "vote_results") {
    return actionType === "ADD_VOTING_CUBES" || actionType === "DECLARE_VOTE_STANCE" || actionType === "DRAW_VOTING_CUBES" || actionType === "COMMIT_VOTE_INFLUENCE" || actionType === "ADVANCE_TO_PRODUCTION";
  }

  if (selectedZoneId === "round_track") {
    return actionType.startsWith("ADVANCE_") || actionType.startsWith("RESOLVE_");
  }

  if (selectedZoneId?.startsWith("policy:")) {
    return actionType === "PROPOSE_BILL" || actionType === "CALL_EXTRAORDINARY_VOTE";
  }

  if (selectedZoneId === "policy_track") {
    return actionType === "PROPOSE_BILL" || actionType === "CALL_EXTRAORDINARY_VOTE";
  }

  if (selectedZoneId === "deals") {
    return actionType === "REFRESH_BUSINESS_DEALS";
  }

  if (selectedZoneId === "export") {
    return false;
  }

  return true;
}

export function buildAvailableInteractions({
  selectedEntity,
  selectedZoneId,
  legalMoves,
  composerMetadata,
}: BuildAvailableInteractionsInput): AvailableInteraction[] {
  const legalByAction = legalMoves.reduce<Map<ActionType, LegalMove[]>>((acc, move) => {
    const list = acc.get(move.actionType) ?? [];
    list.push(move);
    acc.set(move.actionType, list);
    return acc;
  }, new Map());

  const legalInteractions: AvailableInteraction[] = [];
  const legalConsumeActionTypes = CONSUME_ACTIONS.filter((actionType) => legalByAction.has(actionType));

  if (legalConsumeActionTypes.length > 0 && matchesSelectedContext(legalConsumeActionTypes[0], selectedEntity, selectedZoneId)) {
    const legalConsumeMoves = legalConsumeActionTypes.flatMap((actionType) => legalByAction.get(actionType) ?? []);
    const summaryParts = legalConsumeActionTypes.map((actionType) => {
      if (actionType === "CONSUME_LUXURY") {
        return "роскошь";
      }
      if (actionType === "CONSUME_EDUCATION") {
        return "образование";
      }
      return "медицина";
    });
    legalInteractions.push({
      id: "legal:CONSUME_RESOURCE",
      actionType: legalConsumeActionTypes[0],
      label: "Потребить ресурс",
      summary: compactSummary(legalConsumeMoves[0]?.summary ?? `Доступно: ${summaryParts.join(", ")}.`),
      enabled: true,
      legalMove: legalConsumeMoves[0],
      template: legalConsumeMoves[0]?.actionType
        ? composerMetadata.actionTemplates.find((candidate) => candidate.actionType === legalConsumeMoves[0].actionType)
        : undefined,
    });
  }

  legalByAction.forEach((moves, actionType) => {
    if (actionType === "PLAY_CARD" || actionType === "ADVANCE_GAME_FLOW") {
      return;
    }
    if (CONSUME_ACTIONS.includes(actionType)) {
      return;
    }
    if (!matchesSelectedContext(actionType, selectedEntity, selectedZoneId)) {
      return;
    }
    legalInteractions.push({
      id: `legal:${actionType}`,
      actionType,
      label: ACTION_LABEL[actionType] ?? actionType,
      summary: compactSummary(moves[0]?.summary ?? "Доступно сейчас"),
      enabled: true,
      legalMove: moves[0],
      template: composerMetadata.actionTemplates.find((template) => template.actionType === actionType),
    });
  });

  const templateByAction = composerMetadata.actionTemplates.reduce<Map<ActionType, (typeof composerMetadata.actionTemplates)[number]>>((acc, template) => {
    acc.set(template.actionType, template);
    return acc;
  }, new Map());

  const manualActionTypes = new Set<ActionType>([
    ...MANUAL_OVERRIDE_ACTIONS,
    ...composerMetadata.actionTemplates.map((template) => template.actionType),
  ]);
  const manualDisabledActions = new Set<ActionType>(MANUAL_DISABLED_ACTIONS);

  const unavailableInContext: AvailableInteraction[] = [...manualActionTypes]
    .filter((actionType) => !manualDisabledActions.has(actionType))
    .filter((actionType) => !CONSUME_ACTIONS.includes(actionType))
    .filter((actionType) => !legalByAction.has(actionType))
    .filter((actionType) => matchesSelectedContext(actionType, selectedEntity, selectedZoneId))
    .map((actionType) => {
      const template = templateByAction.get(actionType);
      return {
        id: `manual:${actionType}`,
        actionType,
        label: ACTION_LABEL[actionType] ?? actionType,
        summary: compactSummary(template?.summary ?? "Ручное действие доступно игроку."),
        enabled: true,
        template,
      };
    });

  const hasConsumeTemplate = CONSUME_ACTIONS.some((actionType) => templateByAction.has(actionType));
  const hasConsumeLegal = CONSUME_ACTIONS.some((actionType) => legalByAction.has(actionType));
  const consumeContextMatches = matchesSelectedContext("CONSUME_HEALTHCARE", selectedEntity, selectedZoneId);
  if (hasConsumeTemplate && !hasConsumeLegal && consumeContextMatches) {
    const firstTemplate = CONSUME_ACTIONS.map((actionType) => templateByAction.get(actionType)).find((template) => Boolean(template));
    unavailableInContext.push({
      id: "manual:CONSUME_RESOURCE",
      actionType: "CONSUME_LUXURY",
      label: "Потребить ресурс",
      summary: compactSummary(firstTemplate?.summary ?? "Потребите доступный ресурс для повышения благосостояния."),
      enabled: true,
      template: firstTemplate,
    });
  }

  return [...legalInteractions, ...unavailableInContext].sort((a, b) => {
    const prio = actionPriority(a.actionType) - actionPriority(b.actionType);
    if (prio !== 0) {
      return prio;
    }
    return a.actionType.localeCompare(b.actionType);
  });
}

export function buildActionSeedParameters(
  actionType: ActionType,
  actorPlayerId: string,
  selectedEntity?: BoardRenderable,
  state?: GameState,
): Record<string, unknown> {
  const stateServiceResource = selectedStateServiceResource(selectedEntity);

  if (actionType === "PROPOSE_BILL") {
    const sourcePolicy =
      selectedEntity?.sourceRef?.sourceType === "policy" ? selectedEntity.sourceRef.sourceId : "POLICY_1_FISCAL";
    return {
      actorPlayerId,
      policyId: sourcePolicy,
      targetCourse: "B",
    };
  }

  if (actionType === "ASSIGN_WORKERS") {
    if (selectedEntity?.sourceRef?.sourceType === "enterprise" && state) {
      const actor = state.players.find((player) => player.playerId === actorPlayerId);
      const enterprise = state.enterprises.find((item) => item.id === selectedEntity.sourceRef?.sourceId);
      const returnAssignments = enterprise?.slots
        .map((slot) => state.workers.find((worker) => worker.id === slot.occupiedWorkerId))
        .filter((worker): worker is GameState["workers"][number] =>
          Boolean(worker && actor && worker.classType === actor.classType && worker.location === "ENTERPRISE_SLOT" && !worker.tiedContract),
        )
        .map((worker) => ({ workerId: worker.id, targetType: "UNEMPLOYED", targetId: "unemployed" })) ?? [];
      if (returnAssignments.length > 0) {
        return {
          actorPlayerId,
          assignments: returnAssignments,
          selectedWorkerIds: returnAssignments.map((assignment) => assignment.workerId),
          selectedWorkerId: returnAssignments[0]?.workerId ?? "",
        };
      }
    }
    return {
      actorPlayerId,
      assignments: [],
    };
  }

  if (actionType === "CALL_EXTRAORDINARY_VOTE") {
    const sourcePolicy = pendingProposalPolicyIdForActor(state, actorPlayerId, selectedEntity) ?? "POLICY_1_FISCAL";
    return {
      actorPlayerId,
      policyId: sourcePolicy,
    };
  }

  if (actionType === "BUILD_ENTERPRISE") {
    const selectedMarketEnterprise =
      selectedEntity?.sourceRef?.sourceType === "enterpriseMarket" && state
        ? state.capitalistEnterpriseMarket.find((enterprise) => enterprise.id === selectedEntity.sourceRef?.sourceId)
        : undefined;
    const firstMarketEnterprise = state?.capitalistEnterpriseMarket?.[0];
    const enterprise = selectedMarketEnterprise ?? firstMarketEnterprise;
    return {
      actorPlayerId,
      enterpriseId: enterprise?.id ?? "",
      cost: enterprise?.cost ?? 20,
      wageLevel: 2,
    };
  }

  if (actionType === "DECLARE_VOTE_STANCE") {
    const voteActorPlayerId = firstMissingVoteActorPlayerId(state, "stance") ?? actorPlayerId;
    return {
      actorPlayerId: voteActorPlayerId,
      policyId: state?.currentVoteState?.activeProposalPolicyId,
      stance: "FOR",
    };
  }

  if (actionType === "COMMIT_VOTE_INFLUENCE") {
    const voteActorPlayerId = firstMissingVoteActorPlayerId(state, "influence") ?? actorPlayerId;
    return {
      actorPlayerId: voteActorPlayerId,
      influenceAmount: 0,
    };
  }

  if (actionType === "DRAW_VOTING_CUBES") {
    const totalCubes =
      (state?.votingBag.worker ?? 0) + (state?.votingBag.middleClass ?? 0) + (state?.votingBag.capitalist ?? 0);
    return {
      actorPlayerId,
      count: Math.min(5, Math.max(1, totalCubes)),
    };
  }

  if (actionType === "BUY_GOODS_AND_SERVICES") {
    return {
      actorPlayerId,
      resourceType:
        stateServiceResource === "HEALTHCARE" || stateServiceResource === "EDUCATION" ? stateServiceResource : "FOOD",
      purchases: [],
      buyQuantityBySupplier: {},
      buyPriceBySupplier: {},
    };
  }

  if (actionType === "PLACE_STRIKES") {
    return {
      actorPlayerId,
      enterpriseIds: [],
    };
  }

  if (actionType === "PLACE_DEMONSTRATION") {
    return {
      actorPlayerId,
      penaltyAllocation: {},
    };
  }

  if (actionType === "PLAY_CARD") {
    const firstPendingPolicy = state?.policies.find((policy) => policy.occupyingProposalToken)?.id ?? "";
    const victoryPointSeeds = Object.fromEntries(
      (state?.players ?? []).map((player) => [`manualVictoryPointsDelta_${player.playerId}`, 0]),
    );
    return {
      actorPlayerId,
      cardId: "",
      manualMode: false,
      manualSection: "resources",
      manualStateToActorMoney: 0,
      manualCapitalistToActorMoney: 0,
      manualActorToStateMoney: 0,
      manualMoneySourcePlayerId: "",
      manualSourceToActorMoney: 0,
      manualMoneyTargetPlayerId: "",
      manualActorToPlayerMoney: 0,
      manualTransferMoneySourceId: "TREASURY",
      manualTransferMoneyTargetId: actorPlayerId,
      manualTransferMoneyAmount: 0,
      manualTransferResourceSourceId: "STATE_SERVICES",
      manualTransferResourceTargetId: actorPlayerId,
      manualTransferResourceType: "FOOD",
      manualTransferResourceAmount: 0,
      manualExchangeLeftId: "STATE",
      manualExchangeRightId: actorPlayerId,
      manualExchangeLeftToRightMoney: 0,
      manualExchangeRightToLeftMoney: 0,
      manualExchangeLeftToRightResourceType: "FOOD",
      manualExchangeRightToLeftResourceType: "FOOD",
      manualExchangeLeftToRightResourceAmount: 0,
      manualExchangeRightToLeftResourceAmount: 0,
      manualRemoveUnemployedWorkerIds: [],
      manualTakeResourceSourceId: "STATE_SERVICES",
      manualTakeResourceType: "FOOD",
      manualTakeResourceAmount: 0,
      manualGiveResourceTargetId: "STATE_SERVICES",
      manualGiveResourceType: "FOOD",
      manualGiveResourceAmount: 0,
      manualVotePolicyId: firstPendingPolicy,
      manualVoteReturnDrawn: false,
      manualDiscardCubeOwner: "WORKER",
      manualDiscardCubeCount: 0,
      manualDiscardWorkerCubes: 0,
      manualDiscardMiddleClassCubes: 0,
      manualDiscardCapitalistCubes: 0,
      manualAddWorkerCubes: 0,
      manualAddMiddleClassCubes: 0,
      manualAddCapitalistCubes: 0,
      manualProposalTargetCourse: "",
      manualVoteDrawCount: 0,
      manualVoteWorkerCubes: 0,
      manualVoteMiddleClassCubes: 0,
      manualVoteCapitalistCubes: 0,
      manualActorMoneyDelta: 0,
      manualTreasuryDelta: 0,
      manualWelfareDelta: 0,
      manualWorkersColor: "GRAY",
      manualWorkersCount: 0,
      ...victoryPointSeeds,
      ...(stateServiceResource ? { [`manualStateResource_${stateServiceResource}`]: 1 } : {}),
    };
  }

  if (CONSUME_ACTIONS.includes(actionType)) {
    return {
      actorPlayerId,
      consumeSelectedActionType: actionType,
      consumeAvailableActionTypes: CONSUME_ACTIONS,
    };
  }

  return {
    actorPlayerId,
  };
}

function tokenOwnerPlayerId(policy: GameState["policies"][number], state: GameState): string | undefined {
  const token = policy.occupyingProposalToken;
  if (!token) {
    return undefined;
  }
  if (token.ownerPlayerId) {
    return token.ownerPlayerId;
  }
  return state.players.find((player) => player.classType === token.ownerClass)?.playerId;
}

function selectedPolicyId(selectedEntity?: BoardRenderable): PolicyId | undefined {
  return selectedEntity?.sourceRef?.sourceType === "policy" ? (selectedEntity.sourceRef.sourceId as PolicyId) : undefined;
}

function pendingProposalPolicyIdForActor(
  state: GameState | undefined,
  actorPlayerId: string,
  selectedEntity?: BoardRenderable,
): PolicyId | undefined {
  if (!state) {
    return selectedPolicyId(selectedEntity);
  }
  const selectedId = selectedPolicyId(selectedEntity);
  if (selectedId) {
    const selectedPolicy = state.policies.find((policy) => policy.id === selectedId);
    if (selectedPolicy?.occupyingProposalToken && tokenOwnerPlayerId(selectedPolicy, state) === actorPlayerId) {
      return selectedId;
    }
  }
  return state.policies.find((policy) => policy.occupyingProposalToken && tokenOwnerPlayerId(policy, state) === actorPlayerId)?.id;
}

function firstMissingVoteActorPlayerId(state: GameState | undefined, kind: "stance" | "influence"): string | undefined {
  const session = state?.currentVoteState;
  if (!state || !session) {
    return undefined;
  }
  const alreadyDone =
    kind === "stance"
      ? new Set(Object.keys(session.stanceByPlayer ?? {}))
      : new Set(Object.keys(session.influenceCommitments ?? {}));
  const eligiblePlayers =
    kind === "influence"
      ? state.players.filter((player) => Object.prototype.hasOwnProperty.call(session.stanceByPlayer ?? {}, player.playerId))
      : state.players;
  return eligiblePlayers.find((player) => !alreadyDone.has(player.playerId))?.playerId;
}
