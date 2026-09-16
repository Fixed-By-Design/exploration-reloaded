package com.akitain.explorationreloaded.goat;

import net.minecraft.world.item.ItemStack;

public interface GoatVisualState {
    Data exploration$goat();

    final class Data {
        public boolean saddled;
        public boolean airborne;
        public float rear;
        public float landing;
        public ItemStack armor = ItemStack.EMPTY;
    }
}
