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


import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;

import kbp2014.managedocuments.LanguageSpellChecker;


/**
 * Retrieve the begin and end offsets 
 * of a given word in the document.
 * 
 * @author haydaalmeida
 *
 */

public class OffsetFinder {
	/**
	 * Applies normalizations and calls method to find 
	 * the begin and end offsets of a word.
	 * 
	 * @param position offset of last word searched in the original text
	 * @param word  target to find offsets
	 * @param originalText  text containing the word
	 * @param spellchecker  spellchecker object to map correction replacements
	 * @return begin and end offsets of word
	 */	

	public int[]findOriginalOffset(int position, String word, String originalText, LanguageSpellChecker spellchecker) {

		//to hold begin and end offsets
		int[] offset = new int[2];
		int span = 50;

		//receives position of last word searched in the original text
		int count = position;

		//keeping a original text substring, starting from the last word searched		
		String thisText = originalText.substring(count, originalText.length());
		//this span (25) is relevant! do not change it - 20 is to small, 30 is too large.
		if(thisText.length() > 25 && thisText.substring(0,25).contains("<QUOTE PREVIOUSPOST=\"")){
				int quoteCount = StringUtils.substringBetween(thisText, "<QUOTE PREVIOUSPOST=\"", "\">").length();
				count += quoteCount + "<QUOTE PREVIOUSPOST=\"\">".length();			
			thisText = originalText.substring(count, originalText.length());
		}
		

		//context to identify extra HTML chars
		int HTMLspan = count+span;
		if(HTMLspan > originalText.length()) HTMLspan = originalText.length();
		String HTMLflag = originalText.substring(count, HTMLspan).toLowerCase().trim();
		
		
		//------------------------
		// Normalizations
		//------------------------
		//lowerCase to match searched word and context
		word = word.toLowerCase();
		thisText = thisText.toLowerCase();
		//correcting apostrophes
		word = correctApostrophe(word);
		thisText = correctApostrophe(thisText);
		HTMLflag = correctApostrophe(HTMLflag);
		
	
/*		if(count >= (originalText.length())){
			beg = originalText.length()-word.length();
			end = originalText.length();
		}
		else { */
			//handling HTML chars
			if(HTMLflag.contains("&") && !(HTMLflag.contains(word.trim()))){
				count = isHTMLSymbol(HTMLflag, count);				
				offset[0] = count;
				offset[1] = count;
			}

			else{	
				
				//checking if next word is within the first words of context
				//and retrieving the offsets
				if(thisText.contains(word) && isStopWord(word)){
					offset = countOffset(thisText, word, count);			
				}
				//in case the spellchecker has replacements for the document
				else if(spellchecker.size() > 0){
					//look for the current word in the spellchecker list
					if(spellchecker.getSpellReplacement(word)!= null || (offset[0]==0 && offset[1]==0)) {
						//retrieve the original format of the word so we can find its offset 
						word = findReplacement(word, count, spellchecker);
						offset = countOffset(thisText, word, count);
					}
				}

				//when finding extra chars, the text counter (pointer) 
				//is not moved forward, so the alignment is kept				
				else {
					offset[0] = count;
					offset[1] = count;				
				}
			}
			return offset;			
		//	}		
	}		   
	
	private boolean isStopWord(String word) {
		if(word.equalsIgnoreCase("and"))
		return false;
		else return true;
	}

	/**
	 * Finds the begin and end offsets of a given word in the text
	 * 
	 * @param thisText substring from original text
	 * @param word to be found in text
	 * @param count text current position (pointer) 
	 * @return begin and end offsets
	 */
	
	private int[] countOffset(String thisText, String word, int count){
		
		char prev;
		char aft;
		int beg = 0;
		int end = 0;
		String URL = "";
		
		int[] offset = new int[2];
		
		int wordPosit = thisText.indexOf(word);
		int wordEnd = wordPosit + word.length();
		
		try{
			//grabbing the word context 
			prev = thisText.charAt(wordPosit-1);
			aft = thisText.charAt(wordEnd);
			try{
				//grabbing URL context
				URL = thisText.substring(0,wordPosit);
			}
			catch (StringIndexOutOfBoundsException ex){				
				if(thisText.length() > wordPosit+50)
					URL = thisText.substring(wordPosit, wordPosit+50);
				else URL = thisText.substring(wordPosit, thisText.length());
			}
		}		
		catch (StringIndexOutOfBoundsException e){
			prev = thisText.charAt(wordPosit);
			aft = thisText.charAt(wordEnd);			
		}				
		
		//verifying if the word is not in fact a substring of another word
		if(	( !(Character.isLetter(prev)) && !(Character.isLetter(aft)) ) ||
				//handling an apostrofe - when finding one, the previous char will be a letter.
				( !(Character.isLetter(aft)) && (word.contains("'")) ) 	||
				//handling a substring annotated in a URL
				(checkForURLExceptions(URL, word))) {

			//begin offset holds the context index plus the text position (current pointer)
			beg = thisText.indexOf(word)+count;
			end = beg + word.length()-1;

			offset[0] = beg;
			offset[1] = end;			
		}
		
		return offset;
		
	}
	
	/**
	 * Check if an annotated word that is part of an URL
	 * 
	 * @param URL substring for context
	 * @param word current searched word
	 * @return
	 */
	private boolean checkForURLExceptions(String URL, String word){
		
		if(URL.contains("http") || URL.contains("htm") || word.contains("@") 
			|| word.contains("."))
			return true;
			else return false;	
	}
	
	/**
	 * Retrieve the original word format from the spellchecker list  
	 * 
	 * @param word corrected by spell checker
	 * @param count current text position
	 * @param spellchecker object
	 * @return original format of word
	 */
	
	private String findReplacement(String word, int count, LanguageSpellChecker spellchecker){
		
		Map<Integer, String> temp = spellchecker.getSpellReplacement(word);
		Iterator iterator = temp.entrySet().iterator();
		
		while(iterator.hasNext()){
			Map.Entry key = (Entry) iterator.next();
			int location = (int) key.getKey();
			String replaced = (String) key.getValue();

			if(location >= count){
				word = replaced;
			}
		}
		
		return word;
	}
	
	/**
	 * Normalize apostrophes in the text 
	 * @param text
	 * @return
	 */
	
	private String correctApostrophe(String text) {
		
		if(text.contains("’")){		
			text = text.replace("’s", "'s");
			text = text.replace("’n", "'n");
			text = text.replace("’t", "'t");
			text = text.replace("’m", "'m");
			text = text.replace("’d", "'d");
			text = text.replace("’l", "'l");
			text = text.replace("’v", "'v");
			text = text.replace("’ ", "' ");
			text = text.replace("’\"", "'\"");
			
		}	
		
		return text;
	}
	
	/**
	 * Adjusts text alignment when finding a 
	 * HTML special character.
	 * @param text string
	 * @param count pointer to current text position
	 * @return new current text position 
	 */
	
	private int isHTMLSymbol(String text, int count){
		int beg = text.indexOf("&");
		int end = text.indexOf(";", beg);
		
		if(end > beg){
				count += end;				
		}
		else count += text.length();
		
		return count;	
	}
	

}
