package com.reclizer.csgobox.config;

/** Public acquisition data only. Disabled drafts, rewards and weights are never synchronized here. */
public record AcquisitionSummary(String id, String key, AcquisitionDefinition boxRules, AcquisitionDefinition keyRules) {
    public AcquisitionSummary {
        if (!BoxDefinition.validId(id) || key == null || key.length() > 8192)
            throw new IllegalArgumentException("Invalid acquisition summary");
        boxRules = publicRules(boxRules);
        keyRules = key.isBlank() ? AcquisitionDefinition.disabled() : publicRules(keyRules);
        boxRules.validate(value -> !value.isBlank() && value.length() <= 8192);
        keyRules.validate(value -> !value.isBlank() && value.length() <= 8192);
    }
    private static AcquisitionDefinition publicRules(AcquisitionDefinition rules) {
        if (rules == null) return AcquisitionDefinition.disabled();
        return new AcquisitionDefinition(rules.crafting().enabled() ? rules.crafting() : null,
                rules.drop().enabled() ? rules.drop() : null, rules.trade().enabled() ? rules.trade() : null);
    }
    public static AcquisitionSummary of(BoxDefinition box) {
        return new AcquisitionSummary(box.id(), box.key(), box.acquisition(), box.keyAcquisition());
    }
}
