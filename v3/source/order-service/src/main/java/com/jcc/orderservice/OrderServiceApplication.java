package com.jcc.orderservice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Stream;

@SpringBootApplication
public class OrderServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(OrderServiceApplication.class, args);
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

@RestController
class ServiceInstanceRestController {

	@Autowired
	private DiscoveryClient discoveryClient;

	@RequestMapping("/service-instances/{applicationName}")
	public List<ServiceInstance> serviceInstancesByApplicationName(
			@PathVariable String applicationName) {
		return this.discoveryClient.getInstances(applicationName);
	}
}
