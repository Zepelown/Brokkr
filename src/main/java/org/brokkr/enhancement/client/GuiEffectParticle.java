package org.brokkr.enhancement.client;

import net.minecraft.resources.ResourceLocation;

public class GuiEffectParticle {
    public final ResourceLocation texture;
    public float x;
    public float y;
    public final float velocityX;
    public final float velocityY;
    public final float scale;
    public final float rotation;
    public int age;
    public final int lifetime;

    public GuiEffectParticle(ResourceLocation texture, float x, float y, float velocityX, float velocityY, float scale, float rotation, int lifetime) {
        this.texture = texture;
        this.x = x;
        this.y = y;
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.scale = scale;
        this.rotation = rotation;
        this.lifetime = lifetime;
    }

    public void tick() {
        age++;
        x += velocityX;
        y += velocityY;
    }

    public float alpha() {
        return Math.max(0.0F, 1.0F - (float) age / (float) lifetime);
    }

    public boolean alive() {
        return age < lifetime;
    }
}
