package ai;

import model.Constants;
import model.Link;
import model.Owner;
import model.commands.*;
import model.entities.Bomb;
import model.entities.Factory;
import model.entities.Troop;
import model.graph.Graph;

import java.util.*;
import java.util.stream.Collectors;

public class AI {
  final Scanner scanner;
  final int NODES;
  final int EDGES;
  final Graph graph;

  final Map<Integer, Factory> factories = new HashMap<>();
  final Map<Integer, Troop> troops = new HashMap<>();
  final Map<Integer, Bomb> bombs = new HashMap<>();
  int round = 0;
  final Set<Link> links = new HashSet<>();

  final List<Integer> bombsToRemove = new ArrayList<>();
  final List<Integer> troopsToRemove = new ArrayList<>();

  int bombsAvailable = Constants.INITIAL_BOMBS_COUNT;

  public AI(Scanner scanner) {
    this.scanner = scanner;
    this.graph = new Graph();
    NODES = scanner.nextInt(); // the number of factories
    EDGES = scanner.nextInt(); // the number of links between factories
//    graph.add(EDGES, scanner);
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
    bombsToRemove.forEach(bombs::remove);
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
            factory.setSendingBomb(false);
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
          }
          if (!bombs.containsKey(entityId)) {
            Bomb bomb = new Bomb(entityId, owner, arg2, arg3, arg4);
            bombs.put(entityId, bomb);
          } else {
            Bomb bomb = bombs.get(entityId);
            bomb.setOwner(owner);
            bomb.setFrom(arg2);
            bomb.setTo(arg3);
            bomb.setEta(arg4);
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
    updateWaves();
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

    List<Factory> myFactories = factories.values()
      .stream().filter(factory -> factory.getOwner() == Owner.ME)
      .collect(Collectors.toList());

    List<Factory> enemyFactories = factories.values()
      .stream()
      .filter(factory -> factory.getOwner() == Owner.ENEMY)
      .sorted(Comparator.comparingInt(Factory::getCount).reversed())
      .collect(Collectors.toList());

    calculateProductionCommands(myFactories, commands);
    calculateBombCommands(enemyFactories, commands);
    calculateDronesCommand(myFactories, commands);

    return commands;
  }

  private void calculateProductionCommands(List<Factory> myFactories, List<Command> commands) {
    if (round < Constants.ADVANCED_TECH_ROUND_THRESHOLD) {
      return;
    }
    for (Factory myFactory : myFactories) {
      if (myFactory.getProduction() >= Constants.MAX_PRODUCTION) {
        continue;
      }
      if (myFactory.getCount() >= Constants.PRODUCTION_INCREMENT_THRESHOLD) {
        commands.add(new Increment(myFactory.getId()));
        myFactory.setCount(myFactory.getCount() - Constants.PRODUCTION_INCREMENT_COST);
      }
    }
  }

  private void calculateBombCommands(List<Factory> enemyFactories, List<Command> commands) {
    if (round < Constants.ADVANCED_TECH_ROUND_THRESHOLD) {
      return;
    }
    for (Factory enemyFactory : enemyFactories) {
      if (bombsAvailable <= 0) {
        return;
      }
      if (enemyFactory.getCount() < Constants.ENEMY_BOMB_COUNT_THRESHOLD) {
        break;
      }
      Optional<Map.Entry<Factory, Integer>> myClosest = enemyFactory.getDistancesToNeighbours()
        .entrySet().stream().filter(entry -> entry.getKey().getOwner() == Owner.ME)
        .sorted(Map.Entry.comparingByValue())
        .findFirst();
      if (!myClosest.isPresent()) {
        break;
      }
      Factory my = myClosest.get().getKey();
      commands.add(new Boom(my.getId(), enemyFactory.getId()));
      bombsAvailable--;
      my.setSendingBomb(true);
    }
  }

  private void calculateDronesCommand(List<Factory> myFactories, List<Command> commands) {
    for (Factory factory : myFactories) {
      if (factory.isSendingBomb()) {
        continue;
      }
      Map<Factory, Integer> neigs = factory.getDistancesToNeighbours();
      Map<Factory, Integer> scores = new HashMap<>();
      int diff = factory.incomingDiff();
      if (diff > 0) {
        diff = 0;
      }
      int availableCount = (int) ((factory.getCount() + diff) * Constants.SEND_CYBORGS_PART) + 1;
      if (availableCount < Constants.MINIMUM_DEFENDERS_KEEP) {
        continue;
      }

      for (Map.Entry<Factory, Integer> neigh : neigs.entrySet()) {
        Factory target = neigh.getKey();
        if (target.getCount() > availableCount) {
          continue;
        }
        int distToTarget = neigh.getValue();
        double score = 0;
        score += factory.getProduction() * Constants.SCORE_MULTIPLIER_PRODUCTION;
        score += distToTarget * Constants.SCORE_MULTIPLIER_DISTANCE;
        score += target.getCount() * Constants.SCORE_MULTIPLIER_COUNT;
        double allyInfluence = incomingInfluence(target, target.getIncomingAllies());
        double enemyInfluence = incomingInfluence(target, target.getIncomingEnemies());
        switch (target.getOwner()) {
          case ME:
            score *= Constants.MY_FACTORY_SCORE_MULTIPLIER;
            if (enemyInfluence > allyInfluence) {
              score *= 2.0;
            }
            break;
          case ENEMY:
            score *= Constants.ENEMY_FACTORY_SCORE_MULTIPLIER;
            break;
          case NEUTRAL:
            break;
          default:
            throw new IllegalStateException("OH SHI");
        }
        scores.put(target, (int) score);
      }

      LinkedHashMap<Factory, Integer> sortedScores = scores.entrySet()
        .stream()
        .sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
        .collect(Collectors.toMap(
          Map.Entry::getKey,
          Map.Entry::getValue,
          (e1, e2) -> e1,
          LinkedHashMap::new
        ));

      for (Factory target : sortedScores.keySet()) {
        if (availableCount <= 0) {
          break;
        }
        if (availableCount <= target.getCount()) {
          continue;
        }

        int sendCount = Math.min(availableCount, target.getCount() + Constants.ADDITIONAL_SEIZE_CYBORGS - target.incomingDiff());
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
      influence += troop.getCount() * 10 + troop.getEta() * - 20;
    }
    influence *= factory.getProduction();
    return influence;
  }

  private Message coolMessage() {
    return new Message("HammerTime!");
  }
}
