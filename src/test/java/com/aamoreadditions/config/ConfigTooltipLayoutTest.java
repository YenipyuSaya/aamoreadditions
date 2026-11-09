package com.aamoreadditions.config;
import com.aamoreadditions.client.ConfigTooltipLayout;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ConfigTooltipLayoutTest {
    @Test void widthHookIsScopedToOwnNativeScreenNotConfiguredOrOtherMods() throws Exception {
        var s=java.nio.file.Files.readString(java.nio.file.Path.of("src/main/java/com/aamoreadditions/mixin/ConfigTooltipMixin.java"));
        assertTrue(s.contains("screen instanceof ConfigurationScreen.ConfigurationSectionScreen"));
        assertTrue(s.contains("getModContainerById(AAMoreAdditions.MODID)"));
        assertTrue(s.contains("screen.getTitle().getString().contains(owner.getModInfo().getDisplayName())"));
        assertTrue(s.contains("message.getString().startsWith(title+\"\\n\")"));
    }
    @Test void normalWindowFitsEntireTitleInsteadOfFixed170() { assertEquals(214,ConfigTooltipLayout.width(500,214)); }
    @Test void smallWindowStillConstrainsTextAndMayWrapTitle() { assertEquals(144,ConfigTooltipLayout.width(160,214)); }
    @Test void shortTitlesRetainDefaultWidthAndTinyWindowsStayPositive() {
        assertEquals(170,ConfigTooltipLayout.width(500,120));assertEquals(1,ConfigTooltipLayout.width(10,214));
    }
}
