package com.teambind.springproject.application.service.product;

import com.teambind.springproject.application.dto.request.ProductAvailabilityRequest;
import com.teambind.springproject.application.dto.response.AvailableProductDto;
import com.teambind.springproject.application.dto.response.ProductAvailabilityResponse;
import com.teambind.springproject.application.port.in.QueryProductAvailabilityUseCase;
import com.teambind.springproject.application.port.out.ProductRepository;
import com.teambind.springproject.application.port.out.ReservationPricingRepository;
import com.teambind.springproject.domain.product.Product;
import com.teambind.springproject.domain.product.ProductAvailabilityService;
import com.teambind.springproject.domain.shared.PlaceId;
import com.teambind.springproject.domain.shared.RoomId;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 상품 재고 가용성 조회 Application Service.
 */
@Service
@Transactional(readOnly = true)
public class ProductAvailabilityQueryService implements QueryProductAvailabilityUseCase {

  private static final Logger logger = LoggerFactory.getLogger(
      ProductAvailabilityQueryService.class);

  private final ProductRepository productRepository;
  private final ReservationPricingRepository reservationPricingRepository;
  private final ProductAvailabilityService productAvailabilityService;

  public ProductAvailabilityQueryService(
      final ProductRepository productRepository,
      final ReservationPricingRepository reservationPricingRepository,
      final ProductAvailabilityService productAvailabilityService) {
    this.productRepository = productRepository;
    this.reservationPricingRepository = reservationPricingRepository;
    this.productAvailabilityService = productAvailabilityService;
  }

  @Override
  public ProductAvailabilityResponse queryAvailability(
      final ProductAvailabilityRequest request) {

    logger.info("Querying product availability: roomId={}, placeId={}, timeSlots={}",
        request.roomId(), request.placeId(), request.timeSlots().size());

    final PlaceId placeId = PlaceId.of(request.placeId());
    final RoomId roomId = RoomId.of(request.roomId());

    // 1. 룸에서 접근 가능한 모든 상품 목록 조회 (PLACE, ROOM, RESERVATION scope)
    final List<Product> products = productRepository.findAccessibleProducts(placeId, roomId);

    // 2. 각 상품별 가용 수량 계산
    final List<AvailableProductDto> availableProducts = products.stream()
        .map(product -> {
          final int availableQuantity = productAvailabilityService.calculateAvailableQuantity(
              product,
              request.timeSlots(),
              reservationPricingRepository
          );

          return new AvailableProductDto(
              product.getProductId().getValue(),
              product.getName(),
              product.getPricingStrategy().getInitialPrice().getAmount(),
              availableQuantity,
              product.getTotalQuantity()
          );
        })
        .toList();

    logger.info("Found {} available products for roomId={}", availableProducts.size(),
        request.roomId());

    return new ProductAvailabilityResponse(
        request.roomId(),
        request.placeId(),
        availableProducts
    );
  }
}
