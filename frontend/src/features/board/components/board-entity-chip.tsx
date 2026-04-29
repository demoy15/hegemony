import { cn } from "@/lib/utils";
import type { BoardRenderable } from "@/features/board/model/types";

interface BoardEntityChipProps {
  entity: BoardRenderable;
  selected: boolean;
  highlighted: boolean;
  dimmed: boolean;
  onSelectEntity: (entityId: string) => void;
}

function sizeClass(size: BoardRenderable["size"]): string {
  if (size === "lg") {
    return "min-h-11 min-w-11 px-3 text-[12px]";
  }
  if (size === "md") {
    return "min-h-9 min-w-9 px-2.5 text-[11px]";
  }
  return "min-h-7 min-w-7 px-2 text-[10px]";
}

function toneClass(tone: BoardRenderable["tone"]): string {
  if (tone === "positive") {
    return "border-emerald-300/70 bg-emerald-500/25 text-emerald-100";
  }
  if (tone === "warning") {
    return "border-amber-300/80 bg-amber-500/25 text-amber-50";
  }
  if (tone === "danger") {
    return "border-red-300/80 bg-red-500/25 text-red-50";
  }
  if (tone === "info") {
    return "border-sky-300/80 bg-sky-500/25 text-sky-50";
  }
  return "border-zinc-300/70 bg-zinc-700/40 text-zinc-100";
}

type WorkerColor = "GRAY" | "GREEN" | "BLUE" | "RED" | "ORANGE" | "PURPLE" | "WHITE";
type EnterpriseCardState = "ACTIVE" | "RESERVE" | "LOCKED";
type EnterpriseSlotState = "EMPTY" | "OCCUPIED" | "TIED";

interface EnterpriseSlotVisual {
  id: string;
  state: EnterpriseSlotState;
  color: WorkerColor;
  requiredColor: WorkerColor;
  qualification: "UNSKILLED" | "SKILLED";
  workerNumber?: string;
}

interface EnterpriseWageLevelVisual {
  level: 1 | 2 | 3;
  value?: number;
  active: boolean;
  blocked: boolean;
}

interface EnterpriseCardVisual {
  variant: "enterprise-card";
  state: EnterpriseCardState;
  rowLabel?: string;
  policyHint?: string;
  outputLabel: string;
  slots: EnterpriseSlotVisual[];
  wages: EnterpriseWageLevelVisual[];
  minimumWageLevel: 1 | 2 | 3;
}

interface WorkerMeepleVisual {
  variant: "worker-meeple";
  color: WorkerColor;
  workerNumber: string;
  tied: boolean;
}

function enterpriseVisual(entity: BoardRenderable): EnterpriseCardVisual | undefined {
  const candidate = entity as BoardRenderable & { visual?: EnterpriseCardVisual };
  return candidate.visual?.variant === "enterprise-card" ? candidate.visual : undefined;
}

function workerVisual(entity: BoardRenderable): WorkerMeepleVisual | undefined {
  const candidate = entity as BoardRenderable & { visual?: WorkerMeepleVisual };
  return candidate.visual?.variant === "worker-meeple" ? candidate.visual : undefined;
}

function enterpriseCardWidth(size: BoardRenderable["size"]): string {
  if (size === "lg") {
    return "w-[122px]";
  }
  if (size === "md") {
    return "w-[108px]";
  }
  return "w-[82px]";
}

function enterpriseCardToneClasses(tone: BoardRenderable["tone"], state: EnterpriseCardState): string {
  if (state === "LOCKED") {
    return "border-zinc-500/60 bg-zinc-900/90 text-zinc-300";
  }
  if (state === "RESERVE") {
    return "border-amber-300/60 bg-amber-500/12 text-amber-50";
  }
  return toneClass(tone);
}

function slotColorClasses(color: WorkerColor, state: EnterpriseSlotState): string {
  const base =
    color === "GREEN"
      ? "border-emerald-300/60 bg-emerald-500/16 text-emerald-50"
      : color === "BLUE"
        ? "border-sky-300/60 bg-sky-500/16 text-sky-50"
        : color === "RED"
          ? "border-rose-300/60 bg-rose-500/16 text-rose-50"
          : color === "ORANGE"
            ? "border-amber-300/70 bg-amber-500/16 text-amber-50"
            : color === "PURPLE"
              ? "border-fuchsia-300/60 bg-fuchsia-500/16 text-fuchsia-50"
              : color === "WHITE"
                ? "border-slate-200/75 bg-slate-100/18 text-slate-50"
                : "border-zinc-400/60 bg-zinc-500/14 text-zinc-50";

  if (state === "EMPTY") {
    return `${base} border-dashed opacity-90`;
  }
  return `${base} shadow-[0_8px_16px_-10px_rgba(0,0,0,0.9)]`;
}

