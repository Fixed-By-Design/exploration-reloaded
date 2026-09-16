package com.akitain.explorationreloaded;

import com.akitain.explorationreloaded.flight.CampfireSafety;
import com.akitain.explorationreloaded.teleport.TeleportRituals;
import com.akitain.explorationreloaded.flight.ElytraEnchantability;
import com.akitain.explorationreloaded.flight.FlightLoot;
import com.akitain.explorationreloaded.flight.FlightNetworking;
import com.akitain.explorationreloaded.flight.FlightRules;
import com.akitain.explorationreloaded.flight.FlightState;
import com.akitain.explorationreloaded.network.MapBookNetworking;
import com.akitain.explorationreloaded.registry.ExplorationRegistries;
import com.akitain.explorationreloaded.registry.ExplorationComponents;
import com.akitain.explorationreloaded.registry.ExplorationItems;
import com.akitain.explorationreloaded.registry.item.MapBookState;
import com.akitain.explorationreloaded.registry.item.MapBookStateManager;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExplorationReloaded implements ModInitializer {
    public static final String MOD_ID = "exploration-reloaded";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ExplorationComponents.register();
        ExplorationItems.register();
        TeleportRituals.register();
        ExplorationRegistries.register();
        FlightRules.register();
        FlightState.register();
        CampfireSafety.register();
        FlightNetworking.register();
        com.akitain.explorationreloaded.goat.GoatNetworking.register();
        ElytraEnchantability.register();
        FlightLoot.register();
        MapBookNetworking.register();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(Commands.literal("mapBookMarker")
                        .then(Commands.argument("id", IntegerArgumentType.integer())
                                .then(Commands.argument("x", StringArgumentType.string())
                                        .then(Commands.argument("z", StringArgumentType.string())
                                                .then(Commands.argument("dim", StringArgumentType.string())
                                                        .executes(ExplorationReloaded::executeMapBookMarker)))))));
        LOGGER.info("Exploration Reloaded loaded");
    }

    private static int executeMapBookMarker(CommandContext<CommandSourceStack> context) {
        int id = IntegerArgumentType.getInteger(context, "id");
        double x = Double.parseDouble(StringArgumentType.getString(context, "x"));
        double z = Double.parseDouble(StringArgumentType.getString(context, "z"));
        String dimension = StringArgumentType.getString(context, "dim");
        MapBookState mapBookState = MapBookStateManager.INSTANCE.getMapBookState(context.getSource().getServer(), id);
        if (mapBookState != null) {
            mapBookState.setMarker(x, z, dimension);
        }
        return 1;
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
