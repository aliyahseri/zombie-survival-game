package com.zombie.game;

import com.zombie.util.AssetLoader;
import com.zombie.util.Constants;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 * Item drop from killed zombies (weapon boost or armor)
 */
public class ItemDrop {
    
    private double x, y;
    private double width = 25;
    private double height = 25;
    private boolean collected;
    private int type; // 0 = weapon boost, 1 = armor
    private Image itemImage;
    private double spawnTime;
    
    public ItemDrop(double x, double y, int type) {
        this.x = x;
        this.y = y;
        this.type = type;
        this.collected = false;
        this.spawnTime = System.currentTimeMillis() / 1000.0;
        
        // Load appropriate image
        String imagePath = type == 0 ? Constants.WEAPON_BOOST_IMAGE : Constants.ARMOR_IMAGE;
        this.itemImage = AssetLoader.loadImage(Constants.ASSETS_BASE + imagePath);
    }
    
    /**
     * Check if player collected this item
     */
    public boolean checkCollection(Player player, double currentTime) {
        if (collected) return false;
        
        // Remove item after 30 seconds if not collected
        if (currentTime - spawnTime > 30) {
            collected = true;
            return false;
        }
        
        Rectangle itemBounds = getBounds();
        Rectangle playerBounds = player.getBounds();
        
        if (itemBounds.getBoundsInParent().intersects(playerBounds.getBoundsInParent())) {
            collected = true;
            
            // Apply buff to player
            if (type == 0) {
                player.activateWeaponBoost(currentTime);
            } else {
                player.activateArmor(currentTime);
            }
            
            return true;
        }
        
        return false;
    }
    
    /**
     * Render the item drop
     */
    public void render(GraphicsContext gc, double cameraX, double cameraY, double currentTime) {
        if (collected) return;
        
        double renderX = x - cameraX;
        double renderY = y - cameraY;
        
        // Pulsing effect
        double pulse = Math.sin(currentTime * 5) * 0.2 + 0.8;
        double scaledWidth = width * pulse;
        double scaledHeight = height * pulse;
        double offsetX = (width - scaledWidth) / 2;
        double offsetY = (height - scaledHeight) / 2;
        
        if (itemImage != null) {
            gc.drawImage(itemImage, renderX + offsetX, renderY + offsetY, scaledWidth, scaledHeight);
        } else {
            // Fallback to colored rectangle with icon
            Color color = type == 0 ? Color.YELLOW : Color.LIGHTBLUE;
            gc.setFill(color);
            gc.fillRect(renderX + offsetX, renderY + offsetY, scaledWidth, scaledHeight);
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(2);
            gc.strokeRect(renderX + offsetX, renderY + offsetY, scaledWidth, scaledHeight);
            
            // Draw icon
            gc.setFill(Color.BLACK);
            gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 12));
            String text = type == 0 ? "W" : "A";
            gc.fillText(text, renderX + width/2 - 4, renderY + height/2 + 4);
        }
        
        // Add glow effect
        Color glowColor = type == 0 ? Color.YELLOW : Color.LIGHTBLUE;
        gc.setFill(Color.rgb((int)(glowColor.getRed() * 255), 
                            (int)(glowColor.getGreen() * 255), 
                            (int)(glowColor.getBlue() * 255), 0.3));
        gc.fillOval(renderX - 8, renderY - 8, width + 16, height + 16);
        
        // Show remaining time
        double remaining = 30 - (currentTime - spawnTime);
        if (remaining < 10) {
            gc.setFill(Color.WHITE);
            gc.setFont(javafx.scene.text.Font.font("Arial", 10));
            gc.fillText(String.format("%.1f", remaining), renderX, renderY - 5);
        }
    }
    
    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }
    
    public boolean isCollected() {
        return collected;
    }
    
    public int getType() {
        return type;
    }
}
