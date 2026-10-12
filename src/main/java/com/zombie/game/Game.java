package com.zombie.game;

import com.zombie.ai.AStarPathfinder;
import com.zombie.ui.MainApp;
import com.zombie.util.Constants;
import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Main game engine class
 */
public class Game extends StackPane {
    
    private final MainApp mainApp;
    private final String difficulty;
    private final Canvas canvas;
    private final GraphicsContext gc;
    
    // Game state
    private boolean isRunning;
    private boolean isPaused;
    private boolean isGameOver;
    private boolean isWin;
    private double gameTime;
    private int currentWave;
    private int totalWaves;
    private int zombiesInWave;
    private int zombiesKilled;
    
    // Game entities
    private Player player;
    private List<Zombie> zombies = new ArrayList<>();
    private List<HealthPack> healthPacks = new ArrayList<>();
    private List<ItemDrop> itemDrops = new ArrayList<>();
    private List<Bullet> bullets = new ArrayList<>();
    
    // Map and pathfinding
    private boolean[][] walkableGrid;
    private AStarPathfinder pathfinder;
    private int mapWidth, mapHeight;
    
    // Input handling
    private final Set<String> pressedKeys = new HashSet<>();
    private boolean isSpacePressed = false; // Debounce flag for semi-automatic shooting
    
    // Camera
    private double cameraX, cameraY;
    
    // Mouse tracking
    private double mouseX, mouseY;
    private double lastShotTime = 0;
    
    // Game end tracking
    private boolean endGameCalled = false;
    
    // Wave system
    private double waveStartTime;
    private boolean waveInProgress;
    private double nextWaveCountdown;
    
    public Game(MainApp mainApp, String difficulty) {
        this.mainApp = mainApp;
        this.difficulty = difficulty;
        this.canvas = new Canvas(Constants.CANVAS_WIDTH, Constants.CANVAS_HEIGHT);
        this.gc = canvas.getGraphicsContext2D();
        
        initializeGame();
        setupEventHandlers();
        startGameLoop();
    }
    
    private void initializeGame() {
        // Initialize game state
        isRunning = true;
        isPaused = false;
        isGameOver = false;
        isWin = false;
        gameTime = 0;
        currentWave = 1;
        
        // Set total waves based on difficulty
        int[] waveCounts = switch (difficulty) {
            case "EASY" -> Constants.EASY_WAVE_COUNTS;
            case "MEDIUM" -> Constants.MEDIUM_WAVE_COUNTS;
            case "HARD" -> Constants.HARD_WAVE_COUNTS;
            default -> Constants.EASY_WAVE_COUNTS;
        };
        totalWaves = waveCounts.length;
        
        zombiesKilled = 0;
        
        // Create map
        createMap();
        
        // Initialize pathfinder
        pathfinder = new AStarPathfinder(walkableGrid);
        
        // Create player
        player = new Player(100, 100);
        // Apply character stats and load character sprites based on GameState (single source of truth)
        GameState gameState = GameState.getInstance();
        if (gameState.isInitialized() && gameState.getSelectedCharacter() != null) {
            int characterId = gameState.getSelectedCharacter().getId();
            player.applyCharacterStats(characterId);
            player.loadCharacterSprites(characterId);
        } else if (mainApp.getCurrentUser().getLastSelectedCharacterId() != null) {
            // Fallback to database if GameState not initialized
            int characterId = mainApp.getCurrentUser().getLastSelectedCharacterId();
            player.applyCharacterStats(characterId);
            player.loadCharacterSprites(characterId);
        } else if (mainApp.getCurrentUser().getCharacterId() != null) {
            // Final fallback to legacy character_id
            int characterId = mainApp.getCurrentUser().getCharacterId();
            player.applyCharacterStats(characterId);
            player.loadCharacterSprites(characterId);
        }
        
        // Preload zombie sprites before wave starts to avoid per-zombie loading lag
        Zombie.preloadSprites();
        
        // Entity lists are now initialized at field declaration
        
        // Start first wave
        startWave(currentWave);
        
        // Initialize camera
        updateCamera();
    }
    
