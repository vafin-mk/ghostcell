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
  boolean startRound = true;
  final Set<Link> links = new HashSet<>();

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
    }
  }

  private void inputWorld() {
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
          }
          break;
        case "TROOP":
          if (!troops.containsKey(entityId)) {
            Troop troop = new Troop(entityId, owner, arg2, arg3, arg4, arg5);
            troops.put(entityId, troop);
          } else {
            Troop troop = troops.get(entityId);
            troop.setOwner(owner);
            troop.setFactoryFrom(arg2);
            troop.setFactoryTo(arg3);
            troop.setCount(arg4);
            troop.setRemainingTurns(arg5);
          }
          break;
        case "BOMB":
          if (!bombs.containsKey(entityId)) {
            Bomb bomb = new Bomb(entityId, owner, arg2, arg3, arg4);
            bombs.put(entityId, bomb);
          } else {
            Bomb bomb = bombs.get(entityId);
            bomb.setOwner(owner);
            bomb.setFrom(arg2);
            bomb.setTo(arg3);
            bomb.setEta(arg4);
            if (bomb.getEta() == 1) {
              bombs.remove(entityId);
            }
          }
          break;
        default:
          throw new IllegalStateException("Fucks!");
      }
    }
    if (startRound) {
      startRound = false;
      links.forEach(link -> {
        Factory from = factories.get(link.getFrom());
        Factory to = factories.get(link.getTo());
        from.addNeighbour(to, link.getDist());
        to.addNeighbour(from, link.getDist());
      });
    }
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
    }
  }

  private void calculateDronesCommand(List<Factory> myFactories, List<Command> commands) {
    for (Factory factory : myFactories) {
      Map<Factory, Integer> neigs = factory.getDistancesToNeighbours();
      Map<Factory, Integer> scores = new HashMap<>();
      int availableCount = (int) (factory.getCount() * Constants.SEND_CYBORGS_PART) + 1;
      if (availableCount < Constants.MINIMUM_DEFENDERS_KEEP) {
        continue;
      }

      for (Map.Entry<Factory, Integer> neigh : neigs.entrySet()) {
        Factory target = neigh.getKey();
        if (target.getCount() > availableCount) {
          continue;
        }
        int distToTarget = neigh.getValue();
        int score = (int)(factory.getProduction() * Constants.SCORE_MULTIPLIES_PRODUCTION
          - distToTarget * Constants.SCORE_MULTIPLIES_DISTANCE
          - target.getCount() * Constants.SCORE_MULTIPLIES_COUNT);
        if (target.getOwner() == Owner.ME) {
          score *= Constants.MY_FACTORY_SCORE_MULTIPLIER;
        }
        if (target.getOwner() == Owner.ENEMY) {
          score *= Constants.ENEMY_FACTORY_SCORE_MULTIPLIER;
        }
        scores.put(target, score);
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

      System.err.println(sortedScores);

      for (Factory target : sortedScores.keySet()) {
        if (availableCount <= 0) {
          break;
        }
        if (availableCount <= target.getCount()) {
          continue;
        }

        int sendCount = Math.min(availableCount, target.getCount() + Constants.ADDITIONAL_SEIZE_CYBORGS);
        commands.add(new Move(factory.getId(), target.getId(), sendCount));
        availableCount -= sendCount;
      }
    }
  }

  private Message coolMessage() {
    return new Message("MWAHAHA!!!!");
  }
}
