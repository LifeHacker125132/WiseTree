package wise.tree.addon.combat;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.BowItem;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.math.BlockPos;
import wise.tree.addon.utils.WTModule;

public class BowRelease extends WTModule {
    public BowRelease(Category category, String name) {
        super(category, name);
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder().name("delay").defaultValue(3).sliderRange(0, 20).build());

    @EventHandler
    private void onTickEvent(TickEvent.Pre event) {
        if (mc.player.getMainHandStack().getItem() instanceof BowItem && mc.player.isUsingItem() && mc.player.getItemUseTime() >= delay.get()) {
            mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, mc.player.getHorizontalFacing()));
            mc.player.networkHandler.sendPacket(new PlayerInteractItemC2SPacket(mc.player.getActiveHand(), 0));
            mc.player.stopUsingItem();
        }
    }
}
