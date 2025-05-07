package com.siemens.internship;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private ItemService itemService;

    private Item item1;
    private Item item2;

    @BeforeEach
    void setup() {
        item1 = new Item(1L, "item1", "desc1", "NEW", "user@example.com");
        item2 = new Item(2L, "item2", "desc2", "NEW", "test@example.com");
    }

    @Test
    void testFindAll() {
        List<Item> items = List.of(item1, item2);
        when(itemRepository.findAll()).thenReturn(items);

        List<Item> result = itemService.findAll();

        assertEquals(2, result.size());
        assertEquals("item1", result.get(0).getName());
        verify(itemRepository, times(1)).findAll();
    }

    @Test
    void testFindByIdFound() {
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item1));

        Optional<Item> result = itemService.findById(1L);

        assertTrue(result.isPresent());
        assertEquals("item1", result.get().getName());
    }

    @Test
    void testFindByIdNotFound() {
        when(itemRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Item> result = itemService.findById(99L);

        assertFalse(result.isPresent());
    }

    @Test
    void testSaveItem() {
        when(itemRepository.save(item1)).thenReturn(item1);

        Item saved = itemService.save(item1);

        assertEquals("item1", saved.getName());
        verify(itemRepository).save(item1);
    }

    @Test
    void testDeleteById() {
        itemService.deleteById(1L);

        verify(itemRepository).deleteById(1L);
    }

    @Test
    void testProcessItemsAsync() throws Exception {
        when(itemRepository.findAllIds()).thenReturn(List.of(1L, 2L));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item1));
        when(itemRepository.findById(2L)).thenReturn(Optional.of(item2));
        when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CompletableFuture<List<Item>> future = itemService.processItemsAsync();

        List<Item> processed = future.get(); // blocking call for test

        assertEquals(2, processed.size());
        assertTrue(processed.stream().allMatch(i -> i.getStatus().equals("PROCESSED")));

        verify(itemRepository, times(2)).findById(anyLong());
        verify(itemRepository, times(2)).save(any(Item.class));
    }
}
