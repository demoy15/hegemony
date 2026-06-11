import type React from "react";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { MeepleIcon, PriceBadge, ResourceAmount, ResourceIcon, type ResourceKind } from "@/features/board/components/board-visual-primitives";
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

function SectionFrame({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="rounded-md border border-[#8f6b35]/55 bg-black/25 px-2.5 py-2">
      <p className="mb-2 text-[10px] uppercase text-[#c4a56a]">{label}</p>
      {children}
    </div>
  );
}

function MiniTrack({ value, max = 10 }: { value: number; max?: number }) {
  return (
    <div className="grid grid-cols-10 gap-0.5">
      {Array.from({ length: 10 }, (_, index) => {
        const active = index < Math.min(max, Math.max(0, value));
        return <span key={index} className={cn("h-5 rounded-sm border border-[#8f6b35]/45", active ? "bg-[#d8b56b]/70" : "bg-black/25")} />;
      })}
    </div>
  );
}

function ResourceGrid({ storage, keys }: { storage: Record<string, number> | undefined; keys: Array<{ id: string; kind: ResourceKind; label: string }> }) {
  return (
    <div className="grid grid-cols-2 gap-2">
      {keys.map((item) => (
        <ResourceAmount key={item.id} kind={item.kind} amount={amount(storage, item.id)} label={item.label} />
      ))}
    </div>
  );
}

function PriceGrid({ prices, keys }: { prices: Record<string, number> | undefined; keys: Array<{ id: string; kind: ResourceKind; label: string }> }) {
  return (
    <div className="grid grid-cols-2 gap-2">
      {keys.map((item) => (
        <div key={item.id} className="flex items-center justify-between rounded-md border border-[#8f6b35]/55 bg-black/25 px-2 py-1.5">
          <ResourceIcon kind={item.kind} className="h-6 w-6" />
          <PriceBadge value={amount(prices, item.id) || "-"} />
        </div>
      ))}
    </div>
  );
}

