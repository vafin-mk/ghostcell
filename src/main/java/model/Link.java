package model;

public class Link {

  private final int from, to, dist;
  public Link(int from, int to, int dist) {
    this.from = from;
    this.to = to;
    this.dist = dist;
  }

  public int getFrom() {
    return from;
  }

  public int getTo() {
    return to;
  }

  public int getDist() {
    return dist;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Link link = (Link) o;

    if (from != link.from) return false;
    if (to != link.to) return false;
    return dist == link.dist;

  }

  @Override
  public int hashCode() {
    int result = from;
    result = 31 * result + to;
    result = 31 * result + dist;
    return result;
  }

  @Override
  public String toString() {
    return String.format("FROM %s TO %s = %s", from, to, dist);
  }
}
