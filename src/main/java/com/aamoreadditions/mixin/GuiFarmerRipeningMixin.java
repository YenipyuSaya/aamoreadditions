package com.aamoreadditions.mixin;

import com.aamoreadditions.client.FarmerRipeningGui;
import de.ellpeck.actuallyadditions.mod.inventory.gui.GuiFarmer;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=GuiFarmer.class,remap=false)
public abstract class GuiFarmerRipeningMixin {
    @Inject(method="renderBg",at=@At("TAIL"))
    private void aamoreadditions$drawRingSlot(GuiGraphics graphics,float partial,int mouseX,int mouseY,CallbackInfo ci){
        var screen=(GuiFarmer)(Object)this;
        FarmerRipeningGui.draw(graphics,screen.getMenu().farmer,screen.getGuiLeft(),screen.getGuiTop());
    }
}
