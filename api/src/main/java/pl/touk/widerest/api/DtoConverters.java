package pl.touk.widerest.api;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.broadleafcommerce.common.currency.domain.BroadleafCurrency;
import org.broadleafcommerce.common.currency.service.BroadleafCurrencyService;
import org.broadleafcommerce.common.i18n.service.ISOService;
import org.broadleafcommerce.common.media.domain.Media;
import org.broadleafcommerce.common.media.domain.MediaImpl;
import org.broadleafcommerce.common.money.Money;
import org.broadleafcommerce.common.payment.PaymentType;
import org.broadleafcommerce.common.value.ValueAssignable;
import org.broadleafcommerce.core.catalog.domain.CategoryMediaXref;
import org.broadleafcommerce.core.catalog.domain.CategoryMediaXrefImpl;
import org.broadleafcommerce.core.catalog.domain.Product;
import org.broadleafcommerce.core.catalog.domain.ProductAttribute;
import org.broadleafcommerce.core.catalog.domain.ProductOption;
import org.broadleafcommerce.core.catalog.domain.ProductOptionImpl;
import org.broadleafcommerce.core.catalog.domain.ProductOptionValue;
import org.broadleafcommerce.core.catalog.domain.ProductOptionValueImpl;
import org.broadleafcommerce.core.catalog.domain.ProductOptionXref;
import org.broadleafcommerce.core.catalog.domain.ProductOptionXrefImpl;
import org.broadleafcommerce.core.catalog.domain.Sku;
import org.broadleafcommerce.core.catalog.domain.SkuBundleItem;
import org.broadleafcommerce.core.catalog.domain.SkuBundleItemImpl;
import org.broadleafcommerce.core.catalog.domain.SkuMediaXref;
import org.broadleafcommerce.core.catalog.domain.SkuMediaXrefImpl;
import org.broadleafcommerce.core.catalog.service.CatalogService;
import org.broadleafcommerce.core.order.domain.DiscreteOrderItem;
import org.broadleafcommerce.core.order.domain.FulfillmentOption;
import org.broadleafcommerce.core.order.domain.Order;
import org.broadleafcommerce.core.order.domain.OrderAttribute;
import org.broadleafcommerce.core.order.domain.OrderAttributeImpl;
import org.broadleafcommerce.core.order.domain.OrderImpl;
import org.broadleafcommerce.core.order.service.type.OrderStatus;
import org.broadleafcommerce.core.payment.domain.OrderPayment;
import org.broadleafcommerce.core.payment.domain.OrderPaymentImpl;
import org.broadleafcommerce.core.search.domain.SearchFacetDTO;
import org.broadleafcommerce.core.search.domain.SearchFacetResultDTO;
import org.broadleafcommerce.profile.core.domain.Address;
import org.broadleafcommerce.profile.core.domain.AddressImpl;
import org.broadleafcommerce.profile.core.domain.Customer;
import org.broadleafcommerce.profile.core.domain.CustomerAddress;
import org.broadleafcommerce.profile.core.domain.CustomerAddressImpl;
import org.broadleafcommerce.profile.core.domain.CustomerImpl;
import org.springframework.stereotype.Service;

import pl.touk.widerest.api.cart.CartUtils;
import pl.touk.widerest.api.cart.controllers.CustomerController;
import pl.touk.widerest.api.cart.controllers.OrderController;
import pl.touk.widerest.api.cart.dto.AddressDto;
import pl.touk.widerest.api.cart.dto.CartAttributeDto;
import pl.touk.widerest.api.cart.dto.CustomerAddressDto;
import pl.touk.widerest.api.cart.dto.CustomerDto;
import pl.touk.widerest.api.cart.dto.DiscreteOrderItemDto;
import pl.touk.widerest.api.cart.dto.OrderDto;
import pl.touk.widerest.api.cart.dto.OrderPaymentDto;
import pl.touk.widerest.api.catalog.CatalogUtils;
import pl.touk.widerest.api.products.ProductController;
import pl.touk.widerest.api.catalog.dto.BundleItemDto;
import pl.touk.widerest.api.catalog.dto.FacetDto;
import pl.touk.widerest.api.catalog.dto.FacetValueDto;
import pl.touk.widerest.api.catalog.dto.MediaDto;
import pl.touk.widerest.api.catalog.dto.ProductOptionDto;
import pl.touk.widerest.api.catalog.dto.ProductOptionValueDto;
import pl.touk.widerest.api.catalog.dto.SkuProductOptionValueDto;
import pl.touk.widerest.api.catalog.exceptions.ResourceNotFoundException;

