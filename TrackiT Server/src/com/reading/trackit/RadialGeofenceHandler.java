package com.reading.trackit;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

public class RadialGeofenceHandler implements Jsonifiable {
	private long marker_id;
	private String parent_username;
	private double lat;
	private double lng;
	private double radius;
	private boolean loaded = false;
	public static final long NEVER_SAVED = -1;
	public static final int EARTH_RADIUS = 6371000; // Earth radius in M

	private static Logger logger = Logger
			.getLogger(RadialGeofenceHandler.class);

	public RadialGeofenceHandler(long marker_id) {
		this.marker_id = marker_id;
	}

	public boolean loadPoint() {
		List<HashMap<String, Object>> result = null;

		String sqlString = "SELECT * FROM radial_geofences WHERE marker_id = ?";
		LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
		data.put("marker_id", (int) marker_id);

		try {
			result = DatabaseCore.executeSqlQuery(sqlString, data);
		} catch (Exception e) {
			logger.error("Error extracting marker details from the database, marker ID is: "
					+ marker_id);
			e.printStackTrace();
		}

		if (result.size() > 1) {
			logger.error("Multiple markers found for: " + marker_id
					+ " something is broken!");
			return false;
		}

		if (result.size() == 0)
			return false;

		// Check the username provided against the database
		HashMap<String, Object> thisMarker = result.get(0);

		setParent_username((String) thisMarker.get("parent_username"));
		setLat((Double) thisMarker.get("latitude"));
		setLng((Double) thisMarker.get("longitude"));
		setRadius((Double) thisMarker.get("radius"));

		loaded = true;

		return true;
	}

	public boolean deletePoint() {
		LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
		String sqlString = "DELETE FROM radial_geofences WHERE marker_id = ?";
		String sqlString2 = "DELETE FROM radial_details WHERE marker_id = ?";

		data.put("marker_id", (int) marker_id);

		try {
			DatabaseCore.executeSqlUpdate(sqlString, data);
			DatabaseCore.executeSqlUpdate(sqlString2, data);
		} catch (Exception e) {
			logger.error("Error inserting radial geofence into the database for username: "
					+ parent_username);
			e.printStackTrace();
			return false;
		}

		return true;
	}

	// This is perhaps the most important function of the entire application -
	// works out if we need to alert
	public String requiresAlert(DeviceLocation location, Device device) {
		String alertString;
		logger.info("Checking device: " + device.model
				+ " against current radial geofence");
		if (!loaded)
			loadPoint();

		if (pointDetailsExist(device)) {
			// Details already exists for this device and marker
			// Check if we are inside or outside of the geofence
			if (existsInRadius(location)) {
				// We are inside the geofence
				// Check if we were inside it or outside it before
				if (getIsInside(device)) {
					// But we were already inside it before
					logger.info("Device already inside the geofence, no change");
					return null;
				} else {
					// We were outside it before
					alertString = device.model
							+ " has now moved inside your Geofence";

					// Update the database accordingly
					updateMarkerDetails(device, false, true);
					logger.info("Device has moved inside the geofence");
					return alertString;
				}
			}

			else {
				// Details exist for this device and marker, but we are outside
				// the geofence
				if (getIsInside(device)) {
					// But we were inside it before
					alertString = device.model
							+ " has now moved outside of your Geofence";

					// Update the database accordingly
					logger.info("Device has moved outside the geofence");
					updateMarkerDetails(device, false, false);

					return alertString;
				}

				else {
					// We were already outside it before!
					logger.info("Device already outside the geofence, no change");
					return null;
				}
			}

		} else {
			// This is the first check for this device and marker

			// Are we inside the defined radius?
			if (existsInRadius(location)) {
				// If yes and there were no previous details for this device and
				// marker, assume we were outside the radius before

				// Update the database accordingly
				updateMarkerDetails(device, true, true);

				alertString = device.model
						+ " has now moved inside your Geofence";
				logger.info("Device has moved inside the geofence");
				return alertString;

			} else {
				// There are no details for the marker/device, and we are
				// outside the circle so set this in the database
				updateMarkerDetails(device, true, false);
				logger.info("Device already outside the geofence, no change");
				// No alert is required
				return null;
			}

		}
	}

	private boolean existsInRadius(DeviceLocation location) {
		// Uses the Haversine formula
		double sourceLat = location.getLatitude();
		double sourceLng = location.getLongitude();

		double dLat = degreesToRadians((sourceLat - lat));
		double dLng = degreesToRadians((sourceLng - lng));

		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
				+ Math.cos(degreesToRadians(lat))
				* Math.cos(degreesToRadians(sourceLat)) * Math.sin(dLng / 2)
				* Math.sin(dLng / 2);

		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		double d = EARTH_RADIUS * c; // The distance in Km

		// Ensure its a positive
		d = Math.abs(d);
		System.out.println("############");
		System.out.println(d + "\n");

		if (d > radius)
			return false;

		// We are inside the circle...how exciting!
		if (d < radius)
			return true;

		return false;
	}

