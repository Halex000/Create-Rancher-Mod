package com.halex.createrancher.content.herder;

import javax.annotation.Nullable;

import com.halex.createrancher.CreateRancher;
import com.halex.createrancher.content.fisher.MechanicalFisherClient;
import com.halex.createrancher.content.herder.MechanicalHerderBlockEntity.DropOffStatus;
import com.halex.createrancher.index.BlockRegistry;
import com.halex.createrancher.index.DataComponentRegistry;

import net.createmod.catnip.gui.ScreenOpener;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = CreateRancher.MOD_ID, value = Dist.CLIENT)
public class MechanicalHerderClient {

    public static final int AREA_COLOR = 0x4F8A8B;
    public static final int DROP_OFF_COLOR = MechanicalFisherClient.SPOT_COLOR;
    public static final int INVALID_COLOR = 0xFF5D6C;

    private static final Object SELECTION_SLOT = new Object();
    private static final Object PLACEMENT_AREA_SLOT = new Object();
    private static final Object PLACEMENT_CELL_SLOT = new Object();

    public static void openScreen(MechanicalHerderBlockEntity be) {
        ScreenOpener.open(new MechanicalHerderScreen(be));
    }

    // Shows a placed herder's search area and drop-off
    public static void showAreaPreview(MechanicalHerderBlockEntity be, int areaSize, int ticks) {
        BlockPos pos = be.getBlockPos();
        Outliner.getInstance()
                .showAABB(new PreviewSlot("area", pos), MechanicalHerderBlockEntity.getSearchArea(pos, be.getFacing(),
                        areaSize), ticks)
                .colored(AREA_COLOR)
                .lineWidth(1 / 16f);
        BlockPos dropOff = be.getDropOff();
        if (dropOff == null)
            return;
        boolean valid = MechanicalHerderBlockEntity.checkDropOff(be.getLevel(), pos, be.getFacing(), areaSize, dropOff)
                == DropOffStatus.VALID;
        Outliner.getInstance()
                .showAABB(new PreviewSlot("drop_off", pos), new AABB(dropOff), ticks)
                .colored(valid ? DROP_OFF_COLOR : INVALID_COLOR)
                .lineWidth(1 / 16f);
    }

    // Shows the drop-off and search area while holding a herder
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null)
            return;

        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!BlockRegistry.MECHANICAL_HERDER.isIn(stack)) {
            hidePlacementPreview();
            return;
        }

        BlockPos dropOff = stack.get(DataComponentRegistry.DROP_OFF);
        BlockPos placementPos = getPlacementPos(mc);
        Direction facing = player.getDirection();

        if (dropOff != null) {
            boolean valid = placementPos == null || MechanicalHerderBlockEntity.checkDropOff(mc.level, placementPos,
                    facing, MechanicalHerderBlockEntity.DEFAULT_AREA_SIZE, dropOff) == DropOffStatus.VALID;
            Outliner.getInstance()
                    .showAABB(SELECTION_SLOT, new AABB(dropOff))
                    .colored(valid ? DROP_OFF_COLOR : INVALID_COLOR)
                    .lineWidth(1 / 16f);
        }

        if (placementPos == null || player.isShiftKeyDown()) {
            hidePlacementPreview();
            return;
        }
        Outliner.getInstance()
                .showAABB(PLACEMENT_CELL_SLOT, new AABB(placementPos))
                .colored(AREA_COLOR)
                .lineWidth(1 / 16f);
        Outliner.getInstance()
                .showAABB(PLACEMENT_AREA_SLOT, MechanicalHerderBlockEntity.getSearchArea(placementPos, facing,
                        MechanicalHerderBlockEntity.DEFAULT_AREA_SIZE))
                .colored(AREA_COLOR)
                .lineWidth(1 / 32f);
    }

    private static void hidePlacementPreview() {
        Outliner.getInstance()
                .remove(PLACEMENT_AREA_SLOT);
        Outliner.getInstance()
                .remove(PLACEMENT_CELL_SLOT);
    }

    // Where the herder would be placed
    @Nullable
    private static BlockPos getPlacementPos(Minecraft mc) {
        HitResult hit = mc.hitResult;
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK)
            return null;
        BlockPos targeted = blockHit.getBlockPos();
        if (mc.level.getBlockState(targeted)
                .canBeReplaced())
            return targeted;
        return targeted.relative(blockHit.getDirection());
    }

    private record PreviewSlot(String kind, BlockPos pos) {
    }
}
