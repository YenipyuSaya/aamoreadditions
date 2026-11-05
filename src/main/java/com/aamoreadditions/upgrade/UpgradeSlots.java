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
 * Five upgrade slots stored as a NeoForge data attachment on supported AA block entities.
 * Separate from AA's own inventory, so AA slot indices and automation are untouched,
 * and not exposed as an item capability (hoppers/pipes can't reach it).
 */
public final class UpgradeSlots {
    public static final int SLOT_COUNT = 5;
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

    /** Energizer: 2 slots; all others SLOT_COUNT. */
    public static int slotCount(Object holder) {
        if (holder instanceof SpeedOnlyHolder) return SpeedOnlyHolder.UPGRADE_SLOTS;
        return holder instanceof de.ellpeck.actuallyadditions.mod.tile.TileEntityEnergizer ? 2 : SLOT_COUNT;
    }

    /** Energizer accepts only the efficiency line. */
    public static boolean allows(Object holder, UpgradeType t) {
        if (holder instanceof SpeedOnlyHolder) return t != null && t.line == UpgradeType.Line.SPEED;
        return !(holder instanceof de.ellpeck.actuallyadditions.mod.tile.TileEntityEnergizer) || (t != null && t.line == UpgradeType.Line.EFFICIENCY);
    }

    public static Handler get(BlockEntity be) {
        return be.getData(UPGRADE_INV);
    }

    public static void drop(Level level, BlockEntity be) {
        if (level.isClientSide || !be.hasData(UPGRADE_INV)) return;
        Handler h = be.getData(UPGRADE_INV);
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

        private java.util.Set<UpgradeType> activeCache;
        private final int baseCapacity;
        private final int baseMaxReceive;
        private final int baseMaxExtract;

        private void refresh() {
            activeCache = null;
            if (holder instanceof BlockEntity be) com.aamoreadditions.effect.UpgradeEffects.applyCapacity(be, com.aamoreadditions.effect.UpgradeConfig.baseCapacity(be, baseCapacity), activeUpgrades());
            if (holder instanceof BlockEntity be2) com.aamoreadditions.effect.UpgradeEffects.applyInputRate(be2, com.aamoreadditions.effect.UpgradeConfig.baseMaxReceive(be2, baseMaxReceive), activeUpgrades());
            if (holder instanceof BlockEntity be3) com.aamoreadditions.effect.UpgradeEffects.applyOutputRate(be3, com.aamoreadditions.effect.UpgradeConfig.baseMaxExtract(com.aamoreadditions.effect.MachineKind.of(be3), baseMaxExtract), activeUpgrades());
        }

        /** Highest installed tier per line (cached; invalidated when slots change or load). */
        public java.util.Set<UpgradeType> activeUpgrades() {
            java.util.Set<UpgradeType> c = activeCache;
            if (c == null) {
                java.util.Map<UpgradeType.Line, UpgradeType> best = new java.util.EnumMap<>(UpgradeType.Line.class);
                for (ItemStack s : stacks) {
                    UpgradeType t = UpgradeType.of(s);
                    if (t != null) best.merge(t.line, t, (a, b) -> a.tier >= b.tier ? a : b);
                }
                c = best.isEmpty() ? java.util.EnumSet.noneOf(UpgradeType.class) : java.util.EnumSet.copyOf(best.values());
                activeCache = c;
            }
            return c;
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

        /** Old saves stored "Size":2; ItemStackHandler would shrink to that, so grow back to SLOT_COUNT keeping items. */
        @Override
        public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
            super.deserializeNBT(provider, nbt);
            activeCache = null; // capacity applied after the stack list is resized below
            int want = slotCount(holder);
            if (stacks.size() != want) {
                NonNullList<ItemStack> grown = NonNullList.withSize(want, ItemStack.EMPTY);
                for (int i = 0; i < Math.min(want, stacks.size()); i++) grown.set(i, stacks.get(i));
                stacks = grown;
            }
            refresh();
        }

        @Override
        protected void onContentsChanged(int slot) {
            refresh();
            if (holder instanceof BlockEntity be) be.setChanged();
            if (holder instanceof SpeedOnlyHolder h) h.upgradesChanged();
        }
    }
}
