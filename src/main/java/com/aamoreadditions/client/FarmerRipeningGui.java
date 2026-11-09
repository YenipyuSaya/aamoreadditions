package com.aamoreadditions.client;

import com.aamoreadditions.farmer.FarmerRingInventory;
import com.aamoreadditions.farmer.FarmerRipeningMath;
import com.mojang.blaze3d.systems.RenderSystem;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityFarmer;
import de.ellpeck.actuallyadditions.mod.util.AssetUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Final approved pixel layout. Rendered behind actual menu items; ghost always remains underneath. */
public final class FarmerRipeningGui {
    private static final ResourceLocation SLOT=ResourceLocation.fromNamespaceAndPath("aamoreadditions","textures/gui/farmer_ring_slot.png");
    private static final ResourceLocation RING=ResourceLocation.fromNamespaceAndPath("actuallyadditions","textures/item/ring_of_growth.png");
    private FarmerRipeningGui() {}
    public static void draw(GuiGraphics g,TileEntityFarmer tile,int left,int top){
        var ring=FarmerRingInventory.get(tile);
        if(!ring.enabled())return;
        g.blit(SLOT,left+67,top+75,0,0,16,16,16,16);
        g.flush();RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1,1,1,.35f);
        g.blit(RING,left+67,top+75,0,0,16,16,16,16);
        g.flush();RenderSystem.setShaderColor(1,1,1,1);RenderSystem.disableBlend();
        int color=0xff565656;
        if(FarmerRipeningMath.arrowActive(ring.hasRing(),tile.storage.getEnergyStored())){
            float[] rgb=AssetUtil.getWheelColor(tile.getLevel().getGameTime()%256);
            color=0xff000000|Mth.color(rgb[0]/255f,rgb[1]/255f,rgb[2]/255f);
        }
        g.fill(left+52,top+81,left+62,top+84,color); // 3px shaft
        for(int i=0;i<4;i++)g.fill(left+61+i,top+79+i,left+62+i,top+86-i,color); // 7/5/3/1 triangle
    }
}
