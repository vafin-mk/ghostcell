package model.entities;

import model.Owner;

public class Troop extends Entity {

  private int from;
  private int to;
  private int count;
  private int eta;

  public Troop(int id, Owner owner, int from, int to, int count, int eta) {
    super(id, owner);
    this.from = from;
    this.to = to;
    this.count = count;
    this.eta = eta;
  }

  public int getFrom() {
    return from;
  }

  public void setFrom(int from) {
    this.from = from;
  }

  public int getTo() {
    return to;
  }

  public void setTo(int to) {
    this.to = to;
  }

  public int getCount() {
    return count;
  }

  public void setCount(int count) {
    this.count = count;
  }

  public int getEta() {
    return eta;
  }

  public void setEta(int eta) {
    this.eta = eta;
  }

  @Override
  public String toString() {
    return String.format("[Troop(id = %d; owner = %s; from = %d; to = %d; count = %d; ETA = %d)]", getId(), getOwner(), from, to, count, eta);
  }
}
