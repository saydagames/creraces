package mc.sayda.creraces.engine.condition;

import java.util.Objects;
import com.google.gson.JsonObject;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import mc.sayda.creraces.util.ItemUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import mc.sayda.creraces.engine.TargetFilter;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

/**
 * Universal condition check for actions and traits.
 */
public interface Condition {
    boolean evaluate(Player player, @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos);

    @SuppressWarnings("null")
    static Condition fromJson(JsonObject json) {
        String typeStr = GsonHelper.getAsString(json, "type");
        @SuppressWarnings("null")
        ResourceLocation typeLoc = new ResourceLocation(typeStr);
        String type = typeLoc.getPath();

        try {
            return switch (type) {
                case "state_equals" -> {
                    String state = GsonHelper.getAsString(json, "state");
                    ScalingValue value = ScalingValue.fromJson(json, "value", 1.0);
                    yield new StateEqualsCondition(state, value);
                }
                case "morphed" -> {
                    boolean expected = GsonHelper.getAsBoolean(json, "value", true);
                    yield new MorphedCondition(expected);
                }
                case "flying" -> {
                    boolean expected = GsonHelper.getAsBoolean(json, "value", true);
                    yield new FlyingCondition(expected);
                }
                case "ability_state" -> {
                    String ability = GsonHelper.getAsString(json, "ability");
                    ScalingValue value = ScalingValue.fromJson(json, "value", 1.0);
                    yield new StateEqualsCondition(ability, value);
                }
                case "sneaking" -> {
                    boolean expected = GsonHelper.getAsBoolean(json, "value", true);
                    yield new SneakingCondition(expected);
                }
                case "on_ground" -> {
                    boolean expected = GsonHelper.getAsBoolean(json, "value", true);
                    yield new OnGroundCondition(expected);
                }
                case "wearing_armor" -> {
                    String item = GsonHelper.getAsString(json, "item", null);
                    String tag = GsonHelper.getAsString(json, "tag", null);
                    String slot = GsonHelper.getAsString(json, "slot", "0");
                    if (tag != null && !tag.startsWith("#"))
                        tag = "#" + tag;
                    yield new WearingArmorCondition(item != null ? item : tag, slot);
                }
                case "holding_item" -> {
                    String item = GsonHelper.getAsString(json, "item", null);
                    String tag = GsonHelper.getAsString(json, "tag", null);
                    boolean useTarget = GsonHelper.getAsBoolean(json, "use_target", false);
                    if (tag != null && !tag.startsWith("#"))
                        tag = "#" + tag;
                    yield new HoldingItemCondition(item != null ? item : tag, useTarget);
                }
                case "item_interaction" -> {
                    String item = GsonHelper.getAsString(json, "item", "minecraft:air");
                    yield new ItemInteractionCondition(item);
                }
                case "and" -> {
                    com.google.gson.JsonArray array = json.getAsJsonArray("conditions");
                    Condition[] conditions = new Condition[array.size()];
                    for (int i = 0; i < array.size(); i++) {
                        conditions[i] = fromJson(array.get(i).getAsJsonObject());
                    }
                    yield new AndCondition(conditions);
                }
                case "or" -> {
                    com.google.gson.JsonArray array = json.getAsJsonArray("conditions");
                    Condition[] conditions = new Condition[array.size()];
                    for (int i = 0; i < array.size(); i++) {
                        conditions[i] = fromJson(array.get(i).getAsJsonObject());
                    }
                    yield new OrCondition(conditions);
                }
                case "not" -> {
                    Condition condition = fromJson(json.getAsJsonObject("condition"));
                    yield new NotCondition(condition);
                }
                case "biome" -> {
                    String biomeId = GsonHelper.getAsString(json, "biome", null);
                    String tag = GsonHelper.getAsString(json, "tag", null);
                    yield new BiomeCondition(biomeId, tag);
                }
                case "weather" -> {
                    String weatherType = GsonHelper.getAsString(json, "weather");
                    yield new WeatherCondition(weatherType);
                }
                case "time" -> {
                    ScalingValue min = ScalingValue.fromJson(json, "min", 0.0);
                    ScalingValue max = ScalingValue.fromJson(json, "max", 24000.0);
                    yield new TimeCondition(min, max);
                }
                case "in_water" -> {
                    boolean expected = GsonHelper.getAsBoolean(json, "value", true);
                    yield new InWaterCondition(expected);
                }
                case "in_sunlight" -> {
                    boolean expected = GsonHelper.getAsBoolean(json, "value", true);
                    yield new InSunlightCondition(expected);
                }
                case "altitude" -> {
                    ScalingValue min = ScalingValue.fromJson(json, "min", Double.NEGATIVE_INFINITY);
                    ScalingValue max = ScalingValue.fromJson(json, "max", Double.POSITIVE_INFINITY);
                    yield new AltitudeCondition(min, max);
                }
                case "is_burning" -> {
                    boolean expected = GsonHelper.getAsBoolean(json, "value", true);
                    yield new IsBurningCondition(expected);
                }
                case "is_moving" -> {
                    boolean expected = GsonHelper.getAsBoolean(json, "value", true);
                    ScalingValue threshold = ScalingValue.fromJson(json, "threshold", 0.1);
                    yield new IsMovingCondition(expected, threshold);
                }
                case "resource_level" -> {
                    String resource = GsonHelper.getAsString(json, "resource");
                    String operator = GsonHelper.getAsString(json, "operator", ">=");
                    ScalingValue value = ScalingValue.fromJson(json, "value", 0.0);
                    yield new ResourceLevelCondition(resource, operator, value);
                }
                case "has_effect" -> {
                    String id = GsonHelper.getAsString(json, "effect");
                    ScalingValue amp = ScalingValue.fromJson(json, "amplifier", 0);
                    boolean useTarget = GsonHelper.getAsBoolean(json, "use_target", false);
                    @SuppressWarnings("null")
                    Condition c = new HasEffectCondition(new ResourceLocation(id), amp, useTarget);
                    yield c;
                }
                case "is_smeltable" -> {
                    yield new IsSmeltableCondition();
                }
                case "exposed_to_rain" -> {
                    boolean expected = GsonHelper.getAsBoolean(json, "value", true);
                    yield new ExposedToRainCondition(expected);
                }
                case "spirit" -> {
                    boolean expected = GsonHelper.getAsBoolean(json, "value", true);
                    yield new SpiritCondition(expected);
                }
                case "entity_data" -> {
                    yield EntityDataCondition.fromJson(json);
                }
                case "entity" -> {
                    yield EntityCondition.fromJson(json);
                }
                case "distance" -> {
                    // Accept "range" as the canonical name; "max" kept for backwards compatibility
                    ScalingValue max = json.has("range")
                            ? ScalingValue.fromJson(json, "range", 5.0)
                            : ScalingValue.fromJson(json, "max", 5.0);
                    ScalingValue x = ScalingValue.fromJson(json, "x", 0.0);
                    ScalingValue y = ScalingValue.fromJson(json, "y", 0.0);
                    ScalingValue z = ScalingValue.fromJson(json, "z", 0.0);
                    yield new DistanceCondition(x, y, z, max);
                }
                case "attack_charged" -> {
                    ScalingValue threshold = ScalingValue.fromJson(json, "threshold", 0.9);
                    yield new AttackChargedCondition(threshold);
                }
                case "is_position" -> {
                    ScalingValue x = ScalingValue.fromJson(json, "x", 0.0);
                    ScalingValue y = ScalingValue.fromJson(json, "y", 0.0);
                    ScalingValue z = ScalingValue.fromJson(json, "z", 0.0);
                    yield new IsPositionCondition(x, y, z);
                }
                case "dimension" -> {
                    String dim = GsonHelper.getAsString(json, "value", "minecraft:overworld");
                    yield new DimensionCondition(dim);
                }
                case "customization_equals" -> {
                    String id = GsonHelper.getAsString(json, "id");
                    java.util.List<String> validVals = new java.util.ArrayList<>();
                    for (com.google.gson.JsonElement e : json.getAsJsonArray("values")) {
                        validVals.add(e.getAsString());
                    }
                    yield (player, target, slotCtx, interactionPos) -> {
                        return DataUtils.getVariables(player).map(vars -> {
                            ResourceLocation activeRaceLoc = vars.getRace();
                            if (activeRaceLoc == null)
                                return false;

                            mc.sayda.creraces.race.Race rd = mc.sayda.creraces.race.RaceRegistry.get(activeRaceLoc);
                            if (rd == null)
                                return false;

                            mc.sayda.creraces.race.RaceCustomization cData = rd.customization().stream()
                                    .filter(c -> c.id().equals(id))
                                    .findFirst()
                                    .orElse(null);
                            if (cData == null)
                                return false;

                            String val = vars.getCustomization(id.toLowerCase());
                            if (val == null || val.isEmpty())
                                val = cData.defaultValue();

                            return validVals.contains(val);
                        }).orElse(false);
                    };
                }
                case "race_equals" -> {
                    java.util.List<String> validRaces = new java.util.ArrayList<>();
                    if (json.has("race")) {
                        validRaces.add(GsonHelper.getAsString(json, "race"));
                    }
                    if (json.has("races")) {
                        for (com.google.gson.JsonElement e : json.getAsJsonArray("races")) {
                            validRaces.add(e.getAsString());
                        }
                    }
                    yield (player, target, slotCtx, interactionPos) -> {
                        return DataUtils.getVariables(player).map(vars -> {
                            ResourceLocation activeRaceLoc = vars.getRace();
                            String activeRaceId = activeRaceLoc != null ? activeRaceLoc.toString() : null;
                            if (activeRaceId == null || activeRaceId.isEmpty())
                                return false;
                            for (String race : validRaces) {
                                if (activeRaceId.equals(race))
                                    return true;
                            }
                            return false;
                        }).orElse(false);
                    };
                }
                case "has_customization" -> {
                    String id = GsonHelper.getAsString(json, "id");
                    yield (player, target, slotCtx, interactionPos) -> {
                        return DataUtils.getVariables(player).map(vars -> {
                            ResourceLocation activeRaceLoc = vars.getRace();
                            if (activeRaceLoc == null)
                                return false;
                            String val = vars.getCustomization(id.toLowerCase());
                            return val != null && !val.isEmpty() && !val.equals("0.0") && !val.equals("0");
                        }).orElse(false);
                    };
                }
                case "has_entities" -> {
                    mc.sayda.creraces.engine.ScalingValue radius = mc.sayda.creraces.engine.ScalingValue.fromJson(json,
                            "radius", 5.0);
                    TargetFilter targets = TargetFilter.fromJson(json, "targets");
                    yield new HasEntitiesCondition(radius, targets);
                }
                case "is_block" -> {
                    String blockStr = GsonHelper.getAsString(json, "block", "minecraft:air");
                    ScalingValue ox = ScalingValue.fromJson(json, "offset_x", 0.0);
                    ScalingValue oy = ScalingValue.fromJson(json, "offset_y", 0.0);
                    ScalingValue oz = ScalingValue.fromJson(json, "offset_z", 0.0);
                    boolean useInteractionPos = GsonHelper.getAsBoolean(json, "use_interaction_pos", true);
                    boolean absolute = GsonHelper.getAsBoolean(json, "absolute", false);
                    yield new IsBlockCondition(blockStr, ox, oy, oz, useInteractionPos, absolute);
                }
                default ->
                    throw new IllegalArgumentException("Unknown condition type '" + typeStr + "' — check your JSON");
            };
        } catch (Exception e) {
            mc.sayda.creraces.CreRaces.LOGGER.error(
                    "Failed to parse condition '{}': {} — condition will always return false. JSON: {}",
                    typeStr, e.getMessage(), json);
            return (player, target, slot, interactionPos) -> false;
        }
    }
}

