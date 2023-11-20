package ru.flamexander.reactive.service.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.flamexander.reactive.service.dtos.DetailedProductDto;
import ru.flamexander.reactive.service.dtos.ProductDetailsDto;
import ru.flamexander.reactive.service.entities.Product;
import ru.flamexander.reactive.service.services.ProductDetailsService;
import ru.flamexander.reactive.service.services.ProductsService;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/v1/detailed")
@RequiredArgsConstructor
public class ProductsDetailsController {

    private final ProductDetailsService productDetailsService;
    private final ProductsService productsService;

    @GetMapping("/demo")
    public Flux<ProductDetailsDto> getManySlowProducts() {
        Mono<ProductDetailsDto> p1 = productDetailsService.getProductDetailsById(1L);
        Mono<ProductDetailsDto> p2 = productDetailsService.getProductDetailsById(2L);
        Mono<ProductDetailsDto> p3 = productDetailsService.getProductDetailsById(3L);
        return p1.mergeWith(p2).mergeWith(p3);
    }

    @GetMapping("/{id}")
    public Mono<DetailedProductDto> getById(@PathVariable Long id) {
        Mono<ProductDetailsDto> productDetailsDtoMono = productDetailsService.getProductDetailsById(id)
                .onErrorReturn(new ProductDetailsDto(id, "ERROR"));
        Mono<Product> productMono = productsService.findById(id);
        return Mono.zip(productMono, productDetailsDtoMono)
                .map(objects ->
                        new DetailedProductDto(
                                objects.getT1().getId(),
                                objects.getT1().getName(),
                                objects.getT2().getDescription()
                        ));
    }

    @GetMapping("")
    public Flux<DetailedProductDto> getAll() {
        return productsService.findAll().flatMap(product -> {
            Mono<ProductDetailsDto> productDetailsDtoMono = productDetailsService.getProductDetailsById(product.getId())
                    .onErrorReturn(new ProductDetailsDto(product.getId(), "ERROR"));
            return productDetailsDtoMono.map(productDetailsDto ->
                    new DetailedProductDto(
                            product.getId(),
                            product.getName(),
                            productDetailsDto.getDescription()
                    ));
        });
    }

    @GetMapping("/group")
    public Flux<DetailedProductDto> getGroup(@RequestParam String ids) {
        List<Long> idList = Arrays.stream(ids.split(",")).map(Long::parseLong).toList();
        return Flux.fromIterable(idList)
                .flatMap(this::getById);
    }
}
