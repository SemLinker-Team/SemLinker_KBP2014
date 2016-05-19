package semkit.queryhandler;

/*

SemLinker 2014

Copyright (C) 2014  Marie-Jean Meurs & Hayda Almeida
                    Ludovic Jean-Louis & Eric Charton

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


import java.util.HashMap;
import java.util.regex.Pattern;

import semkit.annotatedobjects.AnnotationInterface;

/**
 * Generates a query instance from the 
 * information kept in the annotation object
 * 
 * @author haydaalmeida
 *
 */

public class QueryExtractor {
	
	static boolean verbose = true;
	
	private HashMap<Integer, String> queryID = new HashMap<Integer, String>();
	private HashMap<Integer, String> queryName = new HashMap<Integer, String>();
	private HashMap<Integer, String> queryNELabel = new HashMap<Integer, String>();
	private HashMap<Integer, String> docID = new HashMap<Integer, String>();
	private HashMap<Integer, Integer> annotObjIndex = new HashMap<Integer, Integer>();
	private HashMap<Integer, Integer> beginOffSet = new HashMap<Integer, Integer>();
	private HashMap<Integer, Integer> endOffSet = new HashMap<Integer, Integer>();

	private int qCounter = 0;
	
	
	/**
	 * Manages query generation from the annot object info
	 * 
	 * @param annotations annot object
	 * @param originalContent original text for mapping
	 * @param exQueryCounter number of queries in total
	 * @param docID  current document
	 */	
	public void extractor(AnnotationInterface annotations, String originalContent, int exQueryCounter, String docID){

		for(int i = 0; i < annotations.size(); i++){

			//retrieving NE label of current and next annotation
			String currentNELabel = annotations.getNELabel(i);			
			String nextNELabel = annotations.getNELabel(i+1); 
			
			if(nextNELabel == null) nextNELabel = "";

			//retrieving original current and previous word 
			String currentWord = annotations.getWordAtPos(i).toLowerCase();
			String previousWord = "";
			if(i > 0) 
				previousWord = annotations.getWordAtPos(i-1).toLowerCase();

			//handling "St." cases 			
			if(currentWord.contains(".") && previousWord.contains("st")) i+=1;					


			//keeping only relevant NEs: person, GPE, organizations
			if(((currentNELabel.contains("PER") && !currentNELabel.contains("FIC"))||
					currentNELabel.contains("ORG") ||
					currentNELabel.contains("LOC")) 
					&& checkForbiddenWords(currentWord) 
					&& Pattern.matches("^(.*?[a-zA-Z]){1,}.*$", currentWord)
					){

				//setting metadata to NIL, in case none was previously set
				if(annotations.getMetadata(i) == null) annotations.setMetadataWithKey(i, "NIL");

				//handling a mention with more than one token
				if(nextNELabel.contains(currentNELabel)){

					int beg = i;

					while(nextNELabel.contains(currentNELabel)){
						//continue until the sequence has the same relevant NE
						if(i >= annotations.size()-1){							
							nextNELabel = "";
						}
						else {
							i++;
							nextNELabel = annotations.getNELabel(i);
						}										
					}

					//create the query for current composed mention
					insertQueryInfo(beg, i-1, annotations, docID, originalContent, exQueryCounter);
					i--;
					qCounter++;
				}

				else {										
					//create the query for current mention
					insertQueryInfo(i, 0, annotations, docID, originalContent, exQueryCounter);
					qCounter++;
				}
			}	
		}
	}
	

