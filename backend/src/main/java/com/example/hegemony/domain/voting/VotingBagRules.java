package com.example.hegemony.domain.voting;

import com.example.hegemony.domain.model.ClassType;
import com.example.hegemony.domain.model.Enterprise;
import com.example.hegemony.domain.model.GameState;
import com.example.hegemony.domain.model.PlayerState;
import com.example.hegemony.domain.model.VotingCubeOwnerClass;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class VotingBagRules {
    public VotingCubeOwnerClass ownerForClass(ClassType classType) {
        if (classType == null) {
            return null;
        }
        return switch (classType) {
            case WORKER -> VotingCubeOwnerClass.WORKER;
            case MIDDLE_CLASS -> VotingCubeOwnerClass.MIDDLE_CLASS;
            case CAPITALIST -> VotingCubeOwnerClass.CAPITALIST;
            case STATE -> null;
        };
    }

    public String playerIdForCubeOwner(GameState state, VotingCubeOwnerClass ownerClass) {
        ClassType classType = switch (ownerClass) {
            case WORKER -> ClassType.WORKER;
            case MIDDLE_CLASS -> ClassType.MIDDLE_CLASS;
            case CAPITALIST -> ClassType.CAPITALIST;
        };
        return findPlayerByClass(state, classType).map(PlayerState::getPlayerId).orElse(null);
    }

    public List<VotingCubeOwnerClass> drawCubes(GameState state, int count) {
        List<VotingCubeOwnerClass> drawn = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            if (state.getVotingBag().totalCubes() == 0) {
                refill(state, 2, true);
            }
            if (state.getVotingBag().totalCubes() == 0) {
                break;
            }

            VotingCubeOwnerClass ownerClass = drawOne(state);
            if (ownerClass == null) {
                break;
            }
            drawn.add(ownerClass);
        }
        return drawn;
    }

    public List<VotingCubeOwnerClass> drawExistingCubes(GameState state, int count) {
        List<VotingCubeOwnerClass> drawn = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            if (state.getVotingBag().totalCubes() == 0) {
                break;
            }

            VotingCubeOwnerClass ownerClass = drawOne(state);
            if (ownerClass == null) {
                break;
            }
            drawn.add(ownerClass);
        }
        return drawn;
    }

    public int countForOwner(GameState state, VotingCubeOwnerClass ownerClass) {
        if (ownerClass == null || state == null || state.getVotingBag() == null) {
            return 0;
        }
        return switch (ownerClass) {
            case WORKER -> state.getVotingBag().getWorker();
            case MIDDLE_CLASS -> state.getVotingBag().getMiddleClass();
            case CAPITALIST -> state.getVotingBag().getCapitalist();
        };
    }

    public void refill(GameState state, int repeats, boolean includeTwoPlayerNeutralRule) {
        for (int i = 0; i < repeats; i++) {
            PlayerState worker = findPlayerByClass(state, ClassType.WORKER).orElse(null);
            if (worker != null) {
                state.getVotingBag().add(VotingCubeOwnerClass.WORKER, ceilHalf(worker.getPopulation()));
            }

            PlayerState capitalist = findPlayerByClass(state, ClassType.CAPITALIST).orElse(null);
            if (capitalist != null) {
                int functioning = functioningEnterprisesByOwner(state, ClassType.CAPITALIST);
                state.getVotingBag().add(VotingCubeOwnerClass.CAPITALIST, ceilHalf(functioning));
            }

            PlayerState middleClass = findPlayerByClass(state, ClassType.MIDDLE_CLASS).orElse(null);
            if (middleClass != null) {
                int populationTerm = ceilHalf(middleClass.getPopulation());
                int functioningTerm = ceilHalf(functioningEnterprisesByOwner(state, ClassType.MIDDLE_CLASS));
                state.getVotingBag().add(VotingCubeOwnerClass.MIDDLE_CLASS, Math.max(populationTerm, functioningTerm));
            }

            if (includeTwoPlayerNeutralRule && isTwoPlayerMode(state)) {
                state.getVotingBag().add(VotingCubeOwnerClass.MIDDLE_CLASS, 5);
            }
        }
    }

    public boolean isIgnoredExtraordinaryVoteCube(GameState state, VotingCubeOwnerClass ownerClass) {
        return isTwoPlayerMode(state) && ownerClass == VotingCubeOwnerClass.MIDDLE_CLASS;
    }

    private VotingCubeOwnerClass drawOne(GameState state) {
        Map<VotingCubeOwnerClass, Integer> counts = new EnumMap<>(VotingCubeOwnerClass.class);
        counts.put(VotingCubeOwnerClass.WORKER, state.getVotingBag().getWorker());
        counts.put(VotingCubeOwnerClass.MIDDLE_CLASS, state.getVotingBag().getMiddleClass());
        counts.put(VotingCubeOwnerClass.CAPITALIST, state.getVotingBag().getCapitalist());

        VotingCubeOwnerClass selected = null;
        int best = Integer.MIN_VALUE;
        for (VotingCubeOwnerClass owner : List.of(VotingCubeOwnerClass.WORKER, VotingCubeOwnerClass.MIDDLE_CLASS, VotingCubeOwnerClass.CAPITALIST)) {
            int value = counts.getOrDefault(owner, 0);
            if (value > best) {
                best = value;
                selected = owner;
            }
        }

        if (selected == null || best <= 0) {
            return null;
        }

        state.getVotingBag().removeOne(selected);
        return selected;
    }

    private Optional<PlayerState> findPlayerByClass(GameState state, ClassType classType) {
        return state.getPlayers().stream().filter(player -> player.getClassType() == classType).findFirst();
    }

    private int functioningEnterprisesByOwner(GameState state, ClassType classType) {
        return (int) state.getEnterprises().stream()
                .filter(e -> e.getOwnerClass() == classType)
                .filter(Enterprise::isFunctioning)
                .count();
    }

    private int ceilHalf(int value) {
        return (value + 1) / 2;
    }

    private boolean isTwoPlayerMode(GameState state) {
        return state.getTurnOrder().getActiveClasses().size() == 2;
    }
}
