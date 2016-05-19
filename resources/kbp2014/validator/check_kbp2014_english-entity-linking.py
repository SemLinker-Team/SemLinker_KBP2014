import re
import sys
from bs4 import BeautifulSoup

def check(tab_file, xml_file):
    tab = dict()
    f = open(tab_file, 'r')
    for line in f:
        line = line.strip().split('\t')
        query_id = line[0]
		## for usage with test files (output.tab output.xml)
       	# if not query_id.startswith("EL14_ENG_"):
		#
		## for usage with 2014 training files:
		## tac_2014_kbp_english_EDL_training_KB_links.tab 
		## tac_2014_kbp_english_EDL_training_queries.xml        
	if not query_id.startswith("EDL14_ENG_"):
            print "Invalid query id %s" % str('\t'.join(line))
        if query_id in tab.keys():
            print "Duplicate query id %s" % str('\t'.join(line))
        kb_id = line[1]
        if kb_id.startswith('E'):
            if re.match("E\d{7}", kb_id) == None or len(kb_id) > 8:
                print "Invalid kb id %s" % str('\t'.join(line))
        else:
           if re.match("NIL\d+", kb_id) == None:
               print "Invalid kb id %s" % str('\t'.join(line))
        entity_type = line[2]
        if entity_type not in ["PER", "ORG", "GPE"]:
            print "Invalid entity type %s" % str('\t'.join(line))
        if len(line) == 3:
            tab[line[0]] = (line[1], line[2], 1.0)
        elif len(line) == 4:
            if float(line[3]) < 0 or float(line[3]) > 1.0:
                print "Invalid confidence %s" % str('\t'.join(line))
            tab[line[0]] = (line[1], line[2], line[3])
        else:
            print "Invalid tab file %s" % str('\t'.join(line))

    xml = set()
    soup = BeautifulSoup(open(xml_file))
    queries = soup.find_all("query")
    if len(queries) != len(tab):
        print "Number of queries of tab and xml are not match"
    for q in queries:
        if q["id"] not in tab.keys():
            print "Cannot find query id %s in tab file" % q["id"]
        if q["id"] in xml:
            print "Duplicate query id %s" % str(q)
        xml.add(q["id"])



if __name__ == "__main__":
    usage = "USAGE: check_kbp2014_english-entity-linking.py tab_file xml_file"
    if len(sys.argv) == 3:
        if ".tab" in sys.argv[1] and ".xml" in sys.argv[2]:
            check(sys.argv[1], sys.argv[2])
            print "Done."
        else: 
            print usage
    else: 
        print usage
