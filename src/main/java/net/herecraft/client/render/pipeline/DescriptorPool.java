package net.herecraft.client.render.pipeline;

import net.herecraft.client.render.buffer.UniformBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class DescriptorPool {
    private VkDevice device;
    private long pool;
    private long[] descriptorSets;

    public void create(VkDevice device, long descriptorSetLayout, UniformBuffer[] uniformBuffers, long uniformBufferSize,
                       long textureImageView, long textureSampler) {
        this.device = device;
        int count = uniformBuffers.length;

        try(MemoryStack stack = stackPush()) {
            VkDescriptorPoolSize.Buffer poolSizes = VkDescriptorPoolSize.calloc(2, stack);
            poolSizes.get(0).type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER).descriptorCount(count);
            poolSizes.get(1).type(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER).descriptorCount(count);

            VkDescriptorPoolCreateInfo poolInfo = VkDescriptorPoolCreateInfo.calloc(stack);
            poolInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO);
            poolInfo.pPoolSizes(poolSizes);
            poolInfo.maxSets(count);

            LongBuffer poolPointer = stack.mallocLong(1);
            if(vkCreateDescriptorPool(device, poolInfo, null, poolPointer) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create descriptor pool");
            }
            pool = poolPointer.get(0);

            LongBuffer layouts = stack.mallocLong(count);
            for(int i = 0; i < count; i++) {
                layouts.put(i, descriptorSetLayout);
            }

            VkDescriptorSetAllocateInfo allocInfo = VkDescriptorSetAllocateInfo.calloc(stack);
            allocInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO);
            allocInfo.descriptorPool(pool);
            allocInfo.pSetLayouts(layouts);

            LongBuffer setsPointer = stack.mallocLong(count);
            if(vkAllocateDescriptorSets(device, allocInfo, setsPointer) != VK_SUCCESS) {
                throw new RuntimeException("Failed to allocate descriptor sets");
            }

            descriptorSets = new long[count];
            for(int i = 0; i < count; i++) {
                descriptorSets[i] = setsPointer.get(i);

                VkDescriptorBufferInfo.Buffer bufferInfo = VkDescriptorBufferInfo.calloc(1, stack);
                bufferInfo.buffer(uniformBuffers[i].getBuffer());
                bufferInfo.offset(0);
                bufferInfo.range(uniformBufferSize);

                VkDescriptorImageInfo.Buffer imageInfo = VkDescriptorImageInfo.calloc(1, stack);
                imageInfo.imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
                imageInfo.imageView(textureImageView);
                imageInfo.sampler(textureSampler);

                VkWriteDescriptorSet.Buffer descriptorWrites = VkWriteDescriptorSet.calloc(2, stack);

                descriptorWrites.get(0)
                        .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                        .dstSet(descriptorSets[i])
                        .dstBinding(0)
                        .dstArrayElement(0)
                        .descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                        .descriptorCount(1)
                        .pBufferInfo(bufferInfo);

                descriptorWrites.get(1)
                        .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                        .dstSet(descriptorSets[i])
                        .dstBinding(1)
                        .dstArrayElement(0)
                        .descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                        .descriptorCount(1)
                        .pImageInfo(imageInfo);

                vkUpdateDescriptorSets(device, descriptorWrites, null);
            }
        }
    }

    public long getDescriptorSet(int index) {
        return descriptorSets[index];
    }

    public void destroy() {
        vkDestroyDescriptorPool(device, pool, null);
    }
}