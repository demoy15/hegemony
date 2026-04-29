package com.example.hegemony.domain.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PublicServicesState {
    private int healthcare;
    private int education;
    private int mediaInfluence;

    public PublicServicesState() {
    }

    public PublicServicesState(int healthcare, int education, int mediaInfluence) {
        this.healthcare = healthcare;
        this.education = education;
        this.mediaInfluence = mediaInfluence;
    }

    public PublicServicesState copy() {
        return new PublicServicesState(healthcare, education, mediaInfluence);
    }
}
