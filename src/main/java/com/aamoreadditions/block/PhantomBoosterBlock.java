package com.aamoreadditions.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Same hardness, sound and pillar shape as Actually Additions' Range Booster. Art is referenced, not copied. */
public class PhantomBoosterBlock extends Block {
    /** AA {@code VoxelShapes.BOOSTER_SHAPE}: {@code Block.box(5, 0, 5, 11, 16, 11)}. */
    public static final VoxelShape SHAPE = Block.box(5, 0, 5, 11, 16, 11);

    public PhantomBoosterBlock() {
        super(Properties.ofFullCopy(Blocks.STONE).strength(1.5F, 10.0F).sound(SoundType.STONE));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
