import { BOARD_ZONE_INDEX, BOARD_ZONES } from "@/features/board/layout/board-zones";
import type {
  BoardRenderable,
  BoardViewModel,
  BoardZoneDefinition,
  BoardZoneView,
  PolicyCourseCellView,
  PolicyTrackView,
} from "@/features/board/model/types";
import type { BusinessDealCard, GameState, PolicyCourse, PolicyId, StateEventCard } from "@/types/game";

interface BuildBoardViewModelInput {
  state: GameState;
  selectedZoneId?: string;
  selectedEntityId?: string;
  highlightedZones?: string[];
}

const POLICY_LABELS: Record<PolicyId, string> = {
  POLICY_1_FISCAL: "P1 Бюджет",
  POLICY_2_LABOR_MARKET: "P2 Рынок труда",
  POLICY_3_TAXATION: "P3 Налоги",
  POLICY_4_HEALTHCARE_AND_BENEFITS: "P4 Здравоохранение и льготы",
  POLICY_5_EDUCATION: "P5 Образование",
  POLICY_6_FOREIGN_TRADE: "P6 Внешняя торговля",
  POLICY_7_IMMIGRATION: "P7 Иммиграция",
};

const ENTERPRISE_LABELS: Record<string, string> = {
  supermarket: "Супермаркет",
  mall: "Торговый центр",
  college: "Колледж",
  polyclinic: "Поликлиника",
  state_hospital: "Гос. больница",
  state_university: "Гос. университет",
  state_media: "Гос. медиа",
  mini_market: "Минимаркет",
  private_clinic: "Частная клиника",
};

type WorkerColor = "GRAY" | "GREEN" | "BLUE" | "RED" | "ORANGE" | "PURPLE" | "WHITE";

type EnterpriseCardState = "ACTIVE" | "RESERVE" | "LOCKED";
type EnterpriseSlotState = "EMPTY" | "OCCUPIED" | "TIED";

interface EnterpriseSlotVisual {
  id: string;
  state: EnterpriseSlotState;
  color: WorkerColor;
  requiredColor: WorkerColor;
  qualification: "UNSKILLED" | "SKILLED";
  optional?: boolean;
  classType?: "WORKER" | "MIDDLE_CLASS" | "CAPITALIST" | "STATE";
  workerId?: string;
  workerNumber?: string;
}

interface EnterpriseWageLevelVisual {
  level: 1 | 2 | 3;
  value?: number;
  active: boolean;
  blocked: boolean;
}

interface EnterpriseCardVisual {
  variant: "enterprise-card";
  state: EnterpriseCardState;
  rowLabel?: string;
  policyHint?: string;
  outputLabel: string;
  slots: EnterpriseSlotVisual[];
  wages: EnterpriseWageLevelVisual[];
  minimumWageLevel: 1 | 2 | 3;
}

interface WorkerMeepleVisual {
  variant: "worker-meeple";
  color: WorkerColor;
  workerNumber: string;
  classType: "WORKER" | "MIDDLE_CLASS" | "CAPITALIST" | "STATE";
  tied: boolean;
  posture: "STANDING" | "LYING";
}

function workerColorOf(worker: GameState["workers"][number]): WorkerColor {
  return normalizeWorkerColor(worker.sector);
}

function normalizeWorkerColor(raw: unknown): WorkerColor {
  const value = String(raw ?? "").toUpperCase();
  if (value === "GREEN" || value === "FOOD") {
    return "GREEN";
  }
  if (value === "BLUE" || value === "LUXURY") {
    return "BLUE";
  }
  if (value === "RED" || value === "WHITE" || value === "HEALTHCARE" || value === "MEDICAL") {
    return "WHITE";
  }
  if (value === "ORANGE" || value === "EDUCATION") {
    return "ORANGE";
  }
  if (value === "PURPLE" || value === "MEDIA" || value === "INFLUENCE" || value === "MEDIA_INFLUENCE") {
    return "PURPLE";
  }
  return "GRAY";
}

function workerColorLabel(color: WorkerColor): string {
  if (color === "GREEN") {
    return "Зеленый";
  }
  if (color === "BLUE") {
    return "Синий";
  }
  if (color === "RED") {
    return "Белый";
  }
  if (color === "ORANGE") {
    return "Оранжевый";
  }
  if (color === "PURPLE") {
    return "Фиолетовый";
  }
  if (color === "WHITE") {
    return "Белый";
  }
  return "Серый";
}

function workerColorShort(color: WorkerColor): string {
  if (color === "GREEN") {
    return "ЗЕЛ";
  }
  if (color === "BLUE") {
    return "СИН";
  }
  if (color === "RED") {
    return "БЕЛ";
  }
  if (color === "ORANGE") {
    return "ОРАН";
  }
  if (color === "PURPLE") {
    return "ФИОЛ";
  }
  if (color === "WHITE") {
    return "БЕЛ";
  }
  return "СЕР";
}

function workerTone(color: WorkerColor): BoardRenderable["tone"] {
  if (color === "GREEN") {
    return "positive";
  }
  if (color === "BLUE") {
    return "info";
  }
  if (color === "ORANGE") {
    return "warning";
  }
  if (color === "WHITE") {
    return "info";
  }
  return "neutral";
}

function workerNumberFromId(workerId: string): string {
  const match = workerId.match(/(\d+)$/);
  return match ? match[1] : workerId;
}

function humanizeId(value: string): string {
  return value
    .replace(/^state_/, "гос_")
    .replace(/^private_/, "частн_")
    .replace(/[_-]+/g, " ")
    .trim();
}

function enterpriseDisplayName(enterprise: GameState["enterprises"][number]): string {
  const byId = ENTERPRISE_LABELS[enterprise.id];
  if (byId) {
    return byId;
  }
  const raw = (enterprise.name?.trim() || enterprise.id).toLowerCase();
  const byRaw = ENTERPRISE_LABELS[raw];
  if (byRaw) {
    return byRaw;
  }
  return humanizeId(raw);
}

