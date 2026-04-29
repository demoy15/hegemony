import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { BoardEntityChip } from "@/features/board/components/board-entity-chip";
import { BoardZone } from "@/features/board/components/board-zone";
import { PolicyTrackList } from "@/features/board/components/policy-track-list";
import type { BoardViewModel } from "@/features/board/model/types";
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

  return (
    <Card className="mx-auto w-full border-zinc-700/90 bg-gradient-to-br from-zinc-950 via-zinc-900/95 to-zinc-950 shadow-[0_26px_54px_-26px_rgba(0,0,0,0.9)]">
      <CardHeader className="pb-3">
        <CardTitle className="text-base uppercase tracking-[0.18em] text-zinc-100">Игровое поле</CardTitle>
      </CardHeader>
      <CardContent>
        <div className="relative mx-auto aspect-[16/10] min-h-[700px] w-full overflow-hidden rounded-2xl border border-zinc-600/80 bg-[radial-gradient(circle_at_12%_6%,rgba(254,205,123,0.18),transparent_32%),radial-gradient(circle_at_82%_72%,rgba(56,189,248,0.16),transparent_42%),linear-gradient(170deg,#121318_0%,#0f1117_55%,#0b0d12_100%)] p-1.5 shadow-inner shadow-black/35 xl:min-h-[760px] min-[2100px]:min-h-[920px]">
          <svg viewBox="0 0 100 100" preserveAspectRatio="none" className="h-full w-full">
            <rect x={0} y={0} width={100} height={100} fill="transparent" />
            {viewModel.zones.map((zone) => (
              <BoardZone key={zone.id} zone={zone} onSelectZone={onSelectZone} />
            ))}
          </svg>

          <PolicyTrackList
            tracks={viewModel.policyTracks}
            selectedPolicyId={selectedPolicyId}
            selectedCourse={selectedPolicyCourse}
            highlightedZoneIds={highlightedZones}
            selectedZoneId={selectedZoneId}
            onSelectTrack={onSelectPolicyTrack}
            onSelectCourse={onSelectPolicyCourse}
          />

          <div className="pointer-events-none absolute inset-0">
            {viewModel.renderables.map((entity) => (
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
