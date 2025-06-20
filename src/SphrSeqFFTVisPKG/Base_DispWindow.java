package SphrSeqFFTVisPKG;

import java.util.*;
import java.util.Map.Entry;

import SphrSeqFFTVisPKG.clef.enums.keySigVals;
import SphrSeqFFTVisPKG.instrument.myInstrument;
import SphrSeqFFTVisPKG.note.myChord;
import SphrSeqFFTVisPKG.note.myNote;
import SphrSeqFFTVisPKG.note.enums.noteDurType;
import SphrSeqFFTVisPKG.note.enums.noteValType;
import SphrSeqFFTVisPKG.staff.myKeySig;
import SphrSeqFFTVisPKG.staff.myTimeSig;
import SphrSeqFFTVisPKG.ui.controls.myPlaybackEngine;
import base_Render_Interface.IRenderInterface;
import base_Math_Objects.MyMathUtils;
import base_Math_Objects.vectorObjs.doubles.myPoint;
import base_Math_Objects.vectorObjs.doubles.myVector;
import ddf.minim.AudioOutput;
import processing.core.PConstants;

//abstract class to hold base code for a menu/display window (2D for gui, etc), to handle displaying and controlling the window, and calling the implementing class for the specifics
public abstract class Base_DispWindow {
	public SeqVisFFTOcean pa;
	public static int winCnt = 0;
	public int ID;
	
	public myPlaybackEngine pbe;
	
	public String name, winText;	
	
	//global sequence values - static
	public static myKeySig glblKeySig;
	public static float glblTempo;
	public static myTimeSig glblTimeSig;
	public static noteDurType glblBeatNote;
	public static ArrayList<noteValType> glblKeyNotesAra;					//notes in the current key -- used to force notes to key

		
	public int[] fillClr, strkClr;
	public int trajFillClrCnst, trajStrkClrCnst;
	public float[] rectDim, closeBox, rectDimClosed, mseClickCrnr;	
	public static final float gridYMult = 1.0f/67.0f, gridXMult = .5625f * gridYMult;
	//public static final float xOff = 20 , yOff = 20, btnLblYOff = 2 * yOff, rowStYOff = yOff*.15f;
	public static final float xOff = 20 , txtHeightOff = 13.0f,// * (IRenderInterface.txtSz/12.0f), 
			btnLblYOff = 2 * txtHeightOff, rowStYOff = txtHeightOff*.15f;
	public static final int topOffY = 40;			//offset values to render boolean menu on side of screen - offset at top before drawing
	public static final float clkBxDim = 10;//size of interaction/close window box in pxls

	public int lastAnimTime, stAnimTime;
	
	public int pFlagIdx;					//the flags idx in the PApplet that controls this window - use -1 for none	
	public boolean[] dispFlags;
	
	public static final int 
				showIDX 			= 0,			//whether or not to show this window
				is3DWin 			= 1,
				closeable 			= 2,			//window is able to be closed
				hasScrollBars 		= 3,			//this window has scroll bars (both vert and horizontal)

				canDrawTraj 		= 4,			//whether or not this window will accept a drawn trajectory
				drawingTraj 		= 5,			//whether a trajectory is being drawn in this window - all windows handle trajectory input, has different functions in each window
				editingTraj 		= 6,			//whether a trajectory is being edited in this window
				showTrajEditCrc 	= 7,			//set this when some editing mechanism has taken place - draw a circle of appropriate diameter at mouse and shrink it quickly, to act as visual cue
				smoothTraj 			= 8,			//trajectory has been clicked nearby, time to smooth
				trajDecays 			= 9,			//drawn trajectories eventually/immediately disappear
				trajPointsAreFlat 	= 10,			//trajectory drawn points are flat (for pick, to prevent weird casting collisions
				
				procMouseMove 		= 11,
				mouseSnapMove		= 12,			//mouse locations for this window are discrete multiples - if so implement inherited function to calculate mouse snap location
				
				fltrByKeySig 		= 13,
				plays 				= 14,			//this window responds to playing
				drawsPBERet 		= 15,
				notesLoaded 		= 16,			//notes are all loaded into out for all staffs - wait to move pbe until this has happened.
				uiObjMod			= 17;			//a ui object in this window has been modified

	public static final int numDispFlags = 18;
	
	//private window-specific flags and UI components (buttons)
	public boolean[] privFlags;
	public String[] truePrivFlagNames; //needs to be in order of flags	
	public String[] falsePrivFlagNames;//needs to be in order of flags
	
		//for boolean buttons based on child-class window specific values
	public int[][] privFlagColors;
	public int[] privModFlgIdxs;										//only modifiable idx's will be shown as buttons - this needs to be in order of flag names
	public float[][] privFlagBtns;									//clickable dimensions for these buttons
	public int numClickBools;
	
	
	//edit circle quantities for visual cues when grab and smoothen
	public static final int[] editCrcFillClrs = new int[] {IRenderInterface.gui_FaintMagenta, IRenderInterface.gui_FaintGreen};			
	public static final float[] editCrcRads = new float[] {20.0f,40.0f};			
	public static final float[] editCrcMods = new float[] {1f,2f};			
	public final myPoint[] editCrcCtrs = new myPoint[] {new myPoint(0,0,0),new myPoint(0,0,0)};			
	public float[] editCrcCurRads = new float[] {0,0};	
	
	//UI objects in this window
	//GUI Objects
	public myGUIObj[] guiObjs_Numeric;	
	public int msClkObj, msOvrObj;												//myGUIObj object that was clicked on  - for modification, object mouse moved over
	public double[] uiClkCoords;												//subregion of window where UI objects may be found
	public static final double uiWidthMult = 9;							//multipler of size of label for width of UI components, when aligning components horizontally
	
	public double[][] guiMinMaxModVals;					//min max mod values
	public double[] guiStVals;							//starting values
	public String[] guiObjNames;							//display labels for UI components	
	//idx 0 is treat as int, idx 1 is obj has list vals, idx 2 is object gets sent to windows
	public boolean[][] guiBoolVals;						//array of UI flags for UI objects

	public float[] vsblStLoc;							//pxl location of first visisble note on the left of the screen - idx0 for piano
	public int[] seqVisStTime;							//seq time of first note visible in sequence window
	//drawn trajectory
	public myDrawnSmplTraj tmpDrawnTraj;						//currently drawn curve and all handling code - send to instanced owning screen

	//all trajectories in this particular display window - String key is unique identifier - like staff name or instrument name
	public TreeMap<Integer,TreeMap<String,ArrayList<myDrawnSmplTraj>>> drwnTrajMap;				
	
	public int numSubScrInWin = 2;									//# of subscreens in a window.  will generally be 1, but with sequencer will have at least 2 (piano roll and score view)
	public int[] numTrajInSubScr;									//# of trajectories available for each sub screen
	public static final int 
				prlTrajIDX = 0,
				scrTrajIDX = 1;
	
	public int curDrnTrajScrIDX;									//currently used/shown drawn trajectory - 1st idx (which screen)
	public int curTrajAraIDX;										//currently used/shown drawn trajectory - 2nd idx (which staff trajectory applies to)

	public int[][] drawTrajBoxFillClrs;
	
	public final String[] keySigs = new String[]{"CMaj - 0 #'s","GMaj - 1 #","DMaj - 2 #'s","Amaj - 3 #'s","EMaj - 4 #'s","BMaj - 5 #'s","FsMaj - 6 #'s",
			"CsMaj - 5 b's","GsMaj - 4 b's","DsMaj - 3 b's","AsMaj - 2 b's","Fmaj - 1 b"};
	
	public final int[] timeSigDenom = new int[]{1,2,4,8,16,32};
	
	public final String[] noteVals = new String[]{"Whole","Half","Quarter","Eighth","Sixteenth","Thirtisecond"};
	public final noteDurType[] noteValTypes = new noteDurType[]{noteDurType.Whole,noteDurType.Half,noteDurType.Quarter,noteDurType.Eighth,noteDurType.Sixteenth,noteDurType.Thirtisecond};
	
	//score - related values
	//protected myScore score;									//score being displayed
	protected float[] scoreRect;								//dimensions of score area
	protected String[] scoreStaffNames;					//array of each score staff name
	protected TreeMap<String,myInstrument> instrs;	
	//to control how much is shown in the window - if measures extend off the screen
	public myScrollBars[] scbrs;
	
