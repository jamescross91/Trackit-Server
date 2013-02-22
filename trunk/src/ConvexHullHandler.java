import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import com.jhlabs.map.proj.MercatorProjection;

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

	private boolean existsInPolygon(DeviceLocation location) {

		// Get a projection
		MercatorProjection projection = new MercatorProjection();

		// Project the devices latitude and longitude into cartesian space
		Point2D.Double cartesianDeviceLoc = projection.project(
				location.getLongitude(), location.getLatitude(),
				new Point2D.Double());

		// Compute the convex hull of points - this ensures outliers are removed
		// and the list is correctly sorted
		ConvexHull hullWorker = new ConvexHull();
		ArrayList<ConvexHullPoint> hull = hullWorker
				.computeDCHull(new ArrayList<ConvexHullPoint>(pointList
						.values()));

		int nextPointIndex = 0;
		boolean cross = false;
		for (int i = 0; i < hull.size(); i++) {
			nextPointIndex = i + 1;

			// Load points
			ConvexHullPoint point = hull.get(i);
			ConvexHullPoint nextPoint = hull.get(nextPointIndex);

			// Does the test point exist within the vertical scope of the edge
			// we are testing? IE if we drew horizontal lines of infinite length
			// cross the current and next vertices creating a vertical scope,
			// does the test point exist within it?
			boolean edgeScope = ((point.getCartesianY() > cartesianDeviceLoc.y) != (nextPoint
					.getCartesianY() > cartesianDeviceLoc.y));

			// If the edge was infinite in length would the line through the
			// test point intersect with it?
			boolean lineCross = (cartesianDeviceLoc.x < (((nextPoint
					.getCartesianX() - point.getCartesianX()) * (cartesianDeviceLoc.y - point
					.getCartesianY())) / ((nextPoint.getCartesianY() - point
					.getCartesianY()) + point.getCartesianX())));
			
			//If there is an intersection, invert the flag
			if( edgeScope && lineCross){
				cross = !cross;
			}
		}

		return cross;
	}

	public boolean deletePoints() {

		LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
		String sqlString = "DELETE FROM convex_geofences WHERE group_id = ?";
		String sqlString2 = "DELETE FROM convex_details WHERE group_id = ?";
		String sqlString3 = "DELETE FROM convex_groups WHERE group_id = ?";

		data.put("group_id", group_id);

		try {
			DatabaseCore.executeSqlUpdate(sqlString, data);
			DatabaseCore.executeSqlUpdate(sqlString2, data);
			DatabaseCore.executeSqlUpdate(sqlString3, data);
		} catch (Exception e) {
			logger.error("Error inserting radial geofence into the database for username: "
					+ parent_username);
			e.printStackTrace();
			return false;
		}

		return true;
	}

	public HashMap<String, ConvexHullPoint> loadPoints() {

		HashMap<String, ConvexHullPoint> group = new HashMap<String, ConvexHullPoint>();
		List<HashMap<String, Object>> result = null;

		String sqlString = "SELECT * FROM convex_geofences WHERE parent_username = ?";
		LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
		data.put("group_id", group_id);

		try {
			result = DatabaseCore.executeSqlQuery(sqlString, data);
		} catch (Exception e) {
			logger.error("Error extracting convex geofences from the database, group_if is: "
					+ group_id);
			e.printStackTrace();
		}

		if (result.size() == 0) {
			logger.error("No groups found for group id: " + group_id);
			return null;
		}

		for (int i = 0; i < result.size(); i++) {
			HashMap<String, Object> thisMarker = result.get(i);

			long marker_id = (int) thisMarker.get("marker_id");
			double lat = (double) thisMarker.get("latitude");
			double lng = (double) thisMarker.get("longitude");
			int group_id = (int) thisMarker.get("group_id");

			String niceName = getNiceName(group_id, parent_username);
			ConvexHullPoint thisPoint = new ConvexHullPoint(lat, lng,
					marker_id, niceName);
			thisPoint.setGroup_id(group_id);
			group.put(String.valueOf(marker_id), thisPoint);
		}

		return group;
	}

	private String getNiceName(int group_id, String parent_username) {
		List<HashMap<String, Object>> result = null;

		String sqlString = "SELECT * FROM convex_groups WHERE group_id = ? AND parent_username = ?";
		LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
		data.put("group_id", group_id);
		data.put("parent_username", parent_username);

		try {
			result = DatabaseCore.executeSqlQuery(sqlString, data);
		} catch (Exception e) {
			logger.error("Error extracting convex geofences from the database, group_if is: "
					+ group_id);
			e.printStackTrace();
		}

		if (result.size() == 0) {
			logger.info("No nice name found for group id: " + group_id);
			return null;
		}

		HashMap<String, Object> thisGroup = result.get(0);
		String niceName = (String) thisGroup.get("nice_name");

		return niceName;
	}

	private void setNiceName(int group_id, String nice_name) {
		String sqlString = "UPDATE convex_groups SET nice_name=? WHERE group_id = ?";
		LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
		data.put("nice_name", nice_name);
		data.put("group_id", group_id);

		try {
			DatabaseCore.executeSqlUpdate(sqlString, data);
		} catch (Exception e) {
			logger.error("Unable to update nice name for group id " + group_id);
			e.printStackTrace();
		}
	}

	public HashMap<String, ConvexHullPoint> savePoints() {
		HashMap<String, ConvexHullPoint> updatedList = new HashMap<String, ConvexHullPoint>();
		String niceName = "";

		if (group_id == NEVER_SAVED) {
			// Create a new group of markers
			group_id = createGroup(parent_username);
		}

		for (Entry<String, ConvexHullPoint> entry : pointList.entrySet()) {
			ConvexHullPoint thisPoint = entry.getValue();
			int marker_id = Integer.valueOf(entry.getKey());

			// So we return this to the client so they can update their records
			if (marker_id < 0) {
				thisPoint.setOld_marker_id(Long.valueOf(entry.getKey()));
			}

			niceName = thisPoint.getNice_name();

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

		setNiceName(group_id, niceName);

		return pointList;
	}

	private int saveMarker(int marker_id, int group_id, String parent_username,
			double lat, double lng) {
		LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
		long key = -1;

		if (marker_id < 0) {
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

	public void setGroupID(int group_id) {
		this.group_id = group_id;
	}

	public void setParentUsername(String parent_username) {
		this.parent_username = parent_username;
	}

	@Override
	public JSONObject toJson() {
		JSONObject object = new JSONObject();
		JSONObject markers = new JSONObject();
		try {
			for (Entry<String, ConvexHullPoint> entry : pointList.entrySet()) {
				ConvexHullPoint thisPoint = entry.getValue();
				long marker_id = thisPoint.getMarker_id();
				double lat = thisPoint.getLatitude();
				double lng = thisPoint.getLongitude();

				JSONObject markerObj = new JSONObject();

				// Has this been set? IE is this the first time the point was
				// saved?
				if (thisPoint.getOld_marker_id() < 0) {
					markerObj
							.put("old_marker_id", thisPoint.getOld_marker_id());
				}
				markerObj.put("marker_id", marker_id);
				markerObj.put("lat", lat);
				markerObj.put("lng", lng);

				markers.put(String.valueOf(marker_id), markerObj);
			}
			object.put("group_id", this.group_id);
			object.put("markers", markers);
		} catch (JSONException e) {
			logger.error("An exception occured while trying to Jsonify the ConvexHull handler");
			e.printStackTrace();
		}

		return object;
	}
}
