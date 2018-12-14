/*
 * TODO put header
 */
package com.saic.quentin.carinfocollection.reader.activity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.*;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.*;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.*;
import com.example.MyCommand;
import com.saic.quentin.carinfocollection.commands.AmbientAirTemperatureObdCommand;
import com.saic.quentin.carinfocollection.commands.SpeedObdCommand;
import com.saic.quentin.carinfocollection.commands.control.CommandEquivRatioObdCommand;
import com.saic.quentin.carinfocollection.commands.engine.EngineRPMObdCommand;
import com.saic.quentin.carinfocollection.commands.engine.MassAirFlowObdCommand;
//import com.saic.quentin.carinfocollection.commands.fuel.FuelEconomyObdCommand;
//import com.saic.quentin.carinfocollection.commands.fuel.FuelEconomyWithMAFObdCommand;
//import com.saic.quentin.carinfocollection.commands.fuel.FuelLevelObdCommand;
//import com.saic.quentin.carinfocollection.commands.fuel.FuelTrimObdCommand;
//import com.saic.quentin.carinfocollection.commands.temperature.AmbientAirTemperatureObdCommand;
import com.saic.quentin.carinfocollection.enums.AvailableCommandNames;
//import com.saic.quentin.carinfocollection.enums.FuelTrim;
//import com.saic.quentin.carinfocollection.enums.FuelType;
import com.saic.quentin.carinfocollection.reader.IPostListener;
import com.saic.quentin.carinfocollection.R;
import com.saic.quentin.carinfocollection.reader.io.ObdCommandJob;
import com.saic.quentin.carinfocollection.reader.io.ObdGatewayService;
import com.saic.quentin.carinfocollection.reader.io.ObdGatewayServiceConnection;
import android.support.v7.widget.Toolbar;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Structure;
import org.w3c.dom.Text;

import static android.media.AudioTrack.MODE_STREAM;

/**
 * The main activity.
 */
public class MainActivity extends AppCompatActivity {

	private static final String TAG = "MainActivity";

	/*
	 * TODO put description
	 */
	static final int NO_BLUETOOTH_ID = 0;
	static final int BLUETOOTH_DISABLED = 1;
	//	static final int NO_GPS_ID = 2;
	static final int START_LIVE_DATA = 3;
	static final int STOP_LIVE_DATA = 4;
	static final int SETTINGS = 5;
	static final int COMMAND_ACTIVITY = 6;
	static final int TABLE_ROW_MARGIN = 7;
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
	private double maf = 1;
	private float ltft = 0;
	private double equivRatio = 1;

	//    private EditText commandText;
	private TextView resultText;
//    private Button sendButton;

	private AudioTrack player;
	private short[] buf;
	Data data;
	private ExecutorService mExecutorService;
	private int period = 1000;


