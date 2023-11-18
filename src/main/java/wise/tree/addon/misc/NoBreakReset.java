package wise.tree.addon.misc;

import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import wise.tree.addon.events.ResetBlockRemovingEvent;
import wise.tree.addon.utils.Wrapper;

public class NoBreakReset extends Module implements Wrapper {
    public NoBreakReset(Category category, String name) {
        super(category, name, "");
    }

    @EventHandler
    private void onResetBlockRemovingEvent(ResetBlockRemovingEvent event) {
        event.setCancelled(true);
    }
}
