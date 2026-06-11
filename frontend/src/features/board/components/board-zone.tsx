import type { BoardZoneView } from "@/features/board/model/types";

interface BoardZoneProps {
  zone: BoardZoneView;
  onSelectZone: (zoneId: string) => void;
}

function zonePalette(zoneType: BoardZoneView["type"]): string {
  if (zoneType === "POLICY") {
    return "#31413c82";
  }
  if (zoneType === "VOTING") {
    return "#6f4b1e5c";
  }
  if (zoneType === "STATE") {
    return "#293f4b75";
  }
  if (zoneType === "MARKET") {
    return "#68441f66";
  }
  if (zoneType === "PRIVATE_CAPITALIST") {
    return "#224c5a72";
  }
  if (zoneType === "PRIVATE_MIDDLE_CLASS") {
    return "#5b502572";
  }
  if (zoneType === "PUBLIC_SECTOR") {
    return "#39425576";
  }
  if (zoneType === "WORKFORCE") {
    return "#5c302f70";
  }
  return "#1d272780";
}

export function BoardZone({ zone, onSelectZone }: BoardZoneProps) {
  const isPolicyTrackContainer = zone.id === "policy_track";
  const isPolicyLane = zone.id.startsWith("policy:");
  const hideText = isPolicyLane || isPolicyTrackContainer || zone.id === "treasury" || zone.id === "state_services" || zone.id === "events";
  const showStats = !["import", "deals", "export", "events", "vote_results"].includes(zone.id);

  const fill = zone.dimmed ? "#0b0f0fd8" : zonePalette(zone.type);
  const stroke = zone.active ? "#f8e7b7" : zone.highlighted ? "#7dd3a8" : isPolicyLane ? "#b98b45b0" : "#9b7338";
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
          <text x={zone.x + 0.9} y={zone.y + 2.0} fontSize="1.36" fontWeight="700" fill="#f7dc98">
            {zone.label}
          </text>
          {showStats && zone.stats.slice(0, 2).map((stat, idx) => (
            <text key={`${zone.id}-stat-${idx}`} x={zone.x + 0.9} y={zone.y + 3.9 + idx * 1.28} fontSize="1.08" fill="#d6d0bd">
              {stat}
            </text>
          ))}
        </>
      )}
    </g>
  );
}
