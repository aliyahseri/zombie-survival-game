package com.zombie.ai;

import java.util.*;

/**
 * A* pathfinding algorithm implementation
 */
public class AStarPathfinder {
    
    public static class Node {
        public int x, y;
        public double gCost, hCost, fCost;
        public Node parent;
        public boolean walkable;
        
        public Node(int x, int y, boolean walkable) {
            this.x = x;
            this.y = y;
            this.walkable = walkable;
        }
        
        public void calculateFCost() {
            fCost = gCost + hCost;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Node node = (Node) obj;
            return x == node.x && y == node.y;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }
    }
    
    private final Node[][] grid;
    private final int gridWidth, gridHeight;
    
    public AStarPathfinder(boolean[][] walkableGrid) {
        this.gridHeight = walkableGrid.length;
        this.gridWidth = walkableGrid[0].length;
        this.grid = new Node[gridHeight][gridWidth];
        
        // Initialize grid
        for (int y = 0; y < gridHeight; y++) {
            for (int x = 0; x < gridWidth; x++) {
                grid[y][x] = new Node(x, y, walkableGrid[y][x]);
            }
        }
    }
    
    /**
     * Find path from start to end using A* algorithm
     */
    public List<Node> findPath(int startX, int startY, int endX, int endY) {
        if (!isValidCoordinate(startX, startY) || !isValidCoordinate(endX, endY)) {
            return Collections.emptyList();
        }

        // Reset all nodes before each search to clear stale parent pointers
        for (int y = 0; y < gridHeight; y++) {
            for (int x = 0; x < gridWidth; x++) {
                grid[y][x].gCost = Double.MAX_VALUE;
                grid[y][x].hCost = 0;
                grid[y][x].fCost = Double.MAX_VALUE;
                grid[y][x].parent = null;
            }
        }

        Node startNode = grid[startY][startX];
        Node endNode = grid[endY][endX];

        if (!startNode.walkable || !endNode.walkable) {
            return Collections.emptyList();
        }

        // Cap path length to prevent infinite loops
        int maxIterations = gridWidth * gridHeight;
        int iterations = 0;

        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.fCost));
        Set<Node> closedSet = new HashSet<>();

        startNode.gCost = 0;
        startNode.hCost = calculateDistance(startNode, endNode);
        startNode.calculateFCost();
        openSet.add(startNode);

        while (!openSet.isEmpty() && iterations < maxIterations) {
            iterations++;
            Node currentNode = openSet.poll();

            if (currentNode.equals(endNode)) {
                return reconstructPath(currentNode);
            }

            closedSet.add(currentNode);

            for (Node neighbor : getNeighbors(currentNode)) {
                if (closedSet.contains(neighbor) || !neighbor.walkable) {
                    continue;
                }

                double tentativeGCost = currentNode.gCost + calculateDistance(currentNode, neighbor);

                if (tentativeGCost < neighbor.gCost) {
                    neighbor.parent = currentNode;
                    neighbor.gCost = tentativeGCost;
                    neighbor.hCost = calculateDistance(neighbor, endNode);
                    neighbor.calculateFCost();

                    if (!openSet.contains(neighbor)) {
                        openSet.add(neighbor);
                    }
                }
            }
        }

        return Collections.emptyList();
    }
    
    /**
     * Reconstruct path from end node to start node
     */
    private List<Node> reconstructPath(Node endNode) {
        List<Node> path = new ArrayList<>();
        Node currentNode = endNode;
        
        while (currentNode != null) {
            path.add(currentNode);
            currentNode = currentNode.parent;
        }
        
        Collections.reverse(path);
        return path;
    }
    
    /**
     * Get valid neighbors for a node
     */
    private List<Node> getNeighbors(Node node) {
        List<Node> neighbors = new ArrayList<>();
        
        // Check all 4 directions (up, down, left, right)
        int[][] directions = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};
        
        for (int[] dir : directions) {
            int newX = node.x + dir[0];
            int newY = node.y + dir[1];
            
            if (isValidCoordinate(newX, newY)) {
                neighbors.add(grid[newY][newX]);
            }
        }
        
        return neighbors;
    }
    
    /**
     * Calculate Manhattan distance between two nodes
     */
    private double calculateDistance(Node nodeA, Node nodeB) {
        return Math.abs(nodeA.x - nodeB.x) + Math.abs(nodeA.y - nodeB.y);
    }
    
    /**
     * Check if coordinates are valid within grid bounds
     */
    private boolean isValidCoordinate(int x, int y) {
        return x >= 0 && x < gridWidth && y >= 0 && y < gridHeight;
    }
    
    /**
     * Update walkable status of a grid cell
     */
    public void updateWalkable(int x, int y, boolean walkable) {
        if (isValidCoordinate(x, y)) {
            grid[y][x].walkable = walkable;
        }
    }
}
