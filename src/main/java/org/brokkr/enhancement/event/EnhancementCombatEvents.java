package org.brokkr.enhancement.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.brokkr.Brokkr;
import org.brokkr.enhancement.EnhancementData;
import org.brokkr.enhancement.EnhancementParticles;
import org.brokkr.enhancement.profile.EnhancedWeaponProfile;
import org.brokkr.enhancement.profile.WeaponEnhancementProfiles;

import java.util.Optional;

public final class EnhancementCombatEvents {
    private static final ResourceLocation ATTACK_DAMAGE_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(Brokkr.MODID, "enhancement_attack_damage");

    private EnhancementCombatEvents() {
    }

    public static void addAttackDamage(ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();
        Optional<EnhancedWeaponProfile> profile = WeaponEnhancementProfiles.find(stack);
        if (profile.isEmpty()) {
            return;
        }

        int level = EnhancementData.getLevel(stack);
        if (level <= EnhancementData.MIN_LEVEL) {
            return;
        }

        event.addModifier(
                Attributes.ATTACK_DAMAGE,
                new AttributeModifier(
                        ATTACK_DAMAGE_MODIFIER_ID,
                        profile.get().bonusDamage(level),
                        AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
        );
    }

    public static void spawnHitParticles(LivingIncomingDamageEvent event) {
        if (event.getAmount() <= 0 || !(event.getSource().getEntity() instanceof Player player)) {
            return;
        }

        ItemStack stack = player.getMainHandItem();
        Optional<EnhancedWeaponProfile> profile = WeaponEnhancementProfiles.find(stack);
        if (profile.isEmpty()) {
            return;
        }

        int level = EnhancementData.getLevel(stack);
        if (level <= EnhancementData.MIN_LEVEL || !(event.getEntity().level() instanceof ServerLevel serverLevel)) {
            return;
        }

        EnhancementParticles.spawn(serverLevel, event.getEntity(), profile.get().tier(level));
    }
}
