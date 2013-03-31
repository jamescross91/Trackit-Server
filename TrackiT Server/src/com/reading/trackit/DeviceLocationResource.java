package com.reading.trackit;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import org.restlet.data.Form;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

public class DeviceLocationResource extends ServerResource {

	@Get
	public String represent() {
		return "42";
	}

	@Post
	public Representation acceptItem(Representation entity) {
		Representation result = null;
		Form form = new Form(entity);

		double lat = Double.parseDouble(form.getFirstValue("latitude"));
		double lng = Double.parseDouble(form.getFirstValue("longitude"));
		double accuracy = Double.parseDouble(form.getFirstValue("accuracy"));
		double altitude = Double.parseDouble(form.getFirstValue("altitude"));
		// String provider = form.getFirstValue("provider");
		double bearing = Double.parseDouble(form.getFirstValue("bearing"));
		double speed = Double.parseDouble(form.getFirstValue("speed"));
		String device_id = form.getFirstValue("deviceID");
		String auth_token = form.getFirstValue("authToken");
		// int battery = Integer.parseInt(form.getFirstValue("battlevel"));
		// boolean charging =
		// Boolean.parseBoolean(form.getFirstValue("charging"));

		String provider = "";
		int battery = 10;
		boolean charging = true;

		Device thisDevice = new Device(device_id);
		thisDevice.loadDevice();
		if (thisDevice.authenticateToken(auth_token)) {

		DeviceLocation locInput = new DeviceLocation(thisDevice, lat,
				lng, provider, altitude, accuracy, bearing, battery, charging,
				"guff", "3g", speed);
		locInput.persistLocation();

		AlertsManager manager = new AlertsManager();
		manager.setLocation(locInput);
		manager.processAlerts();

		System.out.println("My current latitude is " + lat
				+ " and longitude is " + lng + "\n");
		}

		return (result);
	}

}