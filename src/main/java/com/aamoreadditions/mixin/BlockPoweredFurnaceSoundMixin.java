package com.aamoreadditions.mixin;

import com.aamoreadditions.block.AlloyFurnaceBlock;
import de.ellpeck.actuallyadditions.mod.blocks.BlockPoweredFurnace;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.spongepowered.asm.mixin.Mixin;

/** Adds vanilla blast furnace crackle to AA's Powered Furnace while lit (not to the Alloy Furnace). */
@Mixin(BlockPoweredFurnace.class)
public abstract class BlockPoweredFurnaceSoundMixin extends Block {
    private BlockPoweredFurnaceSoundMixin(Properties p) { super(p); }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
        if ((Object) this instanceof AlloyFurnaceBlock) return;
        if (state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT)
                && random.nextDouble() < 0.1D) {
            level.playLocalSound(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D,
                    SoundEvents.BLASTFURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 1.0F, 1.0F, true);
        }
    }
}