	/**
	 * Add query information to the list of queries
	 * 
	 * @param i counter with first position of mention
	 * @param j counter with last position of mention
	 * @param annotations annot object
	 * @param documentID  document name
	 * @param originalContent  original text
	 * @param qCounter  # of queries in the document
	 * @param exQueryCounter  # of queries in the experiment
	 */
	public void insertQueryInfo(int i, int j, AnnotationInterface annotations, String documentID, String originalContent, int exQueryCounter){

		String name = annotations.getWordAtPos(i);
		String nextName = annotations.getWordAtPos(i+1);
		
		// total number of queries generated so far
		int marker = qCounter + exQueryCounter;		
		int end;				
		
		//concatenating the mention from the begin offset 
		//until the end offset provided if words have same NE label
		if (j != 0){
			for( int a = i+1; a <= j; a++){
				if(name.endsWith("-")){
					if(annotations.getBeginOffset(a) > 0){						
						if (!(originalContent.charAt(annotations.getBeginOffset(a)-1) == ' ')){
							name += annotations.getWordAtPos(a);
						}
					}
				}
				else if(nextName.contains("-")){
					name += annotations.getWordAtPos(a);
				}
				else name += " " + annotations.getWordAtPos(a);
			}
			end = j;			
		}
		else {
			end = i;
		}		
		
		
		if(name.length() > 1){

			//handling issues with the query offset
			int beginMention = annotations.getBeginOffset(i);
			int endMention = annotations.getEndOffset(end)+1;

			if(beginMention <=0) 
				beginMention = endMention - name.length();
			
			if(endMention <= beginMention && beginMention > 0) 
				endMention = beginMention+(name.length()-1); 
			
			if(endMention > originalContent.length()) 
				endMention = originalContent.length();
			
			if(endMention < beginMention) 
				endMention = beginMention + name.length();

			//-----------
			// To use if want to double check the mention in the original document
			//-----------
			//String mention = "";

			try{			
				//----------- 
				// To use if want to double check the mention in the original document
				//-----------
				//mention = mappingContent.substring(beginMention, endMention);
				//mention = mention.replace("\n", " ");
				//name = mention;

				name = name.replace("\n", " ");

				queryID.put(marker, generateQueryID(qCounter, exQueryCounter));
				queryName.put(marker, name);
				queryNELabel.put(marker, annotations.getNELabel(i));
				docID.put(marker, documentID);
				annotObjIndex.put(marker, i);
				
				if(annotations.getBeginOffset(i) == 0) 
					beginOffSet.put(marker, beginMention);
				else 
					beginOffSet.put(marker, annotations.getBeginOffset(i));
				
				if (annotations.getEndOffset(end) == 0) 
					endOffSet.put(marker, endMention);
				else 
					endOffSet.put(marker, annotations.getEndOffset(end));


			} catch(Exception e){

				queryID.put(marker, generateQueryID(qCounter, exQueryCounter));				
				queryName.put(marker, name);
				queryNELabel.put(marker, annotations.getNELabel(i));
				docID.put(marker, documentID);
				annotObjIndex.put(marker, i);
				beginOffSet.put(marker, 0);
				endOffSet.put(marker, 0);	
			}
		}
		else qCounter--;
	}
	
	private boolean checkForbiddenWords(String currentWord){
		
		if(currentWord.contains("quote") || currentWord.contains("QUOTE") ||
			currentWord.contains("@gmail") || currentWord.contains("@yahoo") ||
			currentWord.contains("@hotmail"))
			return false;
			else return true;
	}
	
		
	/**
	 * Generate a pre-formatted query ID 
	 * 
	 * @param i # of queries in the document
	 * @param queryCounter # of queries in the experiment
	 * @return
	 */
	public String generateQueryID(int i, int queryCounter){
		
		String qNameBase = "EDL14_ENG_CSFG_000";	
		
		return qNameBase + (i + queryCounter + 1);
	}
	
	
	public String toString(int i){
		
		return 
			    "DocID: " + getDocID(i)+ "\n" + 
		       "QueryID: "+ getQueryID(i) + "\n" + 
			   "QueryName: " + getQueryName(i) + "\n" +
			   "QueryNE: " + getQueryNELabel(i) + "\n" +
		       "BeginOffset: " + getBeginOffSet(i) + "\n" +
			   "EndOffSet: " + getEndOffSet(i) + "\n" +
			   "-------------------------------- \n";
	}
	
		
	public int size(){

		return(qCounter);   
	}
			
	public String getQueryID(int position){
		if(queryID.containsKey(position)){
			return (queryID.get(position));
		}
		else return "";
		
	}
	
	public void setQueryID(int position, String ID){
		queryID.put(position, ID);
	}
	
	public void removeQueryID(int position){
		queryID.remove(position);
	}
		
	public String getQueryName(int position){
		if(queryName.containsKey(position)){
			return (queryName.get(position));
		}
		else return "";
	}
	
	public void setQueryName(int position, String name){
		queryName.put(position, name);
	}
	
	public void removeQueryName(int position){
		queryName.remove(position);
	}
	
	public String getQueryNELabel(int position){
		if(queryNELabel.containsKey(position)){
			return (queryNELabel.get(position));
		}
		else return "";
	}
	
	public void setQueryNELabel(int position, String NE){
		queryNELabel.put(position, NE);
	}
	
	public void removeQueryNELabel(int position){
		queryNELabel.remove(position);
	}
	
	public String getDocID(int position){
		if(docID.containsKey(position)){
			return (docID.get(position));			
		}
		else return "";
		
	}
	
	public void setQueryDocID(int position, String documentID){
		docID.put(position, documentID);
	}
	
	public void removeQueryDocID(int position){
		docID.remove(position);
	}
	
	public Integer getAnnotObjIndex(int position){
		if(annotObjIndex.containsKey(position)){
			return(annotObjIndex.get(position));
		}
		else return 0;
	}
	
	
	
	public Integer getBeginOffSet(int position){
		if(beginOffSet.containsKey(position)){
			return (beginOffSet.get(position));
		}
		else return 0;
		
	}
	
	public void removeQueryBegin(int position){
		beginOffSet.remove(position);
	}
	
	public void setQueryBeginOffset(int position, int offset){
		beginOffSet.put(position, offset);
	}
	
	public Integer getEndOffSet (int position){
		if(endOffSet.containsKey(position)){
			return (endOffSet.get(position));
		}
		else return 0;
	}
	
	public void setQueryEndOffset(int position, int offset){
		endOffSet.put(position, offset);
	}
	
	public void removeQueryEnd(int position){
		endOffSet.remove(position);
	}

}
