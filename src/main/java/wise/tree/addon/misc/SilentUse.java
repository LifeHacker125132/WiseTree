package wise.tree.addon.misc;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import wise.tree.addon.utils.Wrapper;

public class SilentUse extends Module implements Wrapper {

    public SilentUse(Category category, String name) {
        super(category, name, "");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>().name("mode").defaultValue(Mode.Any).build());

    @EventHandler
    private void onTickEvent(TickEvent.Post event) {
        FindItemResult item = null;
        switch (mode.get()) {
            case Any -> item = InvUtils.find(Items.ENDER_PEARL, Items.FIREWORK_ROCKET);
            case Pearl -> item = InvUtils.find(Items.ENDER_PEARL);
            case Rocket -> item = InvUtils.find(Items.FIREWORK_ROCKET);
        }
        if (item.found()) {
            onUse(item);
        }
        toggle();
    }

    private void onUse(FindItemResult i) {
        if (i.isHotbar()) {
            int prev = mc.player.getInventory().selectedSlot;
            InvUtils.swap(i.slot(), false);
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            InvUtils.swap(prev, false);
        } else {
            int main = mc.player.getInventory().selectedSlot;
            int it = i.slot();
            InvUtils.move().from(main).to(it);
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            InvUtils.move().from(it).to(main);
        }
    }



    public enum Mode {
        Any,
        Rocket,
        Pearl
    }
}
