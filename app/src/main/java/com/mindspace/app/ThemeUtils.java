package com.mindspace.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Toast;

public class ThemeUtils {

    public static void setTheme(Context context, int attrId, Integer primary, Integer secondary) {
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            activity.recreate();
        }
    }

    public static void saveScheme(Context context, String scheme) {
        SharedPreferences prefs = context.getSharedPreferences("mindspace", Context.MODE_PRIVATE);
        prefs.edit().putString("scheme", scheme).apply();
    }

    public static String loadScheme(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("mindspace", Context.MODE_PRIVATE);
        return prefs.getString("scheme", null);
    }
}
