package semkit.queryhandler;
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import kbp2014.managelinkoutput.BuildNILClusters;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import configure.NistKBPConfiguration;

public class QueryMerger extends QueryExtractor{
	
	private String NAME;
	
	public QueryMerger(String name){
	    this.NAME = name;	
	}
	
	private HashMap<Integer, String> mentionID = new HashMap<Integer, String>();
	private HashMap<Integer, String> KBnode = new HashMap<Integer, String>();
	private HashMap<Integer, String> NElabel = new HashMap<Integer, String>();
	private HashMap<Integer, String> Qnormalized = new HashMap<Integer, String>();
	private HashMap<Integer, String> SurfaceForm = new HashMap<Integer, String>();
	private HashMap<Integer, String> FinalMention = new HashMap<Integer, String>();
	private HashMap<Integer, String> URI = new HashMap<Integer, String>();	
	
	private ArrayList<String> removedQueries = new ArrayList<String>();
	
	private int qCounter = 0;
	private int duplicates = 0;
	private int removed = 0;
	QueryExtractor queryExtractor = new QueryExtractor();	
	
	private int HEURISTIC = 12;
	//-----------------------------------
	// Heuristic 0 = All Wikimeta + (AIDA - duplicates)
	// Heuristic 1 = All Wikimeta PERS + (AIDA - duplicates)
	// Heuristic 2 = All Wikimeta + (AIDA - NIL) 
	// Heuristic 3 = All Wikimeta + (AIDA ORG)
	// Heuristic 4 = All Wikimeta + (AIDA ORG - NIL)
	// Heuristic 5 = All AIDA + (Wikimeta KB)
	// Heuristic 6 = Wikimeta PER + AIDA ORG + AIDA GPE
	// Heuristic 7 = Wikimeta PER + Wikimeta ORG in DF + Wikimeta GPE in WB,NW + AIDA ORG in WB, NW + AIDA GPE in DF
	// Heuristic 8 = Wikimeta PER + Wikimeta ORG in DF in KB + Wikimeta GPE in WB 
	// Heuristic 9 = All Wikimeta + AIDA ORG + AIDA GPE
	// Heuristic 10 = All Wikimeta + AIDA ORG in KB + AIDA GPE in KB
	// Heuristic 11 = (Wikimeta - ORG) + AIDA ORG 
	//-----------------------------------
		
