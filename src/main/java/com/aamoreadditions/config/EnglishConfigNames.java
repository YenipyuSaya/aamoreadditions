package com.aamoreadditions.config;

import java.util.Locale;

/** Display labels only. Never changes a TOML path, category, default or validator. */
public final class EnglishConfigNames {
    private EnglishConfigNames() {}
    public static String label(String key) {
        if (key.equals("Dependency Upgrade Plugins")) return key;
        if (key.equals("farmer_accelerates_ripening")) return "Farmer accelerates ripening";
        String[] words = key.replace('_',' ').replace('-',' ').split("\\s+");
        var result = new java.util.StringJoiner(" ");
        for (String word : words) {
            if (word.isEmpty()) continue;
            result.add(switch(word.toLowerCase(Locale.ROOT)) {
                case "fe" -> "FE";
                case "mb" -> "mB";
                default -> Character.toUpperCase(word.charAt(0)) + word.substring(1);
            });
        }
        return result.toString();
    }
}
