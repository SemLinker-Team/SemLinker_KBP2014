package semkit.annotatedobjects;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import kbp2014.managedocuments.LanguageSpellChecker;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;



/**
 * 
 * This is a annotated object for JSON format used with AIDA
 * 
 * 
 * 
 * @author eric
 *
 */
public class AidaJSONDecoder implements AnnotationInterface {

	
	private boolean verbose = false; // use only to debug
	
	// index by line of CDATA
	private HashMap<Integer, String> WordEntries = new HashMap<Integer, String>();
	private HashMap<Integer, String> WordEntriesNormalized = new HashMap<Integer, String>();
	private HashMap<Integer, String> POSEntries = new HashMap<Integer, String>();
	private HashMap<Integer, String> NEEntries = new HashMap<Integer, String>();
	
	/**
	 * Maps to hold original offset of  
	 * each word and the document ID (HA)
	 */
	private HashMap<Integer, Integer> BegOffset = new HashMap<Integer, Integer>();
	private HashMap<Integer, Integer> EndOffset = new HashMap<Integer, Integer>();
	private HashMap<Integer, String> DocID = new HashMap<Integer, String>();
	
	
	private HashMap<Integer, HashMap<Integer, String>> MetadataUriByLine = new HashMap<Integer, HashMap<Integer, String>>();
	private HashMap<Integer, HashMap<Integer, String>> MetadataKeyByLine = new HashMap<Integer, HashMap<Integer, String>>();
	
	private HashMap<Integer, String> LINKEDDATA = new HashMap<Integer, String>();
	private HashMap<Integer, String> fullSequence = new HashMap<Integer, String>(); // the sequence annotated
	private HashMap<Integer, String> fullSequenceNormalized = new HashMap<Integer, String>(); // the sequence annotated
	
	

