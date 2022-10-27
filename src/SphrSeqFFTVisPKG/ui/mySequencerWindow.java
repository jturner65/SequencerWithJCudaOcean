package SphrSeqFFTVisPKG.ui;

import java.util.*;
import java.util.Map.Entry;

import SphrSeqFFTVisPKG.SeqVisFFTOcean;
import SphrSeqFFTVisPKG.clef.enums.clefType;
import SphrSeqFFTVisPKG.instrument.myInstrument;
import SphrSeqFFTVisPKG.measure.myMeasure;
import SphrSeqFFTVisPKG.note.myNote;
import SphrSeqFFTVisPKG.note.enums.durType;
import SphrSeqFFTVisPKG.note.enums.noteValType;
import SphrSeqFFTVisPKG.staff.myKeySig;
import SphrSeqFFTVisPKG.staff.myStaff;
import SphrSeqFFTVisPKG.staff.myTimeSig;
import SphrSeqFFTVisPKG.ui.base.myMusicSimWindow;
import base_Math_Objects.vectorObjs.doubles.myPoint;
import base_Math_Objects.vectorObjs.doubles.myVector;
import base_UI_Objects.windowUI.drawnObjs.myDrawnSmplTraj;
import base_UI_Objects.windowUI.drawnObjs.myVariStroke;
import ddf.minim.ugens.Waves;

public class mySequencerWindow extends myMusicSimWindow {
	//this window will allow input of notes overlayed on a "player piano"-style 2D mesh, with piano displayed on left side
	
	public int gridX, gridY;									//pxls per grid box
	public final float whiteKeyWidth = 78;
	public float bkModY;				//how long, in pixels, is a white key, blk key is 2/3 as long
	//displayed piano
	private myPianoObj dispPiano;
	
	public int numTrajNoteRpts;
	
	//clicking on piano
	private myNote clickNote;
	private myPoint clickNoteLoc;
	
	//fill and stroke color for notes to show in piano grid
	public int[] drnGrdNtFill, drnGrdNtStrk;
	
	//public boolean[] privFlags;								//mySequencer specific flags
	public static final int 
		prlShowPianoIDX = 0;								//show/hide the piano/piano-roll - needs to be a button
	public static final int numPrivFlags = 1;	
//	//GUI Objects	
	//idx's of objects in gui objs array	- only things needing to be instanced in child window
	public final static int 
		guiTrajToDraw = 0,						//which staff/instrument to draw a trajectory for
		noteDfltLen = 1,						//default note length
		trajRepeats = 2;							//how many repeats a trajectory should have
	public final int numGUIObjs = 3;												//# of gui objects for ui
	
	//audio constructs
	public boolean playClickNote;				//whether to play a note from clicking on piano keyboard
	//public AudioOutput clickNoteOut;
	public myInstrument prlKeyboard;					//piano to play clicks on keys
	
	public durType defaultNoteLength;			//default note length for each square in piano roll
	
	public mySequencerWindow(SeqVisFFTOcean _p, String _n, int _flagIdx, int[] fc, int[] sc, float[] rd, float[] rdClosed,String _winTxt, boolean _canDrawTraj) {
		super(_p, _n, _flagIdx, fc, sc, rd, rdClosed, _winTxt, _canDrawTraj);
		updateGridXandY(false);
		//dispPianoRect = new float[]{0, topOffY, whiteKeyWidth, 52 * gridY};
		dispPiano = new myPianoObj(pa, this,  gridX, gridY, new float[]{0, topOffY, whiteKeyWidth, 52 * gridY}, fillClr, rectDim);		//start with 52 white keys (full keyboard)
		numTrajNoteRpts = 0;		
//		initUIClickCoords(rectDim[0] + .1 * rectDim[2],stY,rectDim[0] + rectDim[2],stY + yOff);
		//setup clickable regions for flag buttons - 1 per boolean flag
		super.initThisWin(_canDrawTraj, false);
	}


	/**
	 * Build button descriptive arrays : each object array holds true label, false label, and idx of button in owning child class
	 * this must return count of -all- booleans managed by privFlags, not just those that are interactive buttons (some may be 
	 * hidden to manage booleans that manage or record state)
	 * @param tmpBtnNamesArray ArrayList of Object arrays to be built containing all button definitions. 
	 * @return count of -all- booleans to be managed by privFlags
	 */
	@Override
	public final int initAllPrivBtns_Indiv(ArrayList<Object[]> tmpBtnNamesArray) {
		
		
		return tmpBtnNamesArray.size();
	}