class WearingArmorCondition implements Condition {
    private final @javax.annotation.Nullable String definition;
    private final String slot;

    public WearingArmorCondition(@javax.annotation.Nullable String definition, String slot) {
        this.definition = definition;
        this.slot = slot;
    }

    @Override
    public boolean evaluate(Player player,
            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot abilitySlot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {

        Iterable<net.minecraft.world.item.ItemStack> items;
        if (slot.equalsIgnoreCase("head")) {
            items = java.util.List.of(player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD));
        } else if (slot.equalsIgnoreCase("chest")) {
            items = java.util.List.of(player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST));
        } else if (slot.equalsIgnoreCase("legs")) {
            items = java.util.List.of(player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS));
        } else if (slot.equalsIgnoreCase("feet")) {
            items = java.util.List.of(player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET));
        } else {
            items = player.getArmorSlots();
        }

        for (net.minecraft.world.item.ItemStack stack : items) {
            if (definition == null) {
                if (!stack.isEmpty())
                    return true;
            } else {
                if (ItemUtils.matches(stack, definition))
                    return true;
            }
        }
        return false;
    }
}

class ItemInteractionCondition implements Condition {
    private final String definition;

    public ItemInteractionCondition(String definition) {
        this.definition = definition;
    }

    @Override
    public boolean evaluate(Player player,
            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        return ItemUtils.matches(player.getMainHandItem(), definition)
                || ItemUtils.matches(player.getOffhandItem(), definition);
    }
}

