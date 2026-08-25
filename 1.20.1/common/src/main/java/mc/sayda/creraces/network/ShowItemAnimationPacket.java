package mc.sayda.creraces.network;

import mc.sayda.creraces.CreRaces;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.function.Supplier;

/**
 * Packet to trigger the "totem-like" item activation animation on the client.
 */
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

    public void handle(Supplier<dev.architectury.networking.NetworkManager.PacketContext> contextSupplier) {
        displayAnimationOnClient(contextSupplier);
    }

    private void displayAnimationOnClient(Supplier<dev.architectury.networking.NetworkManager.PacketContext> contextSupplier) {
        var context = contextSupplier.get();
        context.queue(() -> {
            dev.architectury.utils.EnvExecutor.runInEnv(dev.architectury.utils.Env.CLIENT, () -> () -> {
                var level = mc.sayda.creraces.client.ClientAccess.getLevel();
                if (level != null) {
                    ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(this.itemId));
                    mc.sayda.creraces.client.ClientAccess.displayItemActivation(stack);
                }
            });
        });
    }
}
