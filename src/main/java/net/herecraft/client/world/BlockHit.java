package net.herecraft.client.world;

public class BlockHit {
    public final int x;
    public final int y;
    public final int z;

    public final int faceX;
    public final int faceY;
    public final int faceZ;

    public BlockHit(int x, int y, int z, int faceX, int faceY, int faceZ) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.faceX = faceX;
        this.faceY = faceY;
        this.faceZ = faceZ;
    }
}