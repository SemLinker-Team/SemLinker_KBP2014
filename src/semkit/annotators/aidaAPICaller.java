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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;

import semkit.annotatedobjects.AidaJSONDecoder;
import semkit.annotatedobjects.AnnotationInterface;

/**
 * 
 * This is the static class to call an API annotator
 * 
 * 
 * @author eric
 *
 */
public class aidaAPICaller {

	// curl --data text="Barack Obama lives in DC" http://mining.fungalgenomics.ca/aida/service/disambiguate-defaultsettings
	// curl --data text="They met Paris Hilton in Denver" --data tech="LOCAL" --data maxResults="15" http://mining.fungalgenomics.ca/aida/service/disambiguate-defaultsettings
	
	/**
	 * 
	 * This is to test AIDA
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		 
		 System.out.println("Testing AIDA");
		 String doc = "He became a monk at Cluny Abbey and was appointed Abbot of Gloucester in 1139. During the papal council at Reims in 1148, he was appointed Bishop of Hereford by Pope Eugene III. Despite promising not to recognise King Stephen, on returning to England Foliot nevertheless swore fealty to him, causing a temporary rift in his relationship with Henry of Anjou, the son of the Empress Matilda (Stephen's rival for the crown) who became King Henry II of England in 1154. Henry passed over Foliot for the position of Archbishop of Canterbury in 1162, appointing Thomas Becket instead.";
		 String result =  getResult("http://mining.fungalgenomics.ca/aida/service/disambiguate-defaultsettings", doc);
	
		 // printing the JSON
		 System.out.println("Doc:" + result ); 
		 
		 // Storing in an object
		 AnnotationInterface test = new AidaJSONDecoder();
		 
//		 test.decoder(result, 1);
		 
	}

	
	/**
	 * 
	 * 
	 * 
	 * 
	 * @param apiuri
	 * @param document
	 * @return
	 */
	public static String getResult(String apiuri, String document) {

			String result = "";
		    String url = apiuri;
	        InputStream in = null;

	        try {
	            HttpClient client = new HttpClient();
	            PostMethod method = new PostMethod(url);

	            //Add any parameter if u want to send it with Post req.
	            method.addParameter("text", document);
	            
	            // add parameters to obtain multiple candidates
	            method.addParameter("tag_mode", "stanfordNER");
	            method.addParameter("tech", "LOCAL");
	            //method.addParameter("tech", "GRAPH");
	            method.addParameter("interface", "TRUE");
	            method.addParameter("maxResults", "15");
	            method.addParameter("fast_mode","TRUE");

	            int statusCode = client.executeMethod(method);

	            if (statusCode != -1) {
	                in = method.getResponseBodyAsStream();
	            }

	            
		        // this long string conversion is to avoid Warning from httpclient:
		        // ATTENTION: Going to buffer response body of large or unknown size. Using getResponseBodyAsStream instead is recommended.
		        // see https://groups.google.com/forum/#!topic/typica/hzvDKNMs534
		        InputStreamReader in2 = new InputStreamReader(method.getResponseBodyAsStream(), "UTF-8");
		        StringWriter sw = new StringWriter();
		        int x;
		        while((x = in2.read()) != -1){
		            sw.write(x);
		        }
		        in2.close();
		        result = sw.toString();
		        
		        // this is the alternative method using AsString
		        // result = method.getResponseBodyAsString();
			       
		        
	        } catch (Exception e) {
	            e.printStackTrace();
	        }

		
		
		return result;
		
	
	}
	
	
}
