package com.zombie.game;

import com.zombie.util.AssetLoader;
import com.zombie.util.Constants;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.shape.Rectangle;

/**
 * Bullet class for player shooting system
 */
public class Bullet {
    
    // Static image field for bullet sprite
    private static final Image BULLET_SPRITE = AssetLoader.loadImage(Constants.ITEMS_PATH + "bullet.png");
    
    private double x, y;
    private double dirX, dirY;
    private double speed = 400; // pixels per second
    private double radius = 7.0; // Increased to 7.0 (14px diameter, 5px larger than current 9px)
    private boolean active = true;
    
    // Piercing support
    private boolean piercing = false;
    private int pierceCount = 0;
    
    public Bullet(double startX, double startY, double targetX, double targetY) {
        this.x = startX;
        this.y = startY;
        
        // Calculate direction
        double dx = targetX - startX;
        double dy = targetY - startY;
        double distance = Math.sqrt(dx * dx + dy * dy);
        
        if (distance > 0) {
            this.dirX = dx / distance;
            this.dirY = dy / distance;
        } else {
            // Default direction if target is at same position
            this.dirX = 1;
            this.dirY = 0;
        }
    }
    
    public Bullet(double startX, double startY, double targetX, double targetY, boolean piercing, int pierceCount) {
        this(startX, startY, targetX, targetY);
        this.piercing = piercing;
        this.pierceCount = pierceCount;
    }
    
    /**
     * Update bullet position
     */
    public void update(double deltaTime) {
        if (!active) return;
        
        x += dirX * speed * deltaTime;
        y += dirY * speed * deltaTime;
    }
    
    /**
     * Check if bullet is out of bounds
     */
    public boolean isOutOfBounds() {
        return x < -50 || x > Constants.CANVAS_WIDTH + 50 || 
               y < -50 || y > Constants.CANVAS_HEIGHT + 50;
    }
    
    /**
     * Check if bullet hits a wall tile
     */
    public boolean hitsWall(boolean[][] walkableGrid, double cameraX, double cameraY) {
        if (!active) return false;
        
        // Convert bullet position to tile coordinates
        int tileX = (int) ((x + cameraX) / Constants.TILE_SIZE);
        int tileY = (int) ((y + cameraY) / Constants.TILE_SIZE);
        
        // Check bounds
        if (tileY >= 0 && tileY < walkableGrid.length && 
            tileX >= 0 && tileX < walkableGrid[0].length) {
            return !walkableGrid[tileY][tileX]; // true if it's a wall
        }
        
        return false;
    }
    
    /**
     * Check if bullet intersects with a zombie
     */
    public boolean intersects(Zombie zombie) {
        if (!active) return false;
        
        Rectangle bulletBounds = getBounds();
        Rectangle zombieBounds = zombie.getBounds();
        
        return bulletBounds.getBoundsInParent().intersects(zombieBounds.getBoundsInParent());
    }
    
    /**
     * Render the bullet
     */
    public void render(GraphicsContext gc, double cameraX, double cameraY) {
        if (!active) return;
        
        double renderX = x - cameraX;
        double renderY = y - cameraY;
        
        // Calculate rotation angle based on bullet direction
        double rotation = Math.toDegrees(Math.atan2(dirY, dirX)) + 90; // +90 to align with image orientation
        
        // Save current graphics context state
        gc.save();
        
        // Move to bullet center, rotate, then translate back for centered rotation
        gc.translate(renderX, renderY);
        gc.rotate(rotation);
        gc.translate(-renderX, -renderY);
        
        // Draw bullet image scaled to match collision dimensions
        double imageSize = radius * 2; // Scale to match existing collision size
        gc.drawImage(BULLET_SPRITE, renderX - imageSize/2, renderY - imageSize/2, imageSize, imageSize);
        
        // Restore graphics context state
        gc.restore();
    }
    
    /**
     * Deactivate bullet
     */
    public void deactivate() {
        active = false;
    }
    
    /**
     * Get bullet bounds
     */
    public Rectangle getBounds() {
        return new Rectangle(x - radius, y - radius, radius * 2, radius * 2);
    }
    
    // Getters
    public double getX() { return x; }
    public double getY() { return y; }
    public double getDirX() { return dirX; }
    public double getDirY() { return dirY; }
    public boolean isActive() { return active; }
    
    // Piercing methods
    public boolean isPiercing() { return piercing; }
    public int getPierceCount() { return pierceCount; }
    public void reducePierceCount() { pierceCount--; }
    public boolean isPierceExhausted() { return piercing && pierceCount <= 0; }
}
