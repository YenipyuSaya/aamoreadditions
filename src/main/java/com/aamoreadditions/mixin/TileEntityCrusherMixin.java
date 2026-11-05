package com.aamoreadditions.mixin;

import com.aamoreadditions.effect.UpgradeEffects;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityCrusher;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Slice;

/**
 * Applies upgrade multipliers on top of AA's current values (nothing in AA is changed):
 *  - getMaxCrushTime() return value -> also used by serverTick and getFirst/SecondTimeToScale (progress bar);
 *  - extractEnergyInternal(ENERGY_USE, ..) argument -> modified per-tick use;
 *  - the "storage >= ENERGY_USE" checks: getEnergyStored() is reduced by (modified - original), which is
 *    exactly equivalent to comparing against the modified value. Sliced to stop before the sync code (lastEnergy).
 * ENERGY_USE is a compile-time constant inlined in AA's bytecode, so it's read reflectively from AA's class.
 */
@Mixin(value = TileEntityCrusher.class, remap = false)
public abstract class TileEntityCrusherMixin {
    @Unique
    private static final int AAMACHINEUPGRADE$BASE_ENERGY = readEnergyUse();

    @Unique
    private static int readEnergyUse() {
        try {
            return TileEntityCrusher.class.getField("ENERGY_USE").getInt(null);
        } catch (ReflectiveOperationException e) {
            return 40;
        }
    }

    @ModifyReturnValue(method = "getMaxCrushTime", at = @At("RETURN"))
    private int aamoreadditions$time(int original) {
        return UpgradeEffects.time((BlockEntity) (Object) this, original);
    }

    @ModifyArg(method = "serverTick()V", at = @At(value = "INVOKE",
            target = "Lde/ellpeck/actuallyadditions/mod/tile/CustomEnergyStorage;extractEnergyInternal(IZ)I"), index = 0)
    private int aamoreadditions$energyUse(int original) {
        return UpgradeEffects.energyPerTick((BlockEntity) (Object) this, original);
    }

    @ModifyExpressionValue(method = "serverTick()V",
            at = @At(value = "INVOKE", target = "Lde/ellpeck/actuallyadditions/mod/tile/CustomEnergyStorage;getEnergyStored()I"),
            slice = @Slice(to = @At(value = "FIELD", target = "Lde/ellpeck/actuallyadditions/mod/tile/TileEntityCrusher;lastEnergy:I", opcode = Opcodes.GETFIELD)))
    private int aamoreadditions$energyCheck(int stored) {
        int modified = UpgradeEffects.energyPerTick((BlockEntity) (Object) this, AAMACHINEUPGRADE$BASE_ENERGY);
        return stored - (modified - AAMACHINEUPGRADE$BASE_ENERGY);
    }
}
