package com.teambind.springproject.application.service.reservationpricing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.teambind.springproject.application.dto.request.CreateReservationRequest;
import com.teambind.springproject.application.dto.request.ProductRequest;
import com.teambind.springproject.application.dto.response.ReservationPricingResponse;
import com.teambind.springproject.application.port.out.PricingPolicyRepository;
import com.teambind.springproject.application.port.out.ProductRepository;
import com.teambind.springproject.application.port.out.ReservationPricingRepository;
import com.teambind.springproject.common.config.ReservationConfiguration;
import com.teambind.springproject.domain.pricingpolicy.PricingPolicy;
import com.teambind.springproject.domain.product.Product;
import com.teambind.springproject.domain.product.ProductAvailabilityService;
import com.teambind.springproject.domain.product.PricingStrategy;
import com.teambind.springproject.domain.reservationpricing.ReservationPricing;
import com.teambind.springproject.domain.reservationpricing.exception.ProductNotAvailableException;
import com.teambind.springproject.domain.reservationpricing.exception.ReservationPricingNotFoundException;
import com.teambind.springproject.domain.shared.Money;
import com.teambind.springproject.domain.shared.PlaceId;
import com.teambind.springproject.domain.shared.ProductId;
import com.teambind.springproject.domain.shared.ReservationId;
import com.teambind.springproject.domain.shared.ReservationStatus;
import com.teambind.springproject.domain.shared.RoomId;
import com.teambind.springproject.domain.shared.TimeSlot;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReservationPricingService 단위 테스트")
class ReservationPricingServiceTest {

  @Mock
  private PricingPolicyRepository pricingPolicyRepository;

  @Mock
  private ProductRepository productRepository;

  @Mock
  private ReservationPricingRepository reservationPricingRepository;

  @Mock
  private ProductAvailabilityService productAvailabilityService;

  @Mock
  private ReservationConfiguration reservationConfiguration;

  private ReservationPricingService reservationPricingService;

  private RoomId roomId;
  private PlaceId placeId;
  private PricingPolicy pricingPolicy;
  private Product product;
  private LocalDateTime startTime;
  private LocalDateTime endTime;

  @BeforeEach
  void setUp() {
    final ReservationConfiguration.Pending pending = new ReservationConfiguration.Pending();
    pending.setTimeoutMinutes(10L);
    when(reservationConfiguration.getPending()).thenReturn(pending);

    reservationPricingService = new ReservationPricingService(
        pricingPolicyRepository,
        productRepository,
        reservationPricingRepository,
        productAvailabilityService,
        reservationConfiguration
    );

    roomId = RoomId.of(1L);
    placeId = PlaceId.of(100L);

    pricingPolicy = PricingPolicy.create(
        roomId,
        placeId,
        TimeSlot.HOUR,
        Money.of(new BigDecimal("10000"))
    );

    product = Product.createRoomScoped(
        ProductId.of(1L),
        placeId,
        roomId,
        "노트북",
        PricingStrategy.oneTime(Money.of(new BigDecimal("5000"))),
        10
    );

    startTime = LocalDateTime.of(2025, 1, 15, 10, 0);
    endTime = LocalDateTime.of(2025, 1, 15, 12, 0);
  }

  @Nested
  @DisplayName("createReservation 테스트")
  class CreateReservationTests {

