package com.github.jbescos.presentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.github.jbescos.gameplay.roguelite.RogueliteCardId;
import org.junit.Test;

public class HunterProjectileSpriteTest {
    @Test
    public void selectsDistinctSpritesForBothHunterCards() {
        assertTrue(HunterProjectileSprite.supports(RogueliteCardId.HUNTER_BARRAGE));
        assertTrue(HunterProjectileSprite.supports(RogueliteCardId.HUNTER_STORM));
        assertEquals(
                HunterProjectileSprite.BARRAGE_ASSET_PATH,
                HunterProjectileSprite.assetPath(RogueliteCardId.HUNTER_BARRAGE));
        assertEquals(
                HunterProjectileSprite.STORM_ASSET_PATH,
                HunterProjectileSprite.assetPath(RogueliteCardId.HUNTER_STORM));
    }

    @Test
    public void stormIsVisuallyLargerAndUnrelatedCardsHaveNoSprite() {
        assertTrue(
                HunterProjectileSprite.sizeScale(RogueliteCardId.HUNTER_STORM)
                        > HunterProjectileSprite.sizeScale(RogueliteCardId.HUNTER_BARRAGE));
        assertFalse(HunterProjectileSprite.supports(RogueliteCardId.TAR_TETHER));
        assertNull(HunterProjectileSprite.assetPath(RogueliteCardId.TAR_TETHER));
        assertEquals(0f, HunterProjectileSprite.sizeScale(RogueliteCardId.TAR_TETHER), 0f);
    }
}
