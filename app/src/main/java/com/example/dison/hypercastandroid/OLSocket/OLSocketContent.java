package com.example.dison.hypercastandroid.OLSocket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class for providing sample content for user interfaces created by
 * Android template wizards.
 * <p>
 * TODO: Replace all uses of this class before publishing your app.
 */
public class OLSocketContent {

    /**
     * An array of sample (dummy) items.
     */
    public static final List<OLSocketItem> ITEMS = new ArrayList<>();

    /**
     * A map of sample (dummy) items, by ID.
     */
    public static final Map<String, OLSocketItem> ITEM_MAP = new HashMap<>();


    private static void addItem(OLSocketItem item) {
        ITEMS.add(item);
        ITEM_MAP.put(item.id, item);
    }

    private static OLSocketItem createDummyItem(int position) {
        return new OLSocketItem(String.valueOf(position), "Item " + position, makeDetails(position));
    }

    private static String makeDetails(int position) {
        StringBuilder builder = new StringBuilder();
        builder.append("Details about Item: ").append(position);
        for (int i = 0; i < position; i++) {
            builder.append("\nMore details information here.");
        }
        return builder.toString();
    }

    /**
     * A Overlay Socket item representing a piece of content.
     */
    public static class OLSocketItem {
        public final String id;
        public final String content;
        public final String details;

        public OLSocketItem(String id, String content, String details) {
            this.id = id;
            this.content = content;
            this.details = details;
        }

        @Override
        public String toString() {
            return content;
        }
    }
}
