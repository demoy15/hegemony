import initialResponse from "@/lib/initial-backend-state.json";
import type {
  ActionPreviewDelta,
  ActionType,
  BotTurnResponse,
  BotUntilHumanResponse,
  ClassType,
  CommandResponse,
  ComposerActionTemplate,
  ComposerMetadata,
  DomainEvent,
  Enterprise,
  GameResponse,
  GameState,
  LegalMove,
  PlayerControlMode,
  PolicyCourse,
  PolicyId,
  PreviewActionResponse,
  SaveLoadResponse,
} from "@/types/game";
interface SetupPayload {
  playerCount: number;
  controlModes: Record<string, string>;
  botStrategyModes: Record<string, string>;
}

const STATE_STORAGE_KEY = "hegemony-mobile-offline-state";
const SAVE_PREFIX = "hegemony-mobile-save:";
const ACTIONS_PER_PLAYER = 5;
const CLASS_ORDER: ClassType[] = ["WORKER", "MIDDLE_CLASS", "CAPITALIST", "STATE"];
const PLAYER_CLASS_ORDER_BY_COUNT: Record<number, ClassType[]> = {
  2: ["WORKER", "CAPITALIST"],
  3: ["WORKER", "CAPITALIST", "MIDDLE_CLASS"],
  4: ["WORKER", "CAPITALIST", "MIDDLE_CLASS", "STATE"],
};

type AssignmentTargetType = "ENTERPRISE_SLOT" | "UNION" | "UNEMPLOYED";

interface AssignmentDraft {
  workerId: string;
  targetType: AssignmentTargetType;
  targetId: string;
}

interface PurchaseDraft {
  supplierType: "CAPITALIST" | "MIDDLE_CLASS" | "STATE" | "EXTERNAL_MARKET";
  supplierPlayerId?: string;
  quantity: number;
  unitPriceOverride?: number;
}

const initial = initialResponse as unknown as GameResponse;

function clone<T>(value: T): T {
  return JSON.parse(JSON.stringify(value)) as T;
}

function toInt(value: unknown, fallback = 0): number {
  if (typeof value === "number" && Number.isFinite(value)) {
    return Math.trunc(value);
  }
  const parsed = Number.parseInt(String(value ?? ""), 10);
  return Number.isFinite(parsed) ? parsed : fallback;
}

function toString(value: unknown, fallback = ""): string {
  return value === null || value === undefined ? fallback : String(value);
}

function toRecord(value: unknown): Record<string, unknown> {
  return value && typeof value === "object" && !Array.isArray(value) ? value as Record<string, unknown> : {};
}

function activeClassesForCount(playerCount: number): ClassType[] {
  return PLAYER_CLASS_ORDER_BY_COUNT[Math.max(2, Math.min(4, playerCount))] ?? PLAYER_CLASS_ORDER_BY_COUNT[4];
}

function freshState(): GameState {
  const state = clone(initial.gameState);
  state.eventLog = [];
  state.currentRound = 1;
  state.currentPhase = "ACTIONS";
  state.gameStatus = "IN_PROGRESS";
  state.gameOver = false;
  state.turnOrder = {
    ...state.turnOrder,
    currentPlayerIndex: 0,
    phase: "ACTIONS",
    round: 1,
    activeClasses: state.players.map((player) => player.classType),
    actionsPerPlayer: ACTIONS_PER_PLAYER,
    actionsTakenByPlayer: state.players.map(() => 0),
  };
  state.currentVoteState = undefined;
  state.lastBotTurnSummary = undefined;
  state.finalResult = undefined;
  refreshEnterpriseFlags(state);
  refreshPlayerDerivedCounts(state);
  return state;
}

function loadState(): GameState {
  const stored = localStorage.getItem(STATE_STORAGE_KEY);
  if (!stored) {
    const state = freshState();
    persistState(state);
    return state;
  }
  try {
    return normalizeState(JSON.parse(stored) as GameState);
  } catch {
    const state = freshState();
    persistState(state);
    return state;
  }
}

function persistState(state: GameState): void {
  localStorage.setItem(STATE_STORAGE_KEY, JSON.stringify(state));
}

function normalizeState(state: GameState): GameState {
  const activeClasses = state.turnOrder?.activeClasses?.length
    ? state.turnOrder.activeClasses
    : state.players.map((player) => player.classType);
  const players = state.players.filter((player) => activeClasses.includes(player.classType));
  const normalized = {
    ...state,
    players,
    turnOrder: {
      ...state.turnOrder,
      activeClasses,
      actionsPerPlayer: state.turnOrder?.actionsPerPlayer ?? ACTIONS_PER_PLAYER,
      actionsTakenByPlayer: Array.isArray(state.turnOrder?.actionsTakenByPlayer)
        ? state.turnOrder.actionsTakenByPlayer.slice(0, players.length)
        : players.map(() => 0),
      currentPlayerIndex: Math.min(Math.max(0, state.turnOrder?.currentPlayerIndex ?? 0), Math.max(0, players.length - 1)),
    },
  };
  refreshEnterpriseFlags(normalized);
  refreshPlayerDerivedCounts(normalized);
  return normalized;
}

function currentPlayer(state: GameState) {
  const index = Number.isInteger(state.turnOrder?.currentPlayerIndex) ? state.turnOrder.currentPlayerIndex : 0;
  return state.players[index] ?? state.players[0];
}

function playerById(state: GameState, playerId: string) {
  return state.players.find((player) => player.playerId === playerId);
}

function playerByClass(state: GameState, classType: ClassType) {
  return state.players.find((player) => player.classType === classType);
}

function classCubeKey(classType: ClassType): keyof GameState["votingBag"] | undefined {
  if (classType === "WORKER") return "worker";
  if (classType === "MIDDLE_CLASS") return "middleClass";
  if (classType === "CAPITALIST") return "capitalist";
  return undefined;
}

function event(state: GameState, type: string, message: string): DomainEvent {
  const id = (state.eventLog.at(-1)?.id ?? 0) + 1;
  state.eventLog.push({ id, type, message });
  return { type, description: message };
}

function resourceKey(resourceType: string): string {
  const upper = resourceType.toUpperCase();
  if (upper === "FOOD") return "food";
  if (upper === "HEALTHCARE") return "healthcare";
  if (upper === "EDUCATION") return "education";
  if (upper === "LUXURY") return "luxury";
  return resourceType.toLowerCase();
}

