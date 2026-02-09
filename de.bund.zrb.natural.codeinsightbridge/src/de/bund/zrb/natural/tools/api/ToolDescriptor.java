package de.bund.zrb.natural.tools.api;

/**
 * Descriptor shown in UI and advertised to the AI.
 */
public final class ToolDescriptor {

    private final String id;
    private final String displayName;
    private final String description;
    private final String category;
    private final ToolCapability capability;
    private final ToolRiskLevel riskLevel;
    private final ToolSchema inputSchema;
    private final ToolSchema outputSchema;

    public ToolDescriptor(String id, String displayName, String description, String category,
            ToolCapability capability, ToolRiskLevel riskLevel, ToolSchema inputSchema, ToolSchema outputSchema) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.category = category;
        this.capability = capability;
        this.riskLevel = riskLevel;
        this.inputSchema = inputSchema == null ? ToolSchema.none() : inputSchema;
        this.outputSchema = outputSchema;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public ToolCapability getCapability() {
        return capability;
    }

    public ToolRiskLevel getRiskLevel() {
        return riskLevel;
    }

    public ToolSchema getInputSchema() {
        return inputSchema;
    }

    public ToolSchema getOutputSchema() {
        return outputSchema;
    }
}

