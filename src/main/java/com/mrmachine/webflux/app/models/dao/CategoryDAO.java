package com.mrmachine.webflux.app.models.dao;

import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

import com.mrmachine.webflux.app.models.documents.Category;

import reactor.core.publisher.Mono;

public interface CategoryDAO extends ReactiveMongoRepository<Category, String> {
	@Query("{'name': ?0}")
	public Mono<Category> findByNameCategory(String name);
}
