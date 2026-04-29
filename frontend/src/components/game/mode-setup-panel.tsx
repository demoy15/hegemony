import { useEffect, useMemo, useState } from "react";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Select } from "@/components/ui/select";
import { Button } from "@/components/ui/button";
import type { BotStrategyMode, ClassType, GameState, PlayerControlMode } from "@/types/game";

interface ModeSetupPanelProps {
  state: GameState;
  isApplying: boolean;
  onApply: (payload: {
    playerCount: number;
    controlModes: Record<string, string>;
    botStrategyModes: Record<string, string>;
  }) => void;
}

const PLAYER_CLASS_ORDER_BY_COUNT: Record<number, ClassType[]> = {
  2: ["WORKER", "CAPITALIST"],
  3: ["WORKER", "CAPITALIST", "MIDDLE_CLASS"],
  4: ["WORKER", "CAPITALIST", "MIDDLE_CLASS", "STATE"],
};

function classLabel(classType: ClassType): string {
  if (classType === "WORKER") {
    return "Worker";
  }
  if (classType === "CAPITALIST") {
    return "Capitalist";
  }
  if (classType === "MIDDLE_CLASS") {
    return "Middle Class";
  }
  return "State";
}

function initialStrategyForControlMode(controlMode: PlayerControlMode): BotStrategyMode {
  return controlMode === "BOT" ? "CARD_DRIVEN_SIMPLE_AUTOMA" : "HEURISTIC_FALLBACK";
}

export function ModeSetupPanel({ state, isApplying, onApply }: ModeSetupPanelProps) {
  const activeClasses = useMemo(
    () => state.turnOrder.activeClasses ?? state.players.map((player) => player.classType),
    [state.turnOrder.activeClasses, state.players],
  );
  const normalizedInitialCount = Math.max(2, Math.min(4, activeClasses.length || 4));
  const [playerCount, setPlayerCount] = useState(String(normalizedInitialCount));
  const [controlModes, setControlModes] = useState<Record<string, PlayerControlMode>>({});
  const [strategyModes, setStrategyModes] = useState<Record<string, BotStrategyMode>>({});

  const selectedCount = Math.max(2, Math.min(4, Number(playerCount) || normalizedInitialCount));
  const editableClasses = PLAYER_CLASS_ORDER_BY_COUNT[selectedCount];

  useEffect(() => {
    const nextControl: Record<string, PlayerControlMode> = {};
    const nextStrategy: Record<string, BotStrategyMode> = {};
    state.players.forEach((player) => {
      nextControl[player.classType] = player.controlMode;
      nextStrategy[player.classType] = player.botStrategyMode;
    });
    setControlModes(nextControl);
    setStrategyModes(nextStrategy);
    const nextCount = Math.max(2, Math.min(4, activeClasses.length || 4));
    setPlayerCount(String(nextCount));
  }, [state.players, activeClasses.length]);

  const apply = () => {
    const filteredControlModes = editableClasses.reduce<Record<string, PlayerControlMode>>((acc, classType) => {
      acc[classType] = controlModes[classType] ?? "HUMAN";
      return acc;
    }, {});
    const filteredStrategyModes = editableClasses.reduce<Record<string, BotStrategyMode>>((acc, classType) => {
      acc[classType] = controlModes[classType] === "BOT" ? "CARD_DRIVEN_SIMPLE_AUTOMA" : "HEURISTIC_FALLBACK";
      return acc;
    }, {});

    onApply({
      playerCount: selectedCount,
      controlModes: filteredControlModes,
      botStrategyModes: filteredStrategyModes,
    });
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle>Game Setup Mode</CardTitle>
        <CardDescription>Choose which classes are human-controlled or bot-controlled for this run.</CardDescription>
      </CardHeader>
      <CardContent className="space-y-3">
        <div className="space-y-2">
          <label className="text-xs uppercase tracking-wide text-muted-foreground">Player count</label>
          <Select value={playerCount} onChange={(e) => setPlayerCount(e.target.value)}>
            <option value="2">2</option>
            <option value="3">3</option>
            <option value="4">4</option>
          </Select>
        </div>
        <div className="space-y-2">
          {editableClasses.map((classType) => {
            const selectedControl = controlModes[classType] ?? "HUMAN";
            return (
              <div key={classType} className="grid grid-cols-[110px,1fr,1fr] items-center gap-2">
                <span className="text-xs font-semibold uppercase tracking-wide text-zinc-300">{classLabel(classType)}</span>
                <Select
                  value={selectedControl}
                  onChange={(e) => {
                    const nextControlMode = e.target.value as PlayerControlMode;
                    setControlModes((prev) => ({ ...prev, [classType]: nextControlMode }));
                    setStrategyModes((prev) => ({ ...prev, [classType]: initialStrategyForControlMode(nextControlMode) }));
                  }}
                >
                  <option value="HUMAN">HUMAN</option>
                  <option value="BOT">BOT</option>
                </Select>
                <Select
                  value={selectedControl === "BOT" ? "CARD_DRIVEN_SIMPLE_AUTOMA" : "HEURISTIC_FALLBACK"}
                  onChange={(e) => setStrategyModes((prev) => ({ ...prev, [classType]: e.target.value as BotStrategyMode }))}
                  disabled={selectedControl !== "BOT"}
                >
                  <option value="CARD_DRIVEN_SIMPLE_AUTOMA">CARD_AUTOMA</option>
                  {selectedControl !== "BOT" && <option value="HEURISTIC_FALLBACK">HUMAN</option>}
                </Select>
              </div>
            );
          })}
        </div>
        <Button onClick={apply} disabled={isApplying} className="w-full">
          {isApplying ? "Applying..." : "Apply Setup"}
        </Button>
      </CardContent>
    </Card>
  );
}
