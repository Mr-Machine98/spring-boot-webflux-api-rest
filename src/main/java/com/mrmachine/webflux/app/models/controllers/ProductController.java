package com.mrmachine.webflux.app.models.controllers;

import java.io.File;
import java.time.LocalDate;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.support.WebExchangeBindException;

import com.mrmachine.webflux.app.models.documents.Product;
import com.mrmachine.webflux.app.models.services.ProductService;

import jakarta.validation.Valid;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;




@RestController
@RequestMapping("/api/products")
public class ProductController {
	
	@Autowired
	private ProductService service;
	
	@Value("${config.uploads.path}")
	private String path;
	
	@PostMapping("/v2")
	public Mono<ResponseEntity<Product>> createWithPicture(Product product, @RequestPart FilePart file) {
		
		if (product.getCreateAt() == null) product.setCreateAt(LocalDate.now());
		
		product.setPicture( UUID.randomUUID().toString() + "-" + file.filename()
			.replace(" ", "")
			.replace(":", "")
			.replace("\\", ""));
		
		return file
				.transferTo(new File(path + product.getPicture()))
				.then(this.service.save(product))
				.map(p -> {
					return ResponseEntity
							.status(HttpStatus.CREATED)
							.body(p);
				});
	}
	
	@PostMapping("/upload/{id}")
	public Mono<ResponseEntity<Product>> uploadPicture(@PathVariable String id, @RequestPart FilePart file) {
		return this.service
				.findById(id)
				.flatMap(p -> {
					
					p.setPicture( UUID.randomUUID().toString() + "-" + file.filename()
					.replace(" ", "")
					.replace(":", "")
					.replace("\\", ""));
					
					return file
							.transferTo(new File(path + p.getPicture()))
							.then(this.service.save(p));
				})
				.map( p -> ResponseEntity.ok(p))
				.defaultIfEmpty(ResponseEntity.notFound().build());
	}
	
	
	@GetMapping("/all")
	public Mono<ResponseEntity<Flux<Product>>> findAll() {
		return Mono.just(ResponseEntity.ok(this.service.findAll()));
	}
	
	@GetMapping("/{id}")
	public Mono<ResponseEntity<Product>> getMethodName(@PathVariable String id) {
		return this.service
				.findById(id)
				.map(p -> ResponseEntity.ok(p))
				.defaultIfEmpty(ResponseEntity.notFound().build());
	}
	
	@PostMapping
	public Mono<ResponseEntity<Map<String, Object>>> create(@Valid @RequestBody Mono<Product> monoProduct) {
		
		Map<String, Object> res = new HashMap<String, Object>();
		
		return monoProduct.flatMap( product -> {
			
			if (product.getCreateAt() == null) product.setCreateAt(LocalDate.now());
			
			return this.service
				.save(product)
				.map(p -> {	
					res.put("product", p);
					res.put("msm", "The product has been created successfully.");
					res.put("timestamp", new Date());
					return ResponseEntity
						.status(HttpStatus.CREATED)
						.body(res);
				});
		})
		.onErrorResume(t -> Mono
				.just(t)
				.cast(WebExchangeBindException.class)
				.flatMap( e -> Mono.just(e.getFieldErrors()))
				.flatMapMany(errors -> Flux.fromIterable(errors))
				.map( fieldError -> "Field " + fieldError.getField() + " " + fieldError.getDefaultMessage())
				.collectList()
				.flatMap(list -> {
					res.put("errors", list);
					res.put("timestamp", new Date());
					res.put("status", HttpStatus.BAD_REQUEST.value());
					return Mono.just(ResponseEntity.badRequest().body(res));
				})
		);
	}
	
	@PutMapping("/{id}")
	public Mono<ResponseEntity<Product>> update(@PathVariable String id, @RequestBody Product product) {
		return this.service
					.findById(id)
					.flatMap(p -> {
						p.setCategory(product.getCategory());
						p.setName(product.getName());
						p.setPrice(product.getPrice());
						return this.service.save(p);
					})
					.map(p -> {
						return ResponseEntity
								.status(HttpStatus.OK)
								.body(p);
					})
					.defaultIfEmpty(ResponseEntity.notFound().build());
	}
	
	@DeleteMapping("/{id}")
	public Mono<ResponseEntity<Void>> delete(@PathVariable String id) {
		return this.service
				.findById(id)
				.flatMap( p -> {
					return this.service
							.delete(p)
							.then(Mono.just(new ResponseEntity<Void>(HttpStatus.NO_CONTENT)));
				})
				.defaultIfEmpty(new ResponseEntity<Void>(HttpStatus.NOT_FOUND)); 
	}
	
}
