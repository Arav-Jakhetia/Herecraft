package net.herecraft.world;

import net.herecraft.world.chunk.Chunk;
import net.herecraft.world.chunk.ChunkMesh;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

import org.joml.Vector3f;

public class WorldMesh {
    private final List<ChunkMesh> chunkMeshes = new ArrayList<>();

    public WorldMesh(World world) {
        for (Chunk chunk : world.getChunks()) {
            chunkMeshes.add(new ChunkMesh(world, chunk));
        }
    }

    public void updateStreaming(
            float playerBlockX,
            float playerBlockZ,
            int loadRadiusChunks,
            int unloadRadiusChunks,
            int maxUploadsPerCall
    ) {
        int playerChunkX = toChunkCoord(playerBlockX);
        int playerChunkZ = toChunkCoord(playerBlockZ);
        int loadRadiusSq = loadRadiusChunks * loadRadiusChunks;
        int unloadRadiusSq = unloadRadiusChunks * unloadRadiusChunks;

        for (ChunkMesh mesh : chunkMeshes) {
            if (!mesh.isUploaded()) {
                continue;
            }
            if (chunkDistanceSq(mesh, playerChunkX, playerChunkZ) > unloadRadiusSq) {
                mesh.cleanup();
            }
        }

        List<ChunkMesh> toLoad = new ArrayList<>();
        for (ChunkMesh mesh : chunkMeshes) {
            if (mesh.isUploaded()) {
                continue;
            }
            if (chunkDistanceSq(mesh, playerChunkX, playerChunkZ) <= loadRadiusSq) {
                toLoad.add(mesh);
            }
        }

        toLoad.sort(Comparator.comparingInt(m -> chunkDistanceSq(m, playerChunkX, playerChunkZ)));

        int uploadedNow = 0;
        for (ChunkMesh mesh : toLoad) {
            mesh.upload();
            uploadedNow++;
            if (uploadedNow >= maxUploadsPerCall) {
                break;
            }
        }
    }

    public void upload() {
        for (ChunkMesh mesh : chunkMeshes) {
            mesh.upload();
        }
    }

    public void render(float playerBlockX, float playerBlockZ, Vector3f cameraPos, Vector3f cameraFront) {
        int playerChunkX = Math.floorDiv((int) Math.floor(playerBlockX), Chunk.SIZE);
        int playerChunkZ = Math.floorDiv((int) Math.floor(playerBlockZ), Chunk.SIZE);

        float fx = cameraFront.x;
        float fz = cameraFront.z;
        float fLen = (float) Math.sqrt(fx * fx + fz * fz);
        if (fLen < 0.0001f) {
            fx = 0.0f;
            fz = 1.0f;
        } else {
            fx /= fLen;
            fz /= fLen;
        }

        final int alwaysRenderRadiusChunks = 2;
        final int alwaysRenderRadiusSq = alwaysRenderRadiusChunks * alwaysRenderRadiusChunks;
        final float minDot = (float) Math.cos(Math.toRadians(110.0));

        List<ChunkMesh> sorted = new ArrayList<>(chunkMeshes);
        sorted.sort(Comparator.comparingInt(m -> chunkDistanceSq(m, playerChunkX, playerChunkZ)));

        for (ChunkMesh mesh : sorted) {
            if (!mesh.isUploaded()) {
                continue;
            }

            int dSq = chunkDistanceSq(mesh, playerChunkX, playerChunkZ);
            if (dSq <= alwaysRenderRadiusSq) {
                mesh.render();
                continue;
            }

            float centerBlockX = mesh.getChunkX() * Chunk.SIZE + (Chunk.SIZE * 0.5f);
            float centerBlockZ = mesh.getChunkZ() * Chunk.SIZE + (Chunk.SIZE * 0.5f);
            float centerWorldX = centerBlockX * ChunkMesh.BLOCK_SIZE;
            float centerWorldZ = centerBlockZ * ChunkMesh.BLOCK_SIZE;

            float toX = centerWorldX - cameraPos.x;
            float toZ = centerWorldZ - cameraPos.z;
            float toLen = (float) Math.sqrt(toX * toX + toZ * toZ);

            if (toLen < 1.0f) {
                mesh.render();
                continue;
            }

            toX /= toLen;
            toZ /= toLen;

            float dot = toX * fx + toZ * fz;
            if (dot < minDot) {
                continue;
            }

            mesh.render();
        }
    }

    public void render() {
        for (ChunkMesh mesh : chunkMeshes) {
            mesh.render();
        }
    }

    public int getChunkCount() {
        return chunkMeshes.size();
    }

    public int getTotalVertexCount() {
        int total = 0;
        for (ChunkMesh mesh : chunkMeshes) {
            total += mesh.getVertexCount();
        }
        return total;
    }

    public void cleanup() {
        for (ChunkMesh mesh : chunkMeshes) {
            mesh.cleanup();
        }
    }

    private int toChunkCoord(float blockCoord) {
        return Math.floorDiv((int) Math.floor(blockCoord), Chunk.SIZE);
    }

    private int chunkDistanceSq(ChunkMesh mesh, int playerChunkX, int playerChunkZ) {
        int dx = mesh.getChunkX() - playerChunkX;
        int dz = mesh.getChunkZ() - playerChunkZ;
        return dx * dx + dz * dz;
    }
}