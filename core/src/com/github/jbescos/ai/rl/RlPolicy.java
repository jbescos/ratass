package com.github.jbescos.ai.rl;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.github.jbescos.ai.AiControlDecision;

public final class RlPolicy {
    private static final String FORMAT = "ratass-rl-policy-v1";

    private final Layer[] layers;
    private final int observationSize;
    private final int actionSize;
    private final int scratchSize;

    private RlPolicy(int observationSize, int actionSize, Layer[] layers) {
        this.observationSize = observationSize;
        this.actionSize = actionSize;
        this.layers = layers;

        int maxOutputSize = 0;
        for (int i = 0; i < layers.length; i++) {
            maxOutputSize = Math.max(maxOutputSize, layers[i].outputSize);
        }
        scratchSize = maxOutputSize;
    }

    public static RlPolicy fromJson(String json) {
        JsonValue root = new JsonReader().parse(json);
        String format = root.getString("format", "");
        if (!FORMAT.equals(format)) {
            throw new IllegalArgumentException("Unsupported RL policy format: " + format);
        }

        int observationSize = root.getInt("observationSize");
        int actionSize = root.getInt("actionSize");
        JsonValue layerValues = root.get("layers");
        if (layerValues == null || layerValues.size == 0) {
            throw new IllegalArgumentException("RL policy has no layers.");
        }

        Layer[] layers = new Layer[layerValues.size];
        int index = 0;
        for (JsonValue layerValue = layerValues.child; layerValue != null; layerValue = layerValue.next) {
            layers[index++] = Layer.fromJson(layerValue);
        }

        if (layers[0].inputSize != observationSize) {
            throw new IllegalArgumentException(
                    "RL policy input size "
                            + layers[0].inputSize
                            + " does not match observation size "
                            + observationSize);
        }
        if (layers[layers.length - 1].outputSize < actionSize) {
            throw new IllegalArgumentException("RL policy output is smaller than the action size.");
        }

        return new RlPolicy(observationSize, actionSize, layers);
    }

    public int getObservationSize() {
        return observationSize;
    }

    public int getActionSize() {
        return actionSize;
    }

    public int getScratchSize() {
        return scratchSize;
    }

    public AiControlDecision computeAction(
            float[] observation,
            float[] scratchA,
            float[] scratchB,
            AiControlDecision out) {
        if (observation == null || observation.length < observationSize) {
            throw new IllegalArgumentException("Observation is smaller than the policy input.");
        }
        if (scratchA == null || scratchA.length < scratchSize
                || scratchB == null || scratchB.length < scratchSize) {
            throw new IllegalArgumentException("RL policy scratch buffers are too small.");
        }

        float[] input = observation;
        float[] output = scratchA;
        for (int i = 0; i < layers.length; i++) {
            Layer layer = layers[i];
            layer.forward(input, output);
            input = output;
            output = output == scratchA ? scratchB : scratchA;
        }

        return out.set(
                MathUtils.clamp(input[0], -1f, 1f),
                MathUtils.clamp(input[1], -1f, 1f));
    }

    private static float[] readFloatArray(JsonValue json) {
        if (json == null) {
            throw new IllegalArgumentException("Missing float array in RL policy.");
        }

        float[] values = new float[json.size];
        int index = 0;
        for (JsonValue value = json.child; value != null; value = value.next) {
            values[index++] = value.asFloat();
        }
        return values;
    }

    private static final class Layer {
        private final int inputSize;
        private final int outputSize;
        private final float[] weights;
        private final float[] bias;
        private final boolean tanhActivation;

        private Layer(
                int inputSize,
                int outputSize,
                float[] weights,
                float[] bias,
                boolean tanhActivation) {
            this.inputSize = inputSize;
            this.outputSize = outputSize;
            this.weights = weights;
            this.bias = bias;
            this.tanhActivation = tanhActivation;
        }

        private static Layer fromJson(JsonValue json) {
            int inputSize = json.getInt("inputSize");
            int outputSize = json.getInt("outputSize");
            float[] weights = readFloatArray(json.get("weights"));
            float[] bias = readFloatArray(json.get("bias"));
            if (weights.length != inputSize * outputSize) {
                throw new IllegalArgumentException("RL policy layer has an invalid weight count.");
            }
            if (bias.length != outputSize) {
                throw new IllegalArgumentException("RL policy layer has an invalid bias count.");
            }
            return new Layer(
                    inputSize,
                    outputSize,
                    weights,
                    bias,
                    "tanh".equals(json.getString("activation", "linear")));
        }

        private void forward(float[] input, float[] output) {
            for (int row = 0; row < outputSize; row++) {
                float sum = bias[row];
                int weightOffset = row * inputSize;
                for (int column = 0; column < inputSize; column++) {
                    sum += weights[weightOffset + column] * input[column];
                }
                output[row] = tanhActivation ? (float) Math.tanh(sum) : sum;
            }
        }
    }
}