    private void createMap() {
        // Create a simple map with walls
        mapWidth = 30; // tiles
        mapHeight = 20; // tiles
        walkableGrid = new boolean[mapHeight][mapWidth];
        
        // Initialize all tiles as walkable
        for (int y = 0; y < mapHeight; y++) {
            for (int x = 0; x < mapWidth; x++) {
                walkableGrid[y][x] = true;
            }
        }
        
        // Add border walls
        for (int x = 0; x < mapWidth; x++) {
            walkableGrid[0][x] = false; // Top wall
            walkableGrid[mapHeight - 1][x] = false; // Bottom wall
        }
        for (int y = 0; y < mapHeight; y++) {
            walkableGrid[y][0] = false; // Left wall
            walkableGrid[y][mapWidth - 1] = false; // Right wall
        }
        
        // Add some internal walls for complexity
        createInternalWalls();
        
        // Place health packs
        placeHealthPacks();
    }
    
    private void createInternalWalls() {
        // Create some wall patterns
        // Horizontal wall
        for (int x = 5; x < 15; x++) {
            walkableGrid[5][x] = false;
        }
        
        // Vertical wall
        for (int y = 7; y < 15; y++) {
            walkableGrid[y][20] = false;
        }
        
        // L-shaped wall
        for (int x = 8; x < 12; x++) {
            walkableGrid[12][x] = false;
        }
        for (int y = 12; y < 16; y++) {
            walkableGrid[y][11] = false;
        }
        
        // Small room
        for (int x = 22; x < 28; x++) {
            for (int y = 3; y < 7; y++) {
                if (x == 22 || x == 27 || y == 3 || y == 6) {
                    walkableGrid[y][x] = false;
                }
            }
        }
        
        // Add some random walls
        for (int i = 0; i < 10; i++) {
            int x = 2 + (int) (Math.random() * (mapWidth - 4));
            int y = 2 + (int) (Math.random() * (mapHeight - 4));
            walkableGrid[y][x] = false;
        }
    }
    
    private void placeHealthPacks() {
        // Place 5-10 health packs randomly on the map
        int packCount = 5 + (int) (Math.random() * 6);
        
        for (int i = 0; i < packCount; i++) {
            int x, y;
            do {
                x = (int) (Math.random() * mapWidth);
                y = (int) (Math.random() * mapHeight);
            } while (!walkableGrid[y][x]);
            
            healthPacks.add(new HealthPack(x * Constants.TILE_SIZE, y * Constants.TILE_SIZE));
        }
    }
    
    private void setupEventHandlers() {
        this.getChildren().add(canvas);
        
        // Key press handlers
        this.setOnKeyPressed(e -> handleKeyPressed(e));
        this.setOnKeyReleased(e -> handleKeyReleased(e));
        this.setFocusTraversable(true);
        
        // Mouse tracking
        canvas.setOnMouseMoved(e -> {
            mouseX = e.getX();
            mouseY = e.getY();
        });
        
        canvas.setOnMouseDragged(e -> {
            mouseX = e.getX();
            mouseY = e.getY();
        });
        
        // Focus request for keyboard input
        this.requestFocus();
    }
    
    private void handleKeyPressed(KeyEvent e) {
        KeyCode code = e.getCode();

        if (code == KeyCode.ESCAPE) {
            togglePause();
            return;
        }

        // Handle game over screen inputs directly here
        if (isGameOver) {
            if (code == KeyCode.H) {
                mainApp.showHomeScreen();
            }
            return;
        }

        // Handle pause screen inputs directly here
        if (isPaused) {
            if (code == KeyCode.Q) {
                mainApp.showHomeScreen();
            }
            return;
        }

        // Normal gameplay movement keys
        switch (code) {
            case W, UP -> pressedKeys.add("W");
            case S, DOWN -> pressedKeys.add("S");
            case A, LEFT -> pressedKeys.add("A");
            case D, RIGHT -> pressedKeys.add("D");
            case SPACE -> {
                pressedKeys.add("SPACE");
                // Handle semi-automatic shooting
                if (!isSpacePressed) {
                    isSpacePressed = true;
                    shoot();
                }
            }
            case E -> {
                pressedKeys.add("E");
                // Handle ability activation with debounce
                player.activateAbility(gameTime);
            }
        }
    }
    
