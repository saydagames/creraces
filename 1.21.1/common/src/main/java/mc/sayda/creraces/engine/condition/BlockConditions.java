package mc.sayda.creraces.engine.condition;

import com.google.gson.JsonObject;
import mc.sayda.creraces.engine.AreaSearch;
import mc.sayda.creraces.engine.Comparison;
import mc.sayda.creraces.engine.DataValue;
import mc.sayda.creraces.engine.ItemSlots;
import mc.sayda.creraces.engine.Position;
import mc.sayda.creraces.engine.traits.BlockDefinitionMatcher;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Set;

/** Conditions about blocks at a "position" (the caster's by default), plus has_entities and smeltable. */
@SuppressWarnings("null")
final class BlockConditions {
    private BlockConditions() {
    }

    private static Position position(JsonObject json, String key) {
        return Position.fromJson(json, key, Position.of(Position.Source.SELF));
    }

    static void register() {
        // "block" is a block id or #tag.
        ConditionRegistry.register(ConditionRegistry.id("block"), json -> {
            String block = GsonHelper.getAsString(json, "block");
            Position position = position(json, "position");
            return (player, target, slot, pos) -> {
                BlockPos at = position.resolveBlock(player, target, slot, pos);
                return at != null && BlockDefinitionMatcher.matches(player.level().getBlockState(at), block);
            };
        });

        // Free for a block: air or replaceable, with no entity in the way.
        ConditionRegistry.register(ConditionRegistry.id("can_place_block"), json -> {
            Position position = position(json, "position");
            return (player, target, slot, pos) -> {
                BlockPos at = position.resolveBlock(player, target, slot, pos);
                if (at == null) {
                    return false;
                }
                BlockState state = player.level().getBlockState(at);
                return (state.isAir() || state.canBeReplaced())
                        && player.level().getEntitiesOfClass(Entity.class, new AABB(at), e -> e != player).isEmpty();
            };
        });

        // Whether "position" and "to" are the same block, e.g. the interacted block and a stored node.
        ConditionRegistry.register(ConditionRegistry.id("same_position"), json -> {
            Position position = position(json, "position");
            if (!json.has("to")) {
                throw new IllegalArgumentException("same_position needs \"to\"");
            }
            Position to = position(json, "to");
            return (player, target, slot, pos) -> {
                BlockPos first = position.resolveBlock(player, target, slot, pos);
                return first != null && first.equals(to.resolveBlock(player, target, slot, pos));
            };
        });

        // Compares a key in the block entity's saved data with "value" (a data value).
        ConditionRegistry.register(ConditionRegistry.id("block_data"), json -> {
            String key = GsonHelper.getAsString(json, "key");
            Position position = position(json, "position");
            Comparison comparison = ConditionRegistry.operator(json);
            DataValue value = DataValue.fromJson(json, "value", DataValue.DataType.DOUBLE);
            if (value == null) {
                throw new IllegalArgumentException("block_data needs a \"value\"");
            }
            return (player, target, slot, pos) -> {
                BlockPos at = position.resolveBlock(player, target, slot, pos);
                BlockEntity blockEntity = at != null ? player.level().getBlockEntity(at) : null;
                if (blockEntity == null) {
                    return false;
                }
                CompoundTag tag = blockEntity.saveWithFullMetadata(player.level().registryAccess());
                return comparison.test(DataValue.readTag(tag, key, value.type()), value.resolve(player, target, slot, pos));
            };
        });

        ConditionRegistry.register(ConditionRegistry.id("has_entities"), json -> {
            AreaSearch search = AreaSearch.fromJson(json, 5.0, Set.of("enemies"));
            return (player, target, slot, pos) -> {
                List<LivingEntity> found = search.find(player, target, slot, pos);
                return found != null && !found.isEmpty();
            };
        });

        // Whether the caster's item in "slot" (default mainhand) has a smelting recipe.
        ConditionRegistry.register(ConditionRegistry.id("smeltable"), json -> {
            String slotName = GsonHelper.getAsString(json, "slot", "mainhand");
            return (player, target, slot, pos) -> {
                ItemStack stack = ItemSlots.stack(player, slotName);
                return !stack.isEmpty() && player.level().getRecipeManager()
                        .getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(stack), player.level()).isPresent();
            };
        });
    }
}
