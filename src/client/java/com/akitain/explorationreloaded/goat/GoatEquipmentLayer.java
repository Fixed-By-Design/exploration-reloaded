package com.akitain.explorationreloaded.goat;

import com.akitain.explorationreloaded.ExplorationReloaded;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map;
import net.minecraft.client.model.animal.goat.GoatModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.GoatRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.component.DyedItemColor;

public final class GoatEquipmentLayer extends RenderLayer<GoatRenderState, GoatModel> {
    private static final Identifier SADDLE = texture("saddle");
    private static final Identifier LEATHER_OVERLAY = texture("leather_overlay");
    private static final Map<String, Identifier> ARMOR = Map.of(
            "leather_horse_armor", texture("leather"), "copper_horse_armor", texture("copper"),
            "iron_horse_armor", texture("iron"), "golden_horse_armor", texture("gold"),
            "diamond_horse_armor", texture("diamond"), "netherite_horse_armor", texture("netherite"),
            "chainmail_horse_armor", texture("chainmail"));
    private final GoatEquipmentModel saddle;
    private final GoatEquipmentModel armor;

    public GoatEquipmentLayer(RenderLayerParent<GoatRenderState, GoatModel> parent, EntityRendererProvider.Context context) {
        super(parent);
        saddle = new GoatEquipmentModel(context.bakeLayer(GoatEquipmentModel.SADDLE));
        armor = new GoatEquipmentModel(context.bakeLayer(GoatEquipmentModel.ARMOR));
    }

    private static Identifier texture(String name) { return ExplorationReloaded.id("textures/entity/goat/" + name + ".png"); }

    @Override public void submit(PoseStack poses, SubmitNodeCollector collector, int light, GoatRenderState state, float yaw, float pitch) {
        if (state.isBaby || state.isInvisible) return;
        GoatVisualState.Data data = ((GoatVisualState)state).exploration$goat();
        if (!data.armor.isEmpty()) {
            String material = BuiltInRegistries.ITEM.getKey(data.armor.getItem()).getPath();
            Identifier texture = ARMOR.getOrDefault(material, ARMOR.get("iron_horse_armor"));
            boolean leather = material.equals("leather_horse_armor");
            DyedItemColor dye = data.armor.get(DataComponents.DYED_COLOR);
            // Use the horse's undyed leather colour and leave the straps untinted.
            int color = leather ? (dye == null ? -6265536 : 0xFF000000 | dye.rgb()) : -1;
            renderColoredCutoutModel(armor, texture, poses, collector, light, state, color, 1);
            if (leather) renderColoredCutoutModel(armor, LEATHER_OVERLAY, poses, collector, light, state, -1, 2);
        }
        if (data.saddled) renderColoredCutoutModel(saddle, SADDLE, poses, collector, light, state, -1, 3);
    }
}
