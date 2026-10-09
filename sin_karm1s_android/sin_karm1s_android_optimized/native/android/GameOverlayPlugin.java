package id.sinkarm1s.gamingspace;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.Base64;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@CapacitorPlugin(name = "GameOverlay")
public class GameOverlayPlugin extends Plugin {
    private static final int OVERLAY_SETTINGS_REQUEST = 6150;
    private static final String PREFS = GameOverlayService.PREFS;

    @PluginMethod
    public void showOverlay(PluginCall call) {
        Context context = getContext();
        if (!Settings.canDrawOverlays(context)) {
            openOverlaySettings();
            JSObject result = new JSObject();
            result.put("shown", false);
            result.put("permissionRequired", true);
            call.resolve(result);
            return;
        }
        try {
            Intent intent = new Intent(context, GameOverlayService.class);
            intent.setAction(GameOverlayService.ACTION_SHOW);
            startOverlayService(intent);
            JSObject result = new JSObject();
            result.put("shown", true);
            result.put("permissionRequired", false);
            call.resolve(result);
        } catch (Exception error) {
            call.reject("AX-MODE gagal dijalankan: " + error.getMessage(), error);
        }
    }

    @PluginMethod
    public void hideOverlay(PluginCall call) {
        getContext().stopService(new Intent(getContext(), GameOverlayService.class));
        JSObject result = new JSObject();
        result.put("hidden", true);
        call.resolve(result);
    }

    @PluginMethod
    public void hasOverlayPermission(PluginCall call) {
        JSObject result = new JSObject();
        result.put("granted", Settings.canDrawOverlays(getContext()));
        call.resolve(result);
    }

