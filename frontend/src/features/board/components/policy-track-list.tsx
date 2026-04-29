import { PolicyTrackCard } from "@/features/board/components/policy-track-card";
import { BOARD_ZONE_INDEX } from "@/features/board/layout/board-zones";
import { cn } from "@/lib/utils";
import type { PolicyTrackView } from "@/features/board/model/types";
import type { PolicyCourse, PolicyId } from "@/types/game";

interface PolicyTrackListProps {
  tracks: PolicyTrackView[];
  selectedPolicyId?: PolicyId;
  selectedCourse?: PolicyCourse;
  highlightedZoneIds: string[];
  selectedZoneId?: string;
  onSelectTrack: (policyId: PolicyId) => void;
  onSelectCourse: (policyId: PolicyId, course: PolicyCourse) => void;
}

export function PolicyTrackList({
  tracks,
  selectedPolicyId,
  selectedCourse,
  highlightedZoneIds,
  selectedZoneId,
  onSelectTrack,
  onSelectCourse,
}: PolicyTrackListProps) {
  const policyTrackZone = BOARD_ZONE_INDEX.policy_track;
  if (!policyTrackZone) {
    return null;
  }

  return (
    <section
      className={cn(
        "pointer-events-none absolute z-10",
        "rounded-xl border border-violet-300/45 bg-zinc-950/55 p-1 backdrop-blur-[1px]",
      )}
      style={{
        left: `${policyTrackZone.x}%`,
        top: `${policyTrackZone.y}%`,
        width: `${policyTrackZone.width}%`,
        height: `${policyTrackZone.height}%`,
      }}
    >
      <div className="mb-1 flex items-center justify-between px-1.5">
        <p className="text-[10px] font-semibold uppercase tracking-[0.14em] text-violet-100">Policies 1-7</p>
        <p className="text-[10px] text-zinc-300">A / B / C</p>
      </div>

      <div className="grid h-[calc(100%-1.35rem)] grid-rows-7 gap-1">
        {tracks.map((track) => {
          const highlighted = highlightedZoneIds.includes(track.zoneId) || selectedZoneId === "policy_track";
          const selected = selectedPolicyId === track.policyId || selectedZoneId === track.zoneId;
          const dimmed = highlightedZoneIds.length > 0 && !highlighted && !selected;
          return (
            <PolicyTrackCard
              key={track.policyId}
              track={track}
              selected={selected}
              selectedCourse={selected ? selectedCourse : undefined}
              highlighted={highlighted}
              dimmed={dimmed}
              onSelectTrack={onSelectTrack}
              onSelectCourse={onSelectCourse}
            />
          );
        })}
      </div>
    </section>
  );
}
