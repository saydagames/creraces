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
            @javax.annotation.Nullable net.minecraft.core.BlockPos interact_pos);

    static Condition fromJson(JsonObject json) {
        return ConditionRegistry.fromJson(json);
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
            @javax.annotation.Nullable net.minecraft.core.BlockPos interact_pos) {

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
            @javax.annotation.Nullable net.minecraft.core.BlockPos interact_pos) {
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
            @javax.annotation.Nullable net.minecraft.core.BlockPos interact_pos) {
        net.minecraft.world.entity.LivingEntity entity = (useTarget && target != null) ? target : player;
        if (entity == null)
            return false;

        for (net.minecraft.world.InteractionHand hand : net.minecraft.world.InteractionHand.values()) {
            @SuppressWarnings("null")
            net.minecraft.world.item.ItemStack stack = entity.getItemInHand(hand);
            if (definition == null ? !stack.isEmpty() : ItemUtils.matches(stack, definition))
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
            @javax.annotation.Nullable net.minecraft.core.BlockPos interact_pos) {
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
                case "==" -> Math.abs(current - val) < 0.001;
                case "!=" -> Math.abs(current - val) >= 0.001;
                case ">" -> current > val;
                case ">=" -> current >= val;
                case "<" -> current < val;
                case "<=" -> current <= val;
                default -> {
                    mc.sayda.creraces.CreRaces.LOGGER.warn("StateCondition: unknown operator '{}', returning false", operator);
                    yield false;
                }
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
            @javax.annotation.Nullable net.minecraft.core.BlockPos interact_pos) {
        return DataUtils.getVariables(player)
                .map(vars -> vars.isMorphed() == expected)
                .orElse(false);
    }
}

class FlyingCondition implements Condition {
    private final boolean expected;
    private final boolean useTarget;

    public FlyingCondition(boolean expected, boolean useTarget) {
        this.expected = expected;
        this.useTarget = useTarget;
    }

    @Override
    public boolean evaluate(Player player,
            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interact_pos) {
        net.minecraft.world.entity.LivingEntity entity = (useTarget && target != null) ? target : player;
        boolean isFlying = entity instanceof Player p
                ? p.getAbilities().flying || p.isFallFlying()
                : entity.isFallFlying();
        return isFlying == expected;
    }
}

class SneakingCondition implements Condition {
    private final boolean expected;
    private final boolean useTarget;

    public SneakingCondition(boolean expected, boolean useTarget) {
        this.expected = expected;
        this.useTarget = useTarget;
    }

    @Override
    public boolean evaluate(Player player,
            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interact_pos) {
        net.minecraft.world.entity.LivingEntity entity = (useTarget && target != null) ? target : player;
        return entity.isCrouching() == expected;
    }
}

class OnGroundCondition implements Condition {
    private final boolean expected;
    private final boolean useTarget;

    public OnGroundCondition(boolean expected, boolean useTarget) {
        this.expected = expected;
        this.useTarget = useTarget;
    }