function wageChipClasses(wage: EnterpriseWageLevelVisual): string {
  if (wage.active) {
    return wage.blocked
      ? "border-rose-300/70 bg-rose-500/18 text-rose-50 ring-1 ring-rose-200/60"
      : "border-emerald-300/70 bg-emerald-500/18 text-emerald-50 ring-1 ring-emerald-200/60";
  }
  if (wage.blocked) {
    return "border-zinc-600/70 bg-zinc-800/80 text-zinc-500";
  }
  return "border-zinc-500/70 bg-black/25 text-zinc-200";
}

function slotLabel(slot: EnterpriseSlotVisual): string {
  const qualification = slot.qualification === "SKILLED" ? "квалифицированный" : "обычный";
  if (slot.state === "EMPTY") {
    return `${qualification}, свободно`;
  }
  if (slot.state === "TIED") {
    return `${qualification}, занят по трудовому договору`;
  }
  return `${qualification}, занят`;
}

function workerDotFill(color: WorkerColor): string {
  if (color === "GREEN") {
    return "#34d399";
  }
  if (color === "BLUE") {
    return "#38bdf8";
  }
  if (color === "RED") {
    return "#fb7185";
  }
  if (color === "ORANGE") {
    return "#f59e0b";
  }
  if (color === "PURPLE") {
    return "#c084fc";
  }
  if (color === "WHITE") {
    return "#e2e8f0";
  }
  return "#d4d4d8";
}

function workerColorCode(color: WorkerColor): string {
  if (color === "GREEN") {
    return "GRN";
  }
  if (color === "BLUE") {
    return "BLU";
  }
  if (color === "RED") {
    return "RED";
  }
  if (color === "ORANGE") {
    return "ORG";
  }
  if (color === "PURPLE") {
    return "PUR";
  }
  if (color === "WHITE") {
    return "WHT";
  }
  return "GRY";
}

function WorkerDot({ color, size = "md", tied = false }: { color: WorkerColor; size?: "sm" | "md"; tied?: boolean }) {
  const fill = workerDotFill(color);
  return (
    <svg viewBox="0 0 24 24" className={size === "sm" ? "h-3.5 w-3.5" : "h-5 w-5"} aria-hidden="true">
      <circle cx="12" cy="12" r="8.6" fill={fill} stroke="#0f172a" strokeWidth="1.4" />
      {tied && (
        <>
          <path d="M4.8 12h14.4" stroke="#f8fafc" strokeWidth="1.8" strokeLinecap="round" />
          <path d="M9.3 9.6l2.6 3M11.9 9.6l-2.6 3" stroke="#0f172a" strokeWidth="1" strokeLinecap="round" />
        </>
      )}
    </svg>
  );
}

