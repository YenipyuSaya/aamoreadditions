package com.aamoreadditions.mixin;

import com.aamoreadditions.AAMoreAdditions;
import com.aamoreadditions.client.ConfigTooltipLayout;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.List;

/** Only this mod's dependency toggle in the native fallback UI. Configured has
 * its own unwrapped title line, and is deliberately not intercepted here. */
@Mixin(Tooltip.class)
public abstract class ConfigTooltipMixin {
    @Inject(method="splitTooltip",at=@At("HEAD"),cancellable=true)
    private static void aamoreadditions$ownConfigWidth(Minecraft minecraft,Component message,
                                                       CallbackInfoReturnable<List<FormattedCharSequence>> cir) {
        var screen=minecraft.screen;
        if (!(screen instanceof ConfigurationScreen.ConfigurationSectionScreen)) return;
        var owner=ModList.get().getModContainerById(AAMoreAdditions.MODID).orElse(null);
        if (owner==null || !screen.getTitle().getString().contains(owner.getModInfo().getDisplayName())) return;
        String title="Dependency Upgrade Plugins";
        if (!message.getString().startsWith(title+"\n")) return;
        int titleWidth=minecraft.font.width(Component.literal(title).withStyle(ChatFormatting.BOLD));
        cir.setReturnValue(minecraft.font.split(message,ConfigTooltipLayout.width(screen.width,titleWidth)));
    }
}
