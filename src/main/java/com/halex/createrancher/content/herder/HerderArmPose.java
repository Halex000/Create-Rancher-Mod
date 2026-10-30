package com.halex.createrancher.content.herder;

import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

// Joint angles for the herder's arm
public class HerderArmPose {

    public static final int SEGMENTS = 3;
    public static final float SEGMENT_LENGTH = 3.6f;
    public static final int PIECES_PER_SEGMENT = Math.round(SEGMENT_LENGTH * 2);
    public static final float PLUNGER_REACH = 12 / 16f;
    public static final float TURRET_HEIGHT = 10 / 16f;
    public static final float SHOULDER_HEIGHT = 2 / 16f;

    public final float yaw;
    public final float shoulderAngle;
    public final float bendAngle;

    public HerderArmPose(float yaw, float shoulderAngle, float bendAngle) {
        this.yaw = yaw;
        this.shoulderAngle = shoulderAngle;
        this.bendAngle = bendAngle;
    }

    public static Vec3 shoulderPos(BlockPos armPos) {
        return VecHelper.getCenterOf(armPos)
                .add(0, TURRET_HEIGHT + SHOULDER_HEIGHT, 0);
    }

    // The pose that puts the plunger on the target
    public static HerderArmPose reaching(BlockPos armPos, Vec3 plungerTarget, float fallbackYaw) {
        Vec3 diff = plungerTarget.add(0, PLUNGER_REACH, 0)
                .subtract(shoulderPos(armPos));
        double horizontal = Math.sqrt(diff.x * diff.x + diff.z * diff.z);

        float yaw = horizontal < 1e-3 ? fallbackYaw : AngleHelper.deg(Mth.atan2(-diff.x, -diff.z));
        double distance = Mth.clamp(Math.sqrt(horizontal * horizontal + diff.y * diff.y), 1e-3,
                SEGMENTS * SEGMENT_LENGTH);

        float bend = AngleHelper.deg(Math.acos(Mth.clamp((distance / SEGMENT_LENGTH - 1) / 2, -.5, 1)));
        float shoulder = AngleHelper.deg(Mth.atan2(diff.y, horizontal)) + bend;
        return new HerderArmPose(yaw, shoulder, bend);
    }

    public static HerderArmPose lerp(float t, HerderArmPose from, HerderArmPose to) {
        return new HerderArmPose(AngleHelper.angleLerp(t, from.yaw, to.yaw),
                Mth.lerp(t, from.shoulderAngle, to.shoulderAngle), Mth.lerp(t, from.bendAngle, to.bendAngle));
    }

    // Head pitch that keeps the plunger pointing down
    public static float headPitch(float shoulderAngle, float bendAngle) {
        return -90 - (shoulderAngle - (SEGMENTS - 1) * bendAngle);
    }

    // Where the plunger rim is
    public Vec3 plungerTip(BlockPos armPos) {
        Matrix4f transform = new Matrix4f().translation(.5f, .5f, .5f);
        transform.translate(0, TURRET_HEIGHT, 0)
                .rotateY(rad(yaw));
        transform.translate(0, SHOULDER_HEIGHT, 0)
                .rotateX(rad(shoulderAngle));
        for (int i = 1; i < SEGMENTS; i++)
            transform.translate(0, 0, -SEGMENT_LENGTH)
                    .rotateX(rad(-bendAngle));
        transform.translate(0, 0, -SEGMENT_LENGTH)
                .rotateX(rad(headPitch(shoulderAngle, bendAngle)));
        Vector3f tip = transform.transformPosition(new Vector3f(0, 0, -PLUNGER_REACH));
        return new Vec3(armPos.getX() + (double) tip.x, armPos.getY() + (double) tip.y, armPos.getZ() + (double) tip.z);
    }

    private static float rad(float degrees) {
        return degrees * Mth.DEG_TO_RAD;
    }
}
