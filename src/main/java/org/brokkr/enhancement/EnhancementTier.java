package org.brokkr.enhancement;

import net.minecraft.ChatFormatting;

public enum EnhancementTier {
    LOW(ChatFormatting.GRAY),
    MID(ChatFormatting.AQUA),
    HIGH(ChatFormatting.LIGHT_PURPLE),
    VERY_HIGH(ChatFormatting.RED),
    MAX(ChatFormatting.GOLD);

    private final ChatFormatting color;

    EnhancementTier(ChatFormatting color) {
        this.color = color;
    }

    public ChatFormatting color() {
        return color;
    }

    public static EnhancementTier fromLevel(int level) {
        if (level >= EnhancementData.MAX_LEVEL) {
            return MAX;
        }
        if (level >= 16) {
            return VERY_HIGH;
        }
        if (level >= 11) {
            return HIGH;
        }
        if (level >= 6) {
            return MID;
        }
        return LOW;
    }
}
