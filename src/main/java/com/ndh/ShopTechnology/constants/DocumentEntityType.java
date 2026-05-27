package com.ndh.ShopTechnology.constants;

public final class DocumentEntityType {

    private DocumentEntityType() {
    }

    public static final int ID_DOCUMENT_ENTITY_UNASSIGNED = -1;

    public static final int ID_DOCUMENT_ENTITY_PRODUCT = 100_000;

    public static final int ID_DOCUMENT_ENTITY_PRODUCT_VARIANT = 105_000;

    public static final int ID_DOCUMENT_ENTITY_USER = 200_000;

    public static final int ID_DOCUMENT_ENTITY_CATEGORY = 300_000;

    public static final int ID_DOCUMENT_ENTITY_BRAND = 400_000;

    public static final int ID_DOCUMENT_ENTITY_ORDER = 500_000;

    @Deprecated
    public static final int UNASSIGNED = ID_DOCUMENT_ENTITY_UNASSIGNED;

    @Deprecated
    public static final int PRODUCT = ID_DOCUMENT_ENTITY_PRODUCT;

    @Deprecated
    public static final int USER = ID_DOCUMENT_ENTITY_USER;

    @Deprecated
    public static final int CATEGORY = ID_DOCUMENT_ENTITY_CATEGORY;

    @Deprecated
    public static final int BRAND = ID_DOCUMENT_ENTITY_BRAND;

    @Deprecated
    public static final int ORDER = ID_DOCUMENT_ENTITY_ORDER;

    public static final int MIN_REGISTERED_POSITIVE_TYPE = ID_DOCUMENT_ENTITY_PRODUCT;

    public static final int MAX_REGISTERED_POSITIVE_TYPE = ID_DOCUMENT_ENTITY_ORDER;

    public static boolean isRegistered(int type) {
        return switch (type) {
            case ID_DOCUMENT_ENTITY_UNASSIGNED,
                    ID_DOCUMENT_ENTITY_PRODUCT,
                    ID_DOCUMENT_ENTITY_PRODUCT_VARIANT,
                    ID_DOCUMENT_ENTITY_USER,
                    ID_DOCUMENT_ENTITY_CATEGORY,
                    ID_DOCUMENT_ENTITY_BRAND,
                    ID_DOCUMENT_ENTITY_ORDER -> true;
            default -> false;
        };
    }
}
