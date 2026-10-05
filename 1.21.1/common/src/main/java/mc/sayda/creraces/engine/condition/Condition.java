package mc.sayda.creraces.engine.condition;

import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.config.CreRacesConfig;
import mc.sayda.creraces.engine.BiomeChecker;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.engine.TargetFilter;
import mc.sayda.creraces.engine.WorldState;
import mc.sayda.creraces.engine.traits.BlockDefinitionMatcher;
import mc.sayda.creraces.race.Race;
import mc.sayda.creraces.race.RaceCustomization;
import mc.sayda.creraces.race.RaceRegistry;
import mc.sayda.creraces.territory.ClaimData;
import mc.sayda.creraces.territory.DiplomacyStatus;
import mc.sayda.creraces.territory.FactionLeaderManager;
import mc.sayda.creraces.territory.TerritoryManager;
import mc.sayda.creraces.util.IFoodDataAccessor;
import mc.sayda.creraces.util.ItemUtils;
import mc.sayda.creraces.util.RaceUtils;
import mc.sayda.creraces.util.WorldUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Universal condition check for actions and traits. The built-in types below are package-private
 * and only built through {@link ConditionRegistry}.
 */
public interface Condition {
    boolean evaluate(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos);

    static Condition fromJson(JsonObject json) {
        return ConditionRegistry.fromJson(json);
    }
}

/** The numeric operators shared by the comparing conditions. */
final class NumericComparison {
    private static final double EPSILON = 0.001;
    private static final Set<String> OPERATORS = Set.of("==", "!=", ">", ">=", "<", "<=");

    private NumericComparison() {
    }

    static boolean isOperator(String operator) {
        return OPERATORS.contains(operator);
    }

    /** == and != allow a small epsilon for floating-point noise; an unknown operator never matches. */
    static boolean test(double current, String operator, double expected) {
        return switch (operator) {
            case ">=" -> current >= expected;
            case "<=" -> current <= expected;
            case ">" -> current > expected;
            case "<" -> current < expected;
            case "==" -> Math.abs(current - expected) < EPSILON;
            case "!=" -> Math.abs(current - expected) >= EPSILON;
            default -> false;
        };
    }
}

class WearingArmorCondition implements Condition {
    private final @Nullable String definition;
    private final String slot;

    public WearingArmorCondition(@Nullable String definition, String slot) {
        this.definition = definition;
        this.slot = slot;
    }

