import com.badlogic.gdx.utils.Array;
import com.github.jbescos.RatassGame;
import com.github.jbescos.ai.AiControlDecision;
import com.github.jbescos.ai.rl.RlPolicy;
import com.github.jbescos.gameplay.ArenaMap;
import com.github.jbescos.gameplay.maps.ArenaMaps;

/** Headless rollouts for offline policy search, using the game's exact inference and physics. */
public final class DriverPolicyBenchmark {
    private final Array<ArenaMap> maps = ArenaMaps.createDefaultSet();

    /** Teacher actions sampled at the student's cadence, including held teacher commands. */
    public float[][] demonstrations(String json, int teacherRepeat, int sampleRepeat,
            int laps, long seed, int mapIndex) {
        if (sampleRepeat < 1 || teacherRepeat < 1 || teacherRepeat % sampleRepeat != 0) {
            throw new IllegalArgumentException("Teacher cadence must be divisible by sample cadence");
        }
        RlPolicy policy = RlPolicy.fromJson(json);
        RatassGame.RlTrainingConfig config = new RatassGame.RlTrainingConfig()
                .withControlledAgentCount(1).withFieldSize(1)
                .withActionRepeat(sampleRepeat).withRouteTargets(laps).withRaceMode(true)
                .withMaxActionSteps(Math.max(360, laps * 90) * 60 / sampleRepeat)
                .withNoProgressMaxActionSteps(0).withOffRoadFailureMaxActionSteps(0)
                .withRandomRaceSpawns(false).withSeed(seed)
                .withRewardBreakdownEnabled(false).withStepDetailsEnabled(true);
        config.addMap(maps.get(mapIndex));
        Array<float[]> examples = new Array<float[]>();
        float[] scratchA = new float[policy.getScratchSize()];
        float[] scratchB = new float[policy.getScratchSize()];
        float[] actions = new float[2];
        AiControlDecision decision = new AiControlDecision();
        try (RatassGame.RlTrainingEnvironment env = new RatassGame.RlTrainingEnvironment(config)) {
            RatassGame.RlStepResult result = env.reset();
            while (!result.episodeDone) {
                if (result.actionStep % (teacherRepeat / sampleRepeat) == 0) {
                    policy.computeAction(result.observations, scratchA, scratchB, decision);
                    actions[0] = decision.throttle;
                    actions[1] = decision.turn;
                }
                float[] example = new float[policy.getObservationSize() + 2];
                System.arraycopy(result.observations, 0, example, 0, policy.getObservationSize());
                example[policy.getObservationSize()] = actions[0];
                example[policy.getObservationSize() + 1] = actions[1];
                examples.add(example);
                result = env.step(actions);
            }
            if (result.routeTargetsReached[0] < laps) {
                throw new IllegalStateException("Teacher failed demonstration map " + mapIndex);
            }
        }
        float[][] result = new float[examples.size][];
        for (int i = 0; i < examples.size; i++) {
            result[i] = examples.get(i);
        }
        return result;
    }

    public double[][] evaluate(String json, int repeat, int laps, long seed,
            boolean randomSpawns, int carIndex) {
        return evaluate(json, repeat, laps, seed, randomSpawns, carIndex, "");
    }

    public double[][] evaluate(String json, int repeat, int laps, long seed,
            boolean randomSpawns, int carIndex, String tuningCard) {
        return evaluate(json, repeat, laps, seed, randomSpawns, carIndex, tuningCard, 1f);
    }

    public double[][] evaluate(String json, int repeat, int laps, long seed,
            boolean randomSpawns, int carIndex, String tuningCard, float tuningMultiplier) {
        RlPolicy policy = RlPolicy.fromJson(json);
        double[][] rows = new double[maps.size][6];
        float[] scratchA = new float[policy.getScratchSize()];
        float[] scratchB = new float[policy.getScratchSize()];
        float[] actions = new float[2];
        AiControlDecision decision = new AiControlDecision();
        for (int m = 0; m < maps.size; m++) {
            RatassGame.RlTrainingConfig config = new RatassGame.RlTrainingConfig()
                    .withControlledAgentCount(1).withFieldSize(1)
                    .withActionRepeat(repeat).withRouteTargets(laps).withRaceMode(true)
                    .withMaxActionSteps(Math.max(360, laps * 90) * 60 / repeat)
                    .withNoProgressMaxActionSteps(0).withOffRoadFailureMaxActionSteps(0)
                    .withRandomRaceSpawns(randomSpawns).withSeed(seed)
                    .withRewardBreakdownEnabled(true).withStepDetailsEnabled(true);
            if (carIndex >= 0) {
                config.withCarPerformanceIndex(carIndex);
            }
            if (!tuningCard.isEmpty()) {
                config.withBenchmarkTuningCard(tuningCard);
                config.withBenchmarkTuningEffectMultiplier(tuningMultiplier);
            }
            config.addMap(maps.get(m));
            try (RatassGame.RlTrainingEnvironment env = new RatassGame.RlTrainingEnvironment(config)) {
                RatassGame.RlStepResult result = env.reset();
                int offRoadIndex = -1;
                for (int i = 0; i < result.rewardBreakdownNames.length; i++) {
                    if ("off_road".equals(result.rewardBreakdownNames[i])) {
                        offRoadIndex = i;
                    }
                }
                double offRoad = 0;
                double throttleChanges = 0;
                double steeringChanges = 0;
                float lastThrottle = 0;
                float lastTurn = 0;
                while (!result.episodeDone) {
                    policy.computeAction(result.observations, scratchA, scratchB, decision);
                    actions[0] = decision.throttle;
                    actions[1] = decision.turn;
                    if (result.actionStep > 0) {
                        throttleChanges += Math.abs(actions[0] - lastThrottle);
                        steeringChanges += Math.abs(actions[1] - lastTurn);
                    }
                    lastThrottle = actions[0];
                    lastTurn = actions[1];
                    result = env.step(actions);
                    if (offRoadIndex >= 0 && result.rewardBreakdown[offRoadIndex] < 0) {
                        offRoad++;
                    }
                }
                rows[m][0] = result.routeTargetsReached[0] >= laps ? 1 : 0;
                rows[m][1] = result.actionStep * repeat / 60.0 / laps;
                rows[m][2] = offRoad / Math.max(1, result.actionStep);
                rows[m][3] = throttleChanges / Math.max(1, result.actionStep);
                rows[m][4] = steeringChanges / Math.max(1, result.actionStep);
                rows[m][5] = result.routeTargetsReached[0];
            }
        }
        return rows;
    }
}
