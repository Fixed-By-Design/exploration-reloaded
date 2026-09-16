package com.akitain.explorationreloaded.mixin.client.goat;

import com.akitain.explorationreloaded.goat.GoatEquipmentLayer;
import com.akitain.explorationreloaded.goat.GoatMount;
import com.akitain.explorationreloaded.goat.GoatVisualState;
import net.minecraft.client.model.animal.goat.GoatModel;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.GoatRenderer;
import net.minecraft.client.renderer.entity.state.GoatRenderState;
import net.minecraft.world.entity.animal.goat.Goat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GoatRenderer.class)
public abstract class GoatRendererMixin extends AgeableMobRenderer<Goat, GoatRenderState, GoatModel> {
    protected GoatRendererMixin(EntityRendererProvider.Context context, GoatModel adult, GoatModel baby, float shadow) { super(context, adult, baby, shadow); }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void exploration$equipment(EntityRendererProvider.Context context, CallbackInfo ci) {
        this.addLayer(new GoatEquipmentLayer(this, context));
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/animal/goat/Goat;Lnet/minecraft/client/renderer/entity/state/GoatRenderState;F)V", at = @At("TAIL"))
    private void exploration$mountState(Goat goat, GoatRenderState state, float partialTick, CallbackInfo ci) {
        GoatMount mount = (GoatMount)goat;
        GoatVisualState.Data data = ((GoatVisualState)state).exploration$goat();
        data.saddled = goat.isSaddled();
        data.armor = goat.isWearingBodyArmor() ? goat.getBodyArmorItem().copy() : net.minecraft.world.item.ItemStack.EMPTY;
        data.rear = mount.exploration$rearAnimation(partialTick);
        data.landing = mount.exploration$landingAnimation(partialTick);
        data.airborne = goat.isVehicle() && !goat.onGround() && !goat.isInWater();
    }
}
