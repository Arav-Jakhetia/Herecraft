package net.herecraft.references;

import net.herecraft.core.registries.Registries;
import net.herecraft.resources.Identifier;
import net.herecraft.resources.ResourceKey;
import net.herecraft.world.item.Item;
import net.herecraft.world.level.block.Block;

import java.util.Objects;

public record BlockItemId(ResourceKey<Block> block, ResourceKey<Item> item) {
    public BlockItemId {
        Objects.requireNonNull(block, "block");
        Objects.requireNonNull(item, "item");
    }

    public static BlockItemId create(Identifier blockId, Identifier itemId) {
        return new BlockItemId(ResourceKey.create(Registries.BLOCK, blockId), ResourceKey.create(Registries.ITEM, itemId));
    }

    public static BlockItemId create(String blockName, String itemName) {
        return create(Identifier.withDefaultNamespace(blockName), Identifier.withDefaultNamespace(itemName));
    }

    public static BlockItemId create(String name) {
        Identifier id = Identifier.withDefaultNamespace(name);
        return create(id, id);
    }
}
