package mc.sayda.creraces.engine.actions;

import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class OpenGUIAction implements ActionRegistry.RaceAction {

    private final String guiId;

    public OpenGUIAction(String guiId) {
        this.guiId = guiId;
    }

    @Override
    public boolean execute(Player player, @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        if (player instanceof ServerPlayer sp) {
            // Logic to open a specific GUI by ID
            // This would likely trigger a packet to the client
            CreRaces.LOGGER.info("Opening GUI {} for player {}", guiId, sp.getName().getString());
        }
        return true;
    }

    public static void register() {
        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "open_gui"), json -> {
            String gui = json.has("gui") ? json.get("gui").getAsString() : "race_selection";
            return new OpenGUIAction(gui);
        });
    }
}
