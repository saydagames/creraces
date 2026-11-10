package mc.sayda.creraces.effect;

/**
 * An effect whose damage is credited to whoever applied it. Code that applies one records the
 * applier's UUID string in the target's persistent data under {@link #SOURCE_KEY}.
 */
public interface SourceTrackedEffect {
    String SOURCE_KEY = "creraces:source";

    /** The persistent data key this effect's applier is recorded under. */
    default String sourceKey() {
        return SOURCE_KEY;
    }
}
