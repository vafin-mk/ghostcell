package ai;

import model.Constants;
import model.GameState;
import model.Link;
import model.Owner;
import model.commands.*;
import model.entities.Bomb;
import model.entities.Factory;
import model.entities.Troop;

import java.util.*;
import java.util.stream.Collectors;

public class AI {
  final Scanner scanner;
  final int NODES;
  final int EDGES;

  final Map<Integer, Factory> factories = new HashMap<>();
  final Map<Integer, Troop> troops = new HashMap<>();
  final Map<Integer, Bomb> bombs = new HashMap<>();
  int round = 0;
  final Set<Link> links = new HashSet<>();

  final List<Integer> bombsToRemove = new ArrayList<>();
  final List<Integer> troopsToRemove = new ArrayList<>();

  List<Factory> myFactories;
  List<Factory> enemyFactories;
  List<Factory> neutralFactories;
  List<Troop> myTroops;
  List<Troop> enemyTroops;

  int bombsAvailable = Constants.INITIAL_BOMBS_COUNT;

  int enemyPower, myPower, neutralPower, enemyUnits, myUnits, enemyProduction, myProduction, neutralProduction;
  GameState currentState = GameState.EARLY;
  Factory castle; //my factory which dist to all enemies is lowest

  public AI(Scanner scanner) {
    this.scanner = scanner;
    NODES = scanner.nextInt(); // the number of factories
    EDGES = scanner.nextInt(); // the number of links between factories
    for (int i = 0; i < EDGES; i++) {
      int factory1 = scanner.nextInt();
      int factory2 = scanner.nextInt();
      int distance = scanner.nextInt();
      links.add(new Link(factory1, factory2, distance));
    }
  }

  public void start() {
    while(true) {
      inputWorld();
      outputAnswer();
      round++;
    }
  }

  private void inputWorld() {
    troopsToRemove.forEach(troops::remove);
    troopsToRemove.clear();
    bombsToRemove.forEach(bombId -> {
      Bomb bomb = bombs.remove(bombId);
    });
    bombsToRemove.clear();

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
            troopsToRemove.add(entityId);
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
            bombsToRemove.add(entityId);
            if (owner == Owner.ENEMY) {
              myFactories.forEach(factory -> factory.setExplodeIn(Integer.MAX_VALUE));
            }
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
    if (round == 0) {
      links.forEach(link -> {
        Factory from = factories.get(link.getFrom());
        Factory to = factories.get(link.getTo());
        from.addNeighbour(to, link.getDist());
        to.addNeighbour(from, link.getDist());
      });
    }
    factories.values().forEach(Factory::recalculateDists);
    myFactories = factories.values().stream().filter(factory -> factory.getOwner() == Owner.ME).collect(Collectors.toList());
    enemyFactories = factories.values().stream().filter(factory -> factory.getOwner() == Owner.ENEMY).collect(Collectors.toList());
    neutralFactories = factories.values().stream().filter(factory -> factory.getOwner() == Owner.NEUTRAL).collect(Collectors.toList());
    myTroops = troops.values().stream().filter(troop -> troop.getOwner() == Owner.ME).collect(Collectors.toList());
    enemyTroops = troops.values().stream().filter(troop -> troop.getOwner() == Owner.ENEMY).collect(Collectors.toList());
    recalculatePowers();
    updateWaves();
    recalculateGameState();
    findCastle();
    log("%s", currentState);
    log("castle : %s", castle);
  }

  private void recalculatePowers() {
    enemyPower=myPower=neutralPower=enemyUnits=myUnits=enemyProduction=myProduction=neutralProduction = 0;
    for (Factory factory : factories.values()) {
      switch(factory.getOwner()) {
        case ME:
          myPower += factory.getCount();
          myProduction += factory.getProduction();
          break;
        case ENEMY:
          enemyPower += factory.getCount();
          enemyProduction += factory.getProduction();
          break;
        case NEUTRAL:
          neutralPower += factory.getCount();
          neutralProduction += factory.getProduction();
          break;
      }
    }
    for (Troop troop : troops.values()) {
      switch(troop.getOwner()) {
        case ME:
          myUnits += troop.getCount();
          break;
        case ENEMY:
          enemyUnits += troop.getCount();
          break;
      }
    }
  }