    private void handleKeyReleased(KeyEvent e) {
        KeyCode code = e.getCode();
        
        switch (code) {
            case W, UP -> pressedKeys.remove("W");
            case S, DOWN -> pressedKeys.remove("S");
            case A, LEFT -> pressedKeys.remove("A");
            case D, RIGHT -> pressedKeys.remove("D");
            case SPACE -> {
                pressedKeys.remove("SPACE");
                isSpacePressed = false; // Reset debounce flag
            }
            case E -> {
                pressedKeys.remove("E");
                player.resetEKeyPress(); // Reset E key debounce
            }
        }
    }
    
    private void shoot() {
        double bulletX = player.getX() + player.getWidth() / 2;
        double bulletY = player.getY() + player.getHeight() / 2;
        double targetX = mouseX + cameraX;
        double targetY = mouseY + cameraY;
        
        // Handle Vi's Triple Shot ability - only for Vi character
        if (player.getSpecialAbility().equals("TRIPLE_SHOT") && player.isAbilityActive()) {
            // Calculate base direction
            double dx = targetX - bulletX;
            double dy = targetY - bulletY;
            double distance = Math.sqrt(dx * dx + dy * dy);
            
            if (distance > 0) {
                // Create three bullets with spread: center, +10°, -10°
                double baseDirX = dx / distance;
                double baseDirY = dy / distance;
                
                // Center bullet (at target angle)
                bullets.add(new Bullet(bulletX, bulletY, targetX, targetY, true, 3));
                
                // Left bullet (+10° spread)
                double spreadAngle1 = Math.toRadians(10);
                double spreadDirX1 = baseDirX * Math.cos(spreadAngle1) - baseDirY * Math.sin(spreadAngle1);
                double spreadDirY1 = baseDirX * Math.sin(spreadAngle1) + baseDirY * Math.cos(spreadAngle1);
                bullets.add(new Bullet(bulletX, bulletY, bulletX + spreadDirX1 * 100, targetY + spreadDirY1 * 100, true, 3));
                
                // Right bullet (-10° spread)
                double spreadAngle2 = Math.toRadians(-10);
                double spreadDirX2 = baseDirX * Math.cos(spreadAngle2) - baseDirY * Math.sin(spreadAngle2);
                double spreadDirY2 = baseDirX * Math.sin(spreadAngle2) + baseDirY * Math.cos(spreadAngle2);
                bullets.add(new Bullet(bulletX, bulletY, bulletX + spreadDirX2 * 100, targetY + spreadDirY2 * 100, true, 3));
            }
        } else {
            // Regular single bullet for other characters
            bullets.add(new Bullet(bulletX, bulletY, targetX, targetY));
        }
    }
    
    private void togglePause() {
        isPaused = !isPaused;
    }
    
    private void startWave(int waveNumber) {
        currentWave = waveNumber;
        waveInProgress = true;
        waveStartTime = gameTime;
        zombiesInWave = getZombieCountForWave(waveNumber);
        
        // Spawn zombies gradually
        spawnZombiesForWave(waveNumber);
    }
    
    private int getZombieCountForWave(int waveNumber) {
        int[] counts = switch (difficulty) {
            case "EASY" -> Constants.EASY_WAVE_COUNTS;
            case "MEDIUM" -> Constants.MEDIUM_WAVE_COUNTS;
            case "HARD" -> Constants.HARD_WAVE_COUNTS;
            default -> Constants.EASY_WAVE_COUNTS;
        };
        int index = Math.min(waveNumber - 1, counts.length - 1);
        return counts[index];
    }
    
