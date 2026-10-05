package mc.sayda.creraces.engine.actions;

import com.mojang.datafixers.util.Either;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/** Puts the caster to sleep where they stand, without a bed, optionally setting their spawn there first. */
public class SleepAction implements ActionRegistry.RaceAction {
    public static final ResourceLocation ID = new ResourceLocation(CreRaces.MODID, "sleep");

    /** Tells the sleep mixins to skip their bed checks for this sleep; removed again when the player wakes. */
    private static final String FORCE_SLEEP_TAG = "creraces_force_sleep";

    private final boolean setSpawn;

    public SleepAction(boolean setSpawn) {
        this.setSpawn = setSpawn;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        if (player.level().isClientSide()) {
            return true;
        }

        BlockPos pos = player.blockPosition();
        // Set before tagging: the mixins also cancel respawn changes while the tag is present.
        if (setSpawn && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.setRespawnPosition(player.level().dimension(), pos, player.getYRot(), true, true);
        }

        player.addTag(FORCE_SLEEP_TAG);
        Either<Player.BedSleepingProblem, Unit> result = player.startSleepInBed(pos);
        if (result.left().isPresent()) {
            // No sleep started, so nothing will ever clear the tag if it stays.
            player.removeTag(FORCE_SLEEP_TAG);
            Component message = result.left().get().getMessage();
            if (message != null) {
                player.displayClientMessage(message, true);
            }
            return false;
        }
        return true;
    }

    public static void register() {
        ActionRegistry.register(ID, json -> new SleepAction(GsonHelper.getAsBoolean(json, "set_spawn", false)));
    }
}
