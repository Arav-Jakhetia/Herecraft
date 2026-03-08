# Herecraft

Herecraft is a Minecraft-style voxel game built with Java, LWJGL, OpenGL, and Gradle.

## Requirements

- Java 17
- Windows (current native setup uses `lwjgl` Windows natives)

## Run

```bash
./gradlew run
```

On Windows PowerShell:

```powershell
.\gradlew.bat run
```

## Controls

- `W` move forward
- `S` move backward
- `A` move left
- `D` move right
- `Mouse` look around
- `Space` jump
- `R` respawn at world center
- `F2` take screenshot and save to `Downloads`
- `Esc` close game

## World

- Size: `256 x 64 x 256` blocks
- Chunk size: `16 x 16` blocks (XZ)
- Flat terrain with chunk streaming (load/unload by player distance)
- Top layers can generate grass based on light rules
- Caves generate downward while keeping a bottom cobblestone layer

## Textures

Block textures are loaded from:

- `src/main/resources/herecraft/textures/block/`

Grass colormap is loaded from:

- `src/main/resources/textures/colormap/grass.png`

