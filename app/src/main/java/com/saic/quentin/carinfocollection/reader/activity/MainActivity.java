
package com.saic.quentin.carinfocollection.reader.activity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.*;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.saic.quentin.carinfocollection.commands.AmbientAirTemperatureObdCommand;
import com.saic.quentin.carinfocollection.commands.SpeedObdCommand;
import com.saic.quentin.carinfocollection.commands.EngineRPMObdCommand;
import com.saic.quentin.carinfocollection.enums.AvailableCommandNames;
import com.saic.quentin.carinfocollection.reader.IPostListener;
import com.saic.quentin.carinfocollection.R;
import com.saic.quentin.carinfocollection.reader.io.ObdCommandJob;
import com.saic.quentin.carinfocollection.reader.io.ObdGatewayService;
import com.saic.quentin.carinfocollection.reader.io.ObdGatewayServiceConnection;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Structure;

import static android.media.AudioTrack.MODE_STREAM;

/**
 * The main activity.
 */
public class MainActivity extends AppCompatActivity {

	private static final String TAG = "MainActivity";

	static final int NO_BLUETOOTH_ID = 0;
	static final int BLUETOOTH_DISABLED = 1;
	static final int NO_ORIENTATION_SENSOR = 8;

	private Handler mHandler = new Handler();

	/**
	 * Callback for ObdGatewayService to update UI.
	 */
	private IPostListener mListener = null;
	private Intent mServiceIntent = null;
	private ObdGatewayServiceConnection mServiceConnection = null;

	private SensorManager sensorManager = null;
	private Sensor orientSensor = null;
	private SharedPreferences prefs = null;

	private PowerManager powerManager = null;
	private PowerManager.WakeLock wakeLock = null;

	private boolean preRequisites = true;

	private int speed = 1;

	private AudioTrack player;
	private short[] buf;
	Data data;
	private ExecutorService mExecutorService;
	private int period = 1000;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		/**
		 * JNA
		 */
		mExecutorService = Executors.newSingleThreadExecutor();
		data = new Data();

		ToggleButton mToggleButton = (ToggleButton) findViewById(R.id.toggleButton);
		mToggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (!isChecked) {
					Toast.makeText(getApplication(), "Start", Toast.LENGTH_SHORT).show();
					startLiveData();
					startAudio();
				} else {
					Toast.makeText(getApplication(), "Stop", Toast.LENGTH_SHORT).show();
					stopAudio();
					stopLiveData();
				}
			}

