import type {
  ActionType,
  ComposerMetadata,
  Enterprise,
  GameState,
  LegalMove,
  PlayerState,
  PolicyId,
  Worker,
} from "@/types/game";

export interface SupplierOption {
  key: string;
  supplierType: "CAPITALIST" | "MIDDLE_CLASS" | "STATE" | "EXTERNAL_MARKET";
  supplierPlayerId?: string;
  label: string;
  available: number;
  unitPrice: number;
}

export interface TargetOption {
  value: string;
  label: string;
}

const CONSUME_ACTIONS: ActionType[] = ["CONSUME_LUXURY", "CONSUME_EDUCATION", "CONSUME_HEALTHCARE"];

export function toInt(value: unknown, fallback = 0): number {
  if (typeof value === "number" && Number.isFinite(value)) {
    return Math.trunc(value);
  }
  const parsed = Number.parseInt(String(value ?? ""), 10);
  return Number.isFinite(parsed) ? parsed : fallback;
}

export function toString(value: unknown, fallback = ""): string {
  if (value === null || value === undefined) {
    return fallback;
  }
  return String(value);
}

export function toRecord(value: unknown): Record<string, unknown> {
  if (!value || typeof value !== "object" || Array.isArray(value)) {
    return {};
  }
  return value as Record<string, unknown>;
}

export function prettyJson(value: unknown): string {
  return JSON.stringify(value, null, 2);
}

export function safeClone(value: Record<string, unknown> | undefined): Record<string, unknown> {
  if (!value) {
    return {};
  }
  return JSON.parse(JSON.stringify(value)) as Record<string, unknown>;
}

export function currentPlayer(state: GameState): PlayerState | undefined {
  const index = Number.isInteger(state.turnOrder?.currentPlayerIndex) ? Number(state.turnOrder.currentPlayerIndex) : 0;
  return state.players[index] ?? state.players[0];
}

export function actorPlayerId(state: GameState, composer: ComposerMetadata): string {
  return composer.actorPlayerId || currentPlayer(state)?.playerId || state.players[0]?.playerId || "";
}

export function policyIds(state: GameState): PolicyId[] {
  return state.policies.map((policy) => policy.id);
}

function firstPendingPolicyForActor(state: GameState, playerId: string): PolicyId | undefined {
  return state.policies.find((policy) => {
    const token = policy.occupyingProposalToken;
    if (!token) {
      return false;
    }
    if (token.ownerPlayerId) {
      return token.ownerPlayerId === playerId;
    }
    return state.players.find((player) => player.playerId === playerId)?.classType === token.ownerClass;
  })?.id;
}

function firstMissingVoteActorPlayerId(state: GameState, kind: "stance" | "influence"): string | undefined {
  const vote = state.currentVoteState;
  if (!vote) {
    return undefined;
  }
  const alreadyDone = new Set(
    Object.keys(kind === "stance" ? vote.stanceByPlayer ?? {} : vote.influenceCommitments ?? {}),
  );
  const eligible = kind === "influence"
    ? state.players.filter((player) => Object.prototype.hasOwnProperty.call(vote.stanceByPlayer ?? {}, player.playerId))
    : state.players;
  return eligible.find((player) => !alreadyDone.has(player.playerId))?.playerId;
}

