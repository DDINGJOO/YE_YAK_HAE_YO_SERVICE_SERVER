package com.teambind.springproject.adapter.out.persistence.product;

import static org.assertj.core.api.Assertions.assertThat;

import com.teambind.springproject.domain.shared.ProductId;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(RoomAllowedProductRepositoryAdapter.class)
@DisplayName("RoomAllowedProductRepository 통합 테스트")
class RoomAllowedProductRepositoryAdapterTest {

  @Autowired
  private RoomAllowedProductRepositoryAdapter repository;

  @Nested
  @DisplayName("findAllowedProductIdsByRoomId 테스트")
  class FindAllowedProductIdsByRoomIdTests {

    @Test
    @DisplayName("매핑이 없으면 빈 리스트 반환")
    void returnEmptyListWhenNoMappingExists() {
      // given
      final Long roomId = 999L;

      // when
      final List<ProductId> allowedProductIds = repository.findAllowedProductIdsByRoomId(roomId);

      // then
      assertThat(allowedProductIds).isEmpty();
    }

    @Test
    @DisplayName("룸에 허용된 상품 ID 목록 조회")
    void findAllowedProductIds() {
      // given
      final Long roomId = 1L;
      final List<ProductId> productIds = Arrays.asList(
          ProductId.of(10L),
          ProductId.of(20L),
          ProductId.of(30L)
      );
      repository.saveAll(roomId, productIds);

      // when
      final List<ProductId> allowedProductIds = repository.findAllowedProductIdsByRoomId(roomId);

      // then
      assertThat(allowedProductIds).hasSize(3);
      assertThat(allowedProductIds).containsExactlyInAnyOrder(
          ProductId.of(10L),
          ProductId.of(20L),
          ProductId.of(30L)
      );
    }
  }

  @Nested
  @DisplayName("saveAll 테스트")
  class SaveAllTests {

    @Test
    @DisplayName("새로운 룸 허용 상품 매핑 저장")
    void saveNewMappings() {
      // given
      final Long roomId = 1L;
      final List<ProductId> productIds = Arrays.asList(
          ProductId.of(10L),
          ProductId.of(20L)
      );

      // when
      repository.saveAll(roomId, productIds);

      // then
      final List<ProductId> saved = repository.findAllowedProductIdsByRoomId(roomId);
      assertThat(saved).hasSize(2);
      assertThat(saved).containsExactlyInAnyOrder(
          ProductId.of(10L),
          ProductId.of(20L)
      );
    }

    @Test
    @DisplayName("기존 매핑을 삭제하고 새로운 매핑 저장")
    void replaceExistingMappings() {
      // given
      final Long roomId = 1L;
      final List<ProductId> oldProductIds = Arrays.asList(
          ProductId.of(10L),
          ProductId.of(20L)
      );
      repository.saveAll(roomId, oldProductIds);

      final List<ProductId> newProductIds = Arrays.asList(
          ProductId.of(30L),
          ProductId.of(40L),
          ProductId.of(50L)
      );

      // when
      repository.saveAll(roomId, newProductIds);

      // then
      final List<ProductId> saved = repository.findAllowedProductIdsByRoomId(roomId);
      assertThat(saved).hasSize(3);
      assertThat(saved).containsExactlyInAnyOrder(
          ProductId.of(30L),
          ProductId.of(40L),
          ProductId.of(50L)
      );
      assertThat(saved).doesNotContain(ProductId.of(10L), ProductId.of(20L));
    }

    @Test
    @DisplayName("빈 리스트로 저장하면 모든 매핑 삭제")
    void deleteAllMappingsWhenSavingEmptyList() {
      // given
      final Long roomId = 1L;
      final List<ProductId> productIds = Arrays.asList(
          ProductId.of(10L),
          ProductId.of(20L)
      );
      repository.saveAll(roomId, productIds);

      // when
      repository.saveAll(roomId, Collections.emptyList());

      // then
      final List<ProductId> saved = repository.findAllowedProductIdsByRoomId(roomId);
      assertThat(saved).isEmpty();
    }

    @Test
    @DisplayName("null 리스트로 저장하면 모든 매핑 삭제")
    void deleteAllMappingsWhenSavingNull() {
      // given
      final Long roomId = 1L;
      final List<ProductId> productIds = Arrays.asList(
          ProductId.of(10L),
          ProductId.of(20L)
      );
      repository.saveAll(roomId, productIds);

      // when
      repository.saveAll(roomId, null);

      // then
      final List<ProductId> saved = repository.findAllowedProductIdsByRoomId(roomId);
      assertThat(saved).isEmpty();
    }
  }

  @Nested
  @DisplayName("deleteByRoomId 테스트")
  class DeleteByRoomIdTests {

