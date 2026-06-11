import { useMemo } from "react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import type { PendingActionDraft } from "@/features/board/store/use-board-interaction-store";
import type { ActionType, CommandResponse, GameState, PreviewActionResponse } from "@/types/game";

interface PendingActionPanelProps {
  draft?: PendingActionDraft;
  state: GameState;
  isPreviewing: boolean;
  isSubmitting: boolean;
  lastPreviewResult?: PreviewActionResponse;
  lastCommandResult?: CommandResponse;
  onCancel: () => void;
  onPatchParameters: (patch: Record<string, unknown>) => void;
  onSetStep: (step: number) => void;
  onPreview: (actionType: ActionType, parameters: Record<string, unknown>) => void;
  onSubmit: (actionType: ActionType, parameters: Record<string, unknown>) => void;
}

interface PurchaseDraft {
  supplierType: "CAPITALIST" | "MIDDLE_CLASS" | "STATE" | "EXTERNAL_MARKET";
  supplierPlayerId: string;
  quantity: number;
  unitPriceOverride?: number;
}

interface AssignmentDraft {
  workerId: string;
  targetType: "ENTERPRISE_SLOT" | "UNION" | "UNEMPLOYED";
  targetId: string;
}

type SupplierType = "CAPITALIST" | "MIDDLE_CLASS" | "STATE" | "EXTERNAL_MARKET";

interface SupplierPurchaseOption {
  key: string;
  supplierType: SupplierType;
  supplierPlayerId?: string;
  label: string;
  available: number;
  unitPrice: number;
}

const ACTION_LABEL: Partial<Record<ActionType, string>> = {
  ADVANCE_TO_VOTING: "Перейти к голосованию",
  ADVANCE_TO_PRODUCTION: "Перейти к производству",
  RESOLVE_PRODUCTION_PHASE: "Разрешить производство",
  ADVANCE_TO_SCORING: "Перейти к подсчету очков",
  RESOLVE_SCORING_PHASE: "Разрешить подсчет очков",
  ADVANCE_TO_NEXT_ROUND: "Перейти к следующему раунду",
  RESOLVE_PREPARATION_PHASE: "Разрешить подготовку",
  ADVANCE_GAME_FLOW: "Продвинуть ход игры",
  ADVANCE_ROUND: "Продвинуть раунд",
  DECLARE_VOTE_STANCE: "Объявить позицию по голосованию",
  DRAW_VOTING_CUBES: "Достать кубики из мешка",
  COMMIT_VOTE_INFLUENCE: "Вложить влияние",
  PROPOSE_BILL: "Предложить закон",
  ADD_VOTING_CUBES: "Добавить кубики в мешок",
  CALL_EXTRAORDINARY_VOTE: "Внеочередное голосование",
  ASSIGN_WORKERS: "Назначить рабочих",
  PLACE_STRIKES: "Забастовка",
  PLACE_DEMONSTRATION: "Демонстрация",
  BUY_GOODS_AND_SERVICES: "Купить товары и услуги",
  CONSUME_HEALTHCARE: "Потребить ресурс",
  CONSUME_EDUCATION: "Потребить ресурс",
  CONSUME_LUXURY: "Потребить ресурс",
  REFRESH_BUSINESS_DEALS: "Обновить сделки",
  START_TURN: "Начать ход",
  HIRE_WORKER: "Нанять рабочего",
  PRODUCE_GOODS: "Произвести товары",
  SELL_GOODS: "Продать товары",
  ADJUST_POLICY: "Изменить политику",
  PLAY_CARD: "Ручная корректировка",
  END_TURN: "Завершить ход",
};

const CONSUME_ACTION_TYPES: ActionType[] = ["CONSUME_LUXURY", "CONSUME_EDUCATION", "CONSUME_HEALTHCARE"];
const EDUCATION_TARGET_COLORS = [
  { value: "WHITE", label: "Белый" },
  { value: "GREEN", label: "Зеленый" },
  { value: "BLUE", label: "Синий" },
  { value: "ORANGE", label: "Оранжевый" },
  { value: "PURPLE", label: "Фиолетовый" },
];

function isConsumeActionType(actionType?: ActionType): actionType is "CONSUME_HEALTHCARE" | "CONSUME_EDUCATION" | "CONSUME_LUXURY" {
  return actionType === "CONSUME_HEALTHCARE" || actionType === "CONSUME_EDUCATION" || actionType === "CONSUME_LUXURY";
}

function consumeActionLabel(actionType: ActionType): string {
  if (actionType === "CONSUME_LUXURY") {
    return "Предметы роскоши";
  }
  if (actionType === "CONSUME_EDUCATION") {
    return "Образовательные услуги";
  }
  return "Медицинские услуги";
}

function consumeResourceStorageKey(actionType: ActionType): string {
  if (actionType === "CONSUME_LUXURY") {
    return "luxury";
  }
  if (actionType === "CONSUME_EDUCATION") {
    return "education";
  }
  return "healthcare";
}

function toInt(value: unknown, fallback = 0): number {
  if (typeof value === "number" && Number.isFinite(value)) {
    return Math.trunc(value);
  }
  const parsed = Number.parseInt(String(value ?? ""), 10);
  return Number.isFinite(parsed) ? parsed : fallback;
}

function toString(value: unknown, fallback = ""): string {
  if (value === null || value === undefined) {
    return fallback;
  }
  return String(value);
}

const CLASS_LABEL: Record<string, string> = {
  WORKER: "Рабочий класс",
  MIDDLE_CLASS: "Средний класс",
  CAPITALIST: "Капиталисты",
  STATE: "Государство",
};

const POLICY_LABEL: Record<string, string> = {
  POLICY_1_FISCAL: "Фискальная политика",
  POLICY_2_LABOR_MARKET: "Политика на рынке труда",
  POLICY_3_TAXATION: "Налоговая политика",
  POLICY_4_HEALTHCARE_AND_BENEFITS: "Здравоохранение и льготы",
  POLICY_5_EDUCATION: "Образование",
  POLICY_6_FOREIGN_TRADE: "Внешняя торговля",
  POLICY_7_IMMIGRATION: "Миграционная политика",
};

const VALIDATION_ERROR_LABEL: Record<string, string> = {
  NO_PENDING_PROPOSAL_FOR_POLICY: "по этой политике сейчас нет внесённого законопроекта.",
  NOT_CURRENT_PLAYER: "это действие может выполнить только текущий игрок.",
  NOT_IN_VOTING_PHASE: "это действие доступно только во время голосования.",
  INFLUENCE_EXCEEDS_AVAILABLE: "недостаточно личного влияния.",
  NOT_CURRENT_VOTING_STAGE: "сейчас другой этап голосования.",
  CANNOT_RESOLVE_BEFORE_ALL_STANCES: "сначала все игроки должны объявить позицию.",
  CANNOT_RESOLVE_BEFORE_ALL_INFLUENCE_COMMITS: "сначала все игроки должны подтвердить влияние.",
  INFLUENCE_ALREADY_COMMITTED: "этот игрок уже подтвердил влияние.",
  PROPOSER_CANNOT_VOTE_AGAINST: "автор законопроекта не может голосовать против него.",
  INVALID_STANCE: "позиция должна быть «за» или «против».",
  INVALID_POLICY: "такой политики нет.",
  NOT_IN_ACTIONS_PHASE: "это действие доступно только в фазе действий.",
  STANCE_ALREADY_SUBMITTED: "этот игрок уже объявил позицию.",
  UNSUPPORTED_ACTION: "действие сейчас недоступно по правилам.",
};

function publicValidationError(error: string): string {
  const codes = [...error.matchAll(/[A-Z][A-Z0-9_]+/g)].map((match) => match[0]);
  const translated = codes.map((code) => VALIDATION_ERROR_LABEL[code]).filter((line): line is string => Boolean(line));
  if (translated.length > 0) {
    return `Действие невозможно: ${translated.join(" ")}`;
  }
  if (/Policy has no pending proposal/i.test(error)) {
    return "Действие невозможно: по этой политике сейчас нет внесённого законопроекта.";
  }
  if (/Stance is already submitted/i.test(error)) {
    return "Действие невозможно: этот игрок уже объявил позицию.";
  }
  return error;
}

function playerLabel(player: GameState["players"][number]): string {
  const classLabel = CLASS_LABEL[player.classType] ?? player.classType;
  return `${classLabel} (${player.playerId})`;
}

function proposalOwnerPlayerId(policy: GameState["policies"][number], state: GameState): string | undefined {
  const token = policy.occupyingProposalToken;
  if (!token) {
    return undefined;
  }
  if (token.ownerPlayerId) {
    return token.ownerPlayerId;
  }
  return state.players.find((player) => player.classType === token.ownerClass)?.playerId;
}

function storageAmount(storage: Record<string, number> | undefined, resourceId: string): number {
  return toInt(storage?.[resourceId] ?? storage?.[resourceId.toUpperCase()], 0);
}

function toRecord(value: unknown): Record<string, unknown> {
  if (!value || typeof value !== "object" || Array.isArray(value)) {
    return {};
  }
  return value as Record<string, unknown>;
}

function normalizePurchases(raw: unknown): PurchaseDraft[] {
  if (!Array.isArray(raw)) {
    return [];
  }
  return raw
    .map((item) => {
      const row = toRecord(item);
      const supplierType = toString(row.supplierType, "CAPITALIST").toUpperCase() as PurchaseDraft["supplierType"];
      const safeSupplierType: PurchaseDraft["supplierType"] =
        supplierType === "CAPITALIST" || supplierType === "MIDDLE_CLASS" || supplierType === "STATE" || supplierType === "EXTERNAL_MARKET"
          ? supplierType
          : "CAPITALIST";
      return {
        supplierType: safeSupplierType,
        supplierPlayerId: toString(row.supplierPlayerId),
        quantity: Math.max(0, toInt(row.quantity, 0)),
      };
    })
    .filter((item) => item.quantity > 0 || item.supplierPlayerId.length > 0);
}

function normalizeAssignments(raw: unknown): AssignmentDraft[] {
  if (!Array.isArray(raw)) {
    return [];
  }
  return raw
    .map((item) => {
      const row = toRecord(item);
      return {
        workerId: toString(row.workerId),
        targetType: toString(row.targetType, "ENTERPRISE_SLOT") === "UNION"
          ? "UNION"
          : toString(row.targetType, "ENTERPRISE_SLOT") === "UNEMPLOYED"
            ? "UNEMPLOYED"
            : "ENTERPRISE_SLOT",
        targetId: toString(row.targetId),
      } as AssignmentDraft;
    })
    .filter((item) => item.workerId.length > 0 && item.targetId.length > 0);
}

function normalizeQuantityMap(raw: unknown): Record<string, number> {
  if (!raw || typeof raw !== "object" || Array.isArray(raw)) {
    return {};
  }
  return Object.entries(raw as Record<string, unknown>).reduce<Record<string, number>>((acc, [key, value]) => {
    const amount = Math.max(0, toInt(value, 0));
    if (amount > 0) {
      acc[key] = amount;
    }
    return acc;
  }, {});
}

function normalizeNonnegativeNumberMap(raw: unknown): Record<string, number> {
  if (!raw || typeof raw !== "object" || Array.isArray(raw)) {
    return {};
  }
  return Object.entries(raw as Record<string, unknown>).reduce<Record<string, number>>((acc, [key, value]) => {
    acc[key] = Math.max(0, toInt(value, 0));
    return acc;
  }, {});
}

function resourceStorageKey(resourceType: string): string {
  const upper = resourceType.toUpperCase();
  if (upper === "FOOD") {
    return "food";
  }
  if (upper === "HEALTHCARE") {
    return "healthcare";
  }
  if (upper === "EDUCATION") {
    return "education";
  }
  if (upper === "LUXURY") {
    return "luxury";
  }
  return resourceType.toLowerCase();
}

