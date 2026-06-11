package com.example.hegemony.domain.event;

import com.example.hegemony.domain.model.DrawnVotingCube;
import com.example.hegemony.domain.model.InterpretedVote;
import com.example.hegemony.domain.model.PolicyId;
import com.example.hegemony.domain.model.VotingCubeOwnerClass;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record VotingCubesDrawnEvent(PolicyId policyId, int cubesDrawn, List<DrawnVotingCube> drawnCubes) implements DomainEvent {
    public VotingCubesDrawnEvent(PolicyId policyId, int cubesDrawn) {
        this(policyId, cubesDrawn, List.of());
    }

    public VotingCubesDrawnEvent {
        drawnCubes = drawnCubes == null ? List.of() : drawnCubes.stream()
                .map(DrawnVotingCube::copy)
                .toList();
    }

    @Override
    public String type() {
        return "VOTING_CUBES_DRAWN";
    }

    @Override
    public String description() {
        return "Drawn " + cubesDrawn + " cubes for vote on " + policyId + cubeSummarySuffix() + ".";
    }

    private String cubeSummarySuffix() {
        if (drawnCubes.isEmpty()) {
            return "";
        }
        Map<VotingCubeOwnerClass, Integer> byOwner = new EnumMap<>(VotingCubeOwnerClass.class);
        Map<InterpretedVote, Integer> byVote = new EnumMap<>(InterpretedVote.class);
        for (DrawnVotingCube cube : drawnCubes) {
            if (cube.getOwnerClass() != null) {
                byOwner.merge(cube.getOwnerClass(), 1, Integer::sum);
            }
            if (cube.getInterpretedVote() != null) {
                byVote.merge(cube.getInterpretedVote(), 1, Integer::sum);
            }
        }
        return ": "
                + formatOwnerSummary(byOwner)
                + "; votes "
                + formatVoteSummary(byVote);
    }

    private static String formatOwnerSummary(Map<VotingCubeOwnerClass, Integer> byOwner) {
        return List.of(VotingCubeOwnerClass.WORKER, VotingCubeOwnerClass.MIDDLE_CLASS, VotingCubeOwnerClass.CAPITALIST)
                .stream()
                .filter(ownerClass -> byOwner.getOrDefault(ownerClass, 0) > 0)
                .map(ownerClass -> ownerClass.name() + "=" + byOwner.get(ownerClass))
                .collect(Collectors.joining(", "));
    }

    private static String formatVoteSummary(Map<InterpretedVote, Integer> byVote) {
        return List.of(InterpretedVote.FOR, InterpretedVote.AGAINST, InterpretedVote.NEUTRAL)
                .stream()
                .map(vote -> vote.name() + "=" + byVote.getOrDefault(vote, 0))
                .collect(Collectors.joining(", "));
    }
}
