package com.github.jbescos.gameplay.roguelite;

/** Pulls the offender back repeatedly; Venom Web adds one native debuff per hook. */
final class WebBarrageRevengeEffect extends RevengeUpgradeEffect {
    static final float HOOK_INTERVAL_SECONDS = 3f;
    static final int BASE_HOOK_COUNT = 3;

    private static final RogueliteCardId[] DEBUFF_CARD_IDS = {
        RogueliteCardId.DRAFT_VENDETTA,
        RogueliteCardId.TAR_TETHER,
        RogueliteCardId.EMP_SNARE,
        RogueliteCardId.VOID_ANCHOR,
        RogueliteCardId.SENSOR_JAMMER,
        RogueliteCardId.GRID_BLACKOUT,
        RogueliteCardId.TOTAL_BLACKOUT
    };

    private final RogueliteRandom random;
    private float elapsedSeconds;
    private int hookCount = BASE_HOOK_COUNT;
    private int emittedHookCount;
    private int usedDebuffMask;
    private boolean hookInProgress;
    private boolean venomWebEnabled;
    private boolean amplificationApplied;

    WebBarrageRevengeEffect() {
        this(System.nanoTime() ^ ((long) RogueliteCardId.CIPHER_SIPHON.ordinal() << 32));
    }

    WebBarrageRevengeEffect(long seed) {
        super(RogueliteCardId.CIPHER_SIPHON, RevengeWorkflow.TARGET_IMMEDIATE);
        random = new RogueliteRandom(seed);
    }

    @Override
    boolean isActive() {
        return hasTarget();
    }

    @Override
    boolean isReady() {
        return hasTarget() && !hookInProgress && isNextHookDue();
    }

    @Override
    boolean isArmed() {
        return hasTarget();
    }

    @Override
    float readiness() {
        if (!hasTarget()) {
            return 0f;
        }
        if (hookInProgress) {
            return 1f;
        }
        float nextHookTime = (emittedHookCount + 1) * HOOK_INTERVAL_SECONDS;
        return RogueliteEffectMath.clamp(
                elapsedSeconds / Math.max(HOOK_INTERVAL_SECONDS, nextHookTime),
                0f,
                1f);
    }

    @Override
    float activeTimeRemainingSeconds() {
        return hasTarget()
                ? Math.max(0f, hookCount * HOOK_INTERVAL_SECONDS - elapsedSeconds)
                : 0f;
    }

    @Override
    int activeDisplayPriority() {
        return 5;
    }

    @Override
    float revengeTargetAgeSeconds() {
        return targetAgeSeconds();
    }

    @Override
    boolean allowsOffRoadOffenderStrike() {
        return true;
    }

    @Override
    protected boolean isExecutionInProgress() {
        return hasTarget();
    }

    @Override
    protected void prepareFromHit(int vehicleId, float impactStrength) {
        elapsedSeconds = 0f;
        hookCount = BASE_HOOK_COUNT;
        emittedHookCount = 0;
        usedDebuffMask = 0;
        hookInProgress = false;
        amplificationApplied = false;
    }

    @Override
    void update(float delta, float timerDelta, RogueliteDrivingFrame frame) {
        if (!hasTarget()) {
            return;
        }
        advanceTargetAge(delta);
        elapsedSeconds += Math.max(0f, timerDelta);
    }

    @Override
    RogueliteRevengeStrike tryActivateOffenderStrike(
            int targetVehicleId,
            float distance,
            boolean offenderAhead) {
        if (!targets(targetVehicleId) || hookInProgress || !isNextHookDue()) {
            return null;
        }
        emittedHookCount++;
        hookInProgress = true;
        RogueliteCardId debuffCardId = venomWebEnabled ? selectDebuffCardId() : null;
        return RogueliteRevengeStrike.hook(
                getCardId(),
                emittedHookCount,
                debuffCardId);
    }

    @Override
    void completeOffenderStrike(RogueliteCardId cardId) {
        if (cardId != getCardId() || !hasTarget()) {
            return;
        }
        hookInProgress = false;
        if (emittedHookCount >= hookCount) {
            clearSequence();
        }
    }

    @Override
    void amplifyActiveRevenge(float multiplier) {
        if (!hasTarget() || amplificationApplied || multiplier <= 1f) {
            return;
        }
        hookCount = Math.max(
                BASE_HOOK_COUNT,
                Math.round(BASE_HOOK_COUNT * multiplier));
        amplificationApplied = true;
    }

    @Override
    void setVenomWebEnabled(boolean enabled) {
        venomWebEnabled = enabled;
    }

    @Override
    protected void onTargetCancelled() {
        clearSequenceState();
    }

    private boolean isNextHookDue() {
        return emittedHookCount < hookCount
                && elapsedSeconds >= (emittedHookCount + 1) * HOOK_INTERVAL_SECONDS;
    }

    private RogueliteCardId selectDebuffCardId() {
        int allUsedMask = (1 << DEBUFF_CARD_IDS.length) - 1;
        if (usedDebuffMask == allUsedMask) {
            usedDebuffMask = 0;
        }
        int availableCount = DEBUFF_CARD_IDS.length - Integer.bitCount(usedDebuffMask);
        int selectedAvailableIndex = random.nextInt(availableCount);
        for (int index = 0; index < DEBUFF_CARD_IDS.length; index++) {
            int bit = 1 << index;
            if ((usedDebuffMask & bit) != 0) {
                continue;
            }
            if (selectedAvailableIndex-- == 0) {
                usedDebuffMask |= bit;
                return DEBUFF_CARD_IDS[index];
            }
        }
        throw new IllegalStateException("No Web Barrage debuff available");
    }

    private void clearSequence() {
        clearTarget();
        clearSequenceState();
    }

    private void clearSequenceState() {
        elapsedSeconds = 0f;
        hookCount = BASE_HOOK_COUNT;
        emittedHookCount = 0;
        usedDebuffMask = 0;
        hookInProgress = false;
        amplificationApplied = false;
    }
}
