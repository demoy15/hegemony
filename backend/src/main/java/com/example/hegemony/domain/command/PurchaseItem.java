package com.example.hegemony.domain.command;

import com.example.hegemony.domain.model.SupplierType;

public record PurchaseItem(
        SupplierType supplierType,
        String supplierPlayerId,
        int quantity,
        Integer unitPriceOverride
) {
    public PurchaseItem(SupplierType supplierType, String supplierPlayerId, int quantity) {
        this(supplierType, supplierPlayerId, quantity, null);
    }
}
