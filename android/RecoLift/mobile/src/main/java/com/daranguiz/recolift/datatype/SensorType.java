package com.daranguiz.recolift.datatype;

public enum SensorType {
//    ACCEL_WATCH(0), GYRO_WATCH(1), ACCEL_PHONE(2), GYRO_PHONE(3);
    ACCEL_WATCH(0), GYRO_WATCH(1);

    private final int value;
    private SensorType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
