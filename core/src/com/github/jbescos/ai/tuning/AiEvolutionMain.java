package com.github.jbescos.ai.tuning;

import com.badlogic.gdx.utils.Array;
import com.github.jbescos.RatassGame;
import com.github.jbescos.ai.AiDrivingPersonality;
import com.github.jbescos.ai.AiDrivingPersonalities;
import java.util.Locale;

public final class AiEvolutionMain {
    private AiEvolutionMain() {
    }

    public static void main(String[] args) {
        AiEvolutionTuner.Config config = new AiEvolutionTuner.Config();
        String preset = "all";

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ("--preset".equals(arg) && i + 1 < args.length) {
                preset = args[++i];
            } else if ("--generations".equals(arg) && i + 1 < args.length) {
                config.generations = Integer.parseInt(args[++i]);
            } else if ("--population".equals(arg) && i + 1 < args.length) {
                config.populationSize = Integer.parseInt(args[++i]);
            } else if ("--rounds".equals(arg) && i + 1 < args.length) {
                config.roundsPerEvaluation = Integer.parseInt(args[++i]);
            } else if ("--verify-rounds".equals(arg) && i + 1 < args.length) {
                config.verificationRounds = Integer.parseInt(args[++i]);
            } else if ("--copies".equals(arg) && i + 1 < args.length) {
                config.candidateCopies = Integer.parseInt(args[++i]);
            } else if ("--field-size".equals(arg) && i + 1 < args.length) {
                config.fieldSize = Integer.parseInt(args[++i]);
            } else if ("--seed".equals(arg) && i + 1 < args.length) {
                config.seed = Long.parseLong(args[++i]);
            } else if ("--mutation-chance".equals(arg) && i + 1 < args.length) {
                config.mutationChance = Float.parseFloat(args[++i]);
            } else if ("--mutation-scale".equals(arg) && i + 1 < args.length) {
                config.mutationScale = Float.parseFloat(args[++i]);
            } else {
                throw new IllegalArgumentException("Unknown or incomplete argument: " + arg);
            }
        }

        Array<AiDrivingPersonality> presets = new Array<AiDrivingPersonality>();
        if ("all".equalsIgnoreCase(preset)) {
            presets.addAll(AiDrivingPersonalities.createPresetList());
        } else {
            AiDrivingPersonality selected = AiDrivingPersonalities.byId(preset);
            if (selected == null) {
                throw new IllegalArgumentException("Unknown preset: " + preset);
            }
            presets.add(selected);
        }

        AiEvolutionTuner tuner = new AiEvolutionTuner(config);
        for (int i = 0; i < presets.size; i++) {
            AiDrivingPersonality personality = presets.get(i);
            AiEvolutionTuner.Result result = tuner.evolvePreset(personality);
            printResult(result);
        }
    }

    private static void printResult(AiEvolutionTuner.Result result) {
        RatassGame.AiTournamentEntry baseline = result.baselineTournament.getEntry("candidate");
        RatassGame.AiTournamentEntry evolved = result.evolvedTournament.getEntry("candidate");

        System.out.println("Preset: " + result.seedPersonality.id);
        System.out.printf(
                Locale.US,
                "  Baseline fitness: %.3f  |  avg pts %.3f  |  win rate %.3f%n",
                result.baselineFitness,
                baseline == null ? 0f : baseline.getAverageAwardedPoints(),
                baseline == null ? 0f : baseline.getWinRate());
        System.out.printf(
                Locale.US,
                "  Evolved  fitness: %.3f  |  avg pts %.3f  |  win rate %.3f%n",
                result.evolvedFitness,
                evolved == null ? 0f : evolved.getAverageAwardedPoints(),
                evolved == null ? 0f : evolved.getWinRate());
        System.out.println("  Builder:");
        System.out.println(result.builderSnippet);
        System.out.println();
    }
}
