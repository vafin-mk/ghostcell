package model.commands;

public class Message implements Command{

  private final String message;
  public Message(String message) {
    this.message = message;
  }

  @Override
  public String toString() {
    return String.format("MSG %s", message);
  }
}
