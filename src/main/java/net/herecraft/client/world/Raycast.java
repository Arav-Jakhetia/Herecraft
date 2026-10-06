package net.herecraft.client.world;

import org.joml.Vector3f;

public class Raycast {
    public static BlockHit cast(World world, Vector3f origin, Vector3f direction, float maxDistance) {
        Vector3f ray = new  Vector3f(direction).normalize();

        int x = (int) Math.floor(origin.x);
        int y = (int) Math.floor(origin.y);
        int z = (int) Math.floor(origin.z);

        int stepX = ray.x >= 0.0f ? 1 : -1;
        int stepY = ray.y >= 0 ? 1 : -1;
        int stepZ = ray.z >= 0 ? 1 : -1;

        float deltaX = ray.x == 0.0f ? Float.POSITIVE_INFINITY : Math.abs(1.0f / ray.x);
        float deltaY = ray.y == 0.0f ? Float.POSITIVE_INFINITY : Math.abs(1.0f / ray.y);
        float deltaZ = ray.z == 0.0f ? Float.POSITIVE_INFINITY : Math.abs(1.0f / ray.z);

        float maxX = ray.x >= 0.0f ? (x + 1.0f - origin.x) * deltaX : (origin.x - x) *  deltaX;
        float maxY = ray.y >= 0.0f ? (y + 1.0f - origin.y) * deltaY : (origin.y - y) *  deltaY;
        float maxZ = ray.z >= 0.0f ? (z + 1.0f - origin.z) * deltaZ : (origin.z - z) *  deltaZ;

        int placeX = x;
        int placeY = y;
        int placeZ = z;

        float distance = 0.0f;

        while(distance <= maxDistance) {
            if(world.isSolidBlock(x, y, z)) {
                return new BlockHit(x, y, z, placeX, placeY, placeZ);
            }

            placeX = x;
            placeY = y;
            placeZ = z;

            if(maxX <= maxY && maxX <= maxZ) {
                x += stepX;
                distance = maxX;
                maxX += deltaX;
            } else if(maxY <= maxZ) {
                y += stepY;
                distance = maxY;
                maxY += deltaY;
            } else {
                z += stepZ;
                distance = maxZ;
                maxZ += deltaZ;
            }
        }

        return null;
    }
}