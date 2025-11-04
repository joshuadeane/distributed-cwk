import java.util.concurrent.ThreadLocalRandom;

public class Task1 {
    public static final int SENSOR_COUNT = 20;

    public static void main(String[] args) {
        for (int i = 1; i <= SENSOR_COUNT; i++) {
            int sensorId = i;
            int temp = ThreadLocalRandom.current().nextInt(5, 19);
            int windSpeed = ThreadLocalRandom.current().nextInt(12, 25);
            int relHumidity = ThreadLocalRandom.current().nextInt(30, 61);
            int co2 = ThreadLocalRandom.current().nextInt(400, 1601);
            System.out.printf("Sensor %d: Temp=%d°C, Wind=%d mph, Humidity=%d%%, CO2=%d ppm%n", 
                                sensorId, temp, windSpeed, relHumidity, co2);
        }
    }
}