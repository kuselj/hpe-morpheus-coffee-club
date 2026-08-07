package com.hpe.morpheus.coffeeclub.repository;

import com.hpe.morpheus.coffeeclub.entity.CoffeeOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CoffeeOrderRepository extends JpaRepository<CoffeeOrder, Long> {

    @Query("select max(o.orderDate) from CoffeeOrder o")
    Optional<LocalDate> findLatestOrderDate();

    List<CoffeeOrder> findByOrderDateOrderByIdAsc(LocalDate orderDate);

    /**
     * Full history, newest first. The dataset is a handful of rows per day for a single small
     * team, so loading it in one go keeps the fairness and pre-population rules readable.
     */
    @Query("select o from CoffeeOrder o order by o.orderDate desc, o.id desc")
    List<CoffeeOrder> findAllNewestFirst();
}
