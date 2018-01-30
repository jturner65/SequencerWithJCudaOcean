package SphrSeqFFTVisPKG;

import java.util.*;
import java.util.Map.Entry;

import ddf.minim.AudioOutput;
import ddf.minim.Minim;
import ddf.minim.ugens.Waves;

public class mySequencer extends myDispWindow {
	//this window will allow input of notes overlayed on a "player piano"-style 2D mesh, with piano displayed on left side
	
	public int gridX, gridY;									//pxls per grid box
	public final float whiteKeyWidth = 78;
	public float bkModY;				//how long, in pixels, is a white key, blk key is 2/3 as long
	//displayed piano
	private myPianoObj dispPiano;	
	//private float[] dispPianoRect;
	
//	/public float tempoDurRatio = 1.0f;
	
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
	public myInstr prlKeyboard;					//piano to play clicks on keys
	
	public durType defaultNoteLength;			//default note length for each square in piano roll
	
	public mySequencer(SeqVisFFTOcean _p, String _n, int _flagIdx, int[] fc, int[] sc, float[] rd, float[] rdClosed,String _winTxt, boolean _canDrawTraj) {
		super(_p, _n, _flagIdx, fc, sc, rd, rdClosed, _winTxt, _canDrawTraj);
		updateGridXandY(false);
		//dispPianoRect = new float[]{0, topOffY, whiteKeyWidth, 52 * gridY};
		dispPiano = new myPianoObj(pa, this,  gridX, gridY, new float[]{0, topOffY, whiteKeyWidth, 52 * gridY}, fillClr, rectDim);		//start with 52 white keys (full keyboard)
		numTrajNoteRpts = 0;		
//		initUIClickCoords(rectDim[0] + .1 * rectDim[2],stY,rectDim[0] + rectDim[2],stY + yOff);
		//setup clickable regions for flag buttons - 1 per boolean flag
		super.initThisWin(_canDrawTraj, false);
	}
	@Override
	//initialize all private-flag based UI buttons here - called by base class
	public void initAllPrivBtns(){
		truePrivFlagNames = new String[]{								//needs to be in order of flags
				"Hide Piano Roll"
		};
		falsePrivFlagNames = new String[]{			//needs to be in order of flags
				"Show Piano Roll"
		};
		privModFlgIdxs = new int[]{prlShowPianoIDX};
		numClickBools = privModFlgIdxs.length;	
		//maybe have call for 		initPrivBtnRects(0);	
		initPrivBtnRects(0,numClickBools);
	}