function stateServiceUnitPrice(state: GameState, resourceType: string): number {
  const policyId = resourceType === "HEALTHCARE" ? "POLICY_4_HEALTHCARE_AND_BENEFITS" : "POLICY_5_EDUCATION";
  const course = state.policies.find((policy) => policy.id === policyId)?.currentCourse ?? "B";
  if (course === "A") {
    return 0;
  }
  if (course === "B") {
    return 5;
  }
  if (course === "C") {
    return 10;
  }
  return 5;
}

function externalMarketUnitPrice(state: GameState, resourceType: string): number {
  const basePrice = resourceType === "FOOD" ? 10 : 6;
  const course = state.policies.find((policy) => policy.id === "POLICY_6_FOREIGN_TRADE")?.currentCourse ?? "B";
  if (course === "A") {
    return resourceType === "FOOD" ? basePrice + 10 : basePrice + 6;
  }
  if (course === "B") {
    return resourceType === "FOOD" ? basePrice + 5 : basePrice + 3;
  }
  return basePrice;
}

function resolveSupplierOptions(
  state: GameState,
  actor: GameState["players"][number] | undefined,
  resourceType: string,
): SupplierPurchaseOption[] {
  if (!actor || (actor.classType !== "WORKER" && actor.classType !== "MIDDLE_CLASS")) {
    return [];
  }
  const key = resourceStorageKey(resourceType);
  const populationCap = Math.max(0, actor.population);
  const capitalist = state.players.find((player) => player.classType === "CAPITALIST");
  const middleClass = state.players.find((player) => player.classType === "MIDDLE_CLASS");
  const options: SupplierPurchaseOption[] = [];

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
    const unitPrice = stateServiceUnitPrice(state, resourceType);
    if (available > 0 && unitPrice >= 0) {
      options.push({
        key: "STATE:_",
        supplierType: "STATE",
        label: "Государство",
        available,
        unitPrice,
      });
    }
  }

  if (resourceType === "FOOD" || resourceType === "LUXURY") {
    const unitPrice = externalMarketUnitPrice(state, resourceType);
    if (populationCap > 0 && unitPrice > 0) {
      options.push({
        key: "EXTERNAL_MARKET:_",
        supplierType: "EXTERNAL_MARKET",
        label: "Импорт",
        available: populationCap,
        unitPrice,
      });
    }
  }

  return options;
}

function collectSelectedWorkerIds(parameters: Record<string, unknown>): string[] {
  const fromMulti = Array.isArray(parameters.selectedWorkerIds)
    ? parameters.selectedWorkerIds.map((value) => String(value)).filter((value) => value.length > 0)
    : [];
  if (fromMulti.length > 0) {
    return Array.from(new Set(fromMulti));
  }
  const single = toString(parameters.selectedWorkerId);
  return single ? [single] : [];
}

function workerMatchesSlot(
  worker: GameState["workers"][number],
  slot: GameState["enterprises"][number]["slots"][number],
): boolean {
  if (slot.requiredQualification === "UNSKILLED") {
    return true;
  }
  if (worker.qualificationType !== "SKILLED") {
    return false;
  }
  if (!slot.requiredSector) {
    return true;
  }
  return worker.sector === slot.requiredSector;
}

function firstCompatibleSlotId(
  worker: GameState["workers"][number],
  enterprise: GameState["enterprises"][number],
  usedSlotIds: Set<string> = new Set(),
): string | undefined {
  return enterprise.slots.find((slot) => {
    if (usedSlotIds.has(slot.id)) {
      return false;
    }
    if (slot.occupiedWorkerId && slot.occupiedWorkerId !== worker.id) {
      return false;
    }
    return workerMatchesSlot(worker, slot);
  })?.id;
}

function canAssignWorkersToEnterprise(
  workers: GameState["workers"],
  enterprise: GameState["enterprises"][number],
): boolean {
  if (workers.length === 0) {
    return enterprise.slots.some((slot) => !slot.occupiedWorkerId);
  }
  const selectedWorkerIds = new Set(workers.map((worker) => worker.id));
  const usedSlotIds = new Set<string>(
    enterprise.slots
      .filter((slot) => Boolean(slot.occupiedWorkerId) && !selectedWorkerIds.has(String(slot.occupiedWorkerId)))
      .map((slot) => slot.id),
  );
  for (const worker of workers) {
    const slotId = firstCompatibleSlotId(worker, enterprise, usedSlotIds);
    if (!slotId) {
      return false;
    }
    usedSlotIds.add(slotId);
  }
  return true;
}

function stringifyUnknown(value: unknown): string {
  if (value === null || value === undefined) {
    return "";
  }
  if (typeof value === "string") {
    return value;
  }
  try {
    return JSON.stringify(value);
  } catch {
    return String(value);
  }
}

