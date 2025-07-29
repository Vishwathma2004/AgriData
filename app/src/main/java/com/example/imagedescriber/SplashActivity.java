package com.example.imagedescriber;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 2000; // Total splash duration
    private static final int UPDATE_INTERVAL = 20;   // Progress update interval

    private ProgressBar progressBar;
    private TextView percentageText;
    private Handler handler;
    private int progress = 0;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        hideSystemUI();

        mAuth = FirebaseAuth.getInstance();

        ImageView splashLogo = findViewById(R.id.splash_logo);
        TextView splashText = findViewById(R.id.splash_text);
        progressBar = findViewById(R.id.splash_progress_bar);
        percentageText = findViewById(R.id.splash_percentage_text);

        // Apply animations
        Animation scale = AnimationUtils.loadAnimation(this, R.anim.scale_animation);
        Animation fade = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        splashLogo.startAnimation(scale);
        splashText.startAnimation(fade);

        handler = new Handler(Looper.getMainLooper());
        simulateProgress();
    }

    private void simulateProgress() {
        final int maxProgress = 100;

        Runnable updateTask = new Runnable() {
            @Override
            public void run() {
                progress++;
                if (progress <= maxProgress) {
                    progressBar.setProgress(progress);
                    percentageText.setText("Loading... " + progress + "%");
                    handler.postDelayed(this, UPDATE_INTERVAL);
                } else {
                    checkLoginStatus();
                }
            }
        };
        handler.post(updateTask);
    }

    private void checkLoginStatus() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean("isLoggedIn", false);

        if (currentUser != null && isLoggedIn) {
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
        } else {
            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
        }

        finish();
    }

    private void hideSystemUI() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            final WindowInsetsController insetsController = getWindow().getInsetsController();
            if (insetsController != null) {
                insetsController.hide(WindowInsets.Type.statusBars());
                insetsController.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    android.view.View.SYSTEM_UI_FLAG_FULLSCREEN |
                            android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                            android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
        }
    }
}
