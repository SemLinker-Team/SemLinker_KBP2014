HOME=./resources/kbp2014
TESTREP=test
SCRIPTREP=scripts/archived_scorers
echo ----------------------------------------------------------
echo ----- Score Global 
echo ----------------------------------------------------------
python $HOME/$SCRIPTREP/el_scorer_seg.py $HOME/entitylinkingeval/tac_2012_kbp_english_evaluation_entity_linking_query_types.tab $HOME/entitylinkingeval/$TESTREP/
echo
echo
echo ----- Score sur PERS ---------------
python $HOME/$SCRIPTREP/el_scorer_seg.py $HOME/entitylinkingeval/tac_2012_kbp_english_evaluation_entity_linking_query_types.tab $HOME/entitylinkingeval/$TESTREP/ --PER
echo ----- Score sur ORG ---------------
python $HOME/$SCRIPTREP/el_scorer_seg.py $HOME/entitylinkingeval/tac_2012_kbp_english_evaluation_entity_linking_query_types.tab $HOME/entitylinkingeval/$TESTREP/ --ORG
echo ----- Score sur GPE ---------------
python $HOME/$SCRIPTREP/el_scorer_seg.py $HOME/entitylinkingeval/tac_2012_kbp_english_evaluation_entity_linking_query_types.tab $HOME/entitylinkingeval/$TESTREP/ --GPE
echo ----- Score sur NIL ---------------
python $HOME/$SCRIPTREP/el_scorer_seg.py $HOME/entitylinkingeval/tac_2012_kbp_english_evaluation_entity_linking_query_types.tab $HOME/entitylinkingeval/$TESTREP/ --NIL
echo ----- Score sur KB ---------------
python $HOME/$SCRIPTREP/el_scorer_seg.py $HOME/entitylinkingeval/tac_2012_kbp_english_evaluation_entity_linking_query_types.tab $HOME/entitylinkingeval/$TESTREP/ --NONIL
echo
echo ----- Score sur News ---------------
python $HOME/$SCRIPTREP/el_scorer_seg.py $HOME/entitylinkingeval/tac_2012_kbp_english_evaluation_entity_linking_query_types.tab $HOME/entitylinkingeval/$TESTREP/ --NW
echo ----- Score sur Web ---------------
python $HOME/$SCRIPTREP/el_scorer_seg.py $HOME/entitylinkingeval/tac_2012_kbp_english_evaluation_entity_linking_query_types.tab $HOME/entitylinkingeval/$TESTREP/ --WB
