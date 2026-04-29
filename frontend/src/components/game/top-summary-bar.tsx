import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";
import type { GameState } from "@/types/game";

interface TopSummaryBarProps {
  state: GameState;
}

export function TopSummaryBar({ state }: TopSummaryBarProps) {
  const current = state.players[state.turnOrder.currentPlayerIndex];
  const round = state.currentRound ?? state.turnOrder.round;
  const phase = state.currentPhase ?? state.turnOrder.phase;
  const maxRounds = state.maxRounds ?? 5;
  const status = state.gameStatus ?? "IN_PROGRESS";
  const isGameOver = state.gameOver || phase === "GAME_OVER" || status === "FINISHED";

  return (
    <Card className="border-white/10 bg-gradient-to-r from-zinc-900/90 via-zinc-900/80 to-emerald-900/60">
      <CardContent className="space-y-3 p-4">
        {isGameOver && (
          <div className="rounded-lg border border-amber-500/40 bg-amber-500/10 px-3 py-2 text-sm font-semibold text-amber-200">
            GAME OVER
          </div>
        )}
        <div className="grid gap-3 sm:grid-cols-5">
        <div>
          <p className="text-xs uppercase tracking-[0.12em] text-muted-foreground">Round</p>
          <p className="text-2xl font-extrabold text-foreground">
            {round}/{maxRounds}
          </p>
        </div>
        <div>
          <p className="text-xs uppercase tracking-[0.12em] text-muted-foreground">Phase</p>
          <p className="text-lg font-semibold">{phase}</p>
        </div>
        <div>
          <p className="text-xs uppercase tracking-[0.12em] text-muted-foreground">Game Status</p>
          <p className="text-lg font-semibold">{status}</p>
        </div>
        <div>
          <p className="text-xs uppercase tracking-[0.12em] text-muted-foreground">Current Player</p>
          <p className="text-lg font-semibold">
            {current?.classType ?? "n/a"} {current ? `(${current.controlMode})` : ""}
          </p>
        </div>
        <div className="flex items-center justify-end">
          <Badge tone="positive" className="ml-3">
            Backend Authoritative
          </Badge>
        </div>
        </div>
      </CardContent>
    </Card>
  );
}
