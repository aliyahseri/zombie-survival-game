package com.zombie.game;

import com.zombie.ai.AStarPathfinder;
import com.zombie.util.AssetLoader;
import com.zombie.util.Constants;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.List;

/**
 * Zombie class representing enemy entities
 */
public class Zombie {
    
    private double x, y;
    private double width = 25;
    private double height = 25;
    private int health = 100;
    private boolean isDead;
    private double speed;
    private int damage;
    private double lastDamageTime;
    private int pathRecalculationCounter;
    private List<AStarPathfinder.Node> currentPath;
    private int currentPathIndex;
    
    // Animation system - static shared cache
    private static Image[] idleFrames = null;
    private static Image[] walkFrames = null;
    private static boolean spritesLoaded = false;
    private int currentFrame = 0;
    private double animationTimer = 0;
    private static final double IDLE_FRAME_DURATION = 0.15;
    private static final double WALK_FRAME_DURATION = 0.10;
    private boolean isMoving = false;
    private boolean facingLeft = false;
    private static final double RENDER_WIDTH = 120;
    private static final double RENDER_HEIGHT = 150;
    private int maxHealth = 100;
    
    public Zombie(double startX, double startY, int difficulty) {
        this.x = startX;
        this.y = startY;
        this.isDead = false;
        this.lastDamageTime = 0;
        this.pathRecalculationCounter = 0;
        this.currentPathIndex = 0;
        
        // Set stats based on difficulty
        this.speed = Constants.ZOMBIE_SPEED;
        this.damage = Constants.ZOMBIE_DAMAGE[difficulty];
        this.maxHealth = 100;
        
        // Load zombie sprites
        loadZombieSprites();
    }
    
    /**
     * Load zombie sprite animations - static shared cache
     */
    private static synchronized void loadZombieSprites() {
        if (spritesLoaded) return;
        idleFrames = new Image[6];
        walkFrames = new Image[8];
        String basePath = "/assets/enemies/";
        for (int i = 0; i < 6; i++) {
            try {
                var stream = Zombie.class.getResourceAsStream(basePath + "idle_" + i + ".png");
                if (stream != null) idleFrames[i] = new Image(stream);
            } catch (Exception e) {
                idleFrames[i] = null;
            }
        }
        for (int i = 0; i < 8; i++) {
            try {
                var stream = Zombie.class.getResourceAsStream(basePath + "walk_" + i + ".png");
                if (stream != null) walkFrames[i] = new Image(stream);
            } catch (Exception e) {
                walkFrames[i] = null;
            }
        }
        spritesLoaded = true;
    }
    
    /**
     * Preload zombie sprites - call once before spawning zombies
     */
    public static void preloadSprites() {
        loadZombieSprites();
    }
    
    /**
     * Update zombie movement using A* pathfinding
     */
    public void update(Player player, boolean[][] walkableGrid, AStarPathfinder pathfinder, double deltaTime) {
        if (isDead) return;
        
        // Store previous position for animation
        double prevX = x;
        double prevY = y;
        
        // Recalculate path periodically
        pathRecalculationCounter++;
        if (pathRecalculationCounter >= Constants.PATH_RECALCULATION_INTERVAL || 
            currentPath == null || currentPathIndex >= currentPath.size()) {
            
            calculatePath(player, walkableGrid, pathfinder);
            pathRecalculationCounter = 0;
        }
        
        // If A* fails, move directly toward player
        if (currentPath == null || currentPath.isEmpty()) {
            double dx = (player.getX() + player.getWidth() / 2) - (x + width / 2);
            double dy = (player.getY() + player.getHeight() / 2) - (y + height / 2);
            double dist = Math.sqrt(dx * dx + dy * dy);
            if (dist > 0) {
                x += (dx / dist) * speed;
                y += (dy / dist) * speed;
            }
            return;
        }
        
        // Move along path
        if (currentPath != null && currentPathIndex < currentPath.size()) {
            AStarPathfinder.Node targetNode = currentPath.get(currentPathIndex);
            double targetX = targetNode.x * Constants.TILE_SIZE + Constants.TILE_SIZE / 2 - width / 2;
            double targetY = targetNode.y * Constants.TILE_SIZE + Constants.TILE_SIZE / 2 - height / 2;
            
            double dx = targetX - x;
            double dy = targetY - y;
            double distance = Math.sqrt(dx * dx + dy * dy);
            
            if (distance < 5) {
                // Reached current waypoint, move to next
                currentPathIndex++;
            } else if (distance > 0) {
                // Move towards target
                double moveX = (dx / distance) * speed;
                double moveY = (dy / distance) * speed;
                
                x += moveX;
                y += moveY;
            }
        }
        
        // Detect movement for animation
        double velocityX = x - prevX;
        boolean moving = Math.abs(velocityX) > 0.1 || Math.abs(y - prevY) > 0.1;
        updateAnimation(deltaTime, moving, velocityX);
    }
    
