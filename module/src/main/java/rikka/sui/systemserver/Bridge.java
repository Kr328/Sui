package rikka.sui.systemserver;

import android.content.Intent;

import moe.shizuku.server.IShizukuService;

import static rikka.sui.systemserver.SystemServerConstants.LOGGER;

public class Bridge {

    public static void dispatchPackageChanged(Intent intent) {
        IShizukuService service = BridgeService.get();
        if (service == null) {
            LOGGER.d("binder is null");
            return;
        }

        try {
            service.dispatchPackageChanged(intent);
        } catch (Throwable e) {
            LOGGER.w(e, "dispatchPackageChanged");
        }
    }

    public static boolean isHidden(int uid) {
        IShizukuService service = BridgeService.get();
        if (service == null) {
            LOGGER.d("binder is null");
            return false;
        }

        try {
            return service.isHidden(uid);
        } catch (Throwable e) {
            LOGGER.w(e, "isHidden");
            return false;
        }
    }
}
