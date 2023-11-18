package wise.tree.addon.misc;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.util.math.MathHelper;

public class ElytraFly extends Module {
    public ElytraFly(Category category, String name) {
        super(category, name, "");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Double> fly = sgGeneral.add(new DoubleSetting.Builder().name("boost-factor").sliderRange(0, 1).defaultValue(0).build());
    private final Setting<Boolean> takeOff = sgGeneral.add(new BoolSetting.Builder().name("take-off").defaultValue(false).build());

    @EventHandler
    private void onPostTick(TickEvent.Pre event) {
        if(!mc.player.isFallFlying()) {
            if(takeOff.get() && !mc.player.isOnGround() && mc.options.jumpKey.isPressed())
                mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
            return;
        }
        if (mc.player.getAbilities().flying) {
            mc.player.getAbilities().flying = false;
        }
        float yaw = (float) Math.toRadians(mc.player.getBodyYaw());
        if (mc.options.forwardKey.isPressed()) {
            mc.player.addVelocity(-MathHelper.sin(yaw) * fly.get().floatValue() / 10, 0, MathHelper.cos(yaw) * fly.get().floatValue() / 10);
        }
    }
}
