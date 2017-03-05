package ai;

import model.*;
import model.commands.*;
import model.entities.Bomb;
import model.entities.Factory;
import model.entities.Troop;

import java.util.*;
import java.util.stream.Collectors;

public class AI2 {

  private final Scanner scanner;
  private final int FACTORIES_COUNT;
  private final int LINKS_COUNT;

  private final Map<Integer, Factory> factories = new HashMap<>();
  private final Map<Integer, Troop> troops = new HashMap<>();
  private final Map<Integer, Bomb> bombs = new HashMap<>();

  private final List<Factory> myFactories = new ArrayList<>();
  private final List<Factory> enemyFactories = new ArrayList<>();
  private final List<Factory> neutralFactories = new ArrayList<>();
  private final List<Factory> castles = new ArrayList<>();

  private final List<Troop> myTroops = new ArrayList<>();
  private final List<Troop> enemyTroops = new ArrayList<>();

  private final List<Bomb> myBombs = new ArrayList<>();
  private final List<Bomb> enemyBombs = new ArrayList<>();

  private final List<Integer> bombsRemoveNextTurn = new ArrayList<>();
  private final List<Integer> troopsRemoveNextTurn = new ArrayList<>();

  private int round;
  private GameState gameState = GameState.EARLY;
  private MapSize mapSize = MapSize.MEDIUM;
  private int castlesCount;

  private int myBombsCount = Constants.INITIAL_BOMBS_COUNT;
  private int enemyBombsCount = Constants.INITIAL_BOMBS_COUNT;

  //todo //Floyd–Warshall algorithm for shortcuts

  ///////////////BRAIN///////////////////////

  private void outputAnswer() {
    List<Command> commands = calculateCommands();
    commands.add(new Wait());
    commands.add(coolMessage());

    StringBuilder builder = new StringBuilder();
    commands.forEach(command -> builder.append(command.toString()).append(";"));
    builder.setLength(builder.length() - 1);

    System.out.println(builder);
  }

  private List<Command> calculateCommands() {
    List<Command> commands = new ArrayList<>();

    calculateProductionCommands(commands);
    calculateBombCommands(commands);
    calculateDronesCommand(commands);

    return commands;
  }

  private void calculateDronesCommand(List<Command> commands) {
    Map<Factory, Integer> attackers = new HashMap<>();
    int attackersCount = calculateAttackers(attackers);
    debugAttackers(attackers, attackersCount);
    List<Factory> targets = findTargets();
    prioritizeTargets(targets);
    debugTargets(targets);
    applyAttackersByTargets(attackersCount, attackers, targets, commands);
  }

  private int calculateAttackers(Map<Factory, Integer> attackers) {
    int attackersSum = 0;
    for (Factory myFactory : myFactories) {
      int cyborgs = myFactory.getCount();
      if (myFactory.getExplodeIn() <= 2) {
        //if we explode in 2 turns, remove everyone in next turn, before explode
        attackers.put(myFactory, cyborgs);
        attackersSum += cyborgs;
        continue;
      }

      int defendersNeed = requiredCyborgsToHoldControl(myFactory);
      if (defendersNeed < 0) defendersNeed = 0;
      int attackersCount = cyborgs - defendersNeed;
      if (attackersCount > 0) {
        attackers.put(myFactory, attackersCount);
        attackersSum += attackersCount;
      }
    }
    return attackersSum;
  }

  private List<Factory> findTargets() {
    List<Factory> targets = new ArrayList<>();
    for (Factory potentialTarget : factories.values()) {
//      if (potentialTarget.getOwner() == Owner.ME) {
//        if (requiredCyborgsToHoldControl(potentialTarget) <= 0 && !castles.contains(potentialTarget)) {
//          continue;
//        }
//      }
//      if (gameState == GameState.EARLY) {
//        if (potentialTarget.getProduction() == 0) {
//          continue;
//        }
//      }
      if (gameState == GameState.EARLY) {
        if (potentialTarget.getSumDistToEnemies() < potentialTarget.getSumDistToAllies()) {
          continue;
        }
      } else if (gameState == GameState.MID) {
        if (3 * potentialTarget.getSumDistToEnemies() < potentialTarget.getSumDistToAllies() * 2) {
          continue;
        }
      }

      targets.add(potentialTarget);
    }
    return targets;
  }