	//individual instrument note output - replaces scrollNoteOut
	//public AudioOutput[] instrNoteOut;
	public float tempoDurRatio = 1.0f;
	
	public Base_DispWindow(SeqVisFFTOcean _p, String _n, int _flagIdx, int[] fc,  int[] sc, float[] rd, float[] rdClosed, String _winTxt, boolean _canDrawTraj) {
		pa=_p;
		pbe = new myPlaybackEngine(pa, this, new int[]{255,0,255,255}, new int[]{0,255,255,255}, new float[]{0,pa.height});
		ID = winCnt++;
		name = _n;
		pFlagIdx = _flagIdx;
		fillClr = new int[4];	strkClr = new int[4];	rectDim = new float[4];	rectDimClosed = new float[4]; closeBox = new float[4]; uiClkCoords = new double[4];
		for(int i =0;i<4;++i){fillClr[i] = fc[i];strkClr[i]=sc[i];rectDim[i]=rd[i];rectDimClosed[i]=rdClosed[i];}		
				
		winText = _winTxt;
		trajFillClrCnst = IRenderInterface.gui_Black;		//override this in the ctor of the instancing window class
		trajStrkClrCnst = IRenderInterface.gui_Black;
		
		msClkObj = -1;//	lastTrajIDX = -1; //lastPBEQueryPlayTime = 0;	
		msOvrObj = -1;
		stAnimTime=0;
		lastAnimTime=0;
	}	
	
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
	//build UI clickable region
	protected void setUIClkCoords(double x1, double y1, double x2, double y2){
		//pa.outStr2Scr("setUIClkCoords 4 arg in :"+ name+ " ID : " + ID + ": (" + x1+","+y1+","+x2+","+y2 +")");
		uiClkCoords[0] = x1;uiClkCoords[1] = y1;uiClkCoords[2] = x2; uiClkCoords[3] = y2;}
	protected void setUIClkCoords(double[] cpy){	uiClkCoords[0] = cpy[0];uiClkCoords[1] = cpy[1];uiClkCoords[2] = cpy[2]; uiClkCoords[3] = cpy[3];}

	public void initFlags(){dispFlags = new boolean[numDispFlags];for(int i =0; i<numDispFlags;++i){dispFlags[i]=false;}}		
	
	//child-class flag init
	protected void initPrivFlags(int numPrivFlags){privFlags = new boolean[numPrivFlags];for(int i=0;i<numPrivFlags;++i){privFlags[i] = false;}}
	//set up initial colors for sim specific flags for display
	protected void initPrivFlagColors(){
		privFlagColors = new int[truePrivFlagNames.length][3];
		for (int i = 0; i < privFlagColors.length; ++i) { privFlagColors[i] = new int[]{(int) pa.random(150),(int) pa.random(100),(int) pa.random(150)}; }			
	}
	
	//set up child class button rectangles
	protected void initUIBox(){
		//pa.outStr2Scr("initUIBox in :"+ name+ " ID : " + ID);		
		double [] menuUIClkCoords = pa.getUIRectVals(ID); // pa.dispWinFrames[pa.dispMenuIDX].uiClkCoords;
		setUIClkCoords(menuUIClkCoords[0],menuUIClkCoords[3],menuUIClkCoords[2],menuUIClkCoords[3]);				//final value will be modified when UI objects are made
		//initialize beginning locations for playback reticle - override this in initMe, and modify it in code for scrollbars
		vsblStLoc = new float[]{0,0};
		seqVisStTime = new int[] {0,0};
	}
	
	//custom UI objects
	protected void initExtraUIObjs() {
		initXtraUIObjsIndiv();
	}
	
	//calculate button length
	private static final float ltrLen = 5.0f;private static final int btnStep = 5;
	private float calcBtnLength(String tStr, String fStr){return btnStep * (int)(((MyMathUtils.max(tStr.length(),fStr.length())+4) * ltrLen)/btnStep);}
	//set up child class button rectangles TODO
	//yDisp is displacement for button to be drawn
	protected void initPrivBtnRects(float yDisp, int numBtns){
		//pa.outStr2Scr("initPrivBtnRects in :"+ name + "st value for uiClkCoords[3]");
		float maxBtnLen = .95f * pa.menuWidth, halfBtnLen = .5f*maxBtnLen;
		//pa.pr("maxBtnLen : " + maxBtnLen);
		privFlagBtns = new float[numBtns][];
		this.uiClkCoords[3] += txtHeightOff;
		float oldBtnLen = 0;
		boolean lastBtnHalfStLine = false, startNewLine = true;
		for(int i=0; i<numBtns; ++i){						//clickable button regions - as rect,so x,y,w,h - need to be in terms of sidebar menu 
			float btnLen = calcBtnLength(truePrivFlagNames[i].trim(),falsePrivFlagNames[i].trim());
			//either button of half length or full length.  if half length, might be changed to full length in next iteration.
			//pa.pr("initPrivBtnRects: i "+i+" len : " +btnLen+" cap 1: " + truePrivFlagNames[i].trim()+"|"+falsePrivFlagNames[i].trim());
			if(btnLen > halfBtnLen){//this button is bigger than halfsize - it needs to be made full size, and if last button was half size and start of line, make it full size as well
				btnLen = maxBtnLen;
				if(lastBtnHalfStLine){//make last button full size, and make button this button on another line
					privFlagBtns[i-1][2] = maxBtnLen;
					this.uiClkCoords[3] += txtHeightOff;
				}
				privFlagBtns[i]= new float[] {(float)(uiClkCoords[0]-xOff), (float) uiClkCoords[3], btnLen, txtHeightOff };				
				this.uiClkCoords[3] += txtHeightOff;
				startNewLine = true;
				lastBtnHalfStLine = false;
			} else {//button len should be half width unless this button started a new line
				btnLen = halfBtnLen;
				if(startNewLine){//button is starting new line
					lastBtnHalfStLine = true;
					privFlagBtns[i]= new float[] {(float)(uiClkCoords[0]-xOff), (float) uiClkCoords[3], btnLen, txtHeightOff };
					startNewLine = false;
				} else {//should only get here if 2nd of two <1/2 width buttons in a row
					lastBtnHalfStLine = false;
					privFlagBtns[i]= new float[] {(float)(uiClkCoords[0]-xOff)+oldBtnLen, (float) uiClkCoords[3], btnLen, txtHeightOff };
					this.uiClkCoords[3] += txtHeightOff;
					startNewLine = true;					
				}
			}			
			oldBtnLen = btnLen;
		}
		if(lastBtnHalfStLine){//set last button full length if starting new line
			privFlagBtns[numBtns-1][2] = maxBtnLen;			
		}
		this.uiClkCoords[3] += txtHeightOff;
		initPrivFlagColors();
	}//initPrivBtnRects
//	//pa.outStr2Scr("initPrivBtnRects in :"+ name + "st value for uiClkCoords[3]");
//	privFlagBtns = new float[numBtns][];
//	this.uiClkCoords[3] += yOff;
//	float locX = 0, oldBtnLen = 0;
//	for(int i=0; i<numBtns; ++i){						//clickable button regions - as rect,so x,y,w,h - need to be in terms of sidebar menu 
//		float btnLen = calcBtnLength(truePrivFlagNames[i],falsePrivFlagNames[i]);
//		if(i%2 == 0){//even btns always on a new line
//			privFlagBtns[i]= new float[] {(float)(uiClkCoords[0]-xOff), (float) uiClkCoords[3], btnLen, yOff };
//		} else {	//odd button
//			if ((btnLen + oldBtnLen) > .95f * pa.menuWidth){	//odd button, but too long -> new line		
//				this.uiClkCoords[3] += yOff;
//			}
//			privFlagBtns[i]= new float[] {(float)(uiClkCoords[0]-xOff), (float) uiClkCoords[3], btnLen, yOff };
//			this.uiClkCoords[3] += yOff;//always new line after odd button
//		}
//		oldBtnLen = btnLen;
//	};
//	this.uiClkCoords[3] += yOff;
//	initPrivFlagColors();

	
	public abstract void setPrivFlags(int idx, boolean val);
	
