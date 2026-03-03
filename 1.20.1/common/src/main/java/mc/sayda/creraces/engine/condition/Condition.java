package mc.sayda.creraces.engine.condition;

import com.google.gson.JsonObject;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import mc.sayda.creraces.util.ItemUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

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
                    ScalingValue value = ScalingValue.fromJson(json, "value", 0.0);
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
                    ScalingValue value = ScalingValue.fromJson(json, "value", 0.0);
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
                    ScalingValue threshold = ScalingValue.fromJson(json, "threshold", 0.01);
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
                    ScalingValue amp = ScalingValue.fromJson(json, "amplifier", 0.0);
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
                    ScalingValue max = ScalingValue.fromJson(json, "max", 10.0);
                    ScalingValue x = ScalingValue.fromJson(json, "x", 0.0);
                    ScalingValue y = ScalingValue.fromJson(json, "y", 0.0);
                    ScalingValue z = ScalingValue.fromJson(json, "z", 0.0);
                    yield new DistanceCondition(x, y, z, max);
                }
                case "attack_charged" -> {
                    ScalingValue threshold = ScalingValue.fromJson(json, "threshold", 1.0);
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

record StateEqualsCondition(String state, ScalingValue value) implements Condition {
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
            double val = value.evaluate(player, target);
            return Math.abs(current - val) < 0.001;
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

record AttackChargedCondition(ScalingValue threshold) implements Condition {
    @Override
    public boolean evaluate(Player player,
            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        return player.getAttackStrengthScale(0.5f) >= threshold.evaluate(player, target);
    }
}

record HasEffectCondition(ResourceLocation effectId, ScalingValue minAmplifier, boolean useTarget)
        implements Condition {
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

record TimeCondition(ScalingValue min, ScalingValue max) implements Condition {
    @Override
    public boolean evaluate(Player player,
            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        long time = player.level().getDayTime() % 24000;
        return time >= (long) min.evaluate(player, target) && time <= (long) max.evaluate(player, target);
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

record AltitudeCondition(ScalingValue min, ScalingValue max) implements Condition {
    @Override
    public boolean evaluate(Player player,
            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        double y = player.getY();
        return y >= min.evaluate(player, target) && y <= max.evaluate(player, target);
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

record IsMovingCondition(boolean expected, ScalingValue threshold) implements Condition {
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

record ResourceLevelCondition(String resource, String operator, ScalingValue value) implements Condition {
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

record ExposedToRainCondition(boolean expected) implements Condition {
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
        ScalingValue z, ScalingValue maxDistance) implements Condition {
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
        double maxD = maxDistance.evaluate(player, target);
        return (dx * dx + dy * dy + dz * dz) <= (maxD * maxD);
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

record SpiritCondition(boolean expected) implements Condition {
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
