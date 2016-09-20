package SphrSeqFFTVisPKG;

import java.util.*;
import java.util.Map.Entry;

import ddf.minim.AudioOutput;
import processing.core.PApplet;
import processing.core.PConstants;

//abstract class to hold base code for a menu/display window (2D for gui, etc), to handle displaying and controlling the window, and calling the implementing class for the specifics
public abstract class myDispWindow {
	public SeqVisFFTOcean pa;
	public static int winCnt = 0;
	public int ID;
	
	public myPlaybackEngine pbe;
	
	public String name, winText;	
	
	//global sequence values - static
	public static myKeySig glblKeySig;
	public static float glblTempo;
	public static myTimeSig glblTimeSig;
	public static durType glblBeatNote;
	public static ArrayList<nValType> glblKeyNotesAra;					//notes in the current key -- used to force notes to key

		
	public int[] fillClr, strkClr;
	public int trajFillClrCnst, trajStrkClrCnst;
	public float[] rectDim, closeBox, rectDimClosed, mseClickCrnr;	
	public static final float gridYMult = 1.0f/67.0f, gridXMult = .5625f * gridYMult;
	public static final float xOff = 20 , yOff = 20, btnLblYOff = 2 * yOff, rowStYOff = yOff*.15f;
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
	public static final int[] editCrcFillClrs = new int[] {SeqVisFFTOcean.gui_FaintMagenta, SeqVisFFTOcean.gui_FaintGreen};			
	public static final float[] editCrcRads = new float[] {20.0f,40.0f};			
	public static final float[] editCrcMods = new float[] {1f,2f};			
	public final myPoint[] editCrcCtrs = new myPoint[] {new myPoint(0,0,0),new myPoint(0,0,0)};			
	public float[] editCrcCurRads = new float[] {0,0};	
	
	//UI objects in this window
	//GUI Objects
	public myGUIObj[] guiObjs;	
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
	public myDrawnNoteTraj tmpDrawnTraj;						//currently drawn curve and all handling code - send to instanced owning screen

	//all trajectories in this particular display window - String key is unique identifier - like staff name or instrument name
	public TreeMap<Integer,TreeMap<String,ArrayList<myDrawnNoteTraj>>> drwnTrajMap;				
	
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
	public final durType[] noteValTypes = new durType[]{durType.Whole,durType.Half,durType.Quarter,durType.Eighth,durType.Sixteenth,durType.Thirtisecond};
	
	//score - related values
	//protected myScore score;									//score being displayed
	protected float[] scoreRect;								//dimensions of score area
	protected String[] scoreStaffNames;					//array of each score staff name
	protected TreeMap<String,myInstr> instrs;	
	//to control how much is shown in the window - if measures extend off the screen
	public myScrollBars[] scbrs;
	
	//individual instrument note output - replaces scrollNoteOut
	//public AudioOutput[] instrNoteOut;
	public float tempoDurRatio = 1.0f;
	
	public myDispWindow(SeqVisFFTOcean _p, String _n, int _flagIdx, int[] fc,  int[] sc, float[] rd, float[] rdClosed, String _winTxt, boolean _canDrawTraj) {
		pa=_p;
		pbe = new myPlaybackEngine(pa, this, new int[]{255,0,255,255}, new int[]{0,255,255,255}, new float[]{0,pa.height});
		ID = winCnt++;
		name = _n;
		pFlagIdx = _flagIdx;
		fillClr = new int[4];	strkClr = new int[4];	rectDim = new float[4];	rectDimClosed = new float[4]; closeBox = new float[4]; uiClkCoords = new double[4];
		for(int i =0;i<4;++i){fillClr[i] = fc[i];strkClr[i]=sc[i];rectDim[i]=rd[i];rectDimClosed[i]=rdClosed[i];}		
				
		winText = _winTxt;
		trajFillClrCnst = SeqVisFFTOcean.gui_Black;		//override this in the ctor of the instancing window class
		trajStrkClrCnst = SeqVisFFTOcean.gui_Black;
		
		msClkObj = -1;//	lastTrajIDX = -1; //lastPBEQueryPlayTime = 0;	
		msOvrObj = -1;
		stAnimTime=0;
		lastAnimTime=0;
	}	
	
