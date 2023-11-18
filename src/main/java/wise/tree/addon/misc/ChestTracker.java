package wise.tree.addon.misc;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.ChestMinecartEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class ChestTracker extends Module {
    public ChestTracker(Category category, String name) {
        super(category, name, "");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof ChestMinecartEntity cm)) continue;
            cm.generateInventoryLoot(mc.player);
            for (int i = 0; i < 27; i++) {
                ItemStack s = cm.getInventoryStack(i);
                if (s.getItem().equals(Items.TRIPWIRE_HOOK)) {
                    info("unik find!");
                }
                if (s.getItem().equals(Items.GOLDEN_APPLE)) {
                    info("gapl find!");
                }
            }
            cm.getInventory().forEach(invt -> {
                if (invt.getItem().equals(Items.TRIPWIRE_HOOK)) {
                    info("unik find!");
                }
                if (invt.getItem().equals(Items.GOLDEN_APPLE)) {
                    info("gapl find!");
                }
            });
        }
    }
}
