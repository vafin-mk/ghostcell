package model.graph;

import java.util.*;

public class Graph {

  private Map<Integer, Node> nodes = new HashMap<Integer, Node>();
  private List<Edge> edges = new ArrayList<Edge>();

  public void add(int edgesCount, Scanner scanner) {
    for (int i = 0; i < edgesCount; i++) {
      int factory1 = scanner.nextInt();
      int factory2 = scanner.nextInt();
      int distance = scanner.nextInt();
      Node first = getOrAdd(factory1);
      Node second = getOrAdd(factory2);
      Edge edge = new Edge(first, second, distance);
      if (!edges.contains(edge)) {
        edges.add(edge);
      }
    }
  }

  private Node getOrAdd(int factory) {
    Node node;
    if (nodes.containsKey(factory)) {
      node = nodes.get(factory);
    } else {
      node = new Node(factory);
      nodes.put(factory, node);
    }
    return node;
  }
}
