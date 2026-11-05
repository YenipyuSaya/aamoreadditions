package com.aamoreadditions.mixin;

import com.aamoreadditions.structure.CasingCrossDisplay;
import com.mojang.blaze3d.vertex.PoseStack;
import de.ellpeck.actuallyadditions.mod.blocks.render.RenderEmpowerer;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityEmpowerer;
import de.ellpeck.actuallyadditions.mod.crafting.EmpowererRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.crafting.RecipeHolder;
import de.ellpeck.actuallyadditions.mod.util.ItemStackHandlerAA;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Substitute only the rendered stack; preserve AA's entire rendering implementation. */
@Mixin(value = RenderEmpowerer.class, remap = false)
public abstract class RenderEmpowererMixin {
    @Redirect(method = "render(Lde/ellpeck/actuallyadditions/mod/tile/TileEntityEmpowerer;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            at = @At(value = "INVOKE", target = "Lde/ellpeck/actuallyadditions/mod/util/ItemStackHandlerAA;getStackInSlot(I)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack aamoreadditions$renderSharedInput(ItemStackHandlerAA nativeInventory, int slot,
            TileEntityEmpowerer tile, float partialTicks, PoseStack matrices, MultiBufferSource buffer,
            int light, int overlay) {
        return CasingCrossDisplay.renderedStack(tile, nativeInventory.getStackInSlot(slot));
    }

    @Redirect(method = "render(Lde/ellpeck/actuallyadditions/mod/tile/TileEntityEmpowerer;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            at = @At(value = "INVOKE", target = "Lde/ellpeck/actuallyadditions/mod/tile/TileEntityEmpowerer;getCurrentRecipe()Lnet/minecraft/world/item/crafting/RecipeHolder;"))
    private RecipeHolder<EmpowererRecipe> aamoreadditions$sharedRunningRecipe(TileEntityEmpowerer tile) {
        return CasingCrossDisplay.renderedRecipe(tile, tile.getCurrentRecipe());
    }

    @Redirect(method = "render(Lde/ellpeck/actuallyadditions/mod/tile/TileEntityEmpowerer;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;relative(Lnet/minecraft/core/Direction;I)Lnet/minecraft/core/BlockPos;"))
    private BlockPos aamoreadditions$adjacentStandBeam(BlockPos origin, Direction direction, int distance,
            TileEntityEmpowerer tile, float partialTicks, PoseStack matrices, MultiBufferSource buffer,
            int light, int overlay) {
        boolean active = ((CasingCrossDisplay.View) tile).aamoreadditions$getDisplay() != null;
        return origin.relative(direction, active ? 1 : distance);
    }
}
