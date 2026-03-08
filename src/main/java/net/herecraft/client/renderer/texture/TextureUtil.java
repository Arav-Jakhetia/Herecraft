package net.herecraft.client.renderer.texture;

import net.herecraft.client.resources.IResourceManager;
import net.herecraft.util.ResourceLocation;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBImage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public final class TextureUtil {
    private TextureUtil() {
    }

    public static ImageData readImageData(IResourceManager resourceManager, ResourceLocation location) throws IOException {
        byte[] bytes;
        try (InputStream in = resourceManager.getResource(location)) {
            bytes = in.readAllBytes();
        }

        ByteBuffer raw = BufferUtils.createByteBuffer(bytes.length);
        raw.put(bytes).flip();

        IntBuffer w = BufferUtils.createIntBuffer(1);
        IntBuffer h = BufferUtils.createIntBuffer(1);
        IntBuffer c = BufferUtils.createIntBuffer(1);

        STBImage.stbi_set_flip_vertically_on_load(true);
        ByteBuffer pixels = STBImage.stbi_load_from_memory(raw, w, h, c, 4);
        if (pixels == null) {
            throw new IOException("Failed to decode image: " + location + " | " + STBImage.stbi_failure_reason());
        }

        int width = w.get(0);
        int height = h.get(0);
        int[] rgb = new int[width * height];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int idx = (y * width + x) * 4;
                int r = pixels.get(idx) & 0xFF;
                int g = pixels.get(idx + 1) & 0xFF;
                int b = pixels.get(idx + 2) & 0xFF;
                rgb[y * width + x] = (r << 16) | (g << 8) | b;
            }
        }

        STBImage.stbi_image_free(pixels);
        return new ImageData(rgb, width, height);
    }

    public static final class ImageData {
        private final int[] pixels;
        private final int width;
        private final int height;

        public ImageData(int[] pixels, int width, int height) {
            this.pixels = pixels;
            this.width = width;
            this.height = height;
        }

        public int[] pixels() {
            return pixels;
        }

        public int width() {
            return width;
        }

        public int height() {
            return height;
        }
    }
}
