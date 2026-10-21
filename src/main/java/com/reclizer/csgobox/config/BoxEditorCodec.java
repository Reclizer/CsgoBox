package com.reclizer.csgobox.config;

import com.google.gson.Gson;

/** Bounded admin-only editor payloads; full validation is always repeated on the server. */
public final class BoxEditorCodec {
    public static final int MAX_JSON = 262144;
    private static final Gson GSON = new Gson();
    private BoxEditorCodec() {}
    public static String encode(BoxDefinition definition) { return GSON.toJson(definition); }
    public static BoxDefinition decode(String json) {
        if (json == null || json.length() > MAX_JSON) throw new IllegalArgumentException("Oversized box definition");
        BoxDefinition definition = GSON.fromJson(json, BoxDefinition.class);
        if (definition == null) throw new IllegalArgumentException("Missing box definition");
        return definition;
    }
}
