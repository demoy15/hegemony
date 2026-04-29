package com.example.hegemony.domain.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class CurrentVoteState {
    private PolicyId activeProposalPolicyId;
    private String proposalAuthorPlayerId;
    private PolicyCourse targetCourse;
    private PolicyCourse currentCourseBeforeVote;
    private VotingStage votingStage = VotingStage.DECLARE_STANCES;
    private Map<String, VoteStance> stanceByPlayer = new HashMap<>();
    private List<DrawnVotingCube> drawnVotingCubes = new ArrayList<>();
    private Map<String, Integer> interpretedVotes = new HashMap<>();
    private Map<String, Integer> influenceCommitments = new HashMap<>();
    private VoteResolutionResult result = VoteResolutionResult.PENDING;
    private boolean passedPolicyCourseApplied;
    private boolean extraordinary;
    private int totalForVotes;
    private int totalAgainstVotes;

    public CurrentVoteState() {
    }

    public CurrentVoteState copy() {
        CurrentVoteState copy = new CurrentVoteState();
        copy.activeProposalPolicyId = activeProposalPolicyId;
        copy.proposalAuthorPlayerId = proposalAuthorPlayerId;
        copy.targetCourse = targetCourse;
        copy.currentCourseBeforeVote = currentCourseBeforeVote;
        copy.votingStage = votingStage;
        copy.stanceByPlayer = new HashMap<>(stanceByPlayer);
        copy.drawnVotingCubes = drawnVotingCubes.stream().map(DrawnVotingCube::copy).toList();
        copy.interpretedVotes = new HashMap<>(interpretedVotes);
        copy.influenceCommitments = new HashMap<>(influenceCommitments);
        copy.result = result;
        copy.passedPolicyCourseApplied = passedPolicyCourseApplied;
        copy.extraordinary = extraordinary;
        copy.totalForVotes = totalForVotes;
        copy.totalAgainstVotes = totalAgainstVotes;
        return copy;
    }

    public void setStanceByPlayer(Map<String, VoteStance> stanceByPlayer) {
        this.stanceByPlayer = new HashMap<>(stanceByPlayer);
    }

    public void setDrawnVotingCubes(List<DrawnVotingCube> drawnVotingCubes) {
        this.drawnVotingCubes = new ArrayList<>(drawnVotingCubes);
    }

    public void setInterpretedVotes(Map<String, Integer> interpretedVotes) {
        this.interpretedVotes = new HashMap<>(interpretedVotes);
    }

    public void setInfluenceCommitments(Map<String, Integer> influenceCommitments) {
        this.influenceCommitments = new HashMap<>(influenceCommitments);
    }
}