	@Override
	protected void initMe() {//init/reinit this window
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
		prlKeyboard = new myInstr(pa, "Pianoroll Click Keyboard", pa.clefs[clefVal.Treble.getVal()], pa.hSrsMult, Waves.SINE, false);
		
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
		//pa.glblOut.close();
		prlKeyboard.playSingleNote(clickNote);
		prlKeyboard.stopSingleNote(oldClickNote);			//stop afterward to prevent patching/unpatching
		//pa.glblOut = pa.getAudioOut();
//		pa.glblOut.pauseNotes();
//		playNote(pa.glblOut, 0, 1, clickNote.n.freq);
//		pa.glblOut.resumeNotes();
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
		if(pa.score!=null){
			staffNum = pa.score.staffs.size()-1;
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
	//set current key signature, at time passed - for pa.score, set it at nearest measure boundary
	protected void setLocalKeySigValIndiv(myKeySig lclKeySig, ArrayList<nValType> lclKeyNotesAra, float time){
		if(pa.score != null){
			pa.score.setCurrentKeySig(time, lclKeySig, lclKeyNotesAra);
		}
	}//setCurrentKeySigVal//pbe.updateTimSigTempo(glblTempo, gridX,  glblTimeSig.getTicksPerBeat());
	
	//set time signature at time passed - for pa.score, set it at nearest measure boundary - global time sig need to be set by here
	protected void setLocalTimeSigValIndiv(int tsnum, int tsdenom, durType _beatNoteType, float time){		
		myTimeSig ts = new myTimeSig(pa, tsnum, tsdenom, _beatNoteType);	
		if(pa.score != null){
			pa.score.setCurrentTimeSig(time, ts);
		}	
		//pa.outStr2Scr("setCurrentTimeSigVal : " +glblTimeSigNum+ " over " + glblTimeSigDenom );
	}//setCurrentTimeSigVal	

	@Override
	//set time signature at time passed - for pa.score, set it at nearest measure boundary
	protected void setLocalTempoValIndiv(float tempo, float time){
		
	}//setLocalTempoValIndiv
	
	//set global key signature, at time passed - for pa.score, set it at nearest measure boundary
	protected void setGlobalKeySigValIndiv(int idx, float time){
		//NOTE ! global key sig and key sig array need to be set before here
		//glblKeySig  and glblKeyNotesAra
		if(pa.score != null){
			//pa.pa.score.setCurrentKeySig(time, glblKeySig, glblKeyNotesAra);
			this.forceAllNotesInKey();
		}

	}//setCurrentKeySigVal//pbe.updateTimSigTempo(glblTempo, gridX,  glblTimeSig.getTicksPerBeat());
	
	//set time signature at time passed - for pa.pa.score, set it at nearest measure boundary - global time sig need to be set by here
	protected void setGlobalTimeSigValIndiv(int tsnum, int tsdenom, durType _beatNoteType, float time){		
		//NOTE ! global time sig need to be set by here		
		if(pa.score != null){
			//pa.score.setCurrentTimeSig(time, glblTimeSig);
			forceAllNotesInTimeSig();
		}
		this.defaultNoteLength = _beatNoteType;
		pbe.updateTimSigTempo(glblTempo);
		//pa.outStr2Scr("setCurrentTimeSigVal : " +glblTimeSigNum+ " over " + glblTimeSigDenom );
	}//setCurrentTimeSigVal
	
	//set time signature at time passed - for pa.score, set it at nearest measure boundary
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
				pa.score.staffs.get(scoreStaffNames[0]).debugAllNoteVals();
				break;
			}
			case 1 : {
				pa.score.staffs.get(scoreStaffNames[0]).debugAllMeasVals();
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
			mod = pa.score.hndlMouseClick(mouseX, mouseY); 			
		}
		if(mod){return mod;}
		//other input here, if mod false ( not playing piano) or moding trajectory or clicking on buttons or modding pa.score or...
		
		return checkUIButtons(mouseX, mouseY);
	}//hndlMouseClickIndiv
	@Override
	protected boolean hndlMouseDragIndiv(int mouseX, int mouseY,int pmouseX, int pmouseY, myPoint mouseClickIn3D, myVector mseDragInWorld, int mseBtn) {
		boolean mod = false;
		if(privFlags[prlShowPianoIDX]){
			mod = hndlPianoMseClkDrg(new int[] {mouseX, mouseY, pmouseX, pmouseY},false);//  hndlPianoMseDrag(mouseX, mouseY, pmouseX, pmouseY);
		} else {
			//need to handle displayed staff when piano roll is shown?
			mod = pa.score.hndlMouseDrag(mouseX, mouseY);
		}		
		//other input here, if mod false ( not playing piano) or moding trajectory
		
		return mod;
	}//hndlMouseDragIndiv
	//handle if mouse is released after modifying a UI object in this window (msClkObj is object #)
	@Override
	public void setUIWinVals(int UIidx){
		switch(UIidx){
		case guiTrajToDraw 		: { curTrajAraIDX = (int)guiObjs[UIidx].getVal(); break;}
		case noteDfltLen 		: {	defaultNoteLength = noteValTypes[(int)guiObjs[UIidx].getVal() % noteValTypes.length];updateGridXandY(true);break;}
		case trajRepeats		: { numTrajNoteRpts = (int)guiObjs[UIidx].getVal(); break;}
		}		
	}//setUIWinVals
	//handle the display of UI objects backed by a list
	@Override//pa.score.staffs.size()
	public String getUIListValStr(int UIidx, int validx){
		//pa.outStr2Scr("getUIListValStr : " + UIidx+ " Val : " + validx + " inst len:  "+pa.InstrList.length+ " | "+pa.InstrList[(validx % pa.InstrList.length)].instrName );
		switch(UIidx){//pa.score.staffs.size()
			//case guiTrajToDraw 		: {return pa.InstrList[(validx % pa.score.staffs.size())].instrName; }
			case guiTrajToDraw 		: {return pa.score.staffs.get(pa.score.staffDispOrder.get(validx % pa.score.staffs.size())).instrument.instrName; }
			case noteDfltLen 		: {return noteVals[(validx % noteVals.length)];}
		}
		return "";
	}//dispUIListObj


