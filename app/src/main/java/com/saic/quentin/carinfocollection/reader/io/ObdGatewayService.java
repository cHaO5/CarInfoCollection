/*
 * TODO put header
 */
package com.saic.quentin.carinfocollection.reader.io;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
import com.saic.quentin.carinfocollection.commands.AmbientAirTemperatureObdCommand;
import com.saic.quentin.carinfocollection.commands.ObdCommand;
import com.saic.quentin.carinfocollection.commands.protocol.EchoOffObdCommand;
import com.saic.quentin.carinfocollection.commands.protocol.LineFeedOffObdCommand;
import com.saic.quentin.carinfocollection.commands.protocol.ObdResetCommand;
import com.saic.quentin.carinfocollection.commands.protocol.SelectProtocolObdCommand;
import com.saic.quentin.carinfocollection.commands.protocol.TimeoutObdCommand;
//import com.saic.quentin.carinfocollection.commands.temperature.AmbientAirTemperatureObdCommand;
import com.saic.quentin.carinfocollection.enums.ObdProtocols;
import com.saic.quentin.carinfocollection.reader.IPostListener;
import com.saic.quentin.carinfocollection.reader.IPostMonitor;
import com.saic.quentin.carinfocollection.R;
import com.saic.quentin.carinfocollection.reader.activity.ConfigActivity;
import com.saic.quentin.carinfocollection.reader.activity.MainActivity;
import com.saic.quentin.carinfocollection.reader.io.ObdCommandJob.ObdCommandJobState;

/**
 * This service is primarily responsible for establishing and maintaining a
 * permanent connection between the device where the application runs and a more
 * OBD Bluetooth interface.
 * 
 * Secondarily, it will serve as a repository of ObdCommandJobs and at the same
 * time the application state-machine.
 */
public class ObdGatewayService extends Service {

	private static final String TAG = "ObdGatewayService";

	private IPostListener _callback = null;
	private final Binder _binder = new LocalBinder();
	private AtomicBoolean _isRunning = new AtomicBoolean(false);
//	private NotificationManager _notifManager;

	private BlockingQueue<ObdCommandJob> _queue = new LinkedBlockingQueue<ObdCommandJob>();
	private AtomicBoolean _isQueueRunning = new AtomicBoolean(false);
	private Long _queueCounter = 0L;

	private BluetoothDevice _dev = null;
	private BluetoothSocket _sock = null;
	/*
	 * http://developer.android.com/reference/android/bluetooth/BluetoothDevice.html
	 * #createRfcommSocketToServiceRecord(java.util.UUID)
	 * 
	 * "Hint: If you are connecting to a Bluetooth serial board then try using
	 * the well-known SPP UUID 00001101-0000-1000-8000-00805F9B34FB. However if
	 * you are connecting to an Android peer then please generate your own
	 * unique UUID."
	 */
	private static final UUID MY_UUID = UUID
	        .fromString("00001101-0000-1000-8000-00805F9B34FB");

	/**
	 * As long as the service is bound to another component, say an Activity, it
	 * will remain alive.
	 */
	@Override
	public IBinder onBind(Intent intent) {
		return _binder;
	}

	@Override
	public void onCreate() {
//		_notifManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
//		showNotification();
		Log.d(TAG, "Service create");
	}

	@Override
	public void onDestroy() {
		stopService();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG, "Received start id " + startId + ": " + intent);

		/*
		 * Register listener Start OBD connection
		 */
		startService();