  private void recalculateGameState() {
    if (round < 5) {
      currentState = GameState.EARLY;
      return;
    }
    int neutrals = neutralFactories.size();
    int my = myFactories.size();
    int enemies = enemyFactories.size();
    if (my + enemies < neutrals) {
      currentState = GameState.EARLY;
    } else if ( neutrals <= 1 || neutralProduction == 0) {
      currentState = GameState.LATE;
    } else {
      currentState = GameState.MID;
    }
  }

  private void findCastle() {
    if (myFactories.isEmpty()) return;
    castle = myFactories.get(0);
    int enemyDistSum = Integer.MAX_VALUE;
    for (Factory myFactory : myFactories) {
      int dist = myFactory.getSumDistToEnemies();
      if (myFactory.getExplodeIn() < Constants.MAX_DISTANCE / 5) dist = Integer.MAX_VALUE;
      if (dist < enemyDistSum) {
        castle = myFactory;
        enemyDistSum = dist;
      }
    }
  }

  private void updateWaves() {
    troops.values().forEach(troop -> {
      Factory target = factories.get(troop.getTo());
      if (troop.getOwner() == Owner.ME) {
        target.addIncomingAlly(troop);
      } else {
        target.addIncomingEnemy(troop);
      }
    });
  }

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

  private void calculateProductionCommands(List<Command> commands) {
    for (Factory myFactory : myFactories) {
      if (myFactory.getProduction() >= Constants.MAX_PRODUCTION) {
        continue;
      }
      if (myFactory.getCount() >= Constants.PRODUCTION_INCREMENT_THRESHOLD
        && myPower >= Constants.PRODUCTION_INCREMENT_THRESHOLD * 2
        && myFactory.getCount() + myFactory.incomingDiff() > Constants.PRODUCTION_INCREMENT_COST
        && myFactory.getExplodeIn() > Constants.MAX_DISTANCE) {
        commands.add(new Increment(myFactory.getId()));
        myFactory.setCount(myFactory.getCount() - Constants.PRODUCTION_INCREMENT_COST);
      }
    }
  }

  private void calculateBombCommands(List<Command> commands) {
    /*special condition, bombs on first round*/
    if (round == 0) {
      Factory my = myFactories.get(0);
      Factory enemy = enemyFactories.get(0);
      if (enemy.getProduction() > 0) {
        commands.add(new Boom(my.getId(), enemy.getId()));
        bombsAvailable--;
        enemy.setExplodeIn(my.distanceTo(enemy));
      }
      //if enemy has valuable neutral factories around
      List<Factory> enemyCloseFactories = neutralFactories.stream()
        .filter(factory -> {
          if (factory.distanceTo(my) <= factory.distanceTo(enemy)) {
            return false;
          }
          if (factory.getProduction() == 3
            && factory.distanceTo(enemy) < 5
            && factory.getCount() <= enemy.getCount() - 3) {
            return true;
          }
          if (factory.getProduction() == 2
            && factory.distanceTo(enemy) < 3
            && factory.getCount() <= enemy.getCount() - 6) {
            return true;
          }
          return false;
        })
        .sorted((f1, f2) -> {
          if (f1.getProduction() == f1.getProduction()) {
            if (f1.distanceTo(enemy) == f2.distanceTo(enemy)) {
              return Integer.compare(f1.getCount(), f2.getCount());
            }
            return Integer.compare(f1.distanceTo(enemy), f2.distanceTo(enemy));
          }
          return Integer.compare(f2.getProduction(), f1.getProduction());
        })
        .collect(Collectors.toList());

      for (Factory enemyCloseFactory : enemyCloseFactories) {
        if (bombsAvailable <= 0) {
          return;
        }
        commands.add(new Boom(my.getId(), enemyCloseFactory.getId()));
        bombsAvailable--;
        enemyCloseFactory.setExplodeIn(my.distanceTo(enemyCloseFactory));
      }
    }

    List<Factory> valuableEnemies = enemyFactories.stream()
      .filter(factory -> {
      if (factory.getProduction() <= Constants.ENEMY_BOMB_PRODUCTION_THRESHOLD) return false;
      if (factory.getIncomingAllies().size() > Constants.ENEMY_BOMB_COUNT_THRESHOLD / 3) return false;
      if (factory.getExplodeIn() <= Constants.MAX_DISTANCE) return false;
      if (factory.getCount() < Constants.ENEMY_BOMB_COUNT_THRESHOLD && currentState == GameState.EARLY) return false;
      return true;
    })
      .sorted((f1, f2) -> {
        if (f1.getProduction() == f1.getProduction()) {
          return Integer.compare(f2.getCount(), f1.getCount());
        }
        return Integer.compare(f2.getProduction(), f1.getProduction());
      })
      .collect(Collectors.toList());
    for (Factory enemyFactory : valuableEnemies) {
      if (bombsAvailable <= 0) {
        return;
      }
      Optional<Map.Entry<Factory, Integer>> myClosest = enemyFactory.getDistancesToNeighbours()
        .entrySet().stream()
        .filter(entry -> entry.getKey().getOwner() == Owner.ME && entry.getKey().distanceTo(enemyFactory) <= Constants.MAX_DISTANCE / 2)
        .sorted(Map.Entry.comparingByValue())
        .findFirst();
      if (!myClosest.isPresent()) {
        continue;
      }
      Factory my = myClosest.get().getKey();
      commands.add(new Boom(my.getId(), enemyFactory.getId()));
      bombsAvailable--;
      enemyFactory.setExplodeIn(my.distanceTo(enemyFactory));
    }
  }

