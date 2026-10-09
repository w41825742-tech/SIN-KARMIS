package id.sinkarm1s.gamingspace;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

public class GameOverlayService extends Service {
    public static final String ACTION_SHOW = "id.sinkarm1s.gamingspace.SHOW_OVERLAY";
    public static final String ACTION_SET_CROSSHAIR = "id.sinkarm1s.gamingspace.SET_CROSSHAIR";
    public static final String ACTION_STOP = "id.sinkarm1s.gamingspace.STOP_OVERLAY";
    public static final String PREFS = "sin_karm1s_overlay";
    public static final String PENDING_ACTION = "pending_action";
    public static final String CROSSHAIR_ENABLED = "crosshair_enabled";
    public static final String CROSSHAIR_SIZE = "crosshair_size";
    public static final String CROSSHAIR_THICKNESS = "crosshair_thickness";
    public static final String CROSSHAIR_COLOR = "crosshair_color";
    private static final String CHANNEL_ID = "sin_karm1s_game_overlay";
    private static final int NOTIFICATION_ID = 1907;

    private WindowManager windowManager;
    private WindowManager.LayoutParams panelParams;
    private WindowManager.LayoutParams crosshairParams;
    private View activePanelView;
    private LinearLayout expandedPanel;
    private View collapsedHandle;
    private CrosshairView crosshairView;
    private TextView crosshairActionLabel;
    private boolean panelExpanded = false;
    private int panelWidth;
    private float density;

