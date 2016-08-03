package Project5Pkg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.SortedMap;

import processing.core.PApplet;

//fundamental container holding measures of notes and chords for a single instrument
public class myStaff {
	public static CAProject5 p;
	public static int sCnt = 0;
	public int ID;
	public String name;
	public final myInstr instrument;
	
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
	
	private float lOff,			//ledger offset in pxls - dist between lines
				stfHght;			//staff height
	
	public String hdrDispString;		//display string for header over staff
		
	public myStaff(CAProject5 _p,myScore _song, myInstr _instr, String _nm) {
		p=_p;
		ID = sCnt++;
		song = _song;
		lOff = song.boxStY;  //8.5f*lOff,-lOff,10, 10
		stfSelBoxRect[0] = new float[]{7.5f*lOff,-lOff,10, 10};
		stfSelBoxRect[1] = new float[]{21.5f*lOff,-lOff,10, 10};
		instrument = _instr;
		topLeftCrnr = new float[]{0,lOff*3.0f};
		name =_nm;
		initAllStructs();
		//p.outStr2Scr("call ctor in mystaff id : " + ID);		
		putValsInTreemaps(0, 0, new myTimeSig(p, 4,4, p.getDurTypeForNote(4)),  new myKeySig(p,keySigVals.CMaj), instrument.clef, true, null); 		//initialize time sig and key sig
		initStaffFlags();
		//initial measure of staff
		stfFlags[enabledIDX]=true;		//every staff is initially enabled
		stfHght = song.stOff;			//base height of staff is defined in song file
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
			putValsInTreemaps(firstMeas.m.seqNum,firstMeas.m.stTime, firstMeas.m.ts, firstMeas.m.ks, instrument.clef, true, firstMeas);
		}
		stfHght = 2*(song.stOff);	
	}
	
	//get current last measure  measures.floorEntry(_n.n.stTime).getValue();
	public myMeasure getCurMeasure(int measNum){if(measNum!= -1) {return measures.get(seqNumToTime.get(measNum));} else {return measures.lastEntry().getValue();}}	
	//
	public void putValsInTreemaps(int seqNum, int stTime, myTimeSig ts, myKeySig keyVal, myClefBase newClef, boolean addMeasure, myMeasure _meas){ 
		//if(ID==0){p.outStr2Scr("===\tputValsInTreemaps seqNum : "+seqNum+" stTime : "+stTime);}
		addNewTimeSeq(seqNum, stTime);
		addTSIfNew(seqNum, ts);
		addKSIfNew(seqNum,keyVal);
		addClefIfNew(seqNum,newClef);
		if(!addMeasure){return;}
		if(_meas != null){
			measures.put(stTime,_meas);				
		} else {
			myMeasure tmp = new myMeasure(p, seqNum, seqNumToTime.get(seqNum), this);	
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
		//if(ID==0){p.outStr2Scr("addNoteToMeasure : " + _n.n.nameOct+"\ttimeToAddNote :"+timeToAddNote+"meas to add :"+meas);}
		myNote addNewNote = meas.addNote(_n, timeToAddNote);			//addNewNote is note that needs to be added past this measure's bounds
		if(null != addNewNote){addNoteAtNoteTime(addNewNote);}	
		else {stfFlags[hasNotesIDX] = true;}

	}//addNoteToMeasure
	
	public void addNoteAtNoteTime(myNote _n){
		int noteStTime = _n.n.stTime;
		if(measures.isEmpty()){	addMeasure();	}//add a new measure if empty
		while(measures.get(measures.lastKey()).m.endTime <= noteStTime ){	addMeasure();	}//add measures until we have the measure corresponding with the measure being added
		myMeasure meas = measures.floorEntry(_n.n.stTime).getValue();
		//p.outStr2Scr("addNoteAtNoteTime : note : "+_n.toString() + "\n meas seqNum : " +seqNum+ " : meas : "+meas.toString());
		addNoteToMeasure(_n, _n.n.stTime, meas);
	}
	
	//add new measure at end of list of measures - perpetuate settings from previous measure or use settings from lists
	public void addMeasure(){
		//get sequence number corresponding to start time -> verify that stTime == endTime of last measure or == 0 from last measure in list
		myMeasure lastM = measures.get(measures.lastKey());
		if(lastM == null){
			//p.outStr2Scr("addMeasure : measures is empty, adding new measure");		//no measures in list, so first measure			
			putValsInTreemaps(0, 0, this.timeSigs.get(0),this.keySigs.get(0), this.clefs.get(0), true, null); 		//initialize time sig and key sig			
		} else {
			//p.outStr2Scr("addMeasure : adding new measure next to last measure -->"+lastM.toString());			
			myMeasure m = new myMeasure(lastM, lastM.m.endTime, lastM.m.seqNum + 1);
			addMeasure(m);				
		}
	}	
	private void addMeasure(myMeasure m){
		putValsInTreemaps(m.m.seqNum,m.m.stTime, m.m.ts, m.m.ks, m.m.clef, true, m);
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
			p.outStr2Scr("Key value in allNotes : " + noteVal.getKey() + "\nNote : " + noteVal.getValue().toString()+ "\n");
		}	
	}
	
	public void debugAllMeasVals(){
		SortedMap<Integer,myMeasure> measureSubMap = getAllMeasures(-1, 100000000);		
		for(Map.Entry<Integer,myMeasure> msrVals : measureSubMap.entrySet()) {
			p.outStr2Scr("Key in measureSubMap : " + msrVals.getKey() + "\nMeasure : " + msrVals.getValue().toString() + "\n");
		}
		p.outStr2Scr("Print out seqNumToTime : \n");
		for(Map.Entry<Integer,Integer> timeVal : seqNumToTime.entrySet()) {
			p.outStr2Scr("Key value in seqNumToTime : " + timeVal.getKey() + "\nValue : " + timeVal.getValue().toString()+ "\n");
		}	
		p.outStr2Scr("Print out timeToSeqNum : \n");
		for(Map.Entry<Integer,Integer> seqVal : timeToSeqNum.entrySet()) {
			p.outStr2Scr("Key value in timeToSeqNum : " + seqVal.getKey() + "\nValue : " + seqVal.getValue().toString()+ "\n");
		}	
	}
	
	//get all measures between a specific start and end time
	public SortedMap<Integer,myMeasure> getAllMeasures(float stTime, float endTime){
		int[] keys = getMsrTimeKeys (stTime, endTime);
		//if(ID==0){p.outStr2Scr("\nStart getAllMeasures : " + ID + " between times : " + stTime +"," + endTime +" derived keys :  "+ keys[0] + "|" + keys[1]);}
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
			//if(ID==0){p.outStr2Scr("\tgetAllNotesAndClear : reset all measures");}
			for(Map.Entry<Integer,myMeasure> msrVals : measureSubMap.entrySet()) {	
				msrVals.getValue().buildRestAndEndTime();
			}		
		}
		return allNotes;
	}//getAllNotesAndClear
	//get all notes from measures and re-add them
	private void reAddAllNotes(){
		//if(ID==0){p.outStr2Scr("Start reAddAllNotes in staff : " + ID);}
		//get all notes in staff - should clear out all measures
		SortedMap<Integer,myNote> allNotes = getAllNotesAndClear(-1,100000000, true);
		//re-add all notes into existing staff structure
		//if(ID==0){p.outStr2Scr("start to readd all notes in staff : " + ID + " with # notes : " + allNotes.size());}
		for(Map.Entry<Integer,myNote> note : allNotes.entrySet()) {
			addNoteAtNoteTime(note.getValue());
		}	
		//if(ID==0){p.outStr2Scr("end readd all notes in staff : " + ID + " with # notes : " + allNotes.size());	}	
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
			tmpMsrMap.put(meas.m.stTime,meas);
		}
		measures = tmpMsrMap;
	}
	
	//forces all notes to be in specified key, overrides existing key settings for every measure	
	public void forceNotesSetKey(myKeySig _key, ArrayList<nValType> keyNotesAra, boolean moveUp, myPianoObj dispPiano){				
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
			Integer oldTime = seqNumToTime.put(msr.m.seqNum, newStTime);
			newStTime = msr.setTimeSig(new myTimeSig(_ts),newStTime);
			//if(ID==0){p.outStr2Scr("new start time : " + newStTime);}
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
		//if(ID==0){p.outStr2Scr("\nStart setTimeSigAtTime all notes in staff : " + ID + " ts : " + newTS + " at key time : " + stKeyTime + " to time : " + endKeyTime + " size of msrs : " + measureSubMap.size() );}
		int newStTime = stKeyTime;
		//if(ID==0){p.outStr2Scr("first new start time : " + newStTime);}
		for(Map.Entry<Integer,myMeasure> msrVals : measureSubMap.entrySet()) {
			myMeasure msr = msrVals.getValue();
			Integer oldTime = seqNumToTime.put(msr.m.seqNum, newStTime);
			newStTime = msr.setTimeSig(new myTimeSig(newTS),newStTime);
			//if(ID==0){p.outStr2Scr("new start time : " + newStTime);}
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
		//p.outStr2Scr("Start setKeySigAtTime all notes in staff : " + ID + " ts : " + newKey + " at key time : " + stKeyTime + " to time : " + endKeyTime + " size of msrs : " + measureSubMap.size() );
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
		//p.outStr2Scr("Start setClefAtTime all notes in staff : " + ID + " ts : " + newClef + " at key time : " + stKeyTime + " to time : " + endKeyTime + " size of msrs : " + measureSubMap.size() );
		for(Map.Entry<Integer,myMeasure> msrVals : measureSubMap.entrySet()) {
			msrVals.getValue().m.setClef(newClef);
		}
		putValsInTreemaps(stSeqNum,stKeyTime, timeSigs.get(stSeqNum), keySigs.get(stSeqNum), newClef, false, null);	
	}//setClefAtTime
	
	//should return largest time/seqnum earlier than passed time/seqnum, whether measure exists there or not.  if no measure exists, at checked time, will have same values as last set time
	public Integer getSeqNumAtTime(float time){if(time < 0){p.outStr2Scr("myStaff : neg time at getSeqNumAtTime : " + time ); time = 0;}return this.timeToSeqNum.get(timeToSeqNum.floorKey((int)time));}				//list of sequence numbers (key) of measures to their start time in milliseconds
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
		p.pushMatrix();p.pushStyle();
			for(int i =0; i<5; ++i){p.line(0,0,p.width -p.menuWidth,0);	p.translate(0, lOff);	}	
		p.popMatrix();
		//leading staff bar
		p.pushMatrix();
			p.strokeWeight(2);
			p.line(0,0,0, 0,lOff*4,0);
		p.popStyle();p.popMatrix();	
	}
	
	public void drawGrandStaffLdgrLines(){
		p.pushMatrix();p.pushStyle();
			for(int i =0; i<5; ++i){p.line(0,0,p.width -p.menuWidth,0);	p.translate(0, lOff);	}	
			p.translate(0, lOff*2.0f);
			for(int i =0; i<5; ++i){p.line(0,0,p.width -p.menuWidth,0);	p.translate(0, lOff);	}	
		p.popMatrix();
		p.pushMatrix();
			p.strokeWeight(2);
			p.line(0,0, 0,lOff*10);
		p.popStyle();p.popMatrix();	
	}
	public void drawGrandClefKey(){
		
	}
	private void drawStfSelBox(int idx){
		p.pushMatrix();p.pushStyle();
		p.setColorValFill(CAProject5.gui_Black);
		p.text(stfBtnLbls[idx], stfSelBoxRect[idx][0]+ 1.5f*lOff, 0);
		p.setColorValFill(stfFlags[idx] ? CAProject5.gui_LightGreen : CAProject5.gui_LightRed);
	    p.rect(stfSelBoxRect[idx]);
		p.popStyle();p.popMatrix();	
	}	
	
	private void drawHeader(){	
		p.pushMatrix();p.pushStyle();
		//p.text(name, 0, 0);
		p.text(""+name+ "|" + this.hdrDispString, 0, 0);
		p.translate(((name.length() + this.hdrDispString.length()) * .7f * lOff), 0);
		drawStfSelBox(0);
		drawStfSelBox(1);
		p.popStyle();p.popMatrix();	
	}

	//draw all measures' notes for piano roll
	public void drawMeasPRL(){		for(Map.Entry<Integer,myMeasure> measure : measures.entrySet()) {measure.getValue().drawNotesPRL();}}
	public void drawMeasures(){
		p.pushMatrix();p.pushStyle();
		//need to go through key, time, etc values
		float ksTsClfOffset = 0, measStOffset = 0;//TODO this is the initial offset required because of new keysig, timesig, or clef
		myTimeSig ts;
		myKeySig ks;
		myClefBase clf, dftlClef = clefs.get(0);
		
		for(Map.Entry<Integer,myMeasure> measure : measures.entrySet()) {
			ksTsClfOffset = 0;
			myMeasure meas = measure.getValue();
			ts = timeSigs.get(meas.m.seqNum);
			ks = keySigs.get(meas.m.seqNum);	
			clf = clefs.get(meas.m.seqNum);	
			if(null != clf){
				clf.drawMe(ksTsClfOffset);
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
						
			meas.drawMe(measStOffset + ksTsClfOffset);
			p.translate(meas.dispWidth+ ksTsClfOffset, 0);	//translate to next measure
			//measStOffset += meas.dispWidth;
		}	
		p.popStyle();p.popMatrix();	
	}//drawMeasures

	//draw staff here
	public void drawStaff(){
		p.pushMatrix();p.pushStyle();
			p.setColorValFill(CAProject5.gui_Black);
			p.setColorValStroke(CAProject5.gui_Black);
			p.strokeWeight(1);
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
		p.popStyle();p.popMatrix();	
		
	}
	//playback - returns all notedata for all notes and chords in this measure sorted by start time
	//play all notes via staff's instrument
	public void play(float curPBEPlayTime){
		
		
	}

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


//class to hold a measure of notes for a single instrument - collected into a staff
class myMeasure {
	public static CAProject5 p;
	public static int mCnt = 0;
	public int ID;		
	
	public MeasureData m;			//all measure data for this measure
	
	public myStaff staff;			//owning staff for this measure
	
	public final float minDispLen = 100;		//minimum width of measure
		
	//structure to hold notes in this measure - ordered by location in measure, in ticks
	public TreeMap<Integer,myNote> notes;//, notesGlblLoc;
	
	public float noteStartX,		//x location from beginning of measure bar where first note should start - might be moved if measure has different clef,key,timesig, etc.
				dispWidth;		//display width of measure
	public myMeasure(CAProject5 _p, int _seqNum, int _stTime, myStaff _staff) {
		p=_p;
		ID = mCnt++;
		staff = _staff;
		m = new MeasureData(_p,_seqNum,_stTime,this, staff.getTimeSigAtTime(_stTime), staff.getKeySigsAtTime(_stTime), staff.getClefsAtTime(_stTime), staff.getC4DistForClefsAtTime(_stTime));
		notes = new TreeMap<Integer, myNote>();
		//p.outStr2Scr("Base CTOR, about to buildRestAndEndTime() " + ID);
		buildRestAndEndTime();
		//p.outStr2Scr("Base CTOR, after buildRestAndEndTime() " + ID);
	}
	
	public myMeasure(myMeasure _m, int newStTime, int newSeqNum){ //copy ctor
		this(_m.p, newSeqNum, newStTime, _m.staff);		
		//p.outStr2Scr("copy CTOR : copy meas : "+ _m.seqNum +"\n new  seq num: "+ newSeqNum + "|" + newStTime +" | ID: " + ID);
	}

	public void forceNotesToKey(myKeySig _key, ArrayList<nValType> keyAra, boolean moveUp, myPianoObj dispPiano){
		m.setKeySig(_key);
		//p.outStr2Scr("Key : "+ _key +" : moving notes to be in key");
		for(Map.Entry<Integer,myNote> noteEntry : notes.entrySet()) {
			myNote note = noteEntry.getValue();
			note.moveNoteHalfStep(_key, keyAra,moveUp);//, false, this.staff.song.w.bkModY);	//(boolean up, boolean lowestNote, float bkModY)
			float[] resAra = dispPiano.getRectDimsFromRoll(note.n, note.gridDims[0]);
			note.gridDims = resAra;
		}
	}		
	public float calcOffsetScale(double val, float sc, float off){float res =(float)val - off; res *=sc; return res+=off;}
	
	//returns a map with all the notes in this measure, keyed by their absolute start time
	public TreeMap<Integer,myNote> getAllNotes(){
		TreeMap<Integer,myNote> res = new TreeMap<Integer,myNote>();
		for(Entry<Integer, myNote> noteEntry : notes.entrySet()){
			res.put(noteEntry.getValue().n.stTime, noteEntry.getValue());
		}	
		return res;
	}
	
	public void moveNoteForResize(float scaleY, float topOffY){
		myNote note;
		for(Entry<Integer, myNote> noteEntry : notes.entrySet()) {
			note = noteEntry.getValue();
			float[] noteGridDims = note.gridDims;
			noteGridDims[1]=calcOffsetScale(noteGridDims[1],scaleY,topOffY);
			noteGridDims[3]=calcOffsetScale(noteGridDims[3],scaleY,0);
		}
	}
	public void drawNotesPRL(){	for(Map.Entry<Integer,myNote> note : notes.entrySet()) {  note.getValue().drawMePRL();}	}
	
	//draw all the notes in this measure - return whether a new key,timesig or clef has been specified from previous values : 1 if clef, 2 if timesig, 4 if key sig, bitwise combo of all if multiples otherwise none
	public void drawMe(float xOffset){
		p.pushMatrix();p.pushStyle();		//draw sequence # of measure
			p.setColorValFill(CAProject5.gui_Black);
			p.setColorValStroke(CAProject5.gui_Black);
			p.strokeWeight(1);
			p.scale(.75f);
			p.text(m.seqNum, 0, -staff.getlOff());
		p.popMatrix();		
		p.pushMatrix();p.pushStyle();
			p.translate(xOffset, 0);
			p.pushMatrix();		
				//draw all notes in measure - equally space notes based on how many there are and how large measure is
				//notes need to be as wide as how many "Beats" they have, counting the smallest note as the baseline beat
				float xDisp = (1.0f*dispWidth)/(notes.size()+1);
				p.translate(xDisp,0);
				for(Map.Entry<Integer,myNote> note : notes.entrySet()) {
					  note.getValue().drawMe();
					  p.translate(xDisp,0);			//TODO need to move notes better than this
				}	
			p.popStyle();p.popMatrix();
			p.pushMatrix();p.pushStyle();
				p.translate(dispWidth, 0);				//drawn end line
				p.setColorValFill(CAProject5.gui_Black);
				p.setColorValStroke(CAProject5.gui_Black);
				p.strokeWeight(2);
				p.line(0,0,0,4*staff.getlOff());
			p.popStyle();p.popMatrix();
		p.popStyle();p.popMatrix();
	}
	
	public void setKeySig(myKeySig _ks){
		m.setKeySig(_ks);		
	}
	
	//returns true new end time of this measure
	public int setTimeSig(myTimeSig _timeSig, int newStTime){
//		p.outStr2Scr("setTimeSig before oldDur calc");
//		int oldDur = calcEndTime();
		m.stTime = newStTime;
		m.setTimeSig(_timeSig);
		//if(staff.ID == 0){p.outStr2Scr("setTimeSig before newDur calc");}
		int newDur = calcEndTime();
		return m.endTime; 
	}
	
	//how wide to make the measure display
	private void setDispLen(){	
		dispWidth = PApplet.max(minDispLen, notes.size() * myNote.noteWidth);}
	
	//force all notes in this measure to snap to key
	public void snapToKey(){//TODO
		p.outStr2Scr("snapToKey not implemented in myMeasure", true);
	}
		
	private int calcEndTime(){
		int ticksPerMeasure = (int)(m.ts.tSigMult() * 4 * durType.Quarter.getVal());  //tSigMult gives fraction of 4 qtr notes total that make up measure		
		//int measDur = stTime + (int)((1.0f/tempo) * timeSig.tSigMult() * (p.ticksPerBeat)); 		//tempo is beats per minute, 1/ tempo is minutes per beat, p.ticksPerBeat is ticks per beat
		int measDur = ticksPerMeasure; 		//don't have measure control playback tempo, allow this to be controlled by play back engine
		m.endTime = m.stTime + measDur;
		//if(staff.ID == 0){p.outStr2Scr("Calc End Time meas : "+ m.toString()+ " ticksPerMeasure : "+ ticksPerMeasure );}
		return measDur;
	}
	//prepopulate with rest in note data, calculate end time of this measure
	public void buildRestAndEndTime(){
		//if(staff.ID == 0){p.outStr2Scr("In buildRestAndEndTime");}
		m.setAllVals(staff.getTimeSigAtTime(m.stTime), staff.getKeySigsAtTime(m.stTime), staff.getClefsAtTime(m.stTime), staff.getC4DistForClefsAtTime(m.stTime));
		int measDur = calcEndTime();		//initialize measure to be full of rest
		notes.clear();
		myNote tmpRest = new myNote(p, nValType.rest, 0, this, this.staff);		
		tmpRest.setVals(m.stTime, measDur, false, false, -1);
		putNoteInAra(0,tmpRest);
		//staff.addNoteAtNoteTime(tmpRest);
		setDispLen();		
	}

	//single entrypoint to notes and staff.notesGlblLoc structs
	public myNote putNoteInAra(int stTime, myNote note){
		myNote tmp = notes.put(stTime, note);				//if collision at this time
		//staff.notesGlblLoc.put(note.n.stTime, note);			
		return tmp;
	}
	
	//add note at note's time (note's stTime - this measure's stTime - if past end time, then return endTime, which is next measure's start time, else return -1
	//need to account for rests in empty measure
	public myNote addNote(myNote note, int noteAddTime){
		//p.outStr2Scr("Add Note : " +note.ID+":\t" +note.n.nameOct + "\ttime/dur : "+ note.n.stTime +"/"+note.n.dur +" add Time : " + noteAddTime + "|Meas : seqNum : "+ m.seqNum + " stTime : "+ m.stTime + " endTime : " + m.endTime );
		if(note.flags[myNote.isChord]) {return addChord((myChord)note, noteAddTime);}//note : do not attempt to add a chord of rests
		if(noteAddTime >= m.endTime){								//note starts after this measure ends, add to new measure
			return note;
		}
		myNote newNote = null;										//if note needs to be added after this measure
		int _ntOffset = noteAddTime - m.stTime; 					//offset within this measure
		if(m.endTime < (note.n.dur + noteAddTime)){					// modify end time of note - no tied-across-measure bounds notes
			int newDur = m.endTime - noteAddTime,
				nextDur = note.n.dur - newDur;
			note.setDuration(newDur, false, false,-1); 
			//add rest of note in next measure
			newNote = new myNote(note);
			//TODO completely support tuples and dotted notes
			newNote.setStart(m.endTime); 						//new note starts at end of this measure
			newNote.setDuration(nextDur, false, false,-1); 
		}
		//if same time as another note, make a chord
		myNote tmp = putNoteInAra(_ntOffset, note); 
		if(tmp != null){
			//p.outStr2Scr(" Add Note tmp != null : "+tmp.toString());
			if ((!tmp.flags[myNote.isRest]) && (!note.flags[myNote.isRest])){	//non-rest note already in treemap at this location, and attempting to add new note here, make chord and put in treemap instead, using original note as root or add note to existing chord											
				if(!tmp.flags[myNote.isChord]){						//no chord here, make new chord, put in treemap here
					//p.outStr2Scr("Add Note tmp ! chord: Tmp : " +tmp.toString()+":\tNote :" +note.toString());
					if(!tmp.equals(note)){				//if tmp!=note then make tmp a chord, and add note to temp
						myChord tmpChord = new myChord(tmp);					
						tmpChord.addNote(note);
						putNoteInAra(_ntOffset, tmpChord);
					}
				} else {											//add note to existing chord if different 
					//p.outStr2Scr("Add Note tmp is chord: Tmp : " +tmp.toString()+":\tNote :" +note.toString());
					((myChord)tmp).addNote(note);
					putNoteInAra(_ntOffset, tmp);					//put chord back in notes map
				}				 
			} else if(!tmp.equals(note)) {//either a rest is here or we're attempting to replace a note with a rest;will only return a note if tmp is longer than note (which means that newNote will always be null by here				
				newNote = shiftOldNPastNewN(_ntOffset,tmp,note);}							
		}
		setDispLen();		//reset display size of measure to account for new note
		return newNote;
	}//addNote
	
	private myNote addChord(myChord note, int noteAddTime){
		//p.outStr2Scr("****Add Chord : " +note.ID+":\t" +note.n.nameOct + "\ttime/dur : "+ note.n.stTime +"/"+note.n.dur +" add Time : " + noteAddTime + "|Meas : seqNum : "+ m.seqNum + " stTime : "+ m.stTime + " endTime : " + m.endTime );
		if(noteAddTime >= m.endTime){								//note starts after this measure ends, add to new measure
			return note;
		}
		myNote newNote = null;										//if note needs to be added after this measure
		int _ntOffset = noteAddTime - m.stTime; 					//offset within this measure
		if(m.endTime < (note.n.dur + noteAddTime)){				//modify end time of note - no tied-across-measure bounds notes
			int newDur = m.endTime - noteAddTime,				//dur in this measure
					nextDur = note.n.dur - newDur;				//dur in next measure
				note.setDuration(newDur, false, false,-1); 
				//add rest of note in next measure
				newNote = new myNote(note);
				//TODO completely support tuples and dotted notes
				newNote.setStart(m.endTime); 						//new note starts at end of this measure
				newNote.setDuration(nextDur, false, false,-1); 
		}		
		//if same time as another note or chord, add note's notes to make a (bigger) chord
		myNote tmp = putNoteInAra(_ntOffset, note);
		if(tmp != null){
			if (!tmp.flags[myNote.isRest]){	//non-rest note already in treemap at this location, and attempting to add new note here, make chord and put in treemap instead, using original note as root or add note to existing chord			
				note.addNote(tmp);
				if(tmp.flags[myNote.isChord]){										//tmp is a chord, add tmps notes to note's chord
					tmp.flags[myNote.isChord] = false;
					myChord tmpC = ((myChord)tmp);
					for(int i = 1; i<tmpC.cnotes.size(); ++i){note.addNote(tmpC.cnotes.get(i));}
				}
			} else {//will only return a note if tmp is longer than note (which means that newNote will always be null by here
				newNote = shiftOldNPastNewN(_ntOffset,tmp,note);}						//tmp is a rest, replace with chord
		}		
		setDispLen();		//reset display size of measure to account for new note
		return newNote;	
	}//addChord
		
	//replacing a old val note at location key with a new val note 
	public myNote shiftOldNPastNewN(Integer key, myNote oldN, myNote newN){//if new is longer than old then do nothing
		//p.outStr2Scr("replaceOldNWithNewN : old " + oldN.n.nameOct+ "\ttime/dur : "+ oldN.n.stTime +"/"+oldN.n.dur +" new " + newN.n.nameOct+ "\ttime/dur : "+ newN.n.stTime +"/"+newN.n.dur+"\tkey :"+key);
		if(newN.n.dur < oldN.n.dur){										//move note 
			oldN.modStDur(oldN.n.dur - newN.n.dur, oldN.n.stTime + (int)(newN.n.dur));		//new duration is old duration - new duration, new start is old start + new duration
			return oldN;													//add shifted, shortened note to treemap
		}	
		return null;
	}

	public String toString(){
		String res = "Meas ID : "+ID+ m.toString() + "|Display width : " + String.format("%.2f",dispWidth)+ " #Notes : "+notes.size()+"\n";
		int i =0;
		for(Map.Entry<Integer,myNote> note : notes.entrySet()){
			res += "\t#"+(i++) +" : "+note.getValue().toString()+"\n";
		}		
		return res;
	}	
}//myMeasure class

//fundamental class to hold and process a single note played by a single instrument
class myNote {
	public CAProject5 p;
	public static int nCnt = 0;
	public int ID;		
	public float noteC4DispLoc;		//displacement for this note from C4 for display purposes, governed by owning staff
	public myMeasure meas;		//owning measure
	public NoteData n;			//all specific note data
	public int tupleVal;		//# of notes in space of 2, if tuple (valid are only >= 3)
	public boolean[] flags;
	public static final int isDotted = 0,
							isTuple = 1,
							isRest = 2,
							isChord = 3,
							drawStemUp = 4,			//whether note stem should be drawn up or down ( based on location in staff) - stem should be down only above middle ledger line, or if first note in group is down
							drawCnncted = 5,		//should be connected to neighbor note (flags for 8ths, etc
							showDispMsg = 6,		//show that this note should be played higher/lower than written
							isInStaff = 7,			//note is in range of staff - if false, draw appropriate ledger line through or under note
							isFlipped = 8,			//if note is within 1 note of another chord note that is not flipped, flip this note (put head on other side of stem)
							isOnLdgrLine = 9,		//ledger line goes through note center
							isFromSphere = 10;		//note is made from Sphere UI
	public static final int numFlags = 11;
	public myStaff owningStaff;
	public static float noteWidth;// = staff.getlOff() * 2;			//width of note to display
	//where this note should live on the staff, from lowest ledger line of treble staff as baseline (E4), and then offset based on cleff defined in staff
	public float dispYVal, staffSize;						//translation modifier for note display from top of staff; staffSize to minimize recalc
	public int transMod;						
	public String dispMsg;						//what message to show, if any, above staff for 1 or 2 octave displacement up or down
	
	public float[] gridDims;
	
	//sphere UI values
	public mySphereCntl sphrOwn;
	public float[] sphereDims;
	public float sphereAlpha;
	public int sphereDur;
	public int sphereRing;
	
	
	
	//build note then set duration
	public myNote(CAProject5 _p, nValType _name, int _octave, myMeasure _measure, myStaff _owningStaff) {
		p=_p;
		ID = nCnt++;	
		meas = _measure;
		owningStaff = _owningStaff;
		sphrOwn = null;
		noteWidth = owningStaff.getlOff() * p.ntWdthMult;		//modify based on duration ? TODO
		n = new NoteData(p,_name, _octave);
		tupleVal = -1;
		gridDims = new float[]{0,0,0,0};
		sphereDims = new float[]{0,0,0,0};
		sphereAlpha = 0;
		sphereDur = 0;
		sphereRing = 0;
		
		flags = new boolean[numFlags];
		initFlags();
		if (n.name == nValType.rest){	setRest();}
		setDispMsgVals(owningStaff.getC4DistForClefsAtTime(n.stTime));
		//not every note has an owning measure, but every note has an owning staff
		flags[isInStaff] = (owningStaff.getClefsAtTime(n.stTime).isOnStaff(n) == 0);
	}	
	//ctor for note data for notes in spherical UI
	public myNote(CAProject5 _p, float _alphaSt, float _alphaEnd, int _ring, mySphereCntl _sphrOwn){
		p=_p;
		ID = nCnt++;	
		meas = null;	
		sphrOwn = _sphrOwn;
		setSphereDims(_alphaSt,  _alphaEnd,  _ring);		
		flags = new boolean[numFlags];
		initFlags();
		flags[isFromSphere] = true;
	}
	
	public myNote(myNote _note){
		p=_note.p;
		ID = nCnt++;	
		meas = _note.meas;
		owningStaff = _note.owningStaff;
		sphrOwn = _note.sphrOwn;
		noteWidth = owningStaff.getlOff() * p.ntWdthMult;		
		n = new NoteData(_note.n);
		tupleVal = _note.tupleVal;
		gridDims = new float[]{0,0,0,0};
		sphereDims = new float[]{0,0,0,0};
		for(int i =0; i<_note.gridDims.length;++i){
			gridDims[i]=_note.gridDims[i];
			sphereDims[i]=_note.sphereDims[i];
		}
		sphereAlpha = _note.sphereAlpha;
		sphereDur = _note.sphereDur;
		sphereRing = _note.sphereRing;
		flags = new boolean[numFlags];
		for(int i =0; i<numFlags;++i){	flags[i]=_note.flags[i];}
		if (n.name == nValType.rest){	setRest();}
		setDispMsgVals(owningStaff.getC4DistForClefsAtTime(_note.n.stTime));
		flags[isInStaff] = (owningStaff.getClefsAtTime(_note.n.stTime).isOnStaff(n) == 0);
	}	
	//sphere dims are alphaSt, alphaEnd, ring, thickness
	public void setSphereDims(float _alphaSt, float _alphaEnd, int _ring){
		sphereAlpha = _alphaSt;
		if(sphereAlpha < 0){ sphereAlpha = p.TWO_PI + sphereAlpha;}		//add 2 pi if negative
		float tmpAlphaEnd = _alphaEnd;
		if(tmpAlphaEnd < 0){ tmpAlphaEnd = p.TWO_PI + tmpAlphaEnd;}		//add 2 pi if negative
		//if(tmpAlphaEnd < sphereAlpha){float tmp = sphereAlpha; sphereAlpha = tmpAlphaEnd; tmpAlphaEnd=tmp;}			
		if(tmpAlphaEnd < sphereAlpha){tmpAlphaEnd = p.TWO_PI + tmpAlphaEnd;}
		sphereDims = new float[]{sphereAlpha,tmpAlphaEnd,(_ring + .5f)*sphrOwn.ringRad,sphrOwn.ringRad};
		sphereRing = _ring;	
		sphereDur = sphrOwn.getTickFromAlpha(sphereDims[1] - sphereDims[0]);
		buildSphereNoteName();
	}
	//build the note data given the loaded sphere dims
	public void buildSphereNoteName(){
		if(sphrOwn == null){ p.outStr2Scr("Error buildSphereNoteName : sphrOwn is null"); return;}
		myClefBase clef = sphrOwn.instr.clef;
		n = new NoteData(clef.sphereMidNote);		//use the middle note of the clef as the starting point, then assign that note data value to the middle of the note rings, find disp of actual ring from middle, and displace note data accordingly
		int numRings = sphrOwn.numNoteRings,
			numNotesDisp = -(numRings/2) + sphereRing;	
		if(numNotesDisp >= 12){numNotesDisp -= 12; n.editNoteVal(n.name, n.octave+1);}
		if(numNotesDisp < 0){
			n.editNoteVal(n.name, n.octave-1);
			if(numNotesDisp < -12){numNotesDisp += 12; n.editNoteVal(n.name, n.octave-1);}
		}
		int[] indNDisp = p.getNoteDisp(n, numNotesDisp);		
		this.n.editNoteVal(nValType.getVal(indNDisp[0]), indNDisp[1]);
		//p.outStr2Scr("new note : "+ n.toString() + " #rings : "+sphereRing + " notes disp : " +numNotesDisp);
	}
	
	public float getSphereNoteDur(){return (sphereDims[1]-sphereDims[0]);	}
	
	//must be called after setFlags, set display message and displacement vals
	public void setDispMsgVals(float c4DspLc){//limit octave to 8
		//p.outStr2Scr("setDispMsgVals : " + this.ID + "  c4DspLc : " + c4DspLc);
		if(flags[isRest]){flags[showDispMsg] = false;dispMsg = "";transMod = 0;return;}
		switch (n.octave){
			case 0 : {flags[showDispMsg] = true; dispMsg = "15mb"; transMod = 2;break;}
			case 1 : {flags[showDispMsg] = true; dispMsg = "8vb";transMod = 1;break;}
			case 7 : {flags[showDispMsg] = true; dispMsg = "8va";transMod = -1;break;}
			case 8 : {flags[showDispMsg] = true; dispMsg = "15ma";transMod = -2;break;}
			default : {flags[showDispMsg] = false; dispMsg = "";transMod = 0;break;}		
		}		
		calcDispYVal(c4DspLc);
	}
	public void moveNoteHalfStep(myKeySig _key, ArrayList<nValType> keyAra, boolean up){moveNoteHalfStepPriv(_key,keyAra,up);}
	protected void moveNoteHalfStepPriv(myKeySig _key, ArrayList<nValType> keyAra, boolean up){
		//p.outStr2Scr("Before move:  " + n.toString());
		if(flags[isRest]){return;}
		if(!keyAra.contains(n.name)){
			n.moveHalfStep(up);
			setDispMsgVals(owningStaff.getC4DistForClefsAtTime(n.stTime));
			flags[isInStaff] = (owningStaff.getClefsAtTime(n.stTime).isOnStaff(n) == 0);
			//p.outStr2Scr("After move:  " + n.toString());
			//need to recalc rectangle dims
			int mult = (up? -1 : 1);		
			gridDims[1] += mult * gridDims[3];			//need to resize grid dims!
	//		if(p.isNaturalNote(n.name)){
	//			if (p.chkHasSharps(n.name)){gridDims[1] += bkModY; gridDims[3] -= bkModY;}//increase y0, decrease y1 coord to make room for black key
	//			if (p.chkHasFlats(n.name) && (!lowestNote)){gridDims[3] -= bkModY;}//decrease y1 coord to make room for black key							
	//		}
		}
		//p.outStr2Scr("After move:  " + n.toString());
	}
	//add grid dimensions relating to duration-  idx 2 (length in x of added note)
	
	public void addDurGridDims(float[] _gridDims){addDurGridDims(_gridDims,1);}
	public void addDurGridDims(float[] _gridDims, int mult){gridDims[2] += mult* _gridDims[2];	}	
	
	//calculate appropriate displacement in Y for this note, based upon note value and octave
	protected void calcDispYVal(float c4DispMult){
		int tmpOctave = n.octave + transMod - 4;		//starting at c4
		float tmpDisp = tmpOctave * 7* .5f;		//# of ledger lines per octave is 7
		tmpDisp +=  getLedgerLine() * .5f;
		if(!flags[isFromSphere]){
			noteC4DispLoc = c4DispMult * owningStaff.getlOff();
			dispYVal = noteC4DispLoc - (tmpDisp*owningStaff.getlOff()) ;
			flags[isOnLdgrLine] = ((int)dispYVal % 10 == 0);		//if not on ldgr line then will have a 5 in ones' place
			staffSize = owningStaff.getlOff() * 4;
		}
		//p.outStr2Scr("Calc DispYVal : Name : " +n.name + " is on ledger line : " + flags[isOnLdgrLine] +  " isChord : "+ this.flags[isChord]+ "  DispYVal :" + String.format("%.4f", dispYVal)+ " noteC4DispLoc :" + String.format("%.4f", noteC4DispLoc)  + " : tmpOctave : "+ tmpOctave + " tmpDisp : " + tmpDisp ); 
	}
	//# of ledger lines to get to each note, from c
	protected int getLedgerLine(){		
		switch (n.name){
			case C : 
			case Cs : {return 0;}
			case D  : 
			case Ds : {return 1;}
			case E  : {return 2;}
			case F  : 
			case Fs : {return 3;}
			case G  : 
			case Gs : {return 4;}
			case A  : 
			case As : {return 5;}
			case B  : {return 6;}
		}
		return 0;
	}
	
	public void initFlags(){for(int i =0; i<numFlags;++i){	flags[i]=false;}}
	//set this note to be a rest
	public void setRest(){
		flags[isRest] = true;
		n.freq = 0;
		n.octave = 0;
	}	
	
	public void setVals(int _stTime, durType _typ, boolean _isDot, boolean _isTup, int _tplVal){setStart(_stTime);setDuration( _typ.getVal(),_isDot, _isTup, _tplVal);}
	public void setVals(int _stTime, int _dur, boolean _isDot, boolean _isTup, int _tplVal){setStart(_stTime);setDuration( _dur,_isDot, _isTup, _tplVal);}	
	//dotted noted means note duration is x 1.5; tuple is x in the space of 2 notes, so duration is 2/x where x is the tuple val (>=3)
	public void setDuration(durType _typ, boolean _isDot, boolean _isTup, int _tplVal){	setDuration( _typ.getVal(),_isDot, _isTup, _tplVal);	}	
	public void setDuration(int _dur, boolean _isDot, boolean _isTup, int _tplVal){
		if(_isDot){_dur = (int)(_dur * 1.5f); flags[isDotted] = true;} 
		else if(_isTup){ if(tupleVal < 3){return;} tupleVal = _tplVal; _dur =(int)(_dur * 2.0/(tupleVal)); flags[isTuple] = true;} 
		n.setDur(_dur);
	}
	
	public void setDurationPRL(durType _typ, int defaultVal, boolean _isDot, boolean _isTup, int _tplVal){	setDuration( _typ.getVal(),_isDot, _isTup, _tplVal);	}
	
	public void setDurationPRL(float _scrDur, int defaultVal,  boolean _isDot, boolean _isTup, int _tplVal){
		if(_isDot){_scrDur = _scrDur * 1.5f; flags[isDotted] = true;} 
		else if(_isTup){ if(tupleVal < 3){return;} tupleVal = _tplVal; _scrDur =_scrDur * 2.0f/(tupleVal); flags[isTuple] = true;} 
		n.setDurScroll(_scrDur,defaultVal);
	}
	
	public void addDurationSphere(float _scrAlpha){addDurationSphereIndiv(_scrAlpha);}
	protected void addDurationSphereIndiv(float _scrAlpha){
		sphereDims[1] += _scrAlpha;		
		sphereDur = sphrOwn.getTickFromAlpha(sphereDims[1] - sphereDims[0]);
	}
	
	public void addDurationPRL(float _scrDur, boolean _isDot, boolean _isTup, int _tplVal){
		if(_isDot){_scrDur = _scrDur * 1.5f; flags[isDotted] = true;} 
		else if(_isTup){ if(tupleVal < 3){return;} tupleVal = _tplVal; _scrDur =_scrDur * 2.0f/(tupleVal); flags[isTuple] = true;} 
		n.addDurScroll(_scrDur);
	}
	
	//override for chord
	public void modStDur(int newDur, int newSt){
		setStart(newSt);								//shift start time to end of new note (notes share start time
		n.setDur(newDur);	
	}
	
	//set time for this note from beginning of sequence
	public void setStart(int _stTime){	n.stTime = _stTime;setDispMsgVals(owningStaff.getC4DistForClefsAtTime(n.stTime));}
	//sets start time via piano roll, where pxls from edge need to be translated to edge time + pxl->click conversion
//	public void setStartPiano(float pxlsFromEdge, int edgeStartTime, float pxlsPerTick){
//		//p.outStr2Scr("setStartPiano : pxlsFromEdge : " + pxlsFromEdge + " edge start time : " + edgeStartTime + " pxlsPerTick : " +pxlsPerTick + " stTime offset : " + (int)(pxlsFromEdge/pxlsPerTick));
//		int _stTime = edgeStartTime + (int)(pxlsFromEdge/pxlsPerTick);		
//		setStart(_stTime);
//	}
	
	//take current duration and set to nearest integral duration TODO
	public void quantize(){n.quantMe();}
	
	//if notes are equal, means their st time, duration, note name and octave are all the same
	public boolean equals(myNote _n){return n.equals(_n.n);	}	
	
	//draw piano roll rectangle
	public void drawMePRL(){p.rect(gridDims);}
	
	public void drawMeSphere(){ drawMeSpherePriv();}
	//void noteArc(myPoint ctr, float alphaSt, float alphaEnd, float rad, float thickness, int[] noteClr){
	protected void drawMeSpherePriv(){//
		p.pushMatrix();p.pushStyle();
		p.noteArc(sphereDims, sphrOwn.noteClr);		
		p.popStyle();p.popMatrix();
	}
	
	//draw this note
	public void drawMe(){		drawMePriv();	}
	protected void drawMePriv(){
		p.pushMatrix();p.pushStyle();
		if(flags[isRest]){
			//translate to middle of measure
			p.translate(0, owningStaff.getlOff() * 2.5f);
			p.drawRest(owningStaff.getlOff(), n.typIdx, flags[isDotted]);			
		} else {	
			p.text(dispMsg,0,0);//8va etc
			//where this note should live on the staff (assume measure has translated to appropriate x position), from C4
			p.translate(0, dispYVal);
			if(n.isSharp){//show sharp sign
				p.translate(.5f*owningStaff.getlOff(),0);
				p.pushMatrix();p.pushStyle();
				p.scale(1,1.5f);
				p.text("#",-1.5f*owningStaff.getlOff(),.5f*owningStaff.getlOff());
				p.popStyle();p.popMatrix();
			}
			p.drawNote(owningStaff.getlOff(), new myVector(0,0,0), n.typIdx,0 ,flags, (dispYVal <0 ? dispYVal : (dispYVal - staffSize > 0) ? dispYVal - staffSize : 0)/10.0f  );//TODO
		}
		p.popStyle();p.popMatrix();
	}

	public String toString(){
		String res = "Note ID : "+ID + n.toString() + " Dot : "+(flags[isDotted]?"Yes":"No");
		if(flags[isChord]){res += "| Chord : Yes";	} else {res += "| Chord : No";}		
		if(flags[isFromSphere]){//sphereDims = new float[]{_alphaSt,_alphaEnd,_ring,sphrOwn.ringRad};
			res += "|Alpha Start : " + String.format("%.5f",sphereDims[0]) + " alpha end : " + String.format("%.5f",sphereDims[1]) + " sphere dur : " + String.format("%.5f",(sphereDims[1]-sphereDims[0]))+ " sphereDur ticks : " + sphereDur+ " Ring : " + sphereRing + " Ring Dist : " +String.format("%.2f", sphereDims[2]);			
		}
		else {
			res += "|C4 : " +String.format("%.4f",noteC4DispLoc) + " dispLoc "+ String.format("%.4f",dispYVal)+ " ";
			if(flags[showDispMsg]){res += "| is displayed at a displacement from where played : Yes | Msg : "+dispMsg;	} else {res += "| is displayed at a displacement from where played : No";}		
		}
		if(flags[isTuple]){res += "| Tuple : Yes("+tupleVal+" in the space of 2)";	} else {res += "| Tuple : No";}		
		if(flags[isRest]){res += "| Rest : Yes";} else {res += "| Rest : No";}		
		return res;
	}	
}//class mynote



//collection of notes related in some way for a single instrument, also holds data for root of chord
class myChord extends myNote{
	public static int cCnt = 0;
	public int CID;		
	public String cname;
	
	public chordType type;
	
	public TreeMap<String, myNote> cnotes;	//keyed by frequency
		
	//note passed is root of chord
	public myChord(CAProject5 _p, nValType _name, int _octave, myMeasure _measure, myStaff _stf) {
		super(_p,_name,_octave,_measure,_stf);
		initChord();
	} 
	//turn a note into a chord
	public myChord(CAProject5 _p, float _alphaSt, float _alphaEnd, int _ring, mySphereCntl _sphrOwn){
		super(_p, _alphaSt, _alphaEnd, _ring, _sphrOwn);
		initChord();
	}
	//turn a note into a chord
	public myChord(myNote _note){
		super(_note);
		initChord();
	}
	
	private void initChord(){
		cname = "";
		CID = cCnt++;		
		cnotes = new TreeMap<String,myNote>();	
		cnotes.put(this.n.nameOct,this);		//idx 0 is always root of chord
		flags[isChord] = true;		
		type = chordType.None;
	}
	
	@Override
	//take current duration and set to nearest integral duration 
	public void quantize(){
		n.quantMe();
		for(Entry<String, myNote> note : cnotes.entrySet()){if(this.ID != note.getValue().ID){note.getValue().quantize();}}
	}
//	@Override
//	//playback - returns all notedata for all notes in this chord
//	public ArrayList<NoteData> play(){
//		ArrayList<NoteData> resVals = new ArrayList<NoteData>();
//		resVals.add(playMe().get(0));			//add this note's notedata
//		for(Entry<String, myNote> note : cnotes.entrySet()){if(this.ID != note.getValue().ID){resVals.addAll(note.getValue().play());}}		//return this chord's notes			
//		return resVals;
//	}

	//force the notes of this chord to be the passed type, adding notes if necessary, removing any extraneous notes.
	//keyBased : if true, set chord to be specific chord in current key, otherwise treat root note as key of chord
	public void setChordType(chordType chrd, boolean keyBased, myKeySig key){
		this.type = chrd;
		if(this.type == chordType.None){return;}	//do not modify "none" chords
		int[] noteDispAra = p.getChordDisp(chrd), indNDisp;
		int numNotes = noteDispAra.length;
		ArrayList<myNote> newCNotes = new ArrayList<myNote>();
		myNote newNote;
		if(keyBased){
			//TODO : force root to be key root?  or find closest chord note matching root?  for now, force note to be key root
			nValType root = key.getRoot();
			n.editNoteVal(root, n.octave);			
		} 
		else {	}//treat root as key of chord - move notes appropriately - root remains unchanged	
		//		if(numNotesDisp > 12){numNotesDisp -= 12; n.editNoteVal(n.name, n.octave+1);}
		//	if(numNotesDisp < -12){numNotesDisp += 12; n.editNoteVal(n.name, n.octave-1);}

		//
		if(flags[isFromSphere]){
			for(int i =1; i<numNotes; ++i){
				newNote = new myNote(p,  this.sphereDims[1], this.sphereDims[1], this.sphereRing, this.sphrOwn);
				if(noteDispAra[i] > 12){noteDispAra[i] -= 12; newNote.n.editNoteVal(newNote.n.name, newNote.n.octave+1);}
				if(noteDispAra[i] < -12){noteDispAra[i] += 12; newNote.n.editNoteVal(newNote.n.name, newNote.n.octave-1);}
				indNDisp = p.getNoteDisp(newNote.n, noteDispAra[i]);
				newNote.n.editNoteVal(nValType.getVal(indNDisp[0]), indNDisp[1]);
			}		
		
		} else {
			for(int i =1; i<numNotes; ++i){
				newNote = new myNote(this);
				if(noteDispAra[i] > 12){noteDispAra[i] -= 12; newNote.n.editNoteVal(newNote.n.name, newNote.n.octave+1);}
				if(noteDispAra[i] < -12){noteDispAra[i] += 12; newNote.n.editNoteVal(newNote.n.name, newNote.n.octave-1);}
				indNDisp = p.getNoteDisp(newNote.n, noteDispAra[i]);
				newNote.n.editNoteVal(nValType.getVal(indNDisp[0]), indNDisp[1]);
			}		
		}
		//this gets rid of existing chord notes and builds new chord
		this.rebuildCNotes(this, newCNotes);
	}//setChordType
	
	@Override
	public void drawMePRL(){
		p.rect(gridDims);
		for(Entry<String, myNote> note : cnotes.entrySet()){if(this.ID != note.getValue().ID){note.getValue().drawMePRL();}}
	}
	
	@Override
	public void drawMeSphere(){
		this.drawMeSpherePriv();
		for(Entry<String, myNote> note : cnotes.entrySet()){if(this.ID != note.getValue().ID){note.getValue().drawMeSpherePriv();}}	
	}

	@Override
	public void drawMe(){
		drawMePriv();
		for(Entry<String, myNote> note : cnotes.entrySet()){if(this.ID != note.getValue().ID){note.getValue().drawMe();}}
	}
	@Override	
	public void moveNoteHalfStep(myKeySig _key, ArrayList<nValType> keyAra, boolean up){		
		ArrayList<myNote> newCNotes = new ArrayList<myNote>();
		moveNoteHalfStepPriv(_key, keyAra, up);
		for(Entry<String, myNote> note : cnotes.entrySet()){
			if(this.ID != note.getValue().ID){
				note.getValue().moveNoteHalfStep(_key, keyAra, up);
				newCNotes.add(note.getValue());				
			}
		}
		//need to rebuild cnotes struct with moved notes, since some may be the same after moving
		rebuildCNotes(this,newCNotes);
//		cnotes = new TreeMap<String,myNote>();	//rebuild array
//		cnotes.put(this.n.nameOct,this);		//idx 0 is always root of chord
//		for(int i =0; i<newCNotes.size();++i){	addNote(newCNotes.get(i));}
	}
	//rebuild cnotes array with new array(that doesn't include root) and root note
	public void rebuildCNotes(myNote root, ArrayList<myNote> newCNotes){
		//need to rebuild cnotes struct with moved notes, since some may be the same after moving
		cnotes = new TreeMap<String,myNote>();	//rebuild array
		cnotes.put(root.n.nameOct,root);		//idx 0 is always root of chord
		for(int i =0; i<newCNotes.size();++i){	addNote(newCNotes.get(i));}		
	}
	@Override
	public void addDurationSphere(float _scrAlpha){
		addDurationSphereIndiv(_scrAlpha);
		for(Entry<String, myNote> note : cnotes.entrySet()){
			if(this.ID != note.getValue().ID){	note.getValue().addDurationSphereIndiv(_scrAlpha);}
		}
	}
	public void setChordName(String _name){cname = _name;}	
	//add a note to this chord
	public void addNote (myNote _nt){cnotes.put(_nt.n.nameOct,_nt);}
	//have all notes start at time of first note in chord (root)
	public void alignStart(){ for(Entry<String, myNote> note : cnotes.entrySet()){if(this.ID != note.getValue().ID){note.getValue().setStart(this.n.stTime);}}}
	//have all notes last as long as root (this note)
	public void alignDur(){for(Entry<String, myNote> note : cnotes.entrySet()){if(this.ID != note.getValue().ID){note.getValue().setDuration(this.n.dur, flags[isDotted],flags[isTuple],tupleVal);}}}	
	
	@Override
	//override for chord
	public void modStDur(int newDur, int newSt){
		setStart(newSt);								//shift start time to end of new note (notes share start time
		n.setDur(newDur);
		for(Entry<String, myNote> note : cnotes.entrySet()){if(this.ID != note.getValue().ID){note.getValue().modStDur(newDur, newSt);}}		
	}
	
	public String toString(){
		String res = "|Chord ID : "+CID + " Chord Name : " + cname + "| # notes : "+ cnotes.size()+"\n";
		res+="\t(Root) #0 : "+super.toString()+"\n";
		int i =1;
		for(Entry<String, myNote> note : cnotes.entrySet()){if(this.ID != note.getValue().ID){myNote tmpNote = note.getValue();res += "\t       #"+i+++" : "+((myNote)(tmpNote)).toString()+"\n";}}
		return res;
	}		
}//class mychord

//convenience class to hold the important info for a measure (time sig, key sig, clef, c4 disp location, etc.
class MeasureData implements Comparable<MeasureData>{//comparison by measure seq #
	public CAProject5 p;
	public int seqNum,
				stTime, 
				endTime;		//start time of measure from beginning of sequence in millis; end time in millis - notes after this need to go in new measure	

	public myMeasure meas;
	public myTimeSig ts;
	public myKeySig ks;
	public myClefBase clef;
	public float c4DispLoc;	
	
	public MeasureData(CAProject5 _p,int _seqNum, int _stTime, myMeasure _meas, myTimeSig _ts, myKeySig _ks, myClefBase _clef, float _c4DispLoc){
		p = _p;
		seqNum = _seqNum;		
		c4DispLoc = _c4DispLoc;			//may be overridden based on notes in this measure
		//key = _key;
		ks = _ks;
		ts = _ts;
		stTime = _stTime;
		clef = _clef;		
	}
	public MeasureData(MeasureData _m){	this( _m.p, _m.seqNum,  _m.stTime,  _m.meas, _m.ts, _m.ks, _m.clef,_m.c4DispLoc);	}//copy ctor
//	
//	public void setMeasure(myMeasure _meas){
//		meas = _meas;
//		meas.processNewMeasureData();
//	}
	
	public void setAllVals(myTimeSig _timeSig,myKeySig _ks,myClefBase _clf,float _c4DispLoc){
		setKeySig(_ks);
		setTimeSig(_timeSig);
		setC4DispLoc(_c4DispLoc);
		setClef(_clf);	
	}
	
	//set all values dependent on key signature for this measure
	public void setKeySig(myKeySig _ks){ks = _ks;}
	public void setTimeSig(myTimeSig _timeSig){		ts = _timeSig;	}
	public void setC4DispLoc(float _c4DispLoc){	c4DispLoc = _c4DispLoc;}
	public void setClef(myClefBase _clf){clef = _clf;}

	public myKeySig getKeySig(){return ks;};	
	public myTimeSig getTimeSig(){return ts;};
	public float getC4DispLoc(){return c4DispLoc;}
	
	@Override
	public int compareTo(MeasureData arg0) {return Integer.compare(seqNum, arg0.seqNum);}//compare via seq number
	
	public String toString(){
		String res = "|seq # :" + seqNum+" |Time Sig : "+ ts.toString() + " |Key Sig : " + ks.toString() + "\n|Clef : "+ clef+ " |St time : " + stTime + " End Time : "+endTime; 
		return res;
	}
	
}

//convenience class to hold the important values for a note
class NoteData implements Comparable<NoteData> {//only compares start time
	public static CAProject5 p;
	public static final float C0 = 16.352f;		//baseline for note frequency
	//note value
	public nValType name;
	public int octave;		//what octave this note lives in - 0 is lowest octave 
	
	public String nameOct;
	//use durType for base duration, add capability of handling dotted/tied notes (?)
	public float freq;			//frequency of this note. c0 is lowest note allowable - 16.352. 
	public float amplitude;		//how loud is the note.  default to 1.0f;
	
	//note duration and placement
	public int dur;			//duration, where 256 is a whole note (4 beats)	
	public durType typ;			//type of note
	
	public int typIdx;			//idx used when displaying - single calculation
	public int stTime;			//when this note starts from beginning of sequence in # of ticks
	
	public boolean isSharp;		//this note is a black key (always a sharp)
		
	public NoteData(CAProject5 _p,nValType _name, int _octave){
		p=_p;
		setNoteVals(_name,_octave, 1.0f);
		setDur(0);
		if(_name == nValType.rest){setRest();}
	}	
	
	public NoteData(NoteData _n){//copy ctor
		p=_n.p;
		setNoteVals(_n.name,_n.octave, _n.amplitude);
		setDur(_n.dur);
		setDurType();
		stTime = _n.stTime;
	}
	
	public NoteData(CAProject5 _p,int d, int s, nValType _name, int _octave){
		this(_p,  _name, _octave);
		setDur(d);
		stTime = s;
		if(_name == nValType.rest){setRest();}
	}
	//use to set all values if this note is a rest
	private void setRest(){
		octave = 0;
		freq = 0;
	}
	
	public void moveHalfStep(boolean up){
		int noteVal = name.getVal(), newVal, mod;
		if(up){
			mod = (noteVal == 11 ? 1 : 0);			//octave goes up if going up at b	
			newVal = (noteVal + 1)%12;
		} else {//down
			mod = (noteVal == 0 ? -1 : 0);			//octave goes down if going down at c
			newVal = ((noteVal + 12) - 1)%12;
		}
		setNoteVals(nValType.getVal(newVal),octave+mod, amplitude);		//mod 12 because we want 0-11 (avoid rest==12)			
	}//
	
	//set volume of note TODO
	public void setAmplitude(float _amp){
		setNoteVals(name,octave,_amp);
	}

	//calculate appropriate start time in beats for minim. derp
	public float getStartPlayback(){
		float res = stTime/(1.0f*durType.Quarter.getVal());
		return res;
	}
	//this returns the duration in terms of fractional beats, where a beat is a quarter note
	public float getDurPlayback(){		
		float res = dur/(1.0f*durType.Quarter.getVal());
		return res;
	}
	//this sets duration from the piano scroll, where the duration is in terms of beats, and needs to be multiplied by qtr note value
	public void setDurScroll(float _scrDur, int defaultVal){
		dur = (int)(_scrDur * defaultVal);
		setDurType();		
	}
	//this adds duration from the piano scroll, where the duration is in terms of beats, and needs to be multiplied by qtr note value
	public void addDurScroll(float _scrDur){
		dur += (int)(_scrDur * durType.Quarter.getVal());
		setDurType();		
	}
	//set duration and durType - all duration-modification calcs need to be called before this
	public void setDur(int _dur){
		dur = _dur;
		setDurType();		
	}
	//durType {Whole(256),Half(128),Quarter(64),Eighth(32),Sixteenth(16),Thirtisecond(8);
	//should be analytic
	private void setDurType(){
		if(dur >= durType.Half.getVal() * 1.75f){typ = durType.Whole; typIdx = -2; return;}
		if(dur >= durType.Quarter.getVal() * 1.75f){typ = durType.Half;typIdx = -1; return;}
		if(dur >= durType.Eighth.getVal() * 1.75f){typ = durType.Quarter; typIdx = 0;return;}
		if(dur >= durType.Sixteenth.getVal() * 1.75f){typ = durType.Eighth;typIdx = 1; return;}
		if(dur >= durType.Thirtisecond.getVal() * 1.75f){typ = durType.Sixteenth; typIdx = 2;return;}		
		typ = durType.Thirtisecond;
		typIdx = 3;
	}
	
	public void quantMe(){
		if(dur >= durType.Half.getVal() * 1.75f){setDur(durType.Whole.getVal()); return;}
		if(dur >= durType.Quarter.getVal() * 1.75f){setDur(durType.Half.getVal());  return;}
		if(dur >= durType.Eighth.getVal() * 1.75f){setDur(durType.Quarter.getVal()); return;}
		if(dur >= durType.Sixteenth.getVal() * 1.75f){setDur(durType.Eighth.getVal());  return;}
		if(dur >= durType.Thirtisecond.getVal() * 1.75f){setDur(durType.Sixteenth.getVal()); return;}		
		setDur(durType.Thirtisecond.getVal()); 
	}
	
	//edit this notedata with new values
	public void editNoteVal(nValType _name, int _octave){
		setNoteVals(_name,_octave, amplitude);
	}
	
	//sets note name, octave and frequency, returns frequency
	private float setNoteVals(nValType _name, int _octave, float amp){
		name = _name;
		octave = _octave;
		nameOct = "" + name+octave;
		isSharp = !p.isNaturalNote(name);
		amplitude = amp;
		float octaveMult = (octave*12 + name.getVal());
		freq = C0 * PApplet.pow(2.0f,(1.0f*octaveMult)/12.0f);
		return freq;
	}
	
//	//for two notes to be the same their name, octave, duration and st time need to be the same
//	public boolean equals(NoteData _ot){return ((_ot.stTime == stTime) && (_ot.dur==dur) && (_ot.nameOct.equals(nameOct)));}
	//for two notes to be the same their name, octave, need to be same
	public boolean equals(NoteData _ot){return (_ot.nameOct.equals(nameOct));}
	
	//sorted list sort on start time for note data NOTE!
	@Override
	public int compareTo(NoteData other) {   return Float.compare(this.stTime, other.stTime);}
	//returns whether this note is lower than the passed note
	public boolean isLowerThan(NoteData _ckNote){	return ((this.octave < _ckNote.octave) || ((this.octave == _ckNote.octave) && (this.name.getVal() < _ckNote.name.getVal())));	}
	
	public String getNameOct(){	return "" + this.name + this.octave;}
	
	public String toString(){
		String res = "|Note: " +nameOct+"|Frq: "+String.format("%.2f",freq) + "|St Time (ticks): " + stTime + "|Dur : "+dur+ " Type : "+ typ+" Type idx : "+ typIdx;
		return res;		
	}

}//NoteData class