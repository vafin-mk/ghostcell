package ai;

import model.Constants;
import model.Link;
import model.Owner;
import model.commands.*;
import model.entities.Bomb;
import model.entities.Factory;
import model.entities.Troop;

import java.util.*;
import java.util.stream.Collectors;

public class AI3 {

  private void outputAnswer() {
    List<Command> commands = calculateCommands();
    commands.add(new Wait());

    StringBuilder builder = new StringBuilder();
    commands.forEach(command -> builder.append(command.toString()).append(";"));
    builder.setLength(builder.length() - 1);

    System.out.println(builder);
  }

  private List<Command> calculateCommands() {
    List<Command> commands = new ArrayList<>();

//    myFactories.forEach(factory -> {
//      System.err.println(factory.getId() + "__" + factory.getProduction() + "__" + factory.getCount() + "__" + factory.distanceToClosestEnemy());
//    );
    boolean skipProd = false;
    if (round == 0) {
      List<Factory> neigs = neighFactories(myFactories.get(0));
      if (neigs.stream().filter(factory -> factory.getOwner() == Owner.NEUTRAL)
        .map(Factory::getProduction).reduce((p1,p2) -> p1 + p2).get() > 5) {
        skipProd = true;
      }
    }
    if (!skipProd) {
      myFactories.stream()
        .filter(factory -> factory.getProduction() < Constants.MAX_PRODUCTION
          && factory.getCount() >= Constants.PRODUCTION_INCREMENT_COST
          && unitsRequiredToDefence(factory) < Constants.PRODUCTION_INCREMENT_COST)
        .forEach(factory -> {
          factory.setCount(factory.getCount() - Constants.PRODUCTION_INCREMENT_COST);
          commands.add(new Increment(factory.getId()));
        });
    }

    if (round == 0) {
      immediateBomb(commands);
    }
    myFactories.forEach(factory -> {
      factoryMove(factory, commands);
    });

    return commands;
  }

  private void immediateBomb(List<Command> commands) {
    Factory my = myFactories.get(0);
    Factory enemy = enemyFactories.get(0);
    int distToEnemy = my.distanceTo(enemy);
    if (enemy.getProduction() == 3
      || (enemy.getProduction() == 2 && distToEnemy <= 10)
      || (enemy.getProduction() == 1 && distToEnemy <= 5)) {
      commands.add(new Boom(my.getId(), enemy.getId()));
      enemy.setExplodeIn(my.distanceTo(enemy));
      myBombsCount--;
    }

    List<Factory> enemyTargets = factories.values().stream()
      .filter(factory -> factory.distanceTo(enemy) < 5 && factory.getProduction() > 1)
      .collect(Collectors.toList());

    if (enemyTargets.size() == 1) {
      Factory target = enemyTargets.get(0);
      commands.add(new Boom(my.getId(), target.getId()));
      target.setExplodeIn(my.distanceTo(target));
      myBombsCount--;
    }
  }

  private void factoryMove(Factory factory, List<Command> commands) {
    int forDef = unitsRequiredToDefence(factory);
    if (forDef < 0) forDef = 0;
    int freeUnits = factory.getCount() - forDef;
    if (factory.getExplodeIn() == 2) {
      freeUnits = factory.getCount();
    }
    if (freeUnits <= 0) return;

    if (isFrontier(factory)) {
      frontierMove(factory, commands, freeUnits);
    } else {
      simpleMove(factory, commands, freeUnits);
    }
  }

