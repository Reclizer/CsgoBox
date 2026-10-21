package com.reclizer.csgobox.config;

/** A reward and its grade travel together, including when editor slots are empty. */
public record RewardEntry(String item, int grade) {
    public RewardEntry {
        if (item == null || item.isBlank() || grade < 1 || grade > 5)
            throw new IllegalArgumentException("Invalid reward entry");
    }
}
