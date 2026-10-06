package net.herecraft.client.block;

public class Block {
    private final BlockType type;

    public Block(BlockType type) {
        this.type = type;
    }

    public static Block air() {
        return new Block(BlockType.AIR);
    }

    public static Block grass() {
        return new Block(BlockType.GRASS);
    }

    public static Block dirt() {
        return new Block(BlockType.DIRT);
    }

    public static Block stone() {
        return new Block(BlockType.STONE);
    }

    public static Block cobblestone() {
        return new Block(BlockType.COBBLESTONE);
    }

    public static Block oak_planks() {
        return new Block(BlockType.OAK_PLANKS);
    }

    public boolean isGrass() {
        return type == BlockType.GRASS;
    }

    public boolean isDirt() {
        return type == BlockType.DIRT;
    }

    public boolean isSolid() {
        return type.isSolid();
    }

    public int getTopTextureLayer() {
        return type.getTopTextureLayer();
    }

    public int getSideTextureLayer() {
        return type.getSideTextureLayer();
    }

    public int getBottomTextureLayer() {
        return type.getBottomTextureLayer();
    }
}
