package com.akitain.explorationreloaded.registry;

import com.akitain.explorationreloaded.ExplorationReloaded;
import com.akitain.explorationreloaded.registry.item.MapBookItem;
import com.akitain.explorationreloaded.teleport.WitherCompassItem;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.FireworkRocketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.item.equipment.ArmorMaterials;
import java.util.List;
import java.util.function.Function;

public final class ExplorationItems {
    public static final Item WITHER_COMPASS = register("wither_compass", WitherCompassItem::new,
            new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.RARE));
    public static final Item MAP_BOOK = register("map_book", MapBookItem::new, new Item.Properties().stacksTo(16));
    public static final Item CHAINMAIL_HORSE_ARMOR = register("chainmail_horse_armor", new Item.Properties().horseArmor(ArmorMaterials.CHAINMAIL));
    public static final Item NAUTILUS_ARMOR = register("nautilus_armor", new Item.Properties().nautilusArmor(ArmorMaterials.ARMADILLO_SCUTE));

    private ExplorationItems() {
    }

    public static void register() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> entries.insertAfter(Items.COMPASS, WITHER_COMPASS));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> entries.insertAfter(Items.MAP, MAP_BOOK));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(entries -> entries.insertAfter(Items.LEATHER_HORSE_ARMOR, CHAINMAIL_HORSE_ARMOR));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(entries -> entries.insertAfter(Items.TURTLE_HELMET, NAUTILUS_ARMOR));
    }

    private static Item register(String name, Item.Properties settings) {
        return register(keyOf(name), Item::new, settings);
    }

    private static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties settings) {
        return register(keyOf(name), factory, settings);
    }

    private static Item register(ResourceKey<Item> key, Function<Item.Properties, Item> factory, Item.Properties settings) {
        Item item = factory.apply(settings.setId(key));
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }

    private static ResourceKey<Item> keyOf(String name) {
        return ResourceKey.create(Registries.ITEM, ExplorationReloaded.id(name));
    }
}
