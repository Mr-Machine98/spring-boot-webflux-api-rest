package com.mrmachine.webflux.app;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import com.mrmachine.webflux.app.handler.ProductHandler;

@Configuration
public class RouterFunctionConfig {
	
	@Bean
	public RouterFunction<ServerResponse> routes(ProductHandler handler) {
		return RouterFunctions
				.route(RequestPredicates.GET("/api/v2/products/all"), handler::findAll)
				.andRoute(RequestPredicates.GET("/api/v2/products/{id}"), handler::findByid)
				.andRoute(RequestPredicates.POST("/api/v2/products"), handler::create)
				.andRoute(RequestPredicates.PUT("/api/v2/products/{id}"), handler::edit)
				.andRoute(RequestPredicates.DELETE("/api/v2/products/{id}"), handler::delete)
				.andRoute(RequestPredicates.POST("/api/v2/products/upload/{id}"), handler::upload)
				.andRoute(RequestPredicates.POST("/api/v2/products/createWithPhoto"), handler::createWithPhoto);
	}

}