	public void updateTextView(final TextView view, final String txt) {
		new Handler().post(new Runnable() {
			public void run() {
				view.setText(txt);
			}
		});
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		/*
		 * TODO clean-up this upload thing
		 *
		 * ExceptionHandler.register(this,
		 * "http://www.whidbeycleaning.com/droid/server.php");
		 */
		setContentView(R.layout.main);
//		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

		// JNA
		mExecutorService = Executors.newSingleThreadExecutor();
		data = new Data();

		ToggleButton mToggleButton = (ToggleButton) findViewById(R.id.toggleButton);
		mToggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//				if(!mServiceConnection.isRunning()) {
//					Toast.makeText(getApplication(), "Service is not running", Toast.LENGTH_LONG).show();
//					startService(mServiceIntent);
//				} else {
				if (!isChecked) {
					Toast.makeText(getApplication(), "Start", Toast.LENGTH_SHORT).show();
//					startLiveData();
					startAudio();

					for (int i = 0; i < 200; ++i) {
						int tempRpm = 5 * i + 4000;
//						int tempRpm = (int) (Math.random() * 6000 + 2000);
						// 0~2000
//						int tempRpm = (int) (Math.random() * 2000 + 0);
						// 2001~4000
//						int tempRpm = (int) (Math.random() * 2000 + 2000);
						// 4001~6000
//						int tempRpm = (int) (Math.random() * 2000 + 4000);
						//6001~8000
//						int tempRpm = (int) (Math.random() * 2000 + 5800);
						genAudio(tempRpm);
					}
					stopAudio();
//					isChecked = false;
				} else {
					Toast.makeText(getApplication(), "Stop", Toast.LENGTH_SHORT).show();
					stopAudio();
//					stopLiveData();
				}

//				if (isChecked) {
//					isChecked = false;
//				} else {
//					isChecked = true;
//				}
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




//		setSupportActionBar(toolbar);

//        commandText=(EditText)this.findViewById(R.id.commandText);
//        resultText=(TextView)this.findViewById(R.id.resultText);
//        sendButton=(Button)this.findViewById(R.id.sendButton);

//        sendButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                final MyCommand command=new MyCommand(commandText.getText().toString());
//                mServiceConnection.addJobToQueue(new ObdCommandJob(command));
//
//                mHandler.postDelayed(new Runnable() {
//                    @Override
//                    public void run() {
//                        resultText.setText(command.getName()+">>"+command.getResult());
//                    }
//                },2000);
//            }
//        });


		mListener = new IPostListener() {
			public void stateUpdate(ObdCommandJob job) {
				String cmdName = job.getCommand().getName();
				String cmdResult = job.getCommand().getFormattedResult();

//				Log.d(TAG, FuelTrim.LONG_TERM_BANK_1.getBank() + " equals " + cmdName + "?");

				if (AvailableCommandNames.ENGINE_RPM.getValue().equals(cmdName)) {
//					TextView tvRpm = (TextView) findViewById(R.id.rpm_text);
//					tvRpm.setText(cmdResult);
					TextView engine_rpm_value = (TextView) findViewById(R.id.engine_rpm_value);
					Log.d(TAG, cmdResult + "r/m");
					engine_rpm_value.setText(cmdResult);

					int s = Integer.valueOf(cmdResult.substring(0, cmdResult.length() - 4));
					genAudio(s);
				} else if (AvailableCommandNames.SPEED.getValue().equals(
						cmdName)) {
//					TextView tvSpeed = (TextView) findViewById(R.id.spd_text);
//					tvSpeed.setText(cmdResult);
					TextView speed_value = (TextView) findViewById(R.id.speed_value);
					speed = ((SpeedObdCommand) job.getCommand())
							.getMetricSpeed();
					Log.d(TAG, cmdResult + " speed: " + speed);
					speed_value.setText(cmdResult);
//				} else if (AvailableCommandNames.MAF.getValue().equals(cmdName)) {
//					maf = ((MassAirFlowObdCommand) job.getCommand()).getMAF();
//					addTableRow(cmdName, cmdResult);
//				} else if (FuelTrim.LONG_TERM_BANK_1.getBank().equals(cmdName)) {
//					ltft = ((FuelTrimObdCommand) job.getCommand()).getValue();
//				} else if (AvailableCommandNames.EQUIV_RATIO.getValue().equals(cmdName)) {
//					equivRatio = ((CommandEquivRatioObdCommand) job.getCommand()).getRatio();
//					addTableRow(cmdName, cmdResult);
//				} else {
//					addTableRow(cmdName, cmdResult);
				}
			}
		};

		/*
		 * Validate GPS service.
		 */
//		LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
//		if (locationManager.getProvider(LocationManager.GPS_PROVIDER) == null) {
//			/*
//			 * TODO for testing purposes we'll not make GPS a pre-requisite.
//			 */
//			// preRequisites = false;
//			showDialog(NO_GPS_ID);
//		}

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
			// Bluetooth device is enabled?
			if (!mBtAdapter.isEnabled()) {
				preRequisites = false;
				showDialog(BLUETOOTH_DISABLED);
			}
		}

		/*
		 * Get Orientation sensor.
		 */
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


//			startService(mServiceIntent);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

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

//		sensorManager.registerListener(orientListener, orientSensor,
//				SensorManager.SENSOR_DELAY_UI);
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		// TODO
		period = Integer.valueOf(prefs.getString(ConfigActivity.UPDATE_PERIOD_KEY, "1000"));
		powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK,
				"ObdReader");

		// test test
//		startLiveData();
	}

	private void updateConfig() {
		Intent configIntent = new Intent(this, ConfigActivity.class);
		startActivity(configIntent);
	}


