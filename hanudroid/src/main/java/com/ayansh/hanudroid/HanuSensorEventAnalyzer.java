/**
 * 
 */
package com.ayansh.hanudroid;

import java.util.Date;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * @author Varun Verma
 *
 */
public class HanuSensorEventAnalyzer implements SensorEventListener {
	
	private HanuSensorEventListener caller;
	private long lastUpdate;
	
	public HanuSensorEventAnalyzer(HanuSensorEventListener caller){
		this.caller = caller;
		lastUpdate = System.currentTimeMillis();
	}

	/* (non-Javadoc)
	 * @see android.hardware.SensorEventListener#onAccuracyChanged(android.hardware.Sensor, int)
	 */
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// I don't know what to do !

	}

	/* (non-Javadoc)
	 * @see android.hardware.SensorEventListener#onSensorChanged(android.hardware.SensorEvent)
	 */
	@Override
	public void onSensorChanged(SensorEvent event) {
		// Sensor has sent its data. Analyze this.
		
		if(event.sensor.getType() != Sensor.TYPE_ACCELEROMETER){
			return;
		}
		
		long actualTime = System.currentTimeMillis();
		
		Date now = new Date();
		Date old = new Date(lastUpdate);
		
		if(now.getTime() - old.getTime() < 4500){
			
		}
		
		if(actualTime - lastUpdate < 4500){
			return;
		}
		
		float[] values = event.values;
	    // Movement
	    float x = values[0];
	    float y = values[1];
	    float z = values[2];

	    double threshold = 50 * SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH;
	    
	    double shakeLevel = x*x + y*y + z*z;
	    
	    if (shakeLevel >= threshold) //
	    {
	      if (actualTime - lastUpdate < 3500) {
	        return;
	      }
	      
	      lastUpdate = actualTime;
	      
	      // This is a valid shake
	      caller.shakeDetected();
	      
	    }
	}

}