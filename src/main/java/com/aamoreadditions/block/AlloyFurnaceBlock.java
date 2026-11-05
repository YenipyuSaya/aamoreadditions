package com.aamoreadditions.block;

import de.ellpeck.actuallyadditions.mod.blocks.BlockPoweredFurnace;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Alloy Furnace: for now behaves exactly like AA's Powered Furnace (same logic and GUI). */
public class AlloyFurnaceBlock extends BlockPoweredFurnace {
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AlloyFurnaceBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(net.minecraft.world.level.Level level, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        return level.isClientSide ? de.ellpeck.actuallyadditions.mod.tile.TileEntityPoweredFurnace::clientTick : AlloyFurnaceBlockEntity::alloyServerTick;
    }

    /** Drop the "previously Double Furnace" line inherited from AA's BlockPoweredFurnace. */
    @Override
    public void appendHoverText(net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.Item.TooltipContext ctx, java.util.List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack, ctx, tooltip, flag);
        tooltip.removeIf(c -> c.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t
                && "tooltip.actuallyadditions.previouslyDoubleFurnace".equals(t.getKey()));
    }

    @Override
    public void onRemove(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!level.isClientSide && state.getBlock() != newState.getBlock()
                && level.getBlockEntity(pos) instanceof AlloyFurnaceBlockEntity be) {
            for (int i = 0; i < be.extra.getSlots(); i++) {
                net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), be.extra.getStackInSlot(i).copy());
                be.extra.setStackInSlot(i, net.minecraft.world.item.ItemStack.EMPTY);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public int getLightEmission(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        return state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT)
                && state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT) ? 7 : 0;
    }

    @Override
    public void animateTick(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, net.minecraft.util.RandomSource random) {
        super.animateTick(state, level, pos, random);
        if (level.isClientSide && state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT)
                && state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT)) {
            ClientHook.start(level, pos);
        }
    }

    /** Separate holder so client classes only load when this runs (client side only). */
    private static final class ClientHook {
        static void start(net.minecraft.world.level.Level level, BlockPos pos) {
            if (level instanceof net.minecraft.client.multiplayer.ClientLevel cl)
                com.aamoreadditions.client.sound.AlloyFurnaceSound.ensurePlaying(cl, pos);
        }
    }
}
