package de.appsperre;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/** Der Sperrbildschirm, der ueber einer gesperrten App erscheint. */
public class LockActivity extends Activity {

    static final String EXTRA_PKG = "pkg";

    private String pkg;
    private TextView title;
    private EditText password;
    private EditText minutes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = Ui.column(this);

        title = Ui.text(this, "", 22);
        root.addView(title);

        root.addView(Ui.text(this, "Gib dein Passwort ein, um die App freizugeben.", 15),
                Ui.spaced(this));

        password = Ui.passwordField(this, "Passwort");
        root.addView(password, Ui.spaced(this));

        root.addView(Ui.text(this, "Freigabedauer in Minuten:", 15), Ui.spaced(this));
        minutes = Ui.numberField(this, "Minuten", "15");
        root.addView(minutes);

        root.addView(Ui.button(this, "Für diese Zeit freigeben", v -> unlockTemporarily()),
                Ui.spaced(this));
        root.addView(Ui.button(this, "Dauerhaft freigeben", v -> unlockPermanently()),
                Ui.spaced(this));
        root.addView(Ui.button(this, "Abbrechen", v -> goHome()), Ui.spaced(this));

        setContentView(root);
        applyIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        applyIntent(intent);
    }

    private void applyIntent(Intent intent) {
        pkg = intent == null ? null : intent.getStringExtra(EXTRA_PKG);
        if (pkg == null) {
            finish();
            return;
        }
        String label = pkg;
        try {
            PackageManager pm = getPackageManager();
            label = pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
        } catch (Exception ignored) {
            // Paketname als Ersatz
        }
        title.setText("🔒 " + label + " ist gesperrt");
        password.setText("");
    }

    private boolean passwordOk() {
        if (Prefs.checkPassword(this, password.getText().toString())) {
            return true;
        }
        password.setText("");
        Toast.makeText(this, "Falsches Passwort", Toast.LENGTH_SHORT).show();
        return false;
    }

    private void unlockTemporarily() {
        long mins;
        try {
            mins = Long.parseLong(minutes.getText().toString().trim());
        } catch (NumberFormatException e) {
            mins = 0;
        }
        if (mins < 1 || mins > 24 * 60) {
            Toast.makeText(this, "Bitte 1 bis 1440 Minuten eingeben", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!passwordOk()) {
            return;
        }
        Prefs.unlockFor(this, pkg, mins);
        openApp();
    }

    private void unlockPermanently() {
        if (!passwordOk()) {
            return;
        }
        Prefs.setBlocked(this, pkg, false);
        openApp();
    }

    private void openApp() {
        Intent launch = getPackageManager().getLaunchIntentForPackage(pkg);
        if (launch != null) {
            startActivity(launch);
        }
        finish();
    }

    private void goHome() {
        Intent home = new Intent(Intent.ACTION_MAIN);
        home.addCategory(Intent.CATEGORY_HOME);
        home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(home);
        finish();
    }

    @Override
    public void onBackPressed() {
        goHome();
    }
}
