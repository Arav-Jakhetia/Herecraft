package net.herecraft.client.render.buffer;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class UniformBuffer {
    private final VkDevice device;
    private long buffer;
    private long memory;
    private ByteBuffer mapped;

    public UniformBuffer(VkDevice device) {
        this.device = device;
    }

    public void create(VkPhysicalDevice physicalDevice, long size) {
        try(MemoryStack stack = stackPush()) {
            VkBufferCreateInfo bufferInfo = VkBufferCreateInfo.calloc(stack);
            bufferInfo.sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO);
            bufferInfo.size(size);
            bufferInfo.usage(VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT);
            bufferInfo.sharingMode(VK_SHARING_MODE_EXCLUSIVE);

            LongBuffer bufferPointer = stack.mallocLong(1);
            if(vkCreateBuffer(device, bufferInfo, null, bufferPointer) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create uniform buffer");
            }
            buffer = bufferPointer.get(0);

            VkMemoryRequirements memRequirements = VkMemoryRequirements.calloc(stack);
            vkGetBufferMemoryRequirements(device, buffer, memRequirements);

            int memoryTypeIndex = findMemoryType(physicalDevice, memRequirements.memoryTypeBits(),
                    VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, stack);

            VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.calloc(stack);
            allocInfo.sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO);
            allocInfo.allocationSize(memRequirements.size());
            allocInfo.memoryTypeIndex(memoryTypeIndex);

            LongBuffer memoryPointer = stack.mallocLong(1);
            if(vkAllocateMemory(device, allocInfo, null, memoryPointer) != VK_SUCCESS) {
                throw new RuntimeException("Failed to allocate uniform buffer memory");
            }
            memory = memoryPointer.get(0);

            vkBindBufferMemory(device, buffer, memory, 0);

            PointerBuffer mappedPointer = stack.mallocPointer(1);
            vkMapMemory(device, memory, 0, size, 0, mappedPointer);
            mapped = mappedPointer.getByteBuffer(0, (int)size);
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

    public void update(float[] matrix) {
        mapped.asFloatBuffer().put(matrix).flip();
    }

    public long getBuffer() {
        return buffer;
    }

    public void destroy() {
        vkDestroyBuffer(device, buffer, null);
        vkFreeMemory(device, memory, null);
    }
}