    private void spawnZombiesForWave(int waveNumber) {
        int zombieCount = getZombieCountForWave(waveNumber);
        
        for (int i = 0; i < zombieCount; i++) {
            // Spawn from random edges
            double x, y;
            int edge = (int) (Math.random() * 4);
            
            switch (edge) {
                case 0: // Top
                    x = Math.random() * (mapWidth * Constants.TILE_SIZE);
                    y = 0;
                    break;
                case 1: // Right
                    x = (mapWidth - 1) * Constants.TILE_SIZE;
                    y = Math.random() * (mapHeight * Constants.TILE_SIZE);
                    break;
                case 2: // Bottom
                    x = Math.random() * (mapWidth * Constants.TILE_SIZE);
                    y = (mapHeight - 1) * Constants.TILE_SIZE;
                    break;
                default: // Left
                    x = 0;
                    y = Math.random() * (mapHeight * Constants.TILE_SIZE);
                    break;
            }
            
            int difficultyIndex = switch (difficulty) {
                case "EASY" -> 0;
                case "MEDIUM" -> 1;
                case "HARD" -> 2;
                default -> 0;
            };
            
            zombies.add(new Zombie(x, y, difficultyIndex));
        }
    }
    
    private void updateCamera() {
        // Center camera on player
        cameraX = player.getX() - Constants.CANVAS_WIDTH / 2;
        cameraY = player.getY() - Constants.CANVAS_HEIGHT / 2;
        
        // Clamp camera to map bounds
        cameraX = Math.max(0, Math.min(cameraX, mapWidth * Constants.TILE_SIZE - Constants.CANVAS_WIDTH));
        cameraY = Math.max(0, Math.min(cameraY, mapHeight * Constants.TILE_SIZE - Constants.CANVAS_HEIGHT));
    }
    
    private void startGameLoop() {
        AnimationTimer gameLoop = new AnimationTimer() {
            private long lastTime = 0;
            
            @Override
            public void handle(long currentTime) {
                if (lastTime == 0) {
                    lastTime = currentTime;
                }
                
                double deltaTime = (currentTime - lastTime) / 1_000_000_000.0; // Convert to seconds
                lastTime = currentTime;
                
                if (!isRunning) return;

                if (!isPaused && !isGameOver) {
                    update(deltaTime);
                    render();
                } else if (isPaused) {
                    render();
                    renderPauseScreen();
                } else if (isGameOver) {
                    render();
                    renderGameOverScreen();
                }
            }
        };
        
        gameLoop.start();
    }
    
    private void update(double deltaTime) {
        gameTime += deltaTime;
        
        // Update player
        player.update(pressedKeys, deltaTime, walkableGrid);
        player.updateAbilities(deltaTime, gameTime);
        
        // Semi-automatic shooting is now handled in key press events
        
        // Update camera
        updateCamera();
        
        // Update zombies
        for (int i = zombies.size() - 1; i >= 0; i--) {
            Zombie zombie = zombies.get(i);
            zombie.update(player, walkableGrid, pathfinder, deltaTime);
            
            // Check zombie-player collision
            if (zombie.canDamagePlayer(player, gameTime)) {
                zombie.damagePlayer(player, gameTime);
                
                if (player.isDead()) {
                    handlePlayerDeath();
                }
            }
            
            // Remove dead zombies
            if (zombie.isDead()) {
                // Check for item drops
                if (zombie.shouldDropItem()) {
                    int dropType = zombie.getDropType();
                    itemDrops.add(new ItemDrop(zombie.getX(), zombie.getY(), dropType));
                }
                
                zombies.remove(i);
                zombiesKilled++;
            }
        }
        
        // Update health packs
        for (HealthPack pack : healthPacks) {
            pack.update(deltaTime);
            pack.checkCollection(player);
        }
        
        // Check for item drop collection
        for (int i = itemDrops.size() - 1; i >= 0; i--) {
            ItemDrop drop = itemDrops.get(i);
            
            if (drop.checkCollection(player, gameTime)) {
                itemDrops.remove(i);
            }
        }
        
        // Update bullets
        for (int i = bullets.size() - 1; i >= 0; i--) {
            Bullet bullet = bullets.get(i);
            bullet.update(deltaTime);
            
            // Remove bullets that are out of bounds
            if (bullet.isOutOfBounds()) {
                bullets.remove(i);
                continue;
            }
            
            // Check wall collision
            if (bullet.hitsWall(walkableGrid, cameraX, cameraY)) {
                bullets.remove(i);
                continue;
            }
            
            // Check zombie collision
            for (int j = zombies.size() - 1; j >= 0; j--) {
                Zombie zombie = zombies.get(j);
                if (!zombie.isDead() && bullet.intersects(zombie)) {
                    // Calculate damage
                    int damage = Constants.PLAYER_DAMAGE;
                    if (player.hasWeaponBoost(gameTime)) {
                        damage *= 2; // Double damage with weapon boost
                    }
                    
                    zombie.takeDamage(damage);
                    
                    // Handle piercing bullets
                    if (bullet.isPiercing()) {
                        bullet.reducePierceCount();
                        if (bullet.isPierceExhausted()) {
                            bullets.remove(i);
                            break; // Remove bullet and exit inner loop
                        }
                        // Continue checking for more zombies to hit
                    } else {
                        bullets.remove(i);
                        break; // Regular bullet - remove and exit
                    }
                }
            }
        }
        
        // Check wave completion
        if (waveInProgress && zombies.isEmpty()) {
            waveInProgress = false;
            
            if (currentWave >= totalWaves) {
                // Game won!
                isWin = true;
                isGameOver = true;
            } else {
                // Start countdown for next wave
                nextWaveCountdown = Constants.WAVE_DELAY_SECONDS;
            }
        }
        
        // Handle wave countdown
        if (!waveInProgress && nextWaveCountdown > 0) {
            nextWaveCountdown -= deltaTime;
            if (nextWaveCountdown <= 0) {
                startWave(currentWave + 1);
            }
        }
        
        // Handle game end
        if (isGameOver && !endGameCalled) {
            endGameCalled = true;
            isRunning = false;
            boolean won = isWin;
            int waves = won ? currentWave : currentWave - 1;
            double finalTime = gameTime;
            int finalKills = zombiesKilled;
            javafx.application.Platform.runLater(() ->
                mainApp.endGame(won, waves, finalKills, finalTime)
            );
        }
    }
    
