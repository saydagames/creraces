package mc.sayda.creraces.engine.traits;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Objects;

/** Matches a BlockState against a JSON block definition: either a block ID or a "#tag" reference. */
public final class BlockDefinitionMatcher {
    private BlockDefinitionMatcher() {
    }

    public static boolean matches(BlockState state, String definition) {
        if (definition.startsWith("#")) {
            ResourceLocation tagLoc = ResourceLocation.parse(definition.substring(1));
            return state.is(TagKey.create(Objects.requireNonNull(Registries.BLOCK), tagLoc));
        }
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString().equals(definition);
    }
}
