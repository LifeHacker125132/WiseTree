package wise.tree.addon.mixins;

import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wise.tree.addon.events.SentPacketEvent;

@Mixin(ClientConnection.class)
public class ClientConnectionMixin {
    @Inject(method = "send(Lnet/minecraft/network/packet/Packet;)V", at = @At("TAIL"), cancellable = true)
    private void onSendPacketTail(Packet<?> packet, CallbackInfo info) {
        SentPacketEvent event = new SentPacketEvent(packet);
        if (MeteorClient.EVENT_BUS.post(event).isCancelled()) info.cancel();
    }
}
