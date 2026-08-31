package net.herecraft.client.render.swapchain;

import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;
import org.lwjgl.vulkan.VkSurfaceFormatKHR;

import java.nio.IntBuffer;

public class SwapChainSupportDetails {
    public VkSurfaceCapabilitiesKHR capabilities;
    public VkSurfaceFormatKHR.Buffer formats;
    public IntBuffer presentModes;
}