    @Override
    public boolean evaluate(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot abilitySlot,
            @Nullable BlockPos interactPos) {
        Iterable<ItemStack> items;
        if (slot.equalsIgnoreCase("head")) {
            items = List.of(player.getItemBySlot(EquipmentSlot.HEAD));
        } else if (slot.equalsIgnoreCase("chest")) {
            items = List.of(player.getItemBySlot(EquipmentSlot.CHEST));
        } else if (slot.equalsIgnoreCase("legs")) {
            items = List.of(player.getItemBySlot(EquipmentSlot.LEGS));
        } else if (slot.equalsIgnoreCase("feet")) {
            items = List.of(player.getItemBySlot(EquipmentSlot.FEET));
        } else {
            items = player.getArmorSlots();
        }

        for (ItemStack stack : items) {
            if (definition == null ? !stack.isEmpty() : ItemUtils.matches(stack, definition))
                return true;
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
    public boolean evaluate(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        return ItemUtils.matches(player.getMainHandItem(), definition)
                || ItemUtils.matches(player.getOffhandItem(), definition);
    }
}

class HoldingItemCondition implements Condition {
    private final @Nullable String definition;
    private final boolean useTarget;

    public HoldingItemCondition(@Nullable String definition, boolean useTarget) {
        this.definition = definition;
        this.useTarget = useTarget;
    }

    @Override
    public boolean evaluate(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        LivingEntity entity = (useTarget && target != null) ? target : player;
        for (InteractionHand hand : InteractionHand.values()) {
            @SuppressWarnings("null")
            ItemStack stack = entity.getItemInHand(hand);
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
        if (!NumericComparison.isOperator(operator)) {
            CreRaces.LOGGER.warn("StateCondition: unknown operator '{}', the condition will always be false", operator);
        }
    }

    @Override
    public boolean evaluate(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        return DataUtils.getVariables(player).map(vars -> {
            ResourceLocation stateId = resolveStateId(vars, player, slot);
            if (stateId == null)
                return false;
            return NumericComparison.test(vars.getPersistentState(stateId), operator, value.evaluate(player, target));
        }).orElse(false);
    }

    @Nullable
    private ResourceLocation resolveStateId(IPlayerVariables vars, Player player, @Nullable AbilitySlot slot) {
        if ("self".equalsIgnoreCase(state)) {
            if (slot == null) {
                CreRaces.LOGGER.warn(
                        "state:self used in condition but no ability slot context is available (used outside of an ability?)");
                return null;
            }
            ResourceLocation abilityId = vars.getAbilityInSlot(slot);
            if (abilityId == null) {
                CreRaces.LOGGER.warn("state:self used in condition but no ability found in slot '{}' for player '{}'",
                        slot, player.getName().getString());
            }
            return abilityId;
        }
        if (state == null || state.isEmpty())
            return null;
        String sub = state.startsWith("state:") ? state.substring(6) : state;
        return ResourceLocation.tryParse(sub.contains(":") ? sub : "creraces:" + sub);
    }
}

class MorphedCondition implements Condition {
    private final boolean expected;

    public MorphedCondition(boolean expected) {
        this.expected = expected;
    }

    @Override
    public boolean evaluate(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
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
    public boolean evaluate(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        LivingEntity entity = (useTarget && target != null) ? target : player;
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
    public boolean evaluate(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        LivingEntity entity = (useTarget && target != null) ? target : player;
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
    public boolean evaluate(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        LivingEntity entity = (useTarget && target != null) ? target : player;
        return entity.onGround() == expected;
    }
}

class AttackChargedCondition implements Condition {
    private final ScalingValue threshold;

    public AttackChargedCondition(ScalingValue threshold) {
        this.threshold = threshold;
    }

    @Override
    public boolean evaluate(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
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
    public boolean evaluate(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        @SuppressWarnings("null")
        Holder<MobEffect> effect = BuiltInRegistries.MOB_EFFECT.getHolder(effectId).orElse(null);
        if (effect == null)
            return false;

        LivingEntity subject = TargetFilter.resolveSmartTarget(player, target, useTarget);
        if (subject == null)
            return false;
        MobEffectInstance instance = subject.getEffect(effect);
        return instance != null && instance.getAmplifier() >= (int) minAmplifier.evaluate(player, target);
    }
}

class AndCondition implements Condition {
    private final Condition[] conditions;

    public AndCondition(Condition[] conditions) {
        this.conditions = conditions;
    }

    @Override
    public boolean evaluate(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        for (Condition c : conditions) {
            if (!c.evaluate(player, target, slot, interactPos))
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
    public boolean evaluate(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        for (Condition c : conditions) {
            if (c.evaluate(player, target, slot, interactPos))
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
    public boolean evaluate(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        return !condition.evaluate(player, target, slot, interactPos);
    }
}

class BiomeCondition implements Condition {
    private final @Nullable String biomeId;
    private final @Nullable String tag;

    public BiomeCondition(@Nullable String biomeId, @Nullable String tag) {
        this.biomeId = biomeId;
        this.tag = tag;
    }

    @Override
    public boolean evaluate(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        @SuppressWarnings("null")
        Holder<Biome> holder = player.level().getBiome(player.blockPosition());
        if (biomeId != null) {
            return holder.unwrapKey().map(key -> key.location().toString().equals(biomeId)).orElse(false);
        }
        if (tag != null && !tag.isEmpty()) {
            ResourceLocation tagId = ResourceLocation.tryParse(tag);
            return tagId != null && holder.is(TagKey.create(Registries.BIOME, tagId));
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
    public boolean evaluate(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
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
    public boolean evaluate(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        long time = player.level().getDayTime() % Level.TICKS_PER_DAY;
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
    public boolean evaluate(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        @SuppressWarnings("null")
        boolean inBubble = player.level().getBlockState(player.blockPosition()).is(Blocks.BUBBLE_COLUMN);
        boolean inWater = player.isInWater() || inBubble;
        if (includeRain && !inWater) {
            inWater = WorldUtils.isExposedToRain(player);
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
    public boolean evaluate(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        @SuppressWarnings("null")
        boolean inSun = player.level().isDay()
                && !player.level().isRaining()
                && player.level().canSeeSky(player.blockPosition());
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
    public boolean evaluate(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
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
    public boolean evaluate(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        LivingEntity entity = (useTarget && target != null) ? target : player;
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
    public boolean evaluate(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        LivingEntity entity = (useTarget && target != null) ? target : player;
        Vec3 vel = entity.getDeltaMovement();
        double t = threshold.evaluate(player, target);
        boolean isMoving = vel.lengthSqr() > t * t;
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
    public boolean evaluate(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        return DataUtils.getVariables(player)
                .map(vars -> NumericComparison.test(currentLevel(player, vars), operator, value.evaluate(player, target)))
                .orElse(false);
    }

    private double currentLevel(Player player, IPlayerVariables vars) {
        String res = resource.toLowerCase();
        if (res.startsWith("custom:")) {
            String valStr = vars.getCustomization(resource.substring(7));
            try {
                return (valStr != null && !valStr.isEmpty()) ? Double.parseDouble(valStr) : 0.0;
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }
        if (res.startsWith("state:")) {
            String key = resource.substring(6);
            ResourceLocation loc = ResourceLocation.tryParse(key.contains(":") ? key : "creraces:" + key);
            return loc != null ? vars.getPersistentState(loc) : 0.0;
        }
        return switch (res) {
            case "mana" -> vars.getMana();
            case "energy" -> vars.getEnergy();
            case "grit" -> vars.getGrit();
            case "rage" -> vars.getRage();
            case "karma" -> vars.getKarma();
            case "soul" -> vars.getSoul();
            case "food" -> (double) ((IFoodDataAccessor) player.getFoodData()).creraces$getFoodLevel();
            case "saturation" -> (double) ((IFoodDataAccessor) player.getFoodData()).creraces$getSaturation();
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
}

class ExposedToRainCondition implements Condition {
    private final boolean expected;

    public ExposedToRainCondition(boolean expected) {
        this.expected = expected;
    }

    @Override
    public boolean evaluate(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        return WorldUtils.isExposedToRain(player) == expected;
    }
}

class IsSmeltableCondition implements Condition {
    @Override
    public boolean evaluate(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty())
            return false;
        @SuppressWarnings("null")
        boolean present = player.level().getRecipeManager()
                .getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(stack), player.level())
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
    public boolean evaluate(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        double dx = player.getX() - x.evaluate(player, target);
        double dy = player.getY() - y.evaluate(player, target);
        double dz = player.getZ() - z.evaluate(player, target);
        double maxD = maxDistance.evaluate(player, target);
        return (dx * dx + dy * dy + dz * dz) <= (maxD * maxD);
    }
}

class CustomizationEqualsCondition implements Condition {
    private final String customizationId;
    private final List<String> allowedValues;

    public CustomizationEqualsCondition(String customizationId, String[] allowedValues) {
        this.customizationId = customizationId;
        this.allowedValues = Arrays.asList(allowedValues);
    }

    @Override
    public boolean evaluate(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        return DataUtils.getVariables(player).map(vars -> {
            ResourceLocation raceId = vars.getRace();
            if (raceId == null)
                return false;

            Race race = RaceRegistry.get(raceId);
            if (race == null)
                return false;

            String val;
            if (customizationId.equalsIgnoreCase("race")) {
                val = raceId.toString();
            } else if (customizationId.equalsIgnoreCase("gstate")) {
                val = String.valueOf(vars.getGState());
            } else {
                RaceCustomization customization = race.customization().stream()
                        .filter(c -> c.id().equals(customizationId))
                        .findFirst()
                        .orElse(null);
                if (customization == null)
                    return false;

                val = vars.getCustomization(customizationId.toLowerCase());
                if (val == null || val.isEmpty())
                    val = customization.defaultValue();
            }

            return allowedValues.contains(val);
        }).orElse(false);
    }
}

class RaceEqualsCondition implements Condition {
    private final List<String> validRaces;

    public RaceEqualsCondition(List<String> validRaces) {
        this.validRaces = validRaces;
    }

    @Override
    public boolean evaluate(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        return DataUtils.getVariables(player).map(vars -> {
            ResourceLocation raceId = vars.getRace();
            return raceId != null && validRaces.contains(raceId.toString());
        }).orElse(false);
    }
}

class HasCustomizationCondition implements Condition {
    private final String customizationId;

    public HasCustomizationCondition(String customizationId) {
        this.customizationId = customizationId;
    }

    @Override
    public boolean evaluate(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        return DataUtils.getVariables(player).map(vars -> {
            if (vars.getRace() == null)
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
    public boolean evaluate(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        return player.level().dimension().location().toString().equals(dimension);
    }
}

class SpiritCondition implements Condition {
    private final boolean expected;

    public SpiritCondition(boolean expected) {
        this.expected = expected;
    }

    @Override
    public boolean evaluate(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        return DataUtils.getVariables(player)
                .map(vars -> vars.isInSpiritRealm() == expected)
                .orElse(false);
    }
}

class HasEntitiesCondition implements Condition {
    private final ScalingValue radius;
    private final TargetFilter targets;

    public HasEntitiesCondition(ScalingValue radius, TargetFilter targets) {
        this.radius = radius;
        this.targets = targets;
    }

    @Override
    public boolean evaluate(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        double r = radius.evaluate(player, target, slot);
        int maxAoeRadius = CreRacesConfig.AOE_MAX_RADIUS.get();
        if (maxAoeRadius > 0)
            r = Math.min(r, maxAoeRadius);

        final AABB area = Objects.requireNonNull(player.getBoundingBox().inflate(r));
        return !player.level().getEntitiesOfClass(LivingEntity.class, area,
                e -> e != player && targets.isValid(e, player)).isEmpty();
    }
}

/**
 * Base position for the block-position conditions: the world origin (absolute), the target's
 * block, the interacted block, or the player's position rounded with {@code coordinateMath}.
 */
final class BasePosResolver {
    private BasePosResolver() {
    }

    static BlockPos resolve(Player player, @Nullable LivingEntity target, @Nullable BlockPos interactPos,
            boolean useTarget, boolean useTargetBlock, boolean absolute, ScalingValue.MathOp coordinateMath) {
        if (absolute) {
            return BlockPos.ZERO;
        } else if (useTarget && target != null) {
            return target.blockPosition();
        } else if (useTargetBlock && interactPos != null) {
            return interactPos;
        }
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        if (coordinateMath == ScalingValue.MathOp.ROUND) {
            return new BlockPos((int) Math.round(x), (int) Math.round(y), (int) Math.round(z));
        } else if (coordinateMath == ScalingValue.MathOp.CEIL) {
            return new BlockPos((int) Math.ceil(x), (int) Math.ceil(y), (int) Math.ceil(z));
        }
        return new BlockPos((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
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
        this.coordinateMath = math;
    }

    @Override
    public boolean evaluate(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        BlockPos basePos = BasePosResolver.resolve(player, target, interactPos, useTarget, useTargetBlock,
                absolute, coordinateMath);
        BlockPos targetPos = basePos.offset((int) x.evaluate(player, target, slot),
                (int) y.evaluate(player, target, slot), (int) z.evaluate(player, target, slot));

        // Compared against the interacted block when there is one, else the player's block.
        BlockPos currentPos = (interactPos != null) ? interactPos : player.blockPosition();
        return currentPos.equals(targetPos);
    }
}

class IsBlockCondition implements Condition {
    private final String blockDefinition;
    private final ScalingValue offsetX;
    private final ScalingValue offsetY;
    private final ScalingValue offsetZ;
    private final boolean useInteractPos;
    private final boolean absolute;
    private final ScalingValue.MathOp coordinateMath;
    private final boolean useRaycast;
    private final ScalingValue rayRange;

    public IsBlockCondition(String blockDefinition, ScalingValue offsetX, ScalingValue offsetY, ScalingValue offsetZ,
            boolean useInteractPos, boolean absolute, ScalingValue.MathOp math,
            boolean useRaycast, ScalingValue rayRange) {
        this.blockDefinition = blockDefinition;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.useInteractPos = useInteractPos;
        this.absolute = absolute;
        this.coordinateMath = math;
        this.useRaycast = useRaycast;
        this.rayRange = rayRange;
    }

    @Override
    public boolean evaluate(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        int ox = (int) offsetX.evaluate(player, target);
        int oy = (int) offsetY.evaluate(player, target);
        int oz = (int) offsetZ.evaluate(player, target);

        BlockPos basePos;
        if (useRaycast) {
            Vec3 eye = player.getEyePosition(1f);
            Vec3 end = eye.add(player.getViewVector(1f).scale(rayRange.evaluate(player, target)));
            BlockHitResult hit = player.level().clip(
                    new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
            if (hit.getType() == HitResult.Type.MISS)
                return false;
            basePos = hit.getBlockPos();
        } else {
            basePos = BasePosResolver.resolve(player, target, interactPos, false, useInteractPos, absolute,
                    coordinateMath);
        }

        BlockState state = player.level().getBlockState(basePos.offset(ox, oy, oz));
        return BlockDefinitionMatcher.matches(state, blockDefinition);
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
    public boolean evaluate(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        return DataUtils.getVariables(player).map(vars -> {
            @SuppressWarnings("null")
            ResourceLocation fullId = ResourceLocation.tryParse(id);
            if (fullId == null)
                return false;
            return NumericComparison.test(vars.getCooldown(fullId), operator, value.evaluate(player, target));
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
    public boolean evaluate(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot abilitySlot,
            @Nullable BlockPos interactPos) {
        LivingEntity actor = (useTarget && target != null) ? target : player;
        @SuppressWarnings("null")
        ResourceLocation id = ResourceLocation.tryParse(enchantmentId);
        if (id == null)
            return false;
        @SuppressWarnings("null")
        Holder<Enchantment> enchantment = actor.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                .getHolder(id).orElse(null);
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
        return checkStack(getItemInSlot(actor, slot), enchantment, targetLevel);
    }

    private boolean checkStack(ItemStack stack, Holder<Enchantment> enchantment, double targetLevel) {
        if (stack.isEmpty())
            return false;
        int currentLevel = EnchantmentHelper.getItemEnchantmentLevel(enchantment, stack);
        return NumericComparison.test(currentLevel, operator, targetLevel);
    }

    private ItemStack getItemInSlot(LivingEntity entity, String slot) {
        if (slot.equalsIgnoreCase("mainhand"))
            return entity.getMainHandItem();
        if (slot.equalsIgnoreCase("offhand"))
            return entity.getOffhandItem();
        if (slot.equalsIgnoreCase("head"))
            return entity.getItemBySlot(EquipmentSlot.HEAD);
        if (slot.equalsIgnoreCase("chest"))
            return entity.getItemBySlot(EquipmentSlot.CHEST);
        if (slot.equalsIgnoreCase("legs"))
            return entity.getItemBySlot(EquipmentSlot.LEGS);
        if (slot.equalsIgnoreCase("feet"))
            return entity.getItemBySlot(EquipmentSlot.FEET);

        if (entity instanceof Player player) {
            try {
                int index = Integer.parseInt(slot);
                if (index >= 0 && index < player.getInventory().getContainerSize()) {
                    return player.getInventory().getItem(index);
                }
            } catch (NumberFormatException ignored) {
                // Not an inventory index either: nothing to check.
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
        this.coordinateMath = math;
    }

    @SuppressWarnings("null")
    @Override
    public boolean evaluate(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        BlockPos basePos = BasePosResolver.resolve(player, target, interactPos, useTarget, useTargetBlock,
                absolute, coordinateMath);
        BlockPos finalPos = basePos.offset((int) offsetX.evaluate(player, target, slot),
                (int) offsetY.evaluate(player, target, slot), (int) offsetZ.evaluate(player, target, slot));

        BlockState state = player.level().getBlockState(finalPos);
        if (!state.isAir() && !state.canBeReplaced())
            return false;

        return player.level().getEntitiesOfClass(Entity.class, new AABB(finalPos), e -> e != player).isEmpty();
    }
}

class IsSpiritCondition implements Condition {
    private final boolean expected;

    public IsSpiritCondition(boolean expected) {
        this.expected = expected;
    }

    @Override
    public boolean evaluate(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        return RaceUtils.isSpirit(player) == expected;
    }
}

class IsSpiritMoonCondition implements Condition {
    private final boolean expected;

    public IsSpiritMoonCondition(boolean expected) {
        this.expected = expected;
    }

    @Override
    public boolean evaluate(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        return WorldState.isSpiritMoon(player.level()) == expected;
    }
}

class AbilityLevelCondition implements Condition {
    private final String ability;
    private final String operator;
    private final ScalingValue value;

    public AbilityLevelCondition(String ability, String operator, ScalingValue value) {
        this.ability = ability;
        // An unrecognised operator has always meant "==" for this condition.
        this.operator = NumericComparison.isOperator(operator) ? operator : "==";
        this.value = value;
    }

    @Override
    public boolean evaluate(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        return DataUtils.getVariables(player).map(vars -> {
            ResourceLocation abilityId = null;
            if ("self".equalsIgnoreCase(ability) && slot != null) {
                abilityId = vars.getAbilityInSlot(slot);
            } else if (ability != null && !ability.equalsIgnoreCase("self")) {
                abilityId = ResourceLocation.tryParse(ability.contains(":") ? ability : "creraces:" + ability);
            }

            if (abilityId == null)
                return false;
            return NumericComparison.test(vars.getAbilityLevel(abilityId), operator,
                    value.evaluate(player, target, slot));
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
    public boolean evaluate(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        BlockPos pos = useInteractPos && interactPos != null ? interactPos : player.blockPosition();
        pos = pos.offset((int) ox.evaluate(player, target, slot, interactPos),
                (int) oy.evaluate(player, target, slot, interactPos),
                (int) oz.evaluate(player, target, slot, interactPos));

        BlockEntity be = player.level().getBlockEntity(pos);
        if (be == null)
            return false;

        CompoundTag tag = be.saveWithFullMetadata(player.level().registryAccess());
        if (key.equalsIgnoreCase("owner")) {
            return tag.hasUUID("owner") && tag.getUUID("owner").equals(player.getUUID());
        }
        if (!tag.contains(key))
            return false;

        double actual = tag.getDouble(key);
        double expected = value.evaluate(player, target, slot, interactPos);
        // Exact comparison, unlike NumericComparison: NBT values are compared as stored.
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
}

class InHabitableBiomeCondition implements Condition {
    @Override
    public boolean evaluate(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        return DataUtils.getVariables(player).map(vars -> {
            ResourceLocation raceId = vars.getRace();
            if (raceId == null || raceId.equals(RaceRegistry.NONE)) return false;
            Race race = RaceRegistry.get(raceId);
            if (race == null || !race.enableTerritory()) return false;
            List<String> validBiomes = race.claimValidBiomes();
            if (validBiomes.isEmpty()) return true;
            @SuppressWarnings("null")
            Holder<Biome> biome = player.level().getBiome(player.blockPosition());
            return BiomeChecker.matches(biome, validBiomes);
        }).orElse(false);
    }
}

class HasFactionCondition implements Condition {
    @Override
    public boolean evaluate(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        return DataUtils.getVariables(player).map(vars -> {
            ResourceLocation raceId = vars.getRace();
            if (raceId == null || raceId.equals(RaceRegistry.NONE)) return false;
            Race race = RaceRegistry.get(raceId);
            return race != null && race.factionGroup() != null;
        }).orElse(false);
    }
}

class IsFactionLeaderCondition implements Condition {
    @Override
    public boolean evaluate(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        return FactionLeaderManager.isLeader(player);
    }
}

class InClaimedTerritoryCondition implements Condition {
    private final String scope; // "own", "allied", "any", "other"

    InClaimedTerritoryCondition(String scope) {
        this.scope = scope;
    }

    @Override
    public boolean evaluate(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        TerritoryManager tm = TerritoryManager.get();
        ClaimData claim = tm.getClaimAt(new ChunkPos(player.blockPosition()));
        if (claim == null) return false;

        if ("any".equals(scope)) return true;

        ResourceLocation playerRace = DataUtils.getVariables(player)
                .map(IPlayerVariables::getRace)
                .orElse(null);

        if ("own".equals(scope)) {
            return playerRace != null && claim.getRaceId().equals(playerRace);
        }
        if ("allied".equals(scope)) {
            if (playerRace == null) return false;
            if (claim.getRaceId().equals(playerRace)) return true;
            return tm.getDiplomacy(playerRace, claim.getRaceId()) == DiplomacyStatus.ALLY;
        }
        if ("other".equals(scope)) {
            return playerRace != null && !claim.getRaceId().equals(playerRace);
        }
        return false;
    }
}
