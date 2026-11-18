package com.aamoreadditions.mixin;

import net.minecraft.world.level.storage.loot.entries.CompositeEntryBase;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.List;

/** Alternatives, sequence and group entries. */
@Mixin(CompositeEntryBase.class)
public interface CompositeEntryBaseAccessor {
    @Accessor("children") List<LootPoolEntryContainer> aamoreadditions$children();
}
