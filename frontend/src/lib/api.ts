import type {
  ActionType,
  BotTurnResponse,
  BotUntilHumanResponse,
  BotMoveResponse,
  CommandResponse,
  GameResponse,
  LegalMove,
  PreviewActionResponse,
  SaveLoadResponse,
} from "@/types/game";

export interface CommandPayload {
  actionType: ActionType;
  parameters: Record<string, unknown>;
}

export interface SetupPayload {
  playerCount: number;
  controlModes: Record<string, string>;
  botStrategyModes: Record<string, string>;
}

export interface PreviewPayload extends CommandPayload {
  optionalModifier?: string;
  optionalCardReference?: string;
}

async function http<T>(url: string, init?: RequestInit): Promise<T> {
  const response = await fetch(url, {
    headers: {
      "Content-Type": "application/json",
      ...(init?.headers ?? {}),
    },
    ...init,
  });

  if (!response.ok) {
    const text = await response.text();
    throw new Error(text || `Request failed with status ${response.status}`);
  }
  return (await response.json()) as T;
}

export const gameApi = {
  getGame: () => http<GameResponse>("/api/game"),
  getLegalMoves: () => http<{ legalMoves: LegalMove[] }>("/api/game/legal-moves"),
  resetGame: () => http<GameResponse>("/api/game/reset", { method: "POST" }),
  setupGame: (payload: SetupPayload) =>
    http<GameResponse>("/api/game/setup", {
      method: "POST",
      body: JSON.stringify(payload),
    }),
  submitCommand: (payload: CommandPayload) =>
    http<CommandResponse>("/api/game/command", {
      method: "POST",
      body: JSON.stringify(payload),
    }),
  previewAction: (payload: PreviewPayload) =>
    http<PreviewActionResponse>("/api/game/preview", {
      method: "POST",
      body: JSON.stringify(payload),
    }),
  botMove: () => http<BotMoveResponse>("/api/game/bot-move", { method: "POST" }),
  playBotTurn: () => http<BotTurnResponse>("/api/game/play-bot-turn", { method: "POST" }),
  playBotUntilHuman: () => http<BotUntilHumanResponse>("/api/game/play-bot-until-human", { method: "POST" }),
  saveGame: (fileName: string) =>
    http<SaveLoadResponse>("/api/game/save", {
      method: "POST",
      body: JSON.stringify({ fileName }),
    }),
  loadGame: (fileName: string) =>
    http<SaveLoadResponse>("/api/game/load", {
      method: "POST",
      body: JSON.stringify({ fileName }),
    }),
};
