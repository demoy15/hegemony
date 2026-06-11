import { cn } from "@/lib/utils";
import type { PolicyCourseCellView } from "@/features/board/model/types";
import type { PolicyCourse, PolicyId } from "@/types/game";

interface PolicyCourseCellProps {
  policyId: PolicyId;
  cell: PolicyCourseCellView;
  selected: boolean;
  onSelectCourse: (policyId: PolicyId, course: PolicyCourse) => void;
}

export function PolicyCourseCell({ policyId, cell, selected, onSelectCourse }: PolicyCourseCellProps) {
  return (
    <button
      type="button"
      onClick={() => onSelectCourse(policyId, cell.course)}
      disabled={!cell.selectable && !cell.active && !cell.proposed}
      className={cn(
        "flex h-full w-full min-h-0 items-center justify-center rounded-md border text-[12px] font-semibold transition-all",
        cell.active ? "border-emerald-300/90 bg-emerald-500/25 text-emerald-100" : "",
        cell.proposed ? "border-amber-300/90 bg-amber-500/25 text-amber-50" : "",
        !cell.active && !cell.proposed
          ? cell.selectable
            ? "border-zinc-400/80 bg-zinc-800/75 text-zinc-100 hover:border-emerald-300/80 hover:bg-zinc-700/80"
            : "border-zinc-700/80 bg-zinc-900/70 text-zinc-500"
          : "",
        selected ? "ring-1 ring-white/80" : "",
      )}
      title={
        cell.active
          ? "Текущий курс"
          : cell.proposed
            ? "Цель внесенного законопроекта"
            : cell.selectable
              ? `Выбрать курс ${cell.course} для законопроекта`
              : `Курс ${cell.course} сейчас недоступен`
      }
    >
      {cell.course}
    </button>
  );
}
