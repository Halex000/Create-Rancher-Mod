package com.halex.createrancher;

import net.neoforged.neoforge.common.ModConfigSpec;

public class CommonConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.DoubleValue MECHANICAL_HERDER_STRESS_IMPACT = BUILDER
            .comment("Stress impact of the Mechanical Herder, in SU per RPM.")
            .defineInRange("mechanicalHerderStressImpact", 4.0, 0.0, 1024.0);

    public static final ModConfigSpec.DoubleValue MECHANICAL_FEEDER_STRESS_IMPACT = BUILDER
            .comment("Stress impact of the Mechanical Feeder, in SU per RPM.")
            .defineInRange("mechanicalFeederStressImpact", 2.0, 0.0, 1024.0);

    public static final ModConfigSpec.DoubleValue MECHANICAL_SHEARER_STRESS_IMPACT = BUILDER
            .comment("Stress impact of the Mechanical Shearer, in SU per RPM.")
            .defineInRange("mechanicalShearerStressImpact", 2.0, 0.0, 1024.0);

    public static final ModConfigSpec.DoubleValue MECHANICAL_FISHER_STRESS = BUILDER
            .comment("Stress of the Mechanical Fisher, in SU. It's the same at any speed.")
            .defineInRange("mechanicalFisherStress", 256.0, 0.0, 65536.0);

    public static final ModConfigSpec.IntValue ALFALFA_DIGESTION_TICKS = BUILDER
            .comment("How long an animal takes to digest Alfalfa Pellets before it drops Dung, in ticks.")
            .defineInRange("alfalfaDigestionTicks", 3600, 1, 1728000);

    public static final ModConfigSpec.DoubleValue GRILL_SMOKING_TIME_MULTIPLIER = BUILDER
            .comment("How long a Campfire Grill takes to smoke an item, as a multiple of the smoker recipe's time.")
            .defineInRange("grillSmokingTimeMultiplier", 2.0, 0.01, 100.0);

    public static final ModConfigSpec.IntValue GRILL_HAUNTING_TICKS = BUILDER
            .comment("How long a Soul Campfire Grill takes to haunt an item, in ticks.")
            .defineInRange("grillHauntingTicks", 200, 1, 72000);

    public static final ModConfigSpec.IntValue GRILL_BLASTING_TICKS = BUILDER
            .comment("How long a Cauldron Grill of lava takes to blast an item, in ticks.")
            .defineInRange("grillBlastingTicks", 200, 1, 72000);

    public static final ModConfigSpec.IntValue GRILL_WASHING_TICKS = BUILDER
            .comment("How long a heated Cauldron Grill of water takes to wash an item, in ticks.")
            .defineInRange("grillWashingTicks", 200, 1, 72000);

    public static final ModConfigSpec SPEC = BUILDER.build();
}
