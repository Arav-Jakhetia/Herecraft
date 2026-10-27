package net.herecraft.client.world;

import net.herecraft.world.level.block.Block;
import org.joml.Vector3f;

public class BlockParticle {
    private final Block block;
    private final Vector3f position;
    private final Vector3f velocity;
    private final float size;
    private float age;
    private final int textureLayer;
    private final float textureU;
    private final float textureV;

    public BlockParticle(Block block, Vector3f position, Vector3f velocity, float size, int  textureLayer, float textureU, float textureV) {
        this.block = block;
        this.position = position;
        this.velocity = velocity;
        this.size = size;
        this.textureLayer = textureLayer;
        this.textureU = textureU;
        this.textureV = textureV;
    }

    public void update(float deltaTime, World world) {
        age += deltaTime;

        velocity.y -= 18.0f * deltaTime;
        position.fma(deltaTime, velocity);

        int worldX = (int)Math.floor(position.x);
        int worldZ = (int)Math.floor(position.z);
        float grountY = 0.0f;
        int checkY = (int)Math.floor(position.y - size * 0.5f);

        for(int y = checkY; y >= 0; y--) {
            if(world.isSolidBlock(worldX, y, worldZ)) {
                grountY = y + 1.0f + size * 0.5f;
                break;
            }
        }

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

    public int getTextureLayer() {
        return textureLayer;
    }

    public float getTextureU() {
        return textureU;
    }

    public float getTextureV() {
        return textureV;
    }

    public boolean isExpired() {
        return age >= 1.6f;
    }
}
