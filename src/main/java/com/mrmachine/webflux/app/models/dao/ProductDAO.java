package com.mrmachine.webflux.app.models.dao;

import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

import com.mrmachine.webflux.app.models.documents.Product;

import reactor.core.publisher.Mono;


public interface ProductDAO extends ReactiveMongoRepository<Product, String>{
	
	@Query("{'name': ?0}")
	public Mono<Product> findByName(String name);
}
