package com.veganbeauty.app.features.routine;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class SkincareStep {
    private int index;
    private String name;
    private String description;
    private boolean isChecked;
    /** Estimated minutes for this step within the routine. */
    private int durationMinutes;

    public SkincareStep(int index, String name, String description, boolean isChecked) {
        this(index, name, description, isChecked, defaultDurationForName(name));
    }

    public SkincareStep(int index, String name, String description, boolean isChecked, int durationMinutes) {
        this.index = index;
        this.name = name;
        this.description = description;
        this.isChecked = isChecked;
        this.durationMinutes = Math.max(1, durationMinutes);
    }

    public int getIndex() { return index; }
    public void setIndex(int index) { this.index = index; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isChecked() { return isChecked; }
    public void setChecked(boolean checked) { isChecked = checked; }

    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = Math.max(1, durationMinutes);
    }

    public String getDurationLabel() {
        return durationMinutes + "p";
    }

    /** Serialize: index|name|description|checked|durationMinutes */
    public String toStorageString() {
        return index + "|"
                + sanitize(name) + "|"
                + sanitize(description) + "|"
                + isChecked + "|"
                + durationMinutes;
    }

    public static List<SkincareStep> parseList(Set<String> rawSet) {
        List<SkincareStep> list = new ArrayList<>();
        if (rawSet == null) return list;
        for (String raw : rawSet) {
            SkincareStep step = parseOne(raw);
            if (step != null) list.add(step);
        }
        Collections.sort(list, (a, b) -> Integer.compare(a.getIndex(), b.getIndex()));
        return list;
    }

    public static Set<String> toStorageSet(List<SkincareStep> steps) {
        Set<String> out = new HashSet<>();
        if (steps == null) return out;
        for (int i = 0; i < steps.size(); i++) {
            SkincareStep s = steps.get(i);
            s.setIndex(i);
            out.add(s.toStorageString());
        }
        return out;
    }

    public static void reindex(List<SkincareStep> steps) {
        if (steps == null) return;
        for (int i = 0; i < steps.size(); i++) {
            steps.get(i).setIndex(i);
        }
    }

    @Nullable
    public static SkincareStep parseOne(String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        try {
            if (raw.contains("|")) {
                String[] parts = raw.split("\\|", -1);
                if (parts.length >= 5) {
                    return new SkincareStep(
                            Integer.parseInt(parts[0]),
                            parts[1],
                            parts[2],
                            Boolean.parseBoolean(parts[3]),
                            Integer.parseInt(parts[4])
                    );
                }
                if (parts.length >= 4) {
                    return new SkincareStep(
                            Integer.parseInt(parts[0]),
                            parts[1],
                            parts[2],
                            Boolean.parseBoolean(parts[3])
                    );
                }
            }

            // Legacy: index:name:description:checked
            String[] parts = raw.split(":");
            if (parts.length >= 4) {
                int index = Integer.parseInt(parts[0]);
                String name = parts[1];
                String desc = parts[2];
                if (parts.length > 4) {
                    StringBuilder descBuilder = new StringBuilder(parts[2]);
                    for (int i = 3; i < parts.length - 1; i++) {
                        descBuilder.append(':').append(parts[i]);
                    }
                    desc = descBuilder.toString();
                    return new SkincareStep(index, name, desc, Boolean.parseBoolean(parts[parts.length - 1]));
                }
                return new SkincareStep(index, name, desc, Boolean.parseBoolean(parts[3]));
            }
            if (parts.length == 3) {
                return new SkincareStep(99, parts[0], parts[1], Boolean.parseBoolean(parts[2]));
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public static int defaultDurationForName(String name) {
        if (name == null) return 2;
        String lower = name.toLowerCase(Locale.getDefault());
        if (lower.contains("cleanser") || lower.contains("sữa rửa mặt") || lower.contains("rửa mặt")) return 2;
        if (lower.contains("toner") || lower.contains("nước hoa hồng") || lower.contains("cân bằng")) return 1;
        if (lower.contains("serum") || lower.contains("tinh chất")) return 3;
        if (lower.contains("moisturizer") || lower.contains("kem dưỡng ẩm") || lower.contains("dưỡng ẩm") || lower.contains("khóa ẩm")) return 2;
        if (lower.contains("sunscreen") || lower.contains("chống nắng") || lower.contains("kem chống nắng")) return 5;
        if (lower.contains("makeup remover") || lower.contains("tẩy trang")) return 5;
        return 2;
    }

    private static String sanitize(String value) {
        if (value == null) return "";
        return value.replace("|", "/");
    }
}