function actionTemplate(actionType: ActionType, summary: string, template: Record<string, unknown> = {}): ComposerActionTemplate {
  return {
    actionType,
    summary,
    supported: true,
    supportNote: "Автономный мобильный MVP-движок.",
    template,
    futureModifierSlot: false,
  };
}

function composerMetadata(state: GameState): ComposerMetadata {
  const actor = currentPlayer(state);
  const actorPlayerId = actor?.playerId ?? state.players[0]?.playerId ?? "";
  const templates: ComposerActionTemplate[] = [
    actionTemplate("ADD_VOTING_CUBES", "Добавить 3 кубика своего класса.", { actorPlayerId, amount: 3 }),
    actionTemplate("PROPOSE_BILL", "Предложить законопроект.", { actorPlayerId }),
    actionTemplate("ASSIGN_WORKERS", "Назначить работников на предприятия.", { actorPlayerId, assignments: [] }),
    actionTemplate("BUY_GOODS_AND_SERVICES", "Купить товары или услуги.", { actorPlayerId, resourceType: "FOOD", purchases: [] }),
    actionTemplate("CONSUME_LUXURY", "Потребить ресурс.", { actorPlayerId, consumeSelectedActionType: "CONSUME_LUXURY" }),
    actionTemplate("END_TURN", "Завершить ход.", { actorPlayerId }),
    actionTemplate("ADVANCE_GAME_FLOW", "Продвинуть фазу.", { actorPlayerId }),
    actionTemplate("DECLARE_VOTE_STANCE", "Объявить позицию.", { actorPlayerId }),
    actionTemplate("DRAW_VOTING_CUBES", "Достать кубики.", { actorPlayerId, count: 5 }),
    actionTemplate("COMMIT_VOTE_INFLUENCE", "Вложить влияние.", { actorPlayerId, influenceAmount: 0 }),
  ];
  return {
    actorPlayerId,
    actionTemplates: templates,
    modifierAvailable: false,
    modifierAvailabilityNote: "Автономный режим не использует внешние модификаторы.",
    unavailableActionNotes: [],
  };
}

function legalMove(actionType: ActionType, summary: string, template: Record<string, unknown>): LegalMove {
  return {
    id: `${actionType}:${JSON.stringify(template)}`,
    actionType,
    summary,
    legacyDemo: false,
    template,
  };
}

function generatedLegalMoves(state: GameState): LegalMove[] {
  if (state.gameOver || state.gameStatus === "FINISHED") {
    return [];
  }

  const actor = currentPlayer(state);
  const actorPlayerId = actor?.playerId ?? "";
  const moves: LegalMove[] = [];

  if (!actor) {
    return moves;
  }

  if (state.currentPhase === "ACTIONS") {
    const actorIndex = Math.max(0, state.turnOrder.currentPlayerIndex ?? 0);
    const actionsTaken = state.turnOrder.actionsTakenByPlayer?.[actorIndex] ?? 0;
    if (actionsTaken >= ACTIONS_PER_PLAYER) {
      moves.push(legalMove("END_TURN", "End current class turn.", { actorPlayerId }));
      moves.push(legalMove("ADVANCE_GAME_FLOW", "Advance phase if all action turns are spent.", { actorPlayerId }));
      return moves;
    }

    moves.push(legalMove("ADD_VOTING_CUBES", `Добавить 3 кубика: ${actor.classType}.`, { actorPlayerId, amount: 3 }));
    state.policies.forEach((policy) => {
      (["A", "B", "C"] as PolicyCourse[])
        .filter((course) => course !== policy.currentCourse && !policy.occupyingProposalToken)
        .forEach((targetCourse) => {
          moves.push(legalMove("PROPOSE_BILL", `${policy.id} -> ${targetCourse}`, { actorPlayerId, policyId: policy.id, targetCourse }));
        });
    });

    if (actor.classType === "WORKER" || actor.classType === "MIDDLE_CLASS") {
      moves.push(legalMove("ASSIGN_WORKERS", "Визуально назначить работников.", { actorPlayerId, assignments: [] }));
      ["FOOD", "HEALTHCARE", "EDUCATION", "LUXURY"].forEach((resourceType) => {
        moves.push(legalMove("BUY_GOODS_AND_SERVICES", `Купить ${resourceType}.`, { actorPlayerId, resourceType, purchases: [] }));
      });
      moves.push(legalMove("CONSUME_LUXURY", "Потребить роскошь.", { actorPlayerId, consumeSelectedActionType: "CONSUME_LUXURY" }));
      moves.push(legalMove("CONSUME_EDUCATION", "Потребить образование.", { actorPlayerId, consumeSelectedActionType: "CONSUME_EDUCATION" }));
      moves.push(legalMove("CONSUME_HEALTHCARE", "Потребить медицину.", { actorPlayerId, consumeSelectedActionType: "CONSUME_HEALTHCARE" }));
    }

    moves.push(legalMove("END_TURN", "Завершить ход текущего класса.", { actorPlayerId }));
    moves.push(legalMove("ADVANCE_GAME_FLOW", "Перейти к следующей фазе, если ход действий исчерпан.", { actorPlayerId }));
    return moves;
  }

  if (state.currentPhase === "VOTING" && state.currentVoteState) {
    const vote = state.currentVoteState;
    if (vote.votingStage === "DECLARE_STANCES") {
      state.players
        .filter((player) => !Object.prototype.hasOwnProperty.call(vote.stanceByPlayer ?? {}, player.playerId))
        .forEach((player) => {
          moves.push(legalMove("DECLARE_VOTE_STANCE", `${player.classType}: позиция по голосованию.`, {
            actorPlayerId: player.playerId,
            policyId: vote.activeProposalPolicyId,
            stance: player.playerId === vote.proposalAuthorPlayerId ? "FOR" : "AGAINST",
          }));
        });
    }
    if (vote.votingStage === "DRAW_BAG_CUBES") {
      const total = state.votingBag.worker + state.votingBag.middleClass + state.votingBag.capitalist;
      moves.push(legalMove("DRAW_VOTING_CUBES", "Достать кубики из мешка.", { actorPlayerId, count: Math.min(5, Math.max(1, total)) }));
    }
    if (vote.votingStage === "COMMIT_INFLUENCE") {
      state.players
        .filter((player) => !Object.prototype.hasOwnProperty.call(vote.influenceCommitments ?? {}, player.playerId))
        .forEach((player) => {
          moves.push(legalMove("COMMIT_VOTE_INFLUENCE", `${player.classType}: вложить влияние.`, {
            actorPlayerId: player.playerId,
            influenceAmount: 0,
          }));
        });
    }
    moves.push(legalMove("ADVANCE_GAME_FLOW", "Продолжить голосование.", { actorPlayerId }));
    return moves;
  }

  moves.push(legalMove("ADVANCE_GAME_FLOW", "Продвинуть фазу игры.", { actorPlayerId }));
  if (state.currentPhase === "PRODUCTION") {
    moves.push(legalMove("RESOLVE_PRODUCTION_PHASE", "Разрешить производство.", { actorPlayerId }));
  }
  if (state.currentPhase === "SCORING") {
    moves.push(legalMove("RESOLVE_SCORING_PHASE", "Начислить очки.", { actorPlayerId }));
    moves.push(legalMove("ADVANCE_TO_NEXT_ROUND", "Перейти к следующему раунду.", { actorPlayerId }));
  }
  return moves;
}