	@Override
	public void decoder(String jsonfromapi, String rawText, int numberofcandidates, LanguageSpellChecker spellchecker) {
	
		// hashtable to store the metadata reference
		HashMap<String, String> urientries = new HashMap<String , String>();
		// hashtable to store correspondance between entities and annotation
		HashMap<Integer, Integer> offsetstoknow = new HashMap<Integer, Integer>();
		
		String documentID = "";
		if(StringUtils.containsIgnoreCase(rawText,"<DOCID>")) documentID = StringUtils.substringBetween(rawText, "<DOCID> ", " </DOCID>");
		if(StringUtils.containsIgnoreCase(rawText, "doc id")) documentID = StringUtils.substringBetween(rawText, "<doc id=\"", "\">");
		if(StringUtils.contains(rawText, "DOC id")) documentID = StringUtils.substringBetween(rawText, "<DOC id=\"", "\"");
		String punctuations = ",.;:";
		
		// collect all the lines content

		try {
			
			// prepare the JSON objects
			JSONTokener JSONTokenized = new JSONTokener(jsonfromapi); // tokenize string
			JSONObject jObject = new JSONObject(JSONTokenized); // build JSONObject
			
			//-----------------------------------
			// Collect the metadata informations
			//-----------------------------------
			JSONObject entityMetadata = jObject.getJSONObject("entityMetadata");
		
			//-----------------------------------
			// Collect the lexical informations
			//-----------------------------------fullSequence
			
			JSONArray ArrayjTokens = jObject.getJSONArray("tokens"); // get doc
			
			/**
			 * Counter to keep track of last word checked 
			 * on original text. It serves as starting 
			 * point when searching for next word (HA).
			 */
			int origTextCounter = 0;

			for (int i = 0; i < ArrayjTokens.length(); i++) {

				// get an object
				JSONObject entry = ArrayjTokens.getJSONObject(i);
				
				// Load the informations concerned at the given position
				int position =  Integer.parseInt(entry.getString("stanfordId"));
				int realoffsetbeginat = Integer.parseInt(entry.getString("beginIndex"));
				String word = entry.getString("original");
				String partofspeech = entry.getString("POS");
				String namedentity = entry.getString("NE");
				
				/**
				 * holds the name of annotated entity (HA)
				 */					
				String currentEntity = word;
				String originalText = rawText;
				int[] originalPosition = new int[2];
				
				// verbose
				if (verbose) System.out.println(position + " " + word + " " + partofspeech + " " + namedentity + " " + realoffsetbeginat);
				
				// store words
				WordEntries.put(position, word); 
				WordEntriesNormalized.put(position, word.toLowerCase()); 
				POSEntries.put(position, partofspeech); 
				
								
				// introduce the Part of Speech Punctuation
				if (partofspeech.matches("[\\.;:\\!\\?]")){
					POSEntries.put(position, "SENT"); 
				}
				
				
				// Store normalized NE -> PERS / ORG / LOC 
				String NE = "UNK"; // default value
				if ( namedentity.contains("PERSON")) NE ="PERS";
				if ( namedentity.contains("ORGANIZ")) NE ="ORG";
				if ( namedentity.contains("LOCATION")) NE ="LOC"; // not converted to GSP here
				NEEntries.put(position, NE); 
				
				/**
				 * Find the offset of annotation in original text 
				 * (HA).
				 */			
				if(/*!Pattern.matches("\\p{Punct}", currentEntity) 
						&& */ Pattern.matches("[a-zA-Z]*", currentEntity) 
						&& currentEntity.length() > 1) {
				//if( !(punctuations.contains(currentEntity)) && (currentEntity.length() == 1)) {							
				//	if(!(currentEntity.replaceAll(punctuations, "").matches("^[0-9]+$"))){
						originalPosition = offsetFinder.findOriginalOffset(origTextCounter, currentEntity, originalText, spellchecker);	
				//	}
					if(originalPosition[1] > 0) origTextCounter = originalPosition[1];
				}
			//	}
								
				/**
				 * Inserting offsets and doc ID 
				 * on appropriate lists (HA). 
				 */				
				BegOffset.put(position, originalPosition[0]);
				EndOffset.put(position, originalPosition[1]);
				DocID.put(position, documentID);
				
				
				// store the offset relation with the hashmap line
				offsetstoknow.put(realoffsetbeginat, position);
			}
			
			
			//-----------------------------------
			// Collect the metadata informations
			//-----------------------------------
			if (verbose) System.out.println("---------------");
			
			// collect the annotations informations
			JSONArray ArrayMetadatas = jObject.getJSONArray("mentions"); // get doc	
			
			for (int i = 0; i < ArrayMetadatas.length(); i++) {

				// get an object
				JSONObject entry = ArrayMetadatas.getJSONObject(i);
				
				// get the surface form at pos
				String surfaceform = entry.getString("name").replace('_',' ');
				surfaceform = surfaceform.replace("\n"," ");
				int offsetposition = Integer.parseInt(entry.getString("offset"));
						
				// collect the metadata 
				String id = "";
				String name = "";
				String uri = "NIL"; // by default this is a NIL value
				float score = 0;
				
				// collect the n candidates
				//HashMap<String, String> candidates = new HashMap<String , String>();
				ArrayList<String> arrListCandidates = new ArrayList<String>();
				
				//----------------------------
				// collect the best
				// in simple string uri
				// it is not mandatory to use
				// this section if you
				// collected all the others
				// bellow
				//----------------------------
				if ( entry.has("bestEntity")){
			
					//id = entry.getJSONObject("bestEntity").getString("id");
					id = entry.getJSONObject("bestEntity").getString("kbIdentifier");
					//name = entry.getJSONObject(id).getString("name");
					score = Float.parseFloat(entry.getJSONObject("bestEntity").getString("disambiguationScore"));
					// collect in uri vars the URI with Wikipedia format using directly the JSONobject made from entityMetadata
					if ( id.length() > 2) { 
						uri = entityMetadata.getJSONObject(id).getString("url");
						uri = uri.replace("%20", " ");
					}
				
				}
				//----------------------------
				// collect all the others
				// in a complete list of N
				// Note this section is sufficient 
				// to collect all the candidates
				// as the first ranked is equal
				// to the one in bestentity
				//----------------------------
				if ( entry.has("allEntities")){
					
						// get all the data
						JSONArray allEntities = entry.getJSONArray("allEntities");
						
						for (int k = 0; k < allEntities.length(); k++) {
							
							// collect references
							String thisid = allEntities.getJSONObject(k).getString("kbIdentifier");
							//String thisref = allEntities.getJSONObject(k).getString("name");
							float thisproba =  Float.parseFloat(allEntities.getJSONObject(k).getString("disambiguationScore"));
							String thisuri = "NIL";
							
							// collect in uri vars the URI with Wikipedia format using directly the JSONobject made from entityMetadata
							if ( thisid.length() > 2) { 
								thisuri = entityMetadata.getJSONObject(thisid).getString("url");
								thisuri = thisuri.replace("%20", " ");
							}
							
							// if (verbose) System.out.println("*-" + thisid + " " + thisref + " " + thisproba + " " + thisuri);
							
							// store
							// candidates.put(thisid, thisref);
							if (thisuri == null && k == 0 ) thisuri = "NIL";
							arrListCandidates.add(k, thisuri);
							
						}
						
				}
				
				// load the hashes
				int realposition = offsetstoknow.get(offsetposition); // get the current position of word in the list
				
				// load complementary informations
				// -- surfaceform
				fullSequence.put(realposition, surfaceform);
				fullSequenceNormalized.put(realposition, surfaceform.toLowerCase());
					
				// verbose
				if (verbose) System.out.println("*-->" + realposition + " " + offsetposition + " " + surfaceform  + " " + id + " " + name + " " + score + " " + uri);
				
				// -- uris
				// -- respect wikimeta-NLGbAse format - http://wikimeta.com/wapi/display.pl?query=Lucy Walsh&search=EN
				//            from http://en.wikipedia.org/wiki/Thomas_Becket
				// Store n annotations for this line -> marker
				HashMap<Integer, String> MetadataUriToStore = new HashMap<Integer, String>(); // build temporary hashmap	
				HashMap<Integer, String> MetadataKeyToStore = new HashMap<Integer, String>(); // build temporary hashmap	
				
				
				for (int h =0; h < arrListCandidates.size(); h++){
					
					String theuri = arrListCandidates.get(h);
					String newuri = theuri;
					
					if (theuri.contains("NIL")){
						newuri = "NIL";
					}else{
						newuri = newuri.replace("http://en.wikipedia.org/wiki/", "http://wikimeta.com/wapi/display.pl?query=");
						newuri = newuri + "&search=EN";
						newuri = newuri.replace('_',' ');
						newuri = newuri.replace("%20"," ");
					}
					
					
					//------------------------------
					// store the ranked uris
					//------------------------------
					MetadataUriToStore.put(h, newuri); // default uri in hashmap --> will sotre the n candidates by h rank
					// set the line
					MetadataUriByLine.put(realposition, MetadataUriToStore); // put the ordoned hash by key in the line position
					
					//----------------------------
					// store the ranked key
					//----------------------------
					String metadatakey = theuri.replace("http://en.wikipedia.org/wiki/", "");
					metadatakey = metadatakey.replace('_',' ');
					metadatakey = metadatakey.replace("%20", " ");
					MetadataKeyToStore.put(h, metadatakey);
					// set the line
					MetadataKeyByLine.put(realposition, MetadataKeyToStore);
					
					// verbose
					if (verbose) System.out.println("---------->" + h + " " + metadatakey + " " + newuri);
					
				}
				
				// if URI of first rank is NIL, set manually 
				// append when we have a named entity detected with no candidates
				if (uri.contains("NIL") && arrListCandidates.size() == 0){
					
					MetadataUriToStore.put(0, "NIL"); 
					// set the two hashes of the line to NIL
					MetadataUriByLine.put(realposition, MetadataUriToStore);
					MetadataKeyByLine.put(realposition, MetadataUriToStore);
				}
					
					
				
				/**
				
				String newuri = uri;
				if (uri.contains("NIL")){
					newuri = "NIL";
				}else{
					newuri = newuri.replace("http://en.wikipedia.org/wiki/", "http://wikimeta.com/wapi/display.pl?query=");
					newuri = newuri + "&search=EN";
					newuri = newuri.replace('_',' ');
				}
				
				//------------------------------
				// store the ranked uris
				//------------------------------
				MetadataUriToStore.put(0, newuri); // default uri in hashmap
				// set the line
				MetadataUriByLine.put(realposition, MetadataUriToStore);
				

				// store the ranked keyoin
				String metadatakey = uri.replace("http://en.wikipedia.org/wiki/", "");
				metadatakey = metadatakey.replace('_',' ');
				MetadataKeyToStore.put(0, metadatakey);
				// set the line
				MetadataKeyByLine.put(realposition, MetadataKeyToStore);
				
				*/
				
				
				
				
				
			}
			
		} 
		catch (JSONException e) { 
			System.out.println (e.toString()); 
		}
		
		// display the hashs for control if verbose
		if (verbose){
				for (int u =0; u < this.size(); u++)
						System.out.println(u + " " + getWordAtPos(u) + " " + getNELabel(u) + " " +  getPOSatpos(u) + " SF:" + getSurfaceFormAtPos(u) + " GeRanked:" + getMetadataRanked(0,u) + " GeRanked:" + getMetadataKeyRanked(0,u));
		}
		
	}
	
	
	@Override
	public int size() {
		// TODO Auto-generated method stub
		
		return WordEntries.size();
	}

