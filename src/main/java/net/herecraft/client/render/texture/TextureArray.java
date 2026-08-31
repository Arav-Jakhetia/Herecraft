package net.herecraft.client.render.texture;

import net.herecraft.client.render.buffer.GpuBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.stb.STBImage.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class TextureArray {
    private static final int TEXTURE_SIZE = 16;
    private int textureSize;

    private final VkDevice device;
    private final VkPhysicalDevice physicalDevice;
    private final long commandPool;
    private final VkQueue graphicsQueue;

    private long image;
    private long imageMemory;
    private long imageView;
    private long sampler;

    public TextureArray(VkDevice device, VkPhysicalDevice physicalDevice, long commandPool, VkQueue graphicsQueue) {
        this.device = device;
        this.physicalDevice = physicalDevice;
        this.commandPool = commandPool;
        this.graphicsQueue = graphicsQueue;
    }

    public void create(String[] paths) {
        textureSize = 16;
        ByteBuffer[] pixelLayers = new ByteBuffer[paths.length];
        for(int i = 0; i < paths.length; i++) {
            pixelLayers[i] = loadImage(paths[i]);
        }
        buildFromPixels(pixelLayers);
        for(ByteBuffer pixels : pixelLayers) {
            pixels.rewind();
            stbi_image_free(pixels);
        }
    }

    public void createFromPixels(ByteBuffer[] pixelLayers, int size) {
        textureSize = size;
        buildFromPixels(pixelLayers);
    }

    private void buildFromPixels(ByteBuffer[] pixelLayers) {
        int layerCount = pixelLayers.length;
        long imageSize = (long)textureSize * textureSize * 4 * layerCount;

        GpuBuffer stagingBuffer = new GpuBuffer(device);
        stagingBuffer.create(physicalDevice, imageSize,
                VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);

        ByteBuffer combined = BufferUtils.createByteBuffer((int)imageSize);
        for(ByteBuffer pixels : pixelLayers) {
            combined.put(pixels);
        }
        combined.flip();
        stagingBuffer.uploadRaw(combined);

        createImage(layerCount);
        transitionImageLayout(VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, layerCount);
        copyBufferToImage(stagingBuffer.getBuffer(), layerCount);
        transitionImageLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL, layerCount);

        stagingBuffer.destroy();

        createImageView(layerCount);
        createSampler();
    }

    private void createImage(int layerCount) {
        try(MemoryStack stack = stackPush()) {
            VkImageCreateInfo imageInfo = VkImageCreateInfo.calloc(stack);
            imageInfo.sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO);
            imageInfo.imageType(VK_IMAGE_TYPE_2D);
            imageInfo.extent().width(TEXTURE_SIZE).height(TEXTURE_SIZE).depth(1);
            imageInfo.mipLevels(1);
            imageInfo.arrayLayers(layerCount);
            imageInfo.format(VK_FORMAT_R8G8B8A8_SRGB);
            imageInfo.tiling(VK_IMAGE_TILING_OPTIMAL);
            imageInfo.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
            imageInfo.usage(VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT);
            imageInfo.sharingMode(VK_SHARING_MODE_EXCLUSIVE);
            imageInfo.samples(VK_SAMPLE_COUNT_1_BIT);

            LongBuffer imagePointer = stack.mallocLong(1);
            if(vkCreateImage(device, imageInfo, null, imagePointer) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create texture array image");
            }
            image = imagePointer.get(0);

            VkMemoryRequirements memRequirements = VkMemoryRequirements.calloc(stack);
            vkGetImageMemoryRequirements(device, image, memRequirements);

            VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.calloc(stack);
            allocInfo.sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO);
            allocInfo.allocationSize(memRequirements.size());
            allocInfo.memoryTypeIndex(findMemoryType(memRequirements.memoryTypeBits(), VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, stack));

            LongBuffer memoryPointer = stack.mallocLong(1);
            if(vkAllocateMemory(device, allocInfo, null, memoryPointer) != VK_SUCCESS) {
                throw new RuntimeException("Failed to allocate texture image memory");
            }
            imageMemory = memoryPointer.get(0);

            vkBindImageMemory(device, image, imageMemory, 0);
        }
    }

    private int findMemoryType(int typeFilter, int properties, MemoryStack stack) {
        VkPhysicalDeviceMemoryProperties memProperties = VkPhysicalDeviceMemoryProperties.calloc(stack);
        vkGetPhysicalDeviceMemoryProperties(physicalDevice, memProperties);

        for(int i = 0; i < memProperties.memoryTypeCount(); i++) {
            boolean typeMatches = (typeFilter & (1 << i)) != 0;
            boolean propsMatch = (memProperties.memoryTypes(i).propertyFlags() & properties) == properties;
            if(typeMatches && propsMatch) {
                return i;
            }
        }
        throw new RuntimeException("Failed to find suitable GPU memory type for image");
    }

    private VkCommandBuffer beginSingleTimeCommands(MemoryStack stack) {
        VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.calloc(stack);
        allocInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
        allocInfo.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY);
        allocInfo.commandPool(commandPool);
        allocInfo.commandBufferCount(1);

        PointerBuffer commandBufferPointer = stack.mallocPointer(1);
        vkAllocateCommandBuffers(device, allocInfo, commandBufferPointer);
        VkCommandBuffer commandBuffer = new VkCommandBuffer(commandBufferPointer.get(0), device);

        VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack);
        beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);
        beginInfo.flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);

        vkBeginCommandBuffer(commandBuffer, beginInfo);
        return commandBuffer;
    }

    private void endSingleTimeCommands(VkCommandBuffer commandBuffer, MemoryStack stack) {
        vkEndCommandBuffer(commandBuffer);

        VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack);
        submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);
        submitInfo.pCommandBuffers(stack.pointers(commandBuffer));

        vkQueueSubmit(graphicsQueue, submitInfo, VK_NULL_HANDLE);
        vkQueueWaitIdle(graphicsQueue);

        vkFreeCommandBuffers(device, commandPool, commandBuffer);
    }

    private void transitionImageLayout(int oldLayout, int newLayout, int layerCount) {
        try(MemoryStack stack = stackPush()) {
            VkCommandBuffer commandBuffer = beginSingleTimeCommands(stack);

            VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.calloc(1, stack);
            barrier.sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER);
            barrier.oldLayout(oldLayout);
            barrier.newLayout(newLayout);
            barrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
            barrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
            barrier.image(image);
            barrier.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
            barrier.subresourceRange().baseMipLevel(0);
            barrier.subresourceRange().levelCount(1);
            barrier.subresourceRange().baseArrayLayer(0);
            barrier.subresourceRange().layerCount(layerCount);

            int sourceStage;
            int destinationStage;

            if(oldLayout == VK_IMAGE_LAYOUT_UNDEFINED && newLayout == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL) {
                barrier.srcAccessMask(0);
                barrier.dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
                sourceStage = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
                destinationStage = VK_PIPELINE_STAGE_TRANSFER_BIT;
            } else {
                barrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
                barrier.dstAccessMask(VK_ACCESS_SHADER_READ_BIT);
                sourceStage = VK_PIPELINE_STAGE_TRANSFER_BIT;
                destinationStage = VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT;
            }

            vkCmdPipelineBarrier(commandBuffer, sourceStage, destinationStage, 0, null, null, barrier);

            endSingleTimeCommands(commandBuffer, stack);
        }
    }

    private void copyBufferToImage(long buffer, int layerCount) {
        try(MemoryStack stack = stackPush()) {
            VkCommandBuffer commandBuffer = beginSingleTimeCommands(stack);

            VkBufferImageCopy.Buffer region = VkBufferImageCopy.calloc(1, stack);
            region.bufferOffset(0);
            region.bufferRowLength(0);
            region.bufferImageHeight(0);
            region.imageSubresource().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
            region.imageSubresource().mipLevel(0);
            region.imageSubresource().baseArrayLayer(0);
            region.imageSubresource().layerCount(layerCount);
            region.imageOffset().set(0, 0, 0);
            region.imageExtent().set(TEXTURE_SIZE, TEXTURE_SIZE, 1);

            vkCmdCopyBufferToImage(commandBuffer, buffer, image, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, region);

            endSingleTimeCommands(commandBuffer, stack);
        }
    }

    private void createImageView(int layerCount) {
        try(MemoryStack stack = stackPush()) {
            VkImageViewCreateInfo viewInfo = VkImageViewCreateInfo.calloc(stack);
            viewInfo.sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO);
            viewInfo.image(image);
            viewInfo.viewType(VK_IMAGE_VIEW_TYPE_2D_ARRAY);
            viewInfo.format(VK_FORMAT_R8G8B8A8_SRGB);
            viewInfo.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
            viewInfo.subresourceRange().baseMipLevel(0);
            viewInfo.subresourceRange().levelCount(1);
            viewInfo.subresourceRange().baseArrayLayer(0);
            viewInfo.subresourceRange().layerCount(layerCount);

            LongBuffer viewPointer = stack.mallocLong(1);
            if(vkCreateImageView(device, viewInfo, null, viewPointer) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create texture array image view");
            }
            imageView = viewPointer.get(0);
        }
    }

    private void createSampler() {
        try(MemoryStack stack = stackPush()) {
            VkSamplerCreateInfo samplerInfo = VkSamplerCreateInfo.calloc(stack);
            samplerInfo.sType(VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO);
            samplerInfo.magFilter(VK_FILTER_NEAREST);
            samplerInfo.minFilter(VK_FILTER_NEAREST);
            samplerInfo.addressModeU(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE);
            samplerInfo.addressModeV(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE);
            samplerInfo.addressModeW(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE);
            samplerInfo.anisotropyEnable(false);
            samplerInfo.maxAnisotropy(1.0f);
            samplerInfo.borderColor(VK_BORDER_COLOR_INT_OPAQUE_BLACK);
            samplerInfo.unnormalizedCoordinates(false);
            samplerInfo.compareEnable(false);
            samplerInfo.compareOp(VK_COMPARE_OP_ALWAYS);
            samplerInfo.mipmapMode(VK_SAMPLER_MIPMAP_MODE_NEAREST);

            LongBuffer samplerPointer = stack.mallocLong(1);
            if(vkCreateSampler(device, samplerInfo, null, samplerPointer) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create texture sampler");
            }
            sampler = samplerPointer.get(0);
        }
    }

    private ByteBuffer loadImage(String path) {
        try(MemoryStack stack = stackPush()) {
            ByteBuffer fileBuffer = readResource(path);

            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            ByteBuffer pixels = stbi_load_from_memory(fileBuffer, width, height, channels, 4);

            if(pixels == null) {
                throw new RuntimeException("Failed to load texture: " + path);
            }
            if(width.get(0) != TEXTURE_SIZE || height.get(0) != TEXTURE_SIZE) {
                throw new RuntimeException("Texture must be 16x16: " + path);
            }

            return pixels;
        }
    }

    private ByteBuffer readResource(String path) {
        try(InputStream stream = TextureArray.class.getResourceAsStream(path)) {
            if(stream == null) {
                throw new RuntimeException("Missing resource: " + path);
            }
            byte[] bytes = stream.readAllBytes();
            ByteBuffer buffer = BufferUtils.createByteBuffer(bytes.length);
            buffer.put(bytes);
            buffer.flip();
            return buffer;
        } catch(IOException e) {
            throw new RuntimeException("Could not read resource: " + path, e);
        }
    }

    public long getImageView() {
        return imageView;
    }

    public long getSampler() {
        return sampler;
    }

    public void destroy() {
        vkDestroySampler(device, sampler, null);
        vkDestroyImageView(device, imageView, null);
        vkDestroyImage(device, image, null);
        vkFreeMemory(device, imageMemory, null);
    }
}