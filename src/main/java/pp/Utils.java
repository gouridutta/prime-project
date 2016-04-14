package pp;

import java.io.*;

public class Utils
{
	public static byte[] getBytesFromStream(InputStream in) throws IOException
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		copyStream(in, out);
		return out.toByteArray();
	}

	public static void copyStream(InputStream in, OutputStream out) throws IOException
	{
		byte[] buf = new byte[ 1024 * 4 ];
		while( true )
		{
			int bytesRead = in.read(buf);
			if( bytesRead == -1 )
				break;

			out.write(buf, 0, bytesRead);
		}
	}

	/** @return the extension of the file */
	public static String getExt(String filename)
	{
		final int pos = filename.lastIndexOf(".");
		if( pos == -1 || pos + 1 == filename.length() )
			return "";
		else
			return filename.substring(pos+1);
	}

	/** Attempts to guess the mime type of a file based on file extension */
	public static String guessMimeType(String filename)
	{
		switch( getExt(filename) )
		{
			case "html": case "htm": return "text/html";
			case "css": return "text/css";
			case "js": return "text/javascript";
			default: return "text/plain";
		}
	}
}