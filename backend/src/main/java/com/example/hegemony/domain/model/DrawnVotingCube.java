package com.example.hegemony.domain.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DrawnVotingCube {
    private VotingCubeOwnerClass ownerClass;
    private InterpretedVote interpretedVote;

    public DrawnVotingCube() {
    }

    public DrawnVotingCube(VotingCubeOwnerClass ownerClass, InterpretedVote interpretedVote) {
        this.ownerClass = ownerClass;
        this.interpretedVote = interpretedVote;
    }

    public DrawnVotingCube copy() {
        return new DrawnVotingCube(ownerClass, interpretedVote);
    }
}
