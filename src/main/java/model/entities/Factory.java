package model.entities;

import model.Owner;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Factory extends Entity {

  private int count;
  private int production;
  private Map<Factory, Integer> distancesToNeighbours = new HashMap<>();

  public Factory(int id, Owner owner, int count, int production) {
    super(id, owner);
    this.count = count;
    this.production = production;
  }

  public int getCount() {
    return count;
  }

  public void setCount(int count) {
    this.count = count;
  }

  public int getProduction() {
    return production;
  }

  public void setProduction(int production) {
    this.production = production;
  }

  public void addNeighbour(Factory neigh, Integer distance) {
    distancesToNeighbours.put(neigh, distance);
  }

  public int distanceTo(Factory neigh) {
    return distancesToNeighbours.getOrDefault(neigh, 100500);
  }

  public Map<Factory, Integer> getDistancesToNeighbours() {
    return Collections.unmodifiableMap(distancesToNeighbours);
  }

  @Override
  public String toString() {
    return String.format("[Factory(id = %d; owner = %s; count = %d; production = %d)]", getId(), getOwner(), count, production);
  }
}
