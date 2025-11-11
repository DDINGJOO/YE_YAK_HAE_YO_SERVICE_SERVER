package com.teambind.springproject.domain.shared;

/**
 * 시간 단위를 표현하는 Enum.
 * 룸 서비스의 TimeSlot과 동일한 값을 사용합니다.
 */
public enum TimeSlot {
	HOUR(60),
	HALFHOUR(30);
	
	private final int minutes;
	
	TimeSlot(final int minutes) {
		this.minutes = minutes;
	}
	
	public int getMinutes() {
		return minutes;
	}
	
	public int getHours() {
		return minutes / 60;
	}
	
	public boolean isHour() {
		return this == HOUR;
	}
	
	public boolean isHalfHour() {
		return this == HALFHOUR;
	}
}
