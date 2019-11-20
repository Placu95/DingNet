package iot.networkentity;

import datagenerator.*;
import datagenerator.iaqsensor.IAQDataGeneratorSingleton;
import util.Pair;

import java.time.LocalTime;
import java.util.LinkedList;
import java.util.List;

/**
 * An enum representing sensors for the motes.
 */
public enum MoteSensor {

    SOOT(new SootDataGenerator()),
    OZONE(new OzoneDataGenerator()),
    CARBON_DIOXIDE(new CarbonDioxideDataGenerator()),
    PARTICULATE_MATTER(new ParticulateMatterDataGenerator()),
    GPS(new GPSDataGenerator()),
    IAQ(IAQDataGeneratorSingleton.getInstance());


    private SensorDataGenerator sensorDataGenerator;

    MoteSensor(SensorDataGenerator sensorDataGenerator) {
        this.sensorDataGenerator = sensorDataGenerator;
    }

    public byte[] getValue(int xpos, int ypos, LocalTime time){
        return sensorDataGenerator.generateData(xpos,ypos,time);
    }

    public double getValue(double xpos, double ypos){
        return sensorDataGenerator.nonStaticDataGeneration(xpos,ypos);
    }

    public List<Byte> getValueAsList(int xpos, int ypos, LocalTime time){
        var tmp = sensorDataGenerator.generateData(xpos, ypos, time);
        var ret = new LinkedList<Byte>();
        for (byte b : tmp) {
            ret.add(b);
        }
        return ret;
    }

    public byte[] getValue(Pair<Integer, Integer> pos, LocalTime time){
        return getValue(pos.getLeft(), pos.getRight(), time);
    }

    public List<Byte> getValueAsList(Pair<Integer, Integer> pos, LocalTime time){
        return getValueAsList(pos.getLeft(), pos.getRight(), time);
    }

    public SensorDataGenerator getSensorDataGenerator() {
        return sensorDataGenerator;
    }

    public int getAmountOfData() {
        switch (this) {
            case SOOT:
            case IAQ:
            case OZONE:
            case CARBON_DIOXIDE:
            case PARTICULATE_MATTER:
                return 1;
            case GPS:
                return 8;
        }
        throw new IllegalStateException(
            String.format("Method 'getAmountOfData' unsupported for sensor type %s", this.toString())
        );
    }
}