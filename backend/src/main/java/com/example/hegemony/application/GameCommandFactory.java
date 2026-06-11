package com.example.hegemony.application;

import com.example.hegemony.domain.command.AdjustPolicyCommand;
import com.example.hegemony.domain.command.AdvanceGameFlowCommand;
import com.example.hegemony.domain.command.AdvanceRoundCommand;
import com.example.hegemony.domain.command.AdvanceToNextRoundCommand;
import com.example.hegemony.domain.command.AdvanceToProductionCommand;
import com.example.hegemony.domain.command.AdvanceToScoringCommand;
import com.example.hegemony.domain.command.AdvanceToVotingCommand;
import com.example.hegemony.domain.command.AddVotingCubesCommand;
import com.example.hegemony.domain.command.AssignWorkersCommand;
import com.example.hegemony.domain.command.AssignmentTargetType;
import com.example.hegemony.domain.command.BuyGoodsAndServicesCommand;
import com.example.hegemony.domain.command.CallExtraordinaryVoteCommand;
import com.example.hegemony.domain.command.CapitalistActionCommand;
import com.example.hegemony.domain.command.CommitVoteInfluenceCommand;
import com.example.hegemony.domain.command.ConsumeEducationCommand;
import com.example.hegemony.domain.command.ConsumeHealthcareCommand;
import com.example.hegemony.domain.command.ConsumeLuxuryCommand;
import com.example.hegemony.domain.command.DeclareVoteStanceCommand;
import com.example.hegemony.domain.command.DrawVotingCubesCommand;
import com.example.hegemony.domain.command.EndTurnCommand;
import com.example.hegemony.domain.command.GameCommand;
import com.example.hegemony.domain.command.HireWorkerCommand;
import com.example.hegemony.domain.command.PlayCardCommand;
import com.example.hegemony.domain.command.PlaceDemonstrationCommand;
import com.example.hegemony.domain.command.PlaceStrikesCommand;
import com.example.hegemony.domain.command.ProposeBillCommand;
import com.example.hegemony.domain.command.ProduceGoodsCommand;
import com.example.hegemony.domain.command.RefreshBusinessDealsCommand;
import com.example.hegemony.domain.command.ResolvePreparationPhaseCommand;
import com.example.hegemony.domain.command.ResolveProductionPhaseCommand;
import com.example.hegemony.domain.command.ResolveScoringPhaseCommand;
import com.example.hegemony.domain.command.SellGoodsCommand;
import com.example.hegemony.domain.command.StartTurnCommand;
import com.example.hegemony.domain.command.PurchaseItem;
import com.example.hegemony.domain.command.WorkerAssignmentOperation;
import com.example.hegemony.domain.model.ActionType;
import com.example.hegemony.domain.model.PolicyCourse;
import com.example.hegemony.domain.model.PolicyId;
import com.example.hegemony.domain.model.PolicyTrack;
import com.example.hegemony.domain.model.SupplierType;
import com.example.hegemony.domain.model.WorkerSlotColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GameCommandFactory {
    @SuppressWarnings("unchecked")
    public GameCommand create(ActionType type, Map<String, Object> params) {
        return switch (type) {
            case ADVANCE_TO_VOTING -> new AdvanceToVotingCommand(stringParam(params, "actorPlayerId", ""));
            case ADVANCE_TO_PRODUCTION -> new AdvanceToProductionCommand(stringParam(params, "actorPlayerId", ""));
            case RESOLVE_PRODUCTION_PHASE -> new ResolveProductionPhaseCommand(
                    stringParam(params, "actorPlayerId", ""),
                    purchaseItems(params.get("workerFoodPurchases"))
            );
            case ADVANCE_TO_SCORING -> new AdvanceToScoringCommand(stringParam(params, "actorPlayerId", ""));
            case RESOLVE_SCORING_PHASE -> new ResolveScoringPhaseCommand(stringParam(params, "actorPlayerId", ""));
            case ADVANCE_TO_NEXT_ROUND -> new AdvanceToNextRoundCommand(stringParam(params, "actorPlayerId", ""));
            case RESOLVE_PREPARATION_PHASE -> new ResolvePreparationPhaseCommand(stringParam(params, "actorPlayerId", ""));
            case ADVANCE_GAME_FLOW -> new AdvanceGameFlowCommand(stringParam(params, "actorPlayerId", ""));
            case ADVANCE_ROUND -> new AdvanceRoundCommand(stringParam(params, "actorPlayerId", ""));
            case DECLARE_VOTE_STANCE -> new DeclareVoteStanceCommand(
                    stringParam(params, "actorPlayerId", ""),
                    PolicyId.valueOf(stringParam(params, "policyId", "").toUpperCase()),
                    stringParam(params, "stance", "")
            );
            case DRAW_VOTING_CUBES -> new DrawVotingCubesCommand(
                    stringParam(params, "actorPlayerId", ""),
                    intParam(params, "count", 5)
            );
            case COMMIT_VOTE_INFLUENCE -> new CommitVoteInfluenceCommand(
                    stringParam(params, "actorPlayerId", ""),
                    intParam(params, "influenceAmount", 0)
            );
            case PROPOSE_BILL -> new ProposeBillCommand(
                    stringParam(params, "actorPlayerId", ""),
                    PolicyId.valueOf(stringParam(params, "policyId", "").toUpperCase()),
                    PolicyCourse.valueOf(stringParam(params, "targetCourse", "").toUpperCase())
            );
            case ADD_VOTING_CUBES -> new AddVotingCubesCommand(stringParam(params, "actorPlayerId", ""));
            case BUILD_ENTERPRISE,
                 SELL_ENTERPRISE,
                 SELL_ON_EXTERNAL_MARKET,
                 MAKE_BUSINESS_DEAL,
                 LOBBY_INTERESTS,
                 CHANGE_PRICES,
                 CHANGE_WAGES,
                 PAY_BONUS,
                 BUY_STORAGE,
                 TAKE_STATE_BENEFITS,
                 REPAY_LOAN,
                 RESPOND_TO_EVENT,
                 MEET_DEPUTIES,
                 INTRODUCE_EXTRA_TAX,
                 RUN_CAMPAIGN -> new CapitalistActionCommand(
                    type,
                    stringParam(params, "actorPlayerId", ""),
                    params == null ? Map.of() : Map.copyOf(params)
            );
            case CALL_EXTRAORDINARY_VOTE -> new CallExtraordinaryVoteCommand(
                    stringParam(params, "actorPlayerId", ""),
                    PolicyId.valueOf(stringParam(params, "policyId", "").toUpperCase())
            );
            case ASSIGN_WORKERS -> {
                String actorPlayerId = stringParam(params, "actorPlayerId", "");
                Object raw = params.get("assignments");
                List<WorkerAssignmentOperation> assignments = new ArrayList<>();
                if (raw instanceof List<?> list) {
                    for (Object item : list) {
                        if (item instanceof Map<?, ?> row) {
                            Map<String, Object> cast = (Map<String, Object>) row;
                            assignments.add(new WorkerAssignmentOperation(
                                    stringParam(cast, "workerId", ""),
                                    AssignmentTargetType.valueOf(stringParam(cast, "targetType", "ENTERPRISE_SLOT").toUpperCase()),
                                    stringParam(cast, "targetId", "")
                            ));
                        }
                    }
                }
                yield new AssignWorkersCommand(actorPlayerId, assignments);
            }
            case PLACE_STRIKES -> {
                String actorPlayerId = stringParam(params, "actorPlayerId", "");
                Object raw = params.get("enterpriseIds");
                List<String> enterpriseIds = new ArrayList<>();
                if (raw instanceof List<?> list) {
                    for (Object item : list) {
                        if (item != null && !String.valueOf(item).isBlank()) {
                            enterpriseIds.add(String.valueOf(item));
                        }
                    }
                }
                yield new PlaceStrikesCommand(actorPlayerId, enterpriseIds);
            }
            case PLACE_DEMONSTRATION -> {
                String actorPlayerId = stringParam(params, "actorPlayerId", "");
                Object raw = params.get("penaltyAllocation");
                Map<String, Integer> allocation = new java.util.HashMap<>();
                if (raw instanceof Map<?, ?> map) {
                    for (Map.Entry<?, ?> entry : map.entrySet()) {
                        if (entry.getKey() != null && entry.getValue() != null) {
                            allocation.put(String.valueOf(entry.getKey()), intValue(entry.getValue(), 0));
                        }
                    }
                }
                yield new PlaceDemonstrationCommand(actorPlayerId, allocation);
            }
            case BUY_GOODS_AND_SERVICES -> {
                String actorPlayerId = stringParam(params, "actorPlayerId", "");
                String resourceType = stringParam(params, "resourceType", "");
                List<PurchaseItem> purchases = purchaseItems(params.get("purchases"));
                yield new BuyGoodsAndServicesCommand(actorPlayerId, resourceType, purchases);
            }
            case CONSUME_HEALTHCARE -> new ConsumeHealthcareCommand(stringParam(params, "actorPlayerId", ""));
            case CONSUME_EDUCATION -> new ConsumeEducationCommand(
                    stringParam(params, "actorPlayerId", ""),
                    stringParam(params, "workerId", ""),
                    WorkerSlotColor.fromRaw(stringParam(params, "targetColor", WorkerSlotColor.WHITE.name()))
            );
            case CONSUME_LUXURY -> new ConsumeLuxuryCommand(stringParam(params, "actorPlayerId", ""));
            case REFRESH_BUSINESS_DEALS -> new RefreshBusinessDealsCommand(stringParam(params, "actorPlayerId", ""));

            // Legacy demo actions (kept but separated from new core slice).
            case START_TURN -> new StartTurnCommand();
            case END_TURN -> new EndTurnCommand();
            case HIRE_WORKER -> new HireWorkerCommand(intParam(params, "count", 1));
            case PRODUCE_GOODS -> new ProduceGoodsCommand(intParam(params, "amount", 1));
            case SELL_GOODS -> new SellGoodsCommand(intParam(params, "amount", 1));
            case ADJUST_POLICY -> {
                String trackRaw = stringParam(params, "track", PolicyTrack.TAXATION.name());
                int delta = intParam(params, "delta", 1);
                yield new AdjustPolicyCommand(PolicyTrack.valueOf(trackRaw.toUpperCase()), delta);
            }
            case PLAY_CARD -> new PlayCardCommand(stringParam(params, "cardId", ""));
        };
    }

    @SuppressWarnings("unchecked")
    private List<PurchaseItem> purchaseItems(Object raw) {
        List<PurchaseItem> purchases = new ArrayList<>();
        if (raw instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> row) {
                    Map<String, Object> cast = (Map<String, Object>) row;
                    purchases.add(new PurchaseItem(
                            supplierTypeParam(cast, "supplierType"),
                            nullableStringParam(cast, "supplierPlayerId"),
                            intParam(cast, "quantity", 0),
                            nullableIntParam(cast, "unitPriceOverride")
                    ));
                }
            }
        }
        return purchases;
    }

    private int intParam(Map<String, Object> params, String key, int defaultValue) {
        Object raw = params.get(key);
        if (raw == null) {
            return defaultValue;
        }
        if (raw instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(raw));
    }

    private int intValue(Object raw, int defaultValue) {
        if (raw == null) {
            return defaultValue;
        }
        if (raw instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(raw));
    }

    private Integer nullableIntParam(Map<String, Object> params, String key) {
        Object raw = params.get(key);
        if (raw == null || String.valueOf(raw).isBlank()) {
            return null;
        }
        if (raw instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(raw));
    }

    private String stringParam(Map<String, Object> params, String key, String defaultValue) {
        Object raw = params.get(key);
        return raw == null ? defaultValue : String.valueOf(raw);
    }

    private String nullableStringParam(Map<String, Object> params, String key) {
        Object raw = params.get(key);
        if (raw == null) {
            return null;
        }
        String value = String.valueOf(raw).trim();
        return value.isEmpty() ? null : value;
    }

    private SupplierType supplierTypeParam(Map<String, Object> params, String key) {
        String raw = stringParam(params, key, "");
        if (raw.isBlank()) {
            return null;
        }
        try {
            return SupplierType.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
