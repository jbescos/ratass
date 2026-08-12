package com.github.jbescos.gameplay.roguelite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

public class RandomCardEffectTest {
    private static final RogueliteCardId[] RANDOM_CARDS = {
        RogueliteCardId.LUCKY_SPARK,
        RogueliteCardId.CHAOS_RELAY,
        RogueliteCardId.WILDCARD_CORE,
        RogueliteCardId.LOADED_GRUDGE,
        RogueliteCardId.CHAOS_RETORT,
        RogueliteCardId.FATES_REVENGE
    };

    @Test
    public void eachWildcardOnlySelectsRealCardsFromItsTypeAndTier() {
        for (RogueliteCardId wildcardId : RANDOM_CARDS) {
            RogueliteCardDefinition wildcard = RogueliteCardCatalog.get(wildcardId);
            List<RogueliteCardId> candidates = RandomCardEffect.candidateCardIds(
                    wildcard.getSlotType(), wildcard.getTier());

            assertFalse("No candidates for " + wildcardId, candidates.isEmpty());
            for (RogueliteCardId candidateId : candidates) {
                RogueliteCardDefinition candidate = RogueliteCardCatalog.get(candidateId);
                assertEquals(wildcard.getSlotType(), candidate.getSlotType());
                assertEquals(wildcard.getTier(), candidate.getTier());
                assertFalse(RandomCardEffect.isRandomCard(candidateId));
            }
        }
    }

    @Test
    public void powerupPreparesADifferentCardAfterTheCurrentEffectExecutes() {
        RandomCardEffect effect = effectPreparedAs(
                RogueliteCardId.LUCKY_SPARK,
                RogueliteCardId.NITRO_PULSE);
        RogueliteDrivingFrame frame = straightDrivingFrame();
        RogueliteCardId firstCard = effect.preparedCardId();

        boolean activated = false;
        for (int step = 0; step < 200 && !activated; step++) {
            effect.advance(0.1f, 0.1f, frame);
            activated = effect.isActive();
        }

        assertTrue("Prepared Nitro Pulse never activated", activated);
        assertEquals(RogueliteCardId.NITRO_PULSE, effect.activeDisplayCardId());
        for (int step = 0; step < 200 && effect.preparedCardId() == firstCard; step++) {
            effect.advance(0.1f, 0.1f, frame);
        }
        assertNotEquals(firstCard, effect.preparedCardId());
        assertEquals(
                RogueliteSlotType.POWERUP,
                RogueliteCardCatalog.get(effect.preparedCardId()).getSlotType());
        assertEquals(1, RogueliteCardCatalog.get(effect.preparedCardId()).getTier());
    }

    @Test
    public void randomPowerupDelegatesTemporaryBestDriverControl() {
        RandomCardEffect effect = effectPreparedAs(
                RogueliteCardId.LUCKY_SPARK,
                RogueliteCardId.ACE_HOTLINE);
        RogueliteDrivingFrame frame = straightDrivingFrame();

        for (int step = 0; step < 250 && !effect.usesBestDriver(); step++) {
            effect.advance(0.1f, 0.1f, frame);
        }

        assertTrue(effect.usesBestDriver());
        assertEquals(RogueliteCardId.ACE_HOTLINE, effect.activeDisplayCardId());
    }

    @Test
    public void randomPowerupDelegatesTimeDilation() {
        RandomCardEffect effect = effectPreparedAs(
                RogueliteCardId.LUCKY_SPARK,
                RogueliteCardId.TIME_RIPPLE);
        RogueliteDrivingFrame frame = straightDrivingFrame();

        for (int step = 0; step < 700 && !effect.acceleratesOwnDecisions(); step++) {
            effect.advance(0.1f, 0.1f, frame);
        }

        assertTrue(effect.acceleratesOwnDecisions());
        assertEquals(RogueliteCardId.TIME_RIPPLE, effect.activeDisplayCardId());
    }

