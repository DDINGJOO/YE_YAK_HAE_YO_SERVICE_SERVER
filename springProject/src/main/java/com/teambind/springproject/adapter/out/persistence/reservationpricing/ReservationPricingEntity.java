package com.teambind.springproject.adapter.out.persistence.reservationpricing;

import com.teambind.springproject.domain.product.vo.ProductPriceBreakdown;
import com.teambind.springproject.domain.reservationpricing.ReservationPricing;
import com.teambind.springproject.domain.reservationpricing.TimeSlotPriceBreakdown;
import com.teambind.springproject.domain.shared.Money;
import com.teambind.springproject.domain.shared.ReservationId;
import com.teambind.springproject.domain.shared.ReservationStatus;
import com.teambind.springproject.domain.shared.RoomId;
import com.teambind.springproject.domain.shared.TimeSlot;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ReservationPricing Aggregate의 JPA Entity.
 */
@Entity
@Table(name = "reservation_pricings")
public class ReservationPricingEntity {

  @Id
  @GeneratedValue(generator = "snowflake-id")
  @org.hibernate.annotations.GenericGenerator(
      name = "snowflake-id",
      type = com.teambind.springproject.common.util.generator.SnowflakeIdGenerator.class
  )
  @Column(name = "reservation_id")
  private Long id;

  @Column(name = "room_id", nullable = false)
  private Long roomId;

  @Column(name = "place_id", nullable = false)
  private Long placeId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private ReservationStatus status;

  @Enumerated(EnumType.STRING)
  @Column(name = "time_slot", nullable = false, length = 10)
  private TimeSlot timeSlot;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(
      name = "reservation_pricing_slots",
      joinColumns = @JoinColumn(name = "reservation_id")
  )
  @MapKeyColumn(name = "slot_time")
  @Column(name = "slot_price", precision = 12, scale = 2)
  private Map<LocalDateTime, BigDecimal> slotPrices = new HashMap<>();

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(
      name = "reservation_pricing_products",
      joinColumns = @JoinColumn(name = "reservation_id")
  )
  private List<ProductPriceBreakdownEmbeddable> productBreakdowns = new ArrayList<>();

  @Column(name = "total_price", nullable = false, precision = 12, scale = 2)
  private BigDecimal totalPrice;

  @Column(name = "calculated_at", nullable = false)
  private LocalDateTime calculatedAt;

  @Column(name = "expires_at", nullable = false)
  private LocalDateTime expiresAt;

  protected ReservationPricingEntity() {
    // JPA용 기본 생성자
  }

  public ReservationPricingEntity(
      final Long id,
      final Long roomId,
      final Long placeId,
      final ReservationStatus status,
      final TimeSlot timeSlot,
      final Map<LocalDateTime, BigDecimal> slotPrices,
      final List<ProductPriceBreakdownEmbeddable> productBreakdowns,
      final BigDecimal totalPrice,
      final LocalDateTime calculatedAt,
      final LocalDateTime expiresAt) {
    this.id = id;
    this.roomId = roomId;
    this.placeId = placeId;
    this.status = status;
    this.timeSlot = timeSlot;
    this.slotPrices = new HashMap<>(slotPrices);
    this.productBreakdowns = new ArrayList<>(productBreakdowns);
    this.totalPrice = totalPrice;
    this.calculatedAt = calculatedAt;
    this.expiresAt = expiresAt;
  }

  /**
   * Domain ReservationPricing을 Entity로 변환합니다.
   * PlaceId는 Domain에 없으므로 별도로 전달받아야 합니다.
   *
   * @param pricing ReservationPricing Domain 객체
   * @param placeId Place ID (Room이 속한 Place)
   * @return ReservationPricingEntity
   */
  public static ReservationPricingEntity fromDomain(
      final ReservationPricing pricing,
      final Long placeId) {
    // TimeSlotPriceBreakdown의 Map 변환: Money -> BigDecimal
    final Map<LocalDateTime, BigDecimal> slotPricesMap = pricing.getTimeSlotBreakdown()
        .slotPrices().entrySet().stream()
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            entry -> entry.getValue().getAmount()
        ));

    // ProductPriceBreakdown 리스트 변환
    final List<ProductPriceBreakdownEmbeddable> productBreakdownsList = pricing
        .getProductBreakdowns().stream()
        .map(ProductPriceBreakdownEmbeddable::fromDomain)
        .toList();

    return new ReservationPricingEntity(
        pricing.getReservationId().getValue(),
        pricing.getRoomId().getValue(),
        placeId,
        pricing.getStatus(),
        pricing.getTimeSlotBreakdown().timeSlot(),
        slotPricesMap,
        productBreakdownsList,
        pricing.getTotalPrice().getAmount(),
        pricing.getCalculatedAt(),
        pricing.getExpiresAt()
    );
  }

  /**
   * Entity를 Domain ReservationPricing으로 변환합니다.
   */
  public ReservationPricing toDomain() {
    // Map<LocalDateTime, BigDecimal> -> Map<LocalDateTime, Money>
    final Map<LocalDateTime, Money> domainSlotPrices = slotPrices.entrySet().stream()
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            entry -> Money.of(entry.getValue())
        ));

    // TimeSlotPriceBreakdown 재구성
    final TimeSlotPriceBreakdown timeSlotBreakdown = new TimeSlotPriceBreakdown(
        domainSlotPrices,
        timeSlot
    );

    // ProductPriceBreakdown 리스트 재구성
    final List<ProductPriceBreakdown> domainProductBreakdowns = productBreakdowns.stream()
        .map(ProductPriceBreakdownEmbeddable::toDomain)
        .toList();

    // restore()를 사용하여 저장된 데이터 복원 (status, calculatedAt, expiresAt 보존)
    return ReservationPricing.restore(
        ReservationId.of(id),
        RoomId.of(roomId),
        status,
        timeSlotBreakdown,
        domainProductBreakdowns,
        Money.of(totalPrice),
        calculatedAt,
        expiresAt
    );
  }

  // Getters

  public Long getId() {
    return id;
  }

  public Long getRoomId() {
    return roomId;
  }

  public Long getPlaceId() {
    return placeId;
  }

  public ReservationStatus getStatus() {
    return status;
  }

  public TimeSlot getTimeSlot() {
    return timeSlot;
  }

  public Map<LocalDateTime, BigDecimal> getSlotPrices() {
    return new HashMap<>(slotPrices);
  }

  public List<ProductPriceBreakdownEmbeddable> getProductBreakdowns() {
    return new ArrayList<>(productBreakdowns);
  }

  public BigDecimal getTotalPrice() {
    return totalPrice;
  }

  public LocalDateTime getCalculatedAt() {
    return calculatedAt;
  }

  public LocalDateTime getExpiresAt() {
    return expiresAt;
  }

  public void setStatus(final ReservationStatus status) {
    this.status = status;
  }
}
