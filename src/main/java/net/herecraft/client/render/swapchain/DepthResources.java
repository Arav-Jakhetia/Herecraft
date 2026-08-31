package net.herecraft.client.render.swapchain;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class DepthResources {
    private VkDevice device;
    private long image;
    private long imageMemory;
    private long imageView;
    private int depthFormat;

    public void create(VkDevice device, VkPhysicalDevice physicalDevice, VkExtent2D extent) {
        this.device = device;
        this.depthFormat = findDepthFormat(physicalDevice);

        try(MemoryStack stack = stackPush()) {
            VkImageCreateInfo imageInfo = VkImageCreateInfo.calloc(stack);
            imageInfo.sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO);
            imageInfo.imageType(VK_IMAGE_TYPE_2D);
            imageInfo.extent().width(extent.width()).height(extent.height()).depth(1);
            imageInfo.mipLevels(1);
            imageInfo.arrayLayers(1);
            imageInfo.format(depthFormat);
            imageInfo.tiling(VK_IMAGE_TILING_OPTIMAL);
            imageInfo.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
            imageInfo.usage(VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT);
            imageInfo.sharingMode(VK_SHARING_MODE_EXCLUSIVE);
            imageInfo.samples(VK_SAMPLE_COUNT_1_BIT);

            LongBuffer imagePointer = stack.mallocLong(1);
            if(vkCreateImage(device, imageInfo, null, imagePointer) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create depth image");
            }
            image = imagePointer.get(0);

            VkMemoryRequirements memRequirements = VkMemoryRequirements.calloc(stack);
            vkGetImageMemoryRequirements(device, image, memRequirements);

            VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.calloc(stack);
            allocInfo.sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO);
            allocInfo.allocationSize(memRequirements.size());
            allocInfo.memoryTypeIndex(findMemoryType(physicalDevice, memRequirements.memoryTypeBits(),
                    VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, stack));

            LongBuffer memoryPointer = stack.mallocLong(1);
            if(vkAllocateMemory(device, allocInfo, null, memoryPointer) != VK_SUCCESS) {
                throw new RuntimeException("Failed to allocate depth image memory");
            }
            imageMemory = memoryPointer.get(0);

            vkBindImageMemory(device, image, imageMemory, 0);

            VkImageViewCreateInfo viewInfo = VkImageViewCreateInfo.calloc(stack);
            viewInfo.sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO);
            viewInfo.image(image);
            viewInfo.viewType(VK_IMAGE_VIEW_TYPE_2D);
            viewInfo.format(depthFormat);
            viewInfo.subresourceRange().aspectMask(VK_IMAGE_ASPECT_DEPTH_BIT);
            viewInfo.subresourceRange().baseMipLevel(0);
            viewInfo.subresourceRange().levelCount(1);
            viewInfo.subresourceRange().baseArrayLayer(0);
            viewInfo.subresourceRange().layerCount(1);

            LongBuffer viewPointer = stack.mallocLong(1);
            if(vkCreateImageView(device, viewInfo, null, viewPointer) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create depth image view");
            }
            imageView = viewPointer.get(0);
        }
    }

    private int findDepthFormat(VkPhysicalDevice physicalDevice) {
        int[] candidates = { VK_FORMAT_D32_SFLOAT, VK_FORMAT_D32_SFLOAT_S8_UINT, VK_FORMAT_D24_UNORM_S8_UINT };

        try(MemoryStack stack = stackPush()) {
            for(int format : candidates) {
                VkFormatProperties props = VkFormatProperties.calloc(stack);
                vkGetPhysicalDeviceFormatProperties(physicalDevice, format, props);

                if((props.optimalTilingFeatures() & VK_FORMAT_FEATURE_DEPTH_STENCIL_ATTACHMENT_BIT) != 0) {
                    return format;
                }
            }
        }

        throw new RuntimeException("Failed to find a supported depth format");
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
        throw new RuntimeException("Failed to find suitable GPU memory type for depth image");
    }

    public long getImageView() {
        return imageView;
    }

    public int getDepthFormat() {
        return depthFormat;
    }

    public void destroy() {
        vkDestroyImageView(device, imageView, null);
        vkDestroyImage(device, image, null);
        vkFreeMemory(device, imageMemory, null);
    }
}