export function BoardEntityChip({
  entity,
  selected,
  highlighted,
  dimmed,
  onSelectEntity,
}: BoardEntityChipProps) {
  const visual = enterpriseVisual(entity);
  const workerMeeple = workerVisual(entity);
  const rectangular = entity.kind === "ENTERPRISE" || entity.kind === "CARD";

  if (workerMeeple && entity.kind === "WORKER") {
    return (
      <button
        type="button"
        title={`${entity.label}${entity.details ? ` - ${entity.details}` : ""}`}
        onClick={() => onSelectEntity(entity.id)}
        className={cn(
          "pointer-events-auto absolute -translate-x-1/2 -translate-y-1/2 rounded-full border border-zinc-300/65 bg-zinc-900/88 p-1 text-zinc-100 shadow-lg shadow-black/40 transition-all",
          selected ? "scale-110 ring-2 ring-white/80" : highlighted ? "ring-2 ring-emerald-400/80" : "",
          dimmed ? "opacity-35" : "opacity-100 hover:scale-105",
        )}
        style={{ left: `${entity.xPct}%`, top: `${entity.yPct}%` }}
      >
        <WorkerDot color={workerMeeple.color} tied={workerMeeple.tied} />
      </button>
    );
  }

  if (visual) {
    const wages = [...visual.wages].sort((left, right) => left.level - right.level);
    return (
      <button
        type="button"
        title={`${entity.label}${entity.details ? ` - ${entity.details}` : ""}`}
        onClick={() => onSelectEntity(entity.id)}
        className={cn(
          "pointer-events-auto absolute -translate-x-1/2 -translate-y-1/2 rounded-lg border text-left shadow-lg shadow-black/35 transition-all",
          entity.size === "sm" ? "p-1" : "p-1.5",
          enterpriseCardWidth(entity.size),
          enterpriseCardToneClasses(entity.tone, visual.state),
          selected ? "scale-[1.03] ring-2 ring-white/80" : highlighted ? "ring-2 ring-emerald-400/80" : "",
          dimmed ? "opacity-35" : "opacity-100 hover:scale-[1.02]",
        )}
        style={{ left: `${entity.xPct}%`, top: `${entity.yPct}%` }}
      >
        <div className="flex items-start justify-between gap-1.5">
          <div className="min-w-0">
            <p className={entity.size === "sm" ? "text-[8px] font-semibold leading-tight" : "text-[9.5px] font-semibold leading-tight"}>{entity.label}</p>
            <p className={entity.size === "sm" ? "mt-0.5 text-[7px] leading-tight text-white/80" : "mt-0.5 text-[8px] leading-tight text-white/80"}>{visual.outputLabel}</p>
          </div>
          <div className="flex shrink-0 flex-col items-end gap-0.5">
            {visual.rowLabel && (
              <span className="rounded-full border border-white/15 bg-black/20 px-1.5 py-0.5 text-[7px] uppercase tracking-[0.14em] text-white/80">
                {entity.size === "sm" ? visual.rowLabel.replace("Ряд ", "R") : visual.rowLabel}
              </span>
            )}
          </div>
        </div>

        <div className={entity.size === "sm" ? "mt-1 grid grid-cols-3 gap-0.5" : "mt-1.5 grid grid-cols-3 gap-1"}>
          {visual.slots.map((slot) => (
            <div
              key={slot.id}
              title={slotLabel(slot)}
              className={cn(entity.size === "sm" ? "relative flex h-5 items-center justify-center rounded border" : "relative flex h-7 items-center justify-center rounded-md border", slotColorClasses(slot.color, slot.state))}
            >
              {slot.state === "EMPTY" ? (
                <div className="flex flex-col items-center justify-center">
                  <span className="text-[7px] font-semibold uppercase tracking-[0.16em]">
                    {slot.qualification === "SKILLED" ? "КВ" : "СВ"}
                  </span>
                  {slot.qualification === "SKILLED" && (
                    <span className="text-[6.5px] font-semibold uppercase leading-none tracking-[0.08em]">
                      {workerColorCode(slot.requiredColor)}
                    </span>
                  )}
                  <span className="text-[8px] leading-none">+</span>
                </div>
              ) : (
                <WorkerDot color={slot.color} size="sm" tied={slot.state === "TIED"} />
              )}
            </div>
          ))}
        </div>

        <div className={entity.size === "sm" ? "mt-1 grid grid-cols-3 gap-0.5" : "mt-1.5 grid grid-cols-3 gap-1"}>
          {wages.map((wage) => (
            <div key={`${entity.id}-wage-${wage.level}`} className={cn(entity.size === "sm" ? "rounded border px-0.5 py-0.5 text-center" : "rounded-md border px-1 py-1 text-center", wageChipClasses(wage))}>
              <p className={entity.size === "sm" ? "text-[6px] uppercase" : "text-[7px] uppercase tracking-[0.14em]"}>L{wage.level}</p>
              <p className={entity.size === "sm" ? "text-[7px] font-semibold leading-none" : "text-[9px] font-semibold leading-none"}>{wage.value ?? "-"}</p>
            </div>
          ))}
        </div>

        <p className={entity.size === "sm" ? "mt-0.5 text-[6.5px] leading-tight text-white/70" : "mt-1 text-[8px] leading-tight text-white/70"}>
          Мин. ЗП: L{visual.minimumWageLevel}
          {visual.policyHint ? ` • ${visual.policyHint}` : ""}
        </p>
      </button>
    );
  }

  return (
    <button
      type="button"
      title={`${entity.label}${entity.details ? ` - ${entity.details}` : ""}`}
      onClick={() => onSelectEntity(entity.id)}
      className={cn(
        "pointer-events-auto absolute -translate-x-1/2 -translate-y-1/2 border text-center font-semibold shadow-lg shadow-black/30 transition-all",
        rectangular ? "rounded-md" : "rounded-full",
        sizeClass(entity.size),
        toneClass(entity.tone),
        rectangular ? "max-w-[96px] truncate min-[2100px]:max-w-[118px]" : "",
        selected ? "scale-110 ring-2 ring-white/80" : highlighted ? "ring-2 ring-emerald-400/80" : "",
        dimmed ? "opacity-35" : "opacity-100 hover:scale-105",
      )}
      style={{ left: `${entity.xPct}%`, top: `${entity.yPct}%` }}
    >
      {entity.shortLabel ?? entity.label}
    </button>
  );
}
