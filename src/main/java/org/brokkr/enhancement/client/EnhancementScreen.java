package org.brokkr.enhancement.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.brokkr.Brokkr;
import org.brokkr.enhancement.EnhancementChance;
import org.brokkr.enhancement.EnhancementData;
import org.brokkr.enhancement.EnhancementResult;
import org.brokkr.enhancement.menu.EnhancementMenu;
import org.brokkr.enhancement.profile.EnhancedWeaponProfile;
import org.brokkr.enhancement.profile.WeaponEnhancementProfiles;
import org.brokkr.enhancement.text.EnhancementTextKeys;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;

public class EnhancementScreen extends AbstractContainerScreen<EnhancementMenu> {
    private static final ResourceLocation RUNE_CIRCLE = texture("rune_circle");
    private static final ResourceLocation RUNE_SUCCESS = texture("rune_success");
    private static final ResourceLocation RUNE_FAILURE = texture("rune_failure");
    private static final ResourceLocation SPARK = texture("spark");
    private static final ResourceLocation SMOKE = texture("smoke");
    private static final ResourceLocation HAMMER_FLASH = texture("hammer_flash");
    private static final ResourceLocation RESULT_FLASH = texture("result_flash");

    private static final int REVEAL_TICK = 34;
    private static final int FINISH_TICK = 50;
    private static final int RUNE_EFFECT_SIZE = 128;
    private static final int HAMMER_FLASH_SIZE = 96;
    private static final int RESULT_FLASH_SIZE = 256;

    private final List<GuiEffectParticle> particles = new ArrayList<>();
    private final Random random = new Random();
    private Button enhanceButton;
    private EnhancementScreenState state = EnhancementScreenState.EMPTY;
    private int lastAttemptSequence;
    private int processTick;
    private int screenShakeTicks;
    private boolean strike1Played;
    private boolean strike2Played;
    private boolean strike3Played;
    private boolean revealPlayed;
    private boolean resetSent;

    public EnhancementScreen(EnhancementMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = 176;
        imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        enhanceButton = Button.builder(Component.translatable(EnhancementTextKeys.SCREEN_BUTTON_ENHANCE), button -> sendButton(EnhancementMenu.BUTTON_ENHANCE))
                .bounds(leftPos + 57, topPos + 61, 62, 20)
                .build();
        addRenderableWidget(enhanceButton);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        detectServerAttempt();
        tickProcess();
        if (enhanceButton != null) {
            enhanceButton.active = menu.canEnhance() && state != EnhancementScreenState.PROCESSING;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        if (state == EnhancementScreenState.PROCESSING || state == EnhancementScreenState.SUCCESS || state == EnhancementScreenState.FAILURE) {
            renderProcessOverlay(graphics);
        }
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xEE3E3730);
        graphics.fill(leftPos + 4, topPos + 4, leftPos + imageWidth - 4, topPos + imageHeight - 4, 0xEE5A4A3B);
        graphics.fill(leftPos + 8, topPos + 8, leftPos + imageWidth - 8, topPos + imageHeight - 8, 0xEE2D2925);
        graphics.renderOutline(leftPos, topPos, imageWidth, imageHeight, 0xFFB8843E);
        graphics.renderOutline(leftPos + 40, topPos + 31, 24, 24, 0xFFE0C078);
        graphics.renderOutline(leftPos + 112, topPos + 31, 24, 24, 0xFFE0C078);
        graphics.fill(leftPos + 41, topPos + 32, leftPos + 63, topPos + 54, 0xFF191613);
        graphics.fill(leftPos + 113, topPos + 32, leftPos + 135, topPos + 54, 0xFF191613);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0xF0F0F0, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xD0D0D0, false);

        ItemStack weapon = menu.getWeapon();
        int level = EnhancementData.getLevel(weapon);
        Optional<EnhancedWeaponProfile> profile = WeaponEnhancementProfiles.find(weapon);
        int nextLevel = Math.min(EnhancementData.MAX_LEVEL, level + 1);
        int chance = profile.isPresent() && level < EnhancementData.MAX_LEVEL ? EnhancementChance.successChanceForCurrentLevel(level) : 0;
        float currentBonus = profile.map(value -> value.bonusDamage(level)).orElse(0.0F);
        float nextBonus = profile.map(value -> value.bonusDamage(nextLevel)).orElse(0.0F);

        int y = 17;
        graphics.drawString(font, Component.translatable(EnhancementTextKeys.SCREEN_CURRENT_LEVEL, level), 70, y, 0xE7D27A, false);
        graphics.drawString(font, Component.translatable(EnhancementTextKeys.SCREEN_NEXT_LEVEL, nextLevel), 70, y + 10, 0xE7D27A, false);
        graphics.drawString(font, Component.translatable(EnhancementTextKeys.SCREEN_SUCCESS_CHANCE, chance), 70, y + 20, 0x8DDCFF, false);
        graphics.drawString(font, Component.translatable(
                EnhancementTextKeys.SCREEN_ATTACK_BONUS,
                format(currentBonus),
                format(nextBonus)
        ), 70, y + 30, 0x88FF88, false);

