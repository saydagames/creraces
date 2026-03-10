package mc.sayda.creraces.engine;

public enum GState {
    BOTH,
    MALE,
    FEMALE;

    public static GState fromString(String state) {
        if (state == null)
            return BOTH;
        try {
            return GState.valueOf(state.toUpperCase());
        } catch (IllegalArgumentException e) {
            return BOTH;
        }
    }
}
