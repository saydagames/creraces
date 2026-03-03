package mc.sayda.creraces.network;

import dev.architectury.networking.NetworkManager;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.capability.DataUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

public class EquipAbilityPacket {
    public static final ResourceLocation ID = new ResourceLocation(CreRaces.MODID, "equip_ability");

    private final AbilitySlot slot;
    private final ResourceLocation abilityId;

    public EquipAbilityPacket(AbilitySlot slot, ResourceLocation abilityId) {
        this.slot = slot;
        this.abilityId = abilityId;
    }

    public EquipAbilityPacket(FriendlyByteBuf buf) {
        this.slot = buf.readEnum(AbilitySlot.class);
        if (buf.readBoolean()) {
            this.abilityId = buf.readResourceLocation();
        } else {
            this.abilityId = null;
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(slot);
        if (abilityId != null) {
            buf.writeBoolean(true);
            buf.writeResourceLocation(abilityId);
        } else {
            buf.writeBoolean(false);
        }
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();
        context.queue(() -> {
            DataUtils.getVariables(context.getPlayer()).ifPresent(vars -> {
                // Ownership check: null = unequip (always allowed); non-null must be unlocked
                if (abilityId != null && !vars.isAbilityUnlocked(abilityId)) {
                    CreRaces.LOGGER.warn("Player {} tried to equip unowned ability: {}",
                            context.getPlayer().getName().getString(), abilityId);
                    return;
                }
                vars.equipAbility(slot, abilityId);
                // Sync back to client (and others)
                BoundaryHandler.resyncVariables(context.getPlayer(), context.getPlayer());
            });
        });
    }
}
