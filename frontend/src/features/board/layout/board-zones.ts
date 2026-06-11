import type { PolicyId } from "@/types/game";
import type { BoardZoneDefinition } from "@/features/board/model/types";

const POLICY_ZONE_HEIGHT = 5.2;
const POLICY_ZONE_GAP = 0.4;
const POLICY_ZONE_WIDTH = 28;
const POLICY_ZONE_X = 2;
const POLICY_ZONE_Y_START = 13.4;

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
    width: 25,
    height: 9,
    accepts: ["INFO_MARKER"],
  },
  {
    id: "treasury",
    label: "Казна",
    type: "STATE",
    x: 61,
    y: 2,
    width: 9,
    height: 9,
    accepts: ["MONEY_TOKEN", "RESOURCE_TOKEN"],
  },
  {
    id: "state_services",
    label: "Госуслуги",
    type: "STATE",
    x: 71,
    y: 2,
    width: 12,
    height: 9,
    accepts: ["RESOURCE_TOKEN", "INFO_MARKER"],
  },
  {
    id: "state_benefits",
    label: "Господдержка",
    type: "STATE",
    x: 84,
    y: 2,
    width: 14,
    height: 9,
    accepts: ["INFO_MARKER"],
  },
  {
    id: "policy_track",
    label: "Треки политики",
    type: "POLICY",
    x: 2,
    y: 12.2,
    width: 28.8,
    height: 47.8,
    accepts: ["POLICY_MARKER", "PROPOSAL_TOKEN"],
  },
  ...policyZones,
  {
    id: "import",
    label: "Импорт",
    type: "MARKET",
    x: 31.8,
    y: 12.2,
    width: 26.8,
    height: 14.6,
    accepts: ["RESOURCE_TOKEN", "INFO_MARKER"],
  },
  {
    id: "deals",
    label: "Сделки",
    type: "MARKET",
    x: 31.8,
    y: 27.6,
    width: 26.8,
    height: 14.8,
    accepts: ["RESOURCE_TOKEN", "MONEY_TOKEN"],
  },
  {
    id: "export",
    label: "Экспорт",
    type: "MARKET",
    x: 31.8,
    y: 43.2,
    width: 26.8,
    height: 16.8,
    accepts: ["RESOURCE_TOKEN", "INFO_MARKER"],
  },
  {
    id: "public_sector",
    label: "Государственный сектор",
    type: "PUBLIC_SECTOR",
    x: 59.6,
    y: 12.2,
    width: 38.4,
    height: 47.8,
    accepts: ["ENTERPRISE", "WORKER", "RESOURCE_TOKEN"],
  },
  {
    id: "private_middle_class",
    label: "Предприятия среднего класса",
    type: "PRIVATE_MIDDLE_CLASS",
    x: 2,
    y: 62,
    width: 34,
    height: 34.5,
    accepts: ["ENTERPRISE", "WORKER", "RESOURCE_TOKEN", "MONEY_TOKEN"],
  },
  {
    id: "private_capitalist",
    label: "Предприятия капиталистов",
    type: "PRIVATE_CAPITALIST",
    x: 37,
    y: 79,
    width: 47,
    height: 17.5,
    accepts: ["ENTERPRISE", "WORKER", "RESOURCE_TOKEN", "MONEY_TOKEN"],
  },
  {
    id: "capitalist_enterprise_market",
    label: "Рынок предприятий",
    type: "MARKET",
    x: 37,
    y: 62,
    width: 47,
    height: 15,
    accepts: ["ENTERPRISE", "WORKER", "RESOURCE_TOKEN", "MONEY_TOKEN"],
  },
  {
    id: "unemployed",
    label: "Безработные",
    type: "WORKFORCE",
    x: 85,
    y: 62,
    width: 13,
    height: 24,
    accepts: ["WORKER", "INFO_MARKER"],
  },
  {
    id: "state_sector_staging",
    label: "Госрезерв",
    type: "STATE",
    x: 85,
    y: 78,
    width: 13,
    height: 11.5,
    accepts: ["INFO_MARKER"],
  },
];

export const BOARD_ZONE_INDEX = BOARD_ZONES.reduce<Record<string, BoardZoneDefinition>>((acc, zone) => {
  acc[zone.id] = zone;
  return acc;
}, {});

export const POLICY_ZONE_IDS = POLICY_IDS.map((policyId) => `policy:${policyId}`);
