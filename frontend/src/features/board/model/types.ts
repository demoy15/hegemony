import type { ActionType, ComposerActionTemplate, LegalMove, PolicyCourse, PolicyId } from "@/types/game";

export type BoardZoneType =
  | "META"
  | "POLICY"
  | "VOTING"
  | "STATE"
  | "MARKET"
  | "PRIVATE_CAPITALIST"
  | "PRIVATE_MIDDLE_CLASS"
  | "PUBLIC_SECTOR"
  | "WORKFORCE";

export interface BoardZoneDefinition {
  id: string;
  label: string;
  type: BoardZoneType;
  x: number;
  y: number;
  width: number;
  height: number;
  accepts: string[];
}

export interface BoardZoneView extends BoardZoneDefinition {
  active: boolean;
  highlighted: boolean;
  dimmed: boolean;
  stats: string[];
}

export type RenderableTone = "neutral" | "positive" | "warning" | "danger" | "info";

export type RenderableKind =
  | "POLICY_MARKER"
  | "PROPOSAL_TOKEN"
  | "ENTERPRISE"
  | "CARD"
  | "WORKER"
  | "ROUND_MARKER"
  | "RESOURCE_TOKEN"
  | "MONEY_TOKEN"
  | "VOTE_CUBE"
  | "INFO_MARKER";

export interface RenderableSourceRef {
  sourceType: "policy" | "enterprise" | "worker" | "player" | "zone" | "businessDeal" | "exportCard";
  sourceId: string;
}

export interface BoardRenderable {
  id: string;
  kind: RenderableKind;
  zoneId: string;
  label: string;
  shortLabel?: string;
  xPct: number;
  yPct: number;
  size: "sm" | "md" | "lg";
  tone: RenderableTone;
  sourceRef?: RenderableSourceRef;
  details?: string;
  visual?: unknown;
  clickable: boolean;
}

export interface BoardViewModel {
  zones: BoardZoneView[];
  renderables: BoardRenderable[];
  pendingProposalPoliciesInOrder: string[];
  policyTracks: PolicyTrackView[];
}

export interface PolicyCourseCellView {
  course: PolicyCourse;
  active: boolean;
  proposed: boolean;
  selectable: boolean;
  blocked: boolean;
}

export interface PolicyTrackView {
  policyId: PolicyId;
  zoneId: string;
  label: string;
  x: number;
  y: number;
  width: number;
  height: number;
  currentCourse: PolicyCourse;
  proposedCourse?: PolicyCourse;
  proposerLabel?: string;
  selectable: boolean;
  blocked: boolean;
  courses: PolicyCourseCellView[];
}

export interface AvailableInteraction {
  id: string;
  actionType: ActionType;
  label: string;
  summary: string;
  enabled: boolean;
  disabledReason?: string;
  legalMove?: LegalMove;
  template?: ComposerActionTemplate;
}
