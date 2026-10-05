package mc.sayda.creraces.item;

import dev.architectury.registry.menu.ExtendedMenuProvider;
import dev.architectury.registry.menu.MenuRegistry;
import mc.sayda.creraces.ability.EssenceType;
import mc.sayda.creraces.util.EssenceBeltHelper;
import mc.sayda.creraces.world.inventory.EssenceBeltMenu;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class EssenceBeltItem extends Item {

    public static final int SLOTS = 8;
    private static final String NBT_KEY = "EssenceInventory";

    public EssenceBeltItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            openBeltMenu(sp);
        }
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }

    public static void openBeltMenu(ServerPlayer player) {
        ItemStack belt = EssenceBeltMenu.findBeltStack(player);
        if (belt == null) return;
        SimpleContainer beltInv = loadInventory(belt);
        MenuRegistry.openExtendedMenu(player, new ExtendedMenuProvider() {
            @Override
            public void saveExtraData(FriendlyByteBuf buf) {}
            @Override
            public Component getDisplayName() {
                return Component.translatable("container.creraces.essence_belt");
            }
            @Override
            public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player p) {
                return new EssenceBeltMenu(syncId, inv, beltInv);
            }
        });
    }

    public static SimpleContainer loadInventory(ItemStack stack) {
        SimpleContainer inv = new SimpleContainer(SLOTS);
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(NBT_KEY, Tag.TAG_LIST)) {
            ListTag list = tag.getList(NBT_KEY, Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size() && i < SLOTS; i++) {
                CompoundTag slotTag = list.getCompound(i);
                int slot = slotTag.getByte("Slot") & 0xFF;
                if (slot < SLOTS) {
                    inv.setItem(slot, ItemStack.of(slotTag));
                }
            }
        }
        return inv;
    }

    public static void saveInventory(ItemStack stack, SimpleContainer inv) {
        ListTag list = new ListTag();
        for (int i = 0; i < SLOTS; i++) {
            ItemStack slotStack = inv.getItem(i);
            if (!slotStack.isEmpty()) {
                CompoundTag slotTag = slotStack.save(new CompoundTag());
                slotTag.putByte("Slot", (byte) i);
                list.add(slotTag);
            }
        }
        stack.getOrCreateTag().put(NBT_KEY, list);
    }

    public int getEssenceCount(ItemStack beltStack, EssenceType type) {
        return EssenceBeltHelper.countInContainer(loadInventory(beltStack), type);
    }

    /** Drains {@code amount} charges from the belt's bottles. Leaves the belt untouched and returns false if it holds too few. */
    public boolean consumeEssence(ItemStack beltStack, EssenceType type, int amount) {
        SimpleContainer inv = loadInventory(beltStack);
        if (EssenceBeltHelper.drainFromContainer(inv, type, amount) > 0) return false;
        saveInventory(beltStack, inv);
        return true;
    }
}