//			}
		});


		Button button = (Button) findViewById(R.id.config);
		button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				updateConfig();
			}
		});


		/**
		 * Listen to the command and display the data
		 */
		mListener = new IPostListener() {
			public void stateUpdate(ObdCommandJob job) {
				String cmdName = job.getCommand().getName();
				String cmdResult = job.getCommand().getFormattedResult();

				if (AvailableCommandNames.ENGINE_RPM.getValue().equals(cmdName)) {
					TextView engine_rpm_value = (TextView) findViewById(R.id.engine_rpm_value);
					Log.d(TAG, cmdResult + "r/m");
					engine_rpm_value.setText(cmdResult);

					int s = Integer.valueOf(cmdResult.substring(0, cmdResult.length() - 4));
					genAudio(s);
				} else if (AvailableCommandNames.SPEED.getValue().equals(cmdName)) {
					TextView speed_value = (TextView) findViewById(R.id.speed_value);
					speed = ((SpeedObdCommand) job.getCommand())
							.getMetricSpeed();
					Log.d(TAG, cmdResult + " speed: " + speed);
					speed_value.setText(cmdResult);
				}
			}
		};

		/*
		 * Validate Bluetooth service.
		 */
		// Bluetooth device exists?
		final BluetoothAdapter mBtAdapter = BluetoothAdapter
				.getDefaultAdapter();
		if (mBtAdapter == null) {
			preRequisites = false;
			showDialog(NO_BLUETOOTH_ID);
		} else {
			if (!mBtAdapter.isEnabled()) {
				preRequisites = false;
				showDialog(BLUETOOTH_DISABLED);
			}
		}

		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		List<Sensor> sens = sensorManager
				.getSensorList(Sensor.TYPE_ORIENTATION);
		if (sens.size() <= 0) {
			showDialog(NO_ORIENTATION_SENSOR);
		} else {
			orientSensor = sens.get(0);
		}

		// validate app pre-requisites
		if (preRequisites) {
			/*
			 * Prepare service and its connection
			 */
			mServiceIntent = new Intent(this, ObdGatewayService.class);
			mServiceConnection = new ObdGatewayServiceConnection();
			mServiceConnection.setServiceListener(mListener);

			// bind service
			Log.d(TAG, "Binding service..");
			bindService(mServiceIntent, mServiceConnection,
					Context.BIND_AUTO_CREATE);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (player != null) {
			if (player.getState() == AudioTrack.PLAYSTATE_PLAYING) {
				player.stop();
			}
			if (player != null) {
				player.release();
			}
		}

		releaseWakeLockIfHeld();
		mServiceIntent = null;
		mServiceConnection = null;
		mListener = null;
		mHandler = null;

	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.d(TAG, "Pausing..");
		releaseWakeLockIfHeld();
	}

	/**
	 * If lock is held, release. Lock will be held when the service is running.
	 */
	private void releaseWakeLockIfHeld() {
		if (wakeLock.isHeld()) {
			wakeLock.release();
		}
	}

	protected void onResume() {
		super.onResume();

		Log.d(TAG, "Resuming..");

		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		// get period time
		period = Integer.valueOf(prefs.getString(ConfigActivity.UPDATE_PERIOD_KEY, "1000"));
		powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK,
				"ObdReader");
	}

	private void updateConfig() {
		Intent configIntent = new Intent(this, ConfigActivity.class);
		startActivity(configIntent);
	}

	private void startLiveData() {
		Log.d(TAG, "Starting live data..");

		if (!mServiceConnection.isRunning()) {
			Log.d(TAG, "Service is not running. Going to start it..");
			startService(mServiceIntent);
		}

		// start command execution
		mHandler.post(mQueueCommands);

		// screen won't turn off until wakeLock.release()
		wakeLock.acquire();
	}

	private void stopLiveData() {
		Log.d(TAG, "Stopping live data..");

		if (mServiceConnection.isRunning())
			stopService(mServiceIntent);

		// remove runnable
		mHandler.removeCallbacks(mQueueCommands);

		releaseWakeLockIfHeld();
	}

	/**
	 * Initial buffer
	 */
	private void startAudio() {
		Log.d("AudioPlayer", "Init audio processing...");
		JNATest.INSTANCE.signal_proc_init(data);
	}

	/**
	 * Release player
	 */
	private void stopAudio() {
		if (player != null && player.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
			Log.d("AudioPlayer", "Player is stopping...");
			player.stop();
			player.release();
		}
	}

	protected Dialog onCreateDialog(int id) {
		AlertDialog.Builder build = new AlertDialog.Builder(this);
		switch (id) {
			case NO_BLUETOOTH_ID:
				build.setMessage("Sorry, your device doesn't support Bluetooth.");
				return build.create();
			case BLUETOOTH_DISABLED:
				build.setMessage("You have Bluetooth disabled. Please enable it!");
				return build.create();
		}
		return null;
	}


	private Runnable mQueueCommands = new Runnable() {
		public void run() {
			if (mServiceConnection.isRunning())
				queueCommands();

			Log.d(TAG, "period is : " + period);
			// run again in 2s

			period = 100;

			mHandler.postDelayed(mQueueCommands, period);
		}
	};

	/**
	 * Add commands to the queue
	 */
	private void queueCommands() {
		final ObdCommandJob airTemp = new ObdCommandJob(new AmbientAirTemperatureObdCommand());
		final ObdCommandJob speed = new ObdCommandJob(new SpeedObdCommand());
		final ObdCommandJob rpm = new ObdCommandJob(new EngineRPMObdCommand());

		mServiceConnection.addJobToQueue(airTemp);
		mServiceConnection.addJobToQueue(speed);
		mServiceConnection.addJobToQueue(rpm);
	}

	/**
	 * Generate audio based on the RPM
	 */
	private void genAudio(float rpm) {
		data.carstatus.fRpm = rpm;
		Log.d(TAG, Float.toString(data.carstatus.fRpm));
		JNATest.INSTANCE.signal_proc(data);
		buf = data.buf_out.clone();
		Log.d("AudioPlayer", "Audio is generated.");
		mExecutorService.submit(new Runnable() {
			@Override
			public void run() {
				playAudio();
			}
		});
	}

	/**
	 * Initial audiotrack
	 */
	private void playAudio() {
		int sampleRateInHz = 48000;
		int channelConfig = AudioFormat.CHANNEL_OUT_STEREO;
		int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
		int minBufferSize = AudioTrack.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat);

		int length = minBufferSize + 1024;
		if (player == null) {
			Log.d("Audio", "Player is not ready.");
			if (Build.VERSION.SDK_INT >= 23) {

				player = new AudioTrack.Builder().
						setAudioFormat(new AudioFormat.Builder()
								.setEncoding(audioFormat)
								.setSampleRate(sampleRateInHz)
								.setChannelMask(channelConfig)
								.build())
						.setBufferSizeInBytes(length)
						.build();

				Log.d("Audio", "Now player is ready...");
			} else {
				player = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRateInHz,
						channelConfig, audioFormat,
						length, MODE_STREAM);

				Log.d("Audio", "Now player is ready...");
			}
		}


		Log.d("AudioPlayer", "Playing...");
		player.write(buf, 0, length);
		player.play();
	}


	interface JNATest extends Library {
		JNATest INSTANCE = (JNATest) Native.loadLibrary("audio", JNATest.class);

		public void signal_proc_init(Data data);

		public void signal_proc(Data pData);

	}

	/**
	 * The followings are JNA corresponding classes
	 */
	public static class CarStatus extends Structure {
		private static final List<String> FIELDS_ORDER = createFieldsOrder("fRpm", "nPedal", "nVelocity");
		public float fRpm;
		public int nPedal;
		public int nVelocity;

		public static class ByReference extends CarStatus implements Structure.ByReference {
		}

		public static class ByValue extends CarStatus implements Structure.ByValue {
		}

		@Override
		protected List<String> getFieldOrder() {
			return FIELDS_ORDER;
		}
	}

	public static class AudioPara extends Structure {
		private static final List<String> FIELDS_ORDER = createFieldsOrder("nNumbers", "pOrders", "pRpms",
				"pAmpVsRpms", "pInitPhases");
		public int[] nNumbers = new int[5];
		public float[] pOrders = new float[40];
		public float[] pRpms = new float[129];
		public float[] pAmpVsRpms = new float[40 * 129];
		public float[] pInitPhases = new float[40];

		public static class ByReference extends AudioPara implements Structure.ByReference {
		}

		public static class ByValue extends AudioPara implements Structure.ByValue {
		}

		@Override
		protected List<String> getFieldOrder() {
			return FIELDS_ORDER;
		}
	}

	public static class Data extends Structure {
		private static final List<String> FIELDS_ORDER = createFieldsOrder("audiopara", "carstatus", "buf_out");
		public AudioPara audiopara = new AudioPara();
		public CarStatus carstatus = new CarStatus();
		public short[] buf_out = new short[960 * 20];

		public static class ByReference extends AudioPara implements Structure.ByReference {
		}

		public static class ByValue extends AudioPara implements Structure.ByValue {
		}

		@Override
		protected List<String> getFieldOrder() {
			return FIELDS_ORDER;
		}
	}
}