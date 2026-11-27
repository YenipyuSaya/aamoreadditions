package com.aamoreadditions.booster;

import com.aamoreadditions.AAMoreAdditions;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/** Fixed overload numbers and the custom void-like damage type (t455u–t457u, t517u). */
public final class BoosterOverload {
    public static final ResourceKey<DamageType> DAMAGE_TYPE = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(AAMoreAdditions.MODID, "phantom_booster_overload"));

    public static final float DAMAGE = 4.0F;
    /** Sphere radius, in blocks, measured from the machine-block centre. */
    public static final double RADIUS = 3.0;
    /** 50% of a normal sound event volume. */
    public static final float SOUND_VOLUME = 0.5F;

    private BoosterOverload() {}

    public static boolean inRadius(double distSqr) {
        return distSqr <= RADIUS * RADIUS;
    }

    /** Living entities only: items, experience orbs, arrows and other non-living entities are excluded. */
    public static boolean isTarget(Entity entity) {
        return entity instanceof LivingEntity && entity.isAlive();
    }

    /** How many consecutive boosters sit above the machine (not limited by the stack cap). */
    public static int boostersToDrop(java.util.function.IntFunction<BoosterTier> atOffset) {
        int n = 0;
        while (atOffset.apply(n) != null) n++;
        return n;
    }

    public static DamageSource damageSource(Level level) {
        return new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DAMAGE_TYPE));
    }
}
