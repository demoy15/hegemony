import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { BoardEntityChip } from "@/features/board/components/board-entity-chip";
import { BoardZone } from "@/features/board/components/board-zone";
import { ResourceAmount, ResourceIcon } from "@/features/board/components/board-visual-primitives";
import { PolicyTrackList } from "@/features/board/components/policy-track-list";
import type { BoardViewModel, BoardZoneView } from "@/features/board/model/types";
import type { PolicyCourse, PolicyId } from "@/types/game";

interface GameBoardProps {
  viewModel: BoardViewModel;
  selectedEntityId?: string;
  selectedZoneId?: string;
  selectedPolicyId?: PolicyId;
  selectedPolicyCourse?: PolicyCourse;
  highlightedZones: string[];
  highlightedEntities: string[];
  onSelectZone: (zoneId: string) => void;
  onSelectEntity: (entityId: string) => void;
  onSelectPolicyTrack: (policyId: PolicyId) => void;
  onSelectPolicyCourse: (policyId: PolicyId, course: PolicyCourse) => void;
}

function numberFromStat(stats: string[], prefix: string): string {
  const stat = stats.find((line) => line.toLowerCase().startsWith(prefix.toLowerCase()));
  return stat?.match(/-?\d+/)?.[0] ?? "0";
}

function ZoneDecoration({ zone, boardHeight }: { zone: BoardZoneView; boardHeight: number }) {
  const yScale = 100 / boardHeight;
  const style = {
    left: `${zone.x + zone.width * 0.08}%`,
    top: `${(zone.y + zone.height * 0.32) * yScale}%`,
    width: `${zone.width * 0.84}%`,
    height: `${zone.height * 0.58 * yScale}%`,
  };

  if (zone.id === "treasury") {
    return (
      <div className="pointer-events-none absolute flex flex-col items-center justify-center gap-1 rounded-md border border-emerald-300/70 bg-emerald-950/45 px-2 py-1.5" style={style}>
        <span className="text-[11px] font-semibold uppercase tracking-[0.16em] text-[#f7dc98]">Казна</span>
        <div className="flex items-center justify-center gap-2">
          <ResourceIcon kind="money" className="h-8 w-8" />
          <span className="font-serif text-2xl leading-none text-[#f1d38a]">{numberFromStat(zone.stats, "Казна")}</span>
        </div>
      </div>
    );
  }

  if (zone.id === "state_services") {
    return (
      <div className="pointer-events-none absolute grid grid-cols-3 gap-1" style={style}>
        <ResourceAmount kind="healthcare" amount={numberFromStat(zone.stats, "Мед")} label="мед" />
        <ResourceAmount kind="education" amount={numberFromStat(zone.stats, "Обр")} label="обр" />
        <ResourceAmount kind="influence" amount={numberFromStat(zone.stats, "Медиа")} label="сми" />
      </div>
    );
  }

  return null;
}

export function GameBoard({
  viewModel,
  selectedEntityId,
  selectedZoneId,
  selectedPolicyId,
  selectedPolicyCourse,
  highlightedZones,
  highlightedEntities,
  onSelectZone,
  onSelectEntity,
  onSelectPolicyTrack,
  onSelectPolicyCourse,
}: GameBoardProps) {
  const hasHighlightedEntities = highlightedEntities.length > 0;
  const boardHeight = Math.max(100, viewModel.boardHeight || 100);
  const yScale = 100 / boardHeight;
  const normalizedPolicyTracks = viewModel.policyTracks.map((track) => ({
    ...track,
    y: track.y * yScale,
    height: track.height * yScale,
  }));
  const normalizedRenderables = viewModel.renderables.map((entity) => ({
    ...entity,
    yPct: entity.yPct * yScale,
  }));

  return (
    <Card className="mx-auto w-full border-[#9b7338]/70 bg-[linear-gradient(145deg,rgba(18,27,26,0.96),rgba(8,12,12,0.98))] shadow-[0_26px_54px_-26px_rgba(0,0,0,0.9)]">
      <CardHeader className="border-b border-[#9b7338]/35 px-4 pb-3 pt-3">
        <CardTitle className="text-center text-base uppercase text-[#d8b56b]">Центральное поле</CardTitle>
      </CardHeader>
      <CardContent className="p-3">
        <div
          className="relative mx-auto min-h-[900px] w-full overflow-hidden rounded-md border border-[#b98b45]/70 bg-[linear-gradient(170deg,#101817_0%,#101a1a_45%,#070a0b_100%)] p-1.5 shadow-inner shadow-black/50 xl:min-h-[980px] min-[2100px]:min-h-[1120px]"
          style={{
            aspectRatio: `16 / ${(11 * boardHeight) / 100}`,
            minHeight: `${900 * (boardHeight / 100)}px`,
          }}
        >
          <div className="pointer-events-none absolute inset-0 bg-[linear-gradient(rgba(213,166,83,0.06)_1px,transparent_1px),linear-gradient(90deg,rgba(213,166,83,0.05)_1px,transparent_1px)] bg-[size:36px_36px]" />
          <svg viewBox={`0 0 100 ${boardHeight}`} preserveAspectRatio="none" className="h-full w-full">
            <rect x={0} y={0} width={100} height={boardHeight} fill="transparent" />
            {viewModel.zones.map((zone) => (
              <BoardZone key={zone.id} zone={zone} onSelectZone={onSelectZone} />
            ))}
          </svg>

          {viewModel.zones.map((zone) => (
            <ZoneDecoration key={`${zone.id}-decoration`} zone={zone} boardHeight={boardHeight} />
          ))}

          <PolicyTrackList
            tracks={normalizedPolicyTracks}
            selectedPolicyId={selectedPolicyId}
            selectedCourse={selectedPolicyCourse}
            highlightedZoneIds={highlightedZones}
            selectedZoneId={selectedZoneId}
            onSelectTrack={onSelectPolicyTrack}
            onSelectCourse={onSelectPolicyCourse}
          />

          <div className="pointer-events-none absolute inset-0">
            {normalizedRenderables.map((entity) => (
              <BoardEntityChip
                key={entity.id}
                entity={entity}
                selected={entity.id === selectedEntityId}
                highlighted={highlightedEntities.includes(entity.id)}
                dimmed={hasHighlightedEntities && !highlightedEntities.includes(entity.id) && entity.id !== selectedEntityId}
                onSelectEntity={onSelectEntity}
              />
            ))}
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
