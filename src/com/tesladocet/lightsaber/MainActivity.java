package com.tesladocet.lightsaber;

import java.io.File;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.media.SoundPool.OnLoadCompleteListener;
import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Button;

public class MainActivity extends Activity implements OnTouchListener {

	private final static int ON = 0;
	private final static int HUM = 1;
	private final static int HIT = 2;
	private final static int OFF = 3;
	private final static int SWING = 4;
	
	private final static int SWING_DUR = 700; // 983;
	
	SoundPool pool;
	Button sword;
	int[] sounds = new int[5]; // used by play
	int[] streams = new int[5]; // used by stop and returned by play
	boolean maskStop = false;
	
	Random rand = new Random();
	ScheduledExecutorService scheduler;
	
	private SensorManager mSensorManager;
	private Sensor mGyro;
	private Sensor mAcc;
	private float maxGyro;
	private float maxAcc;
	
	private long startSwing;
	private boolean scanning = false;
	
	boolean iamajedi = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        pool = new SoundPool(4, AudioManager.STREAM_MUSIC, 0);

        pool.setOnLoadCompleteListener(new OnLoadCompleteListener() {
			@Override
			public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
				Log.d("", "loaded "+sampleId);
			}
		});
                
        sounds[0] = pool.load(this,R.raw.on,1);
        sounds[1] = pool.load(this,R.raw.hum,1);
        sounds[2] = pool.load(this,R.raw.hit,1);
        sounds[3] = pool.load(this,R.raw.off,1);
        sounds[4] = pool.load(this,R.raw.swing,1);
        
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mGyro = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mAcc = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        maxGyro = mGyro.getMaximumRange();
        maxAcc = mAcc.getMaximumRange();
        
        File path = Environment.getExternalStoragePublicDirectory(
        		Environment.DIRECTORY_DOWNLOADS);
        File file = new File(path, "iamajedi");
        iamajedi = file.exists();
        
        sword = (Button) findViewById(R.id.sword);
        sword.setOnTouchListener(this);
    }

    @Override
    protected void onResume() {
    	super.onResume();
    	mSensorManager.registerListener(mGyroListener, mGyro, 
    			SensorManager.SENSOR_DELAY_GAME);
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    	stopAll();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if (v.getId() == R.id.sword) {
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				maskStop = false;
				scanning = true;
				stopAll();
				play(ON, 0);
				play(HUM, -1);
				//scheduler = Executors.newSingleThreadScheduledExecutor();
				//scheduler.schedule(timeout, rand.nextInt(3)+1, TimeUnit.SECONDS);
				//scheduler.schedule(read, rand.nextInt(3)+1, TimeUnit.SECONDS);
				return true;
				
			case MotionEvent.ACTION_UP:
				if (maskStop) return true;
				scanning = false;
				//scheduler.shutdownNow();
				stopAll();
				play(OFF,0);
				return true;
			}
		}
		return false;
	}
	
	private SensorEventListener mGyroListener = new SensorEventListener() {
        private boolean mHitStarted = false;

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}

        @Override
        public void onSensorChanged(SensorEvent event) {

            // Rotational speed
            float[] values = event.values;

            if (scanning && 
            		(System.currentTimeMillis()-startSwing) > SWING_DUR &&
            		(Math.abs(values[0]) + Math.abs(values[1]) + Math.abs(values[2])) > 1.5) {
            	stop(SWING);
                play(SWING, 0);
                startSwing = System.currentTimeMillis();
            }
        }
    };
	
	private void play(int id, int loop) {
		streams[id] = pool.play(sounds[id], 1, 1, 0, loop, 1);
		Log.d("","id: "+id+" streamid: "+sounds[id]);
	}
	
	private void stop(int id) {
		pool.stop(streams[id]);
	}

	private void stopAll() {
		for (int i=0;i<sounds.length;i++) {
			stop(i);
		}
	}
	
	Runnable read = new Runnable() {
		public void run() {
			maskStop = true;
			scanning = false;
			scheduler.shutdownNow();
			stopAll();
			play(HIT,0);
		}
	};
	
	Runnable timeout = new Runnable() {
		public void run() {
			maskStop = true;
			scanning = false;
			scheduler.shutdownNow();
			stopAll();
			play(OFF,0);
		}
	};
}