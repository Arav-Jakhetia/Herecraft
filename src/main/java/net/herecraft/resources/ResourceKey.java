package net.herecraft.resources;

import net.herecraft.core.Registry;

public class ResourceKey<T> {
    private final ResourceKey<?> registryName;
    private final Identifier identifier;

    private ResourceKey(ResourceKey<?> registryName, Identifier identifier) {
        this.registryName = registryName;
        this.identifier = identifier;
    }

    private ResourceKey(Identifier identifier) {
        this(null, identifier);
    }

    public static <T> ResourceKey<T> create(ResourceKey<? extends Registry<T>> registryName, final Identifier location) {
        return new ResourceKey<T>(registryName, location);
    }

    public static <T> ResourceKey<Registry<T>> createRegistryKey(Identifier identifier) {
        return new ResourceKey<>(identifier);
    }
}
