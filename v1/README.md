## Handsonlab Microservices with Spring frameworks

### Prerequesites

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

## Source code

All source code is available in github at [handsonlab-microservice](https://github.com/kamel2k/handsonlab-microservice)

to clone the application
```
git clone https://github.com/kamel2k/handsonlab-microservice.git
```

## Applications

config-service : 9999
order-service : 8080
discovery-service : 8761

## Order service

With spring initializr we can bootstrap a spring application very easy.
Go to [Spring Initializr website](https://start.spring.io/) and then generate a maven project named **order-svc**

> **Spring boot** : 2.1.2  
> **Group** : com.jcc  
> **Artifact** : order-svc  
> **Dependencies** : jpa, h2, rest repository , actuator, config client, eureka discovery , Cloud Stream, Zipkin Client  
> **Java version** : 1.8  

Compile project with maven

> mvn clean install

### Model our service

Begin with creating a JPA entity named Order with id and orderName and annotate the class with **@Entity**, here the code
```java
package com.jcc.ordersvc;

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
package com.jcc.ordersvc;

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
### Add REST support
We use Spring mvc and Spring Data Rest to expose order endpoint. Add the annotation **@RepositoryRestRessource** to our repository and put **@RestRessource (path="by-name")** to method **findByOrderName**. Finally put the **Pparam("on")** on to the attribute.
Here the result
```java
package com.jcc.ordersvc;

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
```

browse http://localhost:8080/orders/ to list all orders

and http://localhost:8080/orders/search/by-name?on=kamel to list orders based on the name

Notice that the API generated consist of an hypermedia API. For more information about HATEOAS visit https://fr.wikipedia.org/wiki/HATEOAS

## Add some production functionalities

Spring boot actuator adds several production grade services to your application with little effort on your part. We will add these services in our application.

Some actuator endpoint are activated by default such as **/actuator/health**, **actuator/info**, etc.

To enable all actuator endpoints you must add in **application.properties**
> management.endpoints.web.exposure.include=*

Explore some actuator endpoints and then browse info endpoint and notice that it is empty.

Developer can add some information inside this endpoint, such as application version, name, git commit, build date, etc. to identify the right version in production.

In pom.xml add the following plugin
```xml
<plugin>
   <groupId>pl.project13.maven</groupId>
   <artifactId>git-commit-id-plugin</artifactId>
   <version>2.2.6</version>
</plugin>
```

Type the following command

>git init  
>git add .  
>git commit -m "first commit"

And then rebuild the project with maven and see the result.

We can also customize actuator info endpoint using static properties or environment variables.

Add in **applications.properties** the following informations
>info.app.name=Order Service  
>info.app.description=This is the Order microservice app

Here the result
```json
{
  app: {
    name: "Order Service",
    description: "This is the Order microservice app"
  },
  git: {
    commit: {
      time: "2019-01-15T17:05:01Z",
      id: "0e92b80"
    },
    branch: "master"
  }
}
```

## Externalize configuration

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

With spring initializr we can bootstrap a config service easily.
Go to [Spring Initializr website](https://start.spring.io/) and then generate a maven project named **config-svc**

> **Spring boot** : 2.1.2  
> **Group** : com.jcc  
> **Artifact** : config-svc  
> **Dependencies** : Config Server  
> **Java version** : 1.8  

Compile project with maven.

Inside the main class add this annotation **@EnableConfigServer** to enable config server
Make application listen on port 9999

Launch **config-srv** and test http://localhost:9999/order-srv/default  
**Config-srv** must respond with the given configuration file for **order-srv**

#### Configure order-srv to use config-srv

Every application who need to take the configuration from **config-srv** need to modify application.properties for the microservice with the given lines.  
for order-srv
```properties
spring.cloud.config.name=order-srv
spring.cloud.config.uri=http://localhost:9999
```

## Setup Service discovery

**TODO** detailler la problematique et introduire eureka server.

**Step1: bootstrap discovery-svc project**
Use Spring initializr to bootstrap an eureka service.
Go to [Spring Initializr website](https://start.spring.io/) and then generate a maven project named **discovery-svc**

> **Spring boot** : 2.1.2  
> **Group** : com.jcc  
> **Artifact** : discovery-svc  
> **Dependencies** : Eureka Server, Config Cliet  
> **Java version** : 1.8  

Compile project with maven.

**Step2: Configure discovery-svc to use config-svc**  
Change application.properties with bootstrap.properties and add these lines:
```
server.port=9090
spring.cloud.config.name=discovery-svc
spring.cloud.config.uri=http://localhost:9999
```

**Step3: Enable Eureka Server**  
In the main Java class for discovery-svc add this annotation **@EnableEurekaServer** to enable Eureka Server

**Step4: register order-svc to Eureka Server**
Add dependency to order-svc
```xml
<dependency>
  <groupId>org.springframework.cloud</groupId>
  <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```


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
