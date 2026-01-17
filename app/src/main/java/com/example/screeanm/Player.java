package com.example.screeanm;

public class Player {
    private float x, y;
    private int health;
    private int maxHealth;
    private float speed = 10.0f;

    public Player(float startX, float startY, int maxHealth) {
        this.x = startX;
        this.y = startY;
        this.maxHealth = maxHealth;
        this.health = maxHealth;
    }

    public float getX() { return x; }
    public void setX(float x) { this.x = x; }

    public float getY() { return y; }
    public void setY(float y) { this.y = y; }

    public int getHealth() { return health; }
    public void setHealth(int health) { this.health = health; }

    public float getSpeed() { return speed; }

    public void move(float dx, float dy) {
        this.x += dx * speed;
        this.y += dy * speed;
    }

    public void takeDamage(int damage) {
        health -= damage;
        if (health < 0) health = 0;
    }
}