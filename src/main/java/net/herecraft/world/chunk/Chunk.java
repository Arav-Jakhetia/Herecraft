package net.herecraft.world.chunk;

import net.herecraft.world.BlockType;
import net.herecraft.world.World;

public class Chunk {
    public static final int SIZE = 16;
    private static final int BASE_SURFACE_Y = World.HEIGHT - 7;
    private static final int CAVE_BOTTOM_Y = 1;

    private final int chunkX;
    private final int chunkZ;
    private final BlockType[][][] blocks;

    public Chunk(int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.blocks = new BlockType[SIZE][World.HEIGHT][SIZE];
    }

    public void generate() {
        int startX = chunkX * SIZE;
        int startZ = chunkZ * SIZE;

        for (int lx = 0; lx < SIZE; lx++) {
            for (int lz = 0; lz < SIZE; lz++) {
                int wx = startX + lx;
                int wz = startZ + lz;

                int surfaceY = getSurfaceY(wx, wz);

                for (int y = 0; y < World.HEIGHT; y++) {
                    if (wx < World.MIN_X || wx > World.MAX_X || wz < World.MIN_Z || wz > World.MAX_Z) {
                        blocks[lx][y][lz] = BlockType.AIR;
                    } else if (isCave(wx, y, wz, surfaceY)) {
                        blocks[lx][y][lz] = BlockType.AIR;
                    } else if (y <= surfaceY) {
                        boolean inTopSevenLayers = y >= World.HEIGHT - 7;
                        boolean fullyLit = isAirAt(wx, y + 1, wz, surfaceY);
                        blocks[lx][y][lz] = (inTopSevenLayers && fullyLit)
                                ? BlockType.GRASS_BLOCK
                                : BlockType.COBBLESTONE;
                    } else {
                        blocks[lx][y][lz] = BlockType.AIR;
                    }
                }
            }
        }
    }

    private int getSurfaceY(int wx, int wz) {
        return BASE_SURFACE_Y;
    }

    private boolean isCave(int wx, int y, int wz, int surfaceY) {
        if (y < CAVE_BOTTOM_Y || y > surfaceY) {
            return false;
        }

        double n1 = Math.sin(wx * 0.12) + Math.cos(wz * 0.12);
        double n2 = Math.sin(y * 0.25) + Math.cos((wx + wz + y) * 0.05);
        double n3 = Math.sin((wx * 0.07) + (y * 0.17)) + Math.cos((wz * 0.09) - (y * 0.13));
        double density = n1 + n2 + n3;

        double depth = (surfaceY - y) / (double) Math.max(surfaceY - CAVE_BOTTOM_Y, 1);
        double threshold = 1.55 - depth * 0.30;
        return density > threshold;
    }

    private boolean isAirAt(int wx, int y, int wz, int surfaceY) {
        if (y >= World.HEIGHT) {
            return true;
        }
        if (y > surfaceY) {
            return true;
        }
        return isCave(wx, y, wz, surfaceY);
    }

    public BlockType getLocal(int lx, int y, int lz) {
        return blocks[lx][y][lz];
    }

    public int getChunkX() {
        return chunkX;
    }

    public int getChunkZ() {
        return chunkZ;
    }
}