    @Override
    public boolean evaluate(Player player,
            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interact_pos) {
        net.minecraft.world.entity.LivingEntity entity = (useTarget && target != null) ? target : player;
        return entity.onGround() == expected;
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
            @javax.annotation.Nullable net.minecraft.core.BlockPos interact_pos) {
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
            @javax.annotation.Nullable net.minecraft.core.BlockPos interact_pos) {
        @SuppressWarnings("null")
        net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT
                .getHolder(effectId).map(h -> (net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>) h).orElse(null);
        if (effect == null)
            return false;

        net.minecraft.world.entity.LivingEntity subject = mc.sayda.creraces.engine.TargetFilter.resolveSmartTarget(player, target, useTarget);
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
            @javax.annotation.Nullable net.minecraft.core.BlockPos interact_pos) {
        for (Condition c : conditions) {
            if (!c.evaluate(player, target, slot, interact_pos))
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
            @javax.annotation.Nullable net.minecraft.core.BlockPos interact_pos) {
        for (Condition c : conditions) {
            if (c.evaluate(player, target, slot, interact_pos))
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
            @javax.annotation.Nullable net.minecraft.core.BlockPos interact_pos) {
        return !condition.evaluate(player, target, slot, interact_pos);
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
            @javax.annotation.Nullable net.minecraft.core.BlockPos interact_pos) {
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
                        .create(net.minecraft.core.registries.Registries.BIOME, ResourceLocation.parse(tag));
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
            @javax.annotation.Nullable net.minecraft.core.BlockPos interact_pos) {
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
            @javax.annotation.Nullable net.minecraft.core.BlockPos interact_pos) {
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
            @javax.annotation.Nullable net.minecraft.core.BlockPos interact_pos) {
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
            @javax.annotation.Nullable net.minecraft.core.BlockPos interact_pos) {
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
            @javax.annotation.Nullable net.minecraft.core.BlockPos interact_pos) {
        double y = player.getY();
        return y >= min.evaluate(player, target) && y <= max.evaluate(player, target);
    }
}

class IsBurningCondition implements Condition {
    private final boolean expected;
    private final boolean useTarget;

    public IsBurningCondition(boolean expected, boolean useTarget) {
        this.expected = expected;
        this.useTarget = useTarget;
    }

    @Override
    public boolean evaluate(Player player,
            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interact_pos) {
        net.minecraft.world.entity.LivingEntity entity = (useTarget && target != null) ? target : player;
        return entity.isOnFire() == expected;
    }
}

class IsMovingCondition implements Condition {
    private final boolean expected;
    private final ScalingValue threshold;
    private final boolean useTarget;

    public IsMovingCondition(boolean expected, ScalingValue threshold, boolean useTarget) {
        this.expected = expected;
        this.threshold = threshold;
        this.useTarget = useTarget;
    }

    @Override
    public boolean evaluate(Player player,
            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interact_pos) {
        net.minecraft.world.entity.LivingEntity entity = (useTarget && target != null) ? target : player;
        net.minecraft.world.phys.Vec3 vel = entity.getDeltaMovement();
        double t = threshold.evaluate(player, target);
        boolean isMoving = (vel.x * vel.x + vel.y * vel.y + vel.z * vel.z) > (t * t);
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
            @javax.annotation.Nullable net.minecraft.core.BlockPos interact_pos) {
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
                    case "gstate" -> (double) vars.getGState();
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
            @javax.annotation.Nullable net.minecraft.core.BlockPos interact_pos) {
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
            @javax.annotation.Nullable net.minecraft.core.BlockPos interact_pos) {
        net.minecraft.world.item.ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty())
            return false;
        @SuppressWarnings("null")
        boolean present = player.level().getRecipeManager()
                .getRecipeFor(net.minecraft.world.item.crafting.RecipeType.SMELTING,
                        new net.minecraft.world.item.crafting.SingleRecipeInput(stack), player.level())
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
            @javax.annotation.Nullable net.minecraft.core.BlockPos interact_pos) {
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
            @javax.annotation.Nullable net.minecraft.core.BlockPos interact_pos) {
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
            @javax.annotation.Nullable net.minecraft.core.BlockPos interact_pos) {
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
            @javax.annotation.Nullable net.minecraft.core.BlockPos interact_pos) {
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
            @javax.annotation.Nullable net.minecraft.core.BlockPos interact_pos) {
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
            @javax.annotation.Nullable net.minecraft.core.BlockPos interact_pos) {
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
            @javax.annotation.Nullable net.minecraft.core.BlockPos interact_pos) {
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

/**
 * Shared basePos resolution for conditions that support absolute / target-position /
 * interact_pos / player-position (floor, round, or ceil) as their coordinate origin.
 */
final class BasePosResolver {
    private BasePosResolver() {
    }

    static BlockPos resolve(Player player, @javax.annotation.Nullable LivingEntity target,
            @javax.annotation.Nullable BlockPos interact_pos, boolean useTarget, boolean useTargetBlock,
            boolean absolute, ScalingValue.MathOp coordinateMath) {
        if (absolute) {
            return BlockPos.ZERO;
        } else if (useTarget && target != null) {
            return target.blockPosition();
        } else if (useTargetBlock && interact_pos != null) {
            return interact_pos;
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
            return new BlockPos(bx, by, bz);
        }
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
            @javax.annotation.Nullable net.minecraft.core.BlockPos interact_pos) {

        BlockPos basePos = BasePosResolver.resolve(player, target, interact_pos, useTarget, useTargetBlock,
                absolute, coordinateMath);

        double px = x.evaluate(player, target, slot);
        double py = y.evaluate(player, target, slot);
        double pz = z.evaluate(player, target, slot);

        BlockPos targetPos = basePos.offset((int) px, (int) py, (int) pz);

        // This checks if the player/target/interaction position is at the specified coord
        BlockPos currentPos = (interact_pos != null) ? interact_pos : player.blockPosition();
        return currentPos.equals(targetPos);
    }
}

class IsBlockCondition implements Condition {
    private final String blockDefinition;
    private final ScalingValue offsetX;
    private final ScalingValue offsetY;
    private final ScalingValue offsetZ;
    private final boolean useinteract_pos;
    private final boolean absolute;
    private final ScalingValue.MathOp coordinateMath;
    private final boolean useRaycast;
    private final ScalingValue rayRange;

    public IsBlockCondition(String blockDefinition, ScalingValue offsetX, ScalingValue offsetY, ScalingValue offsetZ,
            boolean useinteract_pos, boolean absolute, ScalingValue.MathOp math,
            boolean useRaycast, ScalingValue rayRange) {
        this.blockDefinition = blockDefinition;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.useinteract_pos = useinteract_pos;
        this.absolute = absolute;
        this.coordinateMath = math != null ? math : ScalingValue.MathOp.FLOOR;
        this.useRaycast = useRaycast;
        this.rayRange = rayRange;
    }

    @Override
    public boolean evaluate(Player player, @javax.annotation.Nullable LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable BlockPos interact_pos) {
        int ox = (int) offsetX.evaluate(player, target);
        int oy = (int) offsetY.evaluate(player, target);
        int oz = (int) offsetZ.evaluate(player, target);

        BlockPos finalPos;
        if (useRaycast) {
            double range = rayRange.evaluate(player, target);
            net.minecraft.world.phys.BlockHitResult hit = player.level().clip(
                    new net.minecraft.world.level.ClipContext(
                            player.getEyePosition(1f),
                            player.getEyePosition(1f).add(player.getViewVector(1f).scale(range)),
                            net.minecraft.world.level.ClipContext.Block.OUTLINE,
                            net.minecraft.world.level.ClipContext.Fluid.NONE,
                            player));
            if (hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS) return false;
            finalPos = hit.getBlockPos().offset(ox, oy, oz);
        } else if (absolute) {
            finalPos = new BlockPos(ox, oy, oz);
        } else {
            BlockPos base;
            if (useinteract_pos && interact_pos != null) {
                base = interact_pos;
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
            ResourceLocation tagLoc = ResourceLocation.parse(blockDefinition.substring(1));
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
            @javax.annotation.Nullable net.minecraft.core.BlockPos interact_pos) {
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
            @javax.annotation.Nullable net.minecraft.core.BlockPos interact_pos) {

        LivingEntity actor = (useTarget && target != null) ? target : player;
        @SuppressWarnings("null")
        ResourceLocation id = ResourceLocation.tryParse(enchantmentId);
        if (id == null)
            return false;
        @SuppressWarnings("null")
        net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment> enchantment = actor.level().registryAccess()
                .registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getHolder(id).orElse(null);

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

    private boolean checkStack(ItemStack stack, net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment> enchantment,
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
        this.coordinateMath = math != null ? math : ScalingValue.MathOp.FLOOR;
    }

    @SuppressWarnings("null")
    @Override
    public boolean evaluate(Player player, @javax.annotation.Nullable LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable BlockPos interact_pos) {
        if (player == null)
            return false;

        BlockPos basePos = BasePosResolver.resolve(player, target, interact_pos, useTarget, useTargetBlock,
                absolute, coordinateMath);

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

class IsSpiritCondition implements Condition {
    private final boolean expected;

    public IsSpiritCondition(boolean expected) {
        this.expected = expected;
    }

    @Override
    public boolean evaluate(Player player,
            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interact_pos) {
        return DataUtils.getVariables(player)
                .map(vars -> vars.isSpirit() == expected)
                .orElse(false);
    }
}

class IsSpiritMoonCondition implements Condition {
    private final boolean expected;

    public IsSpiritMoonCondition(boolean expected) {
        this.expected = expected;
    }

    @Override
    public boolean evaluate(Player player,
            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interact_pos) {
        return mc.sayda.creraces.engine.WorldState.isSpiritMoon(player.level()) == expected;
    }
}

class AbilityLevelCondition implements Condition {
    private final String ability;
    private final String operator;
    private final ScalingValue value;

    public AbilityLevelCondition(String ability, String operator, ScalingValue value) {
        this.ability = ability;
        this.operator = operator;
        this.value = value;
    }

    @Override
    public boolean evaluate(Player player,
            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interact_pos) {
        return DataUtils.getVariables(player).map(vars -> {
            ResourceLocation targetId = null;
            if ("self".equalsIgnoreCase(ability) && slot != null) {
                targetId = vars.getAbilityInSlot(slot);
            } else if (ability != null && !ability.equalsIgnoreCase("self")) {
                String sub = ability;
                if (!sub.contains(":")) {
                    sub = "creraces:" + sub;
                }
                targetId = ResourceLocation.tryParse(sub);
            }

            if (targetId == null)
                return false;

            double current = vars.getAbilityLevel(targetId);
            double val = value.evaluate(player, target, slot);

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

class BlockDataCondition implements Condition {
    private final ScalingValue ox, oy, oz;
    private final String key;
    private final ScalingValue value;
    private final String operator;
    private final boolean useInteractPos;

    public BlockDataCondition(ScalingValue ox, ScalingValue oy, ScalingValue oz, String key, ScalingValue value,
            String operator, boolean useInteractPos) {
        this.ox = ox;
        this.oy = oy;
        this.oz = oz;
        this.key = key;
        this.value = value;
        this.operator = operator;
        this.useInteractPos = useInteractPos;
    }

    @Override
    public boolean evaluate(Player player, @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interact_pos) {
        net.minecraft.core.BlockPos pos = useInteractPos && interact_pos != null ? interact_pos : player.blockPosition();
        pos = pos.offset((int) ox.evaluate(player, target, slot, interact_pos),
                (int) oy.evaluate(player, target, slot, interact_pos),
                (int) oz.evaluate(player, target, slot, interact_pos));

        net.minecraft.world.level.block.entity.BlockEntity be = player.level().getBlockEntity(pos);
        if (be == null)
            return false;

        net.minecraft.nbt.CompoundTag tag = be.saveWithFullMetadata(player.level().registryAccess());
        if (key.equalsIgnoreCase("owner")) {
            return tag.hasUUID("owner") && tag.getUUID("owner").equals(player.getUUID());
        }

        if (tag.contains(key)) {
            double actual = tag.getDouble(key);
            double expected = value.evaluate(player, target, slot, interact_pos);

            return switch (operator) {
                case "==" -> actual == expected;
                case "!=" -> actual != expected;
                case ">" -> actual > expected;
                case ">=" -> actual >= expected;
                case "<" -> actual < expected;
                case "<=" -> actual <= expected;
                default -> false;
            };
        }

        return false;
    }
}

class InHabitableBiomeCondition implements Condition {
    @Override
    public boolean evaluate(net.minecraft.world.entity.player.Player player,
            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interact_pos) {
        return mc.sayda.creraces.capability.DataUtils.getVariables(player).map(vars -> {
            net.minecraft.resources.ResourceLocation raceId = vars.getRace();
            if (raceId == null || raceId.equals(mc.sayda.creraces.race.RaceRegistry.NONE)) return false;
            mc.sayda.creraces.race.Race race = mc.sayda.creraces.race.RaceRegistry.get(raceId);
            if (race == null || !race.enableTerritory()) return false;
            java.util.List<String> validBiomes = race.claimValidBiomes();
            if (validBiomes.isEmpty()) return true;
            @SuppressWarnings("null")
            net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> biome =
                    player.level().getBiome(player.blockPosition());
            return mc.sayda.creraces.engine.BiomeChecker.matches(biome, validBiomes);
        }).orElse(false);
    }
}

class HasFactionCondition implements Condition {
    @Override
    public boolean evaluate(net.minecraft.world.entity.player.Player player,
            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interact_pos) {
        return mc.sayda.creraces.capability.DataUtils.getVariables(player).map(vars -> {
            net.minecraft.resources.ResourceLocation raceId = vars.getRace();
            if (raceId == null || raceId.equals(mc.sayda.creraces.race.RaceRegistry.NONE)) return false;
            mc.sayda.creraces.race.Race race = mc.sayda.creraces.race.RaceRegistry.get(raceId);
            return race != null && race.effectiveFactionGroup() != null;
        }).orElse(false);
    }
}

class IsFactionLeaderCondition implements Condition {
    @Override
    public boolean evaluate(net.minecraft.world.entity.player.Player player,
            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interact_pos) {
        return mc.sayda.creraces.territory.FactionLeaderManager.isLeader(player);
    }
}

class HasFactionGroupCondition implements Condition {
    @Override
    public boolean evaluate(net.minecraft.world.entity.player.Player player,
            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interact_pos) {
        return mc.sayda.creraces.territory.FactionLeaderManager.getFactionGroup(player) != null;
    }
}

class InClaimedTerritoryCondition implements Condition {
    private final String scope; // "own", "allied", "any", "other"

    InClaimedTerritoryCondition(String scope) {
        this.scope = scope;
    }

    @Override
    public boolean evaluate(net.minecraft.world.entity.player.Player player,
            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interact_pos) {
        mc.sayda.creraces.territory.TerritoryManager tm =
                mc.sayda.creraces.territory.TerritoryManager.get();
        net.minecraft.world.level.ChunkPos chunk = new net.minecraft.world.level.ChunkPos(
                player.blockPosition());
        mc.sayda.creraces.territory.ClaimData claim = tm.getClaimAt(chunk);
        if (claim == null) return false;

        if ("any".equals(scope)) return true;

        net.minecraft.resources.ResourceLocation playerRace =
                mc.sayda.creraces.capability.DataUtils.getVariables(player)
                        .map(mc.sayda.creraces.capability.IPlayerVariables::getRace)
                        .orElse(null);

        if ("own".equals(scope)) {
            return playerRace != null && claim.getRaceId().equals(playerRace);
        }
        if ("allied".equals(scope)) {
            if (playerRace == null) return false;
            if (claim.getRaceId().equals(playerRace)) return true;
            return tm.getDiplomacy(playerRace, claim.getRaceId())
                    == mc.sayda.creraces.territory.DiplomacyStatus.ALLY;
        }
        if ("other".equals(scope)) {
            return playerRace != null && !claim.getRaceId().equals(playerRace);
        }
        return false;
    }
}

