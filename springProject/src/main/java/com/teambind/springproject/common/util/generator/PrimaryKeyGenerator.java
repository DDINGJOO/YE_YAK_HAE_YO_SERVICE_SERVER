package com.teambind.springproject.common.util.generator;

import org.springframework.stereotype.Component;

@Component
public interface PrimaryKeyGenerator {
	
	/**
	 * Generate unique Long ID
	 * 내부 시스템에서 사용하는 Long 타입 ID 생성
	 *
	 * @return 64-bit Long ID
	 */
	Long generateLongKey();
	
	/**
	 * Generate unique String ID
	 * 클라이언트 통신용 String 타입 ID 생성 (하위 호환성 유지)
	 *
	 * @return String representation of Long ID
	 * @deprecated Use generateLongKey() for internal use
	 */
	@Deprecated(since = "1.1", forRemoval = false)
	String generateKey();
}
