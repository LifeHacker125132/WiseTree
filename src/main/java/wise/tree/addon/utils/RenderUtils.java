package wise.tree.addon.utils;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.util.math.BlockPos;

public class RenderUtils {
    public void onRenderGradient(Render3DEvent event, BlockPos pos, Color sideColor, Color sideColor2, Color lineColor, Color lineColor2, ShapeMode mode) {
        switch (mode) {
            case Both -> {
                onRenderGradientLine(event, pos, lineColor, lineColor2);
                onRenderGradientSide(event, pos, sideColor, sideColor2);
            }
            case Lines -> onRenderGradientLine(event, pos, lineColor, lineColor2);
            case Sides -> onRenderGradientSide(event, pos, sideColor, sideColor2);
        }
    }

    public void onRenderGradientLine(Render3DEvent event, BlockPos pos, Color lineColor, Color lineColor2) {
        event.renderer.gradientQuadVertical(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY() + 1, pos.getZ() + 0.02, lineColor, lineColor2);
        event.renderer.gradientQuadVertical(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 0.02, pos.getY() + 1, pos.getZ(), lineColor, lineColor2);
        event.renderer.gradientQuadVertical(pos.getX() + 1, pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 0.02, lineColor, lineColor2);
        event.renderer.gradientQuadVertical(pos.getX() + 1, pos.getY(), pos.getZ(), pos.getX() + 0.98, pos.getY() + 1, pos.getZ(), lineColor, lineColor2);
        event.renderer.gradientQuadVertical(pos.getX(), pos.getY(), pos.getZ() + 1, pos.getX(), pos.getY() + 1, pos.getZ() + 0.98, lineColor, lineColor2);
        event.renderer.gradientQuadVertical(pos.getX(), pos.getY(), pos.getZ() + 1, pos.getX() + 0.02, pos.getY() + 1, pos.getZ() + 1, lineColor, lineColor2);
        event.renderer.gradientQuadVertical(pos.getX() + 1, pos.getY(), pos.getZ() + 1, pos.getX() + 1, pos.getY() + 1, pos.getZ() + 0.98, lineColor, lineColor2);
        event.renderer.gradientQuadVertical(pos.getX() + 1, pos.getY(), pos.getZ() + 1, pos.getX() + 0.98, pos.getY() + 1, pos.getZ() + 1, lineColor, lineColor2);
        event.renderer.gradientQuadVertical(pos.getX(), pos.getY() + 1, pos.getZ(), pos.getX() + 1, pos.getY() + 0.98, pos.getZ(), lineColor, lineColor);
        event.renderer.quadHorizontal(pos.getX(), pos.getY() + 1, pos.getZ(), pos.getX() + 1, pos.getZ() + 0.02, lineColor);
        event.renderer.gradientQuadVertical(pos.getX(), pos.getY() + 1, pos.getZ(), pos.getX(), pos.getY() + 0.98, pos.getZ() + 1, lineColor, lineColor);
        event.renderer.quadHorizontal(pos.getX(), pos.getY() + 1, pos.getZ(), pos.getX() + 0.02, pos.getZ() + 1, lineColor);
        event.renderer.gradientQuadVertical(pos.getX(), pos.getY() + 1, pos.getZ() + 1, pos.getX() + 1, pos.getY() + 0.98, pos.getZ() + 1, lineColor, lineColor);
        event.renderer.quadHorizontal(pos.getX(), pos.getY() + 1, pos.getZ() + 1, pos.getX() + 1, pos.getZ() + 0.98, lineColor);
        event.renderer.gradientQuadVertical(pos.getX() + 1, pos.getY() + 1, pos.getZ(), pos.getX() + 1, pos.getY() + 0.98, pos.getZ() + 1, lineColor, lineColor);
        event.renderer.quadHorizontal(pos.getX() + 1, pos.getY() + 1, pos.getZ(), pos.getX() + 0.98, pos.getZ() + 1, lineColor);
        event.renderer.gradientQuadVertical(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 0.02, pos.getZ(), lineColor2, lineColor2);
        event.renderer.quadHorizontal(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getZ() + 0.02, lineColor2);
        event.renderer.gradientQuadVertical(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY() + 0.02, pos.getZ() + 1, lineColor2, lineColor2);
        event.renderer.quadHorizontal(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 0.02, pos.getZ() + 1, lineColor2);
        event.renderer.gradientQuadVertical(pos.getX(), pos.getY(), pos.getZ() + 1, pos.getX() + 1, pos.getY() + 0.02, pos.getZ() + 1, lineColor2, lineColor2);
        event.renderer.quadHorizontal(pos.getX(), pos.getY(), pos.getZ() + 1, pos.getX() + 1, pos.getZ() + 0.98, lineColor);
        event.renderer.gradientQuadVertical(pos.getX() + 1, pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 0.02, pos.getZ() + 1, lineColor2, lineColor2);
        event.renderer.quadHorizontal(pos.getX() + 1, pos.getY(), pos.getZ(), pos.getX() + 0.98, pos.getZ() + 1, lineColor2);
    }

    public void onRenderGradientSide(Render3DEvent event, BlockPos pos, Color sideColor, Color sideColor2) {
        event.renderer.gradientQuadVertical(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ(), sideColor, sideColor2);
        event.renderer.gradientQuadVertical(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY() + 1, pos.getZ() + 1, sideColor, sideColor2);
        event.renderer.gradientQuadVertical(pos.getX() + 1, pos.getY(), pos.getZ() + 1, pos.getX() + 1, pos.getY() + 1, pos.getZ(), sideColor, sideColor);
        event.renderer.gradientQuadVertical(pos.getX() + 1, pos.getY(), pos.getZ() + 1, pos.getX(), pos.getY() + 1, pos.getZ() + 1, sideColor, sideColor2);
        event.renderer.quadHorizontal(pos.getX(), pos.getY() + 1, pos.getZ(), pos.getX() + 1, pos.getZ() + 1, sideColor);
        event.renderer.quadHorizontal(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getZ() + 1, sideColor2);
    }
}
