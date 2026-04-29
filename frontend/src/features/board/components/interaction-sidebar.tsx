import { useMemo, useState } from "react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { CardReadinessPanel } from "@/components/game/card-readiness-panel";
import { ModeSetupPanel } from "@/components/game/mode-setup-panel";
import { SaveLoadControls } from "@/components/game/save-load-controls";
import { PendingActionPanel } from "@/features/board/components/pending-action-panel";
import { buildPublicGameLog } from "@/features/board/model/public-game-log";
import type { AvailableInteraction, BoardRenderable, BoardZoneView } from "@/features/board/model/types";
import type { PendingActionDraft } from "@/features/board/store/use-board-interaction-store";
import type { PublicGameLogEntry } from "@/features/board/model/public-game-log";
import type {
  ActionType,
  BusinessDealCard,
  BotTurnSummary,
  CommandResponse,
  GameState,
  PreviewActionResponse,
} from "@/types/game";

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

interface InteractionSidebarProps {
  state: GameState;
  selectedEntity?: BoardRenderable;
  selectedZone?: BoardZoneView;
  canEndTurn: boolean;
  availableInteractions: AvailableInteraction[];
  pendingAction?: PendingActionDraft;
  isPreviewing: boolean;
  isSubmitting: boolean;
  lastPreviewResult?: PreviewActionResponse;
  lastCommandResult?: CommandResponse;
  lastBotSummary?: BotTurnSummary;
  isBotTurnLoading: boolean;
  isBotUntilLoading: boolean;
  saveFileName: string;
  setSaveFileName: (name: string) => void;
  isSaving: boolean;
  isLoading: boolean;
  isResetting: boolean;
  isApplyingSetup: boolean;
  onStartAction: (actionType: ActionType) => void;
  onCancelPendingAction: () => void;
  onPatchPendingParameters: (patch: Record<string, unknown>) => void;
  onSetPendingStep: (step: number) => void;
  onPreview: (actionType: ActionType, parameters: Record<string, unknown>) => void;
  onSubmit: (actionType: ActionType, parameters: Record<string, unknown>) => void;
  onPlayBotTurn: () => void;
  onPlayBotUntilHuman: () => void;
  onEndTurn: () => void;
  onApplySetup: (payload: { playerCount: number; controlModes: Record<string, string>; botStrategyModes: Record<string, string> }) => void;
  onSave: () => void;
  onLoad: () => void;
  onReset: () => void;
  onUndo: () => void;
}

const PHASE_SEQUENCE: string[] = ["PREPARATION", "ACTIONS", "PRODUCTION", "VOTING", "SCORING"];
const ROUND_ONE_SEQUENCE: string[] = ["ACTIONS", "PRODUCTION", "VOTING", "SCORING"];
const CLASS_LABEL: Record<string, string> = {
  WORKER: "Рабочий класс",
  MIDDLE_CLASS: "Средний класс",
  CAPITALIST: "Капиталист",
  STATE: "Государство",
  NONE: "Система",
};
const CONSUME_ACTIONS: ActionType[] = ["CONSUME_LUXURY", "CONSUME_EDUCATION", "CONSUME_HEALTHCARE"];

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

function resourceAmountText(resourceId: string, amount: number): string {
  return `${amount} ${resourceName(resourceId)}`;
}

function resourceSymbol(resourceId: string): string {
  const normalized = resourceId.toLowerCase();
  if (normalized === "food") return "🍞";
  if (normalized === "luxury") return "💎";
  if (normalized === "healthcare") return "⚕";
  if (normalized === "education") return "🎓";
  if (normalized === "influence" || normalized === "media_influence") return "★";
  return normalized.slice(0, 1).toUpperCase();
}

function compactResourceAmount(resourceId: string, amount: number): string {
  return `${amount}${resourceSymbol(resourceId)}`;
}

function phaseProgress(state: GameState): { index: number; total: number } {
  const sequence = state.currentRound === 1 ? ROUND_ONE_SEQUENCE : PHASE_SEQUENCE;
  const idx = sequence.indexOf(state.currentPhase);
  return { index: idx >= 0 ? idx + 1 : 1, total: sequence.length };
}

function phaseLabel(phase: string): string {
  switch (phase) {
    case "PREPARATION":
      return "Подготовка";
    case "ACTIONS":
      return "Действия";
    case "PRODUCTION":
      return "Производство";
    case "VOTING":
      return "Голосование";
    case "SCORING":
      return "Подсчет очков";
    default:
      return phase;
  }
}