	@Override
	protected void initMe_Indiv() {//init/reinit this window
	//	dispFlags[uiObjsAreVert] = true;
		dispFlags[drawsPBERet] = true;
		dispFlags[plays] = true;						//this window responds to travelling reticle
		dispFlags[hasScrollBars] = true;				//to view measures off the screen and staffs off the screen
		initPrivFlags(numPrivFlags);
		vsblStLoc = new float[]{whiteKeyWidth,0};
		seqVisStTime = new int[] {0,0};
		//initNoteOutIndiv();

		//curTrajAraIDX = 0;
		defaultNoteLength = durType.Quarter;		
		
		playClickNote = false;
		//clickNoteOut = pa.getAudioOut();
		prlKeyboard = new myInstrument(pa, "Pianoroll Click Keyboard", pa.clefs[clefType.Treble.getVal()], pa.hSrsMult, Waves.SINE, false);
		
		//procInitOut(clickNoteOut);
		setPrivFlags(prlShowPianoIDX,true);				//initially show piano roll
	}//initMe
	
	//init any extra ui objs
	@Override
	protected void initXtraUIObjsIndiv() {}

    //called by keyboard clicks to play notes
	private void playKbdClickNote(myNote _clkNote){
		if(null == _clkNote){return;}
		myNote oldClickNote = clickNote;
		clickNote = _clkNote;
		playClickNote = true;
		//glblOut.close();
		prlKeyboard.playSingleNote(clickNote);
		prlKeyboard.stopSingleNote(oldClickNote);			//stop afterward to prevent patching/unpatching
		//glblOut = pa.getAudioOut();
//		glblOut.pauseNotes();
//		playNote(glblOut, 0, 1, clickNote.n.freq);
//		glblOut.resumeNotes();
	}
	@Override
	//set flag values and execute special functionality for this sequencer
	public void setPrivFlags(int idx, boolean val){
		privFlags[idx] = val;
		switch(idx){
			case prlShowPianoIDX : {
				curDrnTrajScrIDX = (val ? prlTrajIDX : scrTrajIDX);
				break;}
		}			
	}//setprivFlags
	
	//calculate grid cube width
	public static int calcGridWidth(float winWidth){return (int)(winWidth*gridXMult);}
	public static int calcGridHeight(float winHeight){return (int)(winHeight*gridYMult);}
	
	//initialize structure to hold modifiable UI regions and UI components
	@Override
	protected void setupGUIObjsAras(){	//noteVals.length - all UI objects need to be built with respect of putting them in the sidebar menu, dimensions-wise
		//pa.outStr2Scr("setupGUIObjsAras in :"+ name);
		//add components to every list to correspond to each UI list-box object
		int staffNum = 0;
		if(score!=null){
			staffNum = score.staffs.size()-1;
		} 
		if(staffNum < 0){staffNum = 0;}
		guiMinMaxModVals = new double [][]{//min max mod values for each modifiable UI comp	
			{0,staffNum,.05},
			{0,noteVals.length-1,.05},
			{0,20,.5}
		};							
		guiStVals = new double[]{0,2,0};								//starting value
		guiObjNames = new String[]{"Current Draw Trajectory", "Default Drawn Note Length", "# of Repeats of Drawn Traj Notes"};							//name/label of component		
		//idx 0 is treat as int, idx 1 is obj has list vals, idx 2 is object gets sent to windows
		guiBoolVals = new boolean [][]{
			{true, true, true},
			{true, true, true},
			{true, false, true}
		};						//per-object  list of boolean flags
		//since horizontal row of UI comps, uiClkCoords[2] will be set in buildGUIObjs		
		guiObjs = new myGUIObj[numGUIObjs];			//list of modifiable gui objects
		if(numGUIObjs > 0){
			buildGUIObjs(guiObjNames,guiStVals,guiMinMaxModVals,guiBoolVals,new double[]{xOff,yOff});			//builds a horizontal list of UI comps
		}
	}//setupMenuClkRegions
	
