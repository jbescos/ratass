package com.github.jbescos.ai.tuning;

import com.badlogic.gdx.utils.Array;
import com.github.jbescos.RatassGame;
import com.github.jbescos.ai.AiDrivingPersonality;
import com.github.jbescos.ai.AiDrivingPersonalities;
import com.github.jbescos.gameplay.ArenaMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public final class AiEvolutionTuner {
    private static final String CANDIDATE_LABEL = "candidate";
    private static final float WIN_RATE_FITNESS_WEIGHT = 2.25f;

    private final Config config;

    public AiEvolutionTuner(Config config) {
        this.config = config == null ? new Config() : config;
    }

    public Result evolvePreset(AiDrivingPersonality seedPersonality) {
        if (seedPersonality == null) {
            throw new IllegalArgumentException("Seed personality is required.");
        }

        Random random = new Random(config.seed ^ seedPersonality.id.hashCode());
        AiPersonalityGenome baseGenome = AiPersonalityGenome.from(seedPersonality);
        List<RankedGenome> population = new ArrayList<RankedGenome>();
        population.add(new RankedGenome(baseGenome));
        while (population.size() < Math.max(2, config.populationSize)) {
            population.add(new RankedGenome(baseGenome.mutate(random, config.mutationChance, config.mutationScale)));
        }

        RankedGenome bestOverall = null;
        for (int generation = 0; generation < Math.max(1, config.generations); generation++) {
            long evaluationSeed = config.seed + generation * 7_919L + seedPersonality.id.hashCode();
            for (int i = 0; i < population.size(); i++) {
                RankedGenome ranked = population.get(i);
                evaluateGenome(ranked, seedPersonality, evaluationSeed, config.roundsPerEvaluation);
            }

            Collections.sort(population, RankedGenome.BY_FITNESS_DESC);
            if (bestOverall == null || population.get(0).fitness > bestOverall.fitness) {
                bestOverall = population.get(0).copy();
            }

            if (generation + 1 >= Math.max(1, config.generations)) {
                break;
            }

            List<RankedGenome> nextPopulation = new ArrayList<RankedGenome>();
            nextPopulation.add(population.get(0).copyForNextGeneration());
            if (population.size() > 1) {
                nextPopulation.add(population.get(1).copyForNextGeneration());
            }

            while (nextPopulation.size() < Math.max(2, config.populationSize)) {
                RankedGenome parentA = selectParent(population, random);
                RankedGenome parentB = selectParent(population, random);
                AiPersonalityGenome child =
                        parentA.genome
                                .crossover(parentB.genome, random)
                                .mutate(random, config.mutationChance, config.mutationScale);
                nextPopulation.add(new RankedGenome(child));
            }

            population = nextPopulation;
        }

        int verificationRounds =
                config.verificationRounds > 0
                        ? config.verificationRounds
                        : Math.max(config.roundsPerEvaluation * 2, config.roundsPerEvaluation + 4);
        long verificationSeed = config.seed ^ 0x5DEECE66DL ^ seedPersonality.id.hashCode();
        Evaluation baseline = evaluate(seedPersonality, baseGenome, verificationSeed, verificationRounds);
        Evaluation evolved = evaluate(seedPersonality, bestOverall.genome, verificationSeed, verificationRounds);

        AiDrivingPersonality evolvedPersonality =
                bestOverall.genome.toPersonality(seedPersonality.id, seedPersonality.displayName);
        return new Result(
                seedPersonality,
                evolvedPersonality,
                baseline.fitness,
                evolved.fitness,
                baseline.tournament,
                evolved.tournament,
                bestOverall.genome.toBuilderSnippet(seedPersonality.id, seedPersonality.displayName));
    }

    private void evaluateGenome(
            RankedGenome ranked,
            AiDrivingPersonality seedPersonality,
            long evaluationSeed,
            int rounds) {
        Evaluation evaluation = evaluate(seedPersonality, ranked.genome, evaluationSeed, rounds);
        ranked.fitness = evaluation.fitness;
        ranked.tournament = evaluation.tournament;
    }

    private Evaluation evaluate(
            AiDrivingPersonality seedPersonality,
            AiPersonalityGenome genome,
            long evaluationSeed,
            int rounds) {
        AiDrivingPersonality candidate =
                genome.toPersonality(seedPersonality.id, seedPersonality.displayName);
        RatassGame.AiTournamentConfig tournament =
                buildTournamentConfig(seedPersonality, candidate, evaluationSeed, rounds);
        RatassGame.AiTournamentResult result = RatassGame.runAiTournament(tournament);
        RatassGame.AiTournamentEntry candidateEntry = result.getEntry(CANDIDATE_LABEL);
        float fitness =
                candidateEntry == null
                        ? -Float.MAX_VALUE
                        : candidateEntry.getAverageAwardedPoints()
                                + candidateEntry.getWinRate() * WIN_RATE_FITNESS_WEIGHT;
        return new Evaluation(fitness, result);
    }

    private RatassGame.AiTournamentConfig buildTournamentConfig(
            AiDrivingPersonality seedPersonality,
            AiDrivingPersonality candidate,
            long evaluationSeed,
            int rounds) {
        RatassGame.AiTournamentConfig config = new RatassGame.AiTournamentConfig()
                .withRounds(rounds)
                .withSeed(evaluationSeed)
                .withSkipCountdown(true);

        for (int i = 0; i < Math.max(1, Math.min(this.config.candidateCopies, this.config.fieldSize)); i++) {
            config.addParticipant(CANDIDATE_LABEL, "Candidate " + (i + 1), candidate);
        }

        Array<AiDrivingPersonality> presets = AiDrivingPersonalities.createPresetList();
        Array<AiDrivingPersonality> opponents = new Array<AiDrivingPersonality>();
        for (int i = 0; i < presets.size; i++) {
            AiDrivingPersonality preset = presets.get(i);
            if (!preset.id.equals(seedPersonality.id)) {
                opponents.add(preset);
            }
        }
        if (opponents.size == 0) {
            opponents.add(seedPersonality);
        }

        int slot = 0;
        while (config.participants.size < Math.max(1, this.config.fieldSize)) {
            AiDrivingPersonality opponent = opponents.get(slot % opponents.size);
            config.addParticipant(
                    opponent.id,
                    opponent.displayName + " " + (slot + 1),
                    opponent);
            slot++;
        }

        for (int i = 0; i < this.config.maps.size; i++) {
            ArenaMap map = this.config.maps.get(i);
            config.addMap(map);
        }
        return config;
    }

    private RankedGenome selectParent(List<RankedGenome> population, Random random) {
        int poolSize = Math.max(1, population.size() / 2);
        RankedGenome candidate = population.get(random.nextInt(poolSize));
        RankedGenome challenger = population.get(random.nextInt(poolSize));
        return challenger.fitness > candidate.fitness ? challenger : candidate;
    }

    public static final class Config {
        public final Array<ArenaMap> maps = new Array<ArenaMap>();
        public int generations = 4;
        public int populationSize = 8;
        public int roundsPerEvaluation = 10;
        public int verificationRounds;
        public int candidateCopies = 3;
        public int fieldSize = 12;
        public long seed = 1L;
        public float mutationChance = 0.55f;
        public float mutationScale = 0.12f;
    }

    public static final class Result {
        public final AiDrivingPersonality seedPersonality;
        public final AiDrivingPersonality evolvedPersonality;
        public final float baselineFitness;
        public final float evolvedFitness;
        public final RatassGame.AiTournamentResult baselineTournament;
        public final RatassGame.AiTournamentResult evolvedTournament;
        public final String builderSnippet;

        private Result(
                AiDrivingPersonality seedPersonality,
                AiDrivingPersonality evolvedPersonality,
                float baselineFitness,
                float evolvedFitness,
                RatassGame.AiTournamentResult baselineTournament,
                RatassGame.AiTournamentResult evolvedTournament,
                String builderSnippet) {
            this.seedPersonality = seedPersonality;
            this.evolvedPersonality = evolvedPersonality;
            this.baselineFitness = baselineFitness;
            this.evolvedFitness = evolvedFitness;
            this.baselineTournament = baselineTournament;
            this.evolvedTournament = evolvedTournament;
            this.builderSnippet = builderSnippet;
        }
    }

    private static final class Evaluation {
        private final float fitness;
        private final RatassGame.AiTournamentResult tournament;

        private Evaluation(float fitness, RatassGame.AiTournamentResult tournament) {
            this.fitness = fitness;
            this.tournament = tournament;
        }
    }

    private static final class RankedGenome {
        private static final Comparator<RankedGenome> BY_FITNESS_DESC = new Comparator<RankedGenome>() {
            @Override
            public int compare(RankedGenome left, RankedGenome right) {
                return Float.compare(right.fitness, left.fitness);
            }
        };

        private final AiPersonalityGenome genome;
        private float fitness = -Float.MAX_VALUE;
        private RatassGame.AiTournamentResult tournament;

        private RankedGenome(AiPersonalityGenome genome) {
            this.genome = genome;
        }

        private RankedGenome copy() {
            RankedGenome copy = new RankedGenome(genome.copy());
            copy.fitness = fitness;
            copy.tournament = tournament;
            return copy;
        }

        private RankedGenome copyForNextGeneration() {
            return new RankedGenome(genome.copy());
        }
    }
}