function classShort(classType: string): string {
  if (classType === "MIDDLE_CLASS") {
    return "M";
  }
  if (classType === "CAPITALIST") {
    return "C";
  }
  if (classType === "STATE") {
    return "S";
  }
  return "W";
}

function foreignTradeCourse(state: GameState): PolicyCourse {
  return state.policies.find((policy) => policy.id === "POLICY_6_FOREIGN_TRADE")?.currentCourse ?? "B";
}

function policyCourseOf(state: GameState, policyId: PolicyId, fallback: PolicyCourse = "B"): PolicyCourse {
  return state.policies.find((policy) => policy.id === policyId)?.currentCourse ?? fallback;
}

function publicRowsUnlockedByFiscalPolicy(state: GameState): number {
  const fiscalCourse = policyCourseOf(state, "POLICY_1_FISCAL", "C");
  if (fiscalCourse === "A") {
    return 3;
  }
  if (fiscalCourse === "B") {
    return 2;
  }
  return 1;
}

function minimumWageLevelFromPolicy(state: GameState): 1 | 2 | 3 {
  const laborMarketCourse = policyCourseOf(state, "POLICY_2_LABOR_MARKET");
  if (laborMarketCourse === "A") {
    return 3;
  }
  if (laborMarketCourse === "B") {
    return 2;
  }
  return 1;
}

function importBasePrice(resource: "FOOD" | "LUXURY"): number {
  return resource === "FOOD" ? 10 : 6;
}

function importSurcharge(course: PolicyCourse, resource: "FOOD" | "LUXURY"): number {
  if (course === "A") {
    return resource === "FOOD" ? 10 : 6;
  }
  if (course === "B") {
    return resource === "FOOD" ? 5 : 3;
  }
  return 0;
}

function importFinalPrice(course: PolicyCourse, resource: "FOOD" | "LUXURY"): number {
  return importBasePrice(resource) + importSurcharge(course, resource);
}

function importCoursePriceMap(resource: "FOOD" | "LUXURY"): Record<PolicyCourse, number> {
  return {
    A: importFinalPrice("A", resource),
    B: importFinalPrice("B", resource),
    C: importFinalPrice("C", resource),
  };
}

function stateServiceResourceForEnterpriseId(enterpriseId: string): "healthcare" | "education" | "media_influence" | null {
  if (enterpriseId === "state_hospital") {
    return "healthcare";
  }
  if (enterpriseId === "state_university") {
    return "education";
  }
  if (enterpriseId === "state_media") {
    return "media_influence";
  }
  return null;
}

function stateEnterpriseGroup(enterprise: GameState["enterprises"][number]): "hospital" | "university" | "media" | "other" {
  const normalizedId = enterprise.id.toLowerCase();
  const sector = String(enterprise.sector ?? "").toUpperCase();
  const outputs = Object.keys(enterprise.producedResources ?? {}).map((value) => value.toLowerCase());

  if (
    normalizedId.includes("hospital") ||
    normalizedId.includes("clinic") ||
    sector === "HEALTHCARE" ||
    outputs.includes("healthcare")
  ) {
    return "hospital";
  }
  if (normalizedId.includes("university") || normalizedId.includes("college") || sector === "EDUCATION" || outputs.includes("education")) {
    return "university";
  }
  if (
    normalizedId.includes("media") ||
    normalizedId.includes("telecom") ||
    normalizedId.includes("publishing") ||
    sector === "MEDIA" ||
    outputs.includes("media_influence") ||
    outputs.includes("influence")
  ) {
    return "media";
  }
  return "other";
}

function stateOutputShort(group: "hospital" | "university" | "media" | "other"): string {
  if (group === "hospital") {
    return "МЕД";
  }
  if (group === "university") {
    return "ОБР";
  }
  if (group === "media") {
    return "МЕДИА";
  }
  return "РЕС";
}

function requiredFiscalCourseForRow(rowIndex: number): PolicyCourse {
  if (rowIndex <= 0) {
    return "C";
  }
  if (rowIndex === 1) {
    return "B";
  }
  return "A";
}

function fiscalPolicyHintForRow(rowIndex: number): string {
  if (rowIndex <= 0) {
    return "Открывается при курсе B или A";
  }
  if (rowIndex === 1) {
    return "Открывается при курсе B или A";
  }
  return "Открывается первой политикой при курсе A";
}

function slotVisualColor(
  slot: GameState["enterprises"][number]["slots"][number],
  worker?: GameState["workers"][number],
): WorkerColor {
  if (worker) {
    return workerColorOf(worker);
  }
  const fromSlot = String(slot.requiredColor ?? "").toUpperCase();
  if (
    fromSlot === "GRAY" ||
    fromSlot === "GREEN" ||
    fromSlot === "BLUE" ||
    fromSlot === "RED" ||
    fromSlot === "ORANGE" ||
    fromSlot === "PURPLE" ||
    fromSlot === "WHITE"
  ) {
    return normalizeWorkerColor(fromSlot);
  }
  if (slot.requiredQualification === "UNSKILLED") {
    return "GRAY";
  }
  return "WHITE";
}

function buildEnterpriseSlotVisual(
  slot: GameState["enterprises"][number]["slots"][number],
  workersById: Map<string, GameState["workers"][number]>,
  forceEmpty = false,
): EnterpriseSlotVisual {
  const worker = !forceEmpty && slot.occupiedWorkerId ? workersById.get(slot.occupiedWorkerId) : undefined;
  return {
    id: slot.id,
    state: forceEmpty ? "EMPTY" : worker ? (worker.tiedContract ? "TIED" : "OCCUPIED") : "EMPTY",
    color: slotVisualColor(slot, worker),
    requiredColor: slotVisualColor(slot),
    qualification: slot.requiredQualification,
    optional: Boolean(slot.optional),
    classType: worker?.classType,
    workerId: worker?.id,
    workerNumber: worker ? workerNumberFromId(worker.id) : undefined,
  };
}

