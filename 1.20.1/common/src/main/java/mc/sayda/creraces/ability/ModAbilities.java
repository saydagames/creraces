package mc.sayda.creraces.ability;

/**
 * Registry for all hardcoded ability executors.
 */
public class ModAbilities {

    public static void registerExecutors() {
        mc.sayda.creraces.ability.impl.CustomAbilities.register();
    }
}
