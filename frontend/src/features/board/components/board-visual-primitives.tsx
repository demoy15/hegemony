import type { ActionType, ClassType } from "@/types/game";

export type ResourceKind = "food" | "luxury" | "healthcare" | "education" | "influence" | "money" | "worker";
export type MeepleColor = "gray" | "green" | "blue" | "red" | "orange" | "purple" | "white";

interface ResourceIconProps {
  kind: ResourceKind;
  className?: string;
}

function resourcePaint(kind: ResourceKind): { fill: string; stroke: string; accent: string } {
  if (kind === "food") return { fill: "#b88a2a", stroke: "#f5d56b", accent: "#6fbf58" };
  if (kind === "luxury") return { fill: "#8fb5d1", stroke: "#e5f6ff", accent: "#4f6c85" };
  if (kind === "healthcare") return { fill: "#9a4a2b", stroke: "#f2c28f", accent: "#f4efe4" };
  if (kind === "education") return { fill: "#25323b", stroke: "#c7a46a", accent: "#e4c983" };
  if (kind === "influence") return { fill: "#315f55", stroke: "#d5b66d", accent: "#6cb6b8" };
  if (kind === "worker") return { fill: "#4f7395", stroke: "#d8b56b", accent: "#8bc4e7" };
  return { fill: "#b47a2d", stroke: "#ffd987", accent: "#f7bd45" };
}

export function ResourceIcon({ kind, className = "h-7 w-7" }: ResourceIconProps) {
  const paint = resourcePaint(kind);

  if (kind === "food") {
    return (
      <svg viewBox="0 0 32 32" className={className} aria-hidden="true">
        <path d="M7 23c4-8 8-13 18-15-1 9-7 15-18 15Z" fill={paint.accent} stroke={paint.stroke} strokeWidth="1.5" />
        <path d="M10 22c3-5 6-9 12-12" stroke="#19351f" strokeWidth="1.4" strokeLinecap="round" />
        <path d="M7 25h18" stroke={paint.stroke} strokeWidth="2" strokeLinecap="round" />
      </svg>
    );
  }

  if (kind === "luxury") {
    return (
      <svg viewBox="0 0 32 32" className={className} aria-hidden="true">
        <path d="M8 10h16l4 6-12 12L4 16l4-6Z" fill={paint.fill} stroke={paint.stroke} strokeWidth="1.5" />
        <path d="M8 10l8 18 8-18M4 16h24M12 10l-2 6M20 10l2 6" stroke="#f7fbff" strokeWidth="1" opacity="0.8" />
      </svg>
    );
  }

  if (kind === "healthcare") {
    return (
      <svg viewBox="0 0 32 32" className={className} aria-hidden="true">
        <circle cx="16" cy="16" r="12" fill={paint.fill} stroke={paint.stroke} strokeWidth="1.6" />
        <path d="M16 8v16M8 16h16" stroke={paint.accent} strokeWidth="5" strokeLinecap="round" />
      </svg>
    );
  }

  if (kind === "education") {
    return (
      <svg viewBox="0 0 32 32" className={className} aria-hidden="true">
        <path d="M4 13 16 7l12 6-12 6-12-6Z" fill={paint.fill} stroke={paint.stroke} strokeWidth="1.5" />
        <path d="M9 17v5c4 3 10 3 14 0v-5" fill="#182126" stroke={paint.stroke} strokeWidth="1.3" />
        <path d="M27 14v8" stroke={paint.accent} strokeWidth="1.5" strokeLinecap="round" />
      </svg>
    );
  }

  if (kind === "influence") {
    return (
      <svg viewBox="0 0 32 32" className={className} aria-hidden="true">
        <circle cx="16" cy="16" r="12" fill={paint.fill} stroke={paint.stroke} strokeWidth="1.5" />
        <path d="M16 7v18M7 16h18M9.5 10.5c4 3.5 9 3.5 13 0M9.5 21.5c4-3.5 9-3.5 13 0" fill="none" stroke={paint.accent} strokeWidth="1.3" />
      </svg>
    );
  }

  if (kind === "worker") {
    return <MeepleIcon color="blue" className={className} />;
  }

  return (
    <svg viewBox="0 0 32 32" className={className} aria-hidden="true">
      <ellipse cx="16" cy="23" rx="10" ry="4" fill="#7a4b18" stroke={paint.stroke} strokeWidth="1.2" />
      <ellipse cx="16" cy="18" rx="10" ry="4" fill={paint.fill} stroke={paint.stroke} strokeWidth="1.2" />
      <ellipse cx="16" cy="13" rx="10" ry="4" fill="#d59a3f" stroke={paint.stroke} strokeWidth="1.2" />
      <path d="M12 13c2 1 6 1 8 0" stroke="#ffe0a0" strokeWidth="1" strokeLinecap="round" />
    </svg>
  );
}

function meepleFill(color: MeepleColor): string {
  if (color === "green") return "#70a857";
  if (color === "blue") return "#1686c7";
  if (color === "red") return "#b95135";
  if (color === "orange") return "#c9782c";
  if (color === "purple") return "#73519b";
  if (color === "white") return "#d8d2c0";
  return "#7d817c";
}

export function MeepleIcon({ color, className = "h-7 w-7", tied = false }: { color: MeepleColor; className?: string; tied?: boolean }) {
  const fill = meepleFill(color);
  return (
    <svg viewBox="0 0 32 32" className={className} aria-hidden="true">
      <circle cx="16" cy="7.5" r="5.2" fill={fill} stroke="#0b0f0f" strokeWidth="1.4" />
      <path d="M8.5 15.5c0-3 15-3 15 0l-2.2 5.2 3 8.3H7.7l3-8.3-2.2-5.2Z" fill={fill} stroke="#0b0f0f" strokeWidth="1.4" />
      <path d="M11 15.8c3 1.2 7 1.2 10 0" stroke="rgba(255,255,255,0.28)" strokeWidth="1.2" strokeLinecap="round" />
      {tied && <path d="M7 20h18" stroke="#f8e7b7" strokeWidth="2.2" strokeLinecap="round" />}
    </svg>
  );
}

