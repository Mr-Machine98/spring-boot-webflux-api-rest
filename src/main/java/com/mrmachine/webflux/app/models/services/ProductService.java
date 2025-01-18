package com.mrmachine.webflux.app.models.services;

import com.mrmachine.webflux.app.models.documents.Category;
import com.mrmachine.webflux.app.models.documents.Product;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ProductService {
	Flux<Product> findAll();
	Flux<Product> findAllUpperCaseName();
	Mono<Product> findById(String id);
	Mono<Product> save(Product p);
	Mono<Void> delete(Product p);
	Flux<Category> findAllCategory();
	Mono<Category> findCategoryById(String id);
	Mono<Category> save(Category c);
	Mono<Product> findByName(String name);
	Mono<Category> findByNameCategory(String name);
}
