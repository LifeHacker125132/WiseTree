package wise.tree.addon.utils;

import meteordevelopment.meteorclient.MeteorClient;

public class EventUtils {
    public static void subscribe(Object object) {
        MeteorClient.EVENT_BUS.subscribe(object);
    }

    public static void unsubscribe(Object object) {
        MeteorClient.EVENT_BUS.unsubscribe(object);
    }

    public static void post(Object object) {
        MeteorClient.EVENT_BUS.post(object);
    }
}
