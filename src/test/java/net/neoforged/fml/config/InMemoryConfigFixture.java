package net.neoforged.fml.config;

import com.electronwill.nightconfig.core.CommentedConfig;

/** Test-only bridge to FML's sealed, package-private holder. Never saves or dispatches mod events. */
public final class InMemoryConfigFixture {
    private InMemoryConfigFixture() {}
    public static IConfigSpec.ILoadedConfig of(CommentedConfig corrected) {
        return new LoadedConfig(corrected, null, null);
    }
}
