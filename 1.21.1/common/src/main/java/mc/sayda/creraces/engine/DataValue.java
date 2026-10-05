package mc.sayda.creraces.engine;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.config.CreRacesConfig;
import mc.sayda.creraces.util.GsonHelper;
import mc.sayda.creraces.util.IPersistentDataAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.UUID;

/**
 * The ScalingValue of non-numeric data: a typed, "literal OR read-from-an-entity" value used
 * anywhere the engine needs to move a string/int/double/boolean/uuid on or off an entity
 * (persistent data key, or an intrinsic property such as the entity's owner or type).
 */
public class DataValue {
    public enum DataType { STRING, INT, DOUBLE, BOOLEAN, UUID }

    private enum Property { UUID, OWNER, ENTITY_TYPE, NAME }

    // Owner of a mob bound with the commanding staff; such mobs are usually not OwnableEntity.
    private static final String SERVANT_OWNER_KEY = "creraces:servant_of";

    private final DataType type;
    private final boolean useTarget;
    @Nullable
    private final Object literal;
    @Nullable
    private final String key;
    @Nullable
    private final Property property;
    @Nullable
    private final ScalingValue scaling;

    private DataValue(DataType type, boolean useTarget, @Nullable Object literal, @Nullable String key,
            @Nullable Property property, @Nullable ScalingValue scaling) {
        this.type = type;
        this.useTarget = useTarget;
        this.literal = literal;
        this.key = key;
        this.property = property;
        this.scaling = scaling;
    }

    public DataType type() {
        return type;
    }

    public static DataValue literal(DataType type, Object value) {
        return new DataValue(type, false, value, null, null, null);
    }

    /**
     * Resolves this value. Null means unresolvable: no entity, an absent tag, or a value that
     * could not be coerced to the declared type.
     */
    @Nullable
    public Object resolve(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        if (literal != null) {
            return literal;
        }

        if (scaling != null) {
            double val = scaling.evaluate(player, target, slot, interactPos);
            return type == DataType.INT ? (int) Math.round(val) : (Object) val;
        }

        LivingEntity entity = resolveEntity(useTarget, player, target);
        if (entity == null) {
            return null;
        }

        if (key != null) {
            return readTag(((IPersistentDataAccessor) entity).creraces$getPersistentData(), key, type);
        }

        if (property != null) {
            Object raw = switch (property) {
                case UUID -> entity.getUUID();
                case OWNER -> readOwner(entity);
                case ENTITY_TYPE -> BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
                case NAME -> entity.getName().getString();
            };
            return coerce(raw, type);
        }

        return null;
    }

    /**
     * Resolves this value and writes it to {@code tag} under {@code key}. Leaves {@code tag}
     * untouched (returns false) when the value is unresolvable.
     */
    public boolean writeInto(CompoundTag tag, String key, Player player, @Nullable LivingEntity target,
            @Nullable AbilitySlot slot, @Nullable BlockPos interactPos) {
        Object value = resolve(player, target, slot, interactPos);
        if (value == null) {
            return false;
        }
        switch (type) {
            case STRING -> tag.putString(key, (String) value);
            case INT -> tag.putInt(key, (Integer) value);
            case DOUBLE -> tag.putDouble(key, (Double) value);
            case BOOLEAN -> tag.putBoolean(key, (Boolean) value);
            case UUID -> tag.putUUID(key, (UUID) value);
        }
        return true;
    }

    @Nullable
    public static DataValue fromJson(JsonObject json, String member) {
        return fromJson(json, member, null);
    }

    @Nullable
    public static DataValue fromJson(JsonObject json, String member, @Nullable DataType shorthandDefault) {
        if (!json.has(member) || json.get(member).isJsonNull()) {
            return null;
        }
        JsonElement el = json.get(member);

        if (el.isJsonPrimitive()) {
            JsonPrimitive prim = el.getAsJsonPrimitive();
            if (prim.isBoolean()) {
                return new DataValue(DataType.BOOLEAN, false, prim.getAsBoolean(), null, null, null);
            }
            if (prim.isNumber()) {
                return new DataValue(DataType.DOUBLE, false, prim.getAsDouble(), null, null, null);
            }
            return new DataValue(DataType.STRING, false, prim.getAsString(), null, null, null);
        }

        if (!el.isJsonObject()) {
            return null;
        }
        JsonObject obj = el.getAsJsonObject();

        String typeStr = GsonHelper.getAsString(obj, "type",
                shorthandDefault != null ? shorthandDefault.name().toLowerCase() : "string");
        DataType type;
        try {
            type = DataType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            CreRaces.LOGGER.error("DataValue: unknown type '{}' in {}", typeStr, member);
            return null;
        }

        boolean useTarget = readUseTarget(obj, false);

        if (obj.has("value")) {
            Object literal = coerceLiteral(obj.get("value"), type);
            if (literal == null) {
                CreRaces.LOGGER.error("DataValue: could not parse literal 'value' as {} in {}", type, member);
                return null;
            }
            return new DataValue(type, useTarget, literal, null, null, null);
        }

        if (obj.has("key")) {
            return new DataValue(type, useTarget, null, GsonHelper.getAsString(obj, "key"), null, null);
        }

        if (obj.has("scaling")) {
            if (type != DataType.INT && type != DataType.DOUBLE) {
                CreRaces.LOGGER.error("DataValue: 'scaling' source only valid for int/double types, got {} in {}",
                        type, member);
                return null;
            }
            return new DataValue(type, useTarget, null, null, null, ScalingValue.fromJson(obj, "scaling", 0.0));
        }

        if (obj.has("property")) {
            String propStr = GsonHelper.getAsString(obj, "property");
            Property property = parseProperty(propStr);
            if (property == null) {
                CreRaces.LOGGER.error("DataValue: unknown property '{}' in {}", propStr, member);
                return null;
            }
            return new DataValue(type, useTarget, null, null, property, null);
        }

        Property defaultProperty = switch (type) {
            case UUID -> Property.UUID;
            case STRING -> Property.ENTITY_TYPE;
            default -> null;
        };
        if (defaultProperty == null) {
            CreRaces.LOGGER.error("DataValue: {} has no source (value/key/property/scaling) and no default", member);
            return null;
        }
        return new DataValue(type, useTarget, null, null, defaultProperty, null);
    }