	@Override
	public String getNELabel(int linenumber) {
		// TODO Auto-generated method stub
		return NEEntries.get(linenumber);
	}

	@Override
	public String getMetadata(int linenumber) {
		
		return getMetadataRanked(0,linenumber);
	}

	@Override
	public String getMetadataRanked(int rank, int linenumber) {
	
		String metatoreturn = null;
		
		if (MetadataUriByLine.containsKey(linenumber)){
			HashMap<Integer, String> thislinemetadata = MetadataUriByLine.get(linenumber); // collect the hash
			metatoreturn = thislinemetadata.get(rank);
		}
		
		return metatoreturn ; // return the value
	}

	@Override
	public String getMetadatakey(int linenumber) {
		
		return getMetadataKeyRanked(0, linenumber); // return the value
	}


	@Override
	public String getMetadataKeyRanked(int rank, int linenumber) {
		
		String metatoreturn = null;
		
		if (MetadataKeyByLine.containsKey(linenumber)){
			HashMap<Integer, String> thislinemetadata = MetadataKeyByLine.get(linenumber); // collect the hash
			metatoreturn = thislinemetadata.get(rank);
		}
		return metatoreturn ; // return the value
	}

	@Override
	public String getLinkedData(int linenumber) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getWordAtPos(int linenumber) {
		// TODO Auto-generated method stub
		return WordEntries.get(linenumber);
	}