function logToneClasses(faction: PublicGameLogEntry["actorClass"]): string {
  switch (faction) {
    case "WORKER":
      return "border-rose-500/40 bg-rose-500/12";
    case "MIDDLE_CLASS":
      return "border-amber-400/40 bg-amber-400/12";
    case "CAPITALIST":
      return "border-sky-500/40 bg-sky-500/12";
    case "STATE":
      return "border-slate-300/35 bg-slate-400/12";
    default:
      return "border-zinc-700/70 bg-black/20";
  }
}

function logToneBadge(faction: PublicGameLogEntry["actorClass"]): string {
  switch (faction) {
    case "WORKER":
      return "text-rose-200";
    case "MIDDLE_CLASS":
      return "text-amber-200";
    case "CAPITALIST":
      return "text-sky-200";
    case "STATE":
      return "text-slate-200";
    default:
      return "text-zinc-300";
  }
}

type StateServiceStockKey = "hospital" | "university" | "media";

interface StateServiceStockSelection {
  key: StateServiceStockKey;
  enterpriseLabel: string;
  resourceLabel: string;
  resourceType: "HEALTHCARE" | "EDUCATION" | "INFLUENCE";
  available: number;
}

function selectedStateServiceStock(state: GameState, selectedEntity?: BoardRenderable): StateServiceStockSelection | undefined {
  const markerId = selectedEntity?.id?.startsWith("state-service-stock:")
    ? selectedEntity.id
    : selectedEntity?.sourceRef?.sourceId?.startsWith("state-service-stock:")
      ? selectedEntity.sourceRef.sourceId
      : undefined;

  if (!markerId) {
    return undefined;
  }

  const key = markerId.slice("state-service-stock:".length) as StateServiceStockKey;
  if (key === "hospital") {
    return {
      key,
      enterpriseLabel: "Гос. больницы",
      resourceLabel: "Медицина",
      resourceType: "HEALTHCARE",
      available: Math.max(0, Number(state.publicServicesStorage?.healthcare ?? 0)),
    };
  }
  if (key === "university") {
    return {
      key,
      enterpriseLabel: "Гос. университеты",
      resourceLabel: "Образование",
      resourceType: "EDUCATION",
      available: Math.max(0, Number(state.publicServicesStorage?.education ?? 0)),
    };
  }
  if (key === "media") {
    return {
      key,
      enterpriseLabel: "Гос. медиа",
      resourceLabel: "Влияние",
      resourceType: "INFLUENCE",
      available: Math.max(0, Number(state.publicServicesStorage?.media_influence ?? state.publicServicesStorage?.influence ?? 0)),
    };
  }
  return undefined;
}

