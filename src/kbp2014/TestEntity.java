package kbp2014;

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
import java.io.IOException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang.StringUtils;
import org.languagetool.JLanguageTool;
import org.languagetool.language.English;
import org.xml.sax.SAXException;

import semkit.annotatedobjects.AidaJSONDecoder;
import semkit.annotatedobjects.AnnotationHandler;
import semkit.annotatedobjects.AnnotationInterface;
import semkit.annotatedobjects.WikimetaXMLDecoder;
import semkit.annotators.AnnotationExtractor;
import semkit.queryhandler.QueryExtractor;
import semkit.queryhandler.QueryProcessing;
import nlp.upperlevel.MutualDisambiguation;
import nlp.upperlevel.NormalizeNE;
import nlp.upperlevel.SimpleCoreferenceDetector;
import kbp2014.defineLink.Link;
import kbp2014.defineLink.RetrieveExactMentionAnnotation;
import kbp2014.managedocuments.DocumentNormalizer;
import kbp2014.managedocuments.ExpandAbbreviation;
import kbp2014.managedocuments.IndexedDocumentCollection;
import kbp2014.managedocuments.LanguageSpellChecker;
import kbp2014.managedocuments.NormalizedWebDoc;
import kbp2014.managelinkoutput.CorrespondanceTableKBPWikimeta;
import kbp2014.tools.Logging;
import kbp2014.wikipedia.AnnotateWithKB;
import configure.NistKBPConfiguration;
import configure.SemkitConfiguration;

/**
 * 
 * This class is intended to test a unique Query from KBP2012 and 
 * KBP2013 test and dev corpora. It is useful to investigate the
 * behavior of the system on a specific query. 
 * 
 * 
 * @author ericcharton
 *
 */
public class TestEntity {

	/**
	 * <br>
	 * Command line:<br>
	 * java -cp semlinker.jar kbp2014.TestEntity<br>
	 * <br>
	 * -config filename<br>
	 * -query filename x y mention<br>
	 * 
	 * 
	 * @param args
	 */
	
