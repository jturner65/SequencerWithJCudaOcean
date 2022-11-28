package SphrSeqFFTVisPKG;

import java.util.ArrayList;
import java.util.SortedMap;

import SphrSeqFFTVisPKG.note.myNote;
import SphrSeqFFTVisPKG.note.enums.noteDurType;
import SphrSeqFFTVisPKG.note.enums.noteValType;
import SphrSeqFFTVisPKG.staff.myKeySig;
import base_JavaProjTools_IRender.base_Render_Interface.IRenderInterface;
import base_Math_Objects.vectorObjs.doubles.myPoint;
import base_Math_Objects.vectorObjs.doubles.myVector;
import processing.core.PApplet;

//displays sidebar menu of interaction and functionality
public class mySideBarMenu extends Base_DispWindow{
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
				IRenderInterface.gui_White,								//disabled color for buttons
				IRenderInterface.gui_LightGray,								//not clicked button color
				IRenderInterface.gui_LightBlue,									//clicked button color
			};
		guiBtnStTxtClr = new int[]{			//text color for buttons
				IRenderInterface.gui_LightGray,									//disabled color for buttons
				IRenderInterface.gui_Black,									//not clicked button color
				IRenderInterface.gui_Black,									//clicked button color
			};			
		super.initThisWin(_canDrawTraj, false, true);
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
		guiObjNames = new String[]{"Tempo","Key Signature", "Beats per Measure", "Beat Note"};		
		
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
				noteDurType dType = pa.getDurTypeForNote(tsDenom);			
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
			if (sel != Base_DispWindow.glblKeySig.keyIdx){for(int i=1; i<pa.dispWinFrames.length; ++i){pa.dispWinFrames[i].setGlobalKeySigVal(sel);} pa.setFlags(pa.forceInKey,false); }			
			break;}
		case gIDX_TimeSigNum 	: 
		case gIDX_TimeSigDenom 	: {
			int tsDenom = timeSigDenom[(int)guiObjs[gIDX_TimeSigDenom].getVal() %timeSigDenom.length],
					tsNum = (int)guiObjs[gIDX_TimeSigNum].getVal();
			noteDurType dType = pa.getDurTypeForNote(tsDenom);			
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
		pa.setColorValFill(IRenderInterface.gui_Black,255);
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
			if(row == mySideBarMenu.modValsRow){
				pa.text(mySideBarMenu.guiBtnSetValsNames[(pa.flags[pa.setGlobalVals] ? 1:0)],0,-yOff*.15f);
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
				pa.setColorValFill(guiBtnStFillClr[guiBtnSt[row][col]+1],255);
				pa.rect(0,0,xWidthOffset, yOff);	
				pa.setColorValFill(guiBtnStTxtClr[guiBtnSt[row][col]+1],255);
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
//		//pa.translate(0,yTransAmt);
//		if(pa.flags[pa.showSimWin]){
//			pa.pushMatrix();pa.pushStyle();	
//			pa.dispWinFrames[pa.dispSimIDX].drawGUIObjs();					//draw what user-modifiable fields are currently available
//			pa.dispWinFrames[pa.dispSimIDX].drawClickableBooleans();					//draw what user-modifiable fields are currently available
//			pa.popStyle();	pa.popMatrix();	
//					
//		}
		//pa.translate(0,yTransAmt);
		if(pa.flags[pa.showInstEdit]){
			pa.pushMatrix();pa.pushStyle();			
			pa.dispWinFrames[SeqVisFFTOcean.dispInstEditIDX].drawGUIObjs();					//draw what user-modifiable fields are currently available
			pa.dispWinFrames[SeqVisFFTOcean.dispInstEditIDX].drawClickableBooleans();					//draw what user-modifiable fields are currently available
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
	protected void modMySongLoc(float modAmt) {};
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
	protected void setGlobalTimeSigValIndiv(int tsnum, int tsdenom, noteDurType _d, float time){	}//setCurrentTimeSigVal
	@Override
	protected void setGlobalTempoValIndiv(float tempo, float time){	}//setCurrentTimeSigVal
	@Override
	//set current key signature, at time passed - for score, set it at nearest measure boundary
	protected void setLocalKeySigValIndiv(myKeySig lclKeySig, ArrayList<noteValType> lclKeyNotesAra, float time){}
	@Override
	//set time signature at time passed - for score, set it at nearest measure boundary
	protected void setLocalTimeSigValIndiv(int tsnum, int tsdenom, noteDurType _beatNoteType, float time){}
	@Override
	//set time signature at time passed - for score, set it at nearest measure boundary
	protected void setLocalTempoValIndiv(float tempo, float time){}
	//no trajectory here
	@Override
	protected void processTrajIndiv(myDrawnSmplTraj drawnTraj){}	
	@Override
	protected void setScoreInstrValsIndiv(){}
	//init any extra ui objs
	@Override
	protected void initXtraUIObjsIndiv() {}
	@Override
	public String toString(){
		String res = super.toString();
		return res;
	}
}//mySideBarMenu