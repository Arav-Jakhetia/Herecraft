package net.herecraft.client.world;

import net.herecraft.client.block.Block;
import org.joml.Vector3f;

public class BlockParticle {
    private final Block block;
    private final Vector3f position;
    private final Vector3f velocity;
    private final float size;
    private float age;

    public BlockParticle(Block block, Vector3f position, Vector3f velocity, float size) {
        this.block = block;
        this.position = position;
        this.velocity = velocity;
        this.size = size;
    }

    public void update(float deltaTime, World world) {
        age += deltaTime;

        velocity.y -= 18.0f * deltaTime;
        position.fma(deltaTime, velocity);

        int worldX = (int)Math.floor(position.x);
        int worldZ = (int)Math.floor(position.z);
        float grountY = world.getGroundHeight(worldX, worldZ) + size * 0.5f;

        if(position.y < grountY) {
            position.y = grountY;
            velocity.y *= -0.25f;
            velocity.x *= 0.65f;
            velocity.z *= 0.65f;

            if(Math.abs(velocity.y) < 1.0f) {
                velocity.y = 0.0f;
            }
        }
    }

    public Block getBlock() {
        return block;
    }

    public Vector3f getPosition() {
        return position;
    }

    public float getSize() {
        return size;
    }

    public boolean isExpired() {
        return age >= 1.6f;
    }
}
