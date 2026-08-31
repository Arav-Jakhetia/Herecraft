package net.herecraft.client.world;

import net.herecraft.client.block.Block;

import java.util.ArrayList;
import java.util.List;

public class Chunk {
    public static final int SIZE = 16;

    private final int chunkX;
    private final int chunkZ;
    private final World world;
    private final Block blocks[][][] = new Block[SIZE][SIZE][SIZE];


    public Chunk(int chunkX, int chunkZ, World world) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.world = world;
        generateBasicTerrain();
    }

    public int getChunkX() {
        return chunkX;
    }

    public int getChunkZ() {
        return chunkZ;
    }

    public Block getBlockLocal(int x, int y, int z) {
        if(x < 0 || x >= SIZE || y < 0 || y >= SIZE || z < 0 || z >= SIZE) {
            return Block.air();
        }
        return blocks[x][y][z];
    }

    public void setBlockLocal(int x, int y, int z, Block block) {
        if(x < 0 || x >= SIZE || y < 0 || y >= SIZE || z < 0 || z >= SIZE) {
            return;
        }
        blocks[x][y][z] = block;
    }

    public boolean isSolidBlockLocal(int x, int y, int z) {
        if(x < 0 || x >= SIZE || z < 0 || z >= SIZE) {
            if(world == null) {
                return false;
            }
            int worldX = chunkX * SIZE + x;
            int worldZ = chunkZ * SIZE + z;
            return world.isSolidBlock(worldX, y, worldZ);
        }
        if(y < 0 || y >= SIZE) {
            return false;
        }

        return blocks[x][y][z].isSolid();
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

        float mesh[] = new float[vertices.size()];
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
                blocks[x][0][z] = Block.stone();
                blocks[x][1][z] = Block.stone();
                blocks[x][2][z] = Block.stone();
                blocks[x][3][z] = Block.stone();
                blocks[x][4][z] = Block.stone();

                blocks[x][5][z] = Block.dirt();
                blocks[x][6][z] = Block.dirt();

                blocks[x][7][z] = Block.grass();
            }
        }
    }

    private boolean isAir(int x, int y, int z) {
        return !isSolidBlockLocal(x, y, z);
    }

    private void addFace(List<Float> vertices, Block block, int x, int y, int z, Face face) {
        float corners[][] = face.corners;
        float uvs[][] = face.uvs;
        int order[] = {0, 1, 2, 2, 3, 0};

        int textureLayer = switch (face) {
            case TOP -> block.getTopTextureLayer();
            case BOTTOM -> block.getBottomTextureLayer();
            default -> block.getSideTextureLayer();
        };

        for(int index : order) {
            float corner[] = corners[index];
            float uv[] = uvs[index];

            vertices.add(x + corner[0]);
            vertices.add(y + corner[1]);
            vertices.add(z + corner[2]);

            vertices.add(uv[0]);
            vertices.add(uv[1]);

            vertices.add(face.shade);
            vertices.add((float)textureLayer);
        }
    }

    private enum Face {
        BACK(new float[][] {
                {0.0f, 0.0f, 0.0f},
                {0.0f, 1.0f, 0.0f},
                {1.0f, 1.0f, 0.0f},
                {1.0f, 0.0f, 0.0f}
        }, new float[][] {
                {0.0f, 1.0f},
                {0.0f, 0.0f},
                {1.0f, 0.0f},
                {1.0f, 1.0f}
        }, 0.70f),

        FRONT(new float[][] {
                {0.0f, 0.0f, 1.0f},
                {1.0f, 0.0f, 1.0f},
                {1.0f, 1.0f, 1.0f},
                {0.0f, 1.0f, 1.0f}
        }, new float[][] {
                {0.0f, 1.0f},
                {1.0f, 1.0f},
                {1.0f, 0.0f},
                {0.0f, 0.0f}
        }, 0.85f),

        LEFT(new float[][] {
                {0.0f, 0.0f, 0.0f},
                {0.0f, 0.0f, 1.0f},
                {0.0f, 1.0f, 1.0f},
                {0.0f, 1.0f, 0.0f}
        }, new float[][] {
                {0.0f, 1.0f},
                {1.0f, 1.0f},
                {1.0f, 0.0f},
                {0.0f, 0.0f}
        }, 0.75f),

        RIGHT(new float[][] {
                {1.0f, 0.0f, 0.0f},
                {1.0f, 1.0f, 0.0f},
                {1.0f, 1.0f, 1.0f},
                {1.0f, 0.0f, 1.0f}
        }, new float[][] {
                {0.0f, 1.0f},
                {0.0f, 0.0f},
                {1.0f, 0.0f},
                {1.0f, 1.0f}
        }, 0.95f),

        BOTTOM(new float[][] {
                {0.0f, 0.0f, 0.0f},
                {1.0f, 0.0f, 0.0f},
                {1.0f, 0.0f, 1.0f},
                {0.0f, 0.0f, 1.0f}
        }, new float[][] {
                {0.0f, 0.0f},
                {1.0f, 0.0f},
                {1.0f, 1.0f},
                {0.0f, 1.0f}
        }, 0.55f),

        TOP(new float[][] {
                {0.0f, 1.0f, 0.0f},
                {0.0f, 1.0f, 1.0f},
                {1.0f, 1.0f, 1.0f},
                {1.0f, 1.0f, 0.0f}
        }, new float[][] {
                {0.0f, 0.0f},
                {0.0f, 1.0f},
                {1.0f, 1.0f},
                {1.0f, 0.0f}
        }, 1.0f);

        private final float shade;
        private final float[][] corners;
        private final float[][] uvs;


        Face(float[][] corners, float[][] uvs, float shade) {
            this.corners = corners;
            this.uvs = uvs;
            this.shade = shade;
        }
    }
}
