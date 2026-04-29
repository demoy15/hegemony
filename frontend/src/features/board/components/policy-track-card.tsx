import { PolicyCourseCell } from "@/features/board/components/policy-course-cell";
import { cn } from "@/lib/utils";
import type { PolicyTrackView } from "@/features/board/model/types";
import type { PolicyCourse, PolicyId } from "@/types/game";

interface PolicyTrackCardProps {
  track: PolicyTrackView;
  selected: boolean;
  selectedCourse?: PolicyCourse;
  highlighted: boolean;
  dimmed: boolean;
  onSelectTrack: (policyId: PolicyId) => void;
  onSelectCourse: (policyId: PolicyId, course: PolicyCourse) => void;
}

export function PolicyTrackCard({
  track,
  selected,
  selectedCourse,
  highlighted,
  dimmed,
  onSelectTrack,
  onSelectCourse,
}: PolicyTrackCardProps) {
  return (
    <article
      className={cn(
        "pointer-events-auto relative h-full rounded-md border px-1 pb-1 pt-0.5 shadow-sm transition-all",
        selected ? "border-emerald-300/90 bg-zinc-900/95 shadow-emerald-500/20" : "",
        !selected && highlighted ? "border-zinc-300/70 bg-zinc-900/92" : "",
        !selected && !highlighted ? "border-zinc-600/75 bg-zinc-950/85" : "",
        dimmed ? "opacity-45" : "opacity-100",
      )}
    >
      <button
        type="button"
        onClick={() => onSelectTrack(track.policyId)}
        className="absolute inset-x-1 top-0.5 z-10 flex items-center justify-center text-center"
        title={`Open ${track.label}`}
      >
        <span className="truncate text-[10px] font-semibold uppercase tracking-wide text-zinc-100">{track.label}</span>
      </button>

      <div className="grid h-full w-full grid-cols-3 gap-1.5 pt-4">
        {track.courses.map((courseCell) => (
          <PolicyCourseCell
            key={`${track.policyId}-${courseCell.course}`}
            policyId={track.policyId}
            cell={courseCell}
            selected={selectedCourse === courseCell.course}
            onSelectCourse={onSelectCourse}
          />
        ))}
      </div>
    </article>
  );
}