function ClassBoardVisualSection({
  state,
  classType,
  sectionId,
  label,
  player,
  fallback,
}: {
  state: GameState;
  classType: ClassType;
  sectionId: string;
  label: string;
  player?: PlayerState;
  fallback: string;
}) {
  if (!player) {
    return <SectionFrame label={label}><p className="text-xs text-zinc-400">{fallback}</p></SectionFrame>;
  }

  if (sectionId === "population") {
    const workerCount = state.workers.filter((worker) => worker.classType === classType).length;
    return (
      <SectionFrame label={label}>
        <div className="flex items-center justify-between gap-2">
          <div>
            <p className="text-[10px] uppercase text-zinc-400">рабочие</p>
            <span className="font-serif text-3xl leading-none text-[#f1d38a]">{workerCount}</span>
          </div>
          <div className="text-right">
            <p className="text-[10px] uppercase text-[#c4a56a]">население</p>
            <span className="font-serif text-3xl leading-none text-[#f1d38a]">{player.population}</span>
          </div>
        </div>
      </SectionFrame>
    );
  }

  if (sectionId === "welfare") {
    return (
      <SectionFrame label={label}>
        <div className="mb-1 flex items-end justify-between">
          <span className="text-[10px] uppercase text-zinc-400">уровень</span>
          <span className="font-serif text-2xl leading-none text-[#f1d38a]">{player.welfare ?? 0}</span>
        </div>
        <MiniTrack value={player.welfare ?? 0} />
      </SectionFrame>
    );
  }

  if (sectionId === "income" || sectionId === "revenue" || sectionId === "capital" || sectionId === "treasury") {
    const value = sectionId === "revenue" ? player.revenue : sectionId === "capital" ? player.capital : sectionId === "treasury" ? state.treasury : player.money;
    return (
      <SectionFrame label={label}>
        <div className="flex items-center justify-between gap-3">
          <ResourceIcon kind="money" className="h-11 w-11" />
          <span className="font-serif text-4xl leading-none text-[#f1d38a]">{value}</span>
        </div>
      </SectionFrame>
    );
  }

  if (sectionId === "goods") {
    const storage = classType === "STATE" ? state.publicServicesStorage : player.goodsAndServicesArea;
    return (
      <SectionFrame label={label}>
        <ResourceGrid
          storage={storage}
          keys={[
            { id: "food", kind: "food", label: "еда" },
            { id: "healthcare", kind: "healthcare", label: "мед" },
            { id: "education", kind: "education", label: "обр" },
            { id: "luxury", kind: "luxury", label: "роск" },
          ]}
        />
      </SectionFrame>
    );
  }

  if (sectionId === "storage") {
    return (
      <SectionFrame label={label}>
        <ResourceGrid
          storage={player.producedResourceStorage}
          keys={[
            { id: "food", kind: "food", label: "еда" },
            { id: "luxury", kind: "luxury", label: "роск" },
            { id: "healthcare", kind: "healthcare", label: "мед" },
            { id: "education", kind: "education", label: "обр" },
          ]}
        />
      </SectionFrame>
    );
  }

  if (sectionId === "prices") {
    return (
      <SectionFrame label={label}>
        <PriceGrid
          prices={player.prices}
          keys={[
            { id: "food", kind: "food", label: "еда" },
            { id: "luxury", kind: "luxury", label: "роск" },
            { id: "healthcare", kind: "healthcare", label: "мед" },
            { id: "education", kind: "education", label: "обр" },
          ]}
        />
      </SectionFrame>
    );
  }

  if (sectionId === "unions") {
    const unions = state.workers.filter((worker) => worker.classType === "WORKER" && worker.location === "UNION").length;
    return (
      <SectionFrame label={label}>
        <div className="grid grid-cols-5 gap-1">
          {["С/х", "Рем", "Тр", "Обр", "СМИ"].map((union, index) => (
            <div key={union} className="rounded border border-[#8f6b35]/55 bg-black/25 p-1 text-center">
              <MeepleIcon color={index < unions ? "blue" : "gray"} className="mx-auto h-6 w-6" />
              <p className="text-[9px] text-zinc-300">{union}</p>
            </div>
          ))}
        </div>
      </SectionFrame>
    );
  }

  if (sectionId === "wealth") {
    return (
      <SectionFrame label={label}>
        <div className="flex items-center justify-between">
          <ResourceIcon kind="influence" className="h-10 w-10" />
          <span className="font-serif text-4xl leading-none text-[#f1d38a]">{player.victoryPoints}</span>
        </div>
      </SectionFrame>
    );
  }

  if (sectionId === "legitimacy") {
    return (
      <SectionFrame label={label}>
        <div className="grid grid-cols-3 gap-2">
          <ResourceAmount kind="worker" amount={player.legitimacyWorker ?? 0} label="раб" />
          <ResourceAmount kind="worker" amount={player.legitimacyMiddleClass ?? 0} label="ср" />
          <ResourceAmount kind="worker" amount={player.legitimacyCapitalist ?? 0} label="кап" />
        </div>
      </SectionFrame>
    );
  }

  if (sectionId === "influence") {
    return <SectionFrame label={label}><ResourceAmount kind="influence" amount={player.influence ?? 0} label="личное" /></SectionFrame>;
  }

  if (sectionId === "services") {
    return (
      <SectionFrame label={label}>
        <div className="grid grid-cols-3 gap-2">
          <ResourceAmount kind="healthcare" amount={state.publicServices.healthcare} label="мед" />
          <ResourceAmount kind="education" amount={state.publicServices.education} label="обр" />
          <ResourceAmount kind="influence" amount={state.publicServices.mediaInfluence} label="сми" />
        </div>
      </SectionFrame>
    );
  }

  return <SectionFrame label={label}><p className="text-xs leading-snug text-zinc-100">{fallback}</p></SectionFrame>;
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

  if (classType === "MIDDLE_CLASS") {
    return [
      `Население ${player.population}`,
      `Благосост. ${player.welfare}`,
      `Деньги ${player.money} | Влияние ${player.influence ?? 0}`,
      `Еда ${amount(player.goodsAndServicesArea, "food")} / Обр ${amount(player.goodsAndServicesArea, "education")}`,
      `Цены Е-${amount(player.prices, "food") || "-"} / О-${amount(player.prices, "education") || "-"}`,
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
        if (!player) {
          return null;
        }

        return (
          <Card
            key={layout.boardId}
            className={cn(
              "h-full overflow-hidden border border-[#9b7338]/60 bg-[linear-gradient(145deg,rgba(18,27,26,0.96),rgba(8,12,12,0.98))] shadow-lg shadow-black/35 transition-colors",
              layout.accentClass,
              active ? "ring-2 ring-[#f8e7b7]/80" : highlighted ? "ring-2 ring-emerald-300/80" : "ring-0",
            )}
          >
            <CardHeader className="border-b border-[#9b7338]/35 px-3 pb-2 pt-3">
              <CardTitle className="flex items-center justify-between gap-2 text-sm uppercase text-[#d8b56b]">
                <button type="button" onClick={() => onSelectZone(zoneId)} className="min-w-0 break-words text-left hover:text-[#f8e7b7]">
                  {classTitle(layout.boardId)}
                </button>
                <Badge tone="positive">{player.controlMode}</Badge>
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-2 p-3">
              {player && (
                <div className="grid grid-cols-1 gap-2">
                  <ResourceAmount kind="influence" amount={player.influence ?? 0} label="влияние" />
                </div>
              )}
              {layout.sections.map((section, index) => ({ section, index })).filter((item) => item.section.id !== "wealth").map(({ section, index }) => (
                <button
                  key={section.id}
                  type="button"
                  onClick={() => onSelectZone(`${zoneId}:${section.id}`)}
                  className="w-full text-left transition-transform hover:scale-[1.01]"
                >
                  <ClassBoardVisualSection
                    state={state}
                    classType={layout.boardId}
                    sectionId={section.id}
                    label={section.label}
                    player={player}
                    fallback={lines[index] ?? "н/д"}
                  />
                </button>
              ))}
            </CardContent>
          </Card>
        );
      })}
    </div>
  );
}
