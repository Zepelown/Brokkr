package org.brokkr.enhancement;

public record EnhancementResult(Type type, int previousLevel, int newLevel, int successChance) {
    public enum Type {
        SUCCESS,
        FAILED_ROLL,
        NOT_SUPPORTED_WEAPON,
        NO_STONE,
        MAX_LEVEL,
        INVALID_LEVEL
    }
}
