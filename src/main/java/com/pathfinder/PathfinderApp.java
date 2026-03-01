package com.pathfinder;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.*;

/**
 * Author: Asfi Chowdhury
 *
 * Pathfinding Visualizer - Dijkstra's Algorithm & BFS
 * Description:
 *      Pathfinding Visualizer uses one of two algorithms;
 *      Dijkstra's or BFS, to determine the shortest path
 *      on an adjustable grid from start to finish.
 *
 * Instructions:
 *   1. Click a cell to place the START node (green).
 *   2. Click another cell to place the END node (red).
 *   3. Click / click & drag to add WALLS.
 *   4. Choose an algorithm and press Start.
 *   5. Use "Clear Path" to re-run without removing placed walls.
 *   6. Use "Reset Grid" to start from scratch.
 */
public class PathfinderApp extends Application {

    // Grid dimensions ────────────────────────────────────────────────────────
    private static final int ROWS      = 35;
    private static final int COLS      = 45;
    private static final int CELL_SIZE = 23;
    private static final int GAP       = 1;

    // Colours ────────────────────────────────────────────────────────────────
    private static final Color C_EMPTY    = Color.rgb(9, 9, 9);
    private static final Color C_GRID     = Color.rgb(125, 125, 125);
    private static final Color C_WALL     = Color.rgb(200,  200,  200);
    private static final Color C_START    = Color.rgb(0,  255,  109);
    private static final Color C_END      = Color.rgb(255,  24,  0);
    private static final Color C_FRONTIER = Color.rgb(194,  52, 219);
    private static final Color C_VISITED  = Color.rgb(90, 180, 255);
    private static final Color C_PATH     = Color.rgb(255, 214,  0);

    // State ─────────────────────────────────────────────────────────────────
    private final Node[][]      grid  = new Node[ROWS][COLS];
    private final Rectangle[][] rects = new Rectangle[ROWS][COLS];

    private Node    startNode   = null;
    private Node    endNode     = null;
    private boolean placingStart= true;
    private boolean placingEnd  = false;
    private boolean placingWalls= false;
    private boolean running     = false;

    private Label         statusLabel;
    private ComboBox<String> algoBox;

