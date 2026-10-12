package com.zombie.model;

/**
 * Character model class
 */
public class Character {
    private int id;
    private String name;
    private String imagePath;
    private String ability;

    public Character() {}

    public Character(String name, String imagePath) {
        this.name = name;
        this.imagePath = imagePath;
    }

    public Character(String name, String imagePath, String ability) {
        this.name = name;
        this.imagePath = imagePath;
        this.ability = ability;
    }

    // Getters and setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getAbility() {
        return ability;
    }

    public void setAbility(String ability) {
        this.ability = ability;
    }

    @Override
    public String toString() {
        return "Character{id=" + id + ", name='" + name + "', imagePath='" + imagePath + "', ability='" + ability + "'}";
    }
}