	@Override
	public String getPOSatpos(int linenumber) {
		// TODO Auto-generated method stub
		return POSEntries.get(linenumber);
	}

	@Override
	public String getSurfaceFormAtPos(int linenumber) {
		// TODO Auto-generated method stub
		return fullSequence.get(linenumber);
	}

	@Override
	public String getSurfaceFormNormalizedAtPos(int linenumber) {
		// TODO Auto-generated method stub
		return fullSequenceNormalized.get(linenumber);
	}

	
	//---------------------------------------
	// set methods
	//--------------------------------------
	
	@Override
	public void setNElabel(int linenumber, String en) {
		// TODO Auto-generated method stub
		NEEntries.put(linenumber, en);
	}

	@Override
	public void setSfAtPos(int linenumber, String Sf, int lenght) {
		// TODO Auto-generated method stub
		fullSequence.put(linenumber, Sf);
		fullSequenceNormalized.put(linenumber, Sf.toLowerCase());
	}

	@Override
	public void removeSfAtPos(int linenumber) {
		// TODO Auto-generated method stub
		fullSequence.remove(linenumber);
		fullSequenceNormalized.remove(linenumber);
	}

	@Override
	public void setMetadataWithUri(int linenumber, String metadata) {
		// TODO Auto-generated method stub
		setMetadatarankedWithUri(linenumber, metadata, 0);
	}

