# Herecraft

A Minecraft-inspired voxel sandbox built from scratch in Java using Vulkan through LWJGL.

Herecraft is a learning-focused rendering project that explores how a block-based world can be created without using an existing game engine. It includes Vulkan rendering, chunk-based terrain, textured blocks, first-person movement, collision, raycasting, and block interaction.

> This project is independent and is not affiliated with, endorsed by, or associated with Mojang Studios or Microsoft.

## Features

- Vulkan renderer built with LWJGL
- GLFW window and input handling
- Chunk-based voxel world (`16 × 16 × 16` blocks per chunk)
- Automatic chunk loading around the player
- Face culling between adjacent solid blocks
- GPU texture-array rendering with nearest-neighbour filtering
- Grass, dirt, stone, and air blocks
- Per-face block textures
- First-person mouse look
- WASD movement, jumping, gravity, and collision detection
- Raycasting for block selection
- Left-click block breaking
- Right-click block placing
- Block-selection outline
- Basic directional face shading

## Controls

| Key | Action |
| --- | --- |
| `W` `A` `S` `D` | Move |
| `Space` | Jump |
| Left Mouse Button | Break selected block |
| Right Mouse Button | Place a stone block |
| `Escape` | Close the game |

## Project Structure

```text
src/main/java/net/herecraft/
├── Herecraft.java              # Application setup and game loop
├── main/Main.java              # Entry point
└── client/
    ├── block/                  # Block definitions and texture layers
    ├── input/                  # Keyboard and mouse input
    ├── player/                 # Player movement and collisions
    ├── render/                 # Vulkan renderer, buffers, pipelines, shaders
    └── world/                  # World, chunks, mesh generation, raycasting

src/main/resources/assets/herecraft/
└── textures/                   # Block and game textures