class HoldingItemCondition implements Condition {
    private final @javax.annotation.Nullable String definition;
    private final boolean useTarget;

    public HoldingItemCondition(@javax.annotation.Nullable String definition, boolean useTarget) {
        this.definition = definition;
        this.useTarget = useTarget;
    }

    @Override
    public boolean evaluate(Player player,
            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        // Smart Targeting: Prefer target if present, otherwise respect useTarget flag
        net.minecraft.world.entity.LivingEntity entity = (target != null) ? target : (useTarget ? null : player);
        if (entity == null || definition == null)
            return false;

        for (net.minecraft.world.InteractionHand hand : net.minecraft.world.InteractionHand.values()) {
            @SuppressWarnings("null")
            net.minecraft.world.item.ItemStack stack = entity.getItemInHand(hand);
            if (ItemUtils.matches(stack, definition))
                return true;
        }
        return false;
    }
}

class StateEqualsCondition implements Condition {
    private final String state;
    private final ScalingValue value;

    public StateEqualsCondition(String state, ScalingValue value) {
        this.state = state;
        this.value = value;
    }

    @Override
    public boolean evaluate(Player player,
            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        return DataUtils.getVariables(player).map(vars -> {
            ResourceLocation targetId = null;
            if ("slot".equalsIgnoreCase(state) && slot != null) {
                targetId = vars.getAbilityInSlot(slot);
            } else if (state != null && !state.isEmpty()) {
                try {
                    targetId = new ResourceLocation(state);
                } catch (Exception e) {
                    return false;
                }
            }

            if (targetId == null)
                return false;

            double current = vars.getAbilityState(targetId);
            double val = value.evaluate(player, target);
            return Math.abs(current - val) < 0.001;
        }).orElse(false);
    }
}

