package com.akitain.explorationreloaded.mixin;

import com.akitain.explorationreloaded.registry.ExplorationItems;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.LodestoneTracker;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilMenu.class)
public abstract class WitherCompassAnvilMixin extends ItemCombinerMenu {
    @Shadow @Final private DataSlot cost;
    @Shadow private int repairItemCountCost;
    @Shadow private boolean onlyRenaming;
    @Shadow private String itemName;
    @Unique private ItemStack exploration$remainingCompasses;

    protected WitherCompassAnvilMixin(MenuType<?> type, int id, Inventory inventory,
                                      ContainerLevelAccess access, ItemCombinerMenuSlotDefinition slots) {
        super(type, id, inventory, access, slots);
    }

    @Unique
    private boolean exploration$isCharging() {
        ItemStack compass = inputSlots.getItem(0);
        LodestoneTracker tracker = compass.get(DataComponents.LODESTONE_TRACKER);
        return compass.is(Items.COMPASS) && inputSlots.getItem(1).is(Items.NETHER_STAR)
                && tracker != null && tracker.target().isPresent();
    }

    @Inject(method = "createResult", at = @At("HEAD"), cancellable = true)
    private void exploration$chargeCompass(CallbackInfo ci) {
        if (!exploration$isCharging()) return;
        ItemStack input = inputSlots.getItem(0);
        ItemStack result = input.transmuteCopy(ExplorationItems.WITHER_COMPASS, 1);
        if (itemName != null) {
            if (itemName.isBlank()) result.remove(DataComponents.CUSTOM_NAME);
            else if (!itemName.equals(input.getHoverName().getString())) {
                result.set(DataComponents.CUSTOM_NAME, Component.literal(itemName));
            }
        }
        repairItemCountCost = 1;
        onlyRenaming = false;
        cost.set(1);
        resultSlots.setItem(0, result);
        broadcastChanges();
        ci.cancel();
    }

    @Inject(method = "onTake", at = @At("HEAD"))
    private void exploration$rememberRemainder(Player player, ItemStack carried, CallbackInfo ci) {
        // Shift-click has already emptied the result stack when vanilla calls onTake.
        exploration$remainingCompasses = exploration$isCharging()
                ? inputSlots.getItem(0).copyWithCount(inputSlots.getItem(0).getCount() - 1) : null;
    }

    @WrapOperation(method = "onTake", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/Container;setItem(ILnet/minecraft/world/item/ItemStack;)V"))
    private void exploration$consumeOneCompass(Container container, int slot, ItemStack stack, Operation<Void> original) {
        if (container == inputSlots && slot == 0 && exploration$remainingCompasses != null) {
            stack = exploration$remainingCompasses;
            exploration$remainingCompasses = null;
        }
        original.call(container, slot, stack);
    }
}
