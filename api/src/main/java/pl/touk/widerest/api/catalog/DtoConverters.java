package pl.touk.widerest.api.catalog;


import com.sun.jndi.cosnaming.IiopUrl;
import org.broadleafcommerce.core.catalog.domain.*;
import org.broadleafcommerce.core.order.domain.Order;
import org.broadleafcommerce.core.order.domain.OrderImpl;
import org.broadleafcommerce.core.order.domain.OrderItem;
import org.broadleafcommerce.core.order.domain.OrderItemImpl;
import org.broadleafcommerce.core.rating.domain.RatingDetail;
import org.broadleafcommerce.core.rating.domain.RatingDetailImpl;
import org.broadleafcommerce.core.rating.domain.ReviewDetail;
import org.broadleafcommerce.core.rating.domain.ReviewDetailImpl;
import org.broadleafcommerce.profile.core.domain.Address;
import org.broadleafcommerce.profile.core.domain.AddressImpl;
import org.broadleafcommerce.profile.core.domain.Customer;
import org.broadleafcommerce.profile.core.domain.CustomerImpl;
import pl.touk.widerest.api.cart.dto.AddressDto;
import pl.touk.widerest.api.cart.dto.CustomerDto;
import pl.touk.widerest.api.cart.dto.OrderDto;
import pl.touk.widerest.api.cart.dto.OrderItemDto;
import pl.touk.widerest.api.catalog.controllers.CategoryController;
import pl.touk.widerest.api.catalog.controllers.ProductController;
import pl.touk.widerest.api.catalog.controllers.SkuController;
import pl.touk.widerest.api.catalog.dto.*;


import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

public class DtoConverters {

    /********************************  CATEGORY   ********************************/

    public static Function<Category, CategoryDto> categoryEntityToDto = entity -> {

        CategoryDto dto = CategoryDto.builder()
                .categoryId(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .longDescription(entity.getLongDescription())
                .build();

        dto.add(linkTo(methodOn(CategoryController.class).readOneCategoryById(entity.getId())).withSelfRel());
        dto.add(linkTo(methodOn(CategoryController.class).readProductsFromCategory(entity.getId())).withRel("products"));
        return dto;
    };

    public static Function<CategoryDto, Category> categoryDtoToEntity
            = dto -> {

        Category categoryEntity = new CategoryImpl();

        categoryEntity.setId(dto.getCategoryId())
        ;
        categoryEntity.setName(dto.getName());

        if(dto.getDescription() != null) {
            categoryEntity.setDescription(dto.getName());
        }

        if(dto.getLongDescription() != null) {
            categoryEntity.setLongDescription(dto.getLongDescription());
        }


        return categoryEntity;
    };
    /********************************  CATEGORY   ********************************/

    /********************************  SKU   ********************************/

    public static Function<Sku, SkuDto> skuEntityToDto = entity -> {
        SkuDto dto = SkuDto.builder()
                .id(entity.getId())
                .description(entity.getDescription())
                //.price(entity.getPrice().getAmount())
                .quantityAvailable(entity.getQuantityAvailable())
                .code(entity.getTaxCode()).build();
        //TODO: selection + HATEOAS links
        return dto;
    };

    public static Function<SkuDto, Sku> skuDtoToEntity = dto -> {
        Sku skuEntity = new SkuImpl();

        skuEntity.setId(dto.getId());
        skuEntity.setDescription(dto.getDescription());
        skuEntity.setTaxCode(dto.getCode());
        skuEntity.setQuantityAvailable(dto.getQuantityAvailable());

        return skuEntity;
    };

    /********************************  SKU   ********************************/

    /********************************  PRODUCT   ********************************/

    public static Function<ProductAttribute, String> getProductAttributeName = input -> {
        return input.getValue();
    };

    public static Function<org.broadleafcommerce.core.catalog.domain.ProductOptionValue, String> getProductOptionValueName = input -> {
        return input.getAttributeValue();
    };

    public static Function<ProductOptionXref, ProductOption> productOptionXrefToDto = input -> {
        org.broadleafcommerce.core.catalog.domain.ProductOption productOption = input.getProductOption();
        //dto.setName(productOption.getAttributeName());
        List<ProductOptionValue> productOptionValues = productOption.getAllowedValues();
        List<String> collectAllowedValues = productOptionValues.stream().map(getProductOptionValueName).collect(toList());
        //dto.setAllowedValues(collectAllowedValues);

        return null;

        //return new ProductOptionImpl(productOption.getAttributeName(), collectAllowedValues);
    };

    public static Function<ProductDto, Product> productDtoToEntity = productDto -> {
        /* TMP! */
        return new ProductImpl();
    };

    public static Function<Product, ProductDto> productEntityToDto
            = entity -> {
        ProductDto dto = new ProductDto();

        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());

        dto.setValidFrom(entity.getActiveStartDate());
        dto.setValidTo(entity.getActiveEndDate());



        if(entity.getDefaultCategory() != null) {
            dto.setCategory(entity.getDefaultCategory().getName());
        }

        if(entity.getLongDescription() != null && !entity.getLongDescription().isEmpty()) {
            dto.setLongDescription(entity.getLongDescription());
        }

        if(entity.getPromoMessage() != null && !entity.getPromoMessage().isEmpty()) {
            // TODO: Mozliwe ze offer message nie istnieje? Czy trzeba wyrzucic wyjatek?
            dto.setOfferMessage(entity.getPromoMessage());
        }


        Map<String, String> productAttributesCollect = entity.getProductAttributes().entrySet().stream()
                .collect(toMap(Map.Entry::getKey, e -> e.getValue().toString()));
        dto.setAttributes(productAttributesCollect);

        //List<ProductOptionDto> productOptionsCollect = entity.getProductOptionXrefs().stream().map(productOptionXrefToDto)
        //        .collect(toList());
        //dto.setOptions(productOptionsCollect);

        // TODO: czy potrzeba cos inaczej ustawic, jako ze jest defaultCategory? - TAK?, na poczatek listy domyslny
        List<SkuDto> skuDtosCollect = entity.getAllSkus().stream()
                .map(skuEntityToDto).collect(toList());
        dto.setSkus(skuDtosCollect);

        //SkuDto defaultSku = skuToDto.apply(entity.getDefaultSku());
        // TODO: znalezc w jakich bundlach jest product




        dto.add(linkTo(methodOn(ProductController.class).readOneProduct(entity.getId())).withSelfRel());

        /* Link to a default SKU if it exists. I mean...it should exist, right?  */
        if(entity.getDefaultSku() != null) {
            dto.add(linkTo(methodOn(SkuController.class).getSkusById(entity.getDefaultSku().getId())).withRel("default-sku"));
        }

        /* Link to a product's category */
        if(entity.getCategory() != null) {
            dto.add(linkTo(methodOn(CategoryController.class).readOneCategoryById(entity.getCategory().getId())).withRel("category"));
        }

        return dto;
    };
    /********************************  PRODUCT   ********************************/

