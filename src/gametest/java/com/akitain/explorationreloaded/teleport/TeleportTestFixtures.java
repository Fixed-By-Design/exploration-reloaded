package com.akitain.explorationreloaded.teleport;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class TeleportTestFixtures implements ModInitializer {
    @Override
    public void onInitialize() {
        // Exercise the optional registry-ID integration without shipping an Additional Additions dependency.
        Identifier id = Identifier.parse("additionaladditions:rose_gold_block");
        if (!FabricLoader.getInstance().isModLoaded("additionaladditions") && !BuiltInRegistries.BLOCK.containsKey(id)) {
            Registry.register(BuiltInRegistries.BLOCK, id, new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.GOLD_BLOCK)
                    .setId(ResourceKey.create(Registries.BLOCK, id))));
        }
    }
}
