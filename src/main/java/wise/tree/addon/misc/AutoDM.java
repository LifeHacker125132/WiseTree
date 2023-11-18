package wise.tree.addon.misc;

import meteordevelopment.meteorclient.events.game.SendMessageEvent;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

public class AutoDM extends Module {
    public AutoDM(Category category, String name) {
        super(category, name, "");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<String> msg = sgGeneral.add(new StringSetting.Builder().name("dm").defaultValue("/msg __aaa__").build());

    @EventHandler
    private void onSendMessage(SendMessageEvent event) {
        String prefix = msg.get();
        String message = event.message;
        event.message = prefix + " " + message;
    }
}
