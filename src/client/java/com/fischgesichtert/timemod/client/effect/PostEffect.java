package com.fischgesichtert.timemod.client.effect;

import com.fischgesichtert.timemod.TimeMod;
import com.mojang.blaze3d.pipeline.RenderTarget;
import java.io.IOException;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/** Ein Vollbild-Shader (Post-Processing) mit Lade-, Größen- und Uniform-Verwaltung. */
final class PostEffect {
    private final ResourceLocation location;
    @Nullable
    private PostChain chain;
    private boolean failed;
    private int width = -1;
    private int height = -1;

    PostEffect(ResourceLocation location) {
        this.location = location;
    }

    /** Setzt die Uniforms und legt den Effekt über das aktuelle Bild. */
    void process(float partialTick, Consumer<EffectInstance> uniforms) {
        Minecraft mc = Minecraft.getInstance();
        RenderTarget main = mc.getMainRenderTarget();
        PostChain post = chain(mc, main);
        if (post == null) {
            return;
        }
        for (PostPass pass : post.passes) {
            uniforms.accept(pass.getEffect());
        }
        post.process(partialTick);
        main.bindWrite(false);
    }

    @Nullable
    private PostChain chain(Minecraft mc, RenderTarget main) {
        if (failed) {
            return null;
        }
        if (chain == null) {
            try {
                chain = new PostChain(mc.getTextureManager(), mc.getResourceManager(), main, location);
            } catch (IOException | RuntimeException e) {
                TimeMod.LOGGER.error("Shader {} konnte nicht geladen werden", location, e);
                failed = true;
                return null;
            }
        }
        if (main.width != width || main.height != height) {
            width = main.width;
            height = main.height;
            chain.resize(width, height);
        }
        return chain;
    }

    void reset() {
        if (chain != null) {
            chain.close();
            chain = null;
        }
        failed = false;
        width = -1;
        height = -1;
    }
}
