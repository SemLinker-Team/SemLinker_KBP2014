package semkit.annotatedobjects;

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

/**
 * Generates a query instance from the 
 * information kept in the annotation object
 * 
 * @author halmeida
 *
 */

public class QueryExtractor {
	
	/**
	 * loop the annotation object
	 * lists for each item
	 *  
	 */	
	private HashMap<Integer, String> queryID = new HashMap<Integer, String>();
	private HashMap<Integer, String> queryName = new HashMap<Integer, String>();
	private HashMap<Integer, String> docID = new HashMap<Integer, String>();
	private HashMap<Integer, Integer> beginOffSet = new HashMap<Integer, Integer>();
	private HashMap<Integer, Integer> endOffSet = new HashMap<Integer, Integer>();
	
	private int qCounter = 0;
	
	
	public void extractor(AnnotationInterface annotations){

		for(int i = 0; i < annotations.size(); i++){
			
			String currentNELabel = annotations.getNELabel(i);
			String nextNELabel = annotations.getNELabel(i+1);

			if(currentNELabel.contains("PERS") ||
					currentNELabel.contains("ORG") ||
					currentNELabel.contains("LOC")					
					){
				
				if(nextNELabel.contains(currentNELabel)){
					
					int beg = i;
					
					while(nextNELabel.contains(currentNELabel)){
						i++;
						nextNELabel = annotations.getNELabel(i);						
						//insertQueryInfo(i, i+1, annotations);						
					}
					insertQueryInfo(beg, i-1, annotations);
					
				}
				
				else {
					insertQueryInfo(i, 0, annotations);
				}
				
				qCounter++;
			}				
		}
	}
	

	public void insertQueryInfo(int i, int j, AnnotationInterface annotations){

		String name = annotations.getwordatpos(i);
		
		if (j != 0){
			for( int a = i+1; a <= j; a++){
			
				name += " " + annotations.getwordatpos(a);
			}
			
			endOffSet.put(i, annotations.getEndOffset(j));
		}
		else {
			endOffSet.put(i, annotations.getEndOffset(i));
		}

		try{
			queryID.put(i, generateQueryID(i));
			queryName.put(i, name);
			docID.put(i, annotations.getDocID(i));
			beginOffSet.put(i, annotations.getBeginOffset(i));
			

		} catch(Exception e){

			queryName.put(i, "Name not found");
			docID.put(i, "DocID not found");
			beginOffSet.put(i, 0);
			endOffSet.put(i, 0);	
		}
	}
	
	
	public String generateQueryID(int i){
		
		String qNameBase = "EDL14_ENG_CSFG_000";
		
		return qNameBase + (size()+1);
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
	
	public String getQueryName(int position){
		if(queryName.containsKey(position)){
			return (queryName.get(position));
		}
		else return "";
	}
	
	public String getDocID(int position){
		if(docID.containsKey(position)){
			return (docID.get(position));			
		}
		else return "";
		
	}
	public Integer getBeginOffSet(int position){
		if(beginOffSet.containsKey(position)){
			return (beginOffSet.get(position));
		}
		else return 0;
		
	}
	public Integer getEndOffSet (int position){
		if(endOffSet.containsKey(position)){
			return (endOffSet.get(position));
		}
		else return 0;
	}

}
