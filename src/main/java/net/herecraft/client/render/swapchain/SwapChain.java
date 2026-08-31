package net.herecraft.client.render.swapchain;

import net.herecraft.client.render.device.QueueFamilyIndices;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

public class SwapChain {
    private VkDevice device;
    private long swapChain;
    private List<Long> images = new ArrayList<>();
    private List<Long> imageViews = new ArrayList<>();
    private int imageFormat;
    private VkExtent2D extent;
    private List<Long> framebuffers = new ArrayList<>();

    public void create(VkPhysicalDevice physicalDevice, VkDevice device, long surface, QueueFamilyIndices indices, int windowWidth, int windowHeight) {
        this.device = device;

        try(MemoryStack stack = stackPush()) {
            SwapChainSupportDetails support = querySupport(physicalDevice, surface, stack);

            VkSurfaceFormatKHR surfaceFormat = chooseSurfaceFormat(support.formats);
            int presentMode = choosePresentMode(support.presentModes);
            VkExtent2D chosenExtent = chooseExtent(support.capabilities, windowWidth, windowHeight, stack);

            int imageCount = support.capabilities.minImageCount() + 1;
            if(support.capabilities.maxImageCount() > 0 && imageCount > support.capabilities.maxImageCount()) {
                imageCount = support.capabilities.maxImageCount();
            }

            VkSwapchainCreateInfoKHR createInfo = VkSwapchainCreateInfoKHR.calloc(stack);
            createInfo.sType(VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR);
            createInfo.surface(surface);
            createInfo.minImageCount(imageCount);
            createInfo.imageFormat(surfaceFormat.format());
            createInfo.imageColorSpace(surfaceFormat.colorSpace());
            createInfo.imageExtent(chosenExtent);
            createInfo.imageArrayLayers(1);
            createInfo.imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT);

            if(!indices.graphicsFamily.equals(indices.presentFamily)) {
                createInfo.imageSharingMode(VK_SHARING_MODE_CONCURRENT);
                createInfo.pQueueFamilyIndices(stack.ints(indices.graphicsFamily, indices.presentFamily));
            } else {
                createInfo.imageSharingMode(VK_SHARING_MODE_EXCLUSIVE);
            }

            createInfo.preTransform(support.capabilities.currentTransform());
            createInfo.compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR);
            createInfo.presentMode(presentMode);
            createInfo.clipped(true);
            createInfo.oldSwapchain(VK_NULL_HANDLE);

            LongBuffer swapChainPointer = stack.longs(VK_NULL_HANDLE);
            if(vkCreateSwapchainKHR(device, createInfo, null, swapChainPointer) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create swap chain");
            }
            swapChain = swapChainPointer.get(0);

            IntBuffer actualImageCount = stack.ints(0);
            vkGetSwapchainImagesKHR(device, swapChain, actualImageCount, null);

            LongBuffer imagesBuffer = stack.mallocLong(actualImageCount.get(0));
            vkGetSwapchainImagesKHR(device, swapChain, actualImageCount, imagesBuffer);

            for(int i = 0; i < imagesBuffer.capacity(); i++) {
                images.add(imagesBuffer.get(i));
            }

            imageFormat = surfaceFormat.format();
            extent = VkExtent2D.create().set(chosenExtent);

