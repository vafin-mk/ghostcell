package model.commands;

public class Increment implements Command{
  private final int id;
  public Increment(int id) {
    this.id = id;
  }

  @Override
  public String toString() {
    return String.format("INC %d", id);
  }
}
