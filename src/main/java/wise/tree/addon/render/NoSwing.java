package wise.tree.addon.render;

import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import wise.tree.addon.events.SwingHandEvent;

public class NoSwing extends Module {
    public NoSwing(Category category, String name) {
        super(category, name, "");
    }

    @EventHandler
    private void onSwingHandEvent(SwingHandEvent event) {
        event.setCancelled(true);
    }
}
