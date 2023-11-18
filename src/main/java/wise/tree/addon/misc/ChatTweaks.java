package wise.tree.addon.misc;

import meteordevelopment.meteorclient.events.game.SendMessageEvent;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import wise.tree.addon.utils.ChatUtils;

public class ChatTweaks extends Module {
    public ChatTweaks(Category category, String name) {
        super(category, name, "");
    }

    @EventHandler
    private void onSendMessage(SendMessageEvent event) {
        String m = event.message;
        event.message = ChatUtils.replacer(m);
    }
}