    // ══════════════════════════════════════════════════════════════════════════
    @Override
    public void start(Stage stage) {

        // Grid pane ────────────────────────────────────────────────────────
        GridPane gridPane = new GridPane();
        gridPane.setHgap(GAP);
        gridPane.setVgap(GAP);
        gridPane.setPadding(new Insets(GAP));
        gridPane.setStyle("-fx-background-color: " + toHex(C_GRID) + ";");

        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                grid[r][c] = new Node(r, c);

                Rectangle rect = new Rectangle(CELL_SIZE, CELL_SIZE);
                rect.setFill(C_EMPTY);
                rect.setArcWidth(3);
                rect.setArcHeight(3);

                final int row = r, col = c;
                rect.setOnMouseClicked(e -> onCellClick(row, col));
                // Support click-drag wall drawing
                rect.setOnMouseDragEntered(e -> { if (!running && placingWalls) toggleWall(row, col, true); });
                rect.setOnMouseEntered(e -> {
                    if (!running && placingWalls && e.isPrimaryButtonDown()) toggleWall(row, col, true);
                });

                rects[r][c] = rect;
                gridPane.add(rect, c, r);
            }
        }

        // Allow drag detection so setOnMouseDragEntered fires
        gridPane.setOnDragDetected(e -> gridPane.startFullDrag());

        // Controls ─────────────────────────────────────────────────────────
        algoBox = new ComboBox<>();
        algoBox.getItems().addAll("Dijkstra's Algorithm", "Breadth-First Search (BFS)");
        algoBox.setValue("Dijkstra's Algorithm");
        algoBox.setStyle("-fx-font-size: 13px; -fx-pref-width: 230px;");

        Button startBtn    = styledButton("Visualize",  "#27ae60");
        Button clearPathBtn= styledButton("Clear Path", "#2980b9");
        Button resetBtn    = styledButton("Reset Grid", "#c0392b");

        startBtn.setOnAction(e -> runAlgorithm());
        clearPathBtn.setOnAction(e -> clearPath());
        resetBtn.setOnAction(e -> resetGrid());

        HBox controls = new HBox(10, algoBox, startBtn, clearPathBtn, resetBtn);
        controls.setAlignment(Pos.CENTER_LEFT);
        controls.setPadding(new Insets(10, 12, 8, 12));
        controls.setStyle("-fx-background-color: #323232;");
        // Legend ───────────────────────────────────────────────────────────
        HBox legend = new HBox(18,
                legendItem(C_START,    "Start"),
                legendItem(C_END,      "End"),
                legendItem(C_WALL,     "Wall"),
                legendItem(C_FRONTIER, "Frontier"),
                legendItem(C_VISITED,  "Visited"),
                legendItem(C_PATH,     "Quickest Path")
        );
        legend.setAlignment(Pos.CENTER_LEFT);
        legend.setPadding(new Insets(6, 12, 6, 12));
        legend.setStyle("-fx-background-color: #191919;");

        // Status bar ───────────────────────────────────────────────────────
        statusLabel = new Label("Step 1 - Click any cell to place the START node");
        statusLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #ecf0f1;");
        HBox statusBar = new HBox(statusLabel);
        statusBar.setPadding(new Insets(6, 12, 6, 12));
        statusBar.setStyle("-fx-background-color: #0F0F0F;");

        // Root ─────────────────────────────────────────────────────────────
        VBox root = new VBox(0, controls, legend, gridPane, statusBar);

        Scene scene = new Scene(root);
        stage.setTitle("Pathfinding Visualizer - Dijkstra & BFS");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Interaction
    // ══════════════════════════════════════════════════════════════════════════

    private void onCellClick(int row, int col) {
        if (running) return;
        Node node = grid[row][col];

        if (placingStart) {
            if (node.isWall) return;
            if (startNode != null) { startNode.isStart = false; paint(startNode, C_EMPTY); }
            node.isStart = true;
            startNode = node;
            paint(node, C_START);
            placingStart = false;
            placingEnd   = true;
            status("Step 2 - Click a cell to place the END node");

        } else if (placingEnd) {
            if (node.isWall || node == startNode) return;
            if (endNode != null) { endNode.isEnd = false; paint(endNode, C_EMPTY); }
            node.isEnd = true;
            endNode = node;
            paint(node, C_END);
            placingEnd   = false;
            placingWalls = true;
            status("Step 3 - Click/drag to add walls, then press Visualize");

        } else if (placingWalls) {
            toggleWall(row, col, false); // toggle on click
        }
    }

    /** @param forceOn true when called from cursor drag (only adds walls, never removes) */
    private void toggleWall(int row, int col, boolean forceOn) {
        Node node = grid[row][col];
        if (node.isStart || node.isEnd) return;
        node.isWall = forceOn || !node.isWall;
        paint(node, node.isWall ? C_WALL : C_EMPTY);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Algorithms
    // ══════════════════════════════════════════════════════════════════════════

    private void runAlgorithm() {
        if (running) return;
        if (startNode == null || endNode == null) {
            status("Place both a START and an END node first."); return;
        }
        clearPath();
        running = true;

        List<Node> visitOrder = new ArrayList<>();
        boolean isDijkstra = algoBox.getValue().startsWith("Dijkstra");

        if (isDijkstra) dijkstra(visitOrder);
        else            bfs(visitOrder);

        status("Running " + algoBox.getValue() + "…");
        animateSearch(visitOrder, isDijkstra ? "Dijkstra's Algorithm" : "BFS");
    }

    // Dijkstra (unit-weight edges --> same result as BFS, but shows priority queue logic) ─────
    private void dijkstra(List<Node> visitOrder) {
        for (Node[] row : grid) for (Node n : row) { n.dist = Integer.MAX_VALUE; n.prev = null; n.visited = false; }
        startNode.dist = 0;

        PriorityQueue<Node> pq = new PriorityQueue<>(Comparator.comparingInt(n -> n.dist));
        pq.add(startNode);

        while (!pq.isEmpty()) {
            Node cur = pq.poll();
            if (cur.visited) continue;
            cur.visited = true;
            visitOrder.add(cur);
            if (cur == endNode) break;

            for (Node nb : neighbors(cur)) {
                if (nb.visited || nb.isWall) continue;
                int newDist = cur.dist + 1;
                if (newDist < nb.dist) {
                    nb.dist = newDist;
                    nb.prev = cur;
                    pq.add(nb);
                }
            }
        }
    }

    // BFS ──────────────────────────────────────────────────────────────────
    private void bfs(List<Node> visitOrder) {
        for (Node[] row : grid) for (Node n : row) { n.prev = null; n.visited = false; }

        Queue<Node> queue = new ArrayDeque<>();
        startNode.visited = true;
        queue.add(startNode);

        while (!queue.isEmpty()) {
            Node cur = queue.poll();
            visitOrder.add(cur);
            if (cur == endNode) break;

            for (Node nb : neighbors(cur)) {
                if (nb.visited || nb.isWall) continue;
                nb.visited = true;
                nb.prev    = cur;
                queue.add(nb);
            }
        }
    }

    private List<Node> neighbors(Node n) {
        List<Node> list = new ArrayList<>(4);
        int r = n.row, c = n.col;
        if (r > 0)       list.add(grid[r - 1][c]);
        if (r < ROWS-1)  list.add(grid[r + 1][c]);
        if (c > 0)       list.add(grid[r][c - 1]);
        if (c < COLS-1)  list.add(grid[r][c + 1]);
        return list;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Animation
    // ══════════════════════════════════════════════════════════════════════════

    private void animateSearch(List<Node> visitOrder, String algoName) {
        final int VISIT_DELAY_MS = 10; // ms per visited node
        Timeline tl = new Timeline();

        for (int i = 0; i < visitOrder.size(); i++) {
            Node node = visitOrder.get(i);
            double t = i * VISIT_DELAY_MS;

            // Flash frontier colour, then switch to visited
            tl.getKeyFrames().add(new KeyFrame(Duration.millis(t), e -> {
                if (!node.isStart && !node.isEnd) paint(node, C_FRONTIER);
            }));
            tl.getKeyFrames().add(new KeyFrame(Duration.millis(t + VISIT_DELAY_MS * 0.8), e -> {
                if (!node.isStart && !node.isEnd) paint(node, C_VISITED);
            }));
        }

        double pathStart = visitOrder.size() * VISIT_DELAY_MS + 150;
        tl.getKeyFrames().add(new KeyFrame(Duration.millis(pathStart), e -> tracePath(algoName)));

        tl.play();
    }

    private void tracePath(String algoName) {
        List<Node> path = new ArrayList<>();
        Node cur = endNode;
        if (cur.prev == null && cur != startNode) {
            status("Couldn't find a path. Try removing some barriers.");
            running = false;
            return;
        }
        while (cur != null) { path.add(0, cur); cur = cur.prev; }

        Timeline tl = new Timeline();
        for (int i = 0; i < path.size(); i++) {
            Node node = path.get(i);
            tl.getKeyFrames().add(new KeyFrame(Duration.millis(i * 35L), e -> {
                if (!node.isStart && !node.isEnd) paint(node, C_PATH);
            }));
        }
        tl.setOnFinished(e -> {
            int visited = 0;
            for (Node[] row : grid) for (Node n : row) if (n.visited) visited++;
            status(String.format("%s  Completed!   Path: %d steps   Nodes visited: %d",
                    algoName, path.size() - 1, visited));
            running = false;
        });
        tl.play();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Grid helpers
    // ══════════════════════════════════════════════════════════════════════════

    private void clearPath() {
        if (running) return;
        for (Node[] row : grid) {
            for (Node n : row) {
                n.visited = false; n.dist = Integer.MAX_VALUE; n.prev = null;
                if (!n.isStart && !n.isEnd && !n.isWall) paint(n, C_EMPTY);
            }
        }
        if (startNode != null) paint(startNode, C_START);
        if (endNode   != null) paint(endNode,   C_END);
        status("Path cleared - press Visualize to run again.");
    }

    private void resetGrid() {
        if (running) return;
        startNode = null; endNode = null;
        placingStart = true; placingEnd = false; placingWalls = false;
        for (int r = 0; r < ROWS; r++)
            for (int c = 0; c < COLS; c++) {
                grid[r][c] = new Node(r, c);
                rects[r][c].setFill(C_EMPTY);
            }
        status("Step 1 - Click any cell to place the START node");
    }

    private void paint(Node n, Color c)  { rects[n.row][n.col].setFill(c); }
    private void status(String msg)      { statusLabel.setText(msg); }

    // ══════════════════════════════════════════════════════════════════════════
    //  UI helpers
    // ══════════════════════════════════════════════════════════════════════════

    private static Button styledButton(String text, String hex) {
        Button b = new Button(text);
        b.setStyle(String.format(
            "-fx-background-color: %s; -fx-text-fill: white; " +
            "-fx-font-weight: bold; -fx-font-size: 13px; " +
            "-fx-padding: 8 18; -fx-background-radius: 5;", hex));
        b.setOnMouseEntered(e -> b.setOpacity(0.85));
        b.setOnMouseExited(e  -> b.setOpacity(1.0));
        return b;
    }

    private static HBox legendItem(Color color, String label) {
        Rectangle r = new Rectangle(14, 14, color);
        r.setArcWidth(3); r.setArcHeight(3);
        r.setStroke(Color.rgb(255, 255, 255, 0.25));
        Label l = new Label(label);
        l.setStyle("-fx-font-size: 12px; -fx-text-fill: #ecf0f1;");
        HBox box = new HBox(6, r, l);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    /** Converts a JavaFX Color to a CSS hex string. */
    private static String toHex(Color c) {
        return String.format("#%02X%02X%02X",
            (int)(c.getRed()*255), (int)(c.getGreen()*255), (int)(c.getBlue()*255));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Node (grid cell)
    // ══════════════════════════════════════════════════════════════════════════

    static class Node {
        final int row, col;
        boolean isStart, isEnd, isWall, visited;
        int  dist = Integer.MAX_VALUE;
        Node prev = null;

        Node(int row, int col) { this.row = row; this.col = col; }
    }

    public static void main(String[] args) { launch(args); }
}
