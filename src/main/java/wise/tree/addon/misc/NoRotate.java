package wise.tree.addon.misc;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.PlayerPositionLookS2CPacketAccessor;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import wise.tree.addon.utils.Wrapper;

public class NoRotate extends Module implements Wrapper {
    public NoRotate(Category category, String name) {
        super(category, name, " ");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Boolean> always = sgGeneral.add(new BoolSetting.Builder().name("always").defaultValue(false).build());
    private final Setting<Boolean> air = sgGeneral.add(new BoolSetting.Builder().name("flying").defaultValue(true).visible(() -> !always.get()).build());
    private final Setting<Boolean> inBlock = sgGeneral.add(new BoolSetting.Builder().name("in-block").defaultValue(false).visible(() -> !always.get()).build());
    private boolean airS, alwaysS, inBlockS;

    @EventHandler
    private void onTickPost(TickEvent.Post event) {
        alwaysS = always.get() && !mc.player.isFallFlying() && bu.isAir(mc.player.getBlockPos());
        airS = air.get() && mc.player.isFallFlying();
        inBlockS = inBlock.get() && !bu.isAir(mc.player.getBlockPos());
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (event.packet instanceof PlayerPositionLookS2CPacket) {
            if (airS) {
                ((PlayerPositionLookS2CPacketAccessor) event.packet).setPitch(mc.player.getPitch());
                ((PlayerPositionLookS2CPacketAccessor) event.packet).setYaw(mc.player.getYaw());
            }
            if (alwaysS) {
                ((PlayerPositionLookS2CPacketAccessor) event.packet).setPitch(mc.player.getPitch());
                ((PlayerPositionLookS2CPacketAccessor) event.packet).setYaw(mc.player.getYaw());
            }
            if (inBlockS) {
                ((PlayerPositionLookS2CPacketAccessor) event.packet).setPitch(mc.player.getPitch());
                ((PlayerPositionLookS2CPacketAccessor) event.packet).setYaw(mc.player.getYaw());
            }
        }
    }
}