function EntityContextPanel({
  state,
  selectedEntity,
  selectedZone,
  availableInteractions,
  onStartAction,
}: {
  state: GameState;
  selectedEntity?: BoardRenderable;
  selectedZone?: BoardZoneView;
  availableInteractions: AvailableInteraction[];
  onStartAction: (actionType: ActionType) => void;
}) {
  const currentPlayerIndex = Number.isInteger(state.turnOrder?.currentPlayerIndex)
    ? Number(state.turnOrder.currentPlayerIndex)
    : 0;
  const currentPlayer = Array.isArray(state.players) ? state.players[currentPlayerIndex] : undefined;
  const isHumanActor = currentPlayer?.controlMode === "HUMAN";
  const selectedPolicyId = selectedEntity?.sourceRef?.sourceType === "policy"
    ? selectedEntity.sourceRef.sourceId
    : selectedZone?.id?.startsWith("policy:")
      ? selectedZone.id.slice("policy:".length)
      : undefined;
  const selectedPolicy = selectedPolicyId
    ? (Array.isArray(state.policies) ? state.policies : []).find((policy) => policy.id === selectedPolicyId)
    : undefined;
  const selectedEnterpriseId =
    selectedEntity?.sourceRef?.sourceType === "enterprise" ? selectedEntity.sourceRef.sourceId : undefined;
  const selectedEnterprise = selectedEnterpriseId
    ? (Array.isArray(state.enterprises) ? state.enterprises : []).find((enterprise) => enterprise.id === selectedEnterpriseId)
    : undefined;
  const selectedBusinessDealId =
    selectedEntity?.sourceRef?.sourceType === "businessDeal" ? selectedEntity.sourceRef.sourceId : undefined;
  const selectedBusinessDeal = selectedBusinessDealId
    ? (Array.isArray(state.businessDealCards) ? state.businessDealCards : []).find((deal) => deal.id === selectedBusinessDealId)
    : undefined;
  const selectedExportCard =
    selectedEntity?.sourceRef?.sourceType === "exportCard" ? state.activeExportCard : selectedZone?.id === "export" ? state.activeExportCard : undefined;
  const visibleBusinessDeals = (Array.isArray(state.businessDealDeck?.visibleCardIds) ? state.businessDealDeck.visibleCardIds : [])
    .map((dealId) => (Array.isArray(state.businessDealCards) ? state.businessDealCards : []).find((deal) => deal.id === dealId))
    .filter((deal): deal is BusinessDealCard => Boolean(deal));
  const stateServiceSelection = selectedStateServiceStock(state, selectedEntity);
  const legalActions = availableInteractions.filter((interaction) => interaction.enabled);
  const canOpenStandardBuy =
    isHumanActor &&
    (stateServiceSelection?.resourceType === "HEALTHCARE" || stateServiceSelection?.resourceType === "EDUCATION") &&
    (currentPlayer?.classType === "WORKER" || currentPlayer?.classType === "MIDDLE_CLASS");
  const canOpenManualTransfer = Boolean(isHumanActor && stateServiceSelection);

  return (
    <Card className="border-zinc-700/80 bg-zinc-950/80">
      <CardHeader className="pb-2">
        <CardTitle className="text-sm uppercase tracking-[0.18em] text-zinc-300">Контекст выбора</CardTitle>
      </CardHeader>
      <CardContent className="space-y-2 text-sm">
        {!selectedEntity && !selectedZone && (
          <p className="text-muted-foreground">
            Выберите трек политики, предприятие, рабочего или зону поля, чтобы увидеть детали и доступные действия.
          </p>
        )}

        {selectedPolicy && (
          <div className="rounded-lg border border-violet-500/35 bg-violet-500/10 p-3">
            <p className="font-semibold text-violet-100">{selectedPolicy.id}</p>
            <p className="mt-1 text-xs text-zinc-300">Текущий курс: {selectedPolicy.currentCourse}</p>
            <p className="text-xs text-zinc-300">
              Законопроект:{" "}
              {selectedPolicy.occupyingProposalToken
                ? `${selectedPolicy.occupyingProposalToken.ownerPlayerId ?? CLASS_LABEL[selectedPolicy.occupyingProposalToken.ownerClass] ?? selectedPolicy.occupyingProposalToken.ownerClass} -> ${selectedPolicy.occupyingProposalToken.targetCourse}`
                : "нет"}
            </p>
            <p className="text-xs text-zinc-300">
              Предложение закона: {legalActions.some((interaction) => interaction.actionType === "PROPOSE_BILL") ? "доступно" : "заблокировано"}
            </p>
          </div>
        )}

        {stateServiceSelection && (
          <div className="rounded-lg border border-emerald-500/35 bg-emerald-500/10 p-3">
            <p className="font-semibold text-emerald-100">{stateServiceSelection.enterpriseLabel}</p>
            <p className="mt-1 text-xs text-zinc-300">Ресурс: {stateServiceSelection.resourceLabel}</p>
            <p className="text-xs text-zinc-300">Доступно в запасе: {stateServiceSelection.available}</p>
            <div className="mt-3 flex flex-wrap gap-2">
              {stateServiceSelection.resourceType !== "INFLUENCE" && (
                <Button type="button" size="sm" onClick={() => onStartAction("BUY_GOODS_AND_SERVICES")} disabled={!canOpenStandardBuy}>
                  Открыть покупку
                </Button>
              )}
              <Button type="button" size="sm" variant="secondary" onClick={() => onStartAction("PLAY_CARD")} disabled={!canOpenManualTransfer}>
                {stateServiceSelection.resourceType === "INFLUENCE" ? "Забрать через ручной перенос" : "Открыть ручной перенос"}
              </Button>
            </div>
            {!isHumanActor && (
              <p className="mt-2 text-xs text-zinc-400">Брать ресурс отсюда можно только на ходу игрока-человека.</p>
            )}
            {isHumanActor && stateServiceSelection.resourceType === "INFLUENCE" && (
              <p className="mt-2 text-xs text-zinc-400">
                Стандартная покупка влияния из госпакета движком не поддерживается, поэтому доступен только ручной перенос.
              </p>
            )}
            {isHumanActor &&
              stateServiceSelection.resourceType !== "INFLUENCE" &&
              !canOpenStandardBuy && (
                <p className="mt-2 text-xs text-zinc-400">
                  Обычная покупка отсюда доступна только рабочему или среднему классу. Для остальных остаётся ручной перенос.
                </p>
              )}
          </div>
        )}

        {selectedEnterprise && (
          <div className="rounded-lg border border-sky-500/35 bg-sky-500/10 p-3">
            <p className="font-semibold text-sky-100">{selectedEntity?.label ?? selectedEnterprise.name ?? selectedEnterprise.id}</p>
            <p className="mt-1 text-xs text-zinc-300">
              Владелец: {CLASS_LABEL[selectedEnterprise.ownerClass] ?? selectedEnterprise.ownerClass} | Сектор: {selectedEnterprise.sector} | Категория: {selectedEnterprise.category ?? "н/д"}
            </p>
            <p className="text-xs text-zinc-300">
              Зарплата: {selectedEnterprise.wageLevel} | Работает: {selectedEnterprise.functioning ? "да" : "нет"} | Авто: {selectedEnterprise.automated ? "да" : "нет"}
            </p>
            <p className="text-xs text-zinc-300">
              Слоты: {selectedEnterprise.slots.filter((slot) => Boolean(slot.occupiedWorkerId)).length}/{selectedEnterprise.slots.length}
            </p>
            <p className="text-xs text-zinc-300">
              Выпуск: {Object.entries(selectedEnterprise.producedResources ?? {}).map(([resource, amount]) => `${resource}:${amount}`).join(", ") || "нет"}
            </p>
            <p className="text-xs text-zinc-300">
              Требования слотов: {selectedEnterprise.slots.map((slot) => `${slot.requiredQualification}${slot.requiredColor ? `/${slot.requiredColor}` : ""}`).join(", ") || "нет"}
            </p>
            {!selectedEnterprise.automated && selectedEnterprise.wageTrack && Object.keys(selectedEnterprise.wageTrack).length > 0 && (
              <p className="text-xs text-zinc-300">
                Трек зарплат: низк. {selectedEnterprise.wageTrack.low ?? "-"}, средн. {selectedEnterprise.wageTrack.medium ?? "-"}, высок. {selectedEnterprise.wageTrack.high ?? "-"}
              </p>
            )}
          </div>
        )}

        {selectedBusinessDeal && (
          <div className="rounded-lg border border-amber-500/35 bg-amber-500/10 p-3">
            <p className="font-semibold text-amber-100">{selectedBusinessDeal.title}</p>
            <p className="mt-1 text-xs text-zinc-300">
              Требования: {selectedBusinessDeal.requirements.map((requirement) => `${compactResourceAmount(requirement.resourceId, requirement.amount)} (${resourceName(requirement.resourceId)})`).join(", ")}
            </p>
            <p className="text-xs text-zinc-300">
              Кратко: {selectedBusinessDeal.requirements.map((requirement) => compactResourceAmount(requirement.resourceId, requirement.amount)).join(" + ")} → {selectedBusinessDeal.payout} монет
            </p>
            <p className="text-xs text-zinc-300">
              Бонус при сумме требований от {selectedBusinessDeal.thresholdAmount}: курс A +{selectedBusinessDeal.policyABonus}, курс B +{selectedBusinessDeal.policyBBonus}
            </p>
          </div>
        )}

        {selectedExportCard?.cardId && (
          <div className="rounded-lg border border-cyan-500/35 bg-cyan-500/10 p-3">
            <p className="font-semibold text-cyan-100">{selectedExportCard.title}</p>
            <p className="mt-1 text-xs text-zinc-300">{selectedExportCard.description}</p>
            <p className="text-xs text-zinc-300">
              Доступные операции: {selectedExportCard.availableOperations} | Раунд: {selectedExportCard.activatedRound}
            </p>
            <div className="mt-2 space-y-1">
              {(Array.isArray(selectedExportCard.offers) ? selectedExportCard.offers : []).map((offer, index) => (
                <p key={`${selectedExportCard.cardId}-${index}`} className="text-xs text-zinc-300">
                  {compactResourceAmount(offer.resourceId, offer.quantity)} → {offer.revenue} монет <span className="text-zinc-500">({resourceName(offer.resourceId)})</span>
                </p>
              ))}
            </div>
          </div>
        )}

        {selectedZone?.id === "deals" && (
          <div className="rounded-lg border border-amber-500/25 bg-amber-500/5 p-3">
            <p className="font-semibold text-amber-100">Открытые сделки</p>
            {visibleBusinessDeals.length === 0 && <p className="mt-1 text-xs text-zinc-400">Сделок нет.</p>}
            {visibleBusinessDeals.map((deal) => (
              <p key={deal.id} className="mt-1 text-xs text-zinc-300">
                {deal.title}: {deal.requirements.map((requirement) => compactResourceAmount(requirement.resourceId, requirement.amount)).join(" + ")} → {deal.payout} монет
              </p>
            ))}
            <p className="mt-2 text-xs text-zinc-400">
              Обновления: {state.businessDealDeck?.refreshCount ?? 0}
              {state.businessDealDeck?.lastRefreshReason ? ` | последнее: ${state.businessDealDeck.lastRefreshReason}` : ""}
            </p>
          </div>
        )}

        {selectedEntity && !selectedPolicy && !selectedEnterprise && !selectedBusinessDeal && !selectedExportCard?.cardId && !stateServiceSelection && (
          <div className="rounded-lg border border-zinc-700/70 bg-black/20 p-3">
            <p className="font-semibold text-zinc-100">{selectedEntity.label}</p>
            <p className="text-xs text-zinc-400">{selectedEntity.kind}</p>
            {selectedEntity.details && <p className="mt-1 text-xs text-zinc-300">{selectedEntity.details}</p>}
            {selectedEntity.sourceRef && (
              <p className="mt-1 text-xs text-zinc-400">
                Источник: {selectedEntity.sourceRef.sourceType} / {selectedEntity.sourceRef.sourceId}
              </p>
            )}
          </div>
        )}

        {selectedZone && !selectedPolicy && (
          <div className="rounded-lg border border-zinc-700/70 bg-black/20 p-3">
            <p className="font-semibold text-zinc-100">{selectedZone.label}</p>
            <p className="text-xs text-zinc-400">{selectedZone.id}</p>
            {selectedZone.stats.map((line, idx) => (
              <p key={`${selectedZone.id}-${idx}`} className="text-xs text-zinc-300">
                {line}
              </p>
            ))}
          </div>
        )}

        {(selectedPolicy || selectedEntity || selectedZone) && (
          <div className="rounded-lg border border-zinc-700/70 bg-black/20 p-3">
            <p className="text-[11px] font-semibold uppercase tracking-wide text-zinc-300">Действия в текущем контексте</p>
            {legalActions.length === 0 && <p className="mt-1 text-xs text-zinc-500">Для текущего выбора действий нет.</p>}
            {legalActions.slice(0, 4).map((interaction) => (
              <p key={interaction.id} className="mt-1 text-xs text-zinc-300">
                {interaction.label}
              </p>
            ))}
          </div>
        )}
      </CardContent>
    </Card>
  );
}