function buildEnterpriseWageVisual(
  enterprise: GameState["enterprises"][number],
  minimumWageLevel: 1 | 2 | 3,
): EnterpriseWageLevelVisual[] {
  const wageTrack = enterprise.wageTrack ?? {};
  const levels: Array<{ level: 1 | 2 | 3; key: "low" | "medium" | "high" }> = [
    { level: 1, key: "low" },
    { level: 2, key: "medium" },
    { level: 3, key: "high" },
  ];

  return levels.map(({ level, key }) => ({
    level,
    value: typeof wageTrack[key] === "number" ? wageTrack[key] : undefined,
    active: Math.max(1, Math.min(3, enterprise.wageLevel)) === level,
    blocked: level < minimumWageLevel,
  }));
}

function enterpriseOutputLabel(enterprise: GameState["enterprises"][number]): string {
  const [resource, amount] = Object.entries(enterprise.producedResources ?? {})[0] ?? [];
  if (!resource) {
    return enterprise.automated ? "Автоматизация" : "Без выпуска";
  }
  const label =
    resource === "healthcare"
      ? "Медицина"
      : resource === "education"
        ? "Образование"
        : resource === "media_influence" || resource === "influence"
          ? "Влияние"
          : resource === "food"
            ? "Еда"
            : resource === "luxury"
              ? "Роскошь"
              : resource;
  return `${label} ${amount}`;
}

function buildEnterpriseRenderable({
  state,
  workersById,
  enterprise,
  zoneId,
  entityId,
  xPct,
  yPct,
  size,
  tone,
  cardState = "ACTIVE",
  rowLabel,
  policyHint,
  sourceRef,
}: {
  state: GameState;
  workersById: Map<string, GameState["workers"][number]>;
  enterprise: GameState["enterprises"][number];
  zoneId: string;
  entityId: string;
  xPct: number;
  yPct: number;
  size: BoardRenderable["size"];
  tone: BoardRenderable["tone"];
  cardState?: EnterpriseCardState;
  rowLabel?: string;
  policyHint?: string;
  sourceRef?: BoardRenderable["sourceRef"];
}): BoardRenderable {
  const minimumWageLevel = minimumWageLevelFromPolicy(state);
  const slotVisuals = enterprise.slots.map((slot) => buildEnterpriseSlotVisual(slot, workersById, cardState !== "ACTIVE"));
  const occupied = slotVisuals.filter((slot) => slot.state !== "EMPTY").length;
  const statusText =
    cardState === "ACTIVE" ? "активно" : cardState === "RESERVE" ? "открыто политикой, но пока без отдельной карты в срезе" : "закрыто политикой";
  const wageSummary = buildEnterpriseWageVisual(enterprise, minimumWageLevel)
    .map((wage) => `L${wage.level}:${wage.value ?? "-"}${wage.active ? "*" : ""}${wage.blocked ? " min" : ""}`)
    .join(", ");
  const slotSummary = slotVisuals
    .map((slot) => `${slot.state === "EMPTY" ? "пусто" : slot.state === "TIED" ? "связан" : "занят"} ${workerColorLabel(slot.color)} / нужно ${workerColorLabel(slot.requiredColor)}`)
    .join("; ");

  return {
    id: entityId,
    kind: "ENTERPRISE",
    zoneId,
    label: enterpriseDisplayName(enterprise),
    shortLabel: enterpriseDisplayName(enterprise),
    xPct,
    yPct,
    size,
    tone,
    sourceRef,
    details: `${rowLabel ? `${rowLabel} | ` : ""}${statusText} | занято ${occupied}/${slotVisuals.length} | выпуск ${enterpriseOutputLabel(enterprise)} | зарплаты ${wageSummary}${policyHint ? ` | ${policyHint}` : ""}${slotSummary ? ` | слоты: ${slotSummary}` : ""}`,
    clickable: true,
    visual: {
      variant: "enterprise-card",
      state: cardState,
      rowLabel,
      policyHint,
      outputLabel: enterpriseOutputLabel(enterprise),
      slots: slotVisuals,
      wages: buildEnterpriseWageVisual(enterprise, minimumWageLevel),
      minimumWageLevel,
    } satisfies EnterpriseCardVisual,
  } as BoardRenderable;
}

function asArray<T>(value: T[] | undefined | null): T[] {
  return Array.isArray(value) ? value : [];
}

function currentPlayerFromState(state: GameState) {
  const players = asArray(state.players);
  const currentIndexRaw = state.turnOrder?.currentPlayerIndex;
  const currentIndex = Number.isInteger(currentIndexRaw) ? Number(currentIndexRaw) : 0;
  return players[currentIndex];
}

function computePolicyTracks(state: GameState): PolicyTrackView[] {
  const currentPlayer = currentPlayerFromState(state);
  const actorCanPropose =
    state.currentPhase === "ACTIONS" &&
    Boolean(currentPlayer?.proposalTokens?.some((token) => token.available));
  const courseOrder: PolicyCourse[] = ["A", "B", "C"];

  return asArray(state.policies)
    .slice()
    .sort((a, b) => a.id.localeCompare(b.id))
    .map((policy) => {
      const zoneId = `policy:${policy.id}`;
      const zone = BOARD_ZONE_INDEX[zoneId];
      const blockedByExistingProposal = Boolean(policy.occupyingProposalToken);
      const blocked = !actorCanPropose || blockedByExistingProposal;
      const proposedCourse = policy.occupyingProposalToken?.targetCourse;

      const courses: PolicyCourseCellView[] = courseOrder.map((course) => ({
        course,
        active: policy.currentCourse === course,
        proposed: proposedCourse === course,
        selectable: !blocked && policy.currentCourse !== course,
        blocked: blocked || policy.currentCourse === course,
      }));

      return {
        policyId: policy.id,
        zoneId,
        label: POLICY_LABELS[policy.id],
        x: zone?.x ?? 0,
        y: zone?.y ?? 0,
        width: zone?.width ?? 0,
        height: zone?.height ?? 0,
        currentCourse: policy.currentCourse,
        proposedCourse,
        proposerLabel: policy.occupyingProposalToken
          ? policy.occupyingProposalToken.ownerPlayerId ?? classShort(policy.occupyingProposalToken.ownerClass)
          : undefined,
        selectable: !blocked,
        blocked,
        courses,
      };
    });
}