@Service("wdDtoConverters")
public class DtoConverters {

    @Resource(name="blCurrencyService")
    protected BroadleafCurrencyService blCurrencyService;

    @Resource(name = "blCatalogService")
    protected CatalogService catalogService;

    @Resource
    protected ISOService isoService;

    private static Function<ProductAttribute, String> getProductAttributeName = ValueAssignable::getValue;

    private static Function<ProductOptionValue, String> getProductOptionValueName = ProductOptionValue::getAttributeValue;

    /******************************** Currency ********************************/

    public Function<String, BroadleafCurrency> currencyCodeToBLEntity = currencyCode -> {
        BroadleafCurrency skuCurrency = null;

        if(currencyCode == null || currencyCode.isEmpty()) {
            skuCurrency = blCurrencyService.findDefaultBroadleafCurrency();
        } else {
            skuCurrency = blCurrencyService.findCurrencyByCode(currencyCode);

            if (skuCurrency == null) {
//                BroadleafCurrency newBLCurrency = new BroadleafCurrencyImpl();
//                newBLCurrency.setCurrencyCode(currencyCode);
//                skuCurrency = blCurrencyService.save(newBLCurrency);
                throw new ResourceNotFoundException("Invalid currency code.");
            }

        }
        return skuCurrency;
    };

    /******************************** Currency ********************************/

    /******************************** SKU ********************************/


    /******************************** SKU ********************************/

    public ProductOption getProductOptionByNameForProduct(String productOptionName, Product product) {
        return Optional.ofNullable(product.getProductOptionXrefs())
                .orElse(Collections.emptyList()).stream()
                    .map(ProductOptionXref::getProductOption)
                    .filter(x -> x.getAttributeName().equals(productOptionName))
                    .findAny()
                    .orElse(null);
    }

    public ProductOptionValue getProductOptionValueByNameForProduct(ProductOption productOption,
                                                                    String productOptionValue) {
        return productOption.getAllowedValues().stream()
                .filter(x -> x.getAttributeValue().equals(productOptionValue))
                .findAny()
                .orElse(null);
    }



    /******************************** Product ********************************/


    /******************************** Product ********************************/

    /******************************** Product Option ********************************/

    public static Function <ProductOption, ProductOptionDto> productOptionEntityToDto = entity -> {

        return ProductOptionDto.builder()
                .name(entity.getAttributeName())
                .allowedValues(entity.getAllowedValues().stream()
                        .map(DtoConverters.getProductOptionValueName)
                        .collect(toList()))
                .build();
    };


    public static Function<ProductOptionDto, ProductOption> productOptionDtoToEntity = dto -> {
        ProductOption productOption = new ProductOptionImpl();
        productOption.setAttributeName(dto.getName());
        productOption.setAllowedValues(dto.getAllowedValues().stream()
                .map(e -> {
                    ProductOptionValue p = new ProductOptionValueImpl();
                    p.setAttributeValue(e);
                    return p;
                })
                .collect(toList()));

        return productOption;
    };

    /******************************** Product Option ********************************/

    /******************************** Product Option Value ********************************/

    public static Function<ProductOptionValue, ProductOptionValueDto> productOptionValueEntityToDto = entity -> {
        return ProductOptionValueDto.builder()
                .attributeValue(entity.getAttributeValue())
                .productOption(DtoConverters.productOptionEntityToDto.apply(entity.getProductOption()))
                .build();
    };


