package org.example.cafeteria_ordering_backend.component;
import org.example.cafeteria_ordering_backend.dto.requests.OrderRequestDTO;
import org.example.cafeteria_ordering_backend.entity.*;
import org.example.cafeteria_ordering_backend.exception.ResourceNotFoundException;
import org.example.cafeteria_ordering_backend.exception.ResourceUnavailableException;
import org.example.cafeteria_ordering_backend.repository.MenuItemRepository;
import org.example.cafeteria_ordering_backend.repository.ModifierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderItemCalculatorTest {

    // Mock repositories to avoid accessing the actual database during tests.
    @Mock
    private MenuItemRepository menuItemRepository;

    @Mock
    private ModifierRepository modifierRepository;

    // Creates the calaculator and injects the mocked repositories.
    @InjectMocks
    private OrderItemCalculator orderItemCalculator;

    // Test objects used to simulate menu items, modifiers, and orders.
    private MenuItems mockMenuItem;
    private Modifier mockModifier;
    private ModifierGroup mockModifierGroup;
    private Order mockOrder;

    // Sets up the default test data before each test.
    @BeforeEach
    void setUp() {
        mockModifierGroup = new ModifierGroup();
        mockModifierGroup.setId(1L);
        mockModifierGroup.setName("Size / Add-ons");
        mockModifierGroup.setMinSelections(1);
        mockModifierGroup.setMaxSelections(2);
        mockModifierGroup.setIsRequired(true);
        mockModifierGroup.setAvailable(true);

        mockModifier = new Modifier();
        mockModifier.setId(100L);
        mockModifier.setName("Extra Shot");
        mockModifier.setPriceModifier(new BigDecimal("5.00"));
        mockModifier.setAvailable(true);
        mockModifier.setModifierGroup(mockModifierGroup);

        mockMenuItem = new MenuItems();
        mockMenuItem.setMenuItemId(10L);
        mockMenuItem.setName("Espresso");
        mockMenuItem.setPrice(new BigDecimal("25.00"));
        mockMenuItem.setAvailable(true);

        // Links the modifier group to the menu item.
        MenuItemModifierGroup menuItemModifierGroup = new MenuItemModifierGroup();
        menuItemModifierGroup.setMenuItem(mockMenuItem);
        menuItemModifierGroup.setModifierGroup(mockModifierGroup);

        mockMenuItem.setMenuItemModifierGroups(new HashSet<>(Set.of(menuItemModifierGroup)));

        // Creates a sample order for the calculated order item.
        mockOrder = new Order();
        mockOrder.setOrderId(50L);
    }

    @Test
    @DisplayName("Should calculate total correctly for quantity greater than one without modifiers")
    void calculateAndBuildItems_Success_NoModifiers() {

        // Removes modifier group to test an item without customisations.
        mockMenuItem.setMenuItemModifierGroups(new HashSet<>());
        OrderRequestDTO.OrderItemRequest itemRequest = new OrderRequestDTO.OrderItemRequest();
        itemRequest.setMenuItemId(10L);
        itemRequest.setQuantity(2);
        itemRequest.setSpecialInstructions("Extra hot");
        itemRequest.setModifierIds(List.of());

        // Returns the sample menu item when the repository is queried.
        when(menuItemRepository.findAllById(Set.of(10L))).thenReturn(List.of(mockMenuItem));
        OrderItemCalculator.CalculationResult result = orderItemCalculator.calculateAndBuildItems(List.of(itemRequest), mockOrder);

        // Checks the total price, item quantity, and subtotal.
        assertThat(result).isNotNull();
        assertThat(result.totalPrice()).isEqualByComparingTo("50.00");
        assertThat(result.orderItems()).hasSize(1);
        assertThat(result.orderItems().get(0).getQuantity()).isEqualTo(2);
        assertThat(result.orderItems().get(0).calculateSubtotal()).isEqualByComparingTo("50.00");
    }

    @Test
    @DisplayName("Should calculate total correctly for quantity one")
    void calculateAndBuildItems_QuantityOne_CalculatesCorrectly() {
        mockMenuItem.setMenuItemModifierGroups(new HashSet<>());

        OrderRequestDTO.OrderItemRequest itemRequest = new OrderRequestDTO.OrderItemRequest();
        itemRequest.setMenuItemId(10L);
        itemRequest.setQuantity(1);
        itemRequest.setModifierIds(List.of());

        when(menuItemRepository.findAllById(Set.of(10L))).thenReturn(List.of(mockMenuItem));
        OrderItemCalculator.CalculationResult result = orderItemCalculator.calculateAndBuildItems(List.of(itemRequest), mockOrder);

        // Confirms that one item costs the original menu price.
        assertThat(result.totalPrice()).isEqualByComparingTo("25.00");
        assertThat(result.orderItems().get(0).getQuantity()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should multiply the base price and modifier price by quantity")
    void calculateAndBuildItems_WithModifier_MultipliesByQuantity() {
        OrderRequestDTO.OrderItemRequest itemRequest =
                new OrderRequestDTO.OrderItemRequest();
        itemRequest.setMenuItemId(10L);
        itemRequest.setQuantity(3);
        itemRequest.setModifierIds(List.of(100L));

        // Mocks the repositories to return the item and its selected modifier.
        when(menuItemRepository.findAllById(Set.of(10L))).thenReturn(List.of(mockMenuItem));
        when(modifierRepository.findAllById(Set.of(100L))).thenReturn(List.of(mockModifier));
        OrderItemCalculator.CalculationResult result = orderItemCalculator.calculateAndBuildItems(List.of(itemRequest), mockOrder);

        // Calculates (base price + modifier price) * quantity.
        assertThat(result.totalPrice()).isEqualByComparingTo("90.00");
        assertThat(result.orderItems()).hasSize(1);
        OrderItem orderItem = result.orderItems().get(0);
        assertThat(orderItem.getQuantity()).isEqualTo(3);
        assertThat(orderItem.calculateSubtotal()).isEqualByComparingTo("90.00");
    }

    @Test
    @DisplayName("Should reject a null quantity")
    void calculateAndBuildItems_NullQuantity_ThrowsException() {
        OrderRequestDTO.OrderItemRequest itemRequest = new OrderRequestDTO.OrderItemRequest();
        itemRequest.setMenuItemId(10L);
        itemRequest.setQuantity(null);

        // Verifies that a missing quantity is rejected.
        assertThatThrownBy(() -> orderItemCalculator.calculateAndBuildItems(List.of(itemRequest), mockOrder
                ))
                .isInstanceOf(ResourceUnavailableException.class)
                .hasMessageContaining("Order quantity must be at least 1");
    }

    @Test
    @DisplayName("Should reject quantity zero")
    void calculateAndBuildItems_ZeroQuantity_ThrowsException() {
        OrderRequestDTO.OrderItemRequest itemRequest = new OrderRequestDTO.OrderItemRequest();
        itemRequest.setMenuItemId(10L);
        itemRequest.setQuantity(0);

        // Verifies that an order cannot contain zero items.
        assertThatThrownBy(() -> orderItemCalculator.calculateAndBuildItems(List.of(itemRequest), mockOrder
                ))
                .isInstanceOf(ResourceUnavailableException.class)
                .hasMessageContaining("Order quantity must be at least 1");
    }

    @Test
    @DisplayName("Should reject a negative quantity")
    void calculateAndBuildItems_NegativeQuantity_ThrowsException() {
        OrderRequestDTO.OrderItemRequest itemRequest =
                new OrderRequestDTO.OrderItemRequest();
        itemRequest.setMenuItemId(10L);
        itemRequest.setQuantity(-2);

        //Verifies that negative quantities are rejected.
        assertThatThrownBy(() -> orderItemCalculator.calculateAndBuildItems(List.of(itemRequest), mockOrder
                ))
                .isInstanceOf(ResourceUnavailableException.class)
                .hasMessageContaining("Order quantity must be at least 1");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when menu item does not exist")
    void calculateAndBuildItems_MenuItemNotFound_ThrowsException() {
        OrderRequestDTO.OrderItemRequest itemRequest = new OrderRequestDTO.OrderItemRequest();
        itemRequest.setMenuItemId(99L);
        itemRequest.setQuantity(1);

        // Simulates a repository search that finds no matching menu item.
        when(menuItemRepository.findAllById(Set.of(99L))).thenReturn(List.of());

        // Verifies that a missing menu item produces the expected exception.
        assertThatThrownBy(() -> orderItemCalculator.calculateAndBuildItems(List.of(itemRequest), mockOrder
                ))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Menu item not found with ID: 99");
    }

    @Test
    @DisplayName("Should throw ResourceUnavailableException when menu item is unavailable")
    void calculateAndBuildItems_MenuItemOutOfStock_ThrowsException() {
        // Marks the menu item as unavailable.
        mockMenuItem.setAvailable(false);
        OrderRequestDTO.OrderItemRequest itemRequest = new OrderRequestDTO.OrderItemRequest();
        itemRequest.setMenuItemId(10L);
        itemRequest.setQuantity(1);

        when(menuItemRepository.findAllById(Set.of(10L))).thenReturn(List.of(mockMenuItem));

        // Verifies that unavailable menu items cannot be ordered.
        assertThatThrownBy(() ->
                orderItemCalculator.calculateAndBuildItems(List.of(itemRequest), mockOrder
                ))
                .isInstanceOf(ResourceUnavailableException.class)
                .hasMessageContaining("currently out of stock");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when modifier does not exist")
    void calculateAndBuildItems_ModifierNotFound_ThrowsException() {
        OrderRequestDTO.OrderItemRequest itemRequest =
                new OrderRequestDTO.OrderItemRequest();
        itemRequest.setMenuItemId(10L);
        itemRequest.setQuantity(1);
        itemRequest.setModifierIds(List.of(999L));

        when(menuItemRepository.findAllById(Set.of(10L))).thenReturn(List.of(mockMenuItem));

        // Simulates a search for modifier that does not exist.
        when(modifierRepository.findAllById(Set.of(999L))).thenReturn(List.of());

        // Verifies that an invalid modifier ID is rejected.
        assertThatThrownBy(() -> orderItemCalculator.calculateAndBuildItems(List.of(itemRequest), mockOrder
                ))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Modifier not found with ID: 999");
    }

    @Test
    @DisplayName("Should throw ResourceUnavailableException when modifier is unavailable")
    void calculateAndBuildItems_ModifierUnavailable_ThrowsException() {
       // Marks the selected modifier as unavailable.
        mockModifier.setAvailable(false);

        OrderRequestDTO.OrderItemRequest itemRequest =
                new OrderRequestDTO.OrderItemRequest();
        itemRequest.setMenuItemId(10L);
        itemRequest.setQuantity(1);
        itemRequest.setModifierIds(List.of(100L));

        when(menuItemRepository.findAllById(Set.of(10L))).thenReturn(List.of(mockMenuItem));
        when(modifierRepository.findAllById(Set.of(100L))).thenReturn(List.of(mockModifier));

        // Verifies that unavailable modifiers cannot be selected.
        assertThatThrownBy(() -> orderItemCalculator.calculateAndBuildItems(List.of(itemRequest), mockOrder
                ))
                .isInstanceOf(ResourceUnavailableException.class)
                .hasMessageContaining(
                        "Modifier 'Extra Shot' is currently unavailable"
                );
    }

    @Test
    @DisplayName("Should reject an order that does not meet the required modifier group minimum")
    void calculateAndBuildItems_BelowMinimumSelections_ThrowsException() {
        OrderRequestDTO.OrderItemRequest itemRequest =
                new OrderRequestDTO.OrderItemRequest();
        itemRequest.setMenuItemId(10L);
        itemRequest.setQuantity(1);
        itemRequest.setModifierIds(List.of());

        when(menuItemRepository.findAllById(Set.of(10L))).thenReturn(List.of(mockMenuItem));

        // Verifiers that required modifier groups meet their minimum selection.
        assertThatThrownBy(() -> orderItemCalculator.calculateAndBuildItems(List.of(itemRequest), mockOrder
                ))
                .isInstanceOf(ResourceUnavailableException.class)
                .hasMessageContaining(
                        "Modifier group 'Size / Add-ons' requires at least 1 selection(s)."
                );
    }

    @Test
    @DisplayName("Should reject selections exceeding the modifier group maximum")
    void calculateAndBuildItems_ExceedsMaximumSelections_ThrowsException() {
        // Creates two additional modifiers in the same modifier group.
        Modifier mockModifier2 = new Modifier();
        mockModifier2.setId(101L);
        mockModifier2.setName("Syrup Pump");
        mockModifier2.setPriceModifier(new BigDecimal("3.00"));
        mockModifier2.setAvailable(true);
        mockModifier2.setModifierGroup(mockModifierGroup);

        Modifier mockModifier3 = new Modifier();
        mockModifier3.setId(102L);
        mockModifier3.setName("Soy Milk");
        mockModifier3.setPriceModifier(new BigDecimal("4.00"));
        mockModifier3.setAvailable(true);
        mockModifier3.setModifierGroup(mockModifierGroup);

        OrderRequestDTO.OrderItemRequest itemRequest =
                new OrderRequestDTO.OrderItemRequest();
        itemRequest.setMenuItemId(10L);
        itemRequest.setQuantity(1);
        itemRequest.setModifierIds(List.of(100L, 101L, 102L));

        when(menuItemRepository.findAllById(Set.of(10L))).thenReturn(List.of(mockMenuItem));
        when(modifierRepository.findAllById(Set.of(100L, 101L, 102L))).thenReturn(List.of(mockModifier, mockModifier2, mockModifier3));

        // Verifies that selecting more than two modifiers is rejected.
        assertThatThrownBy(() ->
                orderItemCalculator.calculateAndBuildItems(
                        List.of(itemRequest), mockOrder
                ))
                .isInstanceOf(ResourceUnavailableException.class)
                .hasMessageContaining(
                        "Modifier group 'Size / Add-ons' allows a maximum of 2 selection(s)."
                );
    }

    @Test
    @DisplayName("Should default null item and modifier prices to zero")
    void calculateAndBuildItems_NullPrices_DefaultsToZero() {
        // Sets both prices to null to test the calculator's fallback behaviour.
        mockMenuItem.setPrice(null);
        mockModifier.setPriceModifier(null);

        OrderRequestDTO.OrderItemRequest itemRequest =
                new OrderRequestDTO.OrderItemRequest();
        itemRequest.setMenuItemId(10L);
        itemRequest.setQuantity(3);
        itemRequest.setModifierIds(List.of(100L));

        when(menuItemRepository.findAllById(Set.of(10L))).thenReturn(List.of(mockMenuItem));
        when(modifierRepository.findAllById(Set.of(100L))).thenReturn(List.of(mockModifier));

        OrderItemCalculator.CalculationResult result = orderItemCalculator.calculateAndBuildItems(List.of(itemRequest), mockOrder);

        // Verifies that null prices are treated as zero in the calculation.
        assertThat(result.totalPrice()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.orderItems().get(0).getQuantity()).isEqualTo(3);
    }
}
