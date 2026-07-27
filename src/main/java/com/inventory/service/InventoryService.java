package com.inventory.service;

import com.inventory.dto.StockMovementRequest;
import com.inventory.entity.Inventory;
import com.inventory.entity.InventoryTransaction;
import com.inventory.entity.Product;
import com.inventory.exception.InsufficientStockException;
import com.inventory.exception.ResourceNotFoundException;
import com.inventory.repository.InventoryRepository;
import com.inventory.repository.InventoryTransactionRepository;
import com.inventory.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryTransactionRepository transactionRepository;

    /**
     * Stock-in: increase quantity_on_hand and write an audit row.
     * @Transactional ensures the inventory update and the transaction-log
     * insert either both commit or both roll back together.
     */
    @Transactional
    public void stockIn(Long productId, StockMovementRequest req) {
        Inventory inventory = getInventoryOrThrow(productId);
        inventory.setQuantityOnHand(inventory.getQuantityOnHand() + req.getQuantity());
        inventoryRepository.save(inventory);

        logTransaction(inventory.getProduct(), InventoryTransaction.Type.IN, req);
    }

    /**
     * Stock-out: validate sufficient quantity exists BEFORE decrementing.
     * Throws InsufficientStockException (mapped to 409 Conflict) rather than
     * silently allowing quantity_on_hand to go negative.
     */
    @Transactional
    public void stockOut(Long productId, StockMovementRequest req) {
        Inventory inventory = getInventoryOrThrow(productId);

        if (inventory.getQuantityOnHand() < req.getQuantity()) {
            throw new InsufficientStockException(
                    "Insufficient stock for product " + productId +
                    ": requested " + req.getQuantity() +
                    ", available " + inventory.getQuantityOnHand());
        }

        inventory.setQuantityOnHand(inventory.getQuantityOnHand() - req.getQuantity());
        inventoryRepository.save(inventory);

        logTransaction(inventory.getProduct(), InventoryTransaction.Type.OUT, req);
    }

    @Transactional(readOnly = true)
    public Page<InventoryTransaction> getTransactionHistory(Long productId, Pageable pageable) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found: " + productId);
        }
        return transactionRepository.findByProductIdOrderByCreatedAtDesc(productId, pageable);
    }

    private Inventory getInventoryOrThrow(Long productId) {
        return inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No inventory record for product: " + productId));
    }

    private void logTransaction(Product product, InventoryTransaction.Type type, StockMovementRequest req) {
        InventoryTransaction txn = new InventoryTransaction();
        txn.setProduct(product);
        txn.setTransactionType(type);
        txn.setQuantity(req.getQuantity());
        txn.setReferenceNote(req.getReferenceNote());
        txn.setCreatedBy(req.getCreatedBy());
        transactionRepository.save(txn);
    }
}
