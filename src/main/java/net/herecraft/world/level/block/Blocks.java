package net.herecraft.world.level.block;

import net.herecraft.references.BlockItemIds;
import net.herecraft.world.level.block.state.BlockBehaviour;
import net.herecraft.world.level.block.state.BlockBehaviour.Properties;
import java.util.function.Function;

public class Blocks {
    public static final Block AIR = register(String.valueOf(BlockItemIds.AIR), AirBlock::new, Properties.of().replaceable().noCollision().air());
    public static final Block GRASS = new Block("grass", new BlockBehaviour.Properties(), 0, 1, 2);
    public static final Block DIRT = new Block("dirt", new BlockBehaviour.Properties(), 2);
    public static final Block STONE = new Block("stone", new BlockBehaviour.Properties(), 3);
    public static final Block COBBLESTONE = new Block("cobblestone", new BlockBehaviour.Properties(), 4);
    public static final Block OAK_PLANKS = new Block("oak_planks", new BlockBehaviour.Properties(), 5);
    public static final Block OAK_SAPLING = new Block("oak_sapling", BlockBehaviour.Properties.of().noCollision(), 7);

    private static Block register(final String id, final Function<BlockBehaviour.Properties, Block> factory, final BlockBehaviour.Properties properties) {
        return factory.apply(properties);
    }
}