	public static void main (String args[]) throws ParserConfigurationException, SAXException, IOException{
	
		String configfile =  null;
		NistKBPConfiguration KBvars = new NistKBPConfiguration();
        
		QueryMerger annotator1 = new QueryMerger("AIDA");
		QueryMerger annotator2 = new QueryMerger("Wiki");
				
		//*** training ***//
//		annotator1.loadQueries(KBvars.TEST_DIR +"/wikimeta/training/tac_2014_kbp_english_EDL_training_queries_20140929_10_33_WIKIMETA.xml");
//		// UNK -> PER
//		annotator1.loadTab(KBvars.TEST_DIR +"/wikimeta/training/tac_2014_kbp_english_EDL_training_queries_evaluation-result_20140929_10_33_WIKIMETA_man_per.tab");
		// UNK -> ORG
//		annotator1.loadTab(KBvars.TEST_DIR +"/wikimeta/training/tac_2014_kbp_english_EDL_training_queries_evaluation-result_20140929_10_33_WIKIMETA_man_org.tab");
//		//*** test ***//
		annotator2.loadQueries(KBvars.TEST_DIR +"/wikimeta/test/tac_2014_kbp_english_EDL_test_queries_20140929_10_46_WIKIMETA.xml");
		// UNK -> PER
		annotator2.loadTab(KBvars.TEST_DIR +"/wikimeta/test/tac_2014_kbp_english_EDL_test_queries_evaluation-result_20140929_10_46_WIKIMETA_man_per.tab");
//		// UNK -> ORG
//		annotator1.loadTab(KBvars.TEST_DIR +"/wikimeta/test/tac_2014_kbp_english_EDL_test_queries_evaluation-result_20140929_10_46_WIKIMETA_man_org.tab");
				
				
		//load annotator2 info
		//*** training ***//
//		annotator2.loadQueries(KBvars.TEST_DIR + "/aida/training/tac_2014_kbp_english_EDL_training_queries_20140925_06_18_AIDA.xml");
//		// UNK -> PER
//		annotator2.loadTab(KBvars.TEST_DIR +"/aida/training/tac_2014_kbp_english_EDL_training_queries_evaluation-result_20140925_06_18_AIDA_man_per.tab");
		// UNK -> ORG
//		annotator2.loadTab(KBvars.TEST_DIR +"/aida/training/tac_2014_kbp_english_EDL_training_queries_evaluation-result_20140925_06_18_AIDA_man_org.tab");
//		//*** test ***//
		annotator1.loadQueries(KBvars.TEST_DIR + "/aida/test/tac_2014_kbp_english_EDL_test_queries_20140926_06_32_AIDA.xml");
		// UNK -> PER
		annotator1.loadTab(KBvars.TEST_DIR +"/aida/test/tac_2014_kbp_english_EDL_test_queries_evaluation-result_20140926_06_32_AIDA_man_per.tab");
//		// UNK -> ORG
//		annotator2.loadTab(KBvars.TEST_DIR +"/aida/test/tac_2014_kbp_english_EDL_test_queries_evaluation-result_20140926_06_32_AIDA_man_org.tab");
		
		//apply heuristic 
		annotator1.compareQueries(annotator2);
		
		//output results
		annotator1.writeMergeXML(annotator2);
    	annotator1.writeMergeTab(KBvars, annotator2);
		
		//re-execute NIL clustering
		annotator1.reCluster(KBvars);		
		
	}
	
	
	public void loadQueries(String path) throws ParserConfigurationException, SAXException, IOException{
		
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(new File(path));
				
		NodeList nodes = doc.getElementsByTagName("query");
		
		for(int i = 0; i < nodes.getLength(); i ++){
			Node node = nodes.item(i);
			node.getNodeName();
			
			if(node.getNodeType() == Node.ELEMENT_NODE){
				Element element = (Element) node;
				int beg = Integer.parseInt(element.getElementsByTagName("beg").item(0).getTextContent());
				int end = Integer.parseInt(element.getElementsByTagName("end").item(0).getTextContent());
				
	//			if( (!(beg < 0)) && (!(end < 0)) ){
					setQueryID(i, element.getAttribute("id"));
					setQueryName(i, element.getElementsByTagName("name").item(0).getTextContent());
					setQueryDocID(i, element.getElementsByTagName("docid").item(0).getTextContent());
					setQueryBeginOffset(i, beg);			
					setQueryEndOffset(i, end);

					qCounter++;
	/*			}
				else removedQueries.add(element.getAttribute("id"));*/
				
			}			
		}			
	}
	
