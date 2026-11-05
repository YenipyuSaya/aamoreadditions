package com.aamoreadditions.structure;

import de.ellpeck.actuallyadditions.mod.crafting.ActuallyRecipes;
import de.ellpeck.actuallyadditions.mod.crafting.EmpowererRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Per-level store of active Casing Cross structures (keyed by Empowerer position): inventory, energy, progress.
 *
 * Slots: 0 up (= NORTH stand), 1 down (= SOUTH), 2 left (= WEST), 3 right (= EAST), 4 centre base item, 5 output.
 * GUI is read like a map with north up. AA's Empowerer passes stands as Direction.from2DDataValue(0..3) =
 * SOUTH, WEST, NORTH, EAST, so we call AA's matcher with slots (1, 2, 0, 3).
 */
public class CasingCrossData extends SavedData {
    public static final int SLOTS = 6;
    public static final int SLOT_UP = 0, SLOT_DOWN = 1, SLOT_LEFT = 2, SLOT_RIGHT = 3, SLOT_BASE = 4, SLOT_OUTPUT = 5;
    /** Stand slots in AA order (SOUTH, WEST, NORTH, EAST). */
    /** Insertion priority for shift-click and hoppers/pipes: centre, north(up), east(right), south(down), west(left). */
    public static final int[] INSERT_PRIORITY = {SLOT_BASE, SLOT_UP, SLOT_RIGHT, SLOT_DOWN, SLOT_LEFT};
    public static final int[] AA_STAND_ORDER = {SLOT_DOWN, SLOT_LEFT, SLOT_UP, SLOT_RIGHT};
    private static final String NAME = "aamoreadditions_casing_cross";

    // ---- recipe helpers (shared by server data and the client menu) ----
    public static boolean isBaseInput(@Nullable RecipeManager rm, ItemStack s) {
        if (rm == null || s.isEmpty()) return false;
        for (RecipeHolder<EmpowererRecipe> r : rm.getAllRecipesFor(ActuallyRecipes.Types.EMPOWERING.get()))
            if (r.value().getInput().test(s)) return true;
        return false;
    }

    public static boolean isStandInput(@Nullable RecipeManager rm, ItemStack s) {
        if (rm == null || s.isEmpty()) return false;
        for (RecipeHolder<EmpowererRecipe> r : rm.getAllRecipesFor(ActuallyRecipes.Types.EMPOWERING.get())) {
            EmpowererRecipe e = r.value();
            if (e.getStandOne().test(s) || e.getStandTwo().test(s) || e.getStandThree().test(s) || e.getStandFour().test(s)) return true;
        }
        return false;
    }

    public static boolean validFor(@Nullable RecipeManager rm, int slot, ItemStack s) {
        if (slot == SLOT_OUTPUT) return false;
        if (slot == SLOT_BASE) return isBaseInput(rm, s);
        return isStandInput(rm, s);
    }

    public static final int LOCK_BASE = 4;

    public static Ingredient standIngredient(EmpowererRecipe r, int i) {
        return switch (i) { case 0 -> r.getStandOne(); case 1 -> r.getStandTwo(); case 2 -> r.getStandThree(); default -> r.getStandFour(); };
    }

    /** Lock check for a recorded lock (r == null: recipe missing on this side -> forbid, it will auto-unlock). */
    public static boolean lockAllows(@Nullable RecipeHolder<EmpowererRecipe> r, int[] map, int slot, ItemStack s) {
        if (r == null || slot < 0 || slot >= map.length) return false;
        int m = map[slot];
        if (m == LOCK_BASE) return r.value().getInput().test(s);
        if (m < 0 || m > 3) return false;
        return standIngredient(r.value(), m).test(s);
    }

    /** First (lexicographic) permutation p with stand ingredient p[k] matching stack k; null if none. */
    @Nullable
    static int[] assign(EmpowererRecipe r, ItemStack[] st) {
        int[] p = new int[4];
        return perm(r, st, p, 0, new boolean[4]) ? p : null;
    }

    private static boolean perm(EmpowererRecipe r, ItemStack[] st, int[] p, int k, boolean[] used) {
        if (k == 4) return true;
        for (int i = 0; i < 4; i++) {
            if (used[i] || !standIngredient(r, i).test(st[k])) continue;
            used[i] = true; p[k] = i;
            if (perm(r, st, p, k + 1, used)) return true;
            used[i] = false;
        }
        return false;
    }

