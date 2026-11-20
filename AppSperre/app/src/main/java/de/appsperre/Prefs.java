package de.appsperre;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Set;

/** Speichert Passwort (als Hash), gesperrte Apps und zeitweise Freigaben. */
final class Prefs {

    private static final String KEY_HASH = "pw_hash";
    private static final String KEY_SALT = "pw_salt";
    private static final String KEY_BLOCKED = "blocked";
    private static final String KEY_UNTIL = "until_";

    private Prefs() {
    }

    private static SharedPreferences sp(Context c) {
        return c.getApplicationContext().getSharedPreferences("applock", Context.MODE_PRIVATE);
    }

    // ---------- Passwort ----------

    static boolean hasPassword(Context c) {
        return sp(c).getString(KEY_HASH, null) != null;
    }

    static void setPassword(Context c, String password) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        String saltText = Base64.encodeToString(salt, Base64.NO_WRAP);
        sp(c).edit()
                .putString(KEY_SALT, saltText)
                .putString(KEY_HASH, hash(saltText, password))
                .apply();
    }

    static boolean checkPassword(Context c, String password) {
        String saltText = sp(c).getString(KEY_SALT, null);
        String stored = sp(c).getString(KEY_HASH, null);
        if (saltText == null || stored == null) {
            return false;
        }
        return stored.equals(hash(saltText, password));
    }

    private static String hash(String saltText, String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(saltText.getBytes(StandardCharsets.UTF_8));
            byte[] digest = md.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.encodeToString(digest, Base64.NO_WRAP);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ---------- Gesperrte Apps ----------

    static Set<String> blocked(Context c) {
        return new HashSet<>(sp(c).getStringSet(KEY_BLOCKED, new HashSet<String>()));
    }

    static void setBlocked(Context c, String pkg, boolean block) {
        Set<String> set = blocked(c);
        if (block) {
            set.add(pkg);
        } else {
            set.remove(pkg);
        }
        sp(c).edit()
                .putStringSet(KEY_BLOCKED, set)
                .remove(KEY_UNTIL + pkg)
                .apply();
    }

    // ---------- Zeitweise Freigabe ----------

    static void unlockFor(Context c, String pkg, long minutes) {
        long until = System.currentTimeMillis() + minutes * 60_000L;
        sp(c).edit().putLong(KEY_UNTIL + pkg, until).apply();
    }

    /** true, wenn die App gesperrt ist und gerade keine Freigabe laeuft. */
    static boolean shouldBlock(Context c, String pkg) {
        SharedPreferences p = sp(c);
        if (!p.getStringSet(KEY_BLOCKED, new HashSet<String>()).contains(pkg)) {
            return false;
        }
        return System.currentTimeMillis() >= p.getLong(KEY_UNTIL + pkg, 0L);
    }
}
