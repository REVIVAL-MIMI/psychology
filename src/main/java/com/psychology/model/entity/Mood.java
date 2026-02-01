package com.psychology.model.entity;

public enum Mood {
    VERY_HAPPY("😊", "Very Happy"),
    HAPPY("🙂", "Happy"),
    NEUTRAL("😐", "Neutral"),
    SAD("😔", "Sad"),
    VERY_SAD("😢", "Very Sad"),
    ANGRY("😠", "Angry"),
    ANXIOUS("😰", "Anxious"),
    STRESSED("😫", "Stressed"),
    TIRED("😴", "Tired"),
    EXCITED("🤩", "Excited"),
    PEACEFUL("😌", "Peaceful"),
    CONFUSED("😕", "Confused");

    private final String emoji;
    private final String description;

    Mood(String emoji, String description) {
        this.emoji = emoji;
        this.description = description;
    }

    public String getEmoji() {
        return emoji;
    }

    public String getDescription() {
        return description;
    }

    public static Mood fromString(String mood) {
        for (Mood m : Mood.values()) {
            if (m.name().equalsIgnoreCase(mood) || m.getDescription().equalsIgnoreCase(mood)) {
                return m;
            }
        }
        return null;
    }
}