    @Nullable
    public static RecipeHolder<EmpowererRecipe> byId(@Nullable RecipeManager rm, @Nullable net.minecraft.resources.ResourceLocation id) {
        if (rm == null || id == null) return null;
        for (RecipeHolder<EmpowererRecipe> r : rm.getAllRecipesFor(ActuallyRecipes.Types.EMPOWERING.get()))
            if (r.id().equals(id)) return r;
        return null;
    }

    /** Empowering recipes sorted by id: deterministic index used to sync the locked recipe to the client. */
    public static java.util.List<RecipeHolder<EmpowererRecipe>> sortedRecipes(@Nullable RecipeManager rm) {
        if (rm == null) return java.util.List.of();
        var l = new java.util.ArrayList<>(rm.getAllRecipesFor(ActuallyRecipes.Types.EMPOWERING.get()));
        l.sort(java.util.Comparator.comparing(h -> h.id().toString()));
        return l;
    }

    @Nullable
    private static RecipeManager serverRecipes() {
        var srv = ServerLifecycleHooks.getCurrentServer();
        return srv == null ? null : srv.getRecipeManager();
    }

    /** Fixed Speed multipliers for this structure (highest installed tier counts; tiers need the lower ones). */
    public static double speedMultiplier(com.aamoreadditions.upgrade.UpgradeSlots.Handler h) {
        double m = 1.0;
        for (var t : h.activeUpgrades()) if (t.line == com.aamoreadditions.upgrade.UpgradeType.Line.SPEED)
            m = switch (t.tier) { case 1 -> 1.5; case 2 -> 2.0; case 3 -> 3.0; default -> 1.0; };
        return m;
    }

    public final class Entry implements com.aamoreadditions.upgrade.SpeedOnlyHolder {
        public final com.aamoreadditions.upgrade.UpgradeSlots.Handler upgrades = new com.aamoreadditions.upgrade.UpgradeSlots.Handler(this);
        @Override public void upgradesChanged() { setDirty(); }
        public final BlockPos anchor;
        public final ItemStackHandler inv = new ItemStackHandler(CasingCrossData.SLOTS) {
            @Override protected void onContentsChanged(int slot) { recipeDirty = true; setDirty(); }
            @Override public boolean isItemValid(int slot, ItemStack stack) {
                RecipeManager rm = serverRecipes();
                return validFor(rm, slot, stack) && lockAllows(rm, slot, stack);
            }
        };
        public final Energy energy = new Energy();
        public int processTime;
        /** Recipe time of the current match (0 = none), for the GUI progress bar. */
        public int recipeTime;
        boolean recipeDirty = true;
        RecipeManager cachedFor;
        RecipeHolder<EmpowererRecipe> cached;
        Entry(BlockPos anchor) { this.anchor = anchor.immutable(); }

        // ---- recipe lock (same rules as the Alloy Furnace) ----
        /** Locked flag; recorded recipe id (null = pending); per input slot 0..4: stand ingredient 0..3 (standOne..Four), LOCK_BASE for the base input. */
        public boolean locked;
        public net.minecraft.resources.ResourceLocation lockId;
        public final int[] lockMap = {-1, -1, -1, -1, -1};
        public boolean lockRecorded() { return locked && lockId != null; }

        public void toggleLock() {
            boolean was = locked;
            unlock();
            locked = !was;
            setDirty();
        }

        void unlock() { locked = false; lockId = null; java.util.Arrays.fill(lockMap, -1); }

        public boolean lockAllows(@Nullable RecipeManager rm, int slot, ItemStack s) {
            if (!lockRecorded() || slot == SLOT_OUTPUT) return true;
            if (rm == null) return true;
            return CasingCrossData.lockAllows(byId(rm, lockId), lockMap, slot, s);
        }

