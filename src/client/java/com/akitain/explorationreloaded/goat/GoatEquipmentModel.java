package com.akitain.explorationreloaded.goat;

import com.akitain.explorationreloaded.ExplorationReloaded;
import net.minecraft.client.model.QuadrupedModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.GoatRenderState;

public final class GoatEquipmentModel extends QuadrupedModel<GoatRenderState> {
    public static final ModelLayerLocation SADDLE = new ModelLayerLocation(ExplorationReloaded.id("goat_saddle"), "main");
    public static final ModelLayerLocation ARMOR = new ModelLayerLocation(ExplorationReloaded.id("goat_armor"), "main");

    public GoatEquipmentModel(ModelPart root) { super(root); }

    @Override public void setupAnim(GoatRenderState state) {
        super.setupAnim(state);
        GoatPose.apply(state, head, body, leftFrontLeg, rightFrontLeg, leftHindLeg, rightHindLeg);
    }
}
