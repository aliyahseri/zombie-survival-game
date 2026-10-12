package com.zombie.game;

import com.zombie.model.Character;

/**
 * GameState singleton for managing global game state and session data
 */
public class GameState {
    private static GameState instance;
    private Character selectedCharacter;
    private boolean initialized = false;
    
    private GameState() {}
    
    public static synchronized GameState getInstance() {
        if (instance == null) {
            instance = new GameState();
        }
        return instance;
    }
    
    public Character getSelectedCharacter() {
        return selectedCharacter;
    }
    
    public void setSelectedCharacter(Character selectedCharacter) {
        this.selectedCharacter = selectedCharacter;
        this.initialized = true;
    }
    
    public boolean isInitialized() {
        return initialized;
    }
    
    public void reset() {
        selectedCharacter = null;
        initialized = false;
    }
    
    public int getSelectedCharacterId() {
        return selectedCharacter != null ? selectedCharacter.getId() : -1;
    }
    
    public String getSelectedCharacterName() {
        return selectedCharacter != null ? selectedCharacter.getName() : "No Character Selected";
    }
    
    public String getSelectedCharacterAbility() {
        return selectedCharacter != null ? selectedCharacter.getAbility() : "None";
    }
}
