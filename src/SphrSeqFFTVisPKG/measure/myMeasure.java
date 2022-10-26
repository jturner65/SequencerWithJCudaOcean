package SphrSeqFFTVisPKG.measure;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import SphrSeqFFTVisPKG.note.myChord;
import SphrSeqFFTVisPKG.note.myNote;
import SphrSeqFFTVisPKG.note.enums.durType;
import SphrSeqFFTVisPKG.note.enums.noteValType;
import SphrSeqFFTVisPKG.staff.myKeySig;
import SphrSeqFFTVisPKG.staff.myStaff;
import SphrSeqFFTVisPKG.staff.myTimeSig;
import SphrSeqFFTVisPKG.ui.myPianoObj;
import SphrSeqFFTVisPKG.ui.base.myMusicSimWindow;
import base_JavaProjTools_IRender.base_Render_Interface.IRenderInterface;
import processing.core.PApplet;

/**
 * class to hold a measure of notes for a single instrument - collected into a staff
 * @author 7strb
 *
 */
public class myMeasure {
	public myMusicSimWindow win;
	public static int mCnt = 0;
	public int ID;		
	
	public MeasureData msrData;			//all measure data for this measure
	
	public myStaff staff;			//owning staff for this measure
	
	public final float minDispLen = 100;		//minimum width of measure
		
	//structure to hold notes in this measure - ordered by location in measure, in ticks
	public TreeMap<Integer,myNote> notes;//, notesGlblLoc;
	
	public float noteStartX,		//x location from beginning of measure bar where first note should start - might be moved if measure has different clef,key,timesig, etc.
				dispWidth;		//display width of measure
	public myMeasure(myMusicSimWindow _win, int _seqNum, int _stTime, myStaff _staff) {
		win=_win;
		ID = mCnt++;
		staff = _staff;
		msrData = new MeasureData(_seqNum,_stTime,this, staff.getTimeSigAtTime(_stTime), staff.getKeySigsAtTime(_stTime), staff.getClefsAtTime(_stTime), staff.getC4DistForClefsAtTime(_stTime));
		notes = new TreeMap<Integer, myNote>();
		//p.outStr2Scr("Base CTOR, about to buildRestAndEndTime() " + ID);
		buildRestAndEndTime();
		//p.outStr2Scr("Base CTOR, after buildRestAndEndTime() " + ID);
	}
	
	public myMeasure(myMeasure _m, int newStTime, int newSeqNum){ //copy ctor
		this(_m.win, newSeqNum, newStTime, _m.staff);		
	}

