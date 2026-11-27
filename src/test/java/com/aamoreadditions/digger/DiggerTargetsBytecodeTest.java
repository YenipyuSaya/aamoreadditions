package com.aamoreadditions.digger;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/** Reads the real AA classes: every digger mixin target exists with the descriptor the mixins use. Does not apply Mixins. */
class DiggerTargetsBytecodeTest {
    static final String AA = "de/ellpeck/actuallyadditions/mod/";

    static ClassNode type(String name) throws IOException {
        var node = new ClassNode();
        try (var in = DiggerTargetsBytecodeTest.class.getClassLoader().getResourceAsStream(name + ".class")) {
            assertNotNull(in, name);
            new ClassReader(in).accept(node, 0);
        }
        return node;
    }

    static void method(ClassNode c, String name, String desc) {
        assertTrue(c.methods.stream().anyMatch(m -> m.name.equals(name) && m.desc.equals(desc)), c.name + "." + name + desc);
    }

    static void field(ClassNode c, String name, String desc) {
        assertTrue(c.fields.stream().anyMatch(f -> f.name.equals(name) && f.desc.equals(desc)), c.name + "." + name);
    }

    @Test void verticalDiggerTargets() throws Exception {
        var d = type(AA + "tile/TileEntityVerticalDigger");
        method(d, "serverTick", "(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/BlockEntity;)V");
        method(d, "onLoad", "()V");
        method(d, "onButtonPressed", "(ILnet/minecraft/world/entity/player/Player;)V");
        String nbt = "(Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/core/HolderLookup$Provider;L" + AA + "tile/TileEntityBase$NBTType;)V";
        method(d, "writeSyncableNBT", nbt);
        method(d, "readSyncableNBT", nbt);
        field(d, "checkX", "I"); field(d, "checkY", "I"); field(d, "checkZ", "I");
        field(d, "onlyMineOres", "Z");
        field(d, "storage", "L" + AA + "tile/CustomEnergyStorage;");
        field(d, "DEFAULT_RANGE", "I");
    }

    @Test void baseAndMenuTargets() throws Exception {
        var b = type(AA + "tile/TileEntityBase");
        field(b, "ticksElapsed", "I");
        method(b, "serverTick", "()V");
        method(b, "sendUpdateWithInterval", "()Z");
        var m = type(AA + "inventory/ContainerMiner");
        method(m, "<init>", "(ILnet/minecraft/world/entity/player/Inventory;L" + AA + "tile/TileEntityVerticalDigger;)V");
        method(m, "quickMoveStack", "(Lnet/minecraft/world/entity/player/Player;I)Lnet/minecraft/world/item/ItemStack;");
        field(m, "miner", "L" + AA + "tile/TileEntityVerticalDigger;");
    }

    @Test void aaHelpersUsedByThePortAreStatic() throws Exception {
        method(type(AA + "util/WorldUtil"), "fireFakeHarvestEventsForDropChance",
                "(Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)F");
        method(type(AA + "tile/TileEntityPhantomface"), "upgradeRange", "(ILnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)I");
    }
}
