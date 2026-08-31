package net.herecraft.client.render;

import net.herecraft.client.render.buffer.CommandPool;
import net.herecraft.client.render.buffer.GpuBuffer;
import net.herecraft.client.render.buffer.UniformBuffer;
import net.herecraft.client.render.device.LogicalDevice;
import net.herecraft.client.render.device.PhysicalDeviceSelector;
import net.herecraft.client.render.device.QueueFamilyIndices;
import net.herecraft.client.render.overlay.BlockHighlightRenderer;
import net.herecraft.client.render.overlay.ChunkRenderer;
import net.herecraft.client.render.overlay.CrosshairRenderer;
import net.herecraft.client.render.pipeline.*;
import net.herecraft.client.render.shader.ChunkShaders;
import net.herecraft.client.render.shader.HighlightShaders;
import net.herecraft.client.render.shader.OverlayShaders;
import net.herecraft.client.render.swapchain.DepthResources;
import net.herecraft.client.render.swapchain.RenderPass;
import net.herecraft.client.render.swapchain.SwapChain;
import net.herecraft.client.render.sync.SyncObjects;
import net.herecraft.client.render.texture.TextureArray;
import net.herecraft.client.world.Chunk;
import net.herecraft.client.world.World;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.EXTDebugUtils.*;
import static org.lwjgl.vulkan.KHRSwapchain.VK_STRUCTURE_TYPE_PRESENT_INFO_KHR;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK12.VK_API_VERSION_1_2;

public class RenderContext {
    private static final boolean ENABLE_VALIDATION = true;
    private static final String[] VALIDATION_LAYERS = { "VK_LAYER_KHRONOS_validation" };
    private VkInstance instance;
    private long debugMessenger = VK_NULL_HANDLE;
    private long surface;
    private VkPhysicalDevice physicalDevice;
    private QueueFamilyIndices queueFamilyIndices;
    private LogicalDevice logicalDevice;
    private SwapChain swapChain;
    private RenderPass renderPass;
    private ChunkShaders triangleShaders;
    private ChunkPipeline graphicsPipeline;
    private CommandPool commandPool;
    private SyncObjects syncObjects;
    private int currentFrame = 0;
    private GpuBuffer vertexBuffer;
    private DescriptorSetLayout descriptorSetLayout;
    private DescriptorPool descriptorPool;
    private UniformBuffer[] uniformBuffers;
    private static final long MVP_MATRIX_SIZE = 16L * Float.BYTES;
    private int chunkVertexCount;
    private TextureArray textureArray;
    private World world;
    private DepthResources depthResources;
    private OverlayShaders overlayShaders;
    private OverlayPipeline overlayPipeline;
    private CrosshairRenderer crosshairRenderer;
    private HighlightShaders highlightShaders;
    private HighlightPipeline highlightPipeline;
    private BlockHighlightRenderer blockHighlightRenderer;

