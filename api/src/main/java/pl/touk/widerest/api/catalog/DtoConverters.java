package pl.touk.widerest.api.catalog;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.broadleafcommerce.common.money.Money;
import org.broadleafcommerce.common.payment.PaymentType;
import org.broadleafcommerce.core.catalog.domain.Category;
import org.broadleafcommerce.core.catalog.domain.CategoryImpl;
import org.broadleafcommerce.core.catalog.domain.CategoryProductXref;
import org.broadleafcommerce.core.catalog.domain.Product;
import org.broadleafcommerce.core.catalog.domain.ProductAttribute;
import org.broadleafcommerce.core.catalog.domain.ProductAttributeImpl;
import org.broadleafcommerce.core.catalog.domain.ProductBundle;
import org.broadleafcommerce.core.catalog.domain.ProductImpl;
import org.broadleafcommerce.core.catalog.domain.ProductOption;
import org.broadleafcommerce.core.catalog.domain.ProductOptionImpl;
import org.broadleafcommerce.core.catalog.domain.ProductOptionValue;
import org.broadleafcommerce.core.catalog.domain.ProductOptionValueImpl;
import org.broadleafcommerce.core.catalog.domain.ProductOptionXref;
import org.broadleafcommerce.core.catalog.domain.ProductOptionXrefImpl;
import org.broadleafcommerce.core.catalog.domain.Sku;
import org.broadleafcommerce.core.catalog.domain.SkuBundleItem;
import org.broadleafcommerce.core.catalog.domain.SkuBundleItemImpl;
import org.broadleafcommerce.core.catalog.domain.SkuImpl;
import org.broadleafcommerce.core.catalog.service.CatalogService;
import org.broadleafcommerce.core.order.domain.DiscreteOrderItem;
import org.broadleafcommerce.core.order.domain.DiscreteOrderItemImpl;
import org.broadleafcommerce.core.order.domain.Order;
import org.broadleafcommerce.core.order.domain.OrderImpl;
import org.broadleafcommerce.core.order.domain.OrderItem;
import org.broadleafcommerce.core.order.domain.OrderItemImpl;
import org.broadleafcommerce.core.order.service.call.OrderItemRequestDTO;
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

import pl.touk.widerest.api.cart.dto.AddressDto;
import pl.touk.widerest.api.cart.dto.CustomerAddressDto;
import pl.touk.widerest.api.cart.dto.CustomerDto;
import pl.touk.widerest.api.cart.dto.DiscreteOrderItemDto;
import pl.touk.widerest.api.cart.dto.OrderDto;
import pl.touk.widerest.api.cart.dto.OrderItemDto;
import pl.touk.widerest.api.cart.dto.OrderPaymentDto;
import pl.touk.widerest.api.catalog.controllers.CategoryController;
import pl.touk.widerest.api.catalog.controllers.ProductController;
import pl.touk.widerest.api.catalog.dto.BundleDto;
import pl.touk.widerest.api.catalog.dto.BundleItemDto;
import pl.touk.widerest.api.catalog.dto.CategoryDto;
import pl.touk.widerest.api.catalog.dto.ProductDto;
import pl.touk.widerest.api.catalog.dto.ProductOptionDto;
import pl.touk.widerest.api.catalog.dto.RatingDto;
import pl.touk.widerest.api.catalog.dto.ReviewDto;
import pl.touk.widerest.api.catalog.dto.SkuDto;

public class DtoConverters {

    @Resource(name = "blCatalogService")
    private static CatalogService catalogService;

    private static Function<ProductAttribute, String> getProductAttributeName = input -> {
        return input.getValue();
    };

    private static Function<org.broadleafcommerce.core.catalog.domain.ProductOptionValue, String> getProductOptionValueName = input -> {
        return input.getAttributeValue();
    };

    /******************************** SKU ********************************/

