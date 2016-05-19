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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.languagetool.JLanguageTool;
import org.languagetool.language.English;
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
import kbp2014.managelinkoutput.BuildNILClusters;
import kbp2014.managelinkoutput.CorrespondanceTableKBPWikimeta;
import kbp2014.tools.LoadTestRef;
import kbp2014.tools.Logging;
import kbp2014.tools.ManageQueries.Query;
import kbp2014.wikipedia.AnnotateWithKB;
import configure.NistKBPConfiguration;
import configure.SemkitConfiguration;
import semkit.annotatedobjects.AidaJSONDecoder;
import semkit.annotatedobjects.AnnotationHandler;
import semkit.annotatedobjects.AnnotationInterface;
import semkit.annotatedobjects.WikimetaXMLDecoder;
import semkit.annotators.AnnotationExtractor;
import semkit.queryhandler.QueryExtractor;
import semkit.queryhandler.QueryProcessing;
import semkit.queryhandler.QueryWriter;


/**
 *
 * Complete method to annotate KBP according to a query file.<br>
 * To use this application you need some additional components :<br>
 * <br>
 * - Lucene Index of KBP Corpus<br>
 *      (only available trough LDC)<br>
 * - Lucene Index of Wikipedia XML Dump [Optional but 10 time faster]<br>
 *      (we provide one for downloading)<br>
 * - Local implementation of Lucene-Search for Wiki [Optional]<br>
 * - A generated correspondence table for KB and Wikimeta [present in this package]<br>
 * - An API Key to Wikimeta engine [Optional if you keep the config unmodified]<br>
 * 
 * @version 0.1
 *
 * @author ericcharton, mariejeanmeurs, ludovicjeanlouis, michelgagnon
 *
 */
public class LinkEntities {
	
	
	private static String configfile =  null; // full config file path / when null, use default from configuration class
	
