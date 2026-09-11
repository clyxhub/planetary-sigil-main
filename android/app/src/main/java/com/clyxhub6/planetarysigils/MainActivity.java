package com.clyxhub6.planetarysigils;

import android.os.Build;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
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

        // Edge-to-edge: draw web content behind the status bar so
        // env(safe-area-inset-top) in the app is honored on notched phones.
        Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);
        WindowCompat.getInsetsController(window, window.getDecorView()).setAppearanceLightStatusBars(false);
    }
}