	public void initThisWin(boolean _canDrawTraj, boolean _trajIsFlat){initThisWin(_canDrawTraj, _trajIsFlat, false);}
	public void initThisWin(boolean _canDrawTraj, boolean _trajIsFlat, boolean _isMenu){
//		float stY = rectDim[1]+ .85f*rectDim[3];
//		setUIClkCoords(rectDim[0] + .1 * rectDim[2],stY,rectDim[0] + rectDim[2],stY + yOff);		//only for first window (sidebar menu) - all subsequent get over-written in iniUIBox
		initTmpTrajStuff(_trajIsFlat);	
		initFlags();	
		dispFlags[canDrawTraj] = _canDrawTraj;
		dispFlags[trajPointsAreFlat] = _trajIsFlat;
		dispFlags[closeable] = true;
		setGlobalTimeSigVal(4,4, pa.getDurTypeForNote(4));	
		if(!_isMenu){
			initUIBox();				//set up ui click region to be in sidebar menu below menu's entries - do not do here for sidebar
		}
		curTrajAraIDX = 0;
		initMe();
		setupGUIObjsAras();
		//record final y value for UI Objects
		initAllUIButtons();
		setClosedBox();
		mseClickCrnr = new float[2];		//this is offset for click to check buttons in x and y - since buttons for all menus will be in menubar, this should be the upper left corner of menubar - upper left corner of rect 
		mseClickCrnr[0] = 0;//pa.dispWinFrames[pa.dispMenuIDX].rectDim[0];
		mseClickCrnr[1] = 0;//pa.dispWinFrames[pa.dispMenuIDX].rectDim[1];
		
		if(dispFlags[hasScrollBars]){
			scbrs = new myScrollBars[numSubScrInWin];
			for(int i =0; i<numSubScrInWin;++i){scbrs[i] = new myScrollBars(pa, this);}
		}
	}	

	//for adding/deleting a screen programatically (loading a song) TODO
	//rebuild arrays of start locs whenever trajectory maps/arrays have changed - passed key is value modded in drwnTrajMap, 
	//modVal is if this is a deleted screen's map(0), a new map (new screen) at this location (1), or a modified map (added or deleted trajectory) (2)
	protected void rbldTrnsprtAras(int modScrKey, int modVal){
		if(modVal == -1){return;}//denotes no mod taken place
		int tmpNumSubScrInWin = drwnTrajMap.size();
		int [] tmpNumTrajInSubScr = new int[tmpNumSubScrInWin];
		float [] tmpVsblStLoc = new float[tmpNumSubScrInWin];
		int [] tmpSeqVisStTime = new int[tmpNumSubScrInWin];
		if(modVal == 0){			//deleted a screen's map
			if(tmpNumSubScrInWin != (numSubScrInWin -1)){pa.outStr2Scr("Error in rbldTrnsprtAras : screen traj map not removed at idx : " + modScrKey); return;}
			for(int i =0; i< numSubScrInWin; ++i){					
			}			
			
		} else if (modVal == 1){	//added a new screen, with a new map			
		} else if (modVal == 2){	//modified an existing map (new or removed traj ara)			
		}
		numSubScrInWin = tmpNumSubScrInWin;
		numTrajInSubScr = tmpNumTrajInSubScr;
		vsblStLoc = tmpVsblStLoc;
		seqVisStTime = tmpSeqVisStTime;
	}//rbldTrnsprtAras
	
	//for adding/deleting a screen programatically (loading a song) TODO
	//add or delete a new map of treemaps (if trajAraKey == "" or null), or a new map of traj arrays to existing key map
	protected void modTrajStructs(int scrKey, String trajAraKey, boolean del){
		int modMthd = -1;
		if(del){//delete a screen's worth of traj arrays, or a single traj array from a screen 
			if((trajAraKey == null) || (trajAraKey == "") ){		//delete screen map				
				TreeMap<String,ArrayList<myDrawnSmplTraj>> tmpTrajMap = drwnTrajMap.remove(scrKey);
				if(null != tmpTrajMap){			pa.outStr2Scr("Screen trajectory map removed for scr : " + scrKey);				modMthd = 0;}
				else {							pa.outStr2Scr("Error : Screen trajectory map not found for scr : " + scrKey); 	modMthd = -1; }
			} else {												//delete a submap within a screen
				modMthd = 2;					//modifying existing map at this location
				TreeMap<String,ArrayList<myDrawnSmplTraj>> tmpTrajMap = drwnTrajMap.get(scrKey);
				if(null == tmpTrajMap){pa.outStr2Scr("Error : Screen trajectory map not found for scr : " + scrKey + " when trying to remove arraylist : "+trajAraKey); modMthd = -1;}
				else { 
					ArrayList<myDrawnSmplTraj> tmpTrajAra = drwnTrajMap.get(scrKey).remove(trajAraKey);modMthd = 2;
					if(null == tmpTrajAra){pa.outStr2Scr("Error : attempting to remove a trajectory array from a screen but trajAra not found. scr : " + scrKey + " | trajAraKey : "+trajAraKey);modMthd = -1; }
				}
			}			 
		} else {													//add
			TreeMap<String,ArrayList<myDrawnSmplTraj>> tmpTrajMap = drwnTrajMap.get(scrKey);
			if((trajAraKey == null) || (trajAraKey == "") ){		//add map of maps - added a new screen				
				if(null != tmpTrajMap){pa.outStr2Scr("Error : attempting to add a new drwnTrajMap where one exists. scr : " + scrKey);modMthd = -1; }
				else {tmpTrajMap = new TreeMap<String,ArrayList<myDrawnSmplTraj>>();	drwnTrajMap.put(scrKey, tmpTrajMap);modMthd = 1;}
			} else {												//add new map of trajs to existing screen's map
				ArrayList<myDrawnSmplTraj> tmpTrajAra = drwnTrajMap.get(scrKey).get(trajAraKey);	
				if(null == tmpTrajMap){pa.outStr2Scr("Error : attempting to add a new trajectory array to a screen that doesn't exist. scr : " + scrKey + " | trajAraKey : "+trajAraKey); modMthd = -1; }
				else if(null != tmpTrajAra){pa.outStr2Scr("Error : attempting to add a new trajectory array to a screen where one already exists. scr : " + scrKey + " | trajAraKey : "+trajAraKey);modMthd = -1; }
				else {	tmpTrajAra = new ArrayList<myDrawnSmplTraj>();			tmpTrajMap.put(trajAraKey, tmpTrajAra);	drwnTrajMap.put(scrKey, tmpTrajMap);modMthd = 2;}
			}			
		}//if del else add
		//rebuild arrays of start loc
		rbldTrnsprtAras(scrKey, modMthd);
	}
	
	//this will set the height of the rectangle enclosing this window - this will be called when a window pushes up or pulls down this window
	//this resizes any drawn trajectories in this window, and calls the instance class's code for resizing
	public void setRectDimsY(float height){
		float oldVal = dispFlags[showIDX] ? rectDim[3] : rectDimClosed[3];
		rectDim[3] = height;
		rectDimClosed[3] = height;
		float scale  = height/oldVal;			//scale of modification - rescale the size and location of all components of this window by this
		//resize drawn all trajectories
		TreeMap<String,ArrayList<myDrawnSmplTraj>> tmpTreeMap = drwnTrajMap.get(this.curDrnTrajScrIDX);
		if((tmpTreeMap != null) && (tmpTreeMap.size() != 0)) {
			for(int i =0; i<tmpTreeMap.size(); ++i){
				ArrayList<myDrawnSmplTraj> tmpAra = tmpTreeMap.get(getTrajAraKeyStr(i));			
				if(null!=tmpAra){	for(int j =0; j<tmpAra.size();++j){		tmpAra.get(j).reCalcCntlPoints(scale);	}	}
			}	
		}
		if(dispFlags[hasScrollBars]){for(int i =0; i<scbrs.length;++i){scbrs[i].setSize();}}
		resizeMe(scale);
	}
//	//resize and relocate UI objects in this window for resizing window
//	public void resizeUIRegion(float scaleY, int numGUIObjs){
//		//re-size where UI objects should be drawn - vertical scale only currently
//		double [] curUIVals = new double[this.guiStVals.length];
//		for(int i=0;i<curUIVals.length;++i){	curUIVals[i] = guiObjs_Numeric[i].getVal();	}
//		
//		uiClkCoords[1]=calcDBLOffsetScale(uiClkCoords[1],scaleY,topOffY);
//		uiClkCoords[3]=calcDBLOffsetScale(uiClkCoords[3],scaleY,topOffY);
//		//setUIClkCoords(uiClkCoords[0],uiClkCoords[1],uiClkCoords[2],uiClkCoords[3]);
//		if(0!=numGUIObjs){
//			buildGUIObjs(guiObjNames,curUIVals,guiMinMaxModVals,guiBoolVals,new double[]{xOff,yOff});
//		}
//	}
	
