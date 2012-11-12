import java.math.BigInteger;
import java.security.SecureRandom;
import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.log4j.Logger;

//Class containing common functionality for applications wishing to connect to the system externally
public abstract class ExtConnect {
	// Authenticate
	// Login
	// Execute SQL - remember we will need to handle both querys and inserts
	// here
	// Privs will need to be changed in DatabaseCore. Try mapping from a class
	// to a username/password - which will in turn be configured in MySQL

	protected int device_id;
	protected String parent_username;
	protected String auth_token;
	
	private static Logger logger = Logger.getLogger(ExtConnect.class);

	protected boolean authenticateDevice() {
		// Check that for the parents username provided, this is a valid device
		// ID

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
		if ((String) thisDevice.get("parent_username") != parent_username) {
			logger.warn("Device " + device_id
					+ " is attempting to connect with an invalid username: "
					+ parent_username + ". Denying access");
			return false;
		}

		if ((String) thisDevice.get("auth_token") != auth_token) {
			logger.warn("Device "
					+ device_id
					+ " is attempting to connect with an invalid authentication token");
			return false;
		}

		return true;
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