	public void loadTab(String path) throws FileNotFoundException{
		
		String line = "";
		String[] result = new String[7];
		int marker = 0;
		
		try {
			
			BufferedReader reader = new BufferedReader(new FileReader(path));
			
			while((line = reader.readLine()) != null){
					
				result = line.split("\t");
				
	//			if(!removedQueries.contains(result[0])){

					mentionID.put(marker, result[0]);
					KBnode.put(marker, result[1]);
					Qnormalized.put(marker, result[2]);
					SurfaceForm.put(marker, result[3]);
					FinalMention.put(marker, result[4]);
					URI.put(marker, result[5]);
					NElabel.put(marker, result[6]);				

					marker++;
	//			}
			}
			reader.close();
			
		} catch (IOException e) {			
			e.printStackTrace();
		}		
	}
	
	
	public void compareQueries(QueryMerger annotator2){
	
	//----------------------
	// Heuristic 0
	//----------------------
		if(this.HEURISTIC==0){		
			for(int i = 0; i < this.size(); i++){
				String[] thisTab = loadQueryTab(this, i, this.getQueryID(i));

				if (thisTab[0] != null) {
					for( int j = 0; j < annotator2.size(); j++){
						String[] annotator2Tab = loadQueryTab(annotator2, j, annotator2.getQueryID(j));

						if (this.isDuplicatedQuery(annotator2, i, j)) {
							removeQuery(annotator2,j);					
						}
					}
				}
			}
		}
		
	//----------------------
	// Heuristic 1 
	//----------------------
	else if(this.HEURISTIC==1){		
		for(int i = 0; i < this.size(); i++){
			String[] thisTab = loadQueryTab(this, i, this.getQueryID(i));

			if((!isNELabel(i, "PER")) ){
				removeQuery(this, i);
			} 
			
			if (this.getQueryID(i) != null) {
				for( int j = 0; j < annotator2.size(); j++){
						String[] annotator2Tab = loadQueryTab(annotator2, j, annotator2.getQueryID(j));

						if (this.isDuplicatedQuery(annotator2, i, j)) {
							removeQuery(annotator2,j);					
						}
				}
			}
		}
	}
			
	//----------------------
	// Heuristic 2
	//----------------------
	else if(this.HEURISTIC==2){			
			for(int i = 0; i < this.size(); i++){
				String[] thisTab = loadQueryTab(this, i, this.getQueryID(i));

				if (thisTab[0] != null) {
					for( int j = 0; j < annotator2.size(); j++){
						String[] annotator2Tab = loadQueryTab(annotator2, j, annotator2.getQueryID(j));

						if (this.isDuplicatedQuery(annotator2, i, j)) {
							removeQuery(annotator2,j);					
						}

						else if(annotator2Tab[0] != null){
							if(annotator2Tab[0].contains("NIL"))
								removeQuery(annotator2,j);
						}
					}
				}
			}
		}
		
	//----------------------
	// Heuristic 3
	//----------------------
	else if(this.HEURISTIC==3){			
			for(int i = 0; i < this.size(); i++){
				String[] thisTab = loadQueryTab(this, i, this.getQueryID(i));

				if (thisTab[0] != null) {
					for( int j = 0; j < annotator2.size(); j++){
						String[] annotator2Tab = loadQueryTab(annotator2, j, annotator2.getQueryID(j));

						if (this.isDuplicatedQuery(annotator2, i, j)) {
							removeQuery(annotator2,j);					
						}

						else if(annotator2Tab[0] != null){
							if(!annotator2Tab[1].contains("ORG"))
								removeQuery(annotator2,j);
						}
					}
				}
			}
		}
	
	//----------------------
	// Heuristic 4
	//----------------------
	else if(this.HEURISTIC==4){			
			for(int i = 0; i < this.size(); i++){
				String[] thisTab = loadQueryTab(this, i, this.getQueryID(i));

				if (thisTab[0] != null) {
					for( int j = 0; j < annotator2.size(); j++){
						String[] annotator2Tab = loadQueryTab(annotator2, j, annotator2.getQueryID(j));

						if (this.isDuplicatedQuery(annotator2, i, j)) {
							removeQuery(annotator2,j);					
						}

						else if(annotator2Tab[0] != null){
							if(!annotator2Tab[1].contains("ORG") && !annotator2Tab[0].contains("E"))
								removeQuery(annotator2,j);
						}
					}
				}
			}
		}
		
	//----------------------
	// Heuristic 5
	//----------------------
	else if(this.HEURISTIC==5){			
			for(int i = 0; i < this.size(); i++){
				String[] thisTab = loadQueryTab(this, i, this.getQueryID(i));
													
				if( (!thisTab[0].contains("E")) ){
					removeQuery(this, i);
				} 
				
				else if (this.getQueryID(i) != null) {				
					for( int j = 0; j < annotator2.size(); j++){
						String[] annotator2Tab = loadQueryTab(annotator2, j, annotator2.getQueryID(j));
						
						if (this.isDuplicatedQuery(annotator2, i, j)) {
							removeQuery(annotator2,j);					
						}						
					}
				}
			}
		}
		
	//----------------------
	// Heuristic 6
	//----------------------	
	else if(this.HEURISTIC==6){			
			for(int i = 0; i < this.size(); i++){
				String[] thisTab = loadQueryTab(this, i, this.getQueryID(i));
													
				if( (!this.isNELabel(i, "PER")) ){
					removeQuery(this, i);
				} 
				
				else if (this.getQueryID(i) != null) {				
					for( int j = 0; j < annotator2.size(); j++){
						String[] annotator2Tab = loadQueryTab(annotator2, j, annotator2.getQueryID(j));
						
						if (this.isDuplicatedQuery(annotator2, i, j)) {
							removeQuery(annotator2,j);					
						}
						else if (annotator2Tab[0] != null && annotator2.isNELabel(j, "PER")){
							removeQuery(annotator2,j);
						}
					}
				}
			}
		}
			
	//----------------------
	// Heuristic 7
	//----------------------
	else if(this.HEURISTIC==7){	
			for(int i = 0; i < this.size(); i++){
				String[] thisTab = loadQueryTab(this, i, this.getQueryID(i));
		
			
						if(this.isNELabel(i, "ORG") && !this.isDocType(this.getQueryID(i), "DF")){
							removeQuery(this, i);
						}
						else if(this.isNELabel(i, "GPE") && this.isDocType(this.getDocID(i), "DF")){
							removeQuery(this, i);
						}
								

					if(this.getQueryID(i) != null) {
						for( int j = 0; j < annotator2.size(); j++){
							String[] annotator2Tab = loadQueryTab(annotator2, j, annotator2.getQueryID(j));

							if(annotator2Tab[0] != null){

								if (this.isDuplicatedQuery(annotator2, i, j)) {
									removeQuery(annotator2,j);					
								}						
								else if(annotator2.isNELabel(j, "PER")){
									removeQuery(annotator2, j);
								}						
								else if(annotator2.isDocType(annotator2.getDocID(j), "DF") && annotator2.isNELabel(j, "ORG")){
									removeQuery(annotator2, j);
								}
								else if (!annotator2.isDocType(annotator2.getDocID(j), "DF") && annotator2.isNELabel(j, "GPE")){
									removeQuery(annotator2, j);
								}
							}

						}
					}
				
			}
		}
		
	//----------------------
	// Heuristic 8
	//----------------------
	else if(this.HEURISTIC==8){	
			for(int i = 0; i < this.size(); i++){
				String[] thisTab = loadQueryTab(this, i, this.getQueryID(i));

				if(this.HEURISTIC==8){
					if (!this.isNELabel(i, "PER")){	
						if(this.isNELabel(i, "ORG")){
							if(this.isDocType(this.getQueryID(i), "DF") && thisTab[0].contains("NIL")){
								removeQuery(this, i);
							}
							else if (this.isDocType(this.getQueryID(i), "NW") || this.isDocType(this.getQueryID(i), "WB")){
								removeQuery(this, i);													
							}
						}
						else if(this.isNELabel(i, "GPE")){
							if ((this.isDocType(this.getDocID(i), "DF")) || 
									(this.isDocType(this.getDocID(i), "WB") && thisTab[0].contains("NIL"))){ 								
								removeQuery(this, i);
							}
						}
					}			

					if(this.getQueryID(i) != null) {
						for( int j = 0; j < annotator2.size(); j++){
							String[] annotator2Tab = loadQueryTab(annotator2, j, annotator2.getQueryID(j));

							if(annotator2Tab[0] != null){

								if (this.isDuplicatedQuery(annotator2, i, j)) {
									removeQuery(annotator2,j);					
								}						
								else if(annotator2.isNELabel(j, "PER")){
									removeQuery(annotator2, j);
								}						
								else if(annotator2.isNELabel(j, "ORG")){
									if(annotator2.isDocType(annotator2.getDocID(j), "DF") && annotator2Tab[0].contains("E")){
										removeQuery(annotator2, j);	
									}
								}
								else if (annotator2.isNELabel(j, "GPE")) {
									if(annotator2.isDocType(annotator2.getDocID(j), "NW") || 
											(annotator2.isDocType(annotator2.getDocID(j), "WB") && annotator2Tab[0].contains("E"))){
										removeQuery(annotator2, j);
									}
								}
							}
						}
					}
				}
			}
		}
		
	//----------------------
	// Heuristic 9
	//----------------------
	else if(this.HEURISTIC==9){			
			for(int i = 0; i < this.size(); i++){
				String[] thisTab = loadQueryTab(this, i, this.getQueryID(i));

				if (thisTab[0] != null) {
					for( int j = 0; j < annotator2.size(); j++){
						String[] annotator2Tab = loadQueryTab(annotator2, j, annotator2.getQueryID(j));

						if (this.isDuplicatedQuery(annotator2, i, j)) {
							removeQuery(annotator2,j);					
						}

						else if(annotator2Tab[0] != null){
							if(annotator2Tab[1].contains("PER")){
								removeQuery(annotator2,j);
							}
						}
					}
				}
			}
		}
	
	//----------------------
	// Heuristic 10
	//----------------------
	else if(this.HEURISTIC==10){			
				for(int i = 0; i < this.size(); i++){
					String[] thisTab = loadQueryTab(this, i, this.getQueryID(i));

					if (thisTab[0] != null) {
						for( int j = 0; j < annotator2.size(); j++){
							String[] annotator2Tab = loadQueryTab(annotator2, j, annotator2.getQueryID(j));

							if (this.isDuplicatedQuery(annotator2, i, j)) {
								removeQuery(annotator2,j);					
							}

							else if(annotator2Tab[0] != null){
								if(annotator2Tab[1].contains("PER") || annotator2Tab[0].contains("NIL")){
									removeQuery(annotator2,j);
								}
							}
						}
					}
				}
			}
		
	//----------------------
	// Heuristic 11
	//----------------------
	else if(this.HEURISTIC==11){			
				for(int i = 0; i < this.size(); i++){
					String[] thisTab = loadQueryTab(this, i, this.getQueryID(i));
					
					if( this.isNELabel(i, "ORG")){
						removeQuery(this, i);
					} 
					
					else if (this.getQueryID(i) != null) {		
						for( int j = 0; j < annotator2.size(); j++){
							String[] annotator2Tab = loadQueryTab(annotator2, j, annotator2.getQueryID(j));

							if (this.isDuplicatedQuery(annotator2, i, j)) {
								removeQuery(annotator2,j);					
							}

							else if(annotator2Tab[0] != null){
								if(!annotator2Tab[1].contains("ORG"))
									removeQuery(annotator2,j);
							}
						}
					}
				}
			}
	
	//----------------------
	// Heuristic 12
	//----------------------		
	else if(this.HEURISTIC==12){
		for(int i = 0; i < this.size(); i++){
			String[] thisTab = loadQueryTab(this, i, this.getQueryID(i));

			if (thisTab[0] != null) {
				for( int j = 0; j < annotator2.size(); j++){
					String[] annotator2Tab = loadQueryTab(annotator2, j, annotator2.getQueryID(j));

					if (this.isDuplicatedQuery(annotator2, i, j)) {
						if(thisTab[0].contains("E")){
							removeQuery(annotator2,j);
						}
						else if(annotator2Tab[0].contains("E")){
							removeQuery(this,i);
						}
						else removeQuery(annotator2,j);
											
					}					
				}
			}
		}		
	}

		System.out.println("Amount of removed queries: "+ removed);
		System.out.println("Amount of duplicate queries: "+ duplicates);
		System.out.println(annotator2.NAME + " queries left: " + annotator2.size());

	}
	