function zoneStats(zoneId: string, state: GameState): string[] {
  if (zoneId === "round_track") {
    return [`Раунд ${state.currentRound}/${state.maxRounds}`, `Фаза ${state.currentPhase}`];
  }
  if (zoneId === "vote_results") {
    if (!state.currentVoteState) {
      return ["Нет активного голосования", "Готово к следующему голосованию"];
    }
    return [
      `Политика ${state.currentVoteState.activeProposalPolicyId}`,
      `ЗА ${state.currentVoteState.totalForVotes} / ПРОТИВ ${state.currentVoteState.totalAgainstVotes}`,
    ];
  }
  if (zoneId === "treasury") {
    return [`Казна ${state.treasury}`];
  }
  if (zoneId === "state_services") {
    const services = state.publicServices ?? { healthcare: 0, education: 0, mediaInfluence: 0 };
    return [
      `Мед ${services.healthcare ?? 0}`,
      `Обр ${services.education ?? 0}`,
      `Медиа ${services.mediaInfluence ?? 0}`,
    ];
  }
  if (zoneId === "private_capitalist") {
    const all = asArray(state.enterprises).filter((enterprise) => enterprise.ownerClass === "CAPITALIST");
    const functioning = all.filter((enterprise) => enterprise.functioning).length;
    return [`Предприятия ${functioning}/${all.length}`];
  }
  if (zoneId === "private_middle_class") {
    const all = asArray(state.enterprises).filter((enterprise) => enterprise.ownerClass === "MIDDLE_CLASS");
    const functioning = all.filter((enterprise) => enterprise.functioning).length;
    return [`Предприятия ${functioning}/${all.length}`];
  }
  if (zoneId === "public_sector") {
    const all = asArray(state.enterprises).filter((enterprise) => enterprise.ownerClass === "STATE");
    const functioning = all.filter((enterprise) => enterprise.functioning).length;
    return [`Госпредприятия ${all.length}`, `Работают ${functioning}`];
  }
  if (zoneId === "unemployed") {
    const workers = asArray(state.workers);
    const unemployed = workers.filter((worker) => worker.location === "UNEMPLOYED").length;
    const unions = workers.filter((worker) => worker.location === "UNION").length;
    return [`Безработные ${unemployed}`, `Профсоюз ${unions}`];
  }
  if (zoneId === "deals") {
    const visibleDeals = currentVisibleBusinessDeals(state);
    const deck = state.businessDealDeck;
    return [
      `Открыто ${visibleDeals.length}`,
      deck?.lastRefreshedRound ? `Раунд ${deck.lastRefreshedRound}` : "Готово",
    ];
  }
  if (zoneId === "events") {
    const visibleEvents = currentVisibleStateEvents(state);
    const deck = state.stateEventDeck;
    return visibleEvents.length > 0
      ? [`События ${visibleEvents.length}`, `Колода ${Math.max(0, (deck?.orderedCardIds?.length ?? 0) - (deck?.nextCardIndex ?? 0))}`]
      : ["Нет событий"];
  }
  if (zoneId === "capitalist_enterprise_market") {
    const market = asArray(state.capitalistEnterpriseMarket);
    return [`Предприятия ${market.length}/4`, `Колода ${asArray(state.capitalistEnterpriseDeck).length}`];
  }
  if (zoneId === "import") {
    return [`Курс ${foreignTradeCourse(state)}`, "Еда и роскошь"];
  }
  if (zoneId === "export") {
    const exportCard = state.activeExportCard;
    if (!exportCard?.cardId) {
      return ["Нет активной карты экспорта"];
    }
    return [
      exportCard.title,
      `Операции ${exportCard.availableOperations}`,
      `Раунд ${exportCard.activatedRound}`,
    ];
  }
  return [];
}

function currentVisibleBusinessDeals(state: GameState): BusinessDealCard[] {
  const visibleIds = Array.isArray(state.businessDealDeck?.visibleCardIds) ? state.businessDealDeck.visibleCardIds : [];
  const cards = asArray(state.businessDealCards);
  return visibleIds
    .map((cardId) => cards.find((card) => card.id === cardId))
    .filter((card): card is BusinessDealCard => Boolean(card));
}

function currentVisibleStateEvents(state: GameState): StateEventCard[] {
  const visibleIds = Array.isArray(state.stateEventDeck?.visibleCardIds) ? state.stateEventDeck.visibleCardIds : [];
  const cards = asArray(state.stateEventCards);
  return visibleIds
    .map((cardId) => cards.find((card) => card.id === cardId))
    .filter((card): card is StateEventCard => Boolean(card));
}

function dealShortResource(resourceId: string): string {
  if (resourceId === "food") {
    return "🌾";
  }
  if (resourceId === "luxury") {
    return "💎";
  }
  if (resourceId === "healthcare") {
    return "⚕";
  }
  if (resourceId === "education") {
    return "🎓";
  }
  if (resourceId === "influence" || resourceId === "media_influence") {
    return "★";
  }
  return resourceId.slice(0, 1).toUpperCase();
}

function resourceName(resourceId: string): string {
  const normalized = resourceId.toLowerCase();
  if (normalized === "food") {
    return "еда";
  }
  if (normalized === "luxury") {
    return "роскошь";
  }
  if (normalized === "healthcare") {
    return "медицина";
  }
  if (normalized === "education") {
    return "образование";
  }
  if (normalized === "influence" || normalized === "media_influence") {
    return "влияние";
  }
  return resourceId.replace(/[_-]+/g, " ");
}

function exportOfferSummary(resourceId: string, quantity: number, revenue: number): string {
  return `${quantity} ${resourceName(resourceId)} -> ${revenue} монет`;
}