  private void prioritizeTargets(List<Factory> targets) {
    targets.sort((f1, f2) -> Integer.compare(calculateFactoryValue(f2), calculateFactoryValue(f1)));
  }

  private int calculateFactoryValue(Factory factory) {
    switch (factory.getOwner()) {
      case ME: return allyFactoryValue(factory);
      case NEUTRAL: return neutralFactoryValue(factory);
      case ENEMY: return enemyFactoryValue(factory);
    }
    throw new IllegalStateException("HOHO");
  }

  private int allyFactoryValue(Factory factory) {
//    if (requiredCyborgsToHoldControl(factory) <= -Constants.PRODUCTION_INCREMENT_COST) return Integer.MIN_VALUE;
    int distsToAlly = factory.getSumDistToAllies();
    int distsToEnemy = factory.getSumDistToEnemies();
    double medianDistToAlly = (double) distsToAlly / myFactories.size();
    double medianDistToEnemy = (double) distsToEnemy / enemyFactories.size();
    double distScore = medianDistToAlly * Constants.SCORE_MULTIPLIER_DISTANCE;
    if (medianDistToAlly > medianDistToEnemy) {
      distScore *= 2;
    } else if (medianDistToAlly < medianDistToEnemy / 2) {
      distScore /= 2;
    }
    double prodScore = factory.getProduction() * Constants.SCORE_MULTIPLIER_PRODUCTION;
    return (int) (prodScore - requiredCyborgsToHoldControl(factory) + distScore);
  }

  private int neutralFactoryValue(Factory factory) {
    int distsToAlly = factory.getSumDistToAllies();
    int distsToEnemy = factory.getSumDistToEnemies();
    double medianDistToAlly = (double) distsToAlly / myFactories.size();
    double medianDistToEnemy = (double) distsToEnemy / enemyFactories.size();
    double distScore = medianDistToAlly * Constants.SCORE_MULTIPLIER_DISTANCE;
    if (medianDistToAlly > medianDistToEnemy) {
      distScore *= 2;
    } else if (medianDistToAlly < medianDistToEnemy / 2) {
      distScore /= 2;
    }
    double prodScore = factory.getProduction() * Constants.SCORE_MULTIPLIER_PRODUCTION;
    double countScore = factory.getCount() * Constants.SCORE_MULTIPLIER_COUNT;
    if (gameState == GameState.EARLY && medianDistToAlly > 8) {
      return Integer.MIN_VALUE;
    }
    return (int) (prodScore + distScore + countScore);
  }

  private int enemyFactoryValue(Factory factory) {
    int distsToAlly = factory.getSumDistToAllies();
    int distsToEnemy = factory.getSumDistToEnemies();
    double medianDistToAlly = (double) distsToAlly / myFactories.size();
    double medianDistToEnemy = (double) distsToEnemy / enemyFactories.size();
    double distScore = medianDistToAlly * Constants.SCORE_MULTIPLIER_DISTANCE;
    if (medianDistToAlly > medianDistToEnemy) {
      distScore *= 2;
    } else if (medianDistToAlly < medianDistToEnemy / 2) {
      distScore /= 2;
    }
    double prodScore = factory.getProduction() * Constants.SCORE_MULTIPLIER_PRODUCTION;
    double countScore = factory.getCount() * Constants.SCORE_MULTIPLIER_COUNT;
    if (gameState == GameState.EARLY && mapSize != MapSize.SMALL) {
      return Integer.MIN_VALUE;
    } else if (medianDistToAlly > 12) {
      return Integer.MIN_VALUE;
    }
    return (int) (prodScore + distScore + countScore);
  }