        /** Record on the first successful run while pending (called before consuming). */
        void recordIfPending(RecipeHolder<EmpowererRecipe> h) {
            if (!locked || lockId != null) return;
            ItemStack[] st = new ItemStack[4];
            for (int i = 0; i < 4; i++) st[i] = inv.getStackInSlot(AA_STAND_ORDER[i]);
            int[] perm = assign(h.value(), st);
            if (perm == null) return;
            lockId = h.id();
            lockMap[SLOT_BASE] = LOCK_BASE;
            for (int i = 0; i < 4; i++) lockMap[AA_STAND_ORDER[i]] = perm[i];
            setDirty();
        }

        /** Pass 1: top up slots already holding the same item, in priority order. Pass 2: empty slots, in priority order. Returns the remainder. */
        public ItemStack insertByPriority(ItemStack stack, boolean simulate) {
            if (stack.isEmpty()) return ItemStack.EMPTY;
            ItemStack rest = stack.copy();
            for (int slot : INSERT_PRIORITY) {
                ItemStack cur = inv.getStackInSlot(slot);
                if (!cur.isEmpty() && ItemStack.isSameItemSameComponents(cur, rest)) {
                    rest = inv.insertItem(slot, rest, simulate);
                    if (rest.isEmpty()) return ItemStack.EMPTY;
                }
            }
            for (int slot : INSERT_PRIORITY) {
                if (!inv.getStackInSlot(slot).isEmpty()) continue;
                rest = inv.insertItem(slot, rest, simulate);
                if (rest.isEmpty()) return ItemStack.EMPTY;
            }
            return rest;
        }

