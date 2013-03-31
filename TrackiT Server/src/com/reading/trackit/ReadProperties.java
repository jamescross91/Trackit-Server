package com.reading.trackit;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

//Class holding TrackiT application properties
public class ReadProperties {

	private static Properties trackitProps = null;

	// Constructor loads the properties file into an in memory properties object
	private static void load() {
		try {
			File propsFile = new File("TrackiT.properties");
			FileInputStream fileInput = new FileInputStream(propsFile);

			trackitProps = new Properties();
			trackitProps.load(fileInput);

			fileInput.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Attempt to load the property
	public static String getProperty(String property) {
		if (trackitProps == null)
			load();

		if (trackitProps != null) {
			return trackitProps.getProperty(property);
		} else {
			return null;
		}
	}

}