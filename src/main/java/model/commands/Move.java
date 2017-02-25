package model.commands;

public class Move implements Command{

  private final int from;
  private final int to;
  private final int count;

  public Move(int from, int to, int count) {
    this.from = from;
    this.to = to;
    this.count = count;
  }

  @Override
  public String toString() {
    return String.format("MOVE %d %d %d", from, to, count);
  }
}
