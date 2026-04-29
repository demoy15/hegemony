import { Bot, Sparkles } from "lucide-react";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import type { BotTurnSummary } from "@/types/game";

interface BotResponsePanelProps {
  lastBotSummary?: BotTurnSummary;
  isTurnLoading: boolean;
  isUntilLoading: boolean;
  onPlayBotTurn: () => void;
  onPlayBotUntilHuman: () => void;
}

function stringifyUnknown(value: unknown): string {
  if (value === null || value === undefined) {
    return "none";
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

export function BotResponsePanel({
  lastBotSummary,
  isTurnLoading,
  isUntilLoading,
  onPlayBotTurn,
  onPlayBotUntilHuman,
}: BotResponsePanelProps) {
  const currentCardNo = lastBotSummary?.automaTrace?.currentCardNo;
  const mode = lastBotSummary?.automaTrace?.mode;
  const checksTrace = lastBotSummary?.automaTrace?.checksTrace;
  const checksTraceCount = Array.isArray(checksTrace) ? checksTrace.length : 0;
  const hasCardNo = currentCardNo !== undefined && currentCardNo !== null && String(currentCardNo).length > 0;
  const modeText = mode === undefined || mode === null ? "SIMPLE" : String(mode);
  const chosenTargetsText = stringifyUnknown(lastBotSummary?.chosenTargets);

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Bot size={18} />
          Bot Turn Controls
        </CardTitle>
        <CardDescription>Bot executes only legal moves. If card data is absent, heuristic fallback mode is explicit.</CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="grid gap-2 sm:grid-cols-2">
          <Button onClick={onPlayBotTurn} disabled={isTurnLoading}>
            <Sparkles size={16} className="mr-2" />
            {isTurnLoading ? "Running..." : "Play bot turn"}
          </Button>
          <Button variant="secondary" onClick={onPlayBotUntilHuman} disabled={isUntilLoading}>
            {isUntilLoading ? "Running..." : "Play until my turn"}
          </Button>
        </div>

        {!lastBotSummary && <p className="text-sm text-muted-foreground">No bot turn summary yet.</p>}

        {lastBotSummary && (
          <div className="space-y-3 rounded-xl border border-border bg-background/40 p-3">
            <div className="flex flex-wrap gap-2">
              <Badge tone={lastBotSummary.fallbackHeuristicMode ? "warning" : "positive"}>
                {lastBotSummary.fallbackHeuristicMode ? "heuristic fallback" : "card-driven"}
              </Badge>
              <Badge tone="positive">{lastBotSummary.strategyModeUsed}</Badge>
            </div>
            <p className="text-sm">
              <span className="text-muted-foreground">Class:</span> <span className="font-semibold">{lastBotSummary.actingClass}</span>
            </p>
            <p className="text-sm">
              <span className="text-muted-foreground">Action:</span> <span className="font-semibold">{lastBotSummary.selectedAction}</span>
            </p>
            <p className="text-sm">
              <span className="text-muted-foreground">Targets:</span> {chosenTargetsText}
            </p>
            {hasCardNo && (
              <p className="text-sm">
                <span className="text-muted-foreground">Automa card:</span> #{String(currentCardNo)}{" "}
                ({modeText})
              </p>
            )}
            {checksTraceCount > 0 && (
              <p className="text-sm">
                <span className="text-muted-foreground">Checks fired:</span> {checksTraceCount}
              </p>
            )}
            <p className="text-sm">{lastBotSummary.rationale}</p>
            <p className="text-xs text-muted-foreground">
              Planner: {lastBotSummary.plannerId} | Legal options: {lastBotSummary.legalOptionsConsidered}
            </p>
            <div className="space-y-1">
              <p className="text-xs uppercase tracking-wide text-muted-foreground">State changes to apply physically</p>
              {lastBotSummary.eventSummaries.length === 0 && <p className="text-sm text-muted-foreground">No events.</p>}
              {lastBotSummary.eventSummaries.map((event, idx) => (
                <p key={`${event}-${idx}`} className="rounded-lg bg-zinc-900/70 px-2 py-1 text-sm">
                  {event}
                </p>
              ))}
            </div>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
