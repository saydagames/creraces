package mc.sayda.creraces.engine.condition;

import com.google.gson.JsonObject;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import mc.sayda.creraces.util.ItemUtils;

/**
 * Universal condition check for actions and traits.
 */
public interface Condition {
    boolean evaluate(Player player, @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot);

    static Condition fromJson(JsonObject json) {
        String typeStr = GsonHelper.getAsString(json, "type");
        @SuppressWarnings("null")
        ResourceLocation typeLoc = new ResourceLocation(typeStr);
        String type = typeLoc.getPath();

        try {
            return switch (type) {
                case "state_equals" -> {
                    String state = GsonHelper.getAsString(json, "state");
                    double value = GsonHelper.getAsDouble(json, "value", 0.0);
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
                    @SuppressWarnings("null")
                    ResourceLocation ability = new ResourceLocation(GsonHelper.getAsString(json, "ability"));
                    double value = GsonHelper.getAsDouble(json, "value", 0.0);
                    yield new AbilityStateCondition(ability, value);
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
                    if (tag != null && !tag.startsWith("#"))
                        tag = "#" + tag;
                    yield new WearingArmorCondition(item != null ? item : tag);
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
                    long min = json.has("min") ? json.get("min").getAsLong() : 0;
                    long max = json.has("max") ? json.get("max").getAsLong() : 24000;
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
                    double min = GsonHelper.getAsDouble(json, "min", Double.NEGATIVE_INFINITY);
                    double max = GsonHelper.getAsDouble(json, "max", Double.POSITIVE_INFINITY);
                    yield new AltitudeCondition(min, max);
                }
                case "is_burning" -> {
                    boolean expected = GsonHelper.getAsBoolean(json, "value", true);
                    yield new IsBurningCondition(expected);
                }
                case "is_moving" -> {
                    boolean expected = GsonHelper.getAsBoolean(json, "value", true);
                    double threshold = GsonHelper.getAsDouble(json, "threshold", 0.01);
                    yield new IsMovingCondition(expected, threshold);
                }
                case "resource_level" -> {
                    String resource = GsonHelper.getAsString(json, "resource");
                    String operator = GsonHelper.getAsString(json, "operator", ">=");
                    double value = GsonHelper.getAsDouble(json, "value", 0.0);
                    yield new ResourceLevelCondition(resource, operator, value);
                }
                case "has_effect" -> {
                    String id = GsonHelper.getAsString(json, "effect");
                    int amp = GsonHelper.getAsInt(json, "amplifier", 0);
                    boolean useTarget = GsonHelper.getAsBoolean(json, "use_target", false);
                    yield new HasEffectCondition(new ResourceLocation(id), amp, useTarget);
                }
                case "is_smeltable" -> {
                    yield new IsSmeltableCondition();
                }
                case "exposed_to_rain" -> {
                    boolean expected = GsonHelper.getAsBoolean(json, "value", true);
                    yield new ExposedToRainCondition(expected);
                }
                case "entity_data" -> {
                    yield EntityDataCondition.fromJson(json);
                }
                case "attack_charged" -> {
                    double threshold = GsonHelper.getAsDouble(json, "threshold", 1.0);
                    yield (player, target, slot) -> player.getAttackStrengthScale(4.0f) >= threshold;
                }
                default ->
                    throw new IllegalArgumentException("Unknown condition type '" + typeStr + "' — check your JSON");
            };
        } catch (Exception e) {
            mc.sayda.creraces.CreRaces.LOGGER.error(
                    "Failed to parse condition '{}': {} — condition will always return false. JSON: {}",
                    typeStr, e.getMessage(), json);
            return (player, target, slot) -> false;
        }
    }

    record WearingArmorCondition(@javax.annotation.Nullable String definition)
            implements Condition {
        @Override
        public boolean evaluate(Player player,
                @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
                @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot) {
            if (definition == null)
                return false;
            for (net.minecraft.world.item.ItemStack stack : player.getArmorSlots()) {
                if (ItemUtils.matches(stack, definition))
                    return true;
            }
            return false;
        }
    }

    record ItemInteractionCondition(String definition) implements Condition {
        @Override
        public boolean evaluate(Player player,
                @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
                @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot) {
            return ItemUtils.matches(player.getMainHandItem(), definition)
                    || ItemUtils.matches(player.getOffhandItem(), definition);
        }
    }

    record HoldingItemCondition(@javax.annotation.Nullable String definition,
            boolean useTarget) implements Condition {
        @Override
        public boolean evaluate(Player player,
                @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
                @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot) {
            net.minecraft.world.entity.LivingEntity entity = useTarget ? target : player;
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

    record StateEqualsCondition(String state, double value) implements Condition {
        @Override
        public boolean evaluate(Player player,
                @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
                @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot) {
            return DataUtils.getVariables(player).map(vars -> {
                ResourceLocation targetAbilityId = null;

                if ("slot".equalsIgnoreCase(state) && slot != null) {
                    targetAbilityId = vars.getAbilityInSlot(slot);
                }

                if (targetAbilityId == null)
                    return false;

                double current = vars.getAbilityState(targetAbilityId);
                return Math.abs(current - value) < 0.001;
            }).orElse(false);
        }
    }

    record MorphedCondition(boolean expected) implements Condition {
        @Override
        public boolean evaluate(Player player,
                @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
                @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot) {
            return DataUtils.getVariables(player)
                    .map(vars -> vars.isMorphed() == expected)
                    .orElse(false);
        }
    }

    record FlyingCondition(boolean expected) implements Condition {
        @Override
        public boolean evaluate(Player player,
                @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
                @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot) {
            boolean isFlying = player.getAbilities().flying || player.isFallFlying();
            return isFlying == expected;
        }
    }

    record AbilityStateCondition(ResourceLocation abilityId, double value) implements Condition {
        @Override
        public boolean evaluate(Player player,
                @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
                @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot) {
            return DataUtils.getVariables(player).map(vars -> {
                double current = vars.getAbilityState(abilityId);
                return Math.abs(current - value) < 0.001;
            }).orElse(false);
        }
    }

    record SneakingCondition(boolean expected) implements Condition {
        @Override
        public boolean evaluate(Player player,
                @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
                @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot) {
            return player.isShiftKeyDown() == expected;
        }
    }

    record OnGroundCondition(boolean expected) implements Condition {
        @Override
        public boolean evaluate(Player player,
                @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
                @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot) {
            return player.onGround() == expected;
        }
    }

    record AttackChargedCondition() implements Condition {
        @Override
        public boolean evaluate(Player player,
                @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
                @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot) {
            return player.getAttackStrengthScale(0.5f) >= 0.9f;
        }
    }

    record HasEffectCondition(ResourceLocation effectId, int minAmplifier, boolean useTarget) implements Condition {
        @Override
        public boolean evaluate(Player player,
                @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
                @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot) {
            @SuppressWarnings("null")
            net.minecraft.world.effect.MobEffect effect = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT
                    .get(effectId);
            if (effect == null)
                return false;

            net.minecraft.world.entity.LivingEntity subject = (useTarget && target != null) ? target : player;
            net.minecraft.world.effect.MobEffectInstance instance = subject.getEffect(effect);
            return instance != null && instance.getAmplifier() >= minAmplifier;
        }
    }

    record AndCondition(Condition[] conditions) implements Condition {
        @Override
        public boolean evaluate(Player player,
                @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
                @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot) {
            for (Condition c : conditions) {
                if (!c.evaluate(player, target, slot))
                    return false;
            }
            return true;
        }
    }

    record OrCondition(Condition[] conditions) implements Condition {
        @Override
        public boolean evaluate(Player player,
                @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
                @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot) {
            for (Condition c : conditions) {
                if (c.evaluate(player, target, slot))
                    return true;
            }
            return false;
        }
    }

    record NotCondition(Condition condition) implements Condition {
        @Override
        public boolean evaluate(Player player,
                @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
                @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot) {
            return !condition.evaluate(player, target, slot);
        }
    }

    record BiomeCondition(@javax.annotation.Nullable String biomeId, @javax.annotation.Nullable String tag)
            implements Condition {
        @Override
        public boolean evaluate(Player player,
                @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
                @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot) {
            @SuppressWarnings("null")
            net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> holder = player.level()
                    .getBiome(player.blockPosition());
            if (biomeId != null) {
                return holder.unwrapKey().map(key -> key.location().toString().equals(biomeId)).orElse(false);
            }
            if (tag != null) {
                @SuppressWarnings("null")
                net.minecraft.tags.TagKey<net.minecraft.world.level.biome.Biome> tagKey = net.minecraft.tags.TagKey
                        .create(net.minecraft.core.registries.Registries.BIOME, new ResourceLocation(tag));
                return holder.is(tagKey);
            }
            return false;
        }
    }

    record WeatherCondition(String type) implements Condition {
        @Override
        public boolean evaluate(Player player,
                @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
                @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot) {
            return switch (type.toLowerCase()) {
                case "rain" -> player.level().isRaining();
                case "thunder" -> player.level().isThundering();
                case "clear" -> !player.level().isRaining();
                default -> false;
            };
        }
    }

    record TimeCondition(long min, long max) implements Condition {
        @Override
        public boolean evaluate(Player player,
                @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
                @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot) {
            long time = player.level().getDayTime() % 24000;
            return time >= min && time <= max;
        }
    }

    record InWaterCondition(boolean expected) implements Condition {
        @Override
        public boolean evaluate(Player player,
                @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
                @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot) {
            @SuppressWarnings("null")
            boolean inBubble = player.level().getBlockState(player.blockPosition())
                    .is(net.minecraft.world.level.block.Blocks.BUBBLE_COLUMN);
            return (player.isInWater() || inBubble) == expected;
        }
    }

    record InSunlightCondition(boolean expected) implements Condition {
        @Override
        public boolean evaluate(Player player,
                @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
                @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot) {
            @SuppressWarnings("null")
            boolean inSun = player.level().isDay() &&
                    !player.level().isRaining() &&
                    player.level().canSeeSky(player.blockPosition());
            return inSun == expected;
        }
    }

    record AltitudeCondition(double min, double max) implements Condition {
        @Override
        public boolean evaluate(Player player,
                @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
                @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot) {
            double y = player.getY();
            return y >= min && y <= max;
        }
    }

    record IsBurningCondition(boolean expected) implements Condition {
        @Override
        public boolean evaluate(Player player,
                @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
                @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot) {
            return player.isOnFire() == expected;
        }
    }

    record IsMovingCondition(boolean expected, double threshold) implements Condition {
        @Override
        public boolean evaluate(Player player,
                @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
                @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot) {
            net.minecraft.world.phys.Vec3 vel = player.getDeltaMovement();
            boolean isMoving = (vel.x * vel.x + vel.z * vel.z) > (threshold * threshold);
            return isMoving == expected;
        }
    }

    record ResourceLevelCondition(String resource, String operator, double value) implements Condition {
        @Override
        public boolean evaluate(Player player,
                @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
                @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot) {
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

                return switch (operator) {
                    case ">=" -> current >= value;
                    case "<=" -> current <= value;
                    case ">" -> current > value;
                    case "<" -> current < value;
                    case "==" -> Math.abs(current - value) < 0.001;
                    case "!=" -> Math.abs(current - value) >= 0.001;
                    default -> false;
                };
            }).orElse(false);
        }
    }

    record ExposedToRainCondition(boolean expected) implements Condition {
        @Override
        public boolean evaluate(Player player,
                @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
                @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot) {
            return player.level().isRainingAt(player.blockPosition()) == expected;
        }
    }

    record IsSmeltableCondition() implements Condition {
        @Override
        public boolean evaluate(Player player,
                @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
                @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot) {
            net.minecraft.world.item.ItemStack stack = player.getMainHandItem();
            if (stack.isEmpty())
                return false;
            return player.level().getRecipeManager()
                    .getRecipeFor(net.minecraft.world.item.crafting.RecipeType.SMELTING,
                            new net.minecraft.world.SimpleContainer(stack), player.level())
                    .isPresent();
        }
    }
}