  private void applyAttackersByTargets(int attackersCount, Map<Factory, Integer> attackers, List<Factory> targets, List<Command> commands) {
    for (Factory target : targets) {
      if (attackersCount <= 0) return;
      attackers.entrySet().removeIf(entry -> entry.getValue() <= 0);
      int sendingAttackers = requiredCyborgsToHoldControl(target);

      if (sendingAttackers <= 0) continue;
      if (attackersCount < sendingAttackers) continue;

      List<Factory> closestAllies = attackers.keySet().stream()
        .sorted((f1, f2) -> Integer.compare(f1.distanceTo(target), f2.distanceTo(target)))
        .collect(Collectors.toList());

      int assignedAttackers = 0;
      for (Factory closestAlly : closestAllies) {
        if(closestAlly.equals(target)) continue;
        int remainingAssigns = sendingAttackers - assignedAttackers;
        if (remainingAssigns <= 0) {
          break;
        }
        int freeAttackers = attackers.get(closestAlly);
        int assign = Math.min(Math.min(freeAttackers, sendingAttackers), remainingAssigns);
        commands.add(new Move(closestAlly.getId(), target.getId(), assign));
        attackers.put(closestAlly, freeAttackers - assign);
        assignedAttackers += assign;
      }
      attackersCount -= assignedAttackers;
    }
    if (attackersCount <= 0) return;

    attackers.entrySet().removeIf(entry -> entry.getValue() <= 0);
    attackers.entrySet().forEach(entry -> {
      List<Factory> facs = myFactories.stream()
        .filter(factory -> factory.getProduction() < Constants.MAX_PRODUCTION)
        .collect(Collectors.toList());
      if (!facs.isEmpty()) {
        int from = entry.getKey().getId();
        int to = facs.get(0).getId();
        int count = entry.getValue();
        if (from != to) {
          commands.add(new Move(from, to, count));
        } else {
          if (facs.size() > 1) {
            to = facs.get(1).getId();
            commands.add(new Move(from, to, count));
          }
        }
      }
    });
    attackers.entrySet().removeIf(entry -> entry.getValue() <= 0);
    attackers.entrySet().forEach(entry -> {
      if (!castles.isEmpty()) {
        Factory castle = orderedCastles(entry.getKey()).get(0);
        if (!entry.getKey().equals(castle)) {
          commands.add(new Move(entry.getKey().getId(), castle.getId(), entry.getValue()));
        }
      }
    });
  }

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

  private int[] unitDiffsByTurns(Factory factory, Map<Integer, Integer> myWave, Map<Integer, Integer> enemyWave, int deep) {
    Owner currentOwner = factory.getOwner();
    int currentUnits = factory.getCount() * (currentOwner == Owner.ME ? 0 : -1);//if we
    int[] diffTurns = new int[deep];
    diffTurns[0] = currentUnits;
    for (int turn = 1; turn < deep; turn++) {
      final int[] diffTurn = new int[] {myWave.get(turn) - enemyWave.get(turn)};
      int prodIncome = currentOwner == Owner.NEUTRAL ? 0 : factory.getProduction();
      prodIncome *= (currentOwner == Owner.ME ? 1 : -1);
      diffTurn[0] += prodIncome;

      int finalTurn = turn;
      factory.getDistancesToNeighbours().entrySet()
        .stream()
        .filter(entry -> entry.getValue() >= finalTurn && entry.getValue() <= deep / 3)
        .map(Map.Entry::getKey)
        .forEach(neigh -> {
          if (neigh.getOwner() == Owner.ME) {
//            diffTurn[0] += neigh.getProduction();
          } else if (neigh.getOwner() == Owner.ENEMY) {
            diffTurn[0] -= neigh.getProduction();
          }
        });

      int absDiff = Math.abs(diffTurn[0]);
      int absUnits = Math.abs(currentUnits);
      if (absUnits >= absDiff) {
        //enemy, neutral -> current units < 0 else current units > 0
        currentUnits = currentUnits - (absDiff * (currentOwner == Owner.ME ? 1 : -1));
      } else {
        int newUnits = absDiff - absUnits;
        switch (currentOwner) {
          case NEUTRAL:
            if (diffTurn[0] > 0) {
              currentOwner = Owner.ME;
              currentUnits = newUnits;
            } else {
              currentOwner = Owner.ENEMY;
              currentUnits = -newUnits;
            }
            break;
          case ENEMY:
            currentOwner = Owner.ME;
            currentUnits = newUnits;
            break;
          case ME:
            currentOwner = Owner.ENEMY;
            currentUnits = -newUnits;
            break;
        }
      }

      diffTurns[turn] = diffTurn[0];
    }

    return diffTurns;
  }

