package pl.touk.widerest.api;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.broadleafcommerce.common.currency.domain.BroadleafCurrency;
import org.broadleafcommerce.common.currency.domain.BroadleafCurrencyImpl;
import org.broadleafcommerce.common.currency.service.BroadleafCurrencyService;
import org.broadleafcommerce.common.locale.service.LocaleService;
import org.broadleafcommerce.common.media.domain.Media;
import org.broadleafcommerce.common.media.domain.MediaImpl;
import org.broadleafcommerce.common.money.Money;
import org.broadleafcommerce.common.payment.PaymentType;
import org.broadleafcommerce.core.catalog.domain.*;
import org.broadleafcommerce.core.catalog.service.CatalogService;
import org.broadleafcommerce.core.order.domain.*;
import org.broadleafcommerce.core.order.service.type.OrderStatus;
import org.broadleafcommerce.core.payment.domain.OrderPayment;
import org.broadleafcommerce.core.payment.domain.OrderPaymentImpl;
import org.broadleafcommerce.core.rating.domain.RatingDetail;
import org.broadleafcommerce.core.rating.domain.RatingDetailImpl;
import org.broadleafcommerce.core.rating.domain.ReviewDetail;
import org.broadleafcommerce.core.rating.domain.ReviewDetailImpl;
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
import pl.touk.widerest.api.cart.dto.CustomerAddressDto;
import pl.touk.widerest.api.cart.dto.CustomerDto;
import pl.touk.widerest.api.cart.dto.DiscreteOrderItemDto;
import pl.touk.widerest.api.cart.dto.OrderDto;
import pl.touk.widerest.api.cart.dto.OrderItemDto;
import pl.touk.widerest.api.cart.dto.OrderPaymentDto;
import pl.touk.widerest.api.catalog.controllers.CategoryController;
import pl.touk.widerest.api.catalog.controllers.ProductController;
import pl.touk.widerest.api.catalog.dto.*;

@Service("wdDtoConverters")
public class DtoConverters {

    @Resource(name = "blLocaleService")
    private LocaleService localeService;

    @Resource(name="blCurrencyService")
    protected BroadleafCurrencyService blCurrencyService;

    @Resource(name = "blCatalogService")
    protected CatalogService catalogService;

    private static Function<ProductAttribute, String> getProductAttributeName = input -> {
        return input.getValue();
    };

    private static Function<org.broadleafcommerce.core.catalog.domain.ProductOptionValue, String> getProductOptionValueName = input -> {
        return input.getAttributeValue();
    };

    /******************************** SKU ********************************/

