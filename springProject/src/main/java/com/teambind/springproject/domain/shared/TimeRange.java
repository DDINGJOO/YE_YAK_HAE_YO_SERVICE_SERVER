package com.teambind.springproject.domain.shared;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * 시간 범위를 표현하는 Value Object.
 * 시작 시간과 종료 시간을 포함합니다.
 */
public class TimeRange {
	
	private final LocalTime startTime;
	private final LocalTime endTime;
	
	private TimeRange(final LocalTime startTime, final LocalTime endTime) {
		validateTimeRange(startTime, endTime);
		this.startTime = startTime;
		this.endTime = endTime;
	}
	
	public static TimeRange of(final LocalTime startTime, final LocalTime endTime) {
		return new TimeRange(startTime, endTime);
	}
	
	private void validateTimeRange(final LocalTime startTime, final LocalTime endTime) {
		if (startTime == null) {
			throw new IllegalArgumentException("Start time cannot be null");
		}
		if (endTime == null) {
			throw new IllegalArgumentException("End time cannot be null");
		}
		if (startTime.isAfter(endTime) || startTime.equals(endTime)) {
			throw new IllegalArgumentException(
					"Start time must be before end time: " + startTime + " - " + endTime);
		}
	}
	
	public int calculateSlots(final TimeSlot timeSlot) {
		if (timeSlot == null) {
			throw new IllegalArgumentException("TimeSlot cannot be null");
		}
		final long minutes = ChronoUnit.MINUTES.between(startTime, endTime);
		final long slots = minutes / timeSlot.getMinutes();
		return (int) slots;
	}
	
	public long getDurationInMinutes() {
		return ChronoUnit.MINUTES.between(startTime, endTime);
	}
	
	public boolean contains(final LocalTime time) {
		if (time == null) {
			throw new IllegalArgumentException("Time cannot be null");
		}
		return !time.isBefore(startTime) && time.isBefore(endTime);
	}
	
	public boolean overlaps(final TimeRange other) {
		if (other == null) {
			throw new IllegalArgumentException("Other time range cannot be null");
		}
		return this.startTime.isBefore(other.endTime) && other.startTime.isBefore(this.endTime);
	}
	
	public LocalTime getStartTime() {
		return startTime;
	}
	
	public LocalTime getEndTime() {
		return endTime;
	}
	
	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final TimeRange timeRange = (TimeRange) o;
		return Objects.equals(startTime, timeRange.startTime)
				&& Objects.equals(endTime, timeRange.endTime);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(startTime, endTime);
	}
	
	@Override
	public String toString() {
		return startTime + " - " + endTime;
	}
}
