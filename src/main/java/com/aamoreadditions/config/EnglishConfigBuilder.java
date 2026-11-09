package com.aamoreadditions.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

/** NeoForge's display metadata contains readable English, not missing language keys.
 * Configured 1.21.1 also has its own human-readable raw-name fallback. Both use the
 * existing English comments; no language selection or config storage is changed. */
public final class EnglishConfigBuilder extends ModConfigSpec.Builder {
    @Override public <T> ModConfigSpec.ConfigValue<T> define(List<String> path, Supplier<T> defaults,
                                                            Predicate<Object> validator, Class<?> type) {
        translation(EnglishConfigNames.label(path.getLast()));
        return super.define(path,defaults,validator,type);
    }
    @Override public ModConfigSpec.Builder push(List<String> path) {
        translation(EnglishConfigNames.label(path.getLast()));
        return super.push(path);
    }
}
