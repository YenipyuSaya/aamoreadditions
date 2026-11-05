package com.aamoreadditions.mixin;

import com.aamoreadditions.upgrade.UpgradeMenus;
import de.ellpeck.actuallyadditions.mod.inventory.CrusherContainer;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityCrusher;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CrusherContainer.class, remap = false)
public class CrusherContainerMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void aamoreadditions$addUpgradeSlots(int windowId, Inventory inventory, TileEntityCrusher tile, CallbackInfo ci) {
        UpgradeMenus.addSlots((AbstractContainerMenu) (Object) this, tile);
    }
}
