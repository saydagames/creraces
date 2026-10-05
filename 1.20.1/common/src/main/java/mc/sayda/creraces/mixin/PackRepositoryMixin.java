package mc.sayda.creraces.mixin;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.util.RacePackProvider;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.RepositorySource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.LinkedHashSet;
import java.util.Set;

/** Adds RacePackProvider to every pack repository, for both resource packs and data packs. */
@Mixin(PackRepository.class)
public class PackRepositoryMixin {

    @Shadow
    @Final
    @Mutable
    private Set<RepositorySource> sources;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void creraces$addRacePackSource(CallbackInfo ci) {
        try {
            RacePackProvider provider = new RacePackProvider(creraces$detectPackType());
            try {
                this.sources.add(provider);
            } catch (UnsupportedOperationException e) {
                // Vanilla keeps the sources in an immutable set, so swap in a mutable copy.
                Set<RepositorySource> mutableSources = new LinkedHashSet<>(this.sources);
                mutableSources.add(provider);
                this.sources = mutableSources;
            }
        } catch (Throwable e) {
            CreRaces.LOGGER.error("Failed to inject RacePackProvider", e);
        }
    }

    /**
     * PackRepository doesn't store its PackType, so infer it from the call stack. Server data
     * repositories are created via ServerPacksSource, even on the client.
     */
    @Unique
    private static PackType creraces$detectPackType() {
        boolean isClientContext = false;
        for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
            String className = element.getClassName();
            if (className.contains("net.minecraft.server.packs.repository.ServerPacksSource")) {
                return PackType.SERVER_DATA;
            }
            if (className.contains("net.minecraft.client.Minecraft")
                    || className.contains("net.minecraft.client.main.Main")) {
                isClientContext = true;
            }
        }
        return isClientContext ? PackType.CLIENT_RESOURCES : PackType.SERVER_DATA;
    }
}
