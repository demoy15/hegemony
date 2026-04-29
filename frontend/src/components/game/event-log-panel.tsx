import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import type { EventLogEntry } from "@/types/game";

interface EventLogPanelProps {
  entries: EventLogEntry[];
}

export function EventLogPanel({ entries }: EventLogPanelProps) {
  const sorted = [...entries].sort((a, b) => b.id - a.id);

  return (
    <Card>
      <CardHeader>
        <CardTitle>Журнал событий</CardTitle>
      </CardHeader>
      <CardContent>
        <div className="max-h-72 space-y-2 overflow-y-auto pr-1">
          {sorted.length === 0 && <p className="text-sm text-muted-foreground">Событий пока нет.</p>}
          {sorted.map((entry) => (
            <div key={entry.id} className="rounded-xl border border-border bg-background/40 p-3">
              <div className="mb-1 flex items-center justify-between text-xs text-muted-foreground">
                <span>{entry.type}</span>
                <span>#{entry.id}</span>
              </div>
              <p className="text-sm">{entry.message}</p>
            </div>
          ))}
        </div>
      </CardContent>
    </Card>
  );
}
