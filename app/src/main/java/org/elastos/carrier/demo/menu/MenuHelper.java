package org.elastos.carrier.demo.menu;

import org.elastos.carrier.demo.MainActivity;
import org.elastos.carrier.demo.StandardAuth;
import org.elastos.carrier.demo.carrier.CarrierHelper;
import org.elastos.carrier.demo.session.CarrierSessionHelper;

public class MenuHelper {
    public class Carrier extends MenuCarrierHelper {
    }
    public class Session extends MenuSessionHelper {
    }
    public class OneDrive extends MenuOneDriveHelper {
    }

    public static void Init(MainActivity activity,
                            CarrierHelper.Listener carrierListener,
                            CarrierSessionHelper.Listener sessionListener) {
        StandardAuth.Init(activity);
        Carrier.Init(activity, carrierListener);
        Session.Init(activity, sessionListener);
        OneDrive.Init(activity);
    }

    public static void Uninit() {
        Carrier.Uninit();
        Session.Uninit();
    }

    private MenuHelper() {}
}
