package net.herecraft.client.render.shader;

import org.lwjgl.vulkan.VkDevice;

import static org.lwjgl.util.shaderc.Shaderc.shaderc_glsl_fragment_shader;
import static org.lwjgl.util.shaderc.Shaderc.shaderc_glsl_vertex_shader;

public class ChunkShaders {
    private final VkDevice device;
    private ShaderModule vertexModule;
    private ShaderModule fragmentModule;

    public ChunkShaders(VkDevice device) {
        this.device = device;
    }

    public void compile() {
        vertexModule = new ShaderModule(device);
        vertexModule.compileAndCreate(VERTEX_SOURCE, "triangle.vert", shaderc_glsl_vertex_shader);

        fragmentModule = new ShaderModule(device);
        fragmentModule.compileAndCreate(FRAGMENT_SOURCE, "triangle.frag", shaderc_glsl_fragment_shader);

        System.out.println("Triangle shaders compiled to SPIR-V successfully");
    }

    public long getVertexModule() {
        return vertexModule.getModule();
    }

    public long getFragmentModule() {
        return fragmentModule.getModule();
    }

    public void destroy() {
        vertexModule.destroy();;
        fragmentModule.destroy();
    }

    private static final String VERTEX_SOURCE = """
        #version 450

        layout(binding = 0) uniform UniformBufferObject {
            mat4 mvp;
        } ubo;

        layout(push_constant) uniform PushConstants {
            vec3 chunkOffset;
        } push;

        layout(location = 0) in vec3 inPosition;
        layout(location = 1) in vec2 inTexCoord;
        layout(location = 2) in float inShade;
        layout(location = 3) in float inTextureLayer;

        layout(location = 0) out vec2 fragTexCoord;
        layout(location = 1) out float fragShade;
        layout(location = 2) out float fragTextureLayer;

        void main() {
            gl_Position = ubo.mvp * vec4(inPosition + push.chunkOffset, 1.0);
            fragTexCoord = inTexCoord;
            fragShade = inShade;
            fragTextureLayer = inTextureLayer;
        }
        """;

    private static final String FRAGMENT_SOURCE = """
        #version 450

        layout(binding = 1) uniform sampler2DArray texSampler;

        layout(location = 0) in vec2 fragTexCoord;
        layout(location = 1) in float fragShade;
        layout(location = 2) in float fragTextureLayer;

        layout(location = 0) out vec4 outColor;

        void main() {
            vec4 texColor = texture(texSampler, vec3(fragTexCoord, fragTextureLayer));
            outColor = vec4(texColor.rgb * fragShade, texColor.a);
        }
        """;
}
