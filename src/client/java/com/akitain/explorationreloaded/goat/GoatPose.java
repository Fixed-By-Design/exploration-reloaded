package com.akitain.explorationreloaded.goat;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.GoatRenderState;
import net.minecraft.util.Mth;

public final class GoatPose {
    private GoatPose() {}

    public static void apply(GoatRenderState state, ModelPart head, ModelPart body,
                             ModelPart leftFront, ModelPart rightFront, ModelPart leftHind, ModelPart rightHind) {
        if (state.isBaby) return;
        GoatVisualState.Data mount = ((GoatVisualState) state).exploration$goat();
        float crouch = mount.landing * 1.5F;
        body.y += crouch;
        head.y += crouch;
        leftFront.xRot += mount.landing * 0.2F;
        rightFront.xRot += mount.landing * 0.2F;
        leftHind.xRot -= mount.landing * 0.25F;
        rightHind.xRot -= mount.landing * 0.25F;
        if (mount.rear > 0) {
            float angle = -(float)Math.PI / 4 * mount.rear;
            rearPart(body, angle, mount.rear);
            rearPart(head, angle, mount.rear);
            rearPart(leftFront, angle, mount.rear);
            rearPart(rightFront, angle, mount.rear);
            // AbstractEquineModel's alternating forelegs and braced hind legs.
            float bob = Mth.cos(state.ageInTicks * 0.6F + (float)Math.PI);
            leftFront.xRot = Mth.lerp(mount.rear, leftFront.xRot, -(float)Math.PI / 3 + bob);
            rightFront.xRot = Mth.lerp(mount.rear, rightFront.xRot, -(float)Math.PI / 3 - bob);
            leftHind.xRot = Mth.lerp(mount.rear, leftHind.xRot, (float)Math.PI / 12);
            rightHind.xRot = Mth.lerp(mount.rear, rightHind.xRot, (float)Math.PI / 12);
            head.xRot += ((float)Math.PI / 4 - (float)Math.PI / 12) * mount.rear;
        }
        if (mount.airborne && mount.rear < 1) {
            float flex = Mth.sin(state.ageInTicks * 0.25F) * 0.06F;
            float flying = 1 - mount.rear;
            leftFront.xRot = Mth.lerp(flying, leftFront.xRot, -0.85F + flex);
            rightFront.xRot = Mth.lerp(flying, rightFront.xRot, -0.85F - flex);
            leftHind.xRot = Mth.lerp(flying, leftHind.xRot, 0.6F - flex);
            rightHind.xRot = Mth.lerp(flying, rightHind.xRot, 0.6F + flex);
        }
    }
    private static void rearPart(ModelPart part, float angle, float rear) {
        float y = part.y - 14;
        float z = part.z - 5.5F;
        part.y = 14 + Mth.cos(angle) * y - Mth.sin(angle) * z - 2 * rear;
        part.z = 5.5F + Mth.sin(angle) * y + Mth.cos(angle) * z;
        part.xRot += angle;
    }
}
