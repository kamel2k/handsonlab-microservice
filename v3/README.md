# Handsonlab Microservices with Spring frameworks

# Table of Contents
1. [Prerequesites](#prerequesites)
2. [Build configuration server](#build-configuration-server)
3. [Setup Service Registration and Discovery with Spring Cloud Netflix Eureka](#setup-service-registration-and-discovery-with-spring-cloud-netflix-eureka)
4. [Use Case Order service](#use-case-order-service)
5. [Production Ready with Actuator](#production-ready-with-actuator)
6. [Develop a client order service](#develop-a-client-order-service)
7. [Resiliency with circuit breaker](#resiliency-with-circuit-breaker)
8. [Distributed tracing with zipkin](#distributed-tracing-with-zipkin)
9. [Deployment with Docker](#deployment-with-docker)


## Prerequesites

You must install all these tools required for this workshop.
For Mac OS users who have Homebrew execute the following commands

* Install openjdk8
```
brew cask install adoptopenjdk8
```

* Install Maven
```
brew install maven@3.5
```

If you need to have maven@3.5 first in your PATH run:
```
echo 'export PATH="/usr/local/opt/maven@3.5/bin:$PATH"' >> ~/.zshrc
```

* Install Docker

Follow the instructions [here](https://hub.docker.com/editions/community/docker-ce-desktop-mac) for MacOS users

* Install your preferred IDE

IntelliJ / Eclipse

### Source code

All source code is available in github at [handsonlab-microservice](https://github.com/kamel2k/handsonlab-microservice)

to clone the application
```
git clone https://github.com/kamel2k/handsonlab-microservice.git
```

### Applications

config-service : 9999
order-service : 8080
discovery-service : 8761

### List all used spring frameworks

## Build configuration server

Every application has a file for configuration application.properties. witch container server.port, etc. It is an environment concern, it can be different from environment to another. We must ensure that whanever chosen value, we must package the application only once.
Spring Cloud Config resolve this limitation.  

### Concept
Spring Cloud Config provides server and client-side support for externalized configuration in a distributed system. With the Config Server you have a central place to manage external properties for applications across all environments. [source](https://spring.io/projects/spring-cloud-config)

### Features
Spring Cloud Config Server features:

* HTTP, resource-based API for external configuration (name-value pairs, or equivalent YAML content)
* Encrypt and decrypt property values (symmetric or asymmetric)
* Embeddable easily in a Spring Boot application using @EnableConfigServer

Config Client features (for Spring applications):

* Bind to the Config Server and initialize Spring Environment with remote property sources
* Encrypt and decrypt property values (symmetric or asymmetric)

FYI: Concept and features are taken from Spring website : [source](https://spring.io/projects/spring-cloud-config)

### Config service

With spring initializr we can bootstrap a config server easily.
Go to [Spring Initializr website](https://start.spring.io/) and then generate a maven project named **config-service**

> **Spring boot** : 2.1.2  
> **Group** : com.jcc  
> **Artifact** : config-service   
> **Dependencies** : Config Server  
> **Java version** : 1.8  

Compile project with maven.

Inside the main class add this annotation **@EnableConfigServer** to enable config server
Make application listen on port 9999 with server.port=${PORT:9999} into **application.properties**.  
Activate the native profile in the Config Server that does not use Git but loads the config files from the local classpath.
In production you must externalize configuration with Git and use the parameter **spring.cloud.config.server.git.uri**. For more details go to [Spring website](https://cloud.spring.io/spring-cloud-config/multi/multi__spring_cloud_config_server.html).  
Here's the content of the **application.properties**
```
server.port=${PORT:9999}
spring.profiles.active=native
```

Copy all configuration files for microservices inside /src/main/resources/config

Launch **config-service** with IntelliJ or Maven
```
mvn clean package
java -jar target/config-service-0.0.1-SNAPSHOT.jar
```

Go to http://localhost:9999/order-service/default  
**config-service** must respond with the given configuration file for **order-service**


## Setup Service Registration and Discovery with Spring Cloud Netflix Eureka

**TODO** detailler la problematique et introduire eureka server.

**Step1: bootstrap discovery-service project**
Use Spring initializr to bootstrap an eureka service.
Go to [Spring Initializr website](https://start.spring.io/) and then generate a maven project named **discovery-service**

> **Spring boot** : 2.1.2  
> **Group** : com.jcc  
> **Artifact** : discovery-service  
> **Dependencies** : Eureka Server, Config Client  
> **Java version** : 1.8  

Compile project with maven.

**Step2: Configure discovery-service to use config-service**  
Rename **application.properties** with **bootstrap.properties** and add these lines:
```
spring.application.name=discovery-service
spring.cloud.config.uri=http://localhost:9999
```

**Step3: Enable Eureka Server**  
In the main Java class for discovery-service add this annotation **@EnableEurekaServer** to enable Eureka Server

**Step4: verify that configuration for discovery-service is exposed correctly with the config server.**  
Go to
> http://localhost:9999/discovery-service/default

**Step5: Start discovery-service**  
Go to
> http://localhost:8761/

**Step6: View logs**  


## Use Case Order service

With spring initializr we can bootstrap a spring application very easy.
Go to [Spring Initializr website](https://start.spring.io/) and then generate a maven project named **order-service**

> **Spring boot** : 2.1.2  
> **Group** : com.jcc  
> **Artifact** : order-service  
> **Dependencies** : jpa, h2, rest repository , actuator, config client, eureka discovery , Zipkin Client
TODO dependency deleted: Cloud Stream  
> **Java version** : 1.8  

Compile project with maven

> mvn clean install

### Model our service

Begin with creating a JPA entity named Order with id and orderName and annotate the class with **@Entity**, here the code
```java
package com.jcc.orderservice;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity(name = "t_order")
public class Order {

    @Id @GeneratedValue
    private Long id;

    private String orderName;

    public Order(String orderName) {
        this.orderName = orderName;
    }

    public Order() {
    }

    // Complete with getter and setter and toString method
}
```
Notice that the table name for order entity is called t_order because the keyword order is reserved in the sql language.

### Spring Data

We will create an interface **OrderRepository** that extends **JpaRepository** with Order in parameter and Long as primary key and then add the method findByOrderName(String on) into **OrderRepository**. Here's the code
```java
package com.jcc.orderservice;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Collection<Order> findByOrderName(String on);
}
```

View all methods implemented in **JpaRepository**

### Populate with some data
In the main class add DataCLR and inject OrderRepository the put some data in database and list all the content of the table
```java
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
```

### Configure order-service to use the config server

Each application that requires a configuration from **config-service** must define two properties **spring.cloud.config.name** and **spring.cloud.config.uri**

Rename application.properties to bootstrap.properties

Add these lines
```
spring.application.name=order-service
spring.cloud.config.uri=http://localhost:9999
```

Launch the application and go to
> http://loalhost:8080/orders/


### Add REST support
We use Spring mvc and Spring Data Rest to expose order endpoint. Add the annotation **@RepositoryRestRessource** to our repository and put **@RestRessource (path="by-name")** to method **findByOrderName**. Finally put the **Param("on")** on to the attribute.
Here the result
```java
package com.jcc.orderservice;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

import java.util.Collection;

@RepositoryRestResource
public interface OrderRepository extends JpaRepository<Order, Long> {

    @RestResource(path = "by-name")
    Collection<Order> findByOrderName(@Param("on") String on);
}
```

browse http://localhost:8080/orders/ to list all orders

and http://localhost:8080/orders/search/by-name?on=kamel to list orders based on the name

Notice that the API generated consist of an hypermedia API. For more information about HATEOAS visit https://fr.wikipedia.org/wiki/HATEOAS

## Production Ready with Actuator

Spring boot actuator adds several production grade services to your application with little effort on your part. We will add these services in our application.

Some actuator endpoint are activated by default such as **/actuator/health**, **actuator/info**, etc.

To enable all actuator endpoints you must add in configuration file
> management.endpoints.web.exposure.include=*

Explore some actuator endpoints and then browse info endpoint.

Developer can add some information inside this endpoint, such as application version, name, git commit, build date, etc. to identify the right version in production.

Here's a sample to add git commit information into the application
In pom.xml add the following plugin
```xml
<plugin>
   <groupId>pl.project13.maven</groupId>
   <artifactId>git-commit-id-plugin</artifactId>
   <version>2.2.6</version>
</plugin>
```

If the project is not tracked by git, you can type the following commands

>git init  
>git add .  
>git commit -m "first commit"

And then rebuild the project with maven and see the result.

We can also customize actuator info endpoint using static properties or environment variables.

Notice that informations such info.app.name, info.app.description are stored in config server into **order-service.properties**

Here the result of http://localhost:8080/actuator/info
```json
{
  app: {
    name: "Order Service",
    description: "This is the Order microservice app"
  },
  git: {
    commit: {
      time: "2019-01-17T10:21:24Z",
      id: "a6ea9bd"
    },
    branch: "master"
  }
}
```

## Service discovery configuration

In the main class or order-service projet add this annotation : **@EnableDiscoveryClient** to activates the Netflix Eureka DiscoveryClient implementation. There are other implementations for other service registries like Hashicorp’s Consul or Apache Zookeeper.

The eureka-client defines a Spring MVC REST endpoint, ServiceInstanceRestController, that returns an enumeration of all the ServiceInstance instances registered in the registry at http://localhost:8080/service-instances/order-service

```java
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
```

Go to
http://localhost:8080/service-instances/order-service

## Running multiple instances

Inside the order-service folder launch the application twice with different ports

```
java -DPORT=8080 -jar target/order-service-0.0.1-SNAPSHOT.jar
java -DPORT=7070 -jar target/order-service-0.0.1-SNAPSHOT.jar
```

Verify that two instances of order-service are registered with Eureka Service
> http://localhost:8761/


### Develop a client order service

Use spring initializr to bootstrap an order client.
Go to [Spring Initializr website](https://start.spring.io/) and then generate a maven project named **order-client**

> **Spring boot** : 2.1.2  
> **Group** : com.jcc  
> **Artifact** : order-client   
> **Dependencies** : Config Client, Eureka discovery, Zipkin client, Hystrix, zuul, Rest Repository, Web, Actuator, Feign  
> **Java version** : 1.8  

Compile project with maven.

### Configure order-client to use the config server

Rename application.properties to bootstrap.properties

And add these lines
```
spring.application.name=order-client
spring.cloud.config.uri=http://localhost:9999
```

### Enable service discovery

To begin using the DiscoveryClient, you first need to annotate the main class with @EnableDiscoveryClient annotation.
This annotation will activate the Spring DiscoveryClient for use.
In the real life you must use DiscoveryClient with RestTemplate to query Ribbon (see later)

### Zuul micro Proxy usage

Zuul is a micro proxy that redirect all requests to downstream service. It is an edge service. It represent a unique entry point of the application. it avoids the problems of CORS, and you can make some operations on the urls like rate limiter, etc.

Begin by annotate main order client class with @EnableZuulProxy

Launch the application and go to main url of the edge service :

> http://localhost:7777/order-service/orders

Then go to the downstream service :
> http://localhost:8080/orders


### Introduce api gateway
We need something that does more than redirecting urls. We need an api gateway to make url transformation, adapter, etc.
For example if we want to list only the names of the order without id, etc. we need a transformation.

Here the snippet of code to do that
```java
@RestController
@RequestMapping("/orders")
class OrderApiGateway {

	private RestTemplate restTemplate;

	@Autowired
	public OrderApiGateway(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}

	@RequestMapping (method = RequestMethod.GET, value = "/names")
	public Collection<String> names() {

		ParameterizedTypeReference<Resources<Order>> ptr = new ParameterizedTypeReference<Resources<Order>>() {};

		ResponseEntity<Resources<Order>> responseEntity = this.restTemplate.exchange("http://order-service/orders", HttpMethod.GET, null, ptr);

    // null because there is no body

		return responseEntity
				.getBody()
				.getContent()
				.stream()
				.map(Order::getOrderName)
				.collect(Collectors.toList());
	}
}
```

Don't forget to create the entity Order
```java
class Order {
	private String orderName;

	public String getOrderName() {
		return orderName;
	}
}
```
Finally introduce a bean to instanciate RestTemplate
```java
@Bean
@LoadBalanced
RestTemplate restTemplate() {
  return new RestTemplate();
}
```

RestTemplate has no idea about ribbon or instances that are registered by Eureka. We need an interceptor like @LoadBalanced, which is a spring cloud annotation.
The @LoadBalanced annotation tells Spring Cloud to create a Ribbon backed RestTemplate class.

**Launch the application**  
Go to http://localhost:7777/orders



### Resiliency with circuit breaker

Application can fail at any moment, to have an application that respond in all situations, even in the case of a degraded response we can use a Circuit Breaker.  
In the client order enable Circuit Breaker with this annotation @EnableCircuitBreaker.  
And then to tell Hystrix to protect the remote call to another service, use @HystrixCommand annotation.
Annotate names() method with  @HystrixCommand(fallbackMethod = "fallback")  
The fallback is the name of the method to deal with call failure. It is implemented like this
```java
public Collection <String> fallback() {
		return new ArrayList<>();
}
```

Test the application and stop the Order service to view the result.



### Distributed tracing with zipkin

projet spring boot
dependences: config client, eureka discovery,

### Deployment with Docker



**References:**  
[Config Server](https://cloud.spring.io/spring-cloud-config/multi/multi__spring_cloud_config_server.html)  
[Autre](https://github.com/joshlong/cloud-native-workshop  )  
[Service registry-1](https://spring.io/guides/gs/service-registration-and-discovery/)  
[Service registry-2](https://cloud.spring.io/spring-cloud-static/Edgware.SR2/multi/multi__service_discovery_eureka_clients.html)


a lire sur :

* service discovery
https://www.baeldung.com/spring-cloud-netflix-eureka
https://github.com/eugenp/tutorials/tree/master/spring-cloud/spring-cloud-eureka


* a faire
- lancer le service order deux fois avec deux ports differents et voir l'interface du registry
- ajouter swagger pour la documentation de l'api
- docker-compose
- changer en yaml c'est plus sexy


tres interessant:
https://piotrminkowski.wordpress.com/2018/04/26/quick-guide-to-microservices-with-spring-boot-2-0-eureka-and-spring-cloud/
