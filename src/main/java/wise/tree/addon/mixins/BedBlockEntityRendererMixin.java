package wise.tree.addon.mixins;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BedBlockEntityRenderer;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import wise.tree.addon.render.BedChams;

@Mixin(BedBlockEntityRenderer.class)
public abstract class BedBlockEntityRendererMixin {

    /**
     * @author
     * @reason
     */
    @Overwrite
    private void renderPart(MatrixStack matrices, VertexConsumerProvider vertexConsumers, ModelPart part, Direction direction, SpriteIdentifier sprite, int light, int overlay, boolean isFoot) {
        Color col = Modules.get().get(BedChams.class).color.get();
        if (Modules.get().get(BedChams.class).isActive()) {
            matrices.push();
            matrices.translate(0.0F, 0.5625F, isFoot ? -1.0F : 0.0F);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));
            matrices.translate(0.5F, 0.5F, 0.5F);
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0F + direction.asRotation()));
            matrices.translate(-0.5F, -0.5F, -0.5F);
            VertexConsumer vertexConsumer = sprite.getVertexConsumer(vertexConsumers, RenderLayer::getEntitySolid);
            part.render(matrices, vertexConsumer, light, overlay, col.r / 255f, col.g / 255f, col.b / 255f, col.a / 255f);
            matrices.pop();
        } else {
            matrices.push();
            matrices.translate(0.0F, 0.5625F, isFoot ? -1.0F : 0.0F);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));
            matrices.translate(0.5F, 0.5F, 0.5F);
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0F + direction.asRotation()));
            matrices.translate(-0.5F, -0.5F, -0.5F);
            VertexConsumer vertexConsumer = sprite.getVertexConsumer(vertexConsumers, RenderLayer::getEntitySolid);
            part.render(matrices, vertexConsumer, light, overlay);
            matrices.pop();
        }
    }
}
