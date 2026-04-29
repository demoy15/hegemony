import { BOARD_ZONE_INDEX, BOARD_ZONES } from "@/features/board/layout/board-zones";
import type {
  BoardRenderable,
  BoardViewModel,
  BoardZoneDefinition,
  BoardZoneView,
  PolicyCourseCellView,
  PolicyTrackView,
} from "@/features/board/model/types";
import type { BusinessDealCard, GameState, PolicyCourse, PolicyId } from "@/types/game";

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
  classType?: "WORKER" | "MIDDLE_CLASS" | "CAPITALIST" | "STATE";
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
}

function workerColorOf(worker: GameState["workers"][number]): WorkerColor {
  const sector = String(worker.sector ?? "").toUpperCase();
  if (sector === "GREEN" || sector === "BLUE" || sector === "RED" || sector === "ORANGE" || sector === "PURPLE" || sector === "WHITE") {
    return sector;
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
    return "Красный";
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
    return "КРАС";
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
  if (color === "RED") {
    return "danger";
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
    return fromSlot;
  }
  if (worker) {
    return workerColorOf(worker);
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
    classType: worker?.classType,
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

function isAdjacent(from: PolicyCourse, to: PolicyCourse): boolean {
  if (from === "A") {
    return to === "B";
  }
  if (from === "B") {
    return to === "A" || to === "C";
  }
  return to === "B";
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
        selectable: !blocked && isAdjacent(policy.currentCourse, course) && policy.currentCourse !== course,
        blocked: blocked || policy.currentCourse === course || !isAdjacent(policy.currentCourse, course),
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

function dealShortResource(resourceId: string): string {
  if (resourceId === "food") {
    return "🍞";
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

function renderBusinessDealObjects(state: GameState): BoardRenderable[] {
  const zone = BOARD_ZONE_INDEX.deals;
  if (!zone) {
    return [];
  }

  const visibleDeals = currentVisibleBusinessDeals(state);
  return visibleDeals.map((deal, index) => {
    const placement = layoutEntityInZone(zone, index, Math.max(visibleDeals.length, 1), 1);
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
  const zone = BOARD_ZONE_INDEX.export;
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
      yPct: zone.y + zone.height * 0.55,
      size: "lg",
      tone: "info",
      sourceRef: { sourceType: "exportCard", sourceId: exportCard.cardId },
      details: `Варианты: ${(Array.isArray(exportCard.offers) ? exportCard.offers : []).slice(0, 4).map((offer) => exportOfferSummary(offer.resourceId, offer.quantity, offer.revenue)).join("; ")}. Активна с раунда ${exportCard.activatedRound}.`,
      clickable: true,
    },
  ];
}

function renderImportPriceObjects(state: GameState): BoardRenderable[] {
  const zone = BOARD_ZONE_INDEX.import;
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

  const foodPlacement = layoutEntityInZone(zone, 0, 2, 1);
  const luxuryPlacement = layoutEntityInZone(zone, 1, 2, 1);

  return [
    {
      id: "import-price-food",
      kind: "CARD",
      zoneId: "import",
      label: `Еда ${foodBase}+${foodTariff}=${foodFinal} (${course})`,
      shortLabel: `Еда ${foodFinal}`,
      xPct: foodPlacement.x,
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
      xPct: luxuryPlacement.x,
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

function layoutEntityInZone(
  zone: BoardZoneDefinition,
  index: number,
  total: number,
  preferredColumns?: number,
): { x: number; y: number; compact: boolean } {
  const titleReserve = 5.0;
  const padX = 1.5;
  const padY = 1.5;
  const columns = preferredColumns ?? Math.max(1, Math.ceil(Math.sqrt(total)));
  const rows = Math.max(1, Math.ceil(total / columns));
  const safeWidth = Math.max(1, zone.width - padX * 2);
  const safeHeight = Math.max(1, zone.height - titleReserve - padY);
  const cellWidth = safeWidth / columns;
  const cellHeight = safeHeight / rows;
  const col = index % columns;
  const row = Math.floor(index / columns);
  return {
    x: zone.x + padX + cellWidth * (col + 0.5),
    y: zone.y + titleReserve + cellHeight * (row + 0.5),
    compact: Math.min(cellWidth, cellHeight) < 5,
  };
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
    const zone = BOARD_ZONE_INDEX[zoneId];
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
      const titleReserve = 3.6;
      const headerY = zone.y + titleReserve + 0.2;
      const enterpriseStartY = zone.y + titleReserve + 5.2;
      const bottomPad = 1.4;
      const columnWidth = zone.width / 3;
      const usableHeight = Math.max(9, zone.y + zone.height - bottomPad - enterpriseStartY);

      orderedGroups.forEach((group, columnIndex) => {
        const enterprisesInColumn = byGroup[group];
        const templateEnterprise = enterprisesInColumn[0];
        if (!templateEnterprise) {
          return;
        }
        const centerX = zone.x + columnWidth * (columnIndex + 0.5);
        const amount = stateResourceAmount(state, group);
        const short = stateResourceSummaryLabel(group);
        const resourceLabel = stateResourceLabel(group);
        const enterpriseLabel = stateEnterpriseLabel(group);

        renderables.push({
          id: `state-service-stock:${group}`,
          kind: "CARD",
          zoneId,
          label: `${resourceLabel}: ${amount}`,
          shortLabel: `${short} ${amount}`,
          xPct: centerX,
          yPct: headerY,
          size: "sm",
          tone: group === "media" ? "info" : "warning",
          sourceRef: { sourceType: "zone", sourceId: `state-service-stock:${group}` },
          details: `${enterpriseLabel}: предприятий ${enterprisesInColumn.length}, доступно ${resourceLabel.toLowerCase()} ${amount}.`,
          clickable: true,
        });

        const sortedEnterprises = [...enterprisesInColumn].sort((left, right) => left.id.localeCompare(right.id));
        const visibleEnterprises = sortedEnterprises.slice(0, unlockedRows);
        const rowStep = usableHeight / Math.max(visibleEnterprises.length, 1);

        visibleEnterprises.forEach((existingEnterprise, rowIndex) => {
          renderables.push(
            buildEnterpriseRenderable({
              state,
              workersById,
              enterprise: existingEnterprise,
              zoneId,
              entityId: `enterprise:${existingEnterprise.id}`,
              xPct: centerX,
              yPct: enterpriseStartY + rowStep * (rowIndex + 0.5),
              size: "sm",
              tone: existingEnterprise.functioning ? "positive" : "danger",
              cardState: "ACTIVE",
              rowLabel: `Ряд ${rowIndex + 1}`,
              sourceRef: { sourceType: "enterprise", sourceId: existingEnterprise.id },
            }),
          );
        });
      });

      return;
    }

    const preferredColumns = zoneId === "private_middle_class" ? 4 : zoneId === "private_capitalist" ? 4 : undefined;

    enterprises.forEach((enterprise, index) => {
      const placement = layoutEntityInZone(zone, index, enterprises.length, preferredColumns);
      const crowdedPrivateSector =
        preferredColumns !== undefined &&
        (zoneId === "private_middle_class" || zoneId === "private_capitalist") &&
        enterprises.length > preferredColumns;
      renderables.push(
        buildEnterpriseRenderable({
          state,
          workersById,
          enterprise,
          zoneId,
          entityId: `enterprise:${enterprise.id}`,
          xPct: placement.x,
          yPct: placement.y,
          size: placement.compact || crowdedPrivateSector ? "sm" : "md",
          tone: enterprise.functioning ? "positive" : "danger",
          sourceRef: { sourceType: "enterprise", sourceId: enterprise.id },
        }),
      );
    });
  });

  return renderables;
}
function renderWorkerObjects(state: GameState): BoardRenderable[] {
  const renderables: BoardRenderable[] = [];
  const unemployed = asArray(state.workers).filter((worker) => worker.location === "UNEMPLOYED");
  const zone = BOARD_ZONE_INDEX.unemployed;
  if (!zone) {
    return renderables;
  }

  const maxVisible = 24;
  const visible = unemployed.slice(0, maxVisible);
  const workerColumns = visible.length <= 6 ? 3 : visible.length <= 12 ? 4 : 5;
  const rows = Math.max(1, Math.ceil(Math.max(visible.length, 1) / workerColumns));
  const padX = 1.2;
  const topReserve = 3.9;
  const bottomReserve = 1.8;
  const safeWidth = Math.max(1, zone.width - padX * 2);
  const safeHeight = Math.max(1, zone.height - topReserve - bottomReserve);
  const cellWidth = safeWidth / workerColumns;
  const cellHeight = safeHeight / rows;
  visible.forEach((worker, index) => {
    const col = index % workerColumns;
    const row = Math.floor(index / workerColumns);
    const x = zone.x + padX + cellWidth * (col + 0.5);
    const y = zone.y + topReserve + cellHeight * (row + 0.5);
    const color = workerColorOf(worker);
    const shortColor = workerColorShort(color);
    const workerNo = workerNumberFromId(worker.id);
    renderables.push({
      id: `worker:${worker.id}`,
      kind: "WORKER",
      zoneId: "unemployed",
      label: `${workerColorLabel(color)} рабочий`,
      shortLabel: shortColor,
      xPct: x,
      yPct: y,
      size: "sm",
      tone: workerTone(color),
      sourceRef: { sourceType: "worker", sourceId: worker.id },
      details: `Рабочий #${workerNo} (${workerColorLabel(color)}, ${worker.qualificationType === "SKILLED" ? "квалифицированный" : "обычный"})`,
      visual: {
        variant: "worker-meeple",
        color,
        workerNumber: workerNo,
        classType: worker.classType,
        tied: worker.tiedContract,
      } satisfies WorkerMeepleVisual,
      clickable: true,
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
  const roundZone = BOARD_ZONE_INDEX.round_track;
  const voteZone = BOARD_ZONE_INDEX.vote_results;
  const treasuryZone = BOARD_ZONE_INDEX.treasury;

  if (roundZone) {
    renderables.push({
      id: "round-marker",
      kind: "ROUND_MARKER",
      zoneId: "round_track",
      label: `R${state.currentRound}`,
      shortLabel: `R${state.currentRound}`,
      xPct: roundZone.x + roundZone.width * 0.5,
      yPct: roundZone.y + roundZone.height * 0.72,
      size: "sm",
      tone: "info",
      sourceRef: { sourceType: "zone", sourceId: "round_track" },
      details: `Фаза: ${state.currentPhase}`,
      clickable: true,
    });
  }

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

  if (treasuryZone) {
    renderables.push({
      id: "treasury-money",
      kind: "MONEY_TOKEN",
      zoneId: "treasury",
      label: `$ ${state.treasury}`,
      shortLabel: `${state.treasury}`,
      xPct: treasuryZone.x + treasuryZone.width * 0.5,
      yPct: treasuryZone.y + treasuryZone.height * 0.72,
      size: "sm",
      tone: "positive",
      sourceRef: { sourceType: "zone", sourceId: "treasury" },
      details: "Государственная казна.",
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
    ...renderBusinessDealObjects(state),
    ...renderExportCardObject(state),
    ...renderEnterpriseObjects(state),
    ...renderWorkerObjects(state),
  ];

  const hasHighlights = highlightedZones.length > 0;
  const zones: BoardZoneView[] = BOARD_ZONES.map((zone) => {
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
  };
}


