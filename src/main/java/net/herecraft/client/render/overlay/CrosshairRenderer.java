package net.herecraft.client.render.overlay;

import net.herecraft.client.render.buffer.GpuBuffer;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkPhysicalDevice;

import static org.lwjgl.vulkan.VK10.*;

public class CrosshairRenderer {
    private final GpuBuffer vertexBuffer;
    private final int vertexCount;

    public CrosshairRenderer(VkDevice device, VkPhysicalDevice physicalDevice) {
        float size = 0.015f;
        float gap = 0.004f;

        float[] vertices = {
                -size, 0.0f,   0, 0, 0,
                -gap, 0.0f,    0, 0, 0,

                gap, 0.0f,     0, 0, 0,
                size, 0.0f,    0, 0, 0,

                0.0f, -size,   0, 0, 0,
                0.0f, -gap,    0, 0, 0,

                0.0f, gap,     0, 0, 0,
                0.0f, size,    0, 0, 0
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