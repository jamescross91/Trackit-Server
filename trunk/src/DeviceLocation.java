import java.util.LinkedHashMap;

import org.apache.log4j.Logger;

public class DeviceLocation extends ExtConnect {
	private String entry_time;
	private double latitude;
	private double longitude;
	private String location_source;
	private int accuracy;
	public int altitude;
	private int bearing;
	private int battery;
	private boolean is_charging;
	private String network;
	private String data_connection;
	private double velocity;

	private static Logger logger = Logger.getLogger(DeviceLocation.class);

	public DeviceLocation(int device_id, String parent_username,
			String auth_token, String entry_time, double latitude,
			double longitude, String location_source, int altitude,
			int accuracy, int bearing, int battery, boolean is_charging,
			String network, String data_connection, double velocity) {

		// Set the private variables for this instance
		this.device_id = device_id;
		this.parent_username = parent_username;
		this.auth_token = auth_token;
		this.entry_time = entry_time;
		this.latitude = latitude;
		this.longitude = longitude;
		this.location_source = location_source;
		this.altitude = altitude;
		this.accuracy = accuracy;
		this.bearing = bearing;
		this.battery = battery;
		this.is_charging = is_charging;
		this.network = network;
		this.velocity = velocity;
	}

	public boolean persistLocation() {
		boolean dbSuccess = false;

		if (!validateData() || authenticateDevice())
			return false;

		//TODO Add the date/time the device creates the location instead of when its inserted into the database
		
		String sqlString = "INSERT INTO location_details (device_id, latitude, longitude, location_source, "
				+ "accuracy, altitude, bearing, battery, is_charging, network, data_connection, velocity) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		LinkedHashMap<Object, String> data = new LinkedHashMap<Object, String>();
		data.put(device_id, "string");
		//data.put("entry_time", "timestamp");
		data.put(latitude, "double");
		data.put(longitude, "double");
		data.put(location_source, "string");
		data.put(altitude, "int");
		data.put(bearing, "int");
		data.put(battery, "int");
		data.put(is_charging, "boolean");
		data.put(network, "string");
		data.put(data_connection, "string");
		data.put(velocity, "double");

		try {
			dbSuccess = DatabaseCore.executeSqlUpdate(sqlString, data);
		} catch (Exception e) {

			logger.error("Error inserting location update into the database for device id: "
					+ device_id);
			e.printStackTrace();
		}

		if (!dbSuccess) {
			logger.error("Error inserting location update into the database for device id: "
					+ device_id);
		}

		return true;
	}

	private boolean validateData() {
		return (validateBattery() && validateBearing() && validateAccuracy()
				&& validateLocSource() && validateLat() && validateLong());
	}

	// Ensure the given remaining battery is within the accepted range
	private boolean validateBattery() {
		if ((battery >= 0) && (battery <= 100)) {
			return true;
		}
		logger.warn("Invalid battery data for location update from device ID: "
				+ device_id);
		return false;
	}

	// Ensure the given bearing is within the accepted range
	private boolean validateBearing() {
		if ((bearing >= 0) && (bearing <= 360)) {
			return true;
		}
		return false;
	}

	private boolean validateAccuracy() {
		return true;
	}

	// Ensure the given location source valid
	private boolean validateLocSource() {
		if ((location_source != "gps") || (location_source != "wifi")) {
			return false;
		}
		return true;
	}

	// Ensure the given latitude is within the accepted range
	private boolean validateLat() {
		if ((latitude >= -90) && (latitude <= 90)) {
			return true;
		}
		return false;
	}

	// Ensure the given longitude is within the accepted range
	private boolean validateLong() {
		if ((longitude >= -180) && (longitude <= 180)) {
			return true;
		}
		return false;
	}
}
