package wise.tree.addon.mixins;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.RenderLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RenderLayer.class)
public class RenderLayerMixin {
    @Redirect(method = "draw", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/BufferBuilder;end()Lnet/minecraft/client/render/BufferBuilder$BuiltBuffer;"))
    private BufferBuilder.BuiltBuffer redirect(BufferBuilder instance) {
        instance.color(1f, 1f, 0f,1f);
        return instance.end();
    }
}
