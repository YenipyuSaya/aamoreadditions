package com.aamoreadditions.menu;

import com.aamoreadditions.structure.CasingCrossData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/** Empowerer structure GUI. Slots: 0 up, 1 down, 2 left, 3 right (stand inputs), 4 centre (base input), 5 output, 6..41 player. */
public class CasingCrossMenu extends AbstractContainerMenu {
    public static final int BASE = CasingCrossData.SLOT_BASE, OUT = CasingCrossData.SLOT_OUTPUT, INV_START = 6, INV_END = 42;
    public final BlockPos anchor;
    public final com.aamoreadditions.upgrade.UpgradeSlots.Handler upgrades;
    public double speedMultiplier() { return CasingCrossData.speedMultiplier(upgrades); }
    private final CasingCrossData.Entry entry; // null on the client
    private final ContainerData data; // 0/1 energy low/high 16 bits, 2 progress, 3 recipe time (DataSlots sync as shorts)
    private final net.minecraft.world.item.crafting.RecipeManager recipes;

    public CasingCrossMenu(int id, Inventory inv, BlockPos anchor, CasingCrossData.Entry entry) {
        this(id, inv, anchor, entry, entry == null ? new ItemStackHandler(CasingCrossData.SLOTS) : entry.inv);
    }

    private CasingCrossMenu(int id, Inventory inv, BlockPos anchor, CasingCrossData.Entry entry, IItemHandler items) {
        super(ModMenus.CASING_CROSS.get(), id);
        this.anchor = anchor;
        this.entry = entry;
        this.recipes = inv.player.level().getRecipeManager();
        this.data = entry == null ? new net.minecraft.world.inventory.SimpleContainerData(DATA) : new ContainerData() {
            @Override public int get(int i) {
                int e = entry.energy.getEnergyStored();
                return switch (i) {
                    case 0 -> e & 0xFFFF;
                    case 1 -> (e >>> 16) & 0xFFFF;
                    case 2 -> Math.min(entry.processTime, Short.MAX_VALUE);
                    case 3 -> Math.min(entry.recipeTime, Short.MAX_VALUE);
                    case 4 -> (entry.locked ? 1 : 0) | (entry.lockRecorded() ? 2 : 0);
                    case 5 -> lockIndex();
                    default -> packMap(entry.lockMap);
                };
            }
            @Override public void set(int i, int v) {}
            @Override public int getCount() { return DATA; }
        };
        addSlot(new Input(items, 0, 72, 14));
        addSlot(new Input(items, 1, 72, 66));
        addSlot(new Input(items, 2, 44, 40));
        addSlot(new Input(items, 3, 100, 40));
        addSlot(new Input(items, BASE, 72, 40));
        addSlot(new SlotItemHandler(items, OUT, 128, 66) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 9; j++)
                addSlot(new Slot(inv, j + i * 9 + 9, 8 + j * 18, 97 + i * 18));
        for (int i = 0; i < 9; i++) addSlot(new Slot(inv, i, 8 + i * 18, 155));
        // upgrade slots appended after everything (3, Speed only); stored per structure, not exposed to pipes
        this.upgrades = entry != null ? entry.upgrades : new com.aamoreadditions.upgrade.UpgradeSlots.Handler(new com.aamoreadditions.upgrade.SpeedOnlyHolder() {});
        for (int i = 0; i < upgrades.getSlots(); i++) addSlot(new com.aamoreadditions.upgrade.UpgradeSlot(this, upgrades, i));
        addDataSlots(this.data);
    }

    /** Validity (and recipe lock) checked with this side's recipe manager, so client and server agree. */
    private final class Input extends SlotItemHandler {
        Input(IItemHandler h, int i, int x, int y) { super(h, i, x, y); }
        @Override public boolean mayPlace(ItemStack stack) {
            return CasingCrossData.validFor(recipes, getSlotIndex(), stack) && lockAllows(getSlotIndex(), stack);
        }
    }

    // ---- recipe lock sync: data 4 = flags (1 locked, 2 recorded), 5 = index into id-sorted empowering recipes, 6 = slot map (3 bits per slot) ----
    public static final int DATA = 7, BUTTON_LOCK = 7710;

