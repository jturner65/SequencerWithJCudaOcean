package SphrSeqFFTVisPKG.staff;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import SphrSeqFFTVisPKG.clef.base.myClefBase;
import SphrSeqFFTVisPKG.clef.enums.keySigVals;
import SphrSeqFFTVisPKG.instrument.myInstrument;
import SphrSeqFFTVisPKG.measure.myMeasure;
import SphrSeqFFTVisPKG.note.myNote;
import SphrSeqFFTVisPKG.note.enums.durType;
import SphrSeqFFTVisPKG.note.enums.noteValType;
import SphrSeqFFTVisPKG.score.myScore;
import SphrSeqFFTVisPKG.ui.myPianoObj;
import SphrSeqFFTVisPKG.ui.base.myMusicSimWindow;
import base_JavaProjTools_IRender.base_Render_Interface.IRenderInterface;

import java.util.SortedMap;

/**
 * fundamental container holding measures of notes and chords for a single instrument
 * @author 7strb
 *
 */
public class myStaff {
	public static IRenderInterface p;
	public myMusicSimWindow win;
	public static int sCnt = 0;
	public int ID;
	public String name;
	public final myInstrument instrument;
	
	public myScore song;		//song that owns this staff	

	public TreeMap<Integer,myMeasure> measures;				//all the measures of this staff, keyed by time
	public TreeMap<Integer,Integer> measureDrawOffset,		//amount to offset every measure for potential key change, time sig change or new clef	
									seqNumToTime,			//list of sequence numbers (key) of measures to their start time in milliseconds
									timeToSeqNum;			//list of start times (key) corresponding to specific measure sequence #s	
	
	public TreeMap<Integer,myTimeSig> timeSigs;				//all the time sigs in this staff, keyed by measure seq#
	public TreeMap<Integer,myKeySig> keySigs;				//all the key sigs of this staff, keyed by measure seq#
	public TreeMap<Integer,myClefBase> clefs;					//all the clefs of this staff, keyed by measure seq#
	public TreeMap<Integer,Float> c4DistForClefs;			//distance for each measure from C4, for displaying notes	
		
	public static float[] topLeftCrnr;						//top left corner in x and y of ledger lines
	public boolean[] stfFlags;								//state flags for this staff
	public static final int 
		enabledIDX = 0,										//whether this staff is enabled or not
		soloIDX = 1,										//whether this staff is soloed
		snapToKeyIDX = 2,									//should notes be snapped to key
		isGrandIDX = 3, 										//is this staff a grand (piano) staff?		
		hasNotesIDX = 4;									//if there are some notes on this staff (not rests)
	
	public static final int numStfFlags = 5;	
	public static final String[] stfBtnLbls = new String[]{": Track Enabled", ": Solo"};
	
	public static float[][] stfSelBoxRect = new float[][]{{0,0,0,0},{0,0,0,0}};
	
	private final float lOff;			//ledger offset in pxls - dist between lines
	private float stfHght;			//staff height
	
	public String hdrDispString;		//display string for header over staff
		
	public myStaff(IRenderInterface _p,myMusicSimWindow _win,myScore _song, myInstrument _instr, String _nm) {
		p=_p;win=_win;
		ID = sCnt++;
		song = _song;
		lOff = myScore.boxStY;  //8.5f*lOff,-lOff,10, 10
		stfSelBoxRect[0] = new float[]{7.5f*lOff,-lOff,10, 10};
		stfSelBoxRect[1] = new float[]{21.5f*lOff,-lOff,10, 10};
		instrument = _instr;
		topLeftCrnr = new float[]{0,lOff*3.0f};
		name =_nm;
		initAllStructs();
		//win.getMsgObj().dispInfoMessage("myStaff","ctor","call ctor in mystaff id : " + ID);		
		putValsInTreemaps(0, 0, new myTimeSig(p, 4,4, durType.getDurTypeForNote(4)),  new myKeySig(p,keySigVals.CMaj), instrument.clef, true, null); 		//initialize time sig and key sig
		initStaffFlags();
		//initial measure of staff
		stfFlags[enabledIDX]=true;		//every staff is initially enabled
		stfHght = myScore.stOff;			//base height of staff is defined in song file
		hdrDispString = instrument.toString();
	}
	public void initStaffFlags(){	stfFlags = new boolean[numStfFlags]; for(int i =0;i<numStfFlags;++i){stfFlags[i]=false;}}
	public float getlOff(){return lOff;}
	
	
	//initialize all treemaps of measure data
	public void initAllStructs(){
		seqNumToTime = new TreeMap<Integer,Integer>();
		timeToSeqNum = new TreeMap<Integer,Integer>();
		measures = new TreeMap<Integer,myMeasure>();		

		timeSigs = new TreeMap<Integer,myTimeSig>();
		keySigs = new TreeMap<Integer,myKeySig>();	
		clefs = new TreeMap<Integer,myClefBase>();	
		c4DistForClefs = new TreeMap<Integer,Float>();
	}

