package com.devendra.healthtracker.ui.elements;

/**
 * ElementType
 * Defines the elemental energy states for animated card borders and fluid bubbles:
 * - BUBBLING: Rising effervescent liquid bubbles, shimmering aquatic rings.
 * - FIRE: Blazing solar flames, flickering tongues of fire, drifting embers.
 * - THUNDER: Crackling electric lightning arcs, zig-zag discharges, high-voltage sparks.
 * - AIR: Swirling ethereal wind currents, flowing aerodynamic vortex ribbons.
 * - VOLCANO: Molten incandescent magma veins, explosive volcanic cinder bursts.
 */
public enum ElementType {
    BUBBLING("bubbling", "🫧 Bubbling", 0xFF06B6D4, 0xFF0891B2, 0xFF67E8F9),
    FIRE("fire", "🔥 Fire", 0xFFEF4444, 0xFFDC2626, 0xFFFDE047),
    THUNDER("thunder", "⚡ Thunder", 0xFF8B5CF6, 0xFF6366F1, 0xFF38BDF8),
    AIR("air", "🌪 Air", 0xFF10B981, 0xFF059669, 0xFFA7F3D0),
    VOLCANO("volcano", "🌋 Volcano", 0xFFF97316, 0xFFB91C1C, 0xFFFBBF24);

    private final String id;
    private final String displayName;
    private final int primaryColor;
    private final int secondaryColor;
    private final int accentColor;

    ElementType(String id, String displayName, int primaryColor, int secondaryColor, int accentColor) {
        this.id = id;
        this.displayName = displayName;
        this.primaryColor = primaryColor;
        this.secondaryColor = secondaryColor;
        this.accentColor = accentColor;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getPrimaryColor() {
        return primaryColor;
    }

    public int getSecondaryColor() {
        return secondaryColor;
    }

    public int getAccentColor() {
        return accentColor;
    }

    public static ElementType fromId(String id) {
        if (id == null) return FIRE;
        for (ElementType type : values()) {
            if (type.id.equalsIgnoreCase(id) || type.name().equalsIgnoreCase(id)) {
                return type;
            }
        }
        return FIRE;
    }

    public static ElementType forFeature(String featureId) {
        if (featureId == null) return FIRE;
        switch (featureId) {
            case "pomodoro":
                return FIRE;
            case "deepclean":
                return VOLCANO;
            case "sysmon":
                return THUNDER;
            case "hydration":
                return BUBBLING;
            case "routine":
                return AIR;
            case "checklist":
                return THUNDER;
            case "blueprint":
                return FIRE;
            case "wellbeing":
                return AIR;
            case "media":
                return THUNDER;
            case "calculator":
                return VOLCANO;
            case "calendar":
                return BUBBLING;
            default:
                return FIRE;
        }
    }
}
