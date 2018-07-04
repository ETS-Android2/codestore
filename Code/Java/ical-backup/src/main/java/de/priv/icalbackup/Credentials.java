package de.priv.icalbackup;


import lombok.Cleanup;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.crypto.stream.CryptoInputStream;
import org.apache.commons.crypto.stream.CryptoOutputStream;
import org.apache.commons.io.IOUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.PostConstruct;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Component
public class Credentials
{
	private SecretKey key;
	private IvParameterSpec iv;
	private Map<String, byte[]> credentials;

	@Getter
	private boolean initialized = false;

	@PostConstruct
	private void generateSecretKey() throws Exception
	{
		KeyGenerator keyGen = KeyGenerator.getInstance("AES");
		keyGen.init(256);
		this.key = keyGen.generateKey();

		byte[] bytes = new byte[256];
		new SecureRandom(){{nextBytes(bytes);}};
		this.iv = new IvParameterSpec(bytes);
	}

	private byte[] encrypt(String data) throws Exception
	{
		@Cleanup
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		@Cleanup
		CryptoOutputStream cos = new CryptoOutputStream("AES/CBC/PKCS5Padding", new Properties(), outputStream, this.key, this.iv);
		cos.write(data.getBytes(StandardCharsets.UTF_8));
		cos.flush();

		return outputStream.toByteArray();
	}

	private String decrypt(byte[] data) throws Exception
	{
		@Cleanup
		InputStream inputStream = new ByteArrayInputStream(data);
		@Cleanup
		CryptoInputStream cis = new CryptoInputStream("AES/CBC/PKCS5Padding", new Properties(), inputStream, this.key, this.iv);

		return IOUtils.toString(cis,StandardCharsets.UTF_8);
	}

	public void setCredentials(Map<String, String> credentials) throws Exception
	{
		this.credentials = new HashMap<>();

		for (Map.Entry<String,String> entry : credentials.entrySet())
		{
			this.credentials.put(entry.getKey(), this.encrypt(entry.getValue()));
		}

		this.initialized = true;
	}

}