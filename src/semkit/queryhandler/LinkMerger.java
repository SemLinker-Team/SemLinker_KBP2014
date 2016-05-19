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
import java.util.Arrays;
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

public class LinkMerger extends QueryExtractor{

	private String NAME;

	public LinkMerger(String name){
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

	private String HEURISTIC = "link_merge_WIKIMETA_ONLY";
	

	public static void main (String args[]) throws ParserConfigurationException, SAXException, IOException{

		String configfile =  null;
		NistKBPConfiguration KBvars = new NistKBPConfiguration();

		LinkMerger goldStandard = new LinkMerger("Gold");
		LinkMerger annotator1 = new LinkMerger("Wikimeta_Lead");
		LinkMerger annotator2 = new LinkMerger("AIDA_Lead");

		//*** gold standard ***//
		goldStandard.loadQueries(KBvars.EXPERIMENT_DIR+"tac_2014_kbp_english_EDL_evaluation_queries.xml");

		//*** training ***//
		//		annotator1.loadQueries(KBvars.TEST_DIR +"/wikimeta/training/tac_2014_kbp_english_EDL_training_queries_20140929_10_33_WIKIMETA.xml");
		//		// UNK -> PER
		//		annotator1.loadTab(KBvars.TEST_DIR +"/wikimeta/training/tac_2014_kbp_english_EDL_training_queries_evaluation-result_20140929_10_33_WIKIMETA_man_per.tab");
		// UNK -> ORG
		//		annotator1.loadTab(KBvars.TEST_DIR +"/wikimeta/training/tac_2014_kbp_english_EDL_training_queries_evaluation-result_20140929_10_33_WIKIMETA_man_org.tab");
		//		//*** test ***//
		annotator1.loadQueries(KBvars.TEST_DIR +"/wikimeta/test/tac_2014_kbp_english_EDL_test_queries_20140929_10_46_WIKIMETA_man_per.xml");
		// UNK -> PER
		annotator1.loadTab(KBvars.TEST_DIR +"/wikimeta/test/tac_2014_kbp_english_EDL_test_queries_evaluation-result_20140929_10_46_WIKIMETA_man_per.tab");
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
//		annotator2.loadQueries(KBvars.TEST_DIR + "/aida/test/tac_2014_kbp_english_EDL_test_queries_20140926_06_32_AIDA.xml");
		// UNK -> PER
//		annotator2.loadTab(KBvars.TEST_DIR +"/aida/test/tac_2014_kbp_english_EDL_test_queries_evaluation-result_20140926_06_32_AIDA_man_per.tab");
		//		// UNK -> ORG
		//		annotator2.loadTab(KBvars.TEST_DIR +"/aida/test/tac_2014_kbp_english_EDL_test_queries_evaluation-result_20140926_06_32_AIDA_man_org.tab");

		//apply heuristic 
		goldStandard.compareQueries(KBvars, annotator1);

		//output results
		//annotator1.writeMergeXML(annotator2);
		//goldStandard.writeMergeTab(KBvars, annotator2);

		//re-execute NIL clustering
		goldStandard.reCluster(KBvars);		

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


	public void compareQueries(NistKBPConfiguration KBvars, LinkMerger annotator){

		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(KBvars.PATH_TO_TEST_MERGED_OUTPUT + HEURISTIC));
			boolean duplicateFlag = false;

			for(int i = 0; i < this.size(); i++){
				duplicateFlag = false;

				for(int j = 0; j < annotator.size(); j++){
					//String[] annotTab = loadQueryTab(annotator, j, annotator.getQueryID(j));
					if(this.isDuplicatedQuery(i, annotator, j)){

						//						if(annotTab[0].contains("E")){

						
						writeLinkTab(KBvars, writer, this, i, annotator, j);
						duplicateFlag = true;
						//						}
						//writing the NIL from wikimeta
						//else  writeLinkTab(KBvars, writer, this, i, annotator, j);
					}
				}		

				if(!duplicateFlag){
					writer.append(this.getQueryID(i) + "\t" + "NIL"  + "\t" + this.getQueryName(i) + 
						"\t" + this.getQueryName(i) + "\t" + this.getQueryName(i) + "\t" 
						+ "NORDF" + "\t" + "PER" + "\n");
				}
			}


			writer.close();
			System.out.println("Tab file exported.\n");

		} catch (IOException e) {
			e.printStackTrace();
		}


		System.out.println("Amount of removed queries: "+ removed);
		System.out.println("Amount of duplicate queries: "+ duplicates);
		System.out.println(annotator.NAME + " queries left: " + annotator.size());

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

	private boolean isDuplicatedQuery(int countAnnot1, LinkMerger annotator2, int countAnnot2){

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
							"Query name: " + this.getQueryName(countAnnot1) + "\t"+ "Query offset: " + this.getBeginOffSet(countAnnot1) + "\n"+
							
							annotator2.NAME + "\t"+ "\t"+ "Position: " + countAnnot2 + "\t"+
							"Doc ID: " + annotator2.getDocID(countAnnot2) + "\t"+ "Query ID: " + annotator2.getQueryID(countAnnot2) + "\t"+
							"Query name: " + annotator2.getQueryName(countAnnot2) + "\t"+ "Query offset: " + annotator2.getBeginOffSet(countAnnot2) +
							"\n");									

					duplicates++;

					return true;
				}						
			}					
		}		
		return false;
	}

		
	public void writeLinkTab(NistKBPConfiguration KBvars, BufferedWriter writer, LinkMerger gold, int i, LinkMerger annotator, int j) throws IOException{

		
		writer.append(gold.getQueryID(i) + "\t" + annotator.KBnode.get(j)  + "\t" + annotator.Qnormalized.get(j) + 
				"\t" + annotator.SurfaceForm.get(j) + "\t" + annotator.FinalMention.get(j) + "\t" 
				+ annotator.URI.get(j) + "\t" + annotator.NElabel.get(j) + "\n");
		writer.flush();
	}

	public void reCluster(NistKBPConfiguration KBvars){

		BuildNILClusters Kbuilder = null;

		Kbuilder = new BuildNILClusters( KBvars.PATH_TO_TEST_MERGED_OUTPUT+ HEURISTIC, KBvars.PATH_TO_TEST_CLUSTERED_MERGED+ HEURISTIC);
		Kbuilder.makeClusters();	    		

	}


	private String[] loadQueryTab(LinkMerger annotator, int position, String queryID){
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

	private void removeQuery(LinkMerger annotator, int position){

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