	//set this staff as grand staff
	public void setGrandStaff(){
		stfFlags[isGrandIDX] = true;
		//dfltClef = instrument.clef;
		myMeasure firstMeas = measures.get(0);
		//need to modify all measure values appropriately for this clef
		if(firstMeas == null){
			putValsInTreemaps(0, 0, this.timeSigs.get(0),this.keySigs.get(0), this.clefs.get(0), true, null); 
		} else {
			putValsInTreemaps(firstMeas.msrData.seqNum,firstMeas.msrData.stTime, firstMeas.msrData.ts, firstMeas.msrData.ks, instrument.clef, true, firstMeas);
		}
		stfHght = 2*(myScore.stOff);	
	}
	
	//get current last measure  measures.floorEntry(_n.n.stTime).getValue();
	public myMeasure getCurMeasure(int measNum){if(measNum!= -1) {return measures.get(seqNumToTime.get(measNum));} else {return measures.lastEntry().getValue();}}	
	//
	public void putValsInTreemaps(int seqNum, int stTime, myTimeSig ts, myKeySig keyVal, myClefBase newClef, boolean addMeasure, myMeasure _meas){ 
		//if(ID==0){win.getMsgObj().dispInfoMessage("myStaff","putValsInTreemaps","===\tputValsInTreemaps seqNum : "+seqNum+" stTime : "+stTime);}
		addNewTimeSeq(seqNum, stTime);
		addTSIfNew(seqNum, ts);
		addKSIfNew(seqNum,keyVal);
		addClefIfNew(seqNum,newClef);
		if(!addMeasure){return;}
		if(_meas != null){
			measures.put(stTime,_meas);				
		} else {
			myMeasure tmp = new myMeasure(win, seqNum, seqNumToTime.get(seqNum), this);	
			measures.put(stTime,tmp);	
		}
	}//putValsInTreemaps
	
	//only add if new
	private void addTSIfNew(int seqNum, myTimeSig ts){
		Integer tmp = timeSigs.floorKey(seqNum-1);
		if((tmp == null) || !(timeSigs.get(tmp).equals(ts))){	timeSigs.put(seqNum,ts);}		
	}
	private void addKSIfNew(int seqNum, myKeySig ks){
		Integer tmp = keySigs.floorKey(seqNum-1);
		if((tmp == null) || !(keySigs.get(tmp).equals(ks))){	keySigs.put(seqNum,ks);	}		
	}
	private void addClefIfNew(int seqNum, myClefBase clf){
		Integer tmp = clefs.floorKey(seqNum-1);
		if((tmp == null) || !(clefs.get(tmp).equals(clf))){		clefs.put(seqNum,clf);	c4DistForClefs.put(seqNum, clf.getC4Mult());	}		
	}
	
	private void addNoteToMeasure(myNote _n, int timeToAddNote, myMeasure meas){
		//if(ID==0){win.getMsgObj().dispInfoMessage("myStaff","addNoteToMeasure","" + _n.n.nameOct+"\ttimeToAddNote :"+timeToAddNote+"meas to add :"+meas);}
		myNote addNewNote = meas.addNote(_n, timeToAddNote);			//addNewNote is note that needs to be added past this measure's bounds
		if(null != addNewNote){addNoteAtNoteTime(addNewNote);}	
		else {stfFlags[hasNotesIDX] = true;}

	}//addNoteToMeasure
	
