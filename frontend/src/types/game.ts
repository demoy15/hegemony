export type ClassType = "WORKER" | "MIDDLE_CLASS" | "CAPITALIST" | "STATE";

export type PolicyId =
  | "POLICY_1_FISCAL"
  | "POLICY_2_LABOR_MARKET"
  | "POLICY_3_TAXATION"
  | "POLICY_4_HEALTHCARE_AND_BENEFITS"
  | "POLICY_5_EDUCATION"
  | "POLICY_6_FOREIGN_TRADE"
  | "POLICY_7_IMMIGRATION";

export type PolicyCourse = "A" | "B" | "C";
export type GameStatus = "IN_PROGRESS" | "FINISHED";
export type RoundPhase = "PREPARATION" | "ACTIONS" | "PRODUCTION" | "VOTING" | "SCORING" | "GAME_OVER";
export type PlayerControlMode = "HUMAN" | "BOT" | "AUTOMA_SIMPLE" | "AUTOMA_COMPLEX";
export type BotStrategyMode = "HEURISTIC_FALLBACK" | "CARD_DRIVEN_SIMPLE_AUTOMA" | "CARD_DRIVEN_COMPLEX_AUTOMA";

export type ActionType =
  | "ADVANCE_TO_VOTING"
  | "ADVANCE_TO_PRODUCTION"
  | "RESOLVE_PRODUCTION_PHASE"
  | "ADVANCE_TO_SCORING"
  | "RESOLVE_SCORING_PHASE"
  | "ADVANCE_TO_NEXT_ROUND"
  | "RESOLVE_PREPARATION_PHASE"
  | "ADVANCE_GAME_FLOW"
  | "ADVANCE_ROUND"
  | "DECLARE_VOTE_STANCE"
  | "COMMIT_VOTE_INFLUENCE"
  | "PROPOSE_BILL"
  | "ADD_VOTING_CUBES"
  | "CALL_EXTRAORDINARY_VOTE"
  | "ASSIGN_WORKERS"
  | "BUY_GOODS_AND_SERVICES"
  | "CONSUME_HEALTHCARE"
  | "CONSUME_EDUCATION"
  | "CONSUME_LUXURY"
  | "REFRESH_BUSINESS_DEALS"
  | "START_TURN"
  | "HIRE_WORKER"
  | "PRODUCE_GOODS"
  | "SELL_GOODS"
  | "ADJUST_POLICY"
  | "PLAY_CARD"
  | "END_TURN";

export interface ProposalToken {
  id: string;
  ownerPlayerId?: string;
  ownerClass: ClassType;
  available: boolean;
  targetCourse?: PolicyCourse;
  policyId?: PolicyId;
}

export interface PlayerState {
  playerId: string;
  classType: ClassType;
  controlMode: PlayerControlMode;
  botStrategyMode: BotStrategyMode;
  money: number;
  revenue: number;
  capital: number;
  influence: number;
  population: number;
  welfare: number;
  lastWelfareDelta: number;
  victoryPoints: number;
  legitimacyWorker: number;
  legitimacyMiddleClass: number;
  legitimacyCapitalist: number;
  resources: Record<string, number>;
  goodsAndServicesArea: Record<string, number>;
  producedResourceStorage: Record<string, number>;
  prices: Record<string, number>;
  proposalTokens: ProposalToken[];
  handCards: string[];
  availableWorkers: number;
  employedWorkers: number;
  enterprises: number;
  goods: number;
}

export interface PolicyState {
  id: PolicyId;
  currentCourse: PolicyCourse;
  occupyingProposalToken?: ProposalToken;
  locked: boolean;
}

export interface EnterpriseSlot {
  id: string;
  requiredQualification: "UNSKILLED" | "SKILLED";
  requiredColor?: "GRAY" | "GREEN" | "BLUE" | "RED" | "ORANGE" | "PURPLE" | "WHITE";
  requiredSector?: string;
  occupiedWorkerId?: string;
}

export interface Enterprise {
  id: string;
  name?: string;
  category?: string;
  cost?: number;
  ownerClass: ClassType;
  sector: string;
  wageLevel: number;
  automated?: boolean;
  productionAmount?: number;
  productionPerWorkers?: number;
  wageTrack?: Record<string, number>;
  producedResources: Record<string, number>;
  slots: EnterpriseSlot[];
  functioning: boolean;
  fullyEmpty: boolean;
  partiallyFilled: boolean;
}

export interface Worker {
  id: string;
  classType: ClassType;
  qualificationType: "UNSKILLED" | "SKILLED";
  sector?: string;
  location: "UNEMPLOYED" | "ENTERPRISE_SLOT" | "UNION";
  tiedContract: boolean;
  enterpriseId?: string;
  slotId?: string;
}

export interface MarketState {
  goodsPrice: number;
  workerHireCost: number;
}

export interface PublicServicesState {
  healthcare: number;
  education: number;
  mediaInfluence: number;
}

export interface VotingBagState {
  worker: number;
  middleClass: number;
  capitalist: number;
}