    /**
     * Update zombie animation
     */
    private void updateAnimation(double deltaTime, boolean moving, double velocityX) {
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
    
    /**
     * Calculate path to player using A* algorithm
     */
    private void calculatePath(Player player, boolean[][] walkableGrid, AStarPathfinder pathfinder) {
        int zombieTileX = (int) (x + width / 2) / Constants.TILE_SIZE;
        int zombieTileY = (int) (y + height / 2) / Constants.TILE_SIZE;
        int playerTileX = (int) (player.getX() + player.getWidth() / 2) / Constants.TILE_SIZE;
        int playerTileY = (int) (player.getY() + player.getHeight() / 2) / Constants.TILE_SIZE;
        
        currentPath = pathfinder.findPath(zombieTileX, zombieTileY, playerTileX, playerTileY);
        currentPathIndex = 0;
    }
    
    /**
     * Check if zombie is in range to damage player
     */
    public boolean canDamagePlayer(Player player, double currentTime) {
        if (isDead || currentTime - lastDamageTime < Constants.DAMAGE_INTERVAL) {
            return false;
        }
        
        Rectangle zombieBounds = getBounds();
        Rectangle playerBounds = player.getBounds();
        
        return zombieBounds.getBoundsInParent().intersects(playerBounds.getBoundsInParent());
    }
    
    /**
     * Damage player
     */
    public int damagePlayer(Player player, double currentTime) {
        if (!canDamagePlayer(player, currentTime)) {
            return 0;
        }
        
        player.takeDamage(damage, currentTime);
        lastDamageTime = currentTime;
        return damage;
    }
    
    /**
     * Take damage from player
     */
    public void takeDamage(int damage) {
        health -= damage;
        if (health <= 0) {
            health = 0;
            isDead = true;
        }
    }
    
    /**
     * Check if zombie should drop an item
     */
    public boolean shouldDropItem() {
        return isDead && Math.random() < Constants.DROP_CHANCE;
    }
    
    /**
     * Get drop type (0 = weapon boost, 1 = armor)
     */
    public int getDropType() {
        return Math.random() < 0.5 ? 0 : 1;
    }
    
    /**
     * Render the zombie
     */
    public void render(GraphicsContext gc, double cameraX, double cameraY) {
        if (isDead) return;
        
        double renderX = x - cameraX;
        double renderY = y - cameraY;

        // Center sprite on hitbox, align bottom of sprite to bottom of hitbox
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
                gc.setFill(Color.DARKGREEN);
                gc.fillRect(renderX, renderY, width, height);
                gc.setStroke(Color.BLACK);
                gc.setLineWidth(1);
                gc.strokeRect(renderX, renderY, width, height);
            }
            // else: skip drawing this frame entirely, invisible for 1 frame is better than a flash
        }
        
                
        // Always show health bar so it never pops in/out
        if (health < maxHealth) {
            double barWidth = 40;
            double barHeight = 5;
            // Position bar directly above the hitbox, not above the sprite
            double barX = renderX + (width / 2) - (barWidth / 2);
            double barY = renderY - 45; // 40px above the collision box

            // Background (red)
            gc.setFill(Color.RED);
            gc.fillRect(barX, barY, barWidth, barHeight);

            // Health fill (green to red gradient based on health)
            double healthPct = (double) health / maxHealth;
            Color barColor = healthPct > 0.5 ? Color.LIMEGREEN : healthPct > 0.25 ? Color.ORANGE : Color.RED;
            gc.setFill(barColor);
            gc.fillRect(barX, barY, barWidth * healthPct, barHeight);

            // Border
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(1);
            gc.strokeRect(barX, barY, barWidth, barHeight);
        }
    }
    
    // Getters and setters
    public double getX() { return x; }
    public double getY() { return y; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
    public int getHealth() { return health; }
    public boolean isDead() { return isDead; }
    public int getDamage() { return damage; }
    
    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }
}
