import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

public class DeviceLogin extends Device implements Jsonifiable {
	private String username;
	private String password;
	private String OS;
	private String authToken = new String();
	private boolean loginSuccess = false;
	private static Logger logger = Logger.getLogger(DeviceLogin.class);

	// This is the process of a new device connecting to the application for the
	// first time
	public DeviceLogin(String deviceID, String username, String password,
			String make, String model, double phone_number, String OS,
			boolean is_child, String gcm_token) {
		super(deviceID);
		this.device_id = deviceID;
		if (deviceID.compareTo("") == 0)
			this.device_id = generateToken();
		this.username = username;
		this.password = password;
		this.make = make;
		this.model = model;
		this.is_child = is_child;
		this.OS = OS;
		this.gcm_token = gcm_token;
	}

	public boolean login() {
		String authToken;
		User thisUser = new User(username);

		// Do these credentials authenticate?
		if (!thisUser.login(password)) {
			logger.warn("Device attempted to login with invalid credentials");
			return false;
		}

		// If the device does not exist, create it
		if (!deviceExists()) {
			// Did the device get inserted correctly?
			if (!createDevice())
				return false;

			// Did a new authentication token get created successfully?
			authToken = generateAuth();
			if (authToken.compareTo("") == 0)
				return false;
		}

		authToken = getAuth();

		// Was the auth token generated and saved as expected?
		if (authToken == new String())
			return false;

		this.authToken = authToken;
		this.loginSuccess = true;

		return true;
	}

	// JSonify the object we need
	public JSONObject toJson() {
		JSONObject object = new JSONObject();
		try {
			object.put("loginSuccess", loginSuccess);
			object.put("authToken", authToken);
		} catch (JSONException e) {
			logger.error("An exception occured while trying to Jsonify the login result for device id "
					+ device_id);
			e.printStackTrace();
		}

		return object;
	}

	private boolean deviceExists() {
		List<HashMap<String, Object>> result = null;
		String sqlString = "SELECT parent_username FROM device_details WHERE device_id = ?";

		LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
		data.put("deviceID", device_id);

		try {
			result = DatabaseCore.executeSqlQuery(sqlString, data);
		} catch (Exception e) {
			logger.error("Error extracting device details when attempting to login.  Device ID is "
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

		HashMap<String, Object> thisUser = result.get(0);
		String user = (String) thisUser.get("parent_username");

		// Does the username in the database match that for the device?
		if (user.compareTo(username) != 0)
			return false;

		return true;

	}

	private boolean createDevice() {
		String sqlString = "INSERT INTO device_details(device_id, parent_username, make, model, OS, is_child, gcm_token) values(?, ?, ?, ?, ?, ?, ?)";

		LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
		data.put("deviceID", device_id);
		data.put("username", username);
		data.put("make", make);
		data.put("model", model);
		data.put("OS", OS);
		data.put("is_child", is_child);
		data.put("gcm_token", gcm_token);

		try {
			if (DatabaseCore.executeSqlUpdate(sqlString, data)) {
				return true;
			}
		} catch (Exception e) {
			logger.error("Unable to insert device into the database for username: "
					+ username);
			e.printStackTrace();
		}

		return false;
	}

	private String getAuth() {
		String sqlString = "SELECT auth_token FROM device_details WHERE device_id = ?";
		List<HashMap<String, Object>> result = null;

		LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
		data.put("device_id", device_id);

		try {
			result = DatabaseCore.executeSqlQuery(sqlString, data);
		} catch (Exception e) {
			logger.error("Error extracting device details when attempting to login.  Device ID is "
					+ device_id);
			e.printStackTrace();
		}

		if (result.size() > 1) {
			logger.error("Multiple devices found for: " + device_id
					+ " something is broken!");
			return "";
		}

		if (result.size() == 0)
			return "";

		HashMap<String, Object> thisAuth = result.get(0);
		return (String) thisAuth.get("auth_token");
	}

	// Generate an authentication token, save it to the database, and return it
	private String generateAuth() {
		String token = generateToken();

		String sqlString = "UPDATE device_details SET auth_token = ? WHERE device_id = ?";
		LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
		data.put("token", token);
		data.put("device_id", device_id);

		try {
			if (DatabaseCore.executeSqlUpdate(sqlString, data)) {
				return token;
			}
		} catch (Exception e) {
			logger.error("Unable to write authentication token to the database for device id: "
					+ device_id);
			e.printStackTrace();
		}

		return new String();
	}

	private String generateToken() {
		// Create a random authentication token to be used with the device, in
		// the same way we create a salt for passwords
		SecureRandom randomSalt = new SecureRandom();
		byte[] salt = new byte[Integer.parseInt(ReadProperties
				.getProperty("salt_bytes")) * 2];
		randomSalt.nextBytes(salt);

		BigInteger bigInt = new BigInteger(1, salt);
		String hex = bigInt.toString(16);
		int paddingLength = (salt.length * 2) - hex.length();
		if (paddingLength > 0)
			return String.format("%0" + paddingLength + "d", 0) + hex;
		else
			return hex;
	}
}
