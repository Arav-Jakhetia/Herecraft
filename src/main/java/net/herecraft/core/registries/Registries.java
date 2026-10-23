package net.herecraft.core.registries;

import net.herecraft.core.Registry;
import net.herecraft.resources.Identifier;
import net.herecraft.resources.ResourceKey;
import net.herecraft.world.item.Item;
import net.herecraft.world.level.block.Block;

public class Registries {
    private Registries() {}

    public static final ResourceKey<Registry<Block>> BLOCK = ResourceKey.<Block>createRegistryKey(Identifier.withDefaultNamespace("block"));
    public static final ResourceKey<Registry<Item>> ITEM = ResourceKey.<Item>createRegistryKey(Identifier.withDefaultNamespace("item"));
}
