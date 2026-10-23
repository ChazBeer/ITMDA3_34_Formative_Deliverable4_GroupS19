package org.example.cafeteria_ordering_backend.service;

import org.example.cafeteria_ordering_backend.entity.MenuItemModifierGroup;
import org.example.cafeteria_ordering_backend.entity.MenuItems;
import org.example.cafeteria_ordering_backend.entity.Modifier;
import org.example.cafeteria_ordering_backend.entity.ModifierGroup;
import org.example.cafeteria_ordering_backend.exception.BusinessException;
import org.example.cafeteria_ordering_backend.exception.ResourceNotFoundException;
import org.example.cafeteria_ordering_backend.repository.MenuItemRepository;
import org.example.cafeteria_ordering_backend.repository.ModifierGroupRepository;
import org.example.cafeteria_ordering_backend.repository.ModifierRepository;
import org.example.cafeteria_ordering_backend.service.implementation.MenuCustomizationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MenuCustomizationServiceImplTest {

    @Mock
    private ModifierGroupRepository groupRepository;

    @Mock
    private ModifierRepository modifierRepository;

    @Mock
    private MenuItemRepository menuItemRepository;

    // Inject mocks into service being tested
    @InjectMocks
    private MenuCustomizationServiceImpl customizationService;

    // Shared test entities
    private MenuItems mockMenuItem;
    private ModifierGroup mockModifierGroup;
    private Modifier mockModifier;

    @BeforeEach
    void setUp () {

        // Initialize menu item with modifier groups and modifier
        mockMenuItem = new MenuItems();
        mockMenuItem.setMenuItemId(1L);
        mockMenuItem.setName("Cappuccino");

        mockMenuItem.setMenuItemModifierGroups(new HashSet<>());

        // Modifier group for milk options
        mockModifierGroup = new ModifierGroup();
        mockModifierGroup.setId(10L);
        mockModifierGroup.setName("Milk Options");

        // Modifier belonging to above milk options
        mockModifier = new Modifier();
        mockModifier.setId(100L);
        mockModifier.setName("Oat Milk");
        mockModifier.setPriceModifier(new BigDecimal("5.00"));
    }

    @Nested
    @DisplayName("Create Modifier Group Tests")
    class CreatedModifierGroupTests {

        @Test
        @DisplayName("Should successfully create modifier group and establish junction link with correct display order")
        void createModifierGroup_Success () {

            // Mock existing menu item and repo save
            Long menuItemId = 1L;

            when(menuItemRepository.findById(menuItemId)).thenReturn(Optional.of(mockMenuItem));
            when(groupRepository.save(any(ModifierGroup.class))).thenReturn(mockModifierGroup);
            when(menuItemRepository.save(any(MenuItems.class))).thenReturn(mockMenuItem);

            // Create modifier group and link it to menu item
            ModifierGroup result = customizationService.createModifierGroup(menuItemId, mockModifierGroup);

            // Check created modifier contains relevant info
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(10L);
            assertThat(result.getName()).isEqualTo("Milk Options");

            // Ensure the bridge entity links menu item to modifier group
            assertThat(mockMenuItem.getMenuItemModifierGroups()).hasSize(1);
            MenuItemModifierGroup link = mockMenuItem.getMenuItemModifierGroups().iterator().next();
            assertThat(link.getMenuItem()).isEqualTo(mockMenuItem);
            assertThat(link.getModifierGroup()).isEqualTo(mockModifierGroup);
            assertThat(link.getDisplayOrder()).isEqualTo(1); // First group linked = display order 1

            // Does the menu item and its modifiers get called as expected
            verify(menuItemRepository, times(1)).findById(menuItemId);
            verify(groupRepository, times(1)).save(mockModifierGroup);
            verify(menuItemRepository, times(1)).save(mockMenuItem);
        }

        @Test
        @DisplayName("Should increment display order sequentially when adding subsequent modifier groups")
        void createModifierGroup_IncrementDisplayOrder () {

            // Prep menu item with 1 modifier group
            Long menuItemId = 1L;

            // Pre-populate existing group link
            MenuItemModifierGroup existingLink = new MenuItemModifierGroup();
            existingLink.setDisplayOrder(1);

            mockMenuItem.getMenuItemModifierGroups().add(existingLink);

            // Prep second modifier group to be linked to item
            ModifierGroup secondGroup = new ModifierGroup();
            secondGroup.setId(11L);
            secondGroup.setName("Syrup Flavors");

            when(menuItemRepository.findById(menuItemId)).thenReturn(Optional.of(mockMenuItem));
            when(groupRepository.save(secondGroup)).thenReturn(secondGroup);

            // Add second modifier group
            customizationService.createModifierGroup(menuItemId, secondGroup);

            // Verify that both mod groups are linked
            assertThat(mockMenuItem.getMenuItemModifierGroups()).hasSize(2);

            // Check new link is in the correct display order
            MenuItemModifierGroup secondLink = mockMenuItem.getMenuItemModifierGroups()
                    .stream()
                    .filter(link -> link.getDisplayOrder() == 2)
                    .findFirst()
                    .orElse(null);

            assertThat(secondLink).isNotNull();
            assertThat(secondLink.getDisplayOrder()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should throw Runtime Exception when menu item ID not found")
        void createModifierGroup_MenuItemNotFound_ThrowsException () {

            // New menuitem ID that doesn't exist
            Long invalidId = 999L;
            when(menuItemRepository.findById(invalidId)).thenReturn(Optional.empty());

            // Check creation fails with correct exception
            assertThatThrownBy(() -> customizationService.createModifierGroup(invalidId, mockModifierGroup))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Menu item not found with id: " + invalidId);

            // Ensure neither saves
            verify(groupRepository, never()).save(any());
            verify(menuItemRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Create Modifier Tests")
    class CreatedModifierTests {
        @Test
        @DisplayName("Should successfully attach modifier to group and save")
        void createModifier_Success () {

            // ID for modifier group for modifier to join
            Long groupId = 10L;

            when(groupRepository.findById(groupId)).thenReturn(Optional.of(mockModifierGroup));
            when(modifierRepository.save(mockModifier)).thenReturn(mockModifier);

            // Crete new modifier
            Modifier result = customizationService.createModifier(groupId, mockModifier);

            // Check modifier is saved with correct group
            assertThat(result).isNotNull();
            assertThat(result.getModifierGroup()).isEqualTo(mockModifierGroup);
            assertThat(result.getName()).isEqualTo("Oat Milk");

            // Ensure group is retrieved with new modifier
            verify(groupRepository, times(1)).findById(groupId);
            verify(modifierRepository, times(1)).save(mockModifier);
        }

        @Test
        @DisplayName("Should throw Runtime Exception when group ID is not found")
        void createModifier_GroupNotFound_ThrowsException () {

            // Sim a modifier non-existent group
            Long invalidGroupId = 999L;

            when(groupRepository.findById(invalidGroupId)).thenReturn(Optional.empty());

            // Check missing group is reported
            assertThatThrownBy(() -> customizationService.createModifier(invalidGroupId, mockModifier))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Modifier group not found with id: " + invalidGroupId);

            // Ensure no modifier is saved
            verify(modifierRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Get customization by menu item tests")
    class GetCustomizationByMenuItemTests {

        @Test
        @DisplayName("Should return modifier group sorted by display order")
        void getCustomizationsByMenuItem_ReturnsSortedGroups () {

            // Create 2 modifier groups with different display orders
            Long menuItemId = 1L;

            ModifierGroup groupA = new ModifierGroup();
            groupA.setId(10L);
            groupA.setName("Size");

            ModifierGroup groupB = new ModifierGroup();
            groupB.setId(11L);
            groupB.setName("Syrups");

            // Add links in backwards order to ensure it functions
            MenuItemModifierGroup link1 = new MenuItemModifierGroup();
            link1.setModifierGroup(groupB);
            link1.setDisplayOrder(2);

            MenuItemModifierGroup link2 = new MenuItemModifierGroup();
            link2.setModifierGroup(groupA);
            link2.setDisplayOrder(1);

            mockMenuItem.getMenuItemModifierGroups().add(link1);
            mockMenuItem.getMenuItemModifierGroups().add(link2);

            when(menuItemRepository.findById(menuItemId)).thenReturn(Optional.of(mockMenuItem));

            // Get groups associate with the item
            List<ModifierGroup> results = customizationService.getCustomizationsByMenuItem(menuItemId);

            // Verify groups are returned in correct display order
            assertThat(results).hasSize(2);
            assertThat(results.get(0)).isEqualTo(groupA); // Display order 1 comes first
            assertThat(results.get(1)).isEqualTo(groupB); // Display order 2 comes second

            // Check menu item is retunred only once
            verify(menuItemRepository, times(1)).findById(menuItemId);
        }

        @Test
        @DisplayName("Should return empty list when menu item has no customizations attached")
        void getCustomizationsByMenuItem_ReturnsEmptyList () {

            // Use menu item with no modifier group
            Long menuItemId = 1L;

            when(menuItemRepository.findById(menuItemId)).thenReturn(Optional.of(mockMenuItem));

            // Get menu items customizations
            List<ModifierGroup> results = customizationService.getCustomizationsByMenuItem(menuItemId);

            // Check there are none
            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("Should throw RuntimeException when menu item ID is not found")
        void getCustomizationsByMenuItem_NotFound_ThrowsException () {

            // Sim a non-existent menu item ID
            Long invalidId = 999L;

            when(menuItemRepository.findById(invalidId)).thenReturn(Optional.empty());

            // Ensure the service throws correct exception, and message
            assertThatThrownBy(() -> customizationService.getCustomizationsByMenuItem(invalidId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Menu item not found with id: " + invalidId);
        }
    }

    @Nested
    @DisplayName("Update Modifier Group Tests")
    class UpdateModifierGroupTests {

        @Test
        @DisplayName("Should successfully update modifier group details")
        void updateModifierGroup_Success () {

            // Prep the updated modifier group
            ModifierGroup updateInfo = new ModifierGroup();
            updateInfo.setName("Updated Milk Options");
            updateInfo.setMinSelections(1);
            updateInfo.setMaxSelections(2);
            updateInfo.setAvailable(false);
            updateInfo.setIsRequired(true);

            when(groupRepository.findById(10L)).thenReturn(Optional.of(mockModifierGroup));
            when(groupRepository.save(any(ModifierGroup.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Apply changes to existing group
            ModifierGroup updated = customizationService.updateModifierGroup(10L, updateInfo);

            // Verify updated fields
            assertThat(updated).isNotNull();
            assertThat(updated.getName()).isEqualTo("Updated Milk Options");
            assertThat(updated.getMinSelections()).isEqualTo(1);
            assertThat(updated.getMaxSelections()).isEqualTo(2);
            assertThat(updated.getAvailable()).isFalse();
            assertThat(updated.getIsRequired()).isTrue();

            // Verify that existing group is retrieved and saved once.
            verify(groupRepository, times(1)).findById(10L);
            verify(groupRepository, times(1)).save(mockModifierGroup);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when modifier group ID for update is not found")
        void updateModifierGroup_NotFound_ThrowsException () {

            // Simulate update req for missing modifier group
            ModifierGroup updateInfo = new ModifierGroup();
            when(groupRepository.findById(999L)).thenReturn(Optional.empty());

            // Ensure service reports missing group
            assertThatThrownBy(() -> customizationService.updateModifierGroup(999L, updateInfo))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Modifier group not found with ID: 999");

            // Make sure no save happens
            verify(groupRepository, times(1)).findById(999L);
            verify(groupRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Delete Modifier Group Tests")
    class DeleteModifierGroupTests {

        @Test
        @DisplayName("Should successfully delete modifier group by ID")
        void deleteModifierGroup_Success () {

            // Config the repo to return existing modifier group
            when(groupRepository.findById(10L)).thenReturn(Optional.of(mockModifierGroup));
            doNothing().when(groupRepository).delete(mockModifierGroup);

            // Ensure that deletion completes without throwing exception
            org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> customizationService.deleteModifierGroup(10L));

            // Verify the Get and deletion works
            verify(groupRepository, times(1)).findById(10L);
            verify(groupRepository, times(1)).delete(mockModifierGroup);
        }

        @Test
        @DisplayName("Should throw BusinessException when deleting modifier group tied to past orders")
        void deleteModifierGroup_ForeignKeyViolation_ThrowsException () {

            // Simulate a DB constraint violation during a Delete event
            when(groupRepository.findById(10L)).thenReturn(Optional.of(mockModifierGroup));
            doThrow(new org.springframework.dao.DataIntegrityViolationException("Foreign key violation"))
                    .when(groupRepository).delete(mockModifierGroup);

            // Verify that the DB exception is translated into the correct exception
            assertThatThrownBy(() -> customizationService.deleteModifierGroup(10L))
                    .isInstanceOf(BusinessException.class) // <--- Updated from ResponseStatusException
                    .hasMessageContaining("Cannot delete this modifier group because one or more of its options have been purchased");

            // Ensure existing group is retrieved and delete once each
            verify(groupRepository, times(1)).findById(10L);
            verify(groupRepository, times(1)).delete(mockModifierGroup);
        }
    }

    @Nested
    @DisplayName("Update Modifier Tests")
    class UpdateModifierTests {

        @Test
        @DisplayName("Should successfully update modifier details")
        void updateModifier_Success () {

            // Prepare updated modifier details
            Modifier updateInfo = new Modifier();
            updateInfo.setName("Almond Milk");
            updateInfo.setPriceModifier(new BigDecimal("7.50"));
            updateInfo.setAvailable(false);

            when(modifierRepository.findById(100L)).thenReturn(Optional.of(mockModifier));
            when(modifierRepository.save(any(Modifier.class))).thenAnswer(invocation ->
                    invocation.getArgument(0));

            Modifier updated = customizationService.updateModifier(100L, updateInfo);

            // Ensure modifier contains updated values
            assertThat(updated).isNotNull();
            assertThat(updated.getName()).isEqualTo("Almond Milk");
            assertThat(updated.getPriceModifier()).isEqualByComparingTo(new BigDecimal("7.50"));
            assertThat(updated.getAvailable()).isFalse();

            verify(modifierRepository, times(1)).findById(100L);
            verify(modifierRepository, times(1)).save(mockModifier);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when modifier ID for update is not found")
        void updateModifier_NotFound_ThrowsException () {

            // Mock an update req for non-existent modifier
            Modifier updateInfo = new Modifier();
            when(modifierRepository.findById(999L)).thenReturn(Optional.empty());

            // Check that correct exception is thrown
            assertThatThrownBy(() -> customizationService.updateModifier(999L, updateInfo))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Modifier not found with ID: 999");

            // Verify no save happens
            verify(modifierRepository, times(1)).findById(999L);
            verify(modifierRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Delete Modifier Tests")
    class DeleteModifierTests {

        @Test
        @DisplayName("Should successfully delete modifier by ID")
        void deleteModifier_Success () {

            // Setup repo to return existing modifier
            when(modifierRepository.findById(100L)).thenReturn(Optional.of(mockModifier));
            doNothing().when(modifierRepository).delete(mockModifier);

            // Verify deletion completes without throwing an exception
            org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> customizationService.deleteModifier(100L));

            // Verify fetch of modifier and deletion occurs
            verify(modifierRepository, times(1)).findById(100L);
            verify(modifierRepository, times(1)).delete(mockModifier);
        }

        @Test
        @DisplayName("Should throw ResponseStatusException when deleting modifier tied to past orders")
        void deleteModifier_ForeignKeyViolation_ThrowsException () {

            // Setup DB constraint violation when deleting a purchase modifier
            when(modifierRepository.findById(100L)).thenReturn(Optional.of(mockModifier));
            doThrow(new org.springframework.dao.DataIntegrityViolationException("Foreign key violation"))
                    .when(modifierRepository).delete(mockModifier);

            // Check service returns the correct exception
            assertThatThrownBy(() -> customizationService.deleteModifier(100L))
                    .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                    .hasMessageContaining("Cannot delete this modifier because it has been purchased");

            verify(modifierRepository, times(1)).findById(100L);
            verify(modifierRepository, times(1)).delete(mockModifier);
        }
    }
}
