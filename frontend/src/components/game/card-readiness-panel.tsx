import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import type { CardReadinessState } from "@/types/game";

interface CardReadinessPanelProps {
  readiness: CardReadinessState;
}

export function CardReadinessPanel({ readiness }: CardReadinessPanelProps) {
  const capitalistAutomaInstalled = readiness.simpleAutomaCardDatasetInstalledByClass?.CAPITALIST ?? false;

  return (
    <Card>
      <CardHeader>
        <CardTitle>Card Readiness</CardTitle>
        <CardDescription>Current card-driven capabilities and fallback transparency.</CardDescription>
      </CardHeader>
      <CardContent className="space-y-3">
        <div className="flex items-center justify-between">
          <span className="text-sm">Enterprise card dataset</span>
          <Badge tone={readiness.enterpriseCardDatasetInstalled ? "positive" : "warning"}>
            {readiness.enterpriseCardDatasetInstalled ? "installed" : "not installed"}
          </Badge>
        </div>
        <div className="flex items-center justify-between">
          <span className="text-sm">Capitalist simple automa cards</span>
          <Badge tone={capitalistAutomaInstalled ? "positive" : "warning"}>
            {capitalistAutomaInstalled ? "installed" : "not installed"}
          </Badge>
        </div>
        <div className="rounded-xl border border-border/80 p-3">
          <p className="text-xs uppercase tracking-wide text-muted-foreground">Bot behavior now</p>
          <p className="text-sm">Heuristic fallback is active when card datasets are absent.</p>
        </div>
      </CardContent>
    </Card>
  );
}
