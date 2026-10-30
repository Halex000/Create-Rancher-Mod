package com.halex.createrancher.content.grate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.world.level.block.SoundType;

// Walking on a grate sounds like walking on an Item Drain
public class GrateSounds {

    private static final SoundType DRAIN = SoundType.COPPER;
    private static final Map<SoundType, SoundType> WITH_GRATE = new ConcurrentHashMap<>();

    // The block's sounds with the grate's steps and landings
    public static SoundType withGrateSteps(SoundType block) {
        return WITH_GRATE.computeIfAbsent(block, sounds -> new SoundType(sounds.getVolume(), sounds.getPitch(),
                sounds.getBreakSound(), DRAIN.getStepSound(), sounds.getPlaceSound(), sounds.getHitSound(),
                DRAIN.getFallSound()));
    }
}