	private boolean isNELabel(int countAnnot, String label){
		if(this.NElabel.get(countAnnot).contains(label))
			return true;
		else return false;
	}
	
	private boolean isDocType(String ID, String type){
		if(ID.contains(type))
			return true;
		else return false;
	}
	
	private boolean isDuplicatedQuery(QueryMerger annotator2, int countAnnot1, int countAnnot2){
				
		String name1 = this.getQueryName(countAnnot1);
		String name2 = annotator2.getQueryName(countAnnot2);
		String doc1 = this.getDocID(countAnnot1);
		String doc2 = annotator2.getDocID(countAnnot2);
		int beg1 = this.getBeginOffSet(countAnnot1);
		int beg2 = annotator2.getBeginOffSet(countAnnot2);
		
		if(name1.equalsIgnoreCase(name2)){
			if (doc1.equalsIgnoreCase(doc2)){
				if(beg1 == beg2){									
																	
					System.out.println("Query match: \n" +
					this.NAME +"\t"+ "Position: " + countAnnot1 + "\t"+
							"Doc ID: " + this.getDocID(countAnnot1) + "\t"+ "Query ID: " + this.getQueryID(countAnnot1) + "\t"+
							"Query name: " + this.getQueryName(countAnnot1) + "\t"+ "Query offset: " + this.getBeginOffSet(countAnnot2) + "\n"+
					annotator2.NAME + "\t"+ "\t"+ "Position: " + countAnnot2 + "\t"+
							"Doc ID: " + annotator2.getDocID(countAnnot2) + "\t"+ "Query ID: " + this.getQueryID(countAnnot2) + "\t"+
							"Query name: " + annotator2.getQueryName(countAnnot2) + "\t"+ "Query offset: " + annotator2.getBeginOffSet(countAnnot2) +
							"\n");									
											
					duplicates++;
					
					return true;
				}						
			}					
		}		
		return false;
	}
	
