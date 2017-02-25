package model.entities;

import model.Owner;

public class Entity {

  private final int id;
  private Owner owner;

  public Entity(int id, Owner owner) {
    this.id = id;
    this.owner = owner;
  }

  public int getId() {
    return id;
  }

  public Owner getOwner() {
    return owner;
  }

  public void setOwner(Owner owner) {
    this.owner = owner;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Entity entity = (Entity) o;

    return id == entity.id;

  }

  @Override
  public int hashCode() {
    return id;
  }
}
