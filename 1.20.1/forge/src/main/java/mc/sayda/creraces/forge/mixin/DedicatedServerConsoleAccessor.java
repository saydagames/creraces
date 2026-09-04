package mc.sayda.creraces.forge.mixin;

import net.minecraft.server.ConsoleInput;
import net.minecraft.server.dedicated.DedicatedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

/**
 * Read-only accessor for DedicatedServer's own console-input queue, the same
 * (synchronized) list its "Server console handler" background thread already safely fills via
 * handleConsoleInput. Deliberately an @Accessor, not an @Inject: it exposes a field without
 * changing any method's behavior, so it can't conflict with another mod's mixin on the same
 * method or alter what vanilla (or any other mod) does with console input.
 */
@Mixin(DedicatedServer.class)
public interface DedicatedServerConsoleAccessor {
    @Accessor("consoleInput")
    List<ConsoleInput> creraces$getConsoleInput();
}
