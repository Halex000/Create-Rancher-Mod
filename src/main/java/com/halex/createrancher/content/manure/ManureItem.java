package com.halex.createrancher.content.manure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.OptionalDispenseItemBehavior;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;

// Fertiliser that works like Bone Meal
public class ManureItem extends BoneMealItem {

    public ManureItem(Properties properties) {
        super(properties);
    }

    // Lets dispensers use manure like bone meal
    public static void registerDispenserBehaviour(ManureItem item) {
        DispenserBlock.registerBehavior(item, new OptionalDispenseItemBehavior() {
            @Override
            protected ItemStack execute(BlockSource source, ItemStack stack) {
                setSuccess(true);
                Level level = source.level();
                BlockPos target = source.pos()
                        .relative(source.state()
                                .getValue(DispenserBlock.FACING));
                if (!growCrop(stack, level, target) && !growWaterPlant(stack, level, target, null))
                    setSuccess(false);
                else if (!level.isClientSide)
                    level.levelEvent(1505, target, 15);
                return stack;
            }
        });
    }
}
