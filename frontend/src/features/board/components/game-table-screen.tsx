import { useEffect, useMemo, useState } from "react";
import { BookOpen, Menu, Settings } from "lucide-react";
import { ClassBoards } from "@/features/board/components/class-boards";
import { GameBoard } from "@/features/board/components/game-board";
import { InteractionSidebar, TurnSummaryLogPanel } from "@/features/board/components/interaction-sidebar";
import { buildActionSeedParameters, buildAvailableInteractions } from "@/features/board/model/build-available-interactions";
import { buildBoardViewModel } from "@/features/board/model/build-board-view-model";
import type { BoardRenderable, BoardViewModel } from "@/features/board/model/types";
import { useBoardInteractionStore } from "@/features/board/store/use-board-interaction-store";
import type {
  ActionType,
  BotTurnSummary,
  CommandResponse,
  ComposerMetadata,
  GameState,
  LegalMove,
  PolicyCourse,
  PolicyId,
  PreviewActionResponse,
} from "@/types/game";

interface GameTableScreenProps {
  state: GameState;
  legalMoves: LegalMove[];
  composerMetadata: ComposerMetadata;
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
  onPreview: (actionType: ActionType, parameters: Record<string, unknown>) => void;
  onSubmit: (actionType: ActionType, parameters: Record<string, unknown>) => void;
  onPlayBotTurn: () => void;
  onPlayBotUntilHuman: () => void;
  onApplySetup: (payload: { playerCount: number; controlModes: Record<string, string>; botStrategyModes: Record<string, string> }) => void;
  onSave: () => void;
  onLoad: () => void;
  onReset: () => void;
}

const CLASS_LABEL: Record<string, string> = {
  WORKER: "Рабочий класс",
  MIDDLE_CLASS: "Средний класс",
  CAPITALIST: "Капиталисты",
  STATE: "Государство",
};

const ROUND_PHASES = ["PREPARATION", "ACTIONS", "PRODUCTION", "VOTING", "SCORING"];
const CONSUME_ACTIONS: ActionType[] = ["CONSUME_LUXURY", "CONSUME_EDUCATION", "CONSUME_HEALTHCARE"];
const LIFECYCLE_ACTIONS: ActionType[] = [
  "RESOLVE_PREPARATION_PHASE",
  "ADVANCE_TO_PRODUCTION",
  "RESOLVE_PRODUCTION_PHASE",
  "ADVANCE_TO_VOTING",
  "ADVANCE_TO_SCORING",
  "RESOLVE_SCORING_PHASE",
  "ADVANCE_TO_NEXT_ROUND",
  "ADVANCE_GAME_FLOW",
  "ADVANCE_ROUND",
];

const LIFECYCLE_ACTION_LABEL: Partial<Record<ActionType, string>> = {
  RESOLVE_PREPARATION_PHASE: "Завершить подготовку",
  ADVANCE_TO_PRODUCTION: "Перейти к производству",
  RESOLVE_PRODUCTION_PHASE: "Завершить производство",
  ADVANCE_TO_VOTING: "Перейти к голосованию",
  ADVANCE_TO_SCORING: "Перейти к подсчету",
  RESOLVE_SCORING_PHASE: "Подсчитать итоги",
  ADVANCE_TO_NEXT_ROUND: "Следующий раунд",
  ADVANCE_GAME_FLOW: "Продолжить",
  ADVANCE_ROUND: "Следующий раунд",
};

interface AssignmentDraft {
  workerId: string;
  targetType: "ENTERPRISE_SLOT" | "UNION" | "UNEMPLOYED";
  targetId: string;
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
    case "GAME_OVER":
      return "Игра окончена";
    default:
      return phase;
  }
}

function classLabel(classType?: string): string {
  return classType ? CLASS_LABEL[classType] ?? classType : "н/д";
}

function parsePolicyIdFromZone(zoneId?: string): PolicyId | undefined {
  if (!zoneId || !zoneId.startsWith("policy:")) {
    return undefined;
  }
  return zoneId.slice("policy:".length) as PolicyId;
}

function sameList(a: string[], b: string[]): boolean {
  if (a.length !== b.length) {
    return false;
  }
  for (let i = 0; i < a.length; i += 1) {
    if (a[i] !== b[i]) {
      return false;
    }
  }
  return true;
}

