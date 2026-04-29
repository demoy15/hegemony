import { useEffect, useMemo, useState } from "react";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";
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

const PHASE_SEQUENCE: string[] = ["PREPARATION", "ACTIONS", "PRODUCTION", "VOTING", "SCORING"];
const ROUND_ONE_SEQUENCE: string[] = ["ACTIONS", "PRODUCTION", "VOTING", "SCORING"];
const CLASS_LABEL: Record<string, string> = {
  WORKER: "Рабочий класс",
  MIDDLE_CLASS: "Средний класс",
  CAPITALIST: "Капиталист",
  STATE: "Государство",
};
const CONSUME_ACTIONS: ActionType[] = ["CONSUME_LUXURY", "CONSUME_EDUCATION", "CONSUME_HEALTHCARE"];

interface AssignmentDraft {
  workerId: string;
  targetType: "ENTERPRISE_SLOT" | "UNION" | "UNEMPLOYED";
  targetId: string;
}

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

function parsePolicyIdFromZone(zoneId?: string): PolicyId | undefined {
  if (!zoneId || !zoneId.startsWith("policy:")) {
    return undefined;
  }
  return zoneId.slice("policy:".length) as PolicyId;
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

function classLabel(classType?: string): string {
  if (!classType) {
    return "н/д";
  }
  return CLASS_LABEL[classType] ?? classType;
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
        const available = canAssignWorkersToEnterprise(selectedWorkers, enterprise);
        const nextTone: BoardRenderable["tone"] = available ? "positive" : "danger";
        return { ...renderable, tone: nextTone };
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

  const availableInteractions = useMemo(
    () =>
      buildAvailableInteractions({
        selectedEntity,
        selectedZoneId,
        legalMoves,
        composerMetadata,
      }),
    [selectedEntity, selectedZoneId, legalMoves, composerMetadata],
  );

  const selectedPolicyId = useMemo(() => {
    if (selectedEntity?.sourceRef?.sourceType === "policy") {
      return selectedEntity.sourceRef.sourceId as PolicyId;
    }
    return parsePolicyIdFromZone(selectedZoneId);
  }, [selectedEntity, selectedZoneId]);

  const selectedPolicyCourse = useMemo(() => {
    if (pendingAction?.actionType === "PROPOSE_BILL") {
      return pendingAction.parameters.targetCourse as PolicyCourse | undefined;
    }
    return undefined;
  }, [pendingAction]);

  useEffect(() => {
    if (lastCommandResult?.accepted) {
      clearSelection();
      setAssignError("");
      const nextState = lastCommandResult.gameState;
      const voteStage = nextState.currentVoteState?.votingStage;
      const nextActionType =
        voteStage === "COMMIT_INFLUENCE"
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
      const policyZoneId = selectedEntity?.sourceRef?.sourceType === "policy" ? `policy:${selectedEntity.sourceRef.sourceId}` : selectedZoneId;
      nextZones = ["policy_track", ...boardViewModel.policyTracks.map((track) => track.zoneId)];
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
    highlightedZones,
    highlightedEntities,
    legalTargets,
    setHighlights,
    setLegalTargets,
  ]);

  const currentPlayerIndex = Number.isInteger(state.turnOrder?.currentPlayerIndex)
    ? Number(state.turnOrder.currentPlayerIndex)
    : 0;
  const currentPlayer = Array.isArray(state.players) ? state.players[currentPlayerIndex] : undefined;
  const turnOrderMeta = state.turnOrder as unknown as { actionsPerPlayer?: number; actionsTakenByPlayer?: unknown };
  const actionsPerPlayer = Number.isInteger(turnOrderMeta.actionsPerPlayer) ? Number(turnOrderMeta.actionsPerPlayer) : 5;
  const actionsTakenByPlayer = Array.isArray(turnOrderMeta.actionsTakenByPlayer)
    ? turnOrderMeta.actionsTakenByPlayer.map((value) => (Number.isInteger(value) ? Number(value) : 0))
    : [];
  const actionsTakenCurrent = actionsTakenByPlayer[currentPlayerIndex] ?? 0;
  const actionSlot = state.currentPhase === "ACTIONS" ? Math.min(actionsPerPlayer, actionsTakenCurrent + 1) : actionsTakenCurrent;
  const lastEvent = Array.isArray(state.eventLog) && state.eventLog.length > 0 ? state.eventLog[state.eventLog.length - 1] : undefined;
  const { index: phaseIndex, total: phaseTotal } = phaseProgress(state);
  const victoryTrackPlayers = Array.isArray(state.players) ? state.players : [];

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
      parameters: {
        actorPlayerId,
        policyId,
        targetCourse: course,
      },
      step: 2,
    });
  };

  const undoSelection = () => {
    clearSelection();
    setAssignError("");
  };

  const selectEntityOnBoard = (entityId: string) => {
    const entity = boardViewModel.renderables.find((item) => item.id === entityId);
    if (!entity) {
      selectEntity(entityId);
      return;
    }

    const actorPlayerId = composerMetadata.actorPlayerId || currentPlayer?.playerId || state.players[0]?.playerId || "";
    const actorClassType = state.players.find((player) => player.playerId === actorPlayerId)?.classType;

    if (!pendingAction && entity.sourceRef?.sourceType === "worker" && legalMoves.some((move) => move.actionType === "ASSIGN_WORKERS")) {
      const sourceRef = entity.sourceRef;
      if (!sourceRef) {
        selectEntity(entityId);
        return;
      }
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
        if (!sourceRef) {
          selectEntity(entityId);
          return;
        }
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
        if (!alreadySelected && selectedWorkerIds.length >= 3) {
          setAssignError("Недопустимо: за одно действие можно выбрать максимум 3 рабочих.");
          selectEntity(entityId);
          return;
        }
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

          const nextAssignments = [...retainedAssignments, ...newAssignments].slice(0, 3);
          patchPendingActionParameters({
            assignments: nextAssignments,
            selectedWorkerIds: [],
            selectedWorkerId: "",
          });
          setPendingActionStep(Math.max(2, pendingAction.step));
          setAssignError("");
        }
      }
    }

    selectEntity(entityId);
  };

  return (
    <div className="space-y-3">
      <Card className="border-zinc-700/80 bg-gradient-to-r from-zinc-950 via-zinc-900 to-zinc-950">
        <CardContent className="grid gap-2 p-3 sm:grid-cols-[1fr,1fr,1fr,1fr,2fr]">
          <div>
            <p className="text-[10px] uppercase tracking-[0.18em] text-zinc-400">Раунд</p>
            <p className="text-xl font-semibold text-zinc-100">
              {state.currentRound}/{state.maxRounds}
            </p>
          </div>
          <div>
            <p className="text-[10px] uppercase tracking-[0.18em] text-zinc-400">Фаза</p>
            <p className="text-xl font-semibold text-zinc-100">
              {phaseLabel(state.currentPhase)}{" "}
              <span className="text-sm text-zinc-400">
                {state.currentPhase === "ACTIONS" ? `(${Math.max(1, actionSlot)}/${actionsPerPlayer})` : `(${phaseIndex}/${phaseTotal})`}
              </span>
            </p>
          </div>
          <div>
            <p className="text-[10px] uppercase tracking-[0.18em] text-zinc-400">Активный игрок</p>
            <p className="text-lg font-semibold text-zinc-100">
              {classLabel(currentPlayer?.classType)}
              {currentPlayer ? ` (${currentPlayer.controlMode})` : ""}
            </p>
          </div>
          <div>
            <p className="text-[10px] uppercase tracking-[0.18em] text-zinc-400">Налоги</p>
            <p className="text-lg font-semibold text-zinc-100">
              x{state.taxMultiplier}
              <span className="text-sm text-zinc-400">
                {" "}
                (
                {state.policies.find((policy) => policy.id === "POLICY_3_TAXATION")?.currentCourse ?? "B"})
              </span>
            </p>
          </div>
          <div className="flex items-center justify-between gap-3 rounded-lg border border-zinc-700/70 bg-black/25 px-3 py-2">
            <div className="min-w-0">
              <p className="text-[10px] uppercase tracking-[0.16em] text-zinc-400">Последнее действие</p>
              <p className="truncate text-sm text-zinc-200">{lastEvent?.message ?? "Действий пока не было"}</p>
              {assignError && <p className="mt-1 text-xs text-red-300">{assignError}</p>}
            </div>
            <Badge tone="positive">Источник истины: бэкенд</Badge>
          </div>
          <div className="sm:col-span-5 rounded-lg border border-zinc-700/70 bg-black/25 px-3 py-2">
            <p className="text-[10px] uppercase tracking-[0.16em] text-zinc-400">Общий трек победных очков</p>
            <div className="mt-2 grid gap-2 sm:grid-cols-2 lg:grid-cols-4">
              {victoryTrackPlayers.map((player) => (
                <div key={player.playerId} className="rounded-md border border-zinc-700/70 bg-black/30 px-3 py-2">
                  <p className="text-xs text-zinc-400">{classLabel(player.classType)}</p>
                  <p className="text-sm font-semibold text-zinc-100">ПО: {player.victoryPoints ?? 0}</p>
                </div>
              ))}
            </div>
          </div>
        </CardContent>
      </Card>

      <div className="grid gap-4 xl:grid-cols-[300px,minmax(0,1fr),360px] min-[2100px]:grid-cols-[300px,minmax(0,1fr),360px,390px]">
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
          <div className="min-[2100px]:hidden">
            <TurnSummaryLogPanel state={state} />
          </div>
        </div>

        <div className="hidden min-[2100px]:block">
          <TurnSummaryLogPanel state={state} />
        </div>
      </div>
    </div>
  );
}
