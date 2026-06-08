# Disaster Management Simulation System

A Java-based desktop application that simulates localized natural disaster scenarios and calculates optimal evacuation routes on an urban road network. 

## Key Features
* **Graph-Based Map Modeling:** Represents a localized map network (modeled after Dehradun) using custom graph data structures where intersections are vertices and roads are edges.
* **Dynamic Route Optimization:** Implements Dijkstra's Algorithm to dynamically calculate and render the shortest and safest escape routes in real time.
* **Disaster Simulation Engine:** Randomly simulates structural destruction and road blockages across the network, forcing the algorithm to dynamically reroute around hazards.
* **Interactive Frontend:** Built entirely using Java Swing to visually render the network graph, updating structural damage and calculated paths seamlessly for the user.

## Tech Stack
* **Language:** Java
* **GUI Framework:** Java Swing
* **Core Concepts:** Data Structures & Algorithms (DSA), Graph Theory, Dynamic Rerouting
