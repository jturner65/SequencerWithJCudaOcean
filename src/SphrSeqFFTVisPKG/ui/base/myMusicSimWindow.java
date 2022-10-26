package SphrSeqFFTVisPKG.ui.base;

import java.util.*;
import java.util.Map.Entry;

import SphrSeqFFTVisPKG.clef.base.myClefBase;
import SphrSeqFFTVisPKG.clef.enums.clefVal;
import SphrSeqFFTVisPKG.clef.enums.keySigVals;
import SphrSeqFFTVisPKG.instrument.myInstrument;
import SphrSeqFFTVisPKG.note.NoteData;
import SphrSeqFFTVisPKG.note.myChord;
import SphrSeqFFTVisPKG.note.myNote;
import SphrSeqFFTVisPKG.note.enums.chordType;
import SphrSeqFFTVisPKG.note.enums.durType;
import SphrSeqFFTVisPKG.note.enums.nValType;
import SphrSeqFFTVisPKG.staff.myKeySig;
import SphrSeqFFTVisPKG.staff.myTimeSig;
import SphrSeqFFTVisPKG.ui.controls.myPlaybackEngine;
import base_JavaProjTools_IRender.base_Render_Interface.IRenderInterface;
import base_Math_Objects.MyMathUtils;
import base_Math_Objects.vectorObjs.doubles.myPoint;
import base_Math_Objects.vectorObjs.doubles.myVector;
import base_UI_Objects.GUI_AppManager;
import base_UI_Objects.windowUI.base.myDispWindow;
import ddf.minim.AudioOutput;
import processing.core.PConstants;

/**
 * abstract class to hold base code for a menu/display window (2D for gui, etc), to handle 
 * displaying and controlling the window, and calling the implementing class for the specifics
 * @author 7strb
 *
 */
public abstract class myMusicSimWindow extends myDispWindow {
	
	public myPlaybackEngine pbe;
	
	//global sequence values - static
	public static myKeySig glblKeySig;
	public static float glblTempo;
	public static myTimeSig glblTimeSig;
	public static durType glblBeatNote;
	public static ArrayList<nValType> glblKeyNotesAra;					//notes in the current key -- used to force notes to key

	
	public int pFlagIdx;					//the flags idx in the PApplet that controls this window - use -1 for none	
	public boolean[] dispFlags;
	
	public static final int 
			
				procMouseMove 		= 11,
				mouseSnapMove		= 12,			//mouse locations for this window are discrete multiples - if so implement inherited function to calculate mouse snap location
				
				fltrByKeySig 		= 13,
				drawsPBERet 		= 15,
				notesLoaded 		= 16,			//notes are all loaded into out for all staffs - wait to move pbe until this has happened.
				uiObjMod			= 17;			//a ui object in this window has been modified

	public static final int numDispFlags = 18;

	//UI objects in this window
	//GUI Objects

	public float[] vsblStLoc;							//pxl location of first visisble note on the left of the screen - idx0 for piano
	public int[] seqVisStTime;							//seq time of first note visible in sequence window
			

		public final String[] keySigs = new String[]{"CMaj - 0 #'s","GMaj - 1 #","DMaj - 2 #'s","Amaj - 3 #'s","EMaj - 4 #'s","BMaj - 5 #'s","FsMaj - 6 #'s",
			"CsMaj - 5 b's","GsMaj - 4 b's","DsMaj - 3 b's","AsMaj - 2 b's","Fmaj - 1 b"};
	
	public final int[] timeSigDenom = new int[]{1,2,4,8,16,32};
	
	public final String[] noteVals = new String[]{"Whole","Half","Quarter","Eighth","Sixteenth","Thirtisecond"};
	public final durType[] noteValTypes = new durType[]{durType.Whole,durType.Half,durType.Quarter,durType.Eighth,durType.Sixteenth,durType.Thirtisecond};
	
	//score - related values
	//protected myScore score;									//score being displayed
	protected float[] scoreRect;								//dimensions of score area
	protected String[] scoreStaffNames;					//array of each score staff name
	protected TreeMap<String,myInstrument> instrs;	
	
