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
        this.action = java.util.Objects.requireNonNull(buf.readUtf());
        this.key = java.util.Objects.requireNonNull(buf.readUtf());
        this.value = java.util.Objects.requireNonNull(buf.readUtf());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(java.util.Objects.requireNonNull(action));
        buf.writeUtf(java.util.Objects.requireNonNull(key));
        buf.writeUtf(java.util.Objects.requireNonNull(value));
    }


    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();
        ServerPlayer player = (ServerPlayer) context.getPlayer();

        if (!player.hasPermissions(2)) {
            return;
        }

        context.queue(() -> {
            DataUtils.getVariables(player).ifPresent(vars -> {
                try {
                    switch (action) {
                        case "variable", "state" -> applyVariable(player, vars, key, value);
                        case "ability_state" -> vars.setPersistentState(new ResourceLocation(key), Double.parseDouble(value));
                        case "cooldown" -> vars.setCooldown(new ResourceLocation(key), (int) Double.parseDouble(value));
                        case "race" -> {
                            ResourceLocation id = new ResourceLocation(value);
                            mc.sayda.creraces.race.RaceIncidents.transformPlayer(player, id);
                        }
                        case "attribute" -> applyAttribute(player, key, value);
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
             mc.sayda.creraces.race.RaceIncidents.transformPlayer(player, new ResourceLocation(value));
             return;
        }

        double val = 0;
        try {
            val = Double.parseDouble(value);
        } catch (NumberFormatException e) {
            // Check for boolean strings
            if (value.equalsIgnoreCase("true")) val = 1.0;
            else if (value.equalsIgnoreCase("false")) val = 0.0;
            else return;
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
            case "morphed" -> vars.setMorphed(val > 0.5);
            case "gstate" -> vars.setGState((int) val);
            case "smallbuild" -> vars.setSmallBuild(val > 0.5);
            case "spirit" -> vars.setInSpiritRealm(val > 0.5);
            case "returndim" -> vars.setReturnDim(value);
        }
    }

    private void applyAttribute(ServerPlayer player, String attrId, String value) {
        try {
            double val = Double.parseDouble(value);
            ResourceLocation id = new ResourceLocation(attrId);
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
