package com.example.hegemony.domain.event;

import java.util.List;

public record BusinessDealsRefreshedEvent(
        String trigger,
        int round,
        List<String> visibleDealIds
) implements DomainEvent {
    @Override
    public String type() {
        return "BUSINESS_DEALS_REFRESHED";
    }

    @Override
    public String description() {
        String visible = visibleDealIds == null || visibleDealIds.isEmpty()
                ? "none visible"
                : String.join(", ", visibleDealIds);
        return "Business deals refreshed via " + trigger + " for round " + round + ": " + visible + ".";
    }
}