            createImageViews(stack);
        }
    }

    private SwapChainSupportDetails querySupport(VkPhysicalDevice physicalDevice, long surface, MemoryStack stack) {
        SwapChainSupportDetails details = new SwapChainSupportDetails();

        details.capabilities = VkSurfaceCapabilitiesKHR.calloc(stack);
        vkGetPhysicalDeviceSurfaceCapabilitiesKHR(physicalDevice, surface, details.capabilities);

        IntBuffer formatCount = stack.ints(0);
        vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, surface, formatCount, null);
        details.formats = VkSurfaceFormatKHR.calloc(formatCount.get(0), stack);
        vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, surface, formatCount, details.formats);

        IntBuffer presentModeCount = stack.ints(0);
        vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice, surface, presentModeCount, null);
        details.presentModes = stack.mallocInt(presentModeCount.get(0));
        vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice, surface, presentModeCount, details.presentModes);

        return details;
    }

    private VkSurfaceFormatKHR chooseSurfaceFormat(VkSurfaceFormatKHR.Buffer formats) {
        for(VkSurfaceFormatKHR format : formats) {
            if(format.format() == VK_FORMAT_B8G8R8A8_SRGB && format.colorSpace() == VK_COLOR_SPACE_SRGB_NONLINEAR_KHR) {
                return format;
            }
        }
        return formats.get(0);
    }

    private int choosePresentMode(IntBuffer presentModes) {
        for(int i = 0; i < presentModes.capacity(); i++) {
            if(presentModes.get(i) == VK_PRESENT_MODE_MAILBOX_KHR) {
                return VK_PRESENT_MODE_MAILBOX_KHR;
            }
        }
        return VK_PRESENT_MODE_FIFO_KHR;
    }

    private VkExtent2D chooseExtent(VkSurfaceCapabilitiesKHR capabilities, int windowWidth, int windowHeight, MemoryStack stack) {
        if(capabilities.currentExtent().width() != 0xFFFFFFFF) {
            return capabilities.currentExtent();
        }

        VkExtent2D actualExtent = VkExtent2D.calloc(stack);
        actualExtent.width(Math.max(capabilities.minImageExtent().width(),
                Math.min(capabilities.maxImageExtent().width(), windowWidth)));
        actualExtent.height(Math.max(capabilities.minImageExtent().height(),
                Math.min(capabilities.maxImageExtent().height(), windowHeight)));
        return actualExtent;
    }

    private void createImageViews(MemoryStack stack) {
        for(long image : images) {
            VkImageViewCreateInfo createInfo = VkImageViewCreateInfo.calloc(stack);
            createInfo.sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO);
            createInfo.image(image);
            createInfo.viewType(VK_IMAGE_VIEW_TYPE_2D);
            createInfo.format(imageFormat);

            createInfo.components().r(VK_COMPONENT_SWIZZLE_IDENTITY);
            createInfo.components().g(VK_COMPONENT_SWIZZLE_IDENTITY);
            createInfo.components().b(VK_COMPONENT_SWIZZLE_IDENTITY);
            createInfo.components().a(VK_COMPONENT_SWIZZLE_IDENTITY);

            createInfo.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
            createInfo.subresourceRange().baseMipLevel(0);
            createInfo.subresourceRange().levelCount(1);
            createInfo.subresourceRange().baseArrayLayer(0);
            createInfo.subresourceRange().layerCount(1);

            LongBuffer imageViewPointer = stack.mallocLong(1);
            if(vkCreateImageView(device, createInfo, null, imageViewPointer) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create image view");
            }
            imageViews.add(imageViewPointer.get(0));
        }
    }

    public void createFramebuffers(long renderPass, long depthImageView) {
        try(MemoryStack stack = stackPush()) {
            for(long imageView : imageViews) {
                LongBuffer attachments = stack.longs(imageView, depthImageView);

                VkFramebufferCreateInfo framebufferInfo = VkFramebufferCreateInfo.calloc(stack);
                framebufferInfo.sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO);
                framebufferInfo.renderPass(renderPass);
                framebufferInfo.pAttachments(attachments);
                framebufferInfo.width(extent.width());
                framebufferInfo.height(extent.height());
                framebufferInfo.layers(1);

                LongBuffer framebufferPointer = stack.mallocLong(1);
                if(vkCreateFramebuffer(device, framebufferInfo, null, framebufferPointer) != VK_SUCCESS) {
                    throw new RuntimeException("Failed to create framebuffer");
                }
                framebuffers.add(framebufferPointer.get(0));
            }
        }
    }

    public long getSwapChain() {
        return swapChain;
    }

    public List<Long> getImages() {
        return images;
    }

    public int getImageFormat() {
        return imageFormat;
    }

    public VkExtent2D getExtent() {
        return extent;
    }

    public List<Long> getFramebuffers() {
        return framebuffers;
    }

    public void destroy() {
        for(long framebuffer : framebuffers) {
            vkDestroyFramebuffer(device, framebuffer, null);
        }
        for(long imageView : imageViews) {
            vkDestroyImageView(device, imageView, null);
        }
        vkDestroySwapchainKHR(device, swapChain, null);
    }
}
