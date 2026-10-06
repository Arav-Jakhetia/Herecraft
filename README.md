# Herecraft

A `Minecraft` inspired voxel sandbox built from scratch in Java using Vulkan through LWJGL.

Herecraft is a learning-focused rendering project that explores how a block-based world can be created without using an existing game engine. It includes Vulkan rendering, chunk-based terrain, textured blocks, first-person movement, collision, raycasting, and block interaction.

> This project is independent and is not affiliated with, endorsed by, or associated with Mojang Studios or Microsoft.

## Build 1

- Additions
  - Blocks
    - Air
    - Grass Block
    - Stone
  - World generation
    - Chunk system (`16 x 16 x 16` blocks per chunk)
    - Face culling between adjacent solid blocks
    - Block-selection outline
    - Automatic chunk loading around the player
  - General
    - GPU texture-array rendering with nearest-neighbour filtering
    - First-person mouse look
    - Raycasting for block selection
    - Left-click block breaking
    - Right-click block placing

## Build 2

- Additions
  - Blocks
    - Cobblestone
    - Dirt
    - Oak Planks
  - General
    - Added the grass block side overlay and colormap.
    - Created the GUI for Main Menu and World Selection Menu.
- Changes
  - World generation
    - Bumps now appear, making hills.

## Under Development

- Particle System
