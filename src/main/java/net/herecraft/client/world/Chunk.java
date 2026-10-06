package net.herecraft.client.world;

import net.herecraft.client.block.Block;
import org.joml.Vector3f;

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
        generateTerrain();
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

    private void generateTerrain() {
        for(int x = 0; x < SIZE; x++) {
            for(int y = 0; y < SIZE; y++) {
                for(int z = 0; z < SIZE; z++) {
                    blocks[x][y][z] = Block.air();
                }
            }
        }

        for(int x = 0; x < SIZE; x++) {
            for(int z = 0; z < SIZE; z++) {
                int worldX = chunkX * SIZE + x;
                int worldZ = chunkZ * SIZE + z;

                int surfaceY = 7 + (int)Math.round(valueNoise(worldX, worldZ, 8) * 5.0);
                surfaceY = Math.max(2, Math.min(SIZE - 1, surfaceY));

                for(int y = 0; y <= surfaceY; y++) {
                    if(y == surfaceY) {
                        blocks[x][y][z] = Block.grass();
                    } else if(y >= surfaceY - 2) {
                        blocks[x][y][z] = Block.dirt();
                    } else {
                        blocks[x][y][z] = Block.stone();
                    }
                }
            }
        }
    }

    private double valueNoise(int worldX, int worldZ, int scale) {
        int cellX = Math.floorDiv(worldX, scale);
        int cellZ = Math.floorDiv(worldZ, scale);

        double localX = (double)Math.floorMod(worldX, scale) / scale;
        double localZ = (double)Math.floorMod(worldZ, scale) / scale;

        localX = localX * localX * (3.0 - 2.0 * localX);
        localZ = localZ * localZ * (3.0 - 2.0 * localZ);

        double top = mix(heightPoint(cellX, cellZ), heightPoint(cellX + 1, cellZ), localX);
        double bottom = mix(heightPoint(cellX, cellZ + 1), heightPoint(cellX + 1, cellZ + 1), localX);

        return mix(top, bottom, localZ);
    }

    private double heightPoint(int x, int z) {
        long value = x * 341873128712L + z * 132897987541L;
        value = (value ^ (value >> 15)) * 1274126177L;
        value ^= value >> 16;

        return ((value & 0x7fffffffL) / (double)0x7fffffffL) * 2.0 - 1.0;
    }

    private double mix(double a, double b, double amount) {
        return a + (b - a) * amount;
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

        Vector3f tint = block.isGrass()
                ? world.getGrassTint(chunkX * SIZE + x, chunkZ * SIZE + z)
                : new  Vector3f(1.0f, 1.0f, 1.0f);

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

            vertices.add(tint.x);
            vertices.add(tint.y);
            vertices.add(tint.z);
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
