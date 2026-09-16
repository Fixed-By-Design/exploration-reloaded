package com.akitain.explorationreloaded.mixin.client.goat;

import com.akitain.explorationreloaded.goat.GoatVisualState;
import net.minecraft.client.renderer.entity.state.GoatRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(GoatRenderState.class)
public class GoatRenderStateMixin implements GoatVisualState {
    @Unique private final GoatVisualState.Data exploration$goat = new GoatVisualState.Data();
    @Override public GoatVisualState.Data exploration$goat() { return exploration$goat; }
}
