package net.herecraft.client.render.device;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class PhysicalDeviceSelector {
    public static VkPhysicalDevice pickPhysicalDevice(VkInstance instance, long surface) {
        try(MemoryStack stack = stackPush()) {
            IntBuffer deviceCount = stack.ints(0);
            vkEnumeratePhysicalDevices(instance, deviceCount, null);

            if(deviceCount.get(0) == 0) {
                throw new RuntimeException("No GPUs found with Vulkan support");
            }

            PointerBuffer devicePointers = stack.mallocPointer(deviceCount.get(0));
            vkEnumeratePhysicalDevices(instance, deviceCount, devicePointers);

            for(int i = 0; i < devicePointers.capacity(); i++) {
                VkPhysicalDevice device = new VkPhysicalDevice(devicePointers.get(i), instance);
                if(isDeviceSuitable(device, surface)) {
                    return device;
                }
            }

            throw new RuntimeException("No suitable GPU found (needs graphics queue + present support)");
        }
    }

    private static boolean isDeviceSuitable(VkPhysicalDevice device, long surface) {
        QueueFamilyIndices indices = findQueueFamilies(device, surface);
        return indices.isComplete();
    }

    public static QueueFamilyIndices findQueueFamilies(VkPhysicalDevice device, long surface) {
        QueueFamilyIndices indices = new QueueFamilyIndices();

        try(MemoryStack stack = stackPush()) {
            IntBuffer queueFamilyCount = stack.ints(0);
            vkGetPhysicalDeviceQueueFamilyProperties(device, queueFamilyCount, null);

            VkQueueFamilyProperties.Buffer queueFamilies = VkQueueFamilyProperties.calloc(queueFamilyCount.get(0), stack);
            vkGetPhysicalDeviceQueueFamilyProperties(device, queueFamilyCount, queueFamilies);

            IntBuffer presentSupport = stack.ints(VK_FALSE);

            for(int i = 0; i < queueFamilies.capacity(); i++) {
                if((queueFamilies.get(i).queueFlags() & VK_QUEUE_GRAPHICS_BIT) != 0) {
                    indices.graphicsFamily = i;
                }

                KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR(device, i, surface, presentSupport);
                if(presentSupport.get(0) == VK_TRUE) {
                    indices.presentFamily = i;
                }

                if(indices.isComplete()) {
                    break;
                }
            }
        }

        return indices;
    }
}
