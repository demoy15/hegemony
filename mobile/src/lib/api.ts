import { createLocalGameApi } from "@/lib/local-game-engine";

export interface CommandPayload {
  actionType: string;
  parameters: Record<string, unknown>;
}

export interface PreviewPayload extends CommandPayload {
  optionalModifier?: string;
  optionalCardReference?: string;
}

export interface SetupPayload {
  playerCount: number;
  controlModes: Record<string, string>;
  botStrategyModes: Record<string, string>;
}

export function defaultApiBase(): string {
  return "offline";
}

export function normalizeApiBase(_value?: string): string {
  return "offline";
}

export function loadStoredApiBase(): string {
  return "offline";
}

export function storeApiBase(): string {
  return "offline";
}

export function createGameApi(_apiBase?: string) {
  return createLocalGameApi();
}
