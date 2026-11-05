package com.aamoreadditions.mixin;

import com.aamoreadditions.upgrade.UpgradeMenuState;
import com.aamoreadditions.upgrade.UpgradeMenus;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Per-menu open/closed flag (closed by default); server side is updated via clickMenuButton. */
@Mixin(AbstractContainerMenu.class)
public class AbstractContainerMenuMixin implements UpgradeMenuState {
    @Unique private boolean aamoreadditions$upgradeOpen;

    @Override public boolean aamoreadditions$isUpgradeOpen() { return aamoreadditions$upgradeOpen; }

    @Override public void aamoreadditions$setUpgradeOpen(boolean open) { aamoreadditions$upgradeOpen = open; }

    @Inject(method = "clickMenuButton", at = @At("HEAD"), cancellable = true)
    private void aamoreadditions$toggle(Player player, int id, CallbackInfoReturnable<Boolean> cir) {
        if ((id == BUTTON_OPEN || id == BUTTON_CLOSE) && UpgradeMenus.hasUpgradeSlots((AbstractContainerMenu) (Object) this)) {
            aamoreadditions$upgradeOpen = id == BUTTON_OPEN;
            cir.setReturnValue(true);
        }
    }
}
