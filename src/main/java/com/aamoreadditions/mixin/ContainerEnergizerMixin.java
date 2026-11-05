package com.aamoreadditions.mixin;

import com.aamoreadditions.upgrade.UpgradeMenus;
import de.ellpeck.actuallyadditions.mod.inventory.ContainerEnergizer;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityEnergizer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ContainerEnergizer.class, remap = false)
public class ContainerEnergizerMixin {
    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lde/ellpeck/actuallyadditions/mod/tile/TileEntityEnergizer;)V", at = @At("TAIL"))
    private void aamoreadditions$addUpgradeSlots(int windowId, Inventory inventory, TileEntityEnergizer tile, CallbackInfo ci) {
        UpgradeMenus.addSlots((AbstractContainerMenu) (Object) this, tile);
    }
}
