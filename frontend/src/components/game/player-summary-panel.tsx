import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import type { GameState } from "@/types/game";
import { Badge } from "@/components/ui/badge";

interface PlayerSummaryPanelProps {
  state: GameState;
}

export function PlayerSummaryPanel({ state }: PlayerSummaryPanelProps) {
  return (
    <Card>
      <CardHeader>
        <CardTitle>Players</CardTitle>
      </CardHeader>
      <CardContent className="space-y-3">
        {state.players.map((player, index) => {
          const isCurrent = index === state.turnOrder.currentPlayerIndex;
          const availableTokens = player.proposalTokens.filter((token) => token.available).length;
          return (
            <div
              key={player.playerId}
              className={`rounded-xl border p-3 ${
                isCurrent ? "border-emerald-400/50 bg-emerald-500/10" : "border-border bg-background/30"
              }`}
            >
              <div className="mb-2 flex items-center justify-between">
                <h4 className="font-semibold">{player.classType}</h4>
                {isCurrent ? <Badge tone="positive">Current</Badge> : <Badge>Waiting</Badge>}
              </div>
              <div className="mb-2 flex flex-wrap gap-2">
                <Badge tone={player.controlMode === "BOT" ? "warning" : "positive"}>{player.controlMode}</Badge>
                <Badge>{player.botStrategyMode}</Badge>
              </div>
              <div className="grid grid-cols-2 gap-2 text-sm text-muted-foreground">
                <p>Money: ${player.money}</p>
                <p>Revenue: {player.revenue}</p>
                <p>Capital: {player.capital}</p>
                <p>Influence: {player.influence}</p>
                <p>Population: {player.population}</p>
                <p>Welfare: {player.welfare} ({player.lastWelfareDelta >= 0 ? "+" : ""}{player.lastWelfareDelta})</p>
                <p>VP: {player.victoryPoints}</p>
                <p>Proposal tokens: {availableTokens}</p>
                <p>Workers free: {player.availableWorkers}</p>
                <p>Workers employed: {player.employedWorkers}</p>
                <p>Enterprises: {player.enterprises}</p>
              </div>
              <p className="mt-2 text-xs text-muted-foreground">Goods/services: {JSON.stringify(player.goodsAndServicesArea ?? {})}</p>
              <p className="text-xs text-muted-foreground">Produced storage: {JSON.stringify(player.producedResourceStorage ?? {})}</p>
            </div>
          );
        })}
      </CardContent>
    </Card>
  );
}
