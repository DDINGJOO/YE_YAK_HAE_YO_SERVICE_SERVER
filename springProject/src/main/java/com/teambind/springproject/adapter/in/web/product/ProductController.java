package com.teambind.springproject.adapter.in.web.product;

import com.teambind.springproject.application.dto.request.RegisterProductRequest;
import com.teambind.springproject.application.dto.request.UpdateProductRequest;
import com.teambind.springproject.application.dto.response.ProductResponse;
import com.teambind.springproject.application.port.in.DeleteProductUseCase;
import com.teambind.springproject.application.port.in.GetProductUseCase;
import com.teambind.springproject.application.port.in.RegisterProductUseCase;
import com.teambind.springproject.application.port.in.UpdateProductUseCase;
import com.teambind.springproject.domain.product.ProductScope;
import com.teambind.springproject.domain.shared.PlaceId;
import com.teambind.springproject.domain.shared.ProductId;
import com.teambind.springproject.domain.shared.RoomId;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 추가상품 관리 REST Controller.
 */
@RestController
@RequestMapping("/api/products")
@Validated
public class ProductController {

  private final RegisterProductUseCase registerProductUseCase;
  private final GetProductUseCase getProductUseCase;
  private final UpdateProductUseCase updateProductUseCase;
  private final DeleteProductUseCase deleteProductUseCase;

  public ProductController(
      final RegisterProductUseCase registerProductUseCase,
      final GetProductUseCase getProductUseCase,
      final UpdateProductUseCase updateProductUseCase,
      final DeleteProductUseCase deleteProductUseCase) {
    this.registerProductUseCase = registerProductUseCase;
    this.getProductUseCase = getProductUseCase;
    this.updateProductUseCase = updateProductUseCase;
    this.deleteProductUseCase = deleteProductUseCase;
  }

  /**
   * 상품 등록.
   *
   * @param request 상품 등록 요청
   * @return 등록된 상품 정보
   */
  @PostMapping
  public ResponseEntity<ProductResponse> registerProduct(
      @RequestBody @Valid final RegisterProductRequest request) {

    final ProductResponse response = registerProductUseCase.register(request);

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /**
   * 상품 ID로 조회.
   *
   * @param productId 상품 ID
   * @return 상품 정보
   */
  @GetMapping("/{productId}")
  public ResponseEntity<ProductResponse> getProduct(
      @PathVariable @Positive(message = "Product ID must be positive") final Long productId) {

    final ProductResponse response = getProductUseCase.getById(ProductId.of(productId));

    return ResponseEntity.ok(response);
  }

  /**
   * 상품 목록 조회.
   * Query Parameter로 필터링 조건을 지정할 수 있습니다.
   *
   * @param scope Scope로 필터링 (선택)
   * @param placeId PlaceId로 필터링 (선택)
   * @param roomId RoomId로 필터링 (선택)
   * @return 상품 목록
   */
  @GetMapping
  public ResponseEntity<List<ProductResponse>> getProducts(
      @RequestParam(required = false) final ProductScope scope,
      @RequestParam(required = false) @Positive(message = "Place ID must be positive") final Long placeId,
      @RequestParam(required = false) @Positive(message = "Room ID must be positive") final Long roomId) {

    final List<ProductResponse> responses;

    // 우선순위: roomId > placeId > scope > all
    if (roomId != null) {
      responses = getProductUseCase.getByRoomId(RoomId.of(roomId));
    } else if (placeId != null) {
      responses = getProductUseCase.getByPlaceId(PlaceId.of(placeId));
    } else if (scope != null) {
      responses = getProductUseCase.getByScope(scope);
    } else {
      responses = getProductUseCase.getAll();
    }

    return ResponseEntity.ok(responses);
  }

  /**
   * 상품 정보 수정.
   *
   * @param productId 상품 ID
   * @param request   수정 요청
   * @return 수정된 상품 정보
   */
  @PutMapping("/{productId}")
  public ResponseEntity<ProductResponse> updateProduct(
      @PathVariable @Positive(message = "Product ID must be positive") final Long productId,
      @RequestBody @Valid final UpdateProductRequest request) {

    final ProductResponse response = updateProductUseCase.update(
        ProductId.of(productId),
        request
    );

    return ResponseEntity.ok(response);
  }

  /**
   * 상품 삭제.
   *
   * @param productId 상품 ID
   * @return 204 No Content
   */
  @DeleteMapping("/{productId}")
  public ResponseEntity<Void> deleteProduct(
      @PathVariable @Positive(message = "Product ID must be positive") final Long productId) {

    deleteProductUseCase.delete(ProductId.of(productId));

    return ResponseEntity.noContent().build();
  }
}