    @Test
    public void revengeKeepsItsPreparedCardUntilItsStrikeExecutesThenRerolls() {
        RandomCardEffect effect = effectPreparedAs(
                RogueliteCardId.CHAOS_RETORT,
                RogueliteCardId.EMP_SNARE);
        RogueliteCardId firstCard = effect.preparedCardId();

        effect.onHitBy(42, 12f);
        assertTrue(effect.isArmed());
        assertEquals(42, effect.revengeTargetVehicleId());
        RogueliteRevengeStrike strike = effect.tryActivateOffenderStrike(42, 3.5f, true);

        assertNotNull(strike);
        assertEquals(RogueliteCardId.EMP_SNARE, strike.getCardId());
        RogueliteDrivingFrame frame = straightDrivingFrame();
        for (int step = 0; step < 200 && effect.preparedCardId() == firstCard; step++) {
            effect.advance(0.1f, 0.1f, frame);
        }
        assertNotEquals(firstCard, effect.preparedCardId());
        assertEquals(
                RogueliteSlotType.REVENGE,
                RogueliteCardCatalog.get(effect.preparedCardId()).getSlotType());
        assertEquals(2, RogueliteCardCatalog.get(effect.preparedCardId()).getTier());
    }

    @Test
    public void randomPositionHijackRerollsWhenItsAheadConditionExpires() {
        RandomCardEffect effect = effectPreparedAs(
                RogueliteCardId.CHAOS_RETORT,
                RogueliteCardId.RECOVERY_BEACON);
        RogueliteCardId firstCard = effect.preparedCardId();
        effect.onHitBy(42, 12f);
        effect.advance(3.1f, 3.1f, straightDrivingFrame());

        assertTrue(effect.expireOffenderStrikeIfConditionFailed(42, false));
        assertFalse(effect.isArmed());
        assertFalse(effect.isActive());
        assertEquals(-1, effect.revengeTargetVehicleId());
        assertNotEquals(firstCard, effect.preparedCardId());
    }

    @Test
    public void everyPowerupWildcardCandidateExecutesAndRerolls() {
        assertEveryPowerupCandidateExecutes(RogueliteCardId.LUCKY_SPARK);
        assertEveryPowerupCandidateExecutes(RogueliteCardId.CHAOS_RELAY);
        assertEveryPowerupCandidateExecutes(RogueliteCardId.WILDCARD_CORE);
    }

    @Test
    public void everyRevengeWildcardCandidateExecutesAndRerolls() {
        assertEveryRevengeCandidateExecutes(RogueliteCardId.LOADED_GRUDGE);
        assertEveryRevengeCandidateExecutes(RogueliteCardId.CHAOS_RETORT);
        assertEveryRevengeCandidateExecutes(RogueliteCardId.FATES_REVENGE);
    }

    @Test
    public void powerupWildcardsKeepLoadedConditionalCardsUntilTheyExecute() {
        assertBlockedPowerupWaitsForTrigger(
                RogueliteCardId.LUCKY_SPARK,
                RogueliteCardId.MIRROR_DUO);
        assertBlockedPowerupWaitsForTrigger(
                RogueliteCardId.CHAOS_RELAY,
                RogueliteCardId.MIRROR_TRIO);
        assertBlockedPowerupWaitsForTrigger(
                RogueliteCardId.WILDCARD_CORE,
                RogueliteCardId.OVERDRIVE_COIL);
    }

    @Test
    public void revengeWildcardsKeepTheLoadedCardWhileItWaitsToExecute() {
        RandomCardEffect effect = effectPreparedAs(
                RogueliteCardId.FATES_REVENGE,
                RogueliteCardId.CROWN_ENGINE);
        RogueliteCardId firstCard = effect.preparedCardId();

        effect.onHitBy(42, 12f);
        effect.advance(20f, 20f, straightWithoutTrafficFrame());

        assertEquals(firstCard, effect.preparedCardId());
        assertEquals(firstCard, effect.loadedDisplayCardId());
        assertEquals(42, effect.revengeTargetVehicleId());
        assertTrue(effect.isArmed());
    }

