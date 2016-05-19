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

import org.apache.commons.lang.StringUtils;
import org.languagetool.JLanguageTool;
import org.languagetool.language.English;

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
public class TestEntityQuery {

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
		int offsetB = 14992;
 		int offsetE = 14993;
		String oMention = "DC";
//		configfile var declaration
		String configfile = null;	
		String docID = "bolt-eng-DF-170-181103-8907371";
				
		
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
								offsetB =  Integer.parseInt(args[x + 2]);
								offsetE =  Integer.parseInt(args[x + 3]);
								oMention = args[x + 4];
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
		// instantiate Annotation Handler, to take care of annotation object normalizations
		AnnotationHandler annotHandler = new AnnotationHandler();		
		// annotation extractor
		AnnotationExtractor label = new AnnotationExtractor(WKMvars); // this is to annotate
	
		
		//---------------------
		// get the doc
		//---------------------

		IndexedDocumentCollection rdoc = new IndexedDocumentCollection(); // this is to get the doc related to the query with lucene
		// save the original content
		String originalcontent = rdoc.getDocFromIR(docID);
		
		String firstContent = originalcontent;
			
		//---------------------------------
		// Normalize query
		// correct mention if needed
		//---------------------------------
/*	HA	String originalQuery = oMention; // this will be used to synchronize the document with the modified query
		oMention = QueryProcessing.correctQuery(oMention, kbpVars.USE_WIKI_SPELLCHECKER, kbpVars.USE_LUCENE_WIKI_SPELLCHECKER); // this will be the new query with misspelling corrected and wrong chars removed
		String QnameNormalized =  QueryProcessing.normalize(oMention); // normalize query mention for later search in link or in kbannotator
	*/	
		//-------------------------------------------------
		// Synchronize content according to mention
        //    Athens in query / athens in doc
		//-------------------------------------------------
		// synchronize document with mention for the mention sequence
//HA	originalcontent  = originalcontent.replace(originalQuery, oMention);
		// make content
		String content = originalcontent; // the content must be synchronized according to the mod in Query
		
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
        // Normalize the query for special chars
        // oMention = documentNormalizer.ApplyNorm(oMention);
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
	    //(HA) changing for now to normalize document without passing a mention.	
		//content = documentNormalizer.NormalizeCapSequences(content, oMention, logging); // always first
		content = documentNormalizer.RemoveTags(content);
	

		// normalize according to mention
		// -- if lower case mention 
/* HA       if (oMention.matches("^[a-z]+")){
			content = documentNormalizer.IntroduceCap(content, oMention);		
		}	
  */      
        //---------------------------------
        // control the length if possible
        //---------------------------------
        
        if ( content.length() > 50000){
// HA        if ( content.length() > 60000){
// HA        		int lengthOfDocResized = offsetB + 5000;
        	 int lengthOfDocResized = offsetB + 5000;
        		if (lengthOfDocResized > content.length() ) lengthOfDocResized = content.length();
        		content = content.substring(0, lengthOfDocResized );
        }
              
       		
        //-------------------------
		// annotate
		//-------------------------
		
        // annotate
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
		
		annotations.decoder(documentreturned, firstContent, maxnumberofannotations, spellchecker);
		
		System.out.println("------------------------------------------");
	    System.out.println("Normalized:" + content.length());
	    System.out.println("------------------------------------------");
	    System.out.println(content);
		
	    System.out.println("------------------------------------------");
	    System.out.println("Processing Query");
	    System.out.println("------------------------------------------");

	    //-------------------------------------------------
        // Process treatments
        //-------------------------------------------------
        
        //handle extra corrections in the annotation object
		annotHandler.correctPersNE(annotations);
        annotHandler.handleForumAuthors(annotations);
	    		
		// apply coreference corrections
		annotations = detector.applyCoreferenceCorrection(annotations);
		// MutualDisambiguation 
		annotations = mdiz.disambig(annotations, content);
		// re-apply coreference corrections
		annotations = detector.applyCoreferenceCorrection(annotations);
		// apply NE normalizer
		annotations = NEnorm.rerankNE(annotations);
		// re-apply coreference corrections
		annotations = detector.applyCoreferenceCorrection(annotations);
		

				int linkAtPos = RetrieveExactMentionAnnotation.getAnnotationAtPosition(
						annotations, oMention, offsetB, 
						offsetE, originalcontent, logging);

				String Qnormalized = QueryProcessing.normalize(oMention);

				// search match
				// class to find the best link
				Link link = new Link(annotations, Qnormalized, oMention, linkAtPos, KBCorrespondanceTable, logging, maxnumberofannotations);
				String FinalKeyValue = link.kbKeyValue();

				//----------------------------------------
				// Use KB on mention if there is no match
				//----------------------------------------
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