  private void simpleMove(Factory factory, List<Command> commands, int freeUnits) {
    List<Factory> neighs = neighFactories(factory, 3);
    if (neighs.isEmpty()) neighs = neighFactories(factory, 6);
    if (neighs.isEmpty()) neighs = neighFactories(factory, 9);
    if (neighs.isEmpty()) neighs = neighFactories(factory, 12);
    if (neighs.isEmpty()) neighs = neighFactories(factory, 15);
    if (neighs.isEmpty()) neighs = neighFactories(factory, 20);

    if (factory.getProduction() == 1 || factory.getProduction() == 2) {
      if (factory.distanceToClosestEnemy() > 7) return;
    }

    for (Factory neigh : neighs) {
      if (freeUnits <= 0) return;
      if (freeUnits > Constants.PRODUCTION_INCREMENT_COST) {
        if (neigh.getOwner() == Owner.ME || neigh.getCount() == 0) {
          commands.add(new Move(factory.getId(), neigh.getId(), Constants.PRODUCTION_INCREMENT_COST));
          freeUnits -= Constants.PRODUCTION_INCREMENT_COST;
        }
      }

      if (neigh.getOwner() == Owner.ENEMY) {
        commands.add(new Move(factory.getId(), neigh.getId(), freeUnits));
        return;
      }

      if (neigh.getOwner() == Owner.NEUTRAL && freeUnits >= neigh.getCount()) {
        commands.add(new Move(factory.getId(), neigh.getId(), freeUnits));
        return;
      }
    }

    if (neighs.contains(castle)) {
      commands.add(new Move(factory.getId(), castle.getId(), freeUnits));
      return;
    }

    int finalFreeUnits = freeUnits;
    neighs.stream()
      .sorted((f1, f2) -> Integer.compare(f1.distanceTo(castle), f2.distanceTo(castle)))
      .findFirst()
      .ifPresent(factory1 -> {
        commands.add(new Move(factory.getId(), factory1.getId(), finalFreeUnits));
        return;
      });
  }

  private void frontierMove(Factory factory, List<Command> commands, int freeUnits) {
    List<Factory> neighs = neighFactories(factory, 5);
    if (neighs.isEmpty()) neighs = neighFactories(factory, 8);
    if (neighs.isEmpty()) neighs = neighFactories(factory, 12);
    if (neighs.isEmpty()) neighs = neighFactories(factory, 16);
    if (neighs.isEmpty()) neighs = neighFactories(factory, 20);

    List<Factory> enemies = neighs.stream()
      .filter(factory1 -> factory1.getOwner() == Owner.ENEMY)
      .sorted((f1, f2) -> {
        if (f1.getProduction() != f2.getProduction()) return Integer.compare(f2.getProduction(), f1.getProduction());
        if (f1.getCount() != f2.getCount()) return Integer.compare(f1.getCount(), f2.getCount());
        return Integer.compare(f1.distanceTo(factory), f2.distanceTo(factory));
      })
      .collect(Collectors.toList());

    if (!enemies.isEmpty()) {
      for (Factory enemy : enemies) {
        if (freeUnits <= 0) return;
        if (myBombsCount > 0) frontierBomb(factory, enemy, commands);
        if (enemy.getExplodeIn() < 20 && enemy.getExplodeIn() > enemy.distanceTo(factory)) {
          continue;
        }
        if (freeUnits > (enemy.getCount() + enemy.getProduction() * enemy.distanceTo(factory))) {
          commands.add(new Move(factory.getId(), enemy.getId(), freeUnits));
          freeUnits = 0;
          continue;
        }
      }
    }

    List<Factory> neutrals = neighs.stream()
      .filter(factory1 -> factory1.getOwner() == Owner.NEUTRAL)
      .sorted((f1, f2) -> {
        if (f1.getProduction() != f2.getProduction()) return Integer.compare(f2.getProduction(), f1.getProduction());
        if (f1.getCount() != f2.getCount()) return Integer.compare(f1.getCount(), f2.getCount());
        return Integer.compare(f1.distanceTo(factory), f2.distanceTo(factory));
      })
      .collect(Collectors.toList());

    for (Factory neutral : neutrals) {
      if (freeUnits <= 0) return;
      if (freeUnits <= neutral.getCount()) continue;
      int sending = neutral.getCount() + 1;
      if (sending + Constants.PRODUCTION_INCREMENT_COST <= freeUnits && neutral.getProduction() < 3) {
        sending += Constants.PRODUCTION_INCREMENT_COST;
      }
      commands.add(new Move(factory.getId(), neutral.getId(), sending));
      freeUnits -= sending;
    }
  }

