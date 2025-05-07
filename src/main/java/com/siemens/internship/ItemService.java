package com.siemens.internship;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;


@Service
public class ItemService {
    @Autowired
    private ItemRepository itemRepository;

    // We must use thread-safe collections and primitives
    private final ConcurrentLinkedQueue<Item> processedItems = new ConcurrentLinkedQueue<>();
    private final AtomicInteger processedCount = new AtomicInteger(0);

    /**
     * Retrieves all items from the database.
     *
     * @return a list of all items
     */
    public List<Item> findAll() {
        return itemRepository.findAll();
    }

    /**
     * Retrieves an item by its id.
     *
     * @param id the id of the item
     * @return an Optional containing the item if found, or empty if not
     */
    public Optional<Item> findById(Long id) {
        return itemRepository.findById(id);
    }

    /**
     * Saves the given item to the database (create or update).
     *
     * @param item the item to save
     * @return the saved item
     */
    public Item save(Item item) {
        return itemRepository.save(item);
    }

    /**
     * Deletes the item with the specified ID from the database.
     *
     * @param id the id of the item to delete
     */
    public void deleteById(Long id) {
        itemRepository.deleteById(id);
    }


    /*
     * Your Tasks
     * Identify all concurrency and asynchronous programming issues in the code
     * Fix the implementation to ensure:
     * All items are properly processed before the CompletableFuture completes
     * Thread safety for all shared state
     * Proper error handling and propagation
     * Efficient use of system resources
     * Correct use of Spring's @Async annotation
     * Add appropriate comments explaining your changes and why they fix the issues
     * Write a brief explanation of what was wrong with the original implementation
     *
     * Hints
     * Consider how CompletableFuture composition can help coordinate multiple async operations
     * Think about appropriate thread-safe collections
     * Examine how errors are handled and propagated
     * Consider the interaction between Spring's @Async and CompletableFuture
     */


    /**
     * Asynchronously processes all items retrieved from the database.
     *
     * <p>Each item is fetched by ID, marked as "PROCESSED", and saved back to the repository.
     * The method launches all operations in parallel using CompletableFuture and ensures:
     * <ul>
     *     <li>Thread safety through use of ConcurrentLinkedQueue and AtomicInteger</li>
     *     <li>Proper handling of InterruptedException by re-setting the interrupt flag</li>
     *     <li>Graceful error propagation by wrapping exceptions in RuntimeException</li>
     *     <li>That the returned CompletableFuture completes only after all items are processed</li>
     * </ul>
     *
     * @return a CompletableFuture that completes with an immutable list of all processed items
     * @throws RuntimeException if processing fails for any item
     */
    @Async
    public CompletableFuture<List<Item>> processItemsAsync() {

        List<Long> itemIds = itemRepository.findAllIds();

        List<CompletableFuture<Void>> futures = itemIds.stream().map(
                // Launch async task to process each item by id
                id->CompletableFuture.runAsync(()-> {
                    try {
                        Thread.sleep(100);

                        Optional<Item> optionalItem = itemRepository.findById(id);
                        if (optionalItem.isPresent()) {
                            Item item = optionalItem.get();
                            item.setStatus("PROCESSED");
                            itemRepository.save(item);
                            // Because we are using thread-safe collections (ConcurrentLinkedQueue) and atomic primitives (AtomicInteger),
                            // concurrent access to processedItems and processedCount is safe and will not lead to race conditions.
                            processedItems.add(item);
                            processedCount.incrementAndGet();
                        }
                    }
                    catch (InterruptedException e)
                    {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Thread interrupted", e);
                    }
                    catch(Exception e)
                    {
                        throw new RuntimeException("Error processing item with id " + id, e);
                    }
                })).toList();

        // We wait until all items have been processed using CompletableFuture.allOf, and then return an immutable copy of processedItems
        // to prevent external modifications.
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> List.copyOf(processedItems));
    }

//    @Async
//    public List<Item> processItemsAsync() {
//
//        List<Long> itemIds = itemRepository.findAllIds();
//
//        for (Long id : itemIds) {
//            CompletableFuture.runAsync(() -> {
//                try {
//                    Thread.sleep(100);
//
//                    Item item = itemRepository.findById(id).orElse(null);
//                    if (item == null) {
//                        return;
//                    }
//
//                    processedCount++;
//
//                    item.setStatus("PROCESSED");
//                    itemRepository.save(item);
//                    processedItems.add(item);
//
//                } catch (InterruptedException e) {
//                    System.out.println("Error: " + e.getMessage());
//                }
//            }, executor);
//        }
//
//        return processedItems;
//    }

}