	public void addNoteAtNoteTime(myNote _n){
		int noteStTime = _n.n.stTime;
		if(measures.isEmpty()){	addMeasure();	}//add a new measure if empty
		while(measures.get(measures.lastKey()).msrData.endTime <= noteStTime ){	addMeasure();	}//add measures until we have the measure corresponding with the measure being added
		myMeasure meas = measures.floorEntry(_n.n.stTime).getValue();
		//win.getMsgObj().dispInfoMessage("myStaff","addNoteAtNoteTime","addNoteAtNoteTime : note : "+_n.toString() + "\n meas seqNum : " +seqNum+ " : meas : "+meas.toString());
		addNoteToMeasure(_n, _n.n.stTime, meas);
	}
	
	//add new measure at end of list of measures - perpetuate settings from previous measure or use settings from lists
	public void addMeasure(){
		//get sequence number corresponding to start time -> verify that stTime == endTime of last measure or == 0 from last measure in list
		myMeasure lastM = measures.get(measures.lastKey());
		if(lastM == null){
			//win.getMsgObj().dispInfoMessage("myStaff","addMeasure","measures is empty, adding new measure");		//no measures in list, so first measure			
			putValsInTreemaps(0, 0, this.timeSigs.get(0),this.keySigs.get(0), this.clefs.get(0), true, null); 		//initialize time sig and key sig			
		} else {
			//win.getMsgObj().dispInfoMessage("myStaff","addMeasure","adding new measure next to last measure -->"+lastM.toString());			
			myMeasure m = new myMeasure(lastM, lastM.msrData.endTime, lastM.msrData.seqNum + 1);
			addMeasure(m);				
		}
	}	
	private void addMeasure(myMeasure m){
		putValsInTreemaps(m.msrData.seqNum,m.msrData.stTime, m.msrData.ts, m.msrData.ks, m.msrData.clef, true, m);
	}	
	
	//return an array of keys in measure struct corresponding to st time and end time
	public int[] getMsrTimeKeys (float stTime, float endTime){
		int[] res = new int[]{0,0};
		Integer tmp1 = measures.floorKey(Math.round(stTime)), 
				tmp2 = measures.ceilingKey(Math.round(endTime));
		res[0] = (tmp1 == null? measures.firstKey() : tmp1);
		res[1] = (tmp2 == null? measures.lastKey()+1 : tmp2+1);
		return res;		
	}//getMsrTimeKeys
	
	//debug method to print out all notes in notesGlblLoc
	public void debugAllNoteVals(){
		SortedMap<Integer,myNote> allNotes = getAllNotesAndClear(-1,100000000, false);
		for(Map.Entry<Integer,myNote> noteVal : allNotes.entrySet()) {
			win.getMsgObj().dispInfoMessage("myStaff","debugAllNoteVals","Key value in allNotes : " + noteVal.getKey() + "\nNote : " + noteVal.getValue().toString()+ "\n");
		}	
	}
	
	public void debugAllMeasVals(){
		SortedMap<Integer,myMeasure> measureSubMap = getAllMeasures(-1, 100000000);		
		for(Map.Entry<Integer,myMeasure> msrVals : measureSubMap.entrySet()) {
			win.getMsgObj().dispInfoMessage("myStaff","debugAllMeasVals","Key in measureSubMap : " + msrVals.getKey() + "\nMeasure : " + msrVals.getValue().toString() + "\n");
		}
		win.getMsgObj().dispInfoMessage("myStaff","debugAllMeasVals","Print out seqNumToTime : \n");
		for(Map.Entry<Integer,Integer> timeVal : seqNumToTime.entrySet()) {
			win.getMsgObj().dispInfoMessage("myStaff","debugAllMeasVals","Key value in seqNumToTime : " + timeVal.getKey() + "\nValue : " + timeVal.getValue().toString()+ "\n");
		}	
		win.getMsgObj().dispInfoMessage("myStaff","debugAllMeasVals","Print out timeToSeqNum : \n");
		for(Map.Entry<Integer,Integer> seqVal : timeToSeqNum.entrySet()) {
			win.getMsgObj().dispInfoMessage("myStaff","debugAllMeasVals","Key value in timeToSeqNum : " + seqVal.getKey() + "\nValue : " + seqVal.getValue().toString()+ "\n");
		}	
	}
	