function AvailableActionsPanel({
  availableInteractions,
  pendingActionType,
  onUndo,
  onStartAction,
}: {
  availableInteractions: AvailableInteraction[];
  pendingActionType?: ActionType;
  onUndo: () => void;
  onStartAction: (actionType: ActionType) => void;
}) {
  const isConsumeAction = (actionType: ActionType) => CONSUME_ACTIONS.includes(actionType);
  const standardActions = availableInteractions.filter(
    (interaction) => Boolean(interaction.legalMove) && interaction.actionType !== "PLAY_CARD" && !isConsumeAction(interaction.actionType),
  );
  const freeRuleActions = availableInteractions.filter(
    (interaction) => interaction.actionType !== "PLAY_CARD" && (isConsumeAction(interaction.actionType) || !interaction.legalMove),
  );
  const nonStandardActions = availableInteractions.filter((interaction) => interaction.actionType === "PLAY_CARD");

  const renderActionButton = (interaction: AvailableInteraction) => (
    <button
      key={interaction.id}
      type="button"
      onClick={() => interaction.enabled && onStartAction(interaction.actionType)}
      disabled={!interaction.enabled}
      className="w-full rounded-lg border border-zinc-700/70 bg-black/20 px-3 py-2 text-left transition-colors hover:border-zinc-500/80 hover:bg-black/35 disabled:cursor-not-allowed disabled:opacity-50"
    >
      <div className="flex items-center justify-between gap-2">
        <span className="text-sm font-semibold text-zinc-100">{interaction.label}</span>
        {pendingActionType === interaction.actionType && <Badge tone="positive">выбрано</Badge>}
        {!interaction.enabled && <Badge tone="warning">заблокировано</Badge>}
      </div>
      <p className="mt-1 text-xs text-zinc-400">{interaction.enabled ? interaction.summary : interaction.disabledReason ?? interaction.summary}</p>
    </button>
  );

  const renderGroup = (
    title: string,
    subtitle: string,
    toneClass: string,
    items: AvailableInteraction[],
    emptyText: string,
  ) => (
    <div className={`space-y-2 rounded-lg border p-3 ${toneClass}`}>
      <div>
        <p className="text-[11px] font-semibold uppercase tracking-[0.15em] text-zinc-200">{title}</p>
        <p className="text-xs text-zinc-400">{subtitle}</p>
      </div>
      {items.length === 0 && <p className="text-xs text-zinc-500">{emptyText}</p>}
      {items.map((interaction) => renderActionButton(interaction))}
    </div>
  );

  return (
    <Card className="border-zinc-700/80 bg-zinc-950/80">
      <CardHeader className="pb-2">
        <div className="flex items-center justify-between gap-2">
          <CardTitle className="text-sm uppercase tracking-[0.18em] text-zinc-300">Доступные действия</CardTitle>
          <Button type="button" variant="outline" size="sm" onClick={onUndo}>
            Отмена выбора
          </Button>
        </div>
      </CardHeader>
      <CardContent className="space-y-2">
        {availableInteractions.length === 0 && <p className="text-sm text-muted-foreground">Для этого выбора нет действий.</p>}
        {renderGroup(
          "Стандартные базовые действия",
          "Строго легальные варианты, подтвержденные движком правил.",
          "border-emerald-500/35 bg-emerald-500/5",
          standardActions,
          "Сейчас нет базовых легальных действий.",
        )}
        {renderGroup(
          "Свободные действия (в рамках правил)",
          "Ручной выбор из поддерживаемых шаблонов без выхода за рамки движка.",
          "border-sky-500/35 bg-sky-500/5",
          freeRuleActions,
          "Сейчас нет свободных действий в рамках правил.",
        )}
        {renderGroup(
          "Полностью свободные нештатные действия",
          "Экстренные ручные корректировки для тестовых и нестандартных ситуаций.",
          "border-rose-500/40 bg-rose-500/10",
          nonStandardActions,
          "Нештатные действия сейчас недоступны.",
        )}
      </CardContent>
    </Card>
  );
}
function TurnControlsPanel({
  state,
  canEndTurn,
  isSubmitting,
  isBotUntilLoading,
  onEndTurn,
}: {
  state: GameState;
  canEndTurn: boolean;
  isSubmitting: boolean;
  isBotUntilLoading: boolean;
  onEndTurn: () => void;
}) {
  const currentPlayerIndex = Number.isInteger(state.turnOrder?.currentPlayerIndex)
    ? Number(state.turnOrder.currentPlayerIndex)
    : 0;
  const currentPlayer = Array.isArray(state.players) ? state.players[currentPlayerIndex] : undefined;
  const { index: phaseIndex, total: phaseTotal } = phaseProgress(state);
  const isHumanActor = currentPlayer?.controlMode === "HUMAN";
  const currentClassLabel = CLASS_LABEL[currentPlayer?.classType ?? "NONE"] ?? currentPlayer?.classType ?? "н/д";

  return (
    <Card className="border-zinc-700/80 bg-zinc-950/80">
      <CardHeader className="pb-2">
        <CardTitle className="text-sm uppercase tracking-[0.18em] text-zinc-300">Порядок хода</CardTitle>
      </CardHeader>
      <CardContent className="space-y-3">
        <div className="rounded-lg border border-zinc-700/70 bg-black/20 p-3 text-sm">
          <p className="font-semibold text-zinc-100">
            {currentClassLabel} <span className="text-zinc-400">({currentPlayer?.controlMode ?? "н/д"})</span>
          </p>
          <p className="mt-1 text-xs text-zinc-400">
            Раунд {state.currentRound}/{state.maxRounds} - Фаза {phaseLabel(state.currentPhase)} ({phaseIndex}/{phaseTotal}) - Позиция {currentPlayerIndex + 1}/{state.turnOrder.activeClasses.length || 1}
          </p>
        </div>
        <Button
          className="h-12 w-full text-base"
          onClick={onEndTurn}
          disabled={!canEndTurn || !isHumanActor || isSubmitting || isBotUntilLoading}
        >
          {isSubmitting || isBotUntilLoading ? "Выполняю переход хода..." : "Завершить ход и запустить ботов"}
        </Button>
        <p className="text-xs text-zinc-400">
          Завершает окно действий человека и автоматически прокручивает ходы ботов до следующего решения человека.
        </p>
      </CardContent>
    </Card>
  );
}

