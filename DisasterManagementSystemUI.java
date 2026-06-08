import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.util.*;
import java.util.List;
import javax.swing.*;

// 1. Data Structures
class Node {
    String name;
    String type; // Warehouse, Hospital, Shelter, Residential, Intersection
    int x, y;

    public Node(String name, String type, int x, int y) {
        this.name = name;
        this.type = type;
        this.x = x;
        this.y = y;
    }
}

class Edge {
    Node source;
    Node target;
    double weight;
    boolean isBlocked;

    public Edge(Node source, Node target, double weight) {
        this.source = source;
        this.target = target;
        this.weight = weight;
        this.isBlocked = false;
    }
}

// 2. High-Readability Canvas Panel supporting Zoom, Pan, and Interactive Clicks
class MapPanel extends JPanel {
    private final Map<Node, List<Edge>> graph;
    private final List<Edge> uniqueEdges;
    private List<Node> shortestPath = new ArrayList<>();
    private final Runnable onEdgeToggleCallback;

    // View Transformation Variables for Zoom & Pan
    private double zoomFactor = 1.0;
    private double offsetX = 0.0;
    private double offsetY = 0.0;
    private Point mousePt;
    
    private boolean isDisasterActive = false;

    public MapPanel(Map<Node, List<Edge>> graph, List<Edge> uniqueEdges, Runnable onEdgeToggleCallback) {
        this.graph = graph;
        this.uniqueEdges = uniqueEdges;
        this.onEdgeToggleCallback = onEdgeToggleCallback;
        setBackground(new Color(240, 244, 248));

        // Mouse Wheel listener for Zooming
        addMouseWheelListener(e -> {
            double zoomDivisor = 1.1;
            if (e.getWheelRotation() < 0) {
                zoomFactor *= zoomDivisor;
            } else {
                zoomFactor /= zoomDivisor;
                if (zoomFactor < 0.4) zoomFactor = 0.4; // Cap minimum zoom out
            }
            repaint();
        });

        // Mouse Listeners for Drag Panning & Vector Line Clicking
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                mousePt = e.getPoint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                handleTransformMapClick(e.getPoint());
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                int dx = e.getX() - mousePt.x;
                int dy = e.getY() - mousePt.y;
                offsetX += dx;
                offsetY += dy;
                mousePt = e.getPoint();
                repaint();
            }
        });
    }

    public void setShortestPath(List<Node> path) {
        this.shortestPath = path;
        repaint();
    }

    public void setDisasterVisualState(boolean active) {
        this.isDisasterActive = active;
        if (active) {
            setBackground(new Color(250, 219, 216)); // High-alert muted crimson backdrop
        } else {
            setBackground(new Color(240, 244, 248)); // Standard serene backdrop
        }
        repaint();
    }

    private void handleTransformMapClick(Point screenPoint) {
        // Reverse translate screen coordinate clicks back into raw underlying model coordinates
        double modelX = (screenPoint.x - offsetX) / zoomFactor;
        double modelY = (screenPoint.y - offsetY) / zoomFactor;
        double clickTolerance = 8.0 / zoomFactor;

        for (Edge edge : uniqueEdges) {
            Line2D line = new Line2D.Double(edge.source.x, edge.source.y, edge.target.x, edge.target.y);
            if (line.ptLineDist(modelX, modelY) < clickTolerance) {
                boolean newState = !edge.isBlocked;
                edge.isBlocked = newState;
                for (Edge rev : graph.get(edge.target)) {
                    if (rev.target.equals(edge.source)) rev.isBlocked = newState;
                }
                onEdgeToggleCallback.run();
                repaint();
                return;
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Backup current transform matrix
        AffineTransform oldTransform = g2.getTransform();

        // Perform dynamic pan and zoom operations
        g2.translate(offsetX, offsetY);
        g2.scale(zoomFactor, zoomFactor);

        // Phase 1: Draw Roads (Edges)
        for (Edge edge : uniqueEdges) {
            if (edge.isBlocked) {
                g2.setColor(new Color(255, 51, 102)); // Hazard Crimson Red
                g2.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{10, 8}, 0));
            } else {
                g2.setColor(edge.source.type.equals("Residential") && edge.target.type.equals("Residential") 
                            ? new Color(149, 165, 166) : new Color(52, 73, 94)); 
                g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            }
            g2.drawLine(edge.source.x, edge.source.y, edge.target.x, edge.target.y);
        }

        // Phase 2: Overlay Shortest Active Path
        if (shortestPath != null && shortestPath.size() > 1) {
            g2.setColor(new Color(0, 229, 255)); // Neon Cyan Track Lines
            g2.setStroke(new BasicStroke(5.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 0; i < shortestPath.size() - 1; i++) {
                g2.drawLine(shortestPath.get(i).x, shortestPath.get(i).y, shortestPath.get(i+1).x, shortestPath.get(i+1).y);
            }
        }

        // Phase 3: Draw Midpoint Weight Badges
        for (Edge edge : uniqueEdges) {
            int midX = (edge.source.x + edge.target.x) / 2;
            int midY = (edge.source.y + edge.target.y) / 2;
            String weightText = String.valueOf((int) edge.weight);
            
            g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
            FontMetrics fm = g2.getFontMetrics();
            int rW = fm.stringWidth(weightText) + 8;
            int rH = fm.getHeight() + 2;

            g2.setColor(new Color(255, 255, 255, 210));
            g2.fillRoundRect(midX - rW / 2, midY - rH / 2, rW, rH, 4, 4);
            g2.setColor(edge.isBlocked ? new Color(255, 51, 102) : new Color(44, 62, 80));
            g2.drawRoundRect(midX - rW / 2, midY - rH / 2, rW, rH, 4, 4);
            g2.drawString(weightText, midX - fm.stringWidth(weightText) / 2, midY + fm.getAscent() / 2 - 1);
        }

        // Phase 4: Draw Node Structures and Smart Labels
        for (Node node : graph.keySet()) {
            int size = 22;
            int hSize = size / 2;

            switch (node.type) {
                case "Warehouse" -> {
                    g2.setColor(new Color(243, 156, 18)); // Orange block
                    g2.fillRect(node.x - hSize, node.y - hSize, size, size);
                }
                case "Hospital" -> {
                    g2.setColor(new Color(211, 47, 47)); // Medical Cross
                    int t = 6;
                    g2.fillRect(node.x - t / 2, node.y - hSize, t, size);
                    g2.fillRect(node.x - hSize, node.y - t / 2, size, t);
                }
                case "Shelter" -> {
                    g2.setColor(new Color(39, 174, 96)); // Green Shelter Roof
                    g2.fillPolygon(new int[]{node.x, node.x - hSize, node.x + hSize}, new int[]{node.y - hSize, node.y + hSize, node.y + hSize}, 3);
                }
                case "Residential" -> {
                    g2.setColor(new Color(155, 89, 182)); // Purple Residential Diamond
                    g2.fillPolygon(new int[]{node.x, node.x - hSize, node.x, node.x + hSize}, new int[]{node.y - hSize, node.y, node.y + hSize, node.y}, 4);
                }
                default -> {
                    g2.setColor(new Color(52, 73, 94)); // Dark Gray Intersections
                    g2.fillOval(node.x - 6, node.y - 6, 12, 12);
                }
            }
            
            g2.setColor(Color.DARK_GRAY);
            if (node.type.equals("Warehouse")) g2.drawRect(node.x - hSize, node.y - hSize, size, size);
            if (node.type.equals("Intersection")) g2.drawOval(node.x - 6, node.y - 6, 12, 12);

            // Smart Anti-Overlap Label Engine
            g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
            FontMetrics fm = g2.getFontMetrics();
            int labelWidth = fm.stringWidth(node.name) + 8;
            int labelHeight = fm.getHeight() + 4;

            // Default label positioning (Top-Right)
            int textX = node.x + 12;
            int textY = node.y - 12;

            // Strategic direction rules based on specific known dense junctions
            if (node.name.equals("Ghanta Ghar (Clock Tower)")) {
                textX = node.x - labelWidth / 2;
                textY = node.y - 24;
            } else if (node.name.contains("Doon Hospital") || node.name.contains("Prince Chowk")) {
                textX = node.x + 14;
                textY = node.y + 4;
            } else if (node.x > 650) { 
                // Push eastern labels to the left side of the node to avoid cutting off screen edge
                textX = node.x - labelWidth - 12;
            } else if (node.x < 200) {
                // Keep leftmost nodes pushing text cleanly inwards
                textX = node.x + 14;
            }

            // Draw translucent protective text container badge
            g2.setColor(new Color(255, 255, 255, 230));
            g2.fillRoundRect(textX - 4, textY - 2, labelWidth, labelHeight, 6, 6);
            g2.setColor(new Color(200, 214, 229));
            g2.drawRoundRect(textX - 4, textY - 2, labelWidth, labelHeight, 6, 6);

            g2.setColor(new Color(44, 62, 80));
            g2.drawString(node.name, textX, textY + fm.getAscent());
        }

        // Restore matrix transforms
        g2.setTransform(oldTransform);
    }
}

