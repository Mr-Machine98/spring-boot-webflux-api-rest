package com.mrmachine.webflux.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.mrmachine.webflux.app.models.documents.Category;
import com.mrmachine.webflux.app.models.documents.Product;
import com.mrmachine.webflux.app.models.services.ProductService;

import reactor.core.publisher.Mono;

@AutoConfigureWebTestClient
@TestMethodOrder(OrderAnnotation.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class SpringBootWebfluxApiRestApplicationTests {

	@Autowired
	private WebTestClient client;
	
	@Autowired
	private ProductService service;
	
	@Value("${config.base.endpoint}")
	private String url;
	
	@Order(1)
	@Test
	void listTest() {
		
		this.client
			.get()
			.uri(url+"/all")
			.accept(MediaType.APPLICATION_JSON)
			.exchange()
			.expectStatus()
				.isOk()
			.expectHeader().contentType(MediaType.APPLICATION_JSON)
			.expectBodyList(Product.class)
			.consumeWith( res -> {
				List<Product> products = res.getResponseBody();
				products.forEach( p -> System.out.println(p));
				assertThat(products.size() > 0).isTrue();
			});
			//.hasSize(10);
	}

	@Order(2)
	@Test
	void testId() throws Exception {
		
		Product product = service.findByName("Smartphone").block();
		
		this.client
			.get()
			.uri(url+"/{id}", Collections.singletonMap("id", product.getId()))
			.accept(MediaType.APPLICATION_JSON)
			.exchange()
			.expectStatus()
				.isOk()
			.expectHeader().contentType(MediaType.APPLICATION_JSON)
			.expectBody(Product.class)
			.consumeWith( res -> {
				Product p = res.getResponseBody();
				assertEquals("Smartphone", p.getName());
				assertThat(p.getId()).isNotEmpty();
			});
//			.jsonPath("$.id").isNotEmpty()
//			.jsonPath("$.name").isEqualTo("Smartphone");
	}
	
	@Order(3)
	@Test
	void testCreate() throws Exception {
		
		Category category = this.service.findByNameCategory("Smartphone").block();
		Product product = new Product("HeadPhones", 100.00, category);
		
		this.client
			.post()
			.uri(url)
			.contentType(MediaType.APPLICATION_JSON)
			.accept(MediaType.APPLICATION_JSON)
			.body(Mono.just(product), Product.class)
			.exchange()
			.expectStatus().isCreated()
			.expectHeader().contentType(MediaType.APPLICATION_JSON)
			.expectBody(Product.class)
			.consumeWith( res -> {
				 Product p = res.getResponseBody();
				 assertThat(p.getId()).isNotEmpty();
				 assertEquals("Smartphone", p.getCategory().getName());
				 assertEquals("HeadPhones", p.getName());
			});
	}
	
	@Order(4)
	@Test
	void testEdit() throws Exception {
		
		Product product = this.service.findByName("HeadPhones").block();
		System.out.println("Product to edit -> " + product);
		product.setPrice(200.00);
		
		this.client
			.put()
			.uri(url+"/{id}", Collections.singletonMap("id", product.getId()))
			.contentType(MediaType.APPLICATION_JSON)
			.accept(MediaType.APPLICATION_JSON)
			.body(Mono.just(product), Product.class)
			.exchange()
			.expectStatus().isAccepted()
			.expectHeader().contentType(MediaType.APPLICATION_JSON)
			.expectBody(Product.class)
			.consumeWith( res -> {
				Product p = res.getResponseBody();
				assertEquals(200.00, p.getPrice());
				System.out.println("Product is already edited -> " + p);
			});

	}
	
	@Order(5)
	@Test
	void testDelete() throws Exception {
		
		Product product = this.service.findByName("HeadPhones").block();
		System.out.println("Product to delete -> " + product);
		
		this.client
			.delete()
			.uri(url+"/{id}", Collections.singletonMap("id", product.getId()))
			.exchange()
			.expectStatus().isNoContent()
			.expectBody().isEmpty();
		
		System.out.println("Product deleted -> " + product);
		
		this.client
			.get()
			.uri(url+"/{id}", Collections.singletonMap("id", product.getId()))
			.exchange()
			.expectStatus().isNotFound()
			.expectBody().isEmpty();
	}
}