class MorphedCondition implements Condition {
    private final boolean expected;

    public MorphedCondition(boolean expected) {
        this.expected = expected;
    }

    @Override
    public boolean evaluate(Player player,
            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        return DataUtils.getVariables(player)
                .map(vars -> vars.isMorphed() == expected)
                .orElse(false);
    }
}

class FlyingCondition implements Condition {
    private final boolean expected;

    public FlyingCondition(boolean expected) {
        this.expected = expected;
    }

    @Override
    public boolean evaluate(Player player,
            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        boolean isFlying = player.getAbilities().flying || player.isFallFlying();
        return isFlying == expected;
    }
}

class SneakingCondition implements Condition {
    private final boolean expected;

    public SneakingCondition(boolean expected) {
        this.expected = expected;
    }

    @Override
    public boolean evaluate(Player player,
            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        return player.isShiftKeyDown() == expected;
    }
}

class OnGroundCondition implements Condition {
    private final boolean expected;

    public OnGroundCondition(boolean expected) {
        this.expected = expected;
    }

    @Override
    public boolean evaluate(Player player,
            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        return player.onGround() == expected;
    }
}

class AttackChargedCondition implements Condition {
    private final ScalingValue threshold;

    public AttackChargedCondition(ScalingValue threshold) {
        this.threshold = threshold;
    }

