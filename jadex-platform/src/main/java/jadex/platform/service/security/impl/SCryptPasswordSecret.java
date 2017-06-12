package jadex.platform.service.security.impl;

import java.util.logging.Logger;

import org.spongycastle.crypto.generators.SCrypt;

import jadex.commons.SUtil;

/**
 *  A secret password used for authentication using the scrypt KDF.
 *
 */
public class SCryptPasswordSecret extends SharedSecret
{
	/** Prefix used to encode secret type as strings.*/
	public static final String PREFIX = "scrypt";
	
	/** Password length warning threshold. */
	protected static final int MIN_PASSWORD_LENGTH = 12;
	
	/** SCrypt work factor / hardness for password strengthening. */
	protected static final int SCRYPT_N = 16384;
	
	/** SCrypt block size. */
	protected static final int SCRYPT_R = 8;
	
	/** SCrypt parallelization. */
	protected static final int SCRYPT_P = 4;
	
	/** The password. */
	protected String password;
	
	/**
	 *  Creates the secret.
	 */
	public SCryptPasswordSecret()
	{
	}
	
	/**
	 *  Creates the secret.
	 */
	public SCryptPasswordSecret(String encodedpassword)
	{
		int ind = encodedpassword.indexOf(':');
		String prefix = encodedpassword.substring(0, ind);
		if (!PREFIX.startsWith(prefix))
			throw new IllegalArgumentException("Not a password secret: " + encodedpassword);
		this.password = encodedpassword.substring(ind + 1);
		
		if (password.length() < MIN_PASSWORD_LENGTH)
			Logger.getLogger("sharedsecret").warning("Weak password detected: + " + password + ", please use at least " + MIN_PASSWORD_LENGTH + " random characters.");
	}
	
	/**
	 *  Gets the password.
	 *  
	 *  @return The password.
	 */
	public String getPassword()
	{
		return password;
	}
	
	/**
	 *  Sets the password.
	 *  
	 *  @param password The password.
	 */
	public void setPassword(String password)
	{
		this.password = password;
	}
	
	/**
	 *  Derives a key from the password with appropriate hardening.
	 *  
	 *  @param keysize The target key size in bytes to generate.
	 *  @param salt Salt to use.
	 *  @return Derived key.
	 */
	public byte[] deriveKey(int keysize, byte[] salt)
	{
		return SCrypt.generate(password.getBytes(SUtil.UTF8), salt, SCRYPT_N, SCRYPT_R, SCRYPT_P, keysize);
	}
	
	/** 
	 *  Creates encoded secret.
	 *  
	 *  @return Encoded secret.
	 */
	public String toString()
	{
		return PREFIX + ":" + password;
	}
}
