package core;

import com.virtenio.driver.device.ADXL345;
import com.virtenio.driver.gpio.GPIO;
import com.virtenio.driver.gpio.NativeGPIO;
import com.virtenio.driver.spi.NativeSPI;

public class Accelerometer {
	private ADXL345 sensor;
	private GPIO accelCs;
	
	public void init() throws Exception{ 
	
	// init ADXL345
	
	//GPIO Init
	accelCs = NativeGPIO.getInstance(20);
	
	//SPI Init
	NativeSPI spi = NativeSPI.getInstance(0);
	spi.open(ADXL345.SPI_MODE, ADXL345.SPI_BIT_ORDER, ADXL345.SPI_MAX_SPEED);
	
	//ADXL Init	
	sensor = new ADXL345(spi, accelCs);

	sensor.open();
	sensor.setDataFormat(ADXL345.DATA_FORMAT_RANGE_2G);
	sensor.setDataRate(ADXL345.DATA_RATE_3200HZ);
	sensor.setPowerControl(ADXL345.POWER_CONTROL_MEASURE);
	
	System.out.println("Done Init");
	}
	
	public float[] sense() throws Exception {
		short[] temp = new short[3];
		float[] result = new float[3];
		sensor.getValuesRaw(temp, 0);
		sensor.convertRaw(temp, 0, result, 0);
		return result;
	}
}
	
