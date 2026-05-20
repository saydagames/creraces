package mc.sayda.creraces.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Scheduler {
    private static final List<DelayedTask> TASKS = new ArrayList<>();
    private static final java.util.Queue<DelayedTask> PENDING = new java.util.concurrent.ConcurrentLinkedQueue<>();

    public static void delay(int ticks, Runnable task) {
        PENDING.add(new DelayedTask(ticks, task));
    }

    public static void tick() {
        // Drain pending into main list (only called on server thread)
        DelayedTask p;
        while ((p = PENDING.poll()) != null) {
            TASKS.add(p);
        }

        Iterator<DelayedTask> iterator = TASKS.iterator();
        while (iterator.hasNext()) {
            DelayedTask task = iterator.next();
            task.ticks--;
            if (task.ticks <= 0) {
                try {
                    task.runnable.run();
                } catch (Exception e) {
                    mc.sayda.creraces.CreRaces.LOGGER.error("Error executing delayed task", e);
                }
                iterator.remove();
            }
        }
    }

    private static class DelayedTask {
        int ticks;
        final Runnable runnable;

        DelayedTask(int ticks, Runnable runnable) {
            this.ticks = ticks;
            this.runnable = runnable;
        }
    }

    /**
     * Clears all pending and active tasks. Must be called on server shutdown
     * to prevent stale tasks from executing in a new singleplayer session.
     */
    public static void clear() {
        TASKS.clear();
        PENDING.clear();
    }
}
