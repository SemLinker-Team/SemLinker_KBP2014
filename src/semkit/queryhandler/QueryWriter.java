package semkit.queryhandler;
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
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import configure.NistKBPConfiguration;

/**
 * Class to produce an output for the QueryExtractor process.
 * 
 * @author haydaalmeida
 *
 */

public class QueryWriter {
	private static NistKBPConfiguration config;
	public static String output;
	static DocumentBuilderFactory documentFactory;
	static DocumentBuilder docBuilder;
	
	public QueryWriter(String task){
		//generate default config and filename with timestamp
		config = new NistKBPConfiguration();
		String timeStamp = new SimpleDateFormat("yyyyMMdd_hh:mm").format(new Date());
	    output = config.TEST_DIR + "tac_2014_kbp_english_EDL_" + task + "_"+ timeStamp.replace(":", "_") + ".xml";	
	}	
	
	
	/**
	 * Creates a XML doc builder
	 * 
	 * @return
	 * @throws ParserConfigurationException
	 */
		
	public Document generateDoc() throws ParserConfigurationException {
		documentFactory = DocumentBuilderFactory.newInstance();
	    DocumentBuilder docBuilder;
	    docBuilder = documentFactory.newDocumentBuilder();
		Document doc = docBuilder.newDocument();
		return doc;			
	}
	
	/**
	 * Defines document root node
	 * @param doc XML doc builder
	 */			
	public void setRoot(Document doc){
		//define root node
		Element mainRoot = doc.createElementNS("", "CSFGgeneratedQueries");
		doc.appendChild(mainRoot);
	}
	
	public Element getRoot(Document doc){		
		return doc.getDocumentElement();				
	}
	
	/**
	 * Inserts query node into complete list of queries
	 * 
	 * @param doc  XML doc builder
	 * @param mainRoot  XMl root node
	 * @param queries  list of queries
	 * @param queryCounter number of queries
	 */	
	public void generateElements(Document doc, Element mainRoot, QueryExtractor queries, int queryCounter){
		
		for(int i = 0; i < queryCounter; i++){
			if(queries.getQueryName(i).length() > 1)			
				mainRoot.appendChild(getQuery(doc, queries, i));		
		}
	}
	
	/**
	 * Writes query nodes into XML file
	 * 
	 * @param doc  XML doc builder
	 * @param output XML filename
	 * 
	 */
	public void outputQueryList(Document doc, String output) throws TransformerFactoryConfigurationError, TransformerException{
		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		DOMSource src = new DOMSource(doc);
		StreamResult file = new StreamResult(new File(output));
		transformer.transform(src, file);
	}
	
	/**
	 * Generates a query node to be written in the XML file output
	 * 
	 * @param doc XML doc builder
	 * @param queries query object 
	 * @param i query counter
	 * @return
	 */
	private static Node getQuery(Document doc, QueryExtractor queries, int i){
		
		Element query = doc.createElement("query");
		query.setAttribute("id", queries.getQueryID(i));
		query.appendChild(getQueryInfo(doc, queries, "name", queries.getQueryName(i)));
		query.appendChild(getQueryInfo(doc, queries, "docid", queries.getDocID(i)));
		query.appendChild(getQueryInfo(doc, queries, "beg", queries.getBeginOffSet(i).toString()));
		query.appendChild(getQueryInfo(doc, queries, "end", queries.getEndOffSet(i).toString()));
		
		return query;
		
	}	

	
	/**
	 * Generates query field containing a given value
	 * 
	 * @param doc  XML doc builder
	 * @param queries  list of queries
	 * @param i  querycounter
	 * @param name  field name
	 * @param value  value of the field
	 * @return
	 */
	private static Node getQueryInfo(Document doc, QueryExtractor queries, String name, String value){
		Element info = doc.createElement(name);
		info.appendChild(doc.createTextNode(value));
		return info;
	}

}
