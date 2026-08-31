package net.herecraft.client.render.shader;

import org.lwjgl.vulkan.VkDevice;

import static org.lwjgl.util.shaderc.Shaderc.shaderc_glsl_fragment_shader;
import static org.lwjgl.util.shaderc.Shaderc.shaderc_glsl_vertex_shader;

public class HighlightShaders {
    private final VkDevice device;
    private ShaderModule vertexModule;
    private ShaderModule fragmentModule;

    public HighlightShaders(VkDevice device) {
        this.device = device;
    }

    public void compile() {
        vertexModule = new ShaderModule(device);
        vertexModule.compileAndCreate(VERTEX_SOURCE, "highlight.vert", shaderc_glsl_vertex_shader);

        fragmentModule = new ShaderModule(device);
        fragmentModule.compileAndCreate(FRAGMENT_SOURCE, "highlight.frag", shaderc_glsl_fragment_shader);
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

            layout(push_constant) uniform PushConstants {
                mat4 mvp;
            } push;

            layout(location = 0) in vec3 inPosition;

            void main() {
                gl_Position = push.mvp * vec4(inPosition, 1.0);
            }
            """;

    private static final String FRAGMENT_SOURCE = """
            #version 450

            layout(location = 0) out vec4 outColor;

            void main() {
                outColor = vec4(0.0, 0.0, 0.0, 1.0);
            }
            """;
}