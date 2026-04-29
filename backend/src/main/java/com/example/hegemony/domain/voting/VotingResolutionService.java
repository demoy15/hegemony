package com.example.hegemony.domain.voting;

import com.example.hegemony.domain.event.DomainEvent;
import com.example.hegemony.domain.event.VoteResolvedEvent;
import com.example.hegemony.domain.model.ClassType;
import com.example.hegemony.domain.model.CurrentVoteState;
import com.example.hegemony.domain.model.DrawnVotingCube;
import com.example.hegemony.domain.model.GameState;
import com.example.hegemony.domain.model.InterpretedVote;
import com.example.hegemony.domain.model.PlayerState;
import com.example.hegemony.domain.model.PolicyCourse;
import com.example.hegemony.domain.model.PolicyState;
import com.example.hegemony.domain.model.ProposalResolutionResult;
import com.example.hegemony.domain.model.ProposalToken;
import com.example.hegemony.domain.model.VoteResolutionResult;
import com.example.hegemony.domain.model.VoteStance;
import com.example.hegemony.domain.model.VotingCubeOwnerClass;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class VotingResolutionService {
    private final VotingBagRules votingBagRules;

    public VotingResolutionService(VotingBagRules votingBagRules) {
        this.votingBagRules = votingBagRules;
    }

    public List<DomainEvent> resolveOrdinaryVote(GameState state, CurrentVoteState session) {
        int bagFor = session.getInterpretedVotes().getOrDefault(InterpretedVote.FOR.name(), 0);
        int bagAgainst = session.getInterpretedVotes().getOrDefault(InterpretedVote.AGAINST.name(), 0);
        int influenceFor = 0;
        int influenceAgainst = 0;

        for (Map.Entry<String, Integer> entry : session.getInfluenceCommitments().entrySet()) {
            VoteStance stance = session.getStanceByPlayer().get(entry.getKey());
            if (stance == VoteStance.FOR) {
                influenceFor += entry.getValue();
            } else {
                influenceAgainst += entry.getValue();
            }
        }

        int totalFor = bagFor + influenceFor;
        int totalAgainst = bagAgainst + influenceAgainst;
        session.setTotalForVotes(totalFor);
        session.setTotalAgainstVotes(totalAgainst);

        VoteResolutionResult result = totalFor >= totalAgainst ? VoteResolutionResult.PASSED : VoteResolutionResult.REJECTED;
        session.setResult(result);

        PolicyState policy = state.findPolicy(session.getActiveProposalPolicyId()).orElseThrow();
        PolicyCourse before = policy.getCurrentCourse();
        if (result == VoteResolutionResult.PASSED) {
            policy.setCurrentCourse(session.getTargetCourse());
            session.setPassedPolicyCourseApplied(true);
            awardVoteVictoryPoints(state, session);
        }

        cleanupVotingBagAfterResolution(state, session, result);
        spendCommittedInfluence(state, session);
        releaseProposalToken(state, policy);

        session.setVotingStage(com.example.hegemony.domain.model.VotingStage.RESOLVED);
        state.setLastProposalResolution(new ProposalResolutionResult(policy.getId(), result, before, session.getTargetCourse()));
        state.setCurrentVoteState(null);

        return List.of(new VoteResolvedEvent(policy.getId(), result, before, session.getTargetCourse(), totalFor, totalAgainst));
    }

    public List<DomainEvent> resolveExtraordinaryVote(GameState state, PlayerState actor, PolicyState policy) {
        ProposalToken token = policy.getOccupyingProposalToken();
        if (token == null) {
            return List.of();
        }

        actor.setInfluence(Math.max(0, actor.getInfluence() - 1));

        List<VotingCubeOwnerClass> drawn = votingBagRules.drawCubes(state, 5);
        VotingCubeOwnerClass proposerOwnerClass = votingBagRules.ownerForClass(actor.getClassType());
        int proposerVotes = 0;
        int opposingVotes = 0;
        int ignoredVotes = 0;

        for (VotingCubeOwnerClass ownerClass : drawn) {
            if (ownerClass == null || votingBagRules.isIgnoredExtraordinaryVoteCube(state, ownerClass)) {
                ignoredVotes++;
                if (ownerClass != null) {
                    state.getVotingBag().add(ownerClass, 1);
                }
                continue;
            }
            if (ownerClass == proposerOwnerClass) {
                proposerVotes++;
            } else {
                opposingVotes++;
            }
        }

        VoteResolutionResult result = proposerVotes >= opposingVotes ? VoteResolutionResult.PASSED : VoteResolutionResult.REJECTED;
        PolicyCourse before = policy.getCurrentCourse();
        PolicyCourse target = token.getTargetCourse();
        if (result == VoteResolutionResult.PASSED && target != null) {
            policy.setCurrentCourse(target);
        }

        returnExtraordinaryLosingCubes(state, drawn, proposerOwnerClass, result);
        releaseProposalToken(state, policy);
        state.setLastProposalResolution(new ProposalResolutionResult(policy.getId(), result, before, target));
        state.appendLog(
                "EXTRAORDINARY_VOTE_RESOLVED",
                "Extraordinary vote on " + policy.getId() + ": " + result
                        + " (" + proposerVotes + " proposer cubes vs " + opposingVotes + " opposing cubes"
                        + ", ignored=" + ignoredVotes + ")."
        );
        return List.of(new VoteResolvedEvent(policy.getId(), result, before, target, proposerVotes, opposingVotes));
    }

    private void awardVoteVictoryPoints(GameState state, CurrentVoteState session) {
        PlayerState proposer = state.findPlayerById(session.getProposalAuthorPlayerId()).orElse(null);
        if (proposer != null) {
            proposer.setVictoryPoints(proposer.getVictoryPoints() + 3);
        }

        for (PlayerState player : activePlayers(state)) {
            if (java.util.Objects.equals(player.getPlayerId(), session.getProposalAuthorPlayerId())) {
                continue;
            }

            VoteStance stance = session.getStanceByPlayer().get(player.getPlayerId());
            if (stance != VoteStance.FOR) {
                continue;
            }

            int bagContribution = session.getInterpretedVotes().getOrDefault("PLAYER:" + player.getPlayerId(), 0);
            int influenceContribution = session.getInfluenceCommitments().getOrDefault(player.getPlayerId(), 0);
            if (bagContribution > 0 || influenceContribution > 0) {
                player.setVictoryPoints(player.getVictoryPoints() + 1);
            }
        }
    }

    private void cleanupVotingBagAfterResolution(GameState state, CurrentVoteState session, VoteResolutionResult result) {
        InterpretedVote winningSide = result == VoteResolutionResult.PASSED ? InterpretedVote.FOR : InterpretedVote.AGAINST;
        for (DrawnVotingCube cube : session.getDrawnVotingCubes()) {
            if (cube.getInterpretedVote() == InterpretedVote.NEUTRAL) {
                if (session.isExtraordinary()) {
                    state.getVotingBag().add(cube.getOwnerClass(), 1);
                }
                continue;
            }
            if (cube.getInterpretedVote() == winningSide) {
                continue;
            }
            state.getVotingBag().add(cube.getOwnerClass(), 1);
        }
    }

    private void returnExtraordinaryLosingCubes(
            GameState state,
            List<VotingCubeOwnerClass> drawn,
            VotingCubeOwnerClass proposerOwnerClass,
            VoteResolutionResult result
    ) {
        for (VotingCubeOwnerClass ownerClass : drawn) {
            if (ownerClass == null || votingBagRules.isIgnoredExtraordinaryVoteCube(state, ownerClass)) {
                continue;
            }
            boolean proposerCube = ownerClass == proposerOwnerClass;
            boolean winningCube = result == VoteResolutionResult.PASSED ? proposerCube : !proposerCube;
            if (!winningCube) {
                state.getVotingBag().add(ownerClass, 1);
            }
        }
    }

    private void spendCommittedInfluence(GameState state, CurrentVoteState session) {
        for (Map.Entry<String, Integer> entry : session.getInfluenceCommitments().entrySet()) {
            state.findPlayerById(entry.getKey()).ifPresent(player -> player.setInfluence(Math.max(0, player.getInfluence() - entry.getValue())));
        }
    }

    private void releaseProposalToken(GameState state, PolicyState policy) {
        ProposalToken token = policy.getOccupyingProposalToken();
        if (token == null) {
            return;
        }

        String owner = token.getOwnerPlayerId();
        if (owner == null || owner.isBlank()) {
            owner = token.getOwnerClass() == null ? null : token.getOwnerClass().playerId();
        }
        if (owner != null) {
            state.findPlayerById(owner).ifPresent(player -> player.returnProposalToken(token.getId()));
        }

        policy.setOccupyingProposalToken(null);
    }

    private List<PlayerState> activePlayers(GameState state) {
        List<PlayerState> ordered = new ArrayList<>();
        for (ClassType classType : state.getTurnOrder().getActiveClasses()) {
            state.getPlayers().stream()
                    .filter(player -> player.getClassType() == classType)
                    .findFirst()
                    .ifPresent(ordered::add);
        }
        return ordered;
    }
}
