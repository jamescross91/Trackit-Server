import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.log4j.Logger;

//Class containing common functionality for applications wishing to connect to the system externally
public class Device {
	// Authenticate
	// Login
	// Execute SQL - remember we will need to handle both querys and inserts
	// here
	// Privs will need to be changed in DatabaseCore. Try mapping from a class
	// to a username/password - which will in turn be configured in MySQL

	public String device_id;
	public String parent_username;
	public String auth_token;
	public String make;
	public String model;
	public boolean is_child;
	public String gcm_token;
	public String OS;

	private static Logger logger = Logger.getLogger(Device.class);

	public Device(String device_id) {
		this.device_id = device_id;
	}

	protected boolean authenticateToken(String token) {

		if (token.compareTo(auth_token) != 0) {
			logger.warn("Device "
					+ device_id
					+ " is attempting to connect with an invalid authentication token");
			return false;
		}

		return true;
	}

	public boolean loadDevice() {
		List<HashMap<String, Object>> result = null;

		String sqlString = "SELECT * FROM device_details WHERE device_id = ?";
		LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
		data.put("device_id", device_id);

		try {
			result = DatabaseCore.executeSqlQuery(sqlString, data);
		} catch (Exception e) {
			logger.error("Error extracting device details from the database, device ID is: "
					+ device_id);
			e.printStackTrace();
		}

		if (result.size() > 1) {
			logger.error("Multiple devices found for: " + device_id
					+ " something is broken!");
			return false;
		}

		if (result.size() == 0)
			return false;

		// Check the username provided against the database
		HashMap<String, Object> thisDevice = result.get(0);

		auth_token = (String) thisDevice.get("auth_token");
		parent_username = (String) thisDevice.get("parent_username");
		make = (String) thisDevice.get("make");
		model = (String) thisDevice.get("model");
		is_child = (boolean) thisDevice.get("is_child");
		gcm_token = (String) thisDevice.get("gcm_token");
		OS = (String) thisDevice.get("OS");

		return true;
	}

	public void requestLocUpdate() {
		if (OS.compareTo("Android") == 0) {
			logger.info("Requesting location update for device: " + device_id);
			AndroidPushNotification notif = new AndroidPushNotification(
					PushNotification.LOCATION_REQUEST, this);
			notif.device = this;
			notif.pushMessage();
		}
	}
}
