package mc.sayda.creraces.network;

import dev.architectury.networking.NetworkManager;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilityIncidents;
import mc.sayda.creraces.ability.AbilitySlot;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;

/** C2S: casts whatever ability is equipped in a slot. */
public class CastAbilityPacket {
    public static final ResourceLocation ID = new ResourceLocation(CreRaces.MODID, "cast_ability");

    private final AbilitySlot slot;

    public CastAbilityPacket(AbilitySlot slot) {
        this.slot = slot;
    }

    public CastAbilityPacket(FriendlyByteBuf buf) {
        this.slot = buf.readEnum(AbilitySlot.class);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(this.slot);
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();
        context.queue(() -> {
            if (context.getPlayer() instanceof ServerPlayer sp) {
                AbilityIncidents.tryCast(sp, slot);
            }
        });
    }
}