    @Override
    public boolean evaluate(Player player,
            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        return player.getAttackStrengthScale(0.5f) >= threshold.evaluate(player, target);
    }
}

class HasEffectCondition implements Condition {
    private final ResourceLocation effectId;
    private final ScalingValue minAmplifier;
    private final boolean useTarget;

    public HasEffectCondition(ResourceLocation effectId, ScalingValue minAmplifier, boolean useTarget) {
        this.effectId = effectId;
        this.minAmplifier = minAmplifier;
        this.useTarget = useTarget;
    }

    @Override
    public boolean evaluate(Player player,
            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        @SuppressWarnings("null")
        net.minecraft.world.effect.MobEffect effect = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT
                .get(effectId);
        if (effect == null)
            return false;

        // Smart Targeting: Prefer target if present, otherwise respect useTarget flag
        net.minecraft.world.entity.LivingEntity subject = (target != null) ? target : (useTarget ? null : player);
        if (subject == null)
            return false;
        net.minecraft.world.effect.MobEffectInstance instance = subject.getEffect(effect);
        return instance != null && instance.getAmplifier() >= (int) minAmplifier.evaluate(player, target);
    }
}

class AndCondition implements Condition {
    private final Condition[] conditions;

    public AndCondition(Condition[] conditions) {
        this.conditions = conditions;
    }

    @Override
    public boolean evaluate(Player player,
            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        for (Condition c : conditions) {
            if (!c.evaluate(player, target, slot, interactionPos))
                return false;
        }
        return true;
    }
}

class OrCondition implements Condition {
    private final Condition[] conditions;

    public OrCondition(Condition[] conditions) {
        this.conditions = conditions;
    }

    @Override
    public boolean evaluate(Player player,
            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        for (Condition c : conditions) {
            if (c.evaluate(player, target, slot, interactionPos))
                return true;
        }
        return false;
    }
}

class NotCondition implements Condition {
    private final Condition condition;

    public NotCondition(Condition condition) {
        this.condition = condition;
    }

    @Override
    public boolean evaluate(Player player,
            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        return !condition.evaluate(player, target, slot, interactionPos);
    }
}

class BiomeCondition implements Condition {
    private final @javax.annotation.Nullable String biomeId;
    private final @javax.annotation.Nullable String tag;

    public BiomeCondition(@javax.annotation.Nullable String biomeId, @javax.annotation.Nullable String tag) {
        this.biomeId = biomeId;
        this.tag = tag;
    }

