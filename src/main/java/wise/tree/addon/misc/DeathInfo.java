package wise.tree.addon.misc;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.HealthUpdateS2CPacket;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import wise.tree.addon.utils.Wrapper;

public class DeathInfo extends Module implements Wrapper {
    public DeathInfo(Category category, String name) {
        super(category, name, " ");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Boolean> coordinate = sgGeneral.add(new BoolSetting.Builder().name("pos").defaultValue(false).build());
    private final Setting<Boolean> totemCount = sgGeneral.add(new BoolSetting.Builder().name("totem").defaultValue(true).build());
    private final Setting<Boolean> currentPing = sgGeneral.add(new BoolSetting.Builder().name("ping").defaultValue(true).build());


    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (event.packet instanceof HealthUpdateS2CPacket packet) {
            if (packet.getHealth() <= 0) onDeath();
        }
    }

    private void onDeath() {
        FindItemResult totem = InvUtils.find(Items.TOTEM_OF_UNDYING);
        BlockPos pos = mc.player.getBlockPos();
        int ping = PlayerUtils.getPing();
        String str = Formatting.AQUA + (coordinate.get() ? "XYZ: " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "; " : "") +
            (totemCount.get() ? "totem" + (totem.count() > 1 ? "s: " : ": ") + totem.count() + "; " : "") +
            (currentPing.get() ? "ping: " + ping : "");
        info(str);
    }
}
