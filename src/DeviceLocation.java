import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

public class DeviceLocation implements Jsonifiable{
	private String entry_time;
	private double latitude;
	private double longitude;
	private String location_source;
	private double accuracy;
	private double altitude;
	private double bearing;
	private int battery;
	private boolean is_charging;
	private String network;
	private String data_connection;
	private double velocity;
	protected String auth_token;
	private Device device;

	private static Logger logger = Logger.getLogger(DeviceLocation.class);

	//Overload the constructer
	public DeviceLocation(){
	}

	public DeviceLocation(Device device, double latitude, double longitude,
			String location_source, double altitude, double accuracy,
			double bearing, int battery, boolean is_charging, String network,
			String data_connection, double velocity) {

		// Set the private variables for this instance
		this.device = device;
		this.auth_token = auth_token;
		// this.entry_time = entry_time;
		this.latitude = latitude;
		this.longitude = longitude;
		this.location_source = location_source;
		this.altitude = altitude;
		this.accuracy = accuracy;
		this.bearing = bearing;
		this.battery = battery;
		this.is_charging = is_charging;
		this.network = network;
		this.data_connection = data_connection;
		this.velocity = velocity;
	}

	public boolean persistLocation() {
		long index = -1;
		
		if (!validateData()){
			logger.warn("Devices location update was invalid");
			return false;
		}

		// TODO Add the date/time the device creates the location instead of
		// when its inserted into the database

		String sqlString = "INSERT INTO location_details (device_id, latitude, longitude, location_source, "
				+ "accuracy, altitude, bearing, battery, is_charging, network, data_connection, velocity) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
		data.put("device_id", device.device_id);
		// data.put("entry_time", "timestamp");
		data.put("latitude", latitude);
		data.put("longitude", longitude);
		data.put("location_source", location_source);
		data.put("accuracy", accuracy);
		data.put("altitude", altitude);
		data.put("bearing", bearing);
		data.put("battery", battery);
		data.put("is_charging", is_charging);
		data.put("network", network);
		data.put("data_connection", data_connection);
		data.put("velocity", velocity);

		try {
			index = DatabaseCore.executeSqlUpdate(sqlString, data);
		} catch (Exception e) {

			logger.error("Error inserting location update into the database for device id: "
					+ device.device_id);
			e.printStackTrace();
		}

		if (index == -1) {
			logger.error("Error inserting location update into the database for device id: "
					+ device.device_id);
		}

		return true;
	}
	
	//Load the latest location for this device	
	public boolean loadLatest(String device_id) {

		List<HashMap<String, Object>> result = null;

		String sqlString = "SELECT * FROM location_details WHERE device_id = ? ORDER BY entry_time DESC LIMIT  1";
		LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
		data.put("device_id", device_id);

		try {
			result = DatabaseCore.executeSqlQuery(sqlString, data);
		} catch (Exception e) {
			logger.error("Error loading latest device location for device: "
					+ device_id);
			e.printStackTrace();
		}

		if (result.size() > 1) {
			logger.error("Multiple locations found for: " + device_id
					+ " something is broken!");
			return false;
		}

		if (result.size() == 0) {
			logger.error("Unable to find the most recent location for device id: "
					+ device_id);

			return false;
		}

		// Check the username provided against the database
		HashMap<String, Object> thisLoc = result.get(0);
		latitude = (double) thisLoc.get("latitude");
		longitude = (double) thisLoc.get("longitude");
		location_source = (String) thisLoc.get("location_source");
		altitude = (int) thisLoc.get("altitude");
		accuracy = (double) thisLoc.get("accuracy");
		bearing = (int) thisLoc.get("bearing");
		battery = (int) thisLoc.get("battery");
		is_charging = (boolean) thisLoc.get("is_charging");
		network = (String) thisLoc.get("network");
		data_connection = (String) thisLoc.get("data_connection");
		velocity = (int) thisLoc.get("velocity");
		this.device = new Device(device_id);

		return true;
	}

	private boolean validateData() {
		return (validateBattery() && validateBearing() && validateAccuracy()
				&& validateLat() && validateLong());
	}

	// Ensure the given remaining battery is within the accepted range
	private boolean validateBattery() {
		if ((battery >= 0) && (battery <= 100)) {
			return true;
		}
		logger.warn("Invalid battery data for location update from device ID: "
				+ device.device_id);
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

	@Override
	public JSONObject toJson() {
		JSONObject object = new JSONObject();
		try {
			object.put("latitude", latitude);
			object.put("longitude", longitude);
			object.put("location_source", location_source);
			object.put("altitude", altitude);
			object.put("accuracy", accuracy);
			object.put("bearing", bearing);
			object.put("battery", battery);
			object.put("is_charging", is_charging);
			object.put("network", network);
			object.put("data_connection", data_connection);
			object.put("device_id", device.device_id);
			object.put("velocity", velocity);
		} catch (JSONException e) {
			logger.error("An exception occured while trying to Jsonify the login result for device id "
					+ device.device_id);
			e.printStackTrace();
		}

		return object;
	}

	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}
	
	public Device getDevice(){
		return device;
	}
}