function dealRequirementSummary(resourceId: string, amount: number): string {
  return `${amount} ${resourceName(resourceId)}`;
}

function dealRequirementCompact(resourceId: string, amount: number): string {
  return `${amount}${dealShortResource(resourceId)}`;
}

function exportOfferCompact(resourceId: string, quantity: number, revenue: number): string {
  return `${quantity}${dealShortResource(resourceId)}→${revenue}$`;
}

function renderCapitalistEnterpriseMarketObjects(state: GameState): BoardRenderable[] {
  const zone = effectiveBoardZone(state, "capitalist_enterprise_market");
  if (!zone) {
    return [];
  }
  const market = asArray(state.capitalistEnterpriseMarket).slice(0, 4);
  const workersById = new Map(asArray(state.workers).map((worker) => [worker.id, worker] as const));
  return market.map((enterprise, index) => {
    const placement = layoutEntityInZone(zone, index, market.length || 4, 4);
    return buildEnterpriseRenderable({
      state,
      workersById,
      enterprise,
      zoneId: "capitalist_enterprise_market",
      entityId: `enterprise-market:${enterprise.id}`,
      xPct: placement.x,
      yPct: placement.y,
      size: "sm",
      tone: "positive",
      cardState: "RESERVE",
      sourceRef: { sourceType: "enterpriseMarket", sourceId: enterprise.id },
      rowLabel: `Цена ${enterprise.cost || 0}`,
    });
  });
}

function renderBusinessDealObjects(state: GameState): BoardRenderable[] {
  const zone = BOARD_ZONE_INDEX.deals;
  if (!zone) {
    return [];
  }

  const visibleDeals = currentVisibleBusinessDeals(state);
  return visibleDeals.map((deal, index) => {
    const total = Math.max(visibleDeals.length, 1);
    const safeIndex = total === 1 ? 0.5 : (index + 0.5) / total;
    const placement = {
      x: zone.x + zone.width * (0.14 + safeIndex * 0.72),
      y: zone.y + zone.height * 0.52,
    };
    const requirementText = deal.requirements
      .map((requirement) => dealRequirementSummary(requirement.resourceId, requirement.amount))
      .join(" + ");
    const requirementCompact = deal.requirements
      .map((requirement) => dealRequirementCompact(requirement.resourceId, requirement.amount))
      .join("+");
    const bonusText = `если требование >= ${deal.thresholdAmount}: курс A +${deal.policyABonus}, курс B +${deal.policyBBonus}`;

    return {
      id: `business-deal:${deal.id}`,
      kind: "CARD",
      zoneId: "deals",
      label: `${deal.title}: выплата ${deal.payout}`,
      shortLabel: `${requirementCompact}→${deal.payout}$`,
      xPct: placement.x,
      yPct: placement.y,
      size: "lg",
      tone: "warning",
      sourceRef: { sourceType: "businessDeal", sourceId: deal.id },
      details: `Нужно отдать: ${requirementText}. Выплата: ${deal.payout} монет. Бонус сделки: ${bonusText}.`,
      clickable: true,
    };
  });
}

function renderExportCardObject(state: GameState): BoardRenderable[] {
  const zone = effectiveBoardZone(state, "export");
  const exportCard = state.activeExportCard;
  if (!zone || !exportCard?.cardId) {
    return [];
  }

  return [
    {
      id: `export-card:${exportCard.cardId}`,
      kind: "CARD",
      zoneId: "export",
      label: exportCard.title,
      shortLabel: `${(Array.isArray(exportCard.offers) ? exportCard.offers : []).map((offer) => exportOfferCompact(offer.resourceId, offer.quantity, offer.revenue)).join(" · ")}`,
      xPct: zone.x + zone.width * 0.5,
      yPct: zone.y + zone.height * 0.54,
      size: "lg",
      tone: "info",
      sourceRef: { sourceType: "exportCard", sourceId: exportCard.cardId },
      details: `Варианты: ${(Array.isArray(exportCard.offers) ? exportCard.offers : []).map((offer) => exportOfferSummary(offer.resourceId, offer.quantity, offer.revenue)).join("; ")}. Активна с раунда ${exportCard.activatedRound}.`,
      clickable: true,
    },
  ];
}

function renderStateEventObjects(state: GameState): BoardRenderable[] {
  const zone = effectiveBoardZone(state, "events");
  if (!zone) {
    return [];
  }

  const visibleEvents = currentVisibleStateEvents(state);
  return visibleEvents.map((event, index) => {
    const total = Math.max(visibleEvents.length, 1);
    const xRatio = total === 1 ? 0.5 : (index + 0.5) / total;
    const options = (Array.isArray(event.options) ? event.options : []).map((option) => option.summary).join("; ");
    const penalties = (Array.isArray(event.noActionPenaltyClasses) && event.noActionPenaltyClasses.length > 0)
      ? event.noActionPenaltyClasses.join(", ")
      : "любые 2 самые низкие";
    return {
      id: `state-event:${event.id}`,
      kind: "CARD",
      zoneId: "events",
      label: event.title,
      shortLabel: event.title.slice(0, 18),
      xPct: zone.x + zone.width * xRatio,
      yPct: zone.y + zone.height * 0.66,
      size: "sm",
      tone: "warning",
      sourceRef: { sourceType: "stateEvent", sourceId: event.id },
      details: `${event.instruction}. Варианты: ${options}. Бездействие: -1 легитимность (${penalties}).`,
      clickable: true,
    };
  });
}