function validateActor(state: GameState, actionType: ActionType, parameters: Record<string, unknown>): string[] {
  const actor = currentPlayer(state);
  const actorPlayerId = toString(parameters.actorPlayerId, actor?.playerId ?? "");
  if (!actor) return ["NO_CURRENT_PLAYER"];
  if (["DECLARE_VOTE_STANCE", "COMMIT_VOTE_INFLUENCE", "DRAW_VOTING_CUBES"].includes(actionType)) {
    return [];
  }
  if (actorPlayerId !== actor.playerId) {
    return ["NOT_CURRENT_PLAYER"];
  }
  return [];
}

function workerMatchesSlot(worker: GameState["workers"][number], slot: Enterprise["slots"][number]): boolean {
  if (slot.requiredQualification === "UNSKILLED") return true;
  if (worker.qualificationType !== "SKILLED") return false;
  if (!slot.requiredSector) return true;
  return worker.sector === slot.requiredSector;
}

function parseAssignments(raw: unknown): AssignmentDraft[] {
  if (!Array.isArray(raw)) return [];
  return raw
    .map((item): AssignmentDraft => {
      const row = toRecord(item);
      const targetType = toString(row.targetType, "ENTERPRISE_SLOT") as AssignmentTargetType;
      const safeTargetType: AssignmentTargetType =
        targetType === "UNION" || targetType === "UNEMPLOYED" ? targetType : "ENTERPRISE_SLOT";
      return {
        workerId: toString(row.workerId),
        targetType: safeTargetType,
        targetId: toString(row.targetId),
      };
    })
    .filter((item) => item.workerId && item.targetId);
}

function clearWorkerSlot(state: GameState, workerId: string): void {
  state.enterprises.forEach((enterprise) => {
    enterprise.slots.forEach((slot) => {
      if (slot.occupiedWorkerId === workerId) {
        slot.occupiedWorkerId = undefined;
      }
    });
  });
}

function refreshEnterpriseFlags(state: GameState): void {
  state.enterprises.forEach((enterprise) => {
    const requiredSlots = enterprise.slots.filter((slot) => !slot.optional);
    const occupiedRequiredSlots = requiredSlots.filter((slot) => slot.occupiedWorkerId).length;
    enterprise.fullyEmpty = occupiedRequiredSlots === 0;
    enterprise.functioning = Boolean(enterprise.automated && enterprise.slots.length === 0)
      || (requiredSlots.length > 0 && occupiedRequiredSlots === requiredSlots.length);
    enterprise.partiallyFilled = !enterprise.functioning && !enterprise.fullyEmpty;
  });
}

function applyAssignments(state: GameState, assignments: AssignmentDraft[], events: DomainEvent[]): string[] {
  const actor = currentPlayer(state);
  if (!actor) return ["NO_CURRENT_PLAYER"];
  if (assignments.length === 0) return ["NO_ASSIGNMENTS"];
  if (assignments.length > 3) return ["TOO_MANY_WORKERS_IN_ONE_ASSIGN_ACTION"];

  const errors: string[] = [];
  assignments.forEach((assignment) => {
    const worker = state.workers.find((candidate) => candidate.id === assignment.workerId);
    if (!worker) {
      errors.push("WORKER_NOT_FOUND");
      return;
    }
    if (worker.classType !== actor.classType) {
      errors.push("WORKER_CLASS_MISMATCH");
      return;
    }
    if (worker.tiedContract && worker.location === "ENTERPRISE_SLOT") {
      errors.push("WORKER_TIED_BY_CONTRACT");
      return;
    }

    clearWorkerSlot(state, worker.id);
    if (assignment.targetType === "UNEMPLOYED") {
      worker.location = "UNEMPLOYED";
      worker.enterpriseId = undefined;
      worker.slotId = undefined;
      events.push(event(state, "WORKER_ASSIGNED", `Работник ${worker.id} отправлен в безработные.`));
      return;
    }
    if (assignment.targetType === "UNION") {
      worker.location = "UNION";
      worker.enterpriseId = undefined;
      worker.slotId = undefined;
      events.push(event(state, "WORKER_ASSIGNED", `Работник ${worker.id} отправлен в профсоюз.`));
      return;
    }

    const [enterpriseId, slotId] = assignment.targetId.split(":");
    const enterprise = state.enterprises.find((candidate) => candidate.id === enterpriseId);
    const slot = enterprise?.slots.find((candidate) => candidate.id === slotId);
    if (!enterprise || !slot) {
      errors.push("INVALID_TARGET");
      return;
    }
    if (slot.occupiedWorkerId && slot.occupiedWorkerId !== worker.id) {
      errors.push("SLOT_ALREADY_OCCUPIED");
      return;
    }
    if (!workerMatchesSlot(worker, slot)) {
      errors.push("SLOT_QUALIFICATION_MISMATCH");
      return;
    }
    slot.occupiedWorkerId = worker.id;
    worker.location = "ENTERPRISE_SLOT";
    worker.enterpriseId = enterprise.id;
    worker.slotId = slot.id;
    worker.tiedContract = true;
    events.push(event(state, "WORKER_ASSIGNED", `Работник ${worker.id} назначен: ${enterprise.name ?? enterprise.id}.`));
  });

  refreshEnterpriseFlags(state);
  refreshPlayerDerivedCounts(state);
  return errors;
}

function refreshPlayerDerivedCounts(state: GameState): void {
  state.players.forEach((player) => {
    player.availableWorkers = state.workers.filter((worker) => worker.classType === player.classType && worker.location === "UNEMPLOYED").length;
    player.employedWorkers = state.workers.filter((worker) => worker.classType === player.classType && worker.location === "ENTERPRISE_SLOT").length;
    player.enterprises = state.enterprises.filter((enterprise) => enterprise.ownerClass === player.classType).length;
  });
}