	public void updateGridXandY(boolean setTempo){
		gridX = (int)(calcGridWidth(rectDim[2]) * (setTempo ? (defaultNoteLength.getVal()/(durType.Quarter.getVal()*1.0f)) : 1));//default width is quarter note
		gridY = calcGridHeight(rectDim[3]);
		bkModY = .3f * gridY;
		//pa.outStr2Scr("wkwidth : " + whiteKeyWidth+ " set Tempo : " + setTempo);
		if(setTempo){
			dispPiano.updateDims(gridX, gridY, new float[]{0, topOffY, whiteKeyWidth, 52 * gridY}, rectDim);
			setGlobalTempoVal(glblTempo);
		}
	}
	//set current key signature, at time passed - for score, set it at nearest measure boundary
	protected void setLocalKeySigValIndiv(myKeySig lclKeySig, ArrayList<noteValType> lclKeyNotesAra, float time){
		if(score != null){
			score.setCurrentKeySig(time, lclKeySig, lclKeyNotesAra);
		}
	}//setCurrentKeySigVal//pbe.updateTimSigTempo(glblTempo, gridX,  glblTimeSig.getTicksPerBeat());
	
	//set time signature at time passed - for score, set it at nearest measure boundary - global time sig need to be set by here
	protected void setLocalTimeSigValIndiv(int tsnum, int tsdenom, durType _beatNoteType, float time){		
		myTimeSig ts = new myTimeSig(pa, tsnum, tsdenom, _beatNoteType);	
		if(score != null){
			score.setCurrentTimeSig(time, ts);
		}	
		//pa.outStr2Scr("setCurrentTimeSigVal : " +glblTimeSigNum+ " over " + glblTimeSigDenom );
	}//setCurrentTimeSigVal	

	@Override
	//set time signature at time passed - for score, set it at nearest measure boundary
	protected void setLocalTempoValIndiv(float tempo, float time){
		
	}//setLocalTempoValIndiv
	
	//set global key signature, at time passed - for score, set it at nearest measure boundary
	protected void setGlobalKeySigValIndiv(int idx, float time){
		//NOTE ! global key sig and key sig array need to be set before here
		//glblKeySig  and glblKeyNotesAra
		if(score != null){
			//pa.score.setCurrentKeySig(time, glblKeySig, glblKeyNotesAra);
			this.forceAllNotesInKey();
		}

	}//setCurrentKeySigVal//pbe.updateTimSigTempo(glblTempo, gridX,  glblTimeSig.getTicksPerBeat());
	
	//set time signature at time passed - for pa.score, set it at nearest measure boundary - global time sig need to be set by here
	protected void setGlobalTimeSigValIndiv(int tsnum, int tsdenom, durType _beatNoteType, float time){		
		//NOTE ! global time sig need to be set by here		
		if(score != null){
			//score.setCurrentTimeSig(time, glblTimeSig);
			forceAllNotesInTimeSig();
		}
		this.defaultNoteLength = _beatNoteType;
		pbe.updateTimSigTempo(glblTempo);
		//pa.outStr2Scr("setCurrentTimeSigVal : " +glblTimeSigNum+ " over " + glblTimeSigDenom );
	}//setCurrentTimeSigVal
	
	//set time signature at time passed - for score, set it at nearest measure boundary
	protected void setGlobalTempoValIndiv(float tempo, float time){
		//tempoDurRatio = baseTempo/glblTempo;

		
		pbe.updateTimSigTempo(glblTempo);
	}//setCurrentTimeSigVal	
	
	//debug is pressed
	@Override
	public void clickDebug(int btnNum){
		pa.outStr2Scr("click debug in "+name+" : btn : " + btnNum);
		switch(btnNum){
			case 0 : {
				score.staffs.get(scoreStaffNames[0]).debugAllNoteVals();
				break;
			}
			case 1 : {
				score.staffs.get(scoreStaffNames[0]).debugAllMeasVals();
				break;
			}
			case 2 : {
				break;
			}
			case 3 : {
				break;
			}
			default : {break;}
		}		
	}
	@Override
	protected boolean hndlMouseMoveIndiv(int mouseX, int mouseY, myPoint mseClckInWorld){
		return false;
	}
	
