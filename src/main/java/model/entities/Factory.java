package model.entities;

import model.Owner;

import java.util.*;

public class Factory extends Entity {

  private int count;
  private int production;
  private boolean incomingBomb;
  private Map<Factory, Integer> distancesToNeighbours = new HashMap<>();
  private List<Troop> incomingAllies = new ArrayList<>();
  private List<Troop> incomingEnemies = new ArrayList<>();
  private int incomingAllyCount;
  private int incomingEnemyCount;

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

  public boolean isIncomingBomb() {
    return incomingBomb;
  }

  public void setIncomingBomb(boolean incomingBomb) {
    this.incomingBomb = incomingBomb;
  }

  public List<Troop> getIncomingAllies() {
    return Collections.unmodifiableList(incomingAllies);
  }

  public void addIncomingAlly(Troop troop) {
    incomingAllies.add(troop);
    incomingAllyCount += troop.getCount();
  }

  public List<Troop> getIncomingEnemies() {
    return Collections.unmodifiableList(incomingEnemies);
  }

  public void addIncomingEnemy(Troop troop) {
    incomingEnemies.add(troop);
    incomingEnemyCount += troop.getCount();
  }

  public int incomingDiff() {
    return incomingAllyCount - incomingEnemyCount;
  }

  public void clearWave() {
    incomingAllies.clear();
    incomingEnemies.clear();
    incomingAllyCount = 0;
    incomingEnemyCount = 0;
  }

  public int getIncomingAllyCount() {
    return incomingAllyCount;
  }

  public int getIncomingEnemyCount() {
    return incomingEnemyCount;
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

  public int enemiesDistSum() {
    int dist = 0;
    for (Map.Entry<Factory, Integer> neigh : distancesToNeighbours.entrySet()) {
      if (neigh.getKey().getOwner() == Owner.ENEMY) {
        dist += neigh.getValue();
      }
    }
    return dist;
  }

  @Override
  public String toString() {
    return String.format("[Factory(id = %d; owner = %s; count = %d; production = %d)]", getId(), getOwner(), count, production);
  }
}
