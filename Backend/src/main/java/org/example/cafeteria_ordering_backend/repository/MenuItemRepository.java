
package org.example.cafeteria_ordering_backend.repository;

import org.example.cafeteria_ordering_backend.entity.MenuItems;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItems, Long> {

    // Retrieves available menu items for a specific category in display order.
    List<MenuItems> findByCategoryCategoryIdAndAvailableTrueOrderByDisplayOrderAsc (Long categoryId);

    // Retrieves all available menu items in display order.
    List<MenuItems> findByAvailableTrueOrderByDisplayOrderAsc ();

    // Deducts stock only when the item is available and enough stock remains. Auto sets to unavailable if 0 stock
    @Modifying
    @Query("""
        UPDATE MenuItems m
        SET m.stockQuantity = m.stockQuantity - :quantity,
            m.available = CASE WHEN (m.stockQuantity - :quantity) <= 0 THEN false ELSE m.available END
        WHERE m.menuItemId = :menuItemId
          AND m.stockQuantity >= :quantity
          AND m.available = true
        """)
    int deductStockAndAutoDisable (@Param("menuItemId") Long menuItemId, @Param("quantity") int quantity);


    // Restores stock when an eligible order is cancelled.
    @Modifying
    @Query("""
        UPDATE MenuItems m
        SET m.stockQuantity = m.stockQuantity + :quantity
        WHERE m.menuItemId = :menuItemId
        """)
    int restoreStock (@Param("menuItemId") Long menuItemId, @Param("quantity") int quantity);
}
