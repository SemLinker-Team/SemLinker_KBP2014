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

import semkit.annotatedobjects.AnnotationInterface;
import semkit.annotatedobjects.WikimetaXMLDecoder;

/**
 * 
 * Expand query using the document annotations object
 * 
 * 
 * @author ericcharton michelgagnon
 *
 */
public class ExpandQuery {

	/**
	 * 
	 * Transform a long sequence into an abbreviation to compare
	 * 
	 * @param name
	 * @return
	 */
	private static String abbreviate(String name){
		
            String abbreviatedName = name;
         
            try{
            	abbreviatedName = abbreviatedName.replaceAll(" of ", " "); // Remove some noise
                abbreviatedName = abbreviatedName.replaceAll(" and ", " "); // Remove some noise
                abbreviatedName = abbreviatedName.replaceAll("[a-z ]+", ""); // Just keep cap letters (remove spaces and lowercases)   
            }catch(Exception e){
            	
            }
            
            return abbreviatedName;
    }
    
	/**
	 * 
	 * Get an expansion of the original query by investigating the annotation found at its position or by expanding an abbreviation
	 * 
	 * @param annotations
	 * @param QnameNormalized
	 * @param Qname
	 * @param linkAtPos
	 * @param activatePLF
	 * @return
	 */
	public static String getExpansion(AnnotationInterface annotations , String QnameNormalized, String Qname, int linkAtPos, boolean activatePLF){
		
            System.out.println("    [Query Expansion]");

            // ---------------------------------------------------------
            // Document based query expansion 
            //   - If the mention found after document annotation is larger than 
            //     the one contained in QnameNormalized, the expanded mention 
            //     is kept
            // ---------------------------------------------------------

            // if the mention was found at position
            
            /**Before it was:
            * if (linkAtPos > 0) {
            * because linkAtPos was always 0;
            * now linkAtPos is the index of each word in the annotation object.
            * Thus we change to the following condition, so the link is not searched 
            * in the fullSequence list when it does not exists in there */
            
            if(annotations.getSurfaceFormNormalizedAtPos(linkAtPos)!=null){
            	
            	//--------------------------------------
            	// If the mention found by Wikimeta at 
            	// declared position is longer than the 
            	// query item, we keep the Wikimeta mention.
            	//--------------------------------------
            	
                if (annotations.getSurfaceFormNormalizedAtPos(linkAtPos).length() > QnameNormalized.length()) {
                    System.out.println("    Query Expanded to:" + annotations.getSurfaceFormNormalizedAtPos(linkAtPos) + " !!!\n");
                    return annotations.getSurfaceFormNormalizedAtPos(linkAtPos);
                }
                
                
                //--------------------------------------
                // Try to locate an expansion according
                // to the key and the length of mentions
                // related
                //--------------------------------------
                String metakeyAtPos = annotations.getMetadatakey(linkAtPos); // metadata at position
                String expandCandidate = QnameNormalized; // original mention put in expanded candidate
                	
                 //    --- search an antecessor with same family name and first name
                 for (int h = 0; h < linkAtPos; h++) {
                        // if 
                    	//   B) contains the original mention
                    	//   C) has the same metakey
                    	//   D) is longer than the original mention
                    	if (annotations.getSurfaceFormNormalizedAtPos(h) != null && annotations.getMetadatakey(h) != null){
  
	                        if (
	                      
	                        		 annotations.getSurfaceFormNormalizedAtPos(h).contains(QnameNormalized) && 
	                        		 annotations.getMetadatakey(h).contentEquals(metakeyAtPos) &&
	                        		 annotations.getSurfaceFormNormalizedAtPos(h).length() > expandCandidate.length()
	                        	) 
	                        {
	                        			// store the longest mention
	                        			expandCandidate = annotations.getSurfaceFormNormalizedAtPos(h);
	                        			
	                        }
                    	}
                   } // end for
                    
                   // if the expanded candidate is longer than the original, save it
                   if (expandCandidate.length() > QnameNormalized.length()){
                    	System.out.println("    NE Query Expanded to:" + expandCandidate + " !!!\n");
                    	return(expandCandidate);
                   }
                
            }
                
            
            //--------------------------------------
            // This is for special decoding
            // ---> sometimes it differs but it's just before the (ABR)
            //           The Czech Telecommunication Office (CTU) has announced that Czech Amateurs will
            //           services of the Angolan Armed Forces (FAA) from Tuesday as the FAA were celebrati
            // --> restrictions don't match (Apple)
            //--------------------------------------
            if (Qname.matches("[A-Z]+") && activatePLF == true) {
                for (int h = 0; h < annotations.size(); h++) {
                	// if mention exists, and is equal to the abbreviation, and is preceded by a parenthesis 
                    if (annotations.getSurfaceFormNormalizedAtPos(h) != null && annotations.getSurfaceFormNormalizedAtPos(h).toLowerCase().equals(QnameNormalized)) {

                        // System.out.println(linkAtPos + " M:" + annotations.getSurfaceFormNormalizedatpos(h) );

                    	// if abbreviation preceded, and followed by a parenthesis
                        //	 --> Angolan Armed Forces (FAA)
                        //   counter example 
                        //   -->Pirates  (BNA/Sony BMG Nashville),EL_ENG_01744
                    	// no space between sequence in parenthesis and what is preceding it 
                        if (annotations.getWordAtPos(h - 1).equals("(") && annotations.getWordAtPos(h + 1).equals(")") && !annotations.getNELabel(h - 2).contentEquals("UNK")) {

                            // look for the end of the mention
                            int posRef = h - 2;
                            while (annotations.getNELabel(posRef).contentEquals(annotations.getNELabel(h - 2))) {
                                posRef--;
                                if (posRef == 0) {
                                    break;
                                }
                            }
                            String PotentialExpansion = annotations.getSurfaceFormAtPos(posRef + 1);
                            String abbreviatedMention = abbreviate(PotentialExpansion);

                            // if contains only capitals 
                            if (abbreviatedMention != null){
	                            if (abbreviatedMention.matches("[A-Z]+")) {
	                                System.out.println("    Query Expanded by Potential long form:" + PotentialExpansion + " !!!\n");
	                                return PotentialExpansion.toLowerCase();
	                            }
                            }
                        }
                    }
                }
            }
        
            //--------------------------------------
            // final choice
            // See if the query is an abbreviation of the Wikimeta mention. In this case, it will take the Wikimeta mention.
            //--------------------------------------
            int sizeOfDecoder = annotations.size();
            for (int h = 0; h < sizeOfDecoder; h++) {
            	// if a mention exists, and is not equal to the abbreviation
                if (annotations.getSurfaceFormNormalizedAtPos(h) != null && !annotations.getSurfaceFormNormalizedAtPos(h).equals(QnameNormalized)) {
                    String abbreviatedMention = abbreviate(annotations.getSurfaceFormAtPos(h));
                    if (abbreviatedMention.startsWith(QnameNormalized.toUpperCase())) {
                        System.out.println("    Query Expanded by abreviation to: " + annotations.getSurfaceFormNormalizedAtPos(h) + " !!!\n");     
                        return annotations.getSurfaceFormNormalizedAtPos(h);
                    }
                }
            }
            
            
        System.out.print("    Query not Expanded:" + QnameNormalized + "\n");
        System.out.println("");
        return QnameNormalized; // by default the expanded query is equal to the original mention
    }	
}