export function seedParameters(
  actionType: ActionType,
  state: GameState,
  composer: ComposerMetadata,
  legalMove?: LegalMove,
): Record<string, unknown> {
  const template = safeClone(
    legalMove?.template ??
      composer.actionTemplates.find((candidate) => candidate.actionType === actionType)?.template,
  );
  const actorId = toString(template.actorPlayerId, actorPlayerId(state, composer));
  const base: Record<string, unknown> = { ...template, actorPlayerId: actorId };

  if (actionType === "PROPOSE_BILL") {
    return {
      ...base,
      policyId: toString(base.policyId, state.policies[0]?.id ?? "POLICY_1_FISCAL"),
      targetCourse: toString(base.targetCourse, "B"),
    };
  }

  if (actionType === "CALL_EXTRAORDINARY_VOTE") {
    return {
      ...base,
      policyId: toString(base.policyId, firstPendingPolicyForActor(state, actorId) ?? state.policies[0]?.id ?? "POLICY_1_FISCAL"),
    };
  }

  if (actionType === "DECLARE_VOTE_STANCE") {
    const voteActor = firstMissingVoteActorPlayerId(state, "stance") ?? actorId;
    return {
      ...base,
      actorPlayerId: voteActor,
      policyId: toString(base.policyId, state.currentVoteState?.activeProposalPolicyId ?? state.policies[0]?.id ?? ""),
      stance: toString(base.stance, "FOR"),
    };
  }

  if (actionType === "COMMIT_VOTE_INFLUENCE") {
    return {
      ...base,
      actorPlayerId: firstMissingVoteActorPlayerId(state, "influence") ?? actorId,
      influenceAmount: Math.max(0, toInt(base.influenceAmount, 0)),
    };
  }

  if (actionType === "DRAW_VOTING_CUBES") {
    const total = (state.votingBag.worker ?? 0) + (state.votingBag.middleClass ?? 0) + (state.votingBag.capitalist ?? 0);
    return {
      ...base,
      count: Math.min(5, Math.max(1, toInt(base.count, total > 0 ? Math.min(5, total) : 1))),
    };
  }

  if (actionType === "ASSIGN_WORKERS") {
    return {
      ...base,
      assignments: Array.isArray(base.assignments) ? base.assignments : [],
    };
  }

  if (actionType === "BUY_GOODS_AND_SERVICES") {
    return {
      ...base,
      resourceType: toString(base.resourceType, "FOOD"),
      purchases: Array.isArray(base.purchases) ? base.purchases : [],
      buyQuantityBySupplier: toRecord(base.buyQuantityBySupplier),
      buyPriceBySupplier: toRecord(base.buyPriceBySupplier),
    };
  }

  if (CONSUME_ACTIONS.includes(actionType)) {
    const selected = toString(base.consumeSelectedActionType, actionType) as ActionType;
    const worker = state.workers.find((candidate) => candidate.qualificationType === "UNSKILLED");
    return {
      ...base,
      consumeSelectedActionType: CONSUME_ACTIONS.includes(selected) ? selected : actionType,
      consumeAvailableActionTypes: CONSUME_ACTIONS,
      workerId: toString(base.workerId, worker?.id ?? ""),
      targetColor: toString(base.targetColor, "WHITE"),
    };
  }

  if (actionType === "PLACE_STRIKES") {
    return {
      ...base,
      enterpriseIds: Array.isArray(base.enterpriseIds) ? base.enterpriseIds : [],
    };
  }

  if (actionType === "PLACE_DEMONSTRATION") {
    return {
      ...base,
      penaltyAllocation: toRecord(base.penaltyAllocation),
    };
  }

  if (actionType === "BUILD_ENTERPRISE") {
    const marketEnterprise = [...state.capitalistEnterpriseMarket, ...state.middleClassEnterpriseMarket][0];
    return {
      ...base,
      enterpriseId: toString(base.enterpriseId, marketEnterprise?.id ?? ""),
      cost: Math.max(0, toInt(base.cost, marketEnterprise?.cost ?? 20)),
      wageLevel: Math.max(1, toInt(base.wageLevel, 2)),
    };
  }

  if (actionType === "HIRE_WORKER") {
    return { ...base, count: Math.max(0, toInt(base.count, 1)) };
  }

  if (actionType === "PRODUCE_GOODS" || actionType === "SELL_GOODS") {
    return { ...base, amount: Math.max(0, toInt(base.amount, 1)) };
  }

  if (actionType === "ADJUST_POLICY") {
    return { ...base, track: toString(base.track, "TAXATION"), delta: toInt(base.delta, 1) };
  }

  return base;
}

export function resourceStorageKey(resourceType: string): string {
  const upper = resourceType.toUpperCase();
  if (upper === "FOOD") return "food";
  if (upper === "HEALTHCARE") return "healthcare";
  if (upper === "EDUCATION") return "education";
  if (upper === "LUXURY") return "luxury";
  return resourceType.toLowerCase();
}

function stateServiceUnitPrice(state: GameState, resourceType: string): number {
  const policyId = resourceType === "HEALTHCARE" ? "POLICY_4_HEALTHCARE_AND_BENEFITS" : "POLICY_5_EDUCATION";
  const course = state.policies.find((policy) => policy.id === policyId)?.currentCourse ?? "B";
  if (course === "A") return 0;
  if (course === "B") return 5;
  return 10;
}

