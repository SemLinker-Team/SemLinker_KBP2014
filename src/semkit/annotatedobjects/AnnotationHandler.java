package semkit.annotatedobjects;

/*SemLinker 2014

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



/**
 * Normalization of NE labels in the annotation object.
 * 
 * @author haydaalmeida
 *
 */
public class AnnotationHandler {
	
	
	/**
	 * Normalize NE label "PERS" to "PER", 
	 * in compliance to expected output
	 * @param annotations annot object
	 */	
	public void correctPersNE(AnnotationInterface annotations){
		
		for(int i = 0; i < annotations.size(); i++){
			
			String currentNElabel = annotations.getNELabel(i);			
			
			if(currentNElabel.contains("PERS")){
			   currentNElabel = currentNElabel.replace("PERS", "PER");
			   annotations.setNElabel(i, currentNElabel);
			}			
		}		
	}	
	

	/**
	 * Identify discussion forum authors according to tag
	 * and call method to assign a pertinent NE label
	 * 
	 * @param annotations annot object
	 */
	public void handleForumAuthors(AnnotationInterface annotations){
				
		String punctuations = "\"";

		for(int i = 0; i < annotations.size(); i++){

			String previousWord = "";
			if(i > 0) previousWord = annotations.getWordAtPos(i-1);
			String currentWord = annotations.getWordAtPos(i);
			String nextWord = annotations.getWordAtPos(i+1);

			//fetching <post author=>
			if(currentWord.contains("post")){
				if(nextWord.equalsIgnoreCase("author")){								
					insertAuthor(annotations, i);
				}
			}			
			//fetching <quote orig_author=>
		/*	if (currentWord.contains("orig")){
				if(nextWord.endsWith("author")){					
					insertAuthor(annotations, i);					
				}
			}*/
			//fetching <POSTER>
			if(currentWord.contains("Poster") && !previousWord.contains("!")){
				//erase possible annotations of POSTER tag
				annotations.setNElabel(i, "UNK");
				if (annotations.getMetadata(i) != null) annotations.setMetadataWithUri(i, "");
				i = i+1;
				//continue tagging the poster author until end of poster tag
				while( (!annotations.getWordAtPos(i).contains("!") && 
						!annotations.getWordAtPos(i+1).contains("Poster"))){
					if(punctuations.contains(annotations.getWordAtPos(i))) annotations.setNElabel(i, "POS");
					else annotations.setNElabel(i, "PER.POS");
					if (annotations.getMetadata(i) != null) annotations.setMetadataWithUri(i, "NIL");
					i++;
				}
			}
		}
	}
	
	/**
	 * Assign a relevant "PER" NE to discussion forum authors 
	 * 
	 * @param annotations annot object
	 * @param i position of annotation
	 */
	private void insertAuthor(AnnotationInterface annotations, int i){
		String pattern = "^[a-zA-Z0-9]*$";
		String punctuations = ".-:";
		i = i+4;
		if(i <= annotations.size()){
			String author = annotations.getWordAtPos(i);
			int offset = annotations.getBeginOffset(i);

			while(author.matches(pattern) || punctuations.contains(author)){
				annotations.setNElabel(i, "PER.DFA");
				annotations.setMetadataWithUri(i, "NIL");
				i++;
				
				author = annotations.getWordAtPos(i);
			}	
		}
	}

	/**
	 * Remove NE labels assigned to non-relevant 
	 * parts of the document (quotes)
	 * 
	 * @param annotations annotation object
	 */
	public void handleForumQuotes(AnnotationInterface annotations) {
		
		

		for(int i = 0; i < annotations.size(); i++){
		
			//fetching <QUOTE PREVIOUSPOST=>
			if(annotations.getWordAtPos(i).contains("PREVIOUSPOST")){

				if(annotations.getWordAtPos(i).contains("\">")){
					annotations.setNElabel(i, "QUT");
					if(annotations.getMetadata(i)!=null) annotations.setMetadataWithUri(i, "");
				}

				else{
					while( (!annotations.getWordAtPos(i+1).contains("\"") && 
							!annotations.getWordAtPos(i+2).contains(">"))){

						annotations.setNElabel(i, "QUT");
						if(annotations.getMetadata(i)!=null) annotations.setMetadataWithUri(i, "");

						i++;					
					}
				}
			}
			//fetching <quote>
			if(annotations.getWordAtPos(i).contains("<quote")){				
				while(!annotations.getWordAtPos(i+1).contains("quote>")){					
						annotations.setNElabel(i, "QUT");
						if(annotations.getMetadata(i)!=null) annotations.setMetadataWithUri(i, "");
					i++;
				
				}
			}
			
			
			
		}
	}
	
	
}


