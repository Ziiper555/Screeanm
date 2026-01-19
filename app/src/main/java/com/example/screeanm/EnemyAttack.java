package com.example.screeanm;

import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

public class EnemyAttack {
    private ImageView view;
    private float x, y;
    private float vx, vy;
    private boolean active = true;

    public EnemyAttack(ImageView view, float startX, float startY, float vx, float vy) {
        this.view = view;
        this.x = startX;
        this.y = startY;
        this.vx = vx;
        this.vy = vy;

        if (view.getLayoutParams() != null) {
            view.setX(x - view.getLayoutParams().width / 2f);
            view.setY(y - view.getLayoutParams().height / 2f);
        }
    }

    public void update() {
        x += vx;
        y += vy;

        if (view != null) {
            view.setX(x - view.getLayoutParams().width / 2f);
            view.setY(y - view.getLayoutParams().height / 2f);
        }

        // Desactivar si sale mucho de la pantalla (ajustar según sea necesario)
        if (x < -200 || x > 2000 || y < -200 || y > 2000) {
            active = false;
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

    public void disableFiltering() {
        if (view == null) return;
        Drawable drawable = view.getDrawable();
        if (drawable instanceof BitmapDrawable) {
            ((BitmapDrawable) drawable).setFilterBitmap(false);
            ((BitmapDrawable) drawable).setAntiAlias(false);
        }
    }
}
