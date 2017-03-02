package model;

public class Constants {

  //MODEL CONSTANTS
  public static final int INITIAL_BOMBS_COUNT = 2;
  public static final int PRODUCTION_INCREMENT_COST = 10;
  public static final int MAX_PRODUCTION = 3;
  public static final int MAX_DISTANCE = 20;
  public static final int BOMB_EFFECT_DURATION = 5;

  //AI CONSTANTS
  public static final double SENDING_CREW_PERCENT = 1.0;//0.00-1.00
  public static final int MINIMUM_DEFENDERS_KEEP = 0;
  public static final int ENEMY_BOMB_COUNT_THRESHOLD = 15;
  public static final int ENEMY_BOMB_PRODUCTION_THRESHOLD = 1;
  public static final int PRODUCTION_INCREMENT_THRESHOLD = 15;
  public static final int ADDITIONAL_SEIZE_CYBORGS = 1;
  public static final double MY_FACTORY_SCORE_MULTIPLIER = 0.5;
  public static final double ENEMY_FACTORY_SCORE_MULTIPLIER = 0.8;
  public static final double SCORE_MULTIPLIER_PRODUCTION = 100;
  public static final double SCORE_MULTIPLIER_DISTANCE = -10;
  public static final double SCORE_MULTIPLIER_COUNT = -1;
  public static final double TROOP_INFLUENCE_MODIFIER_COUNT = 20;
  public static final double TROOP_INFLUENCE_MODIFIER_DIST = -10;
  public static final int EARLY_ROUND_MAX = 10;
  public static final int LATE_ROUND_MIN = 45;

}
