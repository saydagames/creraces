package mc.sayda.creraces.network;

import dev.architectury.networking.NetworkManager;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.race.RaceIncidents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;

/**
 * Packet sent from client to server to modify player variables/states for debug
 * purposes.
 */
public class DebugActionPacket {
    public static final ResourceLocation ID = new ResourceLocation(CreRaces.MODID, "debug_action");

    private final String action;
    private final String key;
    private final String value;

    public DebugActionPacket(String action, String key, String value) {
        this.action = action;
        this.key = key;
        this.value = value;
    }

    public DebugActionPacket(FriendlyByteBuf buf) {
        this.action = java.util.Objects.requireNonNull(buf.readUtf(32));
        this.key = java.util.Objects.requireNonNull(buf.readUtf(256));
        this.value = java.util.Objects.requireNonNull(buf.readUtf(1024));
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(java.util.Objects.requireNonNull(action));
        buf.writeUtf(java.util.Objects.requireNonNull(key));
        buf.writeUtf(java.util.Objects.requireNonNull(value));
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();

        context.queue(() -> {
            // Permission check runs on the server thread to avoid TOCTOU race.
            if (!(context.getPlayer() instanceof ServerPlayer player) || !player.hasPermissions(2)) {
                return;
            }

            DataUtils.getVariables(player).ifPresent(vars -> {
                try {
                    switch (action) {
                        case "variable", "state" -> {
                            applyVariable(player, vars, java.util.Objects.requireNonNull(key),
                                    java.util.Objects.requireNonNull(value));
                            CreRaces.LOGGER.debug("Applied debug variable/state: {} = {}", key, value);
                        }
                        case "customization" -> {
                            vars.setCustomization(java.util.Objects.requireNonNull(key),
                                    java.util.Objects.requireNonNull(value));
                            CreRaces.LOGGER.debug("Applied debug customization: {} = {}", key, value);
                        }
                        case "ability_state" -> {
                            vars.setPersistentState(new ResourceLocation(java.util.Objects.requireNonNull(key)),
                                    Double.parseDouble(java.util.Objects.requireNonNull(value)));
                            CreRaces.LOGGER.debug("Applied debug ability state: {} = {}", key, value);
                        }
                        case "cooldown" -> {
                            vars.setCooldown(new ResourceLocation(java.util.Objects.requireNonNull(key)),
                                    (int) Double.parseDouble(java.util.Objects.requireNonNull(value)));
                            CreRaces.LOGGER.debug("Applied debug cooldown: {} = {}", key, value);
                        }
                        case "race" -> {
                            ResourceLocation id = new ResourceLocation(java.util.Objects.requireNonNull(value));
                            mc.sayda.creraces.race.RaceIncidents.transformPlayer(player, id);
                            CreRaces.LOGGER.debug("Applied debug race transformation: {}", value);
                        }
                        case "attribute" -> {
                            applyAttribute(player, key, value);
                            CreRaces.LOGGER.debug("Applied debug attribute: {} = {}", key, value);
                        }
                        case "flag" -> {
                            applyFlag(vars, key, value);
                            // Refresh to apply scale/attribute changes immediately
                            if (player instanceof net.minecraft.server.level.ServerPlayer) {
                                net.minecraft.server.level.ServerPlayer sp = (net.minecraft.server.level.ServerPlayer) player;
                                mc.sayda.creraces.race.RaceIncidents.refreshPlayer(sp);
                            }
                        }
                    }
                    // Sync changes back to client
                    if (!action.equals("race")) { // Race transformation already syncs
                        RaceIncidents.refreshPlayer(player);
                    }
                } catch (Exception e) {
                    CreRaces.LOGGER.error("Failed to apply debug action: {} {} {}", action, key, value, e);
                }
            });
        });
    }

    private void applyVariable(ServerPlayer player, IPlayerVariables vars, String key, String value) {
        if (key.equalsIgnoreCase("race")) {
            mc.sayda.creraces.race.RaceIncidents.transformPlayer(player,
                    new ResourceLocation(java.util.Objects.requireNonNull(value)));
            return;
        }

        double val = 0;
        try {
            val = Double.parseDouble(value);
        } catch (NumberFormatException e) {
            // Check for boolean strings
            if (value.equalsIgnoreCase("true"))
                val = 1.0;
            else if (value.equalsIgnoreCase("false"))
                val = 0.0;
            else
                return;
        }

        switch (key.toLowerCase()) {
            case "mana" -> vars.setMana(val);
            case "energy" -> vars.setEnergy(val);
            case "grit" -> vars.setGrit(val);
            case "rage" -> vars.setRage(val);
            case "karma" -> vars.setKarma(val);
            case "ap" -> vars.setAp(val);
            case "ad" -> vars.setAd(val);
            case "ah" -> vars.setAh(val);
            case "cr" -> vars.setCr(val);
            case "coins" -> vars.setCoins(val);
            case "soul" -> vars.setSoul(val);
            case "gstate" -> vars.setGState((int) val);
            case "returndim" -> vars.setReturnDim(value);
            case "pocketx" -> vars.setPocketX(val);
            case "pockety" -> vars.setPocketY(val);
            case "pocketz" -> vars.setPocketZ(val);
            case "pocketsize" -> vars.setPocketSize(val);
        }
    }

    private void applyFlag(IPlayerVariables vars, String key, String value) {
        boolean val = value.equalsIgnoreCase("true") || value.equals("1") || value.equals("1.0");
        switch (key) {
            case "isUndead" -> vars.setUndead(val);
            case "isAquatic" -> vars.setAquatic(val);
            case "isSpirit" -> vars.setSpirit(val);
            case "isTiny" -> vars.setTiny(val);
            case "inSpirit" -> vars.setInSpiritRealm(val);
            case "morphed" -> vars.setMorphed(val);
            case "smallBuild" -> vars.setSmallBuild(val);
        }
    }

    private void applyAttribute(ServerPlayer player, String attrId, String value) {
        try {
            double val = Double.parseDouble(value);
            ResourceLocation id = new ResourceLocation(java.util.Objects.requireNonNull(attrId));
            net.minecraft.world.entity.ai.attributes.Attribute attr = net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE
                    .get(id);
            if (attr != null) {
                net.minecraft.world.entity.ai.attributes.AttributeInstance instance = player.getAttribute(attr);
                if (instance != null) {
                    instance.setBaseValue(val);
                }
            }
        } catch (Exception e) {
            CreRaces.LOGGER.error("Failed to apply attribute debug: {} {}", attrId, value, e);
        }
    }
}