	//individual instrument note output - replaces scrollNoteOut
	//public AudioOutput[] instrNoteOut;
	public float tempoDurRatio = 1.0f;
	
	//display note width multiplier
	public final float ntWdthMult = 2.5f;

	//list of clefs  myClef(CAProject5 _p, String _name, clefVal _clef, NoteData _mdNote,PImage _img)
	//Notedata : nValType _name, int _octave
	public NoteData C4;
	public myClefBase[] clefs;
	//list of instruments
	public myInstrument[] InstrList;
	////idx's of instruments available
	public static final int 
	Guit1InstIDX 		= 0,
	Guit2InstIDX 		= 1,
	BassInstIDX 		= 2,
	Vox1InstIDX 		= 3,
	Vox2InstIDX 		= 4,
	Synth1InstIDX 		= 5,
	Synth2InstIDX 		= 6,
	Synth3InstIDX 		= 7,
	Synth4InstIDX 		= 8,
	Synth5InstIDX 		= 9,
	drumsInstIDX		= 10,
	drums2InstIDX		= 11;
	public static final int numInstsAvail = 12;	

	
	//pxl displacements to draw rests
	public float[][] restDisp = new float[][] {
		new float[]{0,-20},
		new float[]{0,-12},
		new float[]{0,-12},
		new float[]{0,-22},
	};
	
	
	public myMusicSimWindow(IRenderInterface _p, GUI_AppManager _AppMgr, String _n, int _flagIdx, int[] fc,  int[] sc, float[] rd, float[] rdClosed, String _winTxt) {
		super(_p,_AppMgr,_n, _flagIdx, fc, sc,rd, rdClosed,_winTxt);

		pbe = new myPlaybackEngine(pa, this, new int[]{255,0,255,255}, new int[]{0,255,255,255}, new float[]{0,pa.getHeight()});
		vsblStLoc = new float[]{0,0};
		seqVisStTime = new int[] {0,0};
	}	
	
	@Override
	protected final void initMe() {
		C4 = new NoteData(nValType.C, 4);
		
		initMe_Indiv();
	}//initMe()
	
	protected abstract void initMe_Indiv();
	
	
	
	protected void initTmpTrajStuff(boolean _trajIsFlat){
//		vsblStLoc = new float[numSubScrInWin];
//		seqVisStTime = new int[numSubScrInWin];
		//drawnTrajAra[curDrnTrajScrIDX][curDrnTrajStaffIDX] init 2 and 10
		tmpDrawnTraj= new myDrawnSmplTraj(pa,this,topOffY,trajFillClrCnst, trajStrkClrCnst, _trajIsFlat, !_trajIsFlat);
		curDrnTrajScrIDX = 0;
//		for(int i =0;i<numSubScrInWin;++i){//
//			vsblStLoc[i] = 0;
//			seqVisStTime[i] = 0;
//		}
	}	
	//initialize traj-specific stuff for this window - only should be called once a song has been made, to have the # of trajectories available
	protected void initTrajStructs(){
		drwnTrajMap = new TreeMap<Integer,TreeMap<String,ArrayList<myDrawnSmplTraj>>>();
		TreeMap<String,ArrayList<myDrawnSmplTraj>> tmpTrajMap;
		for(int scr =0;scr<numSubScrInWin; ++scr){
			tmpTrajMap = new TreeMap<String,ArrayList<myDrawnSmplTraj>>();
			for(int traj =0; traj<numTrajInSubScr[scr]; ++traj){
				tmpTrajMap.put(getTrajAraKeyStr(traj), new ArrayList<myDrawnSmplTraj>());			
			}	
			drwnTrajMap.put(scr, tmpTrajMap);
		}		
	}	

