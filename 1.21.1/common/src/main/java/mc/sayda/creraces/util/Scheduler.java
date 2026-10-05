package mc.sayda.creraces.util;

import mc.sayda.creraces.CreRaces;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Scheduler {
    private static final List<DelayedTask> TASKS = new ArrayList<>();
    private static final Queue<DelayedTask> PENDING = new ConcurrentLinkedQueue<>();

    public static void delay(int ticks, Runnable task) {
        PENDING.add(new DelayedTask(ticks, task));
    }

    /**
     * Must be called on the server thread only. TASKS is not thread-safe;
     * tasks are submitted via PENDING (a concurrent queue) and drained here.
     */
    public static void tick() {
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
                    CreRaces.LOGGER.error("Error executing delayed task", e);
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
