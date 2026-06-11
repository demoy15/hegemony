package com.example.hegemony.domain.production;

import com.example.hegemony.domain.model.ClassType;
import com.example.hegemony.domain.model.Enterprise;
import com.example.hegemony.domain.model.EnterpriseProductionResult;
import com.example.hegemony.domain.model.EnterpriseSlot;
import com.example.hegemony.domain.model.GameState;
import com.example.hegemony.domain.model.PlayerState;
import com.example.hegemony.domain.model.PolicyCourse;
import com.example.hegemony.domain.model.PolicyId;
import com.example.hegemony.domain.model.PolicyState;
import com.example.hegemony.domain.model.ProductionPhaseState;
import com.example.hegemony.domain.model.ResourceType;
import com.example.hegemony.domain.model.SupplierType;
import com.example.hegemony.domain.model.Worker;
import com.example.hegemony.domain.model.WorkerLocation;
import com.example.hegemony.domain.command.PurchaseItem;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductionResolver {
    private static final int STATE_SERVICE_STORAGE_BUFFER = 6;
    private static final int CAPITALIST_DEFAULT_RESOURCE_STORAGE_LIMIT = 12;
    private static final int CAPITALIST_FOOD_STORAGE_LIMIT = 8;
    private static final int MIDDLE_CLASS_RESOURCE_STORAGE_LIMIT = 8;
    private static final int FOOD_EXTRA_STORAGE_TOKEN_CAPACITY = 8;
    private static final int DEFAULT_EXTRA_STORAGE_TOKEN_CAPACITY = 12;
    private static final int STATE_LOAN_AMOUNT = 50;
    private static final int PLAYER_LOAN_AMOUNT = 50;
    private static final String LOAN_RESOURCE_ID = "loan";

    public void resolveGoodsAndServices(GameState state, ProductionPhaseState production) {
        resolveGoodsAndServices(state, production, List.of());
    }

    public void resolveGoodsAndServices(GameState state, ProductionPhaseState production, List<PurchaseItem> workerFoodPurchases) {
        List<EnterpriseProductionResult> results = new ArrayList<>();
        Map<String, Integer> stateServicesProducedThisRound = new HashMap<>();
        enforceStateEnterpriseWagePolicy(state);
        resolveDemonstrationToken(state);

        for (ClassType ownerClass : orderedProductionClasses()) {
            List<Enterprise> enterprises = state.getEnterprises().stream()
                    .filter(enterprise -> enterprise.getOwnerClass() == ownerClass)
                    .toList();
            for (Enterprise enterprise : enterprises) {
                EnterpriseProductionResult result = new EnterpriseProductionResult();
                result.setEnterpriseId(enterprise.getId());
                result.setFunctioning(enterprise.isFunctioning());
                String ownerPlayerId = findPlayerByClass(state, ownerClass).map(PlayerState::getPlayerId).orElse(ownerClass.playerId());
                result.setOwnerPlayerId(ownerPlayerId);

                if (enterprise.isStrikeToken() && enterprise.getWageLevel() >= 3) {
                    enterprise.setStrikeToken(false);
                    state.appendLog("STRIKE_AVERTED", enterprise.getId() + " reached wage level 3 before production; strike token removed.");
                }

                if (enterprise.isStrikeToken()) {
                    result.setFunctioning(false);
                    resolveStrikeToken(state, enterprise);
                    results.add(result);
                    continue;
                }

                if (enterprise.isFunctioning()) {
                    payEnterpriseWages(state, ownerClass, enterprise, result);
                    produceEnterpriseResources(state, ownerClass, enterprise, result, stateServicesProducedThisRound);
                }

                results.add(result);
            }
        }

        releaseTiedContracts(state);
        grantWorkerUnionInfluence(state);
        production.setEnterpriseResults(results);
        satisfyFoodNeeds(state, production, workerFoodPurchases == null ? List.of() : workerFoodPurchases);
        checkImfIntervention(state);
        payProductionTaxes(state, production);
    }

    private void resolveStrikeToken(GameState state, Enterprise enterprise) {
        PlayerState worker = findPlayerByClass(state, ClassType.WORKER).orElse(null);
        if (worker != null) {
            worker.setInfluence(worker.getInfluence() + 1);
        }
        if (enterprise.getOwnerClass() == ClassType.MIDDLE_CLASS) {
            addUnsupportedNote(state,
                    "UNSUPPORTED_MIDDLE_CLASS_STRIKE_PARTIAL_PRODUCTION: owner-only production share is not separated in current enterprise model.");
        }
        state.appendLog(
                "STRIKE_RESOLVED",
                enterprise.getId() + " skipped production and wages due to strike; worker class gained 1 influence."
        );
        enterprise.setStrikeToken(false);
    }

    private void resolveDemonstrationToken(GameState state) {
        if (!state.isDemonstrationToken()) {
            return;
        }

        PlayerState worker = findPlayerByClass(state, ClassType.WORKER).orElse(null);
        if (worker != null) {
            worker.setInfluence(worker.getInfluence() + 1);
        }

        int penaltyPool = demonstrationPenaltyPool(state);
        int remaining = penaltyPool;
        Map<String, Integer> requestedAllocation = state.getDemonstrationPenaltyAllocation();
        if (requestedAllocation == null || requestedAllocation.isEmpty()) {
            requestedAllocation = defaultDemonstrationPenaltyAllocation(state, penaltyPool);
        }

        for (PlayerState opponent : demonstrationOpponentsInPriorityOrder(state)) {
            if (remaining <= 0) {
                break;
            }
            int requested = Math.max(0, requestedAllocation.getOrDefault(opponent.getPlayerId(), 0));
            if (requested <= 0) {
                continue;
            }
            int cap = demonstrationPenaltyCap(state, opponent);
            int loss = Math.min(remaining, Math.min(cap, Math.min(requested, Math.max(0, opponent.getVictoryPoints()))));
            if (loss <= 0) {
                continue;
            }
            opponent.setVictoryPoints(opponent.getVictoryPoints() - loss);
            remaining -= loss;
            state.appendLog("DEMONSTRATION_VP_LOSS", opponent.getPlayerId() + " lost " + loss + " VP due to demonstration.");
        }

        state.appendLog(
                "DEMONSTRATION_RESOLVED",
                "Worker class gained 1 influence from demonstration; VP penalty pool was " + penaltyPool + "."
        );
        state.setDemonstrationToken(false);
        state.setDemonstrationPenaltyAllocation(Map.of());
    }

    private Map<String, Integer> defaultDemonstrationPenaltyAllocation(GameState state, int penaltyPool) {
        Map<String, Integer> allocation = new java.util.HashMap<>();
        int remaining = Math.max(0, penaltyPool);
        for (PlayerState opponent : demonstrationOpponentsInPriorityOrder(state)) {
            if (remaining <= 0) {
                break;
            }
            int amount = Math.min(remaining, demonstrationPenaltyCap(state, opponent));
            allocation.put(opponent.getPlayerId(), amount);
            remaining -= amount;
        }
        return allocation;
    }

    private List<PlayerState> demonstrationOpponentsInPriorityOrder(GameState state) {
        List<PlayerState> opponents = new ArrayList<>();
        for (ClassType classType : List.of(ClassType.CAPITALIST, ClassType.MIDDLE_CLASS, ClassType.STATE)) {
            findPlayerByClass(state, classType).ifPresent(opponents::add);
        }
        return opponents;
    }

    private int demonstrationPenaltyPool(GameState state) {
        int unemployed = (int) state.getWorkers().stream()
                .filter(worker -> worker.getClassType() == ClassType.WORKER)
                .filter(worker -> worker.getLocation() == WorkerLocation.UNEMPLOYED)
                .count();
        int unions = (int) state.getWorkers().stream()
                .filter(worker -> worker.getClassType() == ClassType.WORKER)
                .filter(worker -> worker.getLocation() == WorkerLocation.UNION)
                .count();
        return unemployed + unions;
    }

    private int demonstrationPenaltyCap(GameState state, PlayerState opponent) {
        int ownedEnterprises = (int) state.getEnterprises().stream()
                .filter(enterprise -> enterprise.getOwnerClass() == opponent.getClassType())
                .count();
        return Math.max(0, 6 - ownedEnterprises);
    }

    private void payEnterpriseWages(GameState state, ClassType ownerClass, Enterprise enterprise, EnterpriseProductionResult result) {
        Map<ClassType, Integer> workersByClass = new EnumMap<>(ClassType.class);
        int occupiedSlots = 0;
        for (EnterpriseSlot slot : enterprise.getSlots()) {
            if (slot.getOccupiedWorkerId() == null || slot.getOccupiedWorkerId().isBlank()) {
                continue;
            }
            Worker worker = state.findWorker(slot.getOccupiedWorkerId()).orElse(null);
            if (worker == null || worker.getClassType() == null) {
                continue;
            }
            occupiedSlots++;
            workersByClass.merge(worker.getClassType(), 1, Integer::sum);
        }

        int wageTotalForEnterprise = resolveWagePerEnterprise(state, ownerClass, enterprise);
        int paidShares = 0;
        int recipientIndex = 0;
        for (Map.Entry<ClassType, Integer> entry : workersByClass.entrySet()) {
            recipientIndex++;
            int wageTotal = occupiedSlots <= 0
                    ? 0
                    : recipientIndex == workersByClass.size()
                    ? Math.max(0, wageTotalForEnterprise - paidShares)
                    : Math.max(0, wageTotalForEnterprise * entry.getValue() / occupiedSlots);
            paidShares += wageTotal;
            int paid = payFromOwnerPool(state, ownerClass, wageTotal);
            if (paid < wageTotal) {
                state.appendLog(
                        "WAGE_PARTIAL",
                        "Owner " + ownerClass + " could not fully pay wages for " + enterprise.getId()
                                + " (" + paid + "/" + wageTotal + ")."
                );
            }
            PlayerState recipient = findPlayerByClass(state, entry.getKey()).orElse(null);
            if (recipient != null) {
                creditPlayerMoney(recipient, paid);
                result.getWagesPaidByRecipient().merge(recipient.getPlayerId(), paid, Integer::sum);
                state.appendLog(
                        "WAGE_PAID",
                        ownerPlayerId(ownerClass) + " paid " + paid + " wages to " + recipient.getPlayerId()
                                + " for " + entry.getValue() + " worker(s) at " + enterprise.getId()
                                + " (enterprise wage total " + wageTotalForEnterprise + ")."
                );
            }
        }
    }

    private void produceEnterpriseResources(
            GameState state,
            ClassType ownerClass,
            Enterprise enterprise,
            EnterpriseProductionResult result,
            Map<String, Integer> stateServicesProducedThisRound
    ) {
        for (Map.Entry<String, Integer> produced : enterprise.getProducedResources().entrySet()) {
            if (produced.getValue() <= 0) {
                continue;
            }
            PlayerState owner = findPlayerByClass(state, ownerClass).orElse(null);
            if (ownerClass == ClassType.STATE) {
                String resourceId = normalizedResourceId(produced.getKey());
                int producedSoFar = stateServicesProducedThisRound.getOrDefault(resourceId, 0);
                int limit = producedSoFar + produced.getValue() + STATE_SERVICE_STORAGE_BUFFER;
                int accepted = Math.min(produced.getValue(), Math.max(0, limit - state.getPublicServiceAmount(resourceId)));
                if (accepted > 0) {
                    state.addPublicServiceAmount(resourceId, accepted);
                    result.getProducedResources().merge(resourceId, accepted, Integer::sum);
                    stateServicesProducedThisRound.merge(resourceId, accepted, Integer::sum);
                    state.appendLog(
                            "ENTERPRISE_PRODUCED",
                            ownerPlayerId(ownerClass) + " produced " + accepted + " " + resourceId
                                    + " from " + enterprise.getId() + " into public services storage."
                    );
                }
                int burned = produced.getValue() - accepted;
                if (burned > 0) {
                    state.appendLog("PRODUCTION_OVERFLOW", "State production overflow burned "
                            + burned + " " + produced.getKey() + " from " + enterprise.getId() + ".");
                }
                continue;
            }
            if (owner == null) {
                state.appendLog("PRODUCTION_UNSUPPORTED", "No owner player state found for enterprise " + enterprise.getId() + ".");
                continue;
            }
            if (ownerClass == ClassType.WORKER) {
                owner.addGoods(produced.getKey(), produced.getValue());
                result.getProducedResources().merge(produced.getKey(), produced.getValue(), Integer::sum);
                state.appendLog(
                        "ENTERPRISE_PRODUCED",
                        owner.getPlayerId() + " produced " + produced.getValue() + " " + produced.getKey()
                                + " from " + enterprise.getId() + " into goods/services area."
                );
                continue;
            }
            StoredProduction stored = storeProducedResource(state, ownerClass, owner, produced.getKey(), produced.getValue());
            int accepted = stored.standardStored();
            if (accepted > 0) {
                result.getProducedResources().merge(produced.getKey(), accepted, Integer::sum);
                state.appendLog(
                        "ENTERPRISE_PRODUCED",
                        owner.getPlayerId() + " produced " + accepted + " " + produced.getKey()
                                + " from " + enterprise.getId() + " into produced resource storage."
                );
            }
            if (stored.freeTradeStored() > 0) {
                result.getProducedResources().merge(produced.getKey(), stored.freeTradeStored(), Integer::sum);
                state.appendLog(
                        "FREE_TRADE_ZONE_STORED",
                        owner.getPlayerId() + " stored " + stored.freeTradeStored() + " " + produced.getKey()
                                + " from " + enterprise.getId() + " in the free trade zone."
                );
            }
            int burned = stored.burned();
            if (burned > 0) {
                state.appendLog("PRODUCTION_OVERFLOW", ownerClass + " production overflow burned "
                        + burned + " " + produced.getKey() + " from " + enterprise.getId() + ".");
            }
        }
        if (ownerClass == ClassType.WORKER && enterprise.getProducedResources().isEmpty()) {
            PlayerState owner = findPlayerByClass(state, ownerClass).orElse(null);
            if (owner != null) {
                owner.addGoods(ResourceType.FOOD.id(), 2);
                result.getProducedResources().merge(ResourceType.FOOD.id(), 2, Integer::sum);
                state.appendLog(
                        "ENTERPRISE_PRODUCED",
                        owner.getPlayerId() + " produced 2 food from " + enterprise.getId()
                                + " into goods/services area."
                );
            }
        }
    }

    private void releaseTiedContracts(GameState state) {
        int released = 0;
        for (Worker worker : state.getWorkers()) {
            if (worker.getLocation() == WorkerLocation.ENTERPRISE_SLOT && worker.isTiedContract()) {
                worker.setTiedContract(false);
                released++;
            }
        }
        if (released > 0) {
            state.appendLog("CONTRACTS_RELEASED", "Released tied contracts after production: " + released + " workers.");
        }
    }

    private void grantWorkerUnionInfluence(GameState state) {
        PlayerState worker = findPlayerByClass(state, ClassType.WORKER).orElse(null);
        if (worker == null) {
            return;
        }
        int unionWorkers = (int) state.getWorkers().stream()
                .filter(candidate -> candidate.getClassType() == ClassType.WORKER)
                .filter(candidate -> candidate.getLocation() == WorkerLocation.UNION)
                .count();
        if (unionWorkers > 0) {
            worker.setInfluence(worker.getInfluence() + unionWorkers);
            state.appendLog("UNION_INFLUENCE", "Worker class gained " + unionWorkers + " influence from unions.");
        }
    }

    private void satisfyFoodNeeds(GameState state, ProductionPhaseState production, List<PurchaseItem> workerFoodPurchases) {
        satisfyFoodNeedFor(state, production, ClassType.WORKER, workerFoodPurchases);
        satisfyFoodNeedFor(state, production, ClassType.MIDDLE_CLASS, List.of());
    }

    private void satisfyFoodNeedFor(GameState state, ProductionPhaseState production, ClassType classType, List<PurchaseItem> foodPurchases) {
        PlayerState buyer = findPlayerByClass(state, classType).orElse(null);
        if (buyer == null) {
            return;
        }
        int required = Math.max(0, buyer.getPopulation());
        int consumed = buyer.consumeGoods(ResourceType.FOOD.id(), required);
        int missing = required - consumed;
        if (missing > 0) {
            PurchaseResult purchased = buyFoodForNeed(state, buyer, missing, foodPurchases);
            consumed += purchased.quantity();
            missing -= purchased.quantity();
        }

        if (classType == ClassType.WORKER) {
            production.setWorkerFoodRequired(required);
            production.setWorkerFoodConsumed(consumed);
            production.setWorkerFoodUnmet(Math.max(0, missing));
        } else if (classType == ClassType.MIDDLE_CLASS) {
            production.setMiddleClassFoodRequired(required);
            production.setMiddleClassFoodConsumed(consumed);
            production.setMiddleClassFoodUnmet(Math.max(0, missing));
        }

        if (missing > 0) {
            production.setInsufficientFood(true);
            production.setUnsupportedMarketAcquisition(true);
            state.appendLog("FOOD_NEED_UNMET", buyer.getPlayerId() + " could not satisfy " + missing + " food.");
        }
    }

    private PurchaseResult buyFoodForNeed(GameState state, PlayerState buyer, int requested, List<PurchaseItem> foodPurchases) {
        if (foodPurchases != null && !foodPurchases.isEmpty()) {
            PurchaseResult planned = buyFoodForNeedFromPlan(state, buyer, requested, foodPurchases);
            if (planned.quantity() > 0) {
                state.appendLog("SATISFY_FOOD_NEED", buyer.getPlayerId() + " satisfied " + planned.quantity() + " food need for " + planned.cost() + ".");
            }
            return planned;
        }
        int remaining = Math.max(0, requested);
        int bought = 0;
        int spent = 0;

        for (ClassType sellerClass : List.of(ClassType.CAPITALIST, ClassType.MIDDLE_CLASS)) {
            if (remaining <= 0 || sellerClass == buyer.getClassType()) {
                continue;
            }
            PlayerState seller = findPlayerByClass(state, sellerClass).orElse(null);
            if (seller == null) {
                continue;
            }
            int unitPrice = Math.max(0, seller.getPrice(ResourceType.FOOD.id()));
            int available = Math.max(0, seller.getProducedResourceAmount(ResourceType.FOOD.id()));
            if (unitPrice <= 0 || available <= 0) {
                continue;
            }
            int quantity = Math.min(remaining, available);
            int cost = quantity * unitPrice;
            seller.consumeProducedResource(ResourceType.FOOD.id(), quantity);
            debitWithLoanFallback(state, buyer, cost, "mandatory food purchase");
            creditPlayerMoney(seller, cost);
            state.appendLog(
                    "FOOD_PURCHASE_PAID",
                    buyer.getPlayerId() + " paid " + cost + " to " + seller.getPlayerId()
                            + " for " + quantity + " food at price " + unitPrice + "."
            );
            bought += quantity;
            spent += cost;
            remaining -= quantity;
        }

        if (remaining > 0) {
            int unitPrice = externalMarketFoodPrice(state);
            int cost = remaining * unitPrice;
            debitWithLoanFallback(state, buyer, cost, "mandatory external food purchase");
            state.appendLog(
                    "FOOD_PURCHASE_PAID",
                    buyer.getPlayerId() + " paid " + cost + " to external market for " + remaining
                            + " food at price " + unitPrice + "."
            );
            bought += remaining;
            spent += cost;
            remaining = 0;
        }

        if (bought > 0) {
            state.appendLog("SATISFY_FOOD_NEED", buyer.getPlayerId() + " satisfied " + bought + " food need for " + spent + ".");
        }
        return new PurchaseResult(bought, spent);
    }

    private PurchaseResult buyFoodForNeedFromPlan(GameState state, PlayerState buyer, int requested, List<PurchaseItem> foodPurchases) {
        int remaining = Math.max(0, requested);
        int bought = 0;
        int spent = 0;
        for (PurchaseItem item : foodPurchases) {
            if (remaining <= 0 || item == null || item.supplierType() == null || item.quantity() <= 0) {
                continue;
            }
            int quantity = Math.min(remaining, item.quantity());
            PurchaseResult purchase = buyFoodFromSupplier(state, buyer, item, quantity);
            bought += purchase.quantity();
            spent += purchase.cost();
            remaining -= purchase.quantity();
        }
        return new PurchaseResult(bought, spent);
    }

    private PurchaseResult buyFoodFromSupplier(GameState state, PlayerState buyer, PurchaseItem item, int requested) {
        if (item.supplierType() == SupplierType.EXTERNAL_MARKET) {
            int unitPrice = item.unitPriceOverride() == null ? externalMarketFoodPrice(state) : Math.max(0, item.unitPriceOverride());
            int quantity = Math.max(0, requested);
            int cost = quantity * unitPrice;
            debitWithLoanFallback(state, buyer, cost, "mandatory external food purchase");
            state.appendLog(
                    "FOOD_PURCHASE_PAID",
                    buyer.getPlayerId() + " paid " + cost + " to external market for " + quantity
                            + " food at price " + unitPrice + "."
            );
            return new PurchaseResult(quantity, cost);
        }

        ClassType sellerClass = item.supplierType() == SupplierType.CAPITALIST
                ? ClassType.CAPITALIST
                : item.supplierType() == SupplierType.MIDDLE_CLASS
                ? ClassType.MIDDLE_CLASS
                : null;
        if (sellerClass == null || sellerClass == buyer.getClassType()) {
            return new PurchaseResult(0, 0);
        }
        PlayerState seller = findPlayerByClass(state, sellerClass).orElse(null);
        if (seller == null) {
            return new PurchaseResult(0, 0);
        }
        int unitPrice = item.unitPriceOverride() == null ? Math.max(0, seller.getPrice(ResourceType.FOOD.id())) : Math.max(0, item.unitPriceOverride());
        int available = Math.max(0, seller.getProducedResourceAmount(ResourceType.FOOD.id()));
        int quantity = Math.min(Math.max(0, requested), available);
        if (unitPrice <= 0 || quantity <= 0) {
            return new PurchaseResult(0, 0);
        }
        int cost = quantity * unitPrice;
        seller.consumeProducedResource(ResourceType.FOOD.id(), quantity);
        debitWithLoanFallback(state, buyer, cost, "mandatory food purchase");
        creditPlayerMoney(seller, cost);
        state.appendLog(
                "FOOD_PURCHASE_PAID",
                buyer.getPlayerId() + " paid " + cost + " to " + seller.getPlayerId()
                        + " for " + quantity + " food at price " + unitPrice + "."
        );
        return new PurchaseResult(quantity, cost);
    }

    private int externalMarketFoodPrice(GameState state) {
        PolicyCourse course = policyCourse(state, PolicyId.POLICY_6_FOREIGN_TRADE, PolicyCourse.B);
        int surcharge = switch (course) {
            case A -> 10;
            case B -> 5;
            case C -> 0;
        };
        return 10 + surcharge;
    }

    private void checkImfIntervention(GameState state) {
        addUnsupportedNote(state, "UNSUPPORTED_IMF_INTERVENTION: IMF thresholds are not modeled in current production slice.");
        state.appendLog("IMF_CHECK_SKIPPED", "IMF intervention check skipped because IMF thresholds are not modeled.");
    }

    private void payProductionTaxes(GameState state, ProductionPhaseState production) {
        int taxMultiplier = capitalistEmploymentTaxMultiplier(state);
        state.setTaxMultiplier(taxMultiplier);
        production.setCapitalistTaxesPaid(payCapitalistTaxes(state, taxMultiplier));
        production.setMiddleClassTaxesPaid(payMiddleClassTaxes(state, taxMultiplier));
        production.setWorkerTaxesPaid(payWorkerIncomeTax(state));
        production.setUnsupportedCapitalistTaxModel(false);
        production.setUnsupportedMiddleClassTaxModel(false);
    }

    private int payWorkerIncomeTax(GameState state) {
        PlayerState worker = findPlayerByClass(state, ClassType.WORKER).orElse(null);
        if (worker == null) {
            return 0;
        }
        int tax = Math.max(0, worker.getPopulation()) * workerIncomeTaxRate(state);
        int paid = debitWithLoanFallback(state, worker, tax, "worker income tax");
        state.setTreasury(state.getTreasury() + tax);
        state.appendLog("WORKER_TAX_PAID", "worker paid " + tax + " worker income tax to treasury"
                + " (population " + worker.getPopulation() + " x rate " + workerIncomeTaxRate(state)
                + ", cash covered " + paid + ").");
        return tax;
    }

    private int payCapitalistTaxes(GameState state, int taxMultiplier) {
        PlayerState capitalist = findPlayerByClass(state, ClassType.CAPITALIST).orElse(null);
        if (capitalist == null) {
            return 0;
        }
        int functioningEnterprises = countFunctioningEnterprises(state, ClassType.CAPITALIST);
        int employmentTax = functioningEnterprises * taxMultiplier;
        int revenueBase = Math.max(0, capitalist.getRevenue());
        int paidEmployment = debitWithLoanFallback(state, capitalist, employmentTax, "capitalist employment tax");
        int profitTax = capitalistProfitTax(state, revenueBase);
        int paidProfit = debitWithLoanFallback(state, capitalist, profitTax, "capitalist profit tax");
        int total = employmentTax + profitTax;
        state.setTreasury(state.getTreasury() + total);
        state.appendLog("CAPITALIST_TAX_PAID", "capitalist paid " + total + " taxes to treasury: employment "
                + employmentTax + " (" + functioningEnterprises + " functioning enterprise(s) x multiplier "
                + taxMultiplier + "), profit " + profitTax + " (revenue base " + revenueBase
                + ", cash covered " + (paidEmployment + paidProfit) + ").");
        return total;
    }

    private int payMiddleClassTaxes(GameState state, int taxMultiplier) {
        PlayerState middleClass = findPlayerByClass(state, ClassType.MIDDLE_CLASS).orElse(null);
        if (middleClass == null) {
            return 0;
        }
        int outsideWorkers = countMiddleClassWorkersOnOtherEmployers(state);
        int incomeTax = outsideWorkers * workerIncomeTaxRate(state);
        int employmentTax = countFunctioningEnterprises(state, ClassType.MIDDLE_CLASS) * taxMultiplier;
        int total = incomeTax + employmentTax;
        int paid = debitWithLoanFallback(state, middleClass, total, "middle class taxes");
        state.setTreasury(state.getTreasury() + total);
        state.appendLog("MIDDLE_CLASS_TAX_PAID", "middle_class paid " + total + " taxes to treasury: income "
                + incomeTax + " (" + outsideWorkers + " worker(s) on other employers x rate "
                + workerIncomeTaxRate(state) + "), employment " + employmentTax + " ("
                + countFunctioningEnterprises(state, ClassType.MIDDLE_CLASS) + " functioning enterprise(s) x multiplier "
                + taxMultiplier + ", cash covered " + paid + ").");
        return total;
    }

    private String ownerPlayerId(ClassType ownerClass) {
        return ownerClass == null ? "unknown" : ownerClass.playerId();
    }

    private int countMiddleClassWorkersOnOtherEmployers(GameState state) {
        int count = 0;
        for (Worker worker : state.getWorkers()) {
            if (worker.getClassType() != ClassType.MIDDLE_CLASS || worker.getLocation() != WorkerLocation.ENTERPRISE_SLOT) {
                continue;
            }
            Enterprise enterprise = findEnterpriseById(state, worker.getEnterpriseId());
            if (enterprise != null && (enterprise.getOwnerClass() == ClassType.CAPITALIST || enterprise.getOwnerClass() == ClassType.STATE)) {
                count++;
            }
        }
        return count;
    }

    private int countFunctioningEnterprises(GameState state, ClassType ownerClass) {
        return (int) state.getEnterprises().stream()
                .filter(enterprise -> enterprise.getOwnerClass() == ownerClass)
                .filter(Enterprise::isFunctioning)
                .count();
    }

    private int workerIncomeTaxRate(GameState state) {
        PolicyCourse labor = policyCourse(state, PolicyId.POLICY_2_LABOR_MARKET, PolicyCourse.B);
        PolicyCourse tax = policyCourse(state, PolicyId.POLICY_3_TAXATION, PolicyCourse.B);
        if (labor == PolicyCourse.A) {
            return switch (tax) {
                case A -> 7;
                case B -> 6;
                case C -> 5;
            };
        }
        if (labor == PolicyCourse.B) {
            return 4;
        }
        return switch (tax) {
            case A -> 1;
            case B -> 2;
            case C -> 3;
        };
    }

    private int capitalistEmploymentTaxMultiplier(GameState state) {
        PolicyCourse tax = policyCourse(state, PolicyId.POLICY_3_TAXATION, PolicyCourse.B);
        int socialModifier = socialPolicyModifier(policyCourse(state, PolicyId.POLICY_4_HEALTHCARE_AND_BENEFITS, PolicyCourse.B))
                + socialPolicyModifier(policyCourse(state, PolicyId.POLICY_5_EDUCATION, PolicyCourse.C));
        return switch (tax) {
            case A -> 3 + socialModifier * 2;
            case B -> 2 + socialModifier;
            case C -> 1;
        };
    }

    private int socialPolicyModifier(PolicyCourse course) {
        return switch (course) {
            case A -> 2;
            case B -> 1;
            case C -> 0;
        };
    }

    private int capitalistProfitTax(GameState state, int remainingRevenue) {
        if (remainingRevenue < 5) {
            return 0;
        }
        PolicyCourse tax = policyCourse(state, PolicyId.POLICY_3_TAXATION, PolicyCourse.B);
        if (remainingRevenue <= 9) {
            return tax == PolicyCourse.A ? 1 : 2;
        }
        if (remainingRevenue <= 24) {
            return tax == PolicyCourse.C ? 4 : 5;
        }
        if (remainingRevenue <= 49) {
            return switch (tax) {
                case A -> 12;
                case B -> 10;
                case C -> 7;
            };
        }
        if (remainingRevenue <= 99) {
            return switch (tax) {
                case A -> 24;
                case B -> 15;
                case C -> 10;
            };
        }
        if (remainingRevenue <= 199) {
            return switch (tax) {
                case A -> 40;
                case B -> 30;
                case C -> 20;
            };
        }
        if (remainingRevenue <= 299) {
            return switch (tax) {
                case A -> 100;
                case B -> 70;
                case C -> 40;
            };
        }
        return switch (tax) {
            case A -> 160;
            case B -> 120;
            case C -> 60;
        };
    }

    private PolicyCourse policyCourse(GameState state, PolicyId policyId, PolicyCourse fallback) {
        return state.findPolicy(policyId).map(PolicyState::getCurrentCourse).orElse(fallback);
    }

    private Enterprise findEnterpriseById(GameState state, String enterpriseId) {
        if (enterpriseId == null || enterpriseId.isBlank()) {
            return null;
        }
        return state.getEnterprises().stream()
                .filter(enterprise -> enterpriseId.equals(enterprise.getId()))
                .findFirst()
                .orElse(null);
    }

    private int debitWithLoanFallback(GameState state, PlayerState player, int amount, String reason) {
        if (player == null || amount <= 0) {
            return 0;
        }
        ensurePlayerCanPayMandatory(state, player, amount, reason);
        return consumePlayerFunds(player, amount);
    }

    private void addUnsupportedNote(GameState state, String note) {
        if (note == null || note.isBlank() || state.getEconomyUnsupportedNotes().contains(note)) {
            return;
        }
        state.getEconomyUnsupportedNotes().add(note);
    }

    private StoredProduction storeProducedResource(GameState state, ClassType ownerClass, PlayerState owner, String rawResourceId, int producedAmount) {
        String resourceId = normalizedResourceId(rawResourceId);
        int remaining = Math.max(0, producedAmount);
        if (remaining <= 0 || owner == null) {
            return new StoredProduction(0, 0, 0);
        }

        ResourceType resourceType = ResourceType.fromRaw(resourceId);
        if (resourceType == ResourceType.INFLUENCE || resourceType == ResourceType.MEDIA_INFLUENCE) {
            if (ownerClass == ClassType.MIDDLE_CLASS) {
                owner.addGoods(resourceId, remaining);
            } else {
                owner.addProducedResource(resourceId, remaining);
            }
            return new StoredProduction(remaining, 0, 0);
        }

        int standardLimit = storageLimitFor(ownerClass, owner, resourceId);
        int standardStored;
        if (standardLimit < 0) {
            standardStored = remaining;
        } else {
            int availableSpace = Math.max(0, standardLimit - owner.getProducedResourceAmount(resourceId));
            standardStored = Math.min(remaining, availableSpace);
        }
        if (standardStored > 0) {
            owner.addProducedResource(resourceId, standardStored);
            remaining -= standardStored;
        }

        int freeTradeStored = 0;
        if (ownerClass == ClassType.CAPITALIST && isFreeTradeOverflowCandidate(resourceId) && remaining > 0) {
            freeTradeStored = remaining;
            owner.addFreeTradeZoneResource(resourceId, freeTradeStored);
            remaining = 0;
        }

        return new StoredProduction(standardStored, freeTradeStored, remaining);
    }

    private int storageLimitFor(ClassType ownerClass, PlayerState owner, String resourceId) {
        if (ownerClass == ClassType.CAPITALIST) {
            return capitalistResourceStorageLimit(owner, resourceId);
        }
        if (ownerClass == ClassType.MIDDLE_CLASS) {
            return MIDDLE_CLASS_RESOURCE_STORAGE_LIMIT;
        }
        return -1;
    }

    private int capitalistResourceStorageLimit(PlayerState owner, String resourceId) {
        ResourceType resourceType = ResourceType.fromRaw(resourceId);
        if (resourceType == ResourceType.INFLUENCE || resourceType == ResourceType.MEDIA_INFLUENCE) {
            return -1;
        }
        int base = resourceType == ResourceType.FOOD
                ? CAPITALIST_FOOD_STORAGE_LIMIT
                : CAPITALIST_DEFAULT_RESOURCE_STORAGE_LIMIT;
        return base + owner.getExtraStorageTokens(resourceId) * extraStorageTokenCapacity(resourceType);
    }

    private int extraStorageTokenCapacity(ResourceType resourceType) {
        return resourceType == ResourceType.FOOD
                ? FOOD_EXTRA_STORAGE_TOKEN_CAPACITY
                : DEFAULT_EXTRA_STORAGE_TOKEN_CAPACITY;
    }

    private boolean isFreeTradeOverflowCandidate(String resourceId) {
        ResourceType resourceType = ResourceType.fromRaw(resourceId);
        return resourceType == ResourceType.FOOD || resourceType == ResourceType.LUXURY;
    }

    private String normalizedResourceId(String resourceId) {
        ResourceType resourceType = ResourceType.fromRaw(resourceId);
        return resourceType == null ? resourceId.toLowerCase() : resourceType.id();
    }

    private record StoredProduction(int standardStored, int freeTradeStored, int burned) {
    }

    private List<ClassType> orderedProductionClasses() {
        return List.of(
                ClassType.STATE,
                ClassType.CAPITALIST,
                ClassType.MIDDLE_CLASS,
                ClassType.WORKER
        );
    }

    private int payFromOwnerPool(GameState state, ClassType ownerClass, int amount) {
        if (amount <= 0) {
            return 0;
        }
        if (ownerClass == ClassType.STATE) {
            ensureTreasuryCanPay(state, amount, "state enterprise wages");
            state.setTreasury(Math.max(0, state.getTreasury() - amount));
            return amount;
        }

        PlayerState owner = findPlayerByClass(state, ownerClass).orElse(null);
        if (owner == null) {
            return 0;
        }

        ensurePlayerCanPayMandatory(state, owner, amount, "enterprise wages");
        return consumePlayerFunds(owner, amount);
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

    private void ensurePlayerCanPayMandatory(GameState state, PlayerState player, int amount, String reason) {
        while (availablePlayerFunds(player) < amount) {
            takePlayerLoan(state, player, reason);
        }
    }

    private void takePlayerLoan(GameState state, PlayerState player, String reason) {
        player.addResource(LOAN_RESOURCE_ID, 1);
        if (player.getClassType() == ClassType.CAPITALIST) {
            player.setCapital(player.getCapital() + PLAYER_LOAN_AMOUNT);
        } else {
            player.setMoney(player.getMoney() + PLAYER_LOAN_AMOUNT);
        }
        state.appendLog(
                "PLAYER_LOAN_TAKEN",
                player.getPlayerId() + " took a 50 loan for " + reason
                        + "; active loans: " + player.getResourceAmount(LOAN_RESOURCE_ID) + "."
        );
    }

    private int availablePlayerFunds(PlayerState player) {
        if (player == null) {
            return 0;
        }
        if (player.getClassType() == ClassType.CAPITALIST) {
            return Math.max(0, player.getRevenue()) + Math.max(0, player.getCapital()) + Math.max(0, player.getMoney());
        }
        return Math.max(0, player.getMoney());
    }

    private int consumePlayerFunds(PlayerState player, int requested) {
        if (player == null || requested <= 0) {
            return 0;
        }
        if (player.getClassType() != ClassType.CAPITALIST) {
            int paid = Math.min(Math.max(0, player.getMoney()), requested);
            player.setMoney(player.getMoney() - paid);
            return paid;
        }

        int remaining = requested;
        int fromRevenue = Math.min(Math.max(0, player.getRevenue()), remaining);
        player.setRevenue(player.getRevenue() - fromRevenue);
        remaining -= fromRevenue;

        int fromCapital = Math.min(Math.max(0, player.getCapital()), remaining);
        player.setCapital(player.getCapital() - fromCapital);
        remaining -= fromCapital;

        int fromMoney = Math.min(Math.max(0, player.getMoney()), remaining);
        player.setMoney(player.getMoney() - fromMoney);
        remaining -= fromMoney;
        return requested - remaining;
    }

    private int resolveWagePerEnterprise(GameState state, ClassType ownerClass, Enterprise enterprise) {
        if (ownerClass == ClassType.STATE) {
            enterprise.setWageTrack(stateEnterpriseWageTrack());
            if (findPlayerByClass(state, ClassType.STATE).isEmpty()) {
                enterprise.setWageLevel(minimumStateWageLevel(state));
            } else if (enterprise.getWageLevel() < minimumStateWageLevel(state)) {
                enterprise.setWageLevel(minimumStateWageLevel(state));
            }
        }
        Map<String, Integer> wageTrack = enterprise.getWageTrack() == null ? Map.of() : enterprise.getWageTrack();
        if (wageTrack.isEmpty()) {
            return Math.max(0, enterprise.getWageLevel());
        }
        return switch (Math.max(1, enterprise.getWageLevel())) {
            case 1 -> Math.max(0, wageTrack.getOrDefault("low", enterprise.getWageLevel()));
            case 2 -> Math.max(0, wageTrack.getOrDefault("medium", enterprise.getWageLevel()));
            case 3 -> Math.max(0, wageTrack.getOrDefault("high", enterprise.getWageLevel()));
            default -> Math.max(0, wageTrack.getOrDefault("high", enterprise.getWageLevel()));
        };
    }

    private void enforceStateEnterpriseWagePolicy(GameState state) {
        int minimum = minimumStateWageLevel(state);
        boolean statePlayerActive = findPlayerByClass(state, ClassType.STATE).isPresent();
        for (Enterprise enterprise : state.getEnterprises()) {
            if (enterprise.getOwnerClass() != ClassType.STATE) {
                continue;
            }
            enterprise.setWageTrack(stateEnterpriseWageTrack());
            int before = Math.max(1, enterprise.getWageLevel());
            int after = statePlayerActive ? Math.max(before, minimum) : minimum;
            if (before != after) {
                enterprise.setWageLevel(after);
                state.appendLog(
                        "STATE_WAGE_ADJUSTED",
                        enterprise.getId() + " wage level adjusted from L" + before + " to L" + after
                                + " under labor policy " + policyCourse(state, PolicyId.POLICY_2_LABOR_MARKET, PolicyCourse.B) + "."
                );
            }
        }
    }

    private int minimumStateWageLevel(GameState state) {
        PolicyCourse labor = policyCourse(state, PolicyId.POLICY_2_LABOR_MARKET, PolicyCourse.B);
        return switch (labor) {
            case A -> 3;
            case B -> 2;
            case C -> 1;
        };
    }

    private Map<String, Integer> stateEnterpriseWageTrack() {
        return Map.of("low", 15, "medium", 20, "high", 25);
    }

    private void ensureTreasuryCanPay(GameState state, int amount, String reason) {
        while (state.getTreasury() < amount) {
            state.setTreasury(state.getTreasury() + STATE_LOAN_AMOUNT);
            state.setStateLoans(state.getStateLoans() + 1);
            state.appendLog(
                    "STATE_LOAN_TAKEN",
                    "State took a 50 loan for " + reason + "; active state loans: " + state.getStateLoans() + "."
            );
        }
    }

    private java.util.Optional<PlayerState> findPlayerByClass(GameState state, ClassType classType) {
        return state.getPlayers().stream().filter(player -> player.getClassType() == classType).findFirst();
    }

    private record PurchaseResult(int quantity, int cost) {
    }
}
