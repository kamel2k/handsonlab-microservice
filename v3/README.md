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
spring.cloud.config.name=discovery-service
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


## Use Case Order service

With spring initializr we can bootstrap a spring application very easy.
Go to [Spring Initializr website](https://start.spring.io/) and then generate a maven project named **order-service**

> **Spring boot** : 2.1.2  
> **Group** : com.jcc  
> **Artifact** : order-service  
> **Dependencies** : jpa, h2, rest repository , actuator, config client, eureka discovery , Cloud Stream, Zipkin Client  
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
spring.cloud.config.name=order-service
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


## Running multiple instances

Inside the order-service folder launch the application twice with different ports

```
java -DPORT=8080 -jar target/order-service-0.0.1-SNAPSHOT.jar
java -DPORT=7070 -jar target/order-service-0.0.1-SNAPSHOT.jar
```

Verify that applications are registered with Eureka Service
> http://localhost:8761/






### Develop a client order service



Config Client, Eureka discovery, Stream Rabbit, Zipkin client, Hystrix, zuul, Rest Repository, Web, Actuator, Cloud OAuth2, Feign

dans bootstrap.properties
spring.application.name=order-client
spring.cloud.config.uri=http://localhost:9999

Ajouter dans la classe principale
@EnableDiscoveryClient

### micro Proxy
Ajouter le microproxy zuul
@EnableZuulProxy

lancer le service et allez sur http://localhost:7777/order-service/orders
et le service original http://localhost:8080/orders
zuul c'est un microproxy dont il redirige toutes les requetes vers le downstream service. Il cnstitue un seul point d'entree pour l'application. ça evide les problemes de cors et il permet aussi de faire queles operations sur les requetes par exemple rate limiter.
zuul genere tout ce qui est x-forward- for etc.

Mais il faut qulque chose qui fait plus que ça, api gateway, transformation routage, adaptation, etc.

### Resiliency with circuit breaker

ajouter dans le client @EnabledCircuitbreaker

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
