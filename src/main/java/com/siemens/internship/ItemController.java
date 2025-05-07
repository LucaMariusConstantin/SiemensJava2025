package com.siemens.internship;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * REST controller for managing Item entities.
 * Provides endpoints for creating, retrieving, updating, deleting and processing items.
 */
@RestController
@RequestMapping("/api/items")
public class ItemController {

    @Autowired
    private ItemService itemService;

    /**
     * Retrieves all items from the database.
     *
     * @return HTTP 200 OK with the list of items
     */
    @GetMapping
    public ResponseEntity<List<Item>> getAllItems() {
        return new ResponseEntity<>(itemService.findAll(), HttpStatus.OK);
    }

    /**
     * Creates a new item after validating the request body.
     *
     * @param item the item to be created (validated)
     * @param result the validation result
     * @return HTTP 201 CREATED with the saved item if valid,
     *         otherwise HTTP 400 BAD REQUEST with validation errors
     */
    @PostMapping
    public ResponseEntity<?> createItem(@Valid @RequestBody Item item, BindingResult result) {
        if (result.hasErrors()) {
            List<String> errors = result.getFieldErrors().stream()
                    .map(error -> error.getField() + ": " + error.getDefaultMessage())
                    .toList();

            return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(itemService.save(item), HttpStatus.CREATED);
    }

    /**
     * Retrieves an item by its ID.
     *
     * @param id the ID of the item
     * @return HTTP 200 OK with the item if found, otherwise HTTP 204 NO CONTENT
     */
    @GetMapping("/{id}")
    public ResponseEntity<Item> getItemById(@PathVariable Long id) {
        return itemService.findById(id)
                .map(item -> new ResponseEntity<>(item, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NO_CONTENT));
    }

    /**
     * Updates an existing item by ID.
     *
     * @param id the ID of the item to update
     * @param item the item data to update
     * @return HTTP 201 CREATED with updated item if exists,
     *         otherwise HTTP 404 NOT FOUND
     */
    @PutMapping("/{id}")
    public ResponseEntity<Item> updateItem(@PathVariable Long id, @RequestBody Item item) {
        Optional<Item> existingItem = itemService.findById(id);
        if (existingItem.isPresent()) {
            item.setId(id);
            return new ResponseEntity<>(itemService.save(item), HttpStatus.CREATED);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Deletes an item by its ID.
     *
     * @param id the ID of the item to delete
     * @return HTTP 204 NO CONTENT if deleted
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        if (itemService.findById(id).isPresent()) {
            itemService.deleteById(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Processes all items asynchronously (e.g., setting their status to "PROCESSED").
     *
     * @return CompletableFuture with HTTP 200 OK and the list of processed items
     */
    @GetMapping("/process")
    public CompletableFuture<ResponseEntity<List<Item>>> processItems() {
        return itemService.processItemsAsync()
                .thenApply(ResponseEntity::ok);
    }

//    @GetMapping("/process")
//    public ResponseEntity<List<Item>> processItems() {
//        try {
//            List<Item> result = itemService.processItemsAsync().get(); // It blocks the execution until all items are processed
//            return new ResponseEntity<>(result, HttpStatus.OK);
//        } catch (InterruptedException | ExecutionException e) {
//            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//    }
}