	//get all measures between a specific start and end time
	public SortedMap<Integer,myMeasure> getAllMeasures(float stTime, float endTime){
		int[] keys = getMsrTimeKeys (stTime, endTime);
		//if(ID==0){win.getMsgObj().dispInfoMessage("myStaff","getAllMeasures","\nStart " + ID + " between times : " + stTime +"," + endTime +" derived keys :  "+ keys[0] + "|" + keys[1]);}
		SortedMap<Integer,myMeasure> res = measures.subMap(keys[0],keys[1]);
		return res;
	}	
	
	//get sequential sorted tree map of all notes in this staff - this clears all notes out of measures if clear is true
	public SortedMap<Integer,myNote> getAllNotesAndClear(float stTime, float endTime, boolean clear){
		SortedMap<Integer,myMeasure> measureSubMap = getAllMeasures(stTime, endTime);		
		SortedMap<Integer,myNote> allNotes = new TreeMap<Integer,myNote>(), tmpMap;
		for(Map.Entry<Integer,myMeasure> msrVals : measureSubMap.entrySet()) {
			tmpMap = msrVals.getValue().getAllNotes();
			allNotes.putAll(tmpMap);
		}				
		if(clear){
			//if(ID==0){win.getMsgObj().dispInfoMessage("myStaff","getAllNotesAndClear","\t reset all measures");}
			for(Map.Entry<Integer,myMeasure> msrVals : measureSubMap.entrySet()) {	
				msrVals.getValue().buildRestAndEndTime();
			}		
		}
		return allNotes;
	}//getAllNotesAndClear
	//get all notes from measures and re-add them
	private void reAddAllNotes(){
		//if(ID==0){win.getMsgObj().dispInfoMessage("myStaff","reAddAllNotes","Start reAddAllNotes in staff : " + ID);}
		//get all notes in staff - should clear out all measures
		SortedMap<Integer,myNote> allNotes = getAllNotesAndClear(-1,100000000, true);
		//re-add all notes into existing staff structure
		//if(ID==0){win.getMsgObj().dispInfoMessage("myStaff","reAddAllNotes","start to readd all notes in staff : " + ID + " with # notes : " + allNotes.size());}
		for(Map.Entry<Integer,myNote> note : allNotes.entrySet()) {
			addNoteAtNoteTime(note.getValue());
		}	
		//if(ID==0){win.getMsgObj().dispInfoMessage("myStaff","reAddAllNotes","end readd all notes in staff : " + ID + " with # notes : " + allNotes.size());	}	
	}//reAddAllNotes
	//use measures directly to rebuild  TimeToSeq ara and measures array with new start times
	private void rebuildTimeToSeqNumMsrsMaps(){
		timeToSeqNum = new TreeMap<Integer,Integer>();
		for(Map.Entry<Integer,Integer> seqToTimeVals : seqNumToTime.entrySet()) {
			timeToSeqNum.put(seqToTimeVals.getValue(), seqToTimeVals.getKey());
		}		
		TreeMap<Integer,myMeasure> tmpMsrMap = new TreeMap<Integer,myMeasure>();
		for(Map.Entry<Integer,myMeasure> msrVals : measures.entrySet()){
			myMeasure meas = msrVals.getValue();
			tmpMsrMap.put(meas.msrData.stTime,meas);
		}
		measures = tmpMsrMap;
	}
	
	//forces all notes to be in specified key, overrides existing key settings for every measure	
	public void forceNotesSetKey(myKeySig _key, ArrayList<noteValType> keyNotesAra, boolean moveUp, myPianoObj dispPiano){				
		for(Map.Entry<Integer,myMeasure> measure : measures.entrySet()) {
			measure.getValue().forceNotesToKey( new myKeySig(_key), keyNotesAra, moveUp, dispPiano);
		}	
		keySigs = new TreeMap<Integer,myKeySig>();	
		keySigs.put(0, _key);
	}
	
