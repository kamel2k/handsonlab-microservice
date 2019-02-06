package com.jcc.orderclient;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

@EnableCircuitBreaker
@EnableZuulProxy
@EnableDiscoveryClient
@SpringBootApplication
public class OrderClientApplication {

	@Bean
	@LoadBalanced
	RestTemplate restTemplate() {
		return new RestTemplate();
	}

	public static void main(String[] args) {
		SpringApplication.run(OrderClientApplication.class, args);
	}

}

@RestController
@RequestMapping("/orders")
class OrderApiGateway {

	private RestTemplate restTemplate;

	@Autowired
	public OrderApiGateway(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}

	public Collection <String> fallback() {
		return new ArrayList<>();
	}

	@HystrixCommand(fallbackMethod = "fallback")
	@RequestMapping (method = RequestMethod.GET, value = "/names")
	public Collection<String> names() {

		ParameterizedTypeReference<Resources<Order>> ptr = new ParameterizedTypeReference<Resources<Order>>() {};

		ResponseEntity<Resources<Order>> responseEntity = this.restTemplate.exchange("http://order-service/orders", HttpMethod.GET, null, ptr);

		return responseEntity
				.getBody()
				.getContent()
				.stream()
				.map(Order::getOrderName)
				.collect(Collectors.toList());
	}
}


class Order {
	private String orderName;

	public String getOrderName() {
		return orderName;
	}
}