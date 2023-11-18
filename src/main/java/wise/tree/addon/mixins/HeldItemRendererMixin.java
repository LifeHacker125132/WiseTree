package wise.tree.addon.mixins;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wise.tree.addon.events.ItemRenderEvent;

@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {

    @Shadow
    protected abstract void renderFirstPersonItem(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light);

    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"), cancellable = true)
    private void onRender(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        ItemRenderEvent event = new ItemRenderEvent(Color.WHITE);
        event.stack = item;
        MeteorClient.EVENT_BUS.post(event);
        if (event.isCancelled()) {
        //    ci.cancel();
            float rotateMainX;
            float rotateMainY;
            float rotateMainZ;
            rotateMainX = 43;
            rotateMainY = 130;
            rotateMainZ = 230;
            matrices.push();
            matrices.multiply(new Quaternionf(rotateMainX, rotateMainY, rotateMainZ, 0));
//            renderFirstPersonItem(player,tickDelta,pitch,Hand.MAIN_HAND,swingProgress,item,equipProgress,matrices,vertexConsumers,light);
            matrices.pop();
        }
//        vertexConsumers.draw();
    }
}
