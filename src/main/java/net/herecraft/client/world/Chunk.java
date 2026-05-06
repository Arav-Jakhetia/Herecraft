package net.herecraft.client.world;

import net.herecraft.client.block.Block;

import java.util.ArrayList;
import java.util.List;

public class Chunk {
    public static final int SIZE = 16;

    private final Block[][][] blocks = new Block[SIZE][SIZE][SIZE];

    public Chunk() {
        generateBasicTerrain();
    }

    public float[] buildMesh() {
        List<Float> vertices = new ArrayList<>();

        for(int x = 0; x < SIZE; x++) {
            for(int y = 0; y < SIZE; y++) {
                for(int z = 0; z < SIZE; z++) {
                    Block block = blocks[x][y][z];
                    if(!block.isSolid()) {
                        continue;
                    }

                    if(isAir(x, y, z - 1)) addFace(vertices, block, x, y, z, Face.BACK);
                    if(isAir(x, y, z + 1)) addFace(vertices, block, x, y, z, Face.FRONT);
                    if(isAir(x - 1, y, z)) addFace(vertices, block, x, y, z, Face.LEFT);
                    if(isAir(x + 1, y, z)) addFace(vertices, block, x, y, z, Face.RIGHT);
                    if(isAir(x, y - 1, z)) addFace(vertices, block, x, y, z, Face.BOTTOM);
                    if(isAir(x, y + 1, z)) addFace(vertices, block, x, y, z, Face.TOP);
                }
            }
        }

        float[] mesh = new float[vertices.size()];
        for(int i = 0; i < vertices.size(); i++) {
            mesh[i] = vertices.get(i);
        }
        return mesh;
    }

    private void generateBasicTerrain() {
        for(int x = 0; x < SIZE; x++) {
            for(int y = 0; y < SIZE; y++) {
                for(int z = 0; z < SIZE; z++) {
                    blocks[x][y][z] = Block.air();
                }
            }
        }

        for(int x = 0; x < SIZE; x++) {
            for(int z = 0; z < SIZE; z++) {
                int height = 4 + ((x + z) % 3);

                for(int y = 0; y <= height; y++) {
                    if(y == height) {
                        blocks[x][y][z] = Block.grass();
                    } else if(y > height - 3) {
                        blocks[x][y][z] = Block.dirt();
                    } else {
                        blocks[x][y][z] = Block.stone();
                    }
                }
            }
        }
    }

    private boolean isAir(int x, int y, int z) {
        if(x < 0 || x >= SIZE || y < 0 || y >= SIZE || z < 0 || z >= SIZE) {
            return true;
        }
        return !blocks[x][y][z].isSolid();
    }

    private void addFace(List<Float> vertices, Block block, int x, int y, int z, Face face) {
        float[][] corners = face.corners;
        int[] order = {0, 1, 2, 2, 3, 0};
        float shade = face.shade;

        for(int index : order) {
            float[] corner = corners[index];
            vertices.add(x + corner[0]);
            vertices.add(y + corner[1]);
            vertices.add(z + corner[2]);
            vertices.add(block.red() * shade);
            vertices.add(block.green() * shade);
            vertices.add(block.blue() * shade);
        }
    }

    private enum Face {
        BACK(new float[][] {
                {0.0f, 0.0f, 0.0f},
                {1.0f, 1.0f, 0.0f},
                {1.0f, 0.0f, 0.0f},
                {0.0f, 1.0f, 0.0f}
        }, 0.70f),
        FRONT(new float[][] {
                {0.0f, 0.0f, 1.0f},
                {1.0f, 0.0f, 1.0f},
                {1.0f, 1.0f, 1.0f},
                {0.0f, 1.0f, 1.0f}
        }, 0.85f),
        LEFT(new float[][] {
                {0.0f, 0.0f, 0.0f},
                {0.0f, 0.0f, 1.0f},
                {0.0f, 1.0f, 1.0f},
                {0.0f, 1.0f, 0.0f}
        }, 0.75f),
        RIGHT(new float[][] {
                {1.0f, 0.0f, 0.0f},
                {1.0f, 1.0f, 0.0f},
                {1.0f, 1.0f, 1.0f},
                {1.0f, 0.0f, 1.0f}
        }, 0.95f),
        BOTTOM(new float[][] {
                {0.0f, 0.0f, 0.0f},
                {1.0f, 0.0f, 0.0f},
                {1.0f, 0.0f, 1.0f},
                {0.0f, 0.0f, 1.0f}
        }, 0.55f),
        TOP(new float[][] {
                {0.0f, 1.0f, 0.0f},
                {0.0f, 1.0f, 1.0f},
                {1.0f, 1.0f, 1.0f},
                {1.0f, 1.0f, 0.0f}
        }, 1.0f);

        private final float[][] corners;
        private final float shade;

        Face(float[][] corners, float shade) {
            this.corners = corners;
            this.shade = shade;
        }
    }
}
