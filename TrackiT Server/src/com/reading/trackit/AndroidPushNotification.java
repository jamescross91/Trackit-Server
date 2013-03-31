package com.reading.trackit;
import java.io.IOException;

import org.apache.log4j.Logger;

import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.Result;
import com.google.android.gcm.server.Sender;

public class AndroidPushNotification extends PushNotification implements
		Pushable {

	public static final String LOCATION_UPDATE_KEY = "Loc";
	public static final String GEOFENCE_CROSS_KEY = "Geo";
	public static final String MARKER_RELOAD_KEY = "Marker";
	public static final String DEVICE_DELETE_KEY = "Delete";


	private static Logger logger = Logger
			.getLogger(AndroidPushNotification.class);

	public AndroidPushNotification(int alertType, Device device) {
		super(alertType, device);
	}

	@Override
	public String pushMessage() {
		String resultString = null;

		switch (alertType) {
		case LOCATION_UPDATE:
			resultString = pushLocationUpdate();
			break;
		case GEOFENCE_CROSS:
			resultString = pushGeofenceCross();
			break;
		case MARKER_UPDATE:
			resultString = pushGeofenceUpdate();
			break;
		case LOCATION_REQUEST:
			resultString = requestLocUpdate();
			break;
		case DEVICE_DELETE: forceDeleteDevice();
			break;
		}
		return resultString;
	}
	
	private String forceDeleteDevice(){
		Sender sender = new Sender(ReadProperties.getProperty("gcm_key"));
		Message message = new Message.Builder().addData(DEVICE_DELETE_KEY, "Device is being kicked").build();	
		Result result;
		try {
			result = sender.send(message, device.gcm_token, 1);
			return result.toString();
		} catch (IOException e) {
			logger.error("Loc update request push failed "
					+ e.toString());
		}
		
		return null;
	}
	
	private String requestLocUpdate(){
		Sender sender = new Sender(ReadProperties.getProperty("gcm_key"));
		Message message = new Message.Builder().addData("message", "Push message from server").build();	
		Result result;
		try {
			result = sender.send(message, device.gcm_token, 1);
			return result.toString();
		} catch (IOException e) {
			logger.error("Loc update request push failed "
					+ e.toString());
		}

		return null;
	}
	
	private String pushGeofenceUpdate(){
		Sender sender = new Sender(ReadProperties.getProperty("gcm_key"));
		Message message = new Message.Builder().addData(MARKER_RELOAD_KEY,
				String.valueOf(marker_id)).build();

		Result result;
		try {
			result = sender.send(message, device.gcm_token, 1);
			return result.toString();
		} catch (IOException e) {
			logger.error("Data alert push failed with exception "
					+ e.toString());
		}

		return null;
	}

	private String pushLocationUpdate() {
		String messageString = deviceLocation.toJson().toString();
		Sender sender = new Sender(ReadProperties.getProperty("gcm_key"));
		Message message = new Message.Builder().addData(LOCATION_UPDATE_KEY,
				messageString).build();

		Result result;
		try {
			result = sender.send(message, device.gcm_token, 1);
			return result.toString();
		} catch (IOException e) {
			logger.error("Data alert push failed with exception "
					+ e.toString());
		}

		return null;
	}

	private String pushGeofenceCross() {
		Sender sender = new Sender(ReadProperties.getProperty("gcm_key"));
		Message message = new Message.Builder().addData(GEOFENCE_CROSS_KEY,
				alertMessage).build();

		Result result;
		try {
			result = sender.send(message, device.gcm_token, 1);
			return result.toString();
		} catch (IOException e) {
			logger.error("Data alert push failed with exception "
					+ e.toString());
		}

		return null;
	}
}
