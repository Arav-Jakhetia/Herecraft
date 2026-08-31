package net.herecraft.client.render.sync;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkFenceCreateInfo;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;

import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class SyncObjects {
    public static final int MAX_FRAMES_IN_FLIGHT = 2;

    private VkDevice device;
    private long[] imageAvailableSemaphores = new long[MAX_FRAMES_IN_FLIGHT];
    private long[] renderFinishedSemaphores = new long[MAX_FRAMES_IN_FLIGHT];
    private long[] inFlightFences = new long[MAX_FRAMES_IN_FLIGHT];

    public void create(VkDevice device) {
        this.device = device;

        try(MemoryStack stack = stackPush()) {
            VkSemaphoreCreateInfo semaphoreInfo = VkSemaphoreCreateInfo.calloc(stack);
            semaphoreInfo.sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);

            VkFenceCreateInfo fenceInfo = VkFenceCreateInfo.calloc(stack);
            fenceInfo.sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO);
            fenceInfo.flags(VK_FENCE_CREATE_SIGNALED_BIT);

            for(int i = 0; i < MAX_FRAMES_IN_FLIGHT; i++) {
                LongBuffer semaphorePointer1 = stack.mallocLong(1);
                LongBuffer semaphorePointer2 = stack.mallocLong(1);
                LongBuffer fencePointer = stack.mallocLong(1);

                if(vkCreateSemaphore(device, semaphoreInfo, null, semaphorePointer1) != VK_SUCCESS ||
                        vkCreateSemaphore(device, semaphoreInfo, null, semaphorePointer2) != VK_SUCCESS ||
                        vkCreateFence(device, fenceInfo, null, fencePointer) != VK_SUCCESS) {
                    throw new RuntimeException("Failed to create sync objects for frame " + i);
                }

                imageAvailableSemaphores[i] = semaphorePointer1.get(0);
                renderFinishedSemaphores[i] = semaphorePointer2.get(0);
                inFlightFences[i] = fencePointer.get(0);
            }
        }
    }

    public long getImageAvailableSemaphore(int frame) {
        return imageAvailableSemaphores[frame];
    }

    public long getRenderFinishedSemaphore(int frame) {
        return renderFinishedSemaphores[frame];
    }

    public long getInFlightFence(int frame) {
        return inFlightFences[frame];
    }

    public void destroy() {
        for(int i = 0; i < MAX_FRAMES_IN_FLIGHT; i++) {
            vkDestroySemaphore(device, imageAvailableSemaphores[i], null);
            vkDestroySemaphore(device, renderFinishedSemaphores[i], null);
            vkDestroyFence(device, inFlightFences[i], null);
        }
    }
}