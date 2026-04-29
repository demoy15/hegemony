import { create } from "zustand";
import type { ActionType, BotMoveResponse, CommandResponse, PreviewActionResponse } from "@/types/game";

interface UiState {
  selectedAction: ActionType;
  saveFileName: string;
  lastCommandResult?: CommandResponse;
  lastBotMove?: BotMoveResponse;
  lastPreviewResult?: PreviewActionResponse;
  setSelectedAction: (action: ActionType) => void;
  setSaveFileName: (name: string) => void;
  setLastCommandResult: (result?: CommandResponse) => void;
  setLastBotMove: (result?: BotMoveResponse) => void;
  setLastPreviewResult: (result?: PreviewActionResponse) => void;
}

export const useUiStore = create<UiState>((set) => ({
  selectedAction: "PROPOSE_BILL",
  saveFileName: "demo-save.json",
  setSelectedAction: (selectedAction) => set({ selectedAction }),
  setSaveFileName: (saveFileName) => set({ saveFileName }),
  setLastCommandResult: (lastCommandResult) => set({ lastCommandResult }),
  setLastBotMove: (lastBotMove) => set({ lastBotMove }),
  setLastPreviewResult: (lastPreviewResult) => set({ lastPreviewResult }),
}));
