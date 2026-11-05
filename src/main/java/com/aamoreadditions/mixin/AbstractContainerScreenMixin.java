package com.aamoreadditions.mixin;

import com.aamoreadditions.client.UpgradeStrip;
import com.aamoreadditions.upgrade.UpgradeMenus;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * For screens whose menu has upgrade slots: shift the GUI down by half the strip height so
 * GUI + strip stay centred (AA screens compute everything from topPos after super.init()),
 * and treat clicks on the strip as "inside" so items aren't thrown out.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
    @Shadow protected int topPos;
    @Shadow protected int leftPos;
    @Shadow @Final protected AbstractContainerMenu menu;
    @Shadow protected net.minecraft.world.inventory.Slot hoveredSlot;

    @Inject(method = "init", at = @At("TAIL"))
    private void aamoreadditions$shiftForUpgrades(CallbackInfo ci) {
        if (UpgradeMenus.hasUpgradeSlots(menu)) topPos += UpgradeStrip.extraHeight(menu) / 2;
    }

    @Inject(method = "hasClickedOutside", at = @At("HEAD"), cancellable = true)
    private void aamoreadditions$stripIsInside(double mx, double my, int guiLeft, int guiTop, int button, CallbackInfoReturnable<Boolean> cir) {
        if (UpgradeStrip.isOpen(menu) && UpgradeStrip.contains(menu, mx - leftPos, my - topPos)) cir.setReturnValue(false);
    }

    /** Draw the sliding panel BEFORE the machine background so it appears to come out from behind it. */
    @Inject(method = "renderBackground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderBg(Lnet/minecraft/client/gui/GuiGraphics;FII)V"))
    private void aamoreadditions$drawPanelBehind(GuiGraphics g, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (UpgradeMenus.hasUpgradeSlots(menu)) UpgradeStrip.renderPanel((AbstractContainerScreen<?>) (Object) this, g, partialTick);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"))
    private void aamoreadditions$rejectMessage(double mx, double my, int button, CallbackInfoReturnable<Boolean> cir) {
        if (UpgradeMenus.hasUpgradeSlots(menu)) UpgradeStrip.onClick((AbstractContainerScreen<?>) (Object) this, hoveredSlot);
    }
}
