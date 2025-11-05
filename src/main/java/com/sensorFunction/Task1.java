package com.function;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.sql.*;

public class Task1 {

    public static final int SENSOR_COUNT = 20;

    @FunctionName("Task1")
    public HttpResponseMessage run(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.GET},
                authLevel = AuthorizationLevel.FUNCTION)
            HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        context.getLogger().info("Generating sensor data...");

        List<Map<String, Object>> sensors = new ArrayList<>();
        for (int i = 1; i <= SENSOR_COUNT; i++) {
            Map<String, Object> sensor = new HashMap<>();
            sensor.put("sensorId", i);
            sensor.put("temperature", ThreadLocalRandom.current().nextInt(5, 19));
            sensor.put("windSpeed", ThreadLocalRandom.current().nextInt(12, 25));
            sensor.put("humidity", ThreadLocalRandom.current().nextInt(30, 61));
            sensor.put("co2", ThreadLocalRandom.current().nextInt(400, 1601));
            sensors.add(sensor);
        }

        String connStr = System.getenv("DB_CONNECTION");

        try (Connection conn = DriverManager.getConnection(connStr)) {
            String insert_query = "INSERT INTO SensorReadings (SensorId, Temperature, WindSpeed, Humidity, CO2) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(insert_query)) {
                for (Map<String, Object> sensor : sensors) {
                    pstmt.setInt(1, (Integer) sensor.get("sensorId"));
                    pstmt.setInt(2, (Integer) sensor.get("temperature"));
                    pstmt.setInt(3, (Integer) sensor.get("windSpeed"));
                    pstmt.setInt(4, (Integer) sensor.get("humidity"));
                    pstmt.setInt(5, (Integer) sensor.get("co2"));
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            }
        } catch (SQLException e) {
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                          .body("Database connection failed: " + e.getMessage())
                          .build();
        }

        return request.createResponseBuilder(HttpStatus.OK)
                      .header("Content-Type", "application/json")
                      .body(sensors)
                      .build();
    }
}
