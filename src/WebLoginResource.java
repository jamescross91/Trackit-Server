import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.restlet.data.Cookie;
import org.restlet.data.CookieSetting;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;
import org.restlet.util.Series;

public class WebLoginResource extends ServerResource {
	private static Logger logger = Logger.getLogger(WebLoginResource.class);
	private static final String COOKIE_USER = "trackit_user";
	private static final String COOKIE_AUTH = "trackit_auth";

	@Get("html")
	public Representation represent() {

		//Get the cookies
		Series<Cookie> cookies = this.getRequest().getCookies();
		String parent_username = cookies.getFirstValue(COOKIE_USER);
		String auth_Token = cookies.getFirstValue(COOKIE_AUTH);
		
		if((parent_username == null) || (auth_Token == null))
			return getLoginRetryPage();
		
		User thisUser = new User(parent_username);
		
		if(thisUser.validateCookie(auth_Token)){
			//Cookie matches what we expect!
			return getManagementPage();
		}
		
		return getLoginRetryPage();
	}

	@Post
	public Representation acceptItem(Representation entity) {
		System.out.println("Device attempting to log in");
		Form form = new Form(entity);

		String username = form.getFirstValue("user");
		String password = form.getFirstValue("pass");

		User thisUser = new User(username);
		if (!thisUser.login(password))
			return getLoginRetryPage();

		
		// Successful login

		// Delete cookies here
		
		// Set the cookie
		String authToken = thisUser.generateAndStoreCookie();
		CookieSetting csAuth = new CookieSetting(0, COOKIE_AUTH, authToken);
		CookieSetting csUser = new CookieSetting(0, COOKIE_USER, username);
		
		csAuth.setPath("/");
		csUser.setPath("/");
		
		Series<CookieSetting> cookieSettings = this.getCookieSettings();
		cookieSettings.clear();
		cookieSettings.add(csAuth);
		cookieSettings.add(csUser);
		this.setCookieSettings(cookieSettings);

		return getManagementPage();
	}
	
	private StringRepresentation getLoginRetryPage(){
		StringBuilder builder = new StringBuilder();
		Reader reader;
		
		try {
			File indexFile = new File("resources/loginretry.html");
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

		return new StringRepresentation(builder.toString(),
				MediaType.TEXT_HTML);
	}
	
	private StringRepresentation getManagementPage(){
		StringBuilder builder = new StringBuilder();
		Reader reader;
		
		try {
			File indexFile = new File("resources/management.html");
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
