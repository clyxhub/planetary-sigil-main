package com.clyxhub6.planetarysigils;

import android.os.Bundle;
import android.view.View;
import android.view.Window;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        // Plugin registration MUST happen before super.onCreate(), which builds
        // the Capacitor bridge. Registering afterwards leaves the plugin out of
        // the bridge, so every native call fails with
        // "PlanetaryAlarm plugin is not implemented on android".
        registerPlugin(PlanetaryAlarmPlugin.class);
        super.onCreate(savedInstanceState);

        // Edge-to-edge: draw web content behind the status bar so the notch area
        // is filled. The web layer already pads for the top inset.
        Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);
        WindowCompat.getInsetsController(window, window.getDecorView()).setAppearanceLightStatusBars(false);

        // Apply the *bottom* system-bar inset (navigation/gesture bar) natively.
        // The AdMob plugin parents the banner into android.R.id.content, so
        // padding that container's bottom lifts the banner above the nav bar.
        // On Android WebView env(safe-area-inset-bottom) is always 0, so this
        // cannot be done from CSS — it must be a real WindowInsets read.
        final View content = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(content, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, 0, bars.right, bars.bottom);
            return insets;
        });
    }
}
