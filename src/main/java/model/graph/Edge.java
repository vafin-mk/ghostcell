package model.graph;

public class Edge {

  private final Node first;
  private final Node second;
  private final int dist;

  public Edge(Node first, Node second, int dist) {
    this.first = first;
    this.second = second;
    first.addNeigh(second);
    second.addNeigh(first);
    this.dist = dist;
  }

  public Node getFirst() {
    return first;
  }

  public Node getSecond() {
    return second;
  }

  public int getDist() {
    return dist;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Edge edge = (Edge) o;

    return dist == edge.dist
      && (
      first.getId() == edge.first.getId()
        || first.getId() == edge.second.getId()
    )
      && (
      second.getId() == edge.first.getId()
        || second.getId() == edge.second.getId()
    );
  }

  @Override
  public int hashCode() {
    int result = first.hashCode();
    result = 31 * result + second.hashCode();
    result = 31 * result + dist;
    return result;
  }
}
