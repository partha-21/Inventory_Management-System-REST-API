package com.inventory.service;

import com.inventory.dto.ProductRequest;
import com.inventory.dto.ProductResponse;
import com.inventory.entity.*;
import com.inventory.exception.ResourceNotFoundException;
import com.inventory.repository.CategoryRepository;
import com.inventory.repository.ProductRepository;
import com.inventory.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;

    @Transactional
    public ProductResponse createProduct(ProductRequest req) {
        if (productRepository.existsBySkuIgnoreCase(req.getSku())) {
            throw new IllegalArgumentException("A product with SKU '" + req.getSku() + "' already exists");
        }

        Category category = categoryRepository.findById(req.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + req.getCategoryId()));
        Supplier supplier = supplierRepository.findById(req.getSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found: " + req.getSupplierId()));

        Product product = new Product();
        product.setName(req.getName());
        product.setSku(req.getSku());
        product.setCategory(category);
        product.setSupplier(supplier);
        product.setUnitPrice(req.getUnitPrice());
        product.setReorderLevel(req.getReorderLevel() == null ? 10 : req.getReorderLevel());

        Inventory inventory = new Inventory();
        inventory.setProduct(product);
        inventory.setQuantityOnHand(req.getInitialQuantity() == null ? 0 : req.getInitialQuantity());
        inventory.setWarehouseLocation(req.getWarehouseLocation());
        product.setInventory(inventory);

        Product saved = productRepository.save(product);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long id) {
        Product product = productRepository.findActiveById(id);
        if (product == null) {
            throw new ResourceNotFoundException("Product not found: " + id);
        }
        return toResponse(product);
    }

    /**
     * One query handles filtering + pagination + eager-loading category/supplier/inventory
     * (see ProductRepository#search for the @EntityGraph that prevents N+1 here).
     */
    @Transactional(readOnly = true)
    public Page<ProductResponse> searchProducts(Long categoryId, Long supplierId, String name, Pageable pageable) {
        return productRepository.search(categoryId, supplierId, name, pageable)
                .map(this::toResponse);
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest req) {
        Product product = productRepository.findById(id)
                .filter(Product::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));

        if (!product.getSku().equalsIgnoreCase(req.getSku())
                && productRepository.existsBySkuIgnoreCase(req.getSku())) {
            throw new IllegalArgumentException("A product with SKU '" + req.getSku() + "' already exists");
        }

        Category category = categoryRepository.findById(req.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + req.getCategoryId()));
        Supplier supplier = supplierRepository.findById(req.getSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found: " + req.getSupplierId()));

        product.setName(req.getName());
        product.setSku(req.getSku());
        product.setCategory(category);
        product.setSupplier(supplier);
        product.setUnitPrice(req.getUnitPrice());
        product.setReorderLevel(req.getReorderLevel());

        if (product.getInventory() != null && req.getWarehouseLocation() != null) {
            product.getInventory().setWarehouseLocation(req.getWarehouseLocation());
        }

        return toResponse(productRepository.save(product));
    }

    /** Soft delete: flip is_active rather than a hard DELETE, so history/reports stay intact. */
    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
        product.setActive(false);
        productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getLowStockProducts() {
        return productRepository.findLowStockProducts().stream()
                .map(this::toResponse)
                .toList();
    }

    private ProductResponse toResponse(Product p) {
        Integer qty = p.getInventory() != null ? p.getInventory().getQuantityOnHand() : 0;
        return ProductResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .sku(p.getSku())
                .categoryName(p.getCategory() != null ? p.getCategory().getName() : null)
                .supplierName(p.getSupplier() != null ? p.getSupplier().getName() : null)
                .unitPrice(p.getUnitPrice())
                .reorderLevel(p.getReorderLevel())
                .quantityOnHand(qty)
                .warehouseLocation(p.getInventory() != null ? p.getInventory().getWarehouseLocation() : null)
                .lowStock(qty != null && qty <= p.getReorderLevel())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
