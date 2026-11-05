package com.aamoreadditions.block;

import com.aamoreadditions.menu.AlloyFurnaceMenu;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityPoweredFurnace;
import de.ellpeck.actuallyadditions.mod.util.ItemStackHandlerAA.IRemover;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Reuses AA's Powered Furnace logic. Differences:
 * - both processing lines output into SLOT_OUTPUT_2 (SLOT_OUTPUT_1 is never filled and not shown in the GUI);
 * - an extra 1-slot storage ("Extra"), no function yet.
 */
public class AlloyFurnaceBlockEntity extends TileEntityPoweredFurnace {
    public final ItemStackHandler extra = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) { setChanged(); }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) { return lockAllows(LOCK_EXTRA, stack); }
    };

    /** Recipe lock (slots: 0 input 1, 1 input 2, 2 extra). Saved + synced via writeSyncableNBT. */
    public final com.aamoreadditions.recipe.RecipeLock lock = new com.aamoreadditions.recipe.RecipeLock();
    public static final int LOCK_IN_1 = 0, LOCK_IN_2 = 1, LOCK_EXTRA = 2;

    /** Lock slot index for an AA inv slot (input 1 / input 2), or -1. */
    public static int lockSlotOf(int invSlot) {
        return invSlot == SLOT_INPUT_1 ? LOCK_IN_1 : invSlot == SLOT_INPUT_2 ? LOCK_IN_2 : -1;
    }

    /** The recorded recipe (null if unlocked, pending, or missing on this side). */
    public com.aamoreadditions.recipe.AlloyingRecipe lockedRecipe() {
        return lock.recorded() ? com.aamoreadditions.recipe.ModRecipes.byId(level, lock.recipeId()) : null;
    }

    /** May this stack be placed into lock slot s (any route)? */
    public boolean lockAllows(int s, ItemStack stack) {
        if (!lock.recorded()) return true;
        var r = lockedRecipe();
        if (r == null) return true; // recipe not resolvable here (validated/cleared on the server tick)
        return lock.allows(s, i -> r.ingredientMatches(i, stack));
    }

    /** Automation view of the Extra slot: insert follows the recipe lock only, extraction always refused. */
    private final net.neoforged.neoforge.items.IItemHandlerModifiable extraAuto = new net.neoforged.neoforge.items.IItemHandlerModifiable() {
        public int getSlots() { return 1; }
        public ItemStack getStackInSlot(int slot) { return extra.getStackInSlot(slot); }
        public void setStackInSlot(int slot, ItemStack stack) { extra.setStackInSlot(slot, stack); }
        public int getSlotLimit(int slot) { return extra.getSlotLimit(slot); }
        public boolean isItemValid(int slot, ItemStack stack) {
            return com.aamoreadditions.recipe.ExtraSlotRules.canAutoInsert(autoAccepts(LOCK_EXTRA, stack));
        }
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack.isEmpty() || !isItemValid(slot, stack) ? stack : extra.insertItem(slot, stack, simulate);
        }
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return com.aamoreadditions.recipe.ExtraSlotRules.canAutoExtract() ? extra.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }
    };
    private net.neoforged.neoforge.items.IItemHandler automationHandler;

    /** Exposed to pipes/hoppers on all sides: AA inv (inputs/output rules) + Extra slot appended last. */
    @Override
    public net.neoforged.neoforge.items.IItemHandler getItemHandler(net.minecraft.core.Direction facing) {
        if (automationHandler == null)
            automationHandler = new net.neoforged.neoforge.items.wrapper.CombinedInvWrapper(inv, extraAuto);
        return automationHandler;
    }

    /** Server: toggle via menu button. */
    public void toggleLock() {
        lock.toggle();
        setChanged();
        sendUpdate();
    }

    public AlloyFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state);
    }

    private int lastEnergyA, lastFirstA, lastSecondA;
    private boolean lastSmeltedA;

    /** Current FE/t while working: config base (+ extra if the Extra slot is not empty), upgrade multipliers on top. */
    public int energyPerTick() {
        return com.aamoreadditions.effect.UpgradeEffects.alloyEnergyPerTick(this, !extra.getStackInSlot(0).isEmpty());
    }

    /**
     * Own server tick (replaces AA's for this block only). Single-line machine: while working it is charged
     * energyPerTick() ONCE per tick; without enough energy nothing progresses (items untouched).
     * AA's two internal smelt counters are kept (both feed the single output) but are never charged separately.
     */
    public static <T extends net.minecraft.world.level.block.entity.BlockEntity> void alloyServerTick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, T t) {
        if (t instanceof AlloyFurnaceBlockEntity tile) tile.tickAlloy(level, pos, state);
    }

    private com.aamoreadditions.recipe.AlloyInput alloyInput() {
        return new com.aamoreadditions.recipe.AlloyInput(inv.getStackInSlot(SLOT_INPUT_1), inv.getStackInSlot(SLOT_INPUT_2), extra.getStackInSlot(0));
    }

    /** True if the single output slot can take the whole result (empty, or same item+components with room). */
    private boolean outputFits(ItemStack result) {
        ItemStack out = inv.getStackInSlot(SLOT_OUTPUT_2);
        if (out.isEmpty()) return result.getCount() <= result.getMaxStackSize();
        return ItemStack.isSameItemSameComponents(out, result) && out.getCount() + result.getCount() <= out.getMaxStackSize();
    }

    /** Single-line alloying tick. Progress is kept in firstSmeltTime (secondSmeltTime stays 0). */
    private void tickAlloy(net.minecraft.world.level.Level level, BlockPos pos, BlockState state) {
        this.serverTick();
        mergeLegacyOutput();
        secondSmeltTime = 0;
        if (lock.recorded() && level.getGameTime() % 20 == 0
                && lock.validate(id -> com.aamoreadditions.recipe.ModRecipes.byId(level, id) != null)) {
            setChanged();
            sendUpdate();
        }
        var match = com.aamoreadditions.recipe.ModRecipes.find(level, alloyInput());
        ItemStack result = match == null ? ItemStack.EMPTY : match.recipe().value().assemble(alloyInput(), level.registryAccess());
        boolean working = match != null && !result.isEmpty() && outputFits(result);
        if (!working) firstSmeltTime = 0;
        int cost = working ? energyPerTick() : 0;
        if (com.aamoreadditions.effect.UpgradeMath.alloyTickProgresses(working, storage.getEnergyStored(), cost)) {
            int max = com.aamoreadditions.effect.UpgradeEffects.time(this, 80);
            if (++firstSmeltTime >= max) {
                // record the lock mapping BEFORE consuming (slots may become empty)
                if (lock.recordIfPending(match.recipe().id().toString(), match.take())) { setChanged(); sendUpdate(); }
                finishAlloy(match.plan(), result);
                firstSmeltTime = 0;
            }
            int charge = com.aamoreadditions.effect.UpgradeMath.alloyTickCharge(working, storage.getEnergyStored(), cost);
            if (charge > 0) storage.extractEnergyInternal(charge, false);
        }
        boolean current = state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT);
        boolean changeTo = current;
        if (lastSmeltedA != working) changeTo = working;
        if (isRedstonePowered) changeTo = true;
        if (!working && !isRedstonePowered) changeTo = false;
        if (changeTo != current) level.setBlock(pos, state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT, changeTo), net.minecraft.world.level.block.Block.UPDATE_ALL);
        lastSmeltedA = working;
        if ((lastEnergyA != storage.getEnergyStored() || lastFirstA != firstSmeltTime || lastSecondA != secondSmeltTime) && sendUpdateWithInterval()) {
            lastEnergyA = storage.getEnergyStored();
            lastFirstA = firstSmeltTime;
            lastSecondA = secondSmeltTime;
        }
    }

    /** Output first (re-checked), then consume exactly the planned amounts; nothing is consumed if the output can't fit. */
    private void finishAlloy(int[] plan, ItemStack result) {
        if (!outputFits(result)) return;
        ItemStack out = inv.getStackInSlot(SLOT_OUTPUT_2);
        if (out.isEmpty()) inv.setStackInSlot(SLOT_OUTPUT_2, result.copy());
        else out.grow(result.getCount());
        if (plan[0] > 0) inv.getStackInSlot(SLOT_INPUT_1).shrink(plan[0]);
        if (plan[1] > 0) inv.getStackInSlot(SLOT_INPUT_2).shrink(plan[1]);
        if (plan[2] > 0) extra.extractItem(0, plan[2], false);
        setChanged();
    }

    /** AA's button 0 toggled auto-split; the Alloy Furnace has no auto-split (button is a reserved placeholder). */
    @Override
    public void onButtonPressed(int buttonID, net.minecraft.world.entity.player.Player player) {}

    /** Automation / shift-click: only items used by some alloying recipe. */
    @Override
    public boolean validInput(ItemStack stack) {
        return com.aamoreadditions.recipe.ModRecipes.isIngredient(level, stack);
    }

    /** Automation: inputs only; recorded lock -> lockAllows; otherwise any item, but top up existing same stacks first. */
    @Override
    public de.ellpeck.actuallyadditions.mod.util.ItemStackHandlerAA.IAcceptor getAcceptor() {
        return (slot, stack, automation) -> {
            if (!automation) return true;
            if (slot != SLOT_INPUT_1 && slot != SLOT_INPUT_2) return false;
            return autoAccepts(slot == SLOT_INPUT_1 ? LOCK_IN_1 : LOCK_IN_2, stack);
        };
    }

    /** Shared automation rule for lock slots 0 input1, 1 input2, 2 extra. */
    public boolean autoAccepts(int lockSlot, ItemStack stack) {
        if (!lockAllows(lockSlot, stack)) return false;
        if (lock.recorded()) return true;
        ItemStack[] st = {inv.getStackInSlot(SLOT_INPUT_1), inv.getStackInSlot(SLOT_INPUT_2), extra.getStackInSlot(0)};
        int[] lim = {Math.min(stack.getMaxStackSize(), inv.getSlotLimit(SLOT_INPUT_1)), Math.min(stack.getMaxStackSize(), inv.getSlotLimit(SLOT_INPUT_2)), Math.min(stack.getMaxStackSize(), extra.getSlotLimit(0))};
        boolean[] e = new boolean[3], same = new boolean[3]; int[] c = new int[3];
        for (int i = 0; i < 3; i++) { e[i] = st[i].isEmpty(); same[i] = !e[i] && ItemStack.isSameItemSameComponents(st[i], stack); c[i] = st[i].getCount(); }
        return com.aamoreadditions.recipe.FillFirst.mayAccept(lockSlot, e, same, c, lim);
    }

    private static int redirect(int output) { return output == SLOT_OUTPUT_1 ? SLOT_OUTPUT_2 : output; }

    /** Move anything left in the hidden slot (old saves) into the single output; never deletes items. */
    private void mergeLegacyOutput() {
        ItemStack hidden = inv.getStackInSlot(SLOT_OUTPUT_1);
        if (hidden.isEmpty()) return;
        ItemStack out = inv.getStackInSlot(SLOT_OUTPUT_2);
        if (out.isEmpty()) {
            inv.setStackInSlot(SLOT_OUTPUT_2, hidden.copy());
            inv.setStackInSlot(SLOT_OUTPUT_1, ItemStack.EMPTY);
        } else if (ItemStack.isSameItemSameComponents(out, hidden)) {
            int move = Math.min(hidden.getCount(), out.getMaxStackSize() - out.getCount());
            if (move > 0) {
                out.grow(move);
                hidden.shrink(move);
                setChanged();
            }
        }
    }

    @Override
    public boolean canSmeltOn(int theInput, int theOutput) {
        if (theOutput == SLOT_OUTPUT_1) mergeLegacyOutput();
        return super.canSmeltOn(theInput, redirect(theOutput));
    }

    @Override
    public void finishBurning(int theInput, int theOutput) {
        int out = redirect(theOutput);
        // both lines share one output: re-check room so a same-tick double finish can't overfill
        if (!super.canSmeltOn(theInput, out)) return;
        super.finishBurning(theInput, out);
    }

    @Override
    public IRemover getRemover() {
        // automation may still pull from the hidden slot so nothing can ever get stuck there
        return (slot, automation) -> !automation || slot == SLOT_OUTPUT_1 || slot == SLOT_OUTPUT_2;
    }

    @Override
    public void writeSyncableNBT(CompoundTag compound, HolderLookup.Provider lookupProvider, NBTType type) {
        super.writeSyncableNBT(compound, lookupProvider, type);
        if (type != NBTType.SAVE_BLOCK) {
            compound.put("AAMAExtra", extra.serializeNBT(lookupProvider));
            CompoundTag l = new CompoundTag();
            l.putBoolean("Locked", lock.locked());
            l.putString("Recipe", lock.encodedId());
            l.putIntArray("Slots", lock.encodedMap());
            compound.put("AAMALock", l);
        }
    }

    @Override
    public void readSyncableNBT(CompoundTag compound, HolderLookup.Provider lookupProvider, NBTType type) {
        super.readSyncableNBT(compound, lookupProvider, type);
        if (type != NBTType.SAVE_BLOCK && compound.contains("AAMAExtra")) extra.deserializeNBT(lookupProvider, compound.getCompound("AAMAExtra"));
        if (type != NBTType.SAVE_BLOCK) {
            if (compound.contains("AAMALock")) {
                CompoundTag l = compound.getCompound("AAMALock");
                lock.decode(l.getBoolean("Locked"), l.getString("Recipe"), l.getIntArray("Slots"));
            } else {
                lock.unlock(); // old saves: unlocked
            }
        }
    }

    @Override
    public BlockEntityType<?> getType() {
        return ModBlocks.ALLOY_FURNACE_BE.get();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.aamoreadditions.alloyfurnace");
    }

    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory playerInventory, Player player) {
        return new AlloyFurnaceMenu(windowId, playerInventory, this);
    }
}
