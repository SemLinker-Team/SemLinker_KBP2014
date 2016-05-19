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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import configure.NistKBPConfiguration;

public class QueryEvaluator {
	
	private HashMap<Integer, String> queryID = new HashMap<Integer, String>();
	private HashMap<Integer, String> queryName = new HashMap<Integer, String>();
	private HashMap<Integer, String> docID = new HashMap<Integer, String>();
	private HashMap<Integer, Integer> beginOffSet = new HashMap<Integer, Integer>();
	private HashMap<Integer, Integer> endOffSet = new HashMap<Integer, Integer>();
	
	private HashMap<Integer, String> mentionID = new HashMap<Integer, String>();
	private HashMap<Integer, String> KBnode = new HashMap<Integer, String>();
	private HashMap<Integer, String> NElabel = new HashMap<Integer, String>();
	
	private HashMap<Integer, String> removedQueries = new HashMap<Integer, String>();
	
		
	public static void main (String args[]) throws ParserConfigurationException, SAXException, IOException{
	
		String configfile =  null;
		NistKBPConfiguration KBvars ;
          
        if (configfile == null){
        	KBvars = new NistKBPConfiguration();       
        }else{
        	KBvars = new NistKBPConfiguration(configfile);        
        }
        
		QueryEvaluator AIDAeval = new QueryEvaluator();
		QueryEvaluator Wikieval = new QueryEvaluator();
		
		AIDAeval.loadQueries(KBvars.TEST_DIR + "tac_2014_kbp_english_EDL_generated_queries_20140915_06_35_AIDA.xml");
		AIDAeval.loadTab(KBvars.TEST_DIR +"tac_2014_kbp_english_EDL_training_queries_evaluation-result-k_20140915_06_35_AIDA.tab");
		Wikieval.loadQueries(KBvars.TEST_DIR +"tac_2014_kbp_english_EDL_generated_queries_20140916_10_01_WIKIMETA.xml");
		Wikieval.loadTab(KBvars.TEST_DIR +"tac_2014_kbp_english_EDL_training_queries_evaluation-result-k_20140916_10_01_WIKIMETA.tab");		
		
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
				
				queryID.put(i, element.getAttribute("id"));
				queryName.put(i, element.getElementsByTagName("name").item(0).getTextContent());
				docID.put(i, element.getElementsByTagName("docid").item(0).getTextContent());
				int beg = Integer.parseInt(element.getElementsByTagName("beg").item(0).getTextContent());
				beginOffSet.put(i, beg);
				int end = Integer.parseInt(element.getElementsByTagName("end").item(0).getTextContent());
				endOffSet.put(i, end);
			}			
		}			
	}
	
	public void loadTab(String path) throws FileNotFoundException{
		
		BufferedReader reader = new BufferedReader(new FileReader(path));
		String line = "";
		String[] result = new String[3];
		int queries = 0;
		
		try {
			while((line = reader.readLine()) != null){
					
				result = line.split("\t");
				
				mentionID.put(queries, result[0]);
				KBnode.put(queries, result[1]);
				NElabel.put(queries, result[2]);
				
				queries++;
			}
			
		} catch (IOException e) {			
			e.printStackTrace();
		}		
	}
	
	
	public void compareQueries(QueryEvaluator evalWiki, QueryEvaluator evalAida){
		
		QueryWriter writer = new QueryWriter("final_queries");		
		
		for(int i = 0; i < evalWiki.size(); i++){
			for( int j = 0; j < evalAida.size(); j++){
				if(evalWiki.queryName.get(i).equalsIgnoreCase(evalAida.queryName.get(j))){
					if (evalWiki.docID.get(i).equalsIgnoreCase(evalAida.docID.get(j))){
						if(evalWiki.beginOffSet.get(i) == evalAida.beginOffSet.get(j)){						
							
						
						}						
					}					
				}
				else {
					
				}
			}
		}
	}
	
	
	public int size(){
		return queryID.size();
	}

}
