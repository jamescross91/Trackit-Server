import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.Sender;

public class TrackiTAlert implements Pushable {

	public static final int LOCATION_UPDATE = 1;
	public static final int GEOFENCE_CROSS = 2;

	private String gcmDeviceToken;
	private DeviceLocation deviceLocation;
	private String alertMessage;
	private int alertType;

	public TrackiTAlert(int alertType, String gcmDeviceToken) {
		this.alertType = alertType;
		this.gcmDeviceToken = gcmDeviceToken;
	}

	public void setAlertMessage(String alertMessage) {
		this.alertMessage = alertMessage;
	}

	public void setDeviceLocation(DeviceLocation deviceLocation) {
		this.deviceLocation = deviceLocation;
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
		Sender sender = new Sender(gcmDeviceToken);
		Message message = new Message.Builder()
				.addData("message", "Push message from server")
				.addData("", "")
				.build();

		return "";
	}

	private String pushGeofenceCross() {
		return "";
	}

}