  private void frontierBomb(Factory factory, Factory enemy, List<Command> commands) {
    if (enemy.getExplodeIn() < 20) return;
    int distance = factory.distanceTo(enemy);
    if (enemy.getProduction() == 3
      || (enemy.getProduction() == 2 && distance <= 5 && enemy.getCount() >= 5)
      || (enemy.getProduction() == 1 && distance <= 5 && enemy.getCount() >= 10)) {
      commands.add(new Boom(factory.getId(), enemy.getId()));
      enemy.setExplodeIn(distance);
      myBombsCount--;
    }
  }

  private int unitsRequiredToDefence(Factory factory) {
    int required = 0;
    Map<Integer, Integer> enemyWave = new HashMap<>();
    countIncomingWaves(enemyWave, enemyTroops, factory, 10);
    Map<Integer, Integer> myWave = new HashMap<>();
    countIncomingWaves(myWave, myTroops, factory, 10);

    int prod = factory.getProduction();
    for (int turn = 0; turn < 10; turn++) {
      final int[] diffTurn = new int[] {myWave.get(turn) - enemyWave.get(turn)};
      diffTurn[0] += prod;

      int finalTurn = turn;
//      factory.getDistancesToNeighbours().entrySet()
//        .stream()
//        .filter(entry -> entry.getValue() >= finalTurn
//          && entry.getValue() <= 3
//          && entry.getKey().getOwner() == Owner.ENEMY)
//        .map(Map.Entry::getKey)
//        .forEach(neigh -> diffTurn[0] -= neigh.getProduction());

      if (diffTurn[0] < 0) {
        required -= diffTurn[0];
      }
    }
    return required;
  }

  ////////////////////////////////////////////////////////////////////////////////////

  private void countIncomingWaves(Map<Integer, Integer> wave, List<Troop> troops, Factory targetFactory, int deep) {
    for (int i = 0; i < deep; i++) {
      wave.put(i, 0);
    }
    troops.stream()
      .filter(troop -> troop.getTo() == targetFactory.getId() && troop.getEta() < deep)
      .forEach(troop -> {
        int turn = troop.getEta();
        wave.put(turn, wave.get(turn) + troop.getCount());
      });
  }

  private List<Factory> neighFactories(Factory factory, int lookupDistance) {
    return factories.values().stream().filter(factory1 -> factory1.distanceTo(factory) <= lookupDistance).collect(Collectors.toList());
  }

  private List<Factory> neighFactories(Factory factory) {
    return neighFactories(factory, 5);
  }

  private boolean isFrontier(Factory factory) {
    return castle.equals(factory) || factory.distanceTo(castle) < 3;
  }

  private void updateStructures() {
    myFactories.clear();
    enemyFactories.clear();
    neutralFactories.clear();
    myTroops.clear();
    enemyTroops.clear();
    myBombs.clear();
    enemyBombs.clear();

    factories.values().forEach(factory -> {
      switch (factory.getOwner()) {
        case ME: myFactories.add(factory); break;
        case NEUTRAL: neutralFactories.add(factory); break;
        case ENEMY: enemyFactories.add(factory); break;
      }
    });

    troops.values().forEach(troop -> {
      switch (troop.getOwner()) {
        case ME: myTroops.add(troop); break;
        case ENEMY: enemyTroops.add(troop); break;
      }
    });

    bombs.values().forEach(bomb -> {
      switch (bomb.getOwner()) {
        case ME: myBombs.add(bomb); break;
        case ENEMY: enemyBombs.add(bomb); break;
      }
    });

    for (Factory myFactory : myFactories) {
      if (castle == null) {
        castle = myFactory;
        continue;
      }
      if (myFactory.getSumDistToEnemies() < castle.getSumDistToEnemies()) {
        castle = myFactory;
      }
    }

  }

