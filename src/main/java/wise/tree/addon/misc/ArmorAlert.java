package wise.tree.addon.misc;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.MeteorToast;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import wise.tree.addon.utils.Wrapper;

public class ArmorAlert extends Module implements Wrapper {
    public ArmorAlert(Category category, String name) {
        super(category, name, "");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<AlertType> alertType = sgGeneral.add(new EnumSetting.Builder<AlertType>().name("alert-type").defaultValue(AlertType.Toast).build());
    private final Setting<Double> helmet = sgGeneral.add(new DoubleSetting.Builder().name("min-helmet").defaultValue(30).min(1).sliderMin(1).sliderMax(100).max(100).build());
    private final Setting<Double> chestplate = sgGeneral.add(new DoubleSetting.Builder().name("min-chestplate").defaultValue(20).min(1).sliderMin(1).sliderMax(100).max(100).build());
    private final Setting<Double> leggings = sgGeneral.add(new DoubleSetting.Builder().name("min-leggings").defaultValue(25).min(1).sliderMin(1).sliderMax(100).max(100).build());
    private final Setting<Double> boots = sgGeneral.add(new DoubleSetting.Builder().name("min-boots").defaultValue(20).min(1).sliderMin(1).sliderMax(100).max(100).build());

    private boolean alertedHelm;
    private boolean alertedChest;
    private boolean alertedLegs;
    private boolean alertedBoots;

    @Override
    public void onActivate() {
        alertedHelm = false;
        alertedChest = false;
        alertedLegs = false;
        alertedBoots = false;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        Iterable<ItemStack> armorPieces = mc.player.getArmorItems();
        for (ItemStack armorPiece : armorPieces) {
            if (pu.checkThreshold(armorPiece, helmet.get())) {
                if (pu.isHelm(armorPiece) && !alertedHelm) {
                    onAlert(alertType.get(), ArmorType.H,"Your helmet is low!");
                    alertedHelm = true;
                }
            }
            if (pu.checkThreshold(armorPiece, chestplate.get())) {
                if (pu.isChest(armorPiece) && !alertedChest) {
                    onAlert(alertType.get(), ArmorType.C, "Your chestplate is low!");
                    alertedChest = true;
                }
            }
            if (pu.checkThreshold(armorPiece, leggings.get())) {
                if (pu.isLegs(armorPiece) && !alertedLegs) {
                    onAlert(alertType.get(), ArmorType.L,"Your leggings are low!");
                    alertedLegs = true;
                }
            }
            if (pu.checkThreshold(armorPiece, boots.get())) {
                if (pu.isBoots(armorPiece) && !alertedBoots) {
                    onAlert(alertType.get(), ArmorType.B,"Your boots are low!");
                    alertedBoots = true;
                }
            }
            if (!pu.checkThreshold(armorPiece, helmet.get()))
                if (pu.isHelm(armorPiece) && alertedHelm) alertedHelm = false;
            if (!pu.checkThreshold(armorPiece, chestplate.get()))
                if (pu.isChest(armorPiece) && alertedChest) alertedChest = false;
            if (!pu.checkThreshold(armorPiece, leggings.get()))
                if (pu.isLegs(armorPiece) && alertedLegs) alertedLegs = false;
            if (!pu.checkThreshold(armorPiece, boots.get()))
                if (pu.isBoots(armorPiece) && alertedBoots) alertedBoots = false;
        }
    }

    private void onAlert(AlertType type, ArmorType armorType, String text) {
        if (type.equals(AlertType.Toast)) {
            mc.getToastManager().add(new MeteorToast(getItems(armorType), title, text));
        } else if (type.equals(AlertType.Message)) {
            info(text);
        } else {
            mc.getToastManager().add(new MeteorToast(getItems(armorType), title, text));
            info(text);
        }
    }

    private Item getItems(ArmorType type) {
        return switch (type) {
            case H -> mc.player.getInventory().getArmorStack(3).getItem();
            case C -> mc.player.getInventory().getArmorStack(2).getItem();
            case L -> mc.player.getInventory().getArmorStack(1).getItem();
            case B -> mc.player.getInventory().getArmorStack(0).getItem();
        };
    }

    public enum ArmorType {
        H,
        C,
        L,
        B
    }

    public enum AlertType {
        Toast,
        Message,
        Both
    }
}
