package com.example.hegemony.domain;

import com.example.hegemony.domain.model.ClassType;
import com.example.hegemony.infrastructure.carddata.EmptyBotActionCardCatalog;
import com.example.hegemony.infrastructure.carddata.EmptyEnterpriseCardCatalog;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CardReadyCatalogTest {
    @Test
    void emptyEnterpriseCardCatalogReportsNotInstalled() {
        EmptyEnterpriseCardCatalog catalog = new EmptyEnterpriseCardCatalog();
        assertThat(catalog.isDatasetInstalled()).isFalse();
        assertThat(catalog.datasetStatus()).isEqualTo("CARD_DATA_NOT_INSTALLED");
        assertThat(catalog.listAvailableMarketCards(ClassType.CAPITALIST)).isEmpty();
    }

    @Test
    void emptyBotActionCardCatalogReportsNotInstalled() {
        EmptyBotActionCardCatalog catalog = new EmptyBotActionCardCatalog();
        assertThat(catalog.isSimpleAutomaDatasetInstalled(ClassType.CAPITALIST)).isFalse();
        assertThat(catalog.datasetStatus(ClassType.CAPITALIST)).isEqualTo("CARD_DATA_NOT_INSTALLED");
        assertThat(catalog.listForClass(ClassType.CAPITALIST)).isEmpty();
    }
}
