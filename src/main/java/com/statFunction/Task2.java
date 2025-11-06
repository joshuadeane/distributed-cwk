package com.function;

import java.util.*;
import java.sql.*;
import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;

/**
 * Azure Functions with HTTP Trigger.
 */
public class Task2 {

    @FunctionName("Task2")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = {HttpMethod.GET}, authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        context.getLogger().info("Java HTTP trigger processed a request.");

        String connStr = System.getenv("DB_CONNECTION");

        try (Connection conn = DriverManager.getConnection(connStr)) {

            String searchQuery = "SELECT * FROM SensorReadings";

            Map<Integer, List<Map<String, Object>>> groupedBySensor = new HashMap<>();

            try (PreparedStatement stmt = conn.prepareStatement(searchQuery);
                ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    int sensorId = rs.getInt("SensorId");

                    Map<String, Object> reading = new HashMap<>();
                    reading.put("Temperature", rs.getInt("Temperature"));
                    reading.put("WindSpeed", rs.getInt("WindSpeed"));
                    reading.put("Humidity", rs.getInt("Humidity"));
                    reading.put("CO2", rs.getInt("CO2"));

                    groupedBySensor
                        .computeIfAbsent(sensorId, k -> new ArrayList<>())
                        .add(reading);
                }
            }
            List<Map<String, Object>> sensorStats = new ArrayList<>();
            // Process each sensor’s readings
            for (Map.Entry<Integer, List<Map<String, Object>>> entry : groupedBySensor.entrySet()) {
                int sensorId = entry.getKey();
                List<Map<String, Object>> readings = entry.getValue();

                double sumTemp = 0, sumWind = 0, sumHum = 0, sumCO2 = 0;
                int minTemp = Integer.MAX_VALUE, minWind = Integer.MAX_VALUE, minHum = Integer.MAX_VALUE, minCO2 = Integer.MAX_VALUE;
                int maxTemp = Integer.MIN_VALUE, maxWind = Integer.MIN_VALUE, maxHum = Integer.MIN_VALUE, maxCO2 = Integer.MIN_VALUE;

                for (Map<String, Object> r : readings) {
                    int temp = (Integer) r.get("Temperature");
                    int wind = (Integer) r.get("WindSpeed");
                    int hum = (Integer) r.get("Humidity");
                    int co2 = (Integer) r.get("CO2");

                    sumTemp += temp;
                    sumWind += wind;
                    sumHum += hum;
                    sumCO2 += co2;

                    minTemp = Math.min(minTemp, temp);
                    minWind = Math.min(minWind, wind);
                    minHum = Math.min(minHum, hum);
                    minCO2 = Math.min(minCO2, co2);

                    maxTemp = Math.max(maxTemp, temp);
                    maxWind = Math.max(maxWind, wind);
                    maxHum = Math.max(maxHum, hum);
                    maxCO2 = Math.max(maxCO2, co2);
                }

                int count = readings.size();

                double avgTemp = sumTemp / count;
                double avgWind = sumWind / count;
                double avgHum = sumHum / count;
                double avgCO2 = sumCO2 / count;


                // Create statistics object for this sensor
                Map<String, Object> stats = new HashMap<>();
                stats.put("sensorId", sensorId);
                
                Map<String, Object> temperature = new HashMap<>();
                temperature.put("min", minTemp);
                temperature.put("max", maxTemp);
                temperature.put("avg", Math.round(avgTemp * 100.0) / 100.0);
                stats.put("temperature", temperature);

                Map<String, Object> windSpeed = new HashMap<>();
                windSpeed.put("min", minWind);
                windSpeed.put("max", maxWind);
                windSpeed.put("avg", Math.round(avgWind * 100.0) / 100.0);
                stats.put("windSpeed", windSpeed);

                Map<String, Object> humidity = new HashMap<>();
                humidity.put("min", minHum);
                humidity.put("max", maxHum);
                humidity.put("avg", Math.round(avgHum * 100.0) / 100.0);
                stats.put("humidity", humidity);

                Map<String, Object> co2 = new HashMap<>();
                co2.put("min", minCO2);
                co2.put("max", maxCO2);
                co2.put("avg", Math.round(avgCO2 * 100.0) / 100.0);
                stats.put("co2", co2);

                sensorStats.add(stats);

                context.getLogger().info(String.format(
                    "Sensor %d -> Temperature[min=%d, max=%d, avg=%.2f], Wind[min=%d, max=%d, avg=%.2f], Humidity[min=%d, max=%d, avg=%.2f], CO2[min=%d, max=%d, avg=%.2f]",
                    sensorId,
                    minTemp, maxTemp, avgTemp,
                    minWind, maxWind, avgWind,
                    minHum, maxHum, avgHum,
                    minCO2, maxCO2, avgCO2
                ));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("totalSensors", sensorStats.size());
            response.put("statistics", sensorStats);

            return request.createResponseBuilder(HttpStatus.OK)
                          .header("Content-Type", "application/json")
                          .body(response)
                          .build();

        } catch (SQLException e) {
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                          .body("Database connection failed: " + e.getMessage())
                          .build();
        }  
    }
}
