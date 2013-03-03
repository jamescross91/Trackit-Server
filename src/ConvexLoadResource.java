import java.util.HashMap;
import java.util.Map.Entry;

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
		System.out.println("Device attempting to load convex geofences");
		Representation result = null;
		Form form = new Form(entity);

		String device_id = form.getFirstValue("device_id");
		String auth_token = form.getFirstValue("auth_token");

		Device thisDevice = new Device(device_id);
		thisDevice.loadDevice();
		if (thisDevice.authenticateToken(auth_token)) {
			ConvexHullHandler handler = new ConvexHullHandler(thisDevice.parent_username);
			

			// This is a flattened list of all points for this parent username
			HashMap<String, HashMap<String, ConvexHullPoint>> convexMarkerLists = new HashMap<String, HashMap<String, ConvexHullPoint>> ();
			convexMarkerLists = handler.loadPoints();

			JSONObject groupList = new JSONObject();
			try {
				for(Entry<String, HashMap<String, ConvexHullPoint>> groupEntry : convexMarkerLists.entrySet()){
					JSONObject thisGroup = new JSONObject();
					String group_id = groupEntry.getKey();
					HashMap<String, ConvexHullPoint> group = groupEntry.getValue();
					
					for(Entry<String, ConvexHullPoint> markerEntry : group.entrySet()){
						String marker_id = markerEntry.getKey();
						ConvexHullPoint thisPoint = markerEntry.getValue();
						thisGroup.putOnce(marker_id, thisPoint.toJson());
					}
					
					groupList.putOnce(group_id, thisGroup);
				}
				
				result = new JsonRepresentation(groupList);
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
