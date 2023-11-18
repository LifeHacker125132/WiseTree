package wise.tree.addon.combat;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Direction;
import wise.tree.addon.utils.ItemUtils;
import wise.tree.addon.utils.PlaceUtils;
import wise.tree.addon.utils.Wrapper;

public class AntiAnchor extends Module implements Wrapper {
    public AntiAnchor(Category category, String name) {
        super(category, name, "");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Boolean> place = sgGeneral.add(new BoolSetting.Builder().name("place-top").defaultValue(true).build());

    @EventHandler
    private void onTickEvent(TickEvent.Post event) {
        if (bu.getBlock(mc.player.getBlockPos().up(2)) == Blocks.RESPAWN_ANCHOR) {
            int c = mc.world.getBlockState(mc.player.getBlockPos().up(2)).get(RespawnAnchorBlock.CHARGES);
            if (c == 0) return;
            int prev = mc.player.getInventory().selectedSlot;
            if (mc.player.getMainHandStack().getItem().equals(Items.GLOWSTONE)) {
                InvUtils.swap(mc.player.getInventory().selectedSlot == 8 ? 0 : 8, false);
            }
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(
                vu.getVec3d(mc.player.getBlockPos().up(2)), Direction.DOWN,
                mc.player.getBlockPos().up(2), false));
            FindItemResult obby = ItemUtils.getObby();
            if (place.get() && obby.found() && obby.isHotbar()) {
                PlaceUtils.place(mc.player.getBlockPos().up(2), obby, true, false);
            }
            if (prev != mc.player.getInventory().selectedSlot) InvUtils.swap(prev, false);
        }
    }

}
