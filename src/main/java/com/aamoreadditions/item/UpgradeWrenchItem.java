package com.aamoreadditions.item;

import com.aamoreadditions.AAMoreAdditions;
import com.aamoreadditions.structure.CasingCrossEvents;
import com.aamoreadditions.upgrade.UpgradeRules;
import com.aamoreadditions.upgrade.UpgradeSlots;
import com.aamoreadditions.upgrade.WrenchPlanner;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = AAMoreAdditions.MODID)
public final class UpgradeWrenchItem extends Item {
    public UpgradeWrenchItem(Properties properties) { super(properties); }

    /** Own use behaviour also makes the existing sneak-install hint leave this tool alone. */
    @Override
    public InteractionResult useOn(UseOnContext context) { return InteractionResult.PASS; }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        WrenchRecord record = stack.get(WrenchRecord.TYPE.get());
        if (record == null) return;
        tooltip.add(Component.translatable("tooltip.aamoreadditions.upgrade_wrench.recorded"));
        for (ItemStack upgrade : record.slots()) {
            tooltip.add(upgrade.isEmpty()
                    ? Component.translatable("tooltip.aamoreadditions.upgrade_wrench.empty")
                    : upgrade.getItem().getName(upgrade));
        }
    }

    /** Run before the existing normal-machine and Casing Cross GUI interactions. Main hand
     * only, following UpgradeInteraction: a successful event prevents a second-hand action.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        Player player = event.getEntity();
        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof UpgradeWrenchItem) || player.isSpectator() || !player.mayBuild()) return;
        boolean recording = player.isSecondaryUseActive();
        if (!recording && !held.has(WrenchRecord.TYPE.get())) return;
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        if (level.isClientSide) {
            var be = level.getBlockEntity(pos);
            if ((be == null || !UpgradeSlots.supports(be)) && !CasingCrossEvents.clientLooksActive(level, pos)) return;
        } else {
            UpgradeSlots.Handler handler = UpgradeSlots.target(level, pos);
            if (handler == null) return;
            if ((recording && !handler.legal()) || (!recording && !UpgradeRules.validContents(held.get(WrenchRecord.TYPE.get()).slots()))) {
                player.displayClientMessage(Component.translatable("message.aamoreadditions.upgrades.wrong_logic").withStyle(ChatFormatting.RED), true);
            } else if (recording) {
                held.set(WrenchRecord.TYPE.get(), WrenchRecord.capture(handler.contents()));
                player.displayClientMessage(Component.translatable("message.aamoreadditions.upgrade_wrench.recorded"), true);
            } else if (apply(player, handler, held.get(WrenchRecord.TYPE.get()))) {
                player.displayClientMessage(Component.translatable("message.aamoreadditions.upgrade_wrench.installed"), true);
            }
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
        }
        // Only actual record/apply actions swallow normal block use. Invalid targets or
        // an unrecorded wrench keep the original block/item interaction untouched.
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
    }

    private static Item item(ItemStack stack) { return stack.isEmpty() ? null : stack.getItem(); }

    private static boolean apply(Player player, UpgradeSlots.Handler handler, WrenchRecord record) {
        List<Item> current = handler.contents().stream().map(UpgradeWrenchItem::item).toList();
        List<Item> recorded = record.slots().stream().map(UpgradeWrenchItem::item).toList();
        Map<Item, Integer> inventory = new HashMap<>();
        // Exactly the main 36 slots, including hotbar; never armor, off-hand or cursor.
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty()) inventory.merge(stack.getItem(), stack.getCount(), Integer::sum);
        }
        List<Item> result = WrenchPlanner.plan(recorded, current,
                i -> handler.isItemValid(0, new ItemStack(i)), inventory, player.getAbilities().instabuild,
                items -> valid(handler, items));
        if (!WrenchPlanner.changed(current, result)) return false;

        // Retained stacks stay in place. Displaced real stacks are reserved for reuse
        // before backpack consumption; preserve their components, never clone the record.
        List<ItemStack> displaced = new ArrayList<>();
        List<ItemStack> replacement = new ArrayList<>();
        for (int i = 0; i < current.size(); i++) {
            ItemStack existing = handler.getStackInSlot(i);
            boolean keep = current.get(i) == result.get(i);
            replacement.add(keep ? existing.copy() : ItemStack.EMPTY);
            if (!keep && !existing.isEmpty()) displaced.add(existing.copy());
        }
        // Work on a copy until all resources have been reserved; no half-applied transaction.
        List<ItemStack> backpack = player.getInventory().items.stream().map(ItemStack::copy).toList();
        for (int i = 0; i < result.size(); i++) {
            Item wanted = result.get(i);
            if (wanted == null || !replacement.get(i).isEmpty()) continue;
            ItemStack supplied = take(displaced, wanted);
            if (supplied.isEmpty()) {
                supplied = player.getAbilities().instabuild ? new ItemStack(wanted) : take(backpack, wanted);
            }
            if (supplied.isEmpty()) return false; // defensive: leave both original inventories intact
            replacement.set(i, supplied);
        }
        for (int i = 0; i < backpack.size(); i++) player.getInventory().items.set(i, backpack.get(i));
        handler.replaceContents(replacement); // final-state rules were checked, refresh effects once
        for (ItemStack remainder : displaced) {
            ItemStack overflow = insert(player.getInventory().items, remainder);
            if (!overflow.isEmpty()) player.drop(overflow, false);
        }
        return true;
    }

    private static boolean valid(UpgradeSlots.Handler handler, List<Item> items) {
        List<ItemStack> stacks = items.stream().map(i -> i == null ? ItemStack.EMPTY : new ItemStack(i)).toList();
        for (int i = 0; i < stacks.size(); i++) {
            ItemStack stack = stacks.get(i);
            if (!stack.isEmpty() && (!handler.isItemValid(i, stack)
                    || UpgradeRules.canInstall(stacks, stack, i) != UpgradeRules.Check.OK)) return false;
        }
        return true;
    }

    private static ItemStack take(List<ItemStack> stacks, Item wanted) {
        for (ItemStack stack : stacks) {
            if (stack.is(wanted)) {
                ItemStack one = stack.copyWithCount(1);
                stack.shrink(1);
                return one;
            }
        }
        return ItemStack.EMPTY;
    }

    /** Same add-or-drop policy as empty-hand removal, without Inventory.add's creative
     * overflow deletion or offhand merging. Only the backpack is a destination/source.
     */
    private static ItemStack insert(List<ItemStack> inventory, ItemStack offered) {
        ItemStack stack = offered.copy();
        if (stack.isEmpty()) return ItemStack.EMPTY;
        for (ItemStack existing : inventory) {
            if (ItemStack.isSameItemSameComponents(existing, stack) && !existing.isEmpty()) {
                int count = Math.min(stack.getCount(), Math.max(0, existing.getMaxStackSize() - existing.getCount()));
                existing.grow(count);
                stack.shrink(count);
                if (stack.isEmpty()) return ItemStack.EMPTY;
            }
        }
        for (int i = 0; i < inventory.size(); i++) {
            if (inventory.get(i).isEmpty()) {
                inventory.set(i, stack.copy());
                return ItemStack.EMPTY;
            }
        }
        return stack;
    }
}