function renderImportPriceObjects(state: GameState): BoardRenderable[] {
  const zone = effectiveBoardZone(state, "import");
  if (!zone) {
    return [];
  }
  const course = foreignTradeCourse(state);
  const foodBase = importBasePrice("FOOD");
  const foodTariff = importSurcharge(course, "FOOD");
  const foodFinal = importFinalPrice(course, "FOOD");
  const foodMap = importCoursePriceMap("FOOD");

  const luxuryBase = importBasePrice("LUXURY");
  const luxuryTariff = importSurcharge(course, "LUXURY");
  const luxuryFinal = importFinalPrice(course, "LUXURY");
  const luxuryMap = importCoursePriceMap("LUXURY");

  const foodPlacement = {
    x: zone.x + zone.width * 0.26,
    y: zone.y + zone.height * 0.58,
  };
  const luxuryPlacement = {
    x: zone.x + zone.width * 0.74,
    y: zone.y + zone.height * 0.58,
  };

  return [
    {
      id: "import-price-food",
      kind: "CARD",
      zoneId: "import",
      label: `Еда ${foodBase}+${foodTariff}=${foodFinal} (${course})`,
      shortLabel: `Еда ${foodFinal}`,
      xPct: foodPlacement.x + zone.width * 0.03,
      yPct: foodPlacement.y,
      size: "sm",
      tone: "warning",
      sourceRef: { sourceType: "zone", sourceId: "import" },
      details: `A:${foodMap.A}  B:${foodMap.B}  C:${foodMap.C} | текущая: ${course}`,
      clickable: true,
    },
    {
      id: "import-price-luxury",
      kind: "CARD",
      zoneId: "import",
      label: `Роскошь ${luxuryBase}+${luxuryTariff}=${luxuryFinal} (${course})`,
      shortLabel: `Роскошь ${luxuryFinal}`,
      xPct: luxuryPlacement.x - zone.width * 0.03,
      yPct: luxuryPlacement.y,
      size: "sm",
      tone: "info",
      sourceRef: { sourceType: "zone", sourceId: "import" },
      details: `A:${luxuryMap.A}  B:${luxuryMap.B}  C:${luxuryMap.C} | текущая: ${course}`,
      clickable: true,
    },
  ];
}

function sectorZoneForOwner(ownerClass: string): string {
  if (ownerClass === "CAPITALIST") {
    return "private_capitalist";
  }
  if (ownerClass === "MIDDLE_CLASS") {
    return "private_middle_class";
  }
  if (ownerClass === "STATE") {
    return "public_sector";
  }
  return "private_middle_class";
}

function hasClassPlayer(state: GameState, classType: string): boolean {
  return asArray(state.players).some((player) => player.classType === classType);
}

function effectiveBoardZone(state: GameState, zoneId: string): BoardZoneDefinition | undefined {
  const zone = BOARD_ZONE_INDEX[zoneId];
  if (!zone) {
    return undefined;
  }
  let effectiveZone = zone;
  if ((zoneId === "private_capitalist" || zoneId === "capitalist_enterprise_market") && !hasClassPlayer(state, "MIDDLE_CLASS")) {
    effectiveZone = {
      ...effectiveZone,
      x: 2,
      width: 82,
    };
  }
  if (zoneId === "private_capitalist") {
    return {
      ...effectiveZone,
      height: privateCapitalistZoneHeight(state, effectiveZone),
    };
  }
  return effectiveZone;
}

function effectiveBoardZones(state: GameState): BoardZoneDefinition[] {
  return BOARD_ZONES
    .filter((zone) => zone.id !== "round_track")
    .filter((zone) => zone.id !== "state_sector_staging")
    .filter((zone) => zone.id !== "private_middle_class" || hasClassPlayer(state, "MIDDLE_CLASS"))
    .map((zone) => effectiveBoardZone(state, zone.id) ?? zone);
}

function layoutEntityInZone(
  zone: BoardZoneDefinition,
  index: number,
  total: number,
  preferredColumns?: number,
): { x: number; y: number; compact: boolean } {
  const titleReserve = 5.0;
  const padX = 1.5;
  const padY = 1.5;
  const columns = zone.id === "private_capitalist"
    ? privateCapitalistColumnCount(zone, total)
    : preferredColumns ?? Math.max(1, Math.ceil(Math.sqrt(total)));
  const rows = Math.max(1, Math.ceil(total / columns));
  const fullSafeWidth = Math.max(1, zone.width - padX * 2);
  const safeWidth = fullSafeWidth;
  const safeHeight = Math.max(1, zone.height - (zone.id === "capitalist_enterprise_market" ? 2.8 : titleReserve) - padY);
  const cellWidth = safeWidth / columns;
  const cellHeight = zone.id === "private_capitalist" ? PRIVATE_CAPITALIST_ROW_HEIGHT : safeHeight / rows;
  const col = index % columns;
  const row = Math.floor(index / columns);
  const clusterOffsetX = 0;
  return {
    x: zone.x + padX + clusterOffsetX + cellWidth * (col + 0.5),
    y: zone.y + (zone.id === "capitalist_enterprise_market" ? 2.8 : titleReserve) + cellHeight * (row + 0.5),
    compact: Math.min(cellWidth, cellHeight) < 5,
  };
}

const PRIVATE_CAPITALIST_CARD_SPAN = 11;
const PRIVATE_CAPITALIST_ROW_HEIGHT = 11.2;
const PRIVATE_CAPITALIST_BOTTOM_RESERVE = 2.4;

function privateCapitalistEnterpriseCount(state: GameState): number {
  return asArray(state.enterprises).filter((enterprise) => enterprise.ownerClass === "CAPITALIST").length;
}

function privateCapitalistColumnCount(zone: BoardZoneDefinition, total: number): number {
  if (total <= 0) {
    return 1;
  }
  const usableWidth = Math.max(1, zone.width - 3);
  const columnsThatFit = Math.max(1, Math.floor(usableWidth / PRIVATE_CAPITALIST_CARD_SPAN));
  return Math.min(total, columnsThatFit);
}

function privateCapitalistZoneHeight(state: GameState, zone: BoardZoneDefinition): number {
  const total = privateCapitalistEnterpriseCount(state);
  const columns = privateCapitalistColumnCount(zone, total);
  const rows = Math.max(1, Math.ceil(total / columns));
  return Math.max(zone.height, 5 + rows * PRIVATE_CAPITALIST_ROW_HEIGHT + PRIVATE_CAPITALIST_BOTTOM_RESERVE);
}

