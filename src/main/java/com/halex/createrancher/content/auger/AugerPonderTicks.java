package com.halex.createrancher.content.auger;

import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.world.level.Level;

// Counts ticks for augers in Ponder scenes
public final class AugerPonderTicks {

    // Added to Ponder's scenes by PonderSceneMixin
    public interface Counted {
        long createrancher$ticks();
    }

    private AugerPonderTicks() {}

    static long now(Level level) {
        if (level instanceof PonderLevel ponder && ponder.scene instanceof Counted scene)
            return scene.createrancher$ticks();
        return AugerBlockEntity.clientTick;
    }
}
