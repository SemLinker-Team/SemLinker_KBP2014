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



import kbp2014.managedocuments.LanguageSpellChecker;


/**
 * 
 * 
 * This interface is used to access to an annotated object.
 * An annotated object is a Java Class that contains all
 * the information provided and required by the others classes
 * related to a text. It is for example Metadata, entity links, Named Entity 
 * or POS tags.
 * 
 * 
 * @author eric
 *
 */
public interface AnnotationInterface {
	
	

	void decoder (String XMLtext, String originalText, int numberofcandidates, LanguageSpellChecker spellChecker);
	
	int size();
	
	String getNELabel (int linenumber);
	
	String getMetadata (int linenumber);

	String getMetadatakey (int linenumber);
	
	String getMetadataRanked (int rank, int linenumber);

	String getMetadataKeyRanked (int rank, int linenumber);

	String getLinkedData (int linenumber);

	String getWordAtPos (int linenumber);

	String getPOSatpos (int linenumber);

	String getSurfaceFormAtPos (int linenumber);

	String getSurfaceFormNormalizedAtPos (int linenumber);

	void setNElabel(int linenumber, String en);

	void setSfAtPos(int linenumber, String Sf, int lenght);

	void removeSfAtPos(int linenumber);

	void setMetadataWithUri(int linenumber, String metadata);

	void setMetadatarankedWithUri(int linenumber, String metadata, int rank);

	void setMetadataWithKey(int linenumber, String metadata);
	
	void setMetadatarankedWithKey(int linenumber, String metadata, int rank);
	
	/**
	 * Return doc ID
	 */
	String getDocID(int linenumber);
	
	/**
	 * Return the original offsets 
	 * of the current word. (HA)
	 */
	
	int getBeginOffset (int linenumber);
	
	int getEndOffset (int linenumber);
	

	/**
	 * Sets original offsets in RetrieveExactMention
	 * in case they were missing
	 */
	void setBeginOffset (int linenumber, int offset);
	
	void setEndOffset (int linenumber, int offset);
	/**
	 *  Holds method to identify the
	 *  correct offset on the
	 *  original document. (HA)
	 */
	OffsetFinder offsetFinder = new OffsetFinder();
	
	int getAnnotatedPosition(String mention);
	
	String toString(int linenumber);
	

}
