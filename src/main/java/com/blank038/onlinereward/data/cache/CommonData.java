package com.blank038.onlinereward.data.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Blank038
 * @since 1.3.9-SNAPSHOT
 */
public class CommonData {
    public static final Map<String, PlayerData> DATA_MAP = new ConcurrentHashMap<>();
}
