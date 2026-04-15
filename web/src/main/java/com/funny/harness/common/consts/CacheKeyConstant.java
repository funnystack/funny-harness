package com.funny.harness.common.consts;



/**
 */
public class CacheKeyConstant {

    public static final String PREFIX = "harness:web:";

    public static String LOCK_CREATIVE_COMBO_TASK_CACHE_KEY = "lock_creative_combo_task_%s";
    public static String LOCK_SPLIT_ADUNIT_CACHE_KEY = "lock_split_adunit_%s";

    public static final Integer HALF_DAY_SECOND = 60 * 60 * 12;
    public static final Integer ONE_HOUR_SECOND = 60 * 60;
    public static final Integer ONE_DAY_SECOND = 60 * 60 * 24;
    public static final Integer LOCK_TIME_MIN = 1000 * 10;

}