	@Override
	protected boolean hndlMouseClickIndiv(int mouseX, int mouseY, myPoint mseClckInWorld, int mseBtn) {
		boolean mod = false;
		if(privFlags[prlShowPianoIDX]){
			mod = hndlPianoMseClkDrg(new int[] {mouseX, mouseY},true);// hndlPianoMseClick(mouseX, mouseY);
		} else {
			//need to handle displayed staff when piano roll is shown?
			mod = score.hndlMouseClick(mouseX, mouseY); 			
		}
		if(mod){return mod;}
		//other input here, if mod false ( not playing piano) or moding trajectory or clicking on buttons or modding score or...
		
		return checkUIButtons(mouseX, mouseY);
	}//hndlMouseClickIndiv
	@Override
	protected boolean hndlMouseDragIndiv(int mouseX, int mouseY,int pmouseX, int pmouseY, myPoint mouseClickIn3D, myVector mseDragInWorld, int mseBtn) {
		boolean mod = false;
		if(privFlags[prlShowPianoIDX]){
			mod = hndlPianoMseClkDrg(new int[] {mouseX, mouseY, pmouseX, pmouseY},false);//  hndlPianoMseDrag(mouseX, mouseY, pmouseX, pmouseY);
		} else {
			//need to handle displayed staff when piano roll is shown?
			mod = score.hndlMouseDrag(mouseX, mouseY);
		}		
		//other input here, if mod false ( not playing piano) or moding trajectory
		
		return mod;
	}//hndlMouseDragIndiv
	//handle if mouse is released after modifying a UI object in this window (msClkObj is object #)
	@Override
	public void setUIWinVals(int UIidx){
		switch(UIidx){
		case guiTrajToDraw 		: { curTrajAraIDX = (int)guiObjs[UIidx].getVal(); break;}
		case noteDfltLen 		: {	defaultNoteLength = durType.getVal((int)guiObjs[UIidx].getVal());updateGridXandY(true);break;}
		case trajRepeats		: { numTrajNoteRpts = (int)guiObjs[UIidx].getVal(); break;}
		}		
	}//setUIWinVals
	//handle the display of UI objects backed by a list
	@Override//score.staffs.size()
	public String getUIListValStr(int UIidx, int validx){
		//pa.outStr2Scr("getUIListValStr : " + UIidx+ " Val : " + validx + " inst len:  "+pa.InstrList.length+ " | "+pa.InstrList[(validx % pa.InstrList.length)].instrName );
		switch(UIidx){//score.staffs.size()
			//case guiTrajToDraw 		: {return pa.InstrList[(validx % score.staffs.size())].instrName; }
			case guiTrajToDraw 		: {return score.staffs.get(score.staffDispOrder.get(validx % score.staffs.size())).instrument.instrName; }
			case noteDfltLen 		: {return durType.noteDurNames[validx];}
		}
		return "";
	}//dispUIListObj


	@Override
	protected void hndlMouseRelIndiv() {	
		if(privFlags[prlShowPianoIDX]){
			if(playClickNote){prlKeyboard.stopSingleNote(clickNote);	playClickNote = false; clickNote = null; clickNoteLoc = null;}// instrNoteOut.close();}		//playing directly on piano
		} else {
			//score-related mouse release stuff
		}		
	}//hndlMseRelOnUIObj
	@Override
	protected myPoint getMouseLoc3D(int mouseX, int mouseY){return pa.P(mouseX,mouseY,0);}
	
	@Override
	protected void snapMouseLocs(int oldMouseX, int oldMouseY, int[] newMouseLoc){
		//TODO
	}
	
	private boolean checkNote(myNote newClickNote, boolean isClick){
		if(isClick){return (null != newClickNote);}
		else {return ((null != newClickNote) && (null != clickNote) && (!(newClickNote.n.nameOct.equals(clickNote.n.nameOct)) ));}		
	}
	//"drawing" notes on piano keyboard
	private boolean hndlPianoMseClkDrg(int[] mse, boolean isClick){
		boolean mod = false;
		myPoint tmpLoc = pa.P();
		myNote newClickNote = dispPiano.checkClick(mse[0]-(int)rectDim[0], mse[1]-(int)rectDim[1], tmpLoc);
		if(checkNote(newClickNote,isClick)){
			clickNoteLoc  = tmpLoc;			
			playKbdClickNote(newClickNote);
			mod = true;
		}
		return mod;
	}
	
	//force the passed note to be in the key passed, using the direction given 
	public void forceNoteToKey(myKeySig _key, ArrayList<noteValType> keyAra, myNote note, boolean moveUp){
		//pa.outStr2Scr("force note to key : " + pa.getKeyNames(keyAra));
		if(keyAra.contains(note.n.name)){return;}
		note.moveNoteHalfStep(_key, keyAra, moveUp);//, false, bkModY);		
		float[] resAra = dispPiano.getRectDimsFromRoll(note.n, note.gridDims[0]);
		note.gridDims = resAra;
	}		
	//force all notes in current view to lie in current global key
	
