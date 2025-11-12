package com.teambind.springproject.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 예약 관련 설정을 관리하는 Configuration 클래스.
 */
@Configuration
@ConfigurationProperties(prefix = "reservation")
public class ReservationConfiguration {
	
	private Pending pending = new Pending();
	
	public Pending getPending() {
		return pending;
	}
	
	public void setPending(final Pending pending) {
		this.pending = pending;
	}
	
	public static class Pending {
		private long timeoutMinutes = 20;
		
		public long getTimeoutMinutes() {
			return timeoutMinutes;
		}
		
		public void setTimeoutMinutes(final long timeoutMinutes) {
			this.timeoutMinutes = timeoutMinutes;
		}
	}
}
