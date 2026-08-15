package com.cornerstone.system.constant;

/** 缓存键常量。 */
public final class CacheConstants {

    /** 字典缓存 key：cornerstone:dict:{dictType} */
    public static final String DICT_KEY = "cornerstone:dict:%s";

    /** 参数缓存 key：cornerstone:config:{configKey} */
    public static final String CONFIG_KEY = "cornerstone:config:%s";

    private CacheConstants() {}
}
