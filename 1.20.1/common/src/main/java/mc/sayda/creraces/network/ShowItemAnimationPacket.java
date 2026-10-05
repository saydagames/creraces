package mc.sayda.creraces.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.client.ClientAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

/** S2C: plays the totem-style item activation animation on the client. */
public class ShowItemAnimationPacket {
    public static final ResourceLocation ID = new ResourceLocation(CreRaces.MODID, "show_item_animation");

    private final ResourceLocation itemId;

    public ShowItemAnimationPacket(ResourceLocation itemId) {
        this.itemId = itemId;
    }

    public ShowItemAnimationPacket(FriendlyByteBuf buf) {
        this.itemId = buf.readResourceLocation();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(this.itemId);
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        var context = contextSupplier.get();
        context.queue(() -> {
            EnvExecutor.runInEnv(Env.CLIENT, () -> () -> {
                if (ClientAccess.getLevel() != null) {
                    ClientAccess.displayItemActivation(new ItemStack(BuiltInRegistries.ITEM.get(this.itemId)));
                }
            });
        });
    }
}