    @Test
    public void randomCrownBreakerExecutesWhenItGetsCloseToItsOffender() {
        RandomCardEffect effect = effectPreparedAs(
                RogueliteCardId.FATES_REVENGE,
                RogueliteCardId.CROWN_ENGINE);
        effect.onHitBy(42, 12f);

        assertEquals(30f, effect.activeTimeRemainingSeconds(), 0.0001f);
        assertNull(effect.tryActivateOffenderStrike(
                42,
                CrownBreakerRevengeEffect.RAM_TRIGGER_DISTANCE + 0.01f,
                true));
        assertNull(effect.tryActivateOffenderStrike(
                42,
                CrownBreakerRevengeEffect.RAM_TRIGGER_DISTANCE,
                true));
        effect.advance(2.9f, 2.9f, straightDrivingFrame());
        assertNull(effect.tryActivateOffenderStrike(
                42,
                CrownBreakerRevengeEffect.RAM_TRIGGER_DISTANCE,
                true));
        effect.advance(0.2f, 0.2f, straightDrivingFrame());
        RogueliteRevengeStrike strike = effect.tryActivateOffenderStrike(
                42,
                CrownBreakerRevengeEffect.RAM_TRIGGER_DISTANCE,
                true);

        assertNotNull(strike);
        assertTrue(effect.isActive());
        assertEquals(RogueliteCardId.CROWN_ENGINE, effect.activeDisplayCardId());
    }

    @Test
    public void randomPowerupDelegatesThePreparedRevengeAmplifier() {
        RandomCardEffect effect = effectPreparedAs(
                RogueliteCardId.CHAOS_RELAY,
                RogueliteCardId.VENGEANCE_CORE);

        assertEquals(1.50f, effect.revengeEffectMultiplier(), 0.0001f);
        effect.onRevengeActivated(4f);
        assertTrue(effect.isActive());
        assertEquals(RogueliteCardId.VENGEANCE_CORE, effect.activeDisplayCardId());
    }

    @Test
    public void randomRevengeRetargetsTheLatestHitWhileWaitingToExecute() {
        RandomCardEffect effect = effectPreparedAs(
                RogueliteCardId.FATES_REVENGE,
                RogueliteCardId.CROWN_ENGINE);
        RogueliteCardId loadedCardId = effect.preparedCardId();

        effect.onHitBy(42, 12f);
        effect.onHitBy(7, 20f);
        assertEquals(7, effect.revengeTargetVehicleId());

        effect.advance(10.1f, 10.1f, straightWithoutTrafficFrame());

        assertEquals(loadedCardId, effect.preparedCardId());
        assertTrue(effect.isArmed());
        assertEquals(7, effect.revengeTargetVehicleId());
    }

    @Test
    public void cancellingRandomRevengeDropsItsStoredTrigger() {
        RandomCardEffect effect = effectPreparedAs(
                RogueliteCardId.CHAOS_RETORT,
                RogueliteCardId.EMP_SNARE);
        effect.onHitBy(42, 12f);

        assertTrue(effect.cancelRevengeTarget(42));

        assertFalse(effect.isArmed());
        assertEquals(-1, effect.revengeTargetVehicleId());
    }

    private static void assertBlockedPowerupWaitsForTrigger(
            RogueliteCardId wildcardId,
            RogueliteCardId blockedCandidateId) {
        RandomCardEffect effect = effectPreparedAs(wildcardId, blockedCandidateId);
        assertEquals(
                MirrorPowerupSpec.COOLDOWN_SECONDS,
                effect.cooldownTimeRemainingSeconds(),
                0.0001f);
        RogueliteDrivingFrame noTraffic = straightWithoutTrafficFrame();
        for (int step = 0; step < 700 && !effect.isReady(); step++) {
            effect.advance(0.1f, 0.1f, noTraffic);
        }
        assertTrue(wildcardId + " never became ready", effect.isReady());
        RogueliteCardId firstCard = effect.preparedCardId();

        effect.advance(30f, 30f, noTraffic);

        assertEquals(firstCard, effect.preparedCardId());
        assertEquals(firstCard, effect.loadedDisplayCardId());
        assertTrue(effect.isReady());

        effect.advance(0.1f, 0.1f, straightDrivingFrame());
        assertTrue(blockedCandidateId + " did not execute once its condition was met",
                effect.isActive());
    }

