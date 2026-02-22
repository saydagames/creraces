package mc.sayda.creraces.ability;

public enum AbilityType {
    /**
     * Actively triggered by a keypress. Usually has a cooldown and cost.
     */
    ACTIVE,
    /**
     * Always active if the race is selected, but not necessarily a stat boost.
     * e.g. Night Vision or Underwater Breathing.
     */
    INNATE,
    /**
     * Passive stat boosts or conditional triggers.
     */
    PASSIVE,
    /**
     * Special ability for transformations or unique mechanics.
     */
    ULTIMATE
}