    @Test
    @DisplayName("예약 생성 성공")
    void createReservationSuccess() {
      // given
      final List<LocalDateTime> timeSlots = List.of(startTime, startTime.plusHours(1));
      final ProductRequest productRequest = new ProductRequest(1L, 2);
      final CreateReservationRequest request = new CreateReservationRequest(
          1L,
          timeSlots,
          List.of(productRequest)
      );

      when(pricingPolicyRepository.findById(roomId)).thenReturn(Optional.of(pricingPolicy));
      when(productRepository.findById(ProductId.of(1L))).thenReturn(Optional.of(product));
      when(productAvailabilityService.isAvailable(
          eq(product), eq(timeSlots), eq(2), any())).thenReturn(true);

      final java.util.Map<LocalDateTime, Money> slotPriceMap = pricingPolicy
          .calculatePriceBreakdown(startTime, endTime)
          .getSlotPrices()
          .stream()
          .collect(
              java.util.stream.Collectors.toMap(
                  PricingPolicy.SlotPrice::slotTime,
                  PricingPolicy.SlotPrice::price
              )
          );

      final com.teambind.springproject.domain.reservationpricing.TimeSlotPriceBreakdown timeSlotBreakdown =
          new com.teambind.springproject.domain.reservationpricing.TimeSlotPriceBreakdown(
              slotPriceMap, pricingPolicy.getTimeSlot());

      final ReservationPricing savedReservation = ReservationPricing.calculate(
          ReservationId.of(1L),
          roomId,
          timeSlotBreakdown,
          List.of(product.calculatePrice(2)),
          10L
      );

      when(reservationPricingRepository.save(any(ReservationPricing.class)))
          .thenReturn(savedReservation);

      // when
      final ReservationPricingResponse response = reservationPricingService.createReservation(
          request);

      // then
      assertThat(response).isNotNull();
      assertThat(response.reservationId()).isEqualTo(1L);
      assertThat(response.roomId()).isEqualTo(1L);
      assertThat(response.status()).isEqualTo(ReservationStatus.PENDING);

      verify(pricingPolicyRepository).findById(roomId);
      verify(productRepository).findById(ProductId.of(1L));
      verify(productAvailabilityService).isAvailable(eq(product), eq(timeSlots), eq(2), any());
      verify(reservationPricingRepository).save(any(ReservationPricing.class));
    }

    @Test
    @DisplayName("가격 정책이 없으면 예외 발생")
    void throwsExceptionWhenPricingPolicyNotFound() {
      // given
      final List<LocalDateTime> timeSlots = List.of(startTime);
      final ProductRequest productRequest = new ProductRequest(1L, 1);
      final CreateReservationRequest request = new CreateReservationRequest(
          1L,
          timeSlots,
          List.of(productRequest)
      );

      when(pricingPolicyRepository.findById(roomId)).thenReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> reservationPricingService.createReservation(request))
          .isInstanceOf(ReservationPricingNotFoundException.class)
          .hasMessageContaining("Pricing policy not found");

      verify(pricingPolicyRepository).findById(roomId);
    }

    @Test
    @DisplayName("상품이 없으면 예외 발생")
    void throwsExceptionWhenProductNotFound() {
      // given
      final List<LocalDateTime> timeSlots = List.of(startTime);
      final ProductRequest productRequest = new ProductRequest(999L, 1);
      final CreateReservationRequest request = new CreateReservationRequest(
          1L,
          timeSlots,
          List.of(productRequest)
      );

      when(pricingPolicyRepository.findById(roomId)).thenReturn(Optional.of(pricingPolicy));
      when(productRepository.findById(ProductId.of(999L))).thenReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> reservationPricingService.createReservation(request))
          .isInstanceOf(ReservationPricingNotFoundException.class)
          .hasMessageContaining("Product not found");

