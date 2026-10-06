package net.herecraft.client.render.overlay;

import net.herecraft.client.render.buffer.GpuBuffer;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkPhysicalDevice;

import static org.lwjgl.vulkan.VK10.*;

public class CrosshairRenderer {
    private final GpuBuffer vertexBuffer;
    private final int vertexCount;

    public CrosshairRenderer(VkDevice device, VkPhysicalDevice physicalDevice, float aspectRatio) {
        float verticalSize = 0.018f;
        float horizontalSize = verticalSize / aspectRatio;

        float[] vertices = {
                -horizontalSize, 0.0f,   1.0f, 1.0f, 1.0f,
                horizontalSize, 0.0f,    1.0f, 1.0f, 1.0f,

                0.0f, -verticalSize,   1.0f, 1.0f, 1.0f,
                0.0f, verticalSize,    1.0f, 1.0f, 1.0f,
        };

        vertexCount = vertices.length / 5;

        vertexBuffer = new GpuBuffer(device);
        vertexBuffer.create(
                physicalDevice,
                (long)vertices.length * Float.BYTES,
                VK_BUFFER_USAGE_VERTEX_BUFFER_BIT,
                VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT
        );
        vertexBuffer.uploadData(vertices);
    }

    public GpuBuffer getVertexBuffer() {
        return vertexBuffer;
    }

    public int getVertexCount() {
        return vertexCount;
    }

    public void destroy() {
        vertexBuffer.destroy();
    }
}