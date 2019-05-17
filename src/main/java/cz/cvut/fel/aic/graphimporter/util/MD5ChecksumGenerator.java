package cz.cvut.fel.aic.graphimporter.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * This class can generate a checksum for a given file. That is made by computing an MD5 has for the file
 * and then taking only some of the characters (8-16) of it's hexadecimal representation as the actual checksum.
 * Currently, this is used during graph import to determine, whether the serialized file fits the input graph.
 *
 * @author Michal Cvach
 */
public class MD5ChecksumGenerator {

	public static String getGraphChecksum(File graphFile) {
		String wholeChecksum = getMD5FileChecksum(graphFile);

		return wholeChecksum.substring(8, 16);
	}

	private static String getMD5FileChecksum(File graphFile) {
		MessageDigest md5Digest = null;
		try {
			md5Digest = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}

		String checksum = getFileChecksum(md5Digest, graphFile);

		return checksum;
	}

	private static String getFileChecksum(MessageDigest digest, File file)
	{

		FileInputStream fis;
		try {
			fis = new FileInputStream(file);

			byte[] byteArray = new byte[1024];
			int bytesCount = 0;

			while ((bytesCount = fis.read(byteArray)) != -1) {
				digest.update(byteArray, 0, bytesCount);
			};

			fis.close();

			byte[] bytes = digest.digest();

			StringBuilder sb = new StringBuilder();
			for(int i=0; i< bytes.length ;i++)
			{
				sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
			}

			return sb.toString();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return "";
	}
}