function countAction(state: GameState): void {
  if (state.currentPhase !== "ACTIONS") return;
  const index = state.turnOrder.currentPlayerIndex ?? 0;
  const taken = state.turnOrder.actionsTakenByPlayer ?? state.players.map(() => 0);
  taken[index] = Math.min(ACTIONS_PER_PLAYER, (taken[index] ?? 0) + 1);
  state.turnOrder.actionsTakenByPlayer = taken;
}

function advanceTurn(state: GameState): void {
  if (state.currentPhase !== "ACTIONS") return;
  const taken = state.turnOrder.actionsTakenByPlayer ?? state.players.map(() => 0);
  const allDone = taken.length > 0 && taken.every((count) => count >= ACTIONS_PER_PLAYER);
  if (allDone) {
    state.currentPhase = "PRODUCTION";
    state.turnOrder.phase = "PRODUCTION";
    state.turnOrder.currentPlayerIndex = 0;
    event(state, "PHASE_ADVANCED", "Фаза действий завершена. Начинается производство.");
    return;
  }
  let next = state.turnOrder.currentPlayerIndex ?? 0;
  for (let i = 0; i < state.players.length; i += 1) {
    next = (next + 1) % state.players.length;
    if ((taken[next] ?? 0) < ACTIONS_PER_PLAYER) {
      state.turnOrder.currentPlayerIndex = next;
      event(state, "TURN_STARTED", `Ходит ${state.players[next]?.classType}.`);
      return;
    }
  }
}

function resolveProduction(state: GameState): DomainEvent[] {
  const events: DomainEvent[] = [];
  state.enterprises.forEach((enterprise) => {
    if (!enterprise.functioning) return;
    const owner = playerByClass(state, enterprise.ownerClass);
    if (!owner) return;
    Object.entries(enterprise.producedResources ?? {}).forEach(([resource, amount]) => {
      owner.producedResourceStorage[resource] = (owner.producedResourceStorage[resource] ?? 0) + amount;
    });
    if (Object.keys(enterprise.producedResources ?? {}).length > 0) {
      events.push(event(state, "GOODS_PRODUCED", `${enterprise.name ?? enterprise.id}: произведено ${JSON.stringify(enterprise.producedResources)}.`));
    }
  });
  state.currentPhase = "VOTING";
  state.turnOrder.phase = "VOTING";
  startNextVote(state, events);
  return events;
}

function startNextVote(state: GameState, events: DomainEvent[]): void {
  const proposal = state.policies.find((policy) => policy.occupyingProposalToken);
  if (!proposal?.occupyingProposalToken) {
    state.currentPhase = "SCORING";
    state.turnOrder.phase = "SCORING";
    events.push(event(state, "PHASE_ADVANCED", "Нет законопроектов: переход к подсчету очков."));
    return;
  }
  const token = proposal.occupyingProposalToken;
  const authorId = token.ownerPlayerId ?? playerByClass(state, token.ownerClass)?.playerId ?? state.players[0]?.playerId ?? "";
  state.currentVoteState = {
    activeProposalPolicyId: proposal.id,
    proposalAuthorPlayerId: authorId,
    targetCourse: token.targetCourse ?? proposal.currentCourse,
    currentCourseBeforeVote: proposal.currentCourse,
    votingStage: "DECLARE_STANCES",
    stanceByPlayer: {},
    drawnVotingCubes: [],
    interpretedVotes: {},
    influenceCommitments: {},
    result: "PENDING",
    passedPolicyCourseApplied: false,
    extraordinary: false,
    totalForVotes: 0,
    totalAgainstVotes: 0,
  };
  events.push(event(state, "VOTING_PHASE_STARTED", `Голосование: ${proposal.id} -> ${state.currentVoteState.targetCourse}.`));
}

function resolveVoteIfReady(state: GameState, events: DomainEvent[]): void {
  const vote = state.currentVoteState;
  if (!vote) return;
  if (Object.keys(vote.influenceCommitments).length < state.players.length) return;

  let forVotes = 0;
  let againstVotes = 0;
  state.players.forEach((player) => {
    const stance = vote.stanceByPlayer[player.playerId] ?? "AGAINST";
    const influence = vote.influenceCommitments[player.playerId] ?? 0;
    if (stance === "FOR") forVotes += 1 + influence;
    if (stance === "AGAINST") againstVotes += 1 + influence;
  });
  vote.drawnVotingCubes.forEach((cube) => {
    if (cube.interpretedVote === "FOR") forVotes += 1;
    if (cube.interpretedVote === "AGAINST") againstVotes += 1;
  });
  vote.totalForVotes = forVotes;
  vote.totalAgainstVotes = againstVotes;
  vote.result = forVotes > againstVotes ? "PASSED" : "REJECTED";
  vote.votingStage = "RESOLVED";
  if (vote.result === "PASSED") {
    const policy = state.policies.find((candidate) => candidate.id === vote.activeProposalPolicyId);
    if (policy) {
      policy.currentCourse = vote.targetCourse;
      policy.occupyingProposalToken = undefined;
    }
  } else {
    const policy = state.policies.find((candidate) => candidate.id === vote.activeProposalPolicyId);
    if (policy) policy.occupyingProposalToken = undefined;
  }
  events.push(event(state, "VOTE_RESOLVED", `Итог голосования: ${vote.result}, ${forVotes}/${againstVotes}.`));
  state.currentVoteState = undefined;
  startNextVote(state, events);
}

function resolveScoring(state: GameState): DomainEvent[] {
  const events: DomainEvent[] = [];
  state.players.forEach((player) => {
    const gain = Math.max(0, player.welfare) + Math.floor(Math.max(0, player.money) / 50);
    player.victoryPoints += gain;
    events.push(event(state, "SCORING_RESOLVED", `${player.classType}: +${gain} ПО.`));
  });
  return events;
}