function BotSummaryPanel({
  state,
  summary,
  isTurnLoading,
  isUntilLoading,
  onPlayTurn,
  onPlayUntilHuman,
}: {
  state: GameState;
  summary?: BotTurnSummary;
  isTurnLoading: boolean;
  isUntilLoading: boolean;
  onPlayTurn: () => void;
  onPlayUntilHuman: () => void;
}) {
  const checksTrace = summary?.automaTrace?.checksTrace;
  const checksTraceCount = Array.isArray(checksTrace) ? checksTrace.length : 0;
  const currentCardNo = summary?.automaTrace?.currentCardNo;
  const mode = summary?.automaTrace?.mode;
  const specialActionType = summary?.automaTrace?.specialActionType;
  const bonusTrace = summary?.automaTrace?.bonus;
  const hasCardNo = currentCardNo !== undefined && currentCardNo !== null && String(currentCardNo).length > 0;
  const modeText = mode === undefined || mode === null ? "ПРОСТОЙ" : String(mode);
  const specialActionText = stringifyUnknown(specialActionType);
  const hasSpecialAction = specialActionText.length > 0;
  const bonusTraceText = stringifyUnknown(bonusTrace);
  const hasBonusTrace = bonusTraceText.length > 0;
  const currentPlayerIndex = Number.isInteger(state.turnOrder?.currentPlayerIndex)
    ? Number(state.turnOrder.currentPlayerIndex)
    : 0;
  const currentPlayer = Array.isArray(state.players) ? state.players[currentPlayerIndex] : undefined;
  const canPlayBot = currentPlayer?.controlMode === "BOT";

  return (
    <Card className="border-zinc-700/80 bg-zinc-950/80">
      <CardHeader className="pb-2">
        <CardTitle className="text-sm uppercase tracking-[0.18em] text-zinc-300">Сводка хода бота</CardTitle>
      </CardHeader>
      <CardContent className="space-y-3">
        <div className="grid grid-cols-2 gap-2">
          <Button onClick={onPlayTurn} disabled={isTurnLoading || !canPlayBot}>
            {isTurnLoading ? "Выполняю..." : "Сыграть ход бота"}
          </Button>
          <Button variant="secondary" onClick={onPlayUntilHuman} disabled={isUntilLoading}>
            {isUntilLoading ? "Выполняю..." : "Играть до человека"}
          </Button>
        </div>
        {!canPlayBot && (
          <p className="text-xs text-zinc-400">
            Прямой ход бота доступен только когда текущий игрок находится под управлением бота.
          </p>
        )}
        {!summary && <p className="text-sm text-muted-foreground">Пока нет действий бота.</p>}
        {summary && (
          <div className="space-y-1 rounded-lg border border-zinc-700/70 bg-black/20 p-3 text-xs">
            <p>
              <span className="text-zinc-400">Выбранное действие:</span> {summary.selectedAction}
            </p>
            {hasCardNo && (
              <p>
                <span className="text-zinc-400">Карта:</span> #{String(currentCardNo)} ({modeText})
              </p>
            )}
            <p>
              <span className="text-zinc-400">Почему:</span> {summary.rationale}
            </p>
            <p>
              <span className="text-zinc-400">Проверки / варианты:</span> {summary.legalOptionsConsidered}
            </p>
            <p>
              <span className="text-zinc-400">Планировщик:</span> {summary.plannerId}
            </p>
            {checksTraceCount > 0 && (
              <p>
                <span className="text-zinc-400">Сработавшие проверки:</span> {checksTraceCount}
              </p>
            )}
            {hasSpecialAction && (
              <p>
                <span className="text-zinc-400">Спецдействие:</span> {specialActionText}
              </p>
            )}
            {hasBonusTrace && (
              <p>
                <span className="text-zinc-400">Бонус:</span> {bonusTraceText}
              </p>
            )}
          </div>
        )}
      </CardContent>
    </Card>
  );
}

