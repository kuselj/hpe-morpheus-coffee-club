package com.hpe.morpheus.coffeeclub.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * A single coworker's line on a single day's group coffee order.
 *
 * <p>One row is written per person per order date. The person who was selected to pay for the
 * group on that date carries the whole group total in {@code totalPaidToday}; everyone else
 * carries {@code 0.00}. The lifetime fairness balance for a person is therefore
 * {@code SUM(total_paid_today) - SUM(price)} across all of their rows.</p>
 */
@Entity
@Table(name = "hpe_morpheus_coffee_club")
public class CoffeeOrder {

    /** Surrogate key. Not part of the coworker data itself. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @Column(name = "name", nullable = false, length = 60)
    private String name;

    @Column(name = "drink", nullable = false, length = 80)
    private String drink;

    /** Cost of this person's own drink on this date. */
    @Column(name = "price", nullable = false, precision = 8, scale = 2)
    private BigDecimal price;

    /** Amount this person paid on behalf of the group on this date. Zero unless they were the payer. */
    @Column(name = "total_paid_today", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPaidToday;

    /** {@code "Y"} when the person was removed from the club on this date, otherwise {@code "N"}. */
    @Column(name = "is_removed", nullable = false, length = 1)
    private String isRemoved;

    protected CoffeeOrder() {
        // Required by JPA.
    }

    public CoffeeOrder(LocalDate orderDate,
                       String name,
                       String drink,
                       BigDecimal price,
                       BigDecimal totalPaidToday,
                       String isRemoved) {
        this.orderDate = orderDate;
        this.name = name;
        this.drink = drink;
        this.price = price;
        this.totalPaidToday = totalPaidToday;
        this.isRemoved = isRemoved;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDate orderDate) {
        this.orderDate = orderDate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDrink() {
        return drink;
    }

    public void setDrink(String drink) {
        this.drink = drink;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getTotalPaidToday() {
        return totalPaidToday;
    }

    public void setTotalPaidToday(BigDecimal totalPaidToday) {
        this.totalPaidToday = totalPaidToday;
    }

    public String getIsRemoved() {
        return isRemoved;
    }

    public void setIsRemoved(String isRemoved) {
        this.isRemoved = isRemoved;
    }

    /** Convenience view over the {@code "Y"}/{@code "N"} flag. */
    public boolean isRemovedFlagSet() {
        return "Y".equalsIgnoreCase(isRemoved);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CoffeeOrder that)) {
            return false;
        }
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "CoffeeOrder{orderDate=%s, name='%s', drink='%s', price=%s, totalPaidToday=%s, isRemoved='%s'}"
                .formatted(orderDate, name, drink, price, totalPaidToday, isRemoved);
    }
}
