import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import org.restlet.Response;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

public class WebMapResource extends ServerResource {

	@Get("html")
	public Representation represent() {

		StringBuilder builder = new StringBuilder();
		Reader reader;
		
	    try {
	    	File indexFile = new File("resources/map.html");
			FileInputStream fileInput = new FileInputStream(indexFile);
	    	reader = new InputStreamReader(fileInput, "UTF-8");
	        int thisChar = 0;
	        while (thisChar != -1) {
	        	thisChar = reader.read();
	        	builder.append((char) thisChar);
	        }
	        reader.close();
	    } catch (IOException e) {
	        throw new RuntimeException(e);
	    }

	    
	    return new StringRepresentation(builder.toString(), MediaType.TEXT_HTML);
	}

}