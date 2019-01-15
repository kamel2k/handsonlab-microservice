package com.jcc.ordersrv;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

import java.util.stream.Stream;

@SpringBootApplication
public class OrderSrvApplication {

	public static void main(String[] args) {
		SpringApplication.run(OrderSrvApplication.class, args);
	}
}

@Component
class DataCLR implements CommandLineRunner{

	private final OrderRepository orderRepository;

	@Autowired
	public DataCLR(OrderRepository orderRepository) {
		this.orderRepository = orderRepository;
	}

	@Override
	public void run(String... args) throws Exception {

		Stream.of("kamel", "karim", "joe", "jane").forEach(name -> orderRepository.save(new Order(name)));

		orderRepository.findAll().forEach(System.out::println);
	}
}