      verify(pricingPolicyRepository).findById(roomId);
      verify(productRepository).findById(ProductId.of(999L));
    }

    @Test
    @DisplayName("재고가 부족하면 예외 발생")
    void throwsExceptionWhenProductNotAvailable() {
      // given
      final List<LocalDateTime> timeSlots = List.of(startTime);
      final ProductRequest productRequest = new ProductRequest(1L, 100);  // 재고 초과
      final CreateReservationRequest request = new CreateReservationRequest(
          1L,
          timeSlots,
          List.of(productRequest)
      );

      when(pricingPolicyRepository.findById(roomId)).thenReturn(Optional.of(pricingPolicy));
      when(productRepository.findById(ProductId.of(1L))).thenReturn(Optional.of(product));
      when(productAvailabilityService.isAvailable(
          eq(product), eq(timeSlots), eq(100), any())).thenReturn(false);

      // when & then
      assertThatThrownBy(() -> reservationPricingService.createReservation(request))
          .isInstanceOf(ProductNotAvailableException.class)
          .hasMessageContaining("Product is not available");

      verify(pricingPolicyRepository).findById(roomId);
      verify(productRepository).findById(ProductId.of(1L));
      verify(productAvailabilityService).isAvailable(eq(product), eq(timeSlots), eq(100), any());
    }
  }

  @Nested
  @DisplayName("confirmReservation 테스트")
  class ConfirmReservationTests {

    @Test
    @DisplayName("예약 확정 성공")
    void confirmReservationSuccess() {
      // given
      final Long reservationId = 1L;
      final java.util.Map<LocalDateTime, Money> slotPriceMap = pricingPolicy
          .calculatePriceBreakdown(startTime, endTime)
          .getSlotPrices()
          .stream()
          .collect(
              java.util.stream.Collectors.toMap(
                  PricingPolicy.SlotPrice::slotTime,
                  PricingPolicy.SlotPrice::price
              )
          );

      final com.teambind.springproject.domain.reservationpricing.TimeSlotPriceBreakdown timeSlotBreakdown =
          new com.teambind.springproject.domain.reservationpricing.TimeSlotPriceBreakdown(
              slotPriceMap, pricingPolicy.getTimeSlot());

      final ReservationPricing reservation = ReservationPricing.calculate(
          ReservationId.of(reservationId),
          roomId,
          timeSlotBreakdown,
          List.of(product.calculatePrice(1)),
          10L
      );

      when(reservationPricingRepository.findById(ReservationId.of(reservationId)))
          .thenReturn(Optional.of(reservation));
      when(reservationPricingRepository.save(any(ReservationPricing.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      // when
      final ReservationPricingResponse response = reservationPricingService.confirmReservation(
          reservationId);

      // then
      assertThat(response).isNotNull();
      assertThat(response.status()).isEqualTo(ReservationStatus.CONFIRMED);

      verify(reservationPricingRepository).findById(ReservationId.of(reservationId));
      verify(reservationPricingRepository).save(any(ReservationPricing.class));
    }

    @Test
    @DisplayName("예약이 없으면 예외 발생")
    void throwsExceptionWhenReservationNotFound() {
      // given
      final Long reservationId = 999L;
      when(reservationPricingRepository.findById(ReservationId.of(reservationId)))
          .thenReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> reservationPricingService.confirmReservation(reservationId))
          .isInstanceOf(ReservationPricingNotFoundException.class)
          .hasMessageContaining("Reservation pricing not found");

      verify(reservationPricingRepository).findById(ReservationId.of(reservationId));
    }
  }

  @Nested
  @DisplayName("cancelReservation 테스트")
  class CancelReservationTests {

    @Test
    @DisplayName("예약 취소 성공")
    void cancelReservationSuccess() {
      // given
      final Long reservationId = 1L;
      final java.util.Map<LocalDateTime, Money> slotPriceMap = pricingPolicy
          .calculatePriceBreakdown(startTime, endTime)
          .getSlotPrices()
          .stream()
          .collect(
              java.util.stream.Collectors.toMap(
                  PricingPolicy.SlotPrice::slotTime,
                  PricingPolicy.SlotPrice::price
              )
          );

      final com.teambind.springproject.domain.reservationpricing.TimeSlotPriceBreakdown timeSlotBreakdown =
          new com.teambind.springproject.domain.reservationpricing.TimeSlotPriceBreakdown(
              slotPriceMap, pricingPolicy.getTimeSlot());

      final ReservationPricing reservation = ReservationPricing.calculate(
          ReservationId.of(reservationId),
          roomId,
          timeSlotBreakdown,
          List.of(product.calculatePrice(1)),
          10L
      );

      when(reservationPricingRepository.findById(ReservationId.of(reservationId)))
          .thenReturn(Optional.of(reservation));
      when(reservationPricingRepository.save(any(ReservationPricing.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      // when
      final ReservationPricingResponse response = reservationPricingService.cancelReservation(
          reservationId);

      // then
      assertThat(response).isNotNull();
      assertThat(response.status()).isEqualTo(ReservationStatus.CANCELLED);

      verify(reservationPricingRepository).findById(ReservationId.of(reservationId));
      verify(reservationPricingRepository).save(any(ReservationPricing.class));
    }

    @Test
    @DisplayName("예약이 없으면 예외 발생")
    void throwsExceptionWhenReservationNotFound() {
      // given
      final Long reservationId = 999L;
      when(reservationPricingRepository.findById(ReservationId.of(reservationId)))
          .thenReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> reservationPricingService.cancelReservation(reservationId))
          .isInstanceOf(ReservationPricingNotFoundException.class)
          .hasMessageContaining("Reservation pricing not found");

      verify(reservationPricingRepository).findById(ReservationId.of(reservationId));
    }
  }
}