    private static void assertEveryPowerupCandidateExecutes(
            RogueliteCardId wildcardId) {
        RogueliteCardDefinition wildcard = RogueliteCardCatalog.get(wildcardId);
        List<RogueliteCardId> candidates = RandomCardEffect.candidateCardIds(
                wildcard.getSlotType(), wildcard.getTier());
        for (RogueliteCardId candidateId : candidates) {
            RandomCardEffect effect = effectPreparedAs(wildcardId, candidateId);
            RogueliteCardId firstCard = effect.preparedCardId();

            if (candidateId == RogueliteCardId.GRUDGE_SPARK
                    || candidateId == RogueliteCardId.VENGEANCE_CORE
                    || candidateId == RogueliteCardId.NEMESIS_ENGINE) {
                effect.onRevengeActivated(2f);
            }

            for (int step = 0; step < 1000 && !effect.isActive(); step++) {
                effect.advance(
                        0.1f,
                        0.1f,
                        step % 2 == 0 ? straightDrivingFrame() : cornerDrivingFrame());
            }

            assertTrue(
                    wildcardId + " failed to execute " + candidateId,
                    effect.isActive());
            assertEquals(candidateId, effect.activeDisplayCardId());
            assertRerollsAfterExecution(effect, firstCard, wildcardId, candidateId);
        }
    }

    private static void assertEveryRevengeCandidateExecutes(
            RogueliteCardId wildcardId) {
        RogueliteCardDefinition wildcard = RogueliteCardCatalog.get(wildcardId);
        List<RogueliteCardId> candidates = RandomCardEffect.candidateCardIds(
                wildcard.getSlotType(), wildcard.getTier());
        for (RogueliteCardId candidateId : candidates) {
            RandomCardEffect effect = effectPreparedAs(wildcardId, candidateId);
            RogueliteCardId firstCard = effect.preparedCardId();

            effect.onHitBy(42, 12f);
            executePreparedRevenge(effect, candidateId);
            assertRerollsAfterExecution(effect, firstCard, wildcardId, candidateId);
        }
    }