    private void handlePlayerDeath() {
        player.loseLife();
        
        if (player.getLives() <= 0) {
            // Game over
            isGameOver = true;
            isWin = false;
        } else {
            // Respawn player
            player.respawn(100, 100);
        }
    }
    
    private void render() {
        // Clear canvas
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, Constants.CANVAS_WIDTH, Constants.CANVAS_HEIGHT);
        
        // Render map
        renderMap();
        
        // Render health packs
        for (HealthPack pack : healthPacks) {
            pack.render(gc, cameraX, cameraY);
        }
        
        // Render item drops
        for (ItemDrop drop : itemDrops) {
            drop.render(gc, cameraX, cameraY, gameTime);
        }
        
        // Render bullets
        for (Bullet bullet : bullets) {
            bullet.render(gc, cameraX, cameraY);
        }
        
        // Render zombies
        for (Zombie zombie : zombies) {
            zombie.render(gc, cameraX, cameraY);
        }
        
        // Render player
        player.render(gc, cameraX, cameraY);
        
        // Render HUD
        renderHUD();
    }
    
    private void renderMap() {
        // Calculate visible tile range
        int startX = (int) (cameraX / Constants.TILE_SIZE);
        int startY = (int) (cameraY / Constants.TILE_SIZE);
        int endX = Math.min(startX + (Constants.CANVAS_WIDTH / Constants.TILE_SIZE) + 1, mapWidth);
        int endY = Math.min(startY + (Constants.CANVAS_HEIGHT / Constants.TILE_SIZE) + 1, mapHeight);
        
        for (int y = startY; y < endY; y++) {
            for (int x = startX; x < endX; x++) {
                double renderX = x * Constants.TILE_SIZE - cameraX;
                double renderY = y * Constants.TILE_SIZE - cameraY;
                
                if (!walkableGrid[y][x]) {
                    // Render wall
                    gc.setFill(Color.DARKGRAY);
                    gc.fillRect(renderX, renderY, Constants.TILE_SIZE, Constants.TILE_SIZE);
                    gc.setStroke(Color.BLACK);
                    gc.setLineWidth(1);
                    gc.strokeRect(renderX, renderY, Constants.TILE_SIZE, Constants.TILE_SIZE);
                } else {
                    // Render floor
                    gc.setFill(Color.rgb(40, 40, 40));
                    gc.fillRect(renderX, renderY, Constants.TILE_SIZE, Constants.TILE_SIZE);
                }
            }
        }
    }
    
    private void renderHUD() {
        // Health bar
        renderHealthBar();
        
        // Lives
        renderLives();
        
        // Wave info
        renderWaveInfo();
        
        // Active buffs
        renderBuffs();
        
        // Ability status
        renderAbilityStatus();
        
        // Game time
        renderGameTime();
    }
    
    private void renderHealthBar() {
        double barWidth = 200;
        double barHeight = 20;
        double barX = 20;
        double barY = 20;
        
        // Background
        gc.setFill(Color.DARKGRAY);
        gc.fillRect(barX, barY, barWidth, barHeight);
        
        // Health
        double healthPercentage = (double) player.getHealth() / Constants.MAX_HEALTH;
        Color healthColor = healthPercentage > 0.5 ? Color.GREEN : 
                           healthPercentage > 0.25 ? Color.YELLOW : Color.RED;
        gc.setFill(healthColor);
        gc.fillRect(barX, barY, barWidth * healthPercentage, barHeight);
        
        // Border
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(2);
        gc.strokeRect(barX, barY, barWidth, barHeight);
        
        // Text
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        gc.fillText("HEALTH: " + player.getHealth() + "/" + Constants.MAX_HEALTH, barX + 5, barY + 14);
    }
    
    private void renderLives() {
        double x = 20;
        double y = 50;
        
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        gc.fillText("LIVES: ", x, y);
        
        x += 50;
        for (int i = 0; i < player.getLives(); i++) {
            // Draw heart icon
            gc.setFill(Color.RED);
            gc.fillOval(x + i * 25, y - 12, 15, 15);
            gc.setStroke(Color.DARKRED);
            gc.setLineWidth(1);
            gc.strokeOval(x + i * 25, y - 12, 15, 15);
        }
    }
    
    private void renderWaveInfo() {
        double x = Constants.CANVAS_WIDTH - 200;
        double y = 20;
        
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        
        if (waveInProgress) {
            gc.fillText("WAVE: " + currentWave + "/" + totalWaves, x, y);
            gc.fillText("ZOMBIES: " + zombies.size(), x, y + 20);
        } else if (nextWaveCountdown > 0) {
            gc.fillText("NEXT WAVE IN: " + (int) Math.ceil(nextWaveCountdown), x, y);
        }
    }
    
    private void renderBuffs() {
        double x = 20;
        double y = Constants.CANVAS_HEIGHT - 60;
        
        if (player.hasWeaponBoost(gameTime)) {
            double remaining = player.getWeaponBoostRemaining(gameTime);
            gc.setFill(Color.YELLOW);
            gc.fillRect(x, y, 40, 40);
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(2);
            gc.strokeRect(x, y, 40, 40);
            
            gc.setFill(Color.BLACK);
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 10));
            gc.fillText("WEP", x + 5, y + 15);
            gc.fillText(String.format("%.1f", remaining), x + 5, y + 30);
            
            x += 50;
        }
        
        if (player.hasArmor(gameTime)) {
            double remaining = player.getArmorRemaining(gameTime);
            gc.setFill(Color.LIGHTBLUE);
            gc.fillRect(x, y, 40, 40);
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(2);
            gc.strokeRect(x, y, 40, 40);
            
            gc.setFill(Color.BLACK);
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 10));
            gc.fillText("ARM", x + 5, y + 15);
            gc.fillText(String.format("%.1f", remaining), x + 5, y + 30);
        }
    }
    
    private void renderGameTime() {
        double x = Constants.CANVAS_WIDTH - 100;
        double y = Constants.CANVAS_HEIGHT - 30;
        
        int minutes = (int) (gameTime / 60);
        int seconds = (int) (gameTime % 60);
        
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        gc.fillText(String.format("%02d:%02d", minutes, seconds), x, y);
    }
    
    private void renderPauseScreen() {
        // Darken the game
        gc.setFill(Color.rgb(0, 0, 0, 0.7));
        gc.fillRect(0, 0, Constants.CANVAS_WIDTH, Constants.CANVAS_HEIGHT);
        
        // Pause text
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 48));
        gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
        gc.fillText("PAUSED", Constants.CANVAS_WIDTH / 2, Constants.CANVAS_HEIGHT / 2 - 50);
        
        gc.setFont(Font.font("Arial", 20));
        gc.fillText("Press ESC to Resume", Constants.CANVAS_WIDTH / 2, Constants.CANVAS_HEIGHT / 2 + 20);
        gc.fillText("Press Q to Quit", Constants.CANVAS_WIDTH / 2, Constants.CANVAS_HEIGHT / 2 + 50);
        
        gc.setTextAlign(javafx.scene.text.TextAlignment.LEFT);
    }
    
    private void renderGameOverScreen() {
        // Darken the game
        gc.setFill(Color.rgb(0, 0, 0, 0.8));
        gc.fillRect(0, 0, Constants.CANVAS_WIDTH, Constants.CANVAS_HEIGHT);
        
        // Game over text
        gc.setFill(isWin ? Color.GREEN : Color.RED);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 48));
        gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
        gc.fillText(isWin ? "VICTORY!" : "GAME OVER", Constants.CANVAS_WIDTH / 2, Constants.CANVAS_HEIGHT / 2 - 100);
        
        // Stats
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", 20));
        gc.fillText("Waves Cleared: " + (currentWave - 1), Constants.CANVAS_WIDTH / 2, Constants.CANVAS_HEIGHT / 2 - 30);
        gc.fillText("Zombies Killed: " + zombiesKilled, Constants.CANVAS_WIDTH / 2, Constants.CANVAS_HEIGHT / 2);
        gc.fillText("Time: " + String.format("%02d:%02d", (int) (gameTime / 60), (int) (gameTime % 60)), Constants.CANVAS_WIDTH / 2, Constants.CANVAS_HEIGHT / 2 + 30);
        
        // Instructions
        gc.setFont(Font.font("Arial", 16));
        gc.fillText("Press H to Return to Home", Constants.CANVAS_WIDTH / 2, Constants.CANVAS_HEIGHT / 2 + 80);
        
        gc.setTextAlign(javafx.scene.text.TextAlignment.LEFT);
    }
    
    private void renderAbilityStatus() {
        double x = 20;
        double y = 100;
        double barWidth = 150;
        double barHeight = 15;
        
        // Ability name
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", 12));
        String abilityName = getFriendlyAbilityName(player.getSpecialAbility());
        gc.fillText("Ability: " + abilityName, x, y - 5);
        
        // Cooldown bar
        double cooldownRemaining = player.getAbilityCooldownRemaining(gameTime);
        if (cooldownRemaining > 0) {
            // Show cooldown
            gc.setFill(Color.RED);
            gc.fillRect(x, y, barWidth, barHeight);
            gc.setFill(Color.GREEN);
            gc.fillRect(x, y, barWidth * (1 - cooldownRemaining / 15.0), barHeight);
            
            // Cooldown text
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("Arial", 10));
            gc.fillText(String.format("%.1fs", cooldownRemaining), x + barWidth / 2 - 15, y + barHeight + 12);
        } else {
            // Ready
            gc.setFill(Color.GREEN);
            gc.fillRect(x, y, barWidth, barHeight);
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("Arial", 10));
            gc.fillText("READY (E)", x + barWidth / 2 - 20, y + barHeight + 12);
        }
        
        // Active ability indicators - character-specific
        y += 40;
        if (player.isAbilityActive()) {
            switch (player.getSpecialAbility()) {
                case "TRIPLE_SHOT" -> {
                    gc.setFill(Color.MAGENTA);
                    gc.fillText("PIERCE ACTIVATED", x, y);
                }
                case "SHIELD" -> {
                    gc.setFill(Color.CYAN);
                    gc.fillText("SHIELD ACTIVATED", x, y);
                }
                case "SPEED_BOOST" -> {
                    gc.setFill(Color.GRAY);
                    gc.fillText("SMOKE ACTIVATED", x, y);
                }
            }
        }
    }

    private String getFriendlyAbilityName(String ability) {
        if (ability == null) return "None";
        switch (ability) {
            case "SHIELD" -> { return "Shield"; }
            case "SPEED_BOOST" -> { return "Speed Boost"; }
            case "TRIPLE_SHOT" -> { return "Triple Shot"; }
            case "NONE" -> { return "None"; }
            default -> { return ability; }
        }
    }
}
