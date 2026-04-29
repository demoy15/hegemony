package com.example.hegemony.domain.economy;

import com.example.hegemony.domain.command.PurchaseItem;
import com.example.hegemony.domain.model.ClassType;
import com.example.hegemony.domain.model.GameState;
import com.example.hegemony.domain.model.PlayerState;
import com.example.hegemony.domain.model.PolicyCourse;
import com.example.hegemony.domain.model.PolicyId;
import com.example.hegemony.domain.model.PolicyState;
import com.example.hegemony.domain.model.ResourceType;
import com.example.hegemony.domain.model.SupplierType;
import com.example.hegemony.domain.rules.ValidationReasonCode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConsumerEconomyService {
    public boolean isSupportedConsumerBuyer(ClassType classType) {
        return classType == ClassType.WORKER || classType == ClassType.MIDDLE_CLASS;
    }

    public boolean supportedBuyResource(ResourceType resourceType) {
        return switch (resourceType) {
            case FOOD, LUXURY, HEALTHCARE, EDUCATION, INFLUENCE -> true;
            default -> false;
        };
    }

    public boolean supportedConsumptionResource(ResourceType resourceType) {
        return resourceType == ResourceType.HEALTHCARE || resourceType == ResourceType.EDUCATION || resourceType == ResourceType.LUXURY;
    }

    public boolean canConsumeResource(PlayerState player, ResourceType resourceType) {
        if (player == null || !supportedConsumptionResource(resourceType)) {
            return false;
        }
        if (!isSupportedConsumerBuyer(player.getClassType())) {
            return false;
        }
        int required = Math.max(0, player.getPopulation());
        return player.getGoodsAmount(resourceType.id()) >= required;
    }

    public List<SupplierOffer> listSupplierOffersFor(GameState state, PlayerState buyer, ResourceType resourceType) {
        List<SupplierOffer> offers = new ArrayList<>();
        List<SupplierType> allowedSupplierTypes = allowedSupplierTypesForBuyer(buyer.getClassType());
        for (SupplierType supplierType : allowedSupplierTypes) {
            String supplierPlayerId = supplierPlayerIdFor(state, buyer, supplierType);
            SupplierOffer offer = resolveSupplierOffer(state, buyer, resourceType, supplierType, supplierPlayerId, null);
            if (offer.supported() && offer.availableQuantity() > 0 && offer.unitPrice() > 0) {
                offers.add(offer);
            }
        }
        return offers;
    }

    public PurchaseEvaluation evaluatePurchasePlan(
            GameState state,
            PlayerState buyer,
            ResourceType resourceType,
            List<PurchaseItem> purchases,
            boolean ignoreFundsCheck
    ) {
        List<String> errors = new ArrayList<>();
        List<ValidationReasonCode> codes = new ArrayList<>();
        List<String> unsupportedNotes = new ArrayList<>();

        Map<SupplierKey, Integer> requestedQuantityBySupplier = new HashMap<>();
        Map<SupplierKey, SupplierOffer> offersBySupplier = new HashMap<>();

        for (PurchaseItem item : purchases) {
            if (item == null || item.supplierType() == null) {
                addEvaluationError(errors, codes, ValidationReasonCode.UNSUPPORTED_SUPPLIER, "Supplier type is missing or unsupported.");
                continue;
            }
            if (item.quantity() <= 0) {
                addEvaluationError(errors, codes, ValidationReasonCode.INVALID_TARGET, "Purchase quantity must be positive.");
                continue;
            }

            String resolvedSupplierPlayerId = resolveSupplierPlayerIdFromRequest(state, buyer, item);
            SupplierOffer offer = resolveSupplierOffer(state, buyer, resourceType, item.supplierType(), resolvedSupplierPlayerId, item.unitPriceOverride());
            SupplierKey key = new SupplierKey(item.supplierType(), offer.supplierPlayerId());
            requestedQuantityBySupplier.merge(key, item.quantity(), Integer::sum);
            offersBySupplier.put(key, offer);

            if (!offer.supported()) {
                addEvaluationError(errors, codes,
                        offer.unsupportedCode() == null ? ValidationReasonCode.UNSUPPORTED_RESOURCE_PURCHASE_PATH : offer.unsupportedCode(),
                        offer.unsupportedNote() == null ? "Unsupported purchase path." : offer.unsupportedNote());
            } else if (offer.unsupportedNote() != null && !offer.unsupportedNote().isBlank()) {
                unsupportedNotes.add(offer.unsupportedNote());
            }
        }

        List<ResolvedPurchase> resolved = new ArrayList<>();
        int totalCost = 0;
        for (Map.Entry<SupplierKey, Integer> entry : requestedQuantityBySupplier.entrySet()) {
            SupplierOffer offer = offersBySupplier.get(entry.getKey());
            if (offer == null || !offer.supported()) {
                continue;
            }
            int quantity = entry.getValue();
            if (quantity > Math.max(0, buyer.getPopulation())) {
                addEvaluationError(errors, codes, ValidationReasonCode.PURCHASE_EXCEEDS_POPULATION_LIMIT,
                        "From each supplier, purchased quantity cannot exceed buyer population.");
            }
            if (quantity > offer.availableQuantity()) {
                addEvaluationError(errors, codes, ValidationReasonCode.SUPPLIER_INSUFFICIENT_QUANTITY,
                        "Supplier has insufficient quantity for " + resourceType.name() + ".");
                continue;
            }
            int cost = quantity * offer.unitPrice();
            totalCost += cost;
            resolved.add(new ResolvedPurchase(offer, quantity, cost));
        }

        if (!ignoreFundsCheck && totalCost > payerAvailableMoney(buyer)) {
            addEvaluationError(errors, codes, ValidationReasonCode.INSUFFICIENT_FUNDS,
                    "Buyer does not have enough money for requested purchases.");
        }

        return new PurchaseEvaluation(errors, codes, resolved, totalCost, unsupportedNotes);
    }

    public void applyPurchase(GameState state, PlayerState buyer, ResourceType resourceType, PurchaseEvaluation evaluation) {
        int totalQuantity = 0;
        int totalCost = 0;
        List<String> lines = new ArrayList<>();
        for (ResolvedPurchase purchase : evaluation.resolvedPurchases()) {
            SupplierOffer offer = purchase.offer();
            int quantity = purchase.quantity();
            int cost = purchase.totalCost();

            switch (offer.supplierType()) {
                case CAPITALIST, MIDDLE_CLASS -> {
                    PlayerState seller = state.findPlayerById(offer.supplierPlayerId()).orElseThrow();
                    seller.consumeProducedResource(resourceType.id(), quantity);
                    creditPlayerMoney(seller, cost);
                    state.appendLog(
                            "RESOURCE_PURCHASE_TRANSFER",
                            buyer.getPlayerId() + " paid " + cost + " to " + seller.getPlayerId()
                                    + " for " + quantity + " " + resourceType.id() + "."
                    );
                }
                case STATE -> {
                    state.consumePublicServiceAmount(resourceType.id(), quantity);
                    state.setTreasury(state.getTreasury() + cost);
                    state.appendLog(
                            "RESOURCE_PURCHASE_TRANSFER",
                            buyer.getPlayerId() + " paid " + cost + " to treasury for "
                                    + quantity + " " + resourceType.id() + "."
                    );
                }
                case EXTERNAL_MARKET -> {
                    // Payment leaves the game economy in current supported slice.
                    state.appendLog(
                            "RESOURCE_PURCHASE_TRANSFER",
                            buyer.getPlayerId() + " paid " + cost + " to external market for "
                                    + quantity + " " + resourceType.id() + "."
                    );
                }
            }

            debitBuyerMoney(buyer, cost);
            buyer.addGoods(resourceType.id(), quantity);
            totalQuantity += quantity;
            totalCost += cost;
            String supplierLabel = offer.supplierPlayerId() == null || offer.supplierPlayerId().isBlank()
                    ? offer.supplierType().name()
                    : offer.supplierType().name() + "(" + offer.supplierPlayerId() + ")";
            lines.add(supplierLabel + ": qty=" + quantity + ", cost=" + cost);
        }

        state.appendLog(
                "BUY_GOODS_AND_SERVICES",
                buyer.getPlayerId() + " bought " + totalQuantity + " " + resourceType.id()
                        + " for total " + totalCost + " [" + String.join("; ", lines) + "]."
        );
        if (!evaluation.unsupportedNotes().isEmpty()) {
            mergeEconomyUnsupportedNotes(state, evaluation.unsupportedNotes());
            state.appendLog("BUY_UNSUPPORTED_NOTES", String.join(" | ", evaluation.unsupportedNotes()));
        }
    }

    public int applyConsumption(GameState state, PlayerState actor, ResourceType resourceType) {
        int required = Math.max(0, actor.getPopulation());
        int consumed = actor.consumeGoods(resourceType.id(), required);
        if (consumed < required) {
            throw new IllegalStateException("Consumption became invalid after validation.");
        }

        int beforeWelfare = actor.getWelfare();
        actor.setWelfare(beforeWelfare + 1);
        actor.setLastWelfareDelta(1);
        int vpGain = Math.max(0, actor.getWelfare());
        actor.setVictoryPoints(actor.getVictoryPoints() + vpGain);

        state.appendLog(
                "CONSUMPTION",
                actor.getPlayerId() + " consumed " + required + " " + resourceType.id()
                        + " and welfare increased from " + beforeWelfare + " to " + actor.getWelfare()
                        + " (+" + vpGain + " VP)."
        );
        return vpGain;
    }

    private List<SupplierType> allowedSupplierTypesForBuyer(ClassType buyerClass) {
        if (buyerClass == ClassType.WORKER || buyerClass == ClassType.MIDDLE_CLASS) {
            return List.of(SupplierType.CAPITALIST, SupplierType.MIDDLE_CLASS, SupplierType.STATE, SupplierType.EXTERNAL_MARKET);
        }
        return List.of();
    }

    private String supplierPlayerIdFor(GameState state, PlayerState buyer, SupplierType supplierType) {
        return switch (supplierType) {
            case CAPITALIST -> findPlayerByClass(state, ClassType.CAPITALIST).map(PlayerState::getPlayerId).orElse(null);
            case MIDDLE_CLASS -> {
                PlayerState middle = findPlayerByClass(state, ClassType.MIDDLE_CLASS).orElse(null);
                if (middle == null) {
                    yield null;
                }
                if (buyer.getClassType() == ClassType.MIDDLE_CLASS) {
                    yield buyer.getPlayerId();
                }
                yield middle.getPlayerId();
            }
            case STATE, EXTERNAL_MARKET -> null;
        };
    }

    private String resolveSupplierPlayerIdFromRequest(GameState state, PlayerState buyer, PurchaseItem item) {
        if (item.supplierType() == SupplierType.EXTERNAL_MARKET || item.supplierType() == SupplierType.STATE) {
            return null;
        }
        if (item.supplierPlayerId() != null && !item.supplierPlayerId().isBlank()) {
            return item.supplierPlayerId();
        }
        if (item.supplierType() == SupplierType.MIDDLE_CLASS && buyer.getClassType() == ClassType.MIDDLE_CLASS) {
            return buyer.getPlayerId();
        }
        return supplierPlayerIdFor(state, buyer, item.supplierType());
    }

    private SupplierOffer resolveSupplierOffer(
            GameState state,
            PlayerState buyer,
            ResourceType resourceType,
            SupplierType supplierType,
            String supplierPlayerId
    ) {
        return resolveSupplierOffer(state, buyer, resourceType, supplierType, supplierPlayerId, null);
    }

    private SupplierOffer resolveSupplierOffer(
            GameState state,
            PlayerState buyer,
            ResourceType resourceType,
            SupplierType supplierType,
            String supplierPlayerId,
            Integer unitPriceOverride
    ) {
        if (!isSupportedConsumerBuyer(buyer.getClassType())) {
            return new SupplierOffer(supplierType, supplierPlayerId, 0, 0, false, ValidationReasonCode.NOT_SUPPORTED_BUYER_CLASS,
                    "NOT_SUPPORTED_BUYER_CLASS: buyer class is unsupported in current consumer slice.");
        }

        if (!allowedSupplierTypesForBuyer(buyer.getClassType()).contains(supplierType)) {
            return new SupplierOffer(supplierType, supplierPlayerId, 0, 0, false, ValidationReasonCode.UNSUPPORTED_SUPPLIER,
                    "UNSUPPORTED_SUPPLIER: supplier is not available for buyer class in current slice.");
        }

        return switch (supplierType) {
            case CAPITALIST -> {
                PlayerState seller = findPlayerByClass(state, ClassType.CAPITALIST).orElse(null);
                if (seller == null) {
                    yield new SupplierOffer(supplierType, supplierPlayerId, 0, 0, false, ValidationReasonCode.UNSUPPORTED_SUPPLIER,
                            "UNSUPPORTED_SUPPLIER: capitalist supplier is not active.");
                }
                if (resourceType == ResourceType.HEALTHCARE) {
                    yield new SupplierOffer(supplierType, seller.getPlayerId(), 0, 0, false, ValidationReasonCode.UNSUPPORTED_RESOURCE_PURCHASE_PATH,
                            "UNSUPPORTED_RESOURCE_PURCHASE_PATH: capitalist does not supply healthcare in current slice.");
                }
                int price = effectiveUnitPrice(seller.getPrice(resourceType.id()), unitPriceOverride);
                if (price <= 0 && unitPriceOverride == null) {
                    yield new SupplierOffer(supplierType, seller.getPlayerId(), 0, 0, false, ValidationReasonCode.UNSUPPORTED_RESOURCE_PURCHASE_PATH,
                            "UNSUPPORTED_RESOURCE_PURCHASE_PATH: capitalist price is unavailable for requested resource.");
                }
                int available = seller.getProducedResourceAmount(resourceType.id());
                yield new SupplierOffer(supplierType, seller.getPlayerId(), available, price, true, null, null);
            }
            case MIDDLE_CLASS -> {
                PlayerState seller = findPlayerByClass(state, ClassType.MIDDLE_CLASS).orElse(null);
                if (seller == null) {
                    yield new SupplierOffer(supplierType, supplierPlayerId, 0, 0, false, ValidationReasonCode.UNSUPPORTED_SUPPLIER,
                            "UNSUPPORTED_SUPPLIER: middle class supplier is not active.");
                }
                if (resourceType == ResourceType.INFLUENCE) {
                    yield new SupplierOffer(supplierType, seller.getPlayerId(), 0, 0, false, ValidationReasonCode.UNSUPPORTED_RESOURCE_PURCHASE_PATH,
                            "UNSUPPORTED_RESOURCE_PURCHASE_PATH: middle class influence sales are not modeled.");
                }
                int price = effectiveUnitPrice(seller.getPrice(resourceType.id()), unitPriceOverride);
                if (price <= 0 && unitPriceOverride == null) {
                    yield new SupplierOffer(supplierType, seller.getPlayerId(), 0, 0, false, ValidationReasonCode.UNSUPPORTED_RESOURCE_PURCHASE_PATH,
                            "UNSUPPORTED_RESOURCE_PURCHASE_PATH: middle class price is unavailable for requested resource.");
                }
                int available = seller.getProducedResourceAmount(resourceType.id());
                yield new SupplierOffer(supplierType, seller.getPlayerId(), available, price, true, null, null);
            }
            case STATE -> {
                if (resourceType != ResourceType.HEALTHCARE && resourceType != ResourceType.EDUCATION) {
                    yield new SupplierOffer(supplierType, null, 0, 0, false, ValidationReasonCode.SUPPLIER_CANNOT_SUPPLY_RESOURCE,
                            "SUPPLIER_CANNOT_SUPPLY_RESOURCE: state supplier supports only healthcare and education.");
                }
                int available = state.getPublicServiceAmount(resourceType.id());
                int unitPrice = effectiveUnitPrice(stateServiceUnitPrice(state, resourceType), unitPriceOverride);
                yield new SupplierOffer(supplierType, null, available, unitPrice, true, null, null);
            }
            case EXTERNAL_MARKET -> {
                if (resourceType != ResourceType.FOOD && resourceType != ResourceType.LUXURY) {
                    yield new SupplierOffer(supplierType, null, 0, 0, false, ValidationReasonCode.SUPPLIER_CANNOT_SUPPLY_RESOURCE,
                            "SUPPLIER_CANNOT_SUPPLY_RESOURCE: external market supports only food and luxury in current slice.");
                }
                int unitPrice = effectiveUnitPrice(externalMarketUnitPrice(state, resourceType), unitPriceOverride);
                yield new SupplierOffer(supplierType, null, Math.max(0, buyer.getPopulation()), unitPrice, true, null, null);
            }
        };
    }

    private int effectiveUnitPrice(int listedPrice, Integer unitPriceOverride) {
        if (unitPriceOverride != null) {
            return Math.max(0, unitPriceOverride);
        }
        return Math.max(0, listedPrice);
    }

    private int stateServiceUnitPrice(GameState state, ResourceType resourceType) {
        PolicyId policyId = resourceType == ResourceType.HEALTHCARE
                ? PolicyId.POLICY_4_HEALTHCARE_AND_BENEFITS
                : PolicyId.POLICY_5_EDUCATION;
        PolicyCourse course = state.findPolicy(policyId).map(PolicyState::getCurrentCourse).orElse(PolicyCourse.B);
        return switch (course) {
            case A -> 0;
            case B -> 5;
            case C -> 10;
        };
    }

    private int externalMarketUnitPrice(GameState state, ResourceType resourceType) {
        int basePrice = switch (resourceType) {
            case FOOD -> 10;
            case LUXURY -> 6;
            default -> Math.max(1, state.getMarket().getGoodsPrice());
        };
        PolicyCourse course = state.findPolicy(PolicyId.POLICY_6_FOREIGN_TRADE)
                .map(PolicyState::getCurrentCourse)
                .orElse(PolicyCourse.B);
        int surcharge = switch (course) {
            case A -> resourceType == ResourceType.FOOD ? 10 : 6;
            case B -> resourceType == ResourceType.FOOD ? 5 : 3;
            case C -> 0;
        };
        return Math.max(1, basePrice + surcharge);
    }

    private int payerAvailableMoney(PlayerState player) {
        if (player == null) {
            return 0;
        }
        return switch (player.getClassType()) {
            case CAPITALIST -> Math.max(0, player.getRevenue());
            default -> Math.max(0, player.getMoney());
        };
    }

    private void debitBuyerMoney(PlayerState player, int amount) {
        if (player == null || amount <= 0) {
            return;
        }
        int safe = Math.max(0, amount);
        if (player.getClassType() == ClassType.CAPITALIST) {
            player.setRevenue(Math.max(0, player.getRevenue() - safe));
        } else {
            player.setMoney(Math.max(0, player.getMoney() - safe));
        }
    }

    private void creditPlayerMoney(PlayerState player, int amount) {
        if (player == null || amount <= 0) {
            return;
        }
        if (player.getClassType() == ClassType.CAPITALIST) {
            player.setRevenue(player.getRevenue() + amount);
        } else {
            player.setMoney(player.getMoney() + amount);
        }
    }

    private void mergeEconomyUnsupportedNotes(GameState state, List<String> notes) {
        if (notes == null || notes.isEmpty()) {
            return;
        }
        List<String> merged = state.getEconomyUnsupportedNotes() == null
                ? new ArrayList<>()
                : new ArrayList<>(state.getEconomyUnsupportedNotes());
        for (String note : notes) {
            if (note == null || note.isBlank() || merged.contains(note)) {
                continue;
            }
            merged.add(note);
        }
        state.setEconomyUnsupportedNotes(merged);
    }

    private java.util.Optional<PlayerState> findPlayerByClass(GameState state, ClassType classType) {
        return state.getPlayers().stream().filter(player -> player.getClassType() == classType).findFirst();
    }

    private void addEvaluationError(
            List<String> errors,
            List<ValidationReasonCode> codes,
            ValidationReasonCode code,
            String message
    ) {
        errors.add(message);
        codes.add(code);
    }

    private record SupplierKey(SupplierType supplierType, String supplierPlayerId) {
    }

    public record SupplierOffer(
            SupplierType supplierType,
            String supplierPlayerId,
            int availableQuantity,
            int unitPrice,
            boolean supported,
            ValidationReasonCode unsupportedCode,
            String unsupportedNote
    ) {
    }

    public record ResolvedPurchase(SupplierOffer offer, int quantity, int totalCost) {
    }

    public record PurchaseEvaluation(
            List<String> errors,
            List<ValidationReasonCode> reasonCodes,
            List<ResolvedPurchase> resolvedPurchases,
            int totalCost,
            List<String> unsupportedNotes
    ) {
    }
}
