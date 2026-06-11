import { cn } from "@/lib/utils";
import { MeepleIcon, PriceBadge, ResourceIcon, resourceKindForId, type MeepleColor } from "@/features/board/components/board-visual-primitives";
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
    return "border-emerald-300/75 bg-emerald-500/22 text-emerald-100";
  }
  if (tone === "warning") {
    return "border-[#f0c978]/80 bg-amber-500/22 text-amber-50";
  }
  if (tone === "danger") {
    return "border-red-300/80 bg-red-500/22 text-red-50";
  }
  if (tone === "info") {
    return "border-cyan-300/80 bg-cyan-500/22 text-cyan-50";
  }
  return "border-[#b98b45]/70 bg-[#172220]/88 text-zinc-100";
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
  optional?: boolean;
  workerId?: string;
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
  classType?: "WORKER" | "MIDDLE_CLASS" | "CAPITALIST" | "STATE";
  tied: boolean;
  posture?: "STANDING" | "LYING";
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
    return "w-[154px]";
  }
  if (size === "md") {
    return "w-[128px]";
  }
  return "w-[104px]";
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
      ? "border-emerald-200 bg-emerald-500/35 text-emerald-50"
      : color === "BLUE"
        ? "border-sky-200 bg-sky-500/35 text-sky-50"
        : color === "RED"
          ? "border-rose-200 bg-rose-500/30 text-rose-50"
          : color === "ORANGE"
            ? "border-amber-200 bg-amber-500/35 text-amber-50"
            : color === "PURPLE"
              ? "border-fuchsia-200 bg-fuchsia-500/35 text-fuchsia-50"
              : color === "WHITE"
                ? "border-stone-100 bg-stone-100/28 text-stone-50"
                : "border-zinc-200/80 bg-zinc-500/24 text-zinc-50";

  if (state === "EMPTY") {
    return `${base} border-dashed bg-black/20 opacity-95 ring-1 ring-white/20`;
  }
  if (state === "TIED") {
    return `${base} shadow-[0_8px_16px_-10px_rgba(0,0,0,0.9)] ring-1 ring-amber-100/70`;
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

function meepleColor(color: WorkerColor): MeepleColor {
  if (color === "GREEN") return "green";
  if (color === "BLUE") return "blue";
  if (color === "RED") return "white";
  if (color === "ORANGE") return "orange";
  if (color === "PURPLE") return "purple";
  if (color === "WHITE") return "white";
  return "gray";
}

function WorkerDot({ color, size = "md", tied = false, posture = "STANDING" }: { color: WorkerColor; size?: "sm" | "md"; tied?: boolean; posture?: "STANDING" | "LYING" }) {
  return (
    <MeepleIcon
      color={meepleColor(color)}
      tied={tied}
      className={cn(size === "sm" ? "h-4 w-4" : "h-6 w-6", posture === "LYING" ? "rotate-90" : "")}
    />
  );
}

function VacancyMark({ color }: { color: WorkerColor }) {
  const innerClass =
    color === "GREEN"
      ? "bg-emerald-300"
      : color === "BLUE"
        ? "bg-sky-300"
        : color === "ORANGE"
          ? "bg-amber-300"
          : color === "PURPLE"
            ? "bg-fuchsia-300"
            : color === "WHITE" || color === "RED"
              ? "bg-stone-100"
              : "bg-zinc-300";
  return (
    <div className="relative flex h-5 w-5 items-center justify-center rounded-md border-2 border-dashed border-current bg-black/45 shadow-inner shadow-black/60">
      {color !== "GRAY" && <span className={cn("h-2.5 w-2.5 rounded-full shadow-[0_0_8px_rgba(255,255,255,0.35)]", innerClass)} />}
      {color === "GRAY" && <span className="h-2.5 w-2.5 rounded-full border border-zinc-200/70 bg-zinc-500/45" />}
    </div>
  );
}

function importPriceCard(entity: BoardRenderable) {
  if (entity.id !== "import-price-food" && entity.id !== "import-price-luxury") {
    return null;
  }
  const isFood = entity.id === "import-price-food";
  const match = entity.label.match(/(\d+)\+(\d+)=(\d+).*?\(([ABC])\)/);
  const base = match?.[1] ?? "?";
  const tariff = match?.[2] ?? "?";
  const final = match?.[3] ?? entity.shortLabel?.replace(/\D+/g, "") ?? "?";
  const course = match?.[4] ?? "?";
  const title = isFood ? "Еда" : "Роскошь";
  const kind = isFood ? "food" : "luxury";

  return (
    <div className="min-w-0 space-y-1">
      <p className="text-[9px] font-semibold uppercase tracking-[0.08em] text-[#f1d38a]">{title}</p>
      <div className="flex min-w-0 items-center justify-center gap-2">
        <ResourceIcon kind={kind} className="h-7 w-7 shrink-0" />
        <PriceBadge value={final} label="цена" />
      </div>
      <div className="grid grid-cols-3 gap-1 text-center text-[8px] leading-tight text-[#c4a56a]">
        <span>База {base}</span>
        <span>Пошл. {tariff}</span>
        <span>Y{course}</span>
      </div>
    </div>
  );
}

function exportCardVisual(entity: BoardRenderable) {
  if (!entity.id.startsWith("export-card:")) {
    return null;
  }
  const offersText = entity.details?.match(/Варианты:\s*(.*?)\.\s*Активна/)?.[1] ?? entity.shortLabel ?? "";
  const offers = offersText
    .split(";")
    .map((item) => item.trim())
    .filter(Boolean)
    .map((item) => {
      const match = item.match(/(\d+)\s+(.+?)\s+->\s+(\d+)/);
      return {
        quantity: match?.[1] ?? "",
        resource: match?.[2] ?? item,
        revenue: match?.[3] ?? "",
      };
    });
  return (
    <div className="min-w-0 space-y-1">
      <p className="truncate text-[10px] font-semibold uppercase text-[#f1d38a]">{entity.label}</p>
      <div className="grid grid-cols-2 gap-0.5">
        {offers.map((offer, index) => (
          <div key={`${entity.id}-${index}`} className="flex min-w-0 items-center justify-between gap-1 rounded border border-[#8f6b35]/55 bg-black/25 px-1 py-0.5">
            <div className="flex min-w-0 items-center gap-1">
              <span className="font-serif text-[12px] leading-none text-[#f1d38a]">{offer.quantity}</span>
              <ResourceIcon kind={resourceKindForId(offer.resource)} className="h-3.5 w-3.5 shrink-0" />
            </div>
            <span className="shrink-0 text-[10px] text-[#c4a56a]">{"->"}</span>
            <span className="flex min-w-[46px] items-center justify-end gap-1 rounded-full border border-[#b98b45]/70 bg-[#120d05]/80 px-1 py-0.5">
              <ResourceIcon kind="money" className="h-3.5 w-3.5 shrink-0" />
              <span className="font-serif text-[12px] leading-none text-[#f1d38a]">{offer.revenue}</span>
            </span>
          </div>
        ))}
      </div>
    </div>
  );
}

function dealResourceKind(symbol: string) {
  const normalized = symbol.toLowerCase();
  if (symbol.includes("🌾") || symbol.includes("🍞") || normalized.includes("food") || normalized.includes("еда")) {
    return "food" as const;
  }
  if (symbol.includes("💎") || normalized.includes("luxury") || normalized.includes("роско")) {
    return "luxury" as const;
  }
  if (symbol.includes("⚕") || normalized.includes("health") || normalized.includes("мед")) {
    return "healthcare" as const;
  }
  if (symbol.includes("🎓") || normalized.includes("educ") || normalized.includes("обр")) {
    return "education" as const;
  }
  return "influence" as const;
}

function businessDealCardVisual(entity: BoardRenderable) {
  if (!entity.id.startsWith("business-deal:")) {
    return null;
  }
  const [requirementsRaw = "", payoutRaw = ""] = (entity.shortLabel ?? "").split(/→|->/);
  const requirements = requirementsRaw
    .split("+")
    .map((part) => part.trim())
    .filter(Boolean)
    .map((part) => {
      const match = part.match(/^(\d+)(.*)$/);
      return { amount: match?.[1] ?? part, resource: match?.[2] ?? part };
    });
  const payout = payoutRaw.replace(/\D+/g, "") || "?";

  return (
    <div className="space-y-1">
      <div className="flex flex-wrap items-center justify-center gap-1">
        {requirements.map((requirement, index) => (
          <span key={`${entity.id}-req-${index}`} className="inline-flex items-center gap-1 rounded border border-[#8f6b35]/55 bg-black/25 px-1.5 py-0.5">
            <span className="font-serif text-sm leading-none text-[#f1d38a]">{requirement.amount}</span>
            <ResourceIcon kind={dealResourceKind(requirement.resource)} className="h-4 w-4 shrink-0" />
          </span>
        ))}
      </div>
      <div className="flex items-center justify-center gap-2">
        <span className="text-[#c4a56a]">{"->"}</span>
        <PriceBadge value={payout} />
      </div>
    </div>
  );
}

function stateEventCardVisual(entity: BoardRenderable) {
  if (!entity.id.startsWith("state-event:")) {
    return null;
  }
  const details = entity.details ?? "";
  const [instruction = ""] = details.split(". Варианты:");
  return (
    <div className="min-w-0 space-y-1 text-left">
      <div className="flex items-center justify-between gap-2">
        <span className="rounded border border-amber-300/35 bg-amber-300/12 px-1 py-0.5 text-[7px] font-bold uppercase tracking-[0.12em] text-amber-100">
          Событие
        </span>
        <span className="text-[8px] font-semibold text-[#f1d38a]">Гос.</span>
      </div>
      <p className="text-[10px] font-semibold leading-tight text-amber-50">{entity.label}</p>
      <p className="max-h-[28px] overflow-hidden text-[8.5px] leading-tight text-zinc-100/85">{instruction}</p>
    </div>
  );
}

function EnterpriseIllustration({ label, size = "md" }: { label: string; size?: "sm" | "md" }) {
  const lower = label.toLowerCase();
  const isMedical = lower.includes("clinic") || lower.includes("hospital") || lower.includes("полик") || lower.includes("боль");
  const isEducation = lower.includes("college") || lower.includes("university") || lower.includes("универ") || lower.includes("колледж");
  const isMarket = lower.includes("market") || lower.includes("mall") || lower.includes("магаз") || lower.includes("торгов");
  return (
    <svg viewBox="0 0 48 36" className={cn(size === "sm" ? "h-7 w-9" : "h-9 w-12", "shrink-0")} aria-hidden="true">
      <path d="M7 31V12l17-7 17 7v19H7Z" fill="#233331" stroke="#d8b56b" strokeWidth="1.5" />
      <path d="M11 31V15h26v16" fill="#162423" stroke="#8f6b35" strokeWidth="1" />
      <path d="M14 18h5v5h-5zM22 18h5v5h-5zM30 18h5v5h-5z" fill="#365f67" stroke="#d8b56b" strokeWidth="0.8" />
      <path d="M21 31v-6h6v6" fill="#0b0f0f" stroke="#d8b56b" strokeWidth="0.9" />
      {isMedical && <path d="M24 8v8M20 12h8" stroke="#f4efe4" strokeWidth="3" strokeLinecap="round" />}
      {isEducation && <path d="M14 10 24 6l10 4-10 4-10-4Z" fill="#d8b56b" stroke="#5b3d17" strokeWidth="0.8" />}
      {isMarket && <path d="M10 13h28l-2 5H12l-2-5Z" fill="#3b7891" stroke="#d8b56b" strokeWidth="0.9" />}
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
  const importCard = importPriceCard(entity);
  const exportCard = exportCardVisual(entity);
  const businessDealCard = businessDealCardVisual(entity);
  const stateEventCard = stateEventCardVisual(entity);

  if (workerMeeple && entity.kind === "WORKER") {
    const middleClass = workerMeeple.classType === "MIDDLE_CLASS";
    return (
      <button
        type="button"
        title={`${entity.label}${entity.details ? ` - ${entity.details}` : ""}`}
        onClick={() => onSelectEntity(entity.id)}
        className={cn(
          "pointer-events-auto absolute z-20 -translate-x-1/2 -translate-y-1/2 rounded-full border bg-zinc-900/88 p-1 text-zinc-100 shadow-lg shadow-black/40 transition-all",
          middleClass ? "border-amber-300/80 ring-1 ring-amber-300/45" : "border-zinc-300/65",
          selected ? "scale-110 ring-2 ring-white/80" : highlighted ? "ring-2 ring-emerald-400/80" : "",
          dimmed ? "opacity-35" : "opacity-100 hover:scale-105",
        )}
        style={{ left: `${entity.xPct}%`, top: `${entity.yPct}%` }}
      >
        <WorkerDot color={workerMeeple.color} tied={workerMeeple.tied} posture={workerMeeple.posture} />
        <span
          className={cn(
            "pointer-events-none absolute -right-1 -top-1 flex h-3.5 min-w-3.5 items-center justify-center rounded-full border px-0.5 text-[8px] font-bold leading-none",
            middleClass ? "border-amber-200 bg-amber-300 text-zinc-950" : "border-zinc-200 bg-zinc-100 text-zinc-950",
          )}
        >
          {middleClass ? "M" : "W"}
        </span>
      </button>
    );
  }

  if (visual) {
    const wages = [...visual.wages].sort((left, right) => left.level - right.level);
    return (
      <div
        role="button"
        tabIndex={0}
        title={`${entity.label}${entity.details ? ` - ${entity.details}` : ""}`}
        onClick={() => onSelectEntity(entity.id)}
        onKeyDown={(event) => {
          if (event.key === "Enter" || event.key === " ") {
            event.preventDefault();
            onSelectEntity(entity.id);
          }
        }}
        className={cn(
          "pointer-events-auto absolute -translate-x-1/2 -translate-y-1/2 rounded-md border text-left shadow-lg shadow-black/40 transition-all",
          entity.size === "sm" ? "p-1" : "p-1.5",
          enterpriseCardWidth(entity.size),
          enterpriseCardToneClasses(entity.tone, visual.state),
          selected ? "scale-[1.03] ring-2 ring-white/80" : highlighted ? "ring-2 ring-emerald-400/80" : "",
          dimmed ? "opacity-35" : "opacity-100 hover:scale-[1.02]",
        )}
        style={{ left: `${entity.xPct}%`, top: `${entity.yPct}%` }}
      >
        <div className="grid grid-cols-[auto_minmax(0,1fr)_auto] items-start gap-1.5">
          <EnterpriseIllustration label={entity.label} size={entity.size === "sm" ? "sm" : "md"} />
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

        <div className={entity.size === "sm" ? "relative z-10 mt-1 grid grid-cols-3 gap-0.5" : "relative z-10 mt-1.5 grid grid-cols-3 gap-1"}>
          {visual.slots.map((slot) => (
            <button
              type="button"
              key={slot.id}
              title={slotLabel(slot)}
              onClick={(event) => {
                event.stopPropagation();
                onSelectEntity(slot.workerId ? `worker:${slot.workerId}` : entity.id);
              }}
              className={cn(
                entity.size === "sm" ? "relative flex h-6 items-center justify-center rounded border" : "relative flex h-8 items-center justify-center rounded-md border",
                slotColorClasses(slot.color, slot.state),
                slot.optional && slot.state === "EMPTY" ? "opacity-60" : "",
              )}
            >
              {slot.state === "EMPTY" ? (
                <div className="flex items-center justify-center opacity-100">
                  <VacancyMark color={slot.qualification === "SKILLED" ? slot.requiredColor : "GRAY"} />
                </div>
              ) : (
                <WorkerDot color={slot.color} size="sm" tied={slot.state === "TIED"} posture={slot.state === "TIED" ? "LYING" : "STANDING"} />
              )}
            </button>
          ))}
        </div>

        <div className={entity.size === "sm" ? "relative z-10 mt-1 grid grid-cols-3 gap-0.5" : "relative z-10 mt-1.5 grid grid-cols-3 gap-1"}>
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
      </div>
    );
  }

  if (importCard) {
    return (
      <button
        type="button"
        title={`${entity.label}${entity.details ? ` - ${entity.details}` : ""}`}
        onClick={() => onSelectEntity(entity.id)}
        className={cn(
          "pointer-events-auto absolute -translate-x-1/2 -translate-y-1/2 rounded-md border border-[#b98b45]/70 bg-[#111915]/92 px-3 py-1.5 text-center shadow-lg shadow-black/40 transition-all",
          selected ? "scale-[1.03] ring-2 ring-white/80" : highlighted ? "ring-2 ring-emerald-400/80" : "",
          dimmed ? "opacity-35" : "opacity-100 hover:scale-[1.02]",
        )}
        style={{ left: `${entity.xPct}%`, top: `${entity.yPct}%`, width: entity.size === "sm" ? 138 : 154 }}
      >
        {importCard}
      </button>
    );
  }

  if (exportCard) {
    return (
      <button
        type="button"
        title={`${entity.label}${entity.details ? ` - ${entity.details}` : ""}`}
        onClick={() => onSelectEntity(entity.id)}
        className={cn(
          "pointer-events-auto absolute -translate-x-1/2 -translate-y-1/2 rounded-md border border-[#b98b45]/70 bg-[#111915]/92 px-3 py-1.5 text-center shadow-lg shadow-black/40 transition-all",
          selected ? "scale-[1.03] ring-2 ring-white/80" : highlighted ? "ring-2 ring-emerald-400/80" : "",
          dimmed ? "opacity-35" : "opacity-100 hover:scale-[1.02]",
        )}
        style={{ left: `${entity.xPct}%`, top: `${entity.yPct}%`, width: 286 }}
      >
        {exportCard}
      </button>
    );
  }

  if (businessDealCard) {
    return (
      <button
        type="button"
        title={`${entity.label}${entity.details ? ` - ${entity.details}` : ""}`}
        onClick={() => onSelectEntity(entity.id)}
        className={cn(
          "pointer-events-auto absolute -translate-x-1/2 -translate-y-1/2 rounded-md border border-[#b98b45]/70 bg-[#111915]/92 px-1.5 py-1.5 text-center shadow-lg shadow-black/40 transition-all",
          selected ? "scale-[1.03] ring-2 ring-white/80" : highlighted ? "ring-2 ring-emerald-400/80" : "",
          dimmed ? "opacity-35" : "opacity-100 hover:scale-[1.02]",
        )}
        style={{ left: `${entity.xPct}%`, top: `${entity.yPct}%`, width: 146 }}
      >
        {businessDealCard}
      </button>
    );
  }

  if (stateEventCard) {
    return (
      <button
        type="button"
        title={`${entity.label}${entity.details ? ` - ${entity.details}` : ""}`}
        onClick={() => onSelectEntity(entity.id)}
        className={cn(
          "pointer-events-auto absolute z-10 -translate-x-1/2 -translate-y-1/2 rounded-md border border-amber-300/70 bg-[#151611]/95 px-2 py-1.5 text-left shadow-lg shadow-black/45 transition-all",
          selected ? "scale-[1.03] ring-2 ring-white/80" : highlighted ? "ring-2 ring-emerald-400/80" : "",
          dimmed ? "opacity-35" : "opacity-100 hover:scale-[1.02]",
        )}
        style={{ left: `${entity.xPct}%`, top: `${entity.yPct}%`, width: 132 }}
      >
        {stateEventCard}
      </button>
    );
  }

  return (
    <button
      type="button"
      title={`${entity.label}${entity.details ? ` - ${entity.details}` : ""}`}
      onClick={() => onSelectEntity(entity.id)}
      className={cn(
        "pointer-events-auto absolute -translate-x-1/2 -translate-y-1/2 border text-center font-semibold shadow-lg shadow-black/35 transition-all",
        rectangular ? "rounded-md" : "rounded-full",
        sizeClass(entity.size),
        toneClass(entity.tone),
        rectangular ? "max-w-[96px] truncate min-[2100px]:max-w-[118px]" : "",
        selected ? "scale-110 ring-2 ring-white/80" : highlighted ? "ring-2 ring-emerald-400/80" : "",
        dimmed ? "opacity-35" : "opacity-100 hover:scale-105",
      )}
      style={{ left: `${entity.xPct}%`, top: `${entity.yPct}%` }}
    >
      {entity.kind === "MONEY_TOKEN" ? (
        <span className="flex items-center justify-center gap-1">
          <ResourceIcon kind="money" className="h-6 w-6" />
          <span className="font-serif text-lg text-[#f1d38a]">{entity.shortLabel ?? entity.label}</span>
        </span>
      ) : (
        entity.shortLabel ?? entity.label
      )}
    </button>
  );
}
