package com.aamoreadditions.mixin;

import com.aamoreadditions.booster.BoundHudText;
import com.aamoreadditions.booster.BoundDisplays;
import com.aamoreadditions.booster.BoundWorlds;
import de.ellpeck.actuallyadditions.api.tile.IPhantomTile;
import de.ellpeck.actuallyadditions.mod.components.ActuallyComponents;
import de.ellpeck.actuallyadditions.mod.items.ItemPhantomConnector;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityBase;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/** Always apply a stored bind, including across dimensions; the face itself decides if it is in range. */
@Mixin(value = ItemPhantomConnector.class, remap = false)
public abstract class ItemPhantomConnectorMixin {

    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true, remap = true)
    private void aama$dimensionalBind(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (context.getLevel().isClientSide) return;
        ItemStack stack = context.getItemInHand();
        BlockEntity tile = context.getLevel().getBlockEntity(context.getClickedPos());
        if (!(tile instanceof IPhantomTile phantom)) return;
        BlockPos stored = ItemPhantomConnector.getStoredPosition(stack);
        if (stored == null) return;
        var storedWorld = ItemPhantomConnector.getStoredWorld(stack);
        phantom.setBoundPosition(stored);
        BoundWorlds.set(tile, storedWorld);
        BoundDisplays.refresh(tile);
        if (tile instanceof TileEntityBase base) base.sendUpdate();
        ItemPhantomConnector.clearStorage(stack, ActuallyComponents.POSITION.get(), ActuallyComponents.LEVEL.get());
        if (context.getPlayer() != null) {
            context.getPlayer().displayClientMessage(Component.translatable("tooltip.actuallyadditions.phantom.connected.desc"), true);
        }
        cir.setReturnValue(InteractionResult.SUCCESS);
    }

    @Inject(method = "appendHoverText", at = @At("RETURN"), remap = true)
    private void aama$tooltipDimension(ItemStack stack, Item.TooltipContext context, List<Component> list, TooltipFlag advanced, CallbackInfo ci) {
        if (ItemPhantomConnector.getStoredPosition(stack) == null) return;
        var world = ItemPhantomConnector.getStoredWorld(stack);
        if (world == null || list.isEmpty()) return;
        // Insert the dimension to the right of the XYZ lines, before the clear-storage hint.
        int at = list.size();
        if (at > 0) at = at - 1;
        list.add(at, BoundHudText.dimensionName(world));
    }
}
