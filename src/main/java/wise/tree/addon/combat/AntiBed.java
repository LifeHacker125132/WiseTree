package wise.tree.addon.combat;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BedBlock;
import net.minecraft.item.AxeItem;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import wise.tree.addon.utils.Wrapper;

public class AntiBed extends Module implements Wrapper {
    public AntiBed(Category category, String name) {
        super(category, name, "");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<DestroyMode> mode = sgGeneral.add(new EnumSetting.Builder<DestroyMode>().name("mode").defaultValue(DestroyMode.Break).build());
    private final Setting<Boolean> useAxe = sgGeneral.add(new BoolSetting.Builder().name("use-axe").defaultValue(false).visible(() -> mode.get() == DestroyMode.Break).build());
    private final Setting<Integer> height = sgGeneral.add(new IntSetting.Builder().name("height").defaultValue(3).sliderRange(1, 5).min(1).max(5).build());
    private final Setting<Boolean> checkWorld = sgGeneral.add(new BoolSetting.Builder().name("check-world").defaultValue(true).build());
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder().name("rotate").defaultValue(false).build());
    private final Setting<Boolean> packet = sgGeneral.add(new BoolSetting.Builder().name("packet").defaultValue(false).build());
    private final Setting<Boolean> swing = sgGeneral.add(new BoolSetting.Builder().name("swing").defaultValue(true).build());


    @EventHandler
    private void onTickPre(TickEvent.Pre event) {
        if (checkWorld.get() && mc.world.getDimension().bedWorks()) return;
        BlockPos main = mc.player.getBlockPos();
        for (int i = 0; i < height.get(); i++) {
            if (bu.getBlock(main.up(i)) instanceof BedBlock) {
                switch (mode.get()) {
                    case Click -> pu.interact(main.up(i), Hand.MAIN_HAND, rotate.get(), packet.get(), swing.get());
                    case Break -> {
                        if (useAxe.get() && InvUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof AxeItem).found()) {
                            int prev = mc.player.getInventory().selectedSlot;
                            int axe = InvUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof AxeItem).slot();
                            InvUtils.swap(axe, false);
                            nu.mine(main.up(i), packet.get(), rotate.get(), swing.get());
                            InvUtils.swap(prev, false);
                        } else {
                            nu.mine(main.up(i), packet.get(), rotate.get(), swing.get());
                        }
                    }
                }
                break;
            }
        }
    }

    public enum DestroyMode {
        Click,
        Break
    }
}