    @Override
    public boolean evaluate(Player player,
            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        @SuppressWarnings("null")
        net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> holder = player.level()
                .getBiome(player.blockPosition());
        if (biomeId != null) {
            return holder.unwrapKey().map(key -> key.location().toString().equals(biomeId)).orElse(false);
        }
        if (tag != null && !tag.isEmpty()) {
            try {
                @SuppressWarnings("null")
                net.minecraft.tags.TagKey<net.minecraft.world.level.biome.Biome> tagKey = net.minecraft.tags.TagKey
                        .create(net.minecraft.core.registries.Registries.BIOME, new ResourceLocation(tag));
                return holder.is(tagKey);
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }
}

class WeatherCondition implements Condition {
    private final String type;

    public WeatherCondition(String type) {
        this.type = type;
    }

    @Override
    public boolean evaluate(Player player,
            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        return switch (type.toLowerCase()) {
            case "rain" -> player.level().isRaining();
            case "thunder" -> player.level().isThundering();
            case "clear" -> !player.level().isRaining();
            default -> false;
        };
    }
}

class TimeCondition implements Condition {
    private final ScalingValue min;
    private final ScalingValue max;

    public TimeCondition(ScalingValue min, ScalingValue max) {
        this.min = min;
        this.max = max;
    }

    @Override
    public boolean evaluate(Player player,
            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        long time = player.level().getDayTime() % 24000;
        return time >= (long) min.evaluate(player, target) && time <= (long) max.evaluate(player, target);
    }
}

class InWaterCondition implements Condition {
    private final boolean expected;

    public InWaterCondition(boolean expected) {
        this.expected = expected;
    }

    @Override
    public boolean evaluate(Player player,
            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        @SuppressWarnings("null")
        boolean inBubble = player.level().getBlockState(player.blockPosition())
                .is(net.minecraft.world.level.block.Blocks.BUBBLE_COLUMN);
        return (player.isInWater() || inBubble) == expected;
    }
}

class InSunlightCondition implements Condition {
    private final boolean expected;

    public InSunlightCondition(boolean expected) {
        this.expected = expected;
    }

    @Override
    public boolean evaluate(Player player,
            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        @SuppressWarnings("null")
        boolean inSun = player.level().isDay() &&
                !player.level().isRaining() &&
                player.level().canSeeSky(player.blockPosition());
        return inSun == expected;
    }
}

class AltitudeCondition implements Condition {
    private final ScalingValue min;
    private final ScalingValue max;

    public AltitudeCondition(ScalingValue min, ScalingValue max) {
        this.min = min;
        this.max = max;
    }

    @Override
    public boolean evaluate(Player player,
            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        double y = player.getY();
        return y >= min.evaluate(player, target) && y <= max.evaluate(player, target);
    }
}

class IsBurningCondition implements Condition {
    private final boolean expected;

    public IsBurningCondition(boolean expected) {
        this.expected = expected;
    }

    @Override
    public boolean evaluate(Player player,
            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        return player.isOnFire() == expected;
    }
}

class IsMovingCondition implements Condition {
    private final boolean expected;
    private final ScalingValue threshold;

    public IsMovingCondition(boolean expected, ScalingValue threshold) {
        this.expected = expected;
        this.threshold = threshold;
    }

    @Override
    public boolean evaluate(Player player,
            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        net.minecraft.world.phys.Vec3 vel = player.getDeltaMovement();
        double t = threshold.evaluate(player, target);
        boolean isMoving = (vel.x * vel.x + vel.z * vel.z) > (t * t);
        return isMoving == expected;
    }
}

class ResourceLevelCondition implements Condition {
    private final String resource;
    private final String operator;
    private final ScalingValue value;

    public ResourceLevelCondition(String resource, String operator, ScalingValue value) {
        this.resource = resource;
        this.operator = operator;
        this.value = value;
    }

    @Override
    public boolean evaluate(Player player,
            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        return DataUtils.getVariables(player).map(vars -> {
            double current = switch (resource.toLowerCase()) {
                case "mana" -> vars.getMana();
                case "energy" -> vars.getEnergy();
                case "grit" -> vars.getGrit();
                case "rage" -> vars.getRage();
                case "karma" -> vars.getKarma();
                case "souls" -> vars.getSouls();
                case "coins" -> vars.getCoins();
                case "stacks" -> vars.getStacks();
                case "ap" -> vars.getAp();
                case "ad" -> vars.getAd();
                case "ah" -> vars.getAh();
                case "cr" -> vars.getCr();
                default -> 0.0;
            };

            double val = value.evaluate(player, target);
            return switch (operator) {
                case ">=" -> current >= val;
                case "<=" -> current <= val;
                case ">" -> current > val;
                case "<" -> current < val;
                case "==" -> Math.abs(current - val) < 0.001;
                case "!=" -> Math.abs(current - val) >= 0.001;
                default -> false;
            };
        }).orElse(false);
    }
}

class ExposedToRainCondition implements Condition {
    private final boolean expected;

    public ExposedToRainCondition(boolean expected) {
        this.expected = expected;
    }

    @Override
    public boolean evaluate(Player player,
            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        net.minecraft.world.level.Level level = player.level();
        BlockPos pos = player.blockPosition();

        boolean isVanillaRaining = level.isRainingAt(pos);

        if (isVanillaRaining && expected) {
            // Check for shelter starting from the player's position
            for (int dy = 0; dy <= 16; dy++) {
                BlockPos overheadPos = pos.above(dy);
                BlockState state = level.getBlockState(overheadPos);
                if (state.is(mc.sayda.creraces.registry.ModBlocks.MICRO_BLOCK.get())) {
                    if (level.getBlockEntity(
                            overheadPos) instanceof mc.sayda.creraces.block.entity.MicroBlockEntity micro) {
                        int playerSlotY = -1;
                        if (dy == 0) {
                            double yOffset = player.getY() - pos.getY();
                            playerSlotY = (int) (yOffset * 4);
                        }

                        // Calculate player's sub-grid column once
                        int sx = (int) (((player.getX() - overheadPos.getX()) % 1.0 + 1.0) % 1.0 * 4);
                        int sz = (int) (((player.getZ() - overheadPos.getZ()) % 1.0 + 1.0) % 1.0 * 4);
                        sx = Math.max(0, Math.min(3, sx));
                        sz = Math.max(0, Math.min(3, sz));

                        for (int sy = playerSlotY + 1; sy < 4; sy++) {
                            if (!micro.getSlot(sx, sy, sz).isAir()) {
                                return !expected; // Sheltered by mini-block roof in THIS column!
                            }
                        }
                    }
                } else if (dy > 0 && state.isSolidRender(level, overheadPos)) {
                    // Regular solid block shelter
                    return !expected;
                }
            }
        }

        return isVanillaRaining == expected;
    }
}

class IsSmeltableCondition implements Condition {
    public IsSmeltableCondition() {
    }

    @Override
    public boolean evaluate(Player player,
            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        net.minecraft.world.item.ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty())
            return false;
        return player.level().getRecipeManager()
                .getRecipeFor(net.minecraft.world.item.crafting.RecipeType.SMELTING,
                        new net.minecraft.world.SimpleContainer(stack), player.level())
                .isPresent();
    }
}

class DistanceCondition implements Condition {
    private final ScalingValue x;
    private final ScalingValue y;
    private final ScalingValue z;
    private final ScalingValue maxDistance;

