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

public class ConvexLoadResource extends ServerResource {

	private static Logger logger = Logger.getLogger(ConvexLoadResource.class);

	@Get
	public String represent() {
		return "hello, world";
	}

	@Post
	public Representation acceptItem(Representation entity) {
		System.out.println("Device attempting to log in");
		Representation result = null;
		Form form = new Form(entity);

		String device_id = form.getFirstValue("device_id");
		String auth_token = form.getFirstValue("auth_token");
		int group_id = Integer.getInteger(form.getFirstValue("group_id"));

		Device thisDevice = new Device(device_id);
		thisDevice.loadDevice();
		if (thisDevice.authenticateToken(auth_token)) {
			ConvexHullHandler handler = new ConvexHullHandler(device_id);
			handler.setGroupID(group_id);
			handler.setParentUsername(thisDevice.parent_username);
			
			HashMap<String, ConvexHullPoint> group = new HashMap<String, ConvexHullPoint>();
			group = handler.loadPoints();
			
			JSONObject object = new JSONObject();
			try {
				for (int i = 0; i < group.size(); i++) {
					ConvexHullPoint point = group.get(i);
					
					object.putOnce(String.valueOf(point.getMarker_id()),
							point.toJson());
				}
				result = new JsonRepresentation(object);
			} catch (JSONException e) {
				logger.error("An exception occured while trying to Jsonify the convex geofence markers");
				result = new JsonRepresentation(getErrorObj());
				e.printStackTrace();
			}
		} else {
			result = new JsonRepresentation(getErrorObj());
		}

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

}
