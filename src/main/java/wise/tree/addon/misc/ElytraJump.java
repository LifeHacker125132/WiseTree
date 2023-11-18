package wise.tree.addon.misc;

import meteordevelopment.meteorclient.events.entity.player.JumpVelocityMultiplierEvent;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class ElytraJump extends Module {
    public ElytraJump(Category category, String name) {
        super(category, name, "");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Double> multiplier = sgGeneral.add(new DoubleSetting.Builder().name("jump-factor").defaultValue(1).min(0).build());

    @EventHandler
    private void onJumpVelocityMultiplier(JumpVelocityMultiplierEvent event) {
        Iterable<ItemStack> armorPieces = mc.player.getArmorItems();
        for (ItemStack armorPiece : armorPieces) {
            if (isElytra(armorPiece) && mc.player.isFallFlying()) event.multiplier *= multiplier.get();
        }
    }

    private boolean isElytra(ItemStack itemStack) {
        if (itemStack == null) return false;
        return itemStack.getItem() == Items.ELYTRA;
    }
}
