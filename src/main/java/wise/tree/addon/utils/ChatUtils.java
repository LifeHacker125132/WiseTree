package wise.tree.addon.utils;

public class ChatUtils {
    public static String replacer(String msg) {
        if (msg.contains(":smile:")) msg = msg.replace(":smile:", "☺");
        if (msg.contains(":sad:")) msg = msg.replace(":sad:", "☹");
        if (msg.contains(":heart:")) msg = msg.replace(":heart:", "❤");
        if (msg.contains(":skull:")) msg = msg.replace(":skull:", "☠");
        if (msg.contains(":star:")) msg = msg.replace(":star:", "★");
        if (msg.contains(":flower:")) msg = msg.replace(":flower:", "❀");
        if (msg.contains(":pick:")) msg = msg.replace(":pick:", "⛏");
        if (msg.contains(":wheelchair:")) msg = msg.replace(":wheelchair:", "♿");
        if (msg.contains(":rod:")) msg = msg.replace(":rod:", "🎣");
        if (msg.contains(":potion:")) msg = msg.replace(":potion:", "🧪");
        if (msg.contains(":fire:")) msg = msg.replace(":fire:", "🔥");
        if (msg.contains(":shears:")) msg = msg.replace(":shears:", "✂");
        if (msg.contains(":bell:")) msg = msg.replace(":bell:", "🔔");
        if (msg.contains(":bow:")) msg = msg.replace(":bow:", "🏹");
        if (msg.contains(":trident:")) msg = msg.replace(":trident:", "🔱");
        if (msg.contains(":cloud:")) msg = msg.replace(":cloud:", "☁");
        if (msg.contains(":meteor:")) msg = msg.replace(":meteor:", "☄");
        if (msg.contains(":nuke:")) msg = msg.replace(":nuke:", "☢");
        if (msg.contains(":warning:")) msg = msg.replace(":warning:", "⚠");
        if (msg.contains(":shit:")) msg = msg.replace(":shit:", "☭");
        if (msg.contains(":crown:")) msg = msg.replace(":crown:", "♛");
        if (msg.contains(":crown2:")) msg = msg.replace(":crown2:", "♚");
        if (msg.contains(":leaflet:")) msg = msg.replace(":leaflet:", "✉");
        if (msg.contains(":smiley:")) msg = msg.replace(":smiley:", "ツ");
        if (msg.contains(":nuke2:")) msg = msg.replace(":nuke2:", "☣");
        if (msg.contains(":gender1:")) msg = msg.replace(":gender1:", "♀");
        if (msg.contains(":gender2:")) msg = msg.replace(":gender2:", "♂");
        return msg;
    }
}