    public static Function<Sku, SkuDto> skuEntityToDto = entity -> {
        // Na przyszlosc: jesli dostanie sie wartosc z errCoda to znaczy
        // ze dana wartosc nie ustawiona => admin widzi objekt, klient nie
        Money errCode = new Money(BigDecimal.valueOf(-1337));

        SkuDto dto = SkuDto.builder().skuId(entity.getId()).name(entity.getName()).description(entity.getDescription())
                .salePrice(Optional.ofNullable(entity.getPrice()).orElse(errCode).getAmount())
                .quantityAvailable(entity.getQuantityAvailable()).taxCode(entity.getTaxCode())
                .activeStartDate(entity.getActiveStartDate()).activeEndDate(entity.getActiveEndDate()).build();

        // selection wysylany jest tylko od klienta
        dto.add(linkTo(methodOn(ProductController.class).getSkuById(entity.getProduct().getId(), entity.getId()))
                .withSelfRel());
        return dto;
    };

    public static Function<SkuDto, Sku> skuDtoToEntity = dto -> {
        Sku skuEntity = new SkuImpl();

        skuEntity.setName(dto.getName());
        skuEntity.setDescription(dto.getDescription());
        skuEntity.setSalePrice(new Money(dto.getSalePrice()));
        skuEntity.setQuantityAvailable(dto.getQuantityAvailable());
        skuEntity.setTaxCode(dto.getTaxCode());
        skuEntity.setActiveStartDate(dto.getActiveStartDate());
        skuEntity.setActiveEndDate(dto.getActiveEndDate());
		
		/* (mst) looks like you have to have the Retail Price so in case used has not provided it,
		 * just set it to Sale Price
		 * 
		 * TODO: (mst) Refactor to lambda
		 */
        if(dto.getRetailPrice() == null) {
            skuEntity.setRetailPrice(new Money(dto.getSalePrice()));
        } else {
            skuEntity.setRetailPrice(new Money(dto.getRetailPrice()));
        }

        // TODO: co z selection?

        return skuEntity;
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
        List<String> collectAllowedValues = productOptionValues.stream().map(getProductOptionValueName)
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

    public static Function<ProductDto, Product> productDtoToEntity = productDto -> {
        Product product = new ProductImpl();

        product.setDefaultSku(skuDtoToEntity.apply(productDto.getDefaultSku()));

        product.setName(productDto.getName());

		/* TODO (mst) Category!!! */
        if (productDto.getCategoryName() != null && !productDto.getCategoryName().isEmpty()) {

        }

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
        if(productDto.getOptions() != null) {
            product.setProductOptionXrefs(
                    productDto.getOptions().stream()
                            .map(productOptionDtoToXRef)
                            .collect(toList()));
        }

        // TODO: options

        return product;
    };

    public static Function<Product, ProductDto> productEntityToDto = entity -> {

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

        dto.setOptions(entity.getProductOptionXrefs().stream().map(productOptionXrefToDto).collect(toList()));

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
                    productBundle.getSkuBundleItems().stream().map(skuBundleItemToBundleItemDto).collect(toList()));

            ((BundleDto) dto).setBundlePrice(productBundle.getSalePrice().getAmount());
            ((BundleDto) dto).setPotentialSavings(productBundle.getPotentialSavings());
        }

		/* HATEOAS links */
        dto.add(linkTo(methodOn(ProductController.class).readOneProduct(entity.getId())).withSelfRel());

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
    /******************************** PRODUCT ********************************/

    /******************************** CUSTOMER ********************************/
    public static Function<Customer, CustomerDto> customerEntityToDto = entity -> {

        CustomerDto customerDto = CustomerDto.builder().id(entity.getId()).firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                        // .challengeAnswer(entity.getChallengeAnswer())
                .registered(entity.isRegistered()).receiveEmail(entity.isReceiveEmail())
                        // .challengeQuestion(entity.getChallengeQuestion().getQuestion())
                .deactivaed(entity.isDeactivated())
                .passwordChangeRequired(entity.isPasswordChangeRequired()).addresses(entity.getCustomerAddresses()
                        .stream().map(DtoConverters.customerAddressEntityToDto).collect(Collectors.toList()))
                .username(entity.getUsername()).build();

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
        customerEntity.setUsername(dto.getUsername());
        customerEntity.setCustomerAddresses(
                dto.getAddresses().stream().map(DtoConverters.customerAddressDtoToEntity).collect(Collectors.toList()));

        return customerEntity;
    };

    /******************************** CUSTOMER ********************************/