        /** Outside view for hoppers/pipes: insert follows the priority rule, only the output can be extracted. */
        public final net.neoforged.neoforge.items.IItemHandler external = new net.neoforged.neoforge.items.IItemHandler() {
            @Override public int getSlots() { return CasingCrossData.SLOTS; }
            @Override public ItemStack getStackInSlot(int slot) { return inv.getStackInSlot(slot); }
            @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                // Only slot 0 takes items, and it applies the priority rule; the other slots refuse so that a pipe iterating slots cannot bypass the order.
                return slot == 0 ? insertByPriority(stack, simulate) : stack;
            }
            @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return slot == SLOT_OUTPUT ? inv.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
            }
            @Override public int getSlotLimit(int slot) { return inv.getSlotLimit(slot); }
            @Override public boolean isItemValid(int slot, ItemStack stack) {
                return slot == 0 && insertByPriority(stack, true).getCount() < stack.getCount();
            }
        };

        RecipeHolder<EmpowererRecipe> recipe(RecipeManager rm) {
            if (recipeDirty || cachedFor != rm) { // re-scan only after a slot change or a /reload
                recipeDirty = false;
                cachedFor = rm;
                cached = null;
                ItemStack base = inv.getStackInSlot(SLOT_BASE);
                if (!base.isEmpty()) {
                    ItemStack[] st = new ItemStack[4];
                    for (int i = 0; i < 4; i++) st[i] = inv.getStackInSlot(AA_STAND_ORDER[i]);
                    for (RecipeHolder<EmpowererRecipe> r : rm.getAllRecipesFor(ActuallyRecipes.Types.EMPOWERING.get()))
                        if (r.value().matches(base, st[0], st[1], st[2], st[3])) { cached = r; break; }
                }
            }
            return cached;
        }

        private boolean outputFits(ItemStack out) {
            ItemStack cur = inv.getStackInSlot(SLOT_OUTPUT);
            if (out.isEmpty()) return true;
            if (cur.isEmpty()) return out.getCount() <= inv.getSlotLimit(SLOT_OUTPUT);
            return ItemStack.isSameItemSameComponents(cur, out)
                    && cur.getCount() + out.getCount() <= Math.min(cur.getMaxStackSize(), inv.getSlotLimit(SLOT_OUTPUT));
        }

        /** Same logic as AA's TileEntityEmpowerer#serverTick, energy taken from the shared pool (4 stands' worth). */
        /** True if progress advanced during the last tick (drives the working sound). */
        public boolean running;

        void tick(ServerLevel level, RecipeManager rm) {
            running = false;
            if (lockRecorded() && level.getGameTime() % 20 == 0 && byId(rm, lockId) == null) { unlock(); setDirty(); }
            RecipeHolder<EmpowererRecipe> h = recipe(rm);
            if (h == null) { reset(); return; }
            EmpowererRecipe r = h.value();
            int baseTime = Math.max(1, r.getTime()); // guard: AA would divide by zero
            int time = com.aamoreadditions.effect.UpgradeMath.time(baseTime, speedMultiplier(upgrades)); // speed upgrades: fewer ticks
            recipeTime = time;
            if (!outputFits(r.getOutput())) return; // wait, keep progress
            // Total per recipe stays as without upgrades (4 * (energyPerStand / baseTime) * baseTime, AA's own integer math);
            // spread over the (shorter) time so the per-tick drain rises by ~the speed multiplier. Exact split: tick p charges
            // floor(total*(p+1)/time) - floor(total*p/time), so the ticks sum to exactly `total` (no over/under-charge).
            long total = 4L * (r.getEnergyPerStand() / baseTime) * baseTime;
            int p0 = Math.min(processTime, time - 1);
            long perTick = total * (p0 + 1) / time - total * p0 / time;
            if (perTick > Integer.MAX_VALUE || energy.getEnergyStored() < perTick) return; // not enough: pause, like AA
            energy.extractInternal((int) perTick);
            processTime++;
            running = true;
            setDirty();
            BlockPos p = anchor;
            if (processTime % 5 == 0)
                level.sendParticles(ParticleTypes.FIREWORK, p.getX() + 0.5, p.getY() + 1.1, p.getZ() + 0.5, 2, 0, 0, 0, 0.1D);
            if (processTime >= time) {
                recordIfPending(h);
                for (int s : AA_STAND_ORDER) inv.extractItem(s, 1, false);
                inv.extractItem(SLOT_BASE, 1, false);
                ItemStack out = r.getOutput().copy();
                ItemStack cur = inv.getStackInSlot(SLOT_OUTPUT);
                if (cur.isEmpty()) inv.setStackInSlot(SLOT_OUTPUT, out);
                else if (!out.isEmpty()) inv.setStackInSlot(SLOT_OUTPUT, cur.copyWithCount(cur.getCount() + out.getCount()));
                processTime = 0;
            }
        }

        private void reset() {
            if (processTime != 0) { processTime = 0; setDirty(); }
            recipeTime = 0;
        }
    }

    /** Plain FE pool: external insert unlimited per call (only free space limits it), extraction internal only. */
    public final class Energy extends EnergyStorage {
        Energy() { super(CasingCrossEnergy.capacity(), Integer.MAX_VALUE, 0); }
        @Override public int receiveEnergy(int max, boolean sim) {
            int r = super.receiveEnergy(max, sim);
            if (r > 0 && !sim) setDirty();
            return r;
        }
        void extractInternal(int amount) { this.energy = Math.max(0, this.energy - amount); setDirty(); }
        /** Clamped, so saves made with an older/bigger capacity still load. */
        public void set(int e) { this.energy = Math.max(0, Math.min(capacity, e)); }
    }

    private final Map<Long, Entry> byAnchor = new HashMap<>();
    private final Map<Long, Long> cellToAnchor = new HashMap<>();

    public static CasingCrossData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(CasingCrossData::new, CasingCrossData::load, null), NAME);
    }

    public Entry entry(BlockPos anchor) { return byAnchor.get(anchor.asLong()); }

    /** Anchor of the active structure that owns this cell, or null. */
    public BlockPos anchorOf(BlockPos cell) {
        Long a = cellToAnchor.get(cell.asLong());
        return a == null ? null : BlockPos.of(a);
    }

    public Entry entryAt(BlockPos cell) {
        BlockPos a = anchorOf(cell);
        return a == null ? null : entry(a);
    }

    /** True when none of the cells of a structure at this anchor is already owned by another active structure. */
    public boolean cellsFree(BlockPos anchor) {
        for (var p : CasingCrossStructure.PARTS) if (cellToAnchor.containsKey(anchor.offset(p.offset()).asLong())) return false;
        return true;
    }

    public Entry activate(BlockPos anchor) {
        Entry e = new Entry(anchor);
        put(e);
        setDirty();
        return e;
    }

    private void put(Entry e) {
        byAnchor.put(e.anchor.asLong(), e);
        for (var p : CasingCrossStructure.PARTS) cellToAnchor.put(e.anchor.offset(p.offset()).asLong(), e.anchor.asLong());
    }

    public Entry remove(BlockPos anchor) {
        Entry e = byAnchor.remove(anchor.asLong());
        if (e != null) {
            for (var p : CasingCrossStructure.PARTS) cellToAnchor.remove(anchor.offset(p.offset()).asLong(), anchor.asLong());
            setDirty();
        }
        return e;
    }

    /** Processing for active structures whose Empowerer chunk is ticking (never force-loads). */
    void tick(ServerLevel level) {
        if (byAnchor.isEmpty() && soundSent.isEmpty()) return;
        RecipeManager rm = level.getRecipeManager();
        for (Entry e : byAnchor.values()) {
            if (level.shouldTickBlocksAt(e.anchor)) e.tick(level, rm);
            else e.running = false;
            CasingCrossDisplay.sync(level, e.anchor, false);
        }
        syncSound(level);
    }

    /** Anchors whose last sent working flag was true. */
    private final java.util.Set<Long> soundSent = new java.util.HashSet<>();

    /** Sends the working flag on change, a refresh every 10 ticks while running, and an explicit stop for removed structures. */
    private void syncSound(ServerLevel level) {
        boolean refresh = level.getGameTime() % 10 == 0;
        for (Entry e : byAnchor.values()) {
            long k = e.anchor.asLong();
            boolean was = soundSent.contains(k);
            if (e.running && (!was || refresh)) { send(level, e.anchor, true); soundSent.add(k); }
            else if (!e.running && was) { send(level, e.anchor, false); soundSent.remove(k); }
        }
        for (var it = soundSent.iterator(); it.hasNext(); ) {
            long k = it.next();
            if (!byAnchor.containsKey(k)) { send(level, BlockPos.of(k), false); it.remove(); }
        }
    }

    private static void send(ServerLevel level, BlockPos pos, boolean on) {
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingChunk(level, new net.minecraft.world.level.ChunkPos(pos),
                new com.aamoreadditions.network.ModNetwork.EmpowererSoundPayload(pos, on));
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider reg) {
        ListTag list = new ListTag();
        for (Entry e : byAnchor.values()) {
            CompoundTag t = new CompoundTag();
            t.putLong("anchor", e.anchor.asLong());
            t.put("inv", e.inv.serializeNBT(reg));
            t.putInt("energy", e.energy.getEnergyStored());
            t.putInt("processTime", e.processTime);
            t.putInt("layout", 2);
            t.put("upgrades", e.upgrades.serializeNBT(reg));
            t.putBoolean("lockLocked", e.locked);
            t.putString("lockRecipe", e.lockId == null ? "" : e.lockId.toString());
            t.putIntArray("lockMap", e.lockMap.clone());
            list.add(t);
        }
        tag.put("structures", list);
        return tag;
    }

    public static CasingCrossData load(CompoundTag tag, HolderLookup.Provider reg) {
        CasingCrossData d = new CasingCrossData();
        ListTag list = tag.getList("structures", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            BlockPos a = BlockPos.of(t.getLong("anchor"));
            if (!d.cellsFree(a)) continue;
            Entry e = d.new Entry(a);
            CompoundTag invTag = t.getCompound("inv").copy();
            // old layout had 5 slots (0..3 inputs, 4 centre): same indices, slot 5 simply empty.
            // ItemStackHandler.deserializeNBT resizes to the saved "Size", so force the new size first.
            invTag.putInt("Size", SLOTS);
            e.inv.deserializeNBT(reg, invTag);
            e.energy.set(t.getInt("energy"));
            e.processTime = Math.max(0, t.getInt("processTime"));
            if (t.contains("upgrades")) e.upgrades.deserializeNBT(reg, t.getCompound("upgrades"));
            // lock (absent in old saves -> unlocked)
            if (t.getBoolean("lockLocked")) {
                e.locked = true;
                var id = net.minecraft.resources.ResourceLocation.tryParse(t.getString("lockRecipe"));
                int[] m = t.getIntArray("lockMap");
                if (id != null && !t.getString("lockRecipe").isEmpty()) {
                    if (m.length == 5) { e.lockId = id; System.arraycopy(m, 0, e.lockMap, 0, 5); }
                    else e.unlock();
                }
            }
            d.put(e);
        }
        return d;
    }
}
