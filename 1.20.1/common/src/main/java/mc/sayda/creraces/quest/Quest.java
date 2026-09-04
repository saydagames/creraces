package mc.sayda.creraces.quest;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * Represents a JSON-defined quest offered by the Quest Board.
 * Defined via JSON in data/creraces/quests/
 */
public class Quest {
    private final ResourceLocation id;
    private final int tier;
    private final Component name;
    private final Component description;
    private final int durationDays;
    private final Objective objective;

    private Quest(Builder builder) {
        this.id = builder.id;
        this.tier = builder.tier;
        this.name = builder.name;
        this.description = builder.description;
        this.durationDays = builder.durationDays;
        this.objective = builder.objective;
    }

    public ResourceLocation id() {
        return id;
    }

    public int tier() {
        return tier;
    }

    public Component name() {
        return name;
    }

    public Component description() {
        return description;
    }

    public int durationDays() {
        return durationDays;
    }

    public Objective objective() {
        return objective;
    }

    public static class Builder {
        private final ResourceLocation id;
        private int tier = 1;
        private Component name = Component.empty();
        private Component description = Component.empty();
        private int durationDays = 1;
        private Objective objective;

        public Builder(ResourceLocation id) {
            this.id = id;
        }

        public Builder tier(int tier) {
            this.tier = tier;
            return this;
        }

        public Builder name(Component name) {
            if (name != null) this.name = name;
            return this;
        }

        public Builder description(Component description) {
            if (description != null) this.description = description;
            return this;
        }

        public Builder durationDays(int durationDays) {
            this.durationDays = durationDays;
            return this;
        }

        public Builder objective(Objective objective) {
            this.objective = objective;
            return this;
        }

        public Quest build() {
            if (this.id == null)
                throw new IllegalStateException("Quest ID cannot be null");
            if (this.objective == null)
                throw new IllegalStateException("Quest objective cannot be null for quest " + this.id);
            return new Quest(this);
        }
    }

    /**
     * A quest's win condition. Each concrete type owns its own target matching so new
     * objective types can be added without changing this interface.
     */
    public interface Objective {
        int count();

        /** The dispatch key used in JSON (e.g. "kill_entity"), for display/debugging. */
        String type();

        /** A short verb for GUI display, e.g. "Kill", "Mine", "Collect". */
        Component verb();

        /** A human-readable name for the target entity/block/item or tag. */
        Component targetName();
    }

    /** "minecraft:gold_ingot" -> "Gold Ingot"; "minecraft:zombies" (tag path) -> "Zombies". */
    private static Component prettifyPath(ResourceLocation id) {
        String[] words = id.getPath().split("_");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (w.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        }
        return Component.literal(sb.toString());
    }

    public static class KillEntityObjective implements Objective {
        @Nullable private final ResourceLocation targetId;
        @Nullable private final TagKey<net.minecraft.world.entity.EntityType<?>> targetTag;
        private final int count;

        public KillEntityObjective(@Nullable ResourceLocation targetId,
                @Nullable TagKey<net.minecraft.world.entity.EntityType<?>> targetTag, int count) {
            this.targetId = targetId;
            this.targetTag = targetTag;
            this.count = count;
        }

        public boolean matches(LivingEntity victim) {
            if (targetTag != null) return victim.getType().is(targetTag);
            if (targetId != null) return BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType()).equals(targetId);
            return false;
        }

        @Override public int count() { return count; }
        @Override public String type() { return "kill_entity"; }

        @Override public Component verb() { return Component.translatable("quest.creraces.verb.kill"); }

        @Override public Component targetName() {
            if (targetId != null) {
                var entityType = BuiltInRegistries.ENTITY_TYPE.get(targetId);
                return entityType != null ? entityType.getDescription() : prettifyPath(targetId);
            }
            return targetTag != null ? prettifyPath(targetTag.location()) : Component.literal("?");
        }
    }

    public static class MineBlockObjective implements Objective {
        @Nullable private final ResourceLocation targetId;
        @Nullable private final TagKey<net.minecraft.world.level.block.Block> targetTag;
        private final int count;

        public MineBlockObjective(@Nullable ResourceLocation targetId,
                @Nullable TagKey<net.minecraft.world.level.block.Block> targetTag, int count) {
            this.targetId = targetId;
            this.targetTag = targetTag;
            this.count = count;
        }

        public boolean matches(BlockState state) {
            if (targetTag != null) return state.is(targetTag);
            if (targetId != null) return BuiltInRegistries.BLOCK.getKey(state.getBlock()).equals(targetId);
            return false;
        }

        @Override public int count() { return count; }
        @Override public String type() { return "mine_block"; }

        @Override public Component verb() { return Component.translatable("quest.creraces.verb.mine"); }

        @Override public Component targetName() {
            if (targetId != null) {
                var block = BuiltInRegistries.BLOCK.get(targetId);
                return block != null ? block.getName() : prettifyPath(targetId);
            }
            return targetTag != null ? prettifyPath(targetTag.location()) : Component.literal("?");
        }
    }

    public static class CollectItemObjective implements Objective {
        @Nullable private final ResourceLocation targetId;
        @Nullable private final TagKey<net.minecraft.world.item.Item> targetTag;
        private final int count;

        public CollectItemObjective(@Nullable ResourceLocation targetId,
                @Nullable TagKey<net.minecraft.world.item.Item> targetTag, int count) {
            this.targetId = targetId;
            this.targetTag = targetTag;
            this.count = count;
        }

        public boolean matches(ItemStack stack) {
            if (targetTag != null) return stack.is(targetTag);
            if (targetId != null) return BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(targetId);
            return false;
        }

        @Override public int count() { return count; }
        @Override public String type() { return "collect_item"; }

        @Override public Component verb() { return Component.translatable("quest.creraces.verb.collect"); }

        @Override public Component targetName() {
            if (targetId != null) {
                var item = BuiltInRegistries.ITEM.get(targetId);
                return item != null ? item.getDescription() : prettifyPath(targetId);
            }
            return targetTag != null ? prettifyPath(targetTag.location()) : Component.literal("?");
        }
    }

    /** Parses a "target" string as either a plain id or a #-prefixed tag. */
    public static final class TargetRef {
        @Nullable public final ResourceLocation id;
        public final boolean isTag;

        private TargetRef(@Nullable ResourceLocation id, boolean isTag) {
            this.id = id;
            this.isTag = isTag;
        }

        public static TargetRef parse(String raw) {
            if (raw.startsWith("#")) {
                ResourceLocation id = ResourceLocation.tryParse(raw.substring(1));
                return new TargetRef(id, true);
            }
            return new TargetRef(ResourceLocation.tryParse(raw), false);
        }
    }

    public static TagKey<net.minecraft.world.entity.EntityType<?>> entityTag(ResourceLocation id) {
        return TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, id);
    }

    public static TagKey<net.minecraft.world.level.block.Block> blockTag(ResourceLocation id) {
        return TagKey.create(net.minecraft.core.registries.Registries.BLOCK, id);
    }

    public static TagKey<net.minecraft.world.item.Item> itemTag(ResourceLocation id) {
        return TagKey.create(net.minecraft.core.registries.Registries.ITEM, id);
    }
}
