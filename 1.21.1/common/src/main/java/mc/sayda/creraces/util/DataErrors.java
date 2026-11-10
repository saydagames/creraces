package mc.sayda.creraces.util;

import dev.architectury.event.events.common.PlayerEvent;
import mc.sayda.creraces.CreRaces;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.util.Unit;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Counts the errors CreRaces logs while data packs load and tells operators the count when they
 * join. LOAD_START is registered before the mod's reload listeners and LOAD_END after them.
 */
public final class DataErrors {
    private static final AtomicInteger COUNT = new AtomicInteger();
    private static volatile boolean loading;

    /** Starts a new count. Every listener's reload() is called in order before any of them prepares. */
    public static final PreparableReloadListener LOAD_START = (barrier, manager, prepareProfiler, applyProfiler,
            background, game) -> {
        COUNT.set(0);
        loading = true;
        return barrier.wait(Unit.INSTANCE).thenRun(() -> {
        });
    };

    /** Stops counting. Listeners apply in order, so this runs after every listener before it. */
    public static final PreparableReloadListener LOAD_END = (barrier, manager, prepareProfiler, applyProfiler,
            background, game) -> barrier.wait(Unit.INSTANCE).thenRunAsync(() -> loading = false, game);

    private DataErrors() {
    }

    public static void init() {
        String modPackage = CreRaces.class.getPackageName();
        AbstractAppender counter = new AbstractAppender("creraces-data-errors", null, null, true, Property.EMPTY_ARRAY) {
            @Override
            public void append(LogEvent event) {
                if (loading && event.getLevel().isMoreSpecificThan(Level.ERROR)
                        && event.getLoggerName().startsWith(modPackage)) {
                    COUNT.incrementAndGet();
                }
            }
        };
        counter.start();
        ((Logger) LogManager.getRootLogger()).addAppender(counter);

        PlayerEvent.PLAYER_JOIN.register(DataErrors::tellOperator);
    }

    /** Errors logged during the last data load. */
    public static int count() {
        return COUNT.get();
    }

    private static void tellOperator(ServerPlayer player) {
        int errors = COUNT.get();
        if (errors > 0 && player.hasPermissions(2)) {
            player.sendSystemMessage(Component.translatable("msg.creraces.data_errors", errors)
                    .withStyle(ChatFormatting.RED));
        }
    }
}
