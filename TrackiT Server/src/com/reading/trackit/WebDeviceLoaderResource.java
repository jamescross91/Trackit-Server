package com.reading.trackit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.restlet.data.Cookie;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;
import org.restlet.util.Series;

public class WebDeviceLoaderResource extends ServerResource {
	private static final String COOKIE_USER = "trackit_user";
	private static final String COOKIE_AUTH = "trackit_auth";
	private static Logger logger = Logger
			.getLogger(WebDeviceLoaderResource.class);

	@Post
	public Representation acceptItem(Representation entity) {

		StringBuilder builder = new StringBuilder();
		Series<Cookie> cookies = this.getRequest().getCookies();
		String parent_username = cookies.getFirstValue(COOKIE_USER);
		String auth_Token = cookies.getFirstValue(COOKIE_AUTH);

		if ((parent_username == null) || (auth_Token == null))
			return null;
		
		User thisUser = new User(parent_username);
		
		//Is this a valid cookie?
		if(!thisUser.validateCookie(auth_Token))
			return null;

		ArrayList<Device> devices = loadParentsChildren(parent_username);
		
		builder.append("\n");
		
		for (int i = 0; i < devices.size(); i++) {
			Device thisDevice = devices.get(i);

			builder.append("<div class=\"psdg-left\">" + thisDevice.device_id
					+ "</div>\n");
			builder.append("<div class=\"psdg-right\">"
					+ thisDevice.parent_username + "</div>\n");
			builder.append("<div class=\"psdg-right\">" + thisDevice.make
					+ "</div>\n");
			builder.append("<div class=\"psdg-right\">" + thisDevice.model
					+ "</div>\n");
			builder.append("<div class=\"psdg-right\">"
					+ "<a href=\"#\" title=\"Delete this device\" id=\"deviceKick"
					+ thisDevice.device_id + "\">Delete</a>" + "</div>\n");
		}
		
		for(int j = 0; j < devices.size(); j++){
			Device thisDevice = devices.get(j);
			builder.append(getDeviceKickHTML(thisDevice.device_id));
		}
		
		return new StringRepresentation(builder.toString(), MediaType.TEXT_HTML);
	}

	private String getDeviceKickHTML(String device_id) {
		StringBuilder builder = new StringBuilder();
		builder.append("\n<script type=\"text/javascript\">\n");
		
		builder.append("$('#deviceKick" + device_id + "').click(function (event) { event.preventDefault();")
			.append("$.ajax({")
			.append("type:\"POST\", url:\"/device/delete\", cache:false, dataType: \"html\",\n")
			.append("data: { deviceID: \"" + device_id + "\" } \n")
			.append("})\n")
			.append(".done(function(data) { alert(\"Device deleted, please refesh the page\");\n")
			.append("});\n");		
		
		
		builder.append("});");
				builder.append("</script>\n");

		return builder.toString();
	}

	private ArrayList<Device> loadParentsChildren(String parent_username) {
		ArrayList<Device> devices = new ArrayList<Device>();

		List<HashMap<String, Object>> result = null;
		String sqlString = "SELECT device_id FROM device_details WHERE parent_username = ?";

		LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
		data.put("parent_username", parent_username);

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