function normalizeAssignments(raw: unknown): AssignmentDraft[] {
  if (!Array.isArray(raw)) {
    return [];
  }
  return raw
    .map((entry) => {
      const record = (entry ?? {}) as Record<string, unknown>;
      return {
        workerId: String(record.workerId ?? ""),
        targetType: record.targetType === "UNION" ? "UNION" : record.targetType === "UNEMPLOYED" ? "UNEMPLOYED" : "ENTERPRISE_SLOT",
        targetId: String(record.targetId ?? ""),
      } as AssignmentDraft;
    })
    .filter((entry) => entry.workerId.length > 0 && entry.targetId.length > 0);
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

function collectSelectedWorkerIds(params: Record<string, unknown>): string[] {
  const fromMulti = Array.isArray(params.selectedWorkerIds)
    ? params.selectedWorkerIds.map((value) => String(value)).filter((value) => value.length > 0)
    : [];
  if (fromMulti.length > 0) {
    return Array.from(new Set(fromMulti));
  }
  const single = String(params.selectedWorkerId ?? "");
  return single ? [single] : [];
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

function TopHud({
  state,
  legalMoves,
  isSubmitting,
  isBotUntilLoading,
  onSubmit,
}: {
  state: GameState;
  legalMoves: LegalMove[];
  isSubmitting: boolean;
  isBotUntilLoading: boolean;
  onSubmit: (actionType: ActionType, parameters: Record<string, unknown>) => void;
}) {
  const currentPlayerIndex = Number.isInteger(state.turnOrder?.currentPlayerIndex) ? Number(state.turnOrder.currentPlayerIndex) : 0;
  const actor = state.players[currentPlayerIndex];
  const turnOrderMeta = state.turnOrder as unknown as { actionsPerPlayer?: number; actionsTakenByPlayer?: unknown };
  const actionsPerPlayer = Number.isInteger(turnOrderMeta.actionsPerPlayer) ? Number(turnOrderMeta.actionsPerPlayer) : 5;
  const actionsTakenByPlayer = Array.isArray(turnOrderMeta.actionsTakenByPlayer)
    ? turnOrderMeta.actionsTakenByPlayer.map((value) => (Number.isInteger(value) ? Number(value) : 0))
    : [];
  const actionsTakenCurrent = actionsTakenByPlayer[currentPlayerIndex] ?? 0;
  const actionSlot = state.currentPhase === "ACTIONS" ? Math.min(actionsPerPlayer, actionsTakenCurrent + 1) : actionsTakenCurrent;
  const taxCourse = state.policies.find((policy) => policy.id === "POLICY_3_TAXATION")?.currentCourse ?? "B";
  const lifecycleMove = LIFECYCLE_ACTIONS.map((actionType) => legalMoves.find((move) => move.actionType === actionType)).find(Boolean);
  const lifecycleLabel = lifecycleMove ? LIFECYCLE_ACTION_LABEL[lifecycleMove.actionType] ?? "Продолжить" : "";
  const lifecycleActorPlayerId = actor?.playerId ?? state.players[0]?.playerId ?? "";

  return (
    <div className="grid gap-3 xl:grid-cols-[260px,620px,minmax(420px,1fr),280px]">
      <div className="rounded-md border border-[#9b7338]/65 bg-[linear-gradient(145deg,rgba(18,27,26,0.96),rgba(8,12,12,0.98))] px-5 py-3 shadow-lg shadow-black/30">
        <p className="text-4xl font-light text-stone-50">HEGEMONY</p>
        <p className="text-xs font-semibold uppercase text-[#d8b56b]">Веди свой класс к победе</p>
      </div>

      <div className="grid grid-cols-[0.8fr,0.8fr,0.9fr,0.9fr,1.2fr] rounded-md border border-[#9b7338]/65 bg-[linear-gradient(145deg,rgba(18,27,26,0.96),rgba(8,12,12,0.98))] shadow-lg shadow-black/30">
        <div className="border-r border-[#9b7338]/35 px-5 py-3">
          <p className="text-xs uppercase text-stone-400">Раунд</p>
          <p className="text-2xl font-bold text-amber-300">{state.currentRound}/{state.maxRounds}</p>
        </div>
        <div className="border-r border-[#9b7338]/35 px-5 py-3">
          <p className="text-xs uppercase text-stone-400">Фаза</p>
          <p className="text-xl font-semibold text-stone-100">{phaseLabel(state.currentPhase)}</p>
        </div>
        <div className="border-r border-[#9b7338]/35 px-5 py-3">
          <p className="text-xs uppercase text-stone-400">Действие</p>
          <p className="text-xl font-semibold text-amber-200">{actionSlot}/{actionsPerPlayer}</p>
        </div>
        <div className="border-r border-[#9b7338]/35 px-5 py-3">
          <p className="text-xs uppercase text-stone-400">Налог</p>
          <p className="text-xl font-semibold text-amber-200">x{state.taxMultiplier} <span className="text-sm text-stone-400">({taxCourse})</span></p>
        </div>
        <div className="px-5 py-3">
          <p className="text-xs uppercase text-stone-400">Ходит</p>
          <p className="truncate text-lg font-semibold text-stone-100">{classLabel(actor?.classType)}</p>
        </div>
      </div>

      <div className="rounded-md border border-[#9b7338]/65 bg-[linear-gradient(145deg,rgba(18,27,26,0.96),rgba(8,12,12,0.98))] px-4 py-3 shadow-lg shadow-black/30">
        <p className="mb-2 text-xs uppercase text-stone-400">Победные очки</p>
        <div className="grid grid-cols-2 gap-2 lg:grid-cols-4">
          {state.players.map((player) => {
            const active = actor?.playerId === player.playerId;
            return (
              <div
                key={player.playerId}
                className={`rounded-md border px-3 py-2 ${active ? "border-amber-300/70 bg-amber-500/15 text-amber-100" : "border-[#8f6b35]/45 bg-black/20 text-stone-100"}`}
              >
                <p className="truncate text-[11px] uppercase text-stone-400">{classLabel(player.classType)}</p>
                <p className="text-lg font-semibold leading-none">{player.victoryPoints} ПО</p>
              </div>
            );
          })}
        </div>
      </div>

      <div className="grid grid-cols-3 rounded-md border border-[#9b7338]/65 bg-[linear-gradient(145deg,rgba(18,27,26,0.96),rgba(8,12,12,0.98))] shadow-lg shadow-black/30">
        {[
          { label: "Правила", icon: BookOpen },
          { label: "Настройки", icon: Settings },
          { label: "Меню", icon: Menu },
        ].map((item) => {
          const Icon = item.icon;
          return (
            <button key={item.label} type="button" className="flex flex-col items-center justify-center gap-1 border-r border-[#9b7338]/35 px-3 py-3 text-stone-300 transition hover:bg-[#221907]/45 hover:text-stone-50 last:border-r-0">
              <Icon className="h-5 w-5" />
              <span className="text-xs uppercase">{item.label}</span>
            </button>
          );
        })}
      </div>
      <div className="xl:col-span-4 rounded-md border border-[#9b7338]/65 bg-[linear-gradient(145deg,rgba(18,27,26,0.92),rgba(8,12,12,0.96))] px-4 py-2 shadow-lg shadow-black/25">
        <div className="grid gap-3 lg:grid-cols-[1fr,220px]">
          <div className="grid gap-2 sm:grid-cols-5">
          {ROUND_PHASES.map((phase, index) => {
            const active = state.currentPhase === phase;
            const passed = ROUND_PHASES.indexOf(state.currentPhase) > index;
            return (
              <div
                key={phase}
                className={`rounded-md border px-3 py-2 text-center text-xs uppercase tracking-[0.1em] ${
                  active
                    ? "border-amber-300/75 bg-amber-500/15 text-amber-100"
                    : passed
                      ? "border-emerald-500/35 bg-emerald-500/10 text-emerald-100"
                      : "border-stone-700/70 bg-black/20 text-stone-400"
                }`}
              >
                {index + 1}. {phaseLabel(phase)}
              </div>
            );
          })}
          </div>
          {lifecycleMove ? (
            <button
              type="button"
              onClick={() => onSubmit(lifecycleMove.actionType, { actorPlayerId: lifecycleActorPlayerId })}
              disabled={isSubmitting || isBotUntilLoading}
              className="rounded-md border border-amber-300/70 bg-amber-500/20 px-4 py-2 text-sm font-semibold uppercase text-amber-100 transition hover:bg-amber-500/30 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {isSubmitting ? "Выполняю..." : lifecycleLabel}
            </button>
          ) : (
            <div className="hidden lg:block" />
          )}
        </div>
      </div>
    </div>
  );
}

export function GameTableScreen({
  state,
  legalMoves,
  composerMetadata,
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
  onPreview,
  onSubmit,
  onPlayBotTurn,
  onPlayBotUntilHuman,
  onApplySetup,
  onSave,
  onLoad,
  onReset,
}: GameTableScreenProps) {
  const [assignError, setAssignError] = useState("");

  const selectedEntityId = useBoardInteractionStore((store) => store.selectedEntityId);
  const selectedZoneId = useBoardInteractionStore((store) => store.selectedZoneId);
  const highlightedZones = useBoardInteractionStore((store) => store.highlightedZones);
  const highlightedEntities = useBoardInteractionStore((store) => store.highlightedEntities);
  const legalTargets = useBoardInteractionStore((store) => store.legalTargets);
  const pendingAction = useBoardInteractionStore((store) => store.pendingAction);
  const selectEntity = useBoardInteractionStore((store) => store.selectEntity);
  const selectZone = useBoardInteractionStore((store) => store.selectZone);
  const startPendingAction = useBoardInteractionStore((store) => store.startPendingAction);
  const patchPendingActionParameters = useBoardInteractionStore((store) => store.patchPendingActionParameters);
  const setPendingActionStep = useBoardInteractionStore((store) => store.setPendingActionStep);
  const cancelPendingAction = useBoardInteractionStore((store) => store.cancelPendingAction);
  const clearSelection = useBoardInteractionStore((store) => store.clearSelection);
  const setHighlights = useBoardInteractionStore((store) => store.setHighlights);
  const setLegalTargets = useBoardInteractionStore((store) => store.setLegalTargets);

  const boardViewModel = useMemo(
    () => {
      try {
        return buildBoardViewModel({
          state,
          selectedZoneId,
          selectedEntityId,
          highlightedZones,
        });
      } catch (error) {
        // eslint-disable-next-line no-console
        console.error("Failed to build board view model", error);
        return {
          zones: [],
          renderables: [],
          pendingProposalPoliciesInOrder: [],
          policyTracks: [],
        };
      }
    },
    [state, selectedZoneId, selectedEntityId, highlightedZones],
  );

  const selectedWorkerIdsForAssignment = useMemo(
    () => (pendingAction?.actionType === "ASSIGN_WORKERS" ? collectSelectedWorkerIds(pendingAction.parameters) : []),
    [pendingAction],
  );

  const displayBoardViewModel = useMemo<BoardViewModel>(() => {
    const selectedWorkers = selectedWorkerIdsForAssignment
      .map((workerId) => state.workers.find((worker) => worker.id === workerId))
      .filter((worker): worker is GameState["workers"][number] => Boolean(worker));

    return {
      ...boardViewModel,
      renderables: boardViewModel.renderables.map((renderable): BoardRenderable => {
        if (renderable.kind !== "ENTERPRISE" || renderable.sourceRef?.sourceType !== "enterprise") {
          return renderable;
        }
        const enterprise = state.enterprises.find((item) => item.id === renderable.sourceRef?.sourceId);
        if (!enterprise) {
          return renderable;
        }
        return { ...renderable, tone: canAssignWorkersToEnterprise(selectedWorkers, enterprise) ? "positive" : "danger" };
      }),
    };
  }, [boardViewModel, selectedWorkerIdsForAssignment, state.enterprises, state.workers]);

  const selectedEntity = useMemo(
    () => displayBoardViewModel.renderables.find((entity) => entity.id === selectedEntityId),
    [displayBoardViewModel.renderables, selectedEntityId],
  );
  const selectedZone = useMemo(
    () => boardViewModel.zones.find((zone) => zone.id === selectedZoneId),
    [boardViewModel.zones, selectedZoneId],
  );
  const selectedPolicyId = useMemo(() => {
    if (selectedEntity?.sourceRef?.sourceType === "policy") {
      return selectedEntity.sourceRef.sourceId as PolicyId;
    }
    return parsePolicyIdFromZone(selectedZoneId);
  }, [selectedEntity, selectedZoneId]);
  const selectedPolicyCourse = pendingAction?.actionType === "PROPOSE_BILL"
    ? pendingAction.parameters.targetCourse as PolicyCourse | undefined
    : undefined;

  const availableInteractions = useMemo(
    () => buildAvailableInteractions({ selectedEntity, selectedZoneId, legalMoves, composerMetadata }),
    [selectedEntity, selectedZoneId, legalMoves, composerMetadata],
  );

  useEffect(() => {
    if (lastCommandResult?.accepted) {
      clearSelection();
      setAssignError("");
      const nextState = lastCommandResult.gameState;
      const voteStage = nextState.currentVoteState?.votingStage;
      const nextActionType =
        voteStage === "DRAW_BAG_CUBES"
          ? "DRAW_VOTING_CUBES"
          : voteStage === "COMMIT_INFLUENCE"
          ? "COMMIT_VOTE_INFLUENCE"
          : voteStage === "DECLARE_STANCES"
            ? "DECLARE_VOTE_STANCE"
            : undefined;
      if (nextActionType) {
        const actorPlayerId = composerMetadata.actorPlayerId || nextState.players[0]?.playerId || "";
        startPendingAction({
          actionType: nextActionType,
          sourceZoneId: "vote_results",
          parameters: buildActionSeedParameters(nextActionType, actorPlayerId, undefined, nextState),
          step: 1,
        });
      }
    }
  }, [lastCommandResult, composerMetadata.actorPlayerId, clearSelection, startPendingAction]);

  useEffect(() => {
    let nextZones: string[] = [];
    let nextEntities: string[] = [];
    let nextLegalTargets: string[] = [];

    if (pendingAction?.actionType === "PROPOSE_BILL") {
      nextZones = ["policy_track", ...boardViewModel.policyTracks.map((track) => track.zoneId)];
      const policyZoneId = selectedEntity?.sourceRef?.sourceType === "policy" ? `policy:${selectedEntity.sourceRef.sourceId}` : selectedZoneId;
      if (policyZoneId) {
        nextZones.push(policyZoneId);
        nextLegalTargets.push(policyZoneId);
      }
    } else if (pendingAction?.actionType === "ASSIGN_WORKERS") {
      nextZones = ["unemployed", "private_capitalist", "private_middle_class", "public_sector"];
      nextEntities = boardViewModel.renderables
        .filter((entity) => entity.kind === "WORKER" || entity.kind === "ENTERPRISE")
        .map((entity) => entity.id);
      nextLegalTargets = ["private_capitalist", "private_middle_class", "public_sector"];
    } else if (pendingAction?.actionType === "CONSUME_EDUCATION" || pendingAction?.parameters.consumeSelectedActionType === "CONSUME_EDUCATION") {
      nextZones = ["unemployed", "private_capitalist", "private_middle_class", "public_sector"];
      nextEntities = boardViewModel.renderables
        .filter((entity) => {
          if (entity.sourceRef?.sourceType !== "worker") {
            return false;
          }
          const worker = state.workers.find((item) => item.id === entity.sourceRef?.sourceId);
          return worker?.qualificationType === "UNSKILLED";
        })
        .map((entity) => entity.id);
    } else if (pendingAction?.actionType === "PLACE_STRIKES") {
      nextZones = ["private_capitalist", "private_middle_class", "public_sector"];
      nextEntities = boardViewModel.renderables.filter((entity) => entity.kind === "ENTERPRISE").map((entity) => entity.id);
    } else if (pendingAction?.actionType === "PLACE_DEMONSTRATION") {
      nextZones = ["unemployed"];
    } else if (selectedEntity) {
      nextZones = [selectedEntity.zoneId];
      nextEntities = [selectedEntity.id];
    } else if (selectedZoneId) {
      nextZones = [selectedZoneId];
    }

    if (!sameList(nextZones, highlightedZones) || !sameList(nextEntities, highlightedEntities)) {
      setHighlights(nextZones, nextEntities);
    }
    if (!sameList(nextLegalTargets, legalTargets)) {
      setLegalTargets(nextLegalTargets);
    }
  }, [
    pendingAction,
    selectedEntity,
    selectedZoneId,
    boardViewModel.renderables,
    boardViewModel.policyTracks,
    state.workers,
    highlightedZones,
    highlightedEntities,
    legalTargets,
    setHighlights,
    setLegalTargets,
  ]);

  const currentPlayerIndex = Number.isInteger(state.turnOrder?.currentPlayerIndex) ? Number(state.turnOrder.currentPlayerIndex) : 0;
  const currentPlayer = state.players[currentPlayerIndex];

  const startAction = (actionType: ActionType) => {
    setAssignError("");
    const actorPlayerId = composerMetadata.actorPlayerId || currentPlayer?.playerId || state.players[0]?.playerId || "";
    const parameters = buildActionSeedParameters(actionType, actorPlayerId, selectedEntity, state);
    if (CONSUME_ACTIONS.includes(actionType)) {
      const legalConsumeActionTypes = CONSUME_ACTIONS.filter((candidate) => legalMoves.some((move) => move.actionType === candidate));
      parameters.consumeAvailableActionTypes = legalConsumeActionTypes.length > 0 ? legalConsumeActionTypes : CONSUME_ACTIONS;
      parameters.consumeSelectedActionType =
        legalConsumeActionTypes.find((candidate) => candidate === "CONSUME_LUXURY") ??
        legalConsumeActionTypes.find((candidate) => candidate === "CONSUME_EDUCATION") ??
        legalConsumeActionTypes.find((candidate) => candidate === "CONSUME_HEALTHCARE") ??
        actionType;
    }
    if (actionType === "PROPOSE_BILL" && selectedPolicyId) {
      parameters.policyId = selectedPolicyId;
    }
    startPendingAction({
      actionType,
      sourceEntityId: selectedEntity?.id,
      sourceZoneId: selectedZoneId,
      parameters,
      step: 1,
    });
  };

  const selectPolicyTrack = (policyId: PolicyId) => {
    setAssignError("");
    selectZone(`policy:${policyId}`);
  };

  const selectPolicyCourse = (policyId: PolicyId, course: PolicyCourse) => {
    setAssignError("");
    const zoneId = `policy:${policyId}`;
    selectZone(zoneId);
    if (pendingAction?.actionType === "PROPOSE_BILL") {
      patchPendingActionParameters({ policyId, targetCourse: course });
      setPendingActionStep(Math.max(2, pendingAction.step));
      return;
    }
    const actorPlayerId = composerMetadata.actorPlayerId || currentPlayer?.playerId || state.players[0]?.playerId || "";
    startPendingAction({
      actionType: "PROPOSE_BILL",
      sourceZoneId: zoneId,
      parameters: { actorPlayerId, policyId, targetCourse: course },
      step: 2,
    });
  };

  const selectEntityOnBoard = (entityId: string) => {
    const entity = boardViewModel.renderables.find((item) => item.id === entityId);
    if (!entity) {
      if ((pendingAction?.actionType === "CONSUME_EDUCATION" || pendingAction?.parameters.consumeSelectedActionType === "CONSUME_EDUCATION") && entityId.startsWith("worker:")) {
        const workerId = entityId.slice("worker:".length);
        const worker = state.workers.find((item) => item.id === workerId);
        const actorPlayerId = composerMetadata.actorPlayerId || currentPlayer?.playerId || state.players[0]?.playerId || "";
        const actorClassType = state.players.find((player) => player.playerId === actorPlayerId)?.classType;
        if (worker?.qualificationType === "UNSKILLED" && (!actorClassType || worker.classType === actorClassType)) {
          patchPendingActionParameters({ workerId: worker.id });
          setPendingActionStep(Math.max(2, pendingAction.step));
          setAssignError("");
        }
      }
      selectEntity(entityId);
      return;
    }

    const actorPlayerId = composerMetadata.actorPlayerId || currentPlayer?.playerId || state.players[0]?.playerId || "";
    const actorClassType = state.players.find((player) => player.playerId === actorPlayerId)?.classType;

    if (!pendingAction && entity.sourceRef?.sourceType === "worker" && legalMoves.some((move) => move.actionType === "ASSIGN_WORKERS")) {
      const sourceRef = entity.sourceRef;
      const worker = state.workers.find((item) => item.id === sourceRef.sourceId);
      if (worker && actorClassType && worker.classType !== actorClassType) {
        setAssignError("Недопустимо: можно назначать только рабочих текущего класса.");
        selectEntity(entityId);
        return;
      }
      if (worker?.location === "ENTERPRISE_SLOT" && worker.tiedContract) {
        setAssignError("Недопустимо: рабочий связан трудовым договором.");
        selectEntity(entityId);
        return;
      }
      startPendingAction({
        actionType: "ASSIGN_WORKERS",
        sourceEntityId: entity.id,
        sourceZoneId: entity.zoneId,
        parameters: {
          actorPlayerId,
          assignments: worker?.location === "ENTERPRISE_SLOT"
            ? [{ workerId: sourceRef.sourceId, targetType: "UNEMPLOYED", targetId: "unemployed" }]
            : [],
          selectedWorkerIds: [sourceRef.sourceId],
          selectedWorkerId: sourceRef.sourceId,
        },
        step: 2,
      });
      setAssignError("");
      selectEntity(entityId);
      return;
    }

    if (pendingAction?.actionType === "ASSIGN_WORKERS") {
      if (entity.sourceRef?.sourceType === "worker") {
        const sourceRef = entity.sourceRef;
        const worker = state.workers.find((item) => item.id === sourceRef.sourceId);
        if (worker && actorClassType && worker.classType !== actorClassType) {
          setAssignError("Недопустимо: можно назначать только рабочих текущего класса.");
          selectEntity(entityId);
          return;
        }
        if (worker?.location === "ENTERPRISE_SLOT" && worker.tiedContract) {
          setAssignError("Недопустимо: рабочий связан трудовым договором.");
          selectEntity(entityId);
          return;
        }
        const selectedWorkerIds = collectSelectedWorkerIds(pendingAction.parameters);
        const alreadySelected = selectedWorkerIds.includes(sourceRef.sourceId);
        const nextSelectedWorkerIds = alreadySelected
          ? selectedWorkerIds.filter((workerId) => workerId !== sourceRef.sourceId)
          : [...selectedWorkerIds, sourceRef.sourceId];
        patchPendingActionParameters({
          selectedWorkerIds: nextSelectedWorkerIds,
          selectedWorkerId: nextSelectedWorkerIds[0] ?? "",
        });
        setPendingActionStep(Math.max(2, pendingAction.step));
        setAssignError("");
        selectEntity(entityId);
        return;
      }

      if (entity.sourceRef?.sourceType === "enterprise") {
        const selectedWorkerIds = collectSelectedWorkerIds(pendingAction.parameters);
        if (selectedWorkerIds.length === 0) {
          setAssignError("Сначала выберите хотя бы одного рабочего.");
          selectEntity(entityId);
          return;
        }
        const selectedWorkers = selectedWorkerIds
          .map((workerId) => state.workers.find((item) => item.id === workerId))
          .filter((worker): worker is GameState["workers"][number] => Boolean(worker));
        if (selectedWorkers.length !== selectedWorkerIds.length) {
          setAssignError("Недопустимо: часть выбранных рабочих не найдена.");
          selectEntity(entityId);
          return;
        }

        const enterprise = state.enterprises.find((item) => item.id === entity.sourceRef?.sourceId);
        if (enterprise) {
          const currentAssignments = normalizeAssignments(pendingAction.parameters.assignments);
          const retainedAssignments = currentAssignments.filter((assignment) => !selectedWorkerIds.includes(assignment.workerId));
          const usedSlotIds = new Set<string>(
            retainedAssignments
              .filter((assignment) => assignment.targetType === "ENTERPRISE_SLOT" && assignment.targetId.startsWith(`${enterprise.id}:`))
              .map((assignment) => assignment.targetId.split(":")[1] ?? "")
              .filter((slotId) => slotId.length > 0),
          );
          enterprise.slots.forEach((slot) => {
            if (slot.occupiedWorkerId && !selectedWorkerIds.includes(slot.occupiedWorkerId)) {
              usedSlotIds.add(slot.id);
            }
          });

          const newAssignments: AssignmentDraft[] = [];
          for (const worker of selectedWorkers) {
            const slotId = firstCompatibleSlotId(worker, enterprise, usedSlotIds);
            if (!slotId) {
              setAssignError("Недопустимое действие: не хватает подходящих слотов под выбранных рабочих.");
              selectEntity(entityId);
              return;
            }
            usedSlotIds.add(slotId);
            newAssignments.push({
              workerId: worker.id,
              targetType: "ENTERPRISE_SLOT",
              targetId: `${enterprise.id}:${slotId}`,
            });
          }

          patchPendingActionParameters({
            assignments: [...retainedAssignments, ...newAssignments],
            selectedWorkerIds: [],
            selectedWorkerId: "",
          });
          setPendingActionStep(Math.max(2, pendingAction.step));
          setAssignError("");
        }
      }
    }

    if ((pendingAction?.actionType === "CONSUME_EDUCATION" || pendingAction?.parameters.consumeSelectedActionType === "CONSUME_EDUCATION") && entity.sourceRef?.sourceType === "worker") {
      const worker = state.workers.find((item) => item.id === entity.sourceRef?.sourceId);
      if (worker?.qualificationType !== "UNSKILLED") {
        selectEntity(entityId);
        return;
      }
      if (actorClassType && worker.classType !== actorClassType) {
        selectEntity(entityId);
        return;
      }
      patchPendingActionParameters({ workerId: worker.id });
      setPendingActionStep(Math.max(2, pendingAction.step));
      setAssignError("");
      selectEntity(entityId);
      return;
    }

    if (pendingAction?.actionType === "PLACE_STRIKES" && entity.sourceRef?.sourceType === "enterprise") {
      const currentIds = Array.isArray(pendingAction.parameters.enterpriseIds)
        ? pendingAction.parameters.enterpriseIds.map(String)
        : [];
      const enterpriseId = entity.sourceRef.sourceId;
      const nextIds = currentIds.includes(enterpriseId)
        ? currentIds.filter((id) => id !== enterpriseId)
        : [...currentIds, enterpriseId];
      patchPendingActionParameters({ enterpriseIds: nextIds });
    }

    selectEntity(entityId);
  };

  const undoSelection = () => {
    clearSelection();
    setAssignError("");
  };

  return (
    <div className="space-y-3">
      <TopHud
        state={state}
        legalMoves={legalMoves}
        isSubmitting={isSubmitting}
        isBotUntilLoading={isBotUntilLoading}
        onSubmit={onSubmit}
      />
      {assignError && (
        <div className="rounded-lg border border-red-500/45 bg-red-500/10 px-4 py-2 text-sm text-red-100">
          {assignError}
        </div>
      )}
      <div className="grid gap-4 xl:grid-cols-[300px,minmax(0,1fr),380px] min-[2200px]:grid-cols-[300px,minmax(0,1fr),380px,390px]">
        <div className="xl:sticky xl:top-3 xl:self-start">
          <ClassBoards
            state={state}
            selectedZoneId={selectedZoneId}
            highlightedZones={highlightedZones}
            onSelectZone={(zoneId) => selectZone(zoneId)}
          />
        </div>

        <div className="min-w-0">
          <GameBoard
            viewModel={displayBoardViewModel}
            selectedEntityId={selectedEntityId}
            selectedZoneId={selectedZoneId}
            selectedPolicyId={selectedPolicyId}
            selectedPolicyCourse={selectedPolicyCourse}
            highlightedZones={highlightedZones}
            highlightedEntities={highlightedEntities}
            onSelectZone={(zoneId) => selectZone(zoneId)}
            onSelectEntity={selectEntityOnBoard}
            onSelectPolicyTrack={selectPolicyTrack}
            onSelectPolicyCourse={selectPolicyCourse}
          />
        </div>

        <div className="space-y-4">
          <InteractionSidebar
            state={state}
            selectedEntity={selectedEntity}
            selectedZone={selectedZone}
            canEndTurn={legalMoves.some((move) => move.actionType === "END_TURN")}
            availableInteractions={availableInteractions}
            pendingAction={pendingAction}
            isPreviewing={isPreviewing}
            isSubmitting={isSubmitting}
            lastPreviewResult={lastPreviewResult}
            lastCommandResult={lastCommandResult}
            lastBotSummary={lastBotSummary}
            isBotTurnLoading={isBotTurnLoading}
            isBotUntilLoading={isBotUntilLoading}
            saveFileName={saveFileName}
            setSaveFileName={setSaveFileName}
            isSaving={isSaving}
            isLoading={isLoading}
            isResetting={isResetting}
            isApplyingSetup={isApplyingSetup}
            onStartAction={startAction}
            onCancelPendingAction={cancelPendingAction}
            onPatchPendingParameters={patchPendingActionParameters}
            onSetPendingStep={setPendingActionStep}
            onPreview={onPreview}
            onSubmit={onSubmit}
            onPlayBotTurn={onPlayBotTurn}
            onPlayBotUntilHuman={onPlayBotUntilHuman}
            onEndTurn={() => onSubmit("END_TURN", { actorPlayerId: currentPlayer?.playerId ?? "" })}
            onApplySetup={onApplySetup}
            onSave={onSave}
            onLoad={onLoad}
            onReset={onReset}
            onUndo={undoSelection}
          />
          <div className="min-[2200px]:hidden">
            <TurnSummaryLogPanel state={state} />
          </div>
        </div>

        <div className="hidden min-[2200px]:block">
          <TurnSummaryLogPanel state={state} />
        </div>
      </div>
    </div>
  );
}