//	public boolean onCreateOptionsMenu(Menu menu) {
//		getMenuInflater().inflate(R.menu.toolbar, menu);
//		menu.add(0, START_LIVE_DATA, 0, "Start Live Data");
//		menu.add(0, COMMAND_ACTIVITY, 0, "Run Command");
//		menu.add(0, STOP_LIVE_DATA, 0, "Stop");
//		menu.add(0, SETTINGS, 0, "Settings");
//		return true;
//	}

//	public boolean onOptionsItemSelected(MenuItem item) {
//		switch (item.getItemId()) {
//			case R.id.start_live_data:
//				startLiveData();
//				return true;
//			case R.id.stop:
//				stopLiveData();
//				return true;
//			case R.id.settings:
//				updateConfig();
//				return true;
//		case START_LIVE_DATA:
//			startLiveData();
//			return true;
//		case STOP_LIVE_DATA:
//			stopLiveData();
//			return true;
//		case SETTINGS:
//			updateConfig();
//			return true;
	// case COMMAND_ACTIVITY:
	// staticCommand();
	// return true;
//		}
//		return false;
//	}

	// private void staticCommand() {
	// Intent commandIntent = new Intent(this, ObdReaderCommandActivity.class);
	// startActivity(commandIntent);
	// }

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

	private void startAudio() {
		Log.d("AudioPlayer", "Init audio processing...");

		JNATest.INSTANCE.signal_proc_init(data);

//		for (int i = 0; i < 100; i++) {
//			data.carstatus.fRpm = (int) (Math.random() * 6000 + 2000);
//			System.out.println(data.carstatus.fRpm);
//			JNATest.INSTANCE.signal_proc(data);
//			buf = data.buf_out.clone();
////						Message msg = Message.obtain(mHandler);
////						msg.obj = data.carstatus.fRpm;
////						mHandler.sendMessage(msg);
//			Log.d("AudioPlayer", "Start to play...");
//			mExecutorService.submit(new Runnable() {
//				@Override
//				public void run() {
//					playAudio();
//				}
//			});
//		}

//		mExecutorService.submit(new Runnable() {
//			@Override
//			public void run() {
//				playAudio();
//			}
//		});

	}

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
//		case NO_GPS_ID:
//			build.setMessage("Sorry, your device doesn't support GPS.");e
//			return build.create();
			case NO_ORIENTATION_SENSOR:
				build.setMessage("Orientation sensor missing?");
				return build.create();
		}
		return null;
	}


	/**
	 *
	 */
	private Runnable mQueueCommands = new Runnable() {
		public void run() {
			/*
			 * If values are not default, then we have values to calculate MPG
			 */
//			Log.d(TAG, "SPD:" + speed + ", MAF:" + maf + ", LTFT:" + ltft);
//			if (speed > 1 && maf > 1 && ltft != 0) {
//				FuelEconomyWithMAFObdCommand fuelEconCmd = new FuelEconomyWithMAFObdCommand(
//						FuelType.DIESEL, speed, maf, ltft, false /* TODO */);
//				TextView tvMpg = (TextView) findViewById(R.id.fuel_econ_text);
//				tvMpg.setText("mQueueCommands");
//				String liters100km = String.format("%.2f", fuelEconCmd.getLitersPer100Km());
//				tvMpg.setText("" + liters100km);
////				Log.d(TAG, "FUELECON:" + liters100km);
//			}

			if (mServiceConnection.isRunning())
				queueCommands();

			Log.d(TAG, "period is : " + period);
			// run again in 2s

			period = 100;

			mHandler.postDelayed(mQueueCommands, period);
		}
	};

	/**
	 *
	 */
	private void queueCommands() {
		final ObdCommandJob airTemp = new ObdCommandJob(
				new AmbientAirTemperatureObdCommand());
		final ObdCommandJob speed = new ObdCommandJob(new SpeedObdCommand());
		final ObdCommandJob rpm = new ObdCommandJob(new EngineRPMObdCommand());

		mServiceConnection.addJobToQueue(airTemp);
		mServiceConnection.addJobToQueue(speed);
		mServiceConnection.addJobToQueue(rpm);
	}


	private void genAudio(float rpm) {
		data.carstatus.fRpm = rpm;
		Log.d(TAG, Float.toString(data.carstatus.fRpm));
		JNATest.INSTANCE.signal_proc(data);
		buf = data.buf_out.clone();
//						Message msg = Message.obtain(mHandler);
//						msg.obj = data.carstatus.fRpm;
//						mHandler.sendMessage(msg);
		Log.d("AudioPlayer", "Audio is generated.");
		mExecutorService.submit(new Runnable() {
			@Override
			public void run() {
				playAudio();
			}
		});
	}

	//TODO player是否要每次都Play（）
	private void playAudio() {
		int sampleRateInHz = 48000;
		int channelConfig = AudioFormat.CHANNEL_OUT_STEREO;
		int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
		int minBufferSize = AudioTrack.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat);

		//实测length参数很重要，太大或者大小都有可能导致异常：play() called on uninitialized AudioTrack
