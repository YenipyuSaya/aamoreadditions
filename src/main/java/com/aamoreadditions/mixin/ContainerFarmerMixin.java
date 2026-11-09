package com.aamoreadditions.mixin;

import com.aamoreadditions.upgrade.UpgradeMenus;
import de.ellpeck.actuallyadditions.mod.inventory.ContainerFarmer;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityFarmer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ContainerFarmer.class, remap = false)
public class ContainerFarmerMixin {
    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lde/ellpeck/actuallyadditions/mod/tile/TileEntityFarmer;)V", at = @At("TAIL"))
    private void aamoreadditions$addUpgradeSlots(int windowId, Inventory inventory, TileEntityFarmer tile, CallbackInfo ci) {
        UpgradeMenus.addSlots((AbstractContainerMenu) (Object) this, tile);
        com.aamoreadditions.farmer.FarmerRingMenus.add((AbstractContainerMenu)(Object)this,tile);
    }
}
