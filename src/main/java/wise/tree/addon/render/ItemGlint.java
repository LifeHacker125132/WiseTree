package wise.tree.addon.render;

import meteordevelopment.meteorclient.settings.ItemListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import net.minecraft.item.Item;
import wise.tree.addon.utils.WTModule;

import java.util.List;

public class ItemGlint extends WTModule {
    public ItemGlint(Category category, String name) {
        super(category, name);
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    public final Setting<List<Item>> items = sgGeneral.add(new ItemListSetting.Builder().name("items").build());
}
