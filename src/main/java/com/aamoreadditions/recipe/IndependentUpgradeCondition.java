package com.aamoreadditions.recipe;
import com.aamoreadditions.AAMoreAdditions;
import com.aamoreadditions.upgrade.UpgradeStartup;
import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
public final class IndependentUpgradeCondition implements ICondition {
    public static final IndependentUpgradeCondition INSTANCE = new IndependentUpgradeCondition();
    public static final MapCodec<IndependentUpgradeCondition> CODEC = MapCodec.unit(INSTANCE);
    public static final DeferredRegister<MapCodec<? extends ICondition>> CONDITIONS = DeferredRegister.create(NeoForgeRegistries.CONDITION_SERIALIZERS,AAMoreAdditions.MODID);
    static { CONDITIONS.register("independent_upgrades",()->CODEC); }
    @Override public boolean test(IContext context) { return !UpgradeStartup.local().dependency(); }
    @Override public MapCodec<? extends ICondition> codec() { return CODEC; }
}
