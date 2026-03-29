package mc.sayda.creraces.engine;

/**
 * Shared operation type for attribute modifiers.
 * ADD: Applies the modifier to the entity.
 * REMOVE: Strips the modifier (by ID/UUID) from the entity.
 */
public enum AttributeMethod {
    ADD, REMOVE;

    public static AttributeMethod fromString(String str) {
        if (str == null) return ADD;
        try {
            return valueOf(str.toUpperCase());
        } catch (Exception e) {
            return ADD;
        }
    }
}
