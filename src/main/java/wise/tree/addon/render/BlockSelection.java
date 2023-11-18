package wise.tree.addon.render;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;

import java.util.ArrayList;

public class BlockSelection extends Module {

    public BlockSelection(Category category, String name) {
        super(category, name, "");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Integer> speed = sgGeneral.add(new IntSetting.Builder().name("speed").defaultValue(6).min(1).max(20).build());

    private final SettingGroup sgRender = settings.createGroup("Render");
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").defaultValue(ShapeMode.Both).build());
    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder().name("side-color").visible(() -> shapeMode.get() != ShapeMode.Lines).build());
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder().name("line-color").visible(() -> shapeMode.get() != ShapeMode.Sides).build());

    private final ArrayList<BlockX> blocks = new ArrayList<>();

    @EventHandler
    private void onTickEvent(TickEvent.Pre event) {
        BlockHitResult hitResult = (BlockHitResult) mc.getCameraEntity().raycast(mc.interactionManager.getReachDistance(), 0, false);
        for (BlockX block : blocks) {
            if (block.hr.equals(hitResult)) continue;
            block.tick();
        }
        blocks.removeIf(blockX -> blockX.tick <= 0);
        if (BlockUtils.canPlace(new BlockPos(hitResult.getBlockPos()))) return;
        BlockX bx = new BlockX(hitResult);
        if (!blocks.contains(bx)) blocks.add(bx);
    }

    @EventHandler
    private void onRenderEvent(Render3DEvent event) {
        for (BlockX result : blocks) {
            BlockPos bp = result.hr.getBlockPos();
            BlockState state = mc.world.getBlockState(bp);
            VoxelShape shape = state.getOutlineShape(mc.world, bp);
            if (shape.isEmpty()) continue;
            Box box = shape.getBoundingBox();
            render(event, bp, box, result.tick);
        }
    }

    private void render(Render3DEvent event, BlockPos bp, Box box, int alpha) {
        Color side = new Color(sideColor.get().r, sideColor.get().g, sideColor.get().b, Math.min(sideColor.get().a, alpha));
        Color line = new Color(lineColor.get().r, lineColor.get().g, lineColor.get().b, Math.min(lineColor.get().a, alpha));
        event.renderer.box(
            bp.getX() + box.minX, bp.getY() + box.minY, bp.getZ() + box.minZ,
            bp.getX() + box.maxX, bp.getY() + box.maxY, bp.getZ() + box.maxZ,
            side, line, shapeMode.get(), 0
        );
    }

    private static class BlockX {
        public BlockHitResult hr;
        public int tick = 255;

        public BlockX(BlockHitResult hr) {
            this.hr = hr;
        }

        public void tick() {
            tick -= Modules.get().get(BlockSelection.class).speed.get();
        }
    }

}