    /**
     * This is the main method that runs a complete annotation process on a NIST KBP evaluation set. <br>
     * <br>
	 * Command line:<br>
	 * java -cp semlinker.jar kbp2014.LinkEntities -config config.cfg<br>
	 * <br>
	 * -config filename
	 * 
	 * 
     * @param args
     */
     public static void main(String[] args) {

       
    	boolean testmode = false;
   
    	 
        //-----------------------------------
        // get options of command lines
        // override constants and variables if needed
        //-----------------------------------
		for (int x=0; x < args.length; x++){
			
					try{
							// help
							if ( args[x].matches("-h")){
								System.out.println("-Help:");
								System.exit(0); // help always overrides others
							}
							
							// eval mode
							if ( args[x].matches("-mode") && args[x+1].matches("true")){
								testmode =  true;
							}
							
							// config file
							if ( args[x].matches("-config") ){
								configfile =  args[x + 1];
							}
							
					} catch(Exception e){
						// Error
						System.out.println("An error occured, please check your command line instruction");
						System.exit(0); 
					}
							
		}
		
		//-----------------------------------
		// instantiate classes of constants
		// and config.
		//-----------------------------------
		
        NistKBPConfiguration KBvars ;
        SemkitConfiguration WKMvars;
   
        if (configfile == null){
        	KBvars = new NistKBPConfiguration();
        	WKMvars = new SemkitConfiguration();
        }else{
        	KBvars = new NistKBPConfiguration(configfile);
        	WKMvars = new SemkitConfiguration(configfile);
        }
     
		
        // some informations displayed about configuration
        System.out.println("[LinkEntities]Spelling checker is " + KBvars.USE_WIKI_SPELLCHECKER );
        System.out.println("[LinkEntities]LuceneWikiSpellCheck is " + KBvars.USE_LUCENE_WIKI_SPELLCHECKER);
        
        // define the maximum number of annotations used in various tasks
        int maxnumberofannotations = KBvars.MAX_ANNOTATIONS;
        System.out.println("[LinkEntities]MaxNbOfAnnotations is " + KBvars.MAX_ANNOTATIONS);
        
        testmode = KBvars.TEST_MODE; // position on true to use the 2013 KBP test corpus / false for 2012 dev corpus
        System.out.println("[LinkEntities]Testmode is " + KBvars.TEST_MODE);
    	
       // debug
        System.out.println("[LinkEntities]Abbreviation file is " + KBvars.ABBREVIATION_MAP_FILE);
    	
        
        
        //-----------------------------------
        // instantiate other needed classes
        //----------------------------------
        CorrespondanceTableKBPWikimeta KBCorrespondanceTable = new CorrespondanceTableKBPWikimeta(); 
        // Class to retrieve Lucene indexed document 
        IndexedDocumentCollection docCollection = new IndexedDocumentCollection(); // this is to get the doc related to query with Lucene
        // load reference (if exists) - not in eval mode
        LoadTestRef testreference = new LoadTestRef(KBvars.PATH_TO_TRAIN_REF);
        // instantiate abbreviation expander
        ExpandAbbreviation expandabbrv = new ExpandAbbreviation();
        // instantiate mutual disambiguation 
        // !!! do not change to static -> problems for opening files with Lucene
        MutualDisambiguation mdiz = new MutualDisambiguation(KBvars.INDEX_WIKIPEDIA, maxnumberofannotations);
        // instantiate NE normalizer
        NormalizeNE NEnorm = new NormalizeNE();
        // instantiate a KB annotator
        AnnotateWithKB kbannotator = new AnnotateWithKB();    
        
        //--------------------------------------------------
        // instantiate annotation extractor
        //   - this can be aida, or wikimeta
        //     please note it will have influence 
        //     on the interfac euse for annotatedobjects
        //--------------------------------------------------
        // annotateDoc extractor = new annotateDoc("http://www.wikimeta.com/wapi/semtag.pl"); // this is to annotate
        AnnotationExtractor extractor = new AnnotationExtractor(WKMvars, WKMvars.annotatorname); // this is to annotate with wikimeta or aida according to annotatorname in config        
        
        // instantiate Annotation Handler, to take care of annotation object normalizations
        AnnotationHandler annotHandler = new AnnotationHandler();
        
        //--------------------------------------
        //
        // open and manage the experiment file
        //
        //--------------------------------------		
        // vars
        int docNotFound = 0;
        int nbDocuments = 0;
        int queryCounter = 0;
        

        try {        	
        	
        	//--------------------
        	// open experience files
        	//--------------------
        	// query file readers
        	BufferedReader reader = null; 
        	if (testmode){
        		reader = new BufferedReader(new FileReader(KBvars.PATH_TO_TEST));
        	}else{
        		reader = new BufferedReader(new FileReader(KBvars.PATH_TO_TRAIN));
        	}
        	// output writers
        	BufferedWriter writer = null;
        	BufferedWriter multioutputwriter = null; 
        	// if flag test mode on true, use the current TEST corpus / else use the DEV with extra informations.
        	if (testmode){
        		writer = new BufferedWriter(new FileWriter(KBvars.PATH_TO_TEST_OUTPUT));
        		multioutputwriter  = new BufferedWriter(new FileWriter(KBvars.PATH_TO_TEST_MULTIPLE_OUTPUT));
        	}else{
        		writer = new BufferedWriter(new FileWriter(KBvars.PATH_TO_TRAIN_OUTPUT));
        		multioutputwriter  = new BufferedWriter(new FileWriter(KBvars.PATH_TO_TRAIN_MULTIPLE_OUTPUT));
        	}        	
        	
        	//--------------------
        	// open log files
        	//--------------------
            DocumentNormalizer documentNormalizer = new DocumentNormalizer();

            Logging logging = new Logging(KBvars.PATH_TO_LOGS + "LinkedEntities.log");
            Logging logHeadlines = new Logging(KBvars.PATH_TO_LOGS + "Headlines.log");
            
            //--------------------
        	// Instantiate a SpellChecker to correct entire document
        	//--------------------
            // 
            //JLanguageTool languageTool = new JLanguageTool(new English());
			//languageTool.activateDefaultPatternRules();
			LanguageSpellChecker spellchecker = new LanguageSpellChecker();
            
        	//--------------------
        	// begin experience
        	//--------------------
            String text = null;
            Query query;
            
            QueryWriter qWriter = new QueryWriter(testmode? "test_queries" : "training_queries");       
        	Document queryDoc = qWriter.generateDoc();
        	qWriter.setRoot(queryDoc);
        	Element root = qWriter.getRoot(queryDoc);
            

            while ((text = reader.readLine()) != null) {

                // get query beginning
            	if (text.length() > 1) {            	
                    
            		nbDocuments++;

                    //------------------------------
                    // Retrieve the document
                    //------------------------------
                    // get doc string                    
                    String documentID = text;
                    String content = docCollection.getDocFromIR(documentID);

                    
                    //Keeping the original content for offsets
                    String firstContent = content;                    
            		//and for future mention localization
                    String originalContent = content;

            		
                    //------------------------------
                    // Apply spellchecker in the entire document
                    //------------------------------
                    if(KBvars.USE_WIKI_SPELLCHECKER){         			             			
                    	try {				
                    		content = spellchecker.checkSpelling(firstContent, content);              				
                    	} catch (IOException e) {				
                    		e.printStackTrace();
                    	}			
                    }
                    //------------------------------
            		
                    
            		// Check if the doc is correctly retrieved, if not, NIL by default, please correct the index
                    if (content == null || content.length() == 0) {
                        // no doc found, managing error
                        // log the information but output a nil
                        logging.writeLog("Error: no doc, please correct the index");
                        docNotFound++;
                    } else {                    	
                    	//-------------------------------------------------
                        // Content length management
                        //-------------------------------------------------
                    	String avert = "";
                    	if (content.length() > 50000){ 
                    		avert = " [Content Lenght Oversize API. Splitting]";
                    		}
                    	logging.writeLog("Content:" + content.length() + avert);
                    	// control the length if possible
                    	if ( content.length() > 50000){
//                  		int lenghtofdocresized = query.beg + 5000;
                    		int lenghtofdocresized = 5000;
                    		if (lenghtofdocresized > content.length() ) lenghtofdocresized = content.length();
                    		content = content.substring(0, lenghtofdocresized );
                    	}

                        //-------------------------------------------------
                        // Normalizations
                        //-------------------------------------------------
                              
                        // Normalize for web 
                        content = NormalizedWebDoc.ReplaceHtmlSmileyChars(content); // web doc cleaning
                        content = NormalizedWebDoc.ReplaceHtmlReservedChars(content); // web doc cleaning
                        content = NormalizedWebDoc.ReplaceISO_8859_1Characters(content); // web doc cleaning
                        content = NormalizedWebDoc.ReplaceISO_8859_1Symbols(content); // web doc cleaning
                        content = NormalizedWebDoc.ReplaceHtmlGreekLetter(content); // web doc cleaning// 
                        content = NormalizedWebDoc.ReplaceHtmlMathSymbol(content); // web doc cleaning
                        content = NormalizedWebDoc.replaceSpecialCharacters(content); // web doc cleaning, special chars
                        //content = NormalizedWebDoc.forumnormalizer(content); // forums doc cleaning
                                  		
                        content = documentNormalizer.normalizePunctuation(content);
                        content = documentNormalizer.ApplyNorm(content); // normalize doc for special characters
                        content = documentNormalizer.NormalizeCapSequences(content, logHeadlines); // always first (try to) remove caps in sentences
                        content = documentNormalizer.RemoveTags(content); // remove SGML tags
                        
                        //-------------------------------------------------
                        // Process annotations
                        //-------------------------------------------------    
                        // annotate with the engine selected in the semkit configuration
                        String annotationreturned = extractor.getAnnotations(content); // return is from the engine (can be XML, simple CDATA or JSON)
                        														       // must be decoded with the correct decoder
                                                                                       // see bellow

                        // structure original content in an annotation object
                        AnnotationInterface annotations = null; // instanciate a decoder using standard interface by default.
                        // define annotation object
                        if (WKMvars.annotatorname.contains("aida")){
                        	annotations = new AidaJSONDecoder(); // if decoder is Aida
                        }else{
                        	annotations = new WikimetaXMLDecoder(); // default decoder is Wikimeta
                        }
                        
                        // decode in the object what was sent by the API
                        annotations.decoder(annotationreturned, originalContent, maxnumberofannotations, spellchecker); // build the annotated object
                        
                        
                        //-------------------------------------------------
                        // Process treatments
                        //-------------------------------------------------
                        
                        //apply correction of PERS NE label
                        annotHandler.correctPersNE(annotations);                        
                        //apply NE labels to forum authors 
                        annotHandler.handleForumAuthors(annotations);
                        //remove NE labels from forum quotes 
                        annotHandler.handleForumQuotes(annotations);
                        
//                        //Testing config 20140916_12_19 (WIKIMETA)
//                        // apply NE normalizer
//                        annotations = NEnorm.rerankNE(annotations);
//                        // apply co-reference corrections
//                        annotations = SimpleCoreferenceDetector.applyCoreferenceCorrection(annotations);                        
//                        // Disambiguate using document mutual 
//                        if(!WKMvars.annotatorname.contains("aida")){                        	
//                        	annotations = mdiz.disambig(annotations, content); }
//                        // re-apply co-reference corrections
//                        annotations = SimpleCoreferenceDetector.applyCoreferenceCorrection(annotations);
//                        // re-apply NE normalizer
//                        annotations = NEnorm.rerankNE(annotations);
//                        // re-apply co-reference corrections
//                        annotations = SimpleCoreferenceDetector.applyCoreferenceCorrection(annotations);
                      
                        
                        // re-apply co-reference corrections after NE Normalizer
                        annotations = SimpleCoreferenceDetector.applyCoreferenceCorrection(annotations);                        
                        // Disambiguate using document mutual 
                        if(!WKMvars.annotatorname.contains("aida")){                        	
                        	annotations = mdiz.disambig(annotations, content); }                                         
                        // re-apply co-reference corrections after mutual disambiguation
                        annotations = SimpleCoreferenceDetector.applyCoreferenceCorrection(annotations);
                        // apply NE normalizer
                        annotations = NEnorm.rerankNE(annotations);
                        // re-apply co-reference corrections after NE Normalizer
                        annotations = SimpleCoreferenceDetector.applyCoreferenceCorrection(annotations);

                                          
                        //create a query object
                        QueryExtractor queries = new QueryExtractor();
                        
                        //extract queries from annotation object content
                		queries.extractor(annotations, originalContent, queryCounter, documentID);
                		//keeping track of # of queries generated in the entire experiment
                		queryCounter = queryCounter + queries.size();
                		
                		//listing query fields to XML doc
                		qWriter.generateElements(queryDoc, root, queries, queryCounter);                		              		                		
                		              		
                		//-------------------------------------------------
                        // Begin each query linking 
                        //-------------------------------------------------
                		
                		for(int i = 0; i < queryCounter; i++){

                			if(queries.getQueryName(i).length()>1){
                				System.out.println("-------------------------------- \n" +
                						"Document #: " + nbDocuments);                				
                				System.out.print(queries.toString(i));

                				// get the reference of the pointed mention
                				int linkAtPos = RetrieveExactMentionAnnotation.getAnnotationAtPosition(
                						annotations, queries.getQueryName(i), queries.getBeginOffSet(i), 
                						queries.getEndOffSet(i), originalContent, logging);

                				//---------------------------------
                				// Normalize query
                				// correct mention if needed
                				//---------------------------------                				
                				String Qnormalized = QueryProcessing.correctQuery(queries.getQueryName(i), KBvars.USE_WIKI_SPELLCHECKER, KBvars.USE_LUCENE_WIKI_SPELLCHECKER); // this will be the new query with misspelling corrected and wrong chars removed
                				Qnormalized = QueryProcessing.normalize(Qnormalized);

                				//--------------------------------
                				// search match
                				//--------------------------------
                				// class to find the best link
                				Link link = new Link(annotations, Qnormalized, queries.getQueryName(i), i, linkAtPos, KBCorrespondanceTable, logging, maxnumberofannotations);
                				String FinalKeyValue = link.kbKeyValue();

                				//----------------------------------------
                				// Use KB on mention if there is no match
                				//----------------------------------------
                				if (link.heuristicUsed() == 1 ){                					
                					String directKeyCandidate = kbannotator.getKeyforAMention(Qnormalized);
                					if (directKeyCandidate != null){
                						FinalKeyValue = directKeyCandidate ;
                						System.out.println("    [Result retrieved by direct key match with KB]");
                					}
                				}

                				//-------------------------------------
                				// Display and save results
                				//-------------------------------------
                				// sort the value 
                				//System.out.println("Written:" + queries.getQueryID(i) + "\t" + FinalKeyValue  + "\t" + Qnormalized + "\t" + link.bestSf() + "\t" + link.bestFinalMention() + "\t" + link.bestUri() + "\t" + link.bestEN() + "\n");
                				
                				writer.append(queries.getQueryID(i) + "\t" + FinalKeyValue  + "\t" + Qnormalized + "\t" + link.bestSf() + "\t" + link.bestFinalMention() + "\t" + link.bestUri() + "\t" + link.bestEN() + "\n");               				                				
                				writer.flush();

                				// sort the ranked list from 2
                				multioutputwriter.append(queries.getQueryID(i) + "\t" + link.listofKeysRanked() +"\n");
                				multioutputwriter.append("\t" + link.listofKeysRanked() +"\n");

                				// display reference KB Node against found KB Node
                				if (testmode){
                					logging.writeLog("KB Node -> Test Mode / Returned Node ->" + FinalKeyValue );                					
                				}else{
                					logging.writeLog("KB Node ->" + testreference.returnQueryKBRef(queries.getQueryID(i)) + " EN:" + testreference.returnQueryKBENLabelRef(queries.getQueryID(i)) + " Returned Node ->" + FinalKeyValue );
                				}                				
                			}
                		}
                		System.out.println("\n==================================================\n");       		
             		                         
                   }
               }

            }
            // build XML output - list of queries
            qWriter.outputQueryList(queryDoc, QueryWriter.output);
            
            multioutputwriter.close();
            writer.close();
            reader.close();
            
            logging.writeLog("Documents:" + nbDocuments + " Notfound:" + docNotFound);


        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();    
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformerFactoryConfigurationError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        // end of process

        // call buildNilClusters
        // file in file out    
        BuildNILClusters Kbuilder = null;
    	if (testmode){
    		Kbuilder = new BuildNILClusters( KBvars.PATH_TO_TEST_OUTPUT, KBvars.PATH_TO_TEST_CLUSTERED_OUTPUT);
    		Kbuilder.makeClusters();
    		
    	}else{
    		Kbuilder = new BuildNILClusters(KBvars.PATH_TO_TRAIN_OUTPUT, KBvars.PATH_TO_TRAIN_CLUSTERED_OUTPUT);
    		Kbuilder.makeClusters();
    	}
    	
    	// build an output with ranks
        
    }
}
