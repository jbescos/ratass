package com.github.jbescos.gameplay.roguelite;

/** Computes allocation-free lateral positions for temporary mirror cars. */
public final class MirrorFormationLayout {
    private static final float VEHICLE_SPACING_MULTIPLIER = 1.30f;
    private static final float EDGE_MARGIN_MULTIPLIER = 0.10f;

    private MirrorFormationLayout() {
    }

    public static int fillMirrorOffsets(
            float ownerOffset,
            float leftClearance,
            float rightClearance,
            float vehicleWidth,
            int totalVehicleCount,
            float[] out) {
        int requested = Math.max(0, totalVehicleCount - 1);
        if (out == null || requested == 0) {
            return 0;
        }
        requested = Math.min(requested, out.length);

        float safeHalfWidth = vehicleWidth * (0.5f + EDGE_MARGIN_MULTIPLIER);
        float minimumOffset = -Math.max(0f, rightClearance - safeHalfWidth);
        float maximumOffset = Math.max(0f, leftClearance - safeHalfWidth);
        if (leftClearance <= 0f && rightClearance <= 0f) {
            minimumOffset = ownerOffset - vehicleWidth * requested;
            maximumOffset = ownerOffset + vehicleWidth * requested;
        }

        float spacing = Math.max(0.01f, vehicleWidth * VEHICLE_SPACING_MULTIPLIER);
        float leftRoom = Math.max(0f, maximumOffset - ownerOffset);
        float rightRoom = Math.max(0f, ownerOffset - minimumOffset);
        int leftCapacity = (int) (leftRoom / spacing);
        int rightCapacity = (int) (rightRoom / spacing);
        int leftUsed = 0;
        int rightUsed = 0;
        int count = 0;

        while (count < requested) {
            boolean canUseLeft = leftUsed < leftCapacity;
            boolean canUseRight = rightUsed < rightCapacity;
            if (!canUseLeft && !canUseRight) {
                break;
            }

            float leftRemaining = leftRoom - leftUsed * spacing;
            float rightRemaining = rightRoom - rightUsed * spacing;
            boolean useLeft = canUseLeft && (!canUseRight || leftRemaining >= rightRemaining);
            if (useLeft) {
                leftUsed++;
                out[count++] = ownerOffset + leftUsed * spacing;
            } else {
                rightUsed++;
                out[count++] = ownerOffset - rightUsed * spacing;
            }
        }

        while (count < requested) {
            float alpha = (count + 1f) / (requested + 1f);
            float candidate = minimumOffset + (maximumOffset - minimumOffset) * alpha;
            if (Math.abs(candidate - ownerOffset) < vehicleWidth * 0.55f) {
                candidate = ownerOffset <= (minimumOffset + maximumOffset) * 0.5f
                        ? maximumOffset
                        : minimumOffset;
            }
            out[count++] = candidate;
        }
        return count;
    }
}
