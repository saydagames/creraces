package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Opens a nearby inventory or crafting surface on behalf of the player.
 *
 * Supported {@code gui} values (set in ability JSON):
 * <ul>
 * <li>{@code "crafting_table"} — opens the first CraftingTable within
 * reach</li>
 * <li>{@code "inventory"} — opens a container BlockEntity within reach
 * (chests, barrels, furnaces, etc.)</li>
 * <li>{@code "race_selection"} — opens the race selection screen (default)</li>
 * <li>{@code "skill_wheel"} — opens the skill wheel screen</li>
 * <li>{@code "ability_menu"} — opens the in-game ability menu</li>
 * </ul>
 *
 * The scan radius is controlled by the {@code "radius"} JSON field (default 4).
 */
public class OpenGUIAction implements ActionRegistry.RaceAction {

    private final String guiId;
    /** Max taxicab distance to scan for a matching block. */
    private final int radius;

    public OpenGUIAction(String guiId, int radius) {
        this.guiId = guiId;
        this.radius = Math.max(1, radius);
    }

    @Override
    public boolean execute(Player player,
            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable BlockPos interactionPos) {

        if (!(player instanceof ServerPlayer sp))
            return false;

        return switch (guiId) {
            case "crafting_table" -> openNearestCraftingTable(sp);
            case "inventory" -> openNearestContainer(sp);
            case "race_selection", "race_menu" -> {
                mc.sayda.creraces.network.BoundaryHandler.sendOpenSelection(sp);
                yield true;
            }
            case "skill_wheel" -> {
                mc.sayda.creraces.network.BoundaryHandler.sendOpenSkillWheel(sp);
                yield true;
            }
            case "team_menu" -> {
                mc.sayda.creraces.network.BoundaryHandler.sendOpenTeamGUI(sp);
                yield true;
            }
            case "mirror" -> {
                mc.sayda.creraces.network.BoundaryHandler.sendOpenMirror(sp);
                yield true;
            }
            case "debug" -> {
                mc.sayda.creraces.network.BoundaryHandler.sendOpenDebug(sp);
                yield true;
            }
            default -> {
                CreRaces.LOGGER.warn("[OpenGUIAction] Unknown gui id '{}' — no screen opened.", guiId);
                yield false;
            }
        };
    }

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Opens a full 3×3 crafting grid for the player without requiring a nearby
     * CraftingTable block — equivalent to right-clicking a crafting table but
     * available anywhere.
     */
    private boolean openNearestCraftingTable(ServerPlayer player) {
        player.openMenu(new net.minecraft.world.MenuProvider() {
            @Override
            public net.minecraft.network.chat.Component getDisplayName() {
                return net.minecraft.network.chat.Component.translatable("container.crafting");
            }

            @Override
            public net.minecraft.world.inventory.AbstractContainerMenu createMenu(
                    int syncId,
                    @javax.annotation.Nonnull net.minecraft.world.entity.player.Inventory inv,
                    @javax.annotation.Nonnull net.minecraft.world.entity.player.Player p) {
                return new net.minecraft.world.inventory.CraftingMenu(syncId, inv);
            }
        });
        return true;
    }

    /**
     * Scans nearby blocks for a container {@link BlockEntity} and opens it.
     */
    private boolean openNearestContainer(ServerPlayer player) {
        Level level = player.level();
        BlockPos origin = player.blockPosition();

        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;

        for (BlockPos pos : BlockPos.betweenClosed(
                origin.offset(-radius, -radius, -radius),
                origin.offset(radius, radius, radius))) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof MenuProvider) {
                double dist = pos.distSqr(origin);
                if (dist < bestDist) {
                    bestDist = dist;
                    best = pos.immutable();
                }
            }
        }

        if (best == null) {
            CreRaces.LOGGER.debug("[OpenGUIAction] No container found within {} blocks of {}.", radius,
                    player.getName().getString());
            return false;
        }

        BlockEntity be = level.getBlockEntity(best);
        if (be instanceof MenuProvider mp) {
            player.openMenu(mp);
            return true;
        }
        return false;
    }

    // ─────────────────────────────────────────────────────────────────────────

    public static void register() {
        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "open_gui"), json -> {
            String gui = json.has("gui") ? json.get("gui").getAsString() : "race_selection";
            int radius = json.has("radius") ? json.get("radius").getAsInt() : 4;
            return new OpenGUIAction(gui, radius);
        });
    }
}
