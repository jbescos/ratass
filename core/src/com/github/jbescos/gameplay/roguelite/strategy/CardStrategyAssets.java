package com.github.jbescos.gameplay.roguelite.strategy;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.github.jbescos.ai.rl.RlPolicy;
import java.util.ArrayList;
import java.util.List;

/** Loads optional neural card strategies without coupling asset access to the run model. */
public final class CardStrategyAssets {
    private static final String DIRECTORY = "ai/card-strategies";
    private static final String FILE_NAME = "rl_card_strategy_policy.json";
    private static final String[] PROFILE_IDS = {
        "strategy00", "strategy01", "strategy02"
    };

    private CardStrategyAssets() {
    }

    public static CardStrategyCatalog loadInternal() {
        List<CardStrategy> strategies = new ArrayList<CardStrategy>();
        if (Gdx.files == null) {
            return new CardStrategyCatalog(strategies);
        }
        for (String profileId : PROFILE_IDS) {
            FileHandle file = Gdx.files.internal(
                    DIRECTORY + "/" + profileId + "/" + FILE_NAME);
            if (!file.exists()) {
                continue;
            }
            try {
                String json = file.readString("UTF-8");
                JsonValue root = new JsonReader().parse(json);
                strategies.add(new NeuralCardStrategy(
                        profileId,
                        root.getString("strategyType", profileId),
                        RlPolicy.fromJson(json)));
            } catch (RuntimeException exception) {
                if (Gdx.app != null) {
                    Gdx.app.error(
                            "CardStrategyAssets",
                            "Ignoring invalid card strategy " + profileId + ".",
                            exception);
                }
            }
        }
        return new CardStrategyCatalog(strategies);
    }
}
