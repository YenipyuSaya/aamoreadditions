package com.aamoreadditions.upgrade;

import com.aamoreadditions.AAMoreAdditions;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Sneak + right-click (main hand) on a supported AA machine:
 *  - upgrade item  -> install 1 into first empty slot (left to right); same item already installed -> message
 *  - empty hand    -> remove left slot first, else right; nothing installed -> vanilla behaviour
 *  - other item    -> BlockItems / items with their own useOn: untouched and silent;
 *                     otherwise "invalid item" message, event NOT cancelled.
 * Storage is the same attachment the GUI slots use, so both stay in sync.
 */
@EventBusSubscriber(modid = AAMoreAdditions.MODID)
public final class UpgradeInteraction {
    private UpgradeInteraction() {}

    @SubscribeEvent
    static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return; // off-hand event never handled -> no doubled messages
        Player player = event.getEntity();
        if (!player.isSecondaryUseActive()) return;
        Level level = event.getLevel();
        BlockEntity be = level.getBlockEntity(event.getPos());
        if (be == null || !UpgradeSlots.supports(be)) return;
        ItemStack held = player.getMainHandItem();

        if (held.is(UpgradeSlots.UPGRADES)) {
            // Cancel on both sides (client still sends the use packet; the server decides).
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
            if (level.isClientSide) return;
            install(player, level, be, held);
        } else if (held.isEmpty()) {
            if (level.isClientSide) return; // client can't see slot contents; server cancels only if it removes something
            if (remove(player, level, be)) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
            }
        } else if (!level.isClientSide && !hasOwnUseBehaviour(held)) {
            // Lowest priority: just a hint, vanilla use continues.
            player.displayClientMessage(UpgradeRules.Check.INVALID.message(), true);
        }
    }

    private static void install(Player player, Level level, BlockEntity be, ItemStack held) {
        UpgradeSlots.Handler h = UpgradeSlots.get(be);
        UpgradeRules.Check check = UpgradeRules.canInstall(h, held, -1);
        if (check != UpgradeRules.Check.OK) {
            player.displayClientMessage(check.message(), true);
            return;
        }
        for (int i = 0; i < h.getSlots(); i++) {
            if (h.getStackInSlot(i).isEmpty()) {
                h.setStackInSlot(i, held.copyWithCount(1));
                if (!player.getAbilities().instabuild) held.shrink(1);
                level.playSound(null, be.getBlockPos(), SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1f, 1f);
                return;
            }
        }
        // No empty slot (shouldn't happen with 2 unique upgrade types): do nothing.
    }

    private static boolean remove(Player player, Level level, BlockEntity be) {
        UpgradeSlots.Handler h = UpgradeSlots.get(be);
        for (int i = 0; i < h.getSlots(); i++) { // leftmost filled slot first
            ItemStack s = h.getStackInSlot(i);
            if (!s.isEmpty() && UpgradeRules.canRemove(h.contents(), i)) { // leftmost removable slot
                h.setStackInSlot(i, ItemStack.EMPTY);
                if (!player.getInventory().add(s)) player.drop(s, false);
                level.playSound(null, be.getBlockPos(), SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1f, 1f);
                return true;
            }
        }
        return false;
    }

    private static final ClassValue<Boolean> OVERRIDES_USE_ON = new ClassValue<>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
            try {
                if (type.getMethod("useOn", UseOnContext.class).getDeclaringClass() != Item.class) return true;
                return type.getMethod("onItemUseFirst", ItemStack.class, UseOnContext.class).getDeclaringClass().isInterface() ? false : true;
            } catch (NoSuchMethodException e) {
                return true; // unknown: be conservative and stay silent
            }
        }
    };

    /** BlockItems and items overriding useOn / onItemUseFirst may do something on the block: stay out of the way. */
    static boolean hasOwnUseBehaviour(ItemStack stack) {
        Item item = stack.getItem();
        return item instanceof BlockItem || OVERRIDES_USE_ON.get(item.getClass());
    }
}
