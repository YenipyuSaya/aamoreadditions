package com.aamoreadditions.mixin;

import com.aamoreadditions.structure.CasingCrossDisplay;
import de.ellpeck.actuallyadditions.mod.crafting.EmpowererRecipe;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityBase;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityInventoryBase;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

/** Extra SYNC-only tag, NOT AA's Items tag: no native inventory mirroring, persistence or drops. */
@Mixin(value = TileEntityInventoryBase.class, remap = false)
public abstract class TileEntityStructureDisplayMixin implements CasingCrossDisplay.View {
    @Unique @Nullable private ItemStack aamoreadditions$display;
    @Unique @Nullable private ResourceLocation aamoreadditions$laserId;
    @Unique @Nullable private RecipeHolder<EmpowererRecipe> aamoreadditions$laserRecipe;
    @Unique private long aamoreadditions$laserExpiresAt;

    @Override
    @Nullable
    public ItemStack aamoreadditions$getDisplay() { return aamoreadditions$display; }

    @Override
    public void aamoreadditions$setDisplay(@Nullable ItemStack stack) {
        aamoreadditions$display = stack == null ? null : stack.copy();
    }

    @Override @Nullable
    public ResourceLocation aamoreadditions$getLaserId() { return aamoreadditions$laserId; }

    @Override @Nullable
    public RecipeHolder<EmpowererRecipe> aamoreadditions$getLaserRecipe() { return aamoreadditions$laserRecipe; }

    @Override
    public long aamoreadditions$getLaserExpiresAt() { return aamoreadditions$laserExpiresAt; }

    @Override
    public void aamoreadditions$setLaser(@Nullable ResourceLocation recipeId, long expiresAt) {
        aamoreadditions$laserId = recipeId;
        aamoreadditions$laserExpiresAt = expiresAt;
        TileEntityInventoryBase self = (TileEntityInventoryBase) (Object) this;
        aamoreadditions$laserRecipe = recipeId != null && self.getLevel() != null && self.getLevel().isClientSide
                ? com.aamoreadditions.structure.CasingCrossData.byId(self.getLevel().getRecipeManager(), recipeId) : null;
    }

    @Inject(method = "writeSyncableNBT", at = @At("TAIL"))
    private void aamoreadditions$writeDisplay(CompoundTag tag, HolderLookup.Provider registries,
                                            TileEntityBase.NBTType type, CallbackInfo ci) {
        if (type != TileEntityBase.NBTType.SYNC) return;
        ItemStack stack = CasingCrossDisplay.serverStack((TileEntityInventoryBase) (Object) this);
        if (stack != null) tag.put(CasingCrossDisplay.SYNC_KEY, stack.saveOptional(registries));
        ResourceLocation laser = CasingCrossDisplay.serverLaser((TileEntityInventoryBase) (Object) this);
        if (laser != null) tag.putString(CasingCrossDisplay.LASER_KEY, laser.toString());
    }

    @Inject(method = "readSyncableNBT", at = @At("TAIL"))
    private void aamoreadditions$readDisplay(CompoundTag tag, HolderLookup.Provider registries,
                                           TileEntityBase.NBTType type, CallbackInfo ci) {
        TileEntityInventoryBase self = (TileEntityInventoryBase) (Object) this;
        if (type != TileEntityBase.NBTType.SYNC || self.getLevel() == null || !self.getLevel().isClientSide) return;
        // Missing tag explicitly restores native AA display after structure deactivation.
        aamoreadditions$setDisplay(tag.contains(CasingCrossDisplay.SYNC_KEY)
                ? ItemStack.parseOptional(registries, tag.getCompound(CasingCrossDisplay.SYNC_KEY)) : null);
        ResourceLocation laser = tag.contains(CasingCrossDisplay.SYNC_KEY)
                ? ResourceLocation.tryParse(tag.getString(CasingCrossDisplay.LASER_KEY)) : null;
        aamoreadditions$setLaser(laser, self.getLevel().getGameTime() + CasingCrossDisplay.LASER_LIFETIME);
    }
}
