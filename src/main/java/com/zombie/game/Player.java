package com.zombie.game;

import com.zombie.util.AssetLoader;
import com.zombie.util.Constants;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.Set;

/**
 * Player class representing the survivor character
 */
public class Player {
    
    private double x, y;
    private double width = 32;   // collision box
    private double height = 32;  // collision box
    private static final double RENDER_WIDTH = 120;   // visual size
    private static final double RENDER_HEIGHT = 150; // visual size
    private int health;
    private int lives;
    private boolean isDead;
    private double lastDamageTime;
    private double weaponBoostEndTime;
    private double armorEndTime;
    private Image characterImage;
    private String characterImagePath;
    
    // Character-specific fields
    private int characterId = 1;
    private double maxHealth = 100;
    private int baseDamage = 25;
    private double baseSpeed = Constants.PLAYER_SPEED;
    private String specialAbility = "NONE";
    private boolean abilityActive = false;
    private double abilityStartTime = 0;
    private double abilityCooldownEnd = 0;
    private static final double ABILITY_DURATION = 7.0; // 7 seconds active
    private static final double ABILITY_COOLDOWN = 20.0; // 20 seconds cooldown
    
    // New standardized ability system
    private double abilityTimer = 0;
    private boolean isEPressed = false; // Debounce for E key
    
    // Jax temporary health system
    private int temporaryHealth = 0;
    private static final int MAX_TEMPORARY_HEALTH = 200; // 15 HP per second

    // Jax shield
    private boolean shieldActive = false;

    // Ghost smoke
    private boolean smokeActive = false;
    
    // Animation system - static shared cache
    private static final java.util.HashMap<String, Image> spriteCache = new java.util.HashMap<>();
    private Image[] idleFrames = new Image[6];
    private Image[] walkFrames = new Image[8];
    private int currentFrame = 0;
    private double animationTimer = 0;
    private static final double IDLE_FRAME_DURATION = 0.12; // seconds per frame
    private static final double WALK_FRAME_DURATION = 0.08; // seconds per frame
    private boolean isMoving = false;
    private boolean facingLeft = false;
    
    public Player(double startX, double startY) {
        this.x = startX;
        this.y = startY;
        this.health = Constants.MAX_HEALTH;
        this.lives = Constants.INITIAL_LIVES;
        this.isDead = false;
        this.lastDamageTime = 0;
        this.weaponBoostEndTime = 0;
        this.armorEndTime = 0;
    }
    
    /**
     * Update player position based on input keys
     */
    public void update(Set<String> pressedKeys, double deltaTime, boolean[][] walkableGrid) {
        double dx = 0, dy = 0;
        double currentSpeed = baseSpeed;
        
        // Apply Flash's speed boost (double speed during ability)
        if (smokeActive && specialAbility.equals("SPEED_BOOST")) {
            currentSpeed *= 2.0; // Double speed for Flash
        }
        
        if (pressedKeys.contains("W") || pressedKeys.contains("UP")) {
            dy -= currentSpeed;
        }
        if (pressedKeys.contains("S") || pressedKeys.contains("DOWN")) {
            dy += currentSpeed;
        }
        if (pressedKeys.contains("A") || pressedKeys.contains("LEFT")) {
            dx -= currentSpeed;
        }
        if (pressedKeys.contains("D") || pressedKeys.contains("RIGHT")) {
            dx += currentSpeed;
        }
        
        // Calculate new position
        double newX = x + dx;
        double newY = y + dy;
        
        // Check boundaries
        int gridWidth = walkableGrid[0].length * Constants.TILE_SIZE;
        int gridHeight = walkableGrid.length * Constants.TILE_SIZE;
        
        newX = Math.max(0, Math.min(newX, gridWidth - width));
        newY = Math.max(0, Math.min(newY, gridHeight - height));
        
        // Check collision with walls
        if (canMoveTo(newX, newY, walkableGrid)) {
            x = newX;
            y = newY;
        }
        
        // Update animation
        boolean moving = pressedKeys.contains("W") || pressedKeys.contains("S") ||
                         pressedKeys.contains("A") || pressedKeys.contains("D") ||
                         pressedKeys.contains("UP") || pressedKeys.contains("DOWN") ||
                         pressedKeys.contains("LEFT") || pressedKeys.contains("RIGHT");
        double velocityX = 0;
        if (pressedKeys.contains("A") || pressedKeys.contains("LEFT")) velocityX = -1;
        if (pressedKeys.contains("D") || pressedKeys.contains("RIGHT")) velocityX = 1;
        updateAnimation(deltaTime, moving, velocityX);
    }
    
