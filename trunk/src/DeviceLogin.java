import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.log4j.Logger;

public class DeviceLogin extends ExtConnect {
	private String deviceID;
	private String username;
	private String password;
	private String make;
	private String model;
	private double phone_number;
	private boolean is_child;
	private String OS;
	private static Logger logger = Logger.getLogger(DeviceLogin.class);

	// This is the process of a new device connecting to the application for the
	// first time
	public DeviceLogin(String deviceID, String username, String password, String make,
			String model, double phone_number, String OS, boolean is_child) {
		this.deviceID = deviceID;
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

		// Did the device get inserted correctly?
		if (!createDevice())
			return new String();

		String authToken = generateAuth();
		// Was the auth token generated and saved as expected?
		if (authToken == new String())
			return new String();

		return (authToken);
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
