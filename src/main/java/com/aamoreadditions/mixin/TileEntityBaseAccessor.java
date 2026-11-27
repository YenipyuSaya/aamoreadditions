package com.aamoreadditions.mixin;

import de.ellpeck.actuallyadditions.mod.tile.TileEntityBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Protected AA base members used by the AAMA Vertical Digger tick. */
@Mixin(value = TileEntityBase.class, remap = false)
public interface TileEntityBaseAccessor {
    @Accessor("ticksElapsed") int aamoreadditions$ticksElapsed();
    @Invoker("serverTick") void aamoreadditions$baseServerTick();
    @Invoker("sendUpdateWithInterval") boolean aamoreadditions$sendUpdateWithInterval();
}
