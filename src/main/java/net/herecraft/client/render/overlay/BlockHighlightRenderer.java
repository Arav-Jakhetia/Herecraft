package net.herecraft.client.render.overlay;

import net.herecraft.client.render.buffer.GpuBuffer;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkPhysicalDevice;

import static org.lwjgl.vulkan.VK10.*;

public class BlockHighlightRenderer {
    private final GpuBuffer vertexBuffer;
    private final int vertexCount;

    public BlockHighlightRenderer(VkDevice device, VkPhysicalDevice physicalDevice) {
        float s = 0.002f;

        float[] vertices = {
                -s, -s, -s,  1 + s, -s, -s,
                1 + s, -s, -s,  1 + s, 1 + s, -s,
                1 + s, 1 + s, -s,  -s, 1 + s, -s,
                -s, 1 + s, -s,  -s, -s, -s,

                -s, -s, 1 + s,  1 + s, -s, 1 + s,
                1 + s, -s, 1 + s,  1 + s, 1 + s, 1 + s,
                1 + s, 1 + s, 1 + s,  -s, 1 + s, 1 + s,
                -s, 1 + s, 1 + s,  -s, -s, 1 + s,

                -s, -s, -s,  -s, -s, 1 + s,
                1 + s, -s, -s,  1 + s, -s, 1 + s,
                1 + s, 1 + s, -s,  1 + s, 1 + s, 1 + s,
                -s, 1 + s, -s,  -s, 1 + s, 1 + s
        };

        vertexCount = vertices.length / 3;

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