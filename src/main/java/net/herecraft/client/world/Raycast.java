package net.herecraft.client.world;

import org.joml.Vector3f;

public class Raycast {
    public static BlockHit cast(World world, Vector3f origin, Vector3f direction, float maxDistance) {
        float step = 0.05f;

        int lastX = (int)Math.floor(origin.x);
        int lastY = (int)Math.floor(origin.y);
        int lastZ = (int)Math.floor(origin.z);

        for(float distance = 0.0f; distance <= maxDistance; distance += step) {
            float px = origin.x + direction.x * distance;
            float py = origin.y + direction.y * distance;
            float pz = origin.z + direction.z * distance;

            int blockX = (int)Math.floor(px);
            int blockY = (int)Math.floor(py);
            int blockZ = (int)Math.floor(pz);

            if(world.isSolidBlock(blockX, blockY, blockZ)) {
                return new BlockHit(blockX, blockY, blockZ, lastX, lastY, lastZ);
            }

            lastX = blockX;
            lastY = blockY;
            lastZ = blockZ;
        }

        return null;
    }
}