package com.aamoreadditions.upgrade;

import com.aamoreadditions.AAMoreAdditions;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityCoalGenerator;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityCrusher;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityHeatCollector;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityLeafGenerator;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityOilGenerator;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityPoweredFurnace;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Containers;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/**
 * Configurable upgrade slots stored as a NeoForge data attachment on supported AA block entities.
 * Separate from AA's own inventory, so AA slot indices and automation are untouched,
 * and not exposed as an item capability (hoppers/pipes can't reach it).
 */
public final class UpgradeSlots {
    public static final TagKey<Item> UPGRADES = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(AAMoreAdditions.MODID, "upgrades"));

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, AAMoreAdditions.MODID);
    public static final Supplier<AttachmentType<Handler>> UPGRADE_INV = ATTACHMENTS.register("upgrades",
            () -> AttachmentType.serializable(Handler::new).build());

    private UpgradeSlots() {}

    /** Machines that get upgrade slots. Heat collector / leaf generator have no GUI: sneak-right-click only. */
    public static boolean supports(BlockEntity be) {
        return be instanceof TileEntityCrusher // includes TileEntityCrusherDouble
                || be instanceof TileEntityPoweredFurnace
                || be instanceof TileEntityCoalGenerator
                || be instanceof TileEntityOilGenerator
                || be instanceof TileEntityHeatCollector
                || be instanceof TileEntityLeafGenerator
                || be instanceof de.ellpeck.actuallyadditions.mod.tile.TileEntityEnergizer;
    }

    /** Counts come from the restart-frozen, server-authoritative startup snapshot. */
    public static int slotCount(Object holder) {
        return UpgradeStartup.slots(holder);
    }

    /** Energizer accepts only the efficiency line. */
    public static boolean allows(Object holder, UpgradeType t) {
        if (holder instanceof SpeedOnlyHolder) return t != null && t.line == UpgradeType.Line.SPEED;
        return !(holder instanceof de.ellpeck.actuallyadditions.mod.tile.TileEntityEnergizer) || (t != null && t.line == UpgradeType.Line.EFFICIENCY);
    }

    public static Handler get(BlockEntity be) {
        Handler h = be.getData(UPGRADE_INV);
        h.ensureSize();
        return h;
    }

    public static void drop(Level level, BlockEntity be) {
        if (level.isClientSide || !be.hasData(UPGRADE_INV)) return;
        Handler h = be.getData(UPGRADE_INV);
        if (level instanceof net.minecraft.server.level.ServerLevel sl) h.drainOverflow(sl, be.getBlockPos());
        for (int i = 0; i < h.getSlots(); i++) {
            ItemStack s = h.getStackInSlot(i);
            if (!s.isEmpty()) {
                Containers.dropItemStack(level, be.getBlockPos().getX() + 0.5, be.getBlockPos().getY() + 0.5, be.getBlockPos().getZ() + 0.5, s.copy());
                h.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
    }

    public static class Handler extends ItemStackHandler {
        private final Object holder;

        public Handler(Object holder) {
            super(slotCount(holder));
            this.holder = holder;
            // Capture AA's original capacity before any upgrade changes it.
            var st = holder instanceof BlockEntity be ? com.aamoreadditions.effect.UpgradeEffects.storage(be) : null;
            var base = st instanceof com.aamoreadditions.effect.BaseEnergyValues b ? b : null;
            this.baseCapacity = base != null ? base.aamoreadditions$baseCapacity() : -1;
            this.baseMaxExtract = base != null ? base.aamoreadditions$baseMaxExtract() : -1;
            this.baseMaxReceive = base != null ? base.aamoreadditions$baseMaxReceive() : -1;
            refresh(); // apply configured base caps even with no upgrades installed
        }

        private java.util.List<UpgradeType> activeCache;
        private final int baseCapacity;
        private final int baseMaxReceive;
        private final int baseMaxExtract;

        private boolean effectDependency;
        private void refresh() {
            effectDependency = UpgradeStartup.dependency();
            activeCache = null;
            if (holder instanceof BlockEntity be) com.aamoreadditions.effect.UpgradeEffects.applyCapacity(be, com.aamoreadditions.effect.UpgradeConfig.baseCapacity(be, baseCapacity), activeUpgrades());
            if (holder instanceof BlockEntity be2) com.aamoreadditions.effect.UpgradeEffects.applyInputRate(be2, com.aamoreadditions.effect.UpgradeConfig.baseMaxReceive(be2, baseMaxReceive), activeUpgrades());
            if (holder instanceof BlockEntity be3) com.aamoreadditions.effect.UpgradeEffects.applyOutputRate(be3, com.aamoreadditions.effect.UpgradeConfig.baseMaxExtract(com.aamoreadditions.effect.MachineKind.of(be3), baseMaxExtract), activeUpgrades());
        }

        /** All installed plugins in independent mode, otherwise highest tier per line; cached until refresh. */
        public java.util.List<UpgradeType> activeUpgrades() {
            if (activeCache == null) {
                var installed = stacks.stream().map(UpgradeType::of).filter(java.util.Objects::nonNull).toList();
                activeCache = UpgradeLogic.active(installed,UpgradeStartup.dependency(),t->t.line.name(),t->t.tier);
            }
            return activeCache;
        }

        private void ensureSize() {
            int want = slotCount(holder);
            if (stacks.size() == want) {
                if (effectDependency != UpgradeStartup.dependency()) refresh();
                return;
            }
            var resized = UpgradeLogic.resize(stacks, want, ItemStack.EMPTY);
            for (ItemStack s : resized.overflow()) if (!s.isEmpty()) overflow.add(s.copy());
            stacks = NonNullList.withSize(want, ItemStack.EMPTY);
            for (int i=0;i<want;i++) stacks.set(i,resized.kept().get(i));
            refresh(); dirty();
        }
        private final java.util.List<ItemStack> overflow = new java.util.ArrayList<>();
        public boolean legal() { return UpgradeRules.valid(this); }
        public void drainOverflow(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos) {
            if (!level.hasChunkAt(pos)) return;
            for (var it = overflow.iterator(); it.hasNext();) {
                ItemStack stack = it.next();
                var entity = new net.minecraft.world.entity.item.ItemEntity(level, pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, stack.copy());
                if (level.addFreshEntity(entity)) { it.remove(); dirty(); }
            }
        }
        public void dirty() {
            if (holder instanceof BlockEntity be) be.setChanged();
            if (holder instanceof SpeedOnlyHolder h) h.upgradesChanged();
        }
        @Override public CompoundTag serializeNBT(HolderLookup.Provider provider) {
            CompoundTag tag = super.serializeNBT(provider);
            var list = new net.minecraft.nbt.ListTag();
            for (ItemStack stack : overflow) if (!stack.isEmpty()) list.add(stack.save(provider));
            tag.put("PendingUpgradeDrops", list);
            return tag;
        }

        /** Live view of the slot contents (for UpgradeRules). */
        public Object holder() {
            return holder;
        }

        public java.util.List<ItemStack> contents() {
            return stacks;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.is(UPGRADES) && allows(holder, UpgradeType.of(stack));
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        /** Resize without dropping any stack on the floor of an unloaded chunk; save excess as a pending queue. */
        @Override
        public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
            super.deserializeNBT(provider, nbt);
            overflow.clear();
            for (var value : nbt.getList("PendingUpgradeDrops", 10)) {
                var stack = ItemStack.parseOptional(provider, (CompoundTag) value);
                if (!stack.isEmpty()) overflow.add(stack);
            }
            var resized = UpgradeLogic.resize(stacks, slotCount(holder), ItemStack.EMPTY);
            for (ItemStack stack : resized.overflow()) if (!stack.isEmpty()) overflow.add(stack.copy());
            stacks = NonNullList.withSize(resized.kept().size(), ItemStack.EMPTY);
            for (int i = 0; i < stacks.size(); i++) stacks.set(i, resized.kept().get(i));
            activeCache = null;
            refresh();
        }

        /** Commit a complete, validated wrench transaction with just one effects refresh.
         * In particular, a legal reorder must not temporarily shrink the energy capacity.
         */
        public void replaceContents(java.util.List<ItemStack> contents) {
            if (contents.size() != stacks.size()) throw new IllegalArgumentException("Wrong upgrade slot count");
            for (int i = 0; i < stacks.size(); i++) stacks.set(i, contents.get(i).copy());
            onContentsChanged(0);
        }

        @Override
        protected void onContentsChanged(int slot) {
            refresh();
            if (holder instanceof BlockEntity be && !legal()) UpgradeRuntime.clearWork(be);
            if (holder instanceof BlockEntity be) be.setChanged();
            if (holder instanceof SpeedOnlyHolder h) h.upgradesChanged();
        }
    }
}