  private int requiredCyborgsToHoldControl(Factory factory) {
    int[] requiredCyborgs = new int[]{0};
    int lookDeepness = 10;

    Map<Integer, Integer> enemyWave = new HashMap<>();
    countIncomingWaves(enemyWave, enemyTroops, factory, lookDeepness);
    Map<Integer, Integer> myWave = new HashMap<>();
    countIncomingWaves(myWave, myTroops, factory, lookDeepness);

    int[] diffTurns = unitDiffsByTurns(factory, myWave, enemyWave, lookDeepness);

    int[] diffSum = new int[]{Arrays.stream(diffTurns).reduce((i1, i2) -> i1 + i2).getAsInt()};

    if (round == 14 && factory.getId() == 11) {
      log("%s --> %s --> %s", factory.getId(), Arrays.toString(diffTurns), diffSum[0]);
    }

    if (factory.getOwner() == Owner.ME) {
      //def
      requiredCyborgs[0] = -diffSum[0];
    } else {
      //attack
      requiredCyborgs[0] = -diffSum[0] + 1;
    }
    return requiredCyborgs[0];
  }

  private void calculateBombCommands(List<Command> commands) {
    if (round == 0) {
      sendImmediateBombs(commands);
      return;
    }

    for (Factory enemyFactory : enemyFactories) {
      if (myBombsCount == 0) {
        return;
      }
      if (enemyFactory.getExplodeIn() < Constants.MAX_DISTANCE) {
        continue;
      }
      Factory closest = enemyFactory.closestAlly();
      if (enemyFactory.getProduction() == 3) {
        commands.add(new Boom(closest.getId(), enemyFactory.getId()));
        enemyFactory.setExplodeIn(closest.distanceTo(enemyFactory));
        myBombsCount--;
        continue;
      }
      int distToClosestAlly = enemyFactory.distanceToClosestAlly();
      if (enemyFactory.getProduction() == 2 && distToClosestAlly <= 6) {
        commands.add(new Boom(closest.getId(), enemyFactory.getId()));
        enemyFactory.setExplodeIn(closest.distanceTo(enemyFactory));
        myBombsCount--;
        continue;
      }
    }
  }

  private void sendImmediateBombs(List<Command> commands) {
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
      .filter(factory -> factory.distanceTo(enemy) <= 5 && factory.getProduction() >= 1)
      .collect(Collectors.toList());

    if (enemyTargets.size() == 1) {
      Factory target = enemyTargets.get(0);
      commands.add(new Boom(my.getId(), target.getId()));
      target.setExplodeIn(my.distanceTo(target));
      myBombsCount--;
    }
  }

  private void calculateProductionCommands(List<Command> commands) {
    if (round == 0) {
      if (enemyFactories.get(0).distanceTo(myFactories.get(0)) <= 10) {
        return;
      }
    }
    myFactories.forEach(factory -> {
      if (factory.getProduction() < Constants.MAX_PRODUCTION
        && factory.getCount() >= Constants.PRODUCTION_INCREMENT_COST + 2
        && requiredCyborgsToHoldControl(factory) < -12
        && factory.getExplodeIn() > Constants.PRODUCTION_INCREMENT_COST + 2) {
        commands.add(new Increment(factory.getId()));
        factory.setCount(factory.getCount() - Constants.PRODUCTION_INCREMENT_COST);
      }
    });
  }

  //////////////\BRAIN/////////////////////////

  public AI2(Scanner scanner) {
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

  public void start() {
    while(true) {
      inputWorld();
      outputAnswer();
      round++;
    }
  }

  private void log(String message) {
    System.err.println(message);
  }

  private void log(String format, Object...args) {
    System.err.println(String.format(format, args));
  }

  private void inputWorld() {
    bombsRemoveNextTurn.forEach(bombs::remove);
    bombsRemoveNextTurn.clear();

    troopsRemoveNextTurn.forEach(troops::remove);
    troopsRemoveNextTurn.clear();

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
          updateFactory(entityId, owner, arg2, arg3, arg4);
          break;
        case "TROOP":
          if (arg5 == 1) {//eta
            troopsRemoveNextTurn.add(entityId);
          }
          updateTroop(entityId, owner, arg2, arg3, arg4, arg5);
          break;
        case "BOMB":
          if (arg4 == 1) {//eta
            bombsRemoveNextTurn.add(entityId);
          }
          updateBomb(entityId, owner, arg2, arg3, arg4);
          break;
        default:
          throw new IllegalStateException("Fucks!");
      }
    }