	//forces all notes to be in specified time sig, overrides existing time settings for every measure	
	public void forceNotesSetTimeSig(myTimeSig _ts){
		int newStTime = 0;
		for(Map.Entry<Integer,myMeasure> msrVals : measures.entrySet()) {
			myMeasure msr = msrVals.getValue();
			Integer oldTime = seqNumToTime.put(msr.msrData.seqNum, newStTime);
			newStTime = msr.setTimeSig(new myTimeSig(_ts),newStTime);
			//if(ID==0){win.getMsgObj().dispInfoMessage("myStaff","forceNotesSetTimeSig","new start time : " + newStTime);}
		}
		rebuildTimeToSeqNumMsrsMaps();		//rebuild this map with new times for each seq num from seqNumToTime map	
		//get rid of all timesigs formerly specified
		timeSigs = new TreeMap<Integer,myTimeSig>();
		timeSigs.put(0, _ts);
		reAddAllNotes();
	}

	//change timesig from time measure on - need to rebuild measure structure from stTime on, and re-add notes
	public void setTimeSigAtTime(float stTime, myTimeSig newTS){		
		Integer tmp1 = measures.floorKey(Math.round(stTime));
		int stKeyTime = (tmp1 == null? measures.firstKey() : tmp1),			//this is time, not seqnum
			stSeqNum = timeToSeqNum.get(stKeyTime);	
		Integer tmp2 = timeSigs.ceilingKey(stSeqNum + 1);
		int endSeqNum = (tmp2 == null? timeToSeqNum.get(measures.lastKey()) : tmp2),
			endKeyTime = seqNumToTime.get(seqNumToTime.ceilingKey(endSeqNum));		//these are the measures that must be changed			
		
		SortedMap<Integer,myMeasure> measureSubMap = getAllMeasures(stKeyTime,endKeyTime+1);
		//if(ID==0){win.getMsgObj().dispInfoMessage("myStaff","setTimeSigAtTime","\nStart setTimeSigAtTime all notes in staff : " + ID + " ts : " + newTS + " at key time : " + stKeyTime + " to time : " + endKeyTime + " size of msrs : " + measureSubMap.size() );}
		int newStTime = stKeyTime;
		//if(ID==0){win.getMsgObj().dispInfoMessage("myStaff","setTimeSigAtTime","first new start time : " + newStTime);}
		for(Map.Entry<Integer,myMeasure> msrVals : measureSubMap.entrySet()) {
			myMeasure msr = msrVals.getValue();
			Integer oldTime = seqNumToTime.put(msr.msrData.seqNum, newStTime);
			newStTime = msr.setTimeSig(new myTimeSig(newTS),newStTime);
			//if(ID==0){win.getMsgObj().dispInfoMessage("myStaff","setTimeSigAtTime","new start time : " + newStTime);}
		}
		rebuildTimeToSeqNumMsrsMaps();		//rebuild this map with new times for each seq num from seqNumToTime map		
		putValsInTreemaps(stSeqNum,stKeyTime, newTS, keySigs.get(stSeqNum), clefs.get(stSeqNum), false, null);
		//get all notes and re-add them
		reAddAllNotes();				
	}//setTimeSigAtMeasure
	//timeToSeqNum
	//change keysig at specified time - change only until next specified key sig
	public void setKeySigAtTime(float stTime, myKeySig newKey){
		Integer tmp1 = measures.floorKey(Math.round(stTime));
		int stKeyTime = (tmp1 == null? measures.firstKey() : tmp1),			//this is time, not seqnum
			stSeqNum = timeToSeqNum.get(stKeyTime);	
		Integer tmp2 = keySigs.ceilingKey(stSeqNum + 1);
		int endSeqNum = (tmp2 == null? timeToSeqNum.get(measures.lastKey()) : tmp2),
			endKeyTime = seqNumToTime.get(seqNumToTime.ceilingKey(endSeqNum));		//these are the measures that must be changed		
			
		SortedMap<Integer,myMeasure> measureSubMap = getAllMeasures(stKeyTime,endKeyTime+1);
		//win.getMsgObj().dispInfoMessage("myStaff","setKeySigAtTime","Start setKeySigAtTime all notes in staff : " + ID + " ts : " + newKey + " at key time : " + stKeyTime + " to time : " + endKeyTime + " size of msrs : " + measureSubMap.size() );
		for(Map.Entry<Integer,myMeasure> msrVals : measureSubMap.entrySet()) {
			msrVals.getValue().setKeySig(new myKeySig(newKey));
		}
		putValsInTreemaps(stSeqNum,stKeyTime, timeSigs.get(stSeqNum), newKey, clefs.get(stSeqNum), false, null);	
	}//	setKeySigAtTime
	