  private void calculateDronesCommand(List<Command> commands) {
    Map<Factory, Integer> freeDrones = new HashMap<>();
    int freeSum = fillFreeDrones(freeDrones);
    List<Factory> validTargets = factories.values().stream()
      .filter(factory -> {
        if (currentState == GameState.EARLY && factory.getSumDistToAllies() > factory.getSumDistToEnemies() + 1) {
//          log("filtering out -> %s [%s/%s]", factory, factory.getSumDistToAllies(), factory.getSumDistToEnemies());
          return false;
        }
        if (factory.getOwner() != Owner.ME
          && factory.getExplodeIn() < Constants.MAX_DISTANCE
          && factory.getExplodeIn() >= Constants.BOMB_EFFECT_DURATION) {
          return false;
        }
        if (factory.getOwner() == Owner.ME) {
          //need defence or castle
          return (factory.incomingDiff() + factory.getCount() / 2) < 0 || factory.getProduction() < Constants.MAX_PRODUCTION;
        }
        if (factory.getOwner() == Owner.NEUTRAL) {
          return factory.getProduction() > 0
            || currentState == GameState.LATE
            || (freeSum > Constants.PRODUCTION_INCREMENT_COST * 3 && currentState != GameState.EARLY);
        }
        //enemies
        return factory.getProduction() > 0 || freeSum > Constants.PRODUCTION_INCREMENT_COST * 2;
      })
      .collect(Collectors.toList());

    List<Factory> targets = applyScores(validTargets);
//    log("%s", targets);
    assignDrones(targets, freeDrones, freeSum, commands);
  }

  private int fillFreeDrones(Map<Factory, Integer> freeDrones) {
    int sum = 0;
    for (Factory myFactory : myFactories) {
      if (myFactory.getExplodeIn() == 2) {
        freeDrones.put(myFactory, myFactory.getCount());
        sum += myFactory.getCount();
        continue;
      }

      int freeCount = (int) (myFactory.getCount() * Constants.SENDING_CREW_PERCENT);
      if (myFactory.incomingDiff() < 0) {
        freeCount += myFactory.incomingDiff();
      }
      if (freeCount <= Constants.MINIMUM_DEFENDERS_KEEP) {
        continue;
      }
      freeDrones.put(myFactory, freeCount);
      sum += freeCount;
    }
    return sum;
  }

