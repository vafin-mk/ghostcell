package model.graph;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Node {

  final int id; //equals to factory id
  final Set<Node> neighs = new HashSet<Node>();

  public Node(int id) {
    this.id = id;
  }

  public int getId() {
    return id;
  }

  public Set<Node> getNeighs() {
    return Collections.unmodifiableSet(neighs);
  }

  public void addNeigh(Node neigh) {
    neighs.add(neigh);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Node node = (Node) o;

    return id == node.id;

  }

  @Override
  public int hashCode() {
    return id;
  }
}
