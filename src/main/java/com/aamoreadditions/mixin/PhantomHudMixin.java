package com.aamoreadditions.mixin;

import com.aamoreadditions.booster.BoundDisplays;
import com.aamoreadditions.booster.BoundHudText;
import com.aamoreadditions.booster.BoundTargets;
import de.ellpeck.actuallyadditions.api.tile.IPhantomTile;
import de.ellpeck.actuallyadditions.mod.blocks.blockhuds.PhantomHud;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Cross-dimension HUD: use the synced block name (not the client world's block at those
 * coordinates), drop the numeric distance in favour of {@code Cross-Dimensional}, and
 * append the dimension name on the right. Same-dimension keeps AA's sentence and live
 * block lookup, with the dimension name appended.
 * <p>
 * Do not {@code @WrapOperation} {@code Level.getBlockState}: AA's bytecode invokes
 * {@code ClientLevel.getBlockState}, so a Level target matches nothing and fails client load.
 * The info line is fully replaced (or appended) below, so wrapping getBlockState is unnecessary.
 */
@Mixin(value = PhantomHud.class, remap = false)
public abstract class PhantomHudMixin {

    @ModifyArg(method = "displayHud", at = @At(value = "INVOKE",
            target = "Lde/ellpeck/actuallyadditions/mod/blocks/blockhuds/PhantomHud;drawWordWrap(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/FormattedText;IIIIZ)V",
            ordinal = 0),
            index = 2)
    private FormattedText aama$boundInfoLine(FormattedText original) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return original;
        BlockEntity tile = hudTile();
        if (!(tile instanceof IPhantomTile phantom) || phantom.getBoundPosition() == null) return original;
        var dim = BoundTargets.dimension(tile);
        if (crossDimensional(tile, mc.level)) {
            Block block = BoundDisplays.defaultStateBlock(tile);
            if (block == null) block = Blocks.AIR;
            String name = Item.byBlock(block).getName(new ItemStack(block)).getString();
            return BoundHudText.crossDimensionalLine(name, phantom.getBoundPosition(), dim);
        }
        return BoundHudText.appendDimension((Component) original, dim);
    }

    private static boolean crossDimensional(BlockEntity tile, Level clientLevel) {
        var dim = BoundTargets.dimension(tile);
        return clientLevel != null && dim != null && dim != clientLevel.dimension();
    }

    private static BlockEntity hudTile() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !(mc.hitResult instanceof BlockHitResult hit)) return null;
        return mc.level.getBlockEntity(hit.getBlockPos());
    }
}
