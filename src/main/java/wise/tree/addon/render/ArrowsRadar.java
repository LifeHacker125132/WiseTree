package wise.tree.addon.render;

import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;

public class ArrowsRadar extends Module {
    public ArrowsRadar(Category category, String name) {
        super(category, name, "");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Double> offset = sgGeneral.add(new DoubleSetting.Builder().name("range").defaultValue(10).min(0).sliderMax(100).build());


    @EventHandler
    private void onTick(TickEvent.Pre event) {

    }

    @EventHandler
    private void onTickEvent(Render2DEvent event) {
        Renderer2D.COLOR.begin();
        int _x = mc.getWindow().getScaledWidth();
        int _y = mc.getWindow().getScaledHeight();
        for (PlayerEntity entity : mc.world.getPlayers()) {
            if (entity == mc.player) continue;
            double yaw = Rotations.getYaw(entity);
            double rad = Math.toRadians(yaw);
            double cos = Math.cos(rad) * offset.get();
            double sin = Math.sin(rad) * offset.get();
            int x = (int) (RenderUtils.center.x + cos);
            int y = (int) (RenderUtils.center.y + sin);
            Renderer2D.COLOR.line(_x, _y, x, y, Color.WHITE);
        }
        Renderer2D.COLOR.render(null);
    }
}
