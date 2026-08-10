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
    public void powerupWildcardsRerollAfterTenReadySecondsWithoutATrigger() {
        assertBlockedPowerupTimesOut(
                RogueliteCardId.LUCKY_SPARK,
                RogueliteCardId.MIRROR_DUO);
        assertBlockedPowerupTimesOut(
                RogueliteCardId.CHAOS_RELAY,
                RogueliteCardId.MIRROR_TRIO);
        assertBlockedPowerupTimesOut(
                RogueliteCardId.WILDCARD_CORE,
                RogueliteCardId.OVERDRIVE_COIL);
    }

    @Test
    public void revengeWildcardsRerollAfterTenArmedSecondsAndKeepTheOffender() {
        RandomCardEffect effect = effectPreparedAs(
                RogueliteCardId.FATES_REVENGE,
                RogueliteCardId.CROWN_ENGINE);
        RogueliteCardId firstCard = effect.preparedCardId();

        effect.onHitBy(42, 12f);
        effect.advance(9.9f, 9.9f, straightWithoutTrafficFrame());
        assertEquals(firstCard, effect.preparedCardId());
        effect.advance(0.2f, 0.2f, straightWithoutTrafficFrame());

        assertNotEquals(firstCard, effect.preparedCardId());
        assertTrue(effect.isArmed() || effect.isActive());
    }

    @Test
    public void randomCrownBreakerExecutesOnlyWhenItHitsItsOffender() {
        RandomCardEffect effect = effectPreparedAs(
                RogueliteCardId.FATES_REVENGE,
                RogueliteCardId.CROWN_ENGINE);
        effect.onHitBy(42, 12f);

        assertEquals(30f, effect.activeTimeRemainingSeconds(), 0.0001f);
        assertNull(effect.tryActivateOffenderStrike(42, 0.5f, true));
        RogueliteRevengeStrike strike = effect.tryActivateOffenderHit(42);

        assertNotNull(strike);
        assertTrue(effect.isActive());
        assertEquals(RogueliteCardId.CROWN_ENGINE, effect.activeDisplayCardId());
    }

    private static void assertBlockedPowerupTimesOut(
            RogueliteCardId wildcardId,
            RogueliteCardId blockedCandidateId) {
        RandomCardEffect effect = effectPreparedAs(wildcardId, blockedCandidateId);
        RogueliteDrivingFrame noTraffic = straightWithoutTrafficFrame();
        for (int step = 0; step < 200 && !effect.isReady(); step++) {
            effect.advance(0.1f, 0.1f, noTraffic);
        }
        assertTrue(wildcardId + " never became ready", effect.isReady());
        RogueliteCardId firstCard = effect.preparedCardId();

        effect.advance(9.9f, 9.9f, noTraffic);
        assertEquals(firstCard, effect.preparedCardId());
        effect.advance(0.2f, 0.2f, noTraffic);

        assertNotEquals(firstCard, effect.preparedCardId());
        assertEquals(
                RogueliteCardCatalog.get(wildcardId).getTier(),
                RogueliteCardCatalog.get(effect.preparedCardId()).getTier());
    }

    private static void assertEveryPowerupCandidateExecutes(
            RogueliteCardId wildcardId) {
        RogueliteCardDefinition wildcard = RogueliteCardCatalog.get(wildcardId);
        List<RogueliteCardId> candidates = RandomCardEffect.candidateCardIds(
                wildcard.getSlotType(), wildcard.getTier());
        for (RogueliteCardId candidateId : candidates) {
            RandomCardEffect effect = effectPreparedAs(wildcardId, candidateId);
            RogueliteCardId firstCard = effect.preparedCardId();

            for (int step = 0; step < 200 && !effect.isActive(); step++) {
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
            case REPULSOR_SURGE:
                effect.advance(0.1f, 0.1f, straightDrivingFrame());
                assertTrue(candidateId + " did not activate", effect.isActive());
                assertTrue(
                        candidateId + " did not expose its mechanic",
                        effect.isDraftMagnetActive());
                return;
            case RAM_REACTOR:
                assertTrue(candidateId + " did not become ready", effect.isImpactCounterReady());
                effect.consumeImpactCounter();
                assertTrue(candidateId + " did not execute", effect.isActive());
                return;
            case RECOVERY_BEACON:
            case PAYBACK_SHIELD:
                effect.advance(3.1f, 3.1f, straightDrivingFrame());
                break;
            case CROWN_ENGINE:
                RogueliteRevengeStrike crownStrike = effect.tryActivateOffenderHit(42);
                assertNotNull(candidateId + " did not execute its return hit", crownStrike);
                assertEquals(candidateId, crownStrike.getCardId());
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
