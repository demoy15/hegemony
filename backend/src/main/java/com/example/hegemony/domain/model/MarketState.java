package com.example.hegemony.domain.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MarketState {
    private int goodsPrice = 3;
    private int workerHireCost = 2;

    public MarketState() {
    }

    public MarketState(int goodsPrice, int workerHireCost) {
        this.goodsPrice = goodsPrice;
        this.workerHireCost = workerHireCost;
    }

    public MarketState copy() {
        return new MarketState(goodsPrice, workerHireCost);
    }
}
