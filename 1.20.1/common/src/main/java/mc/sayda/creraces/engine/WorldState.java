package mc.sayda.creraces.engine;

import net.minecraft.world.level.Level;

/** World-wide, time-based states that read the same for every player in a level. */
public class WorldState {
    private static final int SPIRIT_MOON_CYCLE_DAYS = 9;
    // Dusk until just before sunrise, as ticks within the day.
    private static final long NIGHT_START = 12500;
    private static final long NIGHT_END = 23500;

    /** The current in-world day number (0-based), per this level's day time. */
    public static long currentDay(Level level) {
        return Math.floorDiv(level.getDayTime(), Level.TICKS_PER_DAY);
    }

    /** True during the night of every 9th day (days 9, 18, 27, ... counting the first day as day 1). */
    public static boolean isSpiritMoon(Level level) {
        if (level == null) return false;

        long timeOfDay = Math.floorMod(level.getDayTime(), Level.TICKS_PER_DAY);
        boolean isCycleDay = Math.floorMod(currentDay(level), SPIRIT_MOON_CYCLE_DAYS) == SPIRIT_MOON_CYCLE_DAYS - 1;
        return isCycleDay && timeOfDay >= NIGHT_START && timeOfDay < NIGHT_END;
    }
}
