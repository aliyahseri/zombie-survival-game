package com.zombie.util;

/**
 * Game constants
 */
public class Constants {
    
    // Game settings
    public static final int CANVAS_WIDTH = 800;
    public static final int CANVAS_HEIGHT = 600;
    public static final int TILE_SIZE = 40;
    public static final double PLAYER_SPEED = 3.0;
    public static final double ZOMBIE_SPEED = 2.0;
    public static final int PLAYER_DAMAGE = 25;
    
    // Player settings
    public static final int MAX_HEALTH = 100;
    public static final int INITIAL_LIVES = 3;
    public static final int HEALTH_PACK_HEAL = 25;
    
    // Zombie settings
    public static final int PATH_RECALCULATION_INTERVAL = 30; // frames
    public static final double DAMAGE_INTERVAL = 0.5; // seconds
    
    // Wave settings
    public static final int TOTAL_WAVES = 5;
    public static final int WAVE_DELAY_SECONDS = 5;
    public static final double DROP_CHANCE = 0.15; // 15%
    
    // Difficulty damage values
    public static final int[] ZOMBIE_DAMAGE = {5, 8, 12}; // Easy, Medium, Hard
    
    // Wave zombie counts
    public static final int[] EASY_WAVE_COUNTS = {10};
    public static final int[] MEDIUM_WAVE_COUNTS = {20, 40, 60, 80, 100};
    public static final int[] HARD_WAVE_COUNTS = {40, 80, 120, 160, 200};
    
    // Asset paths
    public static final String ASSETS_BASE = "/assets/";
    public static final String CHARACTERS_PATH = ASSETS_BASE + "characters/";
    public static final String TILES_PATH = ASSETS_BASE + "tiles/";
    public static final String ITEMS_PATH = ASSETS_BASE + "items/";
    
    // Character images
    public static final String[] CHARACTER_IMAGES = {
        "character_1.png",
        "character_2.png", 
        "character_3.png",
        "character_4.png"
    };
    
    // Item images
    public static final String WALL_IMAGE = "wall.png";
    public static final String HEALTH_PACK_IMAGE = "health_pack.png";
    public static final String ZOMBIE_IMAGE = "zombie.png";
    public static final String WEAPON_BOOST_IMAGE = "weapon_boost.png";
    public static final String ARMOR_IMAGE = "armor.png";
    public static final String MENU_BG_IMAGE = "menu_bg.png";
    
    // Buff durations
    public static final double WEAPON_BOOST_DURATION = 10.0; // seconds
    public static final double ARMOR_DURATION = 10.0; // seconds
    
    // Database
    public static final String DB_FILE = "zombie_game.db";
    
    // UI
    public static final String CSS_FILE = "/css/dark-theme.css";
    
    private Constants() {
        // Utility class
    }
}
