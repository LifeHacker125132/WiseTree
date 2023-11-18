package wise.tree.addon.events;

import meteordevelopment.meteorclient.events.Cancellable;
import net.minecraft.network.packet.Packet;

public class SentPacketEvent extends Cancellable {
    public Packet<?> packet;

    public SentPacketEvent(Packet<?> packet) {
        this.packet = packet;
    }
}
