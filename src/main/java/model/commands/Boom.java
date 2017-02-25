package model.commands;

public class Boom implements Command {
  private final int from, to;

  public Boom(int from, int to) {
    this.from = from;
    this.to = to;
  }

  @Override
  public String toString() {
    return String.format("BOMB %d %d", from, to);
  }
}