function boardHeightForState(state: GameState): number {
  const privateCapitalistZone = effectiveBoardZone(state, "private_capitalist");
  return Math.max(100, (privateCapitalistZone?.y ?? 0) + (privateCapitalistZone?.height ?? 0) + 1.5);
}

function stateResourceSummaryLabel(group: "hospital" | "university" | "media"): string {
  if (group === "hospital") {
    return "МЕД";
  }
  if (group === "university") {
    return "ОБР";
  }
  return "МЕДИА";
}

function stateEnterpriseLabel(group: "hospital" | "university" | "media"): string {
  if (group === "hospital") {
    return "Гос. больницы";
  }
  if (group === "university") {
    return "Гос. университеты";
  }
  return "Гос. медиа";
}

function stateResourceLabel(group: "hospital" | "university" | "media"): string {
  if (group === "hospital") {
    return "Медицина";
  }
  if (group === "university") {
    return "Образование";
  }
  return "Влияние";
}

function stateResourceAmount(state: GameState, group: "hospital" | "university" | "media"): number {
  if (group === "hospital") {
    return Math.max(0, state.publicServicesStorage?.healthcare ?? 0);
  }
  if (group === "university") {
    return Math.max(0, state.publicServicesStorage?.education ?? 0);
  }
  return Math.max(0, (state.publicServicesStorage?.media_influence ?? state.publicServicesStorage?.influence ?? 0));
}

function renderEnterpriseObjects(state: GameState): BoardRenderable[] {
  const groupedByZone = new Map<string, Array<GameState["enterprises"][number]>>();
  const workersById = new Map(asArray(state.workers).map((worker) => [worker.id, worker] as const));
  asArray(state.enterprises).forEach((enterprise) => {
    const zoneId = sectorZoneForOwner(enterprise.ownerClass);
    const list = groupedByZone.get(zoneId) ?? [];
    list.push(enterprise);
    groupedByZone.set(zoneId, list);
  });

  const renderables: BoardRenderable[] = [];

  groupedByZone.forEach((enterprises, zoneId) => {
    const zone = effectiveBoardZone(state, zoneId);
    if (!zone) {
      return;
    }

    if (zoneId === "public_sector") {
      const unlockedRows = publicRowsUnlockedByFiscalPolicy(state);
      const byGroup = {
        hospital: enterprises.filter((enterprise) => stateEnterpriseGroup(enterprise) === "hospital"),
        university: enterprises.filter((enterprise) => stateEnterpriseGroup(enterprise) === "university"),
        media: enterprises.filter((enterprise) => stateEnterpriseGroup(enterprise) === "media"),
      };
      const orderedGroups: Array<"hospital" | "university" | "media"> = ["hospital", "university", "media"];
      const titleReserve = 7.2;
      const padX = 2.2;
      const padY = 2.0;
      const columns = 3;
      const rows = 3;
      const cellWidth = Math.max(1, (zone.width - padX * 2) / columns);
      const cellHeight = Math.max(1, (zone.height - titleReserve - padY) / rows);

      orderedGroups.forEach((group, columnIndex) => {
        const enterprisesInColumn = byGroup[group];
        const templateEnterprise = enterprisesInColumn[0];
        if (!templateEnterprise) {
          return;
        }
        const centerX = zone.x + padX + cellWidth * (columnIndex + 0.5);
        const sortedEnterprises = [...enterprisesInColumn].sort((left, right) => left.id.localeCompare(right.id));
        const visibleEnterprises = sortedEnterprises.slice(0, unlockedRows);

        visibleEnterprises.forEach((existingEnterprise, rowIndex) => {
          const enterpriseRenderable = buildEnterpriseRenderable({
              state,
              workersById,
              enterprise: existingEnterprise,
              zoneId,
              entityId: `enterprise:${existingEnterprise.id}`,
              xPct: centerX,
              yPct: zone.y + titleReserve + cellHeight * (rowIndex + 0.5),
              size: "md",
              tone: existingEnterprise.functioning ? "positive" : "danger",
              cardState: "ACTIVE",
              sourceRef: { sourceType: "enterprise", sourceId: existingEnterprise.id },
            });
          renderables.push(enterpriseRenderable);
        });
      });

      return;
    }

    const preferredColumns = zoneId === "private_middle_class" ? 3 : undefined;

    enterprises.forEach((enterprise, index) => {
      const placement = layoutEntityInZone(zone, index, enterprises.length, preferredColumns);
      const enterpriseRenderable = buildEnterpriseRenderable({
          state,
          workersById,
          enterprise,
          zoneId,
          entityId: `enterprise:${enterprise.id}`,
          xPct: placement.x,
          yPct: placement.y,
          size: zoneId === "private_capitalist" ? "sm" : placement.compact ? "sm" : "md",
          tone: enterprise.functioning ? "positive" : "danger",
          sourceRef: { sourceType: "enterprise", sourceId: enterprise.id },
        });
      renderables.push(enterpriseRenderable);
    });
  });

  return renderables;
}

