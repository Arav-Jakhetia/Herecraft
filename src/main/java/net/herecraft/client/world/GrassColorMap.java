package net.herecraft.client.world;

import org.joml.Vector3f;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

public class GrassColorMap {
    private final BufferedImage image;

    public GrassColorMap() {
        try(InputStream stream = GrassColorMap.class.getResourceAsStream("/assets/herecraft/textures/colormap/grass.png")) {
            if(stream == null) {
                throw new RuntimeException("Failed to load grass color map");
            }

            image = ImageIO.read(stream);
        } catch(IOException exception) {
            throw new RuntimeException("Failed to load grass color map", exception);
        }
    }

    public Vector3f sample(float temperature, float rainfall) {
        temperature = clamp(temperature);
        rainfall = clamp(rainfall) * temperature;

        int x = (int)((1.0f - temperature) * (image.getWidth() - 1));
        int y = (int)((1.0f - rainfall) * (image.getHeight() - 1));

        int rgb = image.getRGB(x, y);

        return new Vector3f(
                ((rgb >> 16) & 0xFF) / 255.0f,
                ((rgb >> 8) & 0xFF) / 255.0f,
                (rgb & 0xFF) / 255.0f
        );
    }

    private float clamp(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
