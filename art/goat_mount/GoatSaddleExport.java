// Made with Blockbench 5.1.6
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports


public class GoatSaddleExport<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("modid", "goatsaddleexport"), "main");
	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart left_hind_leg;
	private final ModelPart right_hind_leg;
	private final ModelPart left_front_leg;
	private final ModelPart right_front_leg;

	public GoatSaddleExport(ModelPart root) {
		this.body = root.getChild("body");
		this.head = root.getChild("head");
		this.left_hind_leg = root.getChild("left_hind_leg");
		this.right_hind_leg = root.getChild("right_hind_leg");
		this.left_front_leg = root.getChild("left_front_leg");
		this.right_front_leg = root.getChild("right_front_leg");
	}

	public static LayerDefinition createBodyLayer() {
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

	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		body.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		head.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		left_hind_leg.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		right_hind_leg.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		left_front_leg.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		right_front_leg.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}