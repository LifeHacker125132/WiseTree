package wise.tree.addon.combat;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.SignItem;
import net.minecraft.util.math.BlockPos;
import wise.tree.addon.utils.Wrapper;

import java.util.ArrayList;
import java.util.List;

public class DripAura extends Module implements Wrapper {
    public DripAura(Category category, String name) {
        super(category, name, "");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Double> targetRange = sgGeneral.add(new DoubleSetting.Builder().name("target-range").defaultValue(5.5).sliderRange(0, 6).build());
    private final Setting<SortPriority> priority = sgGeneral.add(new EnumSetting.Builder<SortPriority>().name("target-priority").defaultValue(SortPriority.LowestHealth).build());
    private final Setting<Boolean> trap = sgGeneral.add(new BoolSetting.Builder().name("trap").defaultValue(false).build());
    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder().name("trap-delay").defaultValue(1).min(0).sliderMax(10).build());
    private final Setting<Boolean> sign = sgGeneral.add(new BoolSetting.Builder().name("sign-place").defaultValue(false).build());
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder().name("rotate").defaultValue(false).build());
    private final Setting<Boolean> swing = sgGeneral.add(new BoolSetting.Builder().name("swing").defaultValue(false).build());

    private FindItemResult d, p, o, s, n;
    private DAMore daMore;
    private int timer;
    private Stage stage = Stage.Block;



    @Override
    public void onActivate() {
        timer = delay.get();
        daMore = new DAMore();
        stage = Stage.Block;
        d = null;
        p = null;
        o = null;
        s = null;
        n = null;
    }

    @EventHandler
    private void onTickPost(TickEvent.Post event) {
        daMore.setTarget(TargetUtils.getPlayerTarget(targetRange.get(), priority.get()));

        if (daMore.target != null) {
            BlockPos face = daMore.target.getBlockPos().up();

            d = InvUtils.findInHotbar(Items.POINTED_DRIPSTONE);
            o = InvUtils.findInHotbar(Items.OBSIDIAN);
            p = InvUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof PickaxeItem);
            n = InvUtils.findInHotbar(Items.NETHERRACK);
            s = InvUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof SignItem);

            if (!d.found() || !o.found() || !p.found() || !n.found() || !s.found()) return;
/*
            if (!daMore.isEmpty() && trap.get()) {
                if (timer <= 0 && daMore.getSize() > 0) {
                    BlockPos blockPos = daMore.getPos().get(daMore.getSize() - 1);

                    if (BlockUtils.place(blockPos, o, rotate.get(), 50, true)) {
                        daMore.remove(blockPos);
                    }

                    timer = delay.get();
                } else {
                    timer--;
                }
            } /*else {
                for (int i = 0; i < 5; i++) {
                    for (CardinalDirection dir : CardinalDirection.values()) {
                        daMore.add(new BlockPos(face.up(i).offset(dir.toDirection())));
                    }
                }
            }
            */

            if (sign.get()) {
                for (int i = 0; i < 4; i++) {
                    BlockUtils.place(face.up(i), s, rotate.get(), 50, false);
                }
            }

            BlockPos netherrack = face.up(6);
            BlockPos drip = face.up(5);
            switch (stage) {
                case Block -> {
                    if (BlockUtils.place(netherrack, n, rotate.get(), 50, true)) {
                        next(Stage.Drip);
                    }
                }
                case Drip -> {
                    if (BlockUtils.place(drip, d, rotate.get(), 50, true)) {
                        next(Stage.Block);
                    }
                }
                case Mine -> {
                    int prev = mc.player.getInventory().selectedSlot;
                    InvUtils.swap(p.slot(), false);
                    nu.mine(netherrack, false, rotate.get(), swing.get());
                    InvUtils.swap(prev, false);
                    next(Stage.Block);
                }
            }
        }
    }

    private void next(Stage stage) {
        this.stage = stage;
    }

    public enum Stage {
        Block,
        Drip,
        Mine,
    }

    private static class DAMore {
        public PlayerEntity target;
        public List<BlockPos> pos;

        public DAMore() {
            target = null;
            pos = new ArrayList<>();
        }

        public void setTarget(PlayerEntity target) {
            this.target = target;
        }

        public void add(BlockPos pos) {
            this.pos.add(pos);
        }

        public void remove(BlockPos pos) {
            this.pos.remove(pos);
        }

        public void clear() {
            pos.clear();
        }

        public boolean isEmpty() {
            return pos.isEmpty();
        }

        public int getSize() {
            return pos.size();
        }

        public BlockPos getPos(int index) {
            return pos.get(index);
        }

        public List<BlockPos> getPos() {
            return pos;
        }
    }
}
