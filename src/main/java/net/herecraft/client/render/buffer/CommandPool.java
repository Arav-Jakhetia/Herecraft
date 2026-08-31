package net.herecraft.client.render.buffer;

import net.herecraft.client.render.device.QueueFamilyIndices;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class CommandPool {
    private VkDevice device;
    private long commandPool;
    private VkCommandBuffer[] commandBuffers;

    public void create(VkDevice device, QueueFamilyIndices indices, int count) {
        this.device = device;

        try(MemoryStack stack = stackPush()) {
            VkCommandPoolCreateInfo poolInfo = VkCommandPoolCreateInfo.calloc(stack);
            poolInfo.sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO);
            poolInfo.flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT);
            poolInfo.queueFamilyIndex(indices.graphicsFamily);

            org.lwjgl.system.MemoryStack.stackGet();
            java.nio.LongBuffer poolPointer = stack.mallocLong(1);
            if(vkCreateCommandPool(device, poolInfo, null, poolPointer) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create command pool");
            }
            commandPool = poolPointer.get(0);

            VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.calloc(stack);
            allocInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
            allocInfo.commandPool(commandPool);
            allocInfo.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY);
            allocInfo.commandBufferCount(count);

            PointerBuffer buffersPointer = stack.mallocPointer(count);
            if(vkAllocateCommandBuffers(device, allocInfo, buffersPointer) != VK_SUCCESS) {
                throw new RuntimeException("Failed to allocate command buffers");
            }

            commandBuffers = new VkCommandBuffer[count];
            for(int i = 0; i < count; i++) {
                commandBuffers[i] = new VkCommandBuffer(buffersPointer.get(i), device);
            }
        }
    }

    public VkCommandBuffer getCommandBuffer(int index) {
        return commandBuffers[index];
    }

    public long getCommandPool() {
        return commandPool;
    }

    public void destroy() {
        vkDestroyCommandPool(device, commandPool, null);
    }
}