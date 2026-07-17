# Herecraft

A Minecraft-style voxel engine built from scratch in Java, using LWJGL 3 and OpenGL 3.3 (core profile). Herecraft implements chunk-based infinite world generation, first-person player physics with AABB collision, real-time block breaking/placing, and a custom rendering pipeline — all without relying on any existing game engine.

## Features

- **Infinite, chunk-streamed world** — chunks dynamically load and unload around the player based on a configurable render distance, keyed by a hashable `ChunkPos`.
- **First-person player controller** — gravity, jumping, and axis-separated AABB collision resolution (with wall-sliding), decoupled from a free-look camera.
- **Block breaking & placing** — raycast-based block targeting with face detection, live chunk mesh rebuilding (including neighboring chunks on edge edits) so there are no seams.
- **Custom rendering pipeline** — greedy per-face culling, a texture-array-based block atlas, and hand-written GLSL shaders (no external shader libraries).
- **HUD elements** — a crosshair and a compass strip showing live cardinal direction (N/E/S/W) as you turn, driven directly by camera yaw.
- **Block highlight outline** — wireframe box rendered around whichever block you're currently looking at.

## Tech Stack

| Component        | Library / Tool              |
|-------------------|------------------------------|
| Language          | Java                         |
| Windowing / Input | [LWJGL 3](https://www.lwjgl.org/) (GLFW bindings) |
| Graphics          | OpenGL 3.3 Core Profile      |
| Math              | [JOML](https://github.com/JOML-CI/JOML) |
| Build Tool        | Gradle                       |
| IDE               | IntelliJ IDEA                |

## Project Structure

```
net.herecraft
├── Herecraft.java              # Main engine loop (init, update, render, cleanup)
├── main/
│   └── Main.java                # Entry point
├── client/
│   ├── block/
│   │   └── Block.java           # Block types and texture layer mapping
│   ├── input/
│   │   ├── Keyboard.java        # Key state polling
│   │   └── Mouse.java           # Cursor delta + click edge detection
│   ├── player/
│   │   └── Player.java          # Physics: gravity, collision, movement
│   ├── render/
│   │   ├── Camera.java              # View/projection matrix, yaw/pitch look
│   │   ├── ChunkRenderer.java        # Per-chunk mesh upload + draw
│   │   ├── CompassRenderer.java       # N/E/S/W HUD compass strip
│   │   ├── CrosshairRenderer.java     # Center-screen crosshair
│   │   ├── BlockHighlightRenderer.java # Wireframe outline on targeted block
│   │   ├── ShaderProgram.java         # GLSL compile/link helper
│   │   └── TextureArray.java          # 2D texture array loader (STB)
│   └── world/
│       ├── World.java           # Chunk load/unload, block get/set across chunks
│       ├── Chunk.java           # Per-chunk block storage + mesh building
│       ├── ChunkPos.java        # Hashable chunk coordinate key
│       ├── Raycast.java         # DDA-style block targeting
│       └── BlockHit.java        # Hit block + adjacent placement face
```

## Getting Started

### Prerequisites

- JDK 17 or newer
- Gradle (or use the included wrapper, if present)
- A GPU/driver supporting OpenGL 3.3 core profile

### Build & Run

```bash
git clone https://github.com/<your-username>/herecraft.git
cd herecraft
./gradlew build
./gradlew run
```

> **Note:** the current build targets Windows natives (`natives-windows` in `build.gradle`). If you're on macOS or Linux, update the `lwjglNatives` value accordingly before building.

## Controls

| Input          | Action              |
|----------------|---------------------|
| `W A S D`      | Move                |
| `Mouse`        | Look around         |
| `Space`        | Jump                |
| `Left Click`   | Break block         |
| `Right Click`  | Place block         |
| `Esc`          | Quit                |

## Acknowledgments

Built as a learning project to understand voxel engine fundamentals — chunking, meshing, collision, and real-time world editing — from first principles.
