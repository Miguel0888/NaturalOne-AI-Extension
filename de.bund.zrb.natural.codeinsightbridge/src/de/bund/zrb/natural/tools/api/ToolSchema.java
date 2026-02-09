package de.bund.zrb.natural.tools.api;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Very small schema abstraction for v1.
 */
public final class ToolSchema {

    private final String humanHint;
    private final Map<String, String> fields;

    private ToolSchema(String humanHint, Map<String, String> fields) {
        this.humanHint = humanHint;
        this.fields = fields;
    }

    public String getHumanHint() {
        return humanHint;
    }

    public Map<String, String> getFields() {
        return fields;
    }

    public static ToolSchema none() {
        return new ToolSchema("", Collections.<String, String>emptyMap());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String hint;
        private final Map<String, String> fields = new LinkedHashMap<String, String>();

        public Builder hint(String hint) {
            this.hint = hint;
            return this;
        }

        public Builder field(String name, String description) {
            fields.put(name, description);
            return this;
        }

        public ToolSchema build() {
            return new ToolSchema(hint == null ? "" : hint,
                    Collections.unmodifiableMap(new LinkedHashMap<String, String>(fields)));
        }
    }
}

