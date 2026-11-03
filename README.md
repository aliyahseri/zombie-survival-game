# Zombie Survival Game

## Introduction
This project is a top-down 2D zombie survival shooter built with Java and JavaFX. Zombies hunt the player using A* pathfinding, and waves get harder with each difficulty level. Players sign up, choose one of 4 characters, and compete on a leaderboard saved in a local SQLite database.

## Features
* A* pathfinding for zombie movement
* Easy, medium and hard difficulty with zombie waves
* 4 playable characters, 3 with special abilities (shield, triple shot, speed boost)
* Login, sign-up and leaderboard (SQLite)
* Animated sprites, health packs and item drops

## How to Run
* `mvn clean javafx:run`
* The database is created automatically on first run

## Controls
* W A S D / arrow keys: move
* Mouse: aim
* Space: shoot
* E: use ability
* Esc: pause

## Requirements
* Java 17
* Maven
* JavaFX 17
* SQLite JDBC

## Credits
Character sprites by [Rgsdev](https://rgsdev.itch.io) (CC0).