function externalMarketUnitPrice(state: GameState, resourceType: string): number {
  const base = resourceType === "FOOD" ? 10 : 6;
  const course = state.policies.find((policy) => policy.id === "POLICY_6_FOREIGN_TRADE")?.currentCourse ?? "B";
  if (course === "A") return resourceType === "FOOD" ? base + 10 : base + 6;
  if (course === "B") return resourceType === "FOOD" ? base + 5 : base + 3;
  return base;
}

export function supplierOptions(state: GameState, actor: PlayerState | undefined, resourceType: string): SupplierOption[] {
  if (!actor || (actor.classType !== "WORKER" && actor.classType !== "MIDDLE_CLASS")) {
    return [];
  }
  const key = resourceStorageKey(resourceType);
  const populationCap = Math.max(0, actor.population);
  const capitalist = state.players.find((player) => player.classType === "CAPITALIST");
  const middleClass = state.players.find((player) => player.classType === "MIDDLE_CLASS");
  const options: SupplierOption[] = [];

  if (capitalist && resourceType !== "HEALTHCARE") {
    const available = Math.max(0, toInt(capitalist.producedResourceStorage?.[key], 0));
    const unitPrice = Math.max(0, toInt(capitalist.prices?.[key], 0));
    if (available > 0) {
      options.push({
        key: `CAPITALIST:${capitalist.playerId}`,
        supplierType: "CAPITALIST",
        supplierPlayerId: capitalist.playerId,
        label: `Капиталист (${capitalist.playerId})`,
        available,
        unitPrice,
      });
    }
  }

  if (middleClass) {
    const available = Math.max(0, toInt(middleClass.producedResourceStorage?.[key], 0));
    const unitPrice = Math.max(0, toInt(middleClass.prices?.[key], 0));
    if (available > 0) {
      options.push({
        key: `MIDDLE_CLASS:${middleClass.playerId}`,
        supplierType: "MIDDLE_CLASS",
        supplierPlayerId: middleClass.playerId,
        label: `Средний класс (${middleClass.playerId})`,
        available,
        unitPrice,
      });
    }
  }

  if (resourceType === "HEALTHCARE" || resourceType === "EDUCATION") {
    const available = Math.max(0, toInt(state.publicServicesStorage?.[key], 0));
    options.push({
      key: "STATE:_",
      supplierType: "STATE",
      label: "Государство",
      available,
      unitPrice: stateServiceUnitPrice(state, resourceType),
    });
  }

  if (resourceType === "FOOD" || resourceType === "LUXURY") {
    options.push({
      key: "EXTERNAL_MARKET:_",
      supplierType: "EXTERNAL_MARKET",
      label: "Импорт",
      available: populationCap,
      unitPrice: externalMarketUnitPrice(state, resourceType),
    });
  }

  return options.filter((option) => option.available > 0);
}

export function purchaseRowsFromMaps(parameters: Record<string, unknown>, options: SupplierOption[]) {
  const quantityMap = toRecord(parameters.buyQuantityBySupplier);
  const priceMap = toRecord(parameters.buyPriceBySupplier);
  return options
    .map((option) => {
      const quantity = Math.max(0, toInt(quantityMap[option.key], 0));
      const unitPriceOverride = Math.max(0, toInt(priceMap[option.key], option.unitPrice));
      return {
        supplierType: option.supplierType,
        supplierPlayerId: option.supplierPlayerId ?? "",
        quantity,
        unitPriceOverride,
      };
    })
    .filter((row) => row.quantity > 0);
}

export function assignmentTargetOptions(state: GameState): TargetOption[] {
  const enterpriseSlots = state.enterprises.flatMap((enterprise) =>
    enterprise.slots.map((slot) => ({
      value: `${enterprise.id}:${slot.id}`,
      label: `${enterprise.name ?? enterprise.id} / ${slot.id}${slot.occupiedWorkerId ? " занято" : ""}`,
    })),
  );
  return [
    { value: "unemployed", label: "Безработные" },
    { value: "union", label: "Профсоюз" },
    ...enterpriseSlots,
  ];
}

export function workerLabel(worker: Worker): string {
  const no = worker.id.match(/(\d+)$/)?.[1] ?? worker.id;
  const sector = worker.sector ? ` / ${worker.sector.toLowerCase()}` : "";
  return `#${no} ${worker.qualificationType}${sector}`;
}

export function enterpriseMarket(state: GameState): Enterprise[] {
  return [...state.capitalistEnterpriseMarket, ...state.middleClassEnterpriseMarket];
}