	//custom UI objects
	protected void initExtraUIObjs() {
		initXtraUIObjsIndiv();
	}
	
/////////////////////
///Misc utils
/////////////////////
	//get nValType value and octave of note displaced by dispamt # of half steps (negative for down)
	public int[] getNoteDisp(NoteData _note, int dispAmt){
		int[] res = new int[]{0,0};
		int octDisp = dispAmt/12;		//+/- # of octaves
		//if(abs(dispAmt)>12){outStr2Scr("----->Danger attempting to modify note by more than 1 octave in getNoteDisp.  ",true);return res;}			
		int oldNVal = _note.name.getVal(),
			newNValNoMod = (oldNVal + dispAmt),
			newNVal = (newNValNoMod+12)%12;
		res[0] = newNVal;
		res[1] = _note.octave;
		int octModAmt = 0;
		if(newNVal < newNValNoMod)		{	octModAmt++;}		//if newVal is < newVal without mod then wrapped around while adding 
		else if(newNVal > newNValNoMod) {	octModAmt--;}		//if newVal is > newVal without mod then wrapped around negatively while subtracting			
		return res;			
	}
	
	
	
	
	
	
	
	//when score is set up or modified, use this to distribute references to all windows of current score
	public void setScoreInstrVals(TreeMap<String,myInstrument> _instrs, String[] _scoreStaffNames){
		scoreStaffNames = new String[_scoreStaffNames.length];
		drawTrajBoxFillClrs = new int[scoreStaffNames.length][4];
		for(int i =0; i<scoreStaffNames.length; ++i){	
			setScoreStaffName(i, _scoreStaffNames[i]);
			drawTrajBoxFillClrs[i] = pa.getRndClr();
		}	
		instrs = pa.score.getInstrumentList();

		numSubScrInWin = 2;						//# of subscreens in a window.  will generally be 1, but with sequencer will have at least 2 (piano roll and score view)
		numTrajInSubScr = new int[]{scoreStaffNames.length, scoreStaffNames.length};	
		initTrajStructs();		
		setupGUIObjsAras();					//rebuild UI object stuff
		//initNoteOutIndiv();
		setScoreInstrValsIndiv();
	}	

	//set global key signature
	public void setGlobalKeySigVal(int idx){
		//msgObj.dispInfoMessage("myMusicSimWindow","Func","key sig idx : " + idx);
		if((idx >= 0) && (idx < 12)){
			dispFlags[fltrByKeySig] = true;
			glblKeySig = new myKeySig(pa, keySigVals.getVal(idx));	
		}
		else {	dispFlags[fltrByKeySig] = false; glblKeySig = new myKeySig(pa, keySigVals.CMaj);	msgObj.dispInfoMessage("myMusicSimWindow","Func","glblKeySig not correctly set : " + glblKeySig.toString());}	
		glblKeyNotesAra = glblKeySig.getKeyNotesAsList();
		
		setGlobalKeySigValIndiv(idx, pbe.getCurrentTime());	
	}//	setGlobalKeySigVal
	//set time signature at time passed - for score, set it at nearest measure boundary
	public void setGlobalTimeSigVal(int tsnum, int tsdenom, durType _beatNoteType){
		glblBeatNote = _beatNoteType;
		//msgObj.dispInfoMessage("myMusicSimWindow","Func","SetCurrentTimeSigVal in myDispWIn : " + tsnum + " / " + tsdenom);
		glblTimeSig = new myTimeSig(pa, tsnum, tsdenom, glblBeatNote);		
		setGlobalTimeSigValIndiv(tsnum, tsdenom, _beatNoteType, pbe.getCurrentTime());	
	}
	//set time signature at time passed - for score, set it at nearest measure boundary
//	public void setGlobalTempoVal(float tempo){
//		glblTempo = tempo;		
//		if(score != null){
//			for(int i =0; i<score.staffs.size(); ++i){
//				//drawnPRollNotes[i] = new TreeMap<Integer,myNote>();
//				instrNoteOut[i].pauseNotes();
//				instrNoteOut[i].setTempo(tempo);
//				instrNoteOut[i].resumeNotes();			
//			}		
//		}
//		setGlobalTempoValIndiv(tempo, pbe.getCurrentTime());
//	}
	public void setGlobalTempoVal(float tempo){
		glblTempo = tempo;	
		pa.glblOut.pauseNotes();
		pa.glblOut.setTempo(tempo);
		pa.glblOut.resumeNotes();	
//		if(score != null){
//			for(int i =0; i<score.staffs.size(); ++i){
//				//drawnPRollNotes[i] = new TreeMap<Integer,myNote>();
//				instrNoteOut.pauseNotes();
//				instrNoteOut.setTempo(tempo);
//				instrNoteOut.resumeNotes();			
//			}		
//		}
		setGlobalTempoValIndiv(tempo, pbe.getCurrentTime());
	}

