package com.example.hegemony.application.setup;

import com.example.hegemony.domain.model.ClassType;
import com.example.hegemony.domain.model.PolicyCourse;
import com.example.hegemony.domain.model.PolicyId;
import com.example.hegemony.domain.model.PublicServicesState;
import com.example.hegemony.domain.model.VotingBagState;
import com.example.hegemony.domain.model.WorkerQualification;
import com.example.hegemony.domain.model.WorkerSlotColor;
import com.example.hegemony.domain.model.WorkerSector;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class SetupSpecModel {
    private final Map<Integer, List<ClassType>> activeClassesByPlayerCount = new HashMap<>();
    private final Map<PolicyId, PolicyCourse> initialPolicyCourses = new EnumMap<>(PolicyId.class);
    private final Map<Integer, PublicServicesState> publicServicesByPlayerCount = new HashMap<>();
    private final Map<ClassType, PlayerSetup> players = new EnumMap<>(ClassType.class);
    private final Map<Integer, List<EnterpriseSeed>> enterprisesByPlayerCount = new HashMap<>();
    private final Map<Integer, List<WorkerPlacementSeed>> workerPlacementsByPlayerCount = new HashMap<>();
    private final Map<String, EnterpriseDefinition> capitalistEnterpriseDefinitions = new HashMap<>();
    private int treasury;
    private int roundMarker;
    private int taxMultiplier;
    private VotingBagState votingBag = new VotingBagState();

    @Getter
    @Setter
    public static class PlayerSetup {
        private int money;
        private int influence;
        private int population;
        private int welfare;
        private int legitimacyWorker;
        private int legitimacyMiddleClass;
        private int legitimacyCapitalist;
        private int proposalTokenCount;
        private Map<String, Integer> resources = new HashMap<>();
        private Map<String, Integer> prices = new HashMap<>();
    }

    public record EnterpriseSeed(String id, ClassType ownerClass, WorkerSector sector, int wageLevel) {
    }

    public record WorkerPlacementSeed(
            ClassType classType,
            String enterpriseId,
            WorkerQualification qualification,
            WorkerSector sector,
            int count
    ) {
    }

    @Getter
    @Setter
    public static class EnterpriseDefinition {
        private String id;
        private String name;
        private String category;
        private int cost;
        private boolean starting;
        private ProductionDefinition production = new ProductionDefinition();
        private WorkerSpec workers = new WorkerSpec();
        private WageTrack wages;
        private AutomationDefinition automation = new AutomationDefinition();

        public boolean isAutomated() {
            return automation != null && automation.isAutomated();
        }
    }

    @Getter
    @Setter
    public static class ProductionDefinition {
        private String output;
        private int amount;
        private Integer perWorkers;
    }

    @Getter
    @Setter
    public static class WorkerSpec {
        private List<WorkerSlotDefinition> slots = new ArrayList<>();
    }

    @Getter
    @Setter
    public static class WorkerSlotDefinition {
        private String kind;
        private int count;
        private WorkerQualification qualification = WorkerQualification.UNSKILLED;
        private WorkerSlotColor color;
    }

    @Getter
    @Setter
    public static class WageTrack {
        private Integer low;
        private Integer medium;
        private Integer high;
    }

    @Getter
    @Setter
    public static class AutomationDefinition {
        private boolean automated;
    }

    public List<EnterpriseSeed> enterprisesFor(int playerCount) {
        return new ArrayList<>(enterprisesByPlayerCount.getOrDefault(playerCount, List.of()));
    }

    public List<WorkerPlacementSeed> placementsFor(int playerCount) {
        return new ArrayList<>(workerPlacementsByPlayerCount.getOrDefault(playerCount, List.of()));
    }

    public EnterpriseDefinition capitalistEnterpriseDefinition(String enterpriseId) {
        if (enterpriseId == null || enterpriseId.isBlank()) {
            return null;
        }
        return capitalistEnterpriseDefinitions.get(enterpriseId);
    }
}
