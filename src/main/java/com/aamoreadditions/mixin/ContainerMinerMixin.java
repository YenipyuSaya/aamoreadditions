package com.aamoreadditions.mixin;

import com.aamoreadditions.digger.DiggerMenuState;
import com.aamoreadditions.digger.DiggerMenus;
import com.aamoreadditions.upgrade.UpgradeMenus;
import de.ellpeck.actuallyadditions.mod.inventory.ContainerMiner;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityVerticalDigger;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Upgrade slots, then the 9 filler slots, appended after AA's 45 slots. */
@Mixin(value = ContainerMiner.class, remap = false)
public class ContainerMinerMixin implements DiggerMenuState {
    @Unique private boolean aamoreadditions$fillersOpen;

    @Override public boolean aamoreadditions$fillersOpen() { return aamoreadditions$fillersOpen; }
    @Override public void aamoreadditions$setFillersOpen(boolean open) { aamoreadditions$fillersOpen = open; }

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lde/ellpeck/actuallyadditions/mod/tile/TileEntityVerticalDigger;)V", at = @At("TAIL"))
    private void aamoreadditions$addSlots(int windowId, Inventory inventory, TileEntityVerticalDigger tile, CallbackInfo ci) {
        UpgradeMenus.addSlots((AbstractContainerMenu) (Object) this, tile);
        DiggerMenus.addSlots((ContainerMiner) (Object) this);
    }
}
