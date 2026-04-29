import { create } from "zustand";
import type { ActionType } from "@/types/game";

export type InteractionMode = "IDLE" | "ENTITY_SELECTED" | "ZONE_SELECTED" | "COMPOSING_ACTION";

export interface PendingActionDraft {
  actionType: ActionType;
  sourceEntityId?: string;
  sourceZoneId?: string;
  parameters: Record<string, unknown>;
  step: number;
}

interface BoardInteractionState {
  selectedEntityId?: string;
  selectedZoneId?: string;
  pendingAction?: PendingActionDraft;
  legalTargets: string[];
  highlightedZones: string[];
  highlightedEntities: string[];
  interactionMode: InteractionMode;
  selectEntity: (entityId?: string) => void;
  selectZone: (zoneId?: string) => void;
  setHighlights: (zones: string[], entities: string[]) => void;
  setLegalTargets: (targets: string[]) => void;
  startPendingAction: (draft: PendingActionDraft) => void;
  patchPendingActionParameters: (patch: Record<string, unknown>) => void;
  setPendingActionStep: (step: number) => void;
  cancelPendingAction: () => void;
  clearSelection: () => void;
}

export const useBoardInteractionStore = create<BoardInteractionState>((set) => ({
  selectedEntityId: undefined,
  selectedZoneId: undefined,
  pendingAction: undefined,
  legalTargets: [],
  highlightedZones: [],
  highlightedEntities: [],
  interactionMode: "IDLE",
  selectEntity: (selectedEntityId) =>
    set((state) => ({
      selectedEntityId,
      selectedZoneId: selectedEntityId ? undefined : state.selectedZoneId,
      interactionMode: selectedEntityId ? "ENTITY_SELECTED" : state.selectedZoneId ? "ZONE_SELECTED" : "IDLE",
    })),
  selectZone: (selectedZoneId) =>
    set((state) => ({
      selectedZoneId,
      selectedEntityId: selectedZoneId ? undefined : state.selectedEntityId,
      interactionMode: selectedZoneId ? "ZONE_SELECTED" : state.selectedEntityId ? "ENTITY_SELECTED" : "IDLE",
    })),
  setHighlights: (highlightedZones, highlightedEntities) => set({ highlightedZones, highlightedEntities }),
  setLegalTargets: (legalTargets) => set({ legalTargets }),
  startPendingAction: (pendingAction) =>
    set({
      pendingAction,
      interactionMode: "COMPOSING_ACTION",
    }),
  patchPendingActionParameters: (patch) =>
    set((state) => {
      if (!state.pendingAction) {
        return state;
      }
      return {
        pendingAction: {
          ...state.pendingAction,
          parameters: {
            ...state.pendingAction.parameters,
            ...patch,
          },
        },
      };
    }),
  setPendingActionStep: (step) =>
    set((state) => {
      if (!state.pendingAction) {
        return state;
      }
      return {
        pendingAction: {
          ...state.pendingAction,
          step,
        },
      };
    }),
  cancelPendingAction: () =>
    set((state) => ({
      pendingAction: undefined,
      interactionMode: state.selectedEntityId ? "ENTITY_SELECTED" : state.selectedZoneId ? "ZONE_SELECTED" : "IDLE",
      legalTargets: [],
      highlightedZones: [],
      highlightedEntities: [],
    })),
  clearSelection: () =>
    set({
      selectedEntityId: undefined,
      selectedZoneId: undefined,
      pendingAction: undefined,
      legalTargets: [],
      highlightedZones: [],
      highlightedEntities: [],
      interactionMode: "IDLE",
    }),
}));