    /**
     * "self" is always the player, "target" is the resolved target (may be null) - unlike
     * TargetFilter.resolveSmartTarget, this never silently falls through to the target when
     * "self" was asked for.
     */
    @Nullable
    public static LivingEntity resolveEntity(boolean useTarget, Player player, @Nullable LivingEntity target) {
        return useTarget ? target : player;
    }

    /** Reads json["entity"] ("self"|"target"), falling back to the legacy json["use_target"]. */
    public static boolean readUseTarget(JsonObject json, boolean fallback) {
        String entity = GsonHelper.getNullableString(json, "entity", null);
        if (entity != null) {
            return entity.equalsIgnoreCase("target");
        }
        return GsonHelper.getAsBoolean(json, "use_target", fallback);
    }

    @Nullable
    public static Object readTag(CompoundTag tag, String key, DataType type) {
        if (!tag.contains(key)) {
            return null;
        }
        return switch (type) {
            // UUIDs are stored as int arrays, which getString would read as "".
            case STRING -> tag.getTagType(key) == Tag.TAG_INT_ARRAY ? stringifyUuidTag(tag, key) : tag.getString(key);
            case INT -> tag.getInt(key);
            case DOUBLE -> tag.getDouble(key);
            case BOOLEAN -> tag.getBoolean(key);
            case UUID -> DataUtils.loadUUID(tag, key);
        };
    }

    private static String stringifyUuidTag(CompoundTag tag, String key) {
        UUID uuid = DataUtils.loadUUID(tag, key);
        return uuid != null ? uuid.toString() : "";
    }

    public static String sanitizeKey(String key, String context) {
        int maxLen = CreRacesConfig.ENTITY_DATA_KEY_MAX_LENGTH.get();
        if (maxLen > 0 && key.length() > maxLen) {
            CreRaces.LOGGER.warn("{}: key '{}' exceeds {} chars, truncating", context, key, maxLen);
            return key.substring(0, maxLen);
        }
        return key;
    }

    /** Numeric operators use the engine's usual epsilon compare; other types only support ==/!=. */
    public static boolean compare(@Nullable Object current, @Nullable Object expected, String operator) {
        if (current instanceof Number cn && expected instanceof Number en) {
            double c = cn.doubleValue();
            double e = en.doubleValue();
            return switch (operator) {
                case ">=" -> c >= e;
                case "<=" -> c <= e;
                case ">" -> c > e;
                case "<" -> c < e;
                case "!=" -> Math.abs(c - e) >= 0.001;
                default -> Math.abs(c - e) < 0.001;
            };
        }

        boolean equal = Objects.equals(current, expected);
        return switch (operator) {
            case "!=" -> !equal;
            case "==" -> equal;
            default -> {
                CreRaces.LOGGER.warn("DataValue.compare: operator '{}' only supports ==/!= for non-numeric types",
                        operator);
                yield equal;
            }
        };
    }

    @Nullable
    private static UUID readOwner(LivingEntity entity) {
        if (entity instanceof OwnableEntity ownable) {
            UUID uuid = ownable.getOwnerUUID();
            if (uuid != null) {
                return uuid;
            }
        }
        if (entity instanceof IPersistentDataAccessor accessor) {
            CompoundTag data = accessor.creraces$getPersistentData();
            if (data.contains(SERVANT_OWNER_KEY)) {
                return DataUtils.loadUUID(data, SERVANT_OWNER_KEY);
            }
        }
        return null;
    }

    @Nullable
    private static Object coerceLiteral(JsonElement el, DataType type) {
        try {
            return switch (type) {
                case STRING -> el.getAsString();
                case INT -> el.getAsInt();
                case DOUBLE -> el.getAsDouble();
                case BOOLEAN -> el.getAsBoolean();
                case UUID -> UUID.fromString(el.getAsString());
            };
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    private static Object coerce(@Nullable Object raw, DataType type) {
        if (raw == null) {
            return null;
        }
        return switch (type) {
            case STRING -> raw instanceof String s ? s : String.valueOf(raw);
            case UUID -> raw instanceof UUID u ? u : tryParseUuid(String.valueOf(raw));
            case INT -> raw instanceof Number n ? n.intValue() : tryParseInt(String.valueOf(raw));
            case DOUBLE -> raw instanceof Number n ? n.doubleValue() : tryParseDouble(String.valueOf(raw));
            case BOOLEAN -> raw instanceof Boolean b ? b : Boolean.parseBoolean(String.valueOf(raw));
        };
    }

    @Nullable
    private static UUID tryParseUuid(String s) {
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Nullable
    private static Integer tryParseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Nullable
    private static Double tryParseDouble(String s) {
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Nullable
    private static Property parseProperty(String s) {
        return switch (s.toLowerCase()) {
            case "uuid" -> Property.UUID;
            case "owner" -> Property.OWNER;
            case "entity_type" -> Property.ENTITY_TYPE;
            case "name" -> Property.NAME;
            default -> null;
        };
    }
}
