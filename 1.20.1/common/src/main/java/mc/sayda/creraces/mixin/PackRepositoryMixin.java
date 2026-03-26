package mc.sayda.creraces.mixin;

import mc.sayda.creraces.util.RacePackProvider;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.RepositorySource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.util.Set;

@Mixin(PackRepository.class)
public class PackRepositoryMixin {

    @Inject(method = "<init>", at = @At("TAIL"))
    private void creraces$addRacePackSource(CallbackInfo ci) {
        try {
            // Determine PackType from stack trace since PackRepository 1.20.1 doesn't store
            // it.
            // Server data repositories are created via ServerPacksSource, even on the
            // client.
            PackType detectedType = PackType.SERVER_DATA;
            StackTraceElement[] stack = Thread.currentThread().getStackTrace();
            boolean isClientContext = false;
            boolean isServerSource = false;

            for (StackTraceElement element : stack) {
                String className = element.getClassName();
                if (className.contains("net.minecraft.server.packs.repository.ServerPacksSource")) {
                    isServerSource = true;
                    break;
                }
                if (className.contains("net.minecraft.client.Minecraft") ||
                        className.contains("net.minecraft.client.main.Main")) {
                    isClientContext = true;
                }
            }

            if (isClientContext && !isServerSource) {
                detectedType = PackType.CLIENT_RESOURCES;
            }

            // Find the sources field (confirmed name 'sources' in Fabric runtime for
            // 1.20.1)
            Field sourcesField = null;
            for (String name : new String[] { "sources", "repositorySources", "field_14227" }) {
                try {
                    sourcesField = PackRepository.class.getDeclaredField(name);
                    break;
                } catch (NoSuchFieldException ignored) {
                }
            }

            if (sourcesField == null) {
                return;
            }
            sourcesField.setAccessible(true);

            @SuppressWarnings("unchecked")
            Set<RepositorySource> sources = (Set<RepositorySource>) sourcesField.get(this);

            // Extra safe addition for cross-platform stability
            try {
                sources.add(new RacePackProvider(detectedType));
            } catch (UnsupportedOperationException e) {
                // If unmodifiable (common on Forge), create a mutable copy and replace
                Set<RepositorySource> mutableSources = new java.util.LinkedHashSet<>(sources);
                mutableSources.add(new RacePackProvider(detectedType));
                sourcesField.set(this, mutableSources);
            }

        } catch (Throwable e) {
            com.mojang.logging.LogUtils.getLogger().error("Failed to inject RacePackProvider", e);
        }
    }
}
