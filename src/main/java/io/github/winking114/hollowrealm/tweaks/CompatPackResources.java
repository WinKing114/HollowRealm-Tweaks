package io.github.winking114.hollowrealm.tweaks;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** An independent server-data pack, never the ordinary low-priority Forge mod pack. */
final class CompatPackResources extends AbstractPackResources {
    static final String PACK_ID = "hollowrealm_tweaks:compat";
    private static final byte[] METADATA = "{\"pack\":{\"pack_format\":15,\"description\":\"HollowRealm audited data compatibility\"}}".getBytes(StandardCharsets.UTF_8);
    private final Map<ResourceLocation, byte[]> overrides;

    CompatPackResources(String id, Map<ResourceLocation, byte[]> overrides) {
        super(id, true);
        var copy = new HashMap<ResourceLocation, byte[]>();
        overrides.forEach((key, bytes) -> copy.put(key, bytes.clone()));
        this.overrides = Map.copyOf(copy);
    }

    static Pack createPack(Map<ResourceLocation, byte[]> overrides) {
        return Pack.readMetaAndCreate(PACK_ID, Component.literal("HollowRealm Tweaks compatibility"), true,
                id -> new CompatPackResources(id, overrides), PackType.SERVER_DATA, Pack.Position.TOP, PackSource.BUILT_IN);
    }

    @Override public IoSupplier<InputStream> getRootResource(String... parts) {
        return parts.length == 1 && "pack.mcmeta".equals(parts[0]) ? () -> new ByteArrayInputStream(METADATA) : null;
    }

    @Override public IoSupplier<InputStream> getResource(PackType type, ResourceLocation id) {
        byte[] bytes = type == PackType.SERVER_DATA ? overrides.get(id) : null;
        return bytes == null ? null : () -> new ByteArrayInputStream(bytes);
    }

    @Override public void listResources(PackType type, String namespace, String prefix, ResourceOutput output) {
        if (type != PackType.SERVER_DATA) return;
        String cleanPrefix = prefix == null ? "" : prefix;
        while (cleanPrefix.endsWith("/")) {
            cleanPrefix = cleanPrefix.substring(0, cleanPrefix.length() - 1);
        }
        final String normalizedPrefix = cleanPrefix;
        overrides.forEach((id, bytes) -> {
            if (id.getNamespace().equals(namespace)
                    && (normalizedPrefix.isEmpty()
                            || id.getPath().equals(normalizedPrefix)
                            || id.getPath().startsWith(normalizedPrefix + "/"))) {
                output.accept(id, () -> new ByteArrayInputStream(bytes));
            }
        });
    }

    @Override public Set<String> getNamespaces(PackType type) {
        return type == PackType.SERVER_DATA
                ? overrides.keySet().stream().map(ResourceLocation::getNamespace).collect(Collectors.toUnmodifiableSet()) : Set.of();
    }

    @Override public void close() {}
}