	//build myGUIObj objects for interaction - call from setupMenuClkRegions of window, 
	//uiClkCoords needs to be derived before this is called by child class - maxY val(for vertical stack) or maxX val(for horizontal stack) will be derived here
	protected void buildGUIObjs(String[] guiObjNames, double[] guiStVals, double[][] guiMinMaxModVals, boolean[][] guiBoolVals, double[] off){
		//myGUIObj tmp; 
//		if(dispFlags[uiObjsAreVert]){		//vertical stack of UI components - clickable region x is unchanged, y changes with # of objects
			double stClkY = uiClkCoords[1];
			for(int i =0; i< guiStVals.length; ++i){
				guiObjs_Numeric[i] = buildGUIObj(i,guiObjNames[i],guiStVals[i], guiMinMaxModVals[i], guiBoolVals[i], new double[]{uiClkCoords[0], stClkY, uiClkCoords[2], stClkY+txtHeightOff},off);
				stClkY += txtHeightOff;
			}
			uiClkCoords[3] = stClkY;	
//		} else {			//horizontal row of UI components - clickable region y is unchanged, x changes with # of objects
//			double stClkX = uiClkCoords[0];
//			double UICompWidth;
//			for(int i =0; i< guiObjs_Numeric.length; ++i){
//				UICompWidth = (uiWidthMult + (guiBoolVals[i][1] ? 1 : 0)) * guiObjNames[i].length();
//				guiObjs_Numeric[i] = buildGUIObj(i,guiObjNames[i],guiStVals[i], guiMinMaxModVals[i], guiBoolVals[i], new double[]{stClkX, uiClkCoords[1], stClkX+UICompWidth , uiClkCoords[3]},off);
//				stClkX += UICompWidth;
//			}
//			uiClkCoords[2] = stClkX;	
//		}
	}//
	protected myGUIObj buildGUIObj(int i, String guiObjName, double guiStVal, double[] guiMinMaxModVals, boolean[] guiBoolVals, double[] xyDims, double[] off){
		myGUIObj tmp = new myGUIObj(pa, this,i, guiObjName, xyDims[0], xyDims[1], xyDims[2], xyDims[3], guiMinMaxModVals, guiStVal, guiBoolVals, off);		
		return tmp;
	}
	
	public float calcOffsetScale(double val, float sc, float off){float res =(float)val - off; res *=sc; return res+=off;}
	public double calcDBLOffsetScale(double val, float sc, double off){double res = val - off; res *=sc; return res+=off;}
	//returns passed current passed dimension from either rectDim or rectDimClosed
	public float getRectDim(int idx){return (dispFlags[showIDX] ? rectDim[idx] : rectDimClosed[idx]);	}

	public void setClosedBox(){
		if(dispFlags[showIDX]){	closeBox[0] = rectDim[0]+rectDim[2]-clkBxDim;closeBox[1] = rectDim[1];	closeBox[2] = clkBxDim;	closeBox[3] = clkBxDim;} 
		else {					closeBox[0] = rectDimClosed[0]+rectDimClosed[2]-clkBxDim;closeBox[1] = rectDimClosed[1];	closeBox[2] = clkBxDim;	closeBox[3] = clkBxDim;}
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
		//pa.outStr2Scr("key sig idx : " + idx);
		if((idx >= 0) && (idx < 12)){
			dispFlags[fltrByKeySig] = true;
			glblKeySig = new myKeySig(pa, keySigVals.getVal(idx));	
		}
		else {	dispFlags[fltrByKeySig] = false; glblKeySig = new myKeySig(pa, keySigVals.CMaj);	pa.outStr2Scr("glblKeySig not correctly set : " + glblKeySig.toString());}	
		glblKeyNotesAra = glblKeySig.getKeyNotesAsList();
		
		setGlobalKeySigValIndiv(idx, pbe.getCurrentTime());	
	}//	setGlobalKeySigVal
	//set time signature at time passed - for score, set it at nearest measure boundary
	public void setGlobalTimeSigVal(int tsnum, int tsdenom, noteDurType _beatNoteType){
		glblBeatNote = _beatNoteType;
		//pa.outStr2Scr("SetCurrentTimeSigVal in myDispWIn : " + tsnum + " / " + tsdenom);
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
		else {	ks = new myKeySig(pa, keySigVals.CMaj);	pa.outStr2Scr("ks not correctly set @ idx : " + idx + " : " + ks.toString());}	
		ArrayList<noteValType> keyNotesAra = ks.getKeyNotesAsList();
		
		setLocalKeySigValIndiv(ks,keyNotesAra, pbe.getCurrentTime());	
	}	
	//set time signature at time passed - for score, set it at nearest measure boundary
	public void setLocalTimeSigVal(int tsnum, int tsdenom, noteDurType _beatNoteType){
		//myTimeSig ts = new myTimeSig(pa, tsnum, tsdenom, _beatNoteType);		
		setLocalTimeSigValIndiv(tsnum, tsdenom, _beatNoteType, pbe.getCurrentTime());	
	}
	//set time signature at time passed - for score, set it at nearest measure boundary
	public void setLocalTempoVal(float tempo){
		setLocalTempoValIndiv(tempo, pbe.getCurrentTime());
	}
	
	//displays point with a name
	protected void showKeyPt(myPoint a, String s, float rad){	pa.show(a,rad, s, new myVector(10,-5,0), IRenderInterface.gui_Cyan, dispFlags[trajPointsAreFlat]);	}	
	//draw a series of strings in a column
	protected void dispMenuTxtLat(String txt, int[] clrAra, boolean showSphere){
		pa.setFill(clrAra, 255); 
		pa.translate(xOff*.5f,txtHeightOff*.5f);
		if(showSphere){pa.setStroke(clrAra, 255);		pa.sphere(5);	} 
		else {	pa.noStroke();		}
		pa.translate(-xOff*.5f,txtHeightOff*.5f);
		pa.text(""+txt,xOff,-txtHeightOff*.25f);	
	}
	protected void dispBoolStFlag(String txt, int[] clrAra, boolean state, float stMult){
		if(state){
			pa.setFill(clrAra, 255); 
			pa.setStroke(clrAra, 255);
		} else {
			pa.setColorValFill(IRenderInterface.gui_DarkGray,255); 
			pa.noStroke();	
		}
		pa.sphere(5);
		//pa.text(""+txt,-xOff,yOff*.8f);	
		pa.text(""+txt,stMult*txt.length(),txtHeightOff*.8f);	
	}
	
	//draw a series of strings in a row
	protected void dispBttnAtLoc(String txt, float[] loc, int[] clrAra){
		pa.setColorValFill(IRenderInterface.gui_White,255);
		pa.setColorValStroke(IRenderInterface.gui_Black,255);
		pa.rect(loc);		
		pa.setColorValFill(IRenderInterface.gui_Black,255);
		//pa.translate(-xOff*.5f,-yOff*.5f);
		pa.text(""+txt,loc[0] + (txt.length() * .3f),loc[1]+loc[3]*.75f);
		//pa.translate(width, 0);
	}
	
	protected void drawTraj(float animTimeMod){
		pa.pushMatrix();pa.pushStyle();	
		if(null != tmpDrawnTraj){tmpDrawnTraj.drawMe(animTimeMod);}
		TreeMap<String,ArrayList<myDrawnSmplTraj>> tmpTreeMap = drwnTrajMap.get(this.curDrnTrajScrIDX);
		if((tmpTreeMap != null) && (tmpTreeMap.size() != 0)) {
			for(int i =0; i<tmpTreeMap.size(); ++i){
				ArrayList<myDrawnSmplTraj> tmpAra = tmpTreeMap.get(getTrajAraKeyStr(i));			
				if(null!=tmpAra){	for(int j =0; j<tmpAra.size();++j){tmpAra.get(j).drawMe(animTimeMod);}}
			}	
		}
		pa.popStyle();pa.popMatrix();		
	}
	