    public static Function<ProductOptionValueDto, ProductOptionValue> productOptionValueDtoToEntity = dto -> {
        ProductOptionValue productOptionValue = new ProductOptionValueImpl();

        productOptionValue.setAttributeValue(dto.getAttributeValue());
        productOptionValue.setProductOption(DtoConverters.productOptionDtoToEntity.apply(dto.getProductOption()));
        return productOptionValue;
    };

    public static Function<ProductOptionValue, SkuProductOptionValueDto> productOptionValueToSkuValueDto = entity -> {
        return SkuProductOptionValueDto.builder()
                .attributeName(entity.getProductOption().getAttributeName())
                .attributeValue(entity.getAttributeValue())
                .build();
    };

    /******************************** Product Option Value ********************************/

    /******************************** CATEGORY ********************************/

    /******************************** CATEGORY ********************************/

    /******************************** Sku Media  ********************************/

    public static Function<CategoryMediaXref, MediaDto> categoryMediaXrefToDto = xref -> {

        final Media entity = xref.getMedia();

        return MediaDto.builder()
                .title(entity.getTitle())
                .url(entity.getUrl())
                .altText(entity.getAltText())
                .tags(entity.getTags())
//                .key(xref.getKey())
                .build();
    };

//    public static CategoryMediaXref mediaDtoToCategoryMediaXref(MediaDto dto) {
//        CategoryMediaXref categoryMediaXref = new CategoryMediaXrefImpl();
//        Media media = new MediaImpl();
//
//        media = CatalogUtils.updateMediaEntityFromDto(media, dto);
//
//        categoryMediaXref.setMedia(media);
//        return categoryMediaXref;
//    };

//    public static Function<SkuMediaXref, MediaDto> skuMediaXrefToDto = xref -> {
//
//        final Media entity = xref.getMedia();
//
//        return MediaDto.builder()
//                .title(entity.getTitle())
//                .url(entity.getUrl())
//                .altText(entity.getAltText())
//                .tags(entity.getTags())
////                .key(xref.getKey())
//                .build();
//    };

    /* (mst) Remember to set SKU after this one */
//    public static Function<MediaDto, SkuMediaXref> skuMediaDtoToXref = dto -> {
//        SkuMediaXref skuMediaXref = new SkuMediaXrefImpl();
//        Media skuMedia = new MediaImpl();
//
//        skuMedia = CatalogUtils.updateMediaEntityFromDto(skuMedia, dto);
//
//        skuMediaXref.setMedia(skuMedia);
//        return skuMediaXref;
//    };

    /******************************** Sku Media  ********************************/

    /******************************** SKU BUNDLE ITEMS ********************************/

    public static Function<SkuBundleItem, BundleItemDto> skuBundleItemToBundleItemDto = entity -> {
        final BundleItemDto bundleItemDto = BundleItemDto.builder()
                .skuId(entity.getSku().getId())
                .quantity(entity.getQuantity())
                .salePrice(Optional.ofNullable(entity.getSalePrice()).map(Money::getAmount).orElse(null))
                .build();

        final Product associatedProduct = entity.getSku().getProduct();

        bundleItemDto.add(linkTo(methodOn(ProductController.class).getSkuById(
                associatedProduct.getId(),
                entity.getSku().getId())).withSelfRel());


        return bundleItemDto;
    };

    public Function<BundleItemDto, SkuBundleItem> bundleItemDtoToSkuBundleItem = dto -> {
        SkuBundleItem skuBundleItem = new SkuBundleItemImpl();
        skuBundleItem.setQuantity(dto.getQuantity());
        skuBundleItem.setSalePrice(new Money(dto.getSalePrice()));
        skuBundleItem.setSku(catalogService.findSkuById(dto.getSkuId()));
        return skuBundleItem;
    };