    @Test
    @DisplayName("특정 룸의 모든 매핑 삭제")
    void deleteAllMappingsForRoom() {
      // given
      final Long roomId = 1L;
      final List<ProductId> productIds = Arrays.asList(
          ProductId.of(10L),
          ProductId.of(20L),
          ProductId.of(30L)
      );
      repository.saveAll(roomId, productIds);

      // when
      repository.deleteByRoomId(roomId);

      // then
      final List<ProductId> remaining = repository.findAllowedProductIdsByRoomId(roomId);
      assertThat(remaining).isEmpty();
    }

    @Test
    @DisplayName("다른 룸의 매핑은 영향 받지 않음")
    void doNotAffectOtherRooms() {
      // given
      final Long room1 = 1L;
      final Long room2 = 2L;
      repository.saveAll(room1, Arrays.asList(ProductId.of(10L), ProductId.of(20L)));
      repository.saveAll(room2, Arrays.asList(ProductId.of(30L), ProductId.of(40L)));

      // when
      repository.deleteByRoomId(room1);

      // then
      final List<ProductId> room1Mappings = repository.findAllowedProductIdsByRoomId(room1);
      final List<ProductId> room2Mappings = repository.findAllowedProductIdsByRoomId(room2);
      assertThat(room1Mappings).isEmpty();
      assertThat(room2Mappings).hasSize(2);
      assertThat(room2Mappings).containsExactlyInAnyOrder(
          ProductId.of(30L),
          ProductId.of(40L)
      );
    }
  }

  @Nested
  @DisplayName("existsByRoomId 테스트")
  class ExistsByRoomIdTests {

    @Test
    @DisplayName("매핑이 존재하면 true 반환")
    void returnTrueWhenMappingExists() {
      // given
      final Long roomId = 1L;
      repository.saveAll(roomId, Arrays.asList(ProductId.of(10L)));

      // when
      final boolean exists = repository.existsByRoomId(roomId);

      // then
      assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("매핑이 없으면 false 반환")
    void returnFalseWhenNoMappingExists() {
      // given
      final Long roomId = 999L;

      // when
      final boolean exists = repository.existsByRoomId(roomId);

      // then
      assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("매핑 삭제 후 false 반환")
    void returnFalseAfterDeletion() {
      // given
      final Long roomId = 1L;
      repository.saveAll(roomId, Arrays.asList(ProductId.of(10L)));
      repository.deleteByRoomId(roomId);

      // when
      final boolean exists = repository.existsByRoomId(roomId);

      // then
      assertThat(exists).isFalse();
    }
  }

  @Nested
  @DisplayName("비즈니스 시나리오 테스트")
  class BusinessScenarioTests {

    @Test
    @DisplayName("플레이스 어드민이 룸에 특정 상품만 허용")
    void allowSpecificProductsForRoom() {
      // given
      final Long roomId = 5L;
      final ProductId drinkProductId = ProductId.of(100L);
      final ProductId snackProductId = ProductId.of(200L);

      // when
      repository.saveAll(roomId, Arrays.asList(drinkProductId, snackProductId));

      // then
      final List<ProductId> allowedProducts = repository.findAllowedProductIdsByRoomId(roomId);
      assertThat(allowedProducts).hasSize(2);
      assertThat(allowedProducts).containsExactlyInAnyOrder(drinkProductId, snackProductId);
    }

    @Test
    @DisplayName("룸별로 다른 상품 허용 설정")
    void allowDifferentProductsForDifferentRooms() {
      // given
      final Long room1 = 1L;
      final Long room2 = 2L;
      final Long room3 = 3L;

      // when
      repository.saveAll(room1, Arrays.asList(ProductId.of(10L), ProductId.of(20L)));
      repository.saveAll(room2, Arrays.asList(ProductId.of(10L), ProductId.of(20L),
          ProductId.of(30L), ProductId.of(40L)));
      repository.saveAll(room3, Arrays.asList(ProductId.of(30L)));

      // then
      assertThat(repository.findAllowedProductIdsByRoomId(room1)).hasSize(2);
      assertThat(repository.findAllowedProductIdsByRoomId(room2)).hasSize(4);
      assertThat(repository.findAllowedProductIdsByRoomId(room3)).hasSize(1);
    }

    @Test
    @DisplayName("어드민이 허용 상품 목록 업데이트")
    void updateAllowedProducts() {
      // given
      final Long roomId = 1L;
      repository.saveAll(roomId, Arrays.asList(ProductId.of(10L), ProductId.of(20L)));

      // when
      repository.saveAll(roomId, Arrays.asList(
          ProductId.of(10L),
          ProductId.of(30L),
          ProductId.of(40L)
      ));

      // then
      final List<ProductId> updated = repository.findAllowedProductIdsByRoomId(roomId);
      assertThat(updated).hasSize(3);
      assertThat(updated).containsExactlyInAnyOrder(
          ProductId.of(10L),
          ProductId.of(30L),
          ProductId.of(40L)
      );
      assertThat(updated).doesNotContain(ProductId.of(20L));
    }
  }
}
