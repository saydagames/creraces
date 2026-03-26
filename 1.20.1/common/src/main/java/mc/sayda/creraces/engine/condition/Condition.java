package mc.sayda.creraces.engine.condition;


import java.util.Objects;
import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
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
import net.minecraft.world.item.ItemStack;

/**
 * Universal condition check for actions and traits.
 */
public interface Condition {
    boolean evaluate(Player player, @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos);

    @SuppressWarnings("null")
    static Condition fromJson(JsonObject json) {
        if (!json.has("type")) {
            CreRaces.LOGGER.error("Condition missing 'type' field - skipping. JSON: {}", json);
            return (player, target, slot, interactionPos) -> false;
        }
        String typeStr = json.get("type").getAsString();
        @SuppressWarnings("null")
        ResourceLocation typeLoc = ResourceLocation.tryParse(typeStr);
        if (typeLoc == null) {
            CreRaces.LOGGER.error("Malformed condition type '{}' - skipping.", typeStr);
            return (player, target, slot, interactionPos) -> false;
        }
        String type = typeLoc.getPath();

        try {
            return switch (type) {
                case "state", "state_equals" -> {
                    String stateKey = json.has("state") ? GsonHelper.getAsString(json, "state")
                            : GsonHelper.getAsString(json, "ability");
                    ScalingValue value = ScalingValue.fromJson(json, "value", 1.0);
                    String operator = GsonHelper.getAsString(json, "operator", "==");
                    yield new StateCondition(stateKey, value, operator);
                }
                case "morphed" -> {
                    boolean expected = GsonHelper.getAsBoolean(json, "value", true);
                    yield new MorphedCondition(expected);
                }
                case "flying" -> {
                    boolean expected = GsonHelper.getAsBoolean(json, "value", true);
                    yield new FlyingCondition(expected);
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
                    @javax.annotation.Nullable
                    String item = GsonHelper.getNullableString(json, "item", null);
                    @javax.annotation.Nullable
                    String tag = GsonHelper.getNullableString(json, "tag", null);
                    String slot = GsonHelper.getAsString(json, "slot", "0");
                    if (tag != null && !tag.startsWith("#"))
                        tag = "#" + tag;
                    yield new WearingArmorCondition(item != null ? item : tag, slot);
                }
                case "holding_item" -> {
                    @javax.annotation.Nullable
                    String item = GsonHelper.getNullableString(json, "item", null);
                    @javax.annotation.Nullable
                    String tag = GsonHelper.getNullableString(json, "tag", null);
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
                    @javax.annotation.Nullable
                    String biomeId = GsonHelper.getNullableString(json, "biome", null);
                    @javax.annotation.Nullable
                    String tag = GsonHelper.getNullableString(json, "tag", null);
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
                    boolean includeRain = GsonHelper.getAsBoolean(json, "include_rain", false);
                    yield new InWaterCondition(expected, includeRain);
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
                case "can_place_block" -> {
                    ScalingValue ox = ScalingValue.fromJson(json, "offset_x", 0.0);
                    ScalingValue oy = ScalingValue.fromJson(json, "offset_y", 0.0);
                    ScalingValue oz = ScalingValue.fromJson(json, "offset_z", 0.0);
                    boolean useTarget = GsonHelper.getAsBoolean(json, "use_target", false);
                    boolean useTargetBlock = GsonHelper.getAsBoolean(json, "use_target_block", false);
                    boolean absolute = GsonHelper.getAsBoolean(json, "absolute", false);

                    ScalingValue.MathOp math = ScalingValue.MathOp.ROUND;
                    if (json.has("math")) {
                        try {
                            math = ScalingValue.MathOp.valueOf(json.get("math").getAsString().toUpperCase());
                        } catch (Exception e) {
                            CreRaces.LOGGER.warn("Invalid math mode in CanPlaceBlockCondition: {}",
                                    json.get("math").getAsString());
                        }
                    }
                    yield new CanPlaceBlockCondition(ox, oy, oz, useTarget, useTargetBlock, absolute, math);
                }
                case "has_enchantment" -> {
                    String id = GsonHelper.getAsString(json, "enchantment");
                    ScalingValue level = ScalingValue.fromJson(json, "level", 1.0);
                    String slot = GsonHelper.getAsString(json, "slot", "any");
                    String operator = GsonHelper.getAsString(json, "operator", ">=");
                    boolean useTarget = GsonHelper.getAsBoolean(json, "use_target", false);
                    @SuppressWarnings("null")
                    Condition c = new HasEnchantmentCondition(id, level, slot, operator, useTarget);
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
                    boolean useTarget = GsonHelper.getAsBoolean(json, "use_target", false);
                    boolean useTargetBlock = GsonHelper.getAsBoolean(json, "use_target_block", false);
                    boolean absolute = GsonHelper.getAsBoolean(json, "absolute", true);

                    ScalingValue.MathOp math = ScalingValue.MathOp.ROUND;
                    if (json.has("math")) {
                        try {
                            math = ScalingValue.MathOp.valueOf(json.get("math").getAsString().toUpperCase());
                        } catch (Exception e) {
                            CreRaces.LOGGER.warn("Invalid math mode in IsPositionCondition: {}",
                                    json.get("math").getAsString());
                        }
                    }
                    yield new IsPositionCondition(x, y, z, useTarget, useTargetBlock, absolute, math);
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
                    yield new CustomizationEqualsCondition(id, validVals.toArray(new String[0]));
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
                    yield new RaceEqualsCondition(validRaces);
                }
                case "has_customization" -> {
                    String id = GsonHelper.getAsString(json, "id");
                    yield new HasCustomizationCondition(id);
                }
                case "has_entities" -> {
                    mc.sayda.creraces.engine.ScalingValue radius = mc.sayda.creraces.engine.ScalingValue.fromJson(json,
                            "radius", 5.0);
                    TargetFilter targets = TargetFilter.fromJson(json, "targets", java.util.Set.of("enemies"));
                    yield new HasEntitiesCondition(radius, targets);
                }
                case "is_block" -> {
                    String blockStr = GsonHelper.getAsString(json, "block", "minecraft:air");
                    ScalingValue ox = ScalingValue.fromJson(json, "offset_x", 0.0);
                    ScalingValue oy = ScalingValue.fromJson(json, "offset_y", 0.0);
                    ScalingValue oz = ScalingValue.fromJson(json, "offset_z", 0.0);
                    boolean useInteractionPos = GsonHelper.getAsBoolean(json, "use_interaction_pos", true);
                    boolean absolute = GsonHelper.getAsBoolean(json, "absolute", false);

                    ScalingValue.MathOp math = ScalingValue.MathOp.ROUND;
                    if (json.has("math")) {
                        try {
                            math = ScalingValue.MathOp.valueOf(json.get("math").getAsString().toUpperCase());
                        } catch (Exception e) {
                            mc.sayda.creraces.CreRaces.LOGGER.warn("Invalid math mode in IsBlockCondition: {}",
                                    json.get("math").getAsString());
                        }
                    }

                    yield new IsBlockCondition(blockStr, ox, oy, oz, useInteractionPos, absolute, math);
                }
                case "cooldown" -> {
                    String id = json.has("state") ? GsonHelper.getAsString(json, "state")
                            : (json.has("id") ? GsonHelper.getAsString(json, "id")
                                    : GsonHelper.getAsString(json, "ability"));
                    if (!id.contains(":")) {
                        id = "creraces:" + id;
                    }
                    String operator = GsonHelper.getAsString(json, "operator", "<=");
                    ScalingValue value = ScalingValue.fromJson(json, "value", 0.0);
                    yield new CooldownCondition(id, operator, value);
                }
                default ->
                    throw new IllegalArgumentException("Unknown condition type '" + typeStr + "' - check your JSON");
            };
        } catch (Exception e) {
            mc.sayda.creraces.CreRaces.LOGGER.error(
                    "Failed to parse condition '{}': {} - condition will always return false. JSON: {}",
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

class StateCondition implements Condition {
    private final String state;
    private final ScalingValue value;
    private final String operator;

    public StateCondition(String state, ScalingValue value, String operator) {
        this.state = state;
        this.value = value;
        this.operator = operator;
    }

    @Override
    public boolean evaluate(Player player,
            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        return DataUtils.getVariables(player).map(vars -> {
            ResourceLocation targetId = null;
            if ("self".equalsIgnoreCase(state) && slot != null) {
                targetId = vars.getAbilityInSlot(slot);
                if (targetId == null) {
                    mc.sayda.creraces.CreRaces.LOGGER.warn(
                            "state:self used in condition but no ability found in slot '{}' for player '{}'",
                            slot, player.getName().getString());
                }
            } else if ("self".equalsIgnoreCase(state) && slot == null) {
                mc.sayda.creraces.CreRaces.LOGGER.warn(
                        "state:self used in condition but no ability slot context is available (used outside of an ability?)");
            } else if (state != null && !state.isEmpty()) {
                try {
                    String sub = state.startsWith("state:") ? state.substring(6) : state;
                    if (!sub.contains(":")) {
                        sub = "creraces:" + sub;
                    }
                    targetId = ResourceLocation.tryParse(sub);
                } catch (Exception e) {
                    return false;
                }
            }

            if (targetId == null)
                return false;

            double current = vars.getPersistentState(targetId);
            double val = value.evaluate(player, target);

            return switch (operator) {
                case "!=" -> Math.abs(current - val) >= 0.001;
                case ">" -> current > val;
                case ">=" -> current >= val;
                case "<" -> current < val;
                case "<=" -> current <= val;
                default -> Math.abs(current - val) < 0.001;
            };
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
                @SuppressWarnings("null")
                boolean isTag = holder.is(tagKey);
                return isTag;
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
    private final boolean includeRain;

    public InWaterCondition(boolean expected, boolean includeRain) {
        this.expected = expected;
        this.includeRain = includeRain;
    }

    @Override
    public boolean evaluate(Player player,
            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        @SuppressWarnings("null")
        boolean inBubble = player.level().getBlockState(player.blockPosition())
                .is(net.minecraft.world.level.block.Blocks.BUBBLE_COLUMN);
        boolean inWater = player.isInWater() || inBubble;
        if (includeRain && !inWater) {
            inWater = mc.sayda.creraces.util.WorldUtils.isExposedToRain(player);
        }
        return inWater == expected;
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
            double current = 0.0;
            String res = resource.toLowerCase();
            if (res.startsWith("custom:") || res.startsWith("state:")) {
                String key = resource;
                boolean isCustom = res.startsWith("custom:");
                if (isCustom) {
                    key = key.substring(7);
                } else if (res.startsWith("state:")) {
                    key = key.substring(6);
                }

                if (!key.contains(":") && !isCustom) {
                    key = "creraces:" + key;
                }

                if (isCustom) {
                    String valStr = vars.getCustomization(key);
                    try {
                        current = (valStr != null && !valStr.isEmpty()) ? Double.parseDouble(valStr) : 0.0;
                    } catch (NumberFormatException e) {
                        current = 0.0;
                    }
                } else {
                    ResourceLocation loc = ResourceLocation.tryParse(key);
                    if (loc != null) {
                        current = vars.getPersistentState(loc);
                    }
                }
            } else {
                current = switch (res) {
                    case "mana" -> vars.getMana();
                    case "energy" -> vars.getEnergy();
                    case "grit" -> vars.getGrit();
                    case "rage" -> vars.getRage();
                    case "karma" -> vars.getKarma();
                    case "soul" -> vars.getSoul();
                    case "food" -> (double) ((mc.sayda.creraces.util.IFoodDataAccessor) player.getFoodData())
                            .creraces$getFoodLevel();
                    case "saturation" -> (double) ((mc.sayda.creraces.util.IFoodDataAccessor) player.getFoodData())
                            .creraces$getSaturation();
                    case "health" -> (double) player.getHealth();
                    case "air" -> (double) player.getAirSupply();
                    case "coins" -> vars.getCoins();
                    case "ap" -> vars.getAp();
                    case "ad" -> vars.getAd();
                    case "ah" -> vars.getAh();
                    case "cr" -> vars.getCr();
                    default -> 0.0;
                };
            }

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
        return mc.sayda.creraces.util.WorldUtils.isExposedToRain(player) == expected;
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
        @SuppressWarnings("null")
        boolean present = player.level().getRecipeManager()
                .getRecipeFor(net.minecraft.world.item.crafting.RecipeType.SMELTING,
                        new net.minecraft.world.SimpleContainer(stack), player.level())
                .isPresent();
        return present;
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
    private final java.util.List<String> allowedValues;

    public CustomizationEqualsCondition(String customizationId, String[] allowedValues) {
        this.customizationId = customizationId;
        this.allowedValues = java.util.Arrays.asList(allowedValues);
    }

    @Override
    public boolean evaluate(Player player,
            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slotCtx,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        return DataUtils.getVariables(player).map(vars -> {
            ResourceLocation activeRaceLoc = vars.getRace();
            if (activeRaceLoc == null)
                return false;

            mc.sayda.creraces.race.Race rd = mc.sayda.creraces.race.RaceRegistry.get(activeRaceLoc);
            if (rd == null)
                return false;

            String val;
            if (customizationId.equalsIgnoreCase("race")) {
                val = activeRaceLoc.toString();
            } else if (customizationId.equalsIgnoreCase("gstate")) {
                val = String.valueOf(vars.getGState());
            } else {
                mc.sayda.creraces.race.RaceCustomization cData = rd.customization().stream()
                        .filter(c -> c.id().equals(customizationId))
                        .findFirst()
                        .orElse(null);
                if (cData == null)
                    return false;

                val = vars.getCustomization(customizationId.toLowerCase());
                if (val == null || val.isEmpty())
                    val = cData.defaultValue();
            }

            return allowedValues.contains(val);
        }).orElse(false);
    }
}

class RaceEqualsCondition implements Condition {
    private final java.util.List<String> validRaces;

    public RaceEqualsCondition(java.util.List<String> validRaces) {
        this.validRaces = validRaces;
    }

    @Override
    public boolean evaluate(Player player, @javax.annotation.Nullable LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slotCtx,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
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
    }
}

class HasCustomizationCondition implements Condition {
    private final String customizationId;

    public HasCustomizationCondition(String customizationId) {
        this.customizationId = customizationId;
    }

    @Override
    public boolean evaluate(Player player, @javax.annotation.Nullable LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slotCtx,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        return DataUtils.getVariables(player).map(vars -> {
            ResourceLocation activeRaceLoc = vars.getRace();
            if (activeRaceLoc == null)
                return false;

            if (customizationId.equalsIgnoreCase("race") || customizationId.equalsIgnoreCase("gstate"))
                return true;

            String val = vars.getCustomization(customizationId.toLowerCase());
            return val != null && !val.isEmpty() && !val.equals("0.0") && !val.equals("0");
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
        double r = radius.evaluate(player, target, slot);
        int maxAoeRadius = mc.sayda.creraces.config.CreRacesConfig.AOE_MAX_RADIUS.get();
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
    private final boolean useTarget;
    private final boolean useTargetBlock;
    private final boolean absolute;
    private final ScalingValue.MathOp coordinateMath;

    public IsPositionCondition(ScalingValue x, ScalingValue y, ScalingValue z, boolean useTarget,
            boolean useTargetBlock, boolean absolute, ScalingValue.MathOp math) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.useTarget = useTarget;
        this.useTargetBlock = useTargetBlock;
        this.absolute = absolute;
        this.coordinateMath = math != null ? math : ScalingValue.MathOp.ROUND;
    }

    @Override
    public boolean evaluate(Player player, @javax.annotation.Nullable LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {

        BlockPos basePos;
        if (absolute) {
            basePos = BlockPos.ZERO;
        } else if (useTarget && target != null) {
            basePos = target.blockPosition();
        } else if (useTargetBlock && interactionPos != null) {
            basePos = interactionPos;
        } else {
            double tx = player.getX();
            double ty = player.getY();
            double tz = player.getZ();

            int bx = (int) Math.floor(tx);
            int by = (int) Math.floor(ty);
            int bz = (int) Math.floor(tz);

            if (coordinateMath == ScalingValue.MathOp.ROUND) {
                bx = (int) Math.round(tx);
                by = (int) Math.round(ty);
                bz = (int) Math.round(tz);
            } else if (coordinateMath == ScalingValue.MathOp.CEIL) {
                bx = (int) Math.ceil(tx);
                by = (int) Math.ceil(ty);
                bz = (int) Math.ceil(tz);
            }
            basePos = new BlockPos(bx, by, bz);
        }

        double px = x.evaluate(player, target, slot);
        double py = y.evaluate(player, target, slot);
        double pz = z.evaluate(player, target, slot);

        BlockPos targetPos = basePos.offset((int) px, (int) py, (int) pz);

        // This checks if the player/target/interaction position is at the specified coord
        BlockPos currentPos = (interactionPos != null) ? interactionPos : player.blockPosition();
        return currentPos.equals(targetPos);
    }
}

class IsBlockCondition implements Condition {
    private final String blockDefinition;
    private final ScalingValue offsetX;
    private final ScalingValue offsetY;
    private final ScalingValue offsetZ;
    private final boolean useInteractionPos;
    private final boolean absolute;
    private final ScalingValue.MathOp coordinateMath;

    public IsBlockCondition(String blockDefinition, ScalingValue offsetX, ScalingValue offsetY, ScalingValue offsetZ,
            boolean useInteractionPos, boolean absolute, ScalingValue.MathOp math) {
        this.blockDefinition = blockDefinition;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.useInteractionPos = useInteractionPos;
        this.absolute = absolute;
        this.coordinateMath = math != null ? math : ScalingValue.MathOp.ROUND;
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
            BlockPos base;
            if (useInteractionPos && interactionPos != null) {
                base = interactionPos;
            } else {
                double tx = player.getX();
                double ty = player.getY();
                double tz = player.getZ();

                int bx = (int) Math.floor(tx);
                int by = (int) Math.floor(ty);
                int bz = (int) Math.floor(tz);

                if (coordinateMath == ScalingValue.MathOp.ROUND) {
                    bx = (int) Math.round(tx);
                    by = (int) Math.round(ty);
                    bz = (int) Math.round(tz);
                } else if (coordinateMath == ScalingValue.MathOp.CEIL) {
                    bx = (int) Math.ceil(tx);
                    by = (int) Math.ceil(ty);
                    bz = (int) Math.ceil(tz);
                }
                base = new BlockPos(bx, by, bz);
            }
            finalPos = base.offset(ox, oy, oz);
        }

        BlockState state = player.level().getBlockState(finalPos);

        if (blockDefinition.startsWith("#")) {
            @SuppressWarnings("null")
            ResourceLocation tagLoc = new ResourceLocation(blockDefinition.substring(1));
            @SuppressWarnings("null")
            boolean isTag = state.is(net.minecraft.tags.TagKey
                    .create(java.util.Objects.requireNonNull(net.minecraft.core.registries.Registries.BLOCK), tagLoc));
            return isTag;
        } else {
            @SuppressWarnings("null")
            ResourceLocation blockKey = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock());
            return blockKey != null && blockKey.toString().equals(blockDefinition);
        }
    }
}

class CooldownCondition implements Condition {
    private final String id;
    private final String operator;
    private final ScalingValue value;

    public CooldownCondition(String id, String operator, ScalingValue value) {
        this.id = id;
        this.operator = operator;
        this.value = value;
    }

    @Override
    public boolean evaluate(Player player, @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        return DataUtils.getVariables(player).map(vars -> {
            @SuppressWarnings("null")
            ResourceLocation fullId = ResourceLocation.tryParse(id);
            if (fullId == null)
                return false;

            double current = vars.getCooldown(fullId);
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

class HasEnchantmentCondition implements Condition {
    private final String enchantmentId;
    private final ScalingValue level;
    private final String slot;
    private final String operator;
    private final boolean useTarget;

    public HasEnchantmentCondition(String enchantmentId, ScalingValue level, String slot, String operator,
            boolean useTarget) {
        this.enchantmentId = enchantmentId;
        this.level = level;
        this.slot = slot;
        this.operator = operator;
        this.useTarget = useTarget;
    }

    @Override
    public boolean evaluate(Player player, @javax.annotation.Nullable LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot abilitySlot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {

        LivingEntity actor = (useTarget && target != null) ? target : player;
        @SuppressWarnings("null")
        ResourceLocation id = ResourceLocation.tryParse(enchantmentId);
        if (id == null)
            return false;
        @SuppressWarnings("null")
        net.minecraft.world.item.enchantment.Enchantment enchantment = net.minecraft.core.registries.BuiltInRegistries.ENCHANTMENT
                .get(id);

        if (enchantment == null)
            return false;

        double targetLevel = level.evaluate(player, target, abilitySlot);

        if (slot.equalsIgnoreCase("any")) {
            for (ItemStack stack : actor.getAllSlots()) {
                if (checkStack(stack, enchantment, targetLevel))
                    return true;
            }
            return false;
        }

        @SuppressWarnings("null")
        ItemStack stack = getItemInSlot(actor, slot);
        @SuppressWarnings("null")
        boolean result = checkStack(stack, enchantment, targetLevel);
        return result;
    }

    private boolean checkStack(ItemStack stack, net.minecraft.world.item.enchantment.Enchantment enchantment,
            double targetLevel) {
        if (stack.isEmpty())
            return false;
        int currentLevel = net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(enchantment,
                stack);

        return switch (operator) {
            case ">=" -> currentLevel >= targetLevel;
            case "<=" -> currentLevel <= targetLevel;
            case ">" -> currentLevel > targetLevel;
            case "<" -> currentLevel < targetLevel;
            case "==" -> Math.abs(currentLevel - targetLevel) < 0.001;
            case "!=" -> Math.abs(currentLevel - targetLevel) >= 0.001;
            default -> false;
        };
    }

    private ItemStack getItemInSlot(LivingEntity entity, String slot) {
        if (slot.equalsIgnoreCase("mainhand"))
            return entity.getMainHandItem();
        if (slot.equalsIgnoreCase("offhand"))
            return entity.getOffhandItem();
        if (slot.equalsIgnoreCase("head"))
            return entity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD);
        if (slot.equalsIgnoreCase("chest"))
            return entity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
        if (slot.equalsIgnoreCase("legs"))
            return entity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS);
        if (slot.equalsIgnoreCase("feet"))
            return entity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET);

        if (entity instanceof Player player) {
            try {
                int index = Integer.parseInt(slot);
                if (index >= 0 && index < player.getInventory().getContainerSize()) {
                    return player.getInventory().getItem(index);
                }
            } catch (NumberFormatException ignored) {
            }
        }

        return ItemStack.EMPTY;
    }
}

class CanPlaceBlockCondition implements Condition {
    private final ScalingValue offsetX;
    private final ScalingValue offsetY;
    private final ScalingValue offsetZ;
    private final boolean useTarget;
    private final boolean useTargetBlock;
    private final boolean absolute;
    private final ScalingValue.MathOp coordinateMath;

    public CanPlaceBlockCondition(ScalingValue ox, ScalingValue oy, ScalingValue oz, boolean useTarget,
            boolean useTargetBlock, boolean absolute, ScalingValue.MathOp math) {
        this.offsetX = ox;
        this.offsetY = oy;
        this.offsetZ = oz;
        this.useTarget = useTarget;
        this.useTargetBlock = useTargetBlock;
        this.absolute = absolute;
        this.coordinateMath = math != null ? math : ScalingValue.MathOp.ROUND;
    }

    @SuppressWarnings("null")
    @Override
    public boolean evaluate(Player player, @javax.annotation.Nullable LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable BlockPos interactionPos) {
        if (player == null)
            return false;

        BlockPos basePos;
        if (absolute) {
            basePos = BlockPos.ZERO;
        } else if (useTarget && target != null) {
            basePos = target.blockPosition();
        } else if (useTargetBlock && interactionPos != null) {
            basePos = interactionPos;
        } else {
            double tx = player.getX();
            double ty = player.getY();
            double tz = player.getZ();

            int bx = (int) Math.floor(tx);
            int by = (int) Math.floor(ty);
            int bz = (int) Math.floor(tz);

            if (coordinateMath == ScalingValue.MathOp.ROUND) {
                bx = (int) Math.round(tx);
                by = (int) Math.round(ty);
                bz = (int) Math.round(tz);
            } else if (coordinateMath == ScalingValue.MathOp.CEIL) {
                bx = (int) Math.ceil(tx);
                by = (int) Math.ceil(ty);
                bz = (int) Math.ceil(tz);
            }
            basePos = new BlockPos(bx, by, bz);
        }

        int ox = (int) offsetX.evaluate(player, target, slot);
        int oy = (int) offsetY.evaluate(player, target, slot);
        int oz = (int) offsetZ.evaluate(player, target, slot);
        BlockPos finalPos = basePos.offset(ox, oy, oz);

        boolean isBlockClear = player.level().getBlockState(finalPos).isAir()
                || player.level().getBlockState(finalPos).canBeReplaced();
        if (!isBlockClear)
            return false;

        // Entity check
        return player.level().getEntitiesOfClass(net.minecraft.world.entity.Entity.class, new AABB(finalPos), e -> e != player).isEmpty();
    }
}