export function TurnSummaryLogPanel({ state }: { state: GameState }) {
  const recentTurns = useMemo(() => buildPublicGameLog(state), [state]);
  return (
    <Card className="border-zinc-700/80 bg-zinc-950/80">
      <CardHeader className="pb-2">
        <CardTitle className="text-sm uppercase tracking-[0.18em] text-zinc-300">Сводка по ходам</CardTitle>
      </CardHeader>
      <CardContent className="space-y-2">
        {recentTurns.length === 0 && <p className="text-sm text-muted-foreground">Событий пока нет.</p>}
        {recentTurns.map((turn, index) => {
          const faction = turn.actorClass;
          const actorLabel = turn.actorDisplayName;
          return (
            <div key={turn.id} className={`rounded-xl border px-3 py-3 ${logToneClasses(faction)}`}>
              <div className="mb-1 flex items-center justify-between gap-2">
                <p className={`text-xs font-semibold uppercase tracking-[0.16em] ${logToneBadge(faction)}`}>
                  {`Раунд ${turn.round}, ход ${turn.turnNumber} — ${actorLabel}`}
                </p>
                <Badge tone={turn.severity === "WARNING" ? "warning" : "neutral"}>{turn.category === "VOTE" ? "голосование" : turn.category === "REJECTION" ? "отказ" : "событие"}</Badge>
              </div>
              <div className="space-y-1">
                <p className="text-sm leading-5 text-zinc-100">{turn.title}</p>
                {turn.details.map((line, lineIndex) => (
                  <p key={`${turn.id}-${lineIndex}`} className="text-xs leading-5 text-zinc-300">
                    - {line}
                  </p>
                ))}
              </div>
            </div>
          );
        })}
      </CardContent>
    </Card>
  );
}