    public void forceAllNotesInKey(){
    	//pa.outStr2Scr("Force notes to key : " + glblKeySig.key);
    	score.forceAllNotesToKey( glblKeySig,  glblKeyNotesAra,  pa.flags[pa.moveKeyNoteUp],  dispPiano);	
	}
    
    public void forceAllNotesInTimeSig(){
    	//pa.outStr2Scr("Force notes to time sig : " + glblTimeSig);
    	score.forceAllNotesToTime( glblTimeSig);	
    }
    
    //get a note from a location on the piano roll
    public myNote getNoteFromPrlLoc(myPoint pt, int ticksPerDfltBeat, int stGridOff){
		float[] noteRectDims = new float[4];
    	myNote newClickNote;
		newClickNote = dispPiano.checkRollArea((int)pt.x-(int)rectDim[0], (int)pt.y-(int)rectDim[1], noteRectDims);
		if(newClickNote == null){ pa.outStr2Scr("getNoteFromPrlLoc: loc of null pt = " + pt.toStrBrf()); return null;}
		newClickNote.setDurationPRL(1, ticksPerDfltBeat, false, false, 0);
		newClickNote.gridDims = noteRectDims;	//includes displacement for piano key display on piano roll	
		newClickNote.gridDims[0] += stGridOff;	//offset from copy-pasting 
		//newClickNote.gridDims[2] *= ticksPerDfltBeat/(1.0f*durType.Quarter.getVal());	//offset from copy-pasting 
		//pa.outStr2Scr("Check Conversions : orig note rect st x val : " +noteRectDims[0]+" derived rect st x val "+ cnvrtStTimeToGridX(cnvrtGridXToStTime(noteRectDims[0])) );
		//newClickNote.setStart(cnvrtGridXToStTime(noteRectDims[0]) + stTimeOff); 
		newClickNote.setStart(cnvrtGridXToStTime(noteRectDims[0])); 
		//newClickNote.setStart((int)noteRectDims[0]); 
		if(pa.flags[pa.forceInKey]){this.forceNoteToKey(glblKeySig, glblKeyNotesAra, newClickNote, pa.flags[pa.moveKeyNoteUp]);}
    	return newClickNote;
    } 
    
    public myNote getNoteFromStaffLoc(myPoint pt, int ticksPerDfltBeat){		//TODO
		float[] noteRectDims = new float[4];
    	myNote newClickNote = null;
//		newClickNote = score.checkStaffArea((int)pts[i].x-(int)rectDim[0], (int)pts[i].y-(int)rectDim[1]);
//		if(newClickNote == null){ pa.outStr2Scr("getNoteFromPrlLoc: loc of null pt = " + pt.toStrBrf()); return null;}
//		newClickNote.setDurationPRL(1, ticksPerDfltBeat, false, false, 0);
//		newClickNote.gridDims = noteRectDims;	//includes displacement for piano key display on piano roll	
//		//pa.outStr2Scr("Check Conversions : orig note rect st x val : " +noteRectDims[0]+" derived rect st x val "+ cnvrtStTimeToGridX(cnvrtGridXToStTime(noteRectDims[0])) );
//		newClickNote.setStart(cnvrtGridXToStTime(noteRectDims[0])); 
//		//newClickNote.setStart((int)noteRectDims[0]); 
//		if(pa.flags[pa.forceInKey]){this.forceNoteToKey(glblKeyNotesAra, newClickNote, pa.flags[pa.moveKeyNoteUp]);}
    	return newClickNote;
    } 
    
