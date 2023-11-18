package wise.tree.addon.render;

import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;

public class HUE extends Module {
    public HUE(Category category, String name) {
        super(category, name, "");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<SettingColor> hue = sgGeneral.add(new ColorSetting.Builder().name("color").build());

    @EventHandler
    private void onTickEvent(Render2DEvent event) {
        Renderer2D.COLOR.begin();
        Renderer2D.COLOR.quad(0, 0, mc.getWindow().getWidth(), mc.getWindow().getHeight(), new Color(Color.fromRGBA(hue.get().r, hue.get().g, hue.get().b, hue.get().a)));
        Renderer2D.COLOR.render(null);
    }
}
