package wise.tree.addon.misc;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import wise.tree.addon.utils.ItemUtils;
import wise.tree.addon.utils.Wrapper;

public class FrameDupe extends Module implements Wrapper {
    public FrameDupe(Category category, String name) {
        super(category, name, "");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder().name("delay").defaultValue(1).min(0).sliderMax(50).build());
    private final Setting<Boolean> packet = sgGeneral.add(new BoolSetting.Builder().name("packet").defaultValue(false).build());
    private final Setting<Boolean> swing = sgGeneral.add(new BoolSetting.Builder().name("swing").defaultValue(false).build());
    private int timer;

    @Override
    public void onActivate() {
        timer = delay.get();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        for (Entity entity : mc.world.getEntities()) {
            FindItemResult shulker = ItemUtils.getShulker();
            if (!shulker.found()) toggle();
            if (!shulker.isHotbar()) toggle();
            if (!(entity instanceof ItemFrameEntity)) continue;
            if (PlayerUtils.distanceTo(entity) > 3.0) continue;
            int slot = shulker.slot();
            if (mc.player.getInventory().selectedSlot != slot) InvUtils.swap(slot, false);
            if (swing.get()) mc.player.swingHand(Hand.MAIN_HAND);
            if (timer <= 0) {
                if (packet.get()) {
                    mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(entity, false));
                } else {
                    mc.interactionManager.attackEntity(mc.player, entity);
                }
                if (swing.get()) mc.player.swingHand(Hand.MAIN_HAND);
                timer = delay.get();
            } else {
                mc.interactionManager.interactEntity(mc.player, entity, Hand.MAIN_HAND);
                timer--;
            }
        }
    }
}