	public void forceNotesToKey(myKeySig _key, ArrayList<noteValType> keyAra, boolean moveUp, myPianoObj dispPiano){
		msrData.setKeySig(_key);
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
	public void drawNotesPRL(IRenderInterface p){	for(Map.Entry<Integer,myNote> note : notes.entrySet()) {  note.getValue().drawMePRL(p);}	}
	
	//draw all the notes in this measure - return whether a new key,timesig or clef has been specified from previous values : 1 if clef, 2 if timesig, 4 if key sig, bitwise combo of all if multiples otherwise none
	public void drawMe(IRenderInterface p, float xOffset){
		p.pushMatState();		//draw sequence # of measure
			p.setColorValFill(IRenderInterface.gui_Black, 255);
			p.setColorValStroke(IRenderInterface.gui_Black, 255);
			p.setStrokeWt(1);
			p.scale(.75f);
			p.showText(""+msrData.seqNum, 0, -staff.getlOff());
		p.popMatState();		
		p.pushMatState();
			p.translate(xOffset, 0);
			p.pushMatState();		
				//draw all notes in measure - equally space notes based on how many there are and how large measure is
				//notes need to be as wide as how many "Beats" they have, counting the smallest note as the baseline beat
				float xDisp = (1.0f*dispWidth)/(notes.size()+1);
				p.translate(xDisp,0);
				for(Map.Entry<Integer,myNote> note : notes.entrySet()) {
					  note.getValue().drawMe(p);
					  p.translate(xDisp,0);			//TODO need to move notes better than this
				}	
			p.popMatState();
			p.pushMatState();
				p.translate(dispWidth, 0);				//drawn end line
				p.setColorValFill(IRenderInterface.gui_Black, 255);
				p.setColorValStroke(IRenderInterface.gui_Black, 255);
				p.setStrokeWt(2);
				p.drawLine(0,0,0,0,4*staff.getlOff(),0);
			p.popMatState();
		p.popMatState();
	}//drawMe
	
	public void setKeySig(myKeySig _ks){
		msrData.setKeySig(_ks);		
	}
	
	//returns true new end time of this measure
	public int setTimeSig(myTimeSig _timeSig, int newStTime){
//		win.getMsgObj().dispInfoMessage("myMeasure", "setTimeSig", "setTimeSig before oldDur calc");
//		int oldDur = calcEndTime();
		msrData.stTime = newStTime;
		msrData.setTimeSig(_timeSig);
		//if(staff.ID == 0){win.getMsgObj().dispInfoMessage("myMeasure", "setTimeSig", "setTimeSig before newDur calc");}
		int newDur = calcEndTime();
		win.getMsgObj().dispInfoMessage("myMeasure", "setTimeSig", "New Duration after calcEndType call : "+newDur);
		return msrData.endTime; 
	}
	
	//how wide to make the measure display
	private void setDispLen(){	
		dispWidth = PApplet.max(minDispLen, notes.size() * myNote.noteWidth);}
	
	//force all notes in this measure to snap to key
	public void snapToKey(){//TODO
		win.getMsgObj().dispInfoMessage("myMeasure", "snapToKey", "SnapToKey not implemented in myMeasure");
	}
		
	private int calcEndTime(){
		int ticksPerMeasure = (int)(msrData.ts.tSigMult() * 4 * durType.Quarter.getVal());  //tSigMult gives fraction of 4 qtr notes total that make up measure		
		//int measDur = stTime + (int)((1.0f/tempo) * timeSig.tSigMult() * (p.ticksPerBeat)); 		//tempo is beats per minute, 1/ tempo is minutes per beat, p.ticksPerBeat is ticks per beat
		int measDur = ticksPerMeasure; 		//don't have measure control playback tempo, allow this to be controlled by play back engine
		msrData.endTime = msrData.stTime + measDur;
		//if(staff.ID == 0){p.outStr2Scr("Calc End Time meas : "+ m.toString()+ " ticksPerMeasure : "+ ticksPerMeasure );}
		return measDur;
	}
	//prepopulate with rest in note data, calculate end time of this measure
	public void buildRestAndEndTime(){
		//if(staff.ID == 0){p.outStr2Scr("In buildRestAndEndTime");}
		msrData.setAllVals(staff.getTimeSigAtTime(msrData.stTime), staff.getKeySigsAtTime(msrData.stTime), staff.getClefsAtTime(msrData.stTime), staff.getC4DistForClefsAtTime(msrData.stTime));
		int measDur = calcEndTime();		//initialize measure to be full of rest
		notes.clear();
		myNote tmpRest = new myNote(win, noteValType.rest, 0, this, this.staff);		
		tmpRest.setVals(msrData.stTime, measDur, false, false, -1);
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
		if(noteAddTime >= msrData.endTime){								//note starts after this measure ends, add to new measure
			return note;
		}
		myNote newNote = null;										//if note needs to be added after this measure
		int _ntOffset = noteAddTime - msrData.stTime; 					//offset within this measure
		if(msrData.endTime < (note.n.dur + noteAddTime)){					// modify end time of note - no tied-across-measure bounds notes
			int newDur = msrData.endTime - noteAddTime,
				nextDur = note.n.dur - newDur;
			note.setDuration(newDur, false, false,-1); 
			//add rest of note in next measure
			newNote = new myNote(note);
			//TODO completely support tuples and dotted notes
			newNote.setStart(msrData.endTime); 						//new note starts at end of this measure
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
		if(noteAddTime >= msrData.endTime){								//note starts after this measure ends, add to new measure
			return note;
		}
		myNote newNote = null;										//if note needs to be added after this measure
		int _ntOffset = noteAddTime - msrData.stTime; 					//offset within this measure
		if(msrData.endTime < (note.n.dur + noteAddTime)){				//modify end time of note - no tied-across-measure bounds notes
			int newDur = msrData.endTime - noteAddTime,				//dur in this measure
					nextDur = note.n.dur - newDur;				//dur in next measure
				note.setDuration(newDur, false, false,-1); 
				//add rest of note in next measure
				newNote = new myNote(note);
				//TODO completely support tuples and dotted notes
				newNote.setStart(msrData.endTime); 						//new note starts at end of this measure
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
					//for(int i = 1; i<tmpC.cnotes.size(); ++i){note.addNote(tmpC.cnotes.get(i));}
					for(Entry<String, myNote> cNoteEntry : tmpC.cnotes.entrySet()){note.addNote(cNoteEntry.getValue());}
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
		String res = "Meas ID : "+ID+ msrData.toString() + "|Display width : " + String.format("%.2f",dispWidth)+ " #Notes : "+notes.size()+"\n";
		int i =0;
		for(Map.Entry<Integer,myNote> note : notes.entrySet()){
			res += "\t#"+(i++) +" : "+note.getValue().toString()+"\n";
		}		
		return res;
	}	
}//myMeasure class