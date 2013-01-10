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
	public static final long NEVER_SAVED = -1;

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

		return true;
	}
	
	public boolean deletePoint(){
		LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
		String sqlString = "DELETE FROM radial_geofences WHERE marker_id = ?";
		
		data.put("marker_id", (int)marker_id);
		
		try {
			DatabaseCore.executeSqlUpdate(sqlString, data);
		} catch (Exception e) {
			logger.error("Error inserting radial geofence into the database for username: "
					+ parent_username);
			e.printStackTrace();
			return false;
		}
		
		return true;
	}

	public long savePoint() {
		LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
		long key = -1;
		
		if(marker_id == NEVER_SAVED){
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
		
		else{
			String sqlString = "UPDATE radial_geofences SET latitude=?, longitude=?, radius=? WHERE marker_id = ?";
			data.put("latitude", lat);
			data.put("longitude", lng);
			data.put("radius", radius);
			data.put("marker_id", (int)marker_id);

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
