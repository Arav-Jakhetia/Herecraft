package net.herecraft.client.render.shader;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkShaderModuleCreateInfo;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.util.shaderc.Shaderc.*;
import static org.lwjgl.vulkan.VK10.*;

public class ShaderModule {
    private final VkDevice device;
    private long module;

    public ShaderModule(VkDevice device) {
        this.device = device;
    }

    public long compileAndCreate(String source, String fileName, int shaderKind) {
        long compiler = shaderc_compiler_initialize();
        if(compiler == 0) {
            throw new RuntimeException("Failed to initialize shaderc compiler");
        }

        long result = shaderc_compile_into_spv(compiler, source, shaderKind, fileName, "main", 0);

        if(shaderc_result_get_compilation_status(result) != shaderc_compilation_status_success) {
            String errorMessage = shaderc_result_get_error_message(result);
            shaderc_result_release(result);
            shaderc_compiler_release(compiler);
            throw new RuntimeException("Shader compilation failed (" + fileName + "):\n" + errorMessage);
        }

        ByteBuffer spirvCode = shaderc_result_get_bytes(result);

        try(MemoryStack stack = stackPush()) {
            VkShaderModuleCreateInfo createInfo = VkShaderModuleCreateInfo.calloc(stack);
            createInfo.sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO);
            createInfo.pCode(spirvCode);

            LongBuffer modulePointer = stack.mallocLong(1);
            if(vkCreateShaderModule(device, createInfo, null, modulePointer) != VK_SUCCESS) {
                shaderc_result_release(result);
                shaderc_compiler_release(compiler);
                throw new RuntimeException("Failed to create shader module: " + fileName);
            }
            module = modulePointer.get(0);
        }

        shaderc_result_release(result);
        shaderc_compiler_release(compiler);

        return module;
    }

    public long getModule() {
        return module;
    }

    public void destroy() {
        vkDestroyShaderModule(device, module, null);
    }
}