	//set global key signature
	public void setLocalKeySigVal(int idx){
		myKeySig ks;
		if((idx >= 0) && (idx < 12)){			
			ks = new myKeySig(pa, keySigVals.getVal(idx));	
		}
		else {	ks = new myKeySig(pa, keySigVals.CMaj);	
		msgObj.dispInfoMessage("ks not correctly set @ idx : " + idx + " : " + ks.toString());}	
		ArrayList<nValType> keyNotesAra = ks.getKeyNotesAsList();
		
		setLocalKeySigValIndiv(ks,keyNotesAra, pbe.getCurrentTime());	
	}	
	//set time signature at time passed - for score, set it at nearest measure boundary
	public void setLocalTimeSigVal(int tsnum, int tsdenom, durType _beatNoteType){
		//myTimeSig ts = new myTimeSig(pa, tsnum, tsdenom, _beatNoteType);		
		setLocalTimeSigValIndiv(tsnum, tsdenom, _beatNoteType, pbe.getCurrentTime());	
	}
	//set time signature at time passed - for score, set it at nearest measure boundary
	public void setLocalTempoVal(float tempo){
		setLocalTempoValIndiv(tempo, pbe.getCurrentTime());
	}
	
	//displays point with a name
	public void showKeyPt(myPoint a, String s, float rad){	pa.show(a,rad, s, new myVector(10,-5,0), pa.gui_Cyan, dispFlags[trajPointsAreFlat]);	}	

	
	/**
	 * Build button descriptive arrays : each object array holds true label, false label, and idx of button in owning child class
	 * this must return count of -all- booleans managed by privFlags, not just those that are interactive buttons (some may be 
	 * hidden to manage booleans that manage or record state)
	 * @param tmpBtnNamesArray ArrayList of Object arrays to be built containing all button definitions. 
	 * @return count of -all- booleans to be managed by privFlags
	 */
	@Override
	public final int initAllPrivBtns(ArrayList<Object[]> tmpBtnNamesArray) {
		
		
		return initAllPrivBtns(tmpBtnNamesArray);
	}
	public abstract int initAllPrivBtns_Indiv(ArrayList<Object[]> tmpBtnNamesArray);
	
	protected void procInitOut(AudioOutput _out){		//set up so that initial note has no delay
		_out.pauseNotes();
		playNote(_out, 0, 1, 0.01f);			//playing a note to prevent hesitation before first note
		_out.resumeNotes();
	}
	
	//return array of halfstep displacements for each note of chord, starting with 0 for root
	public int[] getChordDisp(chordType typ){
		switch (typ){
		case Major		:{return (new int[]{0,4,7});}	//1,3,5
		case Minor		:{return (new int[]{0,3,7});}	//1,b3,5
		case Augmented	:{return (new int[]{0,4,8});}	//1,3,#5
		case MajFlt5	:{return (new int[]{0,4,6});}	//1,3,b5
		case Diminished	:{return (new int[]{0,3,6});}	//1,b3,b5
		case Sus2       :{return (new int[]{0,2,7});}	//1,2,5
		case Sus4       :{return (new int[]{0,5,7});}	//1,4,5			
		case Maj6       :{return (new int[]{0,4,7,9});}	//1,3,5,6
		case Min6      	:{return (new int[]{0,3,7,9});}	//1,b3,5,6
		case Maj7      	:{return (new int[]{0,4,7,11});}	//1,3,5,7
		case Dom7      	:{return (new int[]{0,4,7,10});}	//1,3,5,b7
		case Min7      	:{return (new int[]{0,3,7,10});}	//1,b3,5,b7
		case Dim7      	:{return (new int[]{0,3,6,9});}	//1,b3,b5,bb7==6
		case None      	:{return (new int[]{0});}	//not a predifined chord type
		default         :{return (new int[]{0});}						
		}			
	}
	
