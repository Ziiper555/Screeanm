package com.example.screeanm;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class BattleActivity extends AppCompatActivity {

    private Player player;
    private Boss currentBoss;
    private ImageView ivPlayer;
    private FrameLayout battleBox;
    private View joystickKnob;
    private View joystickBase;

    private float joystickDeltaX = 0;
    private float joystickDeltaY = 0;
    private final Handler gameHandler = new Handler();
    private boolean isRunning = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_battle);

        ivPlayer = findViewById(R.id.ivPlayer);
        battleBox = findViewById(R.id.battleBox);
        joystickBase = findViewById(R.id.joystickBase);
        joystickKnob = findViewById(R.id.joystickKnob);
        
        TextView tvBossName = findViewById(R.id.tvBossName);
        ProgressBar pbBossHealth = findViewById(R.id.pbBossHealth);

        // Inicializar Boss
        String bossName = getIntent().getStringExtra("BOSS_NAME");
        if ("Shhhark".equals(bossName)) {
            currentBoss = new Shhhark();
        }
        
        if (currentBoss != null) {
            tvBossName.setText(currentBoss.getName());
            pbBossHealth.setMax(currentBoss.getMaxHealth());
            pbBossHealth.setProgress(currentBoss.getCurrentHealth());
        }

        // Inicializar Jugador cuando el layout esté listo para obtener dimensiones
        battleBox.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                battleBox.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                player = new Player(battleBox.getWidth() / 2f, battleBox.getHeight() / 2f, 100);
                updatePlayerView();
            }
        });

        setupJoystick();
        startGameLoop();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupJoystick() {
        joystickBase.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE:
                    float centerX = joystickBase.getWidth() / 2f;
                    float centerY = joystickBase.getHeight() / 2f;
                    
                    float dx = event.getX() - centerX;
                    float dy = event.getY() - centerY;
                    float distance = (float) Math.sqrt(dx * dx + dy * dy);
                    float maxRadius = joystickBase.getWidth() / 2f;

                    if (distance > maxRadius) {
                        dx = (dx / distance) * maxRadius;
                        dy = (dy / distance) * maxRadius;
                    }

                    joystickKnob.setTranslationX(dx);
                    joystickKnob.setTranslationY(dy);

                    // Normalizar para el movimiento del jugador (valor entre -1 y 1)
                    joystickDeltaX = dx / maxRadius;
                    joystickDeltaY = dy / maxRadius;
                    break;

                case MotionEvent.ACTION_UP:
                    joystickKnob.setTranslationX(0);
                    joystickKnob.setTranslationY(0);
                    joystickDeltaX = 0;
                    joystickDeltaY = 0;
                    break;
            }
            return true;
        });
    }

    private void startGameLoop() {
        Runnable gameRunnable = new Runnable() {
            @Override
            public void run() {
                if (isRunning) {
                    update();
                    gameHandler.postDelayed(this, 16); // ~60 FPS
                }
            }
        };
        gameHandler.post(gameRunnable);
    }

    private void update() {
        if (player != null) {
            player.move(joystickDeltaX, joystickDeltaY);
            checkBoundaries();
            updatePlayerView();
        }
        if (currentBoss != null) {
            currentBoss.update();
        }
    }

    private void checkBoundaries() {
        float halfPlayerW = ivPlayer.getWidth() / 2f;
        float halfPlayerH = ivPlayer.getHeight() / 2f;

        if (player.getX() < halfPlayerW) player.setX(halfPlayerW);
        if (player.getX() > battleBox.getWidth() - halfPlayerW) player.setX(battleBox.getWidth() - halfPlayerW);
        if (player.getY() < halfPlayerH) player.setY(halfPlayerH);
        if (player.getY() > battleBox.getHeight() - halfPlayerH) player.setY(battleBox.getHeight() - halfPlayerH);
    }

    private void updatePlayerView() {
        ivPlayer.setX(player.getX() - ivPlayer.getWidth() / 2f);
        ivPlayer.setY(player.getY() - ivPlayer.getHeight() / 2f);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isRunning = false;
    }
}