    private static void executePreparedRevenge(
            RandomCardEffect effect,
            RogueliteCardId candidateId) {
        switch (candidateId) {
            case DRAFT_MAGNET:
            case REPULSOR_WAVE:
            case REPULSOR_SURGE:
                effect.advance(0.1f, 0.1f, straightDrivingFrame());
                assertTrue(candidateId + " did not activate", effect.isActive());
                assertTrue(
                        candidateId + " did not expose its mechanic",
                        effect.isDraftMagnetActive());
                return;
            case RECOVERY_BEACON:
            case PAYBACK_SHIELD:
            case TRIAD_COUP:
                effect.setRevengeSecondaryTargetVehicleId(7);
                effect.advance(3.1f, 3.1f, straightDrivingFrame());
                break;
            case CROWN_ENGINE:
                effect.advance(
                        CrownBreakerRevengeEffect.PREPARATION_SECONDS,
                        CrownBreakerRevengeEffect.PREPARATION_SECONDS,
                        straightDrivingFrame());
                RogueliteRevengeStrike crownStrike = effect.tryActivateOffenderStrike(
                        42,
                        CrownBreakerRevengeEffect.RAM_TRIGGER_DISTANCE,
                        true);
                assertNotNull(candidateId + " did not execute its proximity ram", crownStrike);
                assertEquals(candidateId, crownStrike.getCardId());
                return;
            case HUNTER_BARRAGE:
            case HUNTER_STORM:
                int shotCount = candidateId == RogueliteCardId.HUNTER_STORM
                        ? HunterBarrageRevengeEffect.STORM_SHOT_COUNT
                        : HunterBarrageRevengeEffect.SHOT_COUNT;
                float shotInterval = candidateId == RogueliteCardId.HUNTER_STORM
                        ? HunterBarrageRevengeEffect.STORM_SHOT_INTERVAL_SECONDS
                        : HunterBarrageRevengeEffect.SHOT_INTERVAL_SECONDS;
                for (int shot = 1; shot <= shotCount; shot++) {
                    effect.advance(shotInterval, shotInterval, straightDrivingFrame());
                    RogueliteRevengeStrike barrageStrike =
                            effect.tryActivateOffenderStrike(42, 1000f, false);
                    assertNotNull(candidateId + " missed shot " + shot, barrageStrike);
                    assertEquals(
                            RogueliteRevengeStrike.Action.PUSH_SHOT,
                            barrageStrike.getAction());
                    assertEquals(shot, barrageStrike.getStrikeIndex());
                }
                return;
            default:
                break;
        }

        RogueliteRevengeStrike strike = effect.tryActivateOffenderStrike(42, 3.5f, true);
        assertNotNull(candidateId + " did not execute its strike", strike);
        assertEquals(candidateId, strike.getCardId());
        if (candidateId == RogueliteCardId.SENSOR_JAMMER
                || candidateId == RogueliteCardId.GRID_BLACKOUT
                || candidateId == RogueliteCardId.TOTAL_BLACKOUT) {
            assertEquals(RogueliteRevengeStrike.Action.CURSE, strike.getAction());
        }
        if (candidateId == RogueliteCardId.PAYBACK_SHIELD) {
            effect.completeOffenderStrike(candidateId);
        }
    }

    private static void assertRerollsAfterExecution(
            RandomCardEffect effect,
            RogueliteCardId firstCard,
            RogueliteCardId wildcardId,
            RogueliteCardId candidateId) {
        for (int step = 0; step < 500 && effect.preparedCardId() == firstCard; step++) {
            effect.advance(0.1f, 0.1f, straightDrivingFrame());
        }
        assertNotEquals(
                wildcardId + " did not reroll after executing " + candidateId,
                firstCard,
                effect.preparedCardId());
    }

    private static RandomCardEffect effectPreparedAs(
            RogueliteCardId wildcardId,
            RogueliteCardId preparedId) {
        for (long seed = 1; seed <= 10000; seed++) {
            RandomCardEffect effect = new RandomCardEffect(wildcardId, 0f, seed);
            if (effect.preparedCardId() == preparedId) {
                return effect;
            }
        }
        throw new AssertionError("No deterministic seed prepared " + preparedId);
    }

    private static RogueliteDrivingFrame straightDrivingFrame() {
        RogueliteDrivingFrame frame = new RogueliteDrivingFrame();
        frame.set(
                1f,
                true,
                false,
                false,
                0f,
                0.40f,
                0f,
                50f,
                500f,
                0f,
                0f,
                1f,
                0f,
                0f,
                0.6f,
                false,
                0.5f);
        return frame;
    }

    private static RogueliteDrivingFrame straightWithoutTrafficFrame() {
        RogueliteDrivingFrame frame = new RogueliteDrivingFrame();
        frame.set(
                1f,
                true,
                false,
                false,
                0f,
                0.40f,
                0f,
                50f,
                500f,
                0f,
                0f,
                1f,
                0f,
                0f,
                0f,
                false,
                0.5f);
        return frame;
    }

    private static RogueliteDrivingFrame cornerDrivingFrame() {
        RogueliteDrivingFrame frame = new RogueliteDrivingFrame();
        frame.set(
                1f,
                true,
                false,
                false,
                0.08f,
                0.40f,
                0f,
                50f,
                500f,
                0f,
                0.30f,
                0.25f,
                0.30f,
                0f,
                0.6f,
                false,
                0.5f);
        return frame;
    }
}
