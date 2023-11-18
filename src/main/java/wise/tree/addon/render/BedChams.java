package wise.tree.addon.render;

import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import wise.tree.addon.utils.WTModule;

public class BedChams extends WTModule {
    public BedChams(Category category, String name) {
        super(category, name);
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    public final Setting<SettingColor> color = sgGeneral.add(new ColorSetting.Builder().name("color").build());

}
