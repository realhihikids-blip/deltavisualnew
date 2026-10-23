package dev.deltavisuals.module;

public enum Category {
    VISUALS("Visuals"),
    RENDER("Render"),
    HUD("HUD"),
    PLAYER("Player"),
    SETTINGS("Settings");

    public final String label;

    Category(String label) {
        this.label = label;
    }
}
