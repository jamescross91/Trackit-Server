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

public class GeoFenceLoadResource extends ServerResource {

	private static Logger logger = Logger.getLogger(GeoFenceLoadResource.class);

	@Get
	public String represent() {
		return "hello, world";
	}

	@Post
	public Representation acceptItem(Representation entity) {
		System.out.println("Device attempting to load radial geofences");
		Representation result = null;
		Form form = new Form(entity);

		String device_id = form.getFirstValue("device_id");
		String auth_token = form.getFirstValue("auth_token");

		Device thisDevice = new Device(device_id);
		thisDevice.loadDevice();
		if (thisDevice.authenticateToken(auth_token)) {
			ArrayList<RadialGeofenceHandler> points = loadPoints(thisDevice);

			JSONObject object = new JSONObject();
			try {
				for (int i = 0; i < points.size(); i++) {
					RadialGeofenceHandler marker = points.get(i);
					object.putOnce(String.valueOf(marker.getMarker_id()),
							marker.toJson());
				}
				result = new JsonRepresentation(object);
			} catch (JSONException e) {
				logger.error("An exception occured while trying to Jsonify the radial geofence markers");
				e.printStackTrace();
			}
		} else {
			result = new JsonRepresentation(getErrorObj());
		}

		return (result);
	}

	private ArrayList<RadialGeofenceHandler> loadPoints(Device device) {
		ArrayList<RadialGeofenceHandler> points = new ArrayList<RadialGeofenceHandler>();

		List<HashMap<String, Object>> result = null;

		String sqlString = "SELECT marker_id FROM radial_geofences WHERE parent_username = ?";
		LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
		data.put("parent_username", device.parent_username);

		try {
			result = DatabaseCore.executeSqlQuery(sqlString, data);
		} catch (Exception e) {
			logger.error("Error extracting geofence from the database");
			e.printStackTrace();
		}

		for (int i = 0; i < result.size(); i++) {
			HashMap<String, Object> thisMarker = result.get(i);

			RadialGeofenceHandler marker = new RadialGeofenceHandler(
					Long.valueOf((int) thisMarker.get("marker_id")));
			marker.loadPoint();
			points.add(marker);
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

}
