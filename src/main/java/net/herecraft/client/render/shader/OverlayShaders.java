package net.herecraft.client.render.shader;

import org.lwjgl.vulkan.VkDevice;

import static org.lwjgl.util.shaderc.Shaderc.shaderc_glsl_fragment_shader;
import static org.lwjgl.util.shaderc.Shaderc.shaderc_glsl_vertex_shader;

public class OverlayShaders {
    private final VkDevice device;
    private ShaderModule vertexModule;
    private ShaderModule fragmentModule;

    public OverlayShaders(VkDevice device) {
        this.device = device;
    }

    public void compile() {
        vertexModule = new ShaderModule(device);
        vertexModule.compileAndCreate(VERTEX_SOURCE, "overlay.vert", shaderc_glsl_vertex_shader);

        fragmentModule = new ShaderModule(device);
        fragmentModule.compileAndCreate(FRAGMENT_SOURCE, "overlay.frag", shaderc_glsl_fragment_shader);
    }

    public long getVertexModule() {
        return vertexModule.getModule();
    }

    public long getFragmentModule() {
        return fragmentModule.getModule();
    }

    public void destroy() {
        vertexModule.destroy();
        fragmentModule.destroy();
    }

    private static final String VERTEX_SOURCE = """
            #version 450

            layout(location = 0) in vec2 inPosition;
            layout(location = 1) in vec3 inColor;

            layout(location = 0) out vec3 fragColor;

            void main() {
                gl_Position = vec4(inPosition, 0.0, 1.0);
                fragColor = inColor;
            }
            """;

    private static final String FRAGMENT_SOURCE = """
            #version 450

            layout(location = 0) in vec3 fragColor;
            layout(location = 0) out vec4 outColor;

            void main() {
                outColor = vec4(fragColor, 1.0);
            }
            """;
}