const ENTITY_SHORT_LABELS: Record<string, string> = {
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

function compactEntityLabel(raw: string): string {
  if (!raw) {
    return raw;
  }
  return ENTITY_SHORT_LABELS[raw] ?? raw.replace(/[_-]+/g, " ");
}

function workerNo(workerId: string): string {
  const match = workerId.match(/(\d+)$/);
  return match ? match[1] : workerId;
}

function workerLabel(worker: GameState["workers"][number]): string {
  const no = workerNo(worker.id);
  const color = worker.sector ? String(worker.sector).toLowerCase() : "серый";
  return `Рабочий #${no} (${color})`;
}

function assignmentTargetLabel(targetId: string): string {
  if (targetId === "unemployed") {
    return "клетка безработных";
  }
  const [enterpriseId, slotId] = String(targetId).split(":");
  if (!slotId) {
    return compactEntityLabel(enterpriseId);
  }
  const slotNoMatch = slotId.match(/(\d+)$/);
  const slotNo = slotNoMatch ? slotNoMatch[1] : slotId;
  return `${compactEntityLabel(enterpriseId)} • слот ${slotNo}`;
}

function NumericAdjuster({
  label,
  value,
  onChange,
  step = 1,
  min = -999,
  max = 999,
}: {
  label: string;
  value: number;
  onChange: (nextValue: number) => void;
  step?: number;
  min?: number;
  max?: number;
}) {
  const clamped = Math.min(max, Math.max(min, value));
  return (
    <div className="w-full min-w-0 max-w-full space-y-2 rounded-lg border border-zinc-700/70 bg-black/20 p-3">
      <p className="break-words text-xs leading-snug text-zinc-300">{label}</p>
      <div className="grid w-full min-w-0 grid-cols-[2.25rem_minmax(4.25rem,1fr)_2.25rem] items-center gap-2">
        <Button type="button" variant="outline" size="sm" className="h-9 min-w-0 px-0" onClick={() => onChange(Math.max(min, clamped - step))}>
          -
        </Button>
        <Input
          type="number"
          className="h-9 min-w-0 text-center tabular-nums"
          min={min}
          max={max}
          value={clamped}
          onChange={(event) => onChange(Math.min(max, Math.max(min, toInt(event.target.value, clamped))))}
        />
        <Button type="button" variant="outline" size="sm" className="h-9 min-w-0 px-0" onClick={() => onChange(Math.min(max, clamped + step))}>
          +
        </Button>
      </div>
    </div>
  );
}

function VotingCubeCounter({
  label,
  available,
  value,
  onChange,
  maxValue,
  metaLabel,
}: {
  label: string;
  available: number;
  value: number;
  onChange: (nextValue: number) => void;
  maxValue?: number;
  metaLabel?: string;
}) {
  const max = Math.max(0, maxValue ?? available);
  const clamped = Math.min(max, Math.max(0, value));
  const setNext = (nextValue: number) => onChange(Math.min(max, Math.max(0, nextValue)));
  return (
    <div className="min-w-0 rounded-md border border-zinc-700/60 bg-black/25 p-2.5">
      <div className="mb-2 flex min-w-0 items-baseline justify-between gap-2">
        <p className="min-w-0 text-xs font-medium text-zinc-200">{label}</p>
        <p className="shrink-0 text-[11px] text-zinc-500">{metaLabel ?? `в мешке ${available}`}</p>
      </div>
      <div className="grid grid-cols-[2rem_minmax(2.75rem,1fr)_2rem] items-center gap-2">
        <Button
          type="button"
          variant="outline"
          size="sm"
          className="h-8 px-0"
          onClick={() => setNext(clamped - 1)}
          disabled={clamped <= 0}
          aria-label={`Уменьшить: ${label}`}
        >
          -
        </Button>
        <div className="flex h-8 items-center justify-center rounded-md border border-zinc-700/70 bg-black/35 text-sm font-semibold tabular-nums text-zinc-50">
          {clamped}
        </div>
        <Button
          type="button"
          variant="outline"
          size="sm"
          className="h-8 px-0"
          onClick={() => setNext(clamped + 1)}
          disabled={clamped >= max}
          aria-label={`Увеличить: ${label}`}
        >
          +
        </Button>
      </div>
    </div>
  );
}

function PartyButtonPicker({
  label,
  parties,
  selectedId,
  onSelect,
}: {
  label: string;
  parties: Array<{ id: string; label: string }>;
  selectedId: string;
  onSelect: (partyId: string) => void;
}) {
  return (
    <div className="min-w-0 space-y-2">
      <p className="text-xs text-zinc-400">{label}</p>
      <div className="grid min-w-0 grid-cols-2 gap-2">
        {parties.map((party) => (
          <Button
            key={`${label}-${party.id}`}
            type="button"
            variant={selectedId === party.id ? "secondary" : "outline"}
            size="sm"
            className="min-w-0 justify-start px-2 text-left text-xs"
            onClick={() => onSelect(party.id)}
          >
            <span className="truncate">{party.label}</span>
          </Button>
        ))}
      </div>
    </div>
  );
}

function WorkerCountStepper({
  value,
  onChange,
  min = 0,
  max = 5,
}: {
  value: number;
  onChange: (nextValue: number) => void;
  min?: number;
  max?: number;
}) {
  const clamped = Math.min(max, Math.max(min, value));
  const setNext = (nextValue: number) => onChange(Math.min(max, Math.max(min, nextValue)));
  return (
    <div className="space-y-2">
      <p className="text-xs text-zinc-300">Сколько добавить работников</p>
      <div className="grid grid-cols-[2.5rem_minmax(3rem,1fr)_2.5rem] items-center gap-2">
        <Button type="button" variant="outline" size="sm" onClick={() => setNext(clamped - 1)} disabled={clamped <= min} aria-label="Уменьшить количество работников">
          -
        </Button>
        <div className="flex h-10 items-center justify-center rounded-xl border border-zinc-700/80 bg-black/35 text-base font-semibold tabular-nums text-zinc-50">
          {clamped}
        </div>
        <Button type="button" variant="outline" size="sm" onClick={() => setNext(clamped + 1)} disabled={clamped >= max} aria-label="Увеличить количество работников">
          +
        </Button>
      </div>
    </div>
  );
}

const MANUAL_RESOURCE_OPTIONS = [
  { id: "FOOD", label: "Еда" },
  { id: "HEALTHCARE", label: "Медицина" },
  { id: "EDUCATION", label: "Образование" },
  { id: "LUXURY", label: "Роскошь" },
  { id: "INFLUENCE", label: "Влияние" },
] as const;

type ManualResourceType = (typeof MANUAL_RESOURCE_OPTIONS)[number]["id"];

const STATE_ENTERPRISE_RESOURCE_BLOCKS: Array<{
  enterpriseIdPrefix: string;
  enterpriseLabel: string;
  resourceType: ManualResourceType;
  resourceLabel: string;
  storageKeys: string[];
}> = [
  {
    enterpriseIdPrefix: "state_hospital",
    enterpriseLabel: "Гос. больницы",
    resourceType: "HEALTHCARE",
    resourceLabel: "Медицина",
    storageKeys: ["healthcare"],
  },
  {
    enterpriseIdPrefix: "state_university",
    enterpriseLabel: "Гос. университеты",
    resourceType: "EDUCATION",
    resourceLabel: "Образование",
    storageKeys: ["education"],
  },
  {
    enterpriseIdPrefix: "state_media",
    enterpriseLabel: "Гос. медиа",
    resourceType: "INFLUENCE",
    resourceLabel: "Влияние",
    storageKeys: ["media_influence", "influence", "media"],
  },
];

function stateStorageAmount(state: GameState, keys: string[]): number {
  for (const key of keys) {
    const value = Math.max(0, toInt(state.publicServicesStorage?.[key], 0));
    if (value > 0) {
      return value;
    }
  }
  return 0;
}

function manualResourceParamKey(source: "STATE" | "CAPITALIST", resourceType: ManualResourceType): string {
  return source === "STATE" ? `manualStateResource_${resourceType}` : `manualCapitalistResource_${resourceType}`;
}

function collectManualResourceInstructions(
  parameters: Record<string, unknown>,
  source: "STATE" | "CAPITALIST",
): string[] {
  const instructionPrefix = source === "STATE" ? "state_to_actor_resource" : "capitalist_to_actor_resource";
  return MANUAL_RESOURCE_OPTIONS.flatMap((resource) => {
    const amount = Math.max(0, toInt(parameters[manualResourceParamKey(source, resource.id)], 0));
    if (amount <= 0) {
      return [];
    }
    return [`${instructionPrefix}:${resource.id}:${amount}`];
  });
}

function manualExchangeMoneyPartyId(partyId: string): string {
  return partyId === "STATE" ? "TREASURY" : partyId;
}

function manualExchangeResourcePartyId(partyId: string): string {
  return partyId === "STATE" ? "STATE_SERVICES" : partyId;
}

function manualVictoryPointParamKey(playerId: string): string {
  return `manualVictoryPointsDelta_${playerId}`;
}

function pushManualExchangeInstructions(
  instructions: string[],
  fromPartyId: string,
  toPartyId: string,
  moneyAmount: number,
  resourceType: string,
  resourceAmount: number,
) {
  if (!fromPartyId || !toPartyId || fromPartyId === toPartyId) {
    return;
  }
  if (moneyAmount > 0) {
    instructions.push(`manual_money_transfer:${manualExchangeMoneyPartyId(fromPartyId)}:${manualExchangeMoneyPartyId(toPartyId)}:${moneyAmount}`);
  }
  if (resourceAmount > 0) {
    instructions.push(`manual_resource_transfer:${manualExchangeResourcePartyId(fromPartyId)}:${manualExchangeResourcePartyId(toPartyId)}:${resourceType}:${resourceAmount}`);
  }
}

function buildPlayCardPayload(parameters: Record<string, unknown>, state: GameState): Record<string, unknown> {
  const stateToActor = Math.max(0, toInt(parameters.manualStateToActorMoney, 0));
  const capitalistToActor = Math.max(0, toInt(parameters.manualCapitalistToActorMoney, 0));
  const actorToState = Math.max(0, toInt(parameters.manualActorToStateMoney, 0));
  const sourcePlayerId = toString(parameters.manualMoneySourcePlayerId);
  const sourceToActor = Math.max(0, toInt(parameters.manualSourceToActorMoney, 0));
  const targetPlayerId = toString(parameters.manualMoneyTargetPlayerId);
  const actorToPlayer = Math.max(0, toInt(parameters.manualActorToPlayerMoney, 0));
  const actorMoneyDelta = toInt(parameters.manualActorMoneyDelta, 0);
  const treasuryDelta = toInt(parameters.manualTreasuryDelta, 0);
  const welfareDelta = toInt(parameters.manualWelfareDelta, 0);
  const workerColor = toString(parameters.manualWorkersColor, "GRAY").toUpperCase();
  const workerCount = Math.max(0, toInt(parameters.manualWorkersCount, 0));
  const transferMoneySource = toString(parameters.manualTransferMoneySourceId);
  const transferMoneyTarget = toString(parameters.manualTransferMoneyTargetId);
  const transferMoneyAmount = Math.max(0, toInt(parameters.manualTransferMoneyAmount, 0));
  const takeResourceSource = toString(parameters.manualTakeResourceSourceId);
  const takeResourceType = toString(parameters.manualTakeResourceType, "FOOD").toUpperCase();
  const takeResourceAmount = Math.max(0, toInt(parameters.manualTakeResourceAmount, 0));
  const giveResourceTarget = toString(parameters.manualGiveResourceTargetId);
  const giveResourceType = toString(parameters.manualGiveResourceType, "FOOD").toUpperCase();
  const giveResourceAmount = Math.max(0, toInt(parameters.manualGiveResourceAmount, 0));
  const exchangeLeftId = toString(parameters.manualExchangeLeftId, "STATE");
  const exchangeRightId = toString(parameters.manualExchangeRightId, toString(parameters.actorPlayerId));
  const leftToRightMoney = Math.max(0, toInt(parameters.manualExchangeLeftToRightMoney, 0));
  const rightToLeftMoney = Math.max(0, toInt(parameters.manualExchangeRightToLeftMoney, 0));
  const leftToRightResourceType = toString(parameters.manualExchangeLeftToRightResourceType, "FOOD").toUpperCase();
  const rightToLeftResourceType = toString(parameters.manualExchangeRightToLeftResourceType, "FOOD").toUpperCase();
  const leftToRightResourceAmount = Math.max(0, toInt(parameters.manualExchangeLeftToRightResourceAmount, 0));
  const rightToLeftResourceAmount = Math.max(0, toInt(parameters.manualExchangeRightToLeftResourceAmount, 0));
  const votePolicyId = toString(parameters.manualVotePolicyId);
  const voteReturnDrawn = Boolean(parameters.manualVoteReturnDrawn);
  const discardCubeOwner = toString(parameters.manualDiscardCubeOwner, "WORKER").toUpperCase();
  const discardCubeCount = Math.max(0, toInt(parameters.manualDiscardCubeCount, 0));
  const discardWorkerCubes = Math.max(0, toInt(parameters.manualDiscardWorkerCubes, 0));
  const discardMiddleCubes = Math.max(0, toInt(parameters.manualDiscardMiddleClassCubes, 0));
  const discardCapitalistCubes = Math.max(0, toInt(parameters.manualDiscardCapitalistCubes, 0));
  const exactDiscardCubes = discardWorkerCubes + discardMiddleCubes + discardCapitalistCubes;
  const addWorkerCubes = Math.max(0, toInt(parameters.manualAddWorkerCubes, 0));
  const addMiddleCubes = Math.max(0, toInt(parameters.manualAddMiddleClassCubes, 0));
  const addCapitalistCubes = Math.max(0, toInt(parameters.manualAddCapitalistCubes, 0));
  const exactAddCubes = addWorkerCubes + addMiddleCubes + addCapitalistCubes;
  const manualProposalTargetCourse = toString(parameters.manualProposalTargetCourse);
  const voteDrawCount = Math.max(0, toInt(parameters.manualVoteDrawCount, 0));
  const exactWorkerCubes = Math.max(0, toInt(parameters.manualVoteWorkerCubes, 0));
  const exactMiddleCubes = Math.max(0, toInt(parameters.manualVoteMiddleClassCubes, 0));
  const exactCapitalistCubes = Math.max(0, toInt(parameters.manualVoteCapitalistCubes, 0));
  const exactVoteCubes = exactWorkerCubes + exactMiddleCubes + exactCapitalistCubes;

  const instructions: string[] = [];
  if (stateToActor > 0) {
    instructions.push(`state_to_actor_money:${stateToActor}`);
  }
  if (capitalistToActor > 0) {
    instructions.push(`capitalist_to_actor_money:${capitalistToActor}`);
  }
  if (actorToState > 0) {
    instructions.push(`actor_to_state_money:${actorToState}`);
  }
  if (sourceToActor > 0 && sourcePlayerId.length > 0) {
    instructions.push(`player_to_actor_money:${sourcePlayerId}:${sourceToActor}`);
  }
  if (actorToPlayer > 0 && targetPlayerId.length > 0) {
    instructions.push(`actor_to_player_money:${targetPlayerId}:${actorToPlayer}`);
  }
  if (actorMoneyDelta !== 0) {
    instructions.push(`actor_money_delta:${actorMoneyDelta}`);
  }
  if (treasuryDelta !== 0) {
    instructions.push(`treasury_delta:${treasuryDelta}`);
  }
  if (welfareDelta !== 0) {
    instructions.push(`actor_welfare_delta:${welfareDelta}`);
  }
  if (workerCount > 0) {
    instructions.push(`add_workers_color:${workerColor}:${workerCount}`);
  }
  if (transferMoneyAmount > 0 && transferMoneySource.length > 0 && transferMoneyTarget.length > 0) {
    instructions.push(`manual_money_transfer:${transferMoneySource}:${transferMoneyTarget}:${transferMoneyAmount}`);
  }
  if (takeResourceAmount > 0 && takeResourceSource.length > 0) {
    instructions.push(`manual_resource_transfer:${takeResourceSource}:${toString(parameters.actorPlayerId)}:${takeResourceType}:${takeResourceAmount}`);
  }
  if (giveResourceAmount > 0 && giveResourceTarget.length > 0) {
    instructions.push(`manual_resource_transfer:${toString(parameters.actorPlayerId)}:${giveResourceTarget}:${giveResourceType}:${giveResourceAmount}`);
  }
  pushManualExchangeInstructions(
    instructions,
    exchangeLeftId,
    exchangeRightId,
    leftToRightMoney,
    leftToRightResourceType,
    leftToRightResourceAmount,
  );
  pushManualExchangeInstructions(
    instructions,
    exchangeRightId,
    exchangeLeftId,
    rightToLeftMoney,
    rightToLeftResourceType,
    rightToLeftResourceAmount,
  );
  for (const player of state.players) {
    const delta = toInt(parameters[manualVictoryPointParamKey(player.playerId)], 0);
    if (delta !== 0) {
      instructions.push(`manual_victory_points_delta:${player.playerId}:${delta}`);
    }
  }
  if (Array.isArray(parameters.manualRemoveUnemployedWorkerIds)) {
    for (const workerId of parameters.manualRemoveUnemployedWorkerIds.map(String).filter((value) => value.length > 0)) {
      instructions.push(`manual_remove_unemployed_worker:${workerId}`);
    }
  }
  if (voteReturnDrawn) {
    instructions.push("manual_vote_return_drawn");
  }
  if (exactAddCubes > 0) {
    instructions.push(`manual_voting_bag_add_exact:${addWorkerCubes}:${addMiddleCubes}:${addCapitalistCubes}`);
  }
  if (exactDiscardCubes > 0) {
    instructions.push(`manual_voting_bag_discard_exact:${discardWorkerCubes}:${discardMiddleCubes}:${discardCapitalistCubes}`);
  }
  if (exactDiscardCubes === 0 && discardCubeCount > 0) {
    instructions.push(`manual_voting_bag_discard:${discardCubeOwner}:${discardCubeCount}`);
  }
  if (votePolicyId.length > 0 && ["A", "B", "C"].includes(manualProposalTargetCourse)) {
    instructions.push(`manual_policy_target:${votePolicyId}:${manualProposalTargetCourse}`);
  }
  if (votePolicyId.length > 0 && exactVoteCubes > 0) {
    instructions.push(`manual_extraordinary_vote_exact:${votePolicyId}:${exactWorkerCubes}:${exactMiddleCubes}:${exactCapitalistCubes}`);
  } else if (votePolicyId.length > 0 && voteDrawCount > 0) {
    instructions.push(`manual_extraordinary_vote_draw:${votePolicyId}:${voteDrawCount}`);
  }
  return { cardId: `manual:${instructions.join(";")}` };
}

function buildPayload(draft: PendingActionDraft, state: GameState): Record<string, unknown> {
  const parameters = draft.parameters ?? {};
  switch (draft.actionType) {
    case "PROPOSE_BILL":
      return {
        actorPlayerId: toString(parameters.actorPlayerId),
        policyId: toString(parameters.policyId),
        targetCourse: toString(parameters.targetCourse, "B"),
      };
    case "ADD_VOTING_CUBES":
      return {
        actorPlayerId: toString(parameters.actorPlayerId),
      };
    case "CALL_EXTRAORDINARY_VOTE":
      {
        const actorPlayerId = toString(parameters.actorPlayerId);
        const fallbackPolicyId = state.policies.find(
          (policy) => policy.occupyingProposalToken && proposalOwnerPlayerId(policy, state) === actorPlayerId,
        )?.id;
        return {
          actorPlayerId,
          policyId: toString(parameters.policyId, fallbackPolicyId ?? ""),
        };
      }
    case "ASSIGN_WORKERS":
      return {
        actorPlayerId: toString(parameters.actorPlayerId),
        assignments: Array.isArray(parameters.assignments) ? parameters.assignments : [],
      };
    case "PLACE_STRIKES":
      return {
        actorPlayerId: toString(parameters.actorPlayerId),
        enterpriseIds: Array.isArray(parameters.enterpriseIds) ? parameters.enterpriseIds : [],
      };
    case "PLACE_DEMONSTRATION":
      return {
        actorPlayerId: toString(parameters.actorPlayerId),
        penaltyAllocation: normalizeNonnegativeNumberMap(parameters.penaltyAllocation),
      };
    case "RESOLVE_PRODUCTION_PHASE": {
      const actorPlayerId = toString(parameters.actorPlayerId);
      const worker = state.players.find((player) => player.classType === "WORKER");
      const quantityBySupplier = normalizeQuantityMap(parameters.workerFoodQuantityBySupplier);
      const priceBySupplier = normalizeNonnegativeNumberMap(parameters.workerFoodPriceBySupplier);
      const supplierOptions = resolveSupplierOptions(state, worker, "FOOD");
      const workerFoodPurchases = supplierOptions
        .map((option) => {
          const requested = Math.max(0, toInt(quantityBySupplier[option.key], 0));
          const maxAllowed = Math.min(Math.max(0, worker?.population ?? 0), option.available);
          const quantity = Math.min(requested, maxAllowed);
          return {
            supplierType: option.supplierType,
            supplierPlayerId: option.supplierPlayerId || undefined,
            quantity,
            unitPriceOverride: priceBySupplier[option.key] ?? option.unitPrice,
          };
        })
        .filter((item) => item.quantity > 0);
      return {
        actorPlayerId,
        workerFoodPurchases,
      };
    }
    case "BUY_GOODS_AND_SERVICES": {
      const actorPlayerId = toString(parameters.actorPlayerId);
      const actor = state.players.find((player) => player.playerId === actorPlayerId);
      const resourceType = toString(parameters.resourceType, "FOOD").toUpperCase();
      const quantityBySupplier = normalizeQuantityMap(parameters.buyQuantityBySupplier);
      const priceBySupplier = normalizeNonnegativeNumberMap(parameters.buyPriceBySupplier);
      const supplierOptions = resolveSupplierOptions(state, actor, resourceType);
      const purchasesFromPlan = supplierOptions
        .map((option) => {
          const requested = Math.max(0, toInt(quantityBySupplier[option.key], 0));
          const maxAllowed = Math.min(Math.max(0, actor?.population ?? 0), option.available);
          const quantity = Math.min(requested, maxAllowed);
          return {
            supplierType: option.supplierType,
            supplierPlayerId: option.supplierPlayerId || undefined,
            quantity,
            unitPriceOverride: priceBySupplier[option.key] ?? option.unitPrice,
          };
          })
          .filter((item) => item.quantity > 0);
      return {
        actorPlayerId,
        resourceType,
        purchases: purchasesFromPlan,
      };
    }
    case "DECLARE_VOTE_STANCE":
      return {
        actorPlayerId: toString(parameters.actorPlayerId),
        policyId: toString(parameters.policyId, state.currentVoteState?.activeProposalPolicyId ?? ""),
        stance: toString(parameters.stance, "FOR"),
      };
    case "DRAW_VOTING_CUBES":
      return {
        actorPlayerId: toString(parameters.actorPlayerId),
        count: Math.max(1, toInt(parameters.count, 1)),
      };
    case "CONSUME_HEALTHCARE":
    case "CONSUME_EDUCATION":
    case "CONSUME_LUXURY":
      return {
        actorPlayerId: toString(parameters.actorPlayerId),
        workerId: toString(parameters.workerId),
        targetColor: toString(parameters.targetColor, "WHITE"),
      };
    case "COMMIT_VOTE_INFLUENCE":
      return {
        actorPlayerId: toString(parameters.actorPlayerId),
        influenceAmount: Math.max(0, toInt(parameters.influenceAmount, 0)),
      };
    case "HIRE_WORKER":
      return { count: Math.max(0, toInt(parameters.count, 1)) };
    case "PRODUCE_GOODS":
    case "SELL_GOODS":
      return { amount: Math.max(0, toInt(parameters.amount, 1)) };
    case "ADJUST_POLICY":
      return {
        track: toString(parameters.track, "TAXATION"),
        delta: toInt(parameters.delta, 1),
      };
    case "PLAY_CARD":
      return buildPlayCardPayload(parameters, state);
    default:
      return parameters;
  }
}

function ManualPlayCardControls({
  parameters,
  state,
  actor,
  onPatch,
}: {
  parameters: Record<string, unknown>;
  state: GameState;
  actor?: GameState["players"][number];
  onPatch: (patch: Record<string, unknown>) => void;
}) {
  const capitalist = state.players.find((player) => player.classType === "CAPITALIST");
  const actorMoneyDelta = toInt(parameters.manualActorMoneyDelta, 0);
  const treasuryDelta = toInt(parameters.manualTreasuryDelta, 0);
  const welfareDelta = toInt(parameters.manualWelfareDelta, 0);
  const workersCount = Math.max(0, toInt(parameters.manualWorkersCount, 0));
  const workersColor = toString(parameters.manualWorkersColor, "GRAY").toUpperCase();
  const manualSection = toString(parameters.manualSection, "resources");
  const exchangeParties = [
    { id: "STATE", label: "Государство" },
    ...state.players.map((player) => ({ id: player.playerId, label: playerLabel(player) })),
  ];
  const exchangeLeftId = toString(parameters.manualExchangeLeftId, "STATE");
  const exchangeRightId = toString(parameters.manualExchangeRightId, actor?.playerId ?? state.players[0]?.playerId ?? "");
  const exchangeLeftLabel = exchangeParties.find((party) => party.id === exchangeLeftId)?.label ?? "Слева";
  const exchangeRightLabel = exchangeParties.find((party) => party.id === exchangeRightId)?.label ?? "Справа";
  const pendingVotePolicies = state.policies.filter((policy) => policy.occupyingProposalToken);
  const manualVotePolicyId = toString(parameters.manualVotePolicyId, pendingVotePolicies[0]?.id ?? "");
  const selectedManualVotePolicy = pendingVotePolicies.find((policy) => policy.id === manualVotePolicyId);
  const manualVoteDrawCount = Math.max(0, toInt(parameters.manualVoteDrawCount, 0));
  const manualRemoveUnemployedWorkerIds = Array.isArray(parameters.manualRemoveUnemployedWorkerIds)
    ? parameters.manualRemoveUnemployedWorkerIds.map(String)
    : [];
  const unemployedWorkers = state.workers.filter((worker) => worker.location === "UNEMPLOYED");

  return (
    <div className="w-full min-w-0 max-w-full space-y-3 overflow-hidden rounded-xl border border-emerald-500/40 bg-emerald-500/10 p-3">
      <p className="text-xs text-emerald-200">Ручная настройка: применится только для игрока-человека.</p>
      <p className="text-xs text-zinc-300">
        Балансы: игрок {actor?.classType === "CAPITALIST" ? actor.revenue : actor?.money ?? 0} | казна {state.treasury} | капиталист{" "}
        {capitalist?.revenue ?? 0}
      </p>
      <div className="grid min-w-0 grid-cols-2 gap-2 rounded-lg border border-zinc-700/70 bg-black/20 p-1">
        <Button type="button" variant={manualSection === "resources" ? "secondary" : "ghost"} size="sm" className="min-w-0 whitespace-normal px-2 leading-tight" onClick={() => onPatch({ manualSection: "resources" })}>
          Производство и ресурсы
        </Button>
        <Button type="button" variant={manualSection === "voting" ? "secondary" : "ghost"} size="sm" className="min-w-0 whitespace-normal px-2 leading-tight" onClick={() => onPatch({ manualSection: "voting" })}>
          Голосование
        </Button>
      </div>

      {manualSection === "resources" && (
        <>
      <div className="w-full min-w-0 max-w-full space-y-3 overflow-hidden rounded-lg border border-zinc-700/70 bg-black/20 p-3">
        <div className="grid min-w-0 grid-cols-1 gap-3">
          <PartyButtonPicker
            label="Сторона слева"
            parties={exchangeParties}
            selectedId={exchangeLeftId}
            onSelect={(partyId) => onPatch({ manualExchangeLeftId: partyId })}
          />
          <div className="flex justify-center">
            <Button
              type="button"
              variant="outline"
              size="sm"
              className="h-9 w-full max-w-[8rem] px-3"
              onClick={() => onPatch({ manualExchangeLeftId: exchangeRightId, manualExchangeRightId: exchangeLeftId })}
              aria-label="Поменять стороны обмена"
            >
              ⇄
            </Button>
          </div>
          <PartyButtonPicker
            label="Сторона справа"
            parties={exchangeParties}
            selectedId={exchangeRightId}
            onSelect={(partyId) => onPatch({ manualExchangeRightId: partyId })}
          />
        </div>

        <div className="grid min-w-0 grid-cols-1 gap-3">
          <div className="w-full min-w-0 max-w-full space-y-3 overflow-hidden rounded-md border border-zinc-700/60 bg-black/25 p-3">
            <p className="break-words text-xs font-semibold leading-snug text-zinc-200">{exchangeLeftLabel} → {exchangeRightLabel}</p>
            <NumericAdjuster label="Деньги" value={Math.max(0, toInt(parameters.manualExchangeLeftToRightMoney, 0))} min={0} max={999} onChange={(value) => onPatch({ manualExchangeLeftToRightMoney: value })} />
            <div className="grid min-w-0 grid-cols-1 gap-2">
              <div className="min-w-0 space-y-2">
                <p className="text-xs text-zinc-400">Ресурс</p>
                <Select value={toString(parameters.manualExchangeLeftToRightResourceType, "FOOD")} onChange={(event) => onPatch({ manualExchangeLeftToRightResourceType: event.target.value })}>
                  {MANUAL_RESOURCE_OPTIONS.map((resource) => (
                    <option key={`left-to-right-resource-${resource.id}`} value={resource.id}>
                      {resource.label}
                    </option>
                  ))}
                </Select>
              </div>
              <NumericAdjuster label="Кол-во" value={Math.max(0, toInt(parameters.manualExchangeLeftToRightResourceAmount, 0))} min={0} max={999} onChange={(value) => onPatch({ manualExchangeLeftToRightResourceAmount: value })} />
            </div>
          </div>

          <div className="w-full min-w-0 max-w-full space-y-3 overflow-hidden rounded-md border border-zinc-700/60 bg-black/25 p-3">
            <p className="break-words text-xs font-semibold leading-snug text-zinc-200">{exchangeRightLabel} → {exchangeLeftLabel}</p>
            <NumericAdjuster label="Деньги" value={Math.max(0, toInt(parameters.manualExchangeRightToLeftMoney, 0))} min={0} max={999} onChange={(value) => onPatch({ manualExchangeRightToLeftMoney: value })} />
            <div className="grid min-w-0 grid-cols-1 gap-2">
              <div className="min-w-0 space-y-2">
                <p className="text-xs text-zinc-400">Ресурс</p>
                <Select value={toString(parameters.manualExchangeRightToLeftResourceType, "FOOD")} onChange={(event) => onPatch({ manualExchangeRightToLeftResourceType: event.target.value })}>
                  {MANUAL_RESOURCE_OPTIONS.map((resource) => (
                    <option key={`right-to-left-resource-${resource.id}`} value={resource.id}>
                      {resource.label}
                    </option>
                  ))}
                </Select>
              </div>
              <NumericAdjuster label="Кол-во" value={Math.max(0, toInt(parameters.manualExchangeRightToLeftResourceAmount, 0))} min={0} max={999} onChange={(value) => onPatch({ manualExchangeRightToLeftResourceAmount: value })} />
            </div>
          </div>
        </div>
      </div>

      <div className="w-full min-w-0 max-w-full space-y-3 overflow-hidden rounded-lg border border-zinc-700/70 bg-black/20 p-3">
        <p className="text-xs font-semibold text-zinc-200">Очки победы</p>
        {state.players.map((player) => (
          <NumericAdjuster
            key={`manual-vp-${player.playerId}`}
            label={`${playerLabel(player)}: сейчас ${player.victoryPoints}`}
            value={toInt(parameters[manualVictoryPointParamKey(player.playerId)], 0)}
            min={-999}
            max={999}
            onChange={(value) => onPatch({ [manualVictoryPointParamKey(player.playerId)]: value })}
          />
        ))}
      </div>

      <div className="space-y-3 rounded-lg border border-zinc-700/70 bg-black/20 p-3">
        <p className="text-xs font-semibold text-zinc-200">Убрать рабочих с безработицы</p>
        {unemployedWorkers.length === 0 && <p className="text-xs text-zinc-500">На клетке безработных нет рабочих.</p>}
        {unemployedWorkers.length > 0 && (
          <div className="flex flex-wrap gap-2">
            {unemployedWorkers.map((worker) => {
              const selected = manualRemoveUnemployedWorkerIds.includes(worker.id);
              const nextIds = selected
                ? manualRemoveUnemployedWorkerIds.filter((workerId) => workerId !== worker.id)
                : [...manualRemoveUnemployedWorkerIds, worker.id];
              return (
                <Button
                  key={`manual-remove-unemployed-${worker.id}`}
                  type="button"
                  variant={selected ? "secondary" : "outline"}
                  size="sm"
                  className="px-2 text-xs"
                  onClick={() => onPatch({ manualRemoveUnemployedWorkerIds: nextIds })}
                >
                  {workerLabel(worker)}
                </Button>
              );
            })}
          </div>
        )}
      </div>

      <NumericAdjuster label="Изменение денег актера (+/-)" value={actorMoneyDelta} min={-999} max={999} onChange={(value) => onPatch({ manualActorMoneyDelta: value })} />
      <NumericAdjuster label="Изменение казны (+/-)" value={treasuryDelta} min={-999} max={999} onChange={(value) => onPatch({ manualTreasuryDelta: value })} />
      <NumericAdjuster label="Изменение благосостояния (+/-)" value={welfareDelta} min={-10} max={10} onChange={(value) => onPatch({ manualWelfareDelta: value })} />

      <div className="grid gap-3 rounded-lg border border-zinc-700/70 bg-black/20 p-3 sm:grid-cols-2">
        <div className="space-y-2">
          <p className="text-xs text-zinc-300">Цвет нового рабочего</p>
          <Select value={workersColor} onChange={(event) => onPatch({ manualWorkersColor: event.target.value })}>
            <option value="GRAY">Серый (без квалификации)</option>
            <option value="GREEN">Зеленый</option>
            <option value="BLUE">Синий</option>
            <option value="WHITE">Белый</option>
            <option value="ORANGE">Оранжевый</option>
            <option value="PURPLE">Фиолетовый</option>
          </Select>
        </div>
        <WorkerCountStepper value={workersCount} min={0} max={5} onChange={(value) => onPatch({ manualWorkersCount: value })} />
      </div>
        </>
      )}

      {manualSection === "voting" && (
        <div className="space-y-3 rounded-lg border border-zinc-700/70 bg-black/20 p-3">
          <div className="rounded-lg border border-zinc-700/60 bg-black/30 p-3 text-xs text-zinc-300">
            В мешке сейчас: рабочие {state.votingBag.worker ?? 0}, средний класс {state.votingBag.middleClass ?? 0}, капиталисты {state.votingBag.capitalist ?? 0}.
            Предпросмотр достанет кубики без сохранения; “Подтвердить” примет результат и запустит внеочередное голосование.
          </div>
          <div className="space-y-2">
            <p className="text-xs font-semibold text-zinc-200">Добавить в мешок</p>
            <VotingCubeCounter
              label="Рабочие"
              available={state.votingBag.worker ?? 0}
              maxValue={99}
              metaLabel={`сейчас ${state.votingBag.worker ?? 0}`}
              value={Math.max(0, toInt(parameters.manualAddWorkerCubes, 0))}
              onChange={(value) => onPatch({ manualAddWorkerCubes: value })}
            />
            <VotingCubeCounter
              label="Средний класс"
              available={state.votingBag.middleClass ?? 0}
              maxValue={99}
              metaLabel={`сейчас ${state.votingBag.middleClass ?? 0}`}
              value={Math.max(0, toInt(parameters.manualAddMiddleClassCubes, 0))}
              onChange={(value) => onPatch({ manualAddMiddleClassCubes: value })}
            />
            <VotingCubeCounter
              label="Капиталисты"
              available={state.votingBag.capitalist ?? 0}
              maxValue={99}
              metaLabel={`сейчас ${state.votingBag.capitalist ?? 0}`}
              value={Math.max(0, toInt(parameters.manualAddCapitalistCubes, 0))}
              onChange={(value) => onPatch({ manualAddCapitalistCubes: value })}
            />
          </div>
          <div className="grid gap-3 sm:grid-cols-[1fr,160px]">
            <div className="space-y-2">
              <p className="text-xs text-zinc-400">Законопроект</p>
              <Select value={manualVotePolicyId} onChange={(event) => onPatch({ manualVotePolicyId: event.target.value })}>
                {pendingVotePolicies.map((policy) => (
                  <option key={`manual-vote-${policy.id}`} value={policy.id}>
                    {POLICY_LABEL[policy.id] ?? policy.id}
                  </option>
                ))}
              </Select>
              {pendingVotePolicies.length === 0 && <p className="text-xs text-rose-300">Нет внесенных законопроектов для внеочередного голосования.</p>}
            </div>
            <NumericAdjuster label="Случайно достать" value={manualVoteDrawCount} min={0} max={999} onChange={(value) => onPatch({ manualVoteDrawCount: value })} />
          </div>
          <div className="space-y-3">
            <div className="space-y-2">
              <p className="text-xs text-zinc-400">Подвинуть предложенный курс</p>
              <Select
                value={toString(parameters.manualProposalTargetCourse, selectedManualVotePolicy?.occupyingProposalToken?.targetCourse ?? "")}
                onChange={(event) => onPatch({ manualProposalTargetCourse: event.target.value })}
              >
                <option value="">Не менять</option>
                <option value="A">Курс A</option>
                <option value="B">Курс B</option>
                <option value="C">Курс C</option>
              </Select>
            </div>
            <div className="space-y-2">
              <p className="text-xs font-semibold text-zinc-200">Выкинуть из мешка</p>
              <VotingCubeCounter
                label="Рабочие"
                available={state.votingBag.worker ?? 0}
                value={Math.max(0, toInt(parameters.manualDiscardWorkerCubes, 0))}
                onChange={(value) => onPatch({ manualDiscardWorkerCubes: value })}
              />
              <VotingCubeCounter
                label="Средний класс"
                available={state.votingBag.middleClass ?? 0}
                value={Math.max(0, toInt(parameters.manualDiscardMiddleClassCubes, 0))}
                onChange={(value) => onPatch({ manualDiscardMiddleClassCubes: value })}
              />
              <VotingCubeCounter
                label="Капиталисты"
                available={state.votingBag.capitalist ?? 0}
                value={Math.max(0, toInt(parameters.manualDiscardCapitalistCubes, 0))}
                onChange={(value) => onPatch({ manualDiscardCapitalistCubes: value })}
              />
            </div>
          </div>
          <p className="text-xs font-semibold text-zinc-200">Достать из мешка вручную</p>
          <div className="space-y-2">
            <VotingCubeCounter
              label="Рабочие"
              available={state.votingBag.worker ?? 0}
              value={Math.max(0, toInt(parameters.manualVoteWorkerCubes, 0))}
              onChange={(value) => onPatch({ manualVoteWorkerCubes: value })}
            />
            <VotingCubeCounter
              label="Средний класс"
              available={state.votingBag.middleClass ?? 0}
              value={Math.max(0, toInt(parameters.manualVoteMiddleClassCubes, 0))}
              onChange={(value) => onPatch({ manualVoteMiddleClassCubes: value })}
            />
            <VotingCubeCounter
              label="Капиталисты"
              available={state.votingBag.capitalist ?? 0}
              value={Math.max(0, toInt(parameters.manualVoteCapitalistCubes, 0))}
              onChange={(value) => onPatch({ manualVoteCapitalistCubes: value })}
            />
          </div>
          <p className="text-xs text-zinc-400">
            Эти кубики будут вынуты из мешка точным составом вместо случайной вытяжки. При необходимости включите возврат уже вытянутых кубиков перед новой ручной вытяжкой.
          </p>
          <Button
            type="button"
            variant={parameters.manualVoteReturnDrawn ? "secondary" : "outline"}
            size="sm"
            onClick={() => onPatch({ manualVoteReturnDrawn: !parameters.manualVoteReturnDrawn })}
          >
            {parameters.manualVoteReturnDrawn ? "Кубики будут возвращены" : "Вернуть уже вытянутые кубики в мешок"}
          </Button>
        </div>
      )}
    </div>
  );
}

export function PendingActionPanel({
  draft,
  state,
  isPreviewing,
  isSubmitting,
  lastPreviewResult,
  lastCommandResult,
  onCancel,
  onPatchParameters,
  onSetStep,
  onPreview,
  onSubmit,
}: PendingActionPanelProps) {
  const actionType = draft?.actionType;
  const parameters = draft?.parameters ?? {};
  const voteSession = state.currentVoteState;
  const actorPlayerId = toString(parameters.actorPlayerId);
  const actor = useMemo(
    () =>
      state.players.find((player) => player.playerId === actorPlayerId) ??
      state.players[state.turnOrder.currentPlayerIndex] ??
      state.players[0],
    [actorPlayerId, state.players, state.turnOrder.currentPlayerIndex],
  );
  const voteActor = useMemo(
    () => state.players.find((player) => player.playerId === actorPlayerId) ?? actor,
    [actor, actorPlayerId, state.players],
  );
  const stanceSubmittedPlayers = new Set(Object.keys(voteSession?.stanceByPlayer ?? {}));
  const influenceCommittedPlayers = new Set(Object.keys(voteSession?.influenceCommitments ?? {}));
  const votingBagTotal = Math.max(
    0,
    (state.votingBag.worker ?? 0) + (state.votingBag.middleClass ?? 0) + (state.votingBag.capitalist ?? 0),
  );
  const stancePlayerOptions = voteSession
    ? state.players.filter((player) => !stanceSubmittedPlayers.has(player.playerId))
    : state.players;
  const influencePlayerOptions = voteSession
    ? state.players.filter(
        (player) =>
          Object.prototype.hasOwnProperty.call(voteSession.stanceByPlayer ?? {}, player.playerId) &&
          !influenceCommittedPlayers.has(player.playerId),
      )
    : state.players;
  const extraordinaryProposalOptions = state.policies.filter(
    (policy) => policy.occupyingProposalToken && proposalOwnerPlayerId(policy, state) === actorPlayerId,
  );
  const capitalistEnterpriseMarket = Array.isArray(state.capitalistEnterpriseMarket) ? state.capitalistEnterpriseMarket : [];

  const buyResourceType = toString(parameters.resourceType, "FOOD").toUpperCase();
  const workerPlayer = state.players.find((player) => player.classType === "WORKER");
  const isProductionResolve = actionType === "RESOLVE_PRODUCTION_PHASE";
  const buyQuantityBySupplier = actionType === "BUY_GOODS_AND_SERVICES" ? normalizeQuantityMap(parameters.buyQuantityBySupplier) : {};
  const buyPriceBySupplier = actionType === "BUY_GOODS_AND_SERVICES" ? normalizeNonnegativeNumberMap(parameters.buyPriceBySupplier) : {};
  const workerFoodQuantityBySupplier = isProductionResolve ? normalizeQuantityMap(parameters.workerFoodQuantityBySupplier) : {};
  const workerFoodPriceBySupplier = isProductionResolve ? normalizeNonnegativeNumberMap(parameters.workerFoodPriceBySupplier) : {};
  const supplierOptions = useMemo(
    () => (actionType === "BUY_GOODS_AND_SERVICES" ? resolveSupplierOptions(state, actor, buyResourceType) : []),
    [actionType, state, actor, buyResourceType],
  );
  const workerFoodSupplierOptions = useMemo(
    () => (isProductionResolve ? resolveSupplierOptions(state, workerPlayer, "FOOD") : []),
    [isProductionResolve, state, workerPlayer],
  );
  const buyPlan = useMemo(
    () =>
      supplierOptions
        .map((option) => {
          const maxAllowed = Math.min(Math.max(0, actor?.population ?? 0), option.available);
          const quantity = Math.min(maxAllowed, Math.max(0, toInt(buyQuantityBySupplier[option.key], 0)));
          const unitPrice = buyPriceBySupplier[option.key] ?? option.unitPrice;
          return { option, quantity, unitPrice, cost: quantity * unitPrice };
        })
        .filter((item) => item.quantity > 0),
    [supplierOptions, actor?.population, buyQuantityBySupplier, buyPriceBySupplier],
  );
  const buyTotalCost = useMemo(() => buyPlan.reduce((sum, item) => sum + item.cost, 0), [buyPlan]);
  const workerFoodPlan = useMemo(
    () =>
      workerFoodSupplierOptions
        .map((option) => {
          const maxAllowed = Math.min(Math.max(0, workerPlayer?.population ?? 0), option.available);
          const quantity = Math.min(maxAllowed, Math.max(0, toInt(workerFoodQuantityBySupplier[option.key], 0)));
          const unitPrice = workerFoodPriceBySupplier[option.key] ?? option.unitPrice;
          return { option, quantity, unitPrice, cost: quantity * unitPrice };
        })
        .filter((item) => item.quantity > 0),
    [workerFoodSupplierOptions, workerPlayer?.population, workerFoodQuantityBySupplier, workerFoodPriceBySupplier],
  );
  const workerFoodTotalCost = useMemo(() => workerFoodPlan.reduce((sum, item) => sum + item.cost, 0), [workerFoodPlan]);
  const selectedWorkerIds = actionType === "ASSIGN_WORKERS" ? collectSelectedWorkerIds(parameters) : [];
  const selectedWorkers = useMemo(
    () =>
      selectedWorkerIds
        .map((workerId) => state.workers.find((worker) => worker.id === workerId))
        .filter((worker): worker is GameState["workers"][number] => Boolean(worker)),
    [selectedWorkerIds, state.workers],
  );
  const compatibleEnterpriseIds = useMemo(
    () =>
      state.enterprises
        .filter((enterprise) => canAssignWorkersToEnterprise(selectedWorkers, enterprise))
        .map((enterprise) => enterprise.id),
    [selectedWorkers, state.enterprises],
  );
  const currentAssignments = actionType === "ASSIGN_WORKERS" ? normalizeAssignments(parameters.assignments) : [];
  const consumeAvailableActionTypes = isConsumeActionType(actionType)
    ? (Array.isArray(parameters.consumeAvailableActionTypes) ? parameters.consumeAvailableActionTypes : [])
        .map((value) => String(value))
        .filter((value): value is ActionType => CONSUME_ACTION_TYPES.includes(value as ActionType))
    : [];
  const consumeSelectableActionTypes =
    consumeAvailableActionTypes.length > 0
      ? consumeAvailableActionTypes
      : isConsumeActionType(actionType)
        ? [actionType]
        : [];
  const selectedConsumeActionType = isConsumeActionType(actionType)
    ? ((): ActionType => {
        const raw = String(parameters.consumeSelectedActionType ?? "");
        if (consumeSelectableActionTypes.includes(raw as ActionType)) {
          return raw as ActionType;
        }
        return consumeSelectableActionTypes[0] ?? actionType;
      })()
    : undefined;
  const consumeRequiredAmount = Math.max(0, actor?.population ?? 0);
  const consumeAvailableAmount =
    selectedConsumeActionType && actor
      ? Math.max(0, storageAmount(actor.goodsAndServicesArea ?? actor.resources, consumeResourceStorageKey(selectedConsumeActionType)))
      : 0;
  const consumeEnoughResource = consumeAvailableAmount >= consumeRequiredAmount;
  const educationTrainableWorkers = useMemo(
    () =>
      actor && selectedConsumeActionType === "CONSUME_EDUCATION"
        ? state.workers.filter((worker) => worker.classType === actor.classType && worker.qualificationType === "UNSKILLED")
        : [],
    [actor, selectedConsumeActionType, state.workers],
  );
  const selectedEducationWorkerId =
    selectedConsumeActionType === "CONSUME_EDUCATION"
      ? toString(parameters.workerId, educationTrainableWorkers[0]?.id ?? "")
      : "";
  const submitActionType = isConsumeActionType(actionType)
    ? selectedConsumeActionType ?? actionType
    : actionType;
  const setSupplierQuantity = (supplierKey: string, value: number) => {
    const option = supplierOptions.find((item) => item.key === supplierKey);
    const maxAllowed = option ? Math.min(Math.max(0, actor?.population ?? 0), option.available) : 0;
    const clamped = Math.max(0, Math.min(maxAllowed, value));
    const next = { ...buyQuantityBySupplier };
    if (clamped > 0) {
      next[supplierKey] = clamped;
    } else {
      delete next[supplierKey];
    }
    onPatchParameters({ buyQuantityBySupplier: next });
  };
  const setWorkerFoodQuantity = (supplierKey: string, value: number) => {
    const option = workerFoodSupplierOptions.find((item) => item.key === supplierKey);
    const maxAllowed = option ? Math.min(Math.max(0, workerPlayer?.population ?? 0), option.available) : 0;
    const clamped = Math.max(0, Math.min(maxAllowed, value));
    const next = { ...workerFoodQuantityBySupplier };
    if (clamped > 0) {
      next[supplierKey] = clamped;
    } else {
      delete next[supplierKey];
    }
    onPatchParameters({ workerFoodQuantityBySupplier: next });
  };

  const removeSelectedWorker = (workerId: string) => {
    const nextIds = selectedWorkerIds.filter((id) => id !== workerId);
    onPatchParameters({
      selectedWorkerIds: nextIds,
      selectedWorkerId: nextIds[0] ?? "",
    });
  };

  if (!draft) {
    return null;
  }

  const payload = buildPayload(draft, state);
  const actionLabel = ACTION_LABEL[draft.actionType] ?? draft.actionType;
  const step = Math.max(1, toInt(draft.step, 1));
  const effectiveSubmitActionType: ActionType = submitActionType ?? draft.actionType;

  return (
    <Card className="border-[#9b7338]/65 bg-[linear-gradient(145deg,rgba(18,27,26,0.96),rgba(8,12,12,0.98))]">
      <CardHeader className="border-b border-[#9b7338]/35 px-3 pb-2 pt-3">
        <div className="flex items-center justify-between gap-2">
          <CardTitle className="text-sm uppercase text-[#d8b56b]">Ожидающее действие</CardTitle>
          <Badge tone="warning">{actionLabel}</Badge>
        </div>
      </CardHeader>
      <CardContent className="space-y-3 p-3">
        <div className="flex items-center justify-between rounded-md border border-[#8f6b35]/55 bg-black/25 p-2 text-xs text-zinc-300">
          <span>Шаг сценария</span>
          <div className="flex items-center gap-2">
            <Button type="button" variant="ghost" size="sm" onClick={() => onSetStep(Math.max(1, step - 1))}>
              Назад
            </Button>
            <span>{step}</span>
            <Button type="button" variant="ghost" size="sm" onClick={() => onSetStep(Math.min(5, step + 1))}>
              Далее
            </Button>
          </div>
        </div>

        <div className="space-y-2">
          <p className="text-xs uppercase tracking-wide text-zinc-400">Игрок</p>
          <Select value={toString(parameters.actorPlayerId, actor?.playerId ?? "")} onChange={(event) => onPatchParameters({ actorPlayerId: event.target.value })}>
            {state.players.map((player) => (
              <option key={player.playerId} value={player.playerId}>
                {player.playerId} ({player.classType}, {player.controlMode})
              </option>
            ))}
          </Select>
        </div>

        {actionType === "PROPOSE_BILL" && (
          <div className="grid gap-3 sm:grid-cols-2">
            <div className="space-y-2">
              <p className="text-xs text-zinc-400">Политика</p>
              <Select value={toString(parameters.policyId)} onChange={(event) => onPatchParameters({ policyId: event.target.value })}>
                {state.policies.map((policy) => (
                  <option key={policy.id} value={policy.id}>
                    {policy.id} (текущий курс: {policy.currentCourse})
                  </option>
                ))}
              </Select>
            </div>
            <div className="space-y-2">
              <p className="text-xs text-zinc-400">Целевой курс</p>
              <Select value={toString(parameters.targetCourse, "B")} onChange={(event) => onPatchParameters({ targetCourse: event.target.value })}>
                <option value="A">A</option>
                <option value="B">B</option>
                <option value="C">C</option>
              </Select>
            </div>
          </div>
        )}

        {actionType === "ADD_VOTING_CUBES" && (
          <div className="rounded-lg border border-zinc-700/60 bg-black/30 p-3 text-sm text-zinc-300">
            Сейчас в мешке: W{state.votingBag.worker ?? 0}/M{state.votingBag.middleClass ?? 0}/C{state.votingBag.capitalist ?? 0}. После действия добавится 3 кубика активного игрока.
          </div>
        )}

        {actionType === "CALL_EXTRAORDINARY_VOTE" && (
          <div className="space-y-3">
            <div className="space-y-2">
              <p className="text-xs text-zinc-400">Политика</p>
              <Select
                value={toString(parameters.policyId, extraordinaryProposalOptions[0]?.id ?? "")}
                onChange={(event) => onPatchParameters({ policyId: event.target.value })}
              >
                {extraordinaryProposalOptions
                  .map((policy) => (
                    <option key={policy.id} value={policy.id}>
                      {POLICY_LABEL[policy.id] ?? policy.id}: курс {policy.currentCourse} → {policy.occupyingProposalToken?.targetCourse ?? policy.currentCourse}
                    </option>
                  ))}
              </Select>
            </div>
            <div className="rounded-lg border border-zinc-700/60 bg-black/30 p-3 text-sm text-zinc-300">
              Стоимость: 1 влияние. Движок достанет 5 кубиков; при равенстве выигрывает автор предложения.
            </div>
            {extraordinaryProposalOptions.length === 0 && (
              <p className="text-xs text-rose-300">У выбранного игрока нет своего законопроекта, по которому можно сразу объявить голосование.</p>
            )}
          </div>
        )}

        {actionType === "ASSIGN_WORKERS" && (
          <div className="space-y-3 rounded-lg border border-zinc-700/70 bg-black/20 p-3">
            <p className="text-sm text-zinc-300">
              1) Кликните рабочих на поле. 2) Кликните предприятие. Назначение произойдет автоматически, подходящие слоты подбираются по цвету.
            </p>
            <div className="space-y-2">
              <p className="text-xs uppercase tracking-wide text-zinc-400">Выбранные рабочие</p>
              {selectedWorkers.length === 0 && <p className="text-xs text-zinc-500">Пока никто не выбран.</p>}
              {selectedWorkers.length > 0 && (
                <div className="flex flex-wrap gap-2">
                  {selectedWorkers.map((worker) => (
                    <button
                      key={worker.id}
                      type="button"
                      onClick={() => removeSelectedWorker(worker.id)}
                      className="rounded-full border border-sky-500/40 bg-sky-500/10 px-2 py-1 text-xs text-sky-100"
                    >
                      {workerLabel(worker)} x
                    </button>
                  ))}
                </div>
              )}
            </div>
            <div className="space-y-2">
              <p className="text-xs uppercase tracking-wide text-zinc-400">Куда можно назначить текущий набор</p>
              {compatibleEnterpriseIds.length === 0 && (
                <p className="text-xs text-rose-300">Нет доступных предприятий для выбранного набора рабочих.</p>
              )}
              {compatibleEnterpriseIds.length > 0 && (
                <div className="flex flex-wrap gap-2">
                  {compatibleEnterpriseIds.map((enterpriseId) => (
                    <span
                      key={enterpriseId}
                      className="rounded-full border border-emerald-500/40 bg-emerald-500/10 px-2 py-1 text-xs text-emerald-200"
                    >
                      {compactEntityLabel(enterpriseId)}
                    </span>
                  ))}
                </div>
              )}
            </div>
            {currentAssignments.length > 0 && (
              <div className="space-y-1">
                <p className="text-xs uppercase tracking-wide text-zinc-400">Текущие назначения</p>
                {currentAssignments.map((assignment, index) => (
                  <p key={`${assignment.workerId}-${assignment.targetId}-${index}`} className="text-xs text-zinc-300">
                    Рабочий #{workerNo(assignment.workerId)} {"->"} {assignmentTargetLabel(assignment.targetId)}
                  </p>
                ))}
              </div>
            )}
          </div>
        )}

        {actionType === "PLACE_STRIKES" && (
          <div className="space-y-3 rounded-lg border border-amber-500/35 bg-amber-500/10 p-3">
            <p className="text-sm text-zinc-200">
              Выберите предприятия, где заняты рабочие Рабочего класса, нет трудового договора и зарплата ниже 3 уровня.
            </p>
            <div className="grid gap-2">
              {state.enterprises
                .filter((enterprise) => {
                  const hasWorker = enterprise.slots.some((slot) => {
                    const worker = state.workers.find((candidate) => candidate.id === slot.occupiedWorkerId);
                    return worker?.classType === "WORKER" && !worker.tiedContract;
                  });
                  return hasWorker && enterprise.wageLevel < 3 && !(enterprise as typeof enterprise & { strikeToken?: boolean }).strikeToken;
                })
                .map((enterprise) => {
                  const selected = Array.isArray(parameters.enterpriseIds)
                    ? parameters.enterpriseIds.map(String).includes(enterprise.id)
                    : false;
                  const currentIds = Array.isArray(parameters.enterpriseIds) ? parameters.enterpriseIds.map(String) : [];
                  return (
                    <button
                      key={enterprise.id}
                      type="button"
                      onClick={() => {
                        const next = selected
                          ? currentIds.filter((id) => id !== enterprise.id)
                          : [...currentIds, enterprise.id];
                        onPatchParameters({ enterpriseIds: next });
                      }}
                      className={`rounded-md border px-3 py-2 text-left text-sm transition ${
                        selected
                          ? "border-amber-300/75 bg-amber-400/20 text-amber-100"
                          : "border-zinc-700/70 bg-black/25 text-zinc-300 hover:border-amber-400/50"
                      }`}
                    >
                      {compactEntityLabel(enterprise.id)} • зарплата L{enterprise.wageLevel}
                    </button>
                  );
                })}
            </div>
          </div>
        )}

        {actionType === "PLACE_DEMONSTRATION" && (
          <div className="space-y-3 rounded-lg border border-sky-500/35 bg-sky-500/10 p-3">
            <p className="text-sm text-zinc-200">
              Жетон демонстрации ставится на клетку безработных. Если он доживет до производства, Рабочий класс получит влияние, а соперники потеряют ПО.
            </p>
            <div className="grid gap-2">
              {state.players
                .filter((player) => player.classType !== "WORKER")
                .map((player) => {
                  const allocation = normalizeNonnegativeNumberMap(parameters.penaltyAllocation);
                  return (
                    <NumericAdjuster
                      key={player.playerId}
                      label={`Штраф ПО: ${playerLabel(player)}`}
                      value={allocation[player.playerId] ?? 0}
                      min={0}
                      max={20}
                      onChange={(value) =>
                        onPatchParameters({
                          penaltyAllocation: {
                            ...allocation,
                            [player.playerId]: value,
                          },
                        })
                      }
                    />
                  );
                })}
            </div>
          </div>
        )}

        {actionType === "RESOLVE_PRODUCTION_PHASE" && workerPlayer && (
          <div className="space-y-3 rounded-xl border border-emerald-500/35 bg-emerald-500/10 p-3">
            <div>
              <p className="text-sm font-semibold text-emerald-100">Покупка еды для Рабочего класса</p>
              <p className="text-xs text-zinc-400">
                Население: {workerPlayer.population}. Если указать план, движок купит еду именно у выбранных поставщиков.
              </p>
            </div>
            <div className="rounded-lg border border-zinc-700/60 bg-black/30 p-3">
              <p className="text-xs uppercase tracking-wide text-zinc-400">Итого по выбранному плану</p>
              <p className="mt-1 text-lg font-semibold text-emerald-200">{workerFoodTotalCost}</p>
            </div>
            {workerFoodSupplierOptions.map((option) => {
              const maxAllowed = Math.min(Math.max(0, workerPlayer.population), option.available);
              const quantity = Math.min(maxAllowed, Math.max(0, toInt(workerFoodQuantityBySupplier[option.key], 0)));
              return (
                <div key={`worker-food-${option.key}`} className="grid gap-2 rounded-lg border border-zinc-700/60 p-3 sm:grid-cols-[1fr,150px]">
                  <div className="space-y-1">
                    <p className="text-sm text-zinc-100">{option.label}</p>
                    <p className="text-xs text-zinc-400">
                      Доступно: {option.available} | Цена: {option.unitPrice} | Максимум: {maxAllowed}
                    </p>
                  </div>
                  <div className="grid gap-2">
                    <div className="flex items-center gap-2">
                      <Button type="button" variant="outline" size="sm" onClick={() => setWorkerFoodQuantity(option.key, quantity - 1)}>
                        -
                      </Button>
                      <Input
                        type="number"
                        min={0}
                        max={maxAllowed}
                        value={quantity}
                        onChange={(event) => setWorkerFoodQuantity(option.key, toInt(event.target.value, quantity))}
                      />
                      <Button type="button" variant="outline" size="sm" onClick={() => setWorkerFoodQuantity(option.key, quantity + 1)}>
                        +
                      </Button>
                    </div>
                    <Input
                      type="number"
                      min={0}
                      value={workerFoodPriceBySupplier[option.key] ?? option.unitPrice}
                      onChange={(event) =>
                        onPatchParameters({
                          workerFoodPriceBySupplier: {
                            ...workerFoodPriceBySupplier,
                            [option.key]: Math.max(0, toInt(event.target.value, option.unitPrice)),
                          },
                        })
                      }
                    />
                  </div>
                </div>
              );
            })}
            {workerFoodPlan.length > 0 && (
              <div className="rounded-lg border border-zinc-700/60 bg-black/20 p-3">
                <p className="text-xs uppercase tracking-wide text-zinc-400">План кормления</p>
                {workerFoodPlan.map((item) => (
                  <p key={`worker-food-plan-${item.option.key}`} className="text-xs text-zinc-200">
                    Еда: {item.quantity} у {item.option.label} (цена: {item.unitPrice}, стоимость: {item.cost})
                  </p>
                ))}
              </div>
            )}
          </div>
        )}

        {actionType === "BUY_GOODS_AND_SERVICES" && (
          <div className="space-y-3 rounded-xl border border-zinc-700/70 bg-black/20 p-3">
            <div className="space-y-2">
              <p className="text-xs text-zinc-400">Тип товара</p>
              <Select
                value={toString(parameters.resourceType, "FOOD")}
                onChange={(event) =>
                  onPatchParameters({
                    resourceType: event.target.value,
                    buyQuantityBySupplier: {},
                  })
                }
              >
                <option value="FOOD">Еда</option>
                <option value="HEALTHCARE">Здравоохранение</option>
                <option value="EDUCATION">Образование</option>
                <option value="LUXURY">Роскошь</option>
              </Select>
            </div>
            <p className="text-xs text-zinc-500">Выберите количество у доступных поставщиков. Лимит по каждому поставщику: население покупателя и фактический остаток.</p>
            <div className="rounded-lg border border-zinc-700/60 bg-black/30 p-3">
              <p className="text-xs uppercase tracking-wide text-zinc-400">Итоговая стоимость покупки</p>
              <p className="mt-1 text-lg font-semibold text-emerald-200">{buyTotalCost}</p>
            </div>

            {supplierOptions.length === 0 && (
              <p className="text-xs text-rose-300">Нет доступных поставщиков для выбранного товара.</p>
            )}

            {supplierOptions.map((option) => {
              const maxAllowed = Math.min(Math.max(0, actor?.population ?? 0), option.available);
              const quantity = Math.min(maxAllowed, Math.max(0, toInt(buyQuantityBySupplier[option.key], 0)));
              return (
                <div key={option.key} className="grid gap-2 rounded-lg border border-zinc-700/60 p-3 sm:grid-cols-[1fr,150px]">
                  <div className="space-y-1">
                    <p className="text-sm text-zinc-100">{option.label}</p>
                    <p className="text-xs text-zinc-400">
                      Доступно: {option.available} | Базовая цена: {option.unitPrice} | Максимум: {maxAllowed}
                    </p>
                  </div>
                  <div className="grid gap-2">
                    <div className="flex items-center gap-2">
                      <Button type="button" variant="outline" size="sm" onClick={() => setSupplierQuantity(option.key, quantity - 1)}>
                        -
                      </Button>
                      <Input
                        type="number"
                        min={0}
                        max={maxAllowed}
                        value={quantity}
                        onChange={(event) => setSupplierQuantity(option.key, toInt(event.target.value, quantity))}
                      />
                      <Button type="button" variant="outline" size="sm" onClick={() => setSupplierQuantity(option.key, quantity + 1)}>
                        +
                      </Button>
                    </div>
                    <Input
                      type="number"
                      min={0}
                      value={buyPriceBySupplier[option.key] ?? option.unitPrice}
                      onChange={(event) =>
                        onPatchParameters({
                          buyPriceBySupplier: {
                            ...buyPriceBySupplier,
                            [option.key]: Math.max(0, toInt(event.target.value, option.unitPrice)),
                          },
                        })
                      }
                    />
                  </div>
                </div>
              );
            })}

            {buyPlan.length > 0 && (
              <div className="rounded-lg border border-zinc-700/60 bg-black/20 p-3">
                <p className="text-xs uppercase tracking-wide text-zinc-400">Запрос покупки</p>
                {buyPlan.map((item) => (
                    <p key={`buy-plan-${item.option.key}`} className="text-xs text-zinc-200">
                      {toString(parameters.resourceType, "FOOD")}: {item.quantity} у {item.option.label} (цена: {item.unitPrice}, стоимость: {item.cost})
                    </p>
                  ))}
                <p className="mt-2 text-xs font-semibold text-emerald-200">Итого к оплате: {buyTotalCost}</p>
              </div>
            )}
          </div>
        )}

        {isConsumeActionType(actionType) && selectedConsumeActionType && (
          <div className="space-y-3 rounded-xl border border-zinc-700/70 bg-black/20 p-3">
            <div className="space-y-2">
              <p className="text-xs text-zinc-400">Тип потребления</p>
              <Select
                value={selectedConsumeActionType}
                onChange={(event) => onPatchParameters({ consumeSelectedActionType: event.target.value })}
              >
                {consumeSelectableActionTypes.map((consumeActionType) => (
                  <option key={consumeActionType} value={consumeActionType}>
                    {consumeActionLabel(consumeActionType)}
                  </option>
                ))}
              </Select>
            </div>
            <div className="rounded-lg border border-zinc-700/60 bg-black/30 p-3 text-xs text-zinc-300">
              <p>Сейчас будет потреблено: {consumeRequiredAmount}</p>
              <p>Доступно у игрока: {consumeAvailableAmount}</p>
              <p className="mt-1 text-zinc-400">
                В текущем срезе движок списывает фиксированное количество по населению (не произвольное).
              </p>
              {!consumeEnoughResource && (
                <p className="mt-1 text-rose-300">Ресурса недостаточно для выполнения действия.</p>
              )}
            </div>
            {selectedConsumeActionType === "CONSUME_EDUCATION" && (
              <div className="grid gap-3 sm:grid-cols-2">
                <div className="space-y-2">
                  <p className="text-xs text-zinc-400">Кого обучить</p>
                  <Select
                    value={selectedEducationWorkerId}
                    onChange={(event) => onPatchParameters({ workerId: event.target.value })}
                  >
                    {educationTrainableWorkers.map((worker) => (
                      <option key={`education-worker-${worker.id}`} value={worker.id}>
                        {workerLabel(worker)}
                      </option>
                    ))}
                  </Select>
                  {educationTrainableWorkers.length === 0 && (
                    <p className="text-xs text-rose-300">Нет неквалифицированного рабочего для обучения.</p>
                  )}
                </div>
                <div className="space-y-2">
                  <p className="text-xs text-zinc-400">Цвет специалиста</p>
                  <Select
                    value={toString(parameters.targetColor, "WHITE")}
                    onChange={(event) => onPatchParameters({ targetColor: event.target.value })}
                  >
                    {EDUCATION_TARGET_COLORS.map((color) => (
                      <option key={`education-color-${color.value}`} value={color.value}>
                        {color.label}
                      </option>
                    ))}
                  </Select>
                </div>
              </div>
            )}
            <div className="rounded-lg border border-zinc-700/60 bg-black/30 p-3 text-xs text-zinc-300">
              <p className="font-semibold text-zinc-100">Эффекты в текущей версии</p>
              <p>Все типы: +1 благосостояние и ПО по новому уровню.</p>
              <p>Медицина: +2 ПО и новый серый рабочий. Образование: выбранный рабочий становится специалистом выбранного цвета.</p>
            </div>
          </div>
        )}

        {actionType === "DECLARE_VOTE_STANCE" && (
          <div className="grid gap-3 sm:grid-cols-3">
            <Select value={actorPlayerId} onChange={(event) => onPatchParameters({ actorPlayerId: event.target.value })}>
              {(stancePlayerOptions.length > 0 ? stancePlayerOptions : state.players).map((player) => (
                <option key={`stance-${player.playerId}`} value={player.playerId}>
                  {playerLabel(player)}
                </option>
              ))}
            </Select>
            <Select
              value={toString(parameters.policyId, voteSession?.activeProposalPolicyId ?? "")}
              onChange={(event) => onPatchParameters({ policyId: event.target.value })}
            >
              {(voteSession ? state.policies.filter((policy) => policy.id === voteSession.activeProposalPolicyId) : state.policies).map((policy) => (
                <option key={policy.id} value={policy.id}>
                  {POLICY_LABEL[policy.id] ?? policy.id}
                </option>
              ))}
            </Select>
            <Select value={toString(parameters.stance, "FOR")} onChange={(event) => onPatchParameters({ stance: event.target.value })}>
              <option value="FOR">За</option>
              <option value="AGAINST">Против</option>
            </Select>
            {voteSession?.extraordinary && (
              <p className="sm:col-span-3 text-xs text-amber-200">
                Внеочередное голосование: мешок не пополняется, после результата фаза действий продолжится.
              </p>
            )}
            {stancePlayerOptions.length === 0 && (
              <p className="sm:col-span-3 text-xs text-emerald-200">
                Все позиции объявлены. Следующий шаг — «Вложить влияние»: каждый игрок выбирает, сколько личного влияния потратить.
              </p>
            )}
          </div>
        )}

        {actionType === "BUILD_ENTERPRISE" && capitalistEnterpriseMarket.length > 0 && (
          <div className="grid gap-3">
            <div className="space-y-2">
              <p className="text-xs text-zinc-400">Предприятие из рынка капиталиста</p>
              <Select
                value={toString(parameters.enterpriseId, capitalistEnterpriseMarket[0]?.id ?? "")}
                onChange={(event) => {
                  const enterprise = capitalistEnterpriseMarket.find((candidate) => candidate.id === event.target.value);
                  onPatchParameters({ enterpriseId: event.target.value, cost: enterprise?.cost ?? 20 });
                }}
              >
                {capitalistEnterpriseMarket.map((enterprise) => (
                  <option key={enterprise.id} value={enterprise.id}>
                    {(enterprise.name ?? enterprise.id)} — {enterprise.cost} монет
                  </option>
                ))}
              </Select>
            </div>
            <NumericAdjuster
              label="Уровень зарплаты при найме"
              value={Math.max(1, toInt(parameters.wageLevel, 2))}
              min={1}
              max={3}
              onChange={(value) => onPatchParameters({ wageLevel: value })}
            />
            <p className="text-xs text-zinc-400">
              После постройки выбранная карточка уйдет с рынка; новые карточки добираются в фазе подготовки следующего раунда.
            </p>
          </div>
        )}

        {actionType === "COMMIT_VOTE_INFLUENCE" && (
          <div className="grid gap-3">
            <div className="grid gap-3 sm:grid-cols-2">
              <Select value={actorPlayerId} onChange={(event) => onPatchParameters({ actorPlayerId: event.target.value, influenceAmount: 0 })}>
                {(influencePlayerOptions.length > 0 ? influencePlayerOptions : state.players).map((player) => (
                  <option key={`influence-${player.playerId}`} value={player.playerId}>
                    {playerLabel(player)}
                  </option>
                ))}
              </Select>
              <div className="rounded-lg border border-zinc-700/60 bg-black/30 p-3 text-xs text-zinc-300">
                Доступно влияния: {Math.max(0, voteActor?.influence ?? 0)}
              </div>
            </div>
            <NumericAdjuster
              label="Сколько личного влияния потратить"
              value={Math.max(0, toInt(parameters.influenceAmount, 0))}
              min={0}
              max={Math.max(0, voteActor?.influence ?? 0)}
              onChange={(value) => onPatchParameters({ influenceAmount: value })}
            />
            <p className="text-xs text-zinc-400">
              Каждый потраченный жетон влияния добавляет 1 голос к выбранной стороне игрока. После последнего подтверждения движок покажет результат.
            </p>
          </div>
        )}

        {actionType === "DRAW_VOTING_CUBES" && (
          <div className="grid gap-3">
            <div className="rounded-lg border border-zinc-700/60 bg-black/30 p-3 text-xs text-zinc-300">
              В мешке сейчас: рабочие {state.votingBag.worker ?? 0}, средний класс {state.votingBag.middleClass ?? 0}, капиталисты{" "}
              {state.votingBag.capitalist ?? 0}.
            </div>
            <NumericAdjuster
              label="Сколько кубиков достать"
              value={Math.max(1, toInt(parameters.count, Math.min(5, Math.max(1, votingBagTotal))))}
              min={1}
              max={Math.max(1, votingBagTotal)}
              onChange={(value) => onPatchParameters({ count: value })}
            />
            <p className="text-xs text-zinc-400">
              Движок случайно достанет выбранное количество кубиков и сразу переведет голосование к вложению влияния.
            </p>
          </div>
        )}

        {actionType === "HIRE_WORKER" && (
          <NumericAdjuster label="Количество рабочих для найма" value={Math.max(0, toInt(parameters.count, 1))} min={0} max={5} onChange={(value) => onPatchParameters({ count: value })} />
        )}

        {(actionType === "PRODUCE_GOODS" || actionType === "SELL_GOODS") && (
          <NumericAdjuster label="Количество" value={Math.max(0, toInt(parameters.amount, 1))} min={0} max={20} onChange={(value) => onPatchParameters({ amount: value })} />
        )}

        {actionType === "ADJUST_POLICY" && (
          <div className="grid gap-3 sm:grid-cols-2">
            <div className="space-y-2">
              <p className="text-xs text-zinc-400">Трек политики</p>
              <Select value={toString(parameters.track, "TAXATION")} onChange={(event) => onPatchParameters({ track: event.target.value })}>
                <option value="TAXATION">Налоги</option>
              </Select>
            </div>
            <NumericAdjuster label="Сдвиг курса (+/-)" value={toInt(parameters.delta, 1)} min={-3} max={3} onChange={(value) => onPatchParameters({ delta: value })} />
          </div>
        )}

        {actionType === "PLAY_CARD" && (
          <div className="space-y-3">
            <ManualPlayCardControls parameters={parameters} state={state} actor={actor} onPatch={onPatchParameters} />
          </div>
        )}

        <div className="grid grid-cols-3 gap-2">
          <Button type="button" variant="secondary" onClick={() => onPreview(effectiveSubmitActionType, payload)} disabled={isPreviewing || isSubmitting}>
            {isPreviewing ? "Предпросмотр..." : "Предпросмотр"}
          </Button>
          <Button type="button" onClick={() => onSubmit(effectiveSubmitActionType, payload)} disabled={isSubmitting || isPreviewing}>
            {isSubmitting ? "Отправка..." : "Подтвердить"}
          </Button>
          <Button type="button" variant="outline" onClick={onCancel}>
            Отмена
          </Button>
        </div>

        {lastPreviewResult && (
          <div className="rounded-lg border border-zinc-700/70 bg-black/20 p-3 text-xs text-zinc-300">
            <p className="font-semibold text-zinc-100">Результат предпросмотра</p>
            {!lastPreviewResult.accepted && (
              <ul className="mt-1 list-disc pl-4 text-rose-300">
                {lastPreviewResult.errors.map((error, idx) => (
                  <li key={`${error}-${idx}`}>{publicValidationError(error)}</li>
                ))}
              </ul>
            )}
            {lastPreviewResult.accepted && (
              <div className="mt-2 space-y-1">
                <p>Дельта денег: {stringifyUnknown(lastPreviewResult.delta.moneyDeltaByPlayer)}</p>
                <p>Дельта ресурсов: {stringifyUnknown(lastPreviewResult.delta.resourceDeltaByPlayer)}</p>
                <p>Движение рабочих: {stringifyUnknown(lastPreviewResult.delta.workerMovement)}</p>
                {lastPreviewResult.delta.notes.length > 0 && <p>Заметки: {lastPreviewResult.delta.notes.join(" | ")}</p>}
              </div>
            )}
          </div>
        )}

        {lastCommandResult && !lastCommandResult.accepted && (
          <div className="rounded-lg border border-rose-500/50 bg-rose-500/10 p-3 text-xs text-rose-100">
            <p className="font-semibold">Ошибки валидации</p>
            <ul className="mt-1 list-disc pl-4">
                {lastCommandResult.errors.map((error, idx) => (
                <li key={`${error}-${idx}`}>{publicValidationError(error)}</li>
              ))}
            </ul>
          </div>
        )}
      </CardContent>
    </Card>
  );
}

