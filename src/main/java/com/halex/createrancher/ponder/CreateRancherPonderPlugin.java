package com.halex.createrancher.ponder;

import com.halex.createrancher.CreateRancher;
import com.halex.createrancher.index.BlockRegistry;
import com.halex.createrancher.index.ItemRegistry;
import com.simibubi.create.infrastructure.ponder.AllCreatePonderTags;
import com.tterrag.registrate.util.entry.ItemProviderEntry;
import com.tterrag.registrate.util.entry.RegistryEntry;

import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.SharedTextRegistrationHelper;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

public class CreateRancherPonderPlugin implements PonderPlugin {

    public static final ResourceLocation RANCHING = CreateRancher.asResource("ranching");

    @Override
    public String getModId() {
        return CreateRancher.MOD_ID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        PonderSceneRegistrationHelper<ItemProviderEntry<?, ?>> entries = helper.withKeyFunction(RegistryEntry::getId);
        entries.forComponents(BlockRegistry.MECHANICAL_HERDER)
                .addStoryBoard("mechanical_herder/pen", HerderScenes::movingAnimals, RANCHING)
                .addStoryBoard("mechanical_herder/pen", HerderScenes::filtering, RANCHING);
        entries.forComponents(BlockRegistry.MECHANICAL_FEEDER)
                .addStoryBoard("mechanical_feeder/pen", FeederScenes::breeding, RANCHING);
        entries.forComponents(BlockRegistry.MECHANICAL_SHEARER)
                .addStoryBoard("mechanical_shearer/pen", ShearerScenes::shearing, RANCHING);
        entries.forComponents(BlockRegistry.MECHANICAL_FISHER)
                .addStoryBoard("mechanical_fisher/pond", FisherScenes::fishing, RANCHING);
        entries.forComponents(ItemRegistry.ITEM_GRATE)
                .addStoryBoard("item_grate/belt", GrateScenes::belts, AllCreatePonderTags.LOGISTICS)
                .addStoryBoard("item_grate/grills", GrateScenes::grills);
        entries.forComponents(BlockRegistry.AUGER)
                .addStoryBoard("auger/auger", AugerScenes::auger, AllCreatePonderTags.LOGISTICS);
        entries.forComponents(BlockRegistry.SMART_DEPOT)
                .addStoryBoard("smart_depot/depot", SmartDepotScenes::usingTheSmartDepot, AllCreatePonderTags.LOGISTICS)
                .addStoryBoard("smart_depot/processing", SmartDepotScenes::processing);
        entries.forComponents(BlockRegistry.LIVESTOCK_DRAIN)
                .addStoryBoard("livestock_drain/milking", LivestockDrainScenes::milking, RANCHING)
                .addStoryBoard("livestock_drain/storing", LivestockDrainScenes::storing, AllCreatePonderTags.FLUIDS);
        entries.forComponents(BlockRegistry.LIQUID_BLAZE_BURNER)
                .addStoryBoard("liquid_blaze_burner/burning", LiquidBlazeBurnerScenes::burning,
                        AllCreatePonderTags.FLUIDS);
    }

    // Text for Create's spout scene
    @Override
    public void registerSharedText(SharedTextRegistrationHelper helper) {
        helper.registerSharedText("spout_buckets", "Right-clicking a Spout with a bucket pours it in, or with an empty "
                + "bucket fills it from the Spout");
        helper.registerSharedText("spout_pipes", "Pipes can also be used to supply it with fluids");
    }

    @Override
    public void registerTags(PonderTagRegistrationHelper<ResourceLocation> helper) {
        helper.registerTag(RANCHING)
                .addToIndex()
                .item(BlockRegistry.MECHANICAL_HERDER.get(), true, false)
                .title("Ranching")
                .description("Machines that look after your animals")
                .register();

        PonderTagRegistrationHelper<ItemProviderEntry<?, ?>> entries = helper.withKeyFunction(RegistryEntry::getId);
        entries.addToTag(RANCHING)
                .add(BlockRegistry.MECHANICAL_HERDER)
                .add(BlockRegistry.MECHANICAL_FEEDER)
                .add(BlockRegistry.MECHANICAL_FISHER)
                .add(BlockRegistry.MECHANICAL_SHEARER)
                .add(BlockRegistry.LIVESTOCK_DRAIN);
        entries.addToTag(AllCreatePonderTags.FLUIDS)
                .add(BlockRegistry.LIVESTOCK_DRAIN)
                .add(BlockRegistry.LIQUID_BLAZE_BURNER);
        entries.addToTag(AllCreatePonderTags.LOGISTICS)
                .add(ItemRegistry.ITEM_GRATE)
                .add(BlockRegistry.AUGER)
                .add(BlockRegistry.SMART_DEPOT);
        entries.addToTag(AllCreatePonderTags.KINETIC_APPLIANCES)
                .add(BlockRegistry.MECHANICAL_HERDER)
                .add(BlockRegistry.MECHANICAL_FEEDER)
                .add(BlockRegistry.MECHANICAL_SHEARER)
                .add(BlockRegistry.MECHANICAL_FISHER);
    }
}
