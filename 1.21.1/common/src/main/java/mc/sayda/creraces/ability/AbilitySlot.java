package mc.sayda.creraces.ability;

/**
 * Represents the slots available for equipping active abilities.
 * Defines five named slots: A1 (green), A2 (red), A3 (yellow), A4 (blue), A5 (orange).
 */
public enum AbilitySlot {
    A1(0, "A1", "green"),
    A2(1, "A2", "red"),
    A3(2, "A3", "yellow"),
    A4(3, "A4", "blue"),
    A5(4, "A5", "orange");

    private final int id;
    private final String keyName;
    private final String color;

    AbilitySlot(int id, String keyName, String color) {
        this.id = id;
        this.keyName = keyName;
        this.color = color;
    }

    public int getId() {
        return id;
    }

    public String getKeyName() {
        return keyName;
    }

    public String getColor() {
        return color;
    }

    public static AbilitySlot byId(int id) {
        for (AbilitySlot slot : values()) {
            if (slot.id == id)
                return slot;
        }
        return A1; // Default
    }
}