    private int lockIndex() {
        if (!entry.lockRecorded()) return -1;
        var l = CasingCrossData.sortedRecipes(recipes);
        for (int i = 0; i < l.size(); i++) if (l.get(i).id().equals(entry.lockId)) return i;
        return -1;
    }

    private static int packMap(int[] m) {
        int v = 0;
        for (int i = 0; i < 5; i++) v |= ((m[i] < 0 ? 7 : m[i]) & 7) << (i * 3);
        return v;
    }

    public boolean locked() { return entry != null ? entry.locked : (data.get(4) & 1) != 0; }
    public boolean lockRecorded() { return entry != null ? entry.lockRecorded() : (data.get(4) & 2) != 0; }

    /** Slot -> ingredient map (-1 forbidden, 0..3 stand ingredient, 4 base). */
    public int[] lockMap() {
        if (entry != null) return entry.lockMap.clone();
        int v = data.get(6) & 0x7FFF;
        int[] m = new int[5];
        for (int i = 0; i < 5; i++) { int x = (v >> (i * 3)) & 7; m[i] = x == 7 ? -1 : x; }
        return m;
    }

    /** The recorded recipe as seen on this side, or null. */
    public net.minecraft.world.item.crafting.RecipeHolder<de.ellpeck.actuallyadditions.mod.crafting.EmpowererRecipe> lockedRecipe() {
        if (!lockRecorded()) return null;
        if (entry != null) return CasingCrossData.byId(recipes, entry.lockId);
        int i = (short) data.get(5);
        var l = CasingCrossData.sortedRecipes(recipes);
        return i >= 0 && i < l.size() ? l.get(i) : null;
    }

    private boolean lockAllows(int slot, ItemStack stack) {
        if (!lockRecorded()) return true;
        if (entry != null) return entry.lockAllows(recipes, slot, stack);
        var r = lockedRecipe();
        return r == null || CasingCrossData.lockAllows(r, lockMap(), slot, stack); // client unsure -> let the server decide
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_LOCK) {
            if (entry != null) entry.toggleLock();
            return true;
        }
        return super.clickMenuButton(player, id);
    }

    public static CasingCrossMenu fromNetwork(int id, Inventory inv, FriendlyByteBuf buf) {
        return new CasingCrossMenu(id, inv, buf.readBlockPos(), null);
    }

    public int energy() { return (data.get(0) & 0xFFFF) | ((data.get(1) & 0xFFFF) << 16); }
    public int capacity() { return com.aamoreadditions.structure.CasingCrossEnergy.capacity(); }
    public int progress() { return data.get(2); }
    public int recipeTime() { return data.get(3); }

    public CasingCrossData.Entry entry() { return entry; }

    public boolean isServerFor(Level level) { return entry != null; }

    @Override
    public boolean stillValid(Player player) {
        if (entry == null) return true;
        if (!(player.level() instanceof ServerLevel sl) || CasingCrossData.get(sl).entry(anchor) != entry) return false;
        return player.distanceToSqr(anchor.getX() + 0.5, anchor.getY() + 0.5, anchor.getZ() + 0.5) <= 64.0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack up = com.aamoreadditions.upgrade.UpgradeMenus.quickMove(this, player, index);
        if (up != null) return up;
        if (index >= INV_END) return ItemStack.EMPTY;
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < INV_START) {
            if (!moveItemStackTo(stack, INV_START, INV_END, true)) return ItemStack.EMPTY;
        } else {
            boolean moved = false;
            // Priority: centre, up, right, down, left. First top up slots with the same item, then fill empty ones.
            for (int pass = 0; pass < 2 && !stack.isEmpty(); pass++) {
                for (int s : CasingCrossData.INSERT_PRIORITY) {
                    if (stack.isEmpty()) break;
                    Slot t = slots.get(s);
                    if ((pass == 0) != t.hasItem()) continue;
                    if (pass == 0 && !ItemStack.isSameItemSameComponents(t.getItem(), stack)) continue;
                    if (moveItemStackTo(stack, s, s + 1, false)) moved = true;
                }
            }
            if (!moved) {
                if (index < INV_END - 9) { if (!moveItemStackTo(stack, INV_END - 9, INV_END, false)) return ItemStack.EMPTY; }
                else if (!moveItemStackTo(stack, INV_START, INV_END - 9, false)) return ItemStack.EMPTY;
            }
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        if (stack.getCount() == copy.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return copy;
    }
}
