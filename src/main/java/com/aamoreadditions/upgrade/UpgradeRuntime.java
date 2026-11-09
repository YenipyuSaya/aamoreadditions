package com.aamoreadditions.upgrade;

import de.ellpeck.actuallyadditions.mod.tile.*;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** One authoritative stop policy. Leave FE capability and active FE sharing operational;
 * never call the normal machine tick while its plugin logic is wrong. */
public final class UpgradeRuntime {
    private UpgradeRuntime() {}
    public static boolean blocked(BlockEntity be) {
        return be != null && UpgradeSlots.supports(be) && !UpgradeSlots.get(be).legal();
    }
    public static void clearWork(BlockEntity be) {
        if (!(be.getLevel() instanceof ServerLevel level)) return;
        if (be instanceof UpgradeResetState reset) reset.aamoreadditions$resetUpgradeWork();
        if (be instanceof TileEntityCrusher t) t.firstCrushTime = t.secondCrushTime = 0;
        if (be instanceof TileEntityPoweredFurnace t) t.firstSmeltTime = t.secondSmeltTime = 0;
        if (be instanceof TileEntityCoalGenerator t) t.currentBurnTime = t.maxBurnTime = 0;
        if (be instanceof TileEntityOilGenerator t) { t.currentBurnTime = t.maxBurnTime = 0; t.currentEnergyProduce = 0; }
        var state = be.getBlockState();
        if (state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT))
            level.setBlockAndUpdate(be.getBlockPos(), state.setValue(BlockStateProperties.LIT,false));
        be.setChanged();
    }
    public static boolean tick(BlockEntity be) {
        if (!(be.getLevel() instanceof ServerLevel level)) return false;
        if (be instanceof TileEntityEmpowerer || be instanceof TileEntityDisplayStand) {
            var entry = com.aamoreadditions.structure.CasingCrossData.get(level).entryAt(be.getBlockPos());
            if (entry != null) {
                // The shared structure owns processing, energy and displays. Native
                // server work must not run in parallel, even with old leftover inventory.
                if (be instanceof TileEntityEmpowerer empowerer && empowerer.processTime != 0) {
                    empowerer.processTime = 0; be.setChanged();
                }
                return true;
            }
        }
        if (!UpgradeSlots.supports(be)) return false;
        var upgrades = UpgradeSlots.get(be);
        upgrades.drainOverflow(level, be.getBlockPos());
        if (upgrades.legal()) return false;
        clearWork(be);
        // The normal AA base tick also shares FE. Preserve only that permitted interaction.
        if (be instanceof ISharingEnergyProvider provider && provider.doesShareEnergy()) {
            Direction[] sides = provider.getEnergyShareSides();
            if (sides != null && sides.length > 0) {
                int total = provider.getEnergyToSplitShare();
                int amount = Math.max(total / sides.length, total > 0 ? 1 : 0);
                for (Direction side : sides) {
                    if (!level.isLoaded(be.getBlockPos().relative(side))) continue;
                    var target = level.getBlockEntity(be.getBlockPos().relative(side));
                    if (target != null && provider.canShareTo(target))
                        de.ellpeck.actuallyadditions.mod.util.WorldUtil.doEnergyInteraction(level, be.getBlockPos(), target.getBlockPos(), side, amount);
                }
            }
        }
        be.setChanged();
        if (be instanceof TileEntityBase aa && level.getGameTime() % 5 == 0) aa.sendUpdate();
        return true;
    }
}
