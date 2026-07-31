package com.github.jbescos.gameplay.roguelite;

final class RogueliteEffectFactory {
    private RogueliteEffectFactory() {
    }

    static RogueliteUpgradeEffect create(
            RogueliteCardId id,
            float gadgetCycleOffset) {
        RogueliteCardDefinition definition = RogueliteCardCatalog.get(id);
        switch (definition.getSlotType()) {
            case TUNING:
                return new TieredTuningEffect(id);
            case TECHNIQUE:
                return new RaceTechniqueEffect(id);
            case GADGET:
                return new CooldownGadgetEffect(id, gadgetCycleOffset);
            default:
                throw new IllegalArgumentException("Unsupported roguelite card slot: " + id);
        }
    }
}