    /******************************** SKU BUNDLE ITEMS ********************************/

    /******************************** Product Option Xref ********************************/

    public static Function<ProductOptionXref, ProductOptionDto> productOptionXrefToDto = input -> {
        org.broadleafcommerce.core.catalog.domain.ProductOption productOption = input.getProductOption();

        List<ProductOptionValue> productOptionValues = productOption.getAllowedValues();
        List<String> collectAllowedValues = productOptionValues.stream()
                .map(getProductOptionValueName)
                .collect(toList());
        return new ProductOptionDto(productOption.getAttributeName(), collectAllowedValues);
    };

    // experimental
    public static Function<ProductOptionDto, ProductOptionXref> productOptionDtoToXRef = input -> {
        ProductOptionXref productOptionXref = new ProductOptionXrefImpl();
        ProductOption productOption = new ProductOptionImpl();

        productOption.setAttributeName(input.getName());
        productOption.setAllowedValues(input.getAllowedValues().stream()
                .map(e -> {
                    ProductOptionValue v = new ProductOptionValueImpl();
                    v.setAttributeValue(e);
                    return v;
                }).collect(toList()));

        productOptionXref.setProductOption(productOption);
        return productOptionXref;
    };

    /******************************** Product Option Xref ********************************/

    /******************************** CUSTOMER ********************************/
    public static Function<Customer, CustomerDto> customerEntityToDto = entity -> {

        final CustomerDto customerDto = CustomerDto.builder()
                .customerId(entity.getId())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .deactivated(entity.isDeactivated())
                .addresses(entity.getCustomerAddresses().stream()
                        .map(DtoConverters.customerAddressEntityToDto)
                        .collect(Collectors.toList()))
                .username(entity.getUsername())
//                disabled due to security reasons
//                .passwordHash(entity.getPassword())
                .registered(entity.isRegistered())
                .email(entity.getEmailAddress())
                .build();

        customerDto.add(linkTo(methodOn(CustomerController.class).readOneCustomer(null, entity.getId().toString())).withSelfRel());
        customerDto.add(linkTo(methodOn(CustomerController.class).createAuthorizationCode(null, entity.getId().toString())).withRel("authorization"));


        return customerDto;
    };

    public Function<CustomerDto, Customer> customerDtoToEntity = dto -> {

        final Customer customerEntity = new CustomerImpl();

        customerEntity.setId(dto.getCustomerId());
        customerEntity.setFirstName(dto.getFirstName());
        customerEntity.setLastName(dto.getLastName());
        customerEntity.setRegistered(dto.getRegistered());
        customerEntity.setUsername(dto.getUsername());
        customerEntity.setPassword(dto.getPasswordHash());
        customerEntity.setEmailAddress(dto.getEmail());
        customerEntity.setCustomerAddresses(dto.getAddresses().stream()
                .map(this.customerAddressDtoToEntity)
                .collect(Collectors.toList()));

        return customerEntity;
    };

    /******************************** CUSTOMER ********************************/

    /******************************** ADDRESS ********************************/

    public static Function<Address, AddressDto> addressEntityToDto = entity -> {

        return AddressDto.builder()
                .addressLine1(entity.getAddressLine1())
                .addressLine2(entity.getAddressLine2())
                .addressLine3(entity.getAddressLine3())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .city(entity.getCity())
                .postalCode(entity.getPostalCode())
                .companyName(entity.getCompanyName())
                .countryCode(entity.getIsoCountryAlpha2().getAlpha2())
                .countrySubdivisionCode(entity.getIsoCountrySubdivision())
                .build();
    };

