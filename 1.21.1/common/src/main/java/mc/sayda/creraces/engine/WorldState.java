package mc.sayda.creraces.engine;

import net.minecraft.world.level.Level;

/**
 * Manages global celestial and world-wide states for the CreRaces engine.
 */
public class WorldState {

    /** The current in-world day number, per this level's dayTime. */
    public static long currentDay(Level level) {
        return Math.floorDiv(level.getDayTime(), 24000L);
    }

    /**
     * Checks if the Spirit Moon is currently active in the given level.
     * The Spirit Moon occurs during the night of Day 9, 18, 27, etc.
     */
    public static boolean isSpiritMoon(Level level) {
        if (level == null) return false;

        long time = level.getDayTime();
        long day = currentDay(level);
        long timeOfDay = Math.floorMod(time, 24000L);
        
        // Day 9 (day count 8) during Night (Sunset to Dawn)
        // Night window: 12500 (Dusk) to 23500 (approx. dawn)
        boolean isDay9 = Math.floorMod(day, 9L) == 8L;
        boolean isNightTime = timeOfDay >= 12500 && timeOfDay < 23500;
        
        return isDay9 && isNightTime;
    }
}
