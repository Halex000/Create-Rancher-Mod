package com.halex.createrancher.content.herder;

import com.halex.createrancher.index.PartialModelRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.transform.TransformStack;

// Lays out the herder's arm parts
public class HerderArmParts {

    private static final PartialModel[][] SEGMENT_PIECES = {PartialModelRegistry.HERDER_WIDE_SEGMENT,
            PartialModelRegistry.HERDER_MEDIUM_SEGMENT, PartialModelRegistry.HERDER_THIN_SEGMENT};
    private static final PartialModel[] JOINTS = {PartialModelRegistry.HERDER_LARGE_JOINT,
            PartialModelRegistry.HERDER_LARGE_JOINT, PartialModelRegistry.HERDER_SMALL_JOINT};

    private static final float PIECE_LENGTH = HerderArmPose.SEGMENT_LENGTH / HerderArmPose.PIECES_PER_SEGMENT;
    private static final float PIECE_STRETCH = PIECE_LENGTH / (8 / 16f);

    @FunctionalInterface
    public interface PartConsumer {
        void accept(int index, PartialModel model, PoseStack pose);
    }

    // Places each part of the arm
    public static void layOut(PoseStack pose, float yaw, float shoulder, float bend, boolean carrying,
                              PartConsumer consumer) {
        int index = 0;
        var msr = TransformStack.of(pose);
        pose.pushPose();

        msr.translate(0, HerderArmPose.TURRET_HEIGHT, 0)
                .rotateYDegrees(yaw);
        consumer.accept(index++, AllPartialModels.ARM_BASE, pose);

        msr.translate(0, HerderArmPose.SHOULDER_HEIGHT, 0);
        consumer.accept(index++, JOINTS[0], pose);
        msr.rotateXDegrees(shoulder);
        for (int segment = 0; segment < HerderArmPose.SEGMENTS; segment++) {
            if (segment > 0) {
                msr.translate(0, 0, -HerderArmPose.SEGMENT_LENGTH);
                consumer.accept(index++, JOINTS[segment], pose);
                msr.rotateXDegrees(-bend);
            }
            for (int piece = 0; piece < HerderArmPose.PIECES_PER_SEGMENT; piece++) {
                pose.pushPose();
                pose.translate(0, 0, -piece * PIECE_LENGTH);
                pose.scale(1, 1, PIECE_STRETCH);
                consumer.accept(index++, SEGMENT_PIECES[segment][piece], pose);
                pose.popPose();
            }
        }

        msr.translate(0, 0, -HerderArmPose.SEGMENT_LENGTH)
                .rotateXDegrees(HerderArmPose.headPitch(shoulder, bend));
        consumer.accept(index++, PartialModelRegistry.HERDER_PLUNGER_HANDLE, pose);

        if (carrying) {
            float rim = -HerderArmPose.PLUNGER_REACH;
            msr.translate(0, 0, rim)
                    .scale(1.12f, 1.12f, .75f)
                    .translate(0, 0, -rim);
        }
        consumer.accept(index, PartialModelRegistry.HERDER_PLUNGER_CUP, pose);

        pose.popPose();
    }
}