	//draw ui objects
	public void drawGUIObjs(){	
		pa.pushMatrix();pa.pushStyle();	
		for(int i =0; i<guiObjs_Numeric.length; ++i){guiObjs_Numeric[i].draw();}
		if(guiObjs_Numeric.length > 0) {drawSepBar(this.uiClkCoords[3]);}
		pa.popStyle();pa.popMatrix();
	}
	
	//draw all boolean-based buttons for this window
	public void drawClickableBooleans() {	
		pa.pushMatrix();pa.pushStyle();	
		pa.setColorValFill(IRenderInterface.gui_Black,255);
		for(int i =0; i<privModFlgIdxs.length; ++i){//prlFlagRects dispBttnAtLoc(String txt, float[] loc, int[] clrAra)
			if(privFlags[privModFlgIdxs[i]] ){									dispBttnAtLoc(truePrivFlagNames[i],privFlagBtns[i],privFlagColors[i]);			}
			else {	if(truePrivFlagNames[i].equals(falsePrivFlagNames[i])) {	dispBttnAtLoc(truePrivFlagNames[i],privFlagBtns[i],new int[]{180,180,180});}	
					else {														dispBttnAtLoc(falsePrivFlagNames[i],privFlagBtns[i],new int[]{0,255-privFlagColors[i][1],255-privFlagColors[i][2]});}		
			}
		}		
		pa.popStyle();pa.popMatrix();
	}//drawClickableBooleans	
	
	public abstract void initAllUIButtons();
	
	//draw box to hide window
	protected void drawMouseBox(){
	    pa.setColorValFill((dispFlags[showIDX] ? IRenderInterface.gui_LightGreen : IRenderInterface.gui_DarkRed),255);
		pa.rect(closeBox);
		pa.setFill(strkClr);
		pa.text(dispFlags[showIDX] ? "Close" : "Open", closeBox[0]-35, closeBox[1]+10);
	}
	public void drawSmall(){
		pa.pushMatrix();				pa.pushStyle();	
		//pa.outStr2Scr("Hitting hint code draw small");
		pa.hint(PConstants.DISABLE_DEPTH_TEST);
		pa.noLights();		
		pa.setStroke(strkClr);
		pa.setFill(fillClr);
		//main window drawing
		pa.rect(rectDimClosed);		
		pa.setFill(strkClr);
		if(winText.trim() != ""){
			pa.text(winText.split(" ")[0], rectDimClosed[0]+10, rectDimClosed[1]+25);
		}		
		//close box drawing
		if(dispFlags[closeable]){drawMouseBox();}
		pa.hint(PConstants.ENABLE_DEPTH_TEST);
		pa.popStyle();pa.popMatrix();		
	}
	
	public void drawHeader(){
		if(!dispFlags[showIDX]){return;}
		pa.pushMatrix();				pa.pushStyle();			
		//pa.outStr2Scr("Hitting hint code drawHeader");
		pa.hint(PConstants.DISABLE_DEPTH_TEST);
		pa.noLights();		
		pa.setStroke(strkClr);
		pa.setFill(strkClr);
		if(winText.trim() != ""){	pa.ml_text(winText,  rectDim[0]+10,  rectDim[1]+10);}
		if(dispFlags[canDrawTraj]){	drawNotifications();	}				//if this window accepts a drawn trajectory, then allow it to be displayed
		if(dispFlags[closeable]){drawMouseBox();}
		if(dispFlags[hasScrollBars]){scbrs[curDrnTrajScrIDX].drawMe();}
		pa.lights();	
		pa.hint(PConstants.ENABLE_DEPTH_TEST);
		pa.popStyle();pa.popMatrix();	
	}
	
	public void draw(myPoint trans){
		stAnimTime = pa.millis();
		float animTimeMod = ((stAnimTime-lastAnimTime)/1000.0f);
		lastAnimTime = pa.millis();
		//if(dispFlags[showIDX]){pa.outStr2Scr("win ID : " + ID + " cur ui obj :"+ msClkObj);}
		if(this.dispFlags[Base_DispWindow.is3DWin]){	draw3D(trans,animTimeMod);	} else {	draw2D(trans,animTimeMod);	}
		//pa.outStr2Scr("ID:" + ID +" animTime mod : " + animTimeMod + " stAnimTime : " + stAnimTime+ " lastAnimTime : " + lastAnimTime);
	}
	
	public void draw3D(myPoint trans, float animTimeMod){
		if(!dispFlags[showIDX]){return;}
		pa.pushMatrix();				pa.pushStyle();			
		pa.setFill(fillClr);
		pa.setStroke(strkClr);
		//if(dispFlags[closeable]){drawMouseBox();}
		drawMe(animTimeMod);			//call instance class's draw
		if(dispFlags[canDrawTraj]){
			pa.pushMatrix();				pa.pushStyle();	
			drawTraj3D(animTimeMod, trans);			
			pa.popStyle();pa.popMatrix();
			if(dispFlags[showTrajEditCrc]){drawClkCircle();}
		}				//if this window accepts a drawn trajectory, then allow it to be displayed
		pa.popStyle();pa.popMatrix();		
	}//draw3D
	
	public void drawTraj3D(float animTimeMod,myPoint trans){
		pa.outStr2Scr("Base_DispWindow.drawTraj3D() : I should be overridden in 3d instancing class", true);
//		pa.pushMatrix();pa.pushStyle();	
//		if(null != tmpDrawnTraj){tmpDrawnTraj.drawMe(animTimeMod);}
//		TreeMap<String,ArrayList<myDrawnNoteTraj>> tmpTreeMap = drwnTrajMap.get(this.curDrnTrajScrIDX);
//		if((tmpTreeMap != null) && (tmpTreeMap.size() != 0)) {
//			for(int i =0; i<tmpTreeMap.size(); ++i){
//				ArrayList<myDrawnNoteTraj> tmpAra = tmpTreeMap.get(getTrajAraKeyStr(i));			
//				if(null!=tmpAra){	for(int j =0; j<tmpAra.size();++j){tmpAra.get(j).drawMe(animTimeMod);}}
//			}	
//		}
//		pa.popStyle();pa.popMatrix();		
	}//drawTraj3D
	
	public void draw2D(myPoint trans, float animTimeMod){
		if(!dispFlags[showIDX]){drawSmall();return;}
		pa.pushMatrix();				pa.pushStyle();	
		//pa.outStr2Scr("Hitting hint code draw2D");
		pa.hint(PConstants.DISABLE_DEPTH_TEST);
		pa.setStroke(strkClr);
		pa.setFill(fillClr);
		//main window drawing
		pa.rect(rectDim);
		//close box drawing
		drawMe(animTimeMod);			//call instance class's draw
		if(dispFlags[canDrawTraj]){
			drawTraj(animTimeMod);			
			if(dispFlags[showTrajEditCrc]){drawClkCircle();}
		}				//if this window accepts a drawn trajectory, then allow it to be displayed
		pa.hint(PConstants.ENABLE_DEPTH_TEST);
		pa.popStyle();pa.popMatrix();
	}
	//separating bar for menu
	protected void drawSepBar(double uiClkCoords2) {
		pa.pushMatrix();pa.pushStyle();
			pa.translate(0,uiClkCoords2 + (.5*SeqVisFFTOcean.txtSz),0);
			pa.fill(0,0,0,255);
			pa.strokeWeight(1.0f);
			pa.stroke(0,0,0,255);
			pa.line(0,0,pa.menuWidth,0);
		pa.popStyle();	pa.popMatrix();					
	}//
	
