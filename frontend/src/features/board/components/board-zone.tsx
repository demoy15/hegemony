import type { BoardZoneView } from "@/features/board/model/types";

interface BoardZoneProps {
  zone: BoardZoneView;
  onSelectZone: (zoneId: string) => void;
}

function zonePalette(zoneType: BoardZoneView["type"]): string {
  if (zoneType === "POLICY") {
    return "#8b5cf633";
  }
  if (zoneType === "VOTING") {
    return "#f59e0b29";
  }
  if (zoneType === "STATE") {
    return "#ef444428";
  }
  if (zoneType === "MARKET") {
    return "#f9731626";
  }
  if (zoneType === "PRIVATE_CAPITALIST") {
    return "#38bdf82b";
  }
  if (zoneType === "PRIVATE_MIDDLE_CLASS") {
    return "#f59e0b26";
  }
  if (zoneType === "PUBLIC_SECTOR") {
    return "#a78bfa2a";
  }
  if (zoneType === "WORKFORCE") {
    return "#fb718533";
  }
  return "#ffffff14";
}

export function BoardZone({ zone, onSelectZone }: BoardZoneProps) {
  const isPolicyTrackContainer = zone.id === "policy_track";
  const isPolicyLane = zone.id.startsWith("policy:");
  const hideText = isPolicyLane || isPolicyTrackContainer;

  const fill = zone.dimmed ? "#0f1014d6" : zonePalette(zone.type);
  const stroke = zone.active ? "#f8fafc" : zone.highlighted ? "#22c55e" : isPolicyLane ? "#6d28d9b0" : "#a1a1aa";
  const strokeWidth = zone.active ? 0.42 : zone.highlighted ? 0.32 : isPolicyLane ? 0.14 : 0.2;

  return (
    <g onClick={() => onSelectZone(zone.id)} className="cursor-pointer">
      <rect
        x={zone.x}
        y={zone.y}
        width={zone.width}
        height={zone.height}
        rx={1.2}
        ry={1.2}
        fill={fill}
        stroke={stroke}
        strokeWidth={strokeWidth}
      />
      {!hideText && (
        <>
          <text x={zone.x + 0.9} y={zone.y + 2.0} fontSize="1.36" fontWeight="700" fill="#f4f4f5" letterSpacing="0.08">
            {zone.label}
          </text>
          {zone.stats.slice(0, 2).map((stat, idx) => (
            <text key={`${zone.id}-stat-${idx}`} x={zone.x + 0.9} y={zone.y + 3.9 + idx * 1.28} fontSize="1.08" fill="#d4d4d8">
              {stat}
            </text>
          ))}
        </>
      )}
    </g>
  );
}
