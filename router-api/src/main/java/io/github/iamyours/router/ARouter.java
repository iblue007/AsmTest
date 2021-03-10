package io.github.iamyours.router;

import java.util.HashMap;
import java.util.Map;

public class ARouter {

    private static ARouter instance;//= new ARouter();
    private Map<String, String> routeMap = new HashMap<>();
    private boolean loaded;

    public static ARouter getInstance() {
        if (instance == null) {
            synchronized (ARouter.class) {
                if (instance == null) {
                    instance = new ARouter();
                }
            }
        }
        return instance;
    }

    public void init() {
        if (loaded) {
            return;
        }
        new RouterMap().loadInto(routeMap);
        loaded = true;
    }


}