function renderWorkerObjects(state: GameState): BoardRenderable[] {
  const renderables: BoardRenderable[] = [];
  const unemployed = asArray(state.workers).filter((worker) => worker.location === "UNEMPLOYED");
  const zone = effectiveBoardZone(state, "unemployed");
  if (!zone) {
    return renderables;
  }

  const maxVisible = 24;
  const visible = unemployed.slice(0, maxVisible);
  const byClass = new Map<string, typeof visible>();
  visible.forEach((worker) => {
    const key = worker.classType === "MIDDLE_CLASS" ? "MIDDLE_CLASS" : "WORKER";
    byClass.set(key, [...(byClass.get(key) ?? []), worker]);
  });
  const padX = 1.2;
  const topReserve = 3.9;
  const bottomReserve = 1.8;
  const safeWidth = Math.max(1, zone.width - padX * 2);
  const safeHeight = Math.max(1, zone.height - topReserve - bottomReserve);
  const lanes = [
    { classType: "WORKER", label: "Рабочий класс", workers: byClass.get("WORKER") ?? [] },
    { classType: "MIDDLE_CLASS", label: "Средний класс", workers: byClass.get("MIDDLE_CLASS") ?? [] },
  ].filter((lane) => lane.workers.length > 0);
  const laneHeight = safeHeight / Math.max(1, lanes.length);

  lanes.forEach((lane, laneIndex) => {
    const laneWorkers = lane.workers;
    const workerColumns = laneWorkers.length <= 4 ? 4 : laneWorkers.length <= 10 ? 5 : 6;
    const rows = Math.max(1, Math.ceil(Math.max(laneWorkers.length, 1) / workerColumns));
    const cellWidth = safeWidth / workerColumns;
    const cellHeight = laneHeight / rows;
    const laneTop = zone.y + topReserve + laneHeight * laneIndex;

    renderables.push({
      id: `unemployed-label:${lane.classType}`,
      kind: "INFO_MARKER",
      zoneId: "unemployed",
      label: lane.label,
      shortLabel: lane.classType === "MIDDLE_CLASS" ? "СРЕД" : "РАБ",
      xPct: zone.x + padX + 1.4,
      yPct: laneTop + 0.45,
      size: "sm",
      tone: lane.classType === "MIDDLE_CLASS" ? "warning" : "neutral",
      sourceRef: { sourceType: "zone", sourceId: "unemployed" },
      details: `${lane.label}: ${laneWorkers.length}`,
      clickable: false,
    });

    laneWorkers.forEach((worker, index) => {
      const col = index % workerColumns;
      const row = Math.floor(index / workerColumns);
      const x = zone.x + padX + cellWidth * (col + 0.5);
      const y = laneTop + cellHeight * (row + 0.5) + 0.75;
      const color = workerColorOf(worker);
      const shortColor = workerColorShort(color);
      const workerNo = workerNumberFromId(worker.id);
      const classLabel = worker.classType === "MIDDLE_CLASS" ? "Средний класс" : "Рабочий класс";
      renderables.push({
        id: `worker:${worker.id}`,
        kind: "WORKER",
        zoneId: "unemployed",
        label: `${workerColorLabel(color)} работник (${classShort(worker.classType)})`,
        shortLabel: `${classShort(worker.classType)}${shortColor}`,
        xPct: x,
        yPct: y,
        size: "sm",
        tone: workerTone(color),
        sourceRef: { sourceType: "worker", sourceId: worker.id },
        details: `${classLabel} #${workerNo} (${workerColorLabel(color)}, ${worker.qualificationType === "SKILLED" ? "квалифицированный" : "обычный"})`,
        visual: {
          variant: "worker-meeple",
          color,
          workerNumber: workerNo,
          classType: worker.classType,
          tied: worker.tiedContract,
          posture: "STANDING",
        } satisfies WorkerMeepleVisual,
        clickable: true,
      });
    });
  });

  if (unemployed.length > maxVisible) {
    renderables.push({
      id: "worker:overflow",
      kind: "INFO_MARKER",
      zoneId: "unemployed",
      label: `Еще +${unemployed.length - maxVisible}`,
      shortLabel: `+${unemployed.length - maxVisible}`,
      xPct: zone.x + zone.width * 0.5,
      yPct: zone.y + zone.height - 1.5,
      size: "sm",
      tone: "warning",
      sourceRef: { sourceType: "zone", sourceId: "unemployed" },
      details: "Часть безработных скрыта в компактном режиме.",
      clickable: true,
    });
  }

  return renderables;
}

function renderMetaObjects(state: GameState): BoardRenderable[] {
  const renderables: BoardRenderable[] = [];
  const voteZone = effectiveBoardZone(state, "vote_results");

  if (voteZone) {
    const votingBag = state.votingBag ?? { worker: 0, middleClass: 0, capitalist: 0 };
    renderables.push({
      id: "voting-bag-summary",
      kind: "VOTE_CUBE",
      zoneId: "vote_results",
      label: `Мешок W${votingBag.worker ?? 0}/M${votingBag.middleClass ?? 0}/C${votingBag.capitalist ?? 0}`,
      shortLabel: "МШ",
      xPct: voteZone.x + voteZone.width * 0.5,
      yPct: voteZone.y + voteZone.height * 0.72,
      size: "sm",
      tone: "warning",
      sourceRef: { sourceType: "zone", sourceId: "vote_results" },
      details: "Состав мешка голосования.",
      clickable: true,
    });
  }

  return renderables;
}

export function buildBoardViewModel({
  state,
  selectedZoneId,
  selectedEntityId,
  highlightedZones = [],
}: BuildBoardViewModelInput): BoardViewModel {
  const policyTracks = computePolicyTracks(state);
  const renderables = [
    ...renderMetaObjects(state),
    ...renderImportPriceObjects(state),
    ...renderCapitalistEnterpriseMarketObjects(state),
    ...renderBusinessDealObjects(state),
    ...renderExportCardObject(state),
    ...renderStateEventObjects(state),
    ...renderEnterpriseObjects(state),
    ...renderWorkerObjects(state),
  ];

  const hasHighlights = highlightedZones.length > 0;
  const zones: BoardZoneView[] = effectiveBoardZones(state).map((zone) => {
    const active =
      zone.id === selectedZoneId ||
      renderables.some((entity) => entity.id === selectedEntityId && entity.zoneId === zone.id);
    const highlighted = highlightedZones.includes(zone.id);
    const dimmed = hasHighlights && !active && !highlighted;
    return {
      ...zone,
      active,
      highlighted,
      dimmed,
      stats: zoneStats(zone.id, state),
    };
  });

  const pendingProposalPoliciesInOrder = asArray(state.policies)
    .filter((policy) => Boolean(policy.occupyingProposalToken))
    .sort((a, b) => a.id.localeCompare(b.id))
    .map((policy) => policy.id);

  return {
    zones,
    renderables,
    pendingProposalPoliciesInOrder,
    policyTracks,
    boardHeight: boardHeightForState(state),
  };
}