	public void setClefAtTime(float stTime, myClefBase newClef){
		Integer tmp1 = measures.floorKey(Math.round(stTime));
		int stKeyTime = (tmp1 == null? measures.firstKey() : tmp1),			//this is time, not seqnum
			stSeqNum = timeToSeqNum.get(stKeyTime);	
		Integer tmp2 = clefs.ceilingKey(stSeqNum + 1);
		int endSeqNum = (tmp2 == null? timeToSeqNum.get(measures.lastKey()): tmp2),
			endKeyTime = seqNumToTime.get(seqNumToTime.ceilingKey(endSeqNum));		//these are the measures that must be changed			
		SortedMap<Integer,myMeasure> measureSubMap = getAllMeasures(stKeyTime,endKeyTime+1);
		//win.getMsgObj().dispInfoMessage("myStaff","setClefAtTime","Start setClefAtTime all notes in staff : " + ID + " ts : " + newClef + " at key time : " + stKeyTime + " to time : " + endKeyTime + " size of msrs : " + measureSubMap.size() );
		for(Map.Entry<Integer,myMeasure> msrVals : measureSubMap.entrySet()) {
			msrVals.getValue().msrData.setClef(newClef);
		}
		putValsInTreemaps(stSeqNum,stKeyTime, timeSigs.get(stSeqNum), keySigs.get(stSeqNum), newClef, false, null);	
	}//setClefAtTime
	
	//should return largest time/seqnum earlier than passed time/seqnum, whether measure exists there or not.  if no measure exists, at checked time, will have same values as last set time
	public Integer getSeqNumAtTime(float time){if(time < 0){win.getMsgObj().dispInfoMessage("myStaff","getSeqNumAtTime","neg time at getSeqNumAtTime : " + time ); time = 0;}return this.timeToSeqNum.get(timeToSeqNum.floorKey((int)time));}				//list of sequence numbers (key) of measures to their start time in milliseconds
	public Integer getTimeAtSeqNum (int seqNum){return this.seqNumToTime.get(seqNumToTime.floorKey(seqNum));}			//list of start times (key) corresponding to specific measure sequence #s		
	public myTimeSig getTimeSigAtSeqNum(int seqNum){return this.timeSigs.get(timeSigs.floorKey(seqNum));}				//all the time sigs in this staff, keyed by measure seq#
	public myKeySig getKeySigsAtSeqNum(int seqNum){return this.keySigs.get(keySigs.floorKey(seqNum));}				//all the key sigs of this staff, keyed by measure seq#
	public myClefBase getClefsAtSeqNum(int seqNum){return this.clefs.get(clefs.floorKey(seqNum));}					//all the clefs of this staff, keyed by measure seq#
	public Float getC4DistForClefsAtSeqNum(int seqNum){return this.c4DistForClefs.get(c4DistForClefs.floorKey(seqNum));}			//distance for each measure from C4, for displaying notes

	public myTimeSig getTimeSigAtTime(float time){return getTimeSigAtSeqNum(getSeqNumAtTime(time));	}				//all the time sigs in this staff, keyed by measure seq#
	public myKeySig getKeySigsAtTime(float time){return getKeySigsAtSeqNum(getSeqNumAtTime(time));}				//all the key sigs of this staff, keyed by measure seq#
	public myClefBase getClefsAtTime(float time){return getClefsAtSeqNum(getSeqNumAtTime(time));}					//all the clefs of this staff, keyed by measure seq#
	public Float getC4DistForClefsAtTime(float time){return getC4DistForClefsAtSeqNum(getSeqNumAtTime(time));}			//distance for each measure from C4, for displaying notes

	//draw staff lines
	public void drawLdgrLines(){
		p.pushMatState();
			for(int i =0; i<5; ++i){p.drawLine(0,0,0,win.getRectDim(2),0,0);	p.translate(0, lOff);	}	
		p.popMatState();
		//leading staff bar
		p.pushMatState();
			p.setStrokeWt(2);
			p.drawLine(0,0,0, 0,lOff*4,0);
		p.popMatState();	
	}
	
