
package info.daylemk.viewinplay;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.XModuleResources;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.Toast;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class RecentTaskHook {
    private static final String TAG = "DayL";
    private static final String TAG_CLASS = "[RecentTaskHook]";

    static String TEXT_APP_INFO;
    // the string of the app info read from android
    static String TEXT_APP_INFO_STOCK = null;
    // re-open it
    static String TEXT_OPEN_IN_XHALO;
    static String TEXT_REMOVE_FROM_LIST;
    // the string of remove from list read from android
    static String TEXT_REMOVE_FROM_LIST_STOCK = null;
    static String TEXT_VIEW_IN_PLAY;
    static String TEXT_NO_PLAY;
    static String TEXT_STOCK_APP;
    // static String TEXT_CANT_OPEN_IN_XHALO;

    static int ID_REMOVE_FROM_LIST = 1000;
    static int ID_APP_INFO = 2000;
    // re-open it
    static final int ID_OPEN_IN_XHALO = 3000;
    static final int ID_VIEW_IN_PLAY = 4000;

    private static RecentsOnTouchLis sTouchListener;

    private static boolean bool_compat_xhalo = false;

    // 0 is a better choice
    private static int id_app_thumbnail = 0;
    private static int popupMenuId = 0;

    private static int recentRemoveItemId = 0;
    private static int inspectItemId = 0;

    public static void initZygote(XModuleResources module_res) {
        TEXT_APP_INFO = module_res.getString(R.string.recents_app_info);
        // re-open it
        TEXT_OPEN_IN_XHALO = module_res.getString(R.string.recents_open_halo);
        TEXT_REMOVE_FROM_LIST = module_res.getString(R.string.recents_remove_from_list);
        TEXT_VIEW_IN_PLAY = module_res.getString(R.string.view_in_play);
        TEXT_NO_PLAY = module_res.getString(R.string.no_play_on_the_phone);
        TEXT_STOCK_APP = module_res.getString(R.string.stock_app);
        // TEXT_CANT_OPEN_IN_XHALO =
        // module_res.getString(R.string.cant_open_in_xhalo);
    }

    public static void handleLoadPackage(final LoadPackageParam lpp, final XSharedPreferences pref) {
        if (!lpp.packageName.equals("com.android.systemui"))
            return;
        Common.debugLog(TAG + TAG_CLASS + "handle package");
        Common.debugLog(TAG + TAG_CLASS + "directlyShowInPlay = "
                + XposedInit.directlyShowInPlay);

        StringBuffer sb = new StringBuffer();
        Set<String> set = pref.getAll().keySet();
        Common.debugLog(TAG + TAG_CLASS + "size : " + set.size());
        for (Iterator<String> iterator = set.iterator(); iterator.hasNext();) {
            sb.append(iterator.next() + ", ");
        }
        Common.debugLog(TAG + TAG_CLASS + "keys : " + sb.toString());
        Common.debugLog(TAG + TAG_CLASS + "contain???"
                + pref.contains(XposedInit.KEY_SHOW_IN_RECENT_PANEL));

        pref.reload();

        if (pref.getBoolean(XposedInit.KEY_TWO_FINGER_IN_RECENT_PANEL,
                Common.DEFAULT_TWO_FINGER_IN_RECENT_PANEL)) {
            injectTouchEvents(lpp);
        }

        bool_compat_xhalo = pref.getBoolean(XposedInit.KEY_COMPAT_XHALO,
                Common.DEFAULT_COMPAT_XHALO);

        if (pref.getBoolean(XposedInit.KEY_SHOW_IN_RECENT_PANEL,
                Common.DEFAULT_SHOW_IN_RECENT_PANEL)) {
            injectMenu(lpp);
        }
    }

    private static void injectMenu(final LoadPackageParam lpp) {
        Common.debugLog(TAG + TAG_CLASS + "in inject menu");
        final Class<?> hookClass = XposedHelpers.findClass(
                "com.android.systemui.recent.RecentsPanelView",
                lpp.classLoader);
        XposedBridge.hookAllMethods(hookClass, "handleLongPress", new XC_MethodHook() {
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                final View thiz = (View) param.thisObject;
                final View selectedView = (View) param.args[0];
                final View anchorView = (View) param.args[1];
                final View thumbnailView = (View) param.args[2];

                thumbnailView.setSelected(true);
                final Object viewHolder = selectedView.getTag();

                // if the app is stock app, we should not show the 'view in
                // play' menu
                String pkgName = getPackageName(viewHolder);
                Common.debugLog(TAG + TAG_CLASS + "the package is : " + pkgName);
                if (isAndroidStockApp(pkgName)) {
                    // stock app, return
                    Common.debugLog(TAG + TAG_CLASS + "stock app");
                    return;
                }

                Resources res = thiz.getContext().getResources();
                if (recentRemoveItemId == 0) {
                    recentRemoveItemId = res.getIdentifier("recent_remove_item", "id",
                            "com.android.systemui");
                }
                Common.debugLog(TAG + TAG_CLASS + "the recent remove menu id is "
                        + recentRemoveItemId);
                if (recentRemoveItemId != 0) {
                    ID_REMOVE_FROM_LIST = recentRemoveItemId;
                }

                if (inspectItemId == 0) {
                    inspectItemId = res.getIdentifier("recent_inspect_item", "id",
                            "com.android.systemui");
                }
                XposedBridge
                        .log(TAG + TAG_CLASS + "the recent inspect menu id is " + inspectItemId);
                if (inspectItemId != 0) {
                    ID_APP_INFO = inspectItemId;
                }

                PopupMenu mPopupMenu = new PopupMenu(thiz.getContext(),
                        anchorView == null ? selectedView : anchorView);

                if (popupMenuId == 0) {
                    popupMenuId = res.getIdentifier("recent_popup_menu", "menu",
                            "com.android.systemui");
                }

                // if we didn't get the menu xml, we should build it
                if (popupMenuId == 0) {
                    if (TEXT_REMOVE_FROM_LIST_STOCK == null || TEXT_APP_INFO_STOCK == null) {

                        boolean exceptted = false;
                        try {
                            TEXT_REMOVE_FROM_LIST_STOCK = res.getString(res.getIdentifier(
                                    "status_bar_recent_remove_item_title", "string",
                                    "com.android.systemui"));
                            TEXT_APP_INFO_STOCK = res.getString(res.getIdentifier(
                                    "status_bar_recent_inspect_item_title", "string",
                                    "com.android.systemui"));
                        } catch (Exception e) {
                            Common.debugLog(TAG + TAG_CLASS + "can't get the text of stock text");
                            exceptted = true;
                        }

                        // if we have exception here
                        // if the text is null
                        // if the text is empty
                        // in these conditions, we all needs set the text to the
                        // default
                        if (exceptted || TEXT_REMOVE_FROM_LIST_STOCK == null
                                || TEXT_APP_INFO_STOCK == null
                                || TEXT_REMOVE_FROM_LIST_STOCK.equals("")
                                || TEXT_APP_INFO_STOCK.equals("")) {
                            Common.debugLog(TAG + TAG_CLASS + "set the text to the default ");
                            TEXT_REMOVE_FROM_LIST_STOCK = TEXT_REMOVE_FROM_LIST;
                            TEXT_APP_INFO_STOCK = TEXT_APP_INFO;
                        } else {
                            Common.debugLog(TAG + TAG_CLASS + "got the text");
                        }
                    }

                    mPopupMenu.getMenu().add(Menu.NONE, ID_REMOVE_FROM_LIST, 1,
                            TEXT_REMOVE_FROM_LIST_STOCK);
                    mPopupMenu.getMenu().add(Menu.NONE, ID_APP_INFO, 2, TEXT_APP_INFO_STOCK);
                } else {
                    mPopupMenu.getMenuInflater().inflate(
                            popupMenuId,
                            mPopupMenu.getMenu());
                }

                // if the xhalo compatibility is on, add that menu
                if (bool_compat_xhalo) {
                    Common.debugLog(TAG + TAG_CLASS + "the xhalo compatibility");
                    // re-open it
                    mPopupMenu.getMenu().add(Menu.NONE, ID_OPEN_IN_XHALO, 3,
                            TEXT_OPEN_IN_XHALO);
                }
                Common.debugLog(TAG + TAG_CLASS + "show the vip menu : " + pkgName);
                mPopupMenu.getMenu().add(Menu.NONE, ID_VIEW_IN_PLAY, 4, TEXT_VIEW_IN_PLAY);

                try {
                    XposedHelpers.setObjectField(thiz, "mPopup", mPopupMenu);
                    // thiz.getClass().getDeclaredField("mPopup").set(thiz,
                    // popup);
                } catch (Exception e) {
                    // User on ICS
                }

                final PopupMenu.OnMenuItemClickListener menu = new PopupMenu.OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {
                        int itemId = item.getItemId();
                        Common.debugLog(TAG + TAG_CLASS + "item clicked : " + itemId);
                        try {
                            if (itemId == ID_REMOVE_FROM_LIST) {
                                ViewGroup recentsContainer = (ViewGroup) hookClass
                                        .getDeclaredField("mRecentsContainer").get(thiz);
                                recentsContainer.removeViewInLayout(selectedView);
                                return true;
                            } else if (itemId == ID_APP_INFO) {
                                if (viewHolder != null) {
                                    closeRecentApps(thiz);
                                    String pkgName = getPackageName(viewHolder);
                                    try {
                                        Method method = thiz.getClass().getDeclaredMethod(
                                                "startApplicationDetailsActivity", String.class);
                                        method.setAccessible(true);
                                        method.invoke(thiz, pkgName);
                                    } catch (Exception e) {
                                        XposedBridge.log(e);
                                        // if we can't start it, use my way
                                        startApplicationDetailsActivity(thiz.getContext(), pkgName);
                                    }
                                }
                                return true;
                            } else if (itemId == ID_OPEN_IN_XHALO) {
                                if (viewHolder != null) {
                                    closeRecentApps(thiz);
                                    Object ad = viewHolder.getClass()
                                            .getDeclaredField("taskDescription")
                                            .get(viewHolder);
                                    Intent intent = (Intent) ad.getClass()
                                            .getDeclaredField("intent").get(ad);
                                    intent.addFlags(Common.FLAG_FLOATING_WINDOW
                                            | Intent.FLAG_ACTIVITY_MULTIPLE_TASK
                                            | Intent.FLAG_ACTIVITY_NO_USER_ACTION
                                            | Intent.FLAG_ACTIVITY_NEW_TASK);
                                    thiz.getContext().startActivity(intent);
                                }
                                return true;
                            } else if (itemId == ID_VIEW_IN_PLAY) {
                                if (viewHolder != null) {
                                    closeRecentApps(thiz);
                                    viewInPlay(thiz.getContext(), getPackageName(viewHolder));
                                }
                                return true;
                            }
                        } catch (Throwable t) {
                            Common.debugLog(Common.LOG_TAG);
                            XposedBridge.log(t);
                        }
                        return false;
                    }
                };
                mPopupMenu.setOnMenuItemClickListener(menu);
                mPopupMenu.setOnDismissListener(new PopupMenu.OnDismissListener() {
                    public void onDismiss(PopupMenu menu) {
                        thumbnailView.setSelected(false);
                        try {
                            XposedHelpers.setObjectField(thiz, "mPopup", null);
                            // thiz.getClass().getDeclaredField("mPopup").set(thiz,
                            // null);
                        } catch (Exception e) {
                            // User on ICS
                        }
                    }
                });
                mPopupMenu.show();
                param.setResult(null);
            }
        });
    }

    private static void injectTouchEvents(final LoadPackageParam lpp) {
        Common.debugLog(TAG + TAG_CLASS + "in inject touch eventss");
        final Class<?> hookClass = XposedHelpers.findClass(
                "com.android.systemui.recent.RecentsVerticalScrollView",
                lpp.classLoader);
        XposedBridge.hookAllMethods(hookClass, "update", new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                final Object thiz = param.thisObject;
                final ScrollView sv = (ScrollView) thiz;
                Resources res = sv.getContext().getResources();

                if (id_app_thumbnail == 0) {
                    id_app_thumbnail = res.getIdentifier("app_thumbnail", "id",
                            "com.android.systemui");
                    Common.debugLog(TAG + TAG_CLASS + "identifier 2: " + id_app_thumbnail);
                }

                LinearLayout mLinearLayout = null;
                try {
                    mLinearLayout = (LinearLayout) thiz.getClass()
                            .getDeclaredField("mLinearLayout").get(thiz);
                } catch (Exception e) {
                    XposedBridge
                            .log(TAG + TAG_CLASS
                                    + "Exception in linear layout, can't find mLinearLayout");
                }
                if (mLinearLayout != null) {
                    Common.debugLog(TAG + TAG_CLASS + "in inject touch events, OK");
                    if (sTouchListener == null) {
                        Common.debugLog(TAG + TAG_CLASS + "new a touch listener");
                        sTouchListener = new RecentsOnTouchLis();
                    }
                    FrameLayout frameLayout;
                    for (int i = 0; i < mLinearLayout.getChildCount(); i++) {
                        Common.debugLog(TAG + TAG_CLASS + "order = " + i);
                        frameLayout = (FrameLayout) mLinearLayout.getChildAt(i);
                        // frameLayout.setOnTouchListener(sTouchListener);
                        // set the touch listener to at the thumb nail, so we
                        // can get finger count
                        frameLayout.findViewById(id_app_thumbnail).setOnTouchListener(
                                sTouchListener);
                    }
                } else {
                    Common.debugLog(TAG + TAG_CLASS
                            + "in inject touch events, can't get linearLayout");
                }
            }
        });

        // different class injection
        final Class<?> panelViewHookClass = XposedHelpers.findClass(
                "com.android.systemui.recent.RecentsPanelView",
                lpp.classLoader);
        XposedBridge.hookAllMethods(panelViewHookClass, "handleOnClick", new XC_MethodHook() {
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                // the touch listener is null??? any way, return
                if (sTouchListener == null) {
                    Common.debugLog(TAG + TAG_CLASS + "EEROR, the touch listener is null");
                    return;
                }
                // not the two finger click action, return
                if (!sTouchListener.twoFingerClick) {
                    Common.debugLog(TAG + TAG_CLASS + "not two finger tap");
                    return;
                }
                sTouchListener.twoFingerClick = false;

                Common.debugLog(TAG + TAG_CLASS + "playing in the two finger tap onClick");

                final View thiz = (View) param.thisObject;
                final View selectedView = (View) param.args[0];

                final Object viewHolder = selectedView.getTag();

                // if the app is stock app, we should not go to plays
                String pkgName = getPackageName(viewHolder);
                Common.debugLog(TAG + TAG_CLASS + "the package is : " + pkgName);
                if (isAndroidStockApp(pkgName)) {
                    // stock app, show toast and do nothing
                    Common.debugLog(TAG + TAG_CLASS + "stock app");
                    Toast.makeText(thiz.getContext(), TEXT_STOCK_APP, Toast.LENGTH_SHORT).show();
                    param.setResult(null);
                    return;
                }

                closeRecentApps(thiz);
                // Object ad = viewHolder.getClass()
                // .getDeclaredField("taskDescription")
                // .get(viewHolder);
                // String pkg_name = (String)
                // ad.getClass()
                // .getDeclaredField("packageName").get(ad);
                viewInPlay(thiz.getContext(), getPackageName(viewHolder));

                param.setResult(null);
            }
        });
    }

    private static class RecentsOnTouchLis implements View.OnTouchListener {
        public boolean twoFingerClick;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            Common.debugLog(TAG + TAG_CLASS + "on touch, v : " + v + ", events : " + event);
            Common.debugLog(TAG + TAG_CLASS + "events, action : " + event.getAction());
            Common.debugLog(TAG + TAG_CLASS + "events, pointer count : " + event.getPointerCount());
            if (event.getPointerCount() == 2) {
                Common.debugLog(TAG + TAG_CLASS + "events, two pointer");
                if (event.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN) {
                    twoFingerClick = true;
                    Common.debugLog(TAG + TAG_CLASS + "events, pointer DOWN");
                    XposedBridge
                            .log(TAG + TAG_CLASS + "events, TWO ON, e: " + event);
                }
            } else {
                // don't set it
                // twoFingerClick = false;
            }

            return false;
        }
    }

    private static void closeRecentApps(View thiz) {
        // > 4.1
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
            Common.debugLog(TAG + TAG_CLASS + "call show, >4.1");
            try {
                // DO NOT use callMethod, it wouldn't catch anything here
                thiz.getClass().getDeclaredMethod("show", boolean.class).invoke(thiz, false);
                //XposedHelpers.callMethod(thiz, "dismissAndGoBack");
                return;
            } catch (Exception e) {
                XposedBridge.log(e);
            }
        }
        // 4.1
        StatusBarHook.collapseStatusBarPanel();

        /*new Thread() {
            @Override
            public void run() {
                try {
                    Runtime.getRuntime().exec("input keyevent " + KeyEvent.KEYCODE_BACK);
                } catch (Exception e) {
                    Common.debugLog(e);
                }
            }
        }.start();*/
    }

    // we use this method in the Status bar hook
    /* private */static void startApplicationDetailsActivity(Context ctx, String packageName) {
        Common.debugLog(TAG + TAG_CLASS + "start application details use my way");
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts(
                "package", packageName, null));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(intent);
    }

    /**
     * get the package name from the view holder object
     * 
     * @param viewHolder
     * @return
     * @throws Throwable
     */
    private static String getPackageName(final Object viewHolder) throws Throwable {
        String pkg_name = "";
        if (viewHolder != null) {
            Object ad = viewHolder.getClass()
                    .getDeclaredField("taskDescription")
                    .get(viewHolder);
            pkg_name = (String) ad.getClass()
                    .getDeclaredField("packageName").get(ad);
        }
        return pkg_name;
    }

    /**
     * check if the package is the stock app or not
     * added : Null check
     * @param pkgName
     * @return
     */
    static boolean isAndroidStockApp(String pkgName) {
        if(pkgName == null){
            Common.debugLog(TAG + TAG_CLASS + "the package name is null, isAndroidStockApp");
            return false;
        }
        // to lower case
        String pkgNameInner = pkgName.toLowerCase(Locale.ENGLISH);
        if (XposedInit.isStockAndroidApp(pkgNameInner))
            return true;
        if (XposedInit.isNotStockApp(pkgNameInner))
            return false;
        if (pkgNameInner.startsWith("com.android"))
            return true;
        // this is for the notification package name
        if (pkgNameInner.equals("android"))
            return true;
        return false;
    }

    /**
     * view the package in play, use the default access permission, so we can
     * call this method in this package
     * 
     * @param ctx
     * @param packageName
     */
    static void viewInPlay(Context ctx, String packageName) {
        Common.debugLog(TAG + TAG_CLASS + "view in play : " + packageName);
        Intent intent = new Intent(Intent.ACTION_VIEW);
//        intent.setData(Uri.parse("http://play.google.com/store/apps/details?id=" + packageName));
        // move to this, so can view in different app store
        intent.setData(Uri.parse("market://details?id=" + packageName));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_TASK_ON_HOME);

        checkPlay(ctx, intent);

        ctx.startActivity(intent);
    }

    private static void checkPlay(Context ctx, Intent intent) {
        List<ResolveInfo> list = ctx.getPackageManager().queryIntentActivities(intent, 0);
        Common.debugLog(TAG + TAG_CLASS + "directlyShowInPlay in check play : "
                + XposedInit.directlyShowInPlay);
        if (XposedInit.directlyShowInPlay) {
            String pkgName;
            Common.debugLog(TAG + TAG_CLASS + "size:" + list.size());
            for (int i = 0; i < list.size(); i++) {
                ActivityInfo info = list.get(i).activityInfo;
                Common.debugLog(TAG + TAG_CLASS + "the package name is " + info.packageName);
                pkgName = info.packageName;
                if (pkgName.equals("com.android.vending")) {
                    Common.debugLog(TAG + TAG_CLASS + "we found it : " + pkgName);
                    String appInfo = info.toString();
                    Common.debugLog(TAG + TAG_CLASS + "app info string : " + appInfo);
                    String activityName = appInfo.substring(
                            appInfo.lastIndexOf(" ") + 1, appInfo.length() - 1);
                    Common.debugLog(TAG + TAG_CLASS + "activity name : " + activityName);
                    intent.setComponent(new ComponentName(pkgName, activityName));
                    return;
                }
            }
            Common.debugLog(TAG + TAG_CLASS + "no play here, pity");
            // if didn't find the Google Play Store, show the toast
            // TODO Toast may can not show by system, so we should do later
            // about it, maybe a broadcast instead
            // DONE just need to get the resource at init moment
            Toast.makeText(ctx, TEXT_NO_PLAY,
                    Toast.LENGTH_LONG).show();
        }
    }
}
