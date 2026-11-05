package com.aamoreadditions.mixin;

import com.aamoreadditions.upgrade.UpgradeMenus;
import de.ellpeck.actuallyadditions.mod.inventory.ContainerCoalGenerator;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityCoalGenerator;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ContainerCoalGenerator.class, remap = false)
public class ContainerCoalGeneratorMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void aamoreadditions$addUpgradeSlots(int windowId, Inventory inventory, TileEntityCoalGenerator tile, CallbackInfo ci) {
        UpgradeMenus.addSlots((AbstractContainerMenu) (Object) this, tile);
    }
}
