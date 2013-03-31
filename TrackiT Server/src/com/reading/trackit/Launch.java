package com.reading.trackit;
import org.restlet.Component;
import org.restlet.Server;
import org.restlet.data.Parameter;
import org.restlet.data.Protocol;
import org.restlet.util.Series;

public class Launch {
	public static void main(String[] args) throws Exception {

		//Create a new Component.
		Component component = new Component();
		
		//Allow file handling
		component.getClients().add(Protocol.FILE);
		
		//Create a new HTTPS server listening on port 2620.
		Server server = component.getServers().add(Protocol.HTTPS, 2610);
		Series<Parameter> params = server.getContext().getParameters(); 
		
		params.add("sslContextFactory", "org.restlet.ext.ssl.PkixSslContextFactory");
		params.add("keystorePath", "server.jks");
		params.add("keystorePassword", "password");
		params.add("keyPassword", "password");
		params.add("keystoreType", "JKS");

		//Attach the sample application.
		component.getDefaultHost().attach("/device", new LocationApplication());
		component.getDefaultHost().attach("/web", new WebApplication());

		//Start the component.
		component.start();
	}
}
