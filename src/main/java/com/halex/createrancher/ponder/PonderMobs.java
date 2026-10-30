package com.halex.createrancher.ponder;

import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

// A mob in a Ponder scene
public class PonderMobs {

    private final ElementLink<EntityElement> link;
    private final Entity[] mob;

    private PonderMobs(ElementLink<EntityElement> link, Entity[] mob) {
        this.link = link;
        this.mob = mob;
    }

    public static PonderMobs spawn(SceneBuilder scene, EntityType<? extends Mob> type, Vec3 feet, float yaw,
                                   boolean baby) {
        Entity[] holder = new Entity[1];
        ElementLink<EntityElement> link = scene.world()
                .createEntity(level -> {
                    Mob mob = type.create(level);
                    mob.setBaby(baby);
                    mob.moveTo(feet.x, feet.y, feet.z, yaw, 0);
                    mob.setYHeadRot(yaw);
                    mob.yBodyRot = yaw;
                    mob.yBodyRotO = yaw;
                    mob.yHeadRotO = yaw;
                    holder[0] = mob;
                    return mob;
                });
        return new PonderMobs(link, holder);
    }

    public Entity get() {
        return mob[0];
    }

    public ElementLink<EntityElement> link() {
        return link;
    }

    public Vec3 top() {
        Entity entity = get();
        return entity.position()
                .add(0, entity.getBbHeight(), 0);
    }

    // Shows hearts
    public void showHearts(SceneBuilder scene) {
        scene.world()
                .modifyEntity(link, entity -> entity.handleEntityEvent((byte) 18));
    }

    // Shows eating particles
    public void showEating(SceneBuilder scene, ItemStack food) {
        scene.world()
                .modifyEntity(link, entity -> {
                    RandomSource random = entity.getRandom();
                    for (int i = 0; i < 8; i++)
                        entity.level()
                                .addParticle(new ItemParticleOption(ParticleTypes.ITEM, food),
                                        entity.getX() + random.nextGaussian() * .2, entity.getEyeY(),
                                        entity.getZ() + random.nextGaussian() * .2, random.nextGaussian() * .05,
                                        random.nextFloat() * .1, random.nextGaussian() * .05);
                });
    }

    // Walks the mob in a straight line
    public void walk(SceneBuilder scene, Vec3 offset, int ticks) {
        boolean turn = offset.horizontalDistanceSqr() > 1e-6;
        float yaw = (float) Math.toDegrees(Math.atan2(-offset.x, offset.z));
        Vec3 step = offset.scale(1.0 / ticks);
        for (int i = 0; i < ticks; i++) {
            scene.world()
                    .modifyEntity(link, entity -> {
                        entity.setOldPosAndRot();
                        entity.setPos(entity.position()
                                .add(step));
                        if (!turn)
                            return;
                        entity.setYRot(yaw);
                        entity.setYHeadRot(yaw);
                        if (entity instanceof Mob mob)
                            mob.yBodyRot = yaw;
                    });
            scene.idle(1);
        }
        scene.world()
                .modifyEntity(link, Entity::setOldPosAndRot);
    }
}