    public Function<Sku, SkuDto> skuEntityToDto = entity -> {
        // Na przyszlosc: jesli dostanie sie wartosc z errCoda to znaczy
        // ze dana wartosc nie ustawiona => admin widzi objekt, klient nie
        Money errCode = new Money(BigDecimal.valueOf(-1337));

        SkuDto dto = SkuDto.builder()
                .skuId(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .salePrice(Optional.ofNullable(entity.getPrice()).orElse(errCode).getAmount())
                .quantityAvailable(entity.getQuantityAvailable()).taxCode(entity.getTaxCode())
                .activeStartDate(entity.getActiveStartDate()).activeEndDate(entity.getActiveEndDate())
                .currencyCode(Optional.ofNullable(entity.getCurrency())
                        .orElse(localeService.findDefaultLocale().getDefaultCurrency())
                        .getCurrencyCode())
                .skuAttributes(entity.getSkuAttributes().entrySet().stream()
                        .collect(toMap(Map.Entry::getKey, e -> {
                            return e.getValue().getName();
                        })))
                /*.productOptionValues(entity.getProductOptionValueXrefs().stream()
                        .map(SkuProductOptionValueXref::getProductOptionValue)
                        .map(DtoConverters.productOptionValueEntityToDto)
                        .collect(toSet()))*/
                .skuProductOptionValues(entity.getProductOptionValueXrefs().stream()
                                .map(SkuProductOptionValueXref::getProductOptionValue)
                                .map(DtoConverters.productOptionValueToSkuValueDto)
                                .collect(toSet()))
                .skuMedia(entity.getSkuMediaXref().entrySet().stream()
                        .map(Map.Entry::getValue)
                        .map(DtoConverters.skuMediaXrefToDto)
                        .collect(toList()))
                .build();

        /*
        product.setProductAttributes(
                    productDto.getAttributes().entrySet().stream().collect(toMap(Map.Entry::getKey, e -> {
                        ProductAttribute p = new ProductAttributeImpl();
                        p.setValue(e.getValue());
                        return p;
                    })));
         */



        // selection wysylany jest tylko od klienta
        dto.add(linkTo(methodOn(ProductController.class).getSkuById(entity.getProduct().getId(), entity.getId()))
                .withSelfRel());
        return dto;
    };

    public Function<SkuDto, Sku> skuDtoToEntity = skuDto -> {
        BroadleafCurrency skuCurrency = null;

        if(skuDto.getCurrencyCode() == null || skuDto.getCurrencyCode().isEmpty()) {
            skuCurrency = blCurrencyService.findDefaultBroadleafCurrency();
        } else {
            skuCurrency = blCurrencyService.findCurrencyByCode(skuDto.getCurrencyCode());

            /* TODO: (mst) I'm not sure about this: in case BL does not find provided currency,
                     can we just create it or we should only support those which are already in DB
             */
            if (skuCurrency == null) {
                BroadleafCurrency newBLCurrency = new BroadleafCurrencyImpl();
                newBLCurrency.setCurrencyCode(skuDto.getCurrencyCode());
                skuCurrency = blCurrencyService.save(newBLCurrency);
            }

        }

        Sku skuEntity = new SkuImpl();

        skuEntity.setName(skuDto.getName());
        skuEntity.setDescription(skuDto.getDescription());
        skuEntity.setSalePrice(new Money(skuDto.getSalePrice()));
        skuEntity.setCurrency(skuCurrency);
        skuEntity.setQuantityAvailable(skuDto.getQuantityAvailable());
        skuEntity.setTaxCode(skuDto.getTaxCode());
        skuEntity.setActiveStartDate(skuDto.getActiveStartDate());
        skuEntity.setActiveEndDate(skuDto.getActiveEndDate());

		/* (mst) looks like you have to have the Retail Price so in case used has not provided it,
		 * just set it to Sale Price
		 *
		 * TODO: (mst) Refactor to lambda
		 */
        if(skuDto.getRetailPrice() == null) {
            skuEntity.setRetailPrice(new Money(skuDto.getSalePrice()));
        } else {
            skuEntity.setRetailPrice(new Money(skuDto.getRetailPrice()));
        }

        // TODO: co z selection?
        // - (mst) a co ma byc?

        return skuEntity;
    };

    public ProductOption getProductOptionByNameForProduct(String productOptionName, Product product) {
        ProductOption productOption = null;

        if(product.getProductOptionXrefs() != null) {
            productOption = product.getProductOptionXrefs().stream()
                    .map(ProductOptionXref::getProductOption)
                    .filter(x -> x.getAttributeName().equals(productOptionName))
                    .findAny()
                    .orElse(null);
        }

        return productOption;
    }

    public ProductOptionValue getProductOptionValueByNameForProduct(ProductOption productOption,
                                                              String productOptionValue) {
        return productOption.getAllowedValues().stream()
                .filter(x -> x.getAttributeValue().equals(productOptionValue))
                .findAny()
                .orElse(null);
    }

    public Function<Product, ProductDto> productEntityToDto = entity -> {

        ProductDto dto = null;

        if (entity instanceof ProductBundle) {
            dto = new BundleDto();
        } else {
            dto = new ProductDto();
        }

		/* (mst) Do we really need ProductID? */
        dto.setProductId(entity.getId());

        dto.setName(entity.getName());

		/*
		 * TODO: (mst) Do we need the entire CategoryDto here or Category name +
		 * HATEAOS link will do the job?
		 *
		 * if(entity.getDefaultCategory() != null) {
		 * dto.setCategory(categoryEntityToDto.apply(entity.getDefaultCategory()
		 * )); }
		 */

        // if(entity.getCategory() != null)
        // dto.setCategoryName(Optional.ofNullable(entity.getCategory().getName()).orElse(""));

		/*
		 * TODO: REMOVE if(entity.getLongDescription() != null &&
		 * !entity.getLongDescription().isEmpty()) {
		 * dto.setLongDescription(entity.getLongDescription()); }
		 */

        dto.setLongDescription(Optional.ofNullable(entity.getLongDescription()).orElse(""));

		/*
		 * TODO: REMOVE if(entity.getPromoMessage() != null &&
		 * !entity.getPromoMessage().isEmpty()) {
		 * dto.setOfferMessage(entity.getPromoMessage()); }
		 */

        dto.setDescription(Optional.ofNullable(entity.getDescription()).orElse(""));
        dto.setOfferMessage(Optional.ofNullable(entity.getPromoMessage()).orElse(""));
        dto.setManufacturer(Optional.ofNullable(entity.getManufacturer()).orElse(""));
        dto.setModel(Optional.ofNullable(entity.getModel()).orElse(""));

        dto.setValidFrom(Optional.ofNullable(entity.getActiveStartDate()).orElse(null));
        dto.setValidTo(Optional.ofNullable(entity.getActiveEndDate()).orElse(null));

		/* (Map<String, String>) */
        dto.setAttributes(entity.getProductAttributes().entrySet().stream()
                .collect(toMap(Map.Entry::getKey, e -> e.getValue().toString())));

        dto.setOptions(entity.getProductOptionXrefs().stream().map(DtoConverters.productOptionXrefToDto).collect(toList()));

        dto.setDefaultSku(skuEntityToDto.apply(entity.getDefaultSku()));

		/* (mst) As far as I know, this DOES include Default SKU */
        dto.setSkus(entity.getAllSkus().stream().map(skuEntityToDto).collect(toList()));

		/* TODO: (mst) Implement Possible Bundles */

		/*
		 * Collection<ProductBundle> possibleBundles = Lists.transform(
		 * ((VirginSkuImpl) defaultSku).getSkuBundleItems(), new
		 * Function<SkuBundleItem, ProductBundle>() {
		 *
		 * @Nullable
		 *
		 * @Override public ProductBundle apply(@Nullable SkuBundleItem input) {
		 * return input.getBundle(); } } ); possibleBundles =
		 * Collections2.filter( possibleBundles, new Predicate<ProductBundle>()
		 * {
		 *
		 * @Override public boolean apply(@Nullable ProductBundle input) {
		 * return ((VirginSku) input.getDefaultSku()).getDefaultProductBundle()
		 * == null; } } );
		 * dto.setPossibleBundles(Lists.newArrayList(Iterables.transform(
		 * possibleBundles, new Function<ProductBundle, Long>() {
		 *
		 * @Nullable
		 *
		 * @Override public Long apply(@Nullable ProductBundle input) { return
		 * input.getId(); } } )));
		 */

        if (dto instanceof BundleDto) {
            ProductBundle productBundle = (ProductBundle) entity;

            ((BundleDto) dto).setBundleItems(
                    productBundle.getSkuBundleItems().stream().map(DtoConverters.skuBundleItemToBundleItemDto).collect(toList()));

            ((BundleDto) dto).setBundlePrice(productBundle.getSalePrice().getAmount());
            ((BundleDto) dto).setPotentialSavings(productBundle.getPotentialSavings());
        }

		/* HATEOAS links */
        dto.add(linkTo(methodOn(ProductController.class).readOneProductById(entity.getId())).withSelfRel());

        if (entity.getDefaultSku() != null) {
            dto.add(linkTo(methodOn(ProductController.class).getSkuById(entity.getId(), entity.getDefaultSku().getId()))
                    .withRel("default-sku"));
        }

		/* skus link does not include default SKU! */
        if (entity.getAdditionalSkus() != null && !entity.getAdditionalSkus().isEmpty()) {
            for (Sku additionalSku : entity.getAdditionalSkus()) {
                if (!additionalSku.equals(entity.getDefaultSku())) {
                    dto.add(linkTo(methodOn(ProductController.class).getSkuById(entity.getId(), additionalSku.getId()))
                            .withRel("skus"));
                }
            }
        }

		/*
		 * TODO: (mst) REMOVE because AllParentCategoryRefs() already has the
		 * DefaultCategory if(entity.getCategory() != null) {
		 * dto.add(linkTo(methodOn(CategoryController.class).readOneCategoryById
		 * (entity.getCategory().getId())).withRel("category")); }
		 */

		/* Links to the product's categories */
        if (entity.getAllParentCategoryXrefs() != null && !entity.getAllParentCategoryXrefs().isEmpty()) {
            for (CategoryProductXref parentCategoryXrefs : entity.getAllParentCategoryXrefs()) {
                dto.add(linkTo(methodOn(CategoryController.class)
                        .readOneCategoryById(parentCategoryXrefs.getCategory().getId())).withRel("category"));
            }
        }

        return dto;
    };

    public static Function <ProductOption, ProductOptionDto> productOptionEntityToDto = entity -> {
        ProductOptionDto productOptionDto = ProductOptionDto.builder()
                .name(entity.getAttributeName())
                .allowedValues(entity.getAllowedValues().stream()
                        .map(DtoConverters.getProductOptionValueName)
                        .collect(toList()))
                .build();

        return productOptionDto;
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


    public static Function<ProductOptionValue, ProductOptionValueDto> productOptionValueEntityToDto = entity -> {
        ProductOptionValueDto productOptionValueDto = ProductOptionValueDto.builder()
                .attributeValue(entity.getAttributeValue())
                .productOption(DtoConverters.productOptionEntityToDto.apply(entity.getProductOption()))
                .build();

        return productOptionValueDto;
    };


    public static Function<ProductOptionValueDto, ProductOptionValue> productOptionValueDtoToEntity = dto -> {
        ProductOptionValue productOptionValue = new ProductOptionValueImpl();

        productOptionValue.setAttributeValue(dto.getAttributeValue());
        productOptionValue.setProductOption(DtoConverters.productOptionDtoToEntity.apply(dto.getProductOption()));
        return productOptionValue;
    };

    public static Function<ProductOptionValue, SkuProductOptionValueDto> productOptionValueToSkuValueDto = entity -> {

        SkuProductOptionValueDto skuProductOptionValueDto = SkuProductOptionValueDto.builder()
                .attributeName(entity.getProductOption().getAttributeName())
                .attributeValue(entity.getAttributeValue())
                .build();

        return skuProductOptionValueDto;
    };

    /******************************** CATEGORY ********************************/

    public static Function<Category, CategoryDto> categoryEntityToDto = entity -> {

        CategoryDto dto = CategoryDto.builder().categoryId(entity.getId()).name(entity.getName())
                .description(entity.getDescription()).longDescription(entity.getLongDescription()).build();

        dto.add(linkTo(methodOn(CategoryController.class).readOneCategoryById(entity.getId())).withSelfRel());
        dto.add(linkTo(methodOn(CategoryController.class).readProductsFromCategory(entity.getId()))
                .withRel("products"));
        return dto;
    };

    public static Function<CategoryDto, Category> categoryDtoToEntity = dto -> {

        Category categoryEntity = new CategoryImpl();

        categoryEntity.setId(dto.getCategoryId());
        categoryEntity.setName(dto.getName());

        if (dto.getDescription() != null) {
            categoryEntity.setDescription(dto.getDescription());
        }

        if (dto.getLongDescription() != null) {
            categoryEntity.setLongDescription(dto.getLongDescription());
        }

        return categoryEntity;
    };

    public static Function<SkuMediaXref, SkuMediaDto> skuMediaXrefToDto = xref -> {

        Media entity = xref.getMedia();


        SkuMediaDto skuMediaDto = SkuMediaDto.builder()
                .mediaId(entity.getId())
                .title(entity.getTitle())
                .url(entity.getUrl())
                .altText(entity.getAltText())
                .tags(entity.getTags())
                .key(xref.getKey())
                .build();


        skuMediaDto.add(linkTo(methodOn(ProductController.class).getMediaByIdForSku(xref.getSku().getProduct().getId(),
                                                                                    xref.getSku().getId(),
                                                                                    entity.getId())).withSelfRel());

        return skuMediaDto;
    };

    /* (mst) Remember to set SKU after this one */
    public static Function<SkuMediaDto, SkuMediaXref> skuMediaDtoToXref = dto -> {
        SkuMediaXref skuMediaXref = new SkuMediaXrefImpl();
        Media skuMedia = new MediaImpl();

        skuMedia.setTitle(dto.getTitle());
        skuMedia.setUrl(dto.getUrl());
        skuMedia.setAltText(dto.getAltText());
        skuMedia.setTags(dto.getTags());

        skuMediaXref.setMedia(skuMedia);
        return skuMediaXref;
    };

    /******************************** PRODUCT ********************************/

    public static Function<SkuBundleItem, BundleItemDto> skuBundleItemToBundleItemDto = entity -> {
        BundleItemDto bundleItemDto = new BundleItemDto();
        bundleItemDto.setProductId(entity.getSku().getProduct().getId());
        bundleItemDto.setQuantity(entity.getQuantity());
        return bundleItemDto;
    };

    public static Function<BundleItemDto, SkuBundleItem> bundleItemDtoToSkuBundleItem = dto -> {
        SkuBundleItem skuBundleItem = new SkuBundleItemImpl();
        skuBundleItem.setQuantity(dto.getQuantity());
		/* TODO: (mst) To be continued... */
        return skuBundleItem;
    };

    public static Function<ProductOptionXref, ProductOptionDto> productOptionXrefToDto = input -> {
        org.broadleafcommerce.core.catalog.domain.ProductOption productOption = input.getProductOption();

        List<ProductOptionValue> productOptionValues = productOption.getAllowedValues();
        List<String> collectAllowedValues = productOptionValues.stream()
                .map(getProductOptionValueName)
                .collect(toList());
        ProductOptionDto dto = new ProductOptionDto(productOption.getAttributeName(), collectAllowedValues);
        return dto;
    };

    /* TODO: (mst) experimental */
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

    public Function<ProductDto, Product> productDtoToEntity = productDto -> {
        Product product = new ProductImpl();

        product.setDefaultSku(skuDtoToEntity.apply(productDto.getDefaultSku()));

        product.setName(productDto.getName());
        product.setDescription(productDto.getDescription());
        product.setLongDescription(productDto.getLongDescription());
        product.setPromoMessage(productDto.getOfferMessage());
        product.setActiveStartDate(productDto.getValidFrom());
        product.setActiveEndDate(productDto.getValidTo());
        product.setModel(productDto.getModel());
        product.setManufacturer(productDto.getManufacturer());

        List<Sku> allSkus = new ArrayList<>();
        allSkus.add(product.getDefaultSku());

		/* TODO: Do we have to put DefaultSKU to this list? */
        if (productDto.getSkus() != null && !productDto.getSkus().isEmpty()) {
            allSkus.addAll(productDto.getSkus().stream().map(skuDtoToEntity).collect(toList()));
        }
        product.setAdditionalSkus(allSkus);

		/* TODO: (mst) Refactor to lamda */
        if (productDto.getAttributes() != null) {

            product.setProductAttributes(
                    productDto.getAttributes().entrySet().stream().collect(toMap(Map.Entry::getKey, e -> {
                        ProductAttribute p = new ProductAttributeImpl();
                        p.setValue(e.getValue());
                        return p;
                    })));
        }

		/* TODO: (mst) Refactor to lamda */
        /*
        if(productDto.getOptions() != null) {
            product.setProductOptionXrefs(
                    productDto.getOptions().stream()
                            .map(productOptionDtoToXRef)
                            .collect(toList()));
        }*/

        // TODO: options

        return product;
    };

    /******************************** PRODUCT ********************************/

    /******************************** CUSTOMER ********************************/
    public static Function<Customer, CustomerDto> customerEntityToDto = entity -> {

        CustomerDto customerDto = CustomerDto.builder()
                .customerId(entity.getId())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .deactivaed(entity.isDeactivated())
                .addresses(entity.getCustomerAddresses().stream()
                        .map(DtoConverters.customerAddressEntityToDto)
                        .collect(Collectors.toList()))
                .username(entity.getUsername())
                .build();

        customerDto.add(linkTo(methodOn(CustomerController.class).readOneCustomer(entity.getId())).withSelfRel());



        return customerDto;
    };

    public static Function<CustomerDto, Customer> customerDtoToEntity = dto -> {

        Customer customerEntity = new CustomerImpl();

        customerEntity.setId(dto.getCustomerId());
        customerEntity.setFirstName(dto.getFirstName());
        customerEntity.setLastName(dto.getLastName());
        customerEntity.setRegistered(dto.getRegistered());
        customerEntity.setUsername(dto.getUsername());
        customerEntity.setCustomerAddresses(dto.getAddresses().stream()
                .map(DtoConverters.customerAddressDtoToEntity)
                .collect(Collectors.toList()));

        return customerEntity;
    };

    /******************************** CUSTOMER ********************************/

    /******************************** ADDRESS ********************************/

    public static Function<Address, AddressDto> addressEntityToDto = entity -> {
        AddressDto addressDto = AddressDto.builder()
                .addressLine1(entity.getAddressLine1())
                .addressLine2(entity.getAddressLine2())
                .addressLine3(entity.getAddressLine3())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
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
        addressEntity.setFirstName(dto.getFirstName());
        addressEntity.setLastName(dto.getLastName());
        addressEntity.setCity(dto.getCity());
        addressEntity.setPostalCode(dto.getPostalCode());
        addressEntity.setCompanyName(dto.getCompanyName());
        addressEntity.setCounty(dto.getCounty());


        return addressEntity;
    };

    /******************************** ADDRESS ********************************/

    /******************************** CUSTOMERADDRESS ********************************/

    public static Function<CustomerAddress, CustomerAddressDto> customerAddressEntityToDto = entity -> {
        CustomerAddressDto customerAddressDto = CustomerAddressDto.builder().id(entity.getId())
                .addressName(entity.getAddressName())
                .addressDto(DtoConverters.addressEntityToDto.apply(entity.getAddress())).build();

        return customerAddressDto;
    };

    public static Function<CustomerAddressDto, CustomerAddress> customerAddressDtoToEntity = dto -> {
        CustomerAddress customerAddress = new CustomerAddressImpl();

        customerAddress.setId(dto.getId());
        customerAddress.setAddress(DtoConverters.addressDtoToEntity.apply(dto.getAddressDto()));
        customerAddress.setAddressName(dto.getAddressName());

        return customerAddress;
    };

    /******************************** CUSTOMERADDRESS ********************************/

    /******************************** ORDER ********************************/
    public static Function<Order, OrderDto> orderEntityToDto = entity -> {
        OrderDto orderDto = OrderDto.builder()
                .orderId(entity.getId())
                .orderNumber(entity.getOrderNumber())
                .status(entity.getStatus().getType())
                .orderPaymentDto(entity
                        .getPayments().stream().map(DtoConverters.orderPaymentEntityToDto).collect(Collectors.toList()))
                .orderItems(entity.getDiscreteOrderItems().stream()
                        .map(DtoConverters.discreteOrderItemEntityToDto)
                        .collect(Collectors.toList()))
                .customer(DtoConverters.customerEntityToDto.apply(entity.getCustomer()))
                .totalPrice(entity.getTotal().getAmount())
                .fulfillment(Optional.ofNullable(CartUtils.getFulfilmentOption(entity))
                        .map(FulfillmentOption::getLongDescription)
                        .orElse(null))
                .build();
		/*
		 * orderDto.add(linkTo(methodOn(OrderController.class).(entity.getId()))
		 * .withRel()); orderDto.add(linkTo(methodOn(OrderController.class).
		 * getAllItemsInOrder(entity.getId())).withRel("items"));
		 */

        orderDto.add(linkTo(methodOn(OrderController.class).getOrderById(null, entity.getId())).withSelfRel());

        /* link to items placed in an order */
        orderDto.add(linkTo(methodOn(OrderController.class).getAllItemsInOrder(null, entity.getId())).withRel("items"));

        /* link to fulfillment */
        orderDto.add(linkTo(methodOn(OrderController.class).getOrderFulfilment(null, entity.getId())).withRel("fulfillment"));

        return orderDto;
    };

    public static Function<OrderDto, Order> orderDtoToEntity = dto -> {
        Order orderEntity = new OrderImpl();

        orderEntity.setId(dto.getOrderId());
        orderEntity.setOrderNumber(dto.getOrderNumber());
        orderEntity.setStatus(OrderStatus.getInstance(dto.getStatus()));
        orderEntity.setPayments(dto.getOrderPaymentDto().stream().map(DtoConverters.orderPaymentDtoToEntity)
                .collect(Collectors.toList()));

        return orderEntity;
    };
    /******************************** ORDER ********************************/

    /******************************** PAYMENTINFO ********************************/

    public static Function<OrderPayment, OrderPaymentDto> orderPaymentEntityToDto = entity -> {
        OrderPaymentDto orderPaymentDto = OrderPaymentDto.builder()
                .amount(entity.getAmount())
                .billingAddress(DtoConverters.addressEntityToDto.apply(entity.getBillingAddress()))
                .orderId(entity.getOrder().getId()).paymentId(entity.getId())
                .referenceNumber(entity.getReferenceNumber()).type(entity.getType().getType()).build();

        return orderPaymentDto;

    };

    public static Function<OrderPaymentDto, OrderPayment> orderPaymentDtoToEntity = dto -> {
        OrderPayment orderPayment = new OrderPaymentImpl();

        orderPayment.setId(dto.getOrderId());
        orderPayment.setAmount(dto.getAmount());
        orderPayment.setBillingAddress(DtoConverters.addressDtoToEntity.apply(dto.getBillingAddress()));
        orderPayment.setReferenceNumber(dto.getReferenceNumber());
        orderPayment.setType(PaymentType.getInstance(dto.getType()));

        return orderPayment;

    };

    /******************************** PAYMENTINFO ********************************/

    /******************************** DISCRETEORDERITEM ********************************/
    public static Function<DiscreteOrderItem, DiscreteOrderItemDto> discreteOrderItemEntityToDto = entity -> {
        Money errCode = new Money(BigDecimal.valueOf(-1337));
        Sku sku = entity.getSku();

        long productId = sku.getProduct().getId();

        DiscreteOrderItemDto orderItemDto = DiscreteOrderItemDto.builder()
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

    /******************************** REVIEW ********************************/
    public static Function<ReviewDetail, ReviewDto> reviewEntityToDto = entity -> {
        ReviewDto reviewDto = ReviewDto.builder().reviewText(entity.getReviewText()).helpfulCount(entity.helpfulCount())
                .notHelpfulCount(entity.notHelpfulCount()).statusType(entity.getStatus().getType()).build();

        return reviewDto;
    };

    public static Function<ReviewDto, ReviewDetail> reviewDtoToEntity = dto -> {
        ReviewDetail reviewDetailEntity = new ReviewDetailImpl();

		/*
		 * We cannot set number of counts and status from here, so just update
		 * the rewiev text
		 */
        reviewDetailEntity.setReviewText(dto.getReviewText());

        return reviewDetailEntity;
    };
    /******************************** REVIEW ********************************/

    /******************************** RATING ********************************/
    public static Function<RatingDetail, RatingDto> ratingEntityToDto = entity -> {
        RatingDto ratingDto = RatingDto.builder().rating(entity.getRating())
                .customer(DtoConverters.customerEntityToDto.apply(entity.getCustomer()))
                .submissionDate(entity.getRatingSubmittedDate()).build();

        return ratingDto;
    };

    public static Function<RatingDto, RatingDetail> ratingDtoToEntity = dto -> {
        RatingDetail ratingDetailEntity = new RatingDetailImpl();

        ratingDetailEntity.setRating(dto.getRating());
        ratingDetailEntity.setCustomer(DtoConverters.customerDtoToEntity.apply(dto.getCustomer()));
        ratingDetailEntity.setRatingSubmittedDate(dto.getSubmissionDate());

        return ratingDetailEntity;
    };
    /******************************** RATING ********************************/

    /******************************** FULFILLMENTS ********************************/





}