    public DistanceCondition(ScalingValue x, ScalingValue y, ScalingValue z, ScalingValue maxDistance) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.maxDistance = maxDistance;
    }

    @Override
    public boolean evaluate(Player player,
            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        double tx = x.evaluate(player, target);
        double ty = y.evaluate(player, target);
        double tz = z.evaluate(player, target);
        double dx = player.getX() - tx;
        double dy = player.getY() - ty;
        double dz = player.getZ() - tz;
        double maxD = maxDistance.evaluate(player, target);
        return (dx * dx + dy * dy + dz * dz) <= (maxD * maxD);
    }
}

class CustomizationEqualsCondition implements Condition {
    private final String customizationId;
    private final String[] allowedValues;

    public CustomizationEqualsCondition(String customizationId, String[] allowedValues) {
        this.customizationId = customizationId;
        this.allowedValues = allowedValues;
    }

    @Override
    public boolean evaluate(Player player,
            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        return DataUtils.getVariables(player).map(vars -> {
            String current = vars.getCustomization(customizationId);
            if (current == null)
                return false;
            for (String val : allowedValues) {
                if (current.equalsIgnoreCase(val))
                    return true;
            }
            return false;
        }).orElse(false);
    }
}

class DimensionCondition implements Condition {
    private final String dimension;

