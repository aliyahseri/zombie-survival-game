package com.zombie.util;

import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for loading game assets with fallback to colored rectangles
 */
public class AssetLoader {
    
    private static final Map<String, Image> imageCache = new HashMap<>();
    
    /**
     * Load an image from the assets folder
     * If the image doesn't exist, returns a colored rectangle as fallback
     */
    public static Image loadImage(String imagePath) {
        if (imageCache.containsKey(imagePath)) {
            return imageCache.get(imagePath);
        }
        
        try {
            Image image = new Image(AssetLoader.class.getResourceAsStream(imagePath));
            if (!image.isError()) {
                imageCache.put(imagePath, image);
                return image;
            }
        } catch (Exception e) {
            // Image not found, use fallback
        }
        
        // Create fallback colored rectangle as image
        return createFallbackImage(imagePath);
    }
    
    /**
     * Create a fallback colored rectangle based on the image type
     */
    private static Image createFallbackImage(String imagePath) {
        // For simplicity, we'll return null and handle fallbacks in rendering
        // The actual rendering code will use colored rectangles when image is null
        return null;
    }
    
    /**
     * Get a color for fallback rendering based on image type
     */
    public static Color getFallbackColor(String imagePath) {
        if (imagePath.contains("character")) {
            return Color.BLUE;
        } else if (imagePath.contains("zombie")) {
            return Color.DARKGREEN;
        } else if (imagePath.contains("wall")) {
            return Color.DARKGRAY;
        } else if (imagePath.contains("health")) {
            return Color.LIGHTGREEN;
        } else if (imagePath.contains("weapon")) {
            return Color.YELLOW;
        } else if (imagePath.contains("armor")) {
            return Color.LIGHTBLUE;
        } else {
            return Color.GRAY;
        }
    }
    
    /**
     * Clear the image cache
     */
    public static void clearCache() {
        imageCache.clear();
    }
}