    /**
     * Check if player can move to the specified position
     */
    private boolean canMoveTo(double newX, double newY, boolean[][] walkableGrid) {
        // Check all four corners of the player rectangle
        int[] checkPoints = {
            0, 0, // Top-left
            (int) width, 0, // Top-right
            0, (int) height, // Bottom-left
            (int) width, (int) height // Bottom-right
        };
        
        for (int i = 0; i < checkPoints.length; i += 2) {
            int checkX = (int) (newX + checkPoints[i]);
            int checkY = (int) (newY + checkPoints[i + 1]);
            
            int tileX = checkX / Constants.TILE_SIZE;
            int tileY = checkY / Constants.TILE_SIZE;
            
            if (tileY >= 0 && tileY < walkableGrid.length && 
                tileX >= 0 && tileX < walkableGrid[0].length) {
                if (!walkableGrid[tileY][tileX]) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    /**
     * Take damage from zombies
     */
    public void takeDamage(int damage, double currentTime) {
        if (isDead || currentTime - lastDamageTime < Constants.DAMAGE_INTERVAL) {
            return;
        }

        // Apply armor reduction if active
        if (hasArmor(currentTime)) {
            damage = damage / 2;
        }

        health -= damage;
        lastDamageTime = currentTime;

        if (health <= 0) {
            health = 0;
            isDead = true;
        }
    }
    
    /**
     * Respawn player with full health
     */
    public void respawn(double spawnX, double spawnY) {
        this.x = spawnX;
        this.y = spawnY;
        this.health = Constants.MAX_HEALTH;
        this.isDead = false;
        this.lastDamageTime = 0;
    }
    
    /**
     * Lose a life
     */
    public void loseLife() {
        if (lives > 0) {
            lives--;
        }
    }
    
    /**
     * Heal player
     */
    public void heal(int amount) {
        health = Math.min(health + amount, Constants.MAX_HEALTH);
    }
    
    /**
     * Activate weapon boost
     */
    public void activateWeaponBoost(double currentTime) {
        weaponBoostEndTime = currentTime + Constants.WEAPON_BOOST_DURATION;
    }
    
    /**
     * Activate armor
     */
    public void activateArmor(double currentTime) {
        armorEndTime = currentTime + Constants.ARMOR_DURATION;
    }
    
    /**
     * Check if weapon boost is active
     */
    public boolean hasWeaponBoost(double currentTime) {
        return currentTime < weaponBoostEndTime;
    }
    
    /**
     * Check if armor is active
     */
    public boolean hasArmor(double currentTime) {
        return currentTime < armorEndTime;
    }
    
    /**
     * Get remaining weapon boost time
     */
    public double getWeaponBoostRemaining(double currentTime) {
        return Math.max(0, weaponBoostEndTime - currentTime);
    }
    
    /**
     * Get remaining armor time
     */
    public double getArmorRemaining(double currentTime) {
        return Math.max(0, armorEndTime - currentTime);
    }
    
    /**
     * Render the player
     */
    public void render(GraphicsContext gc, double cameraX, double cameraY) {
        double renderX = x - cameraX;
        double renderY = y - cameraY;

        // Center sprite horizontally on hitbox, align bottom of sprite to bottom of hitbox
        double spriteX = renderX + (width / 2) - (RENDER_WIDTH / 2);
        double spriteY = renderY + height - RENDER_HEIGHT;

        Image[] frames = isMoving ? walkFrames : idleFrames;
        Image currentImage = null;
        if (frames != null && currentFrame >= 0 && currentFrame < frames.length) {
            currentImage = frames[currentFrame];
        }

        if (currentImage != null && !currentImage.isError()) {
            if (facingLeft) {
                gc.save();
                gc.translate(spriteX + RENDER_WIDTH, spriteY);
                gc.scale(-1, 1);
                gc.drawImage(currentImage, 0, 0, RENDER_WIDTH, RENDER_HEIGHT);
                gc.restore();
            } else {
                gc.drawImage(currentImage, spriteX, spriteY, RENDER_WIDTH, RENDER_HEIGHT);
            }
        } else {
            // Only draw fallback if ALL frames are null (sprites never loaded)
            boolean anyFrameLoaded = false;
            if (frames != null) {
                for (Image f : frames) {
                    if (f != null && !f.isError()) { anyFrameLoaded = true; break; }
                }
            }
            if (!anyFrameLoaded) {
                gc.setFill(Color.BLUE);
                // Draw health bar
                // renderHealthBar(gc, renderX, renderY - 20); // Method doesn't exist, removing call
                gc.fillRect(renderX, renderY, width, height);
            }
            // else: skip drawing this frame entirely, invisible for 1 frame is better than a flash
        }
        
        // Uncomment to debug hitbox:
        // gc.setStroke(Color.RED);
        // gc.setLineWidth(1);
        // gc.strokeRect(renderX, renderY, width, height);
    }
    
    // Getters and setters
    public double getX() { return x; }
    public double getY() { return y; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
    public int getHealth() { return health; }
    public int getLives() { return lives; }
    public boolean isDead() { return isDead; }
    
    public void setCharacterImage(String imagePath) {
        this.characterImagePath = imagePath;
        this.characterImage = AssetLoader.loadImage(imagePath);
    }
    
    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }
    
    public void applyCharacterStats(int characterId) {
        this.characterId = characterId;
        switch (characterId) {
            case 1 -> { // Jax
                baseSpeed = 1.5; maxHealth = 100; health = 100; baseDamage = 25;
                specialAbility = "SHIELD";
            }
            case 2 -> { // Vi
                baseSpeed = 3.0; maxHealth = 60; health = 60; baseDamage = 50;
                specialAbility = "TRIPLE_SHOT";
            }
            case 3 -> { // Flash (renamed from Ghost)
                baseSpeed = 5.0; maxHealth = 80; health = 80; baseDamage = 15;
                specialAbility = "SPEED_BOOST";
            }
            default -> { baseSpeed = 3.0; maxHealth = 100; health = 100; baseDamage = 25; }
        }
    }
    
    public void loadCharacterSprites(int characterId) {
        idleFrames = new Image[6];
        walkFrames = new Image[8];
        String basePath = "/assets/characters/char" + characterId + "/";
        for (int frameIndex = 0; frameIndex < 6; frameIndex++) {
            final int currentIndex = frameIndex;
            String key = basePath + "idle_" + currentIndex;
            idleFrames[frameIndex] = spriteCache.computeIfAbsent(key, k -> {
                try {
                    var stream = Player.class.getResourceAsStream(basePath + "idle_" + currentIndex + ".png");
                    return stream != null ? new Image(stream) : null;
                } catch (Exception e) { return null; }
            });
        }
        for (int frameIndex = 0; frameIndex < 8; frameIndex++) {
            final int currentIndex = frameIndex;
            String key = basePath + "walk_" + currentIndex;
            walkFrames[frameIndex] = spriteCache.computeIfAbsent(key, k -> {
                try {
                    var stream = Player.class.getResourceAsStream(basePath + "walk_" + currentIndex + ".png");
                    return stream != null ? new Image(stream) : null;
                } catch (Exception e) { return null; }
            });
        }
    }
    
    public void updateAnimation(double deltaTime, boolean moving, double velocityX) {
        this.isMoving = moving;
        if (velocityX < 0) facingLeft = true;
        else if (velocityX > 0) facingLeft = false;

        animationTimer += deltaTime;
        double frameDuration = moving ? WALK_FRAME_DURATION : IDLE_FRAME_DURATION;
        int frameCount = moving ? 8 : 6;

        if (animationTimer >= frameDuration) {
            animationTimer = 0;
            currentFrame = (currentFrame + 1) % frameCount;
        }
    }
    
    public void activateAbility(double currentTime) {
        if (currentTime < abilityCooldownEnd || isEPressed) return; // still on cooldown or already pressed
        
        // Set debounce flag
        isEPressed = true;
        
        // Start ability with 7-second duration and 14-second cooldown
        abilityActive = true;
        abilityStartTime = currentTime;
        abilityCooldownEnd = currentTime + ABILITY_COOLDOWN;
        abilityTimer = ABILITY_DURATION;

        // Initialize ability-specific effects
        switch (specialAbility) {
            case "SHIELD" -> {
                shieldActive = true;
                maxHealth = 300; // Increase max health to 300
                health = Math.min(300, health + 200); // Add 200 HP, max 300 total
            }
            case "SPEED_BOOST" -> {
                smokeActive = true; // Reuse smoke flag for speed boost
            }
            case "TRIPLE_SHOT" -> {
                // Triple shot handled in Game.java during abilityActive window
            }
        }
    }

    public void updateAbilities(double deltaTime, double currentTime) {
        // Update standardized ability timer
        if (abilityActive) {
            abilityTimer -= deltaTime; // Use actual deltaTime from game loop
            
            if (abilityTimer <= 0) {
                // Ability duration expired - deactivate all effects
                abilityActive = false;
                shieldActive = false;
                
                // Revert Jax's max health back to 100
                if (specialAbility.equals("SHIELD")) {
                    maxHealth = 100; // Reset max health to base
                    if (health > 100) {
                        health = 100; // Cap health at 100
                    }
                }
                smokeActive = false;
                isEPressed = false; // Reset debounce flag
            } else {
                // Handle ongoing ability effects
                // Other abilities have their effects handled elsewhere
            }
        }
        
        // Reset E key debounce when key is released (handled in Game class)
    }
    
    // Getters for character-specific properties
    public boolean isShieldActive() { return shieldActive; }
    public void deactivateShield() { shieldActive = false; }
    public boolean isSmokeActive() { return smokeActive; }
    public boolean isPierceReady() { return abilityActive; }
    public void usePierce() { abilityActive = false; }
    public int getBaseDamage() { return baseDamage; }
    public double getAbilityCooldownRemaining(double currentTime) {
        return Math.max(0, abilityCooldownEnd - currentTime);
    }
    public double getMaxHealth() { return maxHealth; }
    public String getSpecialAbility() { return specialAbility; }
    public double getBaseSpeed() { return baseSpeed; }
    
    // New ability system getters
    public double getAbilityTimer() { return abilityTimer; }
    public boolean isAbilityActive() { return abilityActive; }
    public int getShieldHealth() { return temporaryHealth; }
    public int getMaxShieldHealth() { return MAX_TEMPORARY_HEALTH; }
    
    // E key debounce management
    public void resetEKeyPress() { isEPressed = false; }
}
