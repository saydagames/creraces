package mc.sayda.creraces.engine.actions;

import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class PlaceBlockAction implements ActionRegistry.RaceAction {

    private final String blockId;
    private final boolean atTarget;

    public PlaceBlockAction(String blockId, boolean atTarget) {
        this.blockId = blockId;
        this.atTarget = atTarget;
    }

    @Override
    public void execute(Player player, @javax.annotation.Nullable LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot) {
        LivingEntity entity = atTarget && target != null ? target : player;
        BlockPos pos = entity.blockPosition();

        Block block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(new ResourceLocation(blockId));
        if (block != Blocks.AIR) {
            player.level().setBlockAndUpdate(pos, block.defaultBlockState());
        }
    }

    public static void register() {
        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "place_block"), json -> {
            String block = json.has("block") ? json.get("block").getAsString() : "minecraft:stone";
            boolean atTarget = GsonHelper.getAsBoolean(json, "use_target", false);
            return new PlaceBlockAction(block, atTarget);
        });
    }
}