    /********************************  CUSTOMER   ********************************/
    public static Function<Customer, CustomerDto> customerEntityToDto = entity -> {

        CustomerDto customerDto = CustomerDto.builder()
                .id(entity.getId())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .challengeAnswer(entity.getChallengeAnswer())
                .registered(entity.isRegistered())
                .receiveEmail(entity.isReceiveEmail())
                .build();

        return customerDto;
    };

    public static Function<CustomerDto, Customer> customerDtoToEntity = dto -> {

        Customer customerEntity = new CustomerImpl();

        customerEntity.setId(dto.getId());
        customerEntity.setFirstName(dto.getFirstName());
        customerEntity.setLastName(dto.getLastName());
        customerEntity.setChallengeAnswer(dto.getChallengeAnswer());
        customerEntity.setRegistered(dto.getRegistered());
        customerEntity.setReceiveEmail(dto.getReceiveEmail());

        return customerEntity;
    };

    /********************************  CUSTOMER   ********************************/

    /********************************  ADDRESS   ********************************/

    public static Function<Address, AddressDto> addressEntityToDto = entity -> {
        AddressDto addressDto = AddressDto.builder()
                .addressLine1(entity.getAddressLine1())
                .addressLine2(entity.getAddressLine2())
                .addressLine3(entity.getAddressLine3())
                .city(entity.getCity())
                .postalCode(entity.getPostalCode())
                .companyName(entity.getCompanyName())
                .county(entity.getCounty())
                .build();

        return addressDto;
    };

    public static Function<AddressDto, Address> addressDtoToEntity = dto -> {
        Address addressEntity = new AddressImpl();

        addressEntity.setAddressLine1(dto.getAddressLine1());
        addressEntity.setAddressLine2(dto.getAddressLine2());
        addressEntity.setAddressLine3(dto.getAddressLine3());
        addressEntity.setCity(dto.getCity());
        addressEntity.setPostalCode(dto.getPostalCode());
        addressEntity.setCompanyName(dto.getCompanyName());
        addressEntity.setCounty(dto.getCounty());

        return addressEntity;
    };

    /********************************  ADDRESS   ********************************/

    /********************************  ORDER   ********************************/
    public static Function<Order, OrderDto> orderEntityToDto = entity -> {
        OrderDto orderDto = OrderDto.builder()
                .orderId(entity.getId())
                .orderNumber(entity.getOrderNumber())
                .build();

        return orderDto;
    };

    public static Function<OrderItemDto, OrderItem> orderDtoToEntity = dto -> {
        OrderItem orderItemEntity = new OrderItemImpl();
        orderItemEntity.setName(dto.getProductName());
        //TODO: everything ;
        return orderItemEntity;

    };
    /********************************  ORDER   ********************************/

    /********************************  REVIEW   ********************************/
    public static Function<ReviewDetail, ReviewDto> reviewEntityToDto = entity -> {
        ReviewDto reviewDto = ReviewDto.builder().reviewText(entity.getReviewText()).build();

        return reviewDto;
    };

    public static Function<ReviewDto, ReviewDetail> reviewDtoToEntity = dto -> {
        ReviewDetail reviewDetailEntity = new ReviewDetailImpl();

        reviewDetailEntity.setReviewText(dto.getReviewText());

        return reviewDetailEntity;
    };
    /********************************  REVIEW   ********************************/

    /********************************  RATING   ********************************/
    public static Function<RatingDetail, RatingDto> ratingEntityToDto = entity -> {
        RatingDto ratingDto = RatingDto.builder()
                .rating(entity.getRating())
                .customer(DtoConverters.customerEntityToDto.apply(entity.getCustomer()))
                .build();

        return ratingDto;
    };

    public static Function<RatingDto, RatingDetail> ratingDtoToEntity = dto -> {
        RatingDetail ratingDetailEntity = new RatingDetailImpl();

        ratingDetailEntity.setRating(dto.getRating());
        ratingDetailEntity.setCustomer(DtoConverters.customerDtoToEntity.apply(dto.getCustomer()));

        return ratingDetailEntity;
    };
    /********************************  RATING   ********************************/


}
