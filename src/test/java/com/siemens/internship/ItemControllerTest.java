package com.siemens.internship;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ItemController.class)
public class ItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ItemService itemService;

    @Autowired
    private ObjectMapper objectMapper;

    private final Item item1 = new Item(1L, "item1", "desc", "NEW", "user@example.com");

    @Test
    void getAllItems_shouldReturnList() throws Exception {
        when(itemService.findAll()).thenReturn(List.of(item1));

        mockMvc.perform(get("/api/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("item1"));
    }

    @Test
    void getItemById_shouldReturnItem() throws Exception {
        when(itemService.findById(1L)).thenReturn(Optional.of(item1));

        mockMvc.perform(get("/api/items/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("item1"));
    }

    @Test
    void getItemById_shouldReturnNoContent_whenNotFound() throws Exception {
        when(itemService.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/items/999"))
                .andExpect(status().isNoContent());
    }

    @Test
    void createItem_shouldReturnCreated_whenValid() throws Exception {
        when(itemService.save(any(Item.class))).thenReturn(item1);

        mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("user@example.com"));
    }

    @Test
    void createItem_shouldReturnBadRequest_whenInvalidEmail() throws Exception {
        Item invalidItem = new Item(null, "item1", "desc", "NEW", "invalid");

        mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidItem)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateItem_shouldReturnCreated_whenExists() throws Exception {
        when(itemService.findById(1L)).thenReturn(Optional.of(item1));
        when(itemService.save(any(Item.class))).thenReturn(item1);

        mockMvc.perform(put("/api/items/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item1)))
                .andExpect(status().isCreated());
    }

    @Test
    void updateItem_shouldReturnNotFound_whenNotExists() throws Exception {
        when(itemService.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/items/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item1)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteItem_shouldReturnNoContent() throws Exception {
        when(itemService.findById(1L)).thenReturn(Optional.of(item1));
        doNothing().when(itemService).deleteById(1L);

        mockMvc.perform(delete("/api/items/1"))
                .andExpect(status().isNoContent());
    }


    @Test
    void processItems_shouldReturnProcessedItems() throws Exception {
        when(itemService.processItemsAsync())
                .thenReturn(CompletableFuture.completedFuture(List.of(item1)));

        MvcResult mvcResult = mockMvc.perform(get("/api/items/process"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult)) // we use asyncDispatch because we need CompletableFuture being completed
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("NEW"));
    }
}
