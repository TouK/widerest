    create table BLC_ADDITIONAL_OFFER_INFO (
        BLC_ORDER_ORDER_ID int8 not null,
        OFFER_INFO_ID int8 not null,
        OFFER_ID int8 not null,
        primary key (BLC_ORDER_ORDER_ID, OFFER_ID)
    );

    create table BLC_ADDRESS (
        ADDRESS_ID int8 not null,
        ADDRESS_LINE1 varchar(255) not null,
        ADDRESS_LINE2 varchar(255),
        ADDRESS_LINE3 varchar(255),
        CITY varchar(255) not null,
        COMPANY_NAME varchar(255),
        COUNTY varchar(255),
        EMAIL_ADDRESS varchar(255),
        FAX varchar(255),
        FIRST_NAME varchar(255),
        FULL_NAME varchar(255),
        IS_ACTIVE boolean,
        IS_BUSINESS boolean,
        IS_DEFAULT boolean,
        IS_MAILING boolean,
        IS_STREET boolean,
        ISO_COUNTRY_SUB varchar(255),
        LAST_NAME varchar(255),
        POSTAL_CODE varchar(255),
        PRIMARY_PHONE varchar(255),
        SECONDARY_PHONE varchar(255),
        STANDARDIZED boolean,
        SUB_STATE_PROV_REG varchar(255),
        TOKENIZED_ADDRESS varchar(255),
        VERIFICATION_LEVEL varchar(255),
        ZIP_FOUR varchar(255),
        COUNTRY varchar(255),
        ISO_COUNTRY_ALPHA2 varchar(255),
        PHONE_FAX_ID int8,
        PHONE_PRIMARY_ID int8,
        PHONE_SECONDARY_ID int8,
        STATE_PROV_REGION varchar(255),
        primary key (ADDRESS_ID)
    );

    create table BLC_ADMIN_MODULE (
        ADMIN_MODULE_ID int8 not null,
        DISPLAY_ORDER int4,
        ICON varchar(255),
        MODULE_KEY varchar(255) not null,
        NAME varchar(255) not null,
        primary key (ADMIN_MODULE_ID)
    );

    create table BLC_ADMIN_PASSWORD_TOKEN (
        PASSWORD_TOKEN varchar(255) not null,
        ADMIN_USER_ID int8 not null,
        CREATE_DATE timestamp not null,
        TOKEN_USED_DATE timestamp,
        TOKEN_USED_FLAG boolean not null,
        primary key (PASSWORD_TOKEN)
    );

    create table BLC_ADMIN_PERMISSION (
        ADMIN_PERMISSION_ID int8 not null,
        DESCRIPTION varchar(255) not null,
        IS_FRIENDLY boolean,
        NAME varchar(255) not null,
        PERMISSION_TYPE varchar(255) not null,
        primary key (ADMIN_PERMISSION_ID)
    );

    create table BLC_ADMIN_PERMISSION_ENTITY (
        ADMIN_PERMISSION_ENTITY_ID int8 not null,
        CEILING_ENTITY varchar(255) not null,
        ADMIN_PERMISSION_ID int8,
        primary key (ADMIN_PERMISSION_ENTITY_ID)
    );

    create table BLC_ADMIN_PERMISSION_XREF (
        CHILD_PERMISSION_ID int8 not null,
        ADMIN_PERMISSION_ID int8 not null
    );

    create table BLC_ADMIN_ROLE (
        ADMIN_ROLE_ID int8 not null,
        DESCRIPTION varchar(255) not null,
        NAME varchar(255) not null,
        primary key (ADMIN_ROLE_ID)
    );

    create table BLC_ADMIN_ROLE_PERMISSION_XREF (
        ADMIN_PERMISSION_ID int8 not null,
        ADMIN_ROLE_ID int8 not null,
        primary key (ADMIN_ROLE_ID, ADMIN_PERMISSION_ID)
    );

    create table BLC_ADMIN_SECTION (
        ADMIN_SECTION_ID int8 not null,
        CEILING_ENTITY varchar(255),
        DISPLAY_CONTROLLER varchar(255),
        DISPLAY_ORDER int4,
        NAME varchar(255) not null,
        SECTION_KEY varchar(255) not null,
        URL varchar(255),
        USE_DEFAULT_HANDLER boolean,
        ADMIN_MODULE_ID int8 not null,
        primary key (ADMIN_SECTION_ID)
    );

    create table BLC_ADMIN_SEC_PERM_XREF (
        ADMIN_SECTION_ID int8 not null,
        ADMIN_PERMISSION_ID int8 not null
    );

    create table BLC_ADMIN_USER (
        ADMIN_USER_ID int8 not null,
        ACTIVE_STATUS_FLAG boolean,
        EMAIL varchar(255) not null,
        LOGIN varchar(255) not null,
        NAME varchar(255) not null,
        PASSWORD varchar(255),
        PHONE_NUMBER varchar(255),
        primary key (ADMIN_USER_ID)
    );

    create table BLC_ADMIN_USER_ADDTL_FIELDS (
        ATTRIBUTE_ID int8 not null,
        FIELD_NAME varchar(255) not null,
        FIELD_VALUE varchar(255),
        ADMIN_USER_ID int8 not null,
        primary key (ATTRIBUTE_ID)
    );

    create table BLC_ADMIN_USER_PERMISSION_XREF (
        ADMIN_PERMISSION_ID int8 not null,
        ADMIN_USER_ID int8 not null,
        primary key (ADMIN_USER_ID, ADMIN_PERMISSION_ID)
    );

    create table BLC_ADMIN_USER_ROLE_XREF (
        ADMIN_USER_ID int8 not null,
        ADMIN_ROLE_ID int8 not null,
        primary key (ADMIN_ROLE_ID, ADMIN_USER_ID)
    );

    create table BLC_ADMIN_USER_SANDBOX (
        SANDBOX_ID int8,
        ADMIN_USER_ID int8 not null,
        primary key (ADMIN_USER_ID)
    );

    create table BLC_ASSET_DESC_MAP (
        STATIC_ASSET_ID int8 not null,
        STATIC_ASSET_DESC_ID int8 not null,
        MAP_KEY varchar(255) not null,
        primary key (STATIC_ASSET_ID, MAP_KEY)
    );

    create table BLC_BUNDLE_ORDER_ITEM (
        BASE_RETAIL_PRICE numeric(19, 5),
        BASE_SALE_PRICE numeric(19, 5),
        ORDER_ITEM_ID int8 not null,
        PRODUCT_BUNDLE_ID int8,
        SKU_ID int8,
        primary key (ORDER_ITEM_ID)
    );

    create table BLC_BUND_ITEM_FEE_PRICE (
        BUND_ITEM_FEE_PRICE_ID int8 not null,
        AMOUNT numeric(19, 5),
        IS_TAXABLE boolean,
        NAME varchar(255),
        REPORTING_CODE varchar(255),
        BUND_ORDER_ITEM_ID int8 not null,
        primary key (BUND_ITEM_FEE_PRICE_ID)
    );

    create table BLC_CANDIDATE_FG_OFFER (
        CANDIDATE_FG_OFFER_ID int8 not null,
        DISCOUNTED_PRICE numeric(19, 5),
        FULFILLMENT_GROUP_ID int8,
        OFFER_ID int8 not null,
        primary key (CANDIDATE_FG_OFFER_ID)
    );

    create table BLC_CANDIDATE_ITEM_OFFER (
        CANDIDATE_ITEM_OFFER_ID int8 not null,
        DISCOUNTED_PRICE numeric(19, 5),
        OFFER_ID int8 not null,
        ORDER_ITEM_ID int8,
        primary key (CANDIDATE_ITEM_OFFER_ID)
    );

    create table BLC_CANDIDATE_ORDER_OFFER (
        CANDIDATE_ORDER_OFFER_ID int8 not null,
        DISCOUNTED_PRICE numeric(19, 5),
        OFFER_ID int8 not null,
        ORDER_ID int8,
        primary key (CANDIDATE_ORDER_OFFER_ID)
    );

    create table BLC_CATALOG (
        CATALOG_ID int8 not null,
        ARCHIVED char(1),
        NAME varchar(255),
        primary key (CATALOG_ID)
    );

    create table BLC_CATEGORY (
        CATEGORY_ID int8 not null,
        ACTIVE_END_DATE timestamp,
        ACTIVE_START_DATE timestamp,
        ARCHIVED char(1),
        DESCRIPTION varchar(255),
        DISPLAY_TEMPLATE varchar(255),
        EXTERNAL_ID varchar(255),
        FULFILLMENT_TYPE varchar(255),
        INVENTORY_TYPE varchar(255),
        LONG_DESCRIPTION text,
        NAME varchar(255) not null,
        OVERRIDE_GENERATED_URL boolean,
        TAX_CODE varchar(255),
        URL varchar(255),
        URL_KEY varchar(255),
        DEFAULT_PARENT_CATEGORY_ID int8,
        primary key (CATEGORY_ID)
    );

    create table BLC_CATEGORY_ATTRIBUTE (
        CATEGORY_ATTRIBUTE_ID int8 not null,
        NAME varchar(255) not null,
        SEARCHABLE boolean,
        VALUE varchar(255),
        CATEGORY_ID int8 not null,
        primary key (CATEGORY_ATTRIBUTE_ID)
    );

    create table BLC_CATEGORY_MEDIA_MAP (
        CATEGORY_MEDIA_ID int8 not null,
        MAP_KEY varchar(255) not null,
        BLC_CATEGORY_CATEGORY_ID int8 not null,
        MEDIA_ID int8,
        primary key (CATEGORY_MEDIA_ID)
    );

    create table BLC_CATEGORY_PRODUCT_XREF (
        CATEGORY_PRODUCT_ID int8 not null,
        DEFAULT_REFERENCE boolean,
        DISPLAY_ORDER numeric(10, 6),
        CATEGORY_ID int8 not null,
        PRODUCT_ID int8 not null,
        primary key (CATEGORY_PRODUCT_ID)
    );

    create table BLC_CATEGORY_XREF (
        CATEGORY_XREF_ID int8 not null,
        DEFAULT_REFERENCE boolean,
        DISPLAY_ORDER numeric(10, 6),
        CATEGORY_ID int8 not null,
        SUB_CATEGORY_ID int8 not null,
        primary key (CATEGORY_XREF_ID)
    );

    create table BLC_CAT_SEARCH_FACET_EXCL_XREF (
        CAT_EXCL_SEARCH_FACET_ID int8 not null,
        SEQUENCE numeric(19, 2),
        CATEGORY_ID int8,
        SEARCH_FACET_ID int8,
        primary key (CAT_EXCL_SEARCH_FACET_ID)
    );

    create table BLC_CAT_SEARCH_FACET_XREF (
        CATEGORY_SEARCH_FACET_ID int8 not null,
        SEQUENCE numeric(19, 2),
        CATEGORY_ID int8,
        SEARCH_FACET_ID int8,
        primary key (CATEGORY_SEARCH_FACET_ID)
    );

    create table BLC_CAT_SITE_MAP_GEN_CFG (
        ENDING_DEPTH int4 not null,
        STARTING_DEPTH int4 not null,
        GEN_CONFIG_ID int8 not null,
        ROOT_CATEGORY_ID int8 not null,
        primary key (GEN_CONFIG_ID)
    );

    create table BLC_CHALLENGE_QUESTION (
        QUESTION_ID int8 not null,
        QUESTION varchar(255) not null,
        primary key (QUESTION_ID)
    );

    create table BLC_CODE_TYPES (
        CODE_ID int8 not null,
        CODE_TYPE varchar(255) not null,
        CODE_DESC varchar(255),
        CODE_KEY varchar(255) not null,
        MODIFIABLE char(1),
        primary key (CODE_ID)
    );

    create table BLC_COUNTRY (
        ABBREVIATION varchar(255) not null,
        NAME varchar(255) not null,
        primary key (ABBREVIATION)
    );

    create table BLC_COUNTRY_SUB (
        ABBREVIATION varchar(255) not null,
        ALT_ABBREVIATION varchar(255),
        NAME varchar(255) not null,
        COUNTRY_SUB_CAT int8,
        COUNTRY varchar(255) not null,
        primary key (ABBREVIATION)
    );

    create table BLC_COUNTRY_SUB_CAT (
        COUNTRY_SUB_CAT_ID int8 not null,
        NAME varchar(255) not null,
        primary key (COUNTRY_SUB_CAT_ID)
    );

    create table BLC_CURRENCY (
        CURRENCY_CODE varchar(255) not null,
        DEFAULT_FLAG boolean,
        FRIENDLY_NAME varchar(255),
        primary key (CURRENCY_CODE)
    );

    create table BLC_CUSTOMER (
        CUSTOMER_ID int8 not null,
        CREATED_BY int8,
        DATE_CREATED timestamp,
        DATE_UPDATED timestamp,
        UPDATED_BY int8,
        CHALLENGE_ANSWER varchar(255),
        DEACTIVATED boolean,
        EMAIL_ADDRESS varchar(255),
        FIRST_NAME varchar(255),
        LAST_NAME varchar(255),
        PASSWORD varchar(255),
        PASSWORD_CHANGE_REQUIRED boolean,
        IS_PREVIEW boolean,
        RECEIVE_EMAIL boolean,
        IS_REGISTERED boolean,
        TAX_EXEMPTION_CODE varchar(255),
        USER_NAME varchar(255),
        CHALLENGE_QUESTION_ID int8,
        LOCALE_CODE varchar(255),
        primary key (CUSTOMER_ID)
    );

    create table BLC_CUSTOMER_ADDRESS (
        CUSTOMER_ADDRESS_ID int8 not null,
        ADDRESS_NAME varchar(255),
        ARCHIVED char(1),
        ADDRESS_ID int8 not null,
        CUSTOMER_ID int8 not null,
        primary key (CUSTOMER_ADDRESS_ID)
    );

    create table BLC_CUSTOMER_ATTRIBUTE (
        CUSTOMER_ATTR_ID int8 not null,
        NAME varchar(255) not null,
        VALUE varchar(255),
        CUSTOMER_ID int8 not null,
        primary key (CUSTOMER_ATTR_ID)
    );

    create table BLC_CUSTOMER_OFFER_XREF (
        CUSTOMER_OFFER_ID int8 not null,
        CUSTOMER_ID int8 not null,
        OFFER_ID int8 not null,
        primary key (CUSTOMER_OFFER_ID)
    );

    create table BLC_CUSTOMER_PASSWORD_TOKEN (
        PASSWORD_TOKEN varchar(255) not null,
        CREATE_DATE timestamp not null,
        CUSTOMER_ID int8 not null,
        TOKEN_USED_DATE timestamp,
        TOKEN_USED_FLAG boolean not null,
        primary key (PASSWORD_TOKEN)
    );

    create table BLC_CUSTOMER_PAYMENT (
        CUSTOMER_PAYMENT_ID int8 not null,
        IS_DEFAULT boolean,
        PAYMENT_TOKEN varchar(255),
        ADDRESS_ID int8,
        CUSTOMER_ID int8 not null,
        primary key (CUSTOMER_PAYMENT_ID)
    );

    create table BLC_CUSTOMER_PAYMENT_FIELDS (
        CUSTOMER_PAYMENT_ID int8 not null,
        FIELD_VALUE text,
        FIELD_NAME varchar(255) not null,
        primary key (CUSTOMER_PAYMENT_ID, FIELD_NAME)
    );

    create table BLC_CUSTOMER_PHONE (
        CUSTOMER_PHONE_ID int8 not null,
        PHONE_NAME varchar(255),
        CUSTOMER_ID int8 not null,
        PHONE_ID int8 not null,
        primary key (CUSTOMER_PHONE_ID)
    );

    create table BLC_CUSTOMER_ROLE (
        CUSTOMER_ROLE_ID int8 not null,
        CUSTOMER_ID int8 not null,
        ROLE_ID int8 not null,
        primary key (CUSTOMER_ROLE_ID)
    );

    create table BLC_CUST_SITE_MAP_GEN_CFG (
        GEN_CONFIG_ID int8 not null,
        primary key (GEN_CONFIG_ID)
    );

    create table BLC_DATA_DRVN_ENUM (
        ENUM_ID int8 not null,
        ENUM_KEY varchar(255),
        MODIFIABLE boolean,
        primary key (ENUM_ID)
    );

    create table BLC_DATA_DRVN_ENUM_VAL (
        ENUM_VAL_ID int8 not null,
        DISPLAY varchar(255),
        HIDDEN boolean,
        ENUM_KEY varchar(255),
        ENUM_TYPE int8,
        primary key (ENUM_VAL_ID)
    );

    create table BLC_DISCRETE_ORDER_ITEM (
        BASE_RETAIL_PRICE numeric(19, 5),
        BASE_SALE_PRICE numeric(19, 5),
        ORDER_ITEM_ID int8 not null,
        BUNDLE_ORDER_ITEM_ID int8,
        PRODUCT_ID int8,
        SKU_ID int8 not null,
        SKU_BUNDLE_ITEM_ID int8,
        primary key (ORDER_ITEM_ID)
    );

    create table BLC_DISC_ITEM_FEE_PRICE (
        DISC_ITEM_FEE_PRICE_ID int8 not null,
        AMOUNT numeric(19, 5),
        NAME varchar(255),
        REPORTING_CODE varchar(255),
        ORDER_ITEM_ID int8 not null,
        primary key (DISC_ITEM_FEE_PRICE_ID)
    );

    create table BLC_DYN_DISCRETE_ORDER_ITEM (
        ORDER_ITEM_ID int8 not null,
        primary key (ORDER_ITEM_ID)
    );

    create table BLC_EMAIL_TRACKING (
        EMAIL_TRACKING_ID int8 not null,
        DATE_SENT timestamp,
        EMAIL_ADDRESS varchar(255),
        TYPE varchar(255),
        primary key (EMAIL_TRACKING_ID)
    );

    create table BLC_EMAIL_TRACKING_CLICKS (
        CLICK_ID int8 not null,
        CUSTOMER_ID varchar(255),
        DATE_CLICKED timestamp not null,
        DESTINATION_URI varchar(255),
        QUERY_STRING varchar(255),
        EMAIL_TRACKING_ID int8 not null,
        primary key (CLICK_ID)
    );

    create table BLC_EMAIL_TRACKING_OPENS (
        OPEN_ID int8 not null,
        DATE_OPENED timestamp,
        USER_AGENT varchar(255),
        EMAIL_TRACKING_ID int8,
        primary key (OPEN_ID)
    );

    create table BLC_EXT_SKU (
        BID_PRICE numeric(19, 5),
        MINIMAL_PRICE numeric(19, 5),
        SKU_ID int8 not null,
        primary key (SKU_ID)
    );

    create table BLC_FG_ADJUSTMENT (
        FG_ADJUSTMENT_ID int8 not null,
        ADJUSTMENT_REASON varchar(255) not null,
        ADJUSTMENT_VALUE numeric(19, 5) not null,
        FULFILLMENT_GROUP_ID int8,
        OFFER_ID int8 not null,
        primary key (FG_ADJUSTMENT_ID)
    );

    create table BLC_FG_FEE_TAX_XREF (
        FULFILLMENT_GROUP_FEE_ID int8 not null,
        TAX_DETAIL_ID int8 not null
    );

    create table BLC_FG_FG_TAX_XREF (
        FULFILLMENT_GROUP_ID int8 not null,
        TAX_DETAIL_ID int8 not null
    );

    create table BLC_FG_ITEM_TAX_XREF (
        FULFILLMENT_GROUP_ITEM_ID int8 not null,
        TAX_DETAIL_ID int8 not null
    );

    create table BLC_FIELD (
        FIELD_ID int8 not null,
        ABBREVIATION varchar(255),
        ENTITY_TYPE varchar(255) not null,
        FACET_FIELD_TYPE varchar(255),
        FRIENDLY_NAME varchar(255),
        PROPERTY_NAME varchar(255) not null,
        SEARCHABLE boolean,
        TRANSLATABLE boolean,
        primary key (FIELD_ID)
    );

    create table BLC_FIELD_SEARCH_TYPES (
        FIELD_ID int8 not null,
        SEARCHABLE_FIELD_TYPE varchar(255)
    );

    create table BLC_FLD_DEF (
        FLD_DEF_ID int8 not null,
        ALLOW_MULTIPLES boolean,
        COLUMN_WIDTH varchar(255),
        FLD_ORDER int4,
        FLD_TYPE varchar(255),
        FRIENDLY_NAME varchar(255),
        HELP_TEXT varchar(255),
        HIDDEN_FLAG boolean,
        HINT varchar(255),
        MAX_LENGTH int4,
        NAME varchar(255),
        REQUIRED_FLAG boolean,
        SECURITY_LEVEL varchar(255),
        TEXT_AREA_FLAG boolean,
        TOOLTIP varchar(255),
        VLDTN_ERROR_MSSG_KEY varchar(255),
        VLDTN_REGEX varchar(255),
        ENUM_ID int8,
        FLD_GROUP_ID int8,
        primary key (FLD_DEF_ID)
    );

    create table BLC_FLD_ENUM (
        FLD_ENUM_ID int8 not null,
        NAME varchar(255),
        primary key (FLD_ENUM_ID)
    );

    create table BLC_FLD_ENUM_ITEM (
        FLD_ENUM_ITEM_ID int8 not null,
        FLD_ORDER int4,
        FRIENDLY_NAME varchar(255),
        NAME varchar(255),
        FLD_ENUM_ID int8,
        primary key (FLD_ENUM_ITEM_ID)
    );

    create table BLC_FLD_GROUP (
        FLD_GROUP_ID int8 not null,
        INIT_COLLAPSED_FLAG boolean,
        NAME varchar(255),
        primary key (FLD_GROUP_ID)
    );

    create table BLC_FULFILLMENT_GROUP (
        FULFILLMENT_GROUP_ID int8 not null,
        DELIVERY_INSTRUCTION varchar(255),
        PRICE numeric(19, 5),
        SHIPPING_PRICE_TAXABLE boolean,
        MERCHANDISE_TOTAL numeric(19, 5),
        METHOD varchar(255),
        IS_PRIMARY boolean,
        REFERENCE_NUMBER varchar(255),
        RETAIL_PRICE numeric(19, 5),
        SALE_PRICE numeric(19, 5),
        FULFILLMENT_GROUP_SEQUNCE int4,
        SERVICE varchar(255),
        SHIPPING_OVERRIDE boolean,
        STATUS varchar(255),
        TOTAL numeric(19, 5),
        TOTAL_FEE_TAX numeric(19, 5),
        TOTAL_FG_TAX numeric(19, 5),
        TOTAL_ITEM_TAX numeric(19, 5),
        TOTAL_TAX numeric(19, 5),
        TYPE varchar(255),
        ADDRESS_ID int8,
        FULFILLMENT_OPTION_ID int8,
        ORDER_ID int8 not null,
        PERSONAL_MESSAGE_ID int8,
        PHONE_ID int8,
        primary key (FULFILLMENT_GROUP_ID)
    );

    create table BLC_FULFILLMENT_GROUP_FEE (
        FULFILLMENT_GROUP_FEE_ID int8 not null,
        AMOUNT numeric(19, 5),
        FEE_TAXABLE_FLAG boolean,
        NAME varchar(255),
        REPORTING_CODE varchar(255),
        TOTAL_FEE_TAX numeric(19, 5),
        FULFILLMENT_GROUP_ID int8 not null,
        primary key (FULFILLMENT_GROUP_FEE_ID)
    );

    create table BLC_FULFILLMENT_GROUP_ITEM (
        FULFILLMENT_GROUP_ITEM_ID int8 not null,
        PRORATED_ORDER_ADJ numeric(19, 2),
        QUANTITY int4 not null,
        STATUS varchar(255),
        TOTAL_ITEM_AMOUNT numeric(19, 5),
        TOTAL_ITEM_TAXABLE_AMOUNT numeric(19, 5),
        TOTAL_ITEM_TAX numeric(19, 5),
        FULFILLMENT_GROUP_ID int8 not null,
        ORDER_ITEM_ID int8 not null,
        primary key (FULFILLMENT_GROUP_ITEM_ID)
    );

    create table BLC_FULFILLMENT_OPTION (
        FULFILLMENT_OPTION_ID int8 not null,
        FULFILLMENT_TYPE varchar(255) not null,
        LONG_DESCRIPTION text,
        NAME varchar(255),
        TAX_CODE varchar(255),
        TAXABLE boolean,
        USE_FLAT_RATES boolean,
        primary key (FULFILLMENT_OPTION_ID)
    );

    create table BLC_FULFILLMENT_OPTION_FIXED (
        PRICE numeric(19, 5) not null,
        FULFILLMENT_OPTION_ID int8 not null,
        CURRENCY_CODE varchar(255),
        primary key (FULFILLMENT_OPTION_ID)
    );

    create table BLC_FULFILLMENT_OPT_BANDED_PRC (
        FULFILLMENT_OPTION_ID int8 not null,
        primary key (FULFILLMENT_OPTION_ID)
    );

    create table BLC_FULFILLMENT_OPT_BANDED_WGT (
        FULFILLMENT_OPTION_ID int8 not null,
        primary key (FULFILLMENT_OPTION_ID)
    );

    create table BLC_FULFILLMENT_PRICE_BAND (
        FULFILLMENT_PRICE_BAND_ID int8 not null,
        RESULT_AMOUNT numeric(19, 5) not null,
        RESULT_AMOUNT_TYPE varchar(255) not null,
        RETAIL_PRICE_MINIMUM_AMOUNT numeric(19, 5) not null,
        FULFILLMENT_OPTION_ID int8,
        primary key (FULFILLMENT_PRICE_BAND_ID)
    );

    create table BLC_FULFILLMENT_WEIGHT_BAND (
        FULFILLMENT_WEIGHT_BAND_ID int8 not null,
        RESULT_AMOUNT numeric(19, 5) not null,
        RESULT_AMOUNT_TYPE varchar(255) not null,
        MINIMUM_WEIGHT numeric(19, 5),
        WEIGHT_UNIT_OF_MEASURE varchar(255),
        FULFILLMENT_OPTION_ID int8,
        primary key (FULFILLMENT_WEIGHT_BAND_ID)
    );

    create table BLC_GIFTWRAP_ORDER_ITEM (
        ORDER_ITEM_ID int8 not null,
        primary key (ORDER_ITEM_ID)
    );

    create table BLC_ID_GENERATION (
        ID_TYPE varchar(255) not null,
        BATCH_SIZE int8 not null,
        BATCH_START int8 not null,
        ID_MIN int8,
        ID_MAX int8,
        version int4,
        primary key (ID_TYPE)
    );

    create table BLC_IMG_STATIC_ASSET (
        HEIGHT int4,
        WIDTH int4,
        STATIC_ASSET_ID int8 not null,
        primary key (STATIC_ASSET_ID)
    );

    create table BLC_ISO_COUNTRY (
        ALPHA_2 varchar(255) not null,
        ALPHA_3 varchar(255),
        NAME varchar(255),
        NUMERIC_CODE int4,
        STATUS varchar(255),
        primary key (ALPHA_2)
    );

    create table BLC_ITEM_OFFER_QUALIFIER (
        ITEM_OFFER_QUALIFIER_ID int8 not null,
        QUANTITY int8,
        OFFER_ID int8 not null,
        ORDER_ITEM_ID int8,
        primary key (ITEM_OFFER_QUALIFIER_ID)
    );

    create table BLC_LOCALE (
        LOCALE_CODE varchar(255) not null,
        DEFAULT_FLAG boolean,
        FRIENDLY_NAME varchar(255),
        USE_IN_SEARCH_INDEX boolean,
        CURRENCY_CODE varchar(255),
        primary key (LOCALE_CODE)
    );

    create table BLC_MEDIA (
        MEDIA_ID int8 not null,
        ALT_TEXT varchar(255),
        TAGS varchar(255),
        TITLE varchar(255),
        URL varchar(255) not null,
        primary key (MEDIA_ID)
    );

    create table BLC_MODULE_CONFIGURATION (
        MODULE_CONFIG_ID int8 not null,
        ACTIVE_END_DATE timestamp,
        ACTIVE_START_DATE timestamp,
        ARCHIVED char(1),
        CREATED_BY int8,
        DATE_CREATED timestamp,
        DATE_UPDATED timestamp,
        UPDATED_BY int8,
        CONFIG_TYPE varchar(255) not null,
        IS_DEFAULT boolean not null,
        MODULE_NAME varchar(255) not null,
        MODULE_PRIORITY int4 not null,
        primary key (MODULE_CONFIG_ID)
    );

    create table BLC_OFFER (
        OFFER_ID int8 not null,
        APPLIES_WHEN_RULES text,
        APPLIES_TO_RULES text,
        APPLY_OFFER_TO_MARKED_ITEMS boolean,
        APPLY_TO_SALE_PRICE boolean,
        ARCHIVED char(1),
        AUTOMATICALLY_ADDED boolean,
        COMBINABLE_WITH_OTHER_OFFERS boolean,
        OFFER_DELIVERY_TYPE varchar(255),
        OFFER_DESCRIPTION varchar(255),
        OFFER_DISCOUNT_TYPE varchar(255),
        END_DATE timestamp,
        MARKETING_MESSASGE varchar(255),
        MAX_USES_PER_CUSTOMER int8,
        MAX_USES int4,
        OFFER_NAME varchar(255) not null,
        OFFER_ITEM_QUALIFIER_RULE varchar(255),
        OFFER_ITEM_TARGET_RULE varchar(255),
        OFFER_PRIORITY int4,
        QUALIFYING_ITEM_MIN_TOTAL numeric(19, 5),
        REQUIRES_RELATED_TAR_QUAL boolean,
        STACKABLE boolean,
        START_DATE timestamp,
        TARGET_SYSTEM varchar(255),
        TOTALITARIAN_OFFER boolean,
        USE_NEW_FORMAT boolean,
        OFFER_TYPE varchar(255) not null,
        USES int4,
        OFFER_VALUE numeric(19, 5) not null,
        primary key (OFFER_ID)
    );

    create table BLC_OFFER_AUDIT (
        OFFER_AUDIT_ID int8 not null,
        CUSTOMER_ID int8,
        OFFER_CODE_ID int8,
        OFFER_ID int8,
        ORDER_ID int8,
        REDEEMED_DATE timestamp,
        primary key (OFFER_AUDIT_ID)
    );

    create table BLC_OFFER_CODE (
        OFFER_CODE_ID int8 not null,
        ARCHIVED char(1),
        MAX_USES int4,
        OFFER_CODE varchar(255) not null,
        END_DATE timestamp,
        START_DATE timestamp,
        USES int4,
        OFFER_ID int8 not null,
        primary key (OFFER_CODE_ID)
    );

    create table BLC_OFFER_INFO (
        OFFER_INFO_ID int8 not null,
        primary key (OFFER_INFO_ID)
    );

    create table BLC_OFFER_INFO_FIELDS (
        OFFER_INFO_FIELDS_ID int8 not null,
        FIELD_VALUE varchar(255),
        FIELD_NAME varchar(255) not null,
        primary key (OFFER_INFO_FIELDS_ID, FIELD_NAME)
    );

    create table BLC_OFFER_ITEM_CRITERIA (
        OFFER_ITEM_CRITERIA_ID int8 not null,
        ORDER_ITEM_MATCH_RULE text,
        QUANTITY int4 not null,
        primary key (OFFER_ITEM_CRITERIA_ID)
    );

    create table BLC_OFFER_RULE (
        OFFER_RULE_ID int8 not null,
        MATCH_RULE text,
        primary key (OFFER_RULE_ID)
    );

    create table BLC_OFFER_RULE_MAP (
        OFFER_OFFER_RULE_ID int8 not null,
        MAP_KEY varchar(255) not null,
        BLC_OFFER_OFFER_ID int8 not null,
        OFFER_RULE_ID int8,
        primary key (OFFER_OFFER_RULE_ID)
    );

    create table BLC_ORDER (
        ORDER_ID int8 not null,
        CREATED_BY int8,
        DATE_CREATED timestamp,
        DATE_UPDATED timestamp,
        UPDATED_BY int8,
        EMAIL_ADDRESS varchar(255),
        NAME varchar(255),
        ORDER_NUMBER varchar(255),
        IS_PREVIEW boolean,
        ORDER_STATUS varchar(255),
        ORDER_SUBTOTAL numeric(19, 5),
        SUBMIT_DATE timestamp,
        TAX_OVERRIDE boolean,
        ORDER_TOTAL numeric(19, 5),
        TOTAL_SHIPPING numeric(19, 5),
        TOTAL_TAX numeric(19, 5),
        CURRENCY_CODE varchar(255),
        CUSTOMER_ID int8 not null,
        LOCALE_CODE varchar(255),
        primary key (ORDER_ID)
    );

    create table BLC_ORDER_ADJUSTMENT (
        ORDER_ADJUSTMENT_ID int8 not null,
        ADJUSTMENT_REASON varchar(255) not null,
        ADJUSTMENT_VALUE numeric(19, 5) not null,
        OFFER_ID int8 not null,
        ORDER_ID int8,
        primary key (ORDER_ADJUSTMENT_ID)
    );

    create table BLC_ORDER_ATTRIBUTE (
        ORDER_ATTRIBUTE_ID int8 not null,
        NAME varchar(255) not null,
        VALUE varchar(255),
        ORDER_ID int8 not null,
        primary key (ORDER_ATTRIBUTE_ID)
    );

    create table BLC_ORDER_ITEM (
        ORDER_ITEM_ID int8 not null,
        DISCOUNTS_ALLOWED boolean,
        ITEM_TAXABLE_FLAG boolean,
        NAME varchar(255),
        ORDER_ITEM_TYPE varchar(255),
        PRICE numeric(19, 5),
        QUANTITY int4 not null,
        RETAIL_PRICE numeric(19, 5),
        RETAIL_PRICE_OVERRIDE boolean,
        SALE_PRICE numeric(19, 5),
        SALE_PRICE_OVERRIDE boolean,
        TOTAL_TAX numeric(19, 2),
        CATEGORY_ID int8,
        GIFT_WRAP_ITEM_ID int8,
        ORDER_ID int8,
        PARENT_ORDER_ITEM_ID int8,
        PERSONAL_MESSAGE_ID int8,
        primary key (ORDER_ITEM_ID)
    );

    create table BLC_ORDER_ITEM_ADD_ATTR (
        ORDER_ITEM_ID int8 not null,
        VALUE varchar(255),
        NAME varchar(255) not null,
        primary key (ORDER_ITEM_ID, NAME)
    );

    create table BLC_ORDER_ITEM_ADJUSTMENT (
        ORDER_ITEM_ADJUSTMENT_ID int8 not null,
        APPLIED_TO_SALE_PRICE boolean,
        ADJUSTMENT_REASON varchar(255) not null,
        ADJUSTMENT_VALUE numeric(19, 5) not null,
        OFFER_ID int8 not null,
        ORDER_ITEM_ID int8,
        primary key (ORDER_ITEM_ADJUSTMENT_ID)
    );

    create table BLC_ORDER_ITEM_ATTRIBUTE (
        ORDER_ITEM_ATTRIBUTE_ID int8 not null,
        NAME varchar(255) not null,
        VALUE varchar(255) not null,
        ORDER_ITEM_ID int8 not null,
        primary key (ORDER_ITEM_ATTRIBUTE_ID)
    );

    create table BLC_ORDER_ITEM_DTL_ADJ (
        ORDER_ITEM_DTL_ADJ_ID int8 not null,
        APPLIED_TO_SALE_PRICE boolean,
        OFFER_NAME varchar(255),
        ADJUSTMENT_REASON varchar(255) not null,
        ADJUSTMENT_VALUE numeric(19, 5) not null,
        OFFER_ID int8 not null,
        ORDER_ITEM_PRICE_DTL_ID int8,
        primary key (ORDER_ITEM_DTL_ADJ_ID)
    );

    create table BLC_ORDER_ITEM_PRICE_DTL (
        ORDER_ITEM_PRICE_DTL_ID int8 not null,
        QUANTITY int4 not null,
        USE_SALE_PRICE boolean,
        ORDER_ITEM_ID int8,
        primary key (ORDER_ITEM_PRICE_DTL_ID)
    );

    create table BLC_ORDER_LOCK (
        LOCK_KEY varchar(255) not null,
        ORDER_ID int8 not null,
        LAST_UPDATED int8,
        LOCKED char(1),
        primary key (LOCK_KEY, ORDER_ID)
    );

    create table BLC_ORDER_MULTISHIP_OPTION (
        ORDER_MULTISHIP_OPTION_ID int8 not null,
        ADDRESS_ID int8,
        FULFILLMENT_OPTION_ID int8,
        ORDER_ID int8,
        ORDER_ITEM_ID int8,
        primary key (ORDER_MULTISHIP_OPTION_ID)
    );

    create table BLC_ORDER_OFFER_CODE_XREF (
        ORDER_ID int8 not null,
        OFFER_CODE_ID int8 not null
    );

    create table BLC_ORDER_PAYMENT (
        ORDER_PAYMENT_ID int8 not null,
        AMOUNT numeric(19, 5),
        ARCHIVED char(1),
        GATEWAY_TYPE varchar(255),
        REFERENCE_NUMBER varchar(255),
        PAYMENT_TYPE varchar(255) not null,
        ADDRESS_ID int8,
        ORDER_ID int8,
        primary key (ORDER_PAYMENT_ID)
    );

    create table BLC_ORDER_PAYMENT_TRANSACTION (
        PAYMENT_TRANSACTION_ID int8 not null,
        TRANSACTION_AMOUNT numeric(19, 2),
        ARCHIVED char(1),
        CUSTOMER_IP_ADDRESS varchar(255),
        DATE_RECORDED timestamp,
        RAW_RESPONSE text,
        SUCCESS boolean,
        TRANSACTION_TYPE varchar(255),
        ORDER_PAYMENT int8 not null,
        PARENT_TRANSACTION int8,
        primary key (PAYMENT_TRANSACTION_ID)
    );

    create table BLC_PAGE (
        PAGE_ID int8 not null,
        ACTIVE_END_DATE timestamp,
        ACTIVE_START_DATE timestamp,
        CREATED_BY int8,
        DATE_CREATED timestamp,
        DATE_UPDATED timestamp,
        UPDATED_BY int8,
        DESCRIPTION varchar(255),
        EXCLUDE_FROM_SITE_MAP boolean,
        FULL_URL varchar(255),
        META_DESCRIPTION varchar(255),
        META_TITLE varchar(255),
        OFFLINE_FLAG boolean,
        PRIORITY int4,
        PAGE_TMPLT_ID int8,
        primary key (PAGE_ID)
    );

    create table BLC_PAGE_ATTRIBUTES (
        ATTRIBUTE_ID int8 not null,
        FIELD_NAME varchar(255) not null,
        FIELD_VALUE varchar(255),
        PAGE_ID int8 not null,
        primary key (ATTRIBUTE_ID)
    );

    create table BLC_PAGE_FLD (
        PAGE_FLD_ID int8 not null,
        CREATED_BY int8,
        DATE_CREATED timestamp,
        DATE_UPDATED timestamp,
        UPDATED_BY int8,
        FLD_KEY varchar(255),
        LOB_VALUE text,
        VALUE varchar(255),
        PAGE_ID int8 not null,
        primary key (PAGE_FLD_ID)
    );

    create table BLC_PAGE_ITEM_CRITERIA (
        PAGE_ITEM_CRITERIA_ID int8 not null,
        ORDER_ITEM_MATCH_RULE text,
        QUANTITY int4 not null,
        primary key (PAGE_ITEM_CRITERIA_ID)
    );

    create table BLC_PAGE_RULE (
        PAGE_RULE_ID int8 not null,
        MATCH_RULE text,
        primary key (PAGE_RULE_ID)
    );

    create table BLC_PAGE_RULE_MAP (
        BLC_PAGE_PAGE_ID int8 not null,
        PAGE_RULE_ID int8 not null,
        MAP_KEY varchar(255) not null,
        primary key (BLC_PAGE_PAGE_ID, MAP_KEY)
    );

    create table BLC_PAGE_TMPLT (
        PAGE_TMPLT_ID int8 not null,
        TMPLT_DESCR varchar(255),
        TMPLT_NAME varchar(255),
        TMPLT_PATH varchar(255),
        LOCALE_CODE varchar(255),
        primary key (PAGE_TMPLT_ID)
    );

    create table BLC_PAYMENT_LOG (
        PAYMENT_LOG_ID int8 not null,
        AMOUNT_PAID numeric(19, 5),
        EXCEPTION_MESSAGE varchar(255),
        LOG_TYPE varchar(255) not null,
        ORDER_PAYMENT_ID int8,
        ORDER_PAYMENT_REF_NUM varchar(255),
        TRANSACTION_SUCCESS boolean,
        TRANSACTION_TIMESTAMP timestamp not null,
        TRANSACTION_TYPE varchar(255) not null,
        USER_NAME varchar(255) not null,
        CURRENCY_CODE varchar(255),
        CUSTOMER_ID int8,
        primary key (PAYMENT_LOG_ID)
    );

    create table BLC_PERSONAL_MESSAGE (
        PERSONAL_MESSAGE_ID int8 not null,
        MESSAGE varchar(255),
        MESSAGE_FROM varchar(255),
        MESSAGE_TO varchar(255),
        OCCASION varchar(255),
        primary key (PERSONAL_MESSAGE_ID)
    );

    create table BLC_PGTMPLT_FLDGRP_XREF (
        PG_TMPLT_FLD_GRP_ID int8 not null,
        GROUP_ORDER numeric(10, 6),
        FLD_GROUP_ID int8,
        PAGE_TMPLT_ID int8,
        primary key (PG_TMPLT_FLD_GRP_ID)
    );

    create table BLC_PHONE (
        PHONE_ID int8 not null,
        COUNTRY_CODE varchar(255),
        EXTENSION varchar(255),
        IS_ACTIVE boolean,
        IS_DEFAULT boolean,
        PHONE_NUMBER varchar(255) not null,
        primary key (PHONE_ID)
    );

    create table BLC_PRODUCT (
        PRODUCT_ID int8 not null,
        ARCHIVED char(1),
        CAN_SELL_WITHOUT_OPTIONS boolean,
        DISPLAY_TEMPLATE varchar(255),
        IS_FEATURED_PRODUCT boolean not null,
        MANUFACTURE varchar(255),
        MODEL varchar(255),
        OVERRIDE_GENERATED_URL boolean,
        URL varchar(255),
        URL_KEY varchar(255),
        DEFAULT_CATEGORY_ID int8,
        DEFAULT_SKU_ID int8,
        primary key (PRODUCT_ID)
    );

    create table BLC_PRODUCT_ATTRIBUTE (
        PRODUCT_ATTRIBUTE_ID int8 not null,
        NAME varchar(255) not null,
        SEARCHABLE boolean,
        VALUE varchar(255),
        PRODUCT_ID int8 not null,
        primary key (PRODUCT_ATTRIBUTE_ID)
    );

    create table BLC_PRODUCT_BUNDLE (
        AUTO_BUNDLE boolean,
        BUNDLE_PROMOTABLE boolean,
        ITEMS_PROMOTABLE boolean,
        PRICING_MODEL varchar(255),
        BUNDLE_PRIORITY int4,
        PRODUCT_ID int8 not null,
        primary key (PRODUCT_ID)
    );

    create table BLC_PRODUCT_CROSS_SALE (
        CROSS_SALE_PRODUCT_ID int8 not null,
        PROMOTION_MESSAGE varchar(255),
        SEQUENCE numeric(10, 6),
        CATEGORY_ID int8,
        PRODUCT_ID int8,
        RELATED_SALE_PRODUCT_ID int8 not null,
        primary key (CROSS_SALE_PRODUCT_ID)
    );

    create table BLC_PRODUCT_FEATURED (
        FEATURED_PRODUCT_ID int8 not null,
        PROMOTION_MESSAGE varchar(255),
        SEQUENCE numeric(10, 6),
        CATEGORY_ID int8,
        PRODUCT_ID int8,
        primary key (FEATURED_PRODUCT_ID)
    );

    create table BLC_PRODUCT_OPTION (
        PRODUCT_OPTION_ID int8 not null,
        ATTRIBUTE_NAME varchar(255),
        DISPLAY_ORDER int4,
        ERROR_CODE varchar(255),
        ERROR_MESSAGE varchar(255),
        LABEL varchar(255),
        VALIDATION_STRATEGY_TYPE varchar(255),
        VALIDATION_TYPE varchar(255),
        REQUIRED boolean,
        OPTION_TYPE varchar(255),
        USE_IN_SKU_GENERATION boolean,
        VALIDATION_STRING varchar(255),
        primary key (PRODUCT_OPTION_ID)
    );

    create table BLC_PRODUCT_OPTION_VALUE (
        PRODUCT_OPTION_VALUE_ID int8 not null,
        ATTRIBUTE_VALUE varchar(255),
        DISPLAY_ORDER int8,
        PRICE_ADJUSTMENT numeric(19, 5),
        PRODUCT_OPTION_ID int8,
        primary key (PRODUCT_OPTION_VALUE_ID)
    );

    create table BLC_PRODUCT_OPTION_XREF (
        PRODUCT_OPTION_XREF_ID int8 not null,
        PRODUCT_ID int8 not null,
        PRODUCT_OPTION_ID int8 not null,
        primary key (PRODUCT_OPTION_XREF_ID)
    );

    create table BLC_PRODUCT_UP_SALE (
        UP_SALE_PRODUCT_ID int8 not null,
        PROMOTION_MESSAGE varchar(255),
        SEQUENCE numeric(10, 6),
        CATEGORY_ID int8,
        PRODUCT_ID int8,
        RELATED_SALE_PRODUCT_ID int8,
        primary key (UP_SALE_PRODUCT_ID)
    );

    create table BLC_QUAL_CRIT_OFFER_XREF (
        OFFER_QUAL_CRIT_ID int8 not null,
        OFFER_ID int8 not null,
        OFFER_ITEM_CRITERIA_ID int8,
        primary key (OFFER_QUAL_CRIT_ID)
    );

    create table BLC_QUAL_CRIT_PAGE_XREF (
        PAGE_ID int8,
        PAGE_ITEM_CRITERIA_ID int8 not null,
        primary key (PAGE_ID, PAGE_ITEM_CRITERIA_ID)
    );

    create table BLC_QUAL_CRIT_SC_XREF (
        SC_ID int8 not null,
        SC_ITEM_CRITERIA_ID int8 not null,
        primary key (SC_ID, SC_ITEM_CRITERIA_ID)
    );

    create table BLC_RATING_DETAIL (
        RATING_DETAIL_ID int8 not null,
        RATING float8 not null,
        RATING_SUBMITTED_DATE timestamp not null,
        CUSTOMER_ID int8 not null,
        RATING_SUMMARY_ID int8 not null,
        primary key (RATING_DETAIL_ID)
    );

    create table BLC_RATING_SUMMARY (
        RATING_SUMMARY_ID int8 not null,
        AVERAGE_RATING float8 not null,
        ITEM_ID varchar(255) not null,
        RATING_TYPE varchar(255) not null,
        primary key (RATING_SUMMARY_ID)
    );

    create table BLC_REVIEW_DETAIL (
        REVIEW_DETAIL_ID int8 not null,
        HELPFUL_COUNT int4 not null,
        NOT_HELPFUL_COUNT int4 not null,
        REVIEW_SUBMITTED_DATE timestamp not null,
        REVIEW_STATUS varchar(255) not null,
        REVIEW_TEXT varchar(255) not null,
        CUSTOMER_ID int8 not null,
        RATING_DETAIL_ID int8,
        RATING_SUMMARY_ID int8 not null,
        primary key (REVIEW_DETAIL_ID)
    );

    create table BLC_REVIEW_FEEDBACK (
        REVIEW_FEEDBACK_ID int8 not null,
        IS_HELPFUL boolean not null,
        CUSTOMER_ID int8 not null,
        REVIEW_DETAIL_ID int8 not null,
        primary key (REVIEW_FEEDBACK_ID)
    );

    create table BLC_ROLE (
        ROLE_ID int8 not null,
        ROLE_NAME varchar(255) not null,
        primary key (ROLE_ID)
    );

    create table BLC_SANDBOX (
        SANDBOX_ID int8 not null,
        ARCHIVED char(1),
        AUTHOR int8,
        COLOR varchar(255),
        DESCRIPTION varchar(255),
        GO_LIVE_DATE timestamp,
        SANDBOX_NAME varchar(255),
        SANDBOX_TYPE varchar(255),
        PARENT_SANDBOX_ID int8,
        primary key (SANDBOX_ID)
    );

    create table BLC_SANDBOX_MGMT (
        SANDBOX_MGMT_ID int8 not null,
        SANDBOX_ID int8 not null,
        primary key (SANDBOX_MGMT_ID)
    );

    create table BLC_SC (
        SC_ID int8 not null,
        CREATED_BY int8,
        DATE_CREATED timestamp,
        DATE_UPDATED timestamp,
        UPDATED_BY int8,
        CONTENT_NAME varchar(255) not null,
        OFFLINE_FLAG boolean,
        PRIORITY int4 not null,
        LOCALE_CODE varchar(255) not null,
        SC_TYPE_ID int8,
        primary key (SC_ID)
    );

    create table BLC_SC_FLD (
        SC_FLD_ID int8 not null,
        CREATED_BY int8,
        DATE_CREATED timestamp,
        DATE_UPDATED timestamp,
        UPDATED_BY int8,
        FLD_KEY varchar(255),
        LOB_VALUE text,
        VALUE varchar(255),
        primary key (SC_FLD_ID)
    );

    create table BLC_SC_FLDGRP_XREF (
        SC_FLD_TMPLT_ID int8 not null,
        FLD_GROUP_ID int8 not null,
        GROUP_ORDER int4 not null,
        primary key (SC_FLD_TMPLT_ID, GROUP_ORDER)
    );

    create table BLC_SC_FLD_MAP (
        BLC_SC_SC_FIELD_ID int8 not null,
        MAP_KEY varchar(255) not null,
        SC_ID int8 not null,
        SC_FLD_ID int8,
        primary key (BLC_SC_SC_FIELD_ID)
    );

    create table BLC_SC_FLD_TMPLT (
        SC_FLD_TMPLT_ID int8 not null,
        NAME varchar(255),
        primary key (SC_FLD_TMPLT_ID)
    );

    create table BLC_SC_ITEM_CRITERIA (
        SC_ITEM_CRITERIA_ID int8 not null,
        ORDER_ITEM_MATCH_RULE text,
        QUANTITY int4 not null,
        SC_ID int8,
        primary key (SC_ITEM_CRITERIA_ID)
    );

    create table BLC_SC_RULE (
        SC_RULE_ID int8 not null,
        MATCH_RULE text,
        primary key (SC_RULE_ID)
    );

    create table BLC_SC_RULE_MAP (
        BLC_SC_SC_ID int8 not null,
        SC_RULE_ID int8 not null,
        MAP_KEY varchar(255) not null,
        primary key (BLC_SC_SC_ID, MAP_KEY)
    );

    create table BLC_SC_TYPE (
        SC_TYPE_ID int8 not null,
        DESCRIPTION varchar(255),
        NAME varchar(255),
        SC_FLD_TMPLT_ID int8,
        primary key (SC_TYPE_ID)
    );

    create table BLC_SEARCH_FACET (
        SEARCH_FACET_ID int8 not null,
        MULTISELECT boolean,
        LABEL varchar(255),
        REQUIRES_ALL_DEPENDENT boolean,
        SEARCH_DISPLAY_PRIORITY int4,
        SHOW_ON_SEARCH boolean,
        FIELD_ID int8 not null,
        primary key (SEARCH_FACET_ID)
    );

    create table BLC_SEARCH_FACET_RANGE (
        SEARCH_FACET_RANGE_ID int8 not null,
        MAX_VALUE numeric(19, 5),
        MIN_VALUE numeric(19, 5) not null,
        SEARCH_FACET_ID int8,
        primary key (SEARCH_FACET_RANGE_ID)
    );

    create table BLC_SEARCH_FACET_XREF (
        ID int8 not null,
        REQUIRED_FACET_ID int8 not null,
        SEARCH_FACET_ID int8 not null,
        primary key (ID)
    );

    create table BLC_SEARCH_INTERCEPT (
        SEARCH_REDIRECT_ID int8 not null,
        ACTIVE_END_DATE timestamp,
        ACTIVE_START_DATE timestamp,
        PRIORITY int4,
        SEARCH_TERM varchar(255) not null,
        URL varchar(255) not null,
        primary key (SEARCH_REDIRECT_ID)
    );

    create table BLC_SEARCH_SYNONYM (
        SEARCH_SYNONYM_ID int8 not null,
        SYNONYMS varchar(255),
        TERM varchar(255),
        primary key (SEARCH_SYNONYM_ID)
    );

    create table BLC_SHIPPING_RATE (
        ID int8 not null,
        BAND_RESULT_PCT int4 not null,
        BAND_RESULT_QTY numeric(19, 2) not null,
        BAND_UNIT_QTY numeric(19, 2) not null,
        FEE_BAND int4 not null,
        FEE_SUB_TYPE varchar(255),
        FEE_TYPE varchar(255) not null,
        primary key (ID)
    );

    create table BLC_SITE (
        SITE_ID int8 not null,
        ARCHIVED char(1),
        DEACTIVATED boolean,
        NAME varchar(255),
        SITE_IDENTIFIER_TYPE varchar(255),
        SITE_IDENTIFIER_VALUE varchar(255),
        primary key (SITE_ID)
    );

    create table BLC_SITE_CATALOG (
        SITE_CATALOG_XREF_ID int8 not null,
        CATALOG_ID int8 not null,
        SITE_ID int8 not null,
        primary key (SITE_CATALOG_XREF_ID)
    );

    create table BLC_SITE_MAP_CFG (
        INDEXED_SITE_MAP_FILE_NAME varchar(255),
        INDEXED_SITE_MAP_FILE_PATTERN varchar(255),
        MAX_URL_ENTRIES_PER_FILE int4,
        SITE_MAP_FILE_NAME varchar(255),
        MODULE_CONFIG_ID int8 not null,
        primary key (MODULE_CONFIG_ID)
    );

    create table BLC_SITE_MAP_GEN_CFG (
        GEN_CONFIG_ID int8 not null,
        CHANGE_FREQ varchar(255) not null,
        DISABLED boolean not null,
        GENERATOR_TYPE varchar(255) not null,
        PRIORITY varchar(255),
        MODULE_CONFIG_ID int8 not null,
        primary key (GEN_CONFIG_ID)
    );

    create table BLC_SITE_MAP_URL_ENTRY (
        URL_ENTRY_ID int8 not null,
        CHANGE_FREQ varchar(255) not null,
        LAST_MODIFIED timestamp not null,
        LOCATION varchar(255) not null,
        PRIORITY varchar(255) not null,
        GEN_CONFIG_ID int8 not null,
        primary key (URL_ENTRY_ID)
    );

    create table BLC_SKU (
        SKU_ID int8 not null,
        ACTIVE_END_DATE timestamp,
        ACTIVE_START_DATE timestamp,
        AVAILABLE_FLAG char(1),
        DESCRIPTION varchar(255),
        CONTAINER_SHAPE varchar(255),
        DEPTH numeric(19, 2),
        DIMENSION_UNIT_OF_MEASURE varchar(255),
        GIRTH numeric(19, 2),
        HEIGHT numeric(19, 2),
        CONTAINER_SIZE varchar(255),
        WIDTH numeric(19, 2),
        DISCOUNTABLE_FLAG char(1),
        DISPLAY_TEMPLATE varchar(255),
        EXTERNAL_ID varchar(255),
        FULFILLMENT_TYPE varchar(255),
        INVENTORY_TYPE varchar(255),
        IS_MACHINE_SORTABLE boolean,
        LONG_DESCRIPTION text,
        NAME varchar(255),
        QUANTITY_AVAILABLE int4,
        RETAIL_PRICE numeric(19, 5),
        SALE_PRICE numeric(19, 5),
        TAX_CODE varchar(255),
        TAXABLE_FLAG char(1),
        UPC varchar(255),
        URL_KEY varchar(255),
        WEIGHT numeric(19, 2),
        WEIGHT_UNIT_OF_MEASURE varchar(255),
        CURRENCY_CODE varchar(255),
        DEFAULT_PRODUCT_ID int8,
        ADDL_PRODUCT_ID int8,
        primary key (SKU_ID)
    );

    create table BLC_SKU_ATTRIBUTE (
        SKU_ATTR_ID int8 not null,
        NAME varchar(255) not null,
        SEARCHABLE boolean,
        VALUE varchar(255) not null,
        SKU_ID int8 not null,
        primary key (SKU_ATTR_ID)
    );

    create table BLC_SKU_AVAILABILITY (
        SKU_AVAILABILITY_ID int8 not null,
        AVAILABILITY_DATE timestamp,
        AVAILABILITY_STATUS varchar(255),
        LOCATION_ID int8,
        QTY_ON_HAND int4,
        RESERVE_QTY int4,
        SKU_ID int8,
        primary key (SKU_AVAILABILITY_ID)
    );

    create table BLC_SKU_BUNDLE_ITEM (
        SKU_BUNDLE_ITEM_ID int8 not null,
        ITEM_SALE_PRICE numeric(19, 5),
        QUANTITY int4 not null,
        PRODUCT_BUNDLE_ID int8 not null,
        SKU_ID int8 not null,
        primary key (SKU_BUNDLE_ITEM_ID)
    );

    create table BLC_SKU_FEE (
        SKU_FEE_ID int8 not null,
        AMOUNT numeric(19, 5) not null,
        DESCRIPTION varchar(255),
        EXPRESSION text,
        FEE_TYPE varchar(255),
        NAME varchar(255),
        TAXABLE boolean,
        CURRENCY_CODE varchar(255),
        primary key (SKU_FEE_ID)
    );

    create table BLC_SKU_FEE_XREF (
        SKU_FEE_ID int8 not null,
        SKU_ID int8 not null
    );

    create table BLC_SKU_FULFILLMENT_EXCLUDED (
        SKU_ID int8 not null,
        FULFILLMENT_OPTION_ID int8 not null
    );

    create table BLC_SKU_FULFILLMENT_FLAT_RATES (
        SKU_ID int8 not null,
        RATE numeric(19, 5),
        FULFILLMENT_OPTION_ID int8 not null,
        primary key (SKU_ID, FULFILLMENT_OPTION_ID)
    );

    create table BLC_SKU_MEDIA_MAP (
        SKU_MEDIA_ID int8 not null,
        MAP_KEY varchar(255) not null,
        MEDIA_ID int8,
        BLC_SKU_SKU_ID int8 not null,
        primary key (SKU_MEDIA_ID)
    );

    create table BLC_SKU_OPTION_VALUE_XREF (
        SKU_OPTION_VALUE_XREF_ID int8 not null,
        PRODUCT_OPTION_VALUE_ID int8 not null,
        SKU_ID int8 not null,
        primary key (SKU_OPTION_VALUE_XREF_ID)
    );

    create table BLC_STATE (
        ABBREVIATION varchar(255) not null,
        NAME varchar(255) not null,
        COUNTRY varchar(255) not null,
        primary key (ABBREVIATION)
    );

    create table BLC_STATIC_ASSET (
        STATIC_ASSET_ID int8 not null,
        ALT_TEXT varchar(255),
        CREATED_BY int8,
        DATE_CREATED timestamp,
        DATE_UPDATED timestamp,
        UPDATED_BY int8,
        FILE_EXTENSION varchar(255),
        FILE_SIZE int8,
        FULL_URL varchar(255) not null,
        MIME_TYPE varchar(255),
        NAME varchar(255) not null,
        STORAGE_TYPE varchar(255),
        TITLE varchar(255),
        primary key (STATIC_ASSET_ID)
    );

    create table BLC_STATIC_ASSET_DESC (
        STATIC_ASSET_DESC_ID int8 not null,
        CREATED_BY int8,
        DATE_CREATED timestamp,
        DATE_UPDATED timestamp,
        UPDATED_BY int8,
        DESCRIPTION varchar(255),
        LONG_DESCRIPTION varchar(255),
        primary key (STATIC_ASSET_DESC_ID)
    );

    create table BLC_STORE (
        STORE_ID int8 not null,
        ADDRESS_1 varchar(255),
        ADDRESS_2 varchar(255),
        ARCHIVED char(1),
        STORE_CITY varchar(255),
        STORE_COUNTRY varchar(255),
        LATITUDE float8,
        LONGITUDE float8,
        STORE_NAME varchar(255) not null,
        STORE_PHONE varchar(255),
        STORE_STATE varchar(255),
        STORE_ZIP varchar(255),
        primary key (STORE_ID)
    );

    create table BLC_SYSTEM_PROPERTY (
        BLC_SYSTEM_PROPERTY_ID int8 not null,
        FRIENDLY_GROUP varchar(255),
        FRIENDLY_NAME varchar(255),
        FRIENDLY_TAB varchar(255),
        PROPERTY_NAME varchar(255) not null,
        PROPERTY_TYPE varchar(255),
        PROPERTY_VALUE varchar(255),
        primary key (BLC_SYSTEM_PROPERTY_ID)
    );

    create table BLC_TAR_CRIT_OFFER_XREF (
        OFFER_TAR_CRIT_ID int8 not null,
        OFFER_ID int8 not null,
        OFFER_ITEM_CRITERIA_ID int8,
        primary key (OFFER_TAR_CRIT_ID)
    );

    create table BLC_TAX_DETAIL (
        TAX_DETAIL_ID int8 not null,
        AMOUNT numeric(19, 5),
        TAX_COUNTRY varchar(255),
        JURISDICTION_NAME varchar(255),
        RATE numeric(19, 5),
        TAX_REGION varchar(255),
        TAX_NAME varchar(255),
        TYPE varchar(255),
        CURRENCY_CODE varchar(255),
        MODULE_CONFIG_ID int8,
        primary key (TAX_DETAIL_ID)
    );

    create table BLC_TRANSLATION (
        TRANSLATION_ID int8 not null,
        ENTITY_ID varchar(255),
        ENTITY_TYPE varchar(255),
        FIELD_NAME varchar(255),
        LOCALE_CODE varchar(255),
        TRANSLATED_VALUE text,
        primary key (TRANSLATION_ID)
    );

    create table BLC_TRANS_ADDITNL_FIELDS (
        PAYMENT_TRANSACTION_ID int8 not null,
        FIELD_VALUE text,
        FIELD_NAME varchar(255) not null,
        primary key (PAYMENT_TRANSACTION_ID, FIELD_NAME)
    );

    create table BLC_URL_HANDLER (
        URL_HANDLER_ID int8 not null,
        INCOMING_URL varchar(255) not null,
        NEW_URL varchar(255) not null,
        URL_REDIRECT_TYPE varchar(255),
        primary key (URL_HANDLER_ID)
    );

    create table BLC_UserConnection (
        providerId varchar(255) not null,
        providerUserId varchar(255) not null,
        userId varchar(255) not null,
        accessToken varchar(255) not null,
        displayName varchar(255),
        expireTime int8,
        imageUrl varchar(255),
        profileUrl varchar(255),
        rank int4 not null,
        refreshToken varchar(255),
        secret varchar(255),
        primary key (providerId, providerUserId, userId)
    );

    create table BLC_ZIP_CODE (
        ZIP_CODE_ID varchar(255) not null,
        ZIP_CITY varchar(255),
        ZIP_LATITUDE float8,
        ZIP_LONGITUDE float8,
        ZIP_STATE varchar(255),
        ZIPCODE int4,
        primary key (ZIP_CODE_ID)
    );

    create table DiscreteOrderItemExtImpl (
        ORDER_ITEM_ID int8 not null,
        primary key (ORDER_ITEM_ID)
    );

    create table OrderExtImpl (
        ORDER_ID int8 not null,
        primary key (ORDER_ID)
    );

    alter table BLC_ADDITIONAL_OFFER_INFO 
        add constraint FK3BFDBD63B5D9C34D 
        foreign key (OFFER_INFO_ID) 
        references BLC_OFFER_INFO;

    alter table BLC_ADDITIONAL_OFFER_INFO 
        add constraint FK3BFDBD63D5F3FAF4 
        foreign key (OFFER_ID) 
        references BLC_OFFER;

    alter table BLC_ADDITIONAL_OFFER_INFO 
        add constraint FK3BFDBD631891FF79 
        foreign key (BLC_ORDER_ORDER_ID) 
        references BLC_ORDER;

    create index ADDRESS_COUNTRY_INDEX on BLC_ADDRESS (COUNTRY);

    create index ADDRESS_ISO_COUNTRY_IDX on BLC_ADDRESS (ISO_COUNTRY_ALPHA2);

    create index ADDRESS_PHONE_FAX_IDX on BLC_ADDRESS (PHONE_FAX_ID);

    create index ADDRESS_PHONE_PRI_IDX on BLC_ADDRESS (PHONE_PRIMARY_ID);

    create index ADDRESS_PHONE_SEC_IDX on BLC_ADDRESS (PHONE_SECONDARY_ID);

    create index ADDRESS_STATE_INDEX on BLC_ADDRESS (STATE_PROV_REGION);

    alter table BLC_ADDRESS 
        add constraint FK299F86CEA46E16CF 
        foreign key (COUNTRY) 
        references BLC_COUNTRY;

    alter table BLC_ADDRESS 
        add constraint FK299F86CE3A39A488 
        foreign key (ISO_COUNTRY_ALPHA2) 
        references BLC_ISO_COUNTRY;

    alter table BLC_ADDRESS 
        add constraint FK299F86CEF1A6533F 
        foreign key (PHONE_FAX_ID) 
        references BLC_PHONE;

    alter table BLC_ADDRESS 
        add constraint FK299F86CEBF4449BA 
        foreign key (PHONE_PRIMARY_ID) 
        references BLC_PHONE;

    alter table BLC_ADDRESS 
        add constraint FK299F86CEE12DC0C8 
        foreign key (PHONE_SECONDARY_ID) 
        references BLC_PHONE;

    alter table BLC_ADDRESS 
        add constraint FK299F86CE337C4D50 
        foreign key (STATE_PROV_REGION) 
        references BLC_STATE;

    create index ADMINMODULE_NAME_INDEX on BLC_ADMIN_MODULE (NAME);

    alter table BLC_ADMIN_PERMISSION_ENTITY 
        add constraint FK23C09E3DE88B7D38 
        foreign key (ADMIN_PERMISSION_ID) 
        references BLC_ADMIN_PERMISSION;

    alter table BLC_ADMIN_PERMISSION_XREF 
        add constraint FKBCAD1F5E88B7D38 
        foreign key (ADMIN_PERMISSION_ID) 
        references BLC_ADMIN_PERMISSION;

    alter table BLC_ADMIN_PERMISSION_XREF 
        add constraint FKBCAD1F575A3C445 
        foreign key (CHILD_PERMISSION_ID) 
        references BLC_ADMIN_PERMISSION;

    alter table BLC_ADMIN_ROLE_PERMISSION_XREF 
        add constraint FK4A819D985F43AAD8 
        foreign key (ADMIN_ROLE_ID) 
        references BLC_ADMIN_ROLE;

    alter table BLC_ADMIN_ROLE_PERMISSION_XREF 
        add constraint FK4A819D98E88B7D38 
        foreign key (ADMIN_PERMISSION_ID) 
        references BLC_ADMIN_PERMISSION;

    alter table BLC_ADMIN_SECTION 
        add constraint uc_BLC_ADMIN_SECTION_1 unique (SECTION_KEY);

    create index ADMINSECTION_MODULE_INDEX on BLC_ADMIN_SECTION (ADMIN_MODULE_ID);

    create index ADMINSECTION_NAME_INDEX on BLC_ADMIN_SECTION (NAME);

    alter table BLC_ADMIN_SECTION 
        add constraint FK7EA7D92FB1A18498 
        foreign key (ADMIN_MODULE_ID) 
        references BLC_ADMIN_MODULE;

    alter table BLC_ADMIN_SEC_PERM_XREF 
        add constraint FK5E832966E88B7D38 
        foreign key (ADMIN_PERMISSION_ID) 
        references BLC_ADMIN_PERMISSION;

    alter table BLC_ADMIN_SEC_PERM_XREF 
        add constraint FK5E8329663AF7F0FC 
        foreign key (ADMIN_SECTION_ID) 
        references BLC_ADMIN_SECTION;

    create index ADMINPERM_EMAIL_INDEX on BLC_ADMIN_USER (EMAIL);

    create index ADMINUSER_NAME_INDEX on BLC_ADMIN_USER (NAME);

    create index ADMINUSERATTRIBUTE_INDEX on BLC_ADMIN_USER_ADDTL_FIELDS (ADMIN_USER_ID);

    create index ADMINUSERATTRIBUTE_NAME_INDEX on BLC_ADMIN_USER_ADDTL_FIELDS (FIELD_NAME);

    alter table BLC_ADMIN_USER_ADDTL_FIELDS 
        add constraint FK73274CDD46EBC38 
        foreign key (ADMIN_USER_ID) 
        references BLC_ADMIN_USER;

    alter table BLC_ADMIN_USER_PERMISSION_XREF 
        add constraint FKF0B3BEED46EBC38 
        foreign key (ADMIN_USER_ID) 
        references BLC_ADMIN_USER;

    alter table BLC_ADMIN_USER_PERMISSION_XREF 
        add constraint FKF0B3BEEDE88B7D38 
        foreign key (ADMIN_PERMISSION_ID) 
        references BLC_ADMIN_PERMISSION;

    alter table BLC_ADMIN_USER_ROLE_XREF 
        add constraint FKFFD33A265F43AAD8 
        foreign key (ADMIN_ROLE_ID) 
        references BLC_ADMIN_ROLE;

    alter table BLC_ADMIN_USER_ROLE_XREF 
        add constraint FKFFD33A2646EBC38 
        foreign key (ADMIN_USER_ID) 
        references BLC_ADMIN_USER;

    alter table BLC_ADMIN_USER_SANDBOX 
        add constraint FKD0A97E09579FE59D 
        foreign key (SANDBOX_ID) 
        references BLC_SANDBOX;

    alter table BLC_ADMIN_USER_SANDBOX 
        add constraint FKD0A97E0946EBC38 
        foreign key (ADMIN_USER_ID) 
        references BLC_ADMIN_USER;

    alter table BLC_ASSET_DESC_MAP 
        add constraint FKE886BAE3E2BA0C9D 
        foreign key (STATIC_ASSET_DESC_ID) 
        references BLC_STATIC_ASSET_DESC;

    alter table BLC_ASSET_DESC_MAP 
        add constraint FKE886BAE367F70B63 
        foreign key (STATIC_ASSET_ID) 
        references BLC_STATIC_ASSET;

    alter table BLC_BUNDLE_ORDER_ITEM 
        add constraint FK489703DBCCF29B96 
        foreign key (PRODUCT_BUNDLE_ID) 
        references BLC_PRODUCT_BUNDLE;

    alter table BLC_BUNDLE_ORDER_ITEM 
        add constraint FK489703DBB78C9977 
        foreign key (SKU_ID) 
        references BLC_SKU;

    alter table BLC_BUNDLE_ORDER_ITEM 
        add constraint FK489703DB9AF166DF 
        foreign key (ORDER_ITEM_ID) 
        references BLC_ORDER_ITEM;

    alter table BLC_BUND_ITEM_FEE_PRICE 
        add constraint FK14267A943FC68307 
        foreign key (BUND_ORDER_ITEM_ID) 
        references BLC_BUNDLE_ORDER_ITEM;

    create index CANDIDATE_FG_INDEX on BLC_CANDIDATE_FG_OFFER (FULFILLMENT_GROUP_ID);

    create index CANDIDATE_FGOFFER_INDEX on BLC_CANDIDATE_FG_OFFER (OFFER_ID);

    alter table BLC_CANDIDATE_FG_OFFER 
        add constraint FKCE785605028DC55 
        foreign key (FULFILLMENT_GROUP_ID) 
        references BLC_FULFILLMENT_GROUP;

    alter table BLC_CANDIDATE_FG_OFFER 
        add constraint FKCE78560D5F3FAF4 
        foreign key (OFFER_ID) 
        references BLC_OFFER;

    create index CANDIDATE_ITEMOFFER_INDEX on BLC_CANDIDATE_ITEM_OFFER (OFFER_ID);

    create index CANDIDATE_ITEM_INDEX on BLC_CANDIDATE_ITEM_OFFER (ORDER_ITEM_ID);

    alter table BLC_CANDIDATE_ITEM_OFFER 
        add constraint FK9EEE9B2D5F3FAF4 
        foreign key (OFFER_ID) 
        references BLC_OFFER;

    alter table BLC_CANDIDATE_ITEM_OFFER 
        add constraint FK9EEE9B29AF166DF 
        foreign key (ORDER_ITEM_ID) 
        references BLC_ORDER_ITEM;

    create index CANDIDATE_ORDEROFFER_INDEX on BLC_CANDIDATE_ORDER_OFFER (OFFER_ID);

    create index CANDIDATE_ORDER_INDEX on BLC_CANDIDATE_ORDER_OFFER (ORDER_ID);

    alter table BLC_CANDIDATE_ORDER_OFFER 
        add constraint FK61852289D5F3FAF4 
        foreign key (OFFER_ID) 
        references BLC_OFFER;

    alter table BLC_CANDIDATE_ORDER_OFFER 
        add constraint FK6185228989FE8A02 
        foreign key (ORDER_ID) 
        references BLC_ORDER;

    create index CATEGORY_PARENT_INDEX on BLC_CATEGORY (DEFAULT_PARENT_CATEGORY_ID);

    create index CATEGORY_E_ID_INDEX on BLC_CATEGORY (EXTERNAL_ID);

    create index CATEGORY_NAME_INDEX on BLC_CATEGORY (NAME);

    create index CATEGORY_URL_INDEX on BLC_CATEGORY (URL);

    create index CATEGORY_URLKEY_INDEX on BLC_CATEGORY (URL_KEY);

    alter table BLC_CATEGORY 
        add constraint FK55F82D44B177E6 
        foreign key (DEFAULT_PARENT_CATEGORY_ID) 
        references BLC_CATEGORY;

    create index CATEGORYATTRIBUTE_INDEX on BLC_CATEGORY_ATTRIBUTE (CATEGORY_ID);

    create index CATEGORYATTRIBUTE_NAME_INDEX on BLC_CATEGORY_ATTRIBUTE (NAME);

    alter table BLC_CATEGORY_ATTRIBUTE 
        add constraint FK4E441D4115D1A13D 
        foreign key (CATEGORY_ID) 
        references BLC_CATEGORY;

    alter table BLC_CATEGORY_MEDIA_MAP 
        add constraint FKCD24B106D786CEA2 
        foreign key (BLC_CATEGORY_CATEGORY_ID) 
        references BLC_CATEGORY;

    alter table BLC_CATEGORY_MEDIA_MAP 
        add constraint FKCD24B1066E4720E0 
        foreign key (MEDIA_ID) 
        references BLC_MEDIA;

    alter table BLC_CATEGORY_PRODUCT_XREF 
        add constraint FK635EB1A615D1A13D 
        foreign key (CATEGORY_ID) 
        references BLC_CATEGORY;

    alter table BLC_CATEGORY_PRODUCT_XREF 
        add constraint FK635EB1A65F11A0B7 
        foreign key (PRODUCT_ID) 
        references BLC_PRODUCT;

    alter table BLC_CATEGORY_XREF 
        add constraint FKE889733615D1A13D 
        foreign key (CATEGORY_ID) 
        references BLC_CATEGORY;

    alter table BLC_CATEGORY_XREF 
        add constraint FKE8897336D6D45DBE 
        foreign key (SUB_CATEGORY_ID) 
        references BLC_CATEGORY;

    alter table BLC_CAT_SEARCH_FACET_EXCL_XREF 
        add constraint FK8361EF4E15D1A13D 
        foreign key (CATEGORY_ID) 
        references BLC_CATEGORY;

    alter table BLC_CAT_SEARCH_FACET_EXCL_XREF 
        add constraint FK8361EF4EB96B1C93 
        foreign key (SEARCH_FACET_ID) 
        references BLC_SEARCH_FACET;

    alter table BLC_CAT_SEARCH_FACET_XREF 
        add constraint FK32210EEB15D1A13D 
        foreign key (CATEGORY_ID) 
        references BLC_CATEGORY;

    alter table BLC_CAT_SEARCH_FACET_XREF 
        add constraint FK32210EEBB96B1C93 
        foreign key (SEARCH_FACET_ID) 
        references BLC_SEARCH_FACET;

    alter table BLC_CAT_SITE_MAP_GEN_CFG 
        add constraint FK1BA4E695C5F3D60 
        foreign key (ROOT_CATEGORY_ID) 
        references BLC_CATEGORY;

    alter table BLC_CAT_SITE_MAP_GEN_CFG 
        add constraint FK1BA4E69BCAB9F56 
        foreign key (GEN_CONFIG_ID) 
        references BLC_SITE_MAP_GEN_CFG;

    create index COUNTRY_SUB_ALT_ABRV_IDX on BLC_COUNTRY_SUB (ALT_ABBREVIATION);

    create index COUNTRY_SUB_NAME_IDX on BLC_COUNTRY_SUB (NAME);

    alter table BLC_COUNTRY_SUB 
        add constraint FKA804FBD172AAB3C0 
        foreign key (COUNTRY_SUB_CAT) 
        references BLC_COUNTRY_SUB_CAT;

    alter table BLC_COUNTRY_SUB 
        add constraint FKA804FBD1A46E16CF 
        foreign key (COUNTRY) 
        references BLC_COUNTRY;

    create index CUSTOMER_CHALLENGE_INDEX on BLC_CUSTOMER (CHALLENGE_QUESTION_ID);

    create index CUSTOMER_EMAIL_INDEX on BLC_CUSTOMER (EMAIL_ADDRESS);

    alter table BLC_CUSTOMER 
        add constraint FK7716F0241422B204 
        foreign key (CHALLENGE_QUESTION_ID) 
        references BLC_CHALLENGE_QUESTION;

    alter table BLC_CUSTOMER 
        add constraint FK7716F024A1E1C128 
        foreign key (LOCALE_CODE) 
        references BLC_LOCALE;

    create index CUSTOMERADDRESS_ADDRESS_INDEX on BLC_CUSTOMER_ADDRESS (ADDRESS_ID);

    alter table BLC_CUSTOMER_ADDRESS 
        add constraint FK75B95AB9C13085DD 
        foreign key (ADDRESS_ID) 
        references BLC_ADDRESS;

    alter table BLC_CUSTOMER_ADDRESS 
        add constraint FK75B95AB97470F437 
        foreign key (CUSTOMER_ID) 
        references BLC_CUSTOMER;

    alter table BLC_CUSTOMER_ATTRIBUTE 
        add constraint FKB974C8217470F437 
        foreign key (CUSTOMER_ID) 
        references BLC_CUSTOMER;

    create index CUSTOFFER_CUSTOMER_INDEX on BLC_CUSTOMER_OFFER_XREF (CUSTOMER_ID);

    create index CUSTOFFER_OFFER_INDEX on BLC_CUSTOMER_OFFER_XREF (OFFER_ID);

    alter table BLC_CUSTOMER_OFFER_XREF 
        add constraint FK685E80397470F437 
        foreign key (CUSTOMER_ID) 
        references BLC_CUSTOMER;

    alter table BLC_CUSTOMER_OFFER_XREF 
        add constraint FK685E8039D5F3FAF4 
        foreign key (OFFER_ID) 
        references BLC_OFFER;

    alter table BLC_CUSTOMER_PAYMENT 
        add constraint CSTMR_PAY_UNIQUE_CNSTRNT unique (CUSTOMER_ID, PAYMENT_TOKEN);

    alter table BLC_CUSTOMER_PAYMENT 
        add constraint FK8B3DF0CBC13085DD 
        foreign key (ADDRESS_ID) 
        references BLC_ADDRESS;

    alter table BLC_CUSTOMER_PAYMENT 
        add constraint FK8B3DF0CB7470F437 
        foreign key (CUSTOMER_ID) 
        references BLC_CUSTOMER;

    alter table BLC_CUSTOMER_PAYMENT_FIELDS 
        add constraint FK5CCB14ADCA0B98E0 
        foreign key (CUSTOMER_PAYMENT_ID) 
        references BLC_CUSTOMER_PAYMENT;

    alter table BLC_CUSTOMER_PHONE 
        add constraint CSTMR_PHONE_UNIQUE_CNSTRNT unique (CUSTOMER_ID, PHONE_NAME);

    create index CUSTPHONE_PHONE_INDEX on BLC_CUSTOMER_PHONE (PHONE_ID);

    alter table BLC_CUSTOMER_PHONE 
        add constraint FK3D28ED737470F437 
        foreign key (CUSTOMER_ID) 
        references BLC_CUSTOMER;

    alter table BLC_CUSTOMER_PHONE 
        add constraint FK3D28ED73D894CB5D 
        foreign key (PHONE_ID) 
        references BLC_PHONE;

    create index CUSTROLE_CUSTOMER_INDEX on BLC_CUSTOMER_ROLE (CUSTOMER_ID);

    create index CUSTROLE_ROLE_INDEX on BLC_CUSTOMER_ROLE (ROLE_ID);

    alter table BLC_CUSTOMER_ROLE 
        add constraint FK548EB7B17470F437 
        foreign key (CUSTOMER_ID) 
        references BLC_CUSTOMER;

    alter table BLC_CUSTOMER_ROLE 
        add constraint FK548EB7B1B8587B7 
        foreign key (ROLE_ID) 
        references BLC_ROLE;

    alter table BLC_CUST_SITE_MAP_GEN_CFG 
        add constraint FK67C0DBA0BCAB9F56 
        foreign key (GEN_CONFIG_ID) 
        references BLC_SITE_MAP_GEN_CFG;

    create index ENUM_KEY_INDEX on BLC_DATA_DRVN_ENUM (ENUM_KEY);

    create index HIDDEN_INDEX on BLC_DATA_DRVN_ENUM_VAL (HIDDEN);

    create index ENUM_VAL_KEY_INDEX on BLC_DATA_DRVN_ENUM_VAL (ENUM_KEY);

    alter table BLC_DATA_DRVN_ENUM_VAL 
        add constraint FKB2D5700DA60E0554 
        foreign key (ENUM_TYPE) 
        references BLC_DATA_DRVN_ENUM;

    create index DISCRETE_PRODUCT_INDEX on BLC_DISCRETE_ORDER_ITEM (PRODUCT_ID);

    create index DISCRETE_SKU_INDEX on BLC_DISCRETE_ORDER_ITEM (SKU_ID);

    alter table BLC_DISCRETE_ORDER_ITEM 
        add constraint FKBC3A8A845CDFCA80 
        foreign key (BUNDLE_ORDER_ITEM_ID) 
        references BLC_BUNDLE_ORDER_ITEM;

    alter table BLC_DISCRETE_ORDER_ITEM 
        add constraint FKBC3A8A845F11A0B7 
        foreign key (PRODUCT_ID) 
        references BLC_PRODUCT;

    alter table BLC_DISCRETE_ORDER_ITEM 
        add constraint FKBC3A8A84B78C9977 
        foreign key (SKU_ID) 
        references BLC_SKU;

    alter table BLC_DISCRETE_ORDER_ITEM 
        add constraint FKBC3A8A841285903B 
        foreign key (SKU_BUNDLE_ITEM_ID) 
        references BLC_SKU_BUNDLE_ITEM;

    alter table BLC_DISCRETE_ORDER_ITEM 
        add constraint FKBC3A8A849AF166DF 
        foreign key (ORDER_ITEM_ID) 
        references BLC_ORDER_ITEM;

    alter table BLC_DISC_ITEM_FEE_PRICE 
        add constraint FK2A641CC8B76B9466 
        foreign key (ORDER_ITEM_ID) 
        references BLC_DISCRETE_ORDER_ITEM;

    alter table BLC_DYN_DISCRETE_ORDER_ITEM 
        add constraint FK209DEE9EB76B9466 
        foreign key (ORDER_ITEM_ID) 
        references BLC_DISCRETE_ORDER_ITEM;

    create index EMAILTRACKING_INDEX on BLC_EMAIL_TRACKING (EMAIL_ADDRESS);

    create index TRACKINGCLICKS_CUSTOMER_INDEX on BLC_EMAIL_TRACKING_CLICKS (CUSTOMER_ID);

    create index TRACKINGCLICKS_TRACKING_INDEX on BLC_EMAIL_TRACKING_CLICKS (EMAIL_TRACKING_ID);

    alter table BLC_EMAIL_TRACKING_CLICKS 
        add constraint FKFDF9F52AFA1E5D61 
        foreign key (EMAIL_TRACKING_ID) 
        references BLC_EMAIL_TRACKING;

    create index TRACKINGOPEN_TRACKING on BLC_EMAIL_TRACKING_OPENS (EMAIL_TRACKING_ID);

    alter table BLC_EMAIL_TRACKING_OPENS 
        add constraint FKA5C3722AFA1E5D61 
        foreign key (EMAIL_TRACKING_ID) 
        references BLC_EMAIL_TRACKING;

    alter table BLC_EXT_SKU 
        add constraint FK2040F4B9B78C9977 
        foreign key (SKU_ID) 
        references BLC_SKU;

    create index FGADJUSTMENT_INDEX on BLC_FG_ADJUSTMENT (FULFILLMENT_GROUP_ID);

    create index FGADJUSTMENT_OFFER_INDEX on BLC_FG_ADJUSTMENT (OFFER_ID);

    alter table BLC_FG_ADJUSTMENT 
        add constraint FK468C8F255028DC55 
        foreign key (FULFILLMENT_GROUP_ID) 
        references BLC_FULFILLMENT_GROUP;

    alter table BLC_FG_ADJUSTMENT 
        add constraint FK468C8F25D5F3FAF4 
        foreign key (OFFER_ID) 
        references BLC_OFFER;

    alter table BLC_FG_FEE_TAX_XREF 
        add constraint UK_25426DC0FA888C35 unique (TAX_DETAIL_ID);

    alter table BLC_FG_FEE_TAX_XREF 
        add constraint FK25426DC071448C19 
        foreign key (TAX_DETAIL_ID) 
        references BLC_TAX_DETAIL;

    alter table BLC_FG_FEE_TAX_XREF 
        add constraint FK25426DC0598F6D02 
        foreign key (FULFILLMENT_GROUP_FEE_ID) 
        references BLC_FULFILLMENT_GROUP_FEE;

    alter table BLC_FG_FG_TAX_XREF 
        add constraint UK_61BEA455FA888C35 unique (TAX_DETAIL_ID);

    alter table BLC_FG_FG_TAX_XREF 
        add constraint FK61BEA45571448C19 
        foreign key (TAX_DETAIL_ID) 
        references BLC_TAX_DETAIL;

    alter table BLC_FG_FG_TAX_XREF 
        add constraint FK61BEA4555028DC55 
        foreign key (FULFILLMENT_GROUP_ID) 
        references BLC_FULFILLMENT_GROUP;

    alter table BLC_FG_ITEM_TAX_XREF 
        add constraint UK_DD3E8443FA888C35 unique (TAX_DETAIL_ID);

    alter table BLC_FG_ITEM_TAX_XREF 
        add constraint FKDD3E844371448C19 
        foreign key (TAX_DETAIL_ID) 
        references BLC_TAX_DETAIL;

    alter table BLC_FG_ITEM_TAX_XREF 
        add constraint FKDD3E8443E3BBB4D2 
        foreign key (FULFILLMENT_GROUP_ITEM_ID) 
        references BLC_FULFILLMENT_GROUP_ITEM;

    create index ENTITY_TYPE_INDEX on BLC_FIELD (ENTITY_TYPE);

    alter table BLC_FIELD_SEARCH_TYPES 
        add constraint FKF52D130D3C3907C4 
        foreign key (FIELD_ID) 
        references BLC_FIELD;

    alter table BLC_FLD_DEF 
        add constraint FK3FCB575E38D08AB5 
        foreign key (ENUM_ID) 
        references BLC_DATA_DRVN_ENUM;

    alter table BLC_FLD_DEF 
        add constraint FK3FCB575E6A79BDB5 
        foreign key (FLD_GROUP_ID) 
        references BLC_FLD_GROUP;

    alter table BLC_FLD_ENUM_ITEM 
        add constraint FK83A6A84AFD2EA299 
        foreign key (FLD_ENUM_ID) 
        references BLC_FLD_ENUM;

    create index FG_ADDRESS_INDEX on BLC_FULFILLMENT_GROUP (ADDRESS_ID);

    create index FG_METHOD_INDEX on BLC_FULFILLMENT_GROUP (METHOD);

    create index FG_ORDER_INDEX on BLC_FULFILLMENT_GROUP (ORDER_ID);

    create index FG_MESSAGE_INDEX on BLC_FULFILLMENT_GROUP (PERSONAL_MESSAGE_ID);

    create index FG_PHONE_INDEX on BLC_FULFILLMENT_GROUP (PHONE_ID);

    create index FG_PRIMARY_INDEX on BLC_FULFILLMENT_GROUP (IS_PRIMARY);

    create index FG_REFERENCE_INDEX on BLC_FULFILLMENT_GROUP (REFERENCE_NUMBER);

    create index FG_SERVICE_INDEX on BLC_FULFILLMENT_GROUP (SERVICE);

    create index FG_STATUS_INDEX on BLC_FULFILLMENT_GROUP (STATUS);

    alter table BLC_FULFILLMENT_GROUP 
        add constraint FKC5B9EF18C13085DD 
        foreign key (ADDRESS_ID) 
        references BLC_ADDRESS;

    alter table BLC_FULFILLMENT_GROUP 
        add constraint FKC5B9EF1881F34C7F 
        foreign key (FULFILLMENT_OPTION_ID) 
        references BLC_FULFILLMENT_OPTION;

    alter table BLC_FULFILLMENT_GROUP 
        add constraint FKC5B9EF1889FE8A02 
        foreign key (ORDER_ID) 
        references BLC_ORDER;

    alter table BLC_FULFILLMENT_GROUP 
        add constraint FKC5B9EF1877F565E1 
        foreign key (PERSONAL_MESSAGE_ID) 
        references BLC_PERSONAL_MESSAGE;

    alter table BLC_FULFILLMENT_GROUP 
        add constraint FKC5B9EF18D894CB5D 
        foreign key (PHONE_ID) 
        references BLC_PHONE;

    alter table BLC_FULFILLMENT_GROUP_FEE 
        add constraint FK6AA8E1BF5028DC55 
        foreign key (FULFILLMENT_GROUP_ID) 
        references BLC_FULFILLMENT_GROUP;

    create index FGITEM_FG_INDEX on BLC_FULFILLMENT_GROUP_ITEM (FULFILLMENT_GROUP_ID);

    create index FGITEM_STATUS_INDEX on BLC_FULFILLMENT_GROUP_ITEM (STATUS);

    alter table BLC_FULFILLMENT_GROUP_ITEM 
        add constraint FKEA74EBDA5028DC55 
        foreign key (FULFILLMENT_GROUP_ID) 
        references BLC_FULFILLMENT_GROUP;

    alter table BLC_FULFILLMENT_GROUP_ITEM 
        add constraint FKEA74EBDA9AF166DF 
        foreign key (ORDER_ITEM_ID) 
        references BLC_ORDER_ITEM;

    alter table BLC_FULFILLMENT_OPTION_FIXED 
        add constraint FK408360313E2FC4F9 
        foreign key (CURRENCY_CODE) 
        references BLC_CURRENCY;

    alter table BLC_FULFILLMENT_OPTION_FIXED 
        add constraint FK4083603181F34C7F 
        foreign key (FULFILLMENT_OPTION_ID) 
        references BLC_FULFILLMENT_OPTION;

    alter table BLC_FULFILLMENT_OPT_BANDED_PRC 
        add constraint FKB1FD71E981F34C7F 
        foreign key (FULFILLMENT_OPTION_ID) 
        references BLC_FULFILLMENT_OPTION;

    alter table BLC_FULFILLMENT_OPT_BANDED_WGT 
        add constraint FKB1FD8AEC81F34C7F 
        foreign key (FULFILLMENT_OPTION_ID) 
        references BLC_FULFILLMENT_OPTION;

    alter table BLC_FULFILLMENT_PRICE_BAND 
        add constraint FK46C9EA726CDF59CA 
        foreign key (FULFILLMENT_OPTION_ID) 
        references BLC_FULFILLMENT_OPT_BANDED_PRC;

    alter table BLC_FULFILLMENT_WEIGHT_BAND 
        add constraint FK6A048D95A0B429C3 
        foreign key (FULFILLMENT_OPTION_ID) 
        references BLC_FULFILLMENT_OPT_BANDED_WGT;

    alter table BLC_GIFTWRAP_ORDER_ITEM 
        add constraint FKE1BE1563B76B9466 
        foreign key (ORDER_ITEM_ID) 
        references BLC_DISCRETE_ORDER_ITEM;

    alter table BLC_IMG_STATIC_ASSET 
        add constraint FKCC4B772167F70B63 
        foreign key (STATIC_ASSET_ID) 
        references BLC_STATIC_ASSET;

    alter table BLC_ITEM_OFFER_QUALIFIER 
        add constraint FKD9C50C61D5F3FAF4 
        foreign key (OFFER_ID) 
        references BLC_OFFER;

    alter table BLC_ITEM_OFFER_QUALIFIER 
        add constraint FKD9C50C619AF166DF 
        foreign key (ORDER_ITEM_ID) 
        references BLC_ORDER_ITEM;

    alter table BLC_LOCALE 
        add constraint FK56C7DC203E2FC4F9 
        foreign key (CURRENCY_CODE) 
        references BLC_CURRENCY;

    create index MEDIA_TITLE_INDEX on BLC_MEDIA (TITLE);

    create index MEDIA_URL_INDEX on BLC_MEDIA (URL);

    create index OFFER_DISCOUNT_INDEX on BLC_OFFER (OFFER_DISCOUNT_TYPE);

    create index OFFER_MARKETING_MESSAGE_INDEX on BLC_OFFER (MARKETING_MESSASGE);

    create index OFFER_NAME_INDEX on BLC_OFFER (OFFER_NAME);

    create index OFFER_TYPE_INDEX on BLC_OFFER (OFFER_TYPE);

    create index OFFERAUDIT_CUSTOMER_INDEX on BLC_OFFER_AUDIT (CUSTOMER_ID);

    create index OFFERAUDIT_OFFER_CODE_INDEX on BLC_OFFER_AUDIT (OFFER_CODE_ID);

    create index OFFERAUDIT_OFFER_INDEX on BLC_OFFER_AUDIT (OFFER_ID);

    create index OFFERAUDIT_ORDER_INDEX on BLC_OFFER_AUDIT (ORDER_ID);

    create index OFFERCODE_OFFER_INDEX on BLC_OFFER_CODE (OFFER_ID);

    create index OFFERCODE_CODE_INDEX on BLC_OFFER_CODE (OFFER_CODE);

    alter table BLC_OFFER_CODE 
        add constraint FK76B8C8D6D5F3FAF4 
        foreign key (OFFER_ID) 
        references BLC_OFFER;

    alter table BLC_OFFER_INFO_FIELDS 
        add constraint FKA901886183AE7237 
        foreign key (OFFER_INFO_FIELDS_ID) 
        references BLC_OFFER_INFO;

    alter table BLC_OFFER_RULE_MAP 
        add constraint FKCA468FE245C66D1D 
        foreign key (BLC_OFFER_OFFER_ID) 
        references BLC_OFFER;

    alter table BLC_OFFER_RULE_MAP 
        add constraint FKCA468FE2C11A218D 
        foreign key (OFFER_RULE_ID) 
        references BLC_OFFER_RULE;

    create index ORDER_CUSTOMER_INDEX on BLC_ORDER (CUSTOMER_ID);

    create index ORDER_EMAIL_INDEX on BLC_ORDER (EMAIL_ADDRESS);

    create index ORDER_NAME_INDEX on BLC_ORDER (NAME);

    create index ORDER_NUMBER_INDEX on BLC_ORDER (ORDER_NUMBER);

    create index ORDER_STATUS_INDEX on BLC_ORDER (ORDER_STATUS);

    alter table BLC_ORDER 
        add constraint FK8F5B64A83E2FC4F9 
        foreign key (CURRENCY_CODE) 
        references BLC_CURRENCY;

    alter table BLC_ORDER 
        add constraint FK8F5B64A87470F437 
        foreign key (CUSTOMER_ID) 
        references BLC_CUSTOMER;

    alter table BLC_ORDER 
        add constraint FK8F5B64A8A1E1C128 
        foreign key (LOCALE_CODE) 
        references BLC_LOCALE;

    create index ORDERADJUST_OFFER_INDEX on BLC_ORDER_ADJUSTMENT (OFFER_ID);

    create index ORDERADJUST_ORDER_INDEX on BLC_ORDER_ADJUSTMENT (ORDER_ID);

    alter table BLC_ORDER_ADJUSTMENT 
        add constraint FK1E92D164D5F3FAF4 
        foreign key (OFFER_ID) 
        references BLC_OFFER;

    alter table BLC_ORDER_ADJUSTMENT 
        add constraint FK1E92D16489FE8A02 
        foreign key (ORDER_ID) 
        references BLC_ORDER;

    alter table BLC_ORDER_ATTRIBUTE 
        add constraint FKB3A467A589FE8A02 
        foreign key (ORDER_ID) 
        references BLC_ORDER;

    create index ORDERITEM_CATEGORY_INDEX on BLC_ORDER_ITEM (CATEGORY_ID);

    create index ORDERITEM_GIFT_INDEX on BLC_ORDER_ITEM (GIFT_WRAP_ITEM_ID);

    create index ORDERITEM_ORDER_INDEX on BLC_ORDER_ITEM (ORDER_ID);

    create index ORDERITEM_TYPE_INDEX on BLC_ORDER_ITEM (ORDER_ITEM_TYPE);

    create index ORDERITEM_PARENT_INDEX on BLC_ORDER_ITEM (PARENT_ORDER_ITEM_ID);

    create index ORDERITEM_MESSAGE_INDEX on BLC_ORDER_ITEM (PERSONAL_MESSAGE_ID);

    alter table BLC_ORDER_ITEM 
        add constraint FK9A2E704A15D1A13D 
        foreign key (CATEGORY_ID) 
        references BLC_CATEGORY;

    alter table BLC_ORDER_ITEM 
        add constraint FK9A2E704AFD2F1F10 
        foreign key (GIFT_WRAP_ITEM_ID) 
        references BLC_GIFTWRAP_ORDER_ITEM;

    alter table BLC_ORDER_ITEM 
        add constraint FK9A2E704A89FE8A02 
        foreign key (ORDER_ID) 
        references BLC_ORDER;

    alter table BLC_ORDER_ITEM 
        add constraint FK9A2E704AB0B0D00A 
        foreign key (PARENT_ORDER_ITEM_ID) 
        references BLC_ORDER_ITEM;

    alter table BLC_ORDER_ITEM 
        add constraint FK9A2E704A77F565E1 
        foreign key (PERSONAL_MESSAGE_ID) 
        references BLC_PERSONAL_MESSAGE;

    alter table BLC_ORDER_ITEM_ADD_ATTR 
        add constraint FKA466AB44B76B9466 
        foreign key (ORDER_ITEM_ID) 
        references BLC_DISCRETE_ORDER_ITEM;

    create index OIADJUST_OFFER_INDEX on BLC_ORDER_ITEM_ADJUSTMENT (OFFER_ID);

    create index OIADJUST_ITEM_INDEX on BLC_ORDER_ITEM_ADJUSTMENT (ORDER_ITEM_ID);

    alter table BLC_ORDER_ITEM_ADJUSTMENT 
        add constraint FKA2658C82D5F3FAF4 
        foreign key (OFFER_ID) 
        references BLC_OFFER;

    alter table BLC_ORDER_ITEM_ADJUSTMENT 
        add constraint FKA2658C829AF166DF 
        foreign key (ORDER_ITEM_ID) 
        references BLC_ORDER_ITEM;

    alter table BLC_ORDER_ITEM_ATTRIBUTE 
        add constraint FK9F1ED0C79AF166DF 
        foreign key (ORDER_ITEM_ID) 
        references BLC_ORDER_ITEM;

    alter table BLC_ORDER_ITEM_DTL_ADJ 
        add constraint FK85F0248FD5F3FAF4 
        foreign key (OFFER_ID) 
        references BLC_OFFER;

    alter table BLC_ORDER_ITEM_DTL_ADJ 
        add constraint FK85F0248FD4AEA2C0 
        foreign key (ORDER_ITEM_PRICE_DTL_ID) 
        references BLC_ORDER_ITEM_PRICE_DTL;

    alter table BLC_ORDER_ITEM_PRICE_DTL 
        add constraint FK1FB64BF19AF166DF 
        foreign key (ORDER_ITEM_ID) 
        references BLC_ORDER_ITEM;

    create index MULTISHIP_OPTION_ORDER_INDEX on BLC_ORDER_MULTISHIP_OPTION (ORDER_ID);

    alter table BLC_ORDER_MULTISHIP_OPTION 
        add constraint FKB3D3F7D6C13085DD 
        foreign key (ADDRESS_ID) 
        references BLC_ADDRESS;

    alter table BLC_ORDER_MULTISHIP_OPTION 
        add constraint FKB3D3F7D681F34C7F 
        foreign key (FULFILLMENT_OPTION_ID) 
        references BLC_FULFILLMENT_OPTION;

    alter table BLC_ORDER_MULTISHIP_OPTION 
        add constraint FKB3D3F7D689FE8A02 
        foreign key (ORDER_ID) 
        references BLC_ORDER;

    alter table BLC_ORDER_MULTISHIP_OPTION 
        add constraint FKB3D3F7D69AF166DF 
        foreign key (ORDER_ITEM_ID) 
        references BLC_ORDER_ITEM;

    alter table BLC_ORDER_OFFER_CODE_XREF 
        add constraint FKFDF0E8533BB10F6D 
        foreign key (OFFER_CODE_ID) 
        references BLC_OFFER_CODE;

    alter table BLC_ORDER_OFFER_CODE_XREF 
        add constraint FKFDF0E85389FE8A02 
        foreign key (ORDER_ID) 
        references BLC_ORDER;

    create index ORDERPAYMENT_ADDRESS_INDEX on BLC_ORDER_PAYMENT (ADDRESS_ID);

    create index ORDERPAYMENT_ORDER_INDEX on BLC_ORDER_PAYMENT (ORDER_ID);

    create index ORDERPAYMENT_REFERENCE_INDEX on BLC_ORDER_PAYMENT (REFERENCE_NUMBER);

    create index ORDERPAYMENT_TYPE_INDEX on BLC_ORDER_PAYMENT (PAYMENT_TYPE);

    alter table BLC_ORDER_PAYMENT 
        add constraint FK9517A14FC13085DD 
        foreign key (ADDRESS_ID) 
        references BLC_ADDRESS;

    alter table BLC_ORDER_PAYMENT 
        add constraint FK9517A14F89FE8A02 
        foreign key (ORDER_ID) 
        references BLC_ORDER;

    alter table BLC_ORDER_PAYMENT_TRANSACTION 
        add constraint FK86FDE7CE6A69DD9D 
        foreign key (ORDER_PAYMENT) 
        references BLC_ORDER_PAYMENT;

    alter table BLC_ORDER_PAYMENT_TRANSACTION 
        add constraint FK86FDE7CEE1B66C71 
        foreign key (PARENT_TRANSACTION) 
        references BLC_ORDER_PAYMENT_TRANSACTION;

    create index PAGE_FULL_URL_INDEX on BLC_PAGE (FULL_URL);

    alter table BLC_PAGE 
        add constraint FKF41BEDD5D49D3961 
        foreign key (PAGE_TMPLT_ID) 
        references BLC_PAGE_TMPLT;

    create index PAGEATTRIBUTE_NAME_INDEX on BLC_PAGE_ATTRIBUTES (FIELD_NAME);

    create index PAGEATTRIBUTE_INDEX on BLC_PAGE_ATTRIBUTES (PAGE_ID);

    alter table BLC_PAGE_ATTRIBUTES 
        add constraint FK4FE27601883C2667 
        foreign key (PAGE_ID) 
        references BLC_PAGE;

    alter table BLC_PAGE_FLD 
        add constraint FK86433AD4883C2667 
        foreign key (PAGE_ID) 
        references BLC_PAGE;

    alter table BLC_PAGE_RULE_MAP 
        add constraint FK1ABA0CA336D91846 
        foreign key (PAGE_RULE_ID) 
        references BLC_PAGE_RULE;

    alter table BLC_PAGE_RULE_MAP 
        add constraint FK1ABA0CA3C38455DD 
        foreign key (BLC_PAGE_PAGE_ID) 
        references BLC_PAGE;

    alter table BLC_PAGE_TMPLT 
        add constraint FK325C9D5A1E1C128 
        foreign key (LOCALE_CODE) 
        references BLC_LOCALE;

    create index PAYMENTLOG_CUSTOMER_INDEX on BLC_PAYMENT_LOG (CUSTOMER_ID);

    create index PAYMENTLOG_LOGTYPE_INDEX on BLC_PAYMENT_LOG (LOG_TYPE);

    create index PAYMENTLOG_ORDERPAYMENT_INDEX on BLC_PAYMENT_LOG (ORDER_PAYMENT_ID);

    create index PAYMENTLOG_REFERENCE_INDEX on BLC_PAYMENT_LOG (ORDER_PAYMENT_REF_NUM);

    create index PAYMENTLOG_TRANTYPE_INDEX on BLC_PAYMENT_LOG (TRANSACTION_TYPE);

    create index PAYMENTLOG_USER_INDEX on BLC_PAYMENT_LOG (USER_NAME);

    alter table BLC_PAYMENT_LOG 
        add constraint FKA43703453E2FC4F9 
        foreign key (CURRENCY_CODE) 
        references BLC_CURRENCY;

    alter table BLC_PAYMENT_LOG 
        add constraint FKA43703457470F437 
        foreign key (CUSTOMER_ID) 
        references BLC_CUSTOMER;

    alter table BLC_PGTMPLT_FLDGRP_XREF 
        add constraint FK99D625F66A79BDB5 
        foreign key (FLD_GROUP_ID) 
        references BLC_FLD_GROUP;

    alter table BLC_PGTMPLT_FLDGRP_XREF 
        add constraint FK99D625F6D49D3961 
        foreign key (PAGE_TMPLT_ID) 
        references BLC_PAGE_TMPLT;

    create index PRODUCT_CATEGORY_INDEX on BLC_PRODUCT (DEFAULT_CATEGORY_ID);

    create index PRODUCT_URL_INDEX on BLC_PRODUCT (URL, URL_KEY);

    alter table BLC_PRODUCT 
        add constraint FK5B95B7C9DF057C3F 
        foreign key (DEFAULT_CATEGORY_ID) 
        references BLC_CATEGORY;

    alter table BLC_PRODUCT 
        add constraint FK5B95B7C96D386535 
        foreign key (DEFAULT_SKU_ID) 
        references BLC_SKU;

    create index PRODUCTATTRIBUTE_NAME_INDEX on BLC_PRODUCT_ATTRIBUTE (NAME);

    create index PRODUCTATTRIBUTE_INDEX on BLC_PRODUCT_ATTRIBUTE (PRODUCT_ID);

    alter table BLC_PRODUCT_ATTRIBUTE 
        add constraint FK56CE05865F11A0B7 
        foreign key (PRODUCT_ID) 
        references BLC_PRODUCT;

    alter table BLC_PRODUCT_BUNDLE 
        add constraint FK8CC5B85F11A0B7 
        foreign key (PRODUCT_ID) 
        references BLC_PRODUCT;

    create index CROSSSALE_CATEGORY_INDEX on BLC_PRODUCT_CROSS_SALE (CATEGORY_ID);

    create index CROSSSALE_INDEX on BLC_PRODUCT_CROSS_SALE (PRODUCT_ID);

    create index CROSSSALE_RELATED_INDEX on BLC_PRODUCT_CROSS_SALE (RELATED_SALE_PRODUCT_ID);

    alter table BLC_PRODUCT_CROSS_SALE 
        add constraint FK8324FB3C15D1A13D 
        foreign key (CATEGORY_ID) 
        references BLC_CATEGORY;

    alter table BLC_PRODUCT_CROSS_SALE 
        add constraint FK8324FB3C5F11A0B7 
        foreign key (PRODUCT_ID) 
        references BLC_PRODUCT;

    alter table BLC_PRODUCT_CROSS_SALE 
        add constraint FK8324FB3C62D84F9B 
        foreign key (RELATED_SALE_PRODUCT_ID) 
        references BLC_PRODUCT;

    create index PRODFEATURED_CATEGORY_INDEX on BLC_PRODUCT_FEATURED (CATEGORY_ID);

    create index PRODFEATURED_PRODUCT_INDEX on BLC_PRODUCT_FEATURED (PRODUCT_ID);

    alter table BLC_PRODUCT_FEATURED 
        add constraint FK4C49FFE415D1A13D 
        foreign key (CATEGORY_ID) 
        references BLC_CATEGORY;

    alter table BLC_PRODUCT_FEATURED 
        add constraint FK4C49FFE45F11A0B7 
        foreign key (PRODUCT_ID) 
        references BLC_PRODUCT;

    alter table BLC_PRODUCT_OPTION_VALUE 
        add constraint FK6DEEEDBD92EA8136 
        foreign key (PRODUCT_OPTION_ID) 
        references BLC_PRODUCT_OPTION;

    alter table BLC_PRODUCT_OPTION_XREF 
        add constraint FKDA42AB2F5F11A0B7 
        foreign key (PRODUCT_ID) 
        references BLC_PRODUCT;

    alter table BLC_PRODUCT_OPTION_XREF 
        add constraint FKDA42AB2F92EA8136 
        foreign key (PRODUCT_OPTION_ID) 
        references BLC_PRODUCT_OPTION;

    create index UPSALE_CATEGORY_INDEX on BLC_PRODUCT_UP_SALE (CATEGORY_ID);

    create index UPSALE_PRODUCT_INDEX on BLC_PRODUCT_UP_SALE (PRODUCT_ID);

    create index UPSALE_RELATED_INDEX on BLC_PRODUCT_UP_SALE (RELATED_SALE_PRODUCT_ID);

    alter table BLC_PRODUCT_UP_SALE 
        add constraint FKF69054F515D1A13D 
        foreign key (CATEGORY_ID) 
        references BLC_CATEGORY;

    alter table BLC_PRODUCT_UP_SALE 
        add constraint FKF69054F55F11A0B7 
        foreign key (PRODUCT_ID) 
        references BLC_PRODUCT;

    alter table BLC_PRODUCT_UP_SALE 
        add constraint FKF69054F562D84F9B 
        foreign key (RELATED_SALE_PRODUCT_ID) 
        references BLC_PRODUCT;

    alter table BLC_QUAL_CRIT_OFFER_XREF 
        add constraint FKD592E919D5F3FAF4 
        foreign key (OFFER_ID) 
        references BLC_OFFER;

    alter table BLC_QUAL_CRIT_OFFER_XREF 
        add constraint FKD592E9193615A91A 
        foreign key (OFFER_ITEM_CRITERIA_ID) 
        references BLC_OFFER_ITEM_CRITERIA;

    alter table BLC_QUAL_CRIT_PAGE_XREF 
        add constraint UK_874BE5902B6BC67F unique (PAGE_ITEM_CRITERIA_ID);

    alter table BLC_QUAL_CRIT_PAGE_XREF 
        add constraint FK874BE590883C2667 
        foreign key (PAGE_ID) 
        references BLC_PAGE;

    alter table BLC_QUAL_CRIT_PAGE_XREF 
        add constraint FK874BE590378418CD 
        foreign key (PAGE_ITEM_CRITERIA_ID) 
        references BLC_PAGE_ITEM_CRITERIA;

    alter table BLC_QUAL_CRIT_SC_XREF 
        add constraint UK_C4A353AFFF06F4DE unique (SC_ITEM_CRITERIA_ID);

    alter table BLC_QUAL_CRIT_SC_XREF 
        add constraint FKC4A353AF85C77F2B 
        foreign key (SC_ITEM_CRITERIA_ID) 
        references BLC_SC_ITEM_CRITERIA;

    alter table BLC_QUAL_CRIT_SC_XREF 
        add constraint FKC4A353AF13D95585 
        foreign key (SC_ID) 
        references BLC_SC;

    create index RATING_CUSTOMER_INDEX on BLC_RATING_DETAIL (CUSTOMER_ID);

    alter table BLC_RATING_DETAIL 
        add constraint FKC9D04AD7470F437 
        foreign key (CUSTOMER_ID) 
        references BLC_CUSTOMER;

    alter table BLC_RATING_DETAIL 
        add constraint FKC9D04ADD4E76BF4 
        foreign key (RATING_SUMMARY_ID) 
        references BLC_RATING_SUMMARY;

    create index RATINGSUMM_ITEM_INDEX on BLC_RATING_SUMMARY (ITEM_ID);

    create index RATINGSUMM_TYPE_INDEX on BLC_RATING_SUMMARY (RATING_TYPE);

    create index REVIEWDETAIL_CUSTOMER_INDEX on BLC_REVIEW_DETAIL (CUSTOMER_ID);

    create index REVIEWDETAIL_RATING_INDEX on BLC_REVIEW_DETAIL (RATING_DETAIL_ID);

    create index REVIEWDETAIL_SUMMARY_INDEX on BLC_REVIEW_DETAIL (RATING_SUMMARY_ID);

    create index REVIEWDETAIL_STATUS_INDEX on BLC_REVIEW_DETAIL (REVIEW_STATUS);

    alter table BLC_REVIEW_DETAIL 
        add constraint FK9CD7E6927470F437 
        foreign key (CUSTOMER_ID) 
        references BLC_CUSTOMER;

    alter table BLC_REVIEW_DETAIL 
        add constraint FK9CD7E69245DC39E0 
        foreign key (RATING_DETAIL_ID) 
        references BLC_RATING_DETAIL;

    alter table BLC_REVIEW_DETAIL 
        add constraint FK9CD7E692D4E76BF4 
        foreign key (RATING_SUMMARY_ID) 
        references BLC_RATING_SUMMARY;

    create index REVIEWFEED_CUSTOMER_INDEX on BLC_REVIEW_FEEDBACK (CUSTOMER_ID);

    create index REVIEWFEED_DETAIL_INDEX on BLC_REVIEW_FEEDBACK (REVIEW_DETAIL_ID);

    alter table BLC_REVIEW_FEEDBACK 
        add constraint FK7CC929867470F437 
        foreign key (CUSTOMER_ID) 
        references BLC_CUSTOMER;

    alter table BLC_REVIEW_FEEDBACK 
        add constraint FK7CC92986AE4769D6 
        foreign key (REVIEW_DETAIL_ID) 
        references BLC_REVIEW_DETAIL;

    create index ROLE_NAME_INDEX on BLC_ROLE (ROLE_NAME);

    create index SANDBOX_NAME_INDEX on BLC_SANDBOX (SANDBOX_NAME);

    alter table BLC_SANDBOX 
        add constraint FKDD37A9A174160452 
        foreign key (PARENT_SANDBOX_ID) 
        references BLC_SANDBOX;

    alter table BLC_SANDBOX_MGMT 
        add constraint UK_4845009FE52B6993 unique (SANDBOX_ID);

    alter table BLC_SANDBOX_MGMT 
        add constraint FK4845009F579FE59D 
        foreign key (SANDBOX_ID) 
        references BLC_SANDBOX;

    create index CONTENT_NAME_INDEX on BLC_SC (CONTENT_NAME);

    create index SC_OFFLN_FLG_INDX on BLC_SC (OFFLINE_FLAG);

    create index CONTENT_PRIORITY_INDEX on BLC_SC (PRIORITY);

    alter table BLC_SC 
        add constraint FK74EEB716A1E1C128 
        foreign key (LOCALE_CODE) 
        references BLC_LOCALE;

    alter table BLC_SC 
        add constraint FK74EEB71671EBFA46 
        foreign key (SC_TYPE_ID) 
        references BLC_SC_TYPE;

    alter table BLC_SC_FLDGRP_XREF 
        add constraint FK71612AEA6A79BDB5 
        foreign key (FLD_GROUP_ID) 
        references BLC_FLD_GROUP;

    alter table BLC_SC_FLDGRP_XREF 
        add constraint FK71612AEAF6B0BA84 
        foreign key (SC_FLD_TMPLT_ID) 
        references BLC_SC_FLD_TMPLT;

    alter table BLC_SC_FLD_MAP 
        add constraint FKD948019213D95585 
        foreign key (SC_ID) 
        references BLC_SC;

    alter table BLC_SC_FLD_MAP 
        add constraint FKD9480192DD6FD28A 
        foreign key (SC_FLD_ID) 
        references BLC_SC_FLD;

    alter table BLC_SC_ITEM_CRITERIA 
        add constraint FK6D52BDA213D95585 
        foreign key (SC_ID) 
        references BLC_SC;

    alter table BLC_SC_RULE_MAP 
        add constraint FK169F1C8256E51A06 
        foreign key (SC_RULE_ID) 
        references BLC_SC_RULE;

    alter table BLC_SC_RULE_MAP 
        add constraint FK169F1C82156E72FC 
        foreign key (BLC_SC_SC_ID) 
        references BLC_SC;

    create index SC_TYPE_NAME_INDEX on BLC_SC_TYPE (NAME);

    alter table BLC_SC_TYPE 
        add constraint FKE19886C3F6B0BA84 
        foreign key (SC_FLD_TMPLT_ID) 
        references BLC_SC_FLD_TMPLT;

    alter table BLC_SEARCH_FACET 
        add constraint FK4FFCC9863C3907C4 
        foreign key (FIELD_ID) 
        references BLC_FIELD;

    create index SEARCH_FACET_INDEX on BLC_SEARCH_FACET_RANGE (SEARCH_FACET_ID);

    alter table BLC_SEARCH_FACET_RANGE 
        add constraint FK7EC3B124B96B1C93 
        foreign key (SEARCH_FACET_ID) 
        references BLC_SEARCH_FACET;

    alter table BLC_SEARCH_FACET_XREF 
        add constraint FK35A63034DA7E1C7C 
        foreign key (REQUIRED_FACET_ID) 
        references BLC_SEARCH_FACET;

    alter table BLC_SEARCH_FACET_XREF 
        add constraint FK35A63034B96B1C93 
        foreign key (SEARCH_FACET_ID) 
        references BLC_SEARCH_FACET;

    create index SEARCH_ACTIVE_INDEX on BLC_SEARCH_INTERCEPT (ACTIVE_END_DATE);

    create index SEARCHSYNONYM_TERM_INDEX on BLC_SEARCH_SYNONYM (TERM);

    create index SHIPPINGRATE_FEESUB_INDEX on BLC_SHIPPING_RATE (FEE_SUB_TYPE);

    create index SHIPPINGRATE_FEE_INDEX on BLC_SHIPPING_RATE (FEE_TYPE);

    create index BLC_SITE_ID_VAL_INDEX on BLC_SITE (SITE_IDENTIFIER_VALUE);

    alter table BLC_SITE_CATALOG 
        add constraint FK5F3F2047A350C7F1 
        foreign key (CATALOG_ID) 
        references BLC_CATALOG;

    alter table BLC_SITE_CATALOG 
        add constraint FK5F3F2047843A8B63 
        foreign key (SITE_ID) 
        references BLC_SITE;

    alter table BLC_SITE_MAP_CFG 
        add constraint FK7012930FC50D449 
        foreign key (MODULE_CONFIG_ID) 
        references BLC_MODULE_CONFIGURATION;

    alter table BLC_SITE_MAP_GEN_CFG 
        add constraint FK1D76000A340ED71 
        foreign key (MODULE_CONFIG_ID) 
        references BLC_SITE_MAP_CFG;

    alter table BLC_SITE_MAP_URL_ENTRY 
        add constraint FKE2004FED36AFE1EE 
        foreign key (GEN_CONFIG_ID) 
        references BLC_CUST_SITE_MAP_GEN_CFG;

    create index SKU_ACTIVE_END_INDEX on BLC_SKU (ACTIVE_END_DATE);

    create index SKU_ACTIVE_START_INDEX on BLC_SKU (ACTIVE_START_DATE);

    create index SKU_AVAILABLE_INDEX on BLC_SKU (AVAILABLE_FLAG);

    create index SKU_DISCOUNTABLE_INDEX on BLC_SKU (DISCOUNTABLE_FLAG);

    create index SKU_EXTERNAL_ID_INDEX on BLC_SKU (EXTERNAL_ID);

    create index SKU_NAME_INDEX on BLC_SKU (NAME);

    create index SKU_TAXABLE_INDEX on BLC_SKU (TAXABLE_FLAG);

    create index SKU_UPC_INDEX on BLC_SKU (UPC);

    create index SKU_URL_KEY_INDEX on BLC_SKU (URL_KEY);

    alter table BLC_SKU 
        add constraint FK28E82CF73E2FC4F9 
        foreign key (CURRENCY_CODE) 
        references BLC_CURRENCY;

    alter table BLC_SKU 
        add constraint FK28E82CF77E555D75 
        foreign key (DEFAULT_PRODUCT_ID) 
        references BLC_PRODUCT;

    alter table BLC_SKU 
        add constraint FK28E82CF750D6498B 
        foreign key (ADDL_PRODUCT_ID) 
        references BLC_PRODUCT;

    create index SKUATTR_NAME_INDEX on BLC_SKU_ATTRIBUTE (NAME);

    create index SKUATTR_SKU_INDEX on BLC_SKU_ATTRIBUTE (SKU_ID);

    alter table BLC_SKU_ATTRIBUTE 
        add constraint FK6C6A5934B78C9977 
        foreign key (SKU_ID) 
        references BLC_SKU;

    create index SKUAVAIL_STATUS_INDEX on BLC_SKU_AVAILABILITY (AVAILABILITY_STATUS);

    create index SKUAVAIL_LOCATION_INDEX on BLC_SKU_AVAILABILITY (LOCATION_ID);

    create index SKUAVAIL_SKU_INDEX on BLC_SKU_AVAILABILITY (SKU_ID);

    alter table BLC_SKU_BUNDLE_ITEM 
        add constraint FKD55968CCF29B96 
        foreign key (PRODUCT_BUNDLE_ID) 
        references BLC_PRODUCT_BUNDLE;

    alter table BLC_SKU_BUNDLE_ITEM 
        add constraint FKD55968B78C9977 
        foreign key (SKU_ID) 
        references BLC_SKU;

    alter table BLC_SKU_FEE 
        add constraint FKEEB7181E3E2FC4F9 
        foreign key (CURRENCY_CODE) 
        references BLC_CURRENCY;

    alter table BLC_SKU_FEE_XREF 
        add constraint FKD88D409CB78C9977 
        foreign key (SKU_ID) 
        references BLC_SKU;

    alter table BLC_SKU_FEE_XREF 
        add constraint FKD88D409CCF4C9A82 
        foreign key (SKU_FEE_ID) 
        references BLC_SKU_FEE;

    alter table BLC_SKU_FULFILLMENT_EXCLUDED 
        add constraint FK84162D7381F34C7F 
        foreign key (FULFILLMENT_OPTION_ID) 
        references BLC_FULFILLMENT_OPTION;

    alter table BLC_SKU_FULFILLMENT_EXCLUDED 
        add constraint FK84162D73B78C9977 
        foreign key (SKU_ID) 
        references BLC_SKU;

    alter table BLC_SKU_FULFILLMENT_FLAT_RATES 
        add constraint FKC1988C9681F34C7F 
        foreign key (FULFILLMENT_OPTION_ID) 
        references BLC_FULFILLMENT_OPTION;

    alter table BLC_SKU_FULFILLMENT_FLAT_RATES 
        add constraint FKC1988C96B78C9977 
        foreign key (SKU_ID) 
        references BLC_SKU;

    alter table BLC_SKU_MEDIA_MAP 
        add constraint FKEB4AECF96E4720E0 
        foreign key (MEDIA_ID) 
        references BLC_MEDIA;

    alter table BLC_SKU_MEDIA_MAP 
        add constraint FKEB4AECF9D93D857F 
        foreign key (BLC_SKU_SKU_ID) 
        references BLC_SKU;

    alter table BLC_SKU_OPTION_VALUE_XREF 
        add constraint FK7B61DC0BB0C16A73 
        foreign key (PRODUCT_OPTION_VALUE_ID) 
        references BLC_PRODUCT_OPTION_VALUE;

    alter table BLC_SKU_OPTION_VALUE_XREF 
        add constraint FK7B61DC0BB78C9977 
        foreign key (SKU_ID) 
        references BLC_SKU;

    create index STATE_NAME_INDEX on BLC_STATE (NAME);

    alter table BLC_STATE 
        add constraint FK8F94A1EBA46E16CF 
        foreign key (COUNTRY) 
        references BLC_COUNTRY;

    create index ASST_FULL_URL_INDX on BLC_STATIC_ASSET (FULL_URL);

    alter table BLC_TAR_CRIT_OFFER_XREF 
        add constraint FK125F5803D5F3FAF4 
        foreign key (OFFER_ID) 
        references BLC_OFFER;

    alter table BLC_TAR_CRIT_OFFER_XREF 
        add constraint FK125F58033615A91A 
        foreign key (OFFER_ITEM_CRITERIA_ID) 
        references BLC_OFFER_ITEM_CRITERIA;

    alter table BLC_TAX_DETAIL 
        add constraint FKEABE4A4B3E2FC4F9 
        foreign key (CURRENCY_CODE) 
        references BLC_CURRENCY;

    alter table BLC_TAX_DETAIL 
        add constraint FKEABE4A4BC50D449 
        foreign key (MODULE_CONFIG_ID) 
        references BLC_MODULE_CONFIGURATION;

    create index TRANSLATION_INDEX on BLC_TRANSLATION (ENTITY_TYPE, ENTITY_ID, FIELD_NAME, LOCALE_CODE);

    alter table BLC_TRANS_ADDITNL_FIELDS 
        add constraint FK376DDE4B9E955B1D 
        foreign key (PAYMENT_TRANSACTION_ID) 
        references BLC_ORDER_PAYMENT_TRANSACTION;

    create index INCOMING_URL_INDEX on BLC_URL_HANDLER (INCOMING_URL);

    create index ZIPCODE_CITY_INDEX on BLC_ZIP_CODE (ZIP_CITY);

    create index ZIPCODE_LATITUDE_INDEX on BLC_ZIP_CODE (ZIP_LATITUDE);

    create index ZIPCODE_LONGITUDE_INDEX on BLC_ZIP_CODE (ZIP_LONGITUDE);

    create index ZIPCODE_STATE_INDEX on BLC_ZIP_CODE (ZIP_STATE);

    create index ZIPCODE_ZIP_INDEX on BLC_ZIP_CODE (ZIPCODE);

    alter table DiscreteOrderItemExtImpl 
        add constraint FK32448A59B76B9466 
        foreign key (ORDER_ITEM_ID) 
        references BLC_DISCRETE_ORDER_ITEM;

    alter table OrderExtImpl 
        add constraint FKF0F898D389FE8A02 
        foreign key (ORDER_ID) 
        references BLC_ORDER;

    create table SEQUENCE_GENERATOR (
         ID_NAME varchar(255) not null ,
         ID_VAL int8,
        primary key ( ID_NAME ) 
    ) ;
