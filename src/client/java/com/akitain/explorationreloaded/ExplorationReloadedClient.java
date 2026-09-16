package com.akitain.explorationreloaded;

import com.akitain.explorationreloaded.flight.FlightClient;
import com.akitain.explorationreloaded.flight.FlightPhysics;
import com.akitain.explorationreloaded.map_book.MapBookFilledProperty;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperties;

public class ExplorationReloadedClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        com.akitain.explorationreloaded.goat.GoatClient.register();
        ClientMapBookNetworking.register();
        FlightClient.register();
        FlightPhysics.register();
        ConditionalItemModelProperties.ID_MAPPER.put(ExplorationReloaded.id("map_book/filled"), MapBookFilledProperty.CODEC);
    }
}
