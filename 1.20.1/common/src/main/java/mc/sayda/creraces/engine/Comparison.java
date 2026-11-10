package mc.sayda.creraces.engine;

import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.util.GsonHelper;

import javax.annotation.Nullable;
import java.util.Objects;

/** The comparison operators every comparing condition shares: ==, !=, >, >=, <, <=. */
public enum Comparison {
    EQUAL("=="), NOT_EQUAL("!="), GREATER(">"), GREATER_OR_EQUAL(">="), LESS("<"), LESS_OR_EQUAL("<=");

    /** == and != allow this much floating-point noise. */
    private static final double EPSILON = 0.001;

    private final String symbol;

    Comparison(String symbol) {
        this.symbol = symbol;
    }

    public boolean test(double current, double expected) {
        return switch (this) {
            case EQUAL -> Math.abs(current - expected) < EPSILON;
            case NOT_EQUAL -> Math.abs(current - expected) >= EPSILON;
            case GREATER -> current > expected;
            case GREATER_OR_EQUAL -> current >= expected;
            case LESS -> current < expected;
            case LESS_OR_EQUAL -> current <= expected;
        };
    }

    /** Numbers compare by value; anything else only supports == and !=. */
    public boolean test(@Nullable Object current, @Nullable Object expected) {
        if (current instanceof Number c && expected instanceof Number e) {
            return test(c.doubleValue(), e.doubleValue());
        }
        boolean equal = Objects.equals(current, expected);
        return switch (this) {
            case EQUAL -> equal;
            case NOT_EQUAL -> !equal;
            default -> false;
        };
    }

    /**
     * Reads "operator", defaulting to ==. An unknown operator is a data error, so it is reported
     * and the condition built from it never passes.
     */
    @Nullable
    public static Comparison fromJson(JsonObject json) {
        String symbol = GsonHelper.getAsString(json, "operator", "==");
        for (Comparison comparison : values()) {
            if (comparison.symbol.equals(symbol)) {
                return comparison;
            }
        }
        CreRaces.LOGGER.error("Unknown operator '{}' in {}", symbol, json);
        return null;
    }
}