    /******************************** ADDRESS ********************************/

    public static Function<Address, AddressDto> addressEntityToDto = entity -> {
        AddressDto addressDto = AddressDto.builder().addressLine1(entity.getAddressLine1())
                .addressLine2(entity.getAddressLine2()).addressLine3(entity.getAddressLine3()).city(entity.getCity())
                .postalCode(entity.getPostalCode()).companyName(entity.getCompanyName()).county(entity.getCounty())
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
        OrderDto orderDto = OrderDto.builder().orderId(entity.getId())
                .orderNumber(entity.getOrderNumber()).status(entity.getStatus().getType()).orderPaymentDto(entity
                        .getPayments().stream().map(DtoConverters.orderPaymentEntityToDto).collect(Collectors.toList()))
                .build();
		/*
		 * orderDto.add(linkTo(methodOn(OrderController.class).(entity.getId()))
		 * .withRel()); orderDto.add(linkTo(methodOn(OrderController.class).
		 * getAllItemsInOrder(entity.getId())).withRel("items"));
		 */
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
        OrderPaymentDto orderPaymentDto = OrderPaymentDto.builder().amount(entity.getAmount())
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

    /******************************** ORDERITEM ********************************/

    public static Function<OrderItemDto, OrderItem> orderItemDtoToEntity = dto -> {
        OrderItem orderItemEntity = new OrderItemImpl();
        orderItemEntity.setName(dto.getProductName());
        orderItemEntity.setRetailPrice(dto.getRetailPrice());
        orderItemEntity.setSalePrice(dto.getSalePrice());
        orderItemEntity.setQuantity(dto.getQuantity());
        orderItemEntity.setName(dto.getProductName());

        return orderItemEntity;

    };

    public static Function<OrderItem, OrderItemDto> orderItemEntityToDto = entity -> {
        OrderItemDto orderItemDto = OrderItemDto.builder().itemId(entity.getId()).salePrice(entity.getSalePrice())
                .retailPrice(entity.getRetailPrice()).quantity(entity.getQuantity()).productName(entity.getName())
                .build();

        return orderItemDto;
    };

    public static Function<OrderItemDto, OrderItemRequestDTO> orderItemDtoToRequest = orderItemDto -> {
        OrderItemRequestDTO req = new OrderItemRequestDTO();
        req.setQuantity(orderItemDto.getQuantity());
        req.setSkuId(orderItemDto.getSkuId());
        // req.setProductId(orderItemDto.getProductId());
        // req.setItemAttributes(orderItemDto.getAttributes());

        return req;
    };
    /******************************** ORDERITEM ********************************/

    /******************************** DISCRETEORDERITEM ********************************/

    public static Function<DiscreteOrderItemDto, DiscreteOrderItem> discreteIrderItemDtoToEntity = dto -> {
        DiscreteOrderItem orderItemEntity = new DiscreteOrderItemImpl();
        orderItemEntity.setName(dto.getProductName());
        orderItemEntity.setRetailPrice(dto.getRetailPrice());
        orderItemEntity.setSalePrice(dto.getSalePrice());
        orderItemEntity.setQuantity(dto.getQuantity());
        orderItemEntity.setName(dto.getProductName());
        // TODO: czy nulla wywali?
        orderItemEntity.setSku(catalogService.findSkuById(dto.getSkuId()));

        return orderItemEntity;

    };

    public static Function<DiscreteOrderItem, DiscreteOrderItemDto> discreteOrderItemEntityToDto = entity -> {
        Money errCode = new Money(BigDecimal.valueOf(-1337));
        Sku sku = entity.getSku();
        DiscreteOrderItemDto orderItemDto = DiscreteOrderItemDto.builder().itemId(entity.getId())
                .salePrice(entity.getSalePrice()).retailPrice(entity.getRetailPrice()).quantity(entity.getQuantity())
                .productName(entity.getName()).skuId(sku.getId()).description(sku.getDescription())
                .price(Optional.ofNullable(entity.getTotalPrice()).orElse(errCode).getAmount())
                        // .price(Optional.ofNullable(sku.getPrice()).orElse(errCode).getAmount())
                .build();

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

}