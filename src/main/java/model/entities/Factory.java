package model.entities;

import model.Constants;
import model.FactoryTag;
import model.Owner;

import java.util.*;

public class Factory extends Entity {

  private int count;
  private int production;
  private int explodeIn = Integer.MAX_VALUE;
  private int recoverIn = 0;
  private Map<Factory, Integer> distancesToNeighbours = new HashMap<>();
  private List<Troop> incomingAllies = new ArrayList<>();
  private List<Troop> incomingEnemies = new ArrayList<>();
  private int incomingAllyCount;
  private int incomingEnemyCount;
  private int sumDistToAllies;
  private int sumDistToEnemies;
  private int sumDistToAll;
  private FactoryTag tag = FactoryTag.UNKNOWN;

  public Factory(int id, Owner owner, int count, int production) {
    super(id, owner);
    this.count = count;
    this.production = production;
  }

  public Factory(int id) {
    super(id, Owner.NEUTRAL);
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

  public int getExplodeIn() {
    return explodeIn;
  }

  public void setExplodeIn(int explodeIn) {
    this.explodeIn = explodeIn;
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

  public int influenceDiff() {
    double allyInfluence = 0.0;
    for (Troop troop : incomingAllies) {
      allyInfluence += troop.getCount() * Constants.TROOP_INFLUENCE_MODIFIER_COUNT + troop.getEta() * Constants.TROOP_INFLUENCE_MODIFIER_DIST;
    }
    allyInfluence *= getProduction();

    double enemyInfluence = 0.0;
    for (Troop troop : incomingEnemies) {
      enemyInfluence += troop.getCount() * Constants.TROOP_INFLUENCE_MODIFIER_COUNT + troop.getEta() * Constants.TROOP_INFLUENCE_MODIFIER_DIST;
    }
    enemyInfluence *= getProduction();
    return (int)(allyInfluence - enemyInfluence);
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

  public int distanceToClosestEnemy() {
    int dist = Constants.MAX_DISTANCE;
    for (Map.Entry<Factory, Integer> entry : distancesToNeighbours.entrySet()) {
      if (entry.getKey().getOwner() == Owner.ENEMY && entry.getValue() < dist) {
        dist = entry.getValue();
      }
    }
    return dist;
  }

  public int distanceToClosestAlly() {
    int dist = Constants.MAX_DISTANCE;
    for (Map.Entry<Factory, Integer> entry : distancesToNeighbours.entrySet()) {
      if (entry.getKey().getOwner() == Owner.ME && entry.getValue() < dist) {
        dist = entry.getValue();
      }
    }
    return dist;
  }

  public Factory closestAlly() {
    Factory closest = null;
    int dist = Constants.MAX_DISTANCE;
    for (Map.Entry<Factory, Integer> entry : distancesToNeighbours.entrySet()) {
      if (entry.getKey().getOwner() == Owner.ME && entry.getValue() < dist) {
        dist = entry.getValue();
        closest = entry.getKey();
      }
    }
    return closest;
  }

  public Factory closestEnemy() {
    Factory closest = null;
    int dist = Constants.MAX_DISTANCE;
    for (Map.Entry<Factory, Integer> entry : distancesToNeighbours.entrySet()) {
      if (entry.getKey().getOwner() == Owner.ENEMY && entry.getValue() < dist) {
        dist = entry.getValue();
        closest = entry.getKey();
      }
    }
    return closest;
  }

  public Map<Factory, Integer> getDistancesToNeighbours() {
    return Collections.unmodifiableMap(distancesToNeighbours);
  }

  public int getSumDistToAllies() {
    return sumDistToAllies;
  }

  public int getSumDistToEnemies() {
    return sumDistToEnemies;
  }

  public int getRecoverIn() {
    return recoverIn;
  }

  public void setRecoverIn(int recoverIn) {
    this.recoverIn = recoverIn;
  }

  public void recalculateDists() {
    sumDistToAllies = 0;
    sumDistToEnemies = 0;
    for (Map.Entry<Factory, Integer> neigh : distancesToNeighbours.entrySet()) {
      if (neigh.getKey().getOwner() == Owner.ENEMY) {
        sumDistToEnemies += neigh.getValue();
      } else if (neigh.getKey().getOwner() == Owner.ME) {
        sumDistToAllies += neigh.getValue();
      }
    }
    sumDistToAll = sumDistToAllies + sumDistToEnemies;
  }

  public FactoryTag getTag() {
    return tag;
  }

  public void setTag(FactoryTag tag) {
    this.tag = tag;
  }

  public int getSumDistToAll() {
    return sumDistToAll;
  }

  @Override
  public String toString() {
    return String.format("[Factory(id = %d; owner = %s; count = %d; production = %d; explodeIN = %d)]", getId(), getOwner(), count, production, explodeIn);
  }
}
