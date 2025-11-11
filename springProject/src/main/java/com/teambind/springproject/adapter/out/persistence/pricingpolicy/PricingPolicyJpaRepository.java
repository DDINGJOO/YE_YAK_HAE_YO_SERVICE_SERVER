package com.teambind.springproject.adapter.out.persistence.pricingpolicy;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * PricingPolicyEntity를 위한 Spring Data JPA Repository.
 */
public interface PricingPolicyJpaRepository extends
		JpaRepository<PricingPolicyEntity, RoomIdEmbeddable> {
	
}
