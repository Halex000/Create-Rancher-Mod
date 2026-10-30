package com.halex.createrancher.index;

import com.halex.createrancher.CreateRancher;
import com.halex.createrancher.content.depot.SmartDepotArmPointType;
import com.halex.createrancher.content.feeder.FeedingPoint;
import com.halex.createrancher.content.grill.GrillArmPoint;
import com.halex.createrancher.content.shearer.ShearingPoint;
import com.simibubi.create.api.registry.CreateRegistries;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPointType;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

// Mechanical arm target types
public class ArmPointTypeRegistry {

    private static final DeferredRegister<ArmInteractionPointType> POINT_TYPES =
            DeferredRegister.create(CreateRegistries.ARM_INTERACTION_POINT_TYPE, CreateRancher.MOD_ID);

    public static final DeferredHolder<ArmInteractionPointType, FeedingPoint.Type> ANIMAL_FEEDING =
            POINT_TYPES.register("animal_feeding", FeedingPoint.Type::new);

    public static final DeferredHolder<ArmInteractionPointType, ShearingPoint.Type> SHEARING =
            POINT_TYPES.register("animal_shearing", ShearingPoint.Type::new);

    public static final DeferredHolder<ArmInteractionPointType, GrillArmPoint.Type> GRILL =
            POINT_TYPES.register("campfire_grill", GrillArmPoint.Type::new);

    public static final DeferredHolder<ArmInteractionPointType, SmartDepotArmPointType> SMART_DEPOT =
            POINT_TYPES.register("smart_depot", SmartDepotArmPointType::new);

    public static void register(IEventBus modBus) {
        POINT_TYPES.register(modBus);
    }
}
