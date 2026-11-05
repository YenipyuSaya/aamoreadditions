package com.aamoreadditions.mixin;

import com.aamoreadditions.upgrade.UpgradeMenus;
import de.ellpeck.actuallyadditions.mod.inventory.ContainerFurnaceDouble;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityPoweredFurnace;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ContainerFurnaceDouble.class, remap = false)
public class ContainerFurnaceDoubleMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void aamoreadditions$addUpgradeSlots(int windowId, Inventory inventory, TileEntityPoweredFurnace tile, CallbackInfo ci) {
        UpgradeMenus.addSlots((AbstractContainerMenu) (Object) this, tile);
    }
}