	public void writeMergeXML(QueryMerger annotator2) throws ParserConfigurationException{
		
		QueryWriter queryWriter = new QueryWriter("merge_queries_h_"+HEURISTIC);
		int newQueryCounter = 0;
		
		Document queryDoc = queryWriter.generateDoc();
    	queryWriter.setRoot(queryDoc);
    	Element root = queryWriter.getRoot(queryDoc);
		
		for(int i = 0; i < this.size(); i++){
			if(this.mentionID.get(i)!=null){ 
				this.setQueryID(i, this.generateQueryID(newQueryCounter, 0));
				newQueryCounter++;
			}
		}		
		queryWriter.generateElements(queryDoc, root, this, this.size());
		System.out.println("\n"+ this.NAME + " elements extracted.");
		
		for(int i = 0; i < annotator2.size(); i++){	
			if(annotator2.mentionID.get(i)!=null){				
				annotator2.setQueryID(i, annotator2.generateQueryID(newQueryCounter, 0));
				newQueryCounter++;
			}
		}
		queryWriter.generateElements(queryDoc, root, annotator2, annotator2.size());
		System.out.println(annotator2.NAME + " elements extracted.\n");
		
		try {
			queryWriter.outputQueryList(queryDoc, queryWriter.output);
			System.out.println("XML file exported.\n");
		} catch (TransformerFactoryConfigurationError e) {
			e.printStackTrace();
		} catch (TransformerException e) {
			e.printStackTrace();
		}
	}
	