	//where is middle c for this measure's notes (if clef is different from default staff clef, based on instrument)
	public float getC4LocMultClef(clefVal clef, boolean isGrandStaff){
		if(isGrandStaff){return 5;}
		switch (clef){
			case Treble : {return 5;}
			case Bass   : {return -1;}
			case Alto   : {return 2;}
			case Tenor  : {return 2;}
			case Drum   : {return 2;}
			case Piano	: {return 5;}//should never get here - only will happen if somehow piano grandstaff doesn't get appropriate boolean set
			default:		break;
		}
		return 0;
	}	
	
	//note is tilted ellipse with stem (if  not whole note), and filled (if not whole or half note) and with flags (if 8th or smaller)
	//type  == type of note (0 is whole, 1 is half, 2 is qtr, 3 is eighth, etc)
	//nextNoteLoc is location of next note yikes.
	//flags : 0 : isDotted, 1 : isTuple, 2 : isRest, 3 : isChord, 4 : drawStemUp,   5 : isConnected, 6 :showDisplacement msg (8va, etc), 7 : isInStaff, 8 : isFlipped(part of chord and close to prev note, put note on other side of stem),
	//grpPos : 0 first in group of stem-tied notes, 1 : last in group of stemTied notes, otherwise neither 
	public void drawNote(float noteW, myVector nextNoteLoc, int noteTypIdx, int grpPos, boolean[] flags, float numLedgerLines){
		pa.pushMatState(); 
		//draw body
		//noteIdx : -2,-1, 0, 1, 2, 3
		pa.rotate(MyMathUtils.FIFTH_PI_F,0,0,1);
		pa.setStrokeWt(1);
		pa.setColorValFill(pa.gui_Black, 255);
		pa.setColorValStroke(pa.gui_Black, 255);
		if(flags[myNote.isChord] && flags[myNote.isFlipped]){pa.translate(-noteW,0,0);}		//only flip if close to note
		//line(-noteW,0,0,noteW,0,0);//ledger lines, to help align the note
		if(noteTypIdx <= -1){	pa.setStrokeWt(2);	pa.noFill();	}
		pa.drawEllipse2D(0,0,noteW, .75f*noteW);
		pa.rotate(-MyMathUtils.QUARTER_PI_F,0,0,1);
		if(flags[myNote.isDotted]){pa.drawEllipse2D(1.5f*noteW,0,3,3);	}//is dotted
		if(noteTypIdx > -2){//has stem and is not last in stemmed group
			if(flags[myNote.drawStemUp]){	pa.translate(-.5*noteW,0,0); pa.drawLine(0,0,0,0,-4*noteW,0);}//draw up
			else {							pa.translate(.5*noteW,0,0);pa.drawLine(0,0,0,0,4*noteW,0);}//drawDown
			if((noteTypIdx > 0) && (1 != grpPos)){//has flag and is not last in group so draw flag
				float flagW, flagH1;
				if(flags[myNote.drawCnncted]){	//is tied,  TODO
					flagW = (float)nextNoteLoc.x;
					flagH1 = (float)nextNoteLoc.y;
				} else{
					flagW = 2 * noteW;
					flagH1 = noteW;
				}
				float moveDir;//direction multiplier
				if(flags[myNote.drawStemUp]){	 moveDir = noteW; pa.translate(0,-4*noteW,0);}//draw up
				else {			 moveDir = -noteW;pa.translate(0,4*noteW,0);}//drawDown
				float yVal = 0,flagH2 = - .5f*moveDir;;
				for(int i =0; i<noteTypIdx;++i){ // noteIdx is # of flags to draw too
					quad(0,yVal,flagW,yVal+flagH1,flagW,yVal+flagH1+flagH2,0,yVal + flagH2);
					yVal += moveDir;
				}
			}			
		}
		if(numLedgerLines != 0.0f){ //draw ledger lines outside staff, either above or below (if negative # then below note, above staff)
			pa.translate(-noteW*.5f,0);
			int mult;
			if(numLedgerLines < 0 ){mult=1;} else {mult=-1;}
			if(flags[myNote.isOnLdgrLine]){//put ledger line through middle of note
				pa.drawLine(-noteW,0,0,noteW,0,0);					
			} else {
				pa.drawLine(-noteW,.5f*noteW,0,noteW,.5f*noteW,0);			
			}
			pa.pushMatState(); 
			if(Math.abs(numLedgerLines) - (int)(Math.abs(numLedgerLines)) != 0){pa.translate(0,.5f*noteW);}
			for(int i =0;i<Math.abs(numLedgerLines);++i){
				pa.translate(0,mult*noteW);
				pa.drawLine(-noteW,0,0,noteW,0,0);
			}
			pa.popMatState();
			
		}
		pa.popMatState();
	}//draw a note head

