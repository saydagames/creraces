package mc.sayda.creraces.engine.actions;

import com.google.gson.JsonObject;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.territory.TerritoryManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;

public class UnclaimTerritoryAction implements ActionRegistry.RaceAction {

    public static final ResourceLocation ID = new ResourceLocation("creraces", "unclaim_territory");

    private static final UnclaimTerritoryAction INSTANCE = new UnclaimTerritoryAction();

    @Override
    public boolean execute(Player player,
            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable BlockPos interact_pos) {

        if (!(player instanceof ServerPlayer)) return true;

        ChunkPos chunk = interact_pos != null
                ? new ChunkPos(interact_pos)
                : new ChunkPos(player.blockPosition());

        return TerritoryManager.get().unclaimOwnChunk(player.getUUID(), chunk);
    }

    public static void register() {
        ActionRegistry.register(ID, (JsonObject json) -> INSTANCE);
    }
}
