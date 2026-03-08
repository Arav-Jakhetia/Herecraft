package net.herecraft.client.resources;

import net.herecraft.client.renderer.texture.TextureUtil;
import net.herecraft.util.ResourceLocation;
import net.herecraft.world.ColorizerGrass;

import java.io.IOException;

public class GrassColorReloadListener implements IResourceManagerReloadListener {
    private static final ResourceLocation LOC_GRASS_PNG = new ResourceLocation("herecraft/textures/colormap/grass.png");

    public void onResourceManagerReload(IResourceManager resourceManager) {
        try {
            TextureUtil.ImageData data = TextureUtil.readImageData(resourceManager, LOC_GRASS_PNG);
            ColorizerGrass.setGrassBiomeColorizer(data.pixels(), data.width(), data.height());
        } catch (IOException ignored) {

        }
    }
}
