package com.example.screeanm;

public abstract class Boss {
    protected String name;
    protected float maxHealth;
    protected float currentHealth;

    public Boss(String name, float maxHealth) {
        this.name = name;
        this.maxHealth = maxHealth;
        this.currentHealth = maxHealth;
    }

    public String getName() {
        return name;
    }

    public float getMaxHealth() {
        return maxHealth;
    }

    public float getCurrentHealth() {
        return currentHealth;
    }

    public void takeDamage(float damage) {
        currentHealth -= damage;
        if (currentHealth < 0) currentHealth = 0;
    }

    public boolean isDead() {
        return currentHealth <= 0;
    }

    // Método abstracto para el patrón de ataque
    public abstract void update();
}