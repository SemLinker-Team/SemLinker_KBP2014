package kbp2014.defineLink;

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



import org.apache.commons.lang.StringUtils;

import semkit.annotatedobjects.AnnotationInterface;
import kbp2014.managedocuments.DocumentNormalizer;
import kbp2014.managedocuments.NormalizedWebDoc;
import kbp2014.tools.Logging;


/**
 * 
 * Retrieve the surface form annotated by Wikimeta
 * at the given position of the query
 * 
 * @authors ericcharton, haydaalmeida
 *
 */
public class RetrieveExactMentionAnnotation {

	/**
	 * Retrieve the annotation for the marked mention according to its x and y position	 * 
	 * 
	 * @param annotations Annotation object 
	 * @param Qname The original query
	 * @param deb Beginning of mention
	 * @param fin End of mention
	 * @param originalText Source text of the original document before pre-processing
	 * @return position of mention in the annotation object
	 */
	public static int getAnnotationAtPosition(AnnotationInterface annotations, 
			String Qname, 
			int deb, int fin, 
			String originalText,
			Logging log){

		originalText = DocumentNormalizer.PrepareDocForMentionLocation(originalText);
		
		log.writeLog("    [Retrieve Original Annotation]");

		// define variables
		int span = 50; // what to get around the entity
		
		// explore all the annotation array
		int posOfAnnotation = 0;
		int beg = 0;
		int end = 0;
		
		//handling offsets of words splitted with hifens 
		//in wikimeta but grabbed concatenated in the original text.
		String QnameNorm = Qname;
		if(Qname.charAt(0) == '-') QnameNorm = Qname.replace("-", "- ");	
		
		String[] qSize = StringUtils.split(QnameNorm, " ");		

		String thecontent = originalText;

		//handling QueryExtraction items that have
		//begin or end offset equal to zero.
		if(deb == 0 || fin == 0){
			if (deb == 0){
				deb = fin - Qname.length() + 1;				
			}
			if (fin == 0){
				fin = deb + Qname.length() - 1;
			}
		}
		
		if(deb == fin){
			fin = deb + Qname.length() - 1;
		}

		beg = deb - qSize.length - span;
		end = fin + - qSize.length + span;
	

		if(beg < 1) {
			beg = deb;
		}
		if (end > originalText.length()){
			end = originalText.length();
		}		

		
		if(beg > 0 && end > 0){
			
			String contextFromOriginalText = originalText.substring(beg,end);
			String currentPosition = originalText.substring(deb, fin+1);
			
			//cleaning chars from the original text 
			//that have also been cleaned in the query
			currentPosition = currentPosition.replace("\n", " ");
			currentPosition = currentPosition.replace("’", "'");
						
			if(!(StringUtils.containsIgnoreCase(currentPosition, Qname))){
				deb = contextFromOriginalText.indexOf(qSize[0]) + originalText.indexOf(contextFromOriginalText);
				fin = deb + Qname.length()-1;
			}

			String mention = "";

			//check the annotation objecto to find the
			//corresponding offsets related to the mention
			for(int i = 0; i < annotations.size(); i++){

				//handling beg offset missing in the query
				if(annotations.getBeginOffset(i) == 0){
					//checking if the annot obj sequency match
					if ((annotations.getWordAtPos(i).equalsIgnoreCase(qSize[0])) && 
							(qSize.length > 1 && annotations.getWordAtPos(i+1).equalsIgnoreCase(qSize[1])) && 
							(annotations.getEndOffset(i + qSize.length-1) == fin))
						annotations.setBeginOffset(i, deb);
				}
				
				//begin offset is the reference in the annot obj,
			    // since it keeps only single tokens  
				if(deb == annotations.getBeginOffset(i)){
					posOfAnnotation = i;					
					if(fin == annotations.getEndOffset(i)){					
						mention = annotations.getWordAtPos(i);
					}
					// if deb and fin don't match, the annotation is tokenized.
					//search for the last word of mention
					else{

						for(int j = 0; j < qSize.length; j++){
							if(!(fin == annotations.getEndOffset(j+i))){
								mention += annotations.getWordAtPos(j+i) + " ";
							}
						}
						if(!(mention.contains(annotations.getWordAtPos(i+qSize.length-1)))){
							mention += annotations.getWordAtPos(i+qSize.length-1);
						}
					} 

				}				
			}			

			//-------------------------------------------
			// Verbose
			//-------------------------------------------		
			log.writeLog("    --->Context: " + contextFromOriginalText.replaceAll("\n", " "));
			log.writeLog("    --->Mention: " + mention);
			log.writeLog("    --->Annotation: " + annotations.getMetadata(posOfAnnotation) + " Pos: " + posOfAnnotation);
			log.writeLog("");
		}
		
		return posOfAnnotation;

	}

}