        graphics.drawString(font, statusText(), 8, 72, statusColor(), false);
    }

    private void detectServerAttempt() {
        int sequence = menu.getAttemptSequence();
        if (sequence != lastAttemptSequence) {
            lastAttemptSequence = sequence;
            state = EnhancementScreenState.PROCESSING;
            processTick = 0;
            screenShakeTicks = 0;
            strike1Played = false;
            strike2Played = false;
            strike3Played = false;
            revealPlayed = false;
            resetSent = false;
            particles.clear();
        }
    }

    private void tickProcess() {
        if (state != EnhancementScreenState.PROCESSING && state != EnhancementScreenState.SUCCESS && state != EnhancementScreenState.FAILURE) {
            updatePassiveState();
            return;
        }

        processTick++;
        tickParticles();

        if (processTick == 5 && !strike1Played) {
            playStrike(1, 10);
            strike1Played = true;
        }
        if (processTick == 15 && !strike2Played) {
            playStrike(2, 16);
            strike2Played = true;
        }
        if (processTick == 25 && !strike3Played) {
            playStrike(3, 22);
            strike3Played = true;
        }
        if (processTick >= REVEAL_TICK && !revealPlayed) {
            revealResult();
        }
        if (processTick >= FINISH_TICK && !resetSent) {
            sendButton(EnhancementMenu.BUTTON_RESET);
            resetSent = true;
            updatePassiveState();
        }

        if (screenShakeTicks > 0) {
            screenShakeTicks--;
        }
    }

    private void updatePassiveState() {
        ItemStack weapon = menu.getWeapon();
        if (weapon.isEmpty() && menu.getStone().isEmpty()) {
            state = EnhancementScreenState.EMPTY;
        } else if (!weapon.isEmpty() && !WeaponEnhancementProfiles.isSupported(weapon)) {
            state = EnhancementScreenState.INVALID;
        } else if (WeaponEnhancementProfiles.isSupported(weapon) && EnhancementData.getLevel(weapon) >= EnhancementData.MAX_LEVEL) {
            state = EnhancementScreenState.MAX_LEVEL;
        } else {
            state = menu.canEnhance() ? EnhancementScreenState.READY : EnhancementScreenState.EMPTY;
        }
    }

    private void playStrike(int strikeIndex, int particleCount) {
        if (minecraft != null) {
            EnhancementScreenSounds.playStrike(minecraft, strikeIndex);
        }
        screenShakeTicks = 4 + strikeIndex;
        spawnParticles(SPARK, particleCount, 1.0F, 18);
    }

    private void revealResult() {
        EnhancementResult.Type type = resultType();
        if (type == EnhancementResult.Type.SUCCESS) {
            state = EnhancementScreenState.SUCCESS;
            if (minecraft != null) {
                EnhancementScreenSounds.playSuccess(minecraft);
            }
            spawnParticles(SPARK, 28, 1.25F, 24);
        } else {
            state = EnhancementScreenState.FAILURE;
            if (minecraft != null) {
                EnhancementScreenSounds.playFailure(minecraft);
            }
            spawnParticles(SMOKE, 18, 1.15F, 32);
        }
        screenShakeTicks = 7;
        revealPlayed = true;
    }

    private void tickParticles() {
        Iterator<GuiEffectParticle> iterator = particles.iterator();
        while (iterator.hasNext()) {
            GuiEffectParticle particle = iterator.next();
            particle.tick();
            if (!particle.alive()) {
                iterator.remove();
            }
        }
    }

    private void spawnParticles(ResourceLocation texture, int count, float scale, int lifetime) {
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            float speed = 0.6F + random.nextFloat() * 1.5F;
            float velocityX = (float) Math.cos(angle) * speed;
            float velocityY = (float) Math.sin(angle) * speed - 0.3F;
            particles.add(new GuiEffectParticle(
                    texture,
                    width / 2.0F,
                    height / 2.0F,
                    velocityX,
                    velocityY,
                    scale * (0.75F + random.nextFloat() * 0.7F),
                    random.nextFloat() * 360.0F,
                    lifetime
            ));
        }
    }

    private void renderProcessOverlay(GuiGraphics graphics) {
        graphics.fill(0, 0, width, height, 0xAA050505);

        int centerX = width / 2 + shakeOffset();
        int centerY = height / 2 + shakeOffset();
        blitCentered(graphics, RUNE_CIRCLE, centerX, centerY, RUNE_EFFECT_SIZE, 128, 128);

        if (processTick == 25 || processTick == 26 || processTick == 27) {
            blitCentered(graphics, HAMMER_FLASH, centerX, centerY, HAMMER_FLASH_SIZE, 96, 96);
        }

        for (GuiEffectParticle particle : particles) {
            int size = Math.max(8, (int) (32 * particle.scale));
            blit(graphics, particle.texture, (int) particle.x - size / 2, (int) particle.y - size / 2, size, size, particleTextureSize(particle.texture), particleTextureSize(particle.texture));
        }

        if (processTick >= REVEAL_TICK) {
            blitCentered(graphics, RESULT_FLASH, centerX, centerY, RESULT_FLASH_SIZE, 256, 256);
            if (state == EnhancementScreenState.SUCCESS) {
                blitCentered(graphics, RUNE_SUCCESS, centerX, centerY, RUNE_EFFECT_SIZE, 128, 128);
            } else if (state == EnhancementScreenState.FAILURE) {
                blitCentered(graphics, RUNE_FAILURE, centerX, centerY, RUNE_EFFECT_SIZE, 128, 128);
            }
        }

        graphics.drawCenteredString(font, statusText(), centerX, centerY + 78, statusColor());
        if (state == EnhancementScreenState.SUCCESS || state == EnhancementScreenState.FAILURE) {
            graphics.drawCenteredString(font, Component.literal("+" + menu.getPreviousLevel() + " -> +" + menu.getNewLevel()), centerX, centerY + 92, 0xFFFFFF);
        }
    }

    private int shakeOffset() {
        if (screenShakeTicks <= 0) {
            return 0;
        }
        return random.nextInt(5) - 2;
    }

    private void blit(GuiGraphics graphics, ResourceLocation texture, int x, int y, int width, int height, int textureWidth, int textureHeight) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0.0F, 0.0F, width, height, textureWidth, textureHeight);
    }

    private void blitCentered(GuiGraphics graphics, ResourceLocation texture, int centerX, int centerY, int size, int textureWidth, int textureHeight) {
        blit(graphics, texture, centerX - size / 2, centerY - size / 2, size, size, textureWidth, textureHeight);
    }

    private int particleTextureSize(ResourceLocation texture) {
        if (texture.equals(SPARK)) {
            return 32;
        }
        if (texture.equals(SMOKE)) {
            return 48;
        }
        return 32;
    }

    private Component statusText() {
        return switch (state) {
            case READY -> Component.translatable(EnhancementTextKeys.SCREEN_STATUS_READY);
            case PROCESSING -> Component.translatable(processingStatusKey());
            case SUCCESS -> Component.translatable(EnhancementTextKeys.SCREEN_STATUS_SUCCESS);
            case FAILURE -> Component.translatable(EnhancementTextKeys.SCREEN_STATUS_FAILURE);
            case MAX_LEVEL -> Component.translatable(EnhancementTextKeys.SCREEN_STATUS_MAX_LEVEL);
            case INVALID -> Component.translatable(EnhancementTextKeys.SCREEN_STATUS_INVALID_WEAPON);
            case EMPTY -> menu.getWeapon().isEmpty()
                    ? Component.translatable(EnhancementTextKeys.SCREEN_STATUS_EMPTY)
                    : Component.translatable(EnhancementTextKeys.SCREEN_STATUS_NO_STONE);
        };
    }

    private String processingStatusKey() {
        if (processTick < 10) {
            return EnhancementTextKeys.SCREEN_STATUS_PROCESSING_PREPARE;
        }
        if (processTick < 20) {
            return EnhancementTextKeys.SCREEN_STATUS_PROCESSING_HAMMER_1;
        }
        if (processTick < 30) {
            return EnhancementTextKeys.SCREEN_STATUS_PROCESSING_HAMMER_2;
        }
        return EnhancementTextKeys.SCREEN_STATUS_PROCESSING_HAMMER_3;
    }

    private int statusColor() {
        return switch (state) {
            case SUCCESS -> ChatFormatting.GREEN.getColor();
            case FAILURE, INVALID -> ChatFormatting.RED.getColor();
            case MAX_LEVEL -> ChatFormatting.GOLD.getColor();
            case PROCESSING -> ChatFormatting.AQUA.getColor();
            default -> 0xE0E0E0;
        };
    }

    private EnhancementResult.Type resultType() {
        EnhancementResult.Type[] values = EnhancementResult.Type.values();
        int id = menu.getResultTypeId();
        if (id < 0 || id >= values.length) {
            return EnhancementResult.Type.FAILED_ROLL;
        }
        return values[id];
    }

    private void sendButton(int buttonId) {
        Minecraft client = minecraft;
        if (client != null && client.gameMode != null) {
            client.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
        }
    }

    private static String format(float value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(Brokkr.MODID, "textures/gui/enhancement/" + name + ".png");
    }
}