    @Override
    public void onCreate() {
        super.onCreate();
        density = getResources().getDisplayMetrics().density;
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        createNotificationChannel();
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, createNotification(), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTIFICATION_ID, createNotification());
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? ACTION_SHOW : intent.getAction();
        if (ACTION_STOP.equals(action)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        if (!Settings.canDrawOverlays(this)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        if (ACTION_SET_CROSSHAIR.equals(action)) {
            updateCrosshairOverlay();
            if (activePanelView == null && !isCrosshairEnabled()) {
                stopSelf();
                return START_NOT_STICKY;
            }
            return START_STICKY;
        }
        showExpandedPanel();
        updateCrosshairOverlay();
        return START_STICKY;
    }

    private void showExpandedPanel() {
        if (activePanelView == expandedPanel && expandedPanel != null) return;
        if (expandedPanel == null) expandedPanel = buildExpandedPanel();
        panelWidth = dp(278);
        attachPanel(expandedPanel, panelWidth);
        panelExpanded = true;
    }

    private void showCollapsedHandle() {
        if (activePanelView == collapsedHandle && collapsedHandle != null) return;
        if (collapsedHandle == null) collapsedHandle = buildCollapsedHandle();
        attachPanel(collapsedHandle, dp(94));
        panelExpanded = false;
    }

    private void attachPanel(View view, int width) {
        if (windowManager == null) windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (activePanelView != null) {
            try { windowManager.removeView(activePanelView); } catch (Exception ignored) { }
        }
        if (panelParams == null) {
            int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    : WindowManager.LayoutParams.TYPE_PHONE;
            panelParams = new WindowManager.LayoutParams(
                    width, WindowManager.LayoutParams.WRAP_CONTENT, type,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT);
            panelParams.gravity = Gravity.TOP | Gravity.END;
            panelParams.x = dp(8);
            panelParams.y = dp(118);
        } else {
            panelParams.width = width;
        }
        activePanelView = view;
        try { windowManager.addView(activePanelView, panelParams); }
        catch (Exception error) {
            activePanelView = null;
            stopSelf();
        }
    }

    private LinearLayout buildExpandedPanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(9), dp(9), dp(9), dp(8));
        panel.setBackground(background(Color.rgb(17, 16, 22), Color.rgb(89, 29, 44), 20));
        panel.setElevation(dp(15));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setPadding(dp(10), dp(7), dp(5), dp(7));
        header.setBackground(headerBackground());
        TextView title = text("SIN KARM1S  /  AX-MODE", 12, Color.WHITE, true);
        title.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(34), 1));
        TextView hide = text("HIDE", 10, Color.WHITE, true);
        hide.setGravity(Gravity.CENTER);
        hide.setPadding(dp(8), 0, dp(8), 0);
        hide.setBackground(background(0x22ffffff, 0x55ffffff, 8));
        header.addView(hide, new LinearLayout.LayoutParams(dp(42), dp(30)));
        hide.setOnClickListener(v -> showCollapsedHandle());
        setDragListener(header, false);
        panel.addView(header, new LinearLayout.LayoutParams(-1, -2));

        TextView caption = text("QUICK GAME CONTROLS", 9, Color.rgb(226, 182, 191), true);
        caption.setPadding(dp(3), dp(12), dp(3), dp(6));
        panel.addView(caption);

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(3);
        String[][] actions = {
                {"◎", "CROSSHAIR", "crosshair"}, {"⌁", "NETWORK", "network"}, {"⊙", "TOUCH LAB", "touch"},
                {"⌖", "AIM LAB", "aim"}, {"◷", "SESSION", "session"}, {"⌂", "OPEN APP", "corner"}
        };
        for (String[] action : actions) {
            LinearLayout tile = new LinearLayout(this);
            tile.setOrientation(LinearLayout.VERTICAL);
            tile.setGravity(Gravity.CENTER);
            tile.setPadding(dp(3), dp(5), dp(3), dp(5));
            tile.setBackground(background(Color.rgb(35, 24, 31), Color.rgb(94, 41, 55), 11));
            tile.setElevation(dp(1));
            TextView symbol = text(action[0], 20, Color.rgb(255, 69, 91), true);
            symbol.setGravity(Gravity.CENTER);
            tile.addView(symbol, new LinearLayout.LayoutParams(-1, dp(25)));
            TextView label = text(action[1], 9, Color.WHITE, true);
            label.setGravity(Gravity.CENTER);
            tile.addView(label, new LinearLayout.LayoutParams(-1, dp(18)));
            if ("crosshair".equals(action[2])) crosshairActionLabel = label;
            GridLayout.LayoutParams gp = new GridLayout.LayoutParams();
            gp.width = 0;
            gp.height = dp(59);
            gp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            gp.setMargins(dp(3), dp(3), dp(3), dp(3));
            grid.addView(tile, gp);
            tile.setOnClickListener(v -> onPanelAction(action[2]));
        }
        panel.addView(grid, new LinearLayout.LayoutParams(-1, -2));
        TextView footer = text("DRAG HEADER TO MOVE  •  TAP HIDE TO COLLAPSE", 8, Color.rgb(145, 137, 149), true);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, dp(8), 0, dp(1));
        panel.addView(footer);
        updateCrosshairActionLabel();
        return panel;
    }

    private View buildCollapsedHandle() {
        LinearLayout handle = new LinearLayout(this);
        handle.setOrientation(LinearLayout.HORIZONTAL);
        handle.setGravity(Gravity.CENTER);
        handle.setPadding(dp(10), dp(5), dp(10), dp(5));
        handle.setBackground(headerBackground());
        handle.setElevation(dp(12));
        TextView mark = text("AX", 14, Color.WHITE, true);
        mark.setGravity(Gravity.CENTER);
        handle.addView(mark, new LinearLayout.LayoutParams(dp(30), dp(30)));
        TextView show = text("SHOW", 9, Color.WHITE, true);
        show.setGravity(Gravity.CENTER);
        handle.addView(show, new LinearLayout.LayoutParams(dp(47), dp(30)));
        setDragListener(handle, true);
        return handle;
    }

    private void setDragListener(View view, boolean tapToExpand) {
        view.setOnTouchListener(new View.OnTouchListener() {
            private float startX, startY;
            private int initialX, initialY;
            private boolean moved;
            @Override public boolean onTouch(View v, MotionEvent event) {
                if (panelParams == null || windowManager == null) return false;
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = event.getRawX();
                        startY = event.getRawY();
                        initialX = panelParams.x;
                        initialY = panelParams.y;
                        moved = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float dx = event.getRawX() - startX;
                        float dy = event.getRawY() - startY;
                        if (Math.abs(dx) > dp(4) || Math.abs(dy) > dp(4)) moved = true;
                        int screenWidth = getResources().getDisplayMetrics().widthPixels;
                        int screenHeight = getResources().getDisplayMetrics().heightPixels;
                        int width = panelParams.width;
                        int height = activePanelView == null ? dp(50) : activePanelView.getHeight();
                        if (height <= 0) height = dp(250);
                        panelParams.x = Math.max(0, Math.min(Math.max(0, screenWidth - width), initialX - (int) dx));
                        panelParams.y = Math.max(0, Math.min(Math.max(0, screenHeight - height), initialY + (int) dy));
                        try { windowManager.updateViewLayout(activePanelView, panelParams); } catch (Exception ignored) { }
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (tapToExpand && !moved) showExpandedPanel();
                        return true;
                    case MotionEvent.ACTION_CANCEL:
                        return true;
                    default:
                        return false;
                }
            }
        });
    }

    private void onPanelAction(String action) {
        if ("crosshair".equals(action)) {
            setCrosshairEnabled(!isCrosshairEnabled());
            updateCrosshairActionLabel();
            return;
        }
        openAppForAction(action);
    }

    private void setCrosshairEnabled(boolean enabled) {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(CROSSHAIR_ENABLED, enabled).apply();
        updateCrosshairOverlay();
        updateCrosshairActionLabel();
    }

    private void updateCrosshairActionLabel() {
        if (crosshairActionLabel != null) {
            crosshairActionLabel.setText(isCrosshairEnabled() ? "CROSSHAIR ON" : "CROSSHAIR");
        }
    }

    private boolean isCrosshairEnabled() {
        return getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(CROSSHAIR_ENABLED, false);
    }

    private void updateCrosshairOverlay() {
        if (windowManager == null) windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (!isCrosshairEnabled()) {
            removeCrosshairOverlay();
            return;
        }
        if (crosshairView == null) {
            crosshairView = new CrosshairView(this);
            int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    : WindowManager.LayoutParams.TYPE_PHONE;
            crosshairParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    type,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                            | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT);
            crosshairParams.gravity = Gravity.TOP | Gravity.START;
            // Keep the full-screen transparent window below Android's touch-obscuring threshold.
            crosshairParams.alpha = 0.72f;
            try { windowManager.addView(crosshairView, crosshairParams); }
            catch (Exception error) { crosshairView = null; crosshairParams = null; }
        }
        if (crosshairView != null) {
            crosshairView.invalidate();
            updateCrosshairActionLabel();
        }
    }

    private void removeCrosshairOverlay() {
        if (windowManager != null && crosshairView != null) {
            try { windowManager.removeView(crosshairView); } catch (Exception ignored) { }
        }
        crosshairView = null;
        crosshairParams = null;
    }

    private void openAppForAction(String action) {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(PENDING_ACTION, action).apply();
        Intent launch = getPackageManager().getLaunchIntentForPackage(getPackageName());
        if (launch == null) launch = new Intent(this, MainActivity.class);
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        launch.putExtra("sin_karm1s_overlay_action", action);
        startActivity(launch);
    }

    private Notification createNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return new Notification.Builder(this, CHANNEL_ID)
                    .setContentTitle("SIN KARM1S AX-MODE")
                    .setContentText("Panel kontrol gaming aktif — ketuk AX untuk membuka atau sembunyikan")
                    .setSmallIcon(android.R.drawable.ic_media_play)
                    .setOngoing(true)
                    .build();
        }
        return new Notification.Builder(this)
                .setContentTitle("SIN KARM1S AX-MODE")
                .setContentText("Panel kontrol gaming aktif")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setOngoing(true)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Game Corner AX-MODE", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Notifikasi saat kontrol gaming mengambang aktif");
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL);
        view.setIncludeFontPadding(false);
        return view;
    }

    private GradientDrawable background(int fill, int stroke, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(radius));
        drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    private GradientDrawable headerBackground() {
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{Color.rgb(242, 24, 54), Color.rgb(178, 9, 35)});
        drawable.setCornerRadius(dp(13));
        drawable.setStroke(dp(1), Color.rgb(255, 81, 104));
        return drawable;
    }

    private int dp(float value) { return (int) (value * density + 0.5f); }

    private class CrosshairView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        CrosshairView(Context context) { super(context); }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            android.content.SharedPreferences pref = getSharedPreferences(PREFS, MODE_PRIVATE);
            if (!pref.getBoolean(CROSSHAIR_ENABLED, false)) return;
            int size = pref.getInt(CROSSHAIR_SIZE, 32);
            int thickness = pref.getInt(CROSSHAIR_THICKNESS, 2);
            String color = pref.getString(CROSSHAIR_COLOR, "#ff2638");
            try { paint.setColor(Color.parseColor(color)); } catch (Exception ignored) { paint.setColor(Color.rgb(255, 38, 56)); }
            paint.setStrokeWidth(dp(thickness));
            paint.setStrokeCap(Paint.Cap.SQUARE);
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float half = dp(size) / 2f;
            canvas.drawLine(cx, cy - half, cx, cy + half, paint);
            canvas.drawLine(cx - half, cy, cx + half, cy, paint);
        }
    }

    @Override
    public void onDestroy() {
        if (windowManager != null && activePanelView != null) {
            try { windowManager.removeView(activePanelView); } catch (Exception ignored) { }
        }
        removeCrosshairOverlay();
        activePanelView = null;
        expandedPanel = null;
        collapsedHandle = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) stopForeground(STOP_FOREGROUND_REMOVE);
        else stopForeground(true);
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
