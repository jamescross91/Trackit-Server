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
		
		if(auth_Token.compareTo(loadCookie(parent_username)) == 0){
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

		// Set the cookie
		String authToken = generateToken();
		CookieSetting csAuth = new CookieSetting(0, COOKIE_AUTH, authToken);
		CookieSetting csUser = new CookieSetting(0, COOKIE_USER, username);
		Series<CookieSetting> cookieSettings = this.getCookieSettings();
		cookieSettings.clear();
		cookieSettings.add(csAuth);
		cookieSettings.add(csUser);
		this.setCookieSettings(cookieSettings);

		// Store the cookie in the database
		storeCookie(username, authToken);

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
	
	private String loadCookie(String parent_username){
		List<HashMap<String, Object>> result = null;

		String sqlString = "SELECT * FROM web_cookies WHERE parent_username = ?";
		LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
		data.put("parent_username", parent_username);

		try {
			result = DatabaseCore.executeSqlQuery(sqlString, data);
		} catch (Exception e) {
			logger.error("Error extracting web cookie from the database, parent_username is: "
					+ parent_username);
			e.printStackTrace();
		}

		if (result.size() > 1) {
			logger.error("Multiple cookies found for: " + parent_username
					+ " something is broken!");
			return new String();
		}

		if (result.size() == 0)
			return new String();

		// Check the username provided against the database
		HashMap<String, Object> thisDevice = result.get(0);

		return (String) thisDevice.get("cookie");
	}

	private void storeCookie(String parent_username, String cookie) {
		String sqlString = "INSERT INTO web_cookies(parent_username, cookie) values(?, ?) ON DUPLICATE KEY UPDATE cookie=?";

		LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
		data.put("parent_username", parent_username);
		data.put("cookie", cookie);
		data.put("cookie1", cookie);

		try {
			DatabaseCore.executeSqlUpdate(sqlString, data);
		} catch (Exception e) {
			logger.error("Error inserting web cookie into the database for parent username: "
					+ parent_username);
		}
	}

	private String generateToken() {
		// Create a random authentication token to be used with the device, in
		// the same way we create a salt for passwords
		SecureRandom randomSalt = new SecureRandom();
		byte[] salt = new byte[Integer.parseInt(ReadProperties
				.getProperty("salt_bytes")) * 2];
		randomSalt.nextBytes(salt);

		BigInteger bigInt = new BigInteger(1, salt);
		String hex = bigInt.toString(16);
		int paddingLength = (salt.length * 2) - hex.length();
		if (paddingLength > 0)
			return String.format("%0" + paddingLength + "d", 0) + hex;
		else
			return hex;
	}
}