export function MeepleRow({ colors, className = "" }: { colors: MeepleColor[]; className?: string }) {
  return (
    <div className={`flex items-end justify-center gap-1 ${className}`}>
      {colors.map((color, index) => (
        <MeepleIcon key={`${color}-${index}`} color={color} className="h-7 w-7 drop-shadow-[0_4px_5px_rgba(0,0,0,0.45)]" />
      ))}
    </div>
  );
}

export function ResourceAmount({ kind, amount, label }: { kind: ResourceKind; amount: number | string; label?: string }) {
  return (
    <div className="flex items-center gap-1.5 rounded-md border border-[#8f6b35]/55 bg-black/25 px-2 py-1">
      <ResourceIcon kind={kind} className="h-6 w-6 shrink-0" />
      <div className="min-w-0">
        {label && <p className="truncate text-[10px] uppercase text-[#c4a56a]">{label}</p>}
        <p className="font-serif text-lg leading-none text-[#f1d38a]">{amount}</p>
      </div>
    </div>
  );
}

export function PriceBadge({ value, label }: { value: number | string; label?: string }) {
  return (
    <div className="inline-flex items-center gap-1 rounded-full border border-[#b98b45]/70 bg-[#120d05]/80 px-2 py-1">
      {label && <span className="text-[9px] uppercase text-[#c4a56a]">{label}</span>}
      <ResourceIcon kind="money" className="h-4 w-4" />
      <span className="font-serif text-lg leading-none text-[#f1d38a]">{value}</span>
    </div>
  );
}

export function resourceKindForId(resourceId: string): ResourceKind {
  const normalized = resourceId.toLowerCase();
  if (normalized === "food" || normalized.includes("еда") || normalized.includes("продов") || normalized.includes("🌾") || normalized.includes("🍞")) return "food";
  if (normalized === "luxury" || normalized.includes("роск")) return "luxury";
  if (normalized === "healthcare" || normalized.includes("мед")) return "healthcare";
  if (normalized === "education" || normalized.includes("обр")) return "education";
  if (normalized === "influence" || normalized === "media_influence") return "influence";
  return "money";
}

export function classMeepleColor(classType?: ClassType | string): MeepleColor {
  if (classType === "WORKER") return "blue";
  if (classType === "MIDDLE_CLASS") return "green";
  if (classType === "CAPITALIST") return "orange";
  if (classType === "STATE") return "purple";
  return "gray";
}

export function actionIconFor(actionType: ActionType): ResourceKind | "scroll" | "strike" | "demo" | "podium" {
  if (actionType === "PROPOSE_BILL" || actionType === "DECLARE_VOTE_STANCE" || actionType === "DRAW_VOTING_CUBES" || actionType === "COMMIT_VOTE_INFLUENCE") return "scroll";
  if (actionType === "ASSIGN_WORKERS" || actionType === "HIRE_WORKER") return "worker";
  if (actionType === "BUY_GOODS_AND_SERVICES" || actionType === "CONSUME_LUXURY") return "luxury";
  if (actionType === "CONSUME_HEALTHCARE") return "healthcare";
  if (actionType === "CONSUME_EDUCATION") return "education";
  if (actionType === "PLACE_STRIKES") return "strike";
  if (actionType === "PLACE_DEMONSTRATION") return "demo";
  if (actionType === "ADD_VOTING_CUBES" || actionType === "CALL_EXTRAORDINARY_VOTE") return "influence";
  if (actionType === "END_TURN" || actionType.startsWith("ADVANCE_") || actionType.startsWith("RESOLVE_")) return "podium";
  return "money";
}

export function ActionIcon({ actionType, className = "h-12 w-12" }: { actionType: ActionType; className?: string }) {
  const icon = actionIconFor(actionType);
  if (icon === "scroll") {
    return (
      <svg viewBox="0 0 48 48" className={className} aria-hidden="true">
        <path d="M14 8h22v29c0 4-4 5-7 3H14V8Z" fill="#d8c18a" stroke="#4c3515" strokeWidth="2" />
        <path d="M14 8c-5 0-6 8 0 8" fill="none" stroke="#4c3515" strokeWidth="2" />
        <path d="M20 17h11M20 23h10M20 29h8" stroke="#5c421b" strokeWidth="2" strokeLinecap="round" />
      </svg>
    );
  }
  if (icon === "strike") {
    return (
      <svg viewBox="0 0 48 48" className={className} aria-hidden="true">
        <path d="M24 7 11 21h9l-4 20 21-25h-10l4-9h-7Z" fill="#b95f35" stroke="#f2c18b" strokeWidth="2" />
      </svg>
    );
  }
  if (icon === "demo") {
    return <MeepleRow colors={["blue", "gray", "green"]} className={className} />;
  }
  if (icon === "podium") {
    return (
      <svg viewBox="0 0 48 48" className={className} aria-hidden="true">
        <path d="M14 38h20V18H14v20Z" fill="#54452e" stroke="#d8b56b" strokeWidth="2" />
        <path d="M18 18c3-6 9-6 12 0" fill="none" stroke="#d8b56b" strokeWidth="2" />
        <path d="M10 39h28" stroke="#d8b56b" strokeWidth="2" strokeLinecap="round" />
      </svg>
    );
  }
  return <ResourceIcon kind={icon} className={className} />;
}
