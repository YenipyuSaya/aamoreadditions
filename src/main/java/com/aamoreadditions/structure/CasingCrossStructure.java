package com.aamoreadditions.structure;

import com.aamoreadditions.block.ModBlocks;
import de.ellpeck.actuallyadditions.mod.blocks.ActuallyBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Two-layer cross multiblock, anchored at the Empowerer (the centre of the upper layer).
 * Upper layer: Empowerer in the middle, a Display Stand on each of its four sides (N/S/E/W).
 * Lower layer: five Decorative Machine Casings in the same cross shape, directly below the upper layer.
 * The shape is rotation-symmetric, so no facing needs to be tracked.
 */
public final class CasingCrossStructure {
    public record Part(BlockPos offset, Supplier<Block> block) {}

    /** Offsets are relative to the Empowerer. */
    public static final List<Part> PARTS = buildParts();

    private CasingCrossStructure() {}

    private static List<Part> buildParts() {
        List<Part> list = new ArrayList<>();
        list.add(new Part(new BlockPos(0, 0, 0), ActuallyBlocks.EMPOWERER::get));
        int[][] arms = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] a : arms) {
            list.add(new Part(new BlockPos(a[0], 0, a[1]), ActuallyBlocks.DISPLAY_STAND::get));
        }
        list.add(new Part(new BlockPos(0, -1, 0), ModBlocks.DECORATIVE_MACHINE_CASING::get));
        for (int[] a : arms) {
            list.add(new Part(new BlockPos(a[0], -1, a[1]), ModBlocks.DECORATIVE_MACHINE_CASING::get));
        }
        return List.copyOf(list);
    }

    /** True when the whole structure is built with the Empowerer at {@code empowererPos}. */
    public static boolean matches(BlockGetter level, BlockPos empowererPos) {
        for (Part p : PARTS) {
            if (level.getBlockState(empowererPos.offset(p.offset())).getBlock() != p.block().get()) return false;
        }
        return true;
    }

    /** Positions (absolute) that are still wrong or missing, for hints and debugging. */
    public static List<BlockPos> missing(BlockGetter level, BlockPos empowererPos) {
        List<BlockPos> out = new ArrayList<>();
        for (Part p : PARTS) {
            BlockPos pos = empowererPos.offset(p.offset());
            if (level.getBlockState(pos).getBlock() != p.block().get()) out.add(pos);
        }
        return out;
    }
}
