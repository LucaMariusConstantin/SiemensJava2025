package com.siemens.internship;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long> {

    /**
     * Retrieves all ids from database
     * @return a list of Long ids
     */
    @Query("SELECT id FROM Item")
    List<Long> findAllIds();
}
