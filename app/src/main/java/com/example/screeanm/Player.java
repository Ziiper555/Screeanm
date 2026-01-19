package com.example.screeanm;

public class Player {
    private float x, y;
    private float health;
    private float maxHealth;
    private float speed = 30.0f;
    private float growthRate = 1.5f; 
    
    private float minScale = 2f; 
    private float maxScale = 5.0f; 
    
    private float hitboxFactor = 0.25f;

    // Parámetros de ataque y sonido
    private float chargeProgress = 0;
    private boolean isCharged = false;
    private float baseChargeRequirement = 10f; // Más rápido cuando es pequeño
    private float chargeScaleFactor = 0.4f;   // Escala con el tamaño
    private float attackHealthLoss = 10.0f;    // Parámetro de vida perdida al atacar
    private float soundThreshold = 2000.0f;   // Umbral mínimo de sonido (fuerza del grito)

    public Player(float startX, float startY, float maxHealth) {
        this.x = startX;
        this.y = startY;
        this.maxHealth = maxHealth;
        this.health = 20; // Empezamos con un poco más de vida para que no pierda al primer ataque
    }

    public float getX() { return x; }
    public void setX(float x) { this.x = x; }

    public float getY() { return y; }
    public void setY(float y) { this.y = y; }

    public float getHealth() { return health; }
    public void setHealth(float health) { this.health = health; }
    
    public float getHitboxFactor() { return hitboxFactor; }

    public boolean isCharged() { return isCharged; }

    public float getAttackHealthLoss() { return attackHealthLoss; }
    public void setAttackHealthLoss(float attackHealthLoss) { this.attackHealthLoss = attackHealthLoss; }

    public float getSoundThreshold() { return soundThreshold; }
    public void setSoundThreshold(float soundThreshold) { this.soundThreshold = soundThreshold; }

    public void move(float dx, float dy) {
        this.x += dx * speed;
        this.y += dy * speed;
    }

    public void growByScreaming(float factor) {
        this.health += growthRate * factor; 
        if (this.health > maxHealth) this.health = maxHealth;
        
        // Lógica de carga: aumenta al gritar
        if (!isCharged) {
            chargeProgress += 1.5f * factor; // Carga un poco más rápido
            if (chargeProgress >= getRequiredCharge()) {
                isCharged = true;
                chargeProgress = getRequiredCharge();
            }
        }
    }

    public void decayCharge() {
        // En lugar de resetear a 0, baja gradualmente
        if (!isCharged) {
            chargeProgress -= 0.5f; 
            if (chargeProgress < 0) chargeProgress = 0;
        }
    }

    public float getRequiredCharge() {
        // El tiempo de carga aumenta con la vida (personaje más grande)
        return baseChargeRequirement + (health * chargeScaleFactor);
    }

    public void resetCharge() {
        chargeProgress = 0;
        isCharged = false;
    }

    public void performAttack() {
        resetCharge();
        health -= attackHealthLoss;
        // Quitamos el tope de 5 para que el ataque pueda llevarte a perder
    }

    public float getScale() {
        return minScale + (health / maxHealth) * (maxScale - minScale);
    }

    public int getPhase() {
        if (health < 25) return 1;
        if (health < 50) return 2;
        if (health < 75) return 3;
        return 4;
    }

    public void takeDamage(float damage) {
        health -= damage;
        if (health < 0) health = 0;
    }
}
