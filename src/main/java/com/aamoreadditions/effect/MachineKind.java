package com.aamoreadditions.effect;

import de.ellpeck.actuallyadditions.mod.tile.*;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Each machine has its own config entries; the double crusher is separate from the crusher. */
public enum MachineKind {
    CRUSHER("crusher"),
    CRUSHER_DOUBLE("crusher_double"),
    POWERED_FURNACE("powered_furnace"),
    COAL_GENERATOR("coal_generator"),
    OIL_GENERATOR("oil_generator"),
    HEAT_COLLECTOR("heat_collector"),
    LEAF_GENERATOR("leaf_generator"),
    ENERGIZER("energizer");

    public final String key;

    MachineKind(String key) {
        this.key = key;
    }

    public static MachineKind of(BlockEntity be) {
        if (be instanceof TileEntityCrusherDouble) return CRUSHER_DOUBLE; // check subclass first
        if (be instanceof TileEntityCrusher) return CRUSHER;
        if (be instanceof TileEntityPoweredFurnace) return POWERED_FURNACE;
        if (be instanceof TileEntityCoalGenerator) return COAL_GENERATOR;
        if (be instanceof TileEntityOilGenerator) return OIL_GENERATOR;
        if (be instanceof TileEntityHeatCollector) return HEAT_COLLECTOR;
        if (be instanceof TileEntityLeafGenerator) return LEAF_GENERATOR;
        if (be instanceof TileEntityEnergizer) return ENERGIZER;
        return null;
    }
}
