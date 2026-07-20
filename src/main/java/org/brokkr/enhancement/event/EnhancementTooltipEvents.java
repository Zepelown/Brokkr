package org.brokkr.enhancement.event;

import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import org.brokkr.enhancement.EnhancementData;
import org.brokkr.enhancement.EnhancementTier;
import org.brokkr.enhancement.profile.WeaponEnhancementProfiles;
import org.brokkr.enhancement.text.EnhancementTextKeys;

public final class EnhancementTooltipEvents {
    private EnhancementTooltipEvents() {
    }

    public static void addTooltip(ItemTooltipEvent event) {
        if (!WeaponEnhancementProfiles.isSupported(event.getItemStack())) {
            return;
        }

        int level = EnhancementData.getLevel(event.getItemStack());
        if (level <= EnhancementData.MIN_LEVEL) {
            return;
        }

        event.getToolTip().add(Component.translatable(EnhancementTextKeys.TOOLTIP_LEVEL, level)
                .withStyle(EnhancementTier.fromLevel(level).color()));
    }
}
