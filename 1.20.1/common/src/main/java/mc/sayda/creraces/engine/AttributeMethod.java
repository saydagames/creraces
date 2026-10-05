package mc.sayda.creraces.engine;

/**
 * Whether an attribute_modifier trait or action adds its modifier or strips it (by id) from the
 * entity. Anything unrecognised falls back to ADD.
 */
public enum AttributeMethod {
    ADD, REMOVE;

    public static AttributeMethod fromString(String str) {
        if (str == null) return ADD;
        try {
            return valueOf(str.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ADD;
        }
    }
}