export interface TurnOrderState {
  round: number;
  phase: RoundPhase;
  activeClasses: ClassType[];
  currentPlayerIndex: number;
}

export interface EventLogEntry {
  id: number;
  type: string;
  message: string;
}

export interface BusinessDealRequirement {
  resourceId: string;
  amount: number;
}

export interface BusinessDealCard {
  id: string;
  sequence: number;
  title: string;
  requirements: BusinessDealRequirement[];
  payout: number;
  thresholdAmount: number;
  policyABonus: number;
  policyBBonus: number;
  sourceImageRef?: string;
}

export interface OrderedCardDeckState {
  deckId: string;
  orderedCardIds: string[];
  visibleCardIds: string[];
  visibleWindowSize: number;
  nextCardIndex: number;
  refreshCount: number;
  lastRefreshedRound?: number;
  lastRefreshReason?: string;
}

export interface ExportCardState {
  cardId: string;
  title: string;
  description: string;
  availableOperations: number;
  activatedRound: number;
  placeholder: boolean;
  sequence: number;
  sourceImageRef?: string;
  offers: ExportCardOffer[];
}

export interface ExportCardOffer {
  resourceId: string;
  quantity: number;
  revenue: number;
}

export interface MigrationCardEntry {
  qualificationType: "UNSKILLED" | "SKILLED";
  sector: string;
}

export interface MigrationCardState {
  cardId: string;
  sequence: number;
  workerEntry: MigrationCardEntry;
  middleClassEntry: MigrationCardEntry;
}

export interface GameState {
  players: PlayerState[];
  policies: PolicyState[];
  enterprises: Enterprise[];
  workers: Worker[];
  market: MarketState;
  turnOrder: TurnOrderState;
  treasury: number;
  publicServices: PublicServicesState;
  publicServicesStorage: Record<string, number>;
  votingBag: VotingBagState;
  currentVoteState?: CurrentVoteState;
  lastProposalResolution?: ProposalResolutionResult;
  productionPhaseState?: ProductionPhaseState;
  lastProductionSummary?: ProductionPhaseState;
  currentRound: number;
  currentPhase: RoundPhase;
  stateLoans: number;
  maxRounds: number;
  gameStatus: GameStatus;
  gameOver: boolean;
  lastPreparationSummary?: PreparationSummary;
  lastScoringSummary?: ScoringSummary;
  scoringBreakdown: PlayerScoringBreakdown[];
  lastRoundSummary?: RoundSummary;
  finalResult?: FinalResult;
  lifecycleUnsupportedNotes: string[];
  economyUnsupportedNotes: string[];
  cardReadiness: CardReadinessState;
  lastBotTurnSummary?: BotTurnSummary;
  businessDealCards: BusinessDealCard[];
  businessDealDeck: OrderedCardDeckState;
  exportCards: ExportCardState[];
  exportCardDeck: OrderedCardDeckState;
  migrationCards: MigrationCardState[];
  migrationDeck: OrderedCardDeckState;
  activeExportCard: ExportCardState;
  roundMarker: number;
  taxMultiplier: number;
  eventLog: EventLogEntry[];
  demoMode: boolean;
}

export interface DrawnVotingCube {
  ownerClass: "WORKER" | "MIDDLE_CLASS" | "CAPITALIST";
  interpretedVote: "FOR" | "AGAINST" | "NEUTRAL";
}

export interface CurrentVoteState {
  activeProposalPolicyId: PolicyId;
  proposalAuthorPlayerId: string;
  targetCourse: PolicyCourse;
  currentCourseBeforeVote: PolicyCourse;
  votingStage: "DECLARE_STANCES" | "DRAW_BAG_CUBES" | "COMMIT_INFLUENCE" | "RESOLVED";
  stanceByPlayer: Record<string, "FOR" | "AGAINST">;
  drawnVotingCubes: DrawnVotingCube[];
  interpretedVotes: Record<string, number>;
  influenceCommitments: Record<string, number>;
  result: "PASSED" | "REJECTED" | "PENDING";
  passedPolicyCourseApplied: boolean;
  extraordinary: boolean;
  totalForVotes: number;
  totalAgainstVotes: number;
}

export interface ProposalResolutionResult {
  policyId: PolicyId;
  result: "PASSED" | "REJECTED" | "PENDING";
  fromCourse: PolicyCourse;
  targetCourse: PolicyCourse;
}

export interface EnterpriseProductionResult {
  enterpriseId: string;
  ownerPlayerId: string;
  functioning: boolean;
  wagesPaidByRecipient: Record<string, number>;
  producedResources: Record<string, number>;
}

export interface ProductionPhaseState {
  stage: "PRODUCE_GOODS_AND_SERVICES" | "SATISFY_NEEDS" | "PAY_TAXES" | "ROUND_CLOSE" | "COMPLETED";
  productionResolved: boolean;
  roundAdvanceReady: boolean;
  enterpriseResults: EnterpriseProductionResult[];
  workerFoodRequired: number;
  workerFoodConsumed: number;
  workerFoodUnmet: number;
  middleClassFoodRequired: number;
  middleClassFoodConsumed: number;
  middleClassFoodUnmet: number;
  workerTaxesPaid: number;
  middleClassTaxesPaid: number;
  capitalistTaxesPaid: number;
  insufficientFood: boolean;
  unsupportedMarketAcquisition: boolean;
  unsupportedCapitalistTaxModel: boolean;
  unsupportedMiddleClassTaxModel: boolean;
}

