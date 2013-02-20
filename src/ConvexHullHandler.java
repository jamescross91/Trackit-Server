import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.json.JSONObject;

public class ConvexHullHandler implements Jsonifiable {
	private int group_id;
	private HashMap<String, ConvexHullPoint> pointList;
	public static final int NEVER_SAVED = -1;
	private String parent_username;
	private String device_id;

	private static Logger logger = Logger.getLogger(ConvexHullHandler.class);

	public ConvexHullHandler(int group_id,
			HashMap<String, ConvexHullPoint> pointList, String parent_username) {
		this.group_id = group_id;
		this.pointList = pointList;
		this.parent_username = parent_username;
	}

	public ConvexHullHandler(String device_id) {
		this.device_id = device_id;
	}

	public HashMap<String, ConvexHullPoint> loadPoints() {

	}

	public HashMap<String, ConvexHullPoint> savePoints() {
		HashMap<String, ConvexHullPoint> updatedList = new HashMap<String, ConvexHullPoint>();

		if (group_id == NEVER_SAVED) {
			// Create a new group of markers
			group_id = createGroup(parent_username);
		}

		for (Entry<String, ConvexHullPoint> entry : pointList.entrySet()) {
			ConvexHullPoint thisPoint = entry.getValue();
			int marker_id = Integer.valueOf(entry.getKey());

			thisPoint.setMarker_id(saveMarker(marker_id, group_id,
					parent_username, thisPoint.getLatitude(),
					thisPoint.getLongitude()));

			updatedList
					.put(String.valueOf(thisPoint.getMarker_id()), thisPoint);
		}

		// Clear out the original point list and replace its contents with the
		// updated list
		pointList.clear();
		for (Entry<String, ConvexHullPoint> entry : updatedList.entrySet()) {
			ConvexHullPoint thisPoint = entry.getValue();
			String marker_id = (entry.getKey());

			pointList.put(marker_id, thisPoint);
		}

		return pointList;
	}

	private int saveMarker(int marker_id, int group_id, String parent_username,
			double lat, double lng) {
		LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
		long key = -1;

		if (marker_id == NEVER_SAVED) {
			String sqlString = "INSERT INTO convex_geofences (group_id, parent_username, latitude, longitude) VALUES(?, ?, ?, ?)";

			data.put("group_id", group_id);
			data.put("parent_username", parent_username);
			data.put("lat", lat);
			data.put("lng", lng);
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

			marker_id = (int) key;
		}

		else {
			String sqlString = "UPDATE convex_geofences SET latitude=?, longitude=? WHERE marker_id = ?";
			data.put("lat", lat);
			data.put("lng", lng);

			try {
				key = DatabaseCore.executeSqlUpdate(sqlString, data);
			} catch (Exception e) {
				logger.error("Unable to update radial geofence for marker id "
						+ marker_id);
				e.printStackTrace();
			}
		}
		return (int) key;
	}

	private int createGroup(String parent_username) {
		LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
		long key = -1;

		String sqlString = "INSERT INTO convex_groups (parent_username) VALUES(?)";

		data.put("parent_username", parent_username);
		try {
			key = DatabaseCore.executeSqlUpdate(sqlString, data);
		} catch (Exception e) {
			logger.error("Error convex hull group into the database for username: "
					+ parent_username);
			e.printStackTrace();
		}

		if (key == -1) {
			logger.error("Error convex hull group into the database for username: "
					+ parent_username);
		}

		return (int) key;
	}

	@Override
	public JSONObject toJson() {
		return null;
	}
}
