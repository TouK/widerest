package pl.touk.widerest.base;

public class ApiTestUrls {
    public static final String API_BASE_URL = "http://localhost:{port}/v1";

    /* Categories */
    public static final String CATEGORIES_URL = API_BASE_URL + "/categories";
    public static final String CATEGORIES_FLAT_URL = API_BASE_URL + "/categories?flat=true";
    public static final String CATEGORY_BY_ID_URL = CATEGORIES_URL + "/{categoryId}";
    public static final String CATEGORIES_COUNT_URL = CATEGORIES_URL + "/count";
    public static final String PRODUCTS_IN_CATEGORY_URL = CATEGORIES_URL + "/{categoryId}/products";
    public static final String PRODUCTS_IN_CATEGORY_BY_ID_URL = PRODUCTS_IN_CATEGORY_URL + "/{productId}";
    public static final String ADD_PRODUCTS_IN_CATEGORY_BY_ID_URL = PRODUCTS_IN_CATEGORY_URL + "?href=";
    public static final String PRODUCTS_IN_CATEGORY_COUNT_URL = PRODUCTS_IN_CATEGORY_URL + "/count";
    public static final String CATEGORY_AVAILABILITY_BY_ID_URL = CATEGORY_BY_ID_URL + "/availability";
    public static final String SUBCATEGORY_IN_CATEGORY_BY_ID_URL = CATEGORY_BY_ID_URL + "/subcategories";
    public static final String ADD_SUBCATEGORY_IN_CATEGORY_BY_ID_URL = SUBCATEGORY_IN_CATEGORY_BY_ID_URL + "?href=";

    /* Products */
    public static final String PRODUCTS_URL = API_BASE_URL + "/products";
    public static final String PRODUCT_BY_ID_URL = PRODUCTS_URL + "/{productId}";
    public static final String PRODUCTS_COUNT_URL = PRODUCTS_URL + "/count";
    public static final String PRODUCT_BY_ID_SKUS = PRODUCTS_URL + "/{productId}/skus";
    public static final String PRODUCT_BY_ID_SKU_BY_ID = PRODUCT_BY_ID_SKUS + "/{skuId}";
    public static final String PRODUCT_BY_ID_SKUS_DEFAULT = PRODUCT_BY_ID_SKUS + "/default";
    public static final String CATEGORIES_BY_PRODUCT_BY_ID_COUNT = PRODUCT_BY_ID_URL + "/categories/count";
    public static final String SKUS_COUNT_URL = PRODUCT_BY_ID_SKUS + "/count";
    public static final String MEDIA_BY_KEY_URL = PRODUCT_BY_ID_SKU_BY_ID + "/media/{key}";
    public static final String BUNDLES_URL = PRODUCTS_URL + "/bundles";
    public static final String BUNDLE_BU_ID_URL = BUNDLES_URL + "/{bundleId}";
    public static final String PRODUCT_BY_ID_ATTRIBUTES_URL = PRODUCT_BY_ID_URL + "/attributes";
    public static final String PRODUCT_BY_ID_ATTRIBUTE_BY_NAME_URL = PRODUCT_BY_ID_ATTRIBUTES_URL + "/{attributeName}";

    /* Orders */
    public static final String ORDERS_URL = API_BASE_URL + "/orders";
    public static final String ORDER_BY_ID_URL = ORDERS_URL + "/{orderId}";
    public static final String ORDERS_COUNT = ORDERS_URL+"/count";
    public static final String ORDERS_BY_ID_ITEMS = ORDER_BY_ID_URL + "/items";

    public static final String ORDER_BY_ID_FULFILLMENTS_URL = ORDER_BY_ID_URL + "/fulfillments";
    public static final String ORDER_BY_ID_FULFILLMENT_BY_ID_URL = ORDER_BY_ID_FULFILLMENTS_URL + "/{fulfillmentId}";

    /* PayPal */
    public static final String SYSTEM_PROPERTIES_URL = API_BASE_URL + "/settings";

    /* Customer */
    public static final String CUSTOMERS_URL = API_BASE_URL + "/customers";

    public static final String LOGIN_URL = "http://localhost:{port}/login";

    public static final String OAUTH_AUTHORIZATION = "http://localhost:{port}/oauth/authorize?client_id=default&scope=customer&response_type=token&redirect_uri=/";


    public static final String SETTINGS_URL = API_BASE_URL + "/settings";
    public static final String SETTINGS_BY_NAME_URL = SETTINGS_URL + "/{settingName}";

    public static final String PICTURE_UPLOAD_TOKEN_URL = API_BASE_URL + "/pictureUpload/requestToken";

    public static final String DEFAULT_CURRENCY_URL = API_BASE_URL + "/currency";

}