	//get notes to display on piano roll from trajectory and put in staff
	public void calcPRLNotesFromTraj(myDrawnSmplTraj drawnNoteTraj){
		myPoint[] pts = ((myVariStroke)drawnNoteTraj.drawnTraj).getDrawnPtAra(false);
		TreeMap<Integer,myNote> tmpdrawnPRollNotes = new TreeMap<Integer,myNote>();										//new trajectory of notes to play
		int ticksPerDfltBeat =  defaultNoteLength.getVal();
		myNote newClickNote,lastNewNote = null;
		boolean onlyNulls = false, checkedFirstNote = false;
		int stGridOff = 0, lastGridTime = 0, firstGridTime = 0;
		
		for(int repts = 0; repts <= this.numTrajNoteRpts; ++repts){
			for(int i=0; i< pts.length;++i){
				newClickNote = getNoteFromPrlLoc(pts[i],ticksPerDfltBeat, stGridOff);
				if(!checkedFirstNote && (i==0)){firstGridTime = (int) (newClickNote.gridDims[0]); checkedFirstNote=true;}
				lastGridTime = (int) (newClickNote.gridDims[0]+ newClickNote.gridDims[2]);
				if(pa.flags[pa.joinNotesPianoRoll]){
					if(i==0){					//first note of trajectory
						myNote tmp = tmpdrawnPRollNotes.put((int)(newClickNote.gridDims[0] + stGridOff),newClickNote);					
						lastNewNote = newClickNote;
						if(tmp!= null){}//replace, or build chord like in measure code									
					} else {
						if((newClickNote.n.name.getVal() == lastNewNote.n.name.getVal()) && (newClickNote.gridDims[0]+ newClickNote.gridDims[2]!= lastNewNote.gridDims[0]+ lastNewNote.gridDims[2])){
							lastNewNote.addDurationPRL(newClickNote.n.getDurPlayback(), false, false, 0);	
							lastNewNote.addDurGridDims(newClickNote.gridDims);
						} else if((newClickNote.n.name.getVal() != lastNewNote.n.name.getVal())) {
							if(newClickNote.gridDims[0]+ newClickNote.gridDims[2] == lastNewNote.gridDims[0]+ lastNewNote.gridDims[2]){
								lastNewNote.addDurationPRL(-newClickNote.n.getDurPlayback(), false, false, 0);	//remove last note's contribution, for overlap
								lastNewNote.addDurGridDims(newClickNote.gridDims, -1);
							}
							lastNewNote = newClickNote;
							//note has no notion of time by here
							myNote tmp = tmpdrawnPRollNotes.put((int)(lastNewNote.gridDims[0] + stGridOff),lastNewNote);					
							if(tmp!= null){}//replace, or build chord like in measure code	
						}
					}			
				} else {
					myNote tmp = tmpdrawnPRollNotes.put((int)(newClickNote.gridDims[0] + stGridOff),newClickNote);					
					if(tmp!= null){
						//replace, or build chord like in measure code					
					}				
				}
			}//for each point
			stGridOff = lastGridTime + (int) ( - firstGridTime) ;
			//pa.outStr2Scr(" stGridOff offset for rept " + repts + " : " + stGridOff + " first grid time : " + firstGridTime);
		}//for rpts
		if(!onlyNulls){
			drawnNoteTraj.drawnNotesMap = tmpdrawnPRollNotes;
			addDrawnNotesToScore(curTrajAraIDX,pa.flags[pa.clearStaffNewTraj],drawnNoteTraj.drawnNotesMap);
		}		
	}
	
	public void addDrawnNotesToScore(int trajNum, boolean clearStaff, TreeMap<Integer,myNote> tmpdrawnPRollNotes){
		if(clearStaff){score.clearStaffNotes(scoreStaffNames[trajNum], trajNum);}
		for(Entry<Integer, myNote> note : tmpdrawnPRollNotes.entrySet()) {
			//pa.outStr2Scr("key: "+ note.getKey()+ "|"+note.getValue().toString(),true);
			score.addNoteToStaff(scoreStaffNames[trajNum], note.getValue());			
		}
	}	
	
