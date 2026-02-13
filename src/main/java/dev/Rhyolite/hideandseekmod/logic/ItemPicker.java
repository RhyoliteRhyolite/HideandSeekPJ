package dev.Rhyolite.hideandseekmod.logic;

import dev.Rhyolite.hideandseekmod.item.ITEMS;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import net.minecraft.network.chat.Component;
import java.util.List;
import java.util.Random;

public class ItemPicker {
    private static final Random RANDOM = new Random();

    // 술래 아이템 리스트
    private static final List<Item> SEEKER_ITEMS = List.of(
            ITEMS.TRAP_ITEM.get(),
            ITEMS.SEEKER_MARK.get()
    );

    // 도망자 아이템 리스트
    private static final List<Item> RUNNER_ITEMS = List.of(
            ITEMS.ENERGY_DRINK.get(),
            ITEMS.CHALK_ERASER.get(),
            ITEMS.VOODOO_DOLL.get(),
            ITEMS.FROST_SNOWBALL.get()
    );

    public static void giveRandomItem(Player player) {
        if (player.level().isClientSide) return;

        List<Item> lootTable = player.getTags().contains("seeker") ? SEEKER_ITEMS : RUNNER_ITEMS;
        String roleColor = player.getTags().contains("seeker") ? "§c" : "§b";

        Item selectedItem = lootTable.get(RANDOM.nextInt(lootTable.size()));

        // 아이템 지급 및 메시지
        player.getInventory().add(new ItemStack(selectedItem));
        player.displayClientMessage(Component.literal(roleColor + "아이템 획득: §f" + selectedItem.getDescription().getString()), false);
    }
}