export interface ScoringSourceEntry {
  sourceId: string;
  vpDelta: number;
  supported: boolean;
  note?: string;
}

export interface PlayerScoringBreakdown {
  playerId: string;
  classType: ClassType;
  accumulatedBeforePhase: number;
  gainedThisPhase: number;
  totalAfterPhase: number;
  sources: ScoringSourceEntry[];
  unsupportedSources: string[];
}

export interface PreparationSummary {
  round: number;
  skipped: boolean;
  resolved: boolean;
  executedSubsteps: string[];
  unsupportedSubsteps: string[];
  notes: string[];
}

export interface ScoringSummary {
  round: number;
  resolved: boolean;
  players: PlayerScoringBreakdown[];
  unsupportedSources: string[];
  notes: string[];
}

export interface RoundSummary {
  round: number;
  preparationSummary?: PreparationSummary;
  scoringSummary?: ScoringSummary;
}

export interface FinalStanding {
  playerId: string;
  classType: ClassType;
  totalVp: number;
  rank: number;
}

export interface FinalResult {
  completedRound: number;
  tie: boolean;
  tiebreakApplied: boolean;
  unresolvedTie: boolean;
  winnerPlayerIds: string[];
  standings: FinalStanding[];
  scoringBreakdown: PlayerScoringBreakdown[];
  unsupportedNotes: string[];
}

export interface DomainEvent {
  type: string;
  description: string;
}

export interface CardReadinessState {
  enterpriseCardDatasetInstalled: boolean;
  actionModifierDatasetInstalled: boolean;
  simpleAutomaCardDatasetInstalledByClass: Partial<Record<ClassType, boolean>>;
  notes: string[];
}

export interface BotTurnSummary {
  actingClass: ClassType;
  actingPlayerId: string;
  selectedMoveId: string;
  selectedAction: ActionType;
  chosenTargets: Record<string, unknown>;
  cardModifierPathUsed: boolean;
  plannerId: string;
  rationale: string;
  legalOptionsConsidered: number;
  fallbackHeuristicMode: boolean;
  strategyModeUsed: BotStrategyMode;
  eventSummaries: string[];
  automaTrace?: Record<string, unknown>;
}

export interface MarketCandidate {
  enterpriseId: string;
  ownerClass: ClassType;
  freeSlots: number;
  wageLevel: number;
  decisionSource: string;
}

export interface ComposerActionTemplate {
  actionType: ActionType;
  summary: string;
  supported: boolean;
  supportNote: string;
  template: Record<string, unknown>;
  futureModifierSlot: boolean;
}

export interface ComposerMetadata {
  actorPlayerId: string;
  actionTemplates: ComposerActionTemplate[];
  modifierAvailable: boolean;
  modifierAvailabilityNote: string;
  unavailableActionNotes: string[];
}

export interface ActionPreviewDelta {
  moneyDeltaByPlayer: Record<string, number>;
  resourceDeltaByPlayer: Record<string, Record<string, number>>;
  welfareDeltaByPlayer: Record<string, number>;
  workerMovement: Record<string, string>;
  policyDelta: Record<string, string>;
  proposalTokenDeltaByPlayer: Record<string, number>;
  influenceDeltaByPlayer: Record<string, number>;
  victoryPointDeltaByPlayer: Record<string, number>;
  notes: string[];
}

export interface LegalMove {
  id: string;
  actionType: ActionType;
  summary: string;
  legacyDemo: boolean;
  template: Record<string, unknown>;
}

export interface GameResponse {
  gameState: GameState;
  legalMoves: LegalMove[];
  composerMetadata: ComposerMetadata;
  marketCandidates: MarketCandidate[];
}

export interface CommandResponse {
  accepted: boolean;
  errors: string[];
  reasonCodes: string[];
  events: DomainEvent[];
  gameState: GameState;
}

export interface BotMoveResponse {
  selectedMoveId: string;
  actionType: ActionType;
  explanation: string;
  legalActionCount: number;
  events: DomainEvent[];
  gameState: GameState;
}

export interface BotTurnResponse {
  summary: BotTurnSummary;
  events: DomainEvent[];
  gameState: GameState;
}

export interface BotUntilHumanResponse {
  turnSummaries: BotTurnSummary[];
  executedSteps: number;
  stoppedAtHumanDecisionPoint: boolean;
  gameOver: boolean;
  gameState: GameState;
}

export interface PreviewActionResponse {
  accepted: boolean;
  errors: string[];
  reasonCodes: string[];
  delta: ActionPreviewDelta;
  supportNotes: string[];
}

export interface SaveLoadResponse {
  filePath: string;
  gameState: GameState;
}
