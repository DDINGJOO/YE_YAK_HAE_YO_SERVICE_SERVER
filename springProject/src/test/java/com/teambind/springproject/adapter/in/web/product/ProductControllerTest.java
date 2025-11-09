package com.teambind.springproject.adapter.in.web.product;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teambind.springproject.application.dto.request.PricingStrategyDto;
import com.teambind.springproject.application.dto.request.RegisterProductRequest;
import com.teambind.springproject.application.dto.request.UpdateProductRequest;
import com.teambind.springproject.application.dto.response.ProductResponse;
import com.teambind.springproject.application.port.in.DeleteProductUseCase;
import com.teambind.springproject.application.port.in.GetProductUseCase;
import com.teambind.springproject.application.port.in.QueryProductAvailabilityUseCase;
import com.teambind.springproject.application.port.in.RegisterProductUseCase;
import com.teambind.springproject.application.port.in.UpdateProductUseCase;
import com.teambind.springproject.domain.product.ProductScope;
import com.teambind.springproject.domain.product.PricingType;
import com.teambind.springproject.domain.shared.PlaceId;
import com.teambind.springproject.domain.shared.ProductId;
import com.teambind.springproject.domain.shared.RoomId;
import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProductController.class)
@DisplayName("ProductController 통합 테스트")
class ProductControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private RegisterProductUseCase registerProductUseCase;

  @MockBean
  private GetProductUseCase getProductUseCase;

  @MockBean
  private UpdateProductUseCase updateProductUseCase;

  @MockBean
  private DeleteProductUseCase deleteProductUseCase;

  @MockBean
  private QueryProductAvailabilityUseCase queryProductAvailabilityUseCase;

  @Nested
  @DisplayName("POST /api/products - 상품 등록")
  class RegisterProductTests {

    @Test
    @DisplayName("PLACE Scope 상품 등록에 성공한다")
    void registerPlaceScopedProductSuccess() throws Exception {
      // given
      final PricingStrategyDto pricingStrategy = new PricingStrategyDto(
          PricingType.SIMPLE_STOCK,
          BigDecimal.valueOf(10000),
          null
      );

      final RegisterProductRequest request = new RegisterProductRequest(
          ProductScope.PLACE,
          100L,
          null,
          "공용 빔 프로젝터",
          pricingStrategy,
          5
      );

      final ProductResponse response = new ProductResponse(
          1L,
          ProductScope.PLACE,
          100L,
          null,
          "공용 빔 프로젝터",
          pricingStrategy,
          5
      );

      when(registerProductUseCase.register(any(RegisterProductRequest.class)))
          .thenReturn(response);

      // when & then
      mockMvc.perform(post("/api/products")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.productId").value(1))
          .andExpect(jsonPath("$.scope").value("PLACE"))
          .andExpect(jsonPath("$.placeId").value(100))
          .andExpect(jsonPath("$.roomId").doesNotExist())
          .andExpect(jsonPath("$.name").value("공용 빔 프로젝터"))
          .andExpect(jsonPath("$.pricingStrategy.pricingType").value("SIMPLE_STOCK"))
          .andExpect(jsonPath("$.totalQuantity").value(5));
    }

    @Test
    @DisplayName("ROOM Scope 상품 등록에 성공한다")
    void registerRoomScopedProductSuccess() throws Exception {
      // given
      final PricingStrategyDto pricingStrategy = new PricingStrategyDto(
          PricingType.ONE_TIME,
          BigDecimal.valueOf(15000),
          null
      );

      final RegisterProductRequest request = new RegisterProductRequest(
          ProductScope.ROOM,
          100L,
          200L,
          "룸 전용 화이트보드",
          pricingStrategy,
          3
      );

      final ProductResponse response = new ProductResponse(
          2L,
          ProductScope.ROOM,
          100L,
          200L,
          "룸 전용 화이트보드",
          pricingStrategy,
          3
      );

      when(registerProductUseCase.register(any(RegisterProductRequest.class)))
          .thenReturn(response);

      // when & then
      mockMvc.perform(post("/api/products")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.productId").value(2))
          .andExpect(jsonPath("$.scope").value("ROOM"))
          .andExpect(jsonPath("$.placeId").value(100))
          .andExpect(jsonPath("$.roomId").value(200))
          .andExpect(jsonPath("$.name").value("룸 전용 화이트보드"))
          .andExpect(jsonPath("$.totalQuantity").value(3));
    }

    @Test
    @DisplayName("RESERVATION Scope 상품 등록에 성공한다")
    void registerReservationScopedProductSuccess() throws Exception {
      // given
      final PricingStrategyDto pricingStrategy = new PricingStrategyDto(
          PricingType.SIMPLE_STOCK,
          BigDecimal.valueOf(2000),
          null
      );

      final RegisterProductRequest request = new RegisterProductRequest(
          ProductScope.RESERVATION,
          null,
          null,
          "음료수",
          pricingStrategy,
          100
      );

      final ProductResponse response = new ProductResponse(
          3L,
          ProductScope.RESERVATION,
          null,
          null,
          "음료수",
          pricingStrategy,
          100
      );

      when(registerProductUseCase.register(any(RegisterProductRequest.class)))
          .thenReturn(response);

      // when & then
      mockMvc.perform(post("/api/products")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.productId").value(3))
          .andExpect(jsonPath("$.scope").value("RESERVATION"))
          .andExpect(jsonPath("$.placeId").doesNotExist())
          .andExpect(jsonPath("$.roomId").doesNotExist())
          .andExpect(jsonPath("$.name").value("음료수"))
          .andExpect(jsonPath("$.totalQuantity").value(100));
    }

    @Test
    @DisplayName("필수 필드 누락 시 400 Bad Request를 반환한다")
    void registerProductWithMissingFields() throws Exception {
      // given
      final RegisterProductRequest request = new RegisterProductRequest(
          null,  // scope 누락
          null,
          null,
          null,  // name 누락
          null,  // pricingStrategy 누락
          null   // totalQuantity 누락
      );

      // when & then
      mockMvc.perform(post("/api/products")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("음수 수량 시 400 Bad Request를 반환한다")
    void registerProductWithNegativeQuantity() throws Exception {
      // given
      final PricingStrategyDto pricingStrategy = new PricingStrategyDto(
          PricingType.SIMPLE_STOCK,
          BigDecimal.valueOf(1000),
          null
      );

      final RegisterProductRequest request = new RegisterProductRequest(
          ProductScope.RESERVATION,
          null,
          null,
          "테스트 상품",
          pricingStrategy,
          -5  // 음수 수량
      );

      // when & then
      mockMvc.perform(post("/api/products")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }
  }

  @Nested
  @DisplayName("GET /api/products/{productId} - 상품 조회")
  class GetProductByIdTests {

    @Test
    @DisplayName("상품 조회에 성공한다")
    void getProductSuccess() throws Exception {
      // given
      final Long productId = 1L;
      final ProductResponse response = new ProductResponse(
          productId,
          ProductScope.PLACE,
          100L,
          null,
          "테스트 상품",
          new PricingStrategyDto(PricingType.SIMPLE_STOCK, BigDecimal.valueOf(5000), null),
          10
      );

      when(getProductUseCase.getById(any(ProductId.class))).thenReturn(response);

      // when & then
      mockMvc.perform(get("/api/products/{productId}", productId))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.productId").value(productId))
          .andExpect(jsonPath("$.name").value("테스트 상품"));
    }

    @Test
    @DisplayName("존재하지 않는 상품 조회 시 404를 반환한다")
    void getProductNotFound() throws Exception {
      // given
      final Long productId = 999L;

      when(getProductUseCase.getById(any(ProductId.class)))
          .thenThrow(new NoSuchElementException("Product not found"));

      // when & then
      mockMvc.perform(get("/api/products/{productId}", productId))
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("음수 ID 조회 시 400을 반환한다")
    void getProductWithNegativeId() throws Exception {
      // when & then
      mockMvc.perform(get("/api/products/{productId}", -1))
          .andExpect(status().isBadRequest());
    }
  }

  @Nested
  @DisplayName("GET /api/products - 상품 목록 조회")
  class GetProductsTests {

    @Test
    @DisplayName("전체 상품 목록 조회에 성공한다")
    void getAllProductsSuccess() throws Exception {
      // given
      final List<ProductResponse> responses = List.of(
          new ProductResponse(1L, ProductScope.PLACE, 100L, null, "상품1",
              new PricingStrategyDto(PricingType.SIMPLE_STOCK, BigDecimal.valueOf(5000), null), 10),
          new ProductResponse(2L, ProductScope.RESERVATION, null, null, "상품2",
              new PricingStrategyDto(PricingType.ONE_TIME, BigDecimal.valueOf(3000), null), 20)
      );

      when(getProductUseCase.getAll()).thenReturn(responses);

      // when & then
      mockMvc.perform(get("/api/products"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("Scope로 필터링 조회에 성공한다")
    void getProductsByScopeSuccess() throws Exception {
      // given
      final List<ProductResponse> responses = List.of(
          new ProductResponse(1L, ProductScope.PLACE, 100L, null, "상품1",
              new PricingStrategyDto(PricingType.SIMPLE_STOCK, BigDecimal.valueOf(5000), null), 10)
      );

      when(getProductUseCase.getByScope(ProductScope.PLACE)).thenReturn(responses);

      // when & then
      mockMvc.perform(get("/api/products").param("scope", "PLACE"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(1))
          .andExpect(jsonPath("$[0].scope").value("PLACE"));
    }

    @Test
    @DisplayName("PlaceId로 필터링 조회에 성공한다")
    void getProductsByPlaceIdSuccess() throws Exception {
      // given
      final List<ProductResponse> responses = List.of(
          new ProductResponse(1L, ProductScope.PLACE, 100L, null, "상품1",
              new PricingStrategyDto(PricingType.SIMPLE_STOCK, BigDecimal.valueOf(5000), null), 10)
      );

      when(getProductUseCase.getByPlaceId(any(PlaceId.class))).thenReturn(responses);

      // when & then
      mockMvc.perform(get("/api/products").param("placeId", "100"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(1))
          .andExpect(jsonPath("$[0].placeId").value(100));
    }

    @Test
    @DisplayName("RoomId로 필터링 조회에 성공한다")
    void getProductsByRoomIdSuccess() throws Exception {
      // given
      final List<ProductResponse> responses = List.of(
          new ProductResponse(1L, ProductScope.ROOM, 100L, 200L, "상품1",
              new PricingStrategyDto(PricingType.SIMPLE_STOCK, BigDecimal.valueOf(5000), null), 10)
      );

      when(getProductUseCase.getByRoomId(any(RoomId.class))).thenReturn(responses);

      // when & then
      mockMvc.perform(get("/api/products").param("roomId", "200"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(1))
          .andExpect(jsonPath("$[0].roomId").value(200));
    }
  }

  @Nested
  @DisplayName("PUT /api/products/{productId} - 상품 수정")
  class UpdateProductTests {

    @Test
    @DisplayName("상품 수정에 성공한다")
    void updateProductSuccess() throws Exception {
      // given
      final Long productId = 1L;
      final UpdateProductRequest request = new UpdateProductRequest(
          "수정된 상품명",
          new PricingStrategyDto(PricingType.ONE_TIME, BigDecimal.valueOf(8000), null),
          15
      );

      final ProductResponse response = new ProductResponse(
          productId,
          ProductScope.PLACE,
          100L,
          null,
          "수정된 상품명",
          new PricingStrategyDto(PricingType.ONE_TIME, BigDecimal.valueOf(8000), null),
          15
      );

      when(updateProductUseCase.update(any(ProductId.class), any(UpdateProductRequest.class)))
          .thenReturn(response);

      // when & then
      mockMvc.perform(put("/api/products/{productId}", productId)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.productId").value(productId))
          .andExpect(jsonPath("$.name").value("수정된 상품명"))
          .andExpect(jsonPath("$.totalQuantity").value(15));
    }

    @Test
    @DisplayName("존재하지 않는 상품 수정 시 404를 반환한다")
    void updateProductNotFound() throws Exception {
      // given
      final Long productId = 999L;
      final UpdateProductRequest request = new UpdateProductRequest(
          "수정된 상품명",
          new PricingStrategyDto(PricingType.ONE_TIME, BigDecimal.valueOf(8000), null),
          15
      );

      when(updateProductUseCase.update(any(ProductId.class), any(UpdateProductRequest.class)))
          .thenThrow(new NoSuchElementException("Product not found"));

      // when & then
      mockMvc.perform(put("/api/products/{productId}", productId)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isNotFound());
    }
  }

  @Nested
  @DisplayName("DELETE /api/products/{productId} - 상품 삭제")
  class DeleteProductTests {

    @Test
    @DisplayName("상품 삭제에 성공한다")
    void deleteProductSuccess() throws Exception {
      // given
      final Long productId = 1L;

      doNothing().when(deleteProductUseCase).delete(any(ProductId.class));

      // when & then
      mockMvc.perform(delete("/api/products/{productId}", productId))
          .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("존재하지 않는 상품 삭제 시 404를 반환한다")
    void deleteProductNotFound() throws Exception {
      // given
      final Long productId = 999L;

      doThrow(new NoSuchElementException("Product not found"))
          .when(deleteProductUseCase).delete(any(ProductId.class));

      // when & then
      mockMvc.perform(delete("/api/products/{productId}", productId))
          .andExpect(status().isNotFound());
    }
  }
}