	protected void initTmpTrajStuff(boolean _trajIsFlat){
//		vsblStLoc = new float[numSubScrInWin];
//		seqVisStTime = new int[numSubScrInWin];
		//drawnTrajAra[curDrnTrajScrIDX][curDrnTrajStaffIDX] init 2 and 10
		tmpDrawnTraj= new myDrawnNoteTraj(pa,this,topOffY,trajFillClrCnst, trajStrkClrCnst, _trajIsFlat, !_trajIsFlat);
		curDrnTrajScrIDX = 0;
//		for(int i =0;i<numSubScrInWin;++i){//
//			vsblStLoc[i] = 0;
//			seqVisStTime[i] = 0;
//		}
	}	
	//initialize traj-specific stuff for this window - only should be called once a song has been made, to have the # of trajectories available
	protected void initTrajStructs(){
		drwnTrajMap = new TreeMap<Integer,TreeMap<String,ArrayList<myDrawnNoteTraj>>>();
		TreeMap<String,ArrayList<myDrawnNoteTraj>> tmpTrajMap;
		for(int scr =0;scr<numSubScrInWin; ++scr){
			tmpTrajMap = new TreeMap<String,ArrayList<myDrawnNoteTraj>>();
			for(int traj =0; traj<numTrajInSubScr[scr]; ++traj){
				tmpTrajMap.put(getTrajAraKeyStr(traj), new ArrayList<myDrawnNoteTraj>());			
			}	
			drwnTrajMap.put(scr, tmpTrajMap);
		}		
	}	
	//build UI clickable region
	protected void initUIClickCoords(double x1, double y1, double x2, double y2){
		//pa.outStr2Scr("initUIClickCoords 4 arg in :"+ name+ " ID : " + ID + ": (" + x1+","+y1+","+x2+","+y2 +")");
		uiClkCoords[0] = x1;uiClkCoords[1] = y1;uiClkCoords[2] = x2; uiClkCoords[3] = y2;}
	protected void initUIClickCoords(double[] cpy){	uiClkCoords[0] = cpy[0];uiClkCoords[1] = cpy[1];uiClkCoords[2] = cpy[2]; uiClkCoords[3] = cpy[3];}

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
		initUIClickCoords(menuUIClkCoords[0],menuUIClkCoords[3],menuUIClkCoords[2],menuUIClkCoords[3]);				//final value will be modified when UI objects are made
		//initialize beginning locations for playback reticle - override this in initMe, and modify it in code for scrollbars
		vsblStLoc = new float[]{0,0};
		seqVisStTime = new int[] {0,0};
	}
	
	//calculate button length
	private static final float ltrLen = 6.2f;private static final int btnStep = 6;
	private float calcBtnLength(String tStr, String fStr){return btnStep * ((int)((PApplet.max(tStr.length(),fStr.length()) + 4) * ltrLen)/btnStep);}
	//set up child class button rectangles TODO
	//yDisp is displacement for button to be drawn
	protected void initPrivBtnRects(float yDisp, int numBtns){
		//pa.outStr2Scr("initPrivBtnRects in :"+ name + "st value for uiClkCoords[3]");
		privFlagBtns = new float[numBtns][];
		this.uiClkCoords[3] += yOff;
		float oldBtnLen = 0;
		for(int i=0; i<numBtns; ++i){						//clickable button regions - as rect,so x,y,w,h - need to be in terms of sidebar menu 
			float btnLen = calcBtnLength(truePrivFlagNames[i].trim(),falsePrivFlagNames[i].trim());
			if(i%2 == 0){//even btns always on a new line
				privFlagBtns[i]= new float[] {(float)(uiClkCoords[0]-xOff), (float) uiClkCoords[3], btnLen, yOff };
			} else {	//odd button
				if ((btnLen + oldBtnLen) > .95f * pa.menuWidth){	//odd button, but too long -> new line		
					this.uiClkCoords[3] += yOff;
					oldBtnLen = 0;
				}
				privFlagBtns[i]= new float[] {(float)(uiClkCoords[0]-xOff)+oldBtnLen, (float) uiClkCoords[3], btnLen, yOff };
				this.uiClkCoords[3] += yOff;//always new line after odd button
			}
			oldBtnLen = btnLen;
		};
		this.uiClkCoords[3] += yOff;
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
	
	public void initThisWin(boolean _canDrawTraj, boolean _trajIsFlat){
//		float stY = rectDim[1]+ .85f*rectDim[3];
//		initUIClickCoords(rectDim[0] + .1 * rectDim[2],stY,rectDim[0] + rectDim[2],stY + yOff);		//only for first window (sidebar menu) - all subsequent get over-written in iniUIBox
		initTmpTrajStuff(_trajIsFlat);	
		initFlags();	
		dispFlags[canDrawTraj] = _canDrawTraj;
		dispFlags[trajPointsAreFlat] = _trajIsFlat;
		dispFlags[closeable] = true;
		setGlobalTimeSigVal(4,4, pa.getDurTypeForNote(4));	
		initMe();
		setupGUIObjsAras();
		//record final y value for UI Objects
		initAllPrivBtns();
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
				TreeMap<String,ArrayList<myDrawnNoteTraj>> tmpTrajMap = drwnTrajMap.remove(scrKey);
				if(null != tmpTrajMap){			pa.outStr2Scr("Screen trajectory map removed for scr : " + scrKey);				modMthd = 0;}
				else {							pa.outStr2Scr("Error : Screen trajectory map not found for scr : " + scrKey); 	modMthd = -1; }
			} else {												//delete a submap within a screen
				modMthd = 2;					//modifying existing map at this location
				TreeMap<String,ArrayList<myDrawnNoteTraj>> tmpTrajMap = drwnTrajMap.get(scrKey);
				if(null == tmpTrajMap){pa.outStr2Scr("Error : Screen trajectory map not found for scr : " + scrKey + " when trying to remove arraylist : "+trajAraKey); modMthd = -1;}
				else { 
					ArrayList<myDrawnNoteTraj> tmpTrajAra = drwnTrajMap.get(scrKey).remove(trajAraKey);modMthd = 2;
					if(null == tmpTrajAra){pa.outStr2Scr("Error : attempting to remove a trajectory array from a screen but trajAra not found. scr : " + scrKey + " | trajAraKey : "+trajAraKey);modMthd = -1; }
				}
			}			 
		} else {													//add
			TreeMap<String,ArrayList<myDrawnNoteTraj>> tmpTrajMap = drwnTrajMap.get(scrKey);
			if((trajAraKey == null) || (trajAraKey == "") ){		//add map of maps - added a new screen				
				if(null != tmpTrajMap){pa.outStr2Scr("Error : attempting to add a new drwnTrajMap where one exists. scr : " + scrKey);modMthd = -1; }
				else {tmpTrajMap = new TreeMap<String,ArrayList<myDrawnNoteTraj>>();	drwnTrajMap.put(scrKey, tmpTrajMap);modMthd = 1;}
			} else {												//add new map of trajs to existing screen's map
				ArrayList<myDrawnNoteTraj> tmpTrajAra = drwnTrajMap.get(scrKey).get(trajAraKey);	
				if(null == tmpTrajMap){pa.outStr2Scr("Error : attempting to add a new trajectory array to a screen that doesn't exist. scr : " + scrKey + " | trajAraKey : "+trajAraKey); modMthd = -1; }
				else if(null != tmpTrajAra){pa.outStr2Scr("Error : attempting to add a new trajectory array to a screen where one already exists. scr : " + scrKey + " | trajAraKey : "+trajAraKey);modMthd = -1; }
				else {	tmpTrajAra = new ArrayList<myDrawnNoteTraj>();			tmpTrajMap.put(trajAraKey, tmpTrajAra);	drwnTrajMap.put(scrKey, tmpTrajMap);modMthd = 2;}
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
		TreeMap<String,ArrayList<myDrawnNoteTraj>> tmpTreeMap = drwnTrajMap.get(this.curDrnTrajScrIDX);
		if((tmpTreeMap != null) && (tmpTreeMap.size() != 0)) {
			for(int i =0; i<tmpTreeMap.size(); ++i){
				ArrayList<myDrawnNoteTraj> tmpAra = tmpTreeMap.get(getTrajAraKeyStr(i));			
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
//		for(int i=0;i<curUIVals.length;++i){	curUIVals[i] = guiObjs[i].getVal();	}
//		
//		uiClkCoords[1]=calcDBLOffsetScale(uiClkCoords[1],scaleY,topOffY);
//		uiClkCoords[3]=calcDBLOffsetScale(uiClkCoords[3],scaleY,topOffY);
//		//initUIClickCoords(uiClkCoords[0],uiClkCoords[1],uiClkCoords[2],uiClkCoords[3]);
//		if(0!=numGUIObjs){
//			buildGUIObjs(guiObjNames,curUIVals,guiMinMaxModVals,guiBoolVals,new double[]{xOff,yOff});
//		}
//	}
	
	//build myGUIObj objects for interaction - call from setupMenuClkRegions of window, 
	//uiClkCoords needs to be derived before this is called by child class - maxY val(for vertical stack) or maxX val(for horizontal stack) will be derived here
	protected void buildGUIObjs(String[] guiObjNames, double[] guiStVals, double[][] guiMinMaxModVals, boolean[][] guiBoolVals, double[] off){
		myGUIObj tmp; 
//		if(dispFlags[uiObjsAreVert]){		//vertical stack of UI components - clickable region x is unchanged, y changes with # of objects
			double stClkY = uiClkCoords[1];
			for(int i =0; i< guiObjs.length; ++i){
				guiObjs[i] = buildGUIObj(i,guiObjNames[i],guiStVals[i], guiMinMaxModVals[i], guiBoolVals[i], new double[]{uiClkCoords[0], stClkY, uiClkCoords[2], stClkY+yOff},off);
				stClkY += yOff;
			}
			uiClkCoords[3] = stClkY;	
//		} else {			//horizontal row of UI components - clickable region y is unchanged, x changes with # of objects
//			double stClkX = uiClkCoords[0];
//			double UICompWidth;
//			for(int i =0; i< guiObjs.length; ++i){
//				UICompWidth = (uiWidthMult + (guiBoolVals[i][1] ? 1 : 0)) * guiObjNames[i].length();
//				guiObjs[i] = buildGUIObj(i,guiObjNames[i],guiStVals[i], guiMinMaxModVals[i], guiBoolVals[i], new double[]{stClkX, uiClkCoords[1], stClkX+UICompWidth , uiClkCoords[3]},off);
//				stClkX += UICompWidth;
//			}
//			uiClkCoords[2] = stClkX;	
//		}
	}//
	protected myGUIObj buildGUIObj(int i, String guiObjName, double guiStVal, double[] guiMinMaxModVals, boolean[] guiBoolVals, double[] xyDims, double[] off){
		myGUIObj tmp;
		tmp = new myGUIObj(pa, this,i, guiObjName, xyDims[0], xyDims[1], xyDims[2], xyDims[3], guiMinMaxModVals, guiStVal, guiBoolVals, off);		
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
	public void setScoreInstrVals(TreeMap<String,myInstr> _instrs, String[] _scoreStaffNames){
		scoreStaffNames = new String[_scoreStaffNames.length];
		drawTrajBoxFillClrs = new int[scoreStaffNames.length][4];
		for(int i =0; i<scoreStaffNames.length; ++i){	
			scoreStaffNames[i] = _scoreStaffNames[i];
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
	public void setGlobalTimeSigVal(int tsnum, int tsdenom, durType _beatNoteType){
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
	protected void showKeyPt(myPoint a, String s, float rad){	pa.show(a,rad, s, new myVector(10,-5,0), SeqVisFFTOcean.gui_Cyan, dispFlags[trajPointsAreFlat]);	}	
	//draw a series of strings in a column
	protected void dispMenuTxtLat(String txt, int[] clrAra, boolean showSphere){
		pa.setFill(clrAra, 255); 
		pa.translate(xOff*.5f,yOff*.5f);
		if(showSphere){pa.setStroke(clrAra, 255);		pa.sphere(5);	} 
		else {	pa.noStroke();		}
		pa.translate(-xOff*.5f,yOff*.5f);
		pa.text(""+txt,xOff,-yOff*.25f);	
	}
	protected void dispBoolStFlag(String txt, int[] clrAra, boolean state, float stMult){
		if(state){
			pa.setFill(clrAra, 255); 
			pa.setStroke(clrAra, 255);
		} else {
			pa.setColorValFill(pa.gui_DarkGray); 
			pa.noStroke();	
		}
		pa.sphere(5);
		//pa.text(""+txt,-xOff,yOff*.8f);	
		pa.text(""+txt,stMult*txt.length(),yOff*.8f);	
	}
	
	//draw a series of strings in a row
	protected void dispBttnAtLoc(String txt, float[] loc, int[] clrAra){
		pa.setColorValFill(SeqVisFFTOcean.gui_White);
		pa.setColorValStroke(SeqVisFFTOcean.gui_Black);
		pa.rect(loc);		
		pa.setColorValFill(SeqVisFFTOcean.gui_Black);
		//pa.translate(-xOff*.5f,-yOff*.5f);
		pa.text(""+txt,loc[0] + (txt.length() * .3f),loc[1]+loc[3]*.75f);
		//pa.translate(width, 0);
	}
	
	protected void drawTraj(float animTimeMod){
		pa.pushMatrix();pa.pushStyle();	
		if(null != tmpDrawnTraj){tmpDrawnTraj.drawMe(animTimeMod);}
		TreeMap<String,ArrayList<myDrawnNoteTraj>> tmpTreeMap = drwnTrajMap.get(this.curDrnTrajScrIDX);
		if((tmpTreeMap != null) && (tmpTreeMap.size() != 0)) {
			for(int i =0; i<tmpTreeMap.size(); ++i){
				ArrayList<myDrawnNoteTraj> tmpAra = tmpTreeMap.get(getTrajAraKeyStr(i));			
				if(null!=tmpAra){	for(int j =0; j<tmpAra.size();++j){tmpAra.get(j).drawMe(animTimeMod);}}
			}	
		}
		pa.popStyle();pa.popMatrix();		
	}
	
	//draw ui objects
	public void drawGUIObjs(){	
		pa.pushMatrix();pa.pushStyle();	
		for(int i =0; i<guiObjs.length; ++i){guiObjs[i].draw();}
		pa.popStyle();pa.popMatrix();
	}
	
	//draw all boolean-based buttons for this window
	public void drawClickableBooleans() {	
		pa.pushMatrix();pa.pushStyle();	
		pa.setColorValFill(pa.gui_Black);
		for(int i =0; i<privModFlgIdxs.length; ++i){//prlFlagRects dispBttnAtLoc(String txt, float[] loc, int[] clrAra)
			if(privFlags[privModFlgIdxs[i]] ){									dispBttnAtLoc(truePrivFlagNames[i],privFlagBtns[i],privFlagColors[i]);			}
			else {	if(truePrivFlagNames[i].equals(falsePrivFlagNames[i])) {	dispBttnAtLoc(truePrivFlagNames[i],privFlagBtns[i],new int[]{180,180,180});}	
					else {														dispBttnAtLoc(falsePrivFlagNames[i],privFlagBtns[i],new int[]{0,255-privFlagColors[i][1],255-privFlagColors[i][2]});}		
			}
		}		
		pa.popStyle();pa.popMatrix();
	}//drawClickableBooleans	
	
	public abstract void initAllPrivBtns();
	
	//draw box to hide window
	protected void drawMouseBox(){
	    pa.setColorValFill(dispFlags[showIDX] ? SeqVisFFTOcean.gui_LightGreen : SeqVisFFTOcean.gui_DarkRed);
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
		if(this.dispFlags[myDispWindow.is3DWin]){	draw3D(trans,animTimeMod);	} else {	draw2D(trans,animTimeMod);	}
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
		pa.outStr2Scr("myDispWindow.drawTraj3D() : I should be overridden in 3d instancing class", true);
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
	
	protected void drawNotifications(){		
		//debug stuff
		pa.pushMatrix();				pa.pushStyle();
		pa.translate(rectDim[0]+20,rectDim[1]+rectDim[3]-70);
		dispMenuTxtLat("Drawing curve", pa.getClr((dispFlags[myDispWindow.drawingTraj] ? SeqVisFFTOcean.gui_Green : SeqVisFFTOcean.gui_Red)), true);
		//pa.show(new myPoint(0,0,0),4, "Drawing curve",new myVector(10,15,0),(dispFlags[this.drawingTraj] ? pa.gui_Green : pa.gui_Red));
		//pa.translate(0,-30);
		dispMenuTxtLat("Editing curve", pa.getClr((dispFlags[myDispWindow.editingTraj] ? SeqVisFFTOcean.gui_Green : SeqVisFFTOcean.gui_Red)), true);
		//pa.show(new myPoint(0,0,0),4, "Editing curve",new myVector(10,15,0),(dispFlags[this.editingTraj] ? pa.gui_Green : pa.gui_Red));
		pa.popStyle();pa.popMatrix();		
	}

	protected void drawClkCircle(){
		pa.pushMatrix();				pa.pushStyle();
		boolean doneDrawing = true;
		for(int i =0; i<editCrcFillClrs.length;++i){
			if(editCrcCurRads[i] <= 0){continue;}
			pa.setColorValFill(editCrcFillClrs[i]);
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
	
	public myDrawnNoteTraj findTraj(myPoint mse){
		TreeMap<String,ArrayList<myDrawnNoteTraj>> tmpTreeMap = drwnTrajMap.get(this.curDrnTrajScrIDX);
		if((tmpTreeMap != null) && (tmpTreeMap.size() != 0)) {
			for(int i =0; i<tmpTreeMap.size(); ++i){
				ArrayList<myDrawnNoteTraj> tmpAra = tmpTreeMap.get(getTrajAraKeyStr(i));			
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
		return dispFlags[myDispWindow.is3DWin] ? getMouseLoc3D((int)pt.x, (int)pt.y) : pt;
	}

	protected myPoint getMsePoint(int mouseX, int mouseY){
		return dispFlags[myDispWindow.is3DWin] ? getMouseLoc3D(mouseX, mouseY) : pa.P(mouseX,mouseY,0);
	}
	public boolean handleMouseMove(int mouseX, int mouseY, myPoint mouseClickIn3D){
		if(!dispFlags[showIDX]){return false;}
		if((dispFlags[showIDX])&& (msePtInUIRect(mouseX, mouseY))){//in clickable region for UI interaction
			for(int j=0; j<guiObjs.length; ++j){if(guiObjs[j].checkIn(mouseX, mouseY)){	msOvrObj=j;return true;	}}
		}			
		msOvrObj = -1;
		return false;
	}//handleMouseClick
	
	public boolean msePtInRect(int x, int y, float[] r){return ((x > r[0])&&(x <= r[0]+r[2])&&(y > r[1])&&(y <= r[1]+r[3]));}	
	public boolean msePtInUIRect(int x, int y){return ((x > uiClkCoords[0])&&(x <= uiClkCoords[2])&&(y > uiClkCoords[1])&&(y <= uiClkCoords[3]));}	
	public boolean handleMouseClick(int mouseX, int mouseY, myPoint mouseClickIn3D, int mseBtn){
		boolean mod = false;
		if((dispFlags[showIDX])&& (msePtInUIRect(mouseX, mouseY))){//in clickable region for UI interaction
			for(int j=0; j<guiObjs.length; ++j){
				if(guiObjs[j].checkIn(mouseX, mouseY)){	
					if(pa.flags[pa.shiftKeyPressed]){//allows for click-mod
						int mult = mseBtn * -2 + 1;	//+1 for left, -1 for right btn						
						guiObjs[j].modVal(mult * pa.clickValModMult());
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
		if(msClkObj!=-1){	guiObjs[msClkObj].modVal((mouseX-pmouseX)+(mouseY-pmouseY)*-5.0f);dispFlags[uiObjMod] = true; return true;}		
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
			for(int i=0;i<guiObjs.length;++i){if(guiObjs[i].getFlags(myGUIObj.usedByWinsIDX)){setUIWinVals(i);}}
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
		endShiftKeyI();
	}
	public void endAltKey(){
		if(!dispFlags[showIDX]){return;}
		//if(dispFlags[drawingTraj]){drawnTrajAra[curDrnTrajScrIDX][curDrnTrajStaffIDX].endDrawObj();}	
		if(dispFlags[drawingTraj]){this.tmpDrawnTraj.endDrawObj(getMsePoint(pa.Mouse()));}	
		endAltKeyI();
		this.tmpDrawnTraj = null;
	}	
	public void endCntlKey(){
		if(!dispFlags[showIDX]){return;}
		//
		endCntlKeyI();
	}	
	
	//drawn trajectory stuff	
	public void startBuildDrawObj(){
		pa.flags[pa.drawing] = true;
		//drawnTrajAra[curDrnTrajScrIDX][curDrnTrajStaffIDX].startBuildTraj();
		tmpDrawnTraj= new myDrawnNoteTraj(pa,this,topOffY,trajFillClrCnst, trajStrkClrCnst, dispFlags[trajPointsAreFlat], !dispFlags[trajPointsAreFlat]);
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
		for(TreeMap<String,ArrayList<myDrawnNoteTraj>> tmpTreeMap : drwnTrajMap.values()){
			if((tmpTreeMap != null) && (tmpTreeMap.size() != 0)) {
				for(int i =0; i<tmpTreeMap.size(); ++i){
					ArrayList<myDrawnNoteTraj> tmpAra = tmpTreeMap.get(getTrajAraKeyStr(i));			
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
	//static int plIterStatic = 0, stIterStatic;
	//place holder to stop notes
	protected void addSphereNoteToStopNow(myInstr instr, SortedMap<Integer,ArrayList<myNote>> tmpNotes){
		//pa.outStr2Scr("Iter : " + plIterStatic++ + " Play ");
		int retCode = instr.addSphNotesToStop(tmpNotes);
		
	}
	//static int iterStatic = 0;
	protected void addSphereNoteToPlayNow(myInstr instr, SortedMap<Integer, myNote> tmpNotes, float durMult, int stTime){
		//pa.outStr2Scr("Iter : " + stIterStatic++ + " Stop ");
		int retCode = instr.addSphNotesToPlay(tmpNotes,stTime);
		this.dispFlags[myDispWindow.notesLoaded] = true;
	}//addTrajNoteToPlay
//	static int iterStatic = 0;
//	protected void addSphereNoteToPlayNow(SortedMap<Integer, myNote> tmpNotes, float durMult){
//		if((null == tmpNotes) || (tmpNotes.size() == 0)){return;}
//		float noteDur, noteSt;
//		for(SortedMap.Entry<Integer, myNote> note : tmpNotes.entrySet()) { 	
//			if(note.getValue().n.name == nValType.rest){continue;}
//			myNote _n = note.getValue();
//			noteSt =  0;
//			noteDur = _n.sphereDur * durMult;
//			pa.outStr2Scr("Iter : " + iterStatic + " Play note : "+ _n.n.nameOct + " start : "+ noteSt + " dur: " + noteDur);
//			playNote(pa.glblOut, noteSt,noteDur, _n.n.freq);
//			if(_n.flags[myNote.isChord]){
//				for(Entry<String, myNote> cnote : ((myChord)(_n)).cnotes.entrySet()){
//					myNote chrdN = cnote.getValue();
//					if(_n.ID != chrdN.ID){
//						noteSt = 0;		//now
//						noteDur = chrdN.sphereDur * durMult;
//						//pa.outStr2Scr("Play note of chord : "+ chrdN.n.nameOct + " start : "+ noteSt + " dur: " + noteDur);
//						playNote(pa.glblOut, noteSt, noteDur, chrdN.n.freq);
//					}
//				}
//			}
//		}
//		iterStatic++;
//		this.dispFlags[myDispWindow.notesLoaded] = true;
//	}//addTrajNoteToPlay	
	//return summed outputs to simulation
//	//TODO replace with instrument outputs
//	protected void addTrajNoteToPlay(SortedMap<Integer, myNote> tmpNotes){
//		
//		
//		
//	}
//	
//	protected void addTrajNoteToStop(SortedMap<Integer, myNote> tmpNotes){
//		
//		
//	}
	//playing trajectory note SortedMap<Integer, myNote> tmpNotes
	//TODO replace with reticle-driven play and stop of notes
	protected void addTrajNoteToPlay(SortedMap<Integer, myNote> tmpNotes){
		if((null == tmpNotes) || (tmpNotes.size() == 0)){return;}
		//pa.glblOut.pauseNotes();
		for(SortedMap.Entry<Integer, myNote> note : tmpNotes.entrySet()) { 	
			if(note.getValue().n.name == nValType.rest){continue;}
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
		this.dispFlags[myDispWindow.notesLoaded] = true;
	}//addTrajNoteToPlay
	
	
	//modify playback engine, from UI interaction
	public void modCurrentPBETime(float modAmt){
		pbe.modCurTime(modAmt);		
	}
	
	//audio stuff, if window has audio
	public void playNote(AudioOutput out, float start, float duration, float freq){out.playNote(start,duration,freq);}	
	public void play(){
		if((!dispFlags[plays]) ||(!dispFlags[showIDX])){return;}	//if this doesn't play, or it is not being shown, exit immediately
//		//pa.outStr2Scr("ID :" + ID+" play() : startTime : " + pa.glblStartPlayTime + " last play time : " + pa.glblLastPlayTime + " modAmt Sec : " + String.format("%.4f", modAmtSec) + " frate :"+ String.format("%.4f", pa.frameRate));
//		pbe.play(modAmtSec);
//		//global play handling - move and draw reticle line		
		this.dispFlags[myDispWindow.notesLoaded] = false;
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
		for(int j = 0; j<guiObjs.length; j++){tmp = Arrays.asList(guiObjs[j].getStrData());res.addAll(tmp);}
		return res.toArray(new String[0]);	
	}
	
	//set colors of the trajectory for this window
	public void setTrajColors(int _tfc, int _tsc){trajFillClrCnst = _tfc;trajStrkClrCnst = _tsc;initTmpTrajStuff(dispFlags[trajPointsAreFlat]);}
	//get key used to access arrays in traj array
	protected String getTrajAraKeyStr(int i){return scoreStaffNames[i];}
	protected int getTrajAraIDXVal(String str){for(int i=0; i<scoreStaffNames.length;++i){if(scoreStaffNames[i].equals(str)){return i;}}return -1; }

	//add trajectory to appropriately keyed current trajectory ara in treemap	
	protected void processTrajectory(myDrawnNoteTraj drawnNoteTraj){
		TreeMap<String,ArrayList<myDrawnNoteTraj>> tmpTreeMap = drwnTrajMap.get(this.curDrnTrajScrIDX);
		ArrayList<myDrawnNoteTraj> tmpAra;
		if(curTrajAraIDX != -1){		//make sure some trajectory/staff has been selected
			if((tmpTreeMap != null) && (tmpTreeMap.size() != 0) ) {
				tmpAra = tmpTreeMap.get(getTrajAraKeyStr(curTrajAraIDX));			
				if((null==tmpAra) || (pa.flags[pa.clearStaffNewTraj])){		tmpAra = new ArrayList<myDrawnNoteTraj>();}
				//lastTrajIDX = tmpAra.size();
				tmpAra.add(drawnNoteTraj); 				
			} else {//empty or null tmpTreeMap - tmpAra doesn't exist
				tmpAra = new ArrayList<myDrawnNoteTraj>();
				tmpAra.add(drawnNoteTraj);
				//lastTrajIDX = tmpAra.size();
				if(tmpTreeMap == null) {tmpTreeMap = new TreeMap<String,ArrayList<myDrawnNoteTraj>>();} 
			}
			tmpTreeMap.put(getTrajAraKeyStr(curTrajAraIDX), tmpAra);
			processTrajIndiv(drawnNoteTraj);
		}	
		//individual traj processing
	}
	
	public void clearAllTrajectories(){//int instrIdx){
		TreeMap<String,ArrayList<myDrawnNoteTraj>> tmpTreeMap = drwnTrajMap.get(this.curDrnTrajScrIDX);
		ArrayList<myDrawnNoteTraj> tmpAra;
		if(curTrajAraIDX != -1){		//make sure some trajectory/staff has been selected
			if((tmpTreeMap != null) && (tmpTreeMap.size() != 0) ) {
				tmpTreeMap.put(getTrajAraKeyStr(curTrajAraIDX), new ArrayList<myDrawnNoteTraj>());
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
	
	protected abstract void endShiftKeyI();
	protected abstract void endAltKeyI();
	protected abstract void endCntlKeyI();
	
	//ui init routines
	protected abstract void setupGUIObjsAras();	
	protected abstract void setUIWinVals(int UIidx);
	protected abstract String getUIListValStr(int UIidx, int validx);
	protected abstract void processTrajIndiv(myDrawnNoteTraj drawnTraj);
	
	public abstract void clickDebug(int btnNum);
	
	protected abstract void initMe();
	protected abstract void resizeMe(float scale);	
	protected abstract void showMe();
	protected abstract void closeMe();	
	protected abstract void playMe();
	protected abstract void stopMe();
	protected abstract void drawMe(float animTimeMod);	
	
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
		String res = "Window : "+name+" ID: "+ID+" Fill :("+fillClr[0]+","+fillClr[1]+","+fillClr[2]+","+fillClr[3]+
				") | Stroke :("+fillClr[0]+","+fillClr[1]+","+fillClr[2]+","+fillClr[3]+") | Rect : ("+
				String.format("%.2f",rectDim[0])+","+String.format("%.2f",rectDim[1])+","+String.format("%.2f",rectDim[2])+","+String.format("%.2f",rectDim[3])+")\n";	
		return res;
	}
}//dispWindow


//displays sidebar menu of interaction and functionality
class mySideBarMenu extends myDispWindow{
	//booleans in main program
	public final String[] truePFlagNames = {//needs to be in order of flags
			"Debug Mode",		
			"Save Anim", 		
			"Shift-Key Pressed",
			"Alt-Key Pressed",
			"Cntl-Key Pressed",
			"Click interact", 	
			"Drawing Curve",
			"Changing View",	
			"Stop Playing Music",
			"Use Drawing Vel For Curve",
			"Displaying UI Menu",
			"Hide Piano Roll/Score",
			"Hide Sphere UI",
			"Hide Simulation",
			"Hide Instrument Editor",
			"Reverse Drawn Melody",
			"Notes are Forced To Global Key",
			"Move Forced Note Up in Pitch",
			"Tie Same Pitch Notes in Piano Roll",
			"Clear Staff With New Trajectory",
			"Sets Global Key/Time/Tempo Values"
			};
	
	public final String[] falsePFlagNames = {//needs to be in order of flags
			"Debug Mode",		
			"Save Anim", 		
			"Shift-Key Pressed",
			"Alt-Key Pressed",
			"Cntl-Key Pressed",
			"Click interact", 	
			"Drawing Curve",
			"Changing View",	 	
			"Play Music",
			"Use Drawing Vel For Curve",
			"Displaying UI Menu",
			"Show Score/Sequencer",
			"Show Sphere UI",
			"Show Simulation",
			"Show Instrument Editor",
			"Reverse Drawn Melody",
			"Notes are Not Forced To Key",
			"Move Forced Note Down in Pitch",
			"Keep Same Notes Separate in Piano Roll",
			"Add new trajectory to existing notes",
			"Sets Key/Time/Tempo at Current Time"
			};
	
	public int[][] pFlagColors;
	
	public final int clkFlgsStY = 10;
	
	public final String[] StateBoolNames = {"Shift","Alt","Cntl","Click", "Draw","View"};
	//multiplier for displacement to display text label for stateboolnames
	public final float[] StrWdMult = new float[]{-3.0f,-3.0f,-3.0f,-3.2f,-3.5f,-2.5f};
	public int[][] stBoolFlagColors;

	//	//GUI Objects	
	//idx's of objects in gui objs array	
	public static final int 
		gIDX_Tempo 			= 0, 
		gIDX_KeySig 		= 1,
		gIDX_TimeSigNum		= 2,
		gIDX_TimeSigDenom 	= 3;
//		gIDX_cycModDraw 		= 4;
	public final int numGUIObjs = 4;												//# of gui objects for ui
	
	//private flag based buttons - ui menu won't have these
	public static final int numPrivFlags = 0;
	
	//GUI Buttons
	public double minBtnClkY;			//where buttons should start on side menu

	public static final String[] guiBtnRowNames = new String[]{"Show Editor Window",/**"Add New",**/"DEBUG",/**"Instrument Editor",**/"File", "Transport","Set Vals at Cur Time"};
	public static final int modValsRow = 4;	//Row # in btnRowNames corresponding to the set value buttons
	public static final String[] guiBtnSetValsNames = new String[]{"Set Vals at Cur Time","Set Vals Globally"};
	public static final int 
			btnShowWinIdx = 0, //idxs in btn arrays
			//btnAddNewCmpIdx = 1,
			btnDBGSelCmpIdx = 1,
			//btnInstEditIdx = 3, 
			btnFileCmdIdx = 2,
			btnTrnsprtIdx = 3,
			btnSetGlblValsIdx = 4;
	//names for each row of buttons - idx 1 is name of row
	public final String[][] guiBtnNames = new String[][]{
		new String[]{"Sequence","Sphere UI", "Sim", "Inst Edit"},							//display specific windows - multi-select/ always on if sel
		//new String[]{"Score","Staff","Measure","Note"},			//add - momentary
		new String[]{"Data 1","Data 2","Data 3","Data 4"},			//DEBUG - momentary
		//new String[]{"New","Edit","Delete"},					//Instrument edit/modify - momentary
		new String[]{"Load","Save"},							//load an existing score, save an existing score - momentary		
		new String[]{"Rewind","Stop","Play","FastFwd"},			//transport controls - mixed
		new String[]{"Key Signature","Time Signature","Tempo"},			//transport controls - mixed
	};
	//whether buttons are momentary or not (on only while being clicked)
	public boolean[][] guiBtnInst = new boolean[][]{
//		new boolean[]{false,false,false},         					//display specific windows - multi-select/ always on if sel
		new boolean[]{false,false,false,false},         					//display specific windows - multi-select/ always on if sel
		//new boolean[]{true,true,true,true},                  		//add - momentary
		new boolean[]{true,true,true,true},                   		//delete - momentary
		//new boolean[]{true,true,true},                     			//Instrument edit/modify - momentary
		new boolean[]{true,true},			              					//load an existing score, save an existing score - momentary	
		new boolean[]{true,true,false,true},                   					//transport controls
		new boolean[]{true,true,true},			              					//set values at current reticle location(or closest appropriate) to be global values set in UI	
	};		
	
	//whether buttons are disabled(-1), enabled but not clicked/on (0), or enabled and on/clicked(1)
	public int[][] guiBtnSt = new int[][]{
		new int[]{0,0,0,0},                    					//display specific windows - multi-select/ always on if sel
		//new int[]{0,0,0,0},                  					//add - momentary
		new int[]{0,0,0,0},                   					//delete - momentary
		//new int[]{0,0,0},                     					//Instrument edit/modify - momentary
		new int[]{0,0},			              					//load an existing score, save an existing score - momentary	
		new int[]{0,0,0,0},                   					//transport controls
		new int[]{0,0,0},                   					//transport controls
	};
	
	public int[] guiBtnStFillClr;
	public int[] guiBtnStTxtClr;
	//row and column of currently clicked-on button (for display highlight as pressing)
	public int[] curBtnClick = new int[]{-1,-1};

	public mySideBarMenu(SeqVisFFTOcean _p, String _n, int _flagIdx, int[] fc, int[] sc, float[] rd, float[] rdClosed, String _winTxt, boolean _canDrawTraj) {
		super(_p, _n, _flagIdx, fc, sc, rd, rdClosed, _winTxt, _canDrawTraj);
		guiBtnStFillClr = new int[]{		//button colors based on state
				SeqVisFFTOcean.gui_White,								//disabled color for buttons
				SeqVisFFTOcean.gui_LightGray,								//not clicked button color
				SeqVisFFTOcean.gui_LightBlue,									//clicked button color
			};
		guiBtnStTxtClr = new int[]{			//text color for buttons
				SeqVisFFTOcean.gui_LightGray,									//disabled color for buttons
				SeqVisFFTOcean.gui_Black,									//not clicked button color
				SeqVisFFTOcean.gui_Black,									//clicked button color
			};			
		super.initThisWin(_canDrawTraj, false);
	}
	
	//set up initial colors for papplet's flags for display
	public void initPFlagColors(){
		pFlagColors = new int[pa.numFlags][3];
		for (int i = 0; i < pa.numFlags; ++i) { pFlagColors[i] = new int[]{(int) pa.random(150),(int) pa.random(100),(int) pa.random(150)}; }		
		stBoolFlagColors = new int[pa.numStFlagsToShow][3];
		stBoolFlagColors[0] = new int[]{255,0,0};
		stBoolFlagColors[1] = new int[]{0,255,0};
		stBoolFlagColors[2] = new int[]{0,0,255};		
		for (int i = 3; i < pa.numStFlagsToShow; ++i) { stBoolFlagColors[i] = new int[]{100+((int) pa.random(150)),150+((int) pa.random(100)),150+((int) pa.random(150))};		}
	}
	@Override
	protected void snapMouseLocs(int oldMouseX, int oldMouseY, int[] newMouseLoc){}//not a snap-to window
	
	
	@Override
	//initialize all private-flag based UI buttons here - called by base class
	public void initAllPrivBtns(){
		truePrivFlagNames = new String[]{								//needs to be in order of flags
		};
		falsePrivFlagNames = new String[]{			//needs to be in order of flags
		};
		privModFlgIdxs = new int[]{};
		numClickBools = privModFlgIdxs.length;	
	}
	
	@Override
	protected void initMe() {//init/reinit this window
		dispFlags[plays] = false;									//this window does not respond to/process playing reticle
		dispFlags[closeable] = false;
//		dispFlags[uiObjsAreVert] = true;
		initPrivFlags(numPrivFlags);		
	}	
	//set flag values and execute special functionality for this sequencer
	@Override
	public void setPrivFlags(int idx, boolean val){
		privFlags[idx] = val;
		//switch(idx){}
	}

	//initialize structure to hold modifiable menu regions
	@Override
	protected void setupGUIObjsAras(){						//called from super.initThisWin
		guiMinMaxModVals = new double [][]{						//min max mod values
			{30, 300, .1},										//base tempo value
			{0, 144, .05},										//base Key Sig -> keySigVals 0==Cmaj
			{1, 15, .05},										//base time sig num - 1 to 15 beats per measure
			{0, 5, .05}										//base time sig denom - note which gets the beat - init is 4/4 (this is idx in enum)
		//	{1, pa.maxCycModDraw, .1}
		};
		
		guiStVals = new double[]{
				120,							//gIDX_Tempo = 0, 
				0,								//gIDX_KeySig = 1,
				4,								//gIDX_TimeSigNum = 2,
				2								//gIDX_TimeSigDenom = 3,
			//	1								//how many draw cycles per render - init is render every draw call
		};
		guiObjNames = new String[]{"Tempo","Key Signature", "Beats per Measure", "Beat Note", "Draw Cycle"};		
		
		//idx 0 is treat as int, idx 1 is obj has list vals, idx 2 is object gets sent to windows
		guiBoolVals = new boolean [][]{
				new boolean[]{false, false, true},		//gIDX_Tempo = 0, 
				new boolean[]{true, true, true},		//gIDX_KeySig = 1,
				new boolean[]{true, false, true},		//gIDX_TimeSigNum = 2,
				new boolean[]{true, true, true}		//gIDX_TimeSigDenom = 3,
				//new boolean[]{true, false, false}		//gIDX_cycModDraw = 4;
		};
		
		minBtnClkY = (pa.numFlagsToShow+3) * yOff + clkFlgsStY;										//start of buttons from under boolean flags	
		initUIClickCoords(rectDim[0] + .1 * rectDim[2],minBtnClkY + (guiBtnRowNames.length * 2) * yOff,rectDim[0] + .99f * rectDim[2],0);//last val over-written by actual value in buildGuiObjs
		guiObjs = new myGUIObj[numGUIObjs];			//list of modifiable gui objects
		if(0!=numGUIObjs){
			buildGUIObjs(guiObjNames,guiStVals,guiMinMaxModVals,guiBoolVals, new double[]{xOff,yOff});
		}
	}
	
	//check if buttons clicked
	private boolean checkButtons(int mseX, int mseY){
		double stY = minBtnClkY + rowStYOff, endY = stY+yOff, stX = 0, endX, widthX; //btnLblYOff			
		for(int row=0; row<guiBtnRowNames.length;++row){
			widthX = rectDim[2]/(1.0f * guiBtnNames[row].length);
			stX =0;	endX = widthX;
			for(int col =0; col<guiBtnNames[row].length;++col){	
				if((pa.ptInRange(mseX, mseY,stX, stY, endX, endY)) && (guiBtnSt[row][col] != -1)){
					handleButtonClick(row,col);
					return true;
				}					
				stX += widthX;	endX += widthX; 
			}
			stY = endY + yOff+ rowStYOff;endY = stY + yOff;				
		}
		return false;
	}//handleButtonClick	
	public void clearAllBtnStates(){for(int row=0; row<guiBtnRowNames.length;++row){for(int col =0; col<guiBtnNames[row].length;++col){if((guiBtnInst[row][col]) && (guiBtnSt[row][col] ==1)){	guiBtnSt[row][col] = 0;}}}}
	
	public void handleButtonClick(int row, int col){
		int val = guiBtnSt[row][col];
		guiBtnSt[row][col] = (guiBtnSt[row][col] + 1)%2;
		switch(row){
			case btnShowWinIdx 		: {pa.handleShowWin(col, val);break;}
			//case btnAddNewCmpIdx 	: {pa.handleAddNewCmp(col, val);break;}
			case btnDBGSelCmpIdx  	: {pa.handleDBGSelCmp(col, val);break;}
			//case btnInstEditIdx 	: {pa.handleInstEdit(col, val);break;}
			case btnFileCmdIdx 		: {pa.handleFileCmd(col, val);break;}
			case btnTrnsprtIdx  	: {pa.handleTrnsprt(col, val);break;}		
			case btnSetGlblValsIdx 	: {pa.handleGlobalVals(col, val);break;}		
		}				
	}	

	//handle the display of UI objects backed by a list
	@Override
	protected String getUIListValStr(int UIidx, int validx){
		switch(UIidx){
		case gIDX_KeySig 		: {return keySigs[(validx % keySigs.length)]; }
		case gIDX_TimeSigDenom	: {return ""+ noteVals[(validx % noteVals.length)]+ " ("+timeSigDenom[(validx % timeSigDenom.length)]+")";}		
		}
		return "";
	}//dispUIListObj
	//uses passed time
	@Override //only send new values if actually new values
	protected void setUIWinVals(int UIidx){
		if(pa.flags[pa.setGlobalVals]){setGlblUIWinVals(UIidx); } else {setLclUIWinVals(UIidx);}
	}//setUIWinVals
	private void setLclUIWinVals(int UIidx){
		switch(UIidx){
		//set lcl/global vals
		case gIDX_KeySig 		: {
			int sel = (int)guiObjs[UIidx].getVal() % keySigs.length;			
			for(int i=1; i<pa.dispWinFrames.length; ++i){pa.dispWinFrames[i].setLocalKeySigVal(sel);} pa.setFlags(pa.forceInKey,false);					
			break;}
		case gIDX_TimeSigNum 	: 
		case gIDX_TimeSigDenom 	: {
			int tsDenom = timeSigDenom[(int)guiObjs[gIDX_TimeSigDenom].getVal() %timeSigDenom.length],
					tsNum = (int)guiObjs[gIDX_TimeSigNum].getVal();
				durType dType = pa.getDurTypeForNote(tsDenom);			
			for(int i=1; i<pa.dispWinFrames.length; ++i){pa.dispWinFrames[i].setLocalTimeSigVal(tsNum,tsDenom, dType);} 				
			
			break;}
		case gIDX_Tempo			: {
			float tmpTempo = (float)guiObjs[UIidx].getVal();
			for(int i=1; i<pa.dispWinFrames.length; ++i){pa.dispWinFrames[i].setLocalTempoVal(tmpTempo);}
			break;}
		}			
	
	}
	private void setGlblUIWinVals(int UIidx){
		switch(UIidx){
		//set lcl/global vals
		case gIDX_KeySig 		: {
			int sel = (int)guiObjs[UIidx].getVal() % keySigs.length;
			if (sel != myDispWindow.glblKeySig.keyIdx){for(int i=1; i<pa.dispWinFrames.length; ++i){pa.dispWinFrames[i].setGlobalKeySigVal(sel);} pa.setFlags(pa.forceInKey,false); }			
			break;}
		case gIDX_TimeSigNum 	: 
		case gIDX_TimeSigDenom 	: {
			int tsDenom = timeSigDenom[(int)guiObjs[gIDX_TimeSigDenom].getVal() %timeSigDenom.length],
					tsNum = (int)guiObjs[gIDX_TimeSigNum].getVal();
			durType dType = pa.getDurTypeForNote(tsDenom);			
			if((dType != glblBeatNote) || (glblTimeSig.beatPerMeas != tsNum) || (glblTimeSig.beatNote != tsDenom)){			
				for(int i=1; i<pa.dispWinFrames.length; ++i){pa.dispWinFrames[i].setGlobalTimeSigVal(tsNum,tsDenom, dType);} 
			}
			break;}
		case gIDX_Tempo			: {
			float tmpTempo = (float)guiObjs[UIidx].getVal();
			if(PApplet.abs(tmpTempo - glblTempo) > pa.feps){for(int i=1; i<pa.dispWinFrames.length; ++i){pa.dispWinFrames[i].setGlobalTempoVal(tmpTempo);}}
			break;}
		}			
	}
	
	
	@Override
	protected boolean hndlMouseMoveIndiv(int mouseX, int mouseY, myPoint mseClckInWorld){
		return false;
	}
	@Override
	protected boolean hndlMouseClickIndiv(int mouseX, int mouseY, myPoint mseClckInWorld, int mseBtn) {	
		if((!pa.ptInRange(mouseX, mouseY, rectDim[0], rectDim[1], rectDim[0]+rectDim[2], rectDim[1]+rectDim[3]))){return false;}//not in this window's bounds, quit asap for speedz
		int i = (int)((mouseY-(yOff + yOff + clkFlgsStY))/(yOff));					//TODO Awful - needs to be recalced, dependent on menu being on left
		if((i>=0) && (i<pa.numFlagsToShow)){
			pa.setFlags(pa.flagsToShow.get(i),!pa.flags[pa.flagsToShow.get(i)]);return true;	}
		else if(pa.ptInRange(mouseX, mouseY, 0, minBtnClkY, uiClkCoords[2], uiClkCoords[1])){return checkButtons(mouseX, mouseY);}//in region where clickable buttons are - uiClkCoords[1] is bottom of buttons
		return false;
	}
	@Override
	protected boolean hndlMouseDragIndiv(int mouseX, int mouseY,int pmouseX, int pmouseY, myPoint mouseClickIn3D, myVector mseDragInWorld, int mseBtn) {
		return false;
	}

	@Override
	protected void hndlMouseRelIndiv() {
		clearAllBtnStates();
	}

	private void drawSideBarBooleans(){
		//draw booleans and their state
		pa.translate(10,yOff*2);
		pa.setColorValFill(SeqVisFFTOcean.gui_Black);
		pa.text("Boolean Flags",0,yOff*.20f);
		pa.translate(0,clkFlgsStY);
		for(int idx =0; idx<pa.numFlagsToShow; ++idx){
			int i = pa.flagsToShow.get(idx);
			if(pa.flags[i] ){													dispMenuTxtLat(truePFlagNames[i],pFlagColors[i], true);			}
			else {	if(truePFlagNames[i].equals(falsePFlagNames[i])) {		dispMenuTxtLat(truePFlagNames[i],new int[]{180,180,180}, false);}	
					else {													dispMenuTxtLat(falsePFlagNames[i],new int[]{0,255-pFlagColors[i][1],255-pFlagColors[i][2]}, true);}		
			}
		}	
	}//drawSideBarBooleans
	private void drawSideBarStateBools(){ //numStFlagsToShow
		pa.translate(110,10);
		float xTrans = (int)((pa.menuWidth-100) / pa.numStFlagsToShow);
		for(int idx =0; idx<pa.numStFlagsToShow; ++idx){
			dispBoolStFlag(StateBoolNames[idx],stBoolFlagColors[idx], pa.flags[pa.stateFlagsToShow.get(idx)],StrWdMult[idx]);			
			pa.translate(xTrans,0);
		}	
		
	}
	//draw UI buttons
	private void drawSideBarButtons(){
		pa.translate(xOff*.5f,(float)minBtnClkY);
		pa.setFill(new int[]{0,0,0}, 255);
		for(int row=0; row<guiBtnRowNames.length;++row){
			if(row == this.modValsRow){
				pa.text(this.guiBtnSetValsNames[(pa.flags[pa.setGlobalVals] ? 1:0)],0,-yOff*.15f);
			} else {
				pa.text(guiBtnRowNames[row],0,-yOff*.15f);
			}
			pa.translate(0,rowStYOff);
			float xWidthOffset = rectDim[2]/(1.0f * guiBtnNames[row].length), halfWay;
			pa.pushMatrix();pa.pushStyle();
			pa.strokeWeight(.5f);
			pa.stroke(0,0,0,255);
			pa.noFill();
			pa.translate(-xOff*.5f, 0);
			for(int col =0; col<guiBtnNames[row].length;++col){
				halfWay = (xWidthOffset - pa.textWidth(guiBtnNames[row][col]))/2.0f;
				pa.setColorValFill(guiBtnStFillClr[guiBtnSt[row][col]+1]);
				pa.rect(0,0,xWidthOffset, yOff);	
				pa.setColorValFill(guiBtnStTxtClr[guiBtnSt[row][col]+1]);
				pa.text(guiBtnNames[row][col], halfWay, yOff*.75f);
				pa.translate(xWidthOffset, 0);
			}
			pa.popStyle();	pa.popMatrix();						
			pa.translate(0,btnLblYOff);
		}
	}//drawSideBarButtons	

	@Override
	protected void drawMe(float animTimeMod) {
		pa.pushMatrix();pa.pushStyle();
			drawSideBarBooleans();				//toggleable booleans 
		pa.popStyle();	pa.popMatrix();	
		pa.pushMatrix();pa.pushStyle();
			drawSideBarStateBools();				//lights that reflect various states
		pa.popStyle();	pa.popMatrix();	
		pa.pushMatrix();pa.pushStyle();			
			drawSideBarButtons();						//draw buttons
		pa.popStyle();	pa.popMatrix();	
		pa.pushMatrix();pa.pushStyle();
			drawGUIObjs();					//draw what user-modifiable fields are currently available
		pa.popStyle();	pa.popMatrix();			
		pa.pushMatrix();pa.pushStyle();
			drawWindowGuiObjs();		
		pa.popStyle();	pa.popMatrix();			
	}
	
	private void drawWindowGuiObjs(){
		if(pa.curFocusWin != -1){
			pa.pushMatrix();pa.pushStyle();
			pa.dispWinFrames[pa.curFocusWin].drawGUIObjs();					//draw what user-modifiable fields are currently available
			pa.dispWinFrames[pa.curFocusWin].drawClickableBooleans();					//draw what user-modifiable fields are currently available
			pa.popStyle();	pa.popMatrix();	
		}
		//pa.translate(0,yTransAmt);
		if(pa.flags[pa.showSimWin]){
			pa.pushMatrix();pa.pushStyle();	
			pa.dispWinFrames[pa.dispSimIDX].drawGUIObjs();					//draw what user-modifiable fields are currently available
			pa.dispWinFrames[pa.dispSimIDX].drawClickableBooleans();					//draw what user-modifiable fields are currently available
			pa.popStyle();	pa.popMatrix();	
					
		}
		//pa.translate(0,yTransAmt);
		if(pa.flags[pa.showInstEdit]){
			pa.pushMatrix();pa.pushStyle();			
			pa.dispWinFrames[pa.dispInstEditIDX].drawGUIObjs();					//draw what user-modifiable fields are currently available
			pa.dispWinFrames[pa.dispInstEditIDX].drawClickableBooleans();					//draw what user-modifiable fields are currently available
			pa.popStyle();	pa.popMatrix();						
		}
	}
	
	@Override
	public void drawClickableBooleans() {	}//this is only for non-sidebar menu windows, to display their own personal buttons
	@Override
	protected SortedMap<Integer, myNote> getNotesNow() {return null;}
	
	@Override
	public void clickDebug(int btnNum){}	
	@Override
	protected myPoint getMouseLoc3D(int mouseX, int mouseY){return pa.P(mouseX,mouseY,0);}
	@Override
	protected void initTrajStructs() {}
	@Override
	protected void endShiftKeyI() {}
	@Override
	protected void endAltKeyI() {}
	@Override
	protected void endCntlKeyI() {}
	@Override
	protected void closeMe() {}
	@Override
	protected void showMe() {}
	@Override
	protected void resizeMe(float scale) {}	
	@Override
	protected void playMe() {}
	@Override
	protected void stopMe() {}
	@Override
	protected void addSScrToWinIndiv(int newWinKey){}
	@Override
	protected void addTrajToScrIndiv(int subScrKey, String newTrajKey){}
	@Override
	protected void delSScrToWinIndiv(int idx) {}	
	@Override
	protected void delTrajToScrIndiv(int subScrKey, String newTrajKey) {}		
	@Override
	protected void setGlobalKeySigValIndiv(int idx, float time){	}//setCurrentKeySigVal
	@Override
	protected void setGlobalTimeSigValIndiv(int tsnum, int tsdenom, durType _d, float time){	}//setCurrentTimeSigVal
	@Override
	protected void setGlobalTempoValIndiv(float tempo, float time){	}//setCurrentTimeSigVal
	@Override
	//set current key signature, at time passed - for score, set it at nearest measure boundary
	protected void setLocalKeySigValIndiv(myKeySig lclKeySig, ArrayList<nValType> lclKeyNotesAra, float time){}
	@Override
	//set time signature at time passed - for score, set it at nearest measure boundary
	protected void setLocalTimeSigValIndiv(int tsnum, int tsdenom, durType _beatNoteType, float time){}
	@Override
	//set time signature at time passed - for score, set it at nearest measure boundary
	protected void setLocalTempoValIndiv(float tempo, float time){}
	//no trajectory here
	@Override
	protected void processTrajIndiv(myDrawnNoteTraj drawnTraj){}	
	@Override
	protected void setScoreInstrValsIndiv(){}
	@Override
	public String toString(){
		String res = super.toString();
		return res;
	}
}//mySideBarMenu

//class holds trajctory and 4 macro cntl points, and handling for them
class myDrawnNoteTraj {
	public SeqVisFFTOcean pa;
	public myDispWindow win;
	public static int trjCnt = 0;
	public int ID;

	public myDrawnObject drawnTraj;						//a drawable curve - per staff
	public static float topOffY;
	public int drawnTrajPickedIdx;						//idx of mouse-chosen point	in editable drawn trajectory		
	public static final int drawnTrajEditWidth = 10;			//width in cntl points of the amount of the drawn trajectory deformed by dragging
	public static final float trajDragScaleAmt = 100.0f;					//amt of displacement when dragging drawn trajectory to edit
	public static float msClkPtRad = 10,msClkPt3DRad = 20,	//radius within which a mouse click will register on a point
			spTmplOffset, 
			mseSens = 100.0f,
			minTmplOff, maxTmplOff;		//offset between end points in edit draw region, min and max 
	public static float sqMsClkRad = msClkPtRad*msClkPtRad;

	public myPoint[] edtCrvEndPts;					//end points for edit curve in editable region	- modify these guys	to move curve - pts 2 and 3 are perp control axes						
	public int editEndPt;							//if an endpoint is being edited(moved around) : -1 == no, 0 = pt 0, 1 = pt 1, 2 = pt 2, 3 = pt 3 ;
		
	//public myPoint[] pathBetweenPts;				//Array that stores all the path Points, once they are scaled
	
	public int fillClrCnst, strkClrCnst;
	
	public TreeMap<Integer,myNote> drawnNotesMap;		//all the notes that this trajectory is responsible for
	
	public boolean[] trajFlags;
	public static final int 
				flatPtIDX = 0,						//whether this should draw plat circles or spheres for its points
				smCntlPtsIDX = 1;					//whether the 4 cntl points should be as small as regular points or larger
	public static final int numTrajFlags = 2;
	
	public int ctlRad;
	
	public myDrawnNoteTraj(SeqVisFFTOcean _p, myDispWindow _win,float _topOffy, int _fillClrCnst, int _strkClrCnst, boolean _flat, boolean _smCntl){
		pa = _p;
		fillClrCnst = _fillClrCnst; 
		strkClrCnst = _strkClrCnst;
		win = _win;
		ID = trjCnt++;
		setTopOffy(_topOffy);			//offset in y from top of screen
		initTrajFlags();
		initTrajStuff();		
		trajFlags[flatPtIDX] = _flat;
		trajFlags[smCntlPtsIDX] = _smCntl;
		ctlRad = (trajFlags[smCntlPtsIDX] ? myDrawnObject.trajPtRad : 5 );
	}
	protected void initTrajFlags(){trajFlags = new boolean[numTrajFlags];for(int i=0;i<numTrajFlags;++i){trajFlags[i]=false;}}
	public void initTrajStuff(){
		spTmplOffset = 400;						//vary this based on scale of drawn arc, to bring the end points together and keep arcs within gray region
		minTmplOff = 10; 		
		maxTmplOff = 400;		
		drawnTrajPickedIdx = -1;
		editEndPt = -1;
		
		drawnNotesMap = new TreeMap<Integer,myNote>();

		edtCrvEndPts = new myPoint[4];
		//pathBetweenPts = new myPoint[0];
		edtCrvEndPts[0] = new myPoint(win.rectDim[0] + .25 * win.rectDim[2], .5 * (2*win.rectDim[1] + win.rectDim[3]),0);
		edtCrvEndPts[1] = new myPoint(win.rectDim[0] + .75 * win.rectDim[2], .5 * (2*win.rectDim[1] + win.rectDim[3]),0);		
	}
	
	public void calcPerpPoints(){
		myVector dir = pa.U(myVector._rotAroundAxis(pa.V(edtCrvEndPts[0],edtCrvEndPts[1]), pa.c.drawSNorm));
		float mult =  .125f,
		dist = mult * (float)myPoint._dist(edtCrvEndPts[0],edtCrvEndPts[1]);
		edtCrvEndPts[2] = pa.P(pa.P(edtCrvEndPts[0],edtCrvEndPts[1]), dist,dir);
		edtCrvEndPts[3] = pa.P(pa.P(edtCrvEndPts[0],edtCrvEndPts[1]),-dist,dir);
	}
	//scale edit points and cntl points
	public void reCalcCntlPoints(float scale){
		for(int i = 0; i<edtCrvEndPts.length; ++i){	edtCrvEndPts[i].y = win.calcOffsetScale(edtCrvEndPts[i].y,scale,topOffY);edtCrvEndPts[i].z = 0; }//edtCrvEndPts[curDrnTrajScrIDX][i].y -= topOffY; edtCrvEndPts[curDrnTrajScrIDX][i].y *= scale;edtCrvEndPts[curDrnTrajScrIDX][i].y += topOffY;	}	
		if(drawnTraj != null){
			((myVariStroke)drawnTraj).scaleMeY(false,scale,topOffY);//only for display - rescaling changes the notes slightly so don't recalc notes
		}
	}//reCalcCntlPoints/
	
	public void startBuildTraj(){
		edtCrvEndPts[2] = null;
		edtCrvEndPts[3] = null;
		calcPerpPoints();
		drawnTraj = new myVariStroke(pa, pa.V(pa.c.drawSNorm),fillClrCnst, strkClrCnst);
		drawnTraj.startDrawing();
	}
	public boolean startEditEndPoint(int idx){
		editEndPt = idx; win.dispFlags[myDispWindow.editingTraj] = true;
		//pa.outStr2Scr("Handle TrajClick 2 startEditEndPoint : " + name + " | Move endpoint : "+editEndPt);
		return true;
	}
	//check if initiating an edit on an existing object, if so then set up edit
	public boolean startEditObj(myPoint mse){
		boolean doEdit = false; 
		float chkDist = win.dispFlags[myDispWindow.is3DWin] ? msClkPt3DRad : msClkPtRad;
		//first check endpoints, then check curve points
		double[] distTocPts = new double[1];			//using array as pointer, passing by reference
		int cntpIdx = win.findClosestPt(mse, distTocPts, edtCrvEndPts);	
		if(distTocPts[0] < chkDist){						startEditEndPoint(cntpIdx);} 
		else {
			double[] distToPts = new double[1];			//using array as pointer, passing by reference
			myPoint[] pts = ((myVariStroke)drawnTraj).getDrawnPtAra(false);
			int pIdx = win.findClosestPt(mse, distToPts, pts);
			//pa.outStr2Scr("Handle TrajClick 2 startEditObj : " + name);
			if(distToPts[0] < chkDist){//close enough to mod
				win.setEditCueCircle(0,mse);
				win.dispFlags[myDispWindow.editingTraj] = true;
				doEdit = true;
				//pa.outStr2Scr("Handle TrajClick 3 startEditObj modPt : " + name + " : pIdx : "+ pIdx);
				drawnTrajPickedIdx = pIdx;	
				editEndPt = -1;
			} else if (distToPts[0] < sqMsClkRad){//not close enough to mod but close to curve
				win.setEditCueCircle(1,mse);
				win.dispFlags[myDispWindow.smoothTraj] = true;
			}
		}
		return doEdit;
	}//startEditObj//rectDim
	
	//returns true if within eps of any of the points of this trajector
	public boolean clickedMe(myPoint mse){
		float chkDist = win.dispFlags[myDispWindow.is3DWin] ? msClkPt3DRad : msClkPtRad;	
		double[] distToPts = new double[1];			//using array as pointer, passing by reference
		int cntpIdx = win.findClosestPt(mse, distToPts, edtCrvEndPts);	
		if(distToPts[0] < chkDist){return true;}
		distToPts[0] = 9999;
		int pIdx = win.findClosestPt(mse, distToPts, ((myVariStroke)drawnTraj).getDrawnPtAra(false));
		return (distToPts[0] < chkDist);
	}
	
	private boolean checkEndPoint(myPoint pt){return ((pt.x >= win.rectDim[0]) && (pt.x <= win.rectDim[0]+win.rectDim[2]) && (pt.y >= win.rectDim[1]) && (pt.y <=  win.rectDim[1]+win.rectDim[3]));}
	private void modTrajCntlPts(myVector diff){
		if((editEndPt == 0) || (editEndPt == 1)){
			myPoint newLoc = myPoint._add(edtCrvEndPts[editEndPt], diff);
			if(checkEndPoint(newLoc)){edtCrvEndPts[editEndPt].set(newLoc);}
			//edtCrvEndPts[editEndPt]._add(diff);		
			calcPerpPoints();
			drawnTraj.remakeDrawnTraj(false);
			rebuildDrawnTraj();	
		} else {//scale all traj points based on modification of pts 2 or 3 - only allow them to move along the perp axis
			myVector abRotAxis = pa.U(myVector._rotAroundAxis(pa.V(edtCrvEndPts[0],edtCrvEndPts[1]), pa.c.drawSNorm));
			float dist = (float)myPoint._dist(edtCrvEndPts[2], edtCrvEndPts[3]);
			double modAmt = diff._dot(abRotAxis);
			if(editEndPt == 2){	edtCrvEndPts[2]._add(pa.V(modAmt,abRotAxis));edtCrvEndPts[3]._add(pa.V(-modAmt,abRotAxis));} 
			else {				edtCrvEndPts[2]._add(pa.V(-modAmt,abRotAxis));edtCrvEndPts[3]._add(pa.V(modAmt,abRotAxis));}
			float dist2 = (float)myPoint._dist(edtCrvEndPts[2], edtCrvEndPts[3]);
			//pa.outStr2Scr("modTrajCntlPts : editEndPt : " + editEndPt + " : diff : "+ diff+ " dist : " + dist+ " dist2 :" + dist2 + " rot tangent axis : " + abRotAxis + " | Scale : " + (1+dist2)/(1+dist) );
			((myVariStroke)drawnTraj).scalePointsAboveAxis(edtCrvEndPts[0],edtCrvEndPts[1], abRotAxis, (1+dist2)/(1+dist));
			//pa.outStr2Scr("");
		}			
	}
	
	//edit the trajectory used for UI input in this window
	public boolean editTraj(int mouseX, int mouseY,int pmouseX, int pmouseY, myPoint mouseClickIn3D, myVector mseDragInWorld){
		boolean mod = false;
		if((drawnTrajPickedIdx == -1) && (editEndPt == -1) && (!win.dispFlags[myDispWindow.smoothTraj])){return mod;}			//neither endpoints nor drawn points have been edited, and we're not smoothing
		myVector diff = win.dispFlags[myDispWindow.is3DWin] ? mseDragInWorld : pa.V(mouseX-pmouseX, mouseY-pmouseY,0);		
		//pa.outStr2Scr("Diff in editTraj for  " + name + "  : " +diff.toStrBrf());
		//needs to be before templateZoneY check
		if (editEndPt != -1){//modify scale of ornament here, or modify drawn trajectory	
			modTrajCntlPts(diff);
			mod = true;
		} else if (drawnTrajPickedIdx != -1){	//picked trajectory element to edit other than end points	
			diff._div(mseSens);
			((myVariStroke)drawnTraj).handleMouseDrag(diff,drawnTrajPickedIdx);
			mod = true;
		} 
		return mod;
	}//editTraj
	
	public void endEditObj(){
		if((drawnTrajPickedIdx != -1) || (editEndPt != -1)
			|| ( win.dispFlags[myDispWindow.smoothTraj])){//editing curve
			drawnTraj.remakeDrawnTraj(false);
			rebuildDrawnTraj();		
		}
//		else if( win.dispFlags[myDispWindow.smoothTraj]){		
//			drawnTraj.remakeDrawnTraj(false);	
//			rebuildDrawnTraj();	
//		}
		//pa.outStr2Scr("In Traj : " + this.ID + " endEditObj ");
		win.processTrajectory(this);//dispFlags[trajDirty] = true;
		drawnTrajPickedIdx = -1;
		editEndPt = -1;
		win.dispFlags[myDispWindow.editingTraj] = false;
		win.dispFlags[myDispWindow.smoothTraj] = false;
	}
	
	public void endDrawObj(myPoint endPoint){
		drawnTraj.addPt(endPoint);
		//pa.outStr2Scr("Size of drawn traj : " + drawnTraj.cntlPts.length);
		if(drawnTraj.cntlPts.length >= 2){
			drawnTraj.finalizeDrawing(true);
			myPoint[] pts = ((myVariStroke)drawnTraj).getDrawnPtAra(false);
			//pa.outStr2Scr("Size of pts ara after finalize (use drawn vels : " +false + " ): " + pts.length);
			edtCrvEndPts[0] = pa.P(pts[0]);
			edtCrvEndPts[1] = pa.P(pts[pts.length-1]);
			rebuildDrawnTraj();
			//pa.outStr2Scr("In Traj : " + this.ID + " endDrawObj ");
			win.processTrajectory(this);
		} else {
			drawnTraj = new myVariStroke(pa, pa.V(pa.c.drawSNorm),fillClrCnst, strkClrCnst);
		}
		win.dispFlags[myDispWindow.drawingTraj] = false;
	}	
	
	public void addPoint(myPoint mse){
		drawnTraj.addPt(mse);
	}
	//print out all points in this trajectory
	public void dbgPrintAllPoints(){
		if(drawnTraj == null){return;}
		((myVariStroke) drawnTraj).dbgPrintAllPoints(false);
	}
	
	//use animTimeMod to animate/decay showing this traj TODO 
	public void drawMe(float animTimeMod){
		if(drawnTraj != null){
			pa.setColorValFill(fillClrCnst);
			pa.setColorValStroke(strkClrCnst);
			for(int i =0; i< edtCrvEndPts.length; ++i){
				win.showKeyPt(edtCrvEndPts[i],""+ (i+1),ctlRad);
			}	
			((myVariStroke)drawnTraj).drawMe(false,trajFlags[flatPtIDX]);
		} 
	}
	public void rebuildDrawnTraj(){
		//Once edge is drawn
		calcPerpPoints();
		if(drawnTraj != null){
			if(drawnTraj.flags[drawnTraj.isMade]){				  
				//Initialize the array that stores the path
				int a= 0, b= 1;
				if(pa.flags[pa.flipDrawnTraj]){	 a = 1; b= 0;}
				if(false){//pathBetweenPts =
						((myVariStroke)drawnTraj).moveVelCurveToEndPoints(edtCrvEndPts[a], edtCrvEndPts[b], pa.flags[pa.flipDrawnTraj]); }
				else{//pathBetweenPts =
						((myVariStroke)drawnTraj).moveCntlCurveToEndPoints(edtCrvEndPts[a], edtCrvEndPts[b], pa.flags[pa.flipDrawnTraj]);  	}
			}
		}	
	}//rebuildDrawnTraj	
	public void setTopOffy(float _topOffy){	topOffY = _topOffy;	}		//offset in y from top of screen
}//class myDrawnNoteTraj

class myScrollBars{
	public SeqVisFFTOcean pa;
	public myDispWindow win;
	public static int scrBarCnt = 0;
	public int ID;
	//displacement for scrolling display - x/y location of window, x/y zoom - use scrollbars&zoomVals - displacement is translate, zoom is scale
	public float[] scrollZoomDisp,
	//start x,y, width, height of scroll bar region 
	 	hScrlDims,	
	//start x,y, width, height of scroll bar region 
	 	vScrlDims;
	private final float thk = 20, thmult = 1.5f;				//scrollbar dim is 25 pxls wide	
	
	public float[][] arrowDims = new float[][]{{0,0,0,0},{0,0,0,0},{0,0,0,0},{0,0,0,0},{0,0,0,0},{0,0,0,0}};
	//hthumb x,y width,height,vthumb x,y width,height
	public float[][] thmbs = new float[][]{{0,0},{0,0}};
	//hthumb min/max, vthumb min/max bounds
	public float[][] thmbBnds = new float[][]{{0,0},{0,0}};
	
	public static final int
		upIDX = 0,
		dnIDX = 1,
		ltIDX = 2,
		rtIDX = 3,
		hThmbIDX = 0,
		vThmbIDX = 1;
	
	public int[][] clrs;			//colors for thumb and up/down/left/right arrows
	
	public myScrollBars(SeqVisFFTOcean _pa,myDispWindow _win){
		pa = _pa;
		win = _win;
		ID = scrBarCnt++;
		setSize();
		clrs = new int[][]{ pa.getRndClr(),pa.getRndClr(),pa.getRndClr(),pa.getRndClr(),pa.getRndClr(),pa.getRndClr()};
	}//myScrollBars
	
	public void setSize(){
		float rectWidth = win.rectDim[0]+win.rectDim[2],vScrlStartY = win.rectDim[1]+(win.closeBox[3]),
				rectHeight = win.rectDim[3]-(vScrlStartY);
		vScrlDims = new float[]{rectWidth - thk,vScrlStartY, thk,  rectHeight-thk};
		hScrlDims = new float[]{win.rectDim[0], win.rectDim[1]+win.rectDim[3] - thk,win.rectDim[2]-thk,thk};

		thmbs[hThmbIDX] = new float[]{hScrlDims[0]+thk,hScrlDims[1],thmult*thk,thk};			//location/dims of thumb
		thmbBnds[hThmbIDX] = new float[]{thmbs[hThmbIDX][0],hScrlDims[2]-thmbs[hThmbIDX][2]-thk};		//min/max x val of horiz thumb
		
		thmbs[vThmbIDX] = new float[]{vScrlDims[0],(vScrlDims[1]+thk),thk,thmult*thk};			//location/dims of thumb
		thmbBnds[vThmbIDX] = new float[]{thmbs[vThmbIDX][1],vScrlDims[3]-thmbs[vThmbIDX][3]-thk};		//min/max of y val of vert thumb
		
		//arrow boxes - x,y,widht,height
		arrowDims = new float[][]{
			{vScrlDims[0],vScrlDims[1],thk,thk},			//up
			{vScrlDims[0],vScrlDims[1]+vScrlDims[3]-thk,thk,thk},		//down
			{hScrlDims[0],					hScrlDims[1],thk,thk},			//left
			{hScrlDims[0]+hScrlDims[2]-thk,hScrlDims[1],thk,thk}};		//right

	}
	public void drawMe(){
		pa.pushMatrix(); pa.pushStyle();
		pa.setColorValFill(pa.gui_LightGray);
		pa.setColorValStroke(pa.gui_Black);
		pa.strokeWeight(1.0f);
		pa.rect(vScrlDims);
		pa.rect(hScrlDims);
		for(int i =0; i<arrowDims.length;++i){
			pa.setFill(clrs[i]);
			pa.rect(arrowDims[i]);
		}
		pa.popStyle();pa.popMatrix();
		pa.pushMatrix(); pa.pushStyle();
		for(int i =0; i<thmbs.length;++i){
			pa.setFill(clrs[i + 4]);
			pa.rect(thmbs[i]);
		}
		
		pa.popStyle();pa.popMatrix();
	}//drawMe
	
	
	public boolean msePtInRect(int x, int y, float[] r){return ((x > r[0])&&(x <= r[0]+r[2])&&(y > r[1])&&(y <= r[1]+r[3]));}		
	public boolean handleMouseClick(int mouseX, int mouseY, myPoint mouseClickIn3D){
		
		return false;	
	}//handleMouseClick
	public boolean handleMouseDrag(int mouseX, int mouseY,int pmouseX, int pmouseY, myPoint mouseClickIn3D, myVector mseDragInWorld){	
		
		return false;
	}//handleMouseDrag
	public void handleMouseRelease(){
		
		
	}//handleMouseRelease
	
	
	
}//myScrollBars

