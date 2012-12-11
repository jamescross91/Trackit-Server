import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

public class DeviceLogin extends ExtConnect implements Jsonifiable{
	private String deviceID;
	private String username;
	private String password;
	private String make;
	private String model;
	private double phone_number;
	private boolean is_child;
	private String OS;
	private String authToken = new String();
	private boolean loginSuccess;
	private static Logger logger = Logger.getLogger(DeviceLogin.class);

	// This is the process of a new device connecting to the application for the
	// first time
	public DeviceLogin(String deviceID, String username, String password,
			String make, String model, double phone_number, String OS,
			boolean is_child) {
		this.deviceID = deviceID;
		this.device_id = deviceID;
		this.username = username;
		this.password = password;
		this.make = make;
		this.model = model;
		this.phone_number = phone_number;
		this.is_child = is_child;
		this.OS = OS;
	}

	public String login() {
		User thisUser = new User(username);

		// Do these credentials authenticate?
		if (!thisUser.login(password))
			return new String();

		// If the device does not exist, create it
		if (!deviceExists()) {
			// Did the device get inserted correctly?
			if (!createDevice())
				return new String();

		}
		
		String authToken = getAuth();
		
		// Was the auth token generated and saved as expected?
		if (authToken == new String())
			return new String();

		loginSuccess = true;
		this.authToken = authToken;

		return (authToken);
	}

	// JSonify the object we need
	public JSONObject toJson() {
		JSONObject object = new JSONObject();
		try {
			object.put("loginSuccess", true);
			object.put("authToken", authToken);
		} catch (JSONException e) {
			logger.error("An exception occured while trying to Jsonify the login result for device id "
					+ deviceID);
			e.printStackTrace();
		}

		return object;
	}

	private boolean deviceExists() {
		List<HashMap<String, Object>> result = null;
		String sqlString = "SELECT parent_username FROM device_details WHERE device_id = ?";

		LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
		data.put("deviceID", deviceID);

		try {
			result = DatabaseCore.executeSqlQuery(sqlString, data);
		} catch (Exception e) {
			logger.error("Error extracting device details when attempting to login.  Device ID is "
					+ deviceID);
			e.printStackTrace();
		}

		if (result.size() > 1) {
			logger.error("Multiple devices found for: " + deviceID
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
		String sqlString = "INSERT INTO device_details(device_id, parent_username, make, model, phone_number, OS, is_child) values(?, ?, ?, ?, ?, ?, ?)";

		LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
		data.put("deviceID", deviceID);
		data.put("username", username);
		data.put("make", make);
		data.put("model", model);
		data.put("phone_number", phone_number);
		data.put("OS", OS);
		data.put("is_child", is_child);

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
	
	private String getAuth(){
		String sqlString = "SELECT auth_token FROM device_details WHERE device_id = ?";
		List<HashMap<String, Object>> result = null;
		
		LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
		data.put("device_id", deviceID);
		
		try {
			result = DatabaseCore.executeSqlQuery(sqlString, data);
		} catch (Exception e) {
			logger.error("Error extracting device details when attempting to login.  Device ID is "
					+ deviceID);
			e.printStackTrace();
		}

		if (result.size() > 1) {
			logger.error("Multiple devices found for: " + deviceID
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
