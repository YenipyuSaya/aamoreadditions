package com.aamoreadditions.mixin;

import com.aamoreadditions.structure.CasingCrossDisplay;
import com.mojang.blaze3d.vertex.PoseStack;
import de.ellpeck.actuallyadditions.mod.blocks.render.RenderDisplayStand;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityDisplayStand;
import de.ellpeck.actuallyadditions.mod.util.ItemStackHandlerAA;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Substitute only the rendered stack; preserve AA's entire rendering implementation. */
@Mixin(value = RenderDisplayStand.class, remap = false)
public abstract class RenderDisplayStandMixin {
    @Redirect(method = "render(Lde/ellpeck/actuallyadditions/mod/tile/TileEntityDisplayStand;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            at = @At(value = "INVOKE", target = "Lde/ellpeck/actuallyadditions/mod/util/ItemStackHandlerAA;getStackInSlot(I)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack aamoreadditions$renderSharedInput(ItemStackHandlerAA nativeInventory, int slot,
            TileEntityDisplayStand tile, float partialTicks, PoseStack matrices, MultiBufferSource buffer,
            int light, int overlay) {
        return CasingCrossDisplay.renderedStack(tile, nativeInventory.getStackInSlot(slot));
    }
}
