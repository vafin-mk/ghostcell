package model.entities;

import model.Owner;

public class Troop extends Entity {

  private int factoryFrom;
  private int factoryTo;
  private int count;
  private int remainingTurns;

  public Troop(int id, Owner owner, int factoryFrom, int factoryTo, int count, int remainingTurns) {
    super(id, owner);
    this.factoryFrom = factoryFrom;
    this.factoryTo = factoryTo;
    this.count = count;
    this.remainingTurns = remainingTurns;
  }

  public int getFactoryFrom() {
    return factoryFrom;
  }

  public void setFactoryFrom(int factoryFrom) {
    this.factoryFrom = factoryFrom;
  }

  public int getFactoryTo() {
    return factoryTo;
  }

  public void setFactoryTo(int factoryTo) {
    this.factoryTo = factoryTo;
  }

  public int getCount() {
    return count;
  }

  public void setCount(int count) {
    this.count = count;
  }

  public int getRemainingTurns() {
    return remainingTurns;
  }

  public void setRemainingTurns(int remainingTurns) {
    this.remainingTurns = remainingTurns;
  }

  @Override
  public String toString() {
    return String.format("[Troop(id = %d; owner = %s; from = %d; to = %d; count = %d; ETA = %d)]", getId(), getOwner(), factoryFrom, factoryTo, count, remainingTurns);
  }
}