function nextRound(state: GameState): DomainEvent[] {
  const events: DomainEvent[] = [];
  if (state.currentRound >= state.maxRounds) {
    state.gameOver = true;
    state.gameStatus = "FINISHED";
    state.currentPhase = "GAME_OVER";
    state.turnOrder.phase = "GAME_OVER";
    const standings = [...state.players]
      .sort((a, b) => b.victoryPoints - a.victoryPoints)
      .map((player, index) => ({ playerId: player.playerId, classType: player.classType, totalVp: player.victoryPoints, rank: index + 1 }));
    state.finalResult = {
      completedRound: state.currentRound,
      tie: standings.length > 1 && standings[0].totalVp === standings[1].totalVp,
      tiebreakApplied: false,
      unresolvedTie: standings.length > 1 && standings[0].totalVp === standings[1].totalVp,
      winnerPlayerIds: standings.filter((item) => item.totalVp === standings[0].totalVp).map((item) => item.playerId),
      standings,
      scoringBreakdown: [],
      unsupportedNotes: [],
    };
    events.push(event(state, "GAME_OVER", "Игра завершена."));
    return events;
  }
  state.currentRound += 1;
  state.currentPhase = "ACTIONS";
  state.turnOrder.phase = "ACTIONS";
  state.turnOrder.round = state.currentRound;
  state.turnOrder.currentPlayerIndex = 0;
  state.turnOrder.actionsTakenByPlayer = state.players.map(() => 0);
  state.workers.forEach((worker) => {
    worker.tiedContract = false;
  });
  events.push(...resolvePreparationMigration(state));
  refreshEnterpriseFlags(state);
  refreshPlayerDerivedCounts(state);
  events.push(event(state, "ROUND_STARTED", `Раунд ${state.currentRound}.`));
  return events;
}

function resolvePreparationMigration(state: GameState): DomainEvent[] {
  const events: DomainEvent[] = [];
  const cardsPerClass = migrationCardsPerEligibleClass(state);
  if (cardsPerClass <= 0 || !state.migrationDeck || !Array.isArray(state.migrationCards)) {
    return events;
  }

  state.migrationDeck.visibleCardIds = [];
  state.migrationDeck.lastRefreshedRound = state.currentRound;
  state.migrationDeck.lastRefreshReason = "ROUND_PREPARATION";

  (["WORKER", "MIDDLE_CLASS"] as ClassType[]).forEach((classType) => {
    const player = playerByClass(state, classType);
    if (!player) {
      return;
    }
    for (let i = 0; i < cardsPerClass; i += 1) {
      const card = drawMigrationCard(state);
      if (!card) {
        continue;
      }
      const entry = classType === "WORKER" ? card.workerEntry : card.middleClassEntry;
      const worker = {
        id: nextWorkerId(state, classType),
        classType,
        qualificationType: entry?.qualificationType ?? "UNSKILLED",
        sector: entry?.sector,
        location: "UNEMPLOYED" as const,
        tiedContract: false,
      };
      state.workers.push(worker);
      player.population = populationFromWorkerCount(state.workers.filter((item) => item.classType === classType).length);
      events.push(event(state, "MIGRATION_RESOLVED", `${classType}: прибыл новый рабочий ${worker.id}.`));
    }
  });

  state.migrationDeck.refreshCount = (state.migrationDeck.refreshCount ?? 0) + 1;
  return events;
}

function migrationCardsPerEligibleClass(state: GameState): number {
  if (state.currentRound <= 1) {
    return 0;
  }
  const course = state.policies.find((policy) => policy.id === "POLICY_7_IMMIGRATION")?.currentCourse ?? "B";
  if (course === "A") return 0;
  if (course === "C") return 2;
  return 1;
}

function drawMigrationCard(state: GameState): GameState["migrationCards"][number] | undefined {
  const deck = state.migrationDeck;
  const order = deck?.orderedCardIds ?? [];
  if (order.length === 0) {
    return undefined;
  }
  const startIndex = Math.abs(deck.nextCardIndex ?? 0) % order.length;
  const cardId = order[startIndex];
  deck.nextCardIndex = (startIndex + 1) % order.length;
  deck.visibleCardIds = [...(deck.visibleCardIds ?? []), cardId];
  return state.migrationCards.find((card) => card.cardId === cardId);
}

function nextWorkerId(state: GameState, classType: ClassType): string {
  const prefix = `${classType.toLowerCase()}-worker-`;
  return `${prefix}${state.workers
    .filter((worker) => worker.classType === classType)
    .map((worker) => Number.parseInt(worker.id.replace(prefix, ""), 10))
    .filter(Number.isFinite)
    .reduce((max, value) => Math.max(max, value), 0) + 1}`;
}

function populationFromWorkerCount(workers: number): number {
  const normalized = Math.max(0, workers);
  if (normalized === 0) return 0;
  if (normalized <= 11) return 3;
  return Math.min(10, 4 + Math.floor((normalized - 12) / 3));
}