    factories.values().forEach(Factory::recalculateDists);
    updateStructures();
    updateGameState();
    if (round == 0) {
      initialize();
    }

    debugRoundInfo();
//    debugFactories();
//    debugTroops();
//    debugBombs();

  }

  private void updateFactory(int id, Owner owner, int cyborgs, int production, int freezeOut) {
    Factory factory = factories.get(id);
    factory.setOwner(owner);
    factory.setCount(cyborgs);
    factory.setProduction(production);
    factory.setRecoverIn(freezeOut);

    if (factory.getExplodeIn() <= 1) {
      factory.setExplodeIn(Integer.MAX_VALUE);
    } else {
      factory.setExplodeIn(factory.getExplodeIn() - 1);
    }
    factory.clearWave();
  }

  private void updateTroop(int id, Owner owner, int from, int to, int cyborgs, int eta) {
    if (troops.containsKey(id)) {
      Troop troop = troops.get(id);
      troop.setFrom(from);
      troop.setTo(to);
      troop.setCount(cyborgs);
      troop.setEta(eta);
    } else {
      troops.put(id, new Troop(id, owner, from, to, cyborgs, eta));
    }
  }

  private void updateBomb(int id, Owner owner, int from, int to, int eta) {
    if (bombs.containsKey(id)) {
      Bomb bomb = bombs.get(id);
      bomb.setOwner(owner);
      bomb.setFrom(from);
      bomb.setTo(to);
      bomb.setEta(eta);
      if (owner == Owner.ME) {
        factories.get(bomb.getTo()).setExplodeIn(eta);
      }
    } else {
      if (owner == Owner.ENEMY) {
        enemyBombsCount--;
        myFactories.forEach(factory -> factory.setExplodeIn(factory.closestEnemy().distanceTo(factory)));
      }
      bombs.put(id, new Bomb(id, owner, from, to, eta));
    }
  }

  private void initialize() {
    setTags();
    determineSize();
//    debugDistances();
  }

  private void setTags() {
    Factory mid = factories.get(0);
    Factory my = myFactories.get(0);
    Factory enemy = enemyFactories.get(0);

    mid.setTag(FactoryTag.MID);

    int fronts = 4;
    switch (factories.size()) {
      case 7: fronts = 2;break;
      case 9: fronts = 4;break;
      case 11: fronts = 4;break;
      case 13: fronts = 6;break;
      case 15: fronts = 6;break;
    }

    List<Factory> ordered = mid.getDistancesToNeighbours().entrySet().stream()
      .sorted((e1, e2) -> Integer.compare(e1.getValue(), e2.getValue()))
      .map(Map.Entry::getKey)
      .collect(Collectors.toList());

    for (int i = 0; i < ordered.size(); i++) {
      ordered.get(i).setTag(i < fronts ? FactoryTag.FRONT : FactoryTag.REAR);
    }
  }

  private void recalculateCastles() {
    castlesCount = myFactories.size() / 3;
    if (castlesCount <= 0) castlesCount = 1;
    Factory closestEnemy = closestEnemy();
    if (closestEnemy == null) return;
    castles.addAll(myFactories.stream()
      .sorted((f1, f2) -> Integer.compare(f1.distanceTo(closestEnemy), f2.distanceTo(closestEnemy)))
      .limit(castlesCount).collect(Collectors.toList()));
  }

  private List<Factory> orderedCastles(Factory factory) {
    return castles.stream()
      .sorted((c1, c2) -> Integer.compare(c1.distanceTo(factory), c2.distanceTo(factory)))
      .collect(Collectors.toList());
  }

  private Factory closestEnemy() {
    if (enemyFactories.isEmpty()) return null;
    return enemyFactories.stream()
      .sorted((f1, f2) -> Integer.compare(f1.getSumDistToAllies(), f2.getSumDistToAllies()))
      .findFirst().get();
  }

  private void determineSize() {
    if (factories.size() <= 9) mapSize = MapSize.SMALL;
    else if (factories.size() <= 13) mapSize = MapSize.MEDIUM;
    else mapSize = MapSize.BIG;
  }

  private void updateGameState() {
    if (round < 5) {
      gameState = GameState.EARLY;
    } else if (round > 12) {
      gameState = GameState.LATE;
    } else {
      gameState = GameState.MID;
    }
  }

  private void updateStructures() {
    myFactories.clear();
    enemyFactories.clear();
    neutralFactories.clear();
    myTroops.clear();
    enemyTroops.clear();
    myBombs.clear();
    enemyBombs.clear();
    castles.clear();

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

    recalculateCastles();
  }

  private void debugFactories() {
    StringBuilder builder = new StringBuilder();
    builder.append("=====FACTORIES=======").append(SEP);
    myFactories.forEach(factory -> builder.append(factoryInfo(factory)).append(SEP));
    builder.append(HSEP);
    enemyFactories.forEach(factory -> builder.append(factoryInfo(factory)).append(SEP));
    builder.append(HSEP);
    neutralFactories.forEach(factory -> builder.append(factoryInfo(factory)).append(SEP));
    builder.append("=====FACTORIES=======").append(SEP);
    log(builder.toString());
  }

  private void debugTroops() {
    StringBuilder builder = new StringBuilder();
    builder.append("=====TROOPS=======").append(SEP);
    myTroops.forEach(troop -> builder.append(troop).append(SEP));
    builder.append(HSEP);
    enemyTroops.forEach(troop -> builder.append(troop).append(SEP));
    builder.append("=====TROOPS=======").append(SEP);
    log(builder.toString());
  }

  private void debugBombs() {
    StringBuilder builder = new StringBuilder();
    builder.append("=====BOMBS=======").append(SEP);
    myBombs.forEach(bomb -> builder.append(bomb).append(SEP));
    builder.append(HSEP);
    enemyBombs.forEach(bomb -> builder.append(bomb).append(SEP));
    builder.append("=====BOMBS=======").append(SEP);
    log(builder.toString());
  }

  private void debugRoundInfo() {
    StringBuilder builder = new StringBuilder();
    builder.append("=====ROUND INFO=======").append(SEP);
    builder.append("CURRENT ROUND:").append(round).append(SEP);
    builder.append("MY BOMBS COUNT:").append(myBombsCount).append(SEP);
    builder.append("ENEMY BOMBS COUNT:").append(enemyBombsCount).append(SEP);
    builder.append("GAME STATE ").append(gameState).append(SEP);
    builder.append("MAP SIZE ").append(mapSize).append(SEP);
    builder.append("=====ROUND INFO=======").append(SEP);
    log(builder.toString());
  }

  private void debugAttackers(Map<Factory, Integer> attackers, int attackersCount) {
    StringBuilder builder = new StringBuilder();
    builder.append("=====ATTACKERS=======").append(SEP);
    attackers.entrySet()
      .forEach(entry ->
        builder.append(factoryInfo(entry.getKey()))
          .append(" --> ").append(entry.getValue()).append(SEP));
    builder.append("FULL ATTACKERS COUNT:").append(attackersCount).append(SEP);
    builder.append("=====ATTACKERS=======").append(SEP);
    log(builder.toString());
  }

  private void debugTargets(List<Factory> targets) {
    StringBuilder builder = new StringBuilder();
    builder.append("=====TARGETS=======").append(SEP);
    targets.forEach(target -> builder.append(factoryInfo(target)).append(SEP));
    builder.append("=====TARGETS=======").append(SEP);
    log(builder.toString());
  }

  private void debugDistances() {
    StringBuilder builder = new StringBuilder();
    builder.append("=====DISTANCES=======").append(SEP);
    factories.values().forEach(factory -> {
      factory.getDistancesToNeighbours().forEach((neigh, dist) -> {
        builder.append("From ").append(factory.getId())
          .append(" to ").append(neigh.getId())
          .append(" dist = ").append(dist).append(SEP);
      });
    });
    builder.append("=====DISTANCES=======").append(SEP);
    log(builder.toString());
  }

  private String factoryInfo(Factory factory) {
    StringBuilder builder = new StringBuilder();
    builder.append("ID ").append(factory.getId()).append("; ");
    builder.append(factory.getOwner()).append("; ");
    builder.append(factory.getTag()).append("; ");
    builder.append("U ").append(factory.getCount()).append("; ");
    builder.append("PR ").append(factory.getProduction()).append("; ");
    if (factory.getExplodeIn() < Constants.MAX_DISTANCE) {
      builder.append("EXPL ").append(factory.getExplodeIn()).append("; ");
    }
    if (factory.getRecoverIn() > 0) {
      builder.append("REC ").append(factory.getRecoverIn()).append("; ");
    }
    builder.append("ADD FOR CONTROL:").append(requiredCyborgsToHoldControl(factory)).append("; ");
    return builder.toString();
  }

  private Command coolMessage() {
    return new Message(EMOTICONS[round % EMOTICONS.length]);
  }

  private final String[] EMOTICONS = new String[] {
    "(╯°□°）╯︵ ┻━┻"
    ,"(ノಠ益ಠ)ノ彡┻━┻"
    ,"(╯°□°）╯︵ ┻━┻"
    ,"┬──┬ ¯\\_(ツ)"
    ,"┻━┻︵ヽ(`Д´)ﾉ︵ ┻━┻"
    ,"┻━┻︵ヽ(`Д´)ﾉ︵ ┻━┻"
    ,"┬─┬ノ( º _ ºノ) "
    ,"(ノಠ益ಠ)ノ"
    ,"(╯°□°）╯"
    ," ¯\\_(ツ)_/¯"
  };

  private final String SEP = "\n";
  private final String HSEP = "------------------\n";

  private int unitsRequiredToDefence(Factory factory) {
    int required = 0;
    Map<Integer, Integer> enemyWave = new HashMap<>();
    countIncomingWaves(enemyWave, enemyTroops, factory, 10);
    Map<Integer, Integer> myWave = new HashMap<>();
    countIncomingWaves(myWave, myTroops, factory, 10);

    Owner currentOwner = factory.getOwner();
    int currentUnits = factory.getCount();
    for (int turn = 0; turn < 10; turn++) {
      final int[] diffTurn = new int[] {myWave.get(turn) - enemyWave.get(turn)};
      int prodIncome = currentOwner == Owner.NEUTRAL ? 0 : factory.getProduction();
      prodIncome *= (currentOwner == Owner.ME ? 1 : -1);
      diffTurn[0] += prodIncome;

      int finalTurn = turn;
      factory.getDistancesToNeighbours().entrySet()
        .stream()
        .filter(entry -> entry.getValue() >= finalTurn
          && entry.getValue() <= 3
          && entry.getKey().getOwner() == Owner.ENEMY)
        .map(Map.Entry::getKey)
        .forEach(neigh -> diffTurn[0] -= neigh.getProduction());


      if (currentOwner == Owner.ME && diffTurn[0] >= 0) {
        currentUnits += diffTurn[0];
      } else if (currentOwner == Owner.ENEMY && diffTurn[0] <= 0){
        currentUnits += diffTurn[0];
      } else {
        int absDiff = Math.abs(diffTurn[0]);
        int absUnits = Math.abs(currentUnits);
        if (absUnits > absDiff) {
          //enemy, neutral -> current units < 0 else current units > 0
          currentUnits = currentUnits - (absDiff * (currentOwner == Owner.ME ? 1 : -1));
        } else {
          int newUnits = absDiff - absUnits;
          switch (currentOwner) {
            case NEUTRAL:
              if (diffTurn[0] > 0) {
                currentOwner = Owner.ME;
                currentUnits = newUnits;
              } else {
                currentOwner = Owner.ENEMY;
                currentUnits = -newUnits;
              }
              break;
            case ENEMY:
              currentOwner = Owner.ME;
              currentUnits = newUnits;
              break;
            case ME:
              currentOwner = Owner.ENEMY;
              currentUnits = -newUnits;
              break;
          }
        }
      }

      required += diffTurn[0];
    }
    return -required;
  }

}
