package com.aamoreadditions.upgrade;

import com.aamoreadditions.AAMoreAdditions;
import com.aamoreadditions.effect.MachineKind;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import java.util.*;

/** Common startup file is loaded before recipe conditions. The server sends its frozen
 * snapshot to joining clients; changing the TOML during a session cannot change policy. */
@EventBusSubscriber(modid = AAMoreAdditions.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class UpgradeStartup {
    public static final List<String> KEYS = List.of("crusher","crusher_double","powered_furnace","coal_generator","oil_generator","heat_collector","leaf_generator","energizer","alloy_furnace","casing_cross","farmer");
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue DEPENDENCY;
    private static final List<ModConfigSpec.IntValue> SLOTS = new ArrayList<>();
    public record Snapshot(boolean dependency, List<Integer> slots) {
        public Snapshot {
            slots = List.copyOf(slots);
            if (slots.size() != KEYS.size() || slots.stream().anyMatch(n -> n < 0 || n > 10))
                throw new IllegalArgumentException("Invalid upgrade slot configuration");
        }
    }
    private static Snapshot local = new Snapshot(false, List.of(5,5,5,5,5,5,5,2,5,3,5));
    private static Snapshot current = local;
    private static boolean captured;
    static {
        var b = new com.aamoreadditions.config.EnglishConfigBuilder();
        DEPENDENCY = b.comment("If true, higher-level upgrades cannot be installed independently. Lower-level upgrades must be installed beforehand.",
                "",
                "Warning: This is very likely to cause machines that have already been configured to stop working, you must be clear about what you are doing!")
                .gameRestart().define("Dependency Upgrade Plugins", false);
        b.push("slots");
        for (int i = 0; i < KEYS.size(); i++) SLOTS.add(b.gameRestart().defineInRange(KEYS.get(i), local.slots().get(i), 0, 10));
        b.pop(); SPEC = b.build();
    }
    @SubscribeEvent public static void loaded(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() != SPEC || captured) return;
        local = new Snapshot(DEPENDENCY.get(), SLOTS.stream().map(ModConfigSpec.IntValue::get).toList());
        current = local; captured = true;
    }
    public static Snapshot local() { return local; }
    public static boolean dependency() { return current.dependency(); }
    public static void remote(Snapshot snapshot) { current = snapshot; }
    public static void resetClient() { current = local; }
    public static int slots(Object holder) {
        String key;
        if (holder instanceof SpeedOnlyHolder) key = "casing_cross";
        else if (holder instanceof com.aamoreadditions.block.AlloyFurnaceBlockEntity) key = "alloy_furnace";
        else if (holder instanceof de.ellpeck.actuallyadditions.mod.tile.TileEntityFarmer) key = "farmer";
        else if (holder instanceof net.minecraft.world.level.block.entity.BlockEntity be && MachineKind.of(be) != null) key = MachineKind.of(be).key;
        else return 5;
        return current.slots().get(KEYS.indexOf(key));
    }
}
