package com.mrmachine.webflux.app.handler;

import java.io.File;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.FormFieldPart;
import org.springframework.stereotype.Component;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import com.mrmachine.webflux.app.models.documents.Category;
import com.mrmachine.webflux.app.models.documents.Product;
import com.mrmachine.webflux.app.models.services.ProductService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class ProductHandler {

	@Autowired
	private ProductService service;
	
	@Value("${config.uploads.path}")
	private String path;
	
	@Autowired
	private Validator validator;
	
	public Mono<ServerResponse> findAll(ServerRequest req) {
		return ServerResponse
				.ok()
				.body(this.service.findAll(), Product.class);
	}
	
	public Mono<ServerResponse> findByid(ServerRequest req) {	
		String id = req.pathVariable("id"); 	
		return this.service
				.findById(id)
				.flatMap( p -> ServerResponse
						.ok()
						.body(BodyInserters.fromValue(p)))
				.switchIfEmpty(ServerResponse.notFound().build());
	}
	
	public Mono<ServerResponse> create(ServerRequest req) {
		
		Mono<Product> product = req.bodyToMono(Product.class);
		
		return product.flatMap(p -> {
			
			Errors errors = new BeanPropertyBindingResult(p, Product.class.getName());
			validator.validate(p, errors);			
			
			if (errors.hasErrors()) {
				return Flux
						.fromIterable(errors.getFieldErrors())
						.map(fieldError -> "Field " + fieldError.getField() + " " + fieldError.getDefaultMessage())
						.collectList()
						.flatMap(list -> ServerResponse
								.badRequest()
								.body(BodyInserters.fromValue(list))
						);
			} else {
				if(p.getCreateAt() == null) p.setCreateAt(LocalDate.now());
				return this
						.service
						.save(p)
						.flatMap( pDb -> 
							ServerResponse
								.status(HttpStatus.CREATED)
								.body(BodyInserters.fromValue(pDb)));
			}
			
		});
	}
	
	public Mono<ServerResponse> edit(ServerRequest req) {
		
		Mono<Product> product = req.bodyToMono(Product.class);
		String id = req.pathVariable("id");
		Mono<Product> productDb = this.service.findById(id);
		
		return productDb.zipWith(product, (pDB, p) -> {
			
			pDB.setCategory(p.getCategory());
			pDB.setPrice(p.getPrice());
			pDB.setName(p.getName());
				
			return this.service.save(pDB);
		}).flatMap(p -> p)
		.flatMap(p -> ServerResponse.status(HttpStatus.ACCEPTED).body(BodyInserters.fromValue(p)))
		.switchIfEmpty(ServerResponse.notFound().build());
	}
	
	
	public Mono<ServerResponse> delete(ServerRequest req) { 
		String id = req.pathVariable("id");
		Mono<Product> productDb = this.service.findById(id);
		
		return productDb
				.flatMap( p ->  this.service.delete(p).then(ServerResponse.status(HttpStatus.NO_CONTENT).build()))
				.switchIfEmpty(ServerResponse.notFound().build());
	}
	
	public Mono<ServerResponse> upload(ServerRequest req) {
		String id = req.pathVariable("id");
		return req
				.multipartData()
				.map( multipart -> multipart.toSingleValueMap().get("file") )
				.cast(FilePart.class)
				.flatMap( file -> this.service.findById(id).flatMap( p -> {
					
					p.setPicture(
						UUID.randomUUID().toString() + "-" + file.filename()
						.replace(" ", "-")
						.replace(":", "")
						.replace("\\", "")		
					);
					
					return file
							.transferTo( new File(path + p.getPicture()) )
							.then(this.service.save(p));
				}))
				.flatMap(p -> ServerResponse
						.status(HttpStatus.ACCEPTED)
						.body(BodyInserters.fromValue(p))
				)
				.switchIfEmpty(ServerResponse.notFound().build());
	}
	
	public Mono<ServerResponse> createWithPhoto(ServerRequest req) {
		
		Mono<Product> product = req.multipartData().map( m -> {	
			FormFieldPart name = (FormFieldPart) m.toSingleValueMap().get("name");
			FormFieldPart price = (FormFieldPart) m.toSingleValueMap().get("price");
			FormFieldPart categoryId = (FormFieldPart) m.toSingleValueMap().get("category.id");
			FormFieldPart categoryName = (FormFieldPart) m.toSingleValueMap().get("category.name");
			
			Category category = new Category(categoryName.value());
			category.setId(categoryId.value());
			
			return new Product(name.value(), Double.parseDouble(price.value()), category);
		});
		
		return req
				.multipartData()
				.map( multipart -> multipart.toSingleValueMap().get("file") )
				.cast(FilePart.class)
				.flatMap( file -> product.flatMap( p -> {
					
					p.setPicture(
						UUID.randomUUID().toString() + "-" + file.filename()
						.replace(" ", "-")
						.replace(":", "")
						.replace("\\", "")		
					);
					
					p.setCreateAt(LocalDate.now());
					
					return file
							.transferTo( new File(path + p.getPicture()) )
							.then(this.service.save(p));
				}))
				.flatMap(p -> ServerResponse
						.status(HttpStatus.ACCEPTED)
						.body(BodyInserters.fromValue(p))
				);
	}

}
