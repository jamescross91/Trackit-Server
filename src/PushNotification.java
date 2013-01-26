import java.io.IOException;

import org.apache.log4j.Logger;

import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.Result;
import com.google.android.gcm.server.Sender;

public abstract class PushNotification implements Pushable {

	public static final int LOCATION_UPDATE = 1;
	public static final int GEOFENCE_CROSS = 2;
	public static final int MARKER_UPDATE = 3;
	public static final int LOCATION_REQUEST = 4;

	protected Device device;
	protected DeviceLocation deviceLocation;
	protected String alertMessage;
	protected int alertType;
	protected long marker_id;
	
	private static Logger logger = Logger.getLogger(PushNotification.class);

	public PushNotification(int alertType, Device device) {
		this.alertType = alertType;
		this.device = device;
	}

	public void setAlertMessage(String alertMessage) {
		this.alertMessage = alertMessage;
	}

	public void setDeviceLocation(DeviceLocation deviceLocation) {
		this.deviceLocation = deviceLocation;
	}
	
	public void setmarker_id(long marker_id){
		this.marker_id = marker_id;
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
		}
		return resultString;
	}

	private String pushLocationUpdate() {
		Sender sender = new Sender(ReadProperties.getProperty("gcm_key"));
		Message message = new Message.Builder().addData("message", deviceLocation.toJson().toString()).build();
		
		Result result;
		try {
			result = sender.send(message, device.gcm_token, 1);
			return result.toString();
		} catch (IOException e) {
			logger.error("Data alert push failed with exception " + e.toString());
		}

		return null;
	}

	private String pushGeofenceCross() {
		return "";
	}

}