// 3. Application Frame Framework Engine
public class DisasterManagementSystemUI extends JFrame {
    private final Map<Node, List<Edge>> graph = new HashMap<>();
    private final Map<String, Node> nodeMap = new HashMap<>();
    private final List<Edge> allUniqueRoads = new ArrayList<>();
    
    private MapPanel mapPanel;
    private JTextArea routeDetailsText;
    private JComboBox<String> startDropdown;
    private JComboBox<String> endDropdown;

    public DisasterManagementSystemUI() {
        setTitle("Dehradun City Map - Real-Time Graph Disaster Simulator");
        setSize(1320, 850);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initializeDehradunMap();
        setupUserInterfaceStructure();
        recalculateRoute();
    }

    private void initializeDehradunMap() {
        // --- DEHRADUN DENSE GEOGRAPHIC SCALE (True Relative Proportions) ---
        // Far North (Rajpur & Malsi Hill Areas)
        addNode("Max Hospital Malsi", "Hospital", 480, 70);
        addNode("Rajpur Village", "Residential", 540, 110);
        addNode("Jakhan Colony", "Residential", 490, 190);
        addNode("Chironwali Residential", "Residential", 620, 240);

        // Central Heart (Clock Tower & Surrounding Grid)
        addNode("Ghanta Ghar (Clock Tower)", "Intersection", 450, 390);
        addNode("Kishan Nagar Chowk", "Intersection", 320, 390);
        addNode("Parade Ground Camp", "Shelter", 570, 390);
        addNode("Doon Hospital", "Hospital", 510, 460);
        addNode("Dalanwala Sectors", "Residential", 620, 350);

        // West Core Corridor (Chakrata Road / Premnagar stretch)
        addNode("Ballupur Chowk", "Intersection", 210, 320);
        addNode("Indira Nagar Pocket", "Residential", 100, 290);
        addNode("Synergy Hospital", "Hospital", 90, 390);
        addNode("Balliwala Chowk", "Intersection", 210, 460);
        addNode("Vasant Vihar Blocks", "Residential", 90, 480);

        // South & East Sprawl (Sahastradhara, Raipur, Haridwar Bypass)
        addNode("Sahastradhara Road Enclave", "Residential", 740, 260);
        addNode("FCI Godown Raipur", "Warehouse", 860, 390);
        addNode("Raipur Sports Stadium", "Shelter", 940, 450);
        addNode("Nehru Colony", "Residential", 660, 530);
        addNode("Prince Chowk", "Intersection", 450, 530);
        addNode("Rispana Pul Chowk", "Intersection", 720, 620);
        addNode("ISBT Supply Depot", "Warehouse", 180, 680);

        // --- ACCURATE ROAD NETWORKS ---
        // Western Wing
        addRoad("ISBT Supply Depot", "Balliwala Chowk", 4);
        addRoad("ISBT Supply Depot", "Prince Chowk", 6);
        addRoad("Vasant Vihar Blocks", "Balliwala Chowk", 2);
        addRoad("Vasant Vihar Blocks", "Synergy Hospital", 3);
        addRoad("Indira Nagar Pocket", "Ballupur Chowk", 2);
        addRoad("Indira Nagar Pocket", "Synergy Hospital", 3);
        addRoad("Ballupur Chowk", "Kishan Nagar Chowk", 3);
        addRoad("Balliwala Chowk", "Kishan Nagar Chowk", 3);
        addRoad("Ballupur Chowk", "Balliwala Chowk", 2);

        // Central Infrastructure Connectors
        addRoad("Kishan Nagar Chowk", "Ghanta Ghar (Clock Tower)", 2);
        addRoad("Prince Chowk", "Ghanta Ghar (Clock Tower)", 3);
        addRoad("Prince Chowk", "Doon Hospital", 2);
        addRoad("Ghanta Ghar (Clock Tower)", "Doon Hospital", 1);
        addRoad("Ghanta Ghar (Clock Tower)", "Parade Ground Camp", 2);

        // Rajpur Road Core Spine
        addRoad("Ghanta Ghar (Clock Tower)", "Jakhan Colony", 4);
        addRoad("Jakhan Colony", "Max Hospital Malsi", 3);
        addRoad("Jakhan Colony", "Rajpur Village", 4);
        addRoad("Max Hospital Malsi", "Rajpur Village", 2);

        // East Bound Channels
        addRoad("Parade Ground Camp", "Dalanwala Sectors", 2);
        addRoad("Jakhan Colony", "Chironwali Residential", 3);
        addRoad("Chironwali Residential", "Sahastradhara Road Enclave", 4);
        addRoad("Dalanwala Sectors", "Sahastradhara Road Enclave", 3);
        addRoad("Dalanwala Sectors", "Nehru Colony", 4);

        // South-Eastern Highway Links
        addRoad("Prince Chowk", "Rispana Pul Chowk", 5);
        addRoad("Nehru Colony", "Rispana Pul Chowk", 2);
        addRoad("Rispana Pul Chowk", "FCI Godown Raipur", 4);
        addRoad("Sahastradhara Road Enclave", "FCI Godown Raipur", 5);
        addRoad("FCI Godown Raipur", "Raipur Sports Stadium", 3);
        addRoad("Raipur Sports Stadium", "Parade Ground Camp", 6);
    }