    public Function<AddressDto, Address> addressDtoToEntity = dto -> {
        final Address addressEntity = new AddressImpl();

        addressEntity.setAddressLine1(dto.getAddressLine1());
        addressEntity.setAddressLine2(dto.getAddressLine2());
        addressEntity.setAddressLine3(dto.getAddressLine3());
        addressEntity.setFirstName(dto.getFirstName());
        addressEntity.setLastName(dto.getLastName());
        addressEntity.setCity(dto.getCity());
        addressEntity.setPostalCode(dto.getPostalCode());
        addressEntity.setCompanyName(dto.getCompanyName());
        addressEntity.setCounty(dto.getCountrySubdivisionCode());
        addressEntity.setIsoCountryAlpha2(isoService.findISOCountryByAlpha2Code(dto.getCountryCode()));


        return addressEntity;
    };

    /******************************** ADDRESS ********************************/

    /******************************** CUSTOMERADDRESS ********************************/

    public static Function<CustomerAddress, CustomerAddressDto> customerAddressEntityToDto = entity -> {

        return CustomerAddressDto.builder()
                .addressName(entity.getAddressName())
                .addressDto(DtoConverters.addressEntityToDto.apply(entity.getAddress()))
                .build();
    };

    public Function<CustomerAddressDto, CustomerAddress> customerAddressDtoToEntity = dto -> {
        final CustomerAddress customerAddress = new CustomerAddressImpl();

        customerAddress.setAddress(this.addressDtoToEntity.apply(dto.getAddressDto()));
        customerAddress.setAddressName(dto.getAddressName());

        return customerAddress;
    };

    /******************************** CUSTOMERADDRESS ********************************/

    /******************************** ORDER ********************************/
    public static Function<Order, OrderDto> orderEntityToDto = entity -> {
        final OrderDto orderDto = OrderDto.builder()
                .orderId(entity.getId())
                .orderNumber(entity.getOrderNumber())
                .status(entity.getStatus().getType())
                .orderPaymentDto(entity
                        .getPayments().stream().map(DtoConverters.orderPaymentEntityToDto).collect(Collectors.toList()))
                .orderItems(entity.getDiscreteOrderItems().stream()
                        .map(DtoConverters.discreteOrderItemEntityToDto)
                        .collect(Collectors.toList()))
//                .customer(DtoConverters.customerEntityToDto.apply(entity.getCustomer()))
                .totalPrice(Money.toAmount(entity.getTotal()))
                .fulfillment(CartUtils.getFulfilmentOption(entity)
                        .map(FulfillmentOption::getLongDescription)
                        .orElse(null))
                .cartAttributes(Optional.ofNullable(entity.getOrderAttributes()).orElse(Collections.emptyMap()).entrySet().stream()
                        .map(Map.Entry::getValue)
                        .map(DtoConverters.orderAttributeEntityToDto)
                        .collect(toList()))
                .build();

        orderDto.add(linkTo(
                methodOn(CustomerController.class).readOneCustomer(null, String.valueOf(entity.getCustomer().getId()))
        ).withRel("customer"));

        orderDto.add(linkTo(methodOn(OrderController.class).getOrderById(null, entity.getId())).withSelfRel());

        orderDto.add(linkTo(methodOn(OrderController.class).getOrdersCount(null)).withRel("order-count"));

        /* link to items placed in an order */
        orderDto.add(linkTo(methodOn(OrderController.class).getAllItemsInOrder(null, entity.getId())).withRel("items"));

        orderDto.add(linkTo(methodOn(OrderController.class).getItemsCountByOrderId(null, entity.getId())).withRel("items-count"));

        /* link to fulfillment */
        orderDto.add(linkTo(methodOn(OrderController.class).getOrderFulfilment(null, entity.getId())).withRel("fulfillment"));

        orderDto.add(linkTo(methodOn(OrderController.class).getOrderStatusById(null, entity.getId())).withRel("status"));

        return orderDto;
    };

    public Function<OrderDto, Order> orderDtoToEntity = dto -> {
        final Order orderEntity = new OrderImpl();

        orderEntity.setId(dto.getOrderId());
        orderEntity.setOrderNumber(dto.getOrderNumber());
        orderEntity.setStatus(OrderStatus.getInstance(dto.getStatus()));
        orderEntity.setPayments(dto.getOrderPaymentDto().stream().map(this.orderPaymentDtoToEntity)
                .collect(Collectors.toList()));

        return orderEntity;
    };


