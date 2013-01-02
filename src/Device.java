import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.log4j.Logger;

//Class containing common functionality for applications wishing to connect to the system externally
public class Device{
	// Authenticate
	// Login
	// Execute SQL - remember we will need to handle both querys and inserts
	// here
	// Privs will need to be changed in DatabaseCore. Try mapping from a class
	// to a username/password - which will in turn be configured in MySQL

	protected String device_id;
	protected String parent_username;
	protected String auth_token;
	protected String make;
	protected String model;
	protected double phone_number;
	protected boolean is_child;
	
	private static Logger logger = Logger.getLogger(Device.class);

	protected boolean authenticateDevice() {

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

		if ((String) thisDevice.get("auth_token") != auth_token) {
			logger.warn("Device "
					+ device_id
					+ " is attempting to connect with an invalid authentication token");
			return false;
		}

		return true;
	}
	
	protected boolean loadDevice(){
		return true;
	}
}
