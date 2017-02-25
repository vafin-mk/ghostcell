package ai;

import model.Link;
import model.Owner;
import model.commands.Command;
import model.commands.Move;
import model.commands.Wait;
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
  boolean startRound = true;
  final Set<Link> links = new HashSet<>();

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
    links.forEach(System.err::println);
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
    Command command = bestCommand();
    System.out.println(command);
  }

  private Command bestCommand() {
    List<Factory> myFactories = factories.values()
      .stream().filter(factory -> factory.getOwner() == Owner.ME).collect(Collectors.toList());
    Optional<Factory> myMostPowerful = myFactories.stream().sorted(Comparator.comparingInt(Factory::getCount).reversed()).findFirst();
    if (myMostPowerful.isPresent()) {
      Factory factory = myMostPowerful.get();
      Map<Factory, Integer> neigs = factory.getDistancesToNeighbours();
      Factory bestTarget = null;
      int bestScore = Integer.MIN_VALUE;
      for (Map.Entry<Factory, Integer> neigh : neigs.entrySet()) {
        Factory target = neigh.getKey();
        if (target.getCount() > factory.getCount() / 2) {
          continue;
        }
        int distToTarget = neigh.getValue();
        int score = factory.getProduction() * 5 + (10 - distToTarget) + (10 - target.getCount());
        if (score > bestScore) {
          bestScore = score;
          bestTarget = target;
        }
      }
      if (bestTarget != null) {
        return new Move(factory.getId(), bestTarget.getId(), Math.min(factory.getCount() / 2, bestTarget.getCount() + 1));
      }
    }

    return new Wait();
  }
}
