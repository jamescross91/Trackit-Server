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
			ConvexHullHandler handler = new ConvexHullHandler(device_id);
			handler.setParentUsername(thisDevice.parent_username);

			// This is a flattened list of all points for this parent username
			HashMap<String, ConvexHullPoint> groups = new HashMap<String, ConvexHullPoint>();
			groups = handler.loadPoints();

			HashMap<String, HashMap<String, ConvexHullPoint>> convexMarkerLists = unflatten(groups);

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
