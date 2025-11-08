package com.teambind.springproject.domain.shared;

/**
 * 요일을 표현하는 Enum.
 * java.time.DayOfWeek와 호환되도록 설계되었습니다.
 */
public enum DayOfWeek {
  MONDAY("월요일", 1),
  TUESDAY("화요일", 2),
  WEDNESDAY("수요일", 3),
  THURSDAY("목요일", 4),
  FRIDAY("금요일", 5),
  SATURDAY("토요일", 6),
  SUNDAY("일요일", 7);

  private final String koreanName;
  private final int value;

  DayOfWeek(final String koreanName, final int value) {
    this.koreanName = koreanName;
    this.value = value;
  }

  public static DayOfWeek from(final java.time.DayOfWeek javaDayOfWeek) {
    if (javaDayOfWeek == null) {
      throw new IllegalArgumentException("Java DayOfWeek cannot be null");
    }
    return switch (javaDayOfWeek) {
      case MONDAY -> MONDAY;
      case TUESDAY -> TUESDAY;
      case WEDNESDAY -> WEDNESDAY;
      case THURSDAY -> THURSDAY;
      case FRIDAY -> FRIDAY;
      case SATURDAY -> SATURDAY;
      case SUNDAY -> SUNDAY;
    };
  }

  public java.time.DayOfWeek toJavaDayOfWeek() {
    return switch (this) {
      case MONDAY -> java.time.DayOfWeek.MONDAY;
      case TUESDAY -> java.time.DayOfWeek.TUESDAY;
      case WEDNESDAY -> java.time.DayOfWeek.WEDNESDAY;
      case THURSDAY -> java.time.DayOfWeek.THURSDAY;
      case FRIDAY -> java.time.DayOfWeek.FRIDAY;
      case SATURDAY -> java.time.DayOfWeek.SATURDAY;
      case SUNDAY -> java.time.DayOfWeek.SUNDAY;
    };
  }

  public boolean isWeekend() {
    return this == SATURDAY || this == SUNDAY;
  }

  public boolean isWeekday() {
    return !isWeekend();
  }

  public String getKoreanName() {
    return koreanName;
  }

  public int getValue() {
    return value;
  }
}
