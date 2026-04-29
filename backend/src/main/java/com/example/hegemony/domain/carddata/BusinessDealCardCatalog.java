package com.example.hegemony.domain.carddata;

import com.example.hegemony.domain.model.BusinessDealCard;

import java.util.List;
import java.util.Optional;

public interface BusinessDealCardCatalog {
    List<BusinessDealCard> listAll();

    Optional<BusinessDealCard> findById(String cardId);
}