function applyAction(state: GameState, actionType: ActionType, parameters: Record<string, unknown>, dryRun = false): CommandResponse {
  const work = dryRun ? clone(state) : state;
  const events: DomainEvent[] = [];
  const errors = validateActor(work, actionType, parameters);
  if (errors.length > 0) {
    return { accepted: false, errors, reasonCodes: errors, events: [], gameState: work };
  }

  const actor = currentPlayer(work);
  const actorId = toString(parameters.actorPlayerId, actor?.playerId ?? "");
  const actorPlayer = playerById(work, actorId) ?? actor;

  switch (actionType) {
    case "ADD_VOTING_CUBES": {
      if (!actorPlayer) return rejected(work, "NO_ACTOR");
      const key = classCubeKey(actorPlayer.classType);
      if (!key) return rejected(work, "STATE_HAS_NO_VOTING_CUBES");
      const amount = Math.max(1, toInt(parameters.amount, 3));
      work.votingBag[key] += amount;
      events.push(event(work, "VOTING_CUBES_ADDED", `${actorPlayer.classType}: +${amount} кубика.`));
      countAction(work);
      break;
    }
    case "PROPOSE_BILL": {
      if (!actorPlayer) return rejected(work, "NO_ACTOR");
      const policyId = toString(parameters.policyId) as PolicyId;
      const targetCourse = toString(parameters.targetCourse) as PolicyCourse;
      const policy = work.policies.find((candidate) => candidate.id === policyId);
      if (!policy) return rejected(work, "INVALID_POLICY");
      if (!["A", "B", "C"].includes(targetCourse)) return rejected(work, "INVALID_TARGET_COURSE");
      if (policy.currentCourse === targetCourse) return rejected(work, "TARGET_COURSE_EQUALS_CURRENT");
      if (policy.occupyingProposalToken) return rejected(work, "POLICY_ALREADY_HAS_PROPOSAL");
      policy.occupyingProposalToken = {
        id: `proposal-${Date.now()}`,
        ownerPlayerId: actorPlayer.playerId,
        ownerClass: actorPlayer.classType,
        available: false,
        targetCourse,
        policyId,
      };
      events.push(event(work, "BILL_PROPOSED", `${actorPlayer.classType}: ${policyId} -> ${targetCourse}.`));
      countAction(work);
      break;
    }
    case "ASSIGN_WORKERS": {
      const assignmentErrors = applyAssignments(work, parseAssignments(parameters.assignments), events);
      if (assignmentErrors.length > 0) {
        return { accepted: false, errors: assignmentErrors, reasonCodes: assignmentErrors, events, gameState: work };
      }
      countAction(work);
      break;
    }
    case "BUY_GOODS_AND_SERVICES": {
      if (!actorPlayer) return rejected(work, "NO_ACTOR");
      const resourceType = toString(parameters.resourceType, "FOOD");
      const key = resourceKey(resourceType);
      const purchases = parsePurchases(parameters.purchases);
      if (purchases.length === 0) return rejected(work, "NO_PURCHASES");
      let totalCost = 0;
      purchases.forEach((purchase) => {
        const quantity = Math.max(0, purchase.quantity);
        const price = Math.max(0, purchase.unitPriceOverride ?? unitPrice(work, purchase.supplierType, resourceType));
        if (quantity <= 0) return;
        totalCost += quantity * price;
        if (purchase.supplierType === "CAPITALIST" || purchase.supplierType === "MIDDLE_CLASS") {
          const supplier = purchase.supplierPlayerId ? playerById(work, purchase.supplierPlayerId) : playerByClass(work, purchase.supplierType);
          if (supplier) {
            supplier.producedResourceStorage[key] = Math.max(0, (supplier.producedResourceStorage[key] ?? 0) - quantity);
            supplier.money += quantity * price;
          }
        } else if (purchase.supplierType === "STATE") {
          work.publicServicesStorage[key] = Math.max(0, (work.publicServicesStorage[key] ?? 0) - quantity);
          work.treasury += quantity * price;
        }
        actorPlayer.goodsAndServicesArea[key] = (actorPlayer.goodsAndServicesArea[key] ?? 0) + quantity;
      });
      if (actorPlayer.money < totalCost) return rejected(work, "INSUFFICIENT_MONEY");
      actorPlayer.money -= totalCost;
      events.push(event(work, "GOODS_BOUGHT", `${actorPlayer.classType}: куплено ${resourceType} за ${totalCost}.`));
      countAction(work);
      break;
    }
    case "CONSUME_LUXURY":
    case "CONSUME_EDUCATION":
    case "CONSUME_HEALTHCARE": {
      if (!actorPlayer) return rejected(work, "NO_ACTOR");
      const selected = toString(parameters.consumeSelectedActionType, actionType) as ActionType;
      const effective = selected.startsWith("CONSUME_") ? selected : actionType;
      const key = effective === "CONSUME_LUXURY" ? "luxury" : effective === "CONSUME_EDUCATION" ? "education" : "healthcare";
      const required = Math.max(1, actorPlayer.population);
      const available = (actorPlayer.goodsAndServicesArea[key] ?? 0) + (actorPlayer.producedResourceStorage[key] ?? 0);
      if (available < required) return rejected(work, "INSUFFICIENT_RESOURCE");
      let remaining = required;
      const fromArea = Math.min(remaining, actorPlayer.goodsAndServicesArea[key] ?? 0);
      actorPlayer.goodsAndServicesArea[key] = (actorPlayer.goodsAndServicesArea[key] ?? 0) - fromArea;
      remaining -= fromArea;
      actorPlayer.producedResourceStorage[key] = Math.max(0, (actorPlayer.producedResourceStorage[key] ?? 0) - remaining);
      actorPlayer.welfare += 1;
      actorPlayer.victoryPoints += 1;
      if (effective === "CONSUME_HEALTHCARE") {
        const newId = `${actorPlayer.classType.toLowerCase()}-worker-${work.workers.length + 1}`;
        work.workers.push({
          id: newId,
          classType: actorPlayer.classType,
          qualificationType: "UNSKILLED",
          location: "UNEMPLOYED",
          tiedContract: false,
        });
      }
      if (effective === "CONSUME_EDUCATION") {
        const workerId = toString(parameters.workerId);
        const worker = work.workers.find((candidate) => candidate.id === workerId);
        if (worker) {
          worker.qualificationType = "SKILLED";
          worker.sector = toString(parameters.targetColor, "WHITE");
        }
      }
      refreshPlayerDerivedCounts(work);
      events.push(event(work, "RESOURCE_CONSUMED", `${actorPlayer.classType}: потреблено ${key}.`));
      countAction(work);
      break;
    }
    case "END_TURN": {
      advanceTurn(work);
      break;
    }
    case "RESOLVE_PRODUCTION_PHASE": {
      events.push(...resolveProduction(work));
      break;
    }
    case "RESOLVE_SCORING_PHASE": {
      events.push(...resolveScoring(work));
      break;
    }
    case "ADVANCE_TO_NEXT_ROUND": {
      events.push(...nextRound(work));
      break;
    }
    case "DECLARE_VOTE_STANCE": {
      const vote = work.currentVoteState;
      if (!vote) return rejected(work, "NO_ACTIVE_VOTE");
      const voteActorId = toString(parameters.actorPlayerId);
      vote.stanceByPlayer[voteActorId] = toString(parameters.stance, "FOR") === "FOR" ? "FOR" : "AGAINST";
      events.push(event(work, "VOTE_STANCE_DECLARED", `${voteActorId}: ${vote.stanceByPlayer[voteActorId]}.`));
      if (Object.keys(vote.stanceByPlayer).length >= work.players.length) {
        vote.votingStage = "DRAW_BAG_CUBES";
      }
      break;
    }
    case "DRAW_VOTING_CUBES": {
      const vote = work.currentVoteState;
      if (!vote) return rejected(work, "NO_ACTIVE_VOTE");
      const count = Math.max(1, toInt(parameters.count, 5));
      vote.drawnVotingCubes = deterministicDraw(work, count).map((ownerClass) => ({
        ownerClass,
        interpretedVote: ownerClass === playerById(work, vote.proposalAuthorPlayerId)?.classType ? "FOR" : "AGAINST",
      }));
      vote.votingStage = "COMMIT_INFLUENCE";
      events.push(event(work, "VOTING_CUBES_DRAWN", `Достано кубиков: ${vote.drawnVotingCubes.length}.`));
      break;
    }
    case "COMMIT_VOTE_INFLUENCE": {
      const vote = work.currentVoteState;
      if (!vote) return rejected(work, "NO_ACTIVE_VOTE");
      const voterId = toString(parameters.actorPlayerId);
      const voter = playerById(work, voterId);
      const amount = Math.max(0, toInt(parameters.influenceAmount, 0));
      if (!voter) return rejected(work, "NO_ACTOR");
      if (amount > voter.influence) return rejected(work, "INFLUENCE_EXCEEDS_AVAILABLE");
      voter.influence -= amount;
      vote.influenceCommitments[voterId] = amount;
      events.push(event(work, "VOTE_INFLUENCE_COMMITTED", `${voterId}: ${amount}.`));
      resolveVoteIfReady(work, events);
      break;
    }
    case "ADVANCE_GAME_FLOW": {
      events.push(...advanceGameFlow(work));
      break;
    }
    default:
      return rejected(work, "UNSUPPORTED_ACTION");
  }

  if (!dryRun) {
    persistState(work);
  }
  return { accepted: true, errors: [], reasonCodes: [], events, gameState: work };
}