    public void init(long window, int windowWidth, int windowHeight) {
        createInstance();
        setupDebugMessenger();
        createSurface(window);

        physicalDevice = PhysicalDeviceSelector.pickPhysicalDevice(instance, surface);
        queueFamilyIndices = PhysicalDeviceSelector.findQueueFamilies(physicalDevice, surface);

        logicalDevice = new LogicalDevice();
        logicalDevice.create(physicalDevice, queueFamilyIndices);

        swapChain = new SwapChain();
        swapChain.create(physicalDevice, logicalDevice.getDevice(), surface, queueFamilyIndices, windowWidth, windowHeight);

        depthResources = new DepthResources();
        depthResources.create(logicalDevice.getDevice(), physicalDevice, swapChain.getExtent());

        renderPass = new RenderPass();
        renderPass.create(logicalDevice.getDevice(), swapChain.getImageFormat(), depthResources.getDepthFormat());

        triangleShaders = new ChunkShaders(logicalDevice.getDevice());
        triangleShaders.compile();

        descriptorSetLayout = new DescriptorSetLayout();
        descriptorSetLayout.create(logicalDevice.getDevice());

        graphicsPipeline = new ChunkPipeline();
        graphicsPipeline.create(
                logicalDevice.getDevice(),
                renderPass.getRenderPass(),
                swapChain.getExtent(),
                triangleShaders.getVertexModule(),
                triangleShaders.getFragmentModule(),
                descriptorSetLayout.getLayout()
        );

        swapChain.createFramebuffers(renderPass.getRenderPass(), depthResources.getImageView());

        commandPool = new CommandPool();
        commandPool.create(logicalDevice.getDevice(), queueFamilyIndices, swapChain.getFramebuffers().size());

        textureArray = new TextureArray(logicalDevice.getDevice(), physicalDevice, commandPool.getCommandPool(), logicalDevice.getGraphicsQueue());
        textureArray.create(new String[] {
                "/assets/herecraft/textures/block/grass_block_top.png",
                "/assets/herecraft/textures/block/dirt.png",
                "/assets/herecraft/textures/block/stone.png",
                "/assets/herecraft/textures/block/grass_block_side.png"
        });

        syncObjects = new SyncObjects();
        syncObjects.create(logicalDevice.getDevice());

        uniformBuffers = new UniformBuffer[SyncObjects.MAX_FRAMES_IN_FLIGHT];
        for(int i = 0; i < SyncObjects.MAX_FRAMES_IN_FLIGHT; i++) {
            uniformBuffers[i] = new UniformBuffer(logicalDevice.getDevice());
            uniformBuffers[i].create(physicalDevice, MVP_MATRIX_SIZE);
        }

        descriptorPool = new DescriptorPool();
        descriptorPool.create(logicalDevice.getDevice(), descriptorSetLayout.getLayout(), uniformBuffers, MVP_MATRIX_SIZE, textureArray.getImageView(), textureArray.getSampler());

        overlayShaders = new OverlayShaders(logicalDevice.getDevice());
        overlayShaders.compile();

        overlayPipeline = new OverlayPipeline();
        overlayPipeline.create(
                logicalDevice.getDevice(),
                renderPass.getRenderPass(),
                swapChain.getExtent(),
                overlayShaders.getVertexModule(),
                overlayShaders.getFragmentModule()
        );

        crosshairRenderer = new CrosshairRenderer(logicalDevice.getDevice(), physicalDevice);

        highlightShaders = new HighlightShaders(logicalDevice.getDevice());
        highlightShaders.compile();

        highlightPipeline = new HighlightPipeline();
        highlightPipeline.create(
                logicalDevice.getDevice(),
                renderPass.getRenderPass(),
                swapChain.getExtent(),
                highlightShaders.getVertexModule(),
                highlightShaders.getFragmentModule()
        );

        blockHighlightRenderer = new BlockHighlightRenderer(logicalDevice.getDevice(), physicalDevice);
    }