    public static Function<OrderAttribute, CartAttributeDto> orderAttributeEntityToDto = entity -> {
        return CartAttributeDto.builder()
                .name(entity.getName())
                .value(entity.getValue())
                .build();
    };

    public static Function<CartAttributeDto, OrderAttribute> orderAttributeDtoToEntity = dto -> {
        final OrderAttribute order = new OrderAttributeImpl();
        order.setName(dto.getName());
        order.setValue(dto.getValue());
        return order;
    };




    /******************************** ORDER ********************************/

    /******************************** PAYMENTINFO ********************************/

    public static Function<OrderPayment, OrderPaymentDto> orderPaymentEntityToDto = entity -> {
        return OrderPaymentDto.builder()
                .amount(entity.getAmount())
                .billingAddress(DtoConverters.addressEntityToDto.apply(entity.getBillingAddress()))
                .orderId(entity.getOrder().getId()).paymentId(entity.getId())
                .referenceNumber(entity.getReferenceNumber()).type(entity.getType().getType()).build();

    };

    public Function<OrderPaymentDto, OrderPayment> orderPaymentDtoToEntity = dto -> {
        OrderPayment orderPayment = new OrderPaymentImpl();

        orderPayment.setId(dto.getOrderId());
        orderPayment.setAmount(dto.getAmount());
        orderPayment.setBillingAddress(this.addressDtoToEntity.apply(dto.getBillingAddress()));
        orderPayment.setReferenceNumber(dto.getReferenceNumber());
        orderPayment.setType(PaymentType.getInstance(dto.getType()));

        return orderPayment;

    };

    /******************************** PAYMENTINFO ********************************/

    /******************************** DISCRETEORDERITEM ********************************/
    public static Function<DiscreteOrderItem, DiscreteOrderItemDto> discreteOrderItemEntityToDto = entity -> {
        final Money errCode = new Money(BigDecimal.valueOf(-1337));
        final Sku sku = entity.getSku();

        final long productId = sku.getProduct().getId();

        final DiscreteOrderItemDto orderItemDto = DiscreteOrderItemDto.builder()
                .itemId(entity.getId())
                .salePrice(entity.getSalePrice())
                .retailPrice(entity.getRetailPrice())
                .quantity(entity.getQuantity())
                .productName(entity.getName())
                .productId(productId)
                .skuId(sku.getId())
                .description(sku.getDescription())
                .price(Optional.ofNullable(entity.getTotalPrice()).orElse(errCode).getAmount())
                .build();

        orderItemDto.add(linkTo(methodOn(OrderController.class).getOneItemFromOrder(null, entity.getId(), entity.getOrder().getId())).withSelfRel());
        orderItemDto.add(linkTo(methodOn(ProductController.class).readOneProductById(productId)).withRel("product"));

        return orderItemDto;
    };
    /******************************** DISCRETEORDERITEM ********************************/
    public static Function<SearchFacetResultDTO, FacetValueDto> searchFacetResultDTOFacetValueToDto = entity -> FacetValueDto.builder()
            .value(entity.getValue())
            .minValue(entity.getMinValue())
            .maxValue(entity.getMaxValue())
            .quantity(entity.getQuantity())
            .build();

    public static Function<SearchFacetDTO, FacetDto> searchFacetDTOFacetToDto = entity -> {
        final FacetDto facetDto = FacetDto.builder()
                .active(entity.isActive())
                .label(entity.getFacet().getLabel())
                .build();

        facetDto.setFacetOptions(Optional.ofNullable(entity.getFacetValues()).orElse(Collections.emptyList()).stream()
                .map(searchFacetResultDTOFacetValueToDto)
                .collect(toList()));

        return facetDto;
    };


}