package com.example.hegemony.infrastructure;

import com.example.hegemony.application.GameCommandFactory;
import com.example.hegemony.application.GameInitializer;
import com.example.hegemony.application.GameService;
import com.example.hegemony.application.GameStateRepository;
import com.example.hegemony.application.GameStateStorage;
import com.example.hegemony.application.BusinessDealDeckManager;
import com.example.hegemony.application.BotTurnService;
import com.example.hegemony.application.ExportCardManager;
import com.example.hegemony.application.MigrationCardManager;
import com.example.hegemony.application.rules.RuleSpecLoader;
import com.example.hegemony.bot.LegalMoveBot;
import com.example.hegemony.domain.card.CardCatalog;
import com.example.hegemony.domain.card.CustomCardResolver;
import com.example.hegemony.domain.card.DeclarativeCardEffectProcessor;
import com.example.hegemony.domain.engine.GameRulesEngine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class AppConfig {
    @Bean
    public GameCommandFactory gameCommandFactory() {
        return new GameCommandFactory();
    }

    @Bean
    public LegalMoveBot legalMoveBot() {
        return new LegalMoveBot();
    }

    @Bean
    public DeclarativeCardEffectProcessor declarativeCardEffectProcessor(Map<String, CustomCardResolver> customResolvers) {
        return new DeclarativeCardEffectProcessor(customResolvers);
    }

    @Bean
    public GameRulesEngine gameRulesEngine(
            CardCatalog cardCatalog,
            DeclarativeCardEffectProcessor cardEffectProcessor,
            RuleSpecLoader ruleSpecLoader,
            BusinessDealDeckManager businessDealDeckManager,
            ExportCardManager exportCardManager,
            MigrationCardManager migrationCardManager
    ) {
        return new GameRulesEngine(cardCatalog, cardEffectProcessor, ruleSpecLoader.loadWorkerTaxMatrix(), businessDealDeckManager, exportCardManager, migrationCardManager);
    }

    @Bean
    public GameStateRepository gameStateRepository(GameInitializer initializer) {
        return new InMemoryGameStateRepository(initializer);
    }

    @Bean
    public GameService gameService(
            GameStateRepository repository,
            GameRulesEngine engine,
            LegalMoveBot bot,
            GameCommandFactory commandFactory,
            GameInitializer initializer,
            GameStateStorage storage,
            BotTurnService botTurnService,
            BusinessDealDeckManager businessDealDeckManager,
            ExportCardManager exportCardManager,
            MigrationCardManager migrationCardManager
    ) {
        return new GameService(repository, engine, bot, commandFactory, initializer, storage, botTurnService, businessDealDeckManager, exportCardManager, migrationCardManager);
    }
}
