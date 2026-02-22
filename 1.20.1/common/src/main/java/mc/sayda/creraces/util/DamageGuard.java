package mc.sayda.creraces.util;

/**
 * Prevents infinite recursion when traits trigger damage.
 */
public class DamageGuard {
    private static final ThreadLocal<Boolean> PROCESSING = ThreadLocal.withInitial(() -> false);

    public static boolean isProcessing() {
        return PROCESSING.get();
    }

    public static void setProcessing(boolean processing) {
        PROCESSING.set(processing);
    }
}