	public void calcStaffNotesFromTraj(myDrawnSmplTraj drawnNoteTraj){			//TODO
		myPoint[] pts = ((myVariStroke)drawnNoteTraj.drawnTraj).getDrawnPtAra(false);
		TreeMap<Integer,myNote> tmpdrawnStaffNotes = new TreeMap<Integer,myNote>();										//new trajectory of notes to play		
		int ticksPerDfltBeat =  defaultNoteLength.getVal();
		myNote newClickNote,lastNewNote = null;
		boolean onlyNulls = false;
		for(int i=0; i< pts.length;++i){
			newClickNote = getNoteFromStaffLoc(pts[i],ticksPerDfltBeat);
			if(pa.flags[pa.joinNotesPianoRoll]){
				if(i==0){
					myNote tmp = tmpdrawnStaffNotes.put((int)(newClickNote.gridDims[0]),newClickNote);					
					lastNewNote = newClickNote;
					if(tmp!= null){
						//replace, or build chord like in measure code					
					}		
				} else {
					if((newClickNote.n.name.getVal() == lastNewNote.n.name.getVal()) && (newClickNote.gridDims[0]+ newClickNote.gridDims[2]!= lastNewNote.gridDims[0]+ lastNewNote.gridDims[2])){
						lastNewNote.addDurationPRL(newClickNote.n.getDurPlayback(), false, false, 0);	
						lastNewNote.addDurGridDims(newClickNote.gridDims);
					} else if((newClickNote.n.name.getVal() != lastNewNote.n.name.getVal())) {
						if(newClickNote.gridDims[0]+ newClickNote.gridDims[2] == lastNewNote.gridDims[0]+ lastNewNote.gridDims[2]){
							lastNewNote.addDurationPRL(-newClickNote.n.getDurPlayback(), false, false, 0);	//remove last note's contribution, for overlap
							lastNewNote.addDurGridDims(newClickNote.gridDims, -1);
						}
						//pa.outStr2Scr("-------------------");
						lastNewNote = newClickNote;
						//note has no notion of time by here
						myNote tmp = tmpdrawnStaffNotes.put((int)(lastNewNote.gridDims[0]),lastNewNote);					
						if(tmp!= null){
							//replace, or build chord like in measure code						
						}
					}
				}			
			} else {
				myNote tmp = tmpdrawnStaffNotes.put((int)(newClickNote.gridDims[0]),newClickNote);					
				if(tmp!= null){
					//replace, or build chord like in measure code					
				}				
			}
			if(!onlyNulls){
				drawnNoteTraj.drawnNotesMap = tmpdrawnStaffNotes;
				addDrawnNotesToScore(curTrajAraIDX,pa.flags[pa.clearStaffNewTraj],drawnNoteTraj.drawnNotesMap);
			}
		}//for
	}	
	
	//TODO this is bad : 
	//convert a passed gridX value to a start time value 
	public int cnvrtGridXToStTime(float nrd0){
		int numGridCellsFromEdge = (int)((nrd0 - vsblStLoc[curDrnTrajScrIDX])/gridX);
		//pa.outStr2Scr("Convert grid to st time : # gridCells : " +numGridCellsFromEdge + " st loc : " +vsblStLoc[curDrnTrajScrIDX] + " defaultNoteLen : " + defaultNoteLength.getVal() + " : grid x " + gridX);
		return seqVisStTime[prlTrajIDX] + (int)((nrd0-this.whiteKeyWidth)/ (gridX/(1.0f*defaultNoteLength.getVal())));				
	}
	//convert a note's start time to a grid x value
	public float cnvrtStTimeToGridX(int stTime){
		float denom = (gridX/(1.0f*defaultNoteLength.getVal()));
		//stTime = seqVisStTime[prlTrajIDX] + (int)((nrd0-this.whiteKeyWidth)/ (gridX/(1.0f*defaultNoteLength.getVal())));
		//+ (int)((nrd0-this.whiteKeyWidth)/ (gridX/(1.0f*tperBeat)));
		float res = (stTime - seqVisStTime[prlTrajIDX] + (this.whiteKeyWidth/denom)) * denom;
		return res;
	}
	
	public void tieAllNotes(){
			//TODO set when boolean pa.flags[pa.joinNotesPianoRoll] true
	}	

	@Override  //
	protected void processTrajIndiv(myDrawnSmplTraj drawnNoteTraj){
		if(privFlags[prlShowPianoIDX]){
			calcPRLNotesFromTraj(drawnNoteTraj);
		} else {
			//calcStaffNotesFromTraj(drawnNoteTraj);	
		}		
	}	

