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

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import configure.SemkitConfiguration;

/**
 * 
 * This class call an annotation API (e.g. Wikimeta or Aida) and prepare the annotations according to the format
 * retrieved. It then return it to the decoder.<br>
 * <br>
 * There is two main ideas behind this class :<br>
 *   1) Select between the compatibles annotators (currently AIDA and Wikimeta, we might add others later).
 *      this avoid the need of an extra interface or unnecessary complexity (we will never have more than 
 *      few annotators in SemLinker)<br>
 *   2) Splitting and prepared the documents if needed, before transmitting then to the annotator. 
 *      This is mandatory with some NIST KBP docs, for example, when the document size exceed the capacities of 
 *      the REST API. <br>
 * <br>
 * To implement a new annotator, you will need to integrate it in this class and then to build a decoder using 
 * the annotationInterface in the package semkit.annotatedobjects.
 * 
 * @author eric
 *
 */
public class AnnotationExtractor {

	
	/** Define key or account reference of annotator, can be set to null (as value or string) if there is no account reference */
    private String apikey ;
    /** Define uri to call annotator */
    private String apiuri ;
    /** Define the name of annotator. Valid values are aida or wikimeta */
    private String annotator ;
	
	/** Configuration definition */
    private SemkitConfiguration vars ;
    private boolean verbose = true;
	
	// modifier en static
	private wikimetaAPICaller SampleCaller; 
	
	/**
	 * 
	 * Constructor 
	 * 
	 * @param uriToApi
	 */
	public AnnotationExtractor(SemkitConfiguration semkitConfiguration){
		
		// Variables used to select and configure API caller
		apikey = semkitConfiguration.APIAccount ; 
		annotator = semkitConfiguration.annotatorname;
		apiuri = semkitConfiguration.APIRestURi;
	}
	
	/**
	 * 
	 * Constructor overriding the annotator of the config
	 * 
	 * @param uriToApi
	 */
	public AnnotationExtractor(SemkitConfiguration semkitConfiguration, String annotatorname){
				
		// Variables used to select and configure API caller
		apikey = semkitConfiguration.APIAccount ; 
		apiuri = semkitConfiguration.APIRestURi;
		
		annotator = annotatorname; // in this constructor, the config.cfg is overrided
	}
	
	
	/**
	 * 
	 * Retrieve annotation according to the correct annotator (currently AIDA or Wikimeta)
	 * 
	 * @param document
	 * @return
	 */
	public String getAnnotations(String document){
		
		String annotatedDocument = null;
		
		// if wikimeta is selected
		if (vars.annotatorname.contains("aida")){
			annotatedDocument = getAidaAnnotations(document);
		}else{
		    // Wikimeta is default selected
			annotatedDocument = getWikimetaAnnotations(document);
		}
		return annotatedDocument ;
		
	}
	
	
	//---------------------------------------------
	//
	// This implements calls to annotators
	//
	//---------------------------------------------
	
	/**
	 * 
	 * Retrieve the JSON annotation from AIDA.
	 * 
	 * @param document
	 * @return
	 */
	private String getAidaAnnotations(String document){
		
		String result = aidaAPICaller.getResult( apiuri , document);
	
		return result;
	}
	
	
	/**
	 * 
	 * Retrieve the XML from Wikimeta.<br> 
	 * <br>
	 * If the document is larger that what Wikimeta can retrieve, transform it in 
	 * a suite of CDATA. Those aggregated CDATA will be transfered to an annotation
	 * object and transformed into standardized annotated object. 
	 * 
	 * @param document
	 * @return
	 */
	private String getWikimetaAnnotations(String document){
		
		String result = "";
		int lengthofsplit = 15000;
//		int lengthofsplit = 30000;
		// divide document in an array
		if (document.length() > 50000){
//		if (document.length() > 60000){
			
			  
				// make sized sequential annotations
		        int fin = lengthofsplit; int deb = 0;
		        while(fin < document.length()){
		        	
		        	if (verbose) System.out.println("->Annotation extractor " + deb + " " + fin + " / " + document.length());
		        	result = result + wikimetaAPICaller.getResult( apiuri, apikey, wikimetaAPICaller.Format.XML, document.substring(deb, fin)  , "EN");
		        	
		        	deb = fin + 1;
		        	fin = fin + lengthofsplit;
		        	
		        	if (fin > document.length() ) 
		        	{ 
		        		if (deb < document.length() ){
		        			if (verbose) System.out.println("->*Annotation extractor " + deb + " " + fin + " / " + document.length());
		        			result = result + wikimetaAPICaller.getResult( apiuri, apikey, wikimetaAPICaller.Format.XML, document.substring(deb,  document.length() )  , "EN");
		        		}
		        	}
		        }
		        
		        //-----------------------
		        // rebuild the XML
		        // and aggregate the CDATA
		        //-----------------------
		       
		        String textarray[] = result.split("\n"); 
		        String newresult = "<![CDATA[\n";
		        int y = 0;
		        
		    	// collect CDATA tags
		    	for (int x = 0; x < textarray.length; x++){
		    		if (textarray[x].contains("<![CDATA[")){	
		    			y = x + 1; // go just after CDATA
		    			// accumulate the CDATA
		    			String provisoireresult = ""; 
		    			while(! textarray[y].contains("]]>")){
		    				
		    					provisoireresult = provisoireresult.concat(textarray[y]);
		    					provisoireresult = provisoireresult.concat("\n");
		    					
		    					y++; 
		    					if (y > textarray.length) break;
		    			}
		    			x = y + 1 ;
		    			newresult = newresult.concat(provisoireresult);
		    		
		    		}
		    	}
		    	newresult = newresult + "]]>\n";
		    	result = newresult; 
		    	
		        
		}else{
			
			result = wikimetaAPICaller.getResult(apiuri, apikey, wikimetaAPICaller.Format.XML, document , "EN");
		}
		
		return result;
		
	}
	
	public String eliminateQuotes(String document){
		
		Document doc = Jsoup.parseBodyFragment(document);
		//saving the text as an Jsoup element, with a main tag (the HTML body), 
		//attributes and child nodes (TRIAGE tags)
		Element text = doc.body();
		for( Element element : text.select("QUOTE PREVIOUSPOST")){
			element.remove();
		}
		
		String result = text.toString();
		result = result.replaceAll("<body>\n ", "");
		result = result.replaceAll("</body>", "");
		result = result.replaceAll(" \n ", "\n");
		result = result.replaceAll(" \n  ", "\n");
		result = result.replaceAll("\n  ", "\n");
		result = result.replaceAll("\n ", "\n");
		//result = result.replaceAll("\n  ", "\n");
	
		return result;	
		
	}
	

}