    /** Returns apps that have a launcher activity. Package visibility is declared by the installer. */
    @PluginMethod
    public void getInstalledApps(PluginCall call) {
        try {
            PackageManager pm = getContext().getPackageManager();
            Intent launcher = new Intent(Intent.ACTION_MAIN, null);
            launcher.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> matches = pm.queryIntentActivities(launcher, 0);
            Set<String> seen = new HashSet<>();
            List<JSObject> items = new ArrayList<>();
            String ownPackage = getContext().getPackageName();
            for (ResolveInfo info : matches) {
                if (info.activityInfo == null) continue;
                String packageName = info.activityInfo.packageName;
                if (packageName == null || packageName.equals(ownPackage) || !seen.add(packageName)) continue;
                try {
                    ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
                    String label = String.valueOf(pm.getApplicationLabel(appInfo));
                    boolean isGame = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                            && appInfo.category == ApplicationInfo.CATEGORY_GAME;
                    String search = (label + " " + packageName).toLowerCase(Locale.ROOT);
                    if (!isGame) isGame = looksLikeGame(search);
                    JSObject app = new JSObject();
                    app.put("name", label);
                    app.put("packageName", packageName);
                    app.put("isGame", isGame);
                    app.put("systemApp", (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0);
                    if (isGame) {
                        String iconData = encodeSmallIcon(pm, appInfo);
                        if (!iconData.isEmpty()) app.put("iconData", iconData);
                    }
                    items.add(app);
                } catch (PackageManager.NameNotFoundException ignored) {
                    // App was uninstalled while the list was being read.
                }
            }
            Collections.sort(items, (a, b) -> {
                try {
                    boolean ag = a.getBoolean("isGame");
                    boolean bg = b.getBoolean("isGame");
                    if (ag != bg) return ag ? -1 : 1;
                    return a.getString("name").compareToIgnoreCase(b.getString("name"));
                } catch (Exception ignored) {
                    return 0;
                }
            });
            JSArray apps = new JSArray();
            for (JSObject item : items) apps.put(item);
            JSObject result = new JSObject();
            result.put("apps", apps);
            result.put("count", items.size());
            call.resolve(result);
        } catch (Exception error) {
            call.reject("Daftar aplikasi Android gagal dibaca: " + error.getMessage(), error);
        }
    }

    /** Launches a real installed app. AX-MODE is started first when overlay permission is available. */
    @PluginMethod
    public void launchGame(PluginCall call) {
        String packageName = call.getString("packageName", "").trim();
        if (packageName.isEmpty()) {
            call.reject("Package ID aplikasi belum tersedia.");
            return;
        }
        try {
            Context context = getContext();
            Intent launch = context.getPackageManager().getLaunchIntentForPackage(packageName);
            if (launch == null) {
                JSObject result = new JSObject();
                result.put("launched", false);
                result.put("notInstalled", true);
                call.resolve(result);
                return;
            }
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            boolean overlayAllowed = Settings.canDrawOverlays(context);
            if (overlayAllowed) {
                Intent overlay = new Intent(context, GameOverlayService.class);
                overlay.setAction(GameOverlayService.ACTION_SHOW);
                startOverlayService(overlay);
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    try {
                        context.startActivity(launch);
                        JSObject result = new JSObject();
                        result.put("launched", true);
                        result.put("overlayStarted", true);
                        call.resolve(result);
                    } catch (Exception error) {
                        call.reject("Game ditemukan, tetapi gagal dibuka: " + error.getMessage(), error);
                    }
                }, 280);
            } else {
                // Do not block the game launch just because the user has not granted overlay access.
                context.startActivity(launch);
                JSObject result = new JSObject();
                result.put("launched", true);
                result.put("overlayStarted", false);
                result.put("overlayPermissionRequired", true);
                call.resolve(result);
            }
        } catch (Exception error) {
            call.reject("Gagal membuka aplikasi " + packageName + ": " + error.getMessage(), error);
        }
    }

    @PluginMethod
    public void saveCrosshairSettings(PluginCall call) {
        int size = clamp(call.getInt("size", 32), 12, 96);
        int thickness = clamp(call.getInt("thickness", 2), 1, 8);
        String color = call.getString("color", "#ff2638");
        if (!color.matches("^#[0-9a-fA-F]{6}$")) color = "#ff2638";
        getContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putInt(GameOverlayService.CROSSHAIR_SIZE, size)
                .putInt(GameOverlayService.CROSSHAIR_THICKNESS, thickness)
                .putString(GameOverlayService.CROSSHAIR_COLOR, color)
                .apply();
        JSObject result = new JSObject();
        result.put("saved", true);
        call.resolve(result);
    }

    @PluginMethod
    public void getCrosshairState(PluginCall call) {
        android.content.SharedPreferences prefs = getContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        JSObject result = new JSObject();
        result.put("enabled", prefs.getBoolean(GameOverlayService.CROSSHAIR_ENABLED, false));
        result.put("size", prefs.getInt(GameOverlayService.CROSSHAIR_SIZE, 32));
        result.put("thickness", prefs.getInt(GameOverlayService.CROSSHAIR_THICKNESS, 2));
        result.put("color", prefs.getString(GameOverlayService.CROSSHAIR_COLOR, "#ff2638"));
        call.resolve(result);
    }

    @PluginMethod
    public void setCrosshair(PluginCall call) {
        boolean enabled = call.getBoolean("enabled", false);
        int size = clamp(call.getInt("size", 32), 12, 96);
        int thickness = clamp(call.getInt("thickness", 2), 1, 8);
        String color = call.getString("color", "#ff2638");
        if (!color.matches("^#[0-9a-fA-F]{6}$")) color = "#ff2638";
        Context context = getContext();
        if (enabled && !Settings.canDrawOverlays(context)) {
            openOverlaySettings();
            JSObject result = new JSObject();
            result.put("enabled", false);
            result.put("permissionRequired", true);
            call.resolve(result);
            return;
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putBoolean(GameOverlayService.CROSSHAIR_ENABLED, enabled)
                .putInt(GameOverlayService.CROSSHAIR_SIZE, size)
                .putInt(GameOverlayService.CROSSHAIR_THICKNESS, thickness)
                .putString(GameOverlayService.CROSSHAIR_COLOR, color)
                .apply();
        try {
            Intent intent = new Intent(context, GameOverlayService.class);
            intent.setAction(GameOverlayService.ACTION_SET_CROSSHAIR);
            startOverlayService(intent);
            JSObject result = new JSObject();
            result.put("enabled", enabled);
            result.put("permissionRequired", false);
            call.resolve(result);
        } catch (Exception error) {
            call.reject("Crosshair overlay gagal diperbarui: " + error.getMessage(), error);
        }
    }

    @PluginMethod
    public void consumePendingAction(PluginCall call) {
        Context context = getContext();
        String action = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(GameOverlayService.PENDING_ACTION, "");
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().remove(GameOverlayService.PENDING_ACTION).apply();
        JSObject result = new JSObject();
        result.put("action", action);
        call.resolve(result);
    }

    private void startOverlayService(Intent intent) {
        Context context = getContext();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent);
        else context.startService(intent);
    }

    private void openOverlaySettings() {
        try {
            Intent settings = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getContext().getPackageName()));
            getActivity().startActivityForResult(settings, OVERLAY_SETTINGS_REQUEST);
        } catch (Exception ignored) {
            // Some OEM settings applications do not expose a package-specific screen.
            try { getActivity().startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)); }
            catch (Exception ignoredAgain) { }
        }
    }

    private String encodeSmallIcon(PackageManager pm, ApplicationInfo appInfo) {
        Bitmap bitmap = null;
        ByteArrayOutputStream output = null;
        try {
            Drawable drawable = pm.getApplicationIcon(appInfo);
            int size = 84;
            bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            android.graphics.Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, size, size);
            drawable.draw(canvas);
            output = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output);
            return "data:image/png;base64," + Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP);
        } catch (Exception ignored) {
            return "";
        } finally {
            if (bitmap != null) bitmap.recycle();
            if (output != null) {
                try { output.close(); } catch (Exception ignored) { }
            }
        }
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static boolean looksLikeGame(String value) {
        String[] tokens = {
                "game", "free fire", "freefire", "pubg", "mobile legends", "mlbb", "call of duty", "codm",
                "roblox", "genshin", "minecraft", "clash", "arena", "pokemon", "farlight", "stumble",
                "among us", "delta force", "blood strike", "efootball", "asphalt", "racing", "honkai",
                "wuthering", "zenless", "wild rift", "fortnite", "brawl", "warzone", "valorant", "survival",
                "legend", "heroes", "honor of kings", "rpg", "moba", "shooter", "fighter", "combat",
                "kart", "simulator", "grand theft auto", "gta", "subway surfers", "temple run", "candy crush",
                "stardew", "terraria", "royale", "critical ops", "standoff", "world of tanks", "dragon",
                "football", "soccer", "basketball", "rally", "drift", "fall guys", "tower of fantasy"
        };
        for (String token : tokens) if (value.contains(token)) return true;
        return false;
    }
}