function rejected(state: GameState, reason: string): CommandResponse {
  return { accepted: false, errors: [reason], reasonCodes: [reason], events: [], gameState: state };
}

function parsePurchases(raw: unknown): PurchaseDraft[] {
  if (!Array.isArray(raw)) return [];
  return raw
    .map((item) => {
      const row = toRecord(item);
      return {
        supplierType: toString(row.supplierType, "EXTERNAL_MARKET") as PurchaseDraft["supplierType"],
        supplierPlayerId: toString(row.supplierPlayerId),
        quantity: Math.max(0, toInt(row.quantity, 0)),
        unitPriceOverride: Math.max(0, toInt(row.unitPriceOverride, 0)),
      };
    })
    .filter((item) => item.quantity > 0);
}

function unitPrice(state: GameState, supplierType: string, resourceType: string): number {
  const key = resourceKey(resourceType);
  if (supplierType === "CAPITALIST") return playerByClass(state, "CAPITALIST")?.prices?.[key] ?? 10;
  if (supplierType === "MIDDLE_CLASS") return playerByClass(state, "MIDDLE_CLASS")?.prices?.[key] ?? 8;
  if (supplierType === "STATE") return resourceType === "HEALTHCARE" || resourceType === "EDUCATION" ? 5 : 0;
  return resourceType === "FOOD" ? 15 : 9;
}

function deterministicDraw(state: GameState, count: number): Array<"WORKER" | "MIDDLE_CLASS" | "CAPITALIST"> {
  const result: Array<"WORKER" | "MIDDLE_CLASS" | "CAPITALIST"> = [];
  const order: Array<["worker" | "middleClass" | "capitalist", "WORKER" | "MIDDLE_CLASS" | "CAPITALIST"]> = [
    ["worker", "WORKER"],
    ["middleClass", "MIDDLE_CLASS"],
    ["capitalist", "CAPITALIST"],
  ];
  let guard = 0;
  while (result.length < count && guard < 100) {
    guard += 1;
    for (const [key, owner] of order) {
      if (result.length >= count) break;
      if (state.votingBag[key] > 0) {
        state.votingBag[key] -= 1;
        result.push(owner);
      }
    }
    if (order.every(([key]) => state.votingBag[key] <= 0)) break;
  }
  return result;
}

function advanceGameFlow(state: GameState): DomainEvent[] {
  if (state.currentPhase === "ACTIONS") {
    const taken = state.turnOrder.actionsTakenByPlayer ?? [];
    if (taken.every((count) => count >= ACTIONS_PER_PLAYER)) {
      state.currentPhase = "PRODUCTION";
      state.turnOrder.phase = "PRODUCTION";
      return [event(state, "PHASE_ADVANCED", "Переход к производству.")];
    }
    advanceTurn(state);
    return [];
  }
  if (state.currentPhase === "PRODUCTION") return resolveProduction(state);
  if (state.currentPhase === "VOTING") {
    const events: DomainEvent[] = [];
    if (!state.currentVoteState) startNextVote(state, events);
    return events;
  }
  if (state.currentPhase === "SCORING") {
    const events = resolveScoring(state);
    events.push(...nextRound(state));
    return events;
  }
  return [];
}

function previewDelta(before: GameState, after: GameState): ActionPreviewDelta {
  const moneyDeltaByPlayer = Object.fromEntries(after.players.map((player) => {
    const previous = before.players.find((candidate) => candidate.playerId === player.playerId);
    return [player.playerId, player.money - (previous?.money ?? 0)];
  }).filter(([, delta]) => delta !== 0));
  const welfareDeltaByPlayer = Object.fromEntries(after.players.map((player) => {
    const previous = before.players.find((candidate) => candidate.playerId === player.playerId);
    return [player.playerId, player.welfare - (previous?.welfare ?? 0)];
  }).filter(([, delta]) => delta !== 0));
  const victoryPointDeltaByPlayer = Object.fromEntries(after.players.map((player) => {
    const previous = before.players.find((candidate) => candidate.playerId === player.playerId);
    return [player.playerId, player.victoryPoints - (previous?.victoryPoints ?? 0)];
  }).filter(([, delta]) => delta !== 0));
  const workerMovement = Object.fromEntries(after.workers.map((worker) => {
    const previous = before.workers.find((candidate) => candidate.id === worker.id);
    const next = worker.location === "ENTERPRISE_SLOT" ? `${worker.enterpriseId}:${worker.slotId}` : worker.location;
    const prev = previous?.location === "ENTERPRISE_SLOT" ? `${previous.enterpriseId}:${previous.slotId}` : previous?.location;
    return [worker.id, prev === next ? "" : `${prev ?? "new"} -> ${next}`];
  }).filter(([, value]) => value));
  const policyDelta = Object.fromEntries(after.policies.map((policy) => {
    const previous = before.policies.find((candidate) => candidate.id === policy.id);
    return [policy.id, previous?.currentCourse === policy.currentCourse ? "" : `${previous?.currentCourse} -> ${policy.currentCourse}`];
  }).filter(([, value]) => value));
  return {
    moneyDeltaByPlayer,
    resourceDeltaByPlayer: {},
    welfareDeltaByPlayer,
    workerMovement,
    policyDelta,
    proposalTokenDeltaByPlayer: {},
    influenceDeltaByPlayer: {},
    victoryPointDeltaByPlayer,
    notes: ["Автономный preview без изменения сохраненного состояния."],
  };
}

function gameResponse(state = loadState()): GameResponse {
  return {
    gameState: state,
    legalMoves: generatedLegalMoves(state),
    composerMetadata: composerMetadata(state),
    marketCandidates: [],
  };
}

