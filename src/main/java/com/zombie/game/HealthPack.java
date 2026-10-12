package com.zombie.game;

import com.zombie.util.AssetLoader;
import com.zombie.util.Constants;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;

/**
 * Health pack item that restores player health
 */
public class HealthPack {
    
    private double x, y;
    private double width = 20;
    private double height = 20;
    private boolean active = true;
    private double respawnTimer = 0;
    private static final double RESPAWN_TIME = 15.0;
    private Image healthImage;
    
    public HealthPack(double x, double y) {
        this.x = x;
        this.y = y;
        this.healthImage = AssetLoader.loadImage(Constants.ASSETS_BASE + Constants.HEALTH_PACK_IMAGE);
    }
    
    /**
     * Check if player collected this health pack
     */
    public boolean checkCollection(Player player) {
        if (!active) return false;
        
        Rectangle packBounds = getBounds();
        Rectangle playerBounds = player.getBounds();
        
        if (packBounds.getBoundsInParent().intersects(playerBounds.getBoundsInParent())) {
            player.heal(Constants.HEALTH_PACK_HEAL);
            active = false;
            respawnTimer = RESPAWN_TIME;
            return false; // don't remove from list
        }
        
        return false;
    }
    
    public void update(double deltaTime) {
        if (!active) {
            respawnTimer -= deltaTime;
            if (respawnTimer <= 0) {
                active = true;
            }
        }
    }
    
    /**
     * Render the health pack
     */
    public void render(GraphicsContext gc, double cameraX, double cameraY) {
        double renderX = x - cameraX;
        double renderY = y - cameraY;
        
        if (active) {
            if (healthImage != null) {
                gc.drawImage(healthImage, renderX, renderY, width, height);
            } else {
                // Fallback to green cross
                gc.setFill(Color.LIGHTGREEN);
                gc.fillRect(renderX + 5, renderY, width - 10, height);
                gc.fillRect(renderX, renderY + 5, width, height - 10);
                
                gc.setStroke(Color.DARKGREEN);
                gc.setLineWidth(2);
                gc.strokeRect(renderX + 5, renderY, width - 10, height);
                gc.strokeRect(renderX, renderY + 5, width, height - 10);
            }
            
            // Add glow effect
            gc.setFill(Color.rgb(0, 255, 0, 0.3));
            gc.fillOval(renderX - 5, renderY - 5, width + 10, height + 10);
        } else {
            // draw gray cross with respawn timer text
            gc.setFill(Color.GRAY);
            gc.fillRect(renderX + 8, renderY, 4, 20);
            gc.fillRect(renderX, renderY + 8, 20, 4);
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("Arial", 8));
            gc.fillText(String.format("%.0f", respawnTimer), renderX, renderY - 2);
        }
    }
    
    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }
    
    public boolean isCollected() {
        return !active;
    }
}
