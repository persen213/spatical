package dev.spatial.inversion;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.server.MinecraftServer;

/**
 * Server-side tick scheduler for timed delays and cleanups.
 */
public final class SpatialScheduler {
    private SpatialScheduler() {}

    private record Task(long runAtTick, Runnable action) {}
    private static final List<Task> TASKS = new ArrayList<>();

    public static synchronized void later(MinecraftServer server, int delayTicks, Runnable action) {
        long runAt = server.getTickCount() + Math.max(1, delayTicks);
        TASKS.add(new Task(runAt, action));
    }

    public static synchronized void run(MinecraftServer server) {
        if (TASKS.isEmpty()) return;
        long now = server.getTickCount();
        Iterator<Task> it = TASKS.iterator();
        while (it.hasNext()) {
            Task task = it.next();
            if (task.runAtTick <= now) {
                it.remove();
                try {
                    task.action.run();
                } catch (Throwable t) {
                    SpatialInversionMod.LOGGER.error("Error executing SpatialScheduler task", t);
                }
            }
        }
    }

    public static synchronized void clear() {
        TASKS.clear();
    }
}