//            int length = (int) audioFile.length();
		int length = minBufferSize + 1024;
//            byte bytes[] = new byte[length];
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
//        data.write();


		Log.d("AudioPlayer", "Playing...");
		player.write(buf, 0, length);
		player.play();
//		player.setPlaybackRate(60);

//        }
	}

	interface JNATest extends Library {
		JNATest INSTANCE = (JNATest) Native.loadLibrary("audio", JNATest.class);

		//        public Pointer GenerateAudioStereo(float fRpms, Pointer p);
		public void signal_proc_init(Data data);
		public void signal_proc(Data pData);

	}

	public static class CarStatus extends Structure {
		private static final List<String> FIELDS_ORDER = createFieldsOrder("fRpm", "nPedal", "nVelocity");
		public float fRpm;
		public int nPedal;
		public int nVelocity;
		public static class ByReference extends CarStatus implements Structure.ByReference {}
		public static class ByValue extends CarStatus implements Structure.ByValue {}

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
		public static class ByReference extends AudioPara implements Structure.ByReference {}
		public static class ByValue extends AudioPara implements Structure.ByValue {}

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
		public static class ByReference extends AudioPara implements Structure.ByReference {}
		public static class ByValue extends AudioPara implements Structure.ByValue {}

		@Override
		protected List<String> getFieldOrder() {
			return FIELDS_ORDER;
		}
	}

	//	public boolean onPrepareOptionsMenu(Menu menu) {
//		MenuItem startItem = menu.findItem(START_LIVE_DATA);
//		MenuItem stopItem = menu.findItem(STOP_LIVE_DATA);
//		MenuItem settingsItem = menu.findItem(SETTINGS);
//		MenuItem commandItem = menu.findItem(COMMAND_ACTIVITY);
//
//		// validate if preRequisites are satisfied.
//		if (preRequisites) {
//			if (mServiceConnection.isRunning()) {
//				startItem.setEnabled(false);
//				stopItem.setEnabled(true);
//				settingsItem.setEnabled(false);
//				commandItem.setEnabled(false);
//			} else {
//				stopItem.setEnabled(false);
//				startItem.setEnabled(true);
//				settingsItem.setEnabled(true);
//				commandItem.setEnabled(false);
//			}
//		} else {
//			startItem.setEnabled(false);
//			stopItem.setEnabled(false);
//			settingsItem.setEnabled(false);
//			commandItem.setEnabled(false);
//		}
//
//		return true;
//	}

//	private void addTableRow(String key, String val) {
//		TableLayout tl = (TableLayout) findViewById(R.id.data_table);
//		TableRow tr = new TableRow(this);
//		MarginLayoutParams params = new MarginLayoutParams(
//				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
//		params.setMargins(TABLE_ROW_MARGIN, TABLE_ROW_MARGIN, TABLE_ROW_MARGIN,
//				TABLE_ROW_MARGIN);
//		tr.setLayoutParams(params);
//		tr.setBackgroundColor(Color.BLACK);
//		TextView name = new TextView(this);
//		name.setGravity(Gravity.RIGHT);
//		name.setText(key + ": ");
//		TextView value = new TextView(this);
//		value.setGravity(Gravity.LEFT);
//		value.setText(val);
//		tr.addView(name);
//		tr.addView(value);
//		tl.addView(tr, new TableLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
//				LayoutParams.WRAP_CONTENT));
//
//		/*
//		 * TODO remove this hack
//		 *
//		 * let's define a limit number of rows
//		 */
//		if (tl.getChildCount() > 10)
//			tl.removeViewAt(0);
//	}
}