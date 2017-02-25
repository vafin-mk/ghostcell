package model;

public enum Owner {
  ME(1), NEUTRAL(0), ENEMY(-1);

  final int value;
  Owner(int value) {
    this.value = value;
  }

  public int value() {return value;}

  public static Owner fromVal(int val) {
    if (val == 1) return ME;
    if (val == -1) return ENEMY;
    return NEUTRAL;
  }
}
