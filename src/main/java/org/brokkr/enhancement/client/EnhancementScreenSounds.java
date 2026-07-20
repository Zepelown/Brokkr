package org.brokkr.enhancement.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

public final class EnhancementScreenSounds {
    private EnhancementScreenSounds() {
    }

    public static void playStrike(Minecraft client, int strikeIndex) {
        if (client.getSoundManager() == null) {
            return;
        }
        if (strikeIndex >= 3) {
            client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.ANVIL_LAND, 0.85F, 0.65F));
            return;
        }
        client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.ANVIL_USE, 0.9F, 0.75F + strikeIndex * 0.08F));
    }

    public static void playSuccess(Minecraft client) {
        client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.PLAYER_LEVELUP, 0.9F, 1.05F));
    }

    public static void playFailure(Minecraft client) {
        client.getSoundManager().play(SimpleSoundInstance.forUI(unwrap(SoundEvents.ITEM_BREAK), 0.9F, 0.75F));
    }

    private static SoundEvent unwrap(Holder.Reference<SoundEvent> sound) {
        return sound.value();
    }
}
