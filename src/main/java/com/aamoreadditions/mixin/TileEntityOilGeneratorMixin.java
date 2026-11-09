package com.aamoreadditions.mixin;

import com.aamoreadditions.effect.UpgradeEffects;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityOilGenerator;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Oil generator speed upgrades. When AA starts a new fuel portion it writes the recipe's values to
 * currentEnergyProduce (totalEnergy / burnTime) and maxBurnTime; both writes are scaled:
 * produce -> max(1, round(original * S)), burn -> max(1, round(original / S * U)). Writes of 0 (no fuel) stay 0.
 * The GUI reads the same fields, so "x FE/t for y t" stays correct.
 */
@Mixin(value = TileEntityOilGenerator.class, remap = false)
public abstract class TileEntityOilGeneratorMixin {
    private static final String TICK = "serverTick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/BlockEntity;)V";

    @WrapOperation(method = TICK, require = 0, at = @At(value = "FIELD",
            target = "Lde/ellpeck/actuallyadditions/mod/tile/TileEntityOilGenerator;currentEnergyProduce:I", opcode = Opcodes.PUTFIELD))
    private static void aamoreadditions$produce(TileEntityOilGenerator tile, int value, Operation<Void> op) {
        op.call(tile, value <= 0 ? value : UpgradeEffects.generatorOutput(tile, value));
    }

    @WrapOperation(method = TICK, require = 0, at = @At(value = "FIELD",
            target = "Lde/ellpeck/actuallyadditions/mod/tile/TileEntityOilGenerator;maxBurnTime:I", opcode = Opcodes.PUTFIELD))
    private static void aamoreadditions$burnTime(TileEntityOilGenerator tile, int value, Operation<Void> op) {
        op.call(tile, value <= 0 ? value : UpgradeEffects.generatorBurnTime(tile, value));
    }

    /** Gate only the original decrement write, never the base tick or later sync/FE sharing.
     * The first currentBurnTime write is the decrement, verified against AA 1.3.26 bytecode. */
    @WrapOperation(method=TICK,at=@At(value="FIELD",
            target="Lde/ellpeck/actuallyadditions/mod/tile/TileEntityOilGenerator;currentBurnTime:I",opcode=Opcodes.PUTFIELD,ordinal=0))
    private static void aamoreadditions$pauseFullBurn(TileEntityOilGenerator tile,int value,Operation<Void> original){
        if(com.aamoreadditions.effect.OilBurnPause.mayAdvance(tile.storage.getEnergyStored(),tile.storage.getMaxEnergyStored()))
            original.call(tile,value);
    }
    @WrapOperation(method=TICK,at=@At(value="INVOKE",
            target="Lde/ellpeck/actuallyadditions/mod/tile/CustomEnergyStorage;receiveEnergyInternal(IZ)I"))
    private static int aamoreadditions$pauseFullGeneration(de.ellpeck.actuallyadditions.mod.tile.CustomEnergyStorage storage,
            int amount,boolean simulate,Operation<Integer> original){
        return com.aamoreadditions.effect.OilBurnPause.mayAdvance(storage.getEnergyStored(),storage.getMaxEnergyStored())
                ?original.call(storage,amount,simulate):0;
    }

    // ---- base value overrides: AA reads these recipe getters in serverTick; replace with our configured values ----
    @WrapOperation(method = TICK, require = 0, at = @At(value = "INVOKE", target = "Lde/ellpeck/actuallyadditions/mod/crafting/LiquidFuelRecipe;getBurnTime()I"))
    private static int aamoreadditions$baseBurn(de.ellpeck.actuallyadditions.mod.crafting.LiquidFuelRecipe r, Operation<Integer> op) {
        return com.aamoreadditions.effect.UpgradeConfig.oil(r.getFuel(), 0, op.call(r));
    }

    @WrapOperation(method = TICK, require = 0, at = @At(value = "INVOKE", target = "Lde/ellpeck/actuallyadditions/mod/crafting/LiquidFuelRecipe;getTotalEnergy()I"))
    private static int aamoreadditions$baseTotal(de.ellpeck.actuallyadditions.mod.crafting.LiquidFuelRecipe r, Operation<Integer> op) {
        return com.aamoreadditions.effect.UpgradeConfig.oil(r.getFuel(), 1, op.call(r));
    }

    @WrapOperation(method = TICK, require = 0, at = @At(value = "INVOKE", target = "Lde/ellpeck/actuallyadditions/mod/crafting/LiquidFuelRecipe;getFuelAmount()I"))
    private static int aamoreadditions$baseFuel(de.ellpeck.actuallyadditions.mod.crafting.LiquidFuelRecipe r, Operation<Integer> op) {
        return com.aamoreadditions.effect.UpgradeConfig.oil(r.getFuel(), 2, op.call(r));
    }
}