    public DimensionCondition(String dimension) {
        this.dimension = dimension;
    }

    @Override
    public boolean evaluate(Player player,
            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        return player.level().dimension().location().toString().equals(dimension);
    }
}

class SpiritCondition implements Condition {
    private final boolean expected;

    public SpiritCondition(boolean expected) {
        this.expected = expected;
    }

    @Override
    public boolean evaluate(Player player,
            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        return DataUtils.getVariables(player)
                .map(vars -> vars.isInSpiritRealm() == expected)
                .orElse(false);
    }
}

class HasEntitiesCondition implements Condition {
    private final mc.sayda.creraces.engine.ScalingValue radius;
    private final TargetFilter targets;

    public HasEntitiesCondition(mc.sayda.creraces.engine.ScalingValue radius, TargetFilter targets) {
        this.radius = radius;
        this.targets = targets;
    }

    @Override
    public boolean evaluate(Player player, @javax.annotation.Nullable LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        double r = radius.evaluate(player, target);
        int maxAoeRadius = 100;
        if (maxAoeRadius > 0)
            r = Math.min(r, maxAoeRadius);

        final AABB area = Objects.requireNonNull(player.getBoundingBox().inflate(r));
        return !player.level().getEntitiesOfClass(LivingEntity.class, area, e -> {
            return e != player && targets.isValid(e, player);
        }).isEmpty();
    }
}

class IsPositionCondition implements Condition {
    private final ScalingValue x;
    private final ScalingValue y;
    private final ScalingValue z;

    public IsPositionCondition(ScalingValue x, ScalingValue y, ScalingValue z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public boolean evaluate(Player player, @javax.annotation.Nullable LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        double px = x.evaluate(player, target);
        double py = y.evaluate(player, target);
        double pz = z.evaluate(player, target);

        net.minecraft.core.BlockPos pos = (interactionPos != null) ? interactionPos : player.blockPosition();

        // Check if inside the block boundaries (floor comparison)
        return pos.getX() == (int) Math.floor(px) &&
                pos.getY() == (int) Math.floor(py) &&
                pos.getZ() == (int) Math.floor(pz);
    }
}

class IsBlockCondition implements Condition {
    private final String blockDefinition;
    private final ScalingValue offsetX;
    private final ScalingValue offsetY;
    private final ScalingValue offsetZ;
    private final boolean useInteractionPos;
    private final boolean absolute;

    public IsBlockCondition(String blockDefinition, ScalingValue offsetX, ScalingValue offsetY, ScalingValue offsetZ,
            boolean useInteractionPos, boolean absolute) {
        this.blockDefinition = blockDefinition;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.useInteractionPos = useInteractionPos;
        this.absolute = absolute;
    }

    @Override
    public boolean evaluate(Player player, @javax.annotation.Nullable LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable BlockPos interactionPos) {
        int ox = (int) offsetX.evaluate(player, target);
        int oy = (int) offsetY.evaluate(player, target);
        int oz = (int) offsetZ.evaluate(player, target);

        BlockPos finalPos;
        if (absolute) {
            finalPos = new BlockPos(ox, oy, oz);
        } else {
            BlockPos base = (useInteractionPos && interactionPos != null) ? interactionPos : player.blockPosition();
            finalPos = base.offset(ox, oy, oz);
        }

        BlockState state = player.level().getBlockState(finalPos);

        if (blockDefinition.startsWith("#")) {
            ResourceLocation tagLoc = new ResourceLocation(blockDefinition.substring(1));
            return state.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK, tagLoc));
        } else {
            return net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString()
                    .equals(blockDefinition);
        }
    }
}