	protected void drawNotifications(){		
		//debug stuff
		pa.pushMatrix();				pa.pushStyle();
		pa.translate(rectDim[0]+20,rectDim[1]+rectDim[3]-70);
		dispMenuTxtLat("Drawing curve", pa.getClr((dispFlags[Base_DispWindow.drawingTraj] ? IRenderInterface.gui_Green : IRenderInterface.gui_Red),255), true);
		//pa.show(new myPoint(0,0,0),4, "Drawing curve",new myVector(10,15,0),(dispFlags[this.drawingTraj] ? pa.gui_Green : pa.gui_Red));
		//pa.translate(0,-30);
		dispMenuTxtLat("Editing curve", pa.getClr((dispFlags[Base_DispWindow.editingTraj] ? IRenderInterface.gui_Green : IRenderInterface.gui_Red),255), true);
		//pa.show(new myPoint(0,0,0),4, "Editing curve",new myVector(10,15,0),(dispFlags[this.editingTraj] ? pa.gui_Green : pa.gui_Red));
		pa.popStyle();pa.popMatrix();		
	}

	protected void drawClkCircle(){
		pa.pushMatrix();				pa.pushStyle();
		boolean doneDrawing = true;
		for(int i =0; i<editCrcFillClrs.length;++i){
			if(editCrcCurRads[i] <= 0){continue;}
			pa.setColorValFill(editCrcFillClrs[i],255);
			pa.noStroke();
			pa.circle(editCrcCtrs[i],editCrcCurRads[i]);
			editCrcCurRads[i] -= editCrcMods[i];
			doneDrawing = false;
		}
		if(doneDrawing){dispFlags[showTrajEditCrc] = false;}
		pa.popStyle();pa.popMatrix();		
	}
	
	protected boolean handleTrajClick(boolean keysToDrawClicked, myPoint mse){
		boolean mod = false;
		if(keysToDrawClicked){					//drawing curve with click+alt - drawing on canvas
			//pa.outStr2Scr("Current trajectory key IDX " + curTrajAraIDX);
			startBuildDrawObj();	
			mod = true;
			//
		} else {
		//	pa.outStr2Scr("Current trajectory key IDX edit " + curTrajAraIDX);
			this.tmpDrawnTraj = findTraj(mse);							//find closest trajectory to the mouse's click location
			
			if ((null != this.tmpDrawnTraj)  && (null != this.tmpDrawnTraj.drawnTraj)) {					//alt key not pressed means we're possibly editing a curve, if it exists and if we click within "sight" of it, or moving endpoints
				pa.outStr2Scr("Current trajectory ID " + tmpDrawnTraj.ID);
				mod = this.tmpDrawnTraj.startEditObj(mse);
			}
		}
		return mod;
	}//
	
	public myDrawnSmplTraj findTraj(myPoint mse){
		TreeMap<String,ArrayList<myDrawnSmplTraj>> tmpTreeMap = drwnTrajMap.get(this.curDrnTrajScrIDX);
		if((tmpTreeMap != null) && (tmpTreeMap.size() != 0)) {
			for(int i =0; i<tmpTreeMap.size(); ++i){
				ArrayList<myDrawnSmplTraj> tmpAra = tmpTreeMap.get(getTrajAraKeyStr(i));			
				if(null!=tmpAra){	for(int j =0; j<tmpAra.size();++j){	if(tmpAra.get(j).clickedMe(mse)){return tmpAra.get(j);}}}
			}	
		}
		return null;		
	}
	
	//stuff to do when shown/hidden
	public void setShow(boolean val){
		dispFlags[showIDX]=val;
		setClosedBox();
		if(!dispFlags[showIDX]){//not showing window
			closeMe();//specific instancing window implementation stuff
		} else {
			showMe();
		}
	}
	
	protected void toggleWindowState(){
		//pa.outStr2Scr("Attempting to close window : " + this.name);
		setShow(!dispFlags[showIDX]);
		pa.setFlags(pFlagIdx, dispFlags[showIDX]);		//value has been changed above by close box
	}
	
	protected boolean checkClsBox(int mouseX, int mouseY){
		boolean res = false;
		if(pa.ptInRange(mouseX, mouseY, closeBox[0], closeBox[1], closeBox[0]+closeBox[2], closeBox[1]+closeBox[3])){toggleWindowState(); res = true;}				
		return res;		
	}
	
	protected boolean checkUIButtons(int mouseX, int mouseY){
		boolean mod = false;
		int mx, my;
		//keep checking -see if clicked in UI buttons (flag-based buttons)
		for(int i = 0;i<privFlagBtns.length;++i){//rectDim[0], rectDim[1]  mseClickCrnr
			//mx = (int)(mouseX - rectDim[0]); my = (int)(mouseY - rectDim[1]);
			mx = (int)(mouseX - mseClickCrnr[0]); my = (int)(mouseY - mseClickCrnr[1]);
			mod = msePtInRect(mx, my, privFlagBtns[i]); 
			//pa.outStr2Scr("Handle mouse click in window : "+ ID + " : (" + mouseX+","+mouseY+") : "+mod + ": btn rect : "+privFlagBtns[i][0]+","+privFlagBtns[i][1]+","+privFlagBtns[i][2]+","+privFlagBtns[i][3]);
			if (mod){ 
				setPrivFlags(privModFlgIdxs[i],!privFlags[privModFlgIdxs[i]]); 
				return mod;
			}			
		}
		return mod;
	}//checkUIButtons
	
	
	protected myPoint getMsePoint(myPoint pt){
		return dispFlags[Base_DispWindow.is3DWin] ? getMouseLoc3D((int)pt.x, (int)pt.y) : pt;
	}

	protected myPoint getMsePoint(int mouseX, int mouseY){
		return dispFlags[Base_DispWindow.is3DWin] ? getMouseLoc3D(mouseX, mouseY) : pa.P(mouseX,mouseY,0);
	}
	public boolean handleMouseMove(int mouseX, int mouseY, myPoint mouseClickIn3D){
		if(!dispFlags[showIDX]){return false;}
		if((dispFlags[showIDX])&& (msePtInUIClckCoords(mouseX, mouseY))){//in clickable region for UI interaction
			for(int j=0; j<guiObjs_Numeric.length; ++j){if(guiObjs_Numeric[j].checkIn(mouseX, mouseY)){	msOvrObj=j;return true;	}}
		}			
		msOvrObj = -1;
		return false;
	}//handleMouseClick
	