  private List<Factory> applyScores(List<Factory> validTargets) {
    Map<Factory, Integer> targets = new LinkedHashMap<>();
    for (Factory validTarget : validTargets) {
      double score = 0;
      double prodScore = validTarget.getProduction() * Constants.SCORE_MULTIPLIER_PRODUCTION;
      if (validTarget.getOwner() == Owner.ME) {
        targets.put(validTarget, (int) (prodScore + -validTarget.incomingDiff()));
        continue;
      }

      int distsToAlly = validTarget.getSumDistToAllies();
      double medianDist = (double) distsToAlly / myFactories.size();
      double distScore = medianDist * Constants.SCORE_MULTIPLIER_DISTANCE;
      double countScore = validTarget.getCount() * Constants.SCORE_MULTIPLIER_COUNT;
      if (validTarget.getOwner() == Owner.NEUTRAL) {
        if (currentState == GameState.EARLY && medianDist < Constants.MAX_DISTANCE / 4) {
          prodScore *= 2;
        }
        if (validTarget.getSumDistToEnemies() < distsToAlly / 2) {
          distScore *= 2;
        }
        score = prodScore + distScore + countScore;
        targets.put(validTarget, (int) score);
        continue;
      }
      //ENEMY
      if (currentState != GameState.EARLY) {
        distScore *= 2;
      }
      score = prodScore + distScore + countScore;
      targets.put(validTarget, (int) score);
    }

    targets.entrySet().removeIf(entry -> entry.getValue() < - 100);
//    log("%s", targets);
    return targets.entrySet()
      .stream()
      .filter(entry -> entry.getValue() > Integer.MIN_VALUE)
      .sorted(Map.Entry.<Factory, Integer>comparingByValue().reversed())
      .map(Map.Entry::getKey)
      .collect(Collectors.toList());
  }

  private void assignDrones(List<Factory> targets, Map<Factory, Integer> drones, int dronesSum, List<Command> commands) {
    for (Factory target : targets) {
      if (dronesSum <= 0) return;
      drones.entrySet().removeIf(entry -> entry.getValue() == 0);
      int requiredDrones = 0;
      if (target.getOwner() == Owner.ME) {
        requiredDrones = Constants.ADDITIONAL_SEIZE_CYBORGS + -target.incomingDiff() - target.getCount();
      } else {
        requiredDrones = Constants.ADDITIONAL_SEIZE_CYBORGS + target.getCount() - target.incomingDiff();
        if (target.getOwner() == Owner.ENEMY) {
          requiredDrones += target.getProduction() * (target.getSumDistToAllies() / myFactories.size());
        }
      }
      if (target.getOwner() == Owner.NEUTRAL && target.getProduction() == 0) {
        requiredDrones = Math.max(requiredDrones, Constants.PRODUCTION_INCREMENT_COST);
      }
      if (requiredDrones <= 0) {
        continue;
      }
      if (dronesSum < requiredDrones) {
        continue;
      }
      List<Factory> closestAllies = drones.keySet().stream()
        .sorted((f1, f2) -> Integer.compare(f1.distanceTo(target), f2.distanceTo(target)))
        .collect(Collectors.toList());

      int assignedDrones = 0;
      for (Factory closestAlly : closestAllies) {
        if(closestAlly.equals(target)) continue;
        int remainingAssigns = requiredDrones - assignedDrones;
        if (remainingAssigns == 0) {
          break;
        } else if (remainingAssigns < 0) {
          throw new IllegalStateException("Fail bro!");
        }
        int freeDrones = drones.get(closestAlly);
        if (target.getOwner() == Owner.NEUTRAL && freeDrones < requiredDrones) {
          //dont attack neutrals on midstage if you cant take it in one strike(do not lose drones easily)
          continue;
        }
        int assign = Math.min(Math.min(freeDrones, requiredDrones), remainingAssigns);
        commands.add(new Move(closestAlly.getId(), target.getId(), assign));
        drones.put(closestAlly, freeDrones - assign);
        assignedDrones += assign;
      }
      dronesSum -= assignedDrones;
    }
    if (dronesSum > 0) {
      drones.entrySet().forEach(entry -> {
        if (!entry.getKey().equals(castle)) {
          commands.add(new Move(entry.getKey().getId(), castle.getId(), entry.getValue()));
        }
      });
    }
  }