export function createLocalGameApi() {
  return {
    async getGame(): Promise<GameResponse> {
      return gameResponse();
    },
    async getLegalMoves(): Promise<{ legalMoves: LegalMove[] }> {
      return { legalMoves: generatedLegalMoves(loadState()) };
    },
    async resetGame(): Promise<GameResponse> {
      const state = freshState();
      persistState(state);
      return gameResponse(state);
    },
    async setupGame(payload: SetupPayload): Promise<GameResponse> {
      const base = freshState();
      const activeClasses = activeClassesForCount(payload.playerCount);
      base.players = base.players
        .filter((player) => activeClasses.includes(player.classType))
        .sort((left, right) => CLASS_ORDER.indexOf(left.classType) - CLASS_ORDER.indexOf(right.classType))
        .map((player) => ({
          ...player,
          controlMode: (payload.controlModes[player.classType] ?? "HUMAN") as PlayerControlMode,
          botStrategyMode: payload.botStrategyModes[player.classType] === "CARD_DRIVEN_SIMPLE_AUTOMA"
            ? "CARD_DRIVEN_SIMPLE_AUTOMA"
            : "HEURISTIC_FALLBACK",
        }));
      base.turnOrder.activeClasses = activeClasses;
      base.turnOrder.currentPlayerIndex = 0;
      base.turnOrder.actionsTakenByPlayer = base.players.map(() => 0);
      persistState(base);
      return gameResponse(base);
    },
    async submitCommand(payload: { actionType: ActionType; parameters: Record<string, unknown> }): Promise<CommandResponse> {
      const state = loadState();
      return applyAction(state, payload.actionType, payload.parameters);
    },
    async previewAction(payload: { actionType: ActionType; parameters: Record<string, unknown> }): Promise<PreviewActionResponse> {
      const before = loadState();
      const result = applyAction(before, payload.actionType, payload.parameters, true);
      if (!result.accepted) {
        return {
          accepted: false,
          errors: result.errors,
          reasonCodes: result.reasonCodes,
          delta: previewDelta(before, before),
          supportNotes: [],
        };
      }
      return {
        accepted: true,
        errors: [],
        reasonCodes: [],
        delta: previewDelta(before, result.gameState),
        supportNotes: [],
      };
    },
    async playBotTurn(): Promise<BotTurnResponse> {
      const state = loadState();
      const actor = currentPlayer(state);
      if (!actor || actor.controlMode !== "BOT") {
        throw new Error("Текущий игрок не управляется ботом.");
      }
      const legalMoves = generatedLegalMoves(state);
      const selected = chooseBotMove(legalMoves);
      if (!selected) {
        throw new Error("Нет legal moves для бота.");
      }
      const result = applyAction(state, selected.actionType, selected.template);
      const summary = {
        actingClass: actor.classType,
        actingPlayerId: actor.playerId,
        selectedMoveId: selected.id,
        selectedAction: selected.actionType,
        chosenTargets: selected.template,
        cardModifierPathUsed: false,
        plannerId: "offline-mobile-legal-move",
        rationale: "Бот выбрал первый безопасный legal move из локального движка.",
        legalOptionsConsidered: legalMoves.length,
        fallbackHeuristicMode: true,
        strategyModeUsed: actor.botStrategyMode,
        eventSummaries: result.events.map((item) => item.description),
      };
      result.gameState.lastBotTurnSummary = summary;
      persistState(result.gameState);
      return { summary, events: result.events, gameState: result.gameState };
    },
    async playBotUntilHuman(): Promise<BotUntilHumanResponse> {
      const summaries = [];
      let state = loadState();
      let guard = 0;
      while (currentPlayer(state)?.controlMode === "BOT" && !state.gameOver && guard < 25) {
        guard += 1;
        const turn = await playOneBotTurn();
        summaries.push(turn.summary);
        state = turn.gameState;
      }
      return {
        turnSummaries: summaries,
        executedSteps: summaries.length,
        stoppedAtHumanDecisionPoint: currentPlayer(state)?.controlMode === "HUMAN",
        gameOver: state.gameOver,
        gameState: state,
      };
    },
    async saveGame(fileName: string): Promise<SaveLoadResponse> {
      const safeName = fileName.trim() || "mobile-save.json";
      const state = loadState();
      localStorage.setItem(`${SAVE_PREFIX}${safeName}`, JSON.stringify(state));
      return { filePath: `localStorage:${safeName}`, gameState: state };
    },
    async loadGame(fileName: string): Promise<SaveLoadResponse> {
      const safeName = fileName.trim() || "mobile-save.json";
      const stored = localStorage.getItem(`${SAVE_PREFIX}${safeName}`);
      if (!stored) {
        throw new Error(`Сохранение ${safeName} не найдено.`);
      }
      const state = normalizeState(JSON.parse(stored) as GameState);
      persistState(state);
      return { filePath: `localStorage:${safeName}`, gameState: state };
    },
  };
}

function chooseBotMove(legalMoves: LegalMove[]): LegalMove | undefined {
  return legalMoves.find((move) => move.actionType === "PROPOSE_BILL")
    ?? legalMoves.find((move) => move.actionType === "ADD_VOTING_CUBES")
    ?? legalMoves.find((move) => move.actionType === "END_TURN")
    ?? legalMoves[0];
}

async function playOneBotTurn(): Promise<BotTurnResponse> {
  const state = loadState();
  const actor = currentPlayer(state);
  if (!actor || actor.controlMode !== "BOT") {
    throw new Error("Текущий игрок не управляется ботом.");
  }
  const legalMoves = generatedLegalMoves(state);
  const selected = chooseBotMove(legalMoves);
  if (!selected) {
    throw new Error("Нет legal moves для бота.");
  }
  const result = applyAction(state, selected.actionType, selected.template);
  const summary = {
    actingClass: actor.classType,
    actingPlayerId: actor.playerId,
    selectedMoveId: selected.id,
    selectedAction: selected.actionType,
    chosenTargets: selected.template,
    cardModifierPathUsed: false,
    plannerId: "offline-mobile-legal-move",
    rationale: "Бот выбрал первый безопасный legal move из локального движка.",
    legalOptionsConsidered: legalMoves.length,
    fallbackHeuristicMode: true,
    strategyModeUsed: actor.botStrategyMode,
    eventSummaries: result.events.map((item) => item.description),
  };
  result.gameState.lastBotTurnSummary = summary;
  persistState(result.gameState);
  return { summary, events: result.events, gameState: result.gameState };
}
