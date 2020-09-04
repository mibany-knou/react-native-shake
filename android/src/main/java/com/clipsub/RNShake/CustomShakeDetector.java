package com.clipsub.RNShake;

import javax.annotation.Nullable;

import java.util.concurrent.TimeUnit;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import com.facebook.infer.annotation.Assertions;

/**
 * Listens for the user shaking their phone. Allocation-less once it starts listening.
 */
public class CustomShakeDetector implements SensorEventListener {

  public static int TYPE_MAX_SAMPLES = 1;
  public static int TYPE_MAGNITUDE_THRESHOLD = 2;
  public static int TYPE_PERCENT_OVER_THRESHOLD_FOR_SHAKE = 4;

  //only record and consider the last MAX_SAMPLES number of data points
  private static int MAX_SAMPLES = 40;
  //collect sensor data in this interval (nanoseconds)
  private static final long MIN_TIME_BETWEEN_SAMPLES_NS =
      TimeUnit.NANOSECONDS.convert(20, TimeUnit.MILLISECONDS);
  //expected duration of one shake in nanoseconds
  private static final long VISIBLE_TIME_RANGE_NS =
      TimeUnit.NANOSECONDS.convert(250, TimeUnit.MILLISECONDS);
  //minimum amount of force on accelerometer sensor to constitute a shake
  private static int MAGNITUDE_THRESHOLD = 25;
  //this percentage of data points must have at least the force of MAGNITUDE_THRESHOLD
  private static int PERCENT_OVER_THRESHOLD_FOR_SHAKE = 60;
  //number of nanoseconds to listen for and count shakes
  private static final float SHAKING_WINDOW_NS =
      TimeUnit.NANOSECONDS.convert(3, TimeUnit.SECONDS);

  public static interface ShakeListener {
    void onShake();
  }

  private final ShakeListener mShakeListener;

  @Nullable private SensorManager mSensorManager;
  private long mLastTimestamp;
  private int mCurrentIndex;
  private int mNumShakes;
  private long mLastShakeTimestamp;
  @Nullable private double[] mMagnitudes;
  @Nullable private long[] mTimestamps;
  //number of shakes required to trigger onShake()
  private int mMinNumShakes;

  public CustomShakeDetector(ShakeListener listener) {
    this(listener, 1);
  }

  public CustomShakeDetector(ShakeListener listener, int minNumShakes) {
    mShakeListener = listener;
    mMinNumShakes = minNumShakes;
  }

  /**
   * Start listening for shakes.
   */
  public void start(SensorManager manager) {
    Assertions.assertNotNull(manager);
    Sensor accelerometer = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    if (accelerometer != null) {
      mSensorManager = manager;
      mLastTimestamp = -1;
      mCurrentIndex = 0;
      mMagnitudes = new double[MAX_SAMPLES];
      mTimestamps = new long[MAX_SAMPLES];
      mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
      mNumShakes = 0;
      mLastShakeTimestamp = 0;
    }
  }

  public void config(int type, int value) {
    if(TYPE_MAX_SAMPLES == type) MAX_SAMPLES = value;
    else if(TYPE_MAGNITUDE_THRESHOLD == type) MAGNITUDE_THRESHOLD = value;
    else if(TYPE_PERCENT_OVER_THRESHOLD_FOR_SHAKE == type) PERCENT_OVER_THRESHOLD_FOR_SHAKE = value;
  }

  /**
   * Stop listening for shakes.
   */
  public void stop() {
    if (mSensorManager != null) {
      mSensorManager.unregisterListener(this);
      mSensorManager = null;
    }
  }

  @Override
  public void onSensorChanged(SensorEvent sensorEvent) {
    if (sensorEvent.timestamp - mLastTimestamp < MIN_TIME_BETWEEN_SAMPLES_NS) {
      return;
    }

    Assertions.assertNotNull(mTimestamps);
    Assertions.assertNotNull(mMagnitudes);

    float ax = sensorEvent.values[0];
    float ay = sensorEvent.values[1];
    float az = sensorEvent.values[2];

    mLastTimestamp = sensorEvent.timestamp;
    mTimestamps[mCurrentIndex] = sensorEvent.timestamp;
    mMagnitudes[mCurrentIndex] = Math.sqrt(ax * ax + ay * ay + az * az);

    maybeDispatchShake(sensorEvent.timestamp);

    mCurrentIndex = (mCurrentIndex + 1) % MAX_SAMPLES;
  }

  @Override
  public void onAccuracyChanged(Sensor sensor, int i) {
  }

  private void maybeDispatchShake(long currentTimestamp) {
    Assertions.assertNotNull(mTimestamps);
    Assertions.assertNotNull(mMagnitudes);

    int numOverThreshold = 0;
    int total = 0;
    for (int i = 0; i < MAX_SAMPLES; i++) {
      int index = (mCurrentIndex - i + MAX_SAMPLES) % MAX_SAMPLES;
      if (currentTimestamp - mTimestamps[index] < VISIBLE_TIME_RANGE_NS) {
        total++;
        if (mMagnitudes[index] >= MAGNITUDE_THRESHOLD) {
          numOverThreshold++;
        }
      }
    }
    if (((double) numOverThreshold) / total > PERCENT_OVER_THRESHOLD_FOR_SHAKE / 100.0) {
      if (currentTimestamp - mLastShakeTimestamp >= VISIBLE_TIME_RANGE_NS) {
        mNumShakes++;
      }
      mLastShakeTimestamp = currentTimestamp;
      if (mNumShakes >= mMinNumShakes) {
        mNumShakes = 0;
        mLastShakeTimestamp = 0;
        mShakeListener.onShake();
      }
    }
    if (currentTimestamp - mLastShakeTimestamp > SHAKING_WINDOW_NS) {
      mNumShakes = 0;
      mLastShakeTimestamp = 0;
    }
  }
}
