package wise.tree.addon.events;

import meteordevelopment.meteorclient.events.Cancellable;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.item.ItemStack;

public class ItemRenderEvent extends Cancellable {
    public ItemStack stack;
    public Color color;

    public ItemRenderEvent(Color color) {
        this.color = color;
    }
}