	//flags : 0 : isDotted, 1 : drawUp, 2 : isFlipped(part of chord)
	//durType vals : Whole(256),Half(128),Quarter(64),Eighth(32),Sixteenth(16),Thirtisecond(8); 
	public void drawRest(float restW, int restIdx, boolean isDotted){
		pa.pushMatState(); 
		//draw rest
		//restIdx : -2,-1, 0, 1, 2, 3
		if(restIdx > -1){//draw image
			pa.translate(restDisp[restIdx][0], restDisp[restIdx][1],0);		//center image of rest - move up 2 ledger lines
			pa.scale(1,1.2f,1);
			image(restImgs[restIdx], 0,0);				
		} else {//draw box
			if(restIdx == -2){	pa.translate(0,-.5f * restW,0);}//whole rest is above half rest
			pa.drawRect(-.5f * restW, 0, restW,.5f * restW);				
		}
		pa.popMatState();
	}
	
	//return summed outputs to simulation

	//playing trajectory note SortedMap<Integer, myNote> tmpNotes
	//TODO replace with reticle-driven play and stop of notes
	protected void addTrajNoteToPlay(SortedMap<Integer, myNote> tmpNotes){
		if((null == tmpNotes) || (tmpNotes.size() == 0)){return;}
		//pa.glblOut.pauseNotes();
		for(SortedMap.Entry<Integer, myNote> note : tmpNotes.entrySet()) { 	
			if(note.getValue().n.name == nValType.rest){continue;}
			myNote _n = note.getValue();
			//msgObj.dispInfoMessage("myMusicSimWindow","Func","Play note : "+ _n.n.nameOct + " start : "+ _n.n.getStartPlayback() + " dur: " +  _n.n.getDurPlayback() * tempoDurRatio);
			playNote(pa.glblOut, _n.n.getStartPlayback(), _n.n.getDurPlayback() * tempoDurRatio, _n.n.freq);
			if(_n.flags[myNote.isChord]){
				for(Entry<String, myNote> cnote : ((myChord)(_n)).cnotes.entrySet()){
					myNote chrdN = cnote.getValue();
					if(_n.ID != chrdN.ID){
						//msgObj.dispInfoMessage("myMusicSimWindow","Func","Play note of chord : "+ chrdN.n.nameOct + " start : "+ chrdN.n.getStartPlayback() + " dur: " +  chrdN.n.getDurPlayback() * tempoDurRatio);
						playNote(pa.glblOut, chrdN.n.getStartPlayback(), chrdN.n.getDurPlayback() * tempoDurRatio, chrdN.n.freq);
					}
				}
			}
		}
		//instrNoteOut.resumeNotes();
		this.dispFlags[myMusicSimWindow.notesLoaded] = true;
	}//addTrajNoteToPlay
	
	
	//modify playback engine, from UI interaction
	public void modCurrentPBETime(float modAmt){
		pbe.modCurTime(modAmt);		
		modMySongLoc(modAmt);
	}
	
