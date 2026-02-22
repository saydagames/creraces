package mc.sayda.creraces.network;

import dev.architectury.networking.NetworkManager;
import mc.sayda.creraces.registry.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.world.MenuProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import mc.sayda.creraces.world.inventory.MenuGUIMenu;
import mc.sayda.creraces.CreRaces;

import java.util.function.Supplier;

public class OpenMenuPacket {
    public static final ResourceLocation ID = new ResourceLocation(CreRaces.MODID, "open_menu");

    public OpenMenuPacket() {
    }

    public OpenMenuPacket(FriendlyByteBuf buf) {
    }

    public void encode(FriendlyByteBuf buf) {
    }

    public void handle(Supplier<NetworkManager.PacketContext> context) {
        context.get().queue(() -> {
            var player = context.get().getPlayer();
            if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
                MenuRegistry.openExtendedMenu(sp, new MenuProvider() {
                    @Override
                    public Component getDisplayName() {
                        return Component.translatable("gui.creraces.menu_gui");
                    }

                    @Override
                    public AbstractContainerMenu createMenu(int syncId, Inventory inventory, Player player) {
                        return new MenuGUIMenu(syncId, inventory, null);
                    }
                }, buf -> buf.writeBlockPos(sp.blockPosition()));
            }
        });
    }
}