  private void inputWorld() {
    troopsRemoveNextTurn.forEach(troops::remove);
    troopsRemoveNextTurn.clear();
    bombsRemoveNextTurn.forEach(bombs::remove);
    bombsRemoveNextTurn.clear();

    int entityCount = scanner.nextInt(); // the number of entities (e.g. factories and troops)
    for (int i = 0; i < entityCount; i++) {
      int entityId = scanner.nextInt();
      String entityType = scanner.next();
      int arg1 = scanner.nextInt();
      int arg2 = scanner.nextInt();
      int arg3 = scanner.nextInt();
      int arg4 = scanner.nextInt();
      int arg5 = scanner.nextInt();
      Owner owner = Owner.fromVal(arg1);
      switch (entityType) {
        case "FACTORY":
          if (!factories.containsKey(entityId)) {
            Factory factory = new Factory(entityId, owner, arg2, arg3);
            factories.put(entityId, factory);
          } else {
            Factory factory = factories.get(entityId);
            factory.setOwner(owner);
            factory.setCount(arg2);
            factory.setProduction(arg3);
            if (factory.getExplodeIn() <= 1) {
              factory.setExplodeIn(Integer.MAX_VALUE);
            } else {
              factory.setExplodeIn(factory.getExplodeIn() - 1);
            }
            factory.clearWave();
          }
          break;
        case "TROOP":
          if (arg5 == 1) {//eta
            troopsRemoveNextTurn.add(entityId);
          }
          if (!troops.containsKey(entityId)) {
            Troop troop = new Troop(entityId, owner, arg2, arg3, arg4, arg5);
            troops.put(entityId, troop);
          } else {
            Troop troop = troops.get(entityId);
            troop.setOwner(owner);
            troop.setFrom(arg2);
            troop.setTo(arg3);
            troop.setCount(arg4);
            troop.setEta(arg5);
          }
          break;
        case "BOMB":
          if (arg4 == 1) {//eta
            bombsRemoveNextTurn.add(entityId);
          }
          if (!bombs.containsKey(entityId)) {
            Bomb bomb = new Bomb(entityId, owner, arg2, arg3, arg4);
            bombs.put(entityId, bomb);
            if (owner == Owner.ENEMY) {
              myFactories.forEach(factory -> factory.setExplodeIn(factory.distanceToClosestEnemy()));
            }
          } else {
            Bomb bomb = bombs.get(entityId);
            bomb.setOwner(owner);
            bomb.setFrom(arg2);
            bomb.setTo(arg3);
            bomb.setEta(arg4);
            if (owner == Owner.ME) {
              factories.get(bomb.getTo()).setExplodeIn(arg4);
            }
          }
          break;
        default:
          throw new IllegalStateException("Fucks!");
      }
    }
    factories.values().forEach(Factory::recalculateDists);
    updateStructures();
  }

  public void start() {
    while(true) {
      inputWorld();
      outputAnswer();
      round++;
    }
  }

  public AI3(Scanner scanner) {
    this.scanner = scanner;
    this.FACTORIES_COUNT = scanner.nextInt();
    for (int id = 0; id < FACTORIES_COUNT; id++) {
      factories.put(id, new Factory(id));
    }
    this.LINKS_COUNT = scanner.nextInt();
    for (int i = 0; i < LINKS_COUNT; i++) {
      int idFirst = scanner.nextInt();
      int idSecond = scanner.nextInt();
      int distance = scanner.nextInt();
      factories.get(idFirst).addNeighbour(factories.get(idSecond), distance);
      factories.get(idSecond).addNeighbour(factories.get(idFirst), distance);
    }
  }

  private final Scanner scanner;
  private final int FACTORIES_COUNT;
  private final int LINKS_COUNT;

  private final Map<Integer, Factory> factories = new HashMap<>();
  private final Map<Integer, Troop> troops = new HashMap<>();
  private final Map<Integer, Bomb> bombs = new HashMap<>();

  private final List<Factory> myFactories = new ArrayList<>();
  private final List<Factory> enemyFactories = new ArrayList<>();
  private final List<Factory> neutralFactories = new ArrayList<>();
  private Factory castle;

  private final List<Troop> myTroops = new ArrayList<>();
  private final List<Troop> enemyTroops = new ArrayList<>();

  private final List<Bomb> myBombs = new ArrayList<>();
  private final List<Bomb> enemyBombs = new ArrayList<>();

  private final List<Integer> bombsRemoveNextTurn = new ArrayList<>();
  private final List<Integer> troopsRemoveNextTurn = new ArrayList<>();

  private int round;

  private int myBombsCount = Constants.INITIAL_BOMBS_COUNT;
  private int enemyBombsCount = Constants.INITIAL_BOMBS_COUNT;
}