	private boolean getIsInside(Device device) {
		LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
		List<HashMap<String, Object>> result = null;

		String sqlString = "SELECT * FROM radial_details WHERE device_id = ? AND marker_id = ?";

		data.put("device_id", device.device_id);
		data.put("marker_id", (int) marker_id);
		try {
			result = DatabaseCore.executeSqlQuery(sqlString, data);

		} catch (Exception e) {
			logger.error("Error extracting radial geofence details for marker: "
					+ marker_id);
			e.printStackTrace();
		}

		if (result.size() != 1) {
			logger.error("Error attempting to extract radial details for marker_id "
					+ marker_id
					+ " the incorrect number of results was returned!");
			return false;
		}

		HashMap<String, Object> thisMarker = result.get(0);
		return (boolean) thisMarker.get("is_inside");
	}

	private boolean updateMarkerDetails(Device device, boolean initialInsert,
			boolean is_inside) {
		LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
		long key = -1;
		String sqlString;

		if (initialInsert) {
			sqlString = "INSERT INTO radial_details (device_id, marker_id, is_inside) VALUES(?, ?, ?)";
			data.put("device_id", device.device_id);
			data.put("marker_id", (int) marker_id);
			data.put("is_inside", is_inside);
		} else {
			sqlString = "UPDATE radial_details SET is_inside = ? WHERE marker_id = ? AND device_id = ?";
			data.put("is_inside", is_inside);
			data.put("marker_id", (int) marker_id);
			data.put("device_id", device.device_id);
		}

		try {
			key = DatabaseCore.executeSqlUpdate(sqlString, data);
		} catch (Exception e) {
			logger.error("Error inserting radial geofence into the database for username: "
					+ parent_username);
			e.printStackTrace();
			return false;
		}
		return true;
	}

	private double degreesToRadians(double degrees) {
		return degrees * (Math.PI / 180);
	}

	private boolean pointDetailsExist(Device device) {

		LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
		List<HashMap<String, Object>> result;

		if (marker_id != NEVER_SAVED) {
			String sqlString = "SELECT * FROM radial_details WHERE device_id = ? AND marker_id = ?";

			data.put("device_id", device.device_id);
			data.put("marker_id", (int) marker_id);
			try {
				result = DatabaseCore.executeSqlQuery(sqlString, data);
				if (result.size() > 0)
					return true;
				else
					return false;
			} catch (Exception e) {
				logger.error("Error checking radial geofence detail or username: "
						+ parent_username);
				e.printStackTrace();
			}
		}

		return false;
	}

	public long savePoint() {
		LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
		long key = -1;

		if (marker_id == NEVER_SAVED) {
			String sqlString = "INSERT INTO radial_geofences (parent_username, latitude, longitude, radius) VALUES(?, ?, ?, ?)";

			data.put("parent_username", parent_username);
			data.put("lat", lat);
			data.put("lng", lng);
			data.put("radius", radius);
			try {
				key = DatabaseCore.executeSqlUpdate(sqlString, data);
			} catch (Exception e) {
				logger.error("Error inserting radial geofence into the database for username: "
						+ parent_username);
				e.printStackTrace();
			}

			if (key == -1) {
				logger.error("Error inserting radial geofence into the database for username: "
						+ parent_username);
			}

			marker_id = key;
		}

		else {
			String sqlString = "UPDATE radial_geofences SET latitude=?, longitude=?, radius=? WHERE marker_id = ?";
			data.put("latitude", lat);
			data.put("longitude", lng);
			data.put("radius", radius);
			data.put("marker_id", (int) marker_id);

			try {
				key = DatabaseCore.executeSqlUpdate(sqlString, data);
			} catch (Exception e) {
				logger.error("Unable to update radial geofence for marker id "
						+ marker_id);
				e.printStackTrace();
			}
		}
		return key;
	}

	public String getParent_username() {
		return parent_username;
	}

	public void setParent_username(String parent_username) {
		this.parent_username = parent_username;
	}

	public double getLat() {
		return lat;
	}

	public void setLat(double lat) {
		this.lat = lat;
	}

	public double getLng() {
		return lng;
	}

	public void setLng(double lng) {
		this.lng = lng;
	}

	public double getRadius() {
		return radius;
	}

	public void setRadius(double radius) {
		this.radius = radius;
	}

	public long getMarker_id() {
		return marker_id;
	}

	public void setMarker_id(Long marker_id) {
		this.marker_id = marker_id;
	}

	@Override
	public JSONObject toJson() {
		JSONObject object = new JSONObject();
		try {
			object.put("lat", lat);
			object.put("lng", lng);
			object.put("radius", radius);
			object.put("marker_id", marker_id);
		} catch (JSONException e) {
			logger.error("An exception occured while trying to Jsonify the radial geofence handler "
					+ marker_id);
			e.printStackTrace();
		}

		return object;
	}
}
