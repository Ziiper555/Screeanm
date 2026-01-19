package com.example.screeanm;

import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

public class Shoot {
    private ImageView view;
    private float x, y;
    private float speed = 35.0f;
    private boolean active = true;
    
    private int resId1, resId2;
    private long lastSwitchTime;
    private int currentFrame = 1;
    private static final long SWITCH_INTERVAL = 250; // 100ms

    public Shoot(ImageView view, float startX, float startY, int resId1, int resId2) {
        this.view = view;
        this.x = startX;
        this.y = startY;
        this.resId1 = resId1;
        this.resId2 = resId2;
        this.lastSwitchTime = System.currentTimeMillis();
        
        if (view.getLayoutParams() != null) {
            view.setX(x - view.getLayoutParams().width / 2f);
            view.setY(y - view.getLayoutParams().height / 2f);
        }
    }

    public void update() {
        y -= speed;
        
        // Animación: cambiar sprite cada intervalo
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastSwitchTime >= SWITCH_INTERVAL) {
            currentFrame = (currentFrame == 1) ? 2 : 1;
            view.setImageResource(currentFrame == 1 ? resId1 : resId2);
            disableFiltering();
            lastSwitchTime = currentTime;
        }

        if (view != null) {
            view.setY(y - view.getLayoutParams().height / 2f);
            view.setX(x - view.getLayoutParams().width / 2f);
        }
        
        if (y < -300) {
            active = false;
        }
    }

    private void disableFiltering() {
        if (view == null) return;
        Drawable drawable = view.getDrawable();
        if (drawable instanceof BitmapDrawable) {
            ((BitmapDrawable) drawable).setFilterBitmap(false);
            ((BitmapDrawable) drawable).setAntiAlias(false);
        }
    }

    public boolean isActive() {
        return active;
    }

    public void deactivate() {
        this.active = false;
    }

    public ImageView getView() {
        return view;
    }

    public float getX() { return x; }
    public float getY() { return y; }
}
