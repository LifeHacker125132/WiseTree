package wise.tree.addon.render;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

public class CustomFov extends Module {
    public CustomFov(Category category, String name){
        super(category, name, "") ;
    }

    private final SettingGroup general = settings.getDefaultGroup();
    private final Setting<Integer> fov = general.add(new IntSetting.Builder().name("fov").defaultValue(100).sliderRange(0, 999).noSlider().build());

    private int oldF;

    @Override
    public void onActivate() {
        oldF = mc.options.getFov().getValue();
    }

    @Override
    public void onDeactivate() {
        mc.options.getFov().setValue(oldF);
    }

    @EventHandler
    private void onTickEvent(TickEvent.Post event) {
        mc.options.getFov().setValue(fov.get());
    }
}
