package de.appsperre;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

/**
 * Laeuft im Hintergrund, erkennt die App im Vordergrund und zeigt den
 * Sperrbildschirm, wenn sie gesperrt ist.
 */
public class BlockerService extends AccessibilityService {

    private static final long TICK_MS = 10_000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private String lastPkg;

    /** Prueft regelmaessig, ob eine zeitweise Freigabe abgelaufen ist, waehrend die App offen ist. */
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            checkActiveApp();
            handler.postDelayed(this, TICK_MS);
        }
    };

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        handler.removeCallbacks(tick);
        handler.postDelayed(tick, TICK_MS);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return;
        }
        CharSequence p = event.getPackageName();
        if (p == null) {
            return;
        }
        lastPkg = p.toString();
        maybeLock(lastPkg);
    }

    private void checkActiveApp() {
        String pkg = lastPkg;
        try {
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root != null && root.getPackageName() != null) {
                pkg = root.getPackageName().toString();
            }
        } catch (Exception ignored) {
            // dann eben die zuletzt gemeldete App verwenden
        }
        if (pkg != null) {
            maybeLock(pkg);
        }
    }

    private void maybeLock(String pkg) {
        if (pkg.equals(getPackageName())) {
            return;
        }
        if (!Prefs.shouldBlock(this, pkg)) {
            return;
        }
        Intent i = new Intent(this, LockActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        i.putExtra(LockActivity.EXTRA_PKG, pkg);
        startActivity(i);
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public boolean onUnbind(Intent intent) {
        handler.removeCallbacks(tick);
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(tick);
        super.onDestroy();
    }
}
