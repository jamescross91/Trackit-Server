import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.data.Form;
import org.restlet.data.Parameter;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

public class ConvexSaveResource extends ServerResource {

	private static Logger logger = Logger.getLogger(ConvexSaveResource.class);

	@Get
	public String represent() {
		return "hello, world";
	}

	@Post
	public Representation acceptItem(Representation entity) {
		System.out.println("Device attempting to save geomarker");
		Representation result = null;
		Form form = new Form(entity);

		Parameter param = form.get(0);
		String first = param.getFirst();
		JSONObject object;

		try {
			object = new JSONObject(first);

			String device_id = object.getString("device_id");
			String auth_token = object.getString("auth_token");
			String group_id = object.getString("group_id");

			Device thisDevice = new Device(device_id);
			thisDevice.loadDevice();
			if (thisDevice.authenticateToken(auth_token)) {

				JSONObject groupObject = object.getJSONObject(group_id);
				HashMap<String, ConvexHullPoint> pointList = unwrapJson(groupObject);

				ConvexHullHandler handler = new ConvexHullHandler(
						Integer.valueOf(group_id), pointList,
						thisDevice.parent_username);

				handler.savePoints();

				// Recompute the location alerts
				ArrayList<Device> devices = loadParentsChildren(device_id);
				for (int i = 0; i < devices.size(); i++) {
					AlertsManager thisManager = new AlertsManager();
					DeviceLocation latestLoc = new DeviceLocation();
					latestLoc.loadLatest(devices.get(i).device_id);
					thisManager.setLocation(latestLoc);
					thisManager.processAlerts();
				}

				result = new JsonRepresentation(handler.toJson());
			} else
				result = new JsonRepresentation(getErrorObj());

			// result = new JsonRepresentation(handler.toJson());
		} catch (JSONException e) {
			result = new JsonRepresentation(getErrorObj());
			e.printStackTrace();
		}

		return (result);
	}

	private HashMap<String, ConvexHullPoint> unwrapJson(JSONObject object) {
		HashMap<String, ConvexHullPoint> points = new HashMap<String, ConvexHullPoint>();
		String nice_name;
		
		Iterator<?> keys = object.keys();
		while (keys.hasNext()) {
			try {
				String key = (String) keys.next();
				JSONObject thisObject = (JSONObject) object.get(key);

				double latitude = thisObject.getDouble("latitude");
				double longitude = thisObject.getDouble("longitude");
				long marker_id = thisObject.getLong("marker_id");

				if (thisObject.has("nice_name")) {
					nice_name = thisObject.getString("nice_name");
				} else {
					nice_name = "";
				}
				ConvexHullPoint thisPoint = new ConvexHullPoint(latitude,
						longitude, marker_id, nice_name);
				points.put(Long.toString(marker_id), thisPoint);

			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return points;
	}

	private JSONObject getErrorObj() {
		JSONObject object = new JSONObject();
		try {
			object.put("failure", "Device did not authenticate");
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return object;
	}

	private ArrayList<Device> loadParentsChildren(String parent_device_id) {
		Device device = new Device(parent_device_id);
		device.loadDevice();

		ArrayList<Device> devices = new ArrayList<Device>();

		List<HashMap<String, Object>> result = null;
		String sqlString = "SELECT * FROM device_details WHERE parent_username = ? AND is_child = 1";

		LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
		data.put("parent_username", device.parent_username);

		try {
			result = DatabaseCore.executeSqlQuery(sqlString, data);
		} catch (Exception e) {
			logger.error("Error loading parents device details from the database");
			e.printStackTrace();
		}

		if (result.size() == 0) {
			// No devices
		}

		for (int i = 0; i < result.size(); i++) {
			HashMap<String, Object> thisEntry = result.get(i);
			Device thisDevice = new Device((String) thisEntry.get("device_id"));
			thisDevice.loadDevice();
			devices.add(thisDevice);
		}

		return devices;
	}
}
