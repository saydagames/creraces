package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class SleepAction implements ActionRegistry.RaceAction {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "sleep");
    private final boolean setSpawn;

    public SleepAction(boolean setSpawn) {
        this.setSpawn = setSpawn;
    }

    @Override
    public boolean execute(Player player, @javax.annotation.Nullable LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interact_pos) {
        if (player.level().isClientSide)
            return true;

        net.minecraft.core.BlockPos pos = player.blockPosition();
        if (pos == null) return false;

        if (setSpawn && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            serverPlayer.setRespawnPosition(player.level().dimension(), pos, player.getYRot(), true,
                    true);
        }

        // Apply a custom tag to signal to our LivingEntitySleepMixin
        // that this is a forced, custom sleep that should not require a BedBlock.
        player.addTag("creraces_force_sleep");

        // Try to sleep at current position.
        com.mojang.datafixers.util.Either<net.minecraft.world.entity.player.Player.BedSleepingProblem, net.minecraft.util.Unit> result = player.startSleepInBed(pos);
        if (result.left().isPresent()) {
            net.minecraft.world.entity.player.Player.BedSleepingProblem problem = result.left().get();
            net.minecraft.network.chat.Component msg = problem.getMessage();
            if (msg != null) {
                player.displayClientMessage(msg, true);
            }
            return false;
        }
        return true;
    }

    public static void register() {
        ActionRegistry.register(ID,
                json -> new SleepAction(mc.sayda.creraces.util.GsonHelper.getAsBoolean(json, "set_spawn", false)));
    }
}
