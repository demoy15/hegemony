import type { PolicyId } from "@/types/game";
import type { BoardZoneDefinition } from "@/features/board/model/types";

const POLICY_ZONE_HEIGHT = 5.2;
const POLICY_ZONE_GAP = 0.4;
const POLICY_ZONE_WIDTH = 32;
const POLICY_ZONE_X = 2;
const POLICY_ZONE_Y_START = 13.0;

const POLICY_IDS: PolicyId[] = [
  "POLICY_1_FISCAL",
  "POLICY_2_LABOR_MARKET",
  "POLICY_3_TAXATION",
  "POLICY_4_HEALTHCARE_AND_BENEFITS",
  "POLICY_5_EDUCATION",
  "POLICY_6_FOREIGN_TRADE",
  "POLICY_7_IMMIGRATION",
];

const policyZones: BoardZoneDefinition[] = POLICY_IDS.map((policyId, index) => ({
  id: `policy:${policyId}`,
  label: policyId.replace("POLICY_", "P").replace(/_/g, " "),
  type: "POLICY",
  x: POLICY_ZONE_X,
  y: POLICY_ZONE_Y_START + index * (POLICY_ZONE_HEIGHT + POLICY_ZONE_GAP),
  width: POLICY_ZONE_WIDTH,
  height: POLICY_ZONE_HEIGHT,
  accepts: ["POLICY_MARKER", "PROPOSAL_TOKEN"],
}));

export const BOARD_ZONES: BoardZoneDefinition[] = [
  {
    id: "round_track",
    label: "Ход игры",
    type: "META",
    x: 2,
    y: 2,
    width: 15,
    height: 9,
    accepts: ["ROUND_MARKER", "INFO_MARKER"],
  },
  {
    id: "vote_results",
    label: "Голосование",
    type: "VOTING",
    x: 18,
    y: 2,
    width: 16,
    height: 9,
    accepts: ["VOTE_CUBE", "INFO_MARKER"],
  },
  {
    id: "events",
    label: "События",
    type: "META",
    x: 35,
    y: 2,
    width: 13,
    height: 9,
    accepts: ["INFO_MARKER"],
  },
  {
    id: "treasury",
    label: "Казна",
    type: "STATE",
    x: 49,
    y: 2,
    width: 11,
    height: 9,
    accepts: ["MONEY_TOKEN", "RESOURCE_TOKEN"],
  },
  {
    id: "state_services",
    label: "Госуслуги",
    type: "STATE",
    x: 61,
    y: 2,
    width: 14,
    height: 9,
    accepts: ["RESOURCE_TOKEN", "INFO_MARKER"],
  },
  {
    id: "state_benefits",
    label: "Господдержка",
    type: "STATE",
    x: 76,
    y: 2,
    width: 10,
    height: 9,
    accepts: ["INFO_MARKER"],
  },
  {
    id: "migrants",
    label: "Мигранты",
    type: "WORKFORCE",
    x: 87,
    y: 2,
    width: 11,
    height: 9,
    accepts: ["WORKER", "INFO_MARKER"],
  },
  {
    id: "policy_track",
    label: "Треки политики",
    type: "POLICY",
    x: 2,
    y: 12.2,
    width: 32.8,
    height: 40,
    accepts: ["POLICY_MARKER", "PROPOSAL_TOKEN"],
  },
  ...policyZones,
  {
    id: "import",
    label: "Импорт",
    type: "MARKET",
    x: 35.8,
    y: 12.2,
    width: 10.4,
    height: 12.4,
    accepts: ["RESOURCE_TOKEN", "INFO_MARKER"],
  },
  {
    id: "deals",
    label: "Сделки",
    type: "MARKET",
    x: 35.8,
    y: 25.4,
    width: 10.4,
    height: 12.4,
    accepts: ["RESOURCE_TOKEN", "MONEY_TOKEN"],
  },
  {
    id: "export",
    label: "Экспорт",
    type: "MARKET",
    x: 35.8,
    y: 38.6,
    width: 10.4,
    height: 13.6,
    accepts: ["RESOURCE_TOKEN", "INFO_MARKER"],
  },
  {
    id: "public_sector",
    label: "Государственный сектор",
    type: "PUBLIC_SECTOR",
    x: 47,
    y: 12.2,
    width: 36,
    height: 45,
    accepts: ["ENTERPRISE", "WORKER", "RESOURCE_TOKEN"],
  },
  {
    id: "private_middle_class",
    label: "Частный сектор (средний класс)",
    type: "PRIVATE_MIDDLE_CLASS",
    x: 2,
    y: 57.5,
    width: 46,
    height: 39,
    accepts: ["ENTERPRISE", "WORKER", "RESOURCE_TOKEN", "MONEY_TOKEN"],
  },
  {
    id: "private_capitalist",
    label: "Частный сектор (капиталисты)",
    type: "PRIVATE_CAPITALIST",
    x: 49,
    y: 57.5,
    width: 33,
    height: 39,
    accepts: ["ENTERPRISE", "WORKER", "RESOURCE_TOKEN", "MONEY_TOKEN"],
  },
  {
    id: "unemployed",
    label: "Безработные",
    type: "WORKFORCE",
    x: 83,
    y: 57.5,
    width: 15,
    height: 24,
    accepts: ["WORKER", "INFO_MARKER"],
  },
  {
    id: "state_sector_staging",
    label: "Госрезерв",
    type: "STATE",
    x: 83,
    y: 82,
    width: 15,
    height: 11.5,
    accepts: ["INFO_MARKER"],
  },
];

export const BOARD_ZONE_INDEX = BOARD_ZONES.reduce<Record<string, BoardZoneDefinition>>((acc, zone) => {
  acc[zone.id] = zone;
  return acc;
}, {});

export const POLICY_ZONE_IDS = POLICY_IDS.map((policyId) => `policy:${policyId}`);
