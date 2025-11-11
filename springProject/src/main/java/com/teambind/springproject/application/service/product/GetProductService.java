package com.teambind.springproject.application.service.product;

import com.teambind.springproject.application.dto.response.ProductResponse;
import com.teambind.springproject.application.port.in.GetProductUseCase;
import com.teambind.springproject.application.port.out.ProductRepository;
import com.teambind.springproject.domain.product.Product;
import com.teambind.springproject.domain.product.vo.ProductScope;
import com.teambind.springproject.domain.shared.PlaceId;
import com.teambind.springproject.domain.shared.ProductId;
import com.teambind.springproject.domain.shared.RoomId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * 상품 조회 Application Service.
 * GetProductUseCase를 구현합니다.
 */
@Service
@Transactional(readOnly = true)
public class GetProductService implements GetProductUseCase {
	
	private static final Logger logger = LoggerFactory.getLogger(GetProductService.class);
	
	private final ProductRepository productRepository;
	
	public GetProductService(final ProductRepository productRepository) {
		this.productRepository = productRepository;
	}
	
	@Override
	public ProductResponse getById(final ProductId productId) {
		logger.info("Fetching product by id: {}", productId.getValue());
		
		final Product product = productRepository.findById(productId)
				.orElseThrow(() -> new NoSuchElementException(
						"Product not found with id: " + productId.getValue()));
		
		return ProductResponse.from(product);
	}
	
	@Override
	public List<ProductResponse> getAll() {
		logger.info("Fetching all products");
		
		// ProductRepository에는 findAll()이 없으므로 모든 Scope를 조회
		final List<Product> placeProducts = productRepository.findByScope(ProductScope.PLACE);
		final List<Product> roomProducts = productRepository.findByScope(ProductScope.ROOM);
		final List<Product> reservationProducts = productRepository.findByScope(ProductScope.RESERVATION);
		
		return List.of(placeProducts, roomProducts, reservationProducts).stream()
				.flatMap(List::stream)
				.map(ProductResponse::from)
				.toList();
	}
	
	@Override
	public List<ProductResponse> getByPlaceId(final PlaceId placeId) {
		logger.info("Fetching products by placeId: {}", placeId.getValue());
		
		final List<Product> products = productRepository.findByPlaceId(placeId);
		
		return products.stream()
				.map(ProductResponse::from)
				.toList();
	}
	
	@Override
	public List<ProductResponse> getByRoomId(final RoomId roomId) {
		logger.info("Fetching products by roomId: {}", roomId.getValue());
		
		final List<Product> products = productRepository.findByRoomId(roomId);
		
		return products.stream()
				.map(ProductResponse::from)
				.toList();
	}
	
	@Override
	public List<ProductResponse> getByScope(final ProductScope scope) {
		logger.info("Fetching products by scope: {}", scope);
		
		final List<Product> products = productRepository.findByScope(scope);
		
		return products.stream()
				.map(ProductResponse::from)
				.toList();
	}
}
