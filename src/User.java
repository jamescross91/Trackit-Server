import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.apache.log4j.Logger;

public class User {
	private String userName;
	private String passwordPlain;
	private String passwordHash;
	private String firstName;
	private String lastName;
	private String emailAddress;

	private static Logger logger = Logger.getLogger(DatabaseCore.class);

	public User(String userName) {
		this.userName = userName;
	}

	public boolean login(String password) {
		if(!loadUser()){
			logger.warn("Unable to load information for user: " + userName + " login is declined");
			return false;
		}
		
		try {
			return validatePassword(password, passwordHash);
		} catch (NumberFormatException | NoSuchAlgorithmException
				| InvalidKeySpecException e) {
			logger.error("Error validating user password for user: "
					+ this.userName);
			e.printStackTrace();
		}
		return false;
	}

	public HashMap<String, Object> createUser(String password,
			String firstName, String lastName, String emailAddress) {

		String error = new String();
		HashMap<String, Object> result = new HashMap<String, Object>();
		boolean dbSuccess = false;

		// Attempt to hash the password. If this fails, catch the exception, log
		// it, and return the result hashmap to the calling function
		try {
			passwordHash = createHash(password);
		} catch (Exception e) {
			error = "Error creating password hash for input password: "
					+ password;
			result.put("Created", false);
			result.put("Reason", error);

			logger.error(error);
			e.printStackTrace();

			return result;
		}
		
		String sqlString = "INSERT INTO parent_details(username, password, first_name, last_name, email_address) values(?, ?, ?, ?, ?)";
		LinkedHashMap<Object, String> data = new LinkedHashMap<Object, String>();
		data.put(userName, "string");
		data.put(passwordHash, "string");
		data.put(firstName, "string");
		data.put(lastName, "string");
		data.put(emailAddress, "string");

		try {
			dbSuccess = DatabaseCore.executeSqlUpdate(sqlString, data);
		} catch (Exception e) {
			error = "Error adding parent details to the database, username is: "
					+ userName;
			logger.error(error);
			result.put("Created", false);
			result.put("Reason", error);
			e.printStackTrace();
		}

		result.put("Created", dbSuccess);
		return result;
	}

	// Load the user from the database
	private boolean loadUser() {
		List<HashMap<String, Object>> result = null;
		
		String sqlString = "SELECT * FROM parent_details WHERE username = ?";
		LinkedHashMap<Object, String> data = new LinkedHashMap<Object, String>();
		data.put(userName, "string");
		
		try {
			result = DatabaseCore.executeSqlQuery(sqlString, data);
		} catch (Exception e) {
			logger.error("Error extracting parent details from the database, username is: "
					+ userName);
			e.printStackTrace();
		}

		
		if (result.size() > 1) {
			logger.error("Multiple users found for: " + userName
					+ " something is broken!");
			return false;
		}

		if (result.size() == 0)
			return false;

		HashMap<String, Object> thisUser = result.get(0);
		userName = (String) thisUser.get("username");
		passwordHash = (String) thisUser.get("password");
		firstName = (String) thisUser.get("first_name");
		lastName = (String) thisUser.get("last_name");
		emailAddress = (String) thisUser.get("email_address");

		return true;
	}

	/*
	 * ##########################PASSWORDS#############################
	 * 
	 * To store a password: (1)Generate a long random salt using
	 * java.security.SecureRandom - a crypto secure random number generator. (2)
	 * Add the salt to the beginning of the password, and hash it using a
	 * password based key derivative function (PBKDF2). (3) Prepend the hash
	 * with the salt followed by a colon, and save it.
	 * 
	 * To validate a password input: (1) retrieve the salt/hash combination from
	 * the database. (2) Split them on the colon, and prepend the salt to the
	 * input password. (3) Hash the input password and salt combination and
	 * check this against the hash from the database. If they match, the
	 * password is valid.
	 * 
	 * Various methods and and implementation sourced with thanks from
	 * crackstation.net:
	 * http://crackstation.net/hashing-security.htm#javasourcecode
	 */

	private String createHash(String password) throws NoSuchAlgorithmException,
			InvalidKeySpecException {
		// Generate a random salt for the password hash
		SecureRandom randomSalt = new SecureRandom();
		byte[] salt = new byte[Integer.parseInt(ReadProperties
				.getProperty("salt_bytes"))];
		randomSalt.nextBytes(salt);

		// Hash the password with the salt using a password based key derivation
		// function 2 (RSA)
		byte[] hash = pbkdf2(password.toCharArray(), salt,
				Integer.parseInt(ReadProperties
						.getProperty("pbkdf2_iterations")),
				Integer.parseInt(ReadProperties.getProperty("hash_bytes")));

		// Split the salt and the salted hash so we can easily pivot on this
		// char later
		return toHex(salt) + ":" + toHex(hash);
	}

	// Validate an input password, return true for match or false for fail
	private boolean validatePassword(String password, String goodHash)
			throws NumberFormatException, NoSuchAlgorithmException,
			InvalidKeySpecException {
		char[] pwdArray = password.toCharArray();

		// Split the hash into its parameters
		String[] params = goodHash.split(":");

		byte[] salt = fromHex(params[Integer.parseInt(ReadProperties
				.getProperty("salt_index"))]);
		byte[] hash = fromHex(params[Integer.parseInt(ReadProperties
				.getProperty("pbkdf2_index"))]);

		// hash the test password in the same way
		byte[] testHash = pbkdf2(pwdArray, salt,
				Integer.parseInt(ReadProperties
						.getProperty("pbkdf2_iterations")),
				Integer.parseInt(ReadProperties.getProperty("hash_bytes")));

		if (hash.length != testHash.length)
			return false;

		for (int i = 0; i < hash.length; i++) {
			if (hash[i] != testHash[i])
				return false;
		}

		return true;
	}

	// Creates a PBKDF2 hash of the password string using the random salted
	// provided
	private byte[] pbkdf2(char[] password, byte[] salt, int iterations,
			int bytes) throws NoSuchAlgorithmException, InvalidKeySpecException {
		PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, bytes * 8);
		SecretKeyFactory skf = SecretKeyFactory.getInstance(ReadProperties
				.getProperty("pbkdf2_algo"));

		return skf.generateSecret(spec).getEncoded();
	}

	// Converts our byte array into a hex string
	private String toHex(byte[] array) {
		BigInteger bigInt = new BigInteger(1, array);
		String hex = bigInt.toString(16);
		int paddingLength = (array.length * 2) - hex.length();
		if (paddingLength > 0)
			return String.format("%0" + paddingLength + "d", 0) + hex;
		else
			return hex;
	}

	// Converts our hex into an array of bytes
	private byte[] fromHex(String hex) {
		byte[] binary = new byte[hex.length() / 2];

		for (int i = 0; i < binary.length; i++) {
			binary[i] = (byte) Integer.parseInt(
					hex.substring(2 * i, 2 * i + 2), 16);
		}

		return binary;
	}
}
