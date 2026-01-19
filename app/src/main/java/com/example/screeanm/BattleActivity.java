package com.example.screeanm;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class BattleActivity extends AppCompatActivity {

    private Player player;
    private Boss currentBoss;
    private ImageView ivPlayer;
    private ImageView ivBoss;
    private FrameLayout battleBox;
    private FrameLayout mainContainer;
    private ProgressBar pbBossHealth;
    private View joystickKnob;
    private View joystickBase;

    private float joystickDeltaX = 0;
    private float joystickDeltaY = 0;
    private final Handler gameHandler = new Handler();
    private boolean isRunning = true;
    
    private int currentPhase = 0;
    private boolean wasCharged = false;

    private List<Shoot> shoots = new ArrayList<>();
    private List<EnemyAttack> enemyAttacks = new ArrayList<>();
    private final Random random = new Random();
    private long lastEnemyAttackTime = 0;
    private static final long ENEMY_ATTACK_INTERVAL = 1500; 

    private AudioRecord audioRecord;
    private int bufferSize;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private boolean permissionToRecordAccepted = false;
    private final String[] permissions = {Manifest.permission.RECORD_AUDIO};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_battle);

        mainContainer = findViewById(android.R.id.content);
        ivPlayer = findViewById(R.id.ivPlayer);
        ivBoss = findViewById(R.id.ivBoss);
        battleBox = findViewById(R.id.battleBox);
        joystickBase = findViewById(R.id.joystickBase);
        joystickKnob = findViewById(R.id.joystickKnob);
        
        TextView tvBossName = findViewById(R.id.tvBossName);
        pbBossHealth = findViewById(R.id.pbBossHealth);

        disableFiltering(ivPlayer);
        disableFiltering(ivBoss);

        String bossName = getIntent().getStringExtra("BOSS_NAME");
        if ("Shhhark".equals(bossName)) {
            currentBoss = new Shhhark();
        }
        
        if (currentBoss != null) {
            tvBossName.setText(currentBoss.getName());
            pbBossHealth.setMax((int) currentBoss.getMaxHealth());
            pbBossHealth.setProgress((int) currentBoss.getCurrentHealth());
        }

        battleBox.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                battleBox.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                player = new Player(battleBox.getWidth() / 2f, battleBox.getHeight() / 2f, 100);
                updatePlayerView();
            }
        });

        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);

        setupJoystick();
        startGameLoop();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            permissionToRecordAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
        }
        if (permissionToRecordAccepted) {
            startAudioRecording();
        }
    }

    @SuppressLint("MissingPermission")
    private void startAudioRecording() {
        bufferSize = AudioRecord.getMinBufferSize(8000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, 8000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
        audioRecord.startRecording();
    }

    private float getAmplitude() {
        if (audioRecord == null || audioRecord.getState() != AudioRecord.STATE_INITIALIZED || audioRecord.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
            return 0;
        }
        short[] buffer = new short[bufferSize];
        int read = audioRecord.read(buffer, 0, bufferSize);
        float max = 0;
        for (int i = 0; i < read; i++) {
            if (Math.abs(buffer[i]) > max) {
                max = Math.abs(buffer[i]);
            }
        }
        return max;
    }

    private void disableFiltering(ImageView imageView) {
        if (imageView == null) return;
        imageView.post(() -> {
            Drawable drawable = imageView.getDrawable();
            if (drawable instanceof BitmapDrawable) {
                ((BitmapDrawable) drawable).setFilterBitmap(false);
                ((BitmapDrawable) drawable).setAntiAlias(false);
            }
        });
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
                    gameHandler.postDelayed(this, 16);
                }
            }
        };
        gameHandler.post(gameRunnable);
    }

    private void update() {
        if (!isRunning) return;

        if (player != null) {
            player.move(joystickDeltaX, joystickDeltaY);
            
            float amplitude = getAmplitude();
            if (amplitude > player.getSoundThreshold()) { 
                player.growByScreaming(1.0f);
            } else {
                if (player.isCharged()) {
                    spawnShoot();
                    player.performAttack();
                } else {
                    player.decayCharge();
                }
            }

            // Condición de derrota: 5 de vida o menos
            if (player.getHealth() <= 5) {
                endGame(false);
                return;
            }

            updatePhaseAndSprite();

            float scale = player.getScale();
            ivPlayer.setScaleX(scale);
            ivPlayer.setScaleY(scale);

            checkBoundaries();
            updatePlayerView();

            long currentTime = System.currentTimeMillis();
            if (currentTime - lastEnemyAttackTime > ENEMY_ATTACK_INTERVAL) {
                spawnEnemyAttack();
                lastEnemyAttackTime = currentTime;
            }
        }

        Iterator<Shoot> it = shoots.iterator();
        while (it.hasNext()) {
            Shoot shoot = it.next();
            shoot.update();
            
            if (checkPreciseCollision(shoot.getView(), ivBoss, 0.5f)) {
                if (currentBoss != null) {
                    currentBoss.takeDamage(10);
                    pbBossHealth.setProgress((int) currentBoss.getCurrentHealth());
                    
                    if (currentBoss.isDead()) {
                        endGame(true);
                        return;
                    }
                }
                shoot.deactivate();
            }

            if (!shoot.isActive()) {
                mainContainer.removeView(shoot.getView());
                it.remove();
            }
        }

        Iterator<EnemyAttack> itE = enemyAttacks.iterator();
        while (itE.hasNext()) {
            EnemyAttack attack = itE.next();
            attack.update();

            if (checkPlayerCollision(attack)) {
                player.takeDamage(10);
                attack.deactivate();
            }

            if (!attack.isActive()) {
                mainContainer.removeView(attack.getView());
                itE.remove();
            }
        }

        if (currentBoss != null) {
            currentBoss.update();
        }
    }

    private void endGame(boolean win) {
        isRunning = false;
        String message = win ? "¡HAS GANADO!" : "HAS PERDIDO...";
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(BattleActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        }, 2000);
    }

    private boolean checkPreciseCollision(View v1, View v2, float factorV2) {
        Rect r1 = new Rect();
        v1.getGlobalVisibleRect(r1);
        Rect r2 = new Rect();
        v2.getGlobalVisibleRect(r2);
        
        int width = r2.width();
        int height = r2.height();
        int newWidth = (int) (width * factorV2);
        int newHeight = (int) (height * factorV2);
        int offsetX = (width - newWidth) / 2;
        int offsetY = (height - newHeight) / 2;
        
        Rect preciseR2 = new Rect(r2.left + offsetX, r2.top + offsetY, r2.right - offsetX, r2.bottom - offsetY);
        return Rect.intersects(r1, preciseR2);
    }

    private boolean checkPlayerCollision(EnemyAttack attack) {
        if (player == null || ivPlayer.getWidth() <= 0) return false;
        int[] boxLoc = new int[2];
        battleBox.getLocationInWindow(boxLoc);
        float pCenterX = boxLoc[0] + player.getX();
        float pCenterY = boxLoc[1] + player.getY();
        float visualWidth = ivPlayer.getWidth() * ivPlayer.getScaleX();
        float playerRadius = (visualWidth * player.getHitboxFactor()) / 2f;
        float aCenterX = attack.getX();
        float aCenterY = attack.getY();
        float attackRadius = (attack.getView().getLayoutParams().width * 0.5f) / 2f;
        float dx = pCenterX - aCenterX;
        float dy = pCenterY - aCenterY;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        return distance < (playerRadius + attackRadius);
    }

    private void spawnEnemyAttack() {
        ImageView attackView = new ImageView(this);
        int resId = getResources().getIdentifier("shoot_little_1", "drawable", getPackageName());
        if (resId != 0) {
            attackView.setImageResource(resId);
            attackView.setColorFilter(0xFFFF0000); 
            int size = 40;
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(size, size);
            attackView.setLayoutParams(params);
            mainContainer.addView(attackView);
            int side = random.nextInt(4);
            float startX, startY;
            int margin = 100;
            int w = mainContainer.getWidth();
            int h = mainContainer.getHeight();
            if (side == 0) { startX = random.nextFloat() * w; startY = -margin; }
            else if (side == 1) { startX = w + margin; startY = random.nextFloat() * h; }
            else if (side == 2) { startX = random.nextFloat() * w; startY = h + margin; }
            else { startX = -margin; startY = random.nextFloat() * h; }
            int[] boxLoc = new int[2];
            battleBox.getLocationInWindow(boxLoc);
            float targetX = boxLoc[0] + player.getX();
            float targetY = boxLoc[1] + player.getY();
            float dx = targetX - startX;
            float dy = targetY - startY;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            float speed = 8.0f + random.nextFloat() * 7.0f;
            float vx = (dx / dist) * speed;
            float vy = (dy / dist) * speed;
            EnemyAttack attack = new EnemyAttack(attackView, startX, startY, vx, vy);
            attack.disableFiltering();
            enemyAttacks.add(attack);
        }
    }

    private void spawnShoot() {
        ImageView shootView = new ImageView(this);
        String prefix = (player.getPhase() <= 2) ? "shoot_little_" : "shoot_big_";
        int resId1 = getResources().getIdentifier(prefix + "1", "drawable", getPackageName());
        int resId2 = getResources().getIdentifier(prefix + "2", "drawable", getPackageName());
        if (resId1 != 0 && resId2 != 0) {
            shootView.setImageResource(resId1);
            disableFiltering(shootView);
            int size = (player.getPhase() <= 2) ? 60 : 120;
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(size, size);
            shootView.setLayoutParams(params);
            mainContainer.addView(shootView);
            int[] boxLoc = new int[2];
            battleBox.getLocationInWindow(boxLoc);
            float startX = boxLoc[0] + player.getX();
            float startY = boxLoc[1] + player.getY();
            shoots.add(new Shoot(shootView, startX, startY, resId1, resId2));
        }
    }

    private void updatePhaseAndSprite() {
        int newPhase = player.getPhase();
        boolean isCharged = player.isCharged();
        if (newPhase != currentPhase || isCharged != wasCharged) {
            currentPhase = newPhase;
            wasCharged = isCharged;
            String type = isCharged ? "on" : "off";
            int resId = getResources().getIdentifier("ball_" + type + "_" + currentPhase, "drawable", getPackageName());
            if (resId != 0) {
                ivPlayer.setImageResource(resId);
                disableFiltering(ivPlayer);
            }
        }
    }

    private void checkBoundaries() {
        if (ivPlayer.getWidth() <= 0) return;
        float visualWidth = ivPlayer.getWidth() * ivPlayer.getScaleX();
        float visualHeight = ivPlayer.getHeight() * ivPlayer.getScaleY();
        float solidWidth = visualWidth * player.getHitboxFactor();
        float solidHeight = visualHeight * player.getHitboxFactor();
        float halfW = solidWidth / 2f;
        float halfH = solidHeight / 2f;
        if (solidWidth < battleBox.getWidth()) {
            if (player.getX() < halfW) player.setX(halfW);
            if (player.getX() > battleBox.getWidth() - halfW) player.setX(battleBox.getWidth() - halfW);
        } else { player.setX(battleBox.getWidth() / 2f); }
        if (solidHeight < battleBox.getHeight()) {
            if (player.getY() < halfH) player.setY(halfH);
            if (player.getY() > battleBox.getHeight() - halfH) player.setY(battleBox.getHeight() - halfH);
        } else { player.setY(battleBox.getHeight() / 2f); }
    }

    private void updatePlayerView() {
        if (ivPlayer.getWidth() <= 0) return;
        ivPlayer.setTranslationX(player.getX() - ivPlayer.getWidth() / 2f);
        ivPlayer.setTranslationY(player.getY() - ivPlayer.getHeight() / 2f);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isRunning = false;
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
        }
    }
}