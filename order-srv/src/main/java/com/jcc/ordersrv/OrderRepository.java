package com.jcc.ordersrv;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

import java.util.Collection;

@RepositoryRestResource
public interface OrderRepository extends JpaRepository<Order, Long> {

    @RestResource(path = "by-name")
    Collection<Order> findByOrderName(@Param("on")  String on);
}
