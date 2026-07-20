package org.brokkr.enhancement;

import net.minecraft.network.chat.Component;
import org.brokkr.enhancement.text.EnhancementTextKeys;

public final class EnhancementMessages {
    private EnhancementMessages() {
    }

    public static Component forResult(EnhancementResult result) {
        return switch (result.type()) {
            case SUCCESS -> Component.translatable(
                    EnhancementTextKeys.COMMAND_SUCCESS,
                    result.previousLevel(),
                    result.newLevel()
            );
            case FAILED_ROLL -> Component.translatable(
                    EnhancementTextKeys.COMMAND_FAILURE,
                    result.previousLevel()
            );
            case NOT_SUPPORTED_WEAPON -> Component.translatable(EnhancementTextKeys.COMMAND_NOT_WEAPON);
            case NO_STONE -> Component.translatable(EnhancementTextKeys.COMMAND_NO_STONE);
            case MAX_LEVEL -> Component.translatable(EnhancementTextKeys.COMMAND_MAX_LEVEL);
            case INVALID_LEVEL -> Component.translatable(EnhancementTextKeys.COMMAND_INVALID_LEVEL);
        };
    }
}
