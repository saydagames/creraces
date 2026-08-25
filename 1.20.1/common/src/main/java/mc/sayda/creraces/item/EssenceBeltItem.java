package mc.sayda.creraces.item;

import mc.sayda.creraces.ability.EssenceRegistry;
import mc.sayda.creraces.ability.EssenceType;
import mc.sayda.creraces.world.inventory.EssenceBeltMenu;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import dev.architectury.registry.menu.ExtendedMenuProvider;
import net.minecraft.network.FriendlyByteBuf;

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
        dev.architectury.registry.menu.MenuRegistry.openExtendedMenu(player, new ExtendedMenuProvider() {
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
        SimpleContainer inv = loadInventory(beltStack);
        int total = 0;
        for (int i = 0; i < SLOTS; i++) {
            ItemStack slot = inv.getItem(i);
            EssenceType slotType = EssenceRegistry.typeFromBottle(slot.getItem());
            if (slotType == type) {
                total += slot.getMaxDamage() - slot.getDamageValue();
            }
        }
        return total;
    }

    public boolean consumeEssence(ItemStack beltStack, EssenceType type, int amount) {
        SimpleContainer inv = loadInventory(beltStack);
        int remaining = amount;
        for (int i = 0; i < SLOTS && remaining > 0; i++) {
            ItemStack slot = inv.getItem(i);
            EssenceType slotType = EssenceRegistry.typeFromBottle(slot.getItem());
            if (slotType == type && slot.getItem() instanceof EssenceBottleItem) {
                int charges = slot.getMaxDamage() - slot.getDamageValue();
                if (charges <= remaining) {
                    remaining -= charges;
                    inv.setItem(i, new ItemStack(Items.GLASS_BOTTLE));
                } else {
                    slot.setDamageValue(slot.getDamageValue() + remaining);
                    remaining = 0;
                }
            }
        }
        if (remaining > 0) return false;
        saveInventory(beltStack, inv);
        return true;
    }
}