	public static void main(String[] args) {
		
		///--------------------------
		// default values
		///--------------------------
		// Sample of entry
		String docID = "XIN_ENG_20091231.0177";
		// configfile var declaration
		String configfile = null;		
	
		//-------------------------------------------------
        // get the command lines options
        // override constants and variables if needed
        //-------------------------------------------------
		for (int x=0; x < args.length; x++){
					try{
							// help
							if ( args[x].matches("-h")){
								
								System.out.println("-Help:");
								System.exit(0); // help always overrides others
							}
							
							// config file
							if ( args[x].matches("-config") ){
								configfile =  args[x + 1];
							}
							
							// parameters to test a query
							if ( args[x].matches("-query") ){
								docID =  args[x + 1];
							}
							
					} catch(Exception e){
						// Error
						System.out.println("An error occured, please check your command line instructions");
						System.exit(0); 
					}							
		}
		
		
		// instantiate a logger
		Logging logging = new Logging();
		// instantiate a correspondance table
		CorrespondanceTableKBPWikimeta KBCorrespondanceTable = new CorrespondanceTableKBPWikimeta();
		// load configuration
		NistKBPConfiguration kbpVars;
	    SemkitConfiguration WKMvars;
	   
	        if (configfile == null){
	        	kbpVars= new NistKBPConfiguration();
	        	WKMvars = new SemkitConfiguration();
	        }else{
	        	kbpVars = new NistKBPConfiguration(configfile);
	        	WKMvars = new SemkitConfiguration(configfile);
	        }
		
		// get the configuration for max number of annotations
		int maxnumberofannotations = kbpVars.MAX_ANNOTATIONS;			
		
		
		//-------------------------------- 
		// instantiate classes
		//--------------------------------
		
		// instantiate document normalizer
		DocumentNormalizer documentNormalizer = new DocumentNormalizer();
		// instantiate abbreviation expander
		ExpandAbbreviation expandabbrv = new ExpandAbbreviation();
		// disambiguate
		MutualDisambiguation mdiz = new MutualDisambiguation(kbpVars.INDEX_WIKIPEDIA, maxnumberofannotations);
	    // instantiate NE normalizer
		NormalizeNE NEnorm = new NormalizeNE();
		// instantiate coreference corrector
		SimpleCoreferenceDetector detector = new SimpleCoreferenceDetector();
		// instantiate a class to detect the link to exact mention position
		RetrieveExactMentionAnnotation getLinkatPos = new RetrieveExactMentionAnnotation();
		// instantiate a KB annotator
		AnnotateWithKB kbannotator = new AnnotateWithKB();
		//instantiate Annotation Handler, to take care of annotation object normalizations
		AnnotationHandler annotHandler = new AnnotationHandler();
		
		// annotation extractor
		AnnotationExtractor label = new AnnotationExtractor(WKMvars); // this is to annotate

				
		//---------------------
		// get the doc
		//---------------------
		IndexedDocumentCollection rdoc = new IndexedDocumentCollection(); // this is to get the doc related to the query with lucene
		
		String originalcontent = rdoc.getDocFromIR(docID);		
		
		String content = originalcontent;
		//Keeping the original content for offsets
		String firstContent = originalcontent;				

		//--------------------
    	// Instantiate a SpellChecker to correct entire document
    	//-------------------- 		
		LanguageSpellChecker spellchecker = new LanguageSpellChecker();
		
		//------------------------------
        // Apply spellchecker in the entire document
        //------------------------------
		if(kbpVars.USE_WIKI_SPELLCHECKER){
			
			try {	
				JLanguageTool languageTool = new JLanguageTool(new English());
				languageTool.activateDefaultPatternRules();
				content = spellchecker.checkSpelling(originalcontent, content);				
				
			} catch (IOException e) {				
				e.printStackTrace();
			}			
		}         
		
		System.out.println("------------------------------------------");
		System.out.println("Content length:" + content.length());
		System.out.println("------------------------------------------");
		System.out.println("------------------------------------------");
        System.out.println("Original:");
        System.out.println("------------------------------------------");
        System.out.println(originalcontent);
               
        //-------------------------------------------------
        // Normalizations of content
		//-------------------------------------------------
        System.out.println("------------------------------------------");
        System.out.println("Normalize document");
        System.out.println("------------------------------------------");
        // normalize from web doc
	    content = NormalizedWebDoc.ReplaceHtmlSmileyChars(content); // web doc cleaning
	    content = NormalizedWebDoc.ReplaceHtmlReservedChars(content); // web doc cleaning
	    content = NormalizedWebDoc.ReplaceISO_8859_1Characters(content); // web doc cleaning
	    content = NormalizedWebDoc.ReplaceISO_8859_1Symbols(content); // web doc cleaning
	    content = NormalizedWebDoc.ReplaceHtmlGreekLetter(content); // web doc cleaning
	    content = NormalizedWebDoc.ReplaceHtmlMathSymbol(content); // web doc cleaning
		
		content = documentNormalizer.normalizePunctuation(content);
		content = documentNormalizer.ApplyNorm(content);
		content = documentNormalizer.NormalizeCapSequences(content, logging); // always first
		content = documentNormalizer.RemoveTags(content);
	
        //---------------------------------
        // control the length if possible
        //---------------------------------
        
		if ( content.length() > 50000){
			//int lengthOfDocResized = offsetB + 5000;
			int lengthOfDocResized = 5000;
			if (lengthOfDocResized > content.length() ) lengthOfDocResized = content.length();
			content = content.substring(0, lengthOfDocResized );
		}
      
        		
        //-------------------------
		// annotate
		//-------------------------		
		String documentreturned = label.getAnnotations(content);
		

		// get content
		AnnotationInterface annotations = null;
		
		if (WKMvars.annotatorname.contains("wikimeta")) {
			annotations = new WikimetaXMLDecoder(); // instanciate a decoder using standard interface
			System.out.println("[TestEntity]Annotator is " + WKMvars.annotatorname);
		}
		if (WKMvars.annotatorname.contains("aida")) {
			annotations = new AidaJSONDecoder(); // instanciate a decoder using standard interface
			System.out.println("[TestEntity]Annotator is " + WKMvars.annotatorname);
		}
		
		annotations.decoder(documentreturned, originalcontent, maxnumberofannotations, spellchecker);
		
		//For testing purposes
		System.out.println(documentreturned);
		       
        
		System.out.println("------------------------------------------");
	    System.out.println("Normalized:" + content.length());
	    System.out.println("------------------------------------------");
	    System.out.println(content);
		
	    System.out.println("------------------------------------------");
	    System.out.println("Processing Document");
	    System.out.println("------------------------------------------");
		
	    //-------------------------------------------------
        // Process treatments
        //-------------------------------------------------
	    //apply correction of PERS NE label       
	    annotHandler.correctPersNE(annotations);
	    //apply NE labels to forum authors 
	    annotHandler.handleForumAuthors(annotations);
	  //remove NE labels from forum quotes 
	    annotHandler.handleForumQuotes(annotations);
	    
	        
		// apply coreference corrections
		annotations = detector.applyCoreferenceCorrection(annotations);					
		// Disambiguate using document mutual 
        if(!WKMvars.annotatorname.contains("aida")){                        	
        	annotations = mdiz.disambig(annotations, content); }      
        // re-apply coreference corrections
		annotations = detector.applyCoreferenceCorrection(annotations);		
		// apply NE normalizer
		annotations = NEnorm.rerankNE(annotations);	
		// re-apply coreference corrections
		annotations = detector.applyCoreferenceCorrection(annotations);
		
		//keeping track of # of queries generated in the entire experiment
		int queryCounter = 0;	
		
		//create a query object	
		QueryExtractor queries = new QueryExtractor();
		//extract queries from annotation object content
		queries.extractor(annotations, originalcontent, queryCounter, docID);	
		
		//-------------------------------------------------
        // Begin each query linking 
        //-------------------------------------------------		
		for(int i = 0; i < queries.size(); i++){

			if(queries.getQueryName(i).length()>1){
				System.out.println("-------------------------------- \n" );
				System.out.print(queries.toString(i));				
				
				// get the reference of the pointed mention
				int linkAtPos = RetrieveExactMentionAnnotation.getAnnotationAtPosition(
						annotations, queries.getQueryName(i), queries.getBeginOffSet(i), 
						queries.getEndOffSet(i), originalcontent, logging);
				
				

				//---------------------------------
				// Normalize query
				// correct mention if needed
				//---------------------------------
				String Qnormalized = QueryProcessing.correctQuery(queries.getQueryName(i), kbpVars.USE_WIKI_SPELLCHECKER, kbpVars.USE_LUCENE_WIKI_SPELLCHECKER); // this will be the new query with misspelling corrected and wrong chars removed
				Qnormalized = QueryProcessing.normalize(queries.getQueryName(i));

				//--------------------------------
				// search match
				//--------------------------------
				// class to find the best link
				Link link = new Link(annotations, Qnormalized, queries.getQueryName(i), i, linkAtPos, KBCorrespondanceTable, logging, maxnumberofannotations);
				String FinalKeyValue = link.kbKeyValue();

				//----------------------------------------
				// Use KB on mention if there is no match
				//----------------------------------------
				System.out.println("Written:" + queries.getQueryID(i) + "\t" + FinalKeyValue  + "\t" + Qnormalized + "\t" + link.bestSf() + "\t" + link.bestFinalMention() + "\t" + link.bestUri() + "\t" + link.bestEN() + "\n");
				System.out.println( FinalKeyValue + " / " + link.bestUri() + " / heuristic:" + link.heuristicUsed() );

				if (link.heuristicUsed() == 1 || ( FinalKeyValue.contains("NIL") && link.bestUri().contains("NIL")) ){
					String directKeyCandidate = kbannotator.getKeyforAMention( Qnormalized );
					System.out.println(" ===>"+directKeyCandidate);
					if (directKeyCandidate != null){
						FinalKeyValue = directKeyCandidate ;
					}
				}

				// display reference KB Node against found KB Node
				System.out.println("Returned Node -> " + FinalKeyValue );
			}
		}
		System.out.println("\n==================================================\n");
		
	}

}
