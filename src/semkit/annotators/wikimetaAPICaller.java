package semkit.annotators;

/*

SemLinker 2014

Copyright (C) 2014  Marie-Jean Meurs & Hayda Almeida
                    Ludovic Jean-Louis & Eric Charton

Copyright (C) 2013  Eric Charton & Marie-Jean Meurs &
                    Ludovic Jean-Louis & Michel Gagnon

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, 
Boston, MA  02110-1301, USA.

Contacts :

This software is maintained and released at:

https://github.com/SemLinker-Team/SemLinker_KBP2014

Please contact respective authors from this page for support
or any inquiries. 

 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;



/**
 *
 * The Wikimeta Extractor is a sample API caller to the REST 
 * interface. You just call it with a text sequence, and
 * it manage all the calls trough the Internet and return
 * you a text.
 *
 * @author descl
 * 
 * Improvements by ericcharton / eric.charton@polymtl.ca
 * 
 */
public class wikimetaAPICaller {

	private boolean getByGet = true;
	
	/**
	 * 
	 * Default values for Span and Threshold
	 * 
	 */
	static int defaultSpan = 100; // default 100 - > 300 = test 10 - performance decreased
	static int  defaultTreshold = 10;

	/**
	 * 
	 * 
	 * 
	 * @author eric
	 * @deprecated
	 */
	public enum Format {

		XML("xml"),
		JSON("json");

		private final String value;

		Format(String value) {this.value = value;}

		public String getValue() {return this.value;}
	}

	
	/**
	 * 
	 * Direct call with standard parameters
	 * 
	 * @param uri
	 * @param apiKey
	 * @param format
	 * @param content
	 * @param lng
	 * @param stats
	 * @return
	 */
	public static String getResult(String uri,String apiKey, Format format, String content, String lng) {
		return getResultCall(uri, apiKey, format, content, defaultTreshold, defaultSpan , lng ,true, 0);
		// return getResultCallByPost(uri, apiKey, format, content, defaultTreshold, defaultSpan , lng ,true, 0);
	}

	
	/**
	 * 
	 * @param uri
	 * @param apiKey
	 * @param format
	 * @param content
	 * @param lng
	 * @return
	 */
	public static String getResultFullCall(String uri,String apiKey, Format format, String content, String lng, boolean semtag, int stats) {
		return getResultCall(uri, apiKey, format, content, defaultTreshold, defaultSpan , lng , semtag , stats);
		// return getResultCallByPost(uri, apiKey, format, content, defaultTreshold, defaultSpan , lng , semtag , stats);
	}

	
	/**
	 * 
	 * 
	 * @param uri
	 * @param apiKey
	 * @param format
	 * @param content
	 * @param treshold
	 * @param span
	 * @param lng
	 * @param semtag
	 * @param stats
	 * @return
	 */
	private static String getResultCall(String uri, String apiKey, Format format, String content, int treshold, int span, String lng, boolean semtag, int stats) {    

		String result = "";
		String callFormat = format.value;

		try {
			URL url = new URL(uri);

			HttpURLConnection server = (HttpURLConnection)url.openConnection();

			server.setDoInput(true);
			server.setDoOutput(true);
			server.setRequestMethod("POST");
			server.setRequestProperty("Accept", callFormat );
			server.setAllowUserInteraction(false);


			server.connect();

			BufferedWriter bw = new BufferedWriter(
					new OutputStreamWriter(
							server.getOutputStream()));


			String semtagS = "0";
			if(semtag)semtagS = "1";

			String request = "treshold="+treshold+"&span="+span+"&lng="+lng+"&semtag="+semtagS+"&api="+apiKey+"&textmining="+stats+"&contenu="+content;

			bw.write(request, 0, request.length());
			bw.flush();
			bw.close();

			//send query
			/*PrintStream ps = new PrintStream(server.getOutputStream());
            ps.print(request);
            ps.close();
			 */
			InputStreamReader input = new InputStreamReader(server.getInputStream());
			BufferedReader reader = new BufferedReader(input);
//			BufferedReader reader = new BufferedReader(new InputStreamReader(server.getInputStream()));
			
			String ligne;
			while ((ligne = reader.readLine()) != null) {
				result += ligne+"\n";			
			}
									
			reader.close();
			server.disconnect();
		}
		catch (Exception e)
		{
			Logger.getLogger(wikimetaAPICaller.class.getName()).log(Level.SEVERE, null, e);
		}
		return result;
	}
	
	/**
	 * 
	 * 
	 * 
	 * @param uri
	 * @param apiKey
	 * @param format
	 * @param content
	 * @param treshold
	 * @param span
	 * @param lng
	 * @param semtag
	 * @param stats
	 * @return
	 */
	public static String getResultCallByGet(String uri, String apiKey, Format format, String content, int treshold, int span, String lng, boolean semtag, int stats)  {

		String result = "";
		String callFormat = format.value;

		try {
			URL url = new URL(uri);

			HttpURLConnection server = (HttpURLConnection)url.openConnection();

			server.setDoInput(true);
			server.setDoOutput(true);
			server.setRequestMethod("GET");
			server.setRequestProperty("Accept", callFormat );
			server.setAllowUserInteraction(false);


			server.connect();

			BufferedWriter bw = new BufferedWriter(
					new OutputStreamWriter(
							server.getOutputStream()));


			String semtagS = "0";
			if(semtag)semtagS = "1";

			String request = "treshold="+treshold+"&span="+span+"&lng="+lng+"&semtag="+semtagS+"&api="+apiKey+"&textmining="+stats+"&contenu="+content;

			bw.write(request, 0, request.length());
			bw.flush();
			bw.close();

			//send query
			/*PrintStream ps = new PrintStream(server.getOutputStream());
            ps.print(request);
            ps.close();
			 */
			BufferedReader reader = new BufferedReader(new InputStreamReader(server.getInputStream()));

			String ligne;
			while ((ligne = reader.readLine()) != null) {
				result += ligne+"\n";
			}

			reader.close();
			server.disconnect();
		}
		catch (Exception e)
		{
			Logger.getLogger(wikimetaAPICaller.class.getName()).log(Level.SEVERE, null, e);
		}
		return result;
	

}


}
