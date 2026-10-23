package net.herecraft.resources;

import java.util.Objects;

public record Identifier(String namespace, String path) {
    private static final String DEFAULT_NAMESPACE = "herecraft";

    public Identifier {
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(path, "path");

        if(namespace.isBlank() || path.isBlank()) {
            throw new IllegalArgumentException("namespace and path must not be blank");
        }
    }

    public static Identifier withDefaultNamespace(String path) {
        return new Identifier(DEFAULT_NAMESPACE, path);
    }

    @Override
    public String toString() {
        return namespace + ":" + path;
    }
}
