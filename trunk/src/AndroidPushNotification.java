import java.io.IOException;

import org.apache.log4j.Logger;

import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.Result;
import com.google.android.gcm.server.Sender;

public class AndroidPushNotification extends PushNotification implements Pushable {
	
	private static Logger logger = Logger.getLogger(AndroidPushNotification.class);
	
	public AndroidPushNotification(int alertType, String device_id) {
		super(alertType, device_id);
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
		String messageString = deviceLocation.toJson().toString();
		Sender sender = new Sender(ReadProperties.getProperty("gcm_key"));
		Message message = new Message.Builder().addData("message",
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
		return "";
	}
}
