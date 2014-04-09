
package info.daylemk.viewinplay;

import android.os.Build;
import de.robv.android.xposed.XposedBridge;

public class Common {

    public static void debugLog(String s) {
        // change to debuggable
        if (XposedInit.debuggable) {
            XposedBridge.log(s);
        }
    }

    public static final String THIS_PACKAGE_NAME = Common.class.getPackage().getName();
    public static final String PREFERENCE_MAIN_FILE = THIS_PACKAGE_NAME + "_main";

    public static final boolean DEFAULT_SHOW_IN_RECENT_PANEL = false;
    public static final boolean DEFAULT_SHOW_IN_APP_INFO = false;
    public static final boolean DEFAULT_SHOW_IN_NOTIFICATION = false;
    public static final boolean DEFAULT_TWO_FINGER_IN_RECENT_PANEL = false;
    // halo
    public static final boolean DEFAULT_COMPAT_XHALO = false;
    public static final boolean DEFAULT_DIRECTLY_SHOW_IN_PLAY = false;
    public static final boolean DEFAULT_DEBUG_LOGS = false;

    // for open in halo
    public static final int FLAG_FLOATING_WINDOW = 0x00002000;

    public static final String LOG_TAG = "ViewInPlay(SDK: " + Build.VERSION.SDK_INT + ") - ";
    public static final String XDA_THREAD = "http://forum.xda-developers.com/showthread.php?t=2419287";
}