  private void calculateDronesCommandOld(List<Command> commands) {
    for (Factory factory : myFactories) {
      int diff = factory.incomingDiff();
      if (diff < 0) {
        //todo send everyone when factory lost is inevitable
        continue;
      }
      Map<Factory, Integer> neigs = factory.getDistancesToNeighbours();
      Map<Factory, Integer> scores = new HashMap<>();

      int availableCount = (int) ((factory.getCount()) * Constants.SENDING_CREW_PERCENT) + 1;
      if (availableCount < Constants.MINIMUM_DEFENDERS_KEEP) {
        continue;
      }

      for (Map.Entry<Factory, Integer> neigh : neigs.entrySet()) {
        Factory target = neigh.getKey();
        if (target.getCount() > (availableCount + target.getIncomingAllyCount())) {
          continue;
        }
        int distToTarget = neigh.getValue();
        double score = 0;
        double prodScore = target.getProduction() * Constants.SCORE_MULTIPLIER_PRODUCTION;
        double distScore = distToTarget * Constants.SCORE_MULTIPLIER_DISTANCE;
        double powerScore = target.getCount() * Constants.SCORE_MULTIPLIER_COUNT;
//        log("%s (%s/%s/%s)", target, prodScore, distScore, powerScore);
        double allyInfluence = incomingInfluence(target, target.getIncomingAllies());
        double enemyInfluence = incomingInfluence(target, target.getIncomingEnemies());

        if (target.getOwner() == Owner.ME) {
          if (enemyInfluence > allyInfluence) {
            prodScore *= 2;
          } else if (currentState == GameState.LATE && target.getId() == castle.getId()) {
            prodScore = 1.5 * Constants.SCORE_MULTIPLIER_PRODUCTION;
          } else {
            prodScore = 0;
          }
          score = prodScore + distScore;
        } else {
          if (currentState == GameState.EARLY) {
            if (target.getOwner() == Owner.NEUTRAL && distToTarget < 10) {
              prodScore *= 2;
              distScore /= 2;
              powerScore /= 2;
            }
            score = prodScore + distScore + powerScore;
          } else if (currentState == GameState.MID) {
            if (target.getOwner() == Owner.ENEMY) {
              prodScore /= 2;
              distScore *= 2;
              powerScore *= 2;
            }
            score = prodScore + distScore + powerScore;
          } else { //LATE
            distScore *= 2;
            score = prodScore + distScore + powerScore;
          }
        }
        if (target.getExplodeIn() <= Constants.MAX_DISTANCE && target.getExplodeIn() >= factory.distanceTo(target)) {
          score = Integer.MIN_VALUE;
        }
        scores.put(target, (int) score);
      }

      LinkedHashMap<Factory, Integer> sortedScores = scores.entrySet()
        .stream()
        .filter(entry -> entry.getValue() > Integer.MIN_VALUE)
        .sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
        .collect(Collectors.toMap(
          Map.Entry::getKey,
          Map.Entry::getValue,
          (e1, e2) -> e1,
          LinkedHashMap::new
        ));

//      log("%s", sortedScores);
      for (Factory target : sortedScores.keySet()) {
        if (availableCount <= 0) {
          break;
        }
        if (availableCount <= target.getCount()) {
          continue;
        }

        int sendCount = 0;
        if (target.getOwner() == Owner.ME) {
          sendCount = -target.incomingDiff();
        } else {
          sendCount = target.getCount() + Constants.ADDITIONAL_SEIZE_CYBORGS - target.incomingDiff();
        }
        if (target.getOwner() == Owner.ENEMY) {
          sendCount += target.getProduction() * factory.distanceTo(target);
        }

        sendCount = Math.min(availableCount, sendCount);
        if (target.getProduction() == 0) {
          if (sendCount + target.incomingDiff() < Constants.PRODUCTION_INCREMENT_COST) {
            sendCount = 0;
          }
        }
        if (sendCount <= 0) {
          continue;
        }
        commands.add(new Move(factory.getId(), target.getId(), sendCount));
        availableCount -= sendCount;
      }
    }
  }

  private double incomingInfluence(Factory factory, List<Troop> incomingTroops) {
    double influence = 0.0;
    for (Troop troop : incomingTroops) {
      influence += troop.getCount() * 10 + troop.getEta() * -20;
    }
    influence *= factory.getProduction();
    return influence;
  }

  private Message coolMessage() {
    int powerDiff = (myPower + myUnits) - (enemyPower + enemyUnits);
    if (powerDiff > 10) return new Message("Veni, vidi, vici!");
    else if (powerDiff < -10) return new Message("(╯°□°）╯︵ ┻━┻");
    return new Message("It's a rocket science, baby!");
  }

  private void log(String format, Object...args) {
    System.err.println(String.format(format, args));
  }
}
