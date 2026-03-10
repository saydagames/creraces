package mc.sayda.creraces.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

/**
 * Generic Root Block.
 * Acts as an anchor for race-specific locations (e.g. Dryad's tree).
 * Indestructible by default. Interaction logic is handled via race traits
 * (JSON).
 */
public class RootBlock extends Block {

    public RootBlock(Properties properties) {
        super(properties);
    }

    public static Properties getDefaultProperties() {
        return Properties.of()
                .mapColor(MapColor.DIRT)
                .strength(-1.0f, 3600000.0f) // Indestructible
                .sound(SoundType.GRAVEL)
                .noLootTable();
    }

    public static boolean isOwner(net.minecraft.world.entity.player.Player player,
            net.minecraft.core.BlockPos pos) {
        return mc.sayda.creraces.capability.DataUtils.getVariables(player).map(vars -> {
            String tx = vars.getCustomization("tx");
            String ty = vars.getCustomization("ty");
            String tz = vars.getCustomization("tz");

            if (tx == null || ty == null || tz == null)
                return false;

            try {
                int ox = (int) Math.floor(Double.parseDouble(tx));
                int oy = (int) Math.floor(Double.parseDouble(ty));
                int oz = (int) Math.floor(Double.parseDouble(tz));

                return pos.getX() == ox && pos.getY() == oy && pos.getZ() == oz;
            } catch (NumberFormatException e) {
                return false;
            }
        }).orElse(false);
    }
}
