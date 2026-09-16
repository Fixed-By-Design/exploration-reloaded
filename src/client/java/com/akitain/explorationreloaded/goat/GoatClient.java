package com.akitain.explorationreloaded.goat;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;

public final class GoatClient {
    private static GoatMount previous;
    private static boolean ramHeld;
    private static final KeyMapping RAM = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.exploration-reloaded.goat_ram", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, KeyMapping.Category.MOVEMENT));
    private GoatClient() {}

    public static void register() {
        ModelLayerRegistry.registerModelLayer(GoatEquipmentModel.SADDLE, GoatEquipmentGeometry::saddle);
        ModelLayerRegistry.registerModelLayer(GoatEquipmentModel.ARMOR, GoatEquipmentGeometry::armor);
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            GoatMount mount = client.player != null && client.player.getControlledVehicle() instanceof GoatMount goat ? goat : null;
            if (previous != null && previous != mount) previous.exploration$setLocalCharging(false);
            if (mount != null) mount.exploration$setLocalCharging(client.screen == null && client.options.keyJump.isDown());
            boolean active = mount != null && client.screen == null && ClientPlayNetworking.canSend(GoatNetworking.RamPayload.ID);
            if (previous != mount || !active) {
                if (ramHeld && ClientPlayNetworking.canSend(GoatNetworking.RamPayload.ID)) {
                    ClientPlayNetworking.send(new GoatNetworking.RamPayload(2));
                }
                ramHeld = false;
            } else if (RAM.isDown() && !ramHeld) {
                ClientPlayNetworking.send(new GoatNetworking.RamPayload(0));
                ramHeld = true;
            } else if (!RAM.isDown() && ramHeld) {
                ClientPlayNetworking.send(new GoatNetworking.RamPayload(1));
                ramHeld = false;
            }
            while (RAM.consumeClick()) { }
            previous = mount;
        });
    }
}