		/*
		 * We want this service to continue running until it is explicitly
		 * stopped, so return sticky.
		 */
		return START_STICKY;
	}

	private void startService() {
		Log.d(TAG, "Service start...");

		/*
		 * Retrieve preferences
		 */
		SharedPreferences prefs = PreferenceManager
		        .getDefaultSharedPreferences(this);

		/*
		 * Let's get the remote Bluetooth device
		 */
		String remoteDevice = prefs.getString(
		        ConfigActivity.BLUETOOTH_LIST_KEY, null);
		if (remoteDevice == null || "".equals(remoteDevice)) {
			Toast.makeText(this, "No Bluetooth device selected",
			        Toast.LENGTH_LONG).show();

			// log error
			Log.e(TAG, "No Bluetooth device selected");

			// TODO kill this service gracefully
			stopService();
		}

		final BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
		_dev = btAdapter.getRemoteDevice(remoteDevice);

		/*
		 * TODO put this as deprecated Determine if upload is enabled
		 */
		// boolean uploadEnabled = prefs.getBoolean(
		// ConfigActivity.UPLOAD_DATA_KEY, false);
		// String uploadUrl = null;
		// if (uploadEnabled) {
		// uploadUrl = prefs.getString(ConfigActivity.UPLOAD_URL_KEY,
		// null);
		// }

		/*
		 * Get GPS
		 */
//		LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
//		boolean gps = prefs.getBoolean(ConfigActivity.ENABLE_GPS_KEY, false);

		/*
		 * TODO 采样间隔未使用
		 * 
		 * Get more preferences
		 */
		int period = ConfigActivity.getUpdatePeriod(prefs);
//		double ve = ConfigActivity.getVolumetricEfficieny(prefs);
//		double ed = ConfigActivity.getEngineDisplacement(prefs);
		boolean imperialUnits = prefs.getBoolean(
		        ConfigActivity.IMPERIAL_UNITS_KEY, false);
		ArrayList<ObdCommand> cmds = ConfigActivity.getObdCommands(prefs);

		/*
		 * Establish Bluetooth connection
		 * 
		 * Because discovery is a heavyweight procedure for the Bluetooth
		 * adapter, this method should always be called before attempting to
		 * connect to a remote device with connect(). Discovery is not managed
		 * by the Activity, but is run as a system service, so an application
		 * should always call cancel discovery even if it did not directly
		 * request a discovery, just to be sure. If Bluetooth state is not
		 * STATE_ON, this API will return false.
		 * 
		 * see
		 * http://developer.android.com/reference/android/bluetooth/BluetoothAdapter
		 * .html#cancelDiscovery()
		 */
		Log.d(TAG, "Stop searching bth services");
		btAdapter.cancelDiscovery();

		Toast.makeText(this, "Starting OBD connection..", Toast.LENGTH_SHORT);

		try {
			startObdConnection();
		} catch (Exception e) {
			Log.e(TAG, "Errors when connecting -> "
			        + e.getMessage());

			// in case of failure, stop this service.
			stopService();
		}
	}

	/**
	 * Start and configure the connection to the OBD interface.
	 * 
	 * @throws java.io.IOException
	 */
	private void startObdConnection() throws IOException {
		Log.d(TAG, "Start OBD connection...");

		// Instantiate a BluetoothSocket for the remote device and connect it.
//		_sock = _dev.createRfcommSocketToServiceRecord(MY_UUID);
		int sdk = Build.VERSION.SDK_INT;

		if (sdk >= 10) {
			_sock = _dev.createInsecureRfcommSocketToServiceRecord(MY_UUID);
		} else {
			_sock = _dev.createRfcommSocketToServiceRecord(MY_UUID);
		}

//		try {
//			_sock.connect();
//			Log.e("","Connected");
//		} catch (IOException e) {
//			Log.e("",e.getMessage());
//			try {
//				Log.e("","trying fallback...");
//
//				_sock =(BluetoothSocket) _dev.getClass().getMethod("createRfcommSocket", new Class[] {int.class}).invoke(_dev,1);
//				_sock.connect();
//
//				Log.e("","Connected");
//			}
//			catch (Exception e2) {
//				Log.e("", "Couldn't establish Bluetooth connection!");
//			}
//		}

//		BluetoothSocket sockFallback = null;
//		try {
//			_sock = _dev.createRfcommSocketToServiceRecord(MY_UUID);
//			_sock.connect();
//		} catch (Exception e1) {
//			Log.e(TAG, "There was an error while establishing Bluetooth connection. Falling back..", e1);
//			Class<?> clazz = _sock.getRemoteDevice().getClass();
//			Class<?>[] paramTypes = new Class<?>[]{Integer.TYPE};
//			try {
//				Method m = clazz.getMethod("createRfcommSocket", paramTypes);
//				Object[] params = new Object[]{Integer.valueOf(1)};
//				sockFallback = (BluetoothSocket) m.invoke(_sock.getRemoteDevice(), params);
//				sockFallback.connect();
//				_sock = sockFallback;
//			} catch (Exception e2) {
//				Log.e(TAG, "Couldn't fallback while establishing Bluetooth connection.", e2);
//				throw new IOException(e2.getMessage());
//			}
//		}

		Log.d(TAG, "Now connecting sock..");
		_sock.connect();

		// Let's configure the connection.
		Log.d(TAG, "Configured connection task queue...");
		queueJob(new ObdCommandJob(new ObdResetCommand()));
		queueJob(new ObdCommandJob(new EchoOffObdCommand()));

		/*
		 * Will send second-time based on tests.
		 * 
		 * TODO this can be done w/o having to queue jobs by just issuing
		 * command.run(), command.getResult() and validate the result.
		 */
		queueJob(new ObdCommandJob(new EchoOffObdCommand()));
		queueJob(new ObdCommandJob(new LineFeedOffObdCommand()));
		queueJob(new ObdCommandJob(new TimeoutObdCommand(62)));

		// For now set protocol to AUTO
		queueJob(new ObdCommandJob(new SelectProtocolObdCommand(
		        ObdProtocols.AUTO)));
		
		// Job for returning dummy data
		queueJob(new ObdCommandJob(new AmbientAirTemperatureObdCommand()));

		Log.d(TAG, "Initialize task queue.");

		// Service is running..
		_isRunning.set(true);

		// Set queue execution counter
		_queueCounter = 0L;
	}

	/**
	 * Runs the queue until the service is stopped
	 */
	private void _executeQueue() {
		Log.d(TAG, "Execute queue..");

		_isQueueRunning.set(true);

		while (!_queue.isEmpty()) {
			ObdCommandJob job = null;
			try {
				job = _queue.take();

				// log job
				Log.d(TAG, "Taking job[" + job.getId() + "] from queue..");

				if (job.getState().equals(ObdCommandJobState.NEW)) {
					Log.d(TAG, "Job state is NEW. Run it..");

					job.setState(ObdCommandJobState.RUNNING);
					job.getCommand().run(_sock.getInputStream(),
					        _sock.getOutputStream());
				} else {
					// log not new job
					Log.e(TAG,
					        "Job state was not new, so it shouldn't be in queue. BUG ALERT!");
				}
			} catch (Exception e) {
				job.setState(ObdCommandJobState.EXECUTION_ERROR);
				Log.e(TAG, "Failed to run command. -> " + e.getMessage());
			}

			if (job != null) {
				Log.d(TAG, "Job is finished.");
				job.setState(ObdCommandJobState.FINISHED);
				_callback.stateUpdate(job);
			}
		}

		_isQueueRunning.set(false);
	}

	/**
	 * This method will add a job to the queue while setting its ID to the
	 * internal queue counter.
	 * 
	 * @param job
	 * @return
	 */
	public Long queueJob(ObdCommandJob job) {
		_queueCounter++;
		Log.d(TAG, "Adding job[" + _queueCounter + "] to queue..");

		job.setId(_queueCounter);
		try {
			_queue.put(job);
		} catch (InterruptedException e) {
			job.setState(ObdCommandJobState.QUEUE_ERROR);
			// log error
			Log.e(TAG, "Failed to queue job.");
		}

		Log.d(TAG, "Job queued successfully.");
		return _queueCounter;
	}

	/**
	 * Stop OBD connection and queue processing.
	 */
	public void stopService() {
		Log.d(TAG, "Stopping service..");

		clearNotification();
		_queue.removeAll(_queue); // TODO is this safe?
		_isQueueRunning.set(false);
		_callback = null;
		_isRunning.set(false);

		// close socket
		try {
			if (_dev == null) {
				Log.d(TAG, "dev is null");
			}
			_sock.close();
		} catch (IOException e) {
			Log.e(TAG, e.getMessage());
		}

		// kill service
		stopSelf();
	}

	/**
	 * Show a notification while this service is running.
	 */
	private void showNotification() {
		// Set the icon, scrolling text and timestamp
//		Notification notification = new Notification(R.drawable.icon,
//		        getText(R.string.service_started), System.currentTimeMillis());
		Notification notification;

		// Launch our activity if the user selects this notification
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
		        new Intent(this, MainActivity.class), 0);

		// Set the info for the views that show in the notification panel.
//		notification.setLatestEventInfo(this,
//		        getText(R.string.notification_label),
//		        getText(R.string.service_started), contentIntent);

		Notification.Builder builder = new Notification.Builder(this);
		builder.setContentText("Tap to open OBD-Reader.");
		builder.setContentText("OBD connection has started.");
		builder.setContentIntent(contentIntent);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			builder.build();
		}

		notification = builder.getNotification();



		// Send the notification.
//		_notifManager.notify(R.string.service_started, notification);
	}

	/**
	 * Clear notification.
	 */
	private void clearNotification() {
//		_notifManager.cancel(R.string.service_started);
	}

	/**
	 * TODO put description
	 */
	public class LocalBinder extends Binder implements IPostMonitor {
		public void setListener(IPostListener callback) {
			_callback = callback;
		}

		public boolean isRunning() {
			return _isRunning.get();
		}

		public void executeQueue() {
			_executeQueue();
		}

		public void addJobToQueue(ObdCommandJob job) {
			Log.d(TAG, "Adding job [" + job.getCommand().getName() + "] to queue.");
			_queue.add(job);

			if (!_isQueueRunning.get())
				_executeQueue();
		}
	}

}