	@Override
	public void setMetadatarankedWithUri(int linenumber, String metadata, int rank) {

				// collect the previous line content to update it
				HashMap<Integer, String> MetadataUriToStore = new HashMap<Integer, String>(); // build temporary hashmap
				//if(MetadataUriByLine.get(linenumber) != null)
					MetadataUriToStore = MetadataUriByLine.get(linenumber); // collect the line

				// set or reset the key
				MetadataUriToStore.put(rank, metadata);
				// reset the the key at line
				MetadataUriByLine.put(linenumber, MetadataUriToStore);
		
	}

	@Override
	public void setMetadataWithKey(int linenumber, String metadata) {
		// TODO Auto-generated method stub
		setMetadatarankedWithKey(linenumber, metadata, 0);
	}

	@Override
	public void setMetadatarankedWithKey(int linenumber, String metadata, int rank) {
		    

		HashMap<Integer, String> MetadataUriToStore = new HashMap<Integer, String>(); // build temporary hashmap
		HashMap<Integer, String> temp = new HashMap<Integer, String>(); 
						
			
		// re-build the URI -> later: replace by variablemetalinks.put(annotations.getMetadatakey(h), 1);
		if ( metadata != null){	
			if ( ! metadata.contentEquals("NIL") ){
				metadata = "http://wikimeta.com/wapi/display.pl?query=" + metadata + "&search=EN";
			}
		}
		
						
		// set or reset the key
		MetadataUriToStore.put(rank, metadata);
		// refill the global table
		MetadataUriByLine.put(linenumber, MetadataUriToStore);
	}

	
	/**
	 * Return the document ID for a given
	 * word in the original doc (HA)
	 * @param linenumber position in the annot obj
	 * @return
	 */
	public String getDocID(int linenumber){
		if(DocID.containsKey(linenumber)){
			return (DocID.get(linenumber));
		}
		else return "";
	}
	
	/**
	 * Return the beginning position of a given 
	 * word in the original doc (HA)
	 * 
	 * @param linenumber
	 * @return
	 */

	public int getBeginOffset (int linenumber){
		
		if(BegOffset.containsKey(linenumber)){
			
			return(BegOffset.get(linenumber));
		}
		else return 0;		
	}
	
	/**
	 * Return the ending position of a given 
	 * word in the original doc (HA)
	 * 
	 * @param linenumber
	 * @return
	 */
	public int getEndOffset (int linenumber){
		
		if(EndOffset.containsKey(linenumber)){
			
			return(EndOffset.get(linenumber));
		}
		else return 0;		
	}

	public void setEndOffset(int linenumber, int offset){
		EndOffset.put(linenumber, offset);
	}
	
	public void setBeginOffset(int linenumber, int offset){
		BegOffset.put(linenumber, offset);
	}

	@Override
	public int getAnnotatedPosition(String mention) {
		int value = 0;
		for(Entry<Integer,String> entries : WordEntries.entrySet()){
			if(entries.getValue().equalsIgnoreCase(mention)){
				value = entries.getKey();
			}
			
		}
		return value;
	}


	public String toString(int i) {
		return "Annotation word: " + getWordAtPos(i) + "\n" +   
				"NE: "+ getNELabel(i) + "\n" + 				   
			    "BeginOffset: " + getBeginOffset(i) + "\n" +
				"EndOffSet: " + getEndOffset (i) + "\n" +
				"Metadata: " + getMetadata(i) + "\n" +
				"-------------------------------- \n"; 	
	}
	


	
}
