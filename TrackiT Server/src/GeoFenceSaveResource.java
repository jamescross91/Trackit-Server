import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.data.Form;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

public class GeoFenceSaveResource extends ServerResource {

	private static Logger logger = Logger.getLogger(GeoFenceSaveResource.class);

	@Get
	public String represent() {
		return "hello, world";
	}

	@Post
	public Representation acceptItem(Representation entity) {
		System.out.println("Device attempting to save geomarker");
		Representation result = null;
		Form form = new Form(entity);

		String device_id = form.getFirstValue("device_id");
		String auth_token = form.getFirstValue("auth_token");
		double lat = Double.parseDouble(form.getFirstValue("latitude"));
		double lng = Double.parseDouble(form.getFirstValue("longitude"));
		double radius = Double.parseDouble(form.getFirstValue("radius"));
		long marker_id = Long.parseLong(form.getFirstValue("marker_id"));

		Device thisDevice = new Device(device_id);
		thisDevice.loadDevice();
		if (thisDevice.authenticateToken(auth_token)) {
			RadialGeofenceHandler handler = new RadialGeofenceHandler(marker_id);
			handler.setLat(lat);
			handler.setLng(lng);
			handler.setRadius(radius);
			handler.setParent_username(thisDevice.parent_username);
			handler.savePoint();

			// Issue an update to devices asking them to reload their geofences
			AlertsManager manager = new AlertsManager();
			manager.setDevice(thisDevice);
			manager.setMarker_id(marker_id);
			manager.processGeofenceUpdates();

			// Recompute the location alerts
			ArrayList<Device> devices = loadParentsChildren(device_id);
			for(int i = 0; i < devices.size(); i++){
				AlertsManager thisManager = new AlertsManager();
				DeviceLocation latestLoc = new DeviceLocation();
				latestLoc.loadLatest(devices.get(i).device_id);
				thisManager.setLocation(latestLoc);
				thisManager.processAlerts();
			}

			result = new JsonRepresentation(handler.toJson());
		} else
			result = new JsonRepresentation(getErrorObj());

		return (result);
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
