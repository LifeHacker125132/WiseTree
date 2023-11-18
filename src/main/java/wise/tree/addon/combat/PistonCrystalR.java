package wise.tree.addon.combat;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import wise.tree.addon.utils.ItemUtils;
import wise.tree.addon.utils.TrappedMode;
import wise.tree.addon.utils.WTModule;
import wise.tree.addon.utils.Wrapper;

//todo
public class PistonCrystalR extends WTModule implements Wrapper {
    public PistonCrystalR(Category category, String name) {
        super(category, name);
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<TickMode> tickMode = sgGeneral.add(new EnumSetting.Builder<TickMode>().name("tick-mode").defaultValue(TickMode.Pre).build());
    private final Setting<ItemMode> itemMode = sgGeneral.add(new EnumSetting.Builder<ItemMode>().name("item-mode").defaultValue(ItemMode.Block).build());

    private final SettingGroup sgTarget = settings.createGroup("Target");
    private final Setting<Double> targetRange = sgTarget.add(new DoubleSetting.Builder().name("target-range").defaultValue(7).sliderRange(0, 12).build());
    private final Setting<SortPriority> priority = sgTarget.add(new EnumSetting.Builder<SortPriority>().name("priority").defaultValue(SortPriority.LowestHealth).build());

    private final SettingGroup sgToggle = settings.createGroup("Toggle");
    private final Setting<Boolean> noTarget = sgToggle.add(new BoolSetting.Builder().name("no-target").defaultValue(false).build());
    private final Setting<Boolean> noItem = sgToggle.add(new BoolSetting.Builder().name("no-item").defaultValue(false).build());

    private final SettingGroup sgMisc = settings.createGroup("Misc");
    private final Setting<Boolean> debug = sgMisc.add(new BoolSetting.Builder().name("debug").defaultValue(false).build());

    private PlayerEntity target;
    private FindItemResult obby, piston, crystal, pickaxe, item;

    @EventHandler
    private void onTickPre(TickEvent.Pre event) {
        if (tickMode.get() == TickMode.Post) return;
        tick();
    }

    @EventHandler
    private void  onTickPost(TickEvent.Post event) {
        if (tickMode.get() == TickMode.Pre) return;
        tick();
    }

    private void tick() {
        target = TargetUtils.getPlayerTarget(targetRange.get(), priority.get());
        if (target == null) {
            if (noTarget.get()) {
                if (debug.get()) error("Target no found... disable!");
                toggle();
            }
            return;
        }
        getItem();
        if (noFound()) {
            if (noItem.get()) {
                if (debug.get()) error("Items no found... disable!");
                toggle();
            }
        }
        if (!isTrapped(target)) {
            doPiston1(target);
        } else {
            doPiston2(target);
        }
    }

    private void doPiston1(PlayerEntity target) {
        BlockPos f = target.getBlockPos().up();
    }

    private void doPiston2(PlayerEntity target) {

    }

    private void getItem() {
        obby = ItemUtils.getObby();
        piston = ItemUtils.getPiston();
        crystal = ItemUtils.getCrystal();
        pickaxe = ItemUtils.getPickaxe();
        item = getActivator();
    }

    private boolean noFound() {
        return (!piston.found() || !piston.isHotbar()) || (!crystal.found() || !crystal.isHotbar()) || (!item.found() || !item.isHotbar());
    }

    private FindItemResult getActivator() {
        return switch (itemMode.get()) {
            case Block -> ItemUtils.getRedstoneBlock();
            case Torch -> ItemUtils.getTorch();
            case Lever -> ItemUtils.getLever();
            case Button -> ItemUtils.getButton();
        };
    }

    private boolean isTrapped(PlayerEntity entity)  {
        return bu.isTrapped(entity, TrappedMode.Face) && bu.isTrapped(entity, TrappedMode.Top);
    }

    public enum ItemMode {
        Block,
        Torch,
        Lever,
        Button
    }

    public enum Stage {

    }

    public enum TickMode {
        Pre,
        Post
    }
}
