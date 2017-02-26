package model.entities;

import model.Owner;

import java.util.*;

public class Factory extends Entity {

  private int count;
  private int production;
  private boolean sendingBomb;
  private Map<Factory, Integer> distancesToNeighbours = new HashMap<>();
  private List<Troop> incomingAllies = new ArrayList<>();
  private List<Troop> incomingEnemies = new ArrayList<>();

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

  public boolean isSendingBomb() {
    return sendingBomb;
  }

  public void setSendingBomb(boolean sendingBomb) {
    this.sendingBomb = sendingBomb;
  }

  public List<Troop> getIncomingAllies() {
    return Collections.unmodifiableList(incomingAllies);
  }

  public void addIncomingAlly(Troop troop) {
    incomingAllies.add(troop);
  }

  public List<Troop> getIncomingEnemies() {
    return Collections.unmodifiableList(incomingEnemies);
  }

  public void addIncomingEnemy(Troop troop) {
    incomingEnemies.add(troop);
  }

  public int incomingDiff() {
    int alliesSize = 0;
    for (Troop ally : incomingAllies) {
      alliesSize += ally.getCount();
    }
    int enemySize = 0;
    for (Troop enemy : incomingEnemies) {
      enemySize += enemy.getCount();
    }
    return alliesSize - enemySize;
  }

  public void clearWave() {
    incomingAllies.clear();
    incomingEnemies.clear();
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
