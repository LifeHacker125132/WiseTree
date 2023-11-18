package wise.tree.addon.events;

import meteordevelopment.meteorclient.events.Cancellable;
import net.minecraft.util.Hand;

public class SwingHandEvent extends Cancellable {
    private Hand hand;

    public SwingHandEvent(Hand hand) {
        this.setHand(hand);
    }

    public Hand getHand() {
        return hand;
    }

    public void setHand(Hand hand) {
        this.hand = hand;
    }
}
