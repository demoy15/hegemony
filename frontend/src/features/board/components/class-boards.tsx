import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { CLASS_BOARD_LAYOUTS } from "@/features/board/layout/class-boards-layout";
import { cn } from "@/lib/utils";
import type { ClassType, GameState, PlayerState } from "@/types/game";

interface ClassBoardsProps {
  state: GameState;
  selectedZoneId?: string;
  highlightedZones: string[];
  onSelectZone: (zoneId: string) => void;
}

function findPlayerByClass(state: GameState, classType: ClassType): PlayerState | undefined {
  const players = Array.isArray(state.players) ? state.players : [];
  return players.find((player) => player.classType === classType);
}

function boardZoneId(classType: ClassType): string {
  return `class-board:${classType}`;
}

function classTitle(classType: ClassType): string {
  if (classType === "WORKER") {
    return "Рабочий класс";
  }
  if (classType === "CAPITALIST") {
    return "Капиталисты";
  }
  if (classType === "MIDDLE_CLASS") {
    return "Средний класс";
  }
  return "Государство";
}

function amount(storage: Record<string, number> | undefined, resourceId: string): number {
  return storage?.[resourceId] ?? storage?.[resourceId.toUpperCase()] ?? 0;
}

function resourceLine(storage: Record<string, number> | undefined): string {
  const influence = amount(storage, "influence") + amount(storage, "media_influence");
  return [
    `Еда ${amount(storage, "food")}`,
    `Роскошь ${amount(storage, "luxury")}`,
    `Мед ${amount(storage, "healthcare")}`,
    `Обр ${amount(storage, "education")}`,
    `Влияние ${influence}`,
  ].join(" / ");
}

function priceLine(prices: Record<string, number> | undefined): string {
  return [
    `Е-${amount(prices, "food") || "-"}`,
    `Р-${amount(prices, "luxury") || "-"}`,
    `М-${amount(prices, "healthcare") || "-"}`,
    `О-${amount(prices, "education") || "-"}`,
  ].join(" / ");
}

function classSummaryLines(state: GameState, classType: ClassType): string[] {
  const player = findPlayerByClass(state, classType);
  if (!player) {
    return ["Не участвует в текущей партии"];
  }

  if (classType === "WORKER") {
    const workers = Array.isArray(state.workers) ? state.workers : [];
    const workerCount = workers.filter((worker) => worker.classType === "WORKER").length;
    const unions = workers.filter((worker) => worker.classType === "WORKER" && worker.location === "UNION").length;
    return [
      `Население ${player.population} | Рабочих ${workerCount}`,
      `Благосост. ${player.welfare}`,
      `Деньги ${player.money} | Влияние ${player.influence ?? 0}`,
      `Еда ${amount(player.goodsAndServicesArea, "food")} / Мед ${amount(player.goodsAndServicesArea, "healthcare")} / Обр ${amount(player.goodsAndServicesArea, "education")} / Роскошь ${amount(player.goodsAndServicesArea, "luxury")}`,
      `Профсоюзы ${unions}`,
    ];
  }

  if (classType === "CAPITALIST") {
    return [
      `Доход ${player.revenue}`,
      `Капитал ${player.capital} | Влияние ${player.influence ?? 0}`,
      `ПО ${player.victoryPoints}`,
      resourceLine(player.producedResourceStorage),
      `Цены ${priceLine(player.prices)}`,
    ];
  }

  if (classType === "MIDDLE_CLASS") {
    return [
      `Население ${player.population}`,
      `Благосост. ${player.welfare}`,
      `Деньги ${player.money} | Влияние ${player.influence ?? 0}`,
      `Еда ${amount(player.goodsAndServicesArea, "food")} / Обр ${amount(player.goodsAndServicesArea, "education")}`,
      `Цены Е-${amount(player.prices, "food") || "-"} / О-${amount(player.prices, "education") || "-"}`,
    ];
  }

  return [
    `Казна ${state.treasury}`,
    `Легит. Р${player.legitimacyWorker}/С${player.legitimacyMiddleClass}/К${player.legitimacyCapitalist}`,
    `Влияние ${player.influence}`,
    `Госуслуги МЕД ${state.publicServices.healthcare}/ОБР ${state.publicServices.education}/МЕДИА ${state.publicServices.mediaInfluence}`,
    `Запасы МЕД ${state.publicServicesStorage?.healthcare ?? 0}/ОБР ${state.publicServicesStorage?.education ?? 0}/МЕДИА ${state.publicServicesStorage?.media_influence ?? 0}`,
  ];
}

export function ClassBoards({ state, selectedZoneId, highlightedZones, onSelectZone }: ClassBoardsProps) {
  return (
    <div className="grid grid-cols-1 gap-3">
      {CLASS_BOARD_LAYOUTS.map((layout) => {
        const zoneId = boardZoneId(layout.boardId);
        const active = zoneId === selectedZoneId;
        const highlighted = highlightedZones.includes(zoneId);
        const lines = classSummaryLines(state, layout.boardId);
        const player = findPlayerByClass(state, layout.boardId);

        return (
          <Card
            key={layout.boardId}
            className={cn(
              "h-full border border-zinc-700/80 bg-gradient-to-br from-zinc-950 via-zinc-900/90 to-zinc-950 transition-colors",
              layout.accentClass,
              active ? "ring-2 ring-white/70" : highlighted ? "ring-2 ring-emerald-400/80" : "ring-0",
            )}
          >
            <CardHeader className="pb-2 pt-3">
              <CardTitle className="flex items-center justify-between gap-2 text-sm uppercase tracking-[0.18em]">
                <button type="button" onClick={() => onSelectZone(zoneId)} className="min-w-0 break-words text-left hover:opacity-90">
                  {classTitle(layout.boardId)}
                </button>
                {player ? <Badge tone="positive">{player.controlMode}</Badge> : <Badge tone="warning">Неактивен</Badge>}
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-2 pb-3">
              {layout.sections.map((section, index) => (
                <button
                  key={section.id}
                  type="button"
                  onClick={() => onSelectZone(`${zoneId}:${section.id}`)}
                  className="grid w-full grid-cols-[88px,minmax(0,1fr)] items-start gap-2 rounded-xl border border-zinc-700/70 bg-black/30 px-3 py-2.5 text-left text-xs transition-colors hover:border-zinc-500/80 hover:bg-black/40"
                >
                  <span className="break-words uppercase tracking-wide text-zinc-400">{section.label}</span>
                  <span className="min-w-0 break-words font-medium leading-snug text-zinc-100">{lines[index] ?? "н/д"}</span>
                </button>
              ))}
            </CardContent>
          </Card>
        );
      })}
    </div>
  );
}
