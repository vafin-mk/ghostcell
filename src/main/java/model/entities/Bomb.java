package model.entities;

import model.Owner;

public class Bomb extends Entity {

  private int from, to, eta;
  public Bomb(int id, Owner owner, int from, int to, int eta) {
    super(id, owner);
    this.from = from;
    this.to = to;
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

  public int getEta() {
    return eta;
  }

  public void setEta(int eta) {
    this.eta = eta;
  }

  @Override
  public String toString() {
    return String.format("[Bomb(id = %d, owner = %s, from = %d, to = %d, eta = %d)]", getId(), getOwner(), from, to, eta);
  }
}
