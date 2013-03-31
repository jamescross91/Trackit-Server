package com.reading.trackit;
import org.apache.log4j.Logger;
import org.restlet.data.Cookie;
import org.restlet.data.Form;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;
import org.restlet.util.Series;

public class DeviceDeleteResource extends ServerResource {
	private static Logger logger = Logger.getLogger(DeviceDeleteResource.class);
	private static final String COOKIE_USER = "trackit_user";
	private static final String COOKIE_AUTH = "trackit_auth";

	@Get
	public String represent() {
		return "hello, world";
	}

	@Post
	public Representation acceptItem(Representation entity) {
		logger.info("Attempting to delete a device");

		Series<Cookie> cookies = this.getRequest().getCookies();
		String parent_username = cookies.getFirstValue(COOKIE_USER);
		String auth_Token = cookies.getFirstValue(COOKIE_AUTH);

		if ((parent_username == null) || (auth_Token == null))
			return null;
		
		User thisUser = new User(parent_username);
		
		//Is this a valid cookie?
		if(!thisUser.validateCookie(auth_Token))
			return null;
		
		//Cookie is valid
		Form form = new Form(entity);
		String device_id = form.getFirstValue("deviceID");
		
		Device thisDevice = new Device(device_id);
		thisDevice.deleteDevice();
		
		return null;
	}
}