	@Override
	protected void hndlMouseRelIndiv() {	
		if(privFlags[prlShowPianoIDX]){
			if(playClickNote){prlKeyboard.stopSingleNote(clickNote);	playClickNote = false; clickNote = null; clickNoteLoc = null;}// instrNoteOut.close();}		//playing directly on piano
		} else {
			//pa.score-related mouse release stuff
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
	public void forceNoteToKey(myKeySig _key, ArrayList<nValType> keyAra, myNote note, boolean moveUp){
		//pa.outStr2Scr("force note to key : " + pa.getKeyNames(keyAra));
		if(keyAra.contains(note.n.name)){return;}
		note.moveNoteHalfStep(_key, keyAra, moveUp);//, false, bkModY);		
		float[] resAra = dispPiano.getRectDimsFromRoll(note.n, note.gridDims[0]);
		note.gridDims = resAra;
	}		
	//force all notes in current view to lie in current global key
	
    public void forceAllNotesInKey(){
    	//pa.outStr2Scr("Force notes to key : " + glblKeySig.key);
    	pa.score.forceAllNotesToKey( glblKeySig,  glblKeyNotesAra,  pa.flags[pa.moveKeyNoteUp],  dispPiano);	
	}
    
    public void forceAllNotesInTimeSig(){
    	//pa.outStr2Scr("Force notes to time sig : " + glblTimeSig);
    	pa.score.forceAllNotesToTime( glblTimeSig);	
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
//		newClickNote = pa.score.checkStaffArea((int)pts[i].x-(int)rectDim[0], (int)pts[i].y-(int)rectDim[1]);
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
	public void calcPRLNotesFromTraj(myDrawnNoteTraj drawnNoteTraj){
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
		if(clearStaff){pa.score.clearStaffNotes(scoreStaffNames[trajNum], trajNum);}
		for(Entry<Integer, myNote> note : tmpdrawnPRollNotes.entrySet()) {
			//pa.outStr2Scr("key: "+ note.getKey()+ "|"+note.getValue().toString(),true);
			pa.score.addNoteToStaff(scoreStaffNames[trajNum], note.getValue());			
		}
	}	
	
	public void calcStaffNotesFromTraj(myDrawnNoteTraj drawnNoteTraj){			//TODO
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
	protected void processTrajIndiv(myDrawnNoteTraj drawnNoteTraj){
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
		pa.glblOut.pauseNotes();
		for(int i =0; i<pa.score.staffs.size(); ++i){	
			myStaff staff = pa.score.staffs.get(this.scoreStaffNames[i]);
			if((!staff.stfFlags[myStaff.enabledIDX]) || (!staff.stfFlags[staff.hasNotesIDX])){continue;}
			//tmpNotes = staff.getSubMapOfNotes(lastPBEQueryPlayTime, curPBEPlayTime);
			tmpNotes = staff.getAllNotesAndClear(curPBEPlayTime, 1000000000, false);
			//for(SortedMap.Entry<Integer, myNote> note : tmpNotes.entrySet()) { addTrajNoteToPlay(i,note.getValue());}	
			//addTrajNoteToPlay(i,tmpNotes);
			addTrajNoteToPlay(tmpNotes);
		}
		pa.glblOut.resumeNotes();
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
			staff = pa.score.staffs.get(scoreStaffNames[i]);
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
		pa.pushMatrix();pa.pushStyle();	
		if((playClickNote) && (pa.mousePressed)){pa.show(clickNoteLoc, 4, ""+clickNote.n.nameOct, new myVector(5,5,0), SeqVisFFTOcean.gui_Magenta, dispFlags[trajPointsAreFlat]);}//clicking on piano
		//draw vertical grid partition lines - done in piano
		//draw squares corresponding to trajectory
		for(int i =0; i<pa.score.staffs.size(); ++i){//			pa.setColorValFill((i==curDrnTrajStaffIDX ? pa.gui_Red : );
			//pa.setColorValFill((i==curTrajAraIDX ? pa.gui_Red : drawTrajBoxFillClrs[i]));
			pa.setFill((i==curTrajAraIDX ? pa.getClr(pa.gui_Red) : drawTrajBoxFillClrs[i]));
			pa.score.staffs.get(scoreStaffNames[i]).drawMeasPRL();
		}
		pa.pushMatrix();pa.pushStyle();
		//pa.translate(xOff*2,dispPianoRect[1]+dispPianoRect[3]+(.3f*xOff));
		pa.translate(xOff*2,dispPiano.pianoDim[1]+dispPiano.pianoDim[3] +(.3f*xOff));
		pa.score.staffs.get(scoreStaffNames[curTrajAraIDX]).drawStaff();
		pa.popStyle();pa.popMatrix();
		//draw Staff corresponding to current trajectory
		pa.translate(whiteKeyWidth, 0);
		pbe.drawMe();
		pa.popStyle();pa.popMatrix();
	}
	
	private void drawScoreStuff(){
		pa.score.drawScore();		
		pa.pushMatrix();pa.pushStyle();
		pa.translate(whiteKeyWidth, 0);
		pbe.drawMe();			//TODO need to modify position at the start of every measure
		pa.popStyle();pa.popMatrix();		
	}	
	
	@Override
	protected void drawMe(float animTimeMod) {
		pa.pushMatrix();pa.pushStyle();
		pa.translate(rectDim[0], rectDim[1]);//move to the edges of this window
		
		if(privFlags[prlShowPianoIDX]){drawPRLStuff();}
		else {						drawScoreStuff();}		
		//drawGUIObjs();
		//drawClickableBooleans();
		pa.popStyle();pa.popMatrix();
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
	@Override //code called when pa.score is made or changed
	public void setScoreInstrValsIndiv(){
		//initNoteOutIndiv();
	}

	@Override
	public String toString(){
		String res = super.toString();
		return res;
	}

}//mySequencer class

class myPianoObj{
	public static SeqVisFFTOcean pa;
	public static mySequencer win;
	//dimensions of piano keys, for display and mouse-checking
	public float[][] pianoWKeyDims, pianoBKeyDims;	
	//array of note data for each piano key - played if directly clicked on
	public NoteData[] pianoWNotes, pianoBNotes;
	//background color of window
	public int[] winFillClr;
	
	public final float wkOff_X = .72f;
	
	//location and dimension of piano keyboard in parent display window, location and size of display window
	public float[] pianoDim, winDim;	
	
	public float keyX, keyY;										//x, y resolution of grid/keys, mod amount for black key
	public int numWhiteKeys;										//# of white keys on piano - should be 52, maybe resize if smaller?
	public static  final int numKeys = 88;
	public int numNotesWide;										//# of notes to show as grid squares
	public myPianoObj(SeqVisFFTOcean _p, mySequencer _win, float kx, float ky, float[] _pianoDim, int[] _winFillClr, float[] _winDim){
		pa = _p;
		win = _win;
		pianoDim = new float[_pianoDim.length];
		winFillClr = new int[_winFillClr.length]; for(int i=0;i<_winFillClr.length;++i){winFillClr[i]=_winFillClr[i];}
		winDim = new float[_winDim.length];
		updateDims(kx, ky, _pianoDim, _winDim);	
	}
	//if the window or piano dimensions change, update them here
	public void updateDims(float kx, float ky, float[] _pianoDim, float[] _winDim){
		keyX = kx; keyY = ky; updatePianoDim(_pianoDim);updateWinDim(_winDim);
		numWhiteKeys = 52;//PApplet.min(52,(int)(_winDim[3]/keyY));		//height of containing window will constrain keys in future maybe.
		numNotesWide = (int)((winDim[2] - pianoDim[2])/keyX);
		buildKeyDims();
	}
	//build key dimensions array for checking and displaying
	private void buildKeyDims(){
		pianoWKeyDims = new float[numWhiteKeys][];		//88 x 5, last idx is 0 if white, 1 if black
		pianoWNotes = new NoteData[numWhiteKeys];
		int numBlackKeys = numKeys - numWhiteKeys;
		pianoBKeyDims = new float[numBlackKeys][];		//88 x 5, last idx is 0 if white, 1 if black
		pianoBNotes = new NoteData[numBlackKeys];
		float wHigh = keyY, bHigh = 2.0f * win.bkModY, wWide = win.whiteKeyWidth, bWide = .6f*win.whiteKeyWidth;	
		int blkKeyCnt = 0, octave = 8;
		float stY = pianoDim[1];
		for(int i =0; i < numWhiteKeys; ++i){
			pianoWKeyDims[i] = new float[]{0,stY,wWide,wHigh};	
			int iMod = i % 7;
			pianoWNotes[i] = new NoteData(pa,pa.wKeyVals[iMod], octave);
			if(pa.wKeyVals[iMod] == nValType.C){
				octave--;
			}
			if((iMod != 4) && (iMod != 0) && (i != numWhiteKeys-1)&& (i != 0)){
				pianoBKeyDims[blkKeyCnt] = new float[]{0,stY+(keyY-win.bkModY),bWide,bHigh};
				pianoBNotes[blkKeyCnt] = new NoteData(pa,pa.bKeyVals[blkKeyCnt%5], octave);
				blkKeyCnt++;
			}
			stY +=keyY;
		}	
	}//buildKeyDims
	
	//return note clicked on if clicked on piano directly
	public myNote checkClick(int mouseX, int mouseY, myPoint snapClickLoc){
		myNote res = null;
		if(!pa.ptInRange(mouseX, mouseY, pianoDim[0], pianoDim[1], pianoDim[0]+pianoDim[2], pianoDim[1]+pianoDim[3])){return res;}//not in this window)
		int resIdx = -1, keyType = -1;
		double xLoc = 0, yLoc = 0;
		boolean found = false;
		//black keys
		for(int i =0; i<pianoBKeyDims.length;++i){
			if(pa.ptInRange(mouseX, mouseY, pianoBKeyDims[i][0],pianoBKeyDims[i][1],pianoBKeyDims[i][0]+pianoBKeyDims[i][2],pianoBKeyDims[i][1] + pianoBKeyDims[i][3])){
				resIdx = i;	keyType = 1;found = true; xLoc =pianoBKeyDims[i][0]+(.5f*pianoBKeyDims[i][2]); yLoc  =pianoBKeyDims[i][1]+(.5f*pianoBKeyDims[i][3]); 	break; 
			}
		}
		if(!found){//prevent double-taps with black keys
			for(int i =0; i<pianoWKeyDims.length;++i){
				if(pa.ptInRange(mouseX, mouseY, pianoWKeyDims[i][0],pianoWKeyDims[i][1],pianoWKeyDims[i][0]+pianoWKeyDims[i][2],pianoWKeyDims[i][1]+pianoWKeyDims[i][3])){
					resIdx = i;keyType = 0;found = true;xLoc =pianoWKeyDims[i][0]+(wkOff_X*pianoWKeyDims[i][2]); yLoc =pianoWKeyDims[i][1]+(.5f*pianoWKeyDims[i][3]); 	break;
				}
			}
		}
		if(resIdx != -1){
			//measure-less note to be played immediately
			NoteData tmpa = ( keyType == 0 ? pianoWNotes[resIdx] : pianoBNotes[resIdx]);
			res = new myNote(pa, tmpa.name, tmpa.octave, null, win.pa.score.staffs.get(win.scoreStaffNames[win.curTrajAraIDX]));
			snapClickLoc.set(xLoc,yLoc,0);			
		}
		//pa.outStr2Scr("Key x : " + keyClickX+ " Key y : "+keyClickY + " idx : "+resIdx+" Key Type : "+ keyType, true);
		return res;	
	}//checkClick		
	
	//given a y coordinate in mouse space (piano roll area), return the note this is, or null
	//called directly by piano roll, so no range checking necessary
	public myNote checkRollArea(int x, int y, float[] nrDims){
		myNote res = null;
		int resIdx = -1, keyType = -1;
		boolean found = false, isNatural = false, isBlkKey = false;
		for(int i =0; i<pianoBKeyDims.length;++i){
			if(pa.ptInRange(x, y, win.whiteKeyWidth,pianoBKeyDims[i][1],winDim[2],pianoBKeyDims[i][1] + pianoBKeyDims[i][3])){
				resIdx = i;	keyType = 1;found = true;  	nrDims[1] = pianoBKeyDims[i][1]; nrDims[2] = keyX;nrDims[3] = pianoBKeyDims[i][3];
				isBlkKey = true;
				break; 
			}
		}
		if(!found){//prevent double-taps with black keys
			for(int i =0; i<pianoWKeyDims.length;++i){
				if(pa.ptInRange(x, y, win.whiteKeyWidth,pianoWKeyDims[i][1],winDim[2],pianoWKeyDims[i][1]+pianoWKeyDims[i][3])){
					resIdx = i;keyType = 0;found = true;nrDims[1] = pianoWKeyDims[i][1]; nrDims[2] = keyX;nrDims[3] = pianoWKeyDims[i][3];
					isNatural=(i != 0);	//treat all keys but c7 as naturals - this is to decrease size of drawn box
					break;
				}
			}
		}
		if(resIdx != -1){
			nrDims[0] =(((int)((x-win.whiteKeyWidth)/keyX)) * keyX)+win.whiteKeyWidth;
			//pa.outStr2Scr("checkRollArea NRDIMS 0 : " + nrDims[0] + " orig x : " + x + " | " +  keyX);
			NoteData tmpa = ( keyType == 0 ? pianoWNotes[resIdx] : pianoBNotes[resIdx]);
			res = new myNote(pa, tmpa.name, tmpa.octave, null, win.pa.score.staffs.get(win.scoreStaffNames[win.curTrajAraIDX]));
			//pa.outStr2Scr("Note name in checkRollArea : " + res.n.name, true );
			if(isNatural){//modify note grid dim so box doesn't overlap black keys
				if (pa.chkHasSharps(res.n.name)){nrDims[1] += win.bkModY; nrDims[3] -= win.bkModY;}//increase y0, decrease y1 coord to make room for black key
				if (pa.chkHasFlats(res.n.name) && (resIdx != pianoWKeyDims.length-1)){nrDims[3] -= win.bkModY;}//decrease y1 coord to make room for black key				
			} 
//			else if(isBlkKey){
//				if (pa.chkHasSharps(res.n.name)){nrDims[1] -= bkModY; nrDims[3] += bkModY;}//increase y0, decrease y1 coord to make room for black key
//				if (pa.chkHasFlats(res.n.name)){nrDims[3] += bkModY;}//decrease y1 coord to make room for black key								
//			}
			//pa.outStr2Scr("Note : " + res.toString() );
		} else {
		//	pa.outStr2Scr("Note is null ");
		}
		return res;	
	}//checkRollArea
	
	//get piano roll rectangle dimensions given a specific note data value
	public float[] getRectDimsFromRoll(NoteData nd, float xStOffset){
		//nrDims[0] =(((int)(x/keyX)) * keyX);
		float[] res = new float[4];
		res[0]= xStOffset;
		int resIdx = 0;;
		if(pa.isNaturalNote(nd.name)){//check white keys
			for(int i =0; i<pianoWNotes.length;++i){
				if(nd.nameOct.equals(pianoWNotes[i].nameOct)){
					res[1] = pianoWKeyDims[i][1]; res[2] = keyX;res[3] = pianoWKeyDims[i][3]; resIdx = i;break;
				}
			}
			if (pa.chkHasSharps(nd.name)){res[1] += win.bkModY; res[3] -= win.bkModY;}//increase y0, decrease y1 coord to make room for black key
			if (pa.chkHasFlats(nd.name) && (resIdx != pianoWKeyDims.length-1)){res[3] -= win.bkModY;}//decrease y1 coord to make room for black key				

		} else {				//check black keys
			for(int i =0; i<pianoBNotes.length;++i){
				if(nd.nameOct.equals(pianoBNotes[i].nameOct)){
					res[1] = pianoBKeyDims[i][1]; res[2] = keyX;res[3] = pianoBKeyDims[i][3];resIdx = i;break;					
				}
			}	
		}
		return res;
	}//getRectDimsFromRoll
		
	public void drawMe(){
		pa.pushMatrix();pa.pushStyle();
		pa.setColorValFill(SeqVisFFTOcean.gui_Red);	pa.setColorValStroke(SeqVisFFTOcean.gui_Black);
		pa.strokeWeight(1.0f);
		pa.rect(pianoDim);		//piano box
		//white keys		
		float[] lineYdim = new float[2];
		for(int i =0; i<pianoWKeyDims.length;++i){
			pa.pushMatrix();pa.pushStyle();
			pa.setColorValFill(SeqVisFFTOcean.gui_OffWhite);	pa.setColorValStroke(SeqVisFFTOcean.gui_Black);
			pa.strokeWeight(.5f);
			pa.rect(pianoWKeyDims[i]);
			lineYdim[0] = pianoWKeyDims[i][1]; lineYdim[1] = pianoWKeyDims[i][3];
			if(i!= 0){
				if (pa.chkHasSharps(pianoWNotes[i].name)){lineYdim[0] += win.bkModY; lineYdim[1] -= win.bkModY;}//increase y0, decrease y1 coord to make room for black key
				if (pa.chkHasFlats(pianoWNotes[i].name) && (i != pianoWKeyDims.length-1)){lineYdim[1] -= win.bkModY;}//decrease y1 coord to make room for black key				
			}
			pa.rect(win.whiteKeyWidth,lineYdim[0],winDim[2],lineYdim[1]);	

			pa.setColorValFill(SeqVisFFTOcean.gui_Gray);			
			pa.text(""+pianoWNotes[i].nameOct, (wkOff_X+.05f)*win.whiteKeyWidth, pianoWKeyDims[i][1]+.85f*keyY);			
			pa.popStyle();pa.popMatrix();		
		}
		//black keys
		for(int i =0; i<pianoBKeyDims.length;++i){
			pa.pushMatrix();pa.pushStyle();
			pa.setColorValFill(SeqVisFFTOcean.gui_Black);	pa.setColorValStroke(SeqVisFFTOcean.gui_Black);
			pa.rect(pianoBKeyDims[i]);
			pa.setColorValFill(SeqVisFFTOcean.gui_LightGray,512);
			pa.noStroke();
			pa.rect(win.whiteKeyWidth,pianoBKeyDims[i][1]+.1f*keyY,winDim[2],pianoBKeyDims[i][3]-.2f*keyY);			
			pa.popStyle();pa.popMatrix();		
		}
		//vertical bars
		pa.setColorValStroke(SeqVisFFTOcean.gui_Black);
		pa.strokeWeight(1.0f);
		float startX = pianoDim[2] + pianoDim[0];
		for(int i=0;i<numNotesWide;++i){
			pa.line(startX,pianoDim[1], startX,pianoDim[1]+ pianoDim[3]);		//piano box, p2);
			startX += keyX;
		}
		//pa.outStr2Scr("NumKeysDrawn : "+ keyCnt , true);
		pa.popStyle();pa.popMatrix();		
	}
	
	private void updateWinDim(float[] _winDim){	for(int i =0; i<_winDim.length; ++i){	winDim[i] = _winDim[i];}}
	private void updatePianoDim(float[] _pianoDim){	for(int i =0; i<_pianoDim.length; ++i){	pianoDim[i] = _pianoDim[i];}}	
	
}//myPianoObj class


class myScore {
	public static SeqVisFFTOcean pa;
	public myDispWindow w;
	public static int sngCnt = 0;
	public int ID;
	public String songName;	

	public float[] scoreDim;			//part of containing window holding the pa.score
	
	//instrument-specific staffs - string is instrument name in all structs here
	public TreeMap<String,myStaff>  staffs;
	public ArrayList<String> staffDispOrder;		//order by which staff should be displayed

	private TreeMap<String,myInstr> instruments;	
	private TreeMap<String, Boolean> staffSelList;	//list of staff select boxes
	private myStaff currentStaff;
	
	public boolean[] scrFlags;
	public static final int showSelStfIDX = 0;		//whether to show only selected staff
	
	public static final int numScrFlags = 1;

	//distance between staffs
	public static final float stOff = 90, boxStX = 0, boxStY = 10;
	
	public myScore(SeqVisFFTOcean _p, myDispWindow _w,String _name, float[] _scoreDim, ArrayList<String> staffName, ArrayList<myInstr> _inst) {
		pa=_p;
		w=_w;
		ID = sngCnt++;
		songName = _name;
		scoreDim = new float[_scoreDim.length];
		for(int i =0;i<scoreDim.length;++i){scoreDim[i]=_scoreDim[i];}
		instruments = new TreeMap<String,myInstr>();
		staffs = new TreeMap<String,myStaff>();
		staffDispOrder = new ArrayList<String>();
		staffSelList = new TreeMap<String, Boolean>();
		for(int i =0; i< _inst.size(); ++i){
			addStaff(staffName.get(i), _inst.get(i));
		}	
		initScrFlags();
	}	

	public myScore(SeqVisFFTOcean _p,myDispWindow _w, String _name, float[] _scoreDim) {
		this(_p,_w,_name,_scoreDim,new ArrayList<String>(),new ArrayList<myInstr>());
	}		
	public void initScrFlags(){		scrFlags = new boolean[numScrFlags];for(int i=0;i<numScrFlags;++i){scrFlags[i]=false;}	}
	
	//set current staff for adding notes
	public void setCurrentStaff(String instName){
		currentStaff = staffs.get(instName);
	}
	//ptInRange(double x, double y, double minX, double minY, double maxX, double maxY){return ((x > minX)&&(x < maxX)&&(y > minY)&&(y < maxY));}
	//find where mouse is clicked - add notes manually, set staff selected/deselected
	public boolean hndlMouseClick(int mouseX, int mouseY){
		boolean mod = false;
		//pa.outStr2Scr("click made it to pa.score code");
		float xVal = (float)(boxStX + scoreDim[0]),
			  yVal;
		for(int i = 0; i < staffDispOrder.size(); ++i){
			yVal = (float)(boxStY + scoreDim[1]) + (i*stOff);
			if(pa.ptInRange(mouseX, mouseY, xVal, yVal, xVal+10, yVal+10)){
				String stfName = staffDispOrder.get(i);
				//need to make the current staff the staff that is clicked
				staffSelList.put(stfName, !staffSelList.get(stfName));
				return true;
			}			
		}					
		//handle other mouse click events here
		return mod;
	}//handleMouseClick
	
	public boolean hndlMouseDrag(int mouseX, int mouseY){
		boolean mod = false;
		//handle mouse interaction in this pa.score - if interaction successful, return true;
		return mod;
	}//handleMouseDrag
	
	public TreeMap<String,myInstr> getInstrumentList(){return instruments;}
	
	//add a staff to this song
	public void addStaff(String name, myInstr _inst){
		//pa.outStr2Scr("adding staff name : " + name + " for inst : " + _inst.ID);
		instruments.put(name,_inst);
		myStaff tmp = new myStaff(pa,this, _inst, name);
		staffSelList.remove(name);
		staffSelList.put(name, true);
		staffs.put(name, tmp);
		if(!staffDispOrder.contains(name)){staffDispOrder.add(name);	}
	}
	//clear out staff of all notes
	public void clearStaffNotes(String staffName, int idx){
		if(idx == 0){pa.outStr2Scr(" clear all staff notes : ");}
		myStaff oldStaff = staffs.get(staffName);
		SortedMap<Integer,myNote> dmmyAllNotes = oldStaff.getAllNotesAndClear(-1,100000000, true);
	}//clearStaffNotes

	//overrides all key settings set in measures :  forceNotesSetKey(myKeySig _key, ArrayList<nValType> glblKeyNotesAra, boolean moveUp, myPianoObj dispPiano){			
	public void forceAllNotesToKey(myKeySig _key, ArrayList<nValType> glblKeyNotesAra, boolean moveUp, myPianoObj dispPiano){		
		for(int i =0; i<staffDispOrder.size(); ++i){	
			staffs.get(staffDispOrder.get(i)).forceNotesSetKey( _key,  glblKeyNotesAra,  moveUp,  dispPiano);				
		}
	}
	
	//overrides all time sig settings set in measures :  forceNotesSetKey(myKeySig _key, ArrayList<nValType> glblKeyNotesAra, boolean moveUp, myPianoObj dispPiano){			
	public void forceAllNotesToTime(myTimeSig ts){		
		for(int i =0; i<staffDispOrder.size(); ++i){	
			staffs.get(staffDispOrder.get(i)).forceNotesSetTimeSig(ts);				
		}
	}
	
	//send out key sig info to every staff setKeySigAtTime(float stTime, myKeySig newKey){
	public void setCurrentKeySig(float timeToSet, myKeySig ks, ArrayList<nValType> glblKSNoteVals){
		for(int i =0; i<staffDispOrder.size(); ++i){	
			staffs.get(staffDispOrder.get(i)).setKeySigAtTime(timeToSet, ks);				
		}		
	}
	
	//send out time sig info to every staff setKeySigAtTime(float stTime, myKeySig newKey){
	public void setCurrentTimeSig(float timeToSet, myTimeSig ts){
		for(int i =0; i<staffDispOrder.size(); ++i){	
			staffs.get(staffDispOrder.get(i)).setTimeSigAtTime(timeToSet, ts);				
		}		
	}
	
	//find note clicked on in staff - either select existing note or add new one
	public myNote checkStaffArea(int x, int y){
		myNote res = null;
		//TODO check staff location using y value (pxl) with staff offset
//		if(resIdx != -1){
//			nrDims[0] =(((int)(x/keyX)) * keyX);
//			NoteData tmp = ( keyType == 0 ? pianoWNotes[resIdx] : pianoBNotes[resIdx]);
//			res = new myNote(p, tmpa.name, tmpa.octave, null);
//			//pa.outStr2Scr("Note name in checkRollArea : " + res.n.name, true );
//			if(isNatural){//modify note grid dim so box doesn't overlap black keys
//				if (pa.chkHasSharps(res.n.name)){nrDims[1] += bkModY; nrDims[3] -= bkModY;}//increase y0, decrease y1 coord to make room for black key
//				if (pa.chkHasFlats(res.n.name) && (resIdx != pianoWKeyDims.length-1)){nrDims[3] -= bkModY;}//decrease y1 coord to make room for black key				
//			}
//			//pa.outStr2Scr("Note : " + res.toString() );
//		} else {
//			pa.outStr2Scr("Note is null ");
//		}
		return res;	
	}//checkStaffArea
	
	//build note as being entered, then add it
	public void addNoteToStaff(String staffName, myNote _n ){staffs.get(staffName).addNoteAtNoteTime(_n);}

	//display pa.score in window
	public void drawScore(){
		pa.pushMatrix();pa.pushStyle();
		pa.translate(scoreDim[0],scoreDim[1]);		
		for(int i =0; i<staffDispOrder.size(); ++i){
			String stfName = staffDispOrder.get(i);
			//drawStfSelBox(staffSelList.get(stfName));
			staffs.get(stfName).drawStaff();
			pa.translate(0, stOff);
		}
//		}
		pa.popStyle();pa.popMatrix();		
	}
	
//	//play all note data - should this be on measure by measure basis?
//	public void playScore(){
//		ArrayList<NoteData> allNoteData = new ArrayList<NoteData>();
//		for(int i =0; i<staffDispOrder.size(); ++i){
//			String stfName = staffDispOrder.get(i);
//			if(staffSelList.get(stfName)){	allNoteData.addAll(staffs.get(stfName).play());}
//		}	
//	}
	
	public String toString(){
		String res = "Song name : "+songName+" Instruments : ";
		for(int i =0; i<staffDispOrder.size(); ++i){
			String stfName = staffDispOrder.get(i);
			res += staffs.get(stfName).toString();
		}
		return res;
	}
	

}//myScore class
