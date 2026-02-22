package mc.sayda.creraces.capability;

import net.minecraft.world.entity.player.Player;
import java.util.Optional;

public interface ICapabilityHelper {
    Optional<IPlayerVariables> getVariables(Player player);
}