    public void createInstance() {
        try(MemoryStack stack = stackPush()) {
            if(ENABLE_VALIDATION && !validationLayersSupported(stack)) {
                throw new RuntimeException("Validation layers requested but not available. Install the LunarG Vulkan SDK.");
            }

            VkApplicationInfo appInfo = VkApplicationInfo.calloc(stack);
            appInfo.sType(VK_STRUCTURE_TYPE_APPLICATION_INFO);
            appInfo.pApplicationName(stack.UTF8("Herecraft"));
            appInfo.applicationVersion(VK_MAKE_VERSION(1, 0, 0));
            appInfo.pEngineName(stack.UTF8("Herecraft Engine"));
            appInfo.engineVersion(VK_MAKE_VERSION(1, 0, 0));
            appInfo.apiVersion(VK_API_VERSION_1_2);

            VkInstanceCreateInfo createInfo = VkInstanceCreateInfo.calloc(stack);
            createInfo.sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO);
            createInfo.pApplicationInfo(appInfo);
            createInfo.ppEnabledExtensionNames(getRequiredExtensions(stack));

            if(ENABLE_VALIDATION) {
                createInfo.ppEnabledLayerNames(asPointerBuffer(stack, VALIDATION_LAYERS));

                VkDebugUtilsMessengerCreateInfoEXT debugCreateInfo = VkDebugUtilsMessengerCreateInfoEXT.calloc(stack);
                populateDebugMessengerCreateInfo(debugCreateInfo);
                createInfo.pNext(debugCreateInfo.address());
            }

            PointerBuffer instancePointer = stack.mallocPointer(1);
            if(vkCreateInstance(createInfo, null, instancePointer) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create Vulkan instance");
            }

            instance = new VkInstance(instancePointer.get(0), createInfo);
        }
    }

    private PointerBuffer getRequiredExtensions(MemoryStack stack) {
        PointerBuffer glfwExtensions = GLFWVulkan.glfwGetRequiredInstanceExtensions();
        if(glfwExtensions == null) {
            throw new RuntimeException("GLFW failed to find the required Vulkan surface extensions");
        }

        if(!ENABLE_VALIDATION) {
            return glfwExtensions;
        }

        PointerBuffer extensions = stack.mallocPointer(glfwExtensions.capacity() + 1);
        extensions.put(glfwExtensions);
        extensions.put(stack.UTF8(VK_EXT_DEBUG_UTILS_EXTENSION_NAME));
        extensions.flip();
        return extensions;
    }

    private boolean validationLayersSupported(MemoryStack stack) {
        IntBuffer layerCount = stack.ints(0);
        vkEnumerateInstanceLayerProperties(layerCount, null);

        VkLayerProperties.Buffer availableLayers = VkLayerProperties.calloc(layerCount.get(0), stack);
        vkEnumerateInstanceLayerProperties(layerCount, availableLayers);

        List<String> availableNames = new ArrayList<>();
        for(VkLayerProperties layer : availableLayers) {
            availableNames.add(layer.layerNameString());
        }

        for(String required : VALIDATION_LAYERS) {
            if(!availableNames.contains(required)) {
                return false;
            }
        }
        return true;
    }

    private PointerBuffer asPointerBuffer(MemoryStack stack, String[] values) {
        PointerBuffer buffer = stack.mallocPointer(values.length);
        for(String value : values) {
            buffer.put(stack.UTF8(value));
        }
        return buffer.rewind();
    }

    private void setupDebugMessenger() {
        if(!ENABLE_VALIDATION) {
            return;
        }

        try(MemoryStack stack = stackPush()) {
            VkDebugUtilsMessengerCreateInfoEXT createInfo = VkDebugUtilsMessengerCreateInfoEXT.calloc(stack);
            populateDebugMessengerCreateInfo(createInfo);

            LongBuffer messengerPointer = stack.longs(VK_NULL_HANDLE);
            if(vkCreateDebugUtilsMessengerEXT(instance, createInfo, null, messengerPointer) != VK_SUCCESS) {
                throw new RuntimeException("Failed to set up Vulkan debug messenger");
            }
            debugMessenger = messengerPointer.get(0);
        }
    }

    private void populateDebugMessengerCreateInfo(VkDebugUtilsMessengerCreateInfoEXT createInfo) {
        createInfo.sType(VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT);
        createInfo.messageSeverity(
                VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT |
                        VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT
        );
        createInfo.messageType(
                VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT |
                        VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT |
                        VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT
        );
        createInfo.pfnUserCallback(this::debugCallback);
    }

    private int debugCallback(int messageSeverity, int messageType, long pCallbackData, long pUserData) {
        VkDebugUtilsMessengerCallbackDataEXT callbackData = VkDebugUtilsMessengerCallbackDataEXT.create(pCallbackData);
        System.err.println("[Vulkan Validation] " + callbackData.pMessageString());
        return VK_FALSE;
    }

    private void createSurface(long window) {
        try(MemoryStack stack = stackPush()) {
            LongBuffer surfacePointer = stack.longs(VK_NULL_HANDLE);
            if(GLFWVulkan.glfwCreateWindowSurface(instance, window, null, surfacePointer) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create Vulkan window surface");
            }
            surface = surfacePointer.get(0);
        }
    }

    public void drawFrame(float[] mvpMatrix, float[] highlightMvp) {
        VkDevice device = logicalDevice.getDevice();

        try(MemoryStack stack = stackPush()) {
            long fence = syncObjects.getInFlightFence(currentFrame);
            vkWaitForFences(device, fence, true, Long.MAX_VALUE);

            IntBuffer imageIndexBuffer = stack.mallocInt(1);
            int acquireResult = KHRSwapchain.vkAcquireNextImageKHR(
                    device, swapChain.getSwapChain(), Long.MAX_VALUE,
                    syncObjects.getImageAvailableSemaphore(currentFrame), VK_NULL_HANDLE, imageIndexBuffer);

            if(acquireResult != VK_SUCCESS && acquireResult != KHRSwapchain.VK_SUBOPTIMAL_KHR) {
                throw new RuntimeException("Failed to acquire swap chain image");
            }

            int imageIndex = imageIndexBuffer.get(0);

            vkResetFences(device, fence);
            uniformBuffers[currentFrame].update(mvpMatrix);

            VkCommandBuffer commandBuffer = commandPool.getCommandBuffer(imageIndex);
            vkResetCommandBuffer(commandBuffer, 0);
            recordCommandBuffer(commandBuffer, imageIndex, currentFrame, highlightMvp, stack);

            VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack);
            submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);
            submitInfo.waitSemaphoreCount(1);
            submitInfo.pWaitSemaphores(stack.longs(syncObjects.getImageAvailableSemaphore(currentFrame)));
            submitInfo.pWaitDstStageMask(stack.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT));
            submitInfo.pCommandBuffers(stack.pointers(commandBuffer));
            submitInfo.pSignalSemaphores(stack.longs(syncObjects.getRenderFinishedSemaphore(currentFrame)));

            if(vkQueueSubmit(logicalDevice.getGraphicsQueue(), submitInfo, fence) != VK_SUCCESS) {
                throw new RuntimeException("Failed to submit draw command buffer");
            }

            VkPresentInfoKHR presentInfo = VkPresentInfoKHR.calloc(stack);
            presentInfo.sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR);
            presentInfo.pWaitSemaphores(stack.longs(syncObjects.getRenderFinishedSemaphore(currentFrame)));
            presentInfo.swapchainCount(1);
            presentInfo.pSwapchains(stack.longs(swapChain.getSwapChain()));
            presentInfo.pImageIndices(stack.ints(imageIndex));

            KHRSwapchain.vkQueuePresentKHR(logicalDevice.getPresentQueue(), presentInfo);

            currentFrame = (currentFrame + 1) % SyncObjects.MAX_FRAMES_IN_FLIGHT;
        }
    }

    private void recordCommandBuffer(VkCommandBuffer commandBuffer, int imageIndex, int frameIndex, float[] highlightMvp, MemoryStack stack) {
        VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack);
        beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);

        if(vkBeginCommandBuffer(commandBuffer, beginInfo) != VK_SUCCESS) {
            throw new RuntimeException("Failed to begin recording command buffer");
        }

        VkClearValue.Buffer clearValues = VkClearValue.calloc(2, stack);
        clearValues.get(0).color().float32(stack.floats(0.55f, 0.75f, 0.95f, 1.0f));
        clearValues.get(1).depthStencil().set(1.0f, 0);

        VkRenderPassBeginInfo renderPassInfo = VkRenderPassBeginInfo.calloc(stack);
        renderPassInfo.sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO);
        renderPassInfo.renderPass(renderPass.getRenderPass());
        renderPassInfo.framebuffer(swapChain.getFramebuffers().get(imageIndex));
        renderPassInfo.renderArea().offset().set(0, 0);
        renderPassInfo.renderArea().extent(swapChain.getExtent());
        renderPassInfo.pClearValues(clearValues);

        vkCmdBeginRenderPass(commandBuffer, renderPassInfo, VK_SUBPASS_CONTENTS_INLINE);
        vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, graphicsPipeline.getPipeline());
        vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS,
                graphicsPipeline.getPipelineLayout(), 0, stack.longs(descriptorPool.getDescriptorSet(frameIndex)), null);

        for(ChunkRenderer chunkRenderer : world.getChunkRenderers()) {
            if(chunkRenderer.getVertexCount() == 0) {
                continue;
            }

            LongBuffer vertexBuffers = stack.longs(chunkRenderer.getVertexBuffer().getBuffer());
            LongBuffer offsets = stack.longs(0);
            vkCmdBindVertexBuffers(commandBuffer, 0, vertexBuffers, offsets);

            java.nio.FloatBuffer pushConstant = stack.floats(
                    chunkRenderer.getChunkX() * Chunk.SIZE,
                    0.0f,
                    chunkRenderer.getChunkZ() * Chunk.SIZE
            );
            vkCmdPushConstants(commandBuffer, graphicsPipeline.getPipelineLayout(),
                    VK_SHADER_STAGE_VERTEX_BIT, 0, pushConstant);

            vkCmdDraw(commandBuffer, chunkRenderer.getVertexCount(), 1, 0, 0);
        }

        vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, overlayPipeline.getPipeline());

        LongBuffer crosshairBuffers = stack.longs(crosshairRenderer.getVertexBuffer().getBuffer());
        LongBuffer crosshairOffsets = stack.longs(0);
        vkCmdBindVertexBuffers(commandBuffer, 0, crosshairBuffers, crosshairOffsets);

        vkCmdDraw(commandBuffer, crosshairRenderer.getVertexCount(), 1, 0, 0);

        if(highlightMvp != null) {
            vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, highlightPipeline.getPipeline());

            FloatBuffer highlightPushConstant = stack.floats(highlightMvp);
            vkCmdPushConstants(commandBuffer, highlightPipeline.getPipelineLayout(),
                    VK_SHADER_STAGE_VERTEX_BIT, 0, highlightPushConstant);

            LongBuffer highlightBuffers = stack.longs(blockHighlightRenderer.getVertexBuffer().getBuffer());
            LongBuffer highlightOffsets = stack.longs(0);
            vkCmdBindVertexBuffers(commandBuffer, 0, highlightBuffers, highlightOffsets);

            vkCmdDraw(commandBuffer, blockHighlightRenderer.getVertexCount(), 1, 0, 0);
        }

        vkCmdEndRenderPass(commandBuffer);

        if(vkEndCommandBuffer(commandBuffer) != VK_SUCCESS) {
            throw new RuntimeException("Failed to record command buffer");
        }
    }

    public void setWorld(World world) {
        this.world = world;
    }

    public VkInstance getInstance() {
        return instance;
    }

    public long getSurface() {
        return surface;
    }

    public VkPhysicalDevice getPhysicalDevice() {
        return physicalDevice;
    }

    public QueueFamilyIndices getQueueFamilyIndices() {
        return queueFamilyIndices;
    }

    public VkDevice getDevice() {
        return logicalDevice.getDevice();
    }

    public SwapChain getSwapChain() {
        return swapChain;
    }

    public RenderPass getRenderPass() {
        return renderPass;
    }

    public ChunkShaders getTriangleShaders() {
        return triangleShaders;
    }

    public ChunkPipeline getGraphicsPipeline() {
        return graphicsPipeline;
    }

    public void destroy() {
        vkDeviceWaitIdle(logicalDevice.getDevice());

        depthResources.destroy();
        textureArray.destroy();
        crosshairRenderer.destroy();
        blockHighlightRenderer.destroy();
        highlightPipeline.destroy();
        highlightShaders.destroy();
        overlayPipeline.destroy();
        overlayShaders.destroy();
        vertexBuffer.destroy();
        descriptorPool.destroy();
        for(UniformBuffer uniformBuffer : uniformBuffers) {
            uniformBuffer.destroy();
        }
        descriptorSetLayout.destroy();
        syncObjects.destroy();
        commandPool.destroy();
        graphicsPipeline.destroy();
        triangleShaders.destroy();
        renderPass.destroy();
        swapChain.destroy();
        logicalDevice.destroy();
        if(ENABLE_VALIDATION) {
            vkDestroyDebugUtilsMessengerEXT(instance, debugMessenger, null);
        }
        KHRSurface.vkDestroySurfaceKHR(instance, surface, null);
        vkDestroyInstance(instance, null);
    }
}
