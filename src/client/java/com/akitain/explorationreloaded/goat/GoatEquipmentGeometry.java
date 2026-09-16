package com.akitain.explorationreloaded.goat;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

/** Geometry exported by Blockbench 5.1.6. Editable sources live in art/goat_mount. */
public final class GoatEquipmentGeometry {
    private GoatEquipmentGeometry() {}

    public static LayerDefinition saddle() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(1, 1).addBox(-5.0F, -18.0F, -1.0F, 11.0F, 1.0F, 10.0F, new CubeDeformation(0.0F))
		.texOffs(45, 1).addBox(-3.0F, -19.0F, 0.0F, 7.0F, 1.0F, 7.0F, new CubeDeformation(0.0F))
		.texOffs(75, 1).addBox(-4.0F, -20.0F, -1.0F, 9.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(99, 1).addBox(-4.0F, -20.0F, 6.0F, 9.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(1, 14).addBox(5.0F, -17.0F, 0.0F, 1.0F, 11.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(9, 14).addBox(-5.0F, -17.0F, 0.0F, 1.0F, 11.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(17, 14).addBox(-4.0F, -7.0F, 0.0F, 9.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(41, 14).addBox(6.0F, -14.0F, 1.0F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(47, 14).addBox(6.0F, -14.0F, 4.0F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(53, 14).addBox(6.0F, -10.0F, 1.0F, 1.0F, 1.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(65, 14).addBox(-6.0F, -14.0F, 1.0F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(71, 14).addBox(-6.0F, -14.0F, 4.0F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(77, 14).addBox(-6.0F, -10.0F, 1.0F, 1.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition head = partdefinition.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(1.0F, 14.0F, 0.0F));

		PartDefinition left_hind_leg = partdefinition.addOrReplaceChild("left_hind_leg", CubeListBuilder.create(), PartPose.offset(1.0F, 14.0F, 4.0F));

		PartDefinition right_hind_leg = partdefinition.addOrReplaceChild("right_hind_leg", CubeListBuilder.create(), PartPose.offset(-3.0F, 14.0F, 4.0F));

		PartDefinition left_front_leg = partdefinition.addOrReplaceChild("left_front_leg", CubeListBuilder.create(), PartPose.offset(1.0F, 14.0F, -6.0F));

		PartDefinition right_front_leg = partdefinition.addOrReplaceChild("right_front_leg", CubeListBuilder.create(), PartPose.offset(-3.0F, 14.0F, -6.0F));

		return LayerDefinition.create(meshdefinition, 128, 128);
	}

    public static LayerDefinition armor() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(1, 1).addBox(-4.0F, -16.0F, -9.0F, 9.0F, 8.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(23, 1).addBox(6.0F, -18.0F, -7.0F, 1.0F, 8.0F, 8.0F, new CubeDeformation(0.0F))
		.texOffs(43, 1).addBox(-6.0F, -18.0F, -7.0F, 1.0F, 8.0F, 8.0F, new CubeDeformation(0.0F))
		.texOffs(63, 1).addBox(5.0F, -16.0F, 3.0F, 1.0F, 7.0F, 6.0F, new CubeDeformation(0.0F))
		.texOffs(79, 1).addBox(-5.0F, -16.0F, 3.0F, 1.0F, 7.0F, 6.0F, new CubeDeformation(0.0F))
		.texOffs(95, 1).addBox(-5.0F, -19.0F, -7.0F, 11.0F, 1.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(1, 19).addBox(-4.0F, -18.0F, 8.0F, 9.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition head = partdefinition.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(1.0F, 14.0F, 0.0F));

		PartDefinition left_hind_leg = partdefinition.addOrReplaceChild("left_hind_leg", CubeListBuilder.create().texOffs(51, 19).addBox(0.0F, 6.0F, 0.0F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.18F)), PartPose.offset(1.0F, 14.0F, 4.0F));

		PartDefinition right_hind_leg = partdefinition.addOrReplaceChild("right_hind_leg", CubeListBuilder.create().texOffs(65, 19).addBox(0.0F, 6.0F, 0.0F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.18F)), PartPose.offset(-3.0F, 14.0F, 4.0F));

		PartDefinition left_front_leg = partdefinition.addOrReplaceChild("left_front_leg", CubeListBuilder.create().texOffs(23, 19).addBox(0.0F, 6.0F, 0.0F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.18F)), PartPose.offset(1.0F, 14.0F, -6.0F));

		PartDefinition right_front_leg = partdefinition.addOrReplaceChild("right_front_leg", CubeListBuilder.create().texOffs(37, 19).addBox(0.0F, 6.0F, 0.0F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.18F)), PartPose.offset(-3.0F, 14.0F, -6.0F));

		return LayerDefinition.create(meshdefinition, 128, 128);
	}
}
