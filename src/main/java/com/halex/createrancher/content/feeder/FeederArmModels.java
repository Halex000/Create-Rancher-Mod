package com.halex.createrancher.content.feeder;

import java.util.Map;

import com.halex.createrancher.index.PartialModelRegistry;
import com.simibubi.create.AllPartialModels;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;

// The Mechanical Feeder's own versions of the arm models
public class FeederArmModels {

    private static final Map<PartialModel, PartialModel> FEEDER_PARTS = Map.of(
            AllPartialModels.ARM_COG, PartialModelRegistry.FEEDER_COG,
            AllPartialModels.ARM_BASE, PartialModelRegistry.FEEDER_BASE,
            AllPartialModels.ARM_LOWER_BODY, PartialModelRegistry.FEEDER_LOWER_BODY,
            AllPartialModels.ARM_UPPER_BODY, PartialModelRegistry.FEEDER_UPPER_BODY,
            AllPartialModels.ARM_CLAW_BASE, PartialModelRegistry.FEEDER_CLAW_BASE,
            AllPartialModels.ARM_CLAW_BASE_GOGGLES, PartialModelRegistry.FEEDER_CLAW_BASE_GOGGLES,
            AllPartialModels.ARM_CLAW_GRIP_UPPER, PartialModelRegistry.FEEDER_CLAW_GRIP_UPPER,
            AllPartialModels.ARM_CLAW_GRIP_LOWER, PartialModelRegistry.FEEDER_CLAW_GRIP_LOWER);

    // The feeder's version of an arm part
    public static PartialModel forFeeder(PartialModel armPart) {
        return FEEDER_PARTS.getOrDefault(armPart, armPart);
    }
}