	//audio stuff, if window has audio
	public void playNote(AudioOutput out, float start, float duration, float freq){out.playNote(start,duration,freq);}	
	public void play(){
		if((!dispFlags[plays]) ||(!dispFlags[showIDX])){return;}	//if this doesn't play, or it is not being shown, exit immediately
//		//msgObj.dispInfoMessage("myMusicSimWindow","Func","ID :" + ID+" play() : startTime : " + pa.glblStartPlayTime + " last play time : " + pa.glblLastPlayTime + " modAmt Sec : " + String.format("%.4f", modAmtSec) + " frate :"+ String.format("%.4f", pa.frameRate));
//		pbe.play(modAmtSec);
//		//global play handling - move and draw reticle line		
		this.dispFlags[myMusicSimWindow.notesLoaded] = false;
		//msgObj.dispInfoMessage("myMusicSimWindow","Func","start playing : \n");
		playMe();	//window-specific playing code		
	}//play
	
	public void stopPlaying(){
		if((!dispFlags[plays]) ||(!dispFlags[showIDX])){return;}	//if this doesn't play, or it is not being shown, exit immediately
		pbe.stop();
		//for(int i =0; i<score.staffs.size(); ++i){
			//drawnPRollNotes[i] = new TreeMap<Integer,myNote>();			
//			instrNoteOut[i].pauseNotes();
//			instrNoteOut[i].close();
//			instrNoteOut[i] = pa.getAudioOut();
		if(this.ID != 2){
			pa.glblOut.close();
			pa.resetAudioOut();			//TODO
		}
		//	procInitOut(instrNoteOut[i]);
		//}			
		//clear out playing window
		stopMe();
	}
	
	public void movePBEReticle(float modAmtSec){
		if((!dispFlags[plays]) ||(!dispFlags[showIDX])){return;}	//if this doesn't play, or it is not being shown, exit immediately
		//msgObj.dispInfoMessage("myMusicSimWindow","Func","ID :" + ID+" play() : startTime : " + pa.glblStartPlayTime + " last play time : " + pa.glblLastPlayTime + " modAmt Sec : " + String.format("%.4f", modAmtSec) + " frate :"+ String.format("%.4f", pa.frameRate));
		if(dispFlags[notesLoaded]){pbe.play(modAmtSec);}
	}

	//get notes from sequencer/sphere ui for ocean visualization
	protected abstract SortedMap<Integer, myNote> getNotesNow();
	
	protected abstract void setScoreInstrValsIndiv();
		
	//ui init routines
	public abstract String getUIListValStr(int UIidx, int validx);
	
	public abstract void clickDebug(int btnNum);
	
	//init xtra ui objects on a per-window basis
	protected abstract void initXtraUIObjsIndiv();
	

	protected abstract void playMe();
	protected abstract void modMySongLoc(float modAmt);
	
	//set current key signature, at time passed - for score, set it at nearest measure boundary
	protected abstract void setGlobalKeySigValIndiv(int idx, float time);	
	//set time signature at time passed - for score, set it at nearest measure boundary
	protected abstract void setGlobalTimeSigValIndiv(int tsnum, int tsdenom, durType _beatNoteType, float time);	
	//set time signature at time passed - for score, set it at nearest measure boundary
	protected abstract void setGlobalTempoValIndiv(float tempo, float time);
	
	//set current key signature, at time passed - for score, set it at nearest measure boundary
	protected abstract void setLocalKeySigValIndiv(myKeySig lclKeySig, ArrayList<nValType> lclKeyNotesAra, float time);	
	//set time signature at time passed - for score, set it at nearest measure boundary
	protected abstract void setLocalTimeSigValIndiv(int tsnum, int tsdenom, durType _beatNoteType, float time);	
	//set time signature at time passed - for score, set it at nearest measure boundary
	protected abstract void setLocalTempoValIndiv(float tempo, float time);

	
	public String toString(){
		String res = super.toString() + "Music Window : "+name+"\n";	
		return res;
	}

	/**
	 * @return the scoreStaffNames
	 */
	public String getScoreStaffName(int idx) {return scoreStaffNames[idx];}

	/**
	 * @param scoreStaffNames the scoreStaffNames to set
	 */
	public void setScoreStaffName(int idx, String scoreStaffName) {
		this.scoreStaffNames[idx] = scoreStaffName;
	}
}//dispWindow

