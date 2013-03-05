import java.awt.Polygon;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import com.jhlabs.map.MapMath;
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

	public ConvexHullHandler(String parent_username) {
		this.parent_username = parent_username;
	}

	public String requiresAlert(DeviceLocation location, Device device) {
		String alertString;
		logger.info("Checking device: " + device.model
				+ " against current convex geofence");

		if (hullDetailsExist(device)) {
			// Details already exists for this device and marker
			// Check if we are inside or outside of the geofence
			if (existsInPolygon(location)) {
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
					updateHullDetails(device, false, true);
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
					updateHullDetails(device, false, false);

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
			if (existsInPolygon(location)) {
				// If yes and there were no previous details for this device and
				// marker, assume we were outside the radius before

				// Update the database accordingly
				updateHullDetails(device, true, true);

				alertString = device.model
						+ " has now moved inside your Geofence";
				logger.info("Device has moved inside the geofence");
				return alertString;

			} else {
				// There are no details for the marker/device, and we are
				// outside the circle so set this in the database
				updateHullDetails(device, true, false);
				logger.info("Device already outside the geofence, no change");
				// No alert is required
				return null;
			}

		}
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

		int nextPointIndex = (hull.size() - 1);
		boolean cross = false;
		for (int i = 0; i < hull.size(); nextPointIndex = i++) {

			// Load points
			ConvexHullPoint point = hull.get(i);
			ConvexHullPoint nextPoint = hull.get(nextPointIndex);

			// Does the test point exist within the vertical scope of the edge
			// we are testing? IE if we drew horizontal lines of infinite length
			// cross the current and next vertices creating a vertical scope,
			// does the test point exist within it?

			if (point.getCartesianY() < cartesianDeviceLoc.y
					&& nextPoint.getCartesianY() >= cartesianDeviceLoc.y
					|| nextPoint.getCartesianY() < cartesianDeviceLoc.y
					&& point.getCartesianY() >= cartesianDeviceLoc.y) {

				// If the edge was infinite in length would the line through the
				// test point intersect with it?
				if (point.getCartesianX()
						+ (cartesianDeviceLoc.y - point.getCartesianY())
						/ (nextPoint.getCartesianY() - point.getCartesianY())
						* (nextPoint.getCartesianX() - point.getCartesianX()) < cartesianDeviceLoc.x) {

					// If yes we have crossed one of the edges
					cross = !cross;
				}
			}
			nextPointIndex = i;
		}
		return cross;
	}

	private boolean hullDetailsExist(Device device) {

		LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
		List<HashMap<String, Object>> result;

		if (group_id != NEVER_SAVED) {
			String sqlString = "SELECT * FROM convex_details WHERE device_id = ? AND group_id = ?";

			data.put("device_id", device.device_id);
			data.put("group_id", (int) group_id);
			try {
				result = DatabaseCore.executeSqlQuery(sqlString, data);
				if (result.size() > 0)
					return true;
				else
					return false;
			} catch (Exception e) {
				logger.error("Error inserting convex geofence details into the database for username: "
						+ parent_username);
				e.printStackTrace();
			}
		}

		return false;
	}

	private boolean getIsInside(Device device) {
		LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
		List<HashMap<String, Object>> result = null;

		String sqlString = "SELECT * FROM convex_details WHERE device_id = ? AND group_id = ?";

		data.put("device_id", device.device_id);
		data.put("group_id", (int) group_id);
		try {
			result = DatabaseCore.executeSqlQuery(sqlString, data);

		} catch (Exception e) {
			logger.error("Error extracting convex geofence details for marker: "
					+ group_id);
			e.printStackTrace();
		}

		if (result.size() != 1) {
			logger.error("Error attempting to extract convex details for marker_id "
					+ group_id
					+ " the incorrect number of results was returned!");
			return false;
		}

		HashMap<String, Object> thisMarker = result.get(0);
		return (boolean) thisMarker.get("is_inside");
	}

	private boolean updateHullDetails(Device device, boolean initialInsert,
			boolean is_inside) {
		LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
		long key = -1;
		String sqlString;

		if (initialInsert) {
			sqlString = "INSERT INTO convex_details (device_id, group_id, is_inside) VALUES(?, ?, ?)";
			data.put("device_id", device.device_id);
			data.put("group_id", (int) group_id);
			data.put("is_inside", is_inside);
		} else {
			sqlString = "UPDATE convex_details SET is_inside = ? WHERE group_id = ? AND device_id = ?";
			data.put("is_inside", is_inside);
			data.put("group_id", (int) group_id);
			data.put("device_id", device.device_id);
		}

		try {
			key = DatabaseCore.executeSqlUpdate(sqlString, data);
		} catch (Exception e) {
			logger.error("Error inserting convex geofence detail into the database for username: "
					+ parent_username);
			e.printStackTrace();
			return false;
		}
		return true;
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
			logger.error("Error deleting convex geofence from the database for username: "
					+ parent_username);
			e.printStackTrace();
			return false;
		}

		return true;
	}

	public HashMap<String, HashMap<String, ConvexHullPoint>> loadPoints() {

		HashMap<String, ConvexHullPoint> group = new HashMap<String, ConvexHullPoint>();
		List<HashMap<String, Object>> result = null;

		String sqlString = "SELECT * FROM convex_geofences WHERE parent_username = ?";
		LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
		data.put("parent_username", parent_username);

		HashMap<String, String> niceNames = getNiceNames(parent_username);

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

			String niceName = niceNames.get(String.valueOf(group_id));
			ConvexHullPoint thisPoint = new ConvexHullPoint(lat, lng,
					marker_id, niceName);
			thisPoint.setGroup_id(group_id);
			group.put(String.valueOf(marker_id), thisPoint);
		}

		return unflatten(group);
	}

	private HashMap<String, HashMap<String, ConvexHullPoint>> unflatten(
			HashMap<String, ConvexHullPoint> list) {

		HashMap<String, HashMap<String, ConvexHullPoint>> convexMarkerLists = new HashMap<String, HashMap<String, ConvexHullPoint>>();

		for (Entry<String, ConvexHullPoint> entry : list.entrySet()) {
			ConvexHullPoint thisPoint = entry.getValue();

			// Have we already started creating a list for this group of points?
			if (convexMarkerLists.containsKey(String.valueOf(thisPoint
					.getGroup_id()))) {
				HashMap<String, ConvexHullPoint> groupList = convexMarkerLists
						.get(String.valueOf(thisPoint.getGroup_id()));

				// If we have then add this point to that list
				groupList.put(String.valueOf(thisPoint.getMarker_id()),
						thisPoint);
			} else {
				// Otherwise make a new sublist
				HashMap<String, ConvexHullPoint> groupList = new HashMap<String, ConvexHullPoint>();
				groupList.put(String.valueOf(thisPoint.getMarker_id()),
						thisPoint);
				convexMarkerLists.put(String.valueOf(thisPoint.getGroup_id()),
						groupList);
			}
		}

		return convexMarkerLists;
	}

	private HashMap<String, String> getNiceNames(String parent_username) {
		List<HashMap<String, Object>> result = null;

		String sqlString = "SELECT * FROM convex_groups WHERE parent_username = ?";
		LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
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

		// Loop over the results and put them into a hashmap
		HashMap<String, String> niceNames = new HashMap<String, String>();
		for(int i = 0; i < result.size(); i++){
			HashMap<String, Object> entry = result.get(i);
			
			String niceName = (String) entry.get("nice_name");
			String group_id = String.valueOf((int) entry.get("group_id"));
			
			niceNames.put(group_id, niceName);
		}

		return niceNames;
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
				logger.error("Error inserting convex geofence into the database for username: "
						+ parent_username);
				e.printStackTrace();
			}

			if (key == -1) {
				logger.error("Error inserting convex geofence into the database for username: "
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
				logger.error("Unable to update convex geofence for marker id "
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
