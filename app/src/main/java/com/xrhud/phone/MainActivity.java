package com.xrhud.phone;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends android.app.Activity {

    private static final int REQUEST_LOCATION = 100;

    private EditText deckIp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Let Android handle the system/navigation bars normally.
        Window window = getWindow();

        if (Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(true);
        }

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        // Normal content padding, with extra room at the bottom
        // so controls never sit underneath the navigation bar.
        layout.setPadding(40, 40, 40, 80);

        TextView title = new TextView(this);
        title.setText("XRHUD GPS");
        title.setTextSize(24);

        deckIp = new EditText(this);
        deckIp.setHint("Steam Deck IP");
        deckIp.setText("192.168.1.100");
        deckIp.setSingleLine(true);
        deckIp.setInputType(33);

        Button start = new Button(this);
        start.setText("START GPS");

        Button stop = new Button(this);
        stop.setText("STOP");

        layout.addView(title);
        layout.addView(deckIp);
        layout.addView(start);
        layout.addView(stop);

        // Apply the navigation-bar inset as additional bottom padding.
        if (Build.VERSION.SDK_INT >= 23) {
            layout.setOnApplyWindowInsetsListener((view, insets) -> {

                int bottomInset = 0;

                if (Build.VERSION.SDK_INT >= 30) {
                    bottomInset = insets.getInsets(
                            WindowInsets.Type.navigationBars()
                    ).bottom;
                } else {
                    bottomInset = insets.getSystemWindowInsetBottom();
                }

                view.setPadding(
                        40,
                        40,
                        40,
                        80 + bottomInset
                );

                return insets;
            });

            layout.requestApplyInsets();
        }

        setContentView(layout);

        start.setOnClickListener(v -> startGps());

        stop.setOnClickListener(v -> {
            stopService(new Intent(this, GpsService.class));
        });
    }

    private void startGps() {

        if (checkSelfPermission(
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    REQUEST_LOCATION
            );

            return;
        }

        Intent intent = new Intent(this, GpsService.class);

        intent.putExtra(
                GpsService.EXTRA_DECK_IP,
                deckIp.getText().toString().trim()
        );

        if (Build.VERSION.SDK_INT >= 26) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }
}
