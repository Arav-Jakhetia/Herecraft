package net.herecraft.client.render.device;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.util.HashSet;
import java.util.Set;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class LogicalDevice {
    private VkDevice device;
    private VkQueue graphicsQueue;
    private VkQueue presentQueue;

    public void create(VkPhysicalDevice physicalDevice, QueueFamilyIndices indices) {
        try(MemoryStack stack = stackPush()) {
            Set<Integer> uniqueFamilies = new HashSet<>();
            uniqueFamilies.add(indices.graphicsFamily);
            uniqueFamilies.add(indices.presentFamily);

            VkDeviceQueueCreateInfo.Buffer queueCreateInfos = VkDeviceQueueCreateInfo.calloc(uniqueFamilies.size(), stack);

            int index = 0;
            for(int family : uniqueFamilies) {
                VkDeviceQueueCreateInfo queueCreateInfo = queueCreateInfos.get(index++);
                queueCreateInfo.sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO);
                queueCreateInfo.queueFamilyIndex(family);
                queueCreateInfo.pQueuePriorities(stack.floats(1.0f));
            }

            VkPhysicalDeviceFeatures deviceFeatures = VkPhysicalDeviceFeatures.calloc(stack);
            deviceFeatures.wideLines(true);

            PointerBuffer deviceExtensions = stack.pointers(stack.UTF8(KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME));

            VkDeviceCreateInfo createInfo = VkDeviceCreateInfo.calloc(stack);
            createInfo.sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO);
            createInfo.pQueueCreateInfos(queueCreateInfos);
            createInfo.pEnabledFeatures(deviceFeatures);
            createInfo.ppEnabledExtensionNames(deviceExtensions);

            PointerBuffer devicePointer = stack.mallocPointer(1);
            if(vkCreateDevice(physicalDevice, createInfo, null, devicePointer) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create logical device");
            }

            device = new VkDevice(devicePointer.get(0), physicalDevice, createInfo);

            PointerBuffer graphicsQueuePointer = stack.mallocPointer(1);
            vkGetDeviceQueue(device, indices.graphicsFamily, 0, graphicsQueuePointer);
            graphicsQueue = new VkQueue(graphicsQueuePointer.get(0), device);

            PointerBuffer presentQueuePointer = stack.mallocPointer(1);
            vkGetDeviceQueue(device, indices.graphicsFamily, 0, presentQueuePointer);
            presentQueue = new VkQueue(presentQueuePointer.get(0), device);
        }
    }

    public VkDevice getDevice() {
        return device;
    }

    public VkQueue getGraphicsQueue() {
        return graphicsQueue;
    }

    public VkQueue getPresentQueue() {
        return presentQueue;
    }

    public void destroy() {
        vkDestroyDevice(device, null);
    }
}
