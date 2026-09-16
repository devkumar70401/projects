package com.devendra.healthtracker.features;

/**
 * Immutable metadata model for an application capability/feature.
 * Pure data object - never holds Views, Composables, or mutable UI state.
 */
public class FeatureItem {

    private final String id;
    private final String shortName;
    private final String fullName;
    private final String subtitle;
    private final String accentColorHex;
    private final int defaultPriority;

    public FeatureItem(String id, String shortName, String fullName, String subtitle, String accentColorHex, int defaultPriority) {
        this.id = id;
        this.shortName = shortName;
        this.fullName = fullName;
        this.subtitle = subtitle;
        this.accentColorHex = accentColorHex;
        this.defaultPriority = defaultPriority;
    }

    public String getId() {
        return id;
    }

    public String getShortName() {
        return shortName;
    }

    public String getFullName() {
        return fullName;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public String getAccentColorHex() {
        return accentColorHex;
    }

    public int getDefaultPriority() {
        return defaultPriority;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FeatureItem that = (FeatureItem) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
