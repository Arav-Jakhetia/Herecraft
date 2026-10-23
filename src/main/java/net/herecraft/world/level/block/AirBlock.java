package net.herecraft.world.level.block;

import net.herecraft.world.level.block.state.BlockBehaviour;

public class AirBlock extends Block{
    public AirBlock(BlockBehaviour.Properties properties) {
        super("air", properties, 0);
    }
}
