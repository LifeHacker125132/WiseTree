package wise.tree.addon.misc;

import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import wise.tree.addon.events.InteractEvent;
import wise.tree.addon.utils.Wrapper;

public class MultiTask extends Module implements Wrapper {
    public MultiTask(Category category, String name) {
        super(category, name, "");
    }

    @EventHandler
    public void onInteractEvent(InteractEvent event){
        event.usingItem = false;
    }
}