	public void drawGrandStaffLdgrLines(){
		p.pushMatState();
			for(int i =0; i<5; ++i){p.drawLine(0,0,0,win.getRectDim(2),0,0);	p.translate(0, lOff);	}	
			p.translate(0, lOff*2.0f);
			for(int i =0; i<5; ++i){p.drawLine(0,0,0,win.getRectDim(2),0,0);	p.translate(0, lOff);	}	
		p.popMatState();
		p.pushMatState();
			p.setStrokeWt(2);
			p.drawLine(0,0,0, 0,lOff*10,0);
		p.popMatState();	
	}
	public void drawGrandClefKey(){
		
	}
	private void drawStfSelBox(int idx){
		p.pushMatState();
		p.setColorValFill(IRenderInterface.gui_Black, 255);
		p.showText(stfBtnLbls[idx], stfSelBoxRect[idx][0]+ 1.5f*lOff, 0);
		p.setColorValFill((stfFlags[idx] ? IRenderInterface.gui_LightGreen : IRenderInterface.gui_LightRed), 255);
	    p.drawRect(stfSelBoxRect[idx]);
		p.popMatState();	
	}	
	
	private void drawHeader(){	
		p.pushMatState();
		//p.text(name, 0, 0);
		p.showText(""+name+ "|" + this.hdrDispString, 0, 0);
		p.translate(((name.length() + this.hdrDispString.length()) * .7f * lOff), 0);
		drawStfSelBox(0);
		drawStfSelBox(1);
		p.popMatState();	
	}

	//draw all measures' notes for piano roll
	public void drawMeasPRL(){		for(Map.Entry<Integer,myMeasure> measure : measures.entrySet()) {measure.getValue().drawNotesPRL(p);}}
	public void drawMeasures(){
		p.pushMatState();
		//need to go through key, time, etc values
		float ksTsClfOffset = 0, measStOffset = 0;//TODO this is the initial offset required because of new keysig, timesig, or clef
		myTimeSig ts;
		myKeySig ks;
		myClefBase clf, dftlClef = clefs.get(0);
		
		for(Map.Entry<Integer,myMeasure> measure : measures.entrySet()) {
			ksTsClfOffset = 0;
			myMeasure meas = measure.getValue();
			ts = timeSigs.get(meas.msrData.seqNum);
			ks = keySigs.get(meas.msrData.seqNum);	
			clf = clefs.get(meas.msrData.seqNum);	
			if(null != clf){
				clf.drawMe(p,ksTsClfOffset);
				dftlClef = clf;
				ksTsClfOffset += clf.drawDim[2];//add width of clef to offset width
			}
			if(null != ks){
				ks.drawMe(ksTsClfOffset, dftlClef.occsOffset);
				ksTsClfOffset += ks.drawDim[2];//add width of key sig disp to offset width			
			}
			if(null != ts){
				ts.drawMe(ksTsClfOffset);
				ksTsClfOffset += ts.drawDim[2];//add width of time sig to offset width
			}
						
			meas.drawMe(p,measStOffset + ksTsClfOffset);
			p.translate(meas.dispWidth+ ksTsClfOffset, 0);	//translate to next measure
			//measStOffset += meas.dispWidth;
		}	
		p.popMatState();	
	}//drawMeasures

	//draw staff here
	public void drawStaff(){
		p.pushMatState();
			p.setColorValFill(IRenderInterface.gui_Black, 255);
			p.setColorValStroke(IRenderInterface.gui_Black, 255);
			p.setStrokeWt(1);
			p.translate(0, lOff);
			drawHeader();
			p.translate(0, lOff*2);
			if(stfFlags[isGrandIDX] ) {
				drawGrandStaffLdgrLines();
				drawGrandClefKey();
			} else {
				drawLdgrLines();
			}
			drawMeasures();			
		p.popMatState();	
		
	}
	//playback - returns all notedata for all notes and chords in this measure sorted by start time
	//play all notes via staff's instrument
	public void play(float curPBEPlayTime){	}

	// add new sequence # and new start time
	public void addNewTimeSeq(int newSeq, int sttime){
		Integer oldTime = seqNumToTime.put(newSeq, sttime);
		if(oldTime!=null){		timeToSeqNum.remove(oldTime);	}			//for remapping measures when changing time sig
		timeToSeqNum.put(sttime, newSeq);
	}

	public String toString(){
		String res = "Staff ID : "+ID;
		return res;
	}	
}//myStaff class