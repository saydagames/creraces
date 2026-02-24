package mc.sayda.creraces.engine.condition;

import com.google.gson.JsonObject;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import mc.sayda.creraces.util.ItemUtils;

/**
 * Universal condition check for actions and traits.
 */
public interface Condition {
    boolean evaluate(Player player, @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos);

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
                    String ability = GsonHelper.getAsString(json, "ability");
                    double value = GsonHelper.getAsDouble(json, "value", 0.0);
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
                case "entity" -> {
                    yield EntityCondition.fromJson(json);
                }
                case "distance" -> {
                    double max = GsonHelper.getAsDouble(json, "max", 10.0);
                    ScalingValue x = ScalingValue.fromJson(json, "x", 0.0);
                    ScalingValue y = ScalingValue.fromJson(json, "y", 0.0);
                    ScalingValue z = ScalingValue.fromJson(json, "z", 0.0);
                    yield new DistanceCondition(x, y, z, max);
                }
                case "attack_charged" -> {
                    double threshold = GsonHelper.getAsDouble(json, "threshold", 1.0);
                    yield (player, target, slot, interactionPos) -> player.getAttackStrengthScale(4.0f) >= threshold;
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

record WearingArmorCondition(@javax.annotation.Nullable String definition)
        implements Condition {
    @Override
    public boolean evaluate(Player player,
            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
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
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        return ItemUtils.matches(player.getMainHandItem(), definition)
                || ItemUtils.matches(player.getOffhandItem(), definition);
    }
}

record HoldingItemCondition(@javax.annotation.Nullable String definition,
        boolean useTarget) implements Condition {
    @Override
    public boolean evaluate(Player player,
            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
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
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        return DataUtils.getVariables(player).map(vars -> {
            ResourceLocation targetId;
            if ("slot".equalsIgnoreCase(state) && slot != null) {
                targetId = vars.getAbilityInSlot(slot);
            } else {
                targetId = new ResourceLocation(state);
            }

            if (targetId == null)
                return false;

            double current = vars.getAbilityState(targetId);
            return Math.abs(current - value) < 0.001;
        }).orElse(false);
    }
}

record MorphedCondition(boolean expected) implements Condition {
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

record FlyingCondition(boolean expected) implements Condition {
    @Override
    public boolean evaluate(Player player,
            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        boolean isFlying = player.getAbilities().flying || player.isFallFlying();
        return isFlying == expected;
    }
}

record SneakingCondition(boolean expected) implements Condition {
    @Override
    public boolean evaluate(Player player,
            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        return player.isShiftKeyDown() == expected;
    }
}

record OnGroundCondition(boolean expected) implements Condition {
    @Override
    public boolean evaluate(Player player,
            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        return player.onGround() == expected;
    }
}

record AttackChargedCondition() implements Condition {
    @Override
    public boolean evaluate(Player player,
            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        return player.getAttackStrengthScale(0.5f) >= 0.9f;
    }
}

record HasEffectCondition(ResourceLocation effectId, int minAmplifier, boolean useTarget) implements Condition {
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

        net.minecraft.world.entity.LivingEntity subject = (useTarget && target != null) ? target : player;
        net.minecraft.world.effect.MobEffectInstance instance = subject.getEffect(effect);
        return instance != null && instance.getAmplifier() >= minAmplifier;
    }
}

record AndCondition(Condition[] conditions) implements Condition {
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

record OrCondition(Condition[] conditions) implements Condition {
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

record NotCondition(Condition condition) implements Condition {
    @Override
    public boolean evaluate(Player player,
            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        return !condition.evaluate(player, target, slot, interactionPos);
    }
}

record BiomeCondition(@javax.annotation.Nullable String biomeId, @javax.annotation.Nullable String tag)
        implements Condition {
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

record TimeCondition(long min, long max) implements Condition {
    @Override
    public boolean evaluate(Player player,
            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        long time = player.level().getDayTime() % 24000;
        return time >= min && time <= max;
    }
}

record InWaterCondition(boolean expected) implements Condition {
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

record InSunlightCondition(boolean expected) implements Condition {
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

record AltitudeCondition(double min, double max) implements Condition {
    @Override
    public boolean evaluate(Player player,
            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        double y = player.getY();
        return y >= min && y <= max;
    }
}

record IsBurningCondition(boolean expected) implements Condition {
    @Override
    public boolean evaluate(Player player,
            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        return player.isOnFire() == expected;
    }
}

record IsMovingCondition(boolean expected, double threshold) implements Condition {
    @Override
    public boolean evaluate(Player player,
            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        net.minecraft.world.phys.Vec3 vel = player.getDeltaMovement();
        boolean isMoving = (vel.x * vel.x + vel.z * vel.z) > (threshold * threshold);
        return isMoving == expected;
    }
}

record ResourceLevelCondition(String resource, String operator, double value) implements Condition {
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
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        return player.level().isRainingAt(player.blockPosition()) == expected;
    }
}

record IsSmeltableCondition() implements Condition {
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

record DistanceCondition(ScalingValue x, ScalingValue y,
        ScalingValue z, double maxDistance) implements Condition {
    @Override
    public boolean evaluate(Player player,
            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        // Only evaluate distance if in the same dimension as the binding point
        // (Overworld)
        // Or if dimension checking is handled elsewhere.
        // For now, let's assume tx,ty,tz are always in the Overworld for the Dryad.
        if (!player.level().dimension().location().toString().equals("minecraft:overworld")) {
            return false;
        }
        double tx = x.evaluate(player, target);
        double ty = y.evaluate(player, target);
        double tz = z.evaluate(player, target);
        double dx = player.getX() - tx;
        double dy = player.getY() - ty;
        double dz = player.getZ() - tz;
        return (dx * dx + dy * dy + dz * dz) <= (maxDistance * maxDistance);
    }
}

record CustomizationEqualsCondition(String customizationId, String[] allowedValues) implements Condition {
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

record DimensionCondition(String dimension) implements Condition {
    @Override
    public boolean evaluate(Player player,
            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        return player.level().dimension().location().toString().equals(dimension);
    }
}

record IsPositionCondition(ScalingValue x, ScalingValue y, ScalingValue z) implements Condition {
    @Override
    public boolean evaluate(Player player,
            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        if (interactionPos == null)
            return false;
        double tx = x.evaluate(player, target);
        double ty = y.evaluate(player, target);
        double tz = z.evaluate(player, target);
        return interactionPos.getX() == (int) tx && interactionPos.getY() == (int) ty
                && interactionPos.getZ() == (int) tz;
    }
}