	public void writeMergeTab(NistKBPConfiguration KBvars, QueryMerger annotator2){
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(KBvars.PATH_TO_TEST_MERGED_OUTPUT + "h"+ HEURISTIC));
			
			for(int i = 0; i < this.size(); i ++){
				if(this.mentionID.get(i)!=null) {
					writer.append(this.getQueryID(i) + "\t" + this.KBnode.get(i)  + "\t" + this.Qnormalized.get(i) + "\t" + this.SurfaceForm.get(i) + "\t" 
							+ this.FinalMention.get(i) + "\t" + this.URI.get(i) + "\t" + this.NElabel.get(i) + "\n");
					writer.flush();
				}
			}
			
			for(int i = 0; i < annotator2.size(); i ++){
				if(annotator2.mentionID.get(i)!=null) {
				writer.append(annotator2.getQueryID(i) + "\t" + annotator2.KBnode.get(i)  + "\t" + annotator2.Qnormalized.get(i) + "\t" + annotator2.SurfaceForm.get(i) + "\t" 
						+ annotator2.FinalMention.get(i) + "\t" + annotator2.URI.get(i) + "\t" + annotator2.NElabel.get(i) + "\n");
				writer.flush();
				}
			}			
			
			writer.close();
			System.out.println("Tab file exported.\n");
			
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	public void reCluster(NistKBPConfiguration KBvars){
		
		BuildNILClusters Kbuilder = null;
		 
	    Kbuilder = new BuildNILClusters( KBvars.PATH_TO_TEST_MERGED_OUTPUT+"h"+ HEURISTIC, KBvars.PATH_TO_TEST_CLUSTERED_MERGED);
	    Kbuilder.makeClusters();	    		
	    	
	}
	
	
	private String[] loadQueryTab(QueryMerger annotator, int position, String queryID){
		String docType = "";
		String [] result = new String[3];
		
		if(annotator.mentionID.get(position)!=null && annotator.mentionID.get(position).contentEquals(queryID)){
			
			docType = annotator.getDocType(annotator.getDocID(position));
		
			result[0] = annotator.KBnode.get(position);
			result[1] = annotator.NElabel.get(position);
			result[2] = docType;
 		}		
		
		return result;
		
	}
	
