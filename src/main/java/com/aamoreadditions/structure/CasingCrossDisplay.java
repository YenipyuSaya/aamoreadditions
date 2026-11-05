package com.aamoreadditions.structure;

import de.ellpeck.actuallyadditions.mod.crafting.EmpowererRecipe;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityDisplayStand;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityEmpowerer;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityInventoryBase;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/** Read-only visual projection of the shared inputs. Never an inventory or a capability. */
public final class CasingCrossDisplay {
    public static final String SYNC_KEY = "aamoreadditions:structure_display";
    public static final String LASER_KEY = "aamoreadditions:structure_laser";
    public static final int LASER_LIFETIME = 30;

    private CasingCrossDisplay() {}

    /** Per-block-entity transient state: null = native AA display, EMPTY = active but empty input. */
    public interface View {
        @Nullable ItemStack aamoreadditions$getDisplay();
        void aamoreadditions$setDisplay(@Nullable ItemStack stack);
        @Nullable ResourceLocation aamoreadditions$getLaserId();
        @Nullable RecipeHolder<EmpowererRecipe> aamoreadditions$getLaserRecipe();
        long aamoreadditions$getLaserExpiresAt();
        void aamoreadditions$setLaser(@Nullable ResourceLocation recipeId, long expiresAt);
    }

    /** Absolute world compass, independent of the player or machine facing. Output is never mapped. */
    public static int slotAt(BlockPos anchor, BlockPos cell) {
        return CasingCrossDisplayLayout.slotAt(cell.getX() - anchor.getX(),
                cell.getY() - anchor.getY(), cell.getZ() - anchor.getZ());
    }

    /** Borrowed for reading/serialization ONLY; never mutate this authoritative stack. No handler lookup. */
    @Nullable
    public static ItemStack serverStack(TileEntityInventoryBase tile) {
        if (!(tile instanceof TileEntityDisplayStand) && !(tile instanceof TileEntityEmpowerer)) return null;
        if (!(tile.getLevel() instanceof ServerLevel level)) return null;
        CasingCrossData.Entry entry = CasingCrossData.get(level).entryAt(tile.getBlockPos());
        if (entry == null) return null;
        int slot = slotAt(entry.anchor, tile.getBlockPos());
        return slot < 0 ? null : entry.inv.getStackInSlot(slot);
    }

    @Nullable
    public static ResourceLocation serverLaser(TileEntityInventoryBase tile) {
        if (!(tile instanceof TileEntityEmpowerer) || !(tile.getLevel() instanceof ServerLevel level)) return null;
        CasingCrossData.Entry entry = CasingCrossData.get(level).entryAt(tile.getBlockPos());
        return entry != null && entry.running && entry.cached != null ? entry.cached.id() : null;
    }

    /** Native renderer still draws its four beams, but only for fresh, actually-running shared state. */
    @Nullable
    public static RecipeHolder<EmpowererRecipe> renderedRecipe(TileEntityEmpowerer tile,
            @Nullable RecipeHolder<EmpowererRecipe> nativeRecipe) {
        View view = (View) tile;
        if (view.aamoreadditions$getDisplay() == null) return nativeRecipe;
        var level = tile.getLevel();
        if (level == null || level.getGameTime() >= view.aamoreadditions$getLaserExpiresAt()
                || view.aamoreadditions$getLaserRecipe() == null) return null;
        // Stop immediately on partial client chunk unload or a broken structure, without loading anything.
        for (var part : CasingCrossStructure.PARTS)
            if (!level.hasChunkAt(tile.getBlockPos().offset(part.offset()))) return null;
        return CasingCrossStructure.matches(level, tile.getBlockPos()) ? view.aamoreadditions$getLaserRecipe() : null;
    }

    /** Called only by the two client renderers; all AA transforms, item components and animation survive. */
    public static ItemStack renderedStack(TileEntityInventoryBase tile, ItemStack nativeStack) {
        ItemStack display = ((View) tile).aamoreadditions$getDisplay();
        return display == null ? nativeStack : display;
    }

    /**
     * End-of-tick comparison catches GUI in-place stack mutations as well as automation/JEI/crafting.
     * Each cell sends to its OWN chunk watchers, including structures straddling chunk boundaries.
     * Chunk entry/re-entry uses AA's normal getUpdateTag, which reads shared data directly.
     */
    public static void sync(ServerLevel level, BlockPos anchor, boolean force) {
        for (var part : CasingCrossStructure.PARTS) {
            if (part.offset().getY() != 0) continue;
            BlockPos pos = anchor.offset(part.offset());
            if (!level.hasChunkAt(pos)) continue; // never load another chunk for visuals
            if (!(level.getBlockEntity(pos) instanceof TileEntityInventoryBase tile)) continue;
            if (!(tile instanceof TileEntityDisplayStand) && !(tile instanceof TileEntityEmpowerer)) continue;
            View view = (View) tile;
            ItemStack current = serverStack(tile);
            ItemStack previous = view.aamoreadditions$getDisplay();
            ResourceLocation laser = serverLaser(tile);
            boolean heartbeat = laser != null && level.getGameTime() % 10 == 0;
            if (force || heartbeat || !java.util.Objects.equals(laser, view.aamoreadditions$getLaserId())
                    || (current == null ? previous != null
                    : previous == null || !ItemStack.matches(current, previous))) {
                view.aamoreadditions$setDisplay(current); // setter takes a defensive snapshot, not a real slot
                view.aamoreadditions$setLaser(laser, 0);
                tile.sendUpdate();
            }
        }
    }
}
