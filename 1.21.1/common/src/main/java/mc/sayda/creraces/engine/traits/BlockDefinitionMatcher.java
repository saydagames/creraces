package mc.sayda.creraces.engine.traits;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

/** Matches a BlockState against a JSON block definition: either a block ID or a "#tag" reference. */
final class BlockDefinitionMatcher {
    private BlockDefinitionMatcher() {
    }

    static boolean matches(BlockState state, String definition) {
        if (definition.startsWith("#")) {
            ResourceLocation tagLoc = ResourceLocation.parse(definition.substring(1));
            return state.is(net.minecraft.tags.TagKey.create(
                    java.util.Objects.requireNonNull(net.minecraft.core.registries.Registries.BLOCK), tagLoc));
        } else {
            return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString().equals(definition);
        }
    }
}
