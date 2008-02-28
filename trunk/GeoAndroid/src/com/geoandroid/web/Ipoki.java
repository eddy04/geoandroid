package com.geoandroid.web;

import java.io.*;

import com.geoandroid.*;

import java.net.MalformedURLException;
import java.net.URL; 
import java.net.HttpURLConnection;
import java.io.IOException;
import java.io.InputStream;


import android.graphics.Bitmap;
import android.util.Log;
	
public class Ipoki {

	private static final String urlConnect = "http://www.ipoki.com/signin.php";
	private static final String urlDisconnect = "http://www.ipoki.com/signout.php";
	private static final String urlSetPosition = "http://www.ipoki.com/ear.php";
	private static final String urlGetPosition = "http://www.ipoki.com/readposition.php";
	private static final String urlFriends = "http://www.ipoki.com/myfriends.php";
	
	public static int sendWebReg(String user, String pass) throws IOException
	{ 
		String url = urlConnect + "?user="+user+"&pass="+pass;
		String message = sendWebRequestString(url);
		int result = WebResult.UNKNOWN_MESSAGE_TYPE;
		
		String[] messages = parseMessage(message);

		if (messages.length == 0)
			return WebResult.BAD_RESPONSE;
		
		String messageType = messages[0];
		
		// Si el tipo es CODIGO, obtenemos el IdSeguro
		if (messageType.equals("CODIGO"))
		{
			State.secureId = messages[1];
			System.out.println("WebReg CODIGO: " + State.secureId);
			if (State.secureId.equals("ERROR"))
			{
				return WebResult.CODE_ERROR;
			}
			State.connected = true;
			result = WebResult.CODE_OK;
		}
		
		if (messageType.equals("AVISO"))
		{
			System.out.println("WebReg AVIS: " + messages[1]);
			if (messages.length != 8)
			{
				return WebResult.ALERT_ERROR;
			}
			
			Alert.Text = messages[1];
			Alert.Url = messages[2];
			Alert.Latitude = messages[3];
			Alert.Longitude = messages[4];
			Alert.Distance = messages[5];
			Alert.Login = messages[6];
			Alert.IsPositional = messages[7].equals("S");
			result = WebResult.ALERT_OK;
			
		}
		
		return result;
	}
	
	public static Bitmap sendWebRequestImage(String url) throws IOException
	{
		Bitmap image = null;
		/*HttpConnection c = null;
		InputStream is = null;

		try
		{
			c = (HttpConnection)Connector.open(url);

			int rc = c.getResponseCode();
			if (rc != HttpConnection.HTTP_OK )
			{
				throw new IOException("Error HTTP:" + rc);
			}

			is = c.openInputStream();

			image = Image.createImage(is);
		}
		catch (ClassCastException e)
		{
			System.out.println(e.getMessage());
			e.printStackTrace();
			throw new IllegalArgumentException("Not an HTTP URL");
		}
		catch(Exception ex)
		{
			System.out.println(ex.getMessage());
			ex.printStackTrace();
		}
		finally
		{
			if ( is != null )
				is.close();
			if ( c != null )
				c.close();
		}
		*/
		return image;	
	}
	
	private static String sendWebRequestString(String urlString) throws IOException
	{
		HttpURLConnection con = null;
		URL url = null;
		InputStream in = null;
        OutputStream out;
        byte[] buff;
		StringBuffer str = new StringBuffer();
		String content = "application/x-www-form-urlencoded";
		
		try
		{

	        url = new URL(urlString);
	        con = (HttpURLConnection) url.openConnection();
	        
	        con.setRequestMethod("POST");
	        con.setDoOutput(true);
	        con.setDoInput(true);
	        con.connect();
	        out = con.getOutputStream();
	        buff = content.getBytes("UTF8");
	        out.write(buff);
	        out.flush();
	        out.close();
	        in = con.getInputStream(); 
	        
			int actual = -1;
			while ((actual = in.read()) != -1)
			{
				str.append((char)actual);
			}
			
			if (str.length() == 0)
				throw new IOException("Unexpected error: empty response.");
		}
		catch (ClassCastException e)
		{
			throw new IllegalArgumentException("Not an HTTP URL");
		}
		finally
		{
			if ( in != null )
				in.close();
			if ( con != null )
				con.disconnect();
		}
		
		return str.toString();
	}
	
	public static void sendWebPos(String lat, String lon) throws IOException
	{
		String url = urlSetPosition + "?iduser=" + State.secureId + "&lat=" + lat + "&lon=" + lon;
		sendWebRequestString(url);
	}
	
	public static void sendWebDisconnection() throws IOException
	{
		String url = urlDisconnect + "?iduser=" + State.secureId;
		sendWebRequestString(url);
	}
	
	private static String[] parseMessage(String message)
	{
		java.util.Vector<String> messages = new java.util.Vector<String>();

		while (message.indexOf("$$$") != -1)
		{
			messages.addElement(message.substring(0, message.indexOf("$$$")));
			message = message.substring(message.indexOf("$$$") + 3);
		}
		
		String[] result = new String[messages.size()];
		messages.copyInto(result);
		return result;
	}
}
