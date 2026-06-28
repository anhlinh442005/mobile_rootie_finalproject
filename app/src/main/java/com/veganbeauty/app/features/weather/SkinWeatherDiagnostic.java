package com.veganbeauty.app.features.weather;
import java.util.List;

public class SkinWeatherDiagnostic {
    private String id;
    private long timestamp;
    private String date;
    private String city;
    private double temperature;
    private int humidity;
    private double uv;
    private int pm25;
    private String skinType;
    private int oilyPercent;
    private int hydrationPercent;
    private int sensitivityPercent;
    private String insight;
    private List<RoutineItem> recommendedRoutine;

    public SkinWeatherDiagnostic(String id, long timestamp, String date, String city, double temperature, int humidity, double uv, int pm25, String skinType, int oilyPercent, int hydrationPercent, int sensitivityPercent, String insight, List<RoutineItem> recommendedRoutine) {
        this.id = id; this.timestamp = timestamp; this.date = date; this.city = city; this.temperature = temperature; this.humidity = humidity; this.uv = uv; this.pm25 = pm25; this.skinType = skinType; this.oilyPercent = oilyPercent; this.hydrationPercent = hydrationPercent; this.sensitivityPercent = sensitivityPercent; this.insight = insight; this.recommendedRoutine = recommendedRoutine;
    }

    public String getId() { return id; }
    public long getTimestamp() { return timestamp; }
    public String getDate() { return date; }
    public String getCity() { return city; }
    public double getTemperature() { return temperature; }
    public int getHumidity() { return humidity; }
    public double getUv() { return uv; }
    public int getPm25() { return pm25; }
    public String getSkinType() { return skinType; }
    public int getOilyPercent() { return oilyPercent; }
    public int getHydrationPercent() { return hydrationPercent; }
    public int getSensitivityPercent() { return sensitivityPercent; }
    public String getInsight() { return insight; }
    public List<RoutineItem> getRecommendedRoutine() { return recommendedRoutine; }

    public static class RoutineItem {
        private String category;
        private String productName;
        private String description;
        public RoutineItem(String category, String productName, String description) {
            this.category = category; this.productName = productName; this.description = description;
        }
        public String getCategory() { return category; }
        public String getProductName() { return productName; }
        public String getDescription() { return description; }
    }
}
