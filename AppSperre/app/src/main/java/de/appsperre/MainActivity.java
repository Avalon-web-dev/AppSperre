package de.appsperre;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Passwortabfrage und Auswahl der zu sperrenden Apps. */
public class MainActivity extends Activity {

    private boolean authed = false;
    private boolean changingPassword = false;

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Beim Verlassen der App wieder absperren
        if (!isChangingConfigurations()) {
            authed = false;
            changingPassword = false;
        }
    }

    private void render() {
        if (!Prefs.hasPassword(this) || changingPassword) {
            showSetup();
        } else if (!authed) {
            showLogin();
        } else {
            showMain();
        }
    }

    // ---------- Passwort festlegen / aendern ----------

    private void showSetup() {
        LinearLayout root = Ui.column(this);
        root.addView(Ui.text(this, "Passwort festlegen", 22));
        root.addView(Ui.text(this,
                "Mindestens 4 Zeichen. Merke es dir gut: Ohne Passwort lässt sich die Sperre "
                        + "nur aufheben, indem du AppSperre deinstallierst.", 15), Ui.spaced(this));

        EditText first = Ui.passwordField(this, "Neues Passwort");
        EditText second = Ui.passwordField(this, "Passwort wiederholen");
        root.addView(first, Ui.spaced(this));
        root.addView(second, Ui.spaced(this));

        root.addView(Ui.button(this, "Speichern", v -> {
            String a = first.getText().toString();
            String b = second.getText().toString();
            if (a.length() < 4) {
                toast("Das Passwort braucht mindestens 4 Zeichen");
            } else if (!a.equals(b)) {
                toast("Die Passwörter stimmen nicht überein");
            } else {
                Prefs.setPassword(this, a);
                authed = true;
                changingPassword = false;
                toast("Passwort gespeichert");
                render();
            }
        }), Ui.spaced(this));

        if (changingPassword) {
            root.addView(Ui.button(this, "Abbrechen", v -> {
                changingPassword = false;
                render();
            }), Ui.spaced(this));
        }
        setContentView(root);
    }

    // ---------- Anmelden ----------

    private void showLogin() {
        LinearLayout root = Ui.column(this);
        root.addView(Ui.text(this, "AppSperre", 22));
        EditText pw = Ui.passwordField(this, "Passwort");
        root.addView(pw, Ui.spaced(this));
        root.addView(Ui.button(this, "Entsperren", v -> {
            if (Prefs.checkPassword(this, pw.getText().toString())) {
                authed = true;
                render();
            } else {
                pw.setText("");
                toast("Falsches Passwort");
            }
        }), Ui.spaced(this));
        setContentView(root);
    }

    // ---------- Hauptansicht ----------

    private void showMain() {
        LinearLayout root = Ui.column(this);
        root.addView(Ui.text(this, "AppSperre", 22));

        boolean enabled = isServiceEnabled();
        TextView status = Ui.text(this, enabled
                ? "✅ Der Sperrdienst ist aktiv."
                : "⚠️ Der Sperrdienst ist noch aus. Tippe unten auf den Knopf und schalte "
                + "„AppSperre“ in den Bedienungshilfen ein, sonst wird nichts gesperrt.", 15);
        root.addView(status, Ui.spaced(this));

        root.addView(Ui.button(this, "Bedienungshilfen öffnen", v ->
                        startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))),
                Ui.spaced(this));
        root.addView(Ui.button(this, "Passwort ändern", v -> {
            changingPassword = true;
            render();
        }));

        root.addView(Ui.text(this, "Apps antippen, um sie zu sperren:", 15), Ui.spaced(this));

        ListView list = new ListView(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        lp.topMargin = Ui.dp(this, 8);
        root.addView(list, lp);

        setContentView(root);

        new Thread(() -> {
            List<AppItem> items = loadApps();
            runOnUiThread(() -> {
                AppAdapter adapter = new AppAdapter(this, items);
                list.setAdapter(adapter);
                list.setOnItemClickListener((parent, view, position, id) -> {
                    AppItem item = items.get(position);
                    item.blocked = !item.blocked;
                    Prefs.setBlocked(this, item.pkg, item.blocked);
                    adapter.notifyDataSetChanged();
                });
            });
        }).start();
    }

    private boolean isServiceEnabled() {
        String enabled = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        String me = new ComponentName(this, BlockerService.class).flattenToString();
        return enabled != null && enabled.toLowerCase().contains(me.toLowerCase());
    }

    private List<AppItem> loadApps() {
        PackageManager pm = getPackageManager();
        Intent launcher = new Intent(Intent.ACTION_MAIN);
        launcher.addCategory(Intent.CATEGORY_LAUNCHER);

        Set<String> blocked = Prefs.blocked(this);
        Set<String> seen = new HashSet<>();
        List<AppItem> items = new ArrayList<>();

        for (ResolveInfo ri : pm.queryIntentActivities(launcher, 0)) {
            if (ri.activityInfo == null) {
                continue;
            }
            String pkg = ri.activityInfo.packageName;
            if (pkg.equals(getPackageName()) || !seen.add(pkg)) {
                continue;
            }
            AppItem item = new AppItem();
            item.pkg = pkg;
            CharSequence label = ri.loadLabel(pm);
            item.label = label == null ? pkg : label.toString();
            item.blocked = blocked.contains(pkg);
            items.add(item);
        }

        // Gesperrte Apps zuerst, danach alphabetisch
        Collections.sort(items, (a, b) -> {
            if (a.blocked != b.blocked) {
                return a.blocked ? -1 : 1;
            }
            return a.label.compareToIgnoreCase(b.label);
        });
        return items;
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    // ---------- Liste ----------

    private static final class AppItem {
        String pkg;
        String label;
        boolean blocked;
        Drawable icon;
    }

    private static final class Row {
        ImageView icon;
        TextView label;
        CheckBox check;
    }

    private static final class AppAdapter extends BaseAdapter {
        private final Context context;
        private final List<AppItem> items;

        AppAdapter(Context context, List<AppItem> items) {
            this.context = context;
            this.items = items;
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public Object getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Row row;
            if (convertView == null) {
                LinearLayout line = new LinearLayout(context);
                line.setOrientation(LinearLayout.HORIZONTAL);
                line.setGravity(Gravity.CENTER_VERTICAL);
                int pad = Ui.dp(context, 8);
                line.setPadding(0, pad, 0, pad);

                row = new Row();
                row.icon = new ImageView(context);
                int size = Ui.dp(context, 40);
                line.addView(row.icon, new LinearLayout.LayoutParams(size, size));

                row.label = new TextView(context);
                row.label.setTextSize(16);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                lp.leftMargin = Ui.dp(context, 12);
                line.addView(row.label, lp);

                row.check = new CheckBox(context);
                row.check.setClickable(false);
                row.check.setFocusable(false);
                line.addView(row.check);

                line.setTag(row);
                convertView = line;
            } else {
                row = (Row) convertView.getTag();
            }

            AppItem item = items.get(position);
            if (item.icon == null) {
                try {
                    item.icon = context.getPackageManager().getApplicationIcon(item.pkg);
                } catch (PackageManager.NameNotFoundException e) {
                    item.icon = context.getPackageManager().getDefaultActivityIcon();
                }
            }
            row.icon.setImageDrawable(item.icon);
            row.label.setText(item.label);
            row.check.setChecked(item.blocked);
            return convertView;
        }
    }
}
