package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.territory.TerritoryManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;

import javax.annotation.Nullable;

/** Releases the caster's claim on the chunk of the interacted block (or the caster's own chunk). */
public class UnclaimTerritoryAction implements ActionRegistry.RaceAction {

    public static final ResourceLocation ID = new ResourceLocation(CreRaces.MODID, "unclaim_territory");

    private static final UnclaimTerritoryAction INSTANCE = new UnclaimTerritoryAction();

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        if (!(player instanceof ServerPlayer)) {
            return true;
        }
        ChunkPos chunk = new ChunkPos(interactPos != null ? interactPos : player.blockPosition());
        return TerritoryManager.get().unclaimOwnChunk(player.getUUID(), chunk);
    }

    public static void register() {
        ActionRegistry.register(ID, json -> INSTANCE);
    }
}