	private String getDocType(String docID){
		
		if(docID.contains("bolt-eng-DF"))
			return "DF";
		else if (docID.contains("eng-NG") || docID.contains("eng-WL") )
			return "WB";
		else return "NW";
		
	}
	
	private void removeQuery(QueryMerger annotator, int position){
		
		System.out.println("Query removed: \t" + annotator.NAME +"\t"+ "Position: " + position + "\t" 
		+ "Doc ID: " + annotator.getDocID(position) + "\t"+ "KB: "+ annotator.KBnode.get(position) +"\t" 
		+ "NE Label: " + annotator.NElabel.get(position)+ "\t"+ "Query ID: " + annotator.getQueryID(position) 
		+ "\t"+ "Query name: " + annotator.getQueryName(position) + "\t"+ "Query offset: " + annotator.getBeginOffSet(position)) ;
		
		annotator.removeQueryID(position);
		annotator.removeQueryDocID(position);
		annotator.removeQueryName(position);
		annotator.removeQueryBegin(position);
		annotator.removeQueryEnd(position);
		//this.removeQueryNELabel(position);
		annotator.removeQueryMention(position);
		annotator.removeQueryKB(position);
		annotator.removeQueryNE(position);
		annotator.removeQueryNormalized(position);
		annotator.removeQuerySurface(position);
		annotator.removeQueryFinalMention(position);
		annotator.removeQueryURI(position);
		annotator.qCounter--;
		annotator.removed++;		
		
	}
	
	private void removeQueryMention(int position){
		mentionID.remove(position);
	}	
	private void removeQueryKB(int position){
		KBnode.remove(position);
	}	
	private void removeQueryNE(int position){
		NElabel.remove(position);
	}	
	private void removeQueryNormalized(int position){
		Qnormalized.remove(position);
	}	
	private void removeQuerySurface(int position){
		SurfaceForm.remove(position);
	}	
	private void removeQueryFinalMention(int position){
		FinalMention.remove(position);
	}	
	private void removeQueryURI(int position){
		URI.remove(position);
	}
	
	
	@Override
	public int size(){
		return qCounter;
	}

}
