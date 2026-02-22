package mc.sayda.creraces.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Scheduler {
    private static final List<DelayedTask> TASKS = new ArrayList<>();

    public static void delay(int ticks, Runnable task) {
        TASKS.add(new DelayedTask(ticks, task));
    }

    public static void tick() {
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
}