export function InteractionSidebar({
  state,
  selectedEntity,
  selectedZone,
  canEndTurn,
  availableInteractions,
  pendingAction,
  isPreviewing,
  isSubmitting,
  lastPreviewResult,
  lastCommandResult,
  lastBotSummary,
  isBotTurnLoading,
  isBotUntilLoading,
  saveFileName,
  setSaveFileName,
  isSaving,
  isLoading,
  isResetting,
  isApplyingSetup,
  onStartAction,
  onCancelPendingAction,
  onPatchPendingParameters,
  onSetPendingStep,
  onPreview,
  onSubmit,
  onPlayBotTurn,
  onPlayBotUntilHuman,
  onEndTurn,
  onApplySetup,
  onSave,
  onLoad,
  onReset,
  onUndo,
}: InteractionSidebarProps) {
  const [showSetupPanel, setShowSetupPanel] = useState(true);
  const handleApplySetup = (payload: { playerCount: number; controlModes: Record<string, string>; botStrategyModes: Record<string, string> }) => {
    onApplySetup(payload);
    setShowSetupPanel(false);
  };
  const handleReset = () => {
    setShowSetupPanel(true);
    onReset();
  };

  return (
    <aside className="space-y-3">
      {showSetupPanel ? (
        <ModeSetupPanel state={state} isApplying={isApplyingSetup} onApply={handleApplySetup} />
      ) : (
        <Card className="border-zinc-700/80 bg-zinc-950/80">
          <CardContent className="flex items-center justify-between gap-3 p-3">
            <p className="text-xs text-zinc-400">Настройка применена. Панель можно открыть снова, если нужно поменять режимы управления.</p>
            <Button type="button" variant="outline" size="sm" onClick={() => setShowSetupPanel(true)}>
              Открыть настройки
            </Button>
          </CardContent>
        </Card>
      )}

      <PendingActionPanel
        draft={pendingAction}
        state={state}
        isPreviewing={isPreviewing}
        isSubmitting={isSubmitting}
        lastPreviewResult={lastPreviewResult}
        lastCommandResult={lastCommandResult}
        onCancel={onCancelPendingAction}
        onPatchParameters={onPatchPendingParameters}
        onSetStep={onSetPendingStep}
        onPreview={onPreview}
        onSubmit={onSubmit}
      />

      <EntityContextPanel
        state={state}
        selectedEntity={selectedEntity}
        selectedZone={selectedZone}
        availableInteractions={availableInteractions}
        onStartAction={onStartAction}
      />

      <TurnControlsPanel
        state={state}
        canEndTurn={canEndTurn}
        isSubmitting={isSubmitting}
        isBotUntilLoading={isBotUntilLoading}
        onEndTurn={onEndTurn}
      />

      <AvailableActionsPanel
        availableInteractions={availableInteractions}
        pendingActionType={pendingAction?.actionType}
        onUndo={onUndo}
        onStartAction={onStartAction}
      />

      <BotSummaryPanel
        state={state}
        summary={lastBotSummary}
        isTurnLoading={isBotTurnLoading}
        isUntilLoading={isBotUntilLoading}
        onPlayTurn={onPlayBotTurn}
        onPlayUntilHuman={onPlayBotUntilHuman}
      />
      <SaveLoadControls
        fileName={saveFileName}
        setFileName={setSaveFileName}
        onSave={onSave}
        onLoad={onLoad}
        onReset={handleReset}
        isSaving={isSaving}
        isLoading={isLoading}
        isResetting={isResetting}
      />
      <CardReadinessPanel readiness={state.cardReadiness} />
    </aside>
  );
}