	public boolean msePtInRect(int x, int y, float[] r){return ((x > r[0])&&(x <= r[0]+r[2])&&(y > r[1])&&(y <= r[1]+r[3]));}	
	public boolean msePtInUIClckCoords(int x, int y){return ((x > uiClkCoords[0])&&(x <= uiClkCoords[2])&&(y > uiClkCoords[1])&&(y <= uiClkCoords[3]));}	
	public boolean handleMouseClick(int mouseX, int mouseY, myPoint mouseClickIn3D, int mseBtn){
		boolean mod = false;
		//pa.outStr2Scr("ID :" +ID +" loc : ("  +mouseX +", " + mouseY + ") before mouse click check mod : "+ mod);
		if((dispFlags[showIDX])&& (msePtInUIClckCoords(mouseX, mouseY))){//in clickable region for UI interaction
			for(int j=0; j<guiObjs_Numeric.length; ++j){
				if(guiObjs_Numeric[j].checkIn(mouseX, mouseY)){	
					if(pa.flags[pa.shiftKeyPressed]){//allows for click-mod
						int mult = mseBtn * -2 + 1;	//+1 for left, -1 for right btn						
						guiObjs_Numeric[j].modVal(mult * pa.clickValModMult());
						dispFlags[uiObjMod] = true;
					} else {										//has drag mod
						msClkObj=j;
					}
					return true;	
				}
			}
		}			
		if(dispFlags[closeable]){mod = checkClsBox(mouseX, mouseY);}							//check if trying to close or open the window via click, if possible
		if(!dispFlags[showIDX]){return mod;}
		//pa.outStr2Scr("ID :" +ID +" before mouse click indiv mod "+ mod);
		if(!mod){mod = hndlMouseClickIndiv(mouseX, mouseY,mouseClickIn3D, mseBtn);}			//if nothing triggered yet, then specific instancing window implementation stuff		
		//pa.outStr2Scr("ID :" +ID +" after mouse click indiv mod "+ mod);
		if((!mod) && (msePtInRect(mouseX, mouseY, this.rectDim)) && (dispFlags[canDrawTraj])){ 
			myPoint pt =  getMsePoint(mouseX, mouseY);
			if(null==pt){return false;}
			mod = handleTrajClick(pa.flags[pa.altKeyPressed], pt);}			//click + alt for traj drawing : only allow drawing trajectory if it can be drawn and no other interaction has occurred
		return mod;
	}//handleMouseClick
	//vector for drag in 3D
	public boolean handleMouseDrag(int mouseX, int mouseY,int pmouseX, int pmouseY, myPoint mouseClickIn3D, myVector mseDragInWorld, int mseBtn){
		boolean mod = false;
		if(!dispFlags[showIDX]){return mod;}
		//any generic dragging stuff - need flag to determine if trajectory is being entered
		if(msClkObj!=-1){	guiObjs_Numeric[msClkObj].modVal((mouseX-pmouseX)+(mouseY-pmouseY)*-5.0f);dispFlags[uiObjMod] = true; return true;}		
		if(dispFlags[drawingTraj]){ 		//if drawing trajectory has started, then process it
			//pa.outStr2Scr("drawing traj");
			myPoint pt =  getMsePoint(mouseX, mouseY);
			if(null==pt){return false;}
			this.tmpDrawnTraj.addPoint(pt);
			mod = true;
		}else if(dispFlags[editingTraj]){		//if editing trajectory has started, then process it
			//pa.outStr2Scr("edit traj");	
			myPoint pt =  getMsePoint(mouseX, mouseY);
			if(null==pt){return false;}
			//mod = this.tmpDrawnTraj.editTraj(mouseX, mouseY,pmouseX, pmouseY,getMouseLoc3D(mouseX, mouseY),mseDragInWorld);
			mod = this.tmpDrawnTraj.editTraj(mouseX, mouseY,pmouseX, pmouseY,pt,mseDragInWorld);
		}
		else {
			if((!pa.ptInRange(mouseX, mouseY, rectDim[0], rectDim[1], rectDim[0]+rectDim[2], rectDim[1]+rectDim[3]))){return false;}	//if not drawing or editing a trajectory, force all dragging to be within window rectangle
			//pa.outStr2Scr("before handle indiv drag traj");
			mod = hndlMouseDragIndiv(mouseX, mouseY,pmouseX, pmouseY,mouseClickIn3D,mseDragInWorld, mseBtn);}		//handle specific, non-trajectory functionality for implementation of window
		return mod;
	}//handleMouseClick	
	
	public void handleMouseRelease(){
		if(!dispFlags[showIDX]){return;}
		if(dispFlags[uiObjMod]){//dispFlags[uiObjMod] = true;
			for(int i=0;i<guiObjs_Numeric.length;++i){if(guiObjs_Numeric[i].getFlags(myGUIObj.usedByWinsIDX)){setUIWinVals(i);}}
			dispFlags[uiObjMod] = false;
			msClkObj = -1;	
		}//some object was clicked - pass the values out to all windows
		if (dispFlags[editingTraj]){    this.tmpDrawnTraj.endEditObj();}    //this process assigns tmpDrawnTraj to owning window's traj array
		if (dispFlags[drawingTraj]){	this.tmpDrawnTraj.endDrawObj(getMsePoint(pa.Mouse()));}	//drawing curve
		msClkObj = -1;	
		hndlMouseRelIndiv();//specific instancing window implementation stuff
		this.tmpDrawnTraj = null;
	}//handleMouseRelease	
	
	public void endShiftKey(){
		if(!dispFlags[showIDX]){return;}
		//
		endShiftKey_Indiv();
	}
	public void endAltKey(){
		if(!dispFlags[showIDX]){return;}
		//if(dispFlags[drawingTraj]){drawnTrajAra[curDrnTrajScrIDX][curDrnTrajStaffIDX].endDrawObj();}	
		if(dispFlags[drawingTraj]){this.tmpDrawnTraj.endDrawObj(getMsePoint(pa.Mouse()));}	
		endAltKey_Indiv();
		this.tmpDrawnTraj = null;
	}	
	public void endCntlKey(){
		if(!dispFlags[showIDX]){return;}
		//
		endCntlKey_Indiv();
	}	
	
	//drawn trajectory stuff	
	public void startBuildDrawObj(){
		pa.flags[pa.drawing] = true;
		//drawnTrajAra[curDrnTrajScrIDX][curDrnTrajStaffIDX].startBuildTraj();
		tmpDrawnTraj= new myDrawnSmplTraj(pa,this,topOffY,trajFillClrCnst, trajStrkClrCnst, dispFlags[trajPointsAreFlat], !dispFlags[trajPointsAreFlat]);
		tmpDrawnTraj.startBuildTraj();
		dispFlags[drawingTraj] = true;
	}
	
	//finds closest point to p in sPts - put dist in d
	public final int findClosestPt(myPoint p, double[] d, myPoint[] _pts){
		int res = -1;
		double mindist = 99999999, _d;
		for(int i=0; i<_pts.length; ++i){_d = myPoint._dist(p,_pts[i]);if(_d < mindist){mindist = _d; d[0]=_d;res = i;}}	
		return res;
	}

	//initialize circle so that it will draw at location of edit
	public void setEditCueCircle(int idx,myPoint mse){
		dispFlags[showTrajEditCrc] = true;
		editCrcCtrs[idx].set(mse.x, mse.y, 0);
		editCrcCurRads[idx] = editCrcRads[idx];
	}
	
	public void rebuildAllDrawnTrajs(){
		for(TreeMap<String,ArrayList<myDrawnSmplTraj>> tmpTreeMap : drwnTrajMap.values()){
			if((tmpTreeMap != null) && (tmpTreeMap.size() != 0)) {
				for(int i =0; i<tmpTreeMap.size(); ++i){
					ArrayList<myDrawnSmplTraj> tmpAra = tmpTreeMap.get(getTrajAraKeyStr(i));			
					if(null!=tmpAra){	for(int j =0; j<tmpAra.size();++j){	tmpAra.get(j).rebuildDrawnTraj();}}
				}
			}	
		}			
	}
	protected void procInitOut(AudioOutput _out){		//set up so that initial note has no delay
		_out.pauseNotes();
		playNote(_out, 0, 1, 0.01f);			//playing a note to prevent hesitation before first note
		_out.resumeNotes();
	}
	
	//return summed outputs to simulation

	//playing trajectory note SortedMap<Integer, myNote> tmpNotes
	//TODO replace with reticle-driven play and stop of notes
	protected void addTrajNoteToPlay(SortedMap<Integer, myNote> tmpNotes){
		if((null == tmpNotes) || (tmpNotes.size() == 0)){return;}
		//pa.glblOut.pauseNotes();
		for(SortedMap.Entry<Integer, myNote> note : tmpNotes.entrySet()) { 	
			if(note.getValue().n.name == noteValType.rest){continue;}
			myNote _n = note.getValue();
			//pa.outStr2Scr("Play note : "+ _n.n.nameOct + " start : "+ _n.n.getStartPlayback() + " dur: " +  _n.n.getDurPlayback() * tempoDurRatio);
			playNote(pa.glblOut, _n.n.getStartPlayback(), _n.n.getDurPlayback() * tempoDurRatio, _n.n.freq);
			if(_n.flags[myNote.isChord]){
				for(Entry<String, myNote> cnote : ((myChord)(_n)).cnotes.entrySet()){
					myNote chrdN = cnote.getValue();
					if(_n.ID != chrdN.ID){
						//pa.outStr2Scr("Play note of chord : "+ chrdN.n.nameOct + " start : "+ chrdN.n.getStartPlayback() + " dur: " +  chrdN.n.getDurPlayback() * tempoDurRatio);
						playNote(pa.glblOut, chrdN.n.getStartPlayback(), chrdN.n.getDurPlayback() * tempoDurRatio, chrdN.n.freq);
					}
				}
			}
		}
		//instrNoteOut.resumeNotes();
		this.dispFlags[Base_DispWindow.notesLoaded] = true;
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
//		//pa.outStr2Scr("ID :" + ID+" play() : startTime : " + pa.glblStartPlayTime + " last play time : " + pa.glblLastPlayTime + " modAmt Sec : " + String.format("%.4f", modAmtSec) + " frate :"+ String.format("%.4f", pa.frameRate));
//		pbe.play(modAmtSec);
//		//global play handling - move and draw reticle line		
		this.dispFlags[Base_DispWindow.notesLoaded] = false;
		//pa.outStr2Scr("start playing : \n");
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
		//pa.outStr2Scr("ID :" + ID+" play() : startTime : " + pa.glblStartPlayTime + " last play time : " + pa.glblLastPlayTime + " modAmt Sec : " + String.format("%.4f", modAmtSec) + " frate :"+ String.format("%.4f", pa.frameRate));
		if(dispFlags[notesLoaded]){pbe.play(modAmtSec);}
	}
	
