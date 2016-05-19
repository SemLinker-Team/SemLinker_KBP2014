package kbp2014.tools;

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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Set;
import java.lang.Integer;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import configure.NistKBPConfiguration;
import configure.SemkitConfiguration;


/**
 * Class to parse and extract
 * doc id list 
 * @author Marie-Jean Meurs
 * 2014
 */

public class ExtractDocId {	
	
	public ExtractDocId(){
				
	}
	private static String configfile =  null; // full config file path / when null, use default from configuration class
	
	public static void main(String[] args) throws IOException {
		
		try{
		
        NistKBPConfiguration KBvars ;
        if (configfile == null){
        	KBvars = new NistKBPConfiguration();
        }else{
        	KBvars = new NistKBPConfiguration(configfile);
        }
		
		String queryList = KBvars.HOME_DIR + KBvars.PATH_TO_TRAIN;
		System.out.println("home dir:\t" +KBvars.PATH_TO_TRAIN_DOC_LIST );
		String extractedDocList= KBvars.HOME_DIR + KBvars.PATH_TO_TRAIN_DOC_LIST;
		
//		Calendar today = new GregorianCalendar();
//		String todayStamp = Integer.toString(today.get(Calendar.MONTH) + 1) + "_" 
//							+ Integer.toString(today.get(Calendar.DAY_OF_MONTH)) + "_" 
//							+ Integer.toString(today.get(Calendar.YEAR));
		
		String extractedContent = "";
		
		FileWriter resultFile;
//		resultFile = new FileWriter(extractedDocList + "_" + todayStamp);
		resultFile = new FileWriter(extractedDocList);
        PrintWriter result = new PrintWriter(resultFile);
        
			File input = new File(queryList);
			System.out.println("input file:\t" + input);
			Document doc = Jsoup.parse(input, "UTF-8");
//			System.out.println("doc:\n\n" + doc);
			
			Elements docIdField = doc.getElementsByTag("docid");
//			System.out.println("docId:\t" + docIdField.text());
			System.out.println("size before:\t" + docIdField.size());
			
			Set<String> docList = new HashSet<String>(docIdField.size());
			
			for(Element docId : docIdField){
				docList.add(docId.text());
			}
			
			System.out.println("size after:\t" + docList.size());
			for(String docInList : docList){
				extractedContent = docInList;
//				System.out.println("extractedContent:\t" + extractedContent);
				result.println(extractedContent);
		    }
		    
		    result.close();
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();			
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
		


	}		

}
