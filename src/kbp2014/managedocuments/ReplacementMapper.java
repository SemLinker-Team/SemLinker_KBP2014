package kbp2014.managedocuments;
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

/**
 * Class to map abbreviation replacements
 * 
 * @author haydaalmeida
 *
 */

public class ReplacementMapper {
	
	/**
	 * 
	 * @param mappingContent  text to map the replacements
	 * @param target  original word to be mapped
	 * @param newForm   corrected word	 
	 * @return
	 */
	public String mapReplacement(String mappingContent, String target, String newForm){
		
		target = target.replace(".", " ");
		target = target.replace(",", " ");
		target = target.replace(":", " ");
		target = target.replace(";", " ");
		target = target.replace("\"", " ");
		
		newForm = newForm.replace(".", " ");
		newForm = newForm.replace(",", " ");
		newForm = newForm.replace(":", " ");
		newForm = newForm.replace(";", " ");
		newForm = newForm.replace("\"", " ");
		
		newForm = newForm.replaceAll(",.;:\"", "");
		
		String replacement = "[REP_CSFG"+ newForm + ":" + target + ">]";
		
		mappingContent = StringUtils.replace(mappingContent, target, replacement);
		
		return mappingContent;
		
	}
	
}