	//when music stops
	@Override
	protected void stopMe() {
	
	}
	@Override
	//only call 1 time
	protected void playMe() {
		float curPBEPlayTime = pbe.getCurrentTime() + vsblStLoc[curDrnTrajScrIDX] - 1;  //getCurrentPlayTime - this is start of notes - get all notes from here on
		//need to calculate offset of reticle and use to determine accurate play time		
		//replace this with individual instrument note playing in myStaff			
		SortedMap<Integer, myNote> tmpNotes;
		glblOut.pauseNotes();
		for(int i =0; i<score.staffs.size(); ++i){	
			myStaff staff = score.staffs.get(scoreStaffNames[i]);
			if((!staff.stfFlags[myStaff.enabledIDX]) || (!staff.stfFlags[staff.hasNotesIDX])){continue;}
			//tmpNotes = staff.getSubMapOfNotes(lastPBEQueryPlayTime, curPBEPlayTime);
			tmpNotes = staff.getAllNotesAndClear(curPBEPlayTime, 1000000000, false);
			//for(SortedMap.Entry<Integer, myNote> note : tmpNotes.entrySet()) { addTrajNoteToPlay(i,note.getValue());}	
			//addTrajNoteToPlay(i,tmpNotes);
			addTrajNoteToPlay(tmpNotes);
		}
		glblOut.resumeNotes();
	}//playMe
	//move current play position when playing mp3/sample (i.e. something not controlled by pbe reticle
	@Override
	protected void modMySongLoc(float modAmt) {
		
	};

		
	@Override
	protected void resizeMe(float scaleY) {//also use topOffY to modify grid blocs being displayed	
		updateGridXandY(true);										//update grid dimensions for new scaling
		myStaff staff;
		myMeasure meas;
		myNote note;
		//need to move and resize currently shown staff
		
		//need to move every note
		for(int i =0; i<scoreStaffNames.length; ++i){
			staff = score.staffs.get(scoreStaffNames[i]);
			for(Entry<Integer, myMeasure> measrEntry : staff.measures.entrySet()){
				measrEntry.getValue().moveNoteForResize(scaleY, topOffY);
			}
		}		
		//re-size other components here
	}//resizeMe
	
	@Override
	//gets current notes for simulation visualization
	protected SortedMap<Integer, myNote> getNotesNow() {
		 SortedMap<Integer, myNote> res = new TreeMap<Integer, myNote> ();
		 
		 
		 //TODO
		 return res;
	}//getNotesNow

	
	@Override
	protected void closeMe() {}	
	@Override
	protected void showMe() {}
	private void drawPRLStuff(){
		dispPiano.drawMe();
		pa.pushMatState();	
		if((playClickNote) && (pa.mousePressed)){pa.show(clickNoteLoc, 4, ""+clickNote.n.nameOct, new myVector(5,5,0), SeqVisFFTOcean.gui_Magenta, dispFlags[trajPointsAreFlat]);}//clicking on piano
		//draw vertical grid partition lines - done in piano
		//draw squares corresponding to trajectory
		for(int i =0; i<score.staffs.size(); ++i){//			pa.setColorValFill((i==curDrnTrajStaffIDX ? pa.gui_Red : );
			//pa.setColorValFill((i==curTrajAraIDX ? pa.gui_Red : drawTrajBoxFillClrs[i]));
			pa.setFill((i==curTrajAraIDX ? pa.getClr(pa.gui_Red, 255) : drawTrajBoxFillClrs[i]));
			score.staffs.get(scoreStaffNames[i]).drawMeasPRL();
		}
		pa.pushMatState();
		//pa.translate(xOff*2,dispPianoRect[1]+dispPianoRect[3]+(.3f*xOff));
		pa.translate(xOff*2,dispPiano.pianoDim[1]+dispPiano.pianoDim[3] +(.3f*xOff));
		score.staffs.get(scoreStaffNames[curTrajAraIDX]).drawStaff();
		pa.popMatState();
		//draw Staff corresponding to current trajectory
		pa.translate(whiteKeyWidth, 0);
		pbe.drawMe();
		pa.popMatState();
	}
	
	private void drawScoreStuff(){
		score.drawScore();		
		pa.pushMatState();
		pa.translate(whiteKeyWidth, 0);
		pbe.drawMe();			//TODO need to modify position at the start of every measure
		pa.popMatState();		
	}	
	
	@Override
	protected void drawMe(float animTimeMod) {
		pa.pushMatState();
		pa.translate(rectDim[0], rectDim[1]);//move to the edges of this window
		
		if(privFlags[prlShowPianoIDX]){drawPRLStuff();}
		else {						drawScoreStuff();}		
		//drawGUIObjs();
		//drawClickableBooleans();
		pa.popMatState();
	}	
	@Override
	protected void endShiftKeyI() {}
	@Override
	protected void endAltKeyI() {}
	@Override
	protected void endCntlKeyI() {}
	@Override
	protected void addSScrToWinIndiv(int newWinKey){}
	@Override
	protected void addTrajToScrIndiv(int subScrKey, String newTrajKey){}
	@Override
	protected void delSScrToWinIndiv(int idx) {}	
	@Override
	protected void delTrajToScrIndiv(int subScrKey, String newTrajKey) {}
	@Override //code called when score is made or changed
	public void setScoreInstrValsIndiv(){
		//initNoteOutIndiv();
	}

	@Override
	public String toString(){
		String res = super.toString();
		return res;
	}

}//mySequencer class
