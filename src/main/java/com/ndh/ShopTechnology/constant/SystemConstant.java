package com.ndh.ShopTechnology.constant;

import org.springframework.beans.factory.annotation.Value;

public class SystemConstant {

    @Value("${api.prefix}")
    private static String apiPrefix;

    public static final Long MAN_ID         = 1L;
    public static final int ACTIVE_STATUS   = 1;
    public static final int INACTIVE_STATUS = 0;

    public static final long ACCESS_TOKEN_VALIDITY_SECONDS  = 3600000;//1h
    public static final int EXPIRY_TOKEN_RESET_PASSWORD     = 10;//10p

    public static final String SIGNING_KEY      = "======================ndh===========================";
    public static final String TOKEN_PREFIX     = "Bearer ";
    public static final String HEADER_STRING    = "Authorization";
    public static final String AUTHORITIES_KEY  = "scopes";

    public static final String SERVER_PATH      = "http://localhost:8080/"+apiPrefix+"/";

}