package wise.tree.addon.utils;

import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import net.minecraft.item.Items;

public class ItemUtils {
    public static FindItemResult getPiston() {
        return InvUtils.findInHotbar(Items.PISTON, Items.STICKY_PISTON);
    }

    public static FindItemResult getCrystal() {
        return InvUtils.findInHotbar(Items.END_CRYSTAL);
    }

    public static FindItemResult getPickaxe() {
        return InvUtils.findInHotbar(Items.DIAMOND_PICKAXE, Items.NETHERITE_PICKAXE);
    }

    public static FindItemResult getObby() {
        return InvUtils.find(Items.OBSIDIAN);
    }

    public static FindItemResult getRedstoneBlock() {
        return InvUtils.findInHotbar(Items.REDSTONE_BLOCK);
    }

    public static FindItemResult getCobweb() {
        return InvUtils.find(Items.COBWEB);
    }
    public static FindItemResult getTorch() {
        return InvUtils.find(Items.REDSTONE_TORCH);
    }
    public static FindItemResult getButton() {
        return InvUtils.find(Items.STONE_BUTTON, Items.OAK_BUTTON, Items.DARK_OAK_BUTTON, Items.JUNGLE_BUTTON, Items.SPRUCE_BUTTON, Items.BIRCH_BUTTON, Items.ACACIA_BUTTON, Items.CRIMSON_BUTTON, Items.WARPED_BUTTON, Items.MANGROVE_BUTTON, Items.POLISHED_BLACKSTONE_BUTTON);
    }

    public static FindItemResult getLever() {
        return InvUtils.find(Items.LEVER);
    }

    public static FindItemResult getShulker() {
        return InvUtils.find(Items.SHULKER_BOX,Items.RED_SHULKER_BOX,Items.BLUE_SHULKER_BOX,Items.GREEN_SHULKER_BOX,Items.MAGENTA_SHULKER_BOX,Items.PINK_SHULKER_BOX,Items.GRAY_SHULKER_BOX,Items.LIGHT_BLUE_SHULKER_BOX,Items.BLACK_SHULKER_BOX,Items.BROWN_SHULKER_BOX, Items.CYAN_SHULKER_BOX, Items.ORANGE_SHULKER_BOX, Items.LIME_SHULKER_BOX, Items.YELLOW_SHULKER_BOX, Items.LIGHT_GRAY_SHULKER_BOX, Items.WHITE_SHULKER_BOX, Items.PURPLE_SHULKER_BOX);
    }
}