    private void addNode(String name, String type, int x, int y) {
        Node node = new Node(name, type, x, y);
        graph.put(node, new ArrayList<>());
        nodeMap.put(name, node);
    }

    private void addRoad(String from, String to, double weight) {
        Node u = nodeMap.get(from);
        Node v = nodeMap.get(to);
        if (u != null && v != null) {
            Edge edge1 = new Edge(u, v, weight);
            Edge edge2 = new Edge(v, u, weight);
            graph.get(u).add(edge1);
            graph.get(v).add(edge2);
            allUniqueRoads.add(edge1); 
        }
    }

    private void setupUserInterfaceStructure() {
        setLayout(new BorderLayout());

        mapPanel = new MapPanel(graph, allUniqueRoads, this::recalculateRoute);
        add(mapPanel, BorderLayout.CENTER);

        // Sidebar Styling Panel Layout
        JPanel sideBar = new JPanel();
        sideBar.setLayout(new BoxLayout(sideBar, BoxLayout.Y_AXIS));
        sideBar.setBackground(new Color(24, 28, 36));
        sideBar.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        sideBar.setPreferredSize(new Dimension(360, 850));

        JLabel mainTitle = new JLabel("⚡ DEHRADUN CRISIS COMMAND");
        mainTitle.setForeground(Color.WHITE);
        mainTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        mainTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        sideBar.add(mainTitle);
        sideBar.add(Box.createRigidArea(new Dimension(0, 5)));

        JLabel hintGrid = new JLabel("Scroll: Zoom | Drag: Pan | Click Line: Block Road");
        hintGrid.setForeground(new Color(150, 160, 175));
        hintGrid.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        hintGrid.setAlignmentX(Component.LEFT_ALIGNMENT);
        sideBar.add(hintGrid);
        sideBar.add(Box.createRigidArea(new Dimension(0, 25)));

        // Path Dropdown Selectors
        JPanel routePanel = new JPanel(new GridLayout(2, 2, 8, 8));
        routePanel.setBackground(new Color(24, 28, 36));
        routePanel.setMaximumSize(new Dimension(340, 65));
        routePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel srcL = new JLabel("Source Hub:"); srcL.setForeground(Color.LIGHT_GRAY);
        JLabel dstL = new JLabel("Target Relief:"); dstL.setForeground(Color.LIGHT_GRAY);

        String[] nodeNameChoices = nodeMap.keySet().toArray(new String[0]);
        Arrays.sort(nodeNameChoices);

        startDropdown = new JComboBox<>(nodeNameChoices);
        endDropdown = new JComboBox<>(nodeNameChoices);
        startDropdown.setSelectedItem("ISBT Supply Depot");
        endDropdown.setSelectedItem("Parade Ground Camp");

        startDropdown.addActionListener(e -> recalculateRoute());
        endDropdown.addActionListener(e -> recalculateRoute());

        routePanel.add(srcL); routePanel.add(dstL);
        routePanel.add(startDropdown); routePanel.add(endDropdown);
        sideBar.add(routePanel);
        sideBar.add(Box.createRigidArea(new Dimension(0, 25)));

        // Action Trigger Buttons
        JPanel buttonRow = new JPanel(new GridLayout(1, 2, 10, 0));
        buttonRow.setBackground(new Color(24, 28, 36));
        buttonRow.setMaximumSize(new Dimension(340, 45));
        buttonRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton simDisasterBtn = new JButton("TRIGGER DISASTER");
        simDisasterBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        simDisasterBtn.setBackground(new Color(231, 76, 60)); // Vivid crimson alert button
        simDisasterBtn.setForeground(Color.WHITE);
        simDisasterBtn.setFocusPainted(false);
        simDisasterBtn.addActionListener(e -> triggerRandomDisasterState());

        JButton clearBtn = new JButton("RESET CITY MAP");
        clearBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        clearBtn.setBackground(new Color(52, 152, 219));
        clearBtn.setForeground(Color.WHITE);
        clearBtn.setFocusPainted(false);
        clearBtn.addActionListener(e -> resetCityState());

        buttonRow.add(simDisasterBtn);
        buttonRow.add(clearBtn);
        sideBar.add(buttonRow);
        sideBar.add(Box.createRigidArea(new Dimension(0, 25)));

        // Logs Terminal Panel Component
        JLabel monitorLbl = new JLabel("📋 LOGISTICS NAVIGATION COMMAND FEED");
        monitorLbl.setForeground(new Color(156, 163, 175));
        monitorLbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        monitorLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        sideBar.add(monitorLbl);
        sideBar.add(Box.createRigidArea(new Dimension(0, 8)));

        routeDetailsText = new JTextArea(22, 20);
        routeDetailsText.setFont(new Font("Consolas", Font.PLAIN, 12));
        routeDetailsText.setEditable(false);
        routeDetailsText.setLineWrap(true);
        routeDetailsText.setWrapStyleWord(true);
        routeDetailsText.setBackground(new Color(15, 18, 24));
        
        JScrollPane scrollBox = new JScrollPane(routeDetailsText);
        scrollBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        scrollBox.setBorder(BorderFactory.createEmptyBorder());
        sideBar.add(scrollBox);

        add(sideBar, BorderLayout.EAST);
    }

