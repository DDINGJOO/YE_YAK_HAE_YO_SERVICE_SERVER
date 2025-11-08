package com.teambind.springproject.adapter.out.persistence.pricingpolicy;

import com.teambind.springproject.application.port.out.PricingPolicyRepository;
import com.teambind.springproject.domain.pricingpolicy.PricingPolicy;
import com.teambind.springproject.domain.shared.RoomId;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * PricingPolicyRepository Port의 JPA Adapter 구현.
 * Hexagonal Architecture의 Adapter 계층입니다.
 */
@Repository
public class PricingPolicyRepositoryAdapter implements PricingPolicyRepository {

  private final PricingPolicyJpaRepository jpaRepository;

  public PricingPolicyRepositoryAdapter(final PricingPolicyJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  public Optional<PricingPolicy> findById(final RoomId roomId) {
    final RoomIdEmbeddable id = new RoomIdEmbeddable(roomId.getValue());
    return jpaRepository.findById(id)
        .map(PricingPolicyEntity::toDomain);
  }

  @Override
  public PricingPolicy save(final PricingPolicy policy) {
    final PricingPolicyEntity entity = PricingPolicyEntity.fromDomain(policy);
    final PricingPolicyEntity savedEntity = jpaRepository.save(entity);
    return savedEntity.toDomain();
  }

  @Override
  public void deleteById(final RoomId roomId) {
    final RoomIdEmbeddable id = new RoomIdEmbeddable(roomId.getValue());
    jpaRepository.deleteById(id);
  }

  @Override
  public boolean existsById(final RoomId roomId) {
    final RoomIdEmbeddable id = new RoomIdEmbeddable(roomId.getValue());
    return jpaRepository.existsById(id);
  }
}
