# Pathfinding Visualizer - Dijkstra & BFS

A JavaFX desktop app that animates the quickest path from start to finish using **Dijkstra's Algorithm** and **Breadth-First Search** on an interactive grid.

---

## Features

| Feature | Detail |
|---|---|
| **Algorithms** | Dijkstra's Algorithm, Breadth-First Search |
| **Interactive grid** | 35 × 45 cells (adjustable in code) - click to place start/end, drag to draw walls |
| **Animated search** | Frontier (blue) --> Visited (light blue) --> Shortest path (gold) |
| **Stats** | Reports the path length and nodes visited after each run |
| **Clear Path** | Wipe the last path without removing walls |
| **Reset Grid** | Start a completely fresh grid |

---

## Requirements

| Tool | Version |
|---|---|
| JDK | 21 (or 17+) |
| Maven | 3.8+ |

---

## Opening in IDE

1. Clone the repository on **Github** then extract .zip file
2. Import the remaining folder into IDE of choice with Java and Maven support (preferably IntelliJ)
3. **File --> Open** - IntelliJ automatically detects Maven project and downloads JavaFX 21.
4. Wait for IDE to finish indexing project, then run **PathFinderApp.java** through IDE or Maven plugin

---

## How to Use

1. **Click** any cell --> places the **Start** node (green).
2. **Click** another cell --> places the **End** node (red).
3. **Click/drag** --> draws **walls** (black cells).
4. Pick an algorithm from the dropdown menu.
5. Press **Visualize** to watch the search.
6. **Clear Path** re-runs the visualizer with the same maze; **Reset Grid** clears everything.

---

## Algorithm Notes

### Dijkstra's Algorithm
Uses a min-heap priority queue ordered by distance. On an unweighted grid every edge costs 1, so it expands nodes in exact order of distance from the source - **always finds the shortest path**.

### Breadth-First Search
Uses a FIFO queue and explores neighbours level by level. On an unweighted grid it is equivalent to Dijkstra but with simpler bookkeeping - also **always finds the shortest path**.

---

## Project Structure

```
pathfinding-visualizer/
├── pom.xml                              ← Maven config + JavaFX plugin
└── src/main/java/
    ├── module-info.java                 ← Java module descriptor
    └── com/pathfinder/
        └── PathfinderApp.java           ← Entire application (single file)
```