	//debug data to display on screen
	//get string array for onscreen display of debug info for each object
	public String[] getDebugData(){
		ArrayList<String> res = new ArrayList<String>();
		List<String>tmp;
		for(int j = 0; j<guiObjs_Numeric.length; j++){tmp = Arrays.asList(guiObjs_Numeric[j].getStrData());res.addAll(tmp);}
		return res.toArray(new String[0]);	
	}
	
	//set colors of the trajectory for this window
	public void setTrajColors(int _tfc, int _tsc){trajFillClrCnst = _tfc;trajStrkClrCnst = _tsc;initTmpTrajStuff(dispFlags[trajPointsAreFlat]);}
	//get key used to access arrays in traj array
	protected String getTrajAraKeyStr(int i){return scoreStaffNames[i];}
	protected int getTrajAraIDXVal(String str){for(int i=0; i<scoreStaffNames.length;++i){if(scoreStaffNames[i].equals(str)){return i;}}return -1; }

	//add trajectory to appropriately keyed current trajectory ara in treemap	
	protected void processTrajectory(myDrawnSmplTraj drawnNoteTraj){
		TreeMap<String,ArrayList<myDrawnSmplTraj>> tmpTreeMap = drwnTrajMap.get(this.curDrnTrajScrIDX);
		ArrayList<myDrawnSmplTraj> tmpAra;
		if(curTrajAraIDX != -1){		//make sure some trajectory/staff has been selected
			if((tmpTreeMap != null) && (tmpTreeMap.size() != 0) ) {
				tmpAra = tmpTreeMap.get(getTrajAraKeyStr(curTrajAraIDX));			
				if((null==tmpAra) || (pa.flags[pa.clearStaffNewTraj])){		tmpAra = new ArrayList<myDrawnSmplTraj>();}
				//lastTrajIDX = tmpAra.size();
				tmpAra.add(drawnNoteTraj); 				
			} else {//empty or null tmpTreeMap - tmpAra doesn't exist
				tmpAra = new ArrayList<myDrawnSmplTraj>();
				tmpAra.add(drawnNoteTraj);
				//lastTrajIDX = tmpAra.size();
				if(tmpTreeMap == null) {tmpTreeMap = new TreeMap<String,ArrayList<myDrawnSmplTraj>>();} 
			}
			tmpTreeMap.put(getTrajAraKeyStr(curTrajAraIDX), tmpAra);
			processTrajIndiv(drawnNoteTraj);
		}	
		//individual traj processing
	}
	
	public void clearAllTrajectories(){//int instrIdx){
		TreeMap<String,ArrayList<myDrawnSmplTraj>> tmpTreeMap = drwnTrajMap.get(this.curDrnTrajScrIDX);
		ArrayList<myDrawnSmplTraj> tmpAra;
		if(curTrajAraIDX != -1){		//make sure some trajectory/staff has been selected
			if((tmpTreeMap != null) && (tmpTreeMap.size() != 0) ) {
				tmpTreeMap.put(getTrajAraKeyStr(curTrajAraIDX), new ArrayList<myDrawnSmplTraj>());
			}
		}	
	}//clearAllTrajectories
	
	//add another screen to this window - need to handle specific trajectories - always remake traj structure
	public void addSubScreenToWin(int newWinKey){
		modTrajStructs(newWinKey, "",false);
		
		addSScrToWinIndiv(newWinKey);
	}
	public void addTrajToSubScreen(int subScrKey, String newTrajKey){
		modTrajStructs(subScrKey, newTrajKey,false);
		
		addTrajToScrIndiv(subScrKey, newTrajKey);
	}
	
	public void delSubScreenToWin(int delWinKey){
		modTrajStructs(delWinKey, "",true);
		
		delSScrToWinIndiv(delWinKey);
	}
	public void delTrajToSubScreen(int subScrKey, String newTrajKey){
		modTrajStructs(subScrKey, newTrajKey,true);
		
		
		delTrajToScrIndiv(subScrKey,newTrajKey);
	}
	//get notes from sequencer/sphere ui for ocean visualization
	protected abstract SortedMap<Integer, myNote> getNotesNow();
	
	protected abstract void setScoreInstrValsIndiv();
	protected abstract void addSScrToWinIndiv(int newWinKey);
	protected abstract void addTrajToScrIndiv(int subScrKey, String newTrajKey);
	protected abstract void delSScrToWinIndiv(int idx);
	protected abstract void delTrajToScrIndiv(int subScrKey, String newTrajKey);
	
	protected abstract myPoint getMouseLoc3D(int mouseX, int mouseY);
	
	//implementing class' necessary functions - implement for each individual window
	protected abstract boolean hndlMouseMoveIndiv(int mouseX, int mouseY, myPoint mseClckInWorld);
	protected abstract boolean hndlMouseClickIndiv(int mouseX, int mouseY, myPoint mseClckInWorld, int mseBtn);
	protected abstract boolean hndlMouseDragIndiv(int mouseX, int mouseY,int pmouseX, int pmouseY, myPoint mouseClickIn3D, myVector mseDragInWorld, int mseBtn);
	protected abstract void snapMouseLocs(int oldMouseX, int oldMouseY, int[] newMouseLoc);	
	
	protected abstract void hndlMouseRelIndiv();
	
	protected abstract void endShiftKey_Indiv();
	protected abstract void endAltKey_Indiv();
	protected abstract void endCntlKey_Indiv();
	
	//ui init routines
	protected abstract void setupGUIObjsAras();	
	protected abstract void setUIWinVals(int UIidx);
	protected abstract String getUIListValStr(int UIidx, int validx);
	protected abstract void processTrajIndiv(myDrawnSmplTraj drawnTraj);
	
	public abstract void clickDebug(int btnNum);
	
	protected abstract void initMe();
	//init xtra ui objects on a per-window basis
	protected abstract void initXtraUIObjsIndiv();
	
	protected abstract void resizeMe(float scale);	
	protected abstract void showMe();
	protected abstract void closeMe();	
	protected abstract void playMe();
	protected abstract void stopMe();
	protected abstract void modMySongLoc(float modAmt);
	protected abstract void drawMe(float animTimeMod);	
	
	//set current key signature, at time passed - for score, set it at nearest measure boundary
	protected abstract void setGlobalKeySigValIndiv(int idx, float time);	
	//set time signature at time passed - for score, set it at nearest measure boundary
	protected abstract void setGlobalTimeSigValIndiv(int tsnum, int tsdenom, noteDurType _beatNoteType, float time);	
	//set time signature at time passed - for score, set it at nearest measure boundary
	protected abstract void setGlobalTempoValIndiv(float tempo, float time);
	
	//set current key signature, at time passed - for score, set it at nearest measure boundary
	protected abstract void setLocalKeySigValIndiv(myKeySig lclKeySig, ArrayList<noteValType> lclKeyNotesAra, float time);	
	//set time signature at time passed - for score, set it at nearest measure boundary
	protected abstract void setLocalTimeSigValIndiv(int tsnum, int tsdenom, noteDurType _beatNoteType, float time);	
	//set time signature at time passed - for score, set it at nearest measure boundary
	protected abstract void setLocalTempoValIndiv(float tempo, float time);

	
	public String toString(){
		String res = "Window : "+name+" ID: "+ID+" Fill :("+fillClr[0]+","+fillClr[1]+","+fillClr[2]+","+fillClr[3]+
				") | Stroke :("+fillClr[0]+","+fillClr[1]+","+fillClr[2]+","+fillClr[3]+") | Rect : ("+
				String.format("%.2f",rectDim[0])+","+String.format("%.2f",rectDim[1])+","+String.format("%.2f",rectDim[2])+","+String.format("%.2f",rectDim[3])+")\n";	
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