    private void triggerRandomDisasterState() {
        for (Edge edge : allUniqueRoads) {
            edge.isBlocked = false;
        }

        // Simulate disaster on roughly 35% of the roads
        int roadsToDestroyCount = (int) (allUniqueRoads.size() * 0.35);
        Collections.shuffle(allUniqueRoads);
        
        for (int i = 0; i < roadsToDestroyCount; i++) {
            Edge targetEdge = allUniqueRoads.get(i);
            targetEdge.isBlocked = true;
            for (Edge rev : graph.get(targetEdge.target)) {
                if (rev.target.equals(targetEdge.source)) rev.isBlocked = true;
            }
        }

        mapPanel.setDisasterVisualState(true);
        recalculateRoute();
    }

    private void resetCityState() {
        for (Edge edge : allUniqueRoads) {
            edge.isBlocked = false;
            for (Edge rev : graph.get(edge.target)) {
                if (rev.target.equals(edge.source)) rev.isBlocked = false;
            }
        }
        mapPanel.setDisasterVisualState(false);
        recalculateRoute();
    }

    // 4. Dijkstra Algorithm Core Optimization Loop
    private void recalculateRoute() {
        String startItem = (String) startDropdown.getSelectedItem();
        String endItem = (String) endDropdown.getSelectedItem();

        if (startItem == null || endItem == null || startItem.equals(endItem)) {
            mapPanel.setShortestPath(new ArrayList<>());
            routeDetailsText.setText("📍 System Standby:\nOrigin and destination targets overlap.");
            routeDetailsText.setForeground(Color.ORANGE);
            return;
        }

        Node start = nodeMap.get(startItem);
        Node end = nodeMap.get(endItem);

        Map<Node, Double> distances = new HashMap<>();
        Map<Node, Node> parentMap = new HashMap<>();
        PriorityQueue<NodeDistancePair> pq = new PriorityQueue<>(Comparator.comparingDouble(p -> p.distance));

        for (Node node : graph.keySet()) {
            distances.put(node, Double.MAX_VALUE);
        }
        distances.put(start, 0.0);
        pq.add(new NodeDistancePair(start, 0.0));

        while (!pq.isEmpty()) {
            Node u = pq.poll().node;
            if (u.equals(end)) break;

            for (Edge edge : graph.get(u)) {
                if (edge.isBlocked) continue;

                Node v = edge.target;
                double newDist = distances.get(u) + edge.weight;
                if (newDist < distances.get(v)) {
                    distances.put(v, newDist);
                    parentMap.put(v, u);
                    pq.add(new NodeDistancePair(v, newDist));
                }
            }
        }

        if (distances.get(end) == Double.MAX_VALUE) {
            mapPanel.setShortestPath(new ArrayList<>());
            routeDetailsText.setText("🚨 EMERGENCY NOTICE:\nDehradun routing vectors severed. Destination completely isolated due to road blocks!");
            routeDetailsText.setForeground(new Color(255, 51, 102));
        } else {
            List<Node> computedPath = new ArrayList<>();
            Node current = end;
            while (current != null) {
                computedPath.add(0, current);
                current = parentMap.get(current);
            }
            mapPanel.setShortestPath(computedPath);

            StringBuilder pathString = new StringBuilder("➔ EN-ROUTE IN DEHRADUN:\n\n");
            for (int i = 0; i < computedPath.size(); i++) {
                pathString.append("[").append(computedPath.get(i).name).append("]");
                if (i < computedPath.size() - 1) pathString.append("\n    ➔ ");
            }
            pathString.append("\n\n⏱️ Path Travel Time Cost: ").append(distances.get(end).intValue()).append(" mins");
            
            routeDetailsText.setText(pathString.toString());
            routeDetailsText.setForeground(new Color(0, 229, 255));
        }
    }

    private static class NodeDistancePair {
        Node node;
        double distance;
        NodeDistancePair(Node node, double distance) {
            this.node = node;
            this.distance = distance;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DisasterManagementSystemUI().setVisible(true));
    }
}