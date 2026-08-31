package net.herecraft.client.render.buffer;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class GpuBuffer {
    private final VkDevice device;
    private long buffer;
    private long memory;
    private long size;

    public GpuBuffer(VkDevice device) {
        this.device = device;
    }

    public void create(VkPhysicalDevice physicalDevice, long size, int usage, int properties) {
        this.size = size;

        try(MemoryStack stack = stackPush()) {
            VkBufferCreateInfo bufferInfo = VkBufferCreateInfo.calloc(stack);
            bufferInfo.sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO);
            bufferInfo.size(size);
            bufferInfo.usage(usage);
            bufferInfo.sharingMode(VK_SHARING_MODE_EXCLUSIVE);

            LongBuffer bufferPointer = stack.mallocLong(1);
            if(vkCreateBuffer(device, bufferInfo, null, bufferPointer) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create vertex buffer");
            }
            buffer = bufferPointer.get(0);

            VkMemoryRequirements memRequirements = VkMemoryRequirements.calloc(stack);
            vkGetBufferMemoryRequirements(device, buffer, memRequirements);

            VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.calloc(stack);
            allocInfo.sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO);
            allocInfo.allocationSize(memRequirements.size());
            allocInfo.memoryTypeIndex(findMemoryType(physicalDevice, memRequirements.memoryTypeBits(), properties, stack));

            LongBuffer memoryPointer = stack.mallocLong(1);
            if(vkAllocateMemory(device, allocInfo, null, memoryPointer) != VK_SUCCESS) {
                throw new RuntimeException("Failed to allocate vertex buffer memory");
            }
            memory = memoryPointer.get(0);

            vkBindBufferMemory(device, buffer, memory, 0);
        }
    }

    private int findMemoryType(VkPhysicalDevice physicalDevice, int typeFilter, int properties, MemoryStack stack) {
        VkPhysicalDeviceMemoryProperties memProperties = VkPhysicalDeviceMemoryProperties.calloc(stack);
        vkGetPhysicalDeviceMemoryProperties(physicalDevice, memProperties);

        for(int i = 0; i < memProperties.memoryTypeCount(); i++) {
            boolean typeMatches = (typeFilter & (1 << i)) != 0;
            boolean propsMatch = (memProperties.memoryTypes(i).propertyFlags() & properties) == properties;
            if(typeMatches && propsMatch) {
                return i;
            }
        }

        throw new RuntimeException("Failed to find suitable GPU memory type");
    }

    public void uploadData(float[] data) {
        try(MemoryStack stack = stackPush()) {
            PointerBuffer dataPointer = stack.mallocPointer(1);
            vkMapMemory(device, memory, 0, size, 0, dataPointer);

            ByteBuffer byteBuffer = dataPointer.getByteBuffer(0, (int)size);
            for(float value : data) {
                byteBuffer.putFloat(value);
            }
            byteBuffer.flip();

            vkUnmapMemory(device, memory);
        }
    }

    public void uploadRaw(ByteBuffer data) {
        try(MemoryStack stack = stackPush()) {
            PointerBuffer dataPointer = stack.mallocPointer(1);
            vkMapMemory(device, memory, 0, size, 0, dataPointer);

            ByteBuffer mapped = dataPointer.getByteBuffer(0, (int)size);
            mapped.put(data);

            vkUnmapMemory(device, memory);
        }
    }

    public long getBuffer() {
        return buffer;
    }

    public void destroy() {
        vkDestroyBuffer(device, buffer, null);
        vkFreeMemory(device, memory, null);
    }
}