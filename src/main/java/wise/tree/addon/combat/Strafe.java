package wise.tree.addon.combat;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import wise.tree.addon.utils.Wrapper;

public class Strafe extends Module implements Wrapper {
    public Strafe(Category category, String name) {
        super(category, name, "");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Boolean> speedBool = sgGeneral.add(new BoolSetting.Builder().name("speed").defaultValue(true).build());
    private final Setting<Double> speedVal = sgGeneral.add(new DoubleSetting.Builder().name("speed").defaultValue(5.5).sliderRange(0, 12).visible(speedBool::get).build());
    private final Setting<Boolean> sprint = sgGeneral.add(new BoolSetting.Builder().name("sprint").defaultValue(false).build());
    private final Setting<Boolean> lowHop = sgGeneral.add(new BoolSetting.Builder().name("low-hop").defaultValue(false).build());
    private final Setting<Double> height = sgGeneral.add(new DoubleSetting.Builder().name("height").defaultValue(0.4).sliderRange(0, 1).visible(lowHop::get).build());



    @EventHandler
    private void onTickPre(TickEvent.Pre event) {
        if (mc.player.input.movementForward != 0 || mc.player.input.movementSideways != 0) {
            if (sprint.get()) {
                mc.player.setSprinting(true);
            }

            if (mc.player.isOnGround() && lowHop.get()) mc.player.addVelocity(0, height.get(), 0);

            if (mc.player.isOnGround()) return;

            float speed;
            if (!speedBool.get()) speed = (float) Math.sqrt(mc.player.getVelocity().x * mc.player.getVelocity().x + mc.player.getVelocity().z * mc.player.getVelocity().z);
            else speed = speedVal.get().floatValue();

            float yaw = mc.player.getYaw();
            float forward = 1;

            if (mc.player.forwardSpeed < 0) {
                yaw += 180;
                forward = -0.5f;
            } else if (mc.player.forwardSpeed > 0) forward = 0.5f;

            if (mc.player.sidewaysSpeed > 0) yaw -= 90 * forward;
            if (mc.player.sidewaysSpeed < 0) yaw += 90 * forward;

            yaw = (float) Math.toRadians(yaw);

            mc.player.setVelocity(-Math.sin(yaw) * speed, mc.player.getVelocity().y, Math.cos(yaw) * speed);
        }
    }
}
