package Project5Pkg;

import java.util.ArrayList;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import ddf.minim.AudioOutput;
import ddf.minim.Minim;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PImage;
import processing.core.PShape;

public class mySphereUI extends myDispWindow {
	public final int numGUIObjs = 0;												//# of gui objects for ui
	
	public TreeMap<String, mySphereCntl> sphereCntls;								//controls for each instrument
	
	public String curSelSphere;
	public float focusZoom = 1;				//amt to zoom the in-focus sphere
	
	//to handle real-time update of locations of spheres
	public myVector curMseLookVec;  //pa.c.getMse2DtoMse3DinWorld()
	public myPoint curMseLoc3D;		//pa.c.getMseLoc(pa.sceneCtrVals[pa.sceneIDX])

	//private child-class flags
	public static final int 
			sphereSelIDX = 0,			//sphere has been clicked on and fixed for input
			showTrajIDX =  1;			//show drawn trajectories
	public static final int numPrivFlags = 2;
	
		
	private float sepDist,zMin, zMax, zStep;
	private int numTunnelCircles; 
	private myVector[] tunnelCrcls;
	
	private float[] orbitRads;				//radii of rings showing planets
	//private myVector ctrVec;
	//private myVector[] sphereOrbits;
	//private float[] angles,angleIncr;					//angle for each radius
	//focus location of sphere UI 
	public static final myPoint fcsCtr = new myPoint(396,396,1190);
	
	public mySphereUI(CAProject5 _p, String _n, int _flagIdx, int[] fc, int[] sc, float[] rd, float[] rdClosed,String _winTxt, boolean _canDrawTraj) {
		super(_p, _n, _flagIdx, fc, sc, rd, rdClosed, _winTxt, _canDrawTraj);
		float stY = rectDim[1]+rectDim[3]-4*yOff,stYFlags = stY + 2*yOff;
		//initUIClickCoords(rectDim[0] + .1 * rectDim[2],stY,rectDim[0] + rectDim[2],stY + yOff);
		curSelSphere = "";	
		trajFillClrCnst = CAProject5.gui_DarkCyan;		//override this in the ctor of the instancing window class
		trajStrkClrCnst = CAProject5.gui_Cyan;
		super.initThisWin(_canDrawTraj, true);
	}
	
	@Override
	//initialize all private-flag based UI buttons here - called by base class
	public void initAllPrivBtns(){
		truePrivFlagNames = new String[]{								//needs to be in order of flags
				"Showing Drawn Tajectories"
		};
		falsePrivFlagNames = new String[]{			//needs to be in order of flags
				"Hiding Drawn Tajectories"
		};
		privModFlgIdxs = new int[]{showTrajIDX};
		numClickBools = privModFlgIdxs.length;	
		//maybe have call for 		initPrivBtnRects(0);		 here
		initPrivBtnRects(0,numClickBools);
	}
	
	@Override
	protected void initMe() {	
		initUIBox();				//set up ui click region to be in sidebar menu below menu's entries		
		dispFlags[plays] = true;								//this window responds to travelling reticle/playing
		vsblStLoc = new float[]{0,0};
		dispFlags[trajDecays] = true;								//this window responds to travelling reticle/playing
		curTrajAraIDX = 0;
		
		initPrivFlags(numPrivFlags);
	}
	
	@Override
	//set flag values and execute special functionality for this sequencer
	public void setPrivFlags(int idx, boolean val){
		privFlags[idx] = val;
		switch(idx){
			case sphereSelIDX : { 
				if(val){	
					//curTrajAraIDX = 
				} else {
					
				}
				break;}				//a sphere has been selected and either fixed or released
			case showTrajIDX : {
				break;
			}
		}		
	}//setPrivFlags		
	
	//initialize structure to hold modifiable menu regions
	@Override
	protected void setupGUIObjsAras(){	
		//pa.outStr2Scr("setupGUIObjsAras in :"+ name);
		guiMinMaxModVals = new double [][]{{}};					//min max mod values for each modifiable UI comp		
		guiStVals = new double[]{};								//starting value
		guiObjNames = new String[]{};							//name/label of component		
		//idx 0 is treat as int, idx 1 is obj has list vals, idx 2 is object gets sent to windows
		guiBoolVals = new boolean [][]{{}};						//per-object  list of boolean flags
		
		//since horizontal row of UI comps, uiClkCoords[2] will be set in buildGUIObjs		
		guiObjs = new myGUIObj[numGUIObjs];			//list of modifiable gui objects
		if(numGUIObjs > 0){
			buildGUIObjs(guiObjNames,guiStVals,guiMinMaxModVals,guiBoolVals,new double[]{xOff,yOff});			//builds a horizontal list of UI comps
		}
	}
	@Override
	protected void setUIWinVals(int UIidx) {
		switch(UIidx){
		default : {break;}
		}
	}
	//if any ui values have a string behind them for display
	@Override
	protected String getUIListValStr(int UIidx, int validx) {
		switch(UIidx){
			default : {break;}
		}
		return "";
	}

	public final myVector ctrVec = new myVector(400, 400,0);
	//once score instruments have been set, this function is called
	//initSphInstrGraphics(float _rad, myPoint _ctr, PImage _txtr, int[] specClrInt, int[] ambClrInt, int[] emmClrInt)
	@Override
	public void setScoreInstrValsIndiv(){
		int cnt = 0, numBalls = instrs.size(), numCols = (int)(Math.sqrt(numBalls-1))+1, numRows = (int)(numBalls/numCols);
		initPrivFlags(numPrivFlags);
		sphereCntls = new TreeMap<String, mySphereCntl>();
		float offset = pa.sphereRad * 4.5f;
		int[][] clrAra = new int[][]{ 
				new int[]{255,255,255},
				new int[]{100,100,100},
				new int[]{0,0,0}
			};
		//set sphere detail before building instruments
		pa.sphereDetail(50);
		//myPoint ctr;
		int orbitCnt = (instrs.size()/4) + 1;		//how  many per orbit
		orbitRads = new float[orbitCnt];			//radius of orbit
		for(int i =0; i<orbitCnt;++i){orbitRads[i] = (i+1) * offset * .9f;}
		int numPerOrbit = 3,					//starts with 3 in the ring, then up to 4, then up to 5, etc.  add offset to each ring radius
			ringRadIDX = 0,	perRingCnt = 0;
		float rotAngle = 0, angleDelta = PConstants.TWO_PI/(1.0f*numPerOrbit);
		for(int i =0; i<instrs.size(); ++i){		//for every instrument
			if(numPerOrbit == perRingCnt){
				perRingCnt = 0;
				numPerOrbit++;
				rotAngle = pa.random(1.0f)*PConstants.TWO_PI;
				ringRadIDX++;
				angleDelta = PConstants.TWO_PI/(1.0f*numPerOrbit);
//				mult *= -1;
			}
			myInstr entry = instrs.get(scoreStaffNames[i]);
			//for every instrument build a new mySphereCntl and give it appropriate values
			clrAra[2] = new int[]{(int)pa.random(100,200),(int)pa.random(100,200),(int)pa.random(100,200)};
			//int idx = (int)( pa.random(cnt + 5)-5);
			//ctr =  new myPoint(((int)(cnt % numCols))*offset,((int)(cnt/numCols))*offset,0); 
			//ctr =  new myPoint(ctrVec.x+(orbitRads[ringRadIDX] * pa.sin(rotAngle)), ctrVec.y+(orbitRads[ringRadIDX] * pa.cos(rotAngle)),ctrVec.z);
			//mySphereCntl tmpSph = new mySphereCntl(pa, this, entry, scoreStaffNames[i], pa.sphereRad, ctr, orbitRads[ringRadIDX], rotAngle, 
			mySphereCntl tmpSph = new mySphereCntl(pa, this, entry, scoreStaffNames[i], pa.sphereRad, orbitRads[ringRadIDX], rotAngle, 
					ringRadIDX,
					pa.sphereImgs[cnt%pa.sphereImgs.length],//,pa.sphereImgs[(idx +1 + pa.sphereImgs.length)%pa.sphereImgs.length],pa.sphereImgs[(idx +2 + pa.sphereImgs.length)%pa.sphereImgs.length]},
					clrAra);
			if(scoreStaffNames[i].toLowerCase().contains("Drums".toLowerCase())){
				//pa.outStr2Scr("Drums are set to be a drumkit");
				tmpSph.setFlag(mySphereCntl.isDrumKitIDX, true);
				//tmpSph.isDrumKit=true;
			}
			sphereCntls.put(tmpSph.name, tmpSph);
			cnt++;
			perRingCnt++;
			rotAngle += angleDelta;
		}
		sepDist = 20;
		zMin = -2250;
		zMax = 150;
		zStep = 5.1f;
		numTunnelCircles = (int)((zMax - zMin) / sepDist); 
		tunnelCrcls = new myVector[numTunnelCircles]; 
		for (int i = 0; i < numTunnelCircles; i++) { tunnelCrcls[i] = new myVector(0, 0, PApplet.map(i, 0, numTunnelCircles - 1, zMax, zMin));} 
				
	}
	
	public void setLights(){
		pa.ambientLight(102, 102, 102);
		pa.lightSpecular(204, 204, 204);
		pa.directionalLight(111, 111, 111, 0, 1, -1);	
	}
	
	//overrides function in base class mseClkDisp
	@Override
	public void drawTraj3D(float animTimeMod,myPoint trans){
		//pa.outStr2Scr("mySphereUI.drawTraj3D() : I am overridden in 3d instancing class", true);
		if(!((privFlags[sphereSelIDX]) && (curSelSphere!=""))){return;}
		pa.pushMatrix();pa.pushStyle();	
		pa.translate(0,0,1);
		mySphereCntl tmp = sphereCntls.get(curSelSphere);
		if(null != tmpDrawnTraj){tmp.drawTrajPts(tmpDrawnTraj, animTimeMod);}
		if(privFlags[showTrajIDX]){
		TreeMap<String,ArrayList<myDrawnNoteTraj>> tmpTreeMap = drwnTrajMap.get(curDrnTrajScrIDX);
			if((tmpTreeMap != null) && (tmpTreeMap.size() != 0)) {
				//pa.outStr2Scr("MATCh : cur sel : " + curSelSphere + " cur instr : " + instrs.get(getTrajAraKeyStr(i)).instrName + " key : " + getTrajAraKeyStr(i));
				ArrayList<myDrawnNoteTraj> tmpAra = tmpTreeMap.get(curSelSphere);			
				if(null!=tmpAra){	
					for(int j =0; j<tmpAra.size();++j){tmp.drawTrajPts(tmpAra.get(j),animTimeMod);}
				}	
			}
		}
		pa.popStyle();pa.popMatrix();		
	}//drawTraj3D
	
	//draw tardis tunnel skeeeeeoooooooooo woooeeeooooo weeeoooeeeeee weeeoooo oooo weee oo ooooooo eee ooooo
	private static final float tcYConst = - 3325.0f/ 650.0f;	//precalc constant disp val
	public void drawTunnel(){
		pa.pushMatrix();pa.pushStyle();
		pa.noFill();
		pa.stroke(255,255,255,255);
		pa.strokeWeight(1);
		float fc = (float)pa.frameCount, a, tcScl,r; 
		float tunnelRad = pa.sphereRad * 150;
		for (int i = 0; i < numTunnelCircles; i++) { 
			tunnelCrcls[i].z += zStep; 
			tcScl = PApplet.map((float)(tunnelCrcls[i].z), zMin, zMax, 6,0) * .25f;
			r = PApplet.map((float)(tunnelCrcls[i].z), zMin, zMax, tunnelRad*.01f, tunnelRad); 
			a = PApplet.map((float)(tunnelCrcls[i].z), zMin, zMax, 111,255); 
			tunnelCrcls[i].x = (pa.noise((float)((fc - tunnelCrcls[i].z) / 650.0f) - .5f) * rectDim[2] * tcScl); 
			tunnelCrcls[i].y = (pa.noise((float)((fc + tunnelCrcls[i].z) / 650.0f) - tcYConst) * rectDim[3]* tcScl); 
			pa.stroke(0,a*.5f,a, a); 
			pa.strokeWeight(2.0f);
			pa.pushMatrix(); 
			pa.translate(tunnelCrcls[i].x, tunnelCrcls[i].y, tunnelCrcls[i].z); 
			pa.ellipse(0, 0, r, r); 
			pa.popMatrix(); 
			if (tunnelCrcls[i].z > zMax) {				tunnelCrcls[i].z = zMin;			} 
		} 
		pa.popStyle();pa.popMatrix();		
	}//drawTunnel

	@Override
	protected void drawMe(float animTimeMod) {
		pa.background(0,15,55,255);
		drawTunnel();
		curMseLookVec = pa.c.getMse2DtoMse3DinWorld(pa.sceneCtrVals[pa.sceneIDX]);			//needs to be here - used for determination of whether a sphere is hit or not
		curMseLoc3D = pa.c.getMseLoc(pa.sceneCtrVals[pa.sceneIDX]);
		//pa.outStr2Scr("Current mouse loc in 3D : " + curMseLoc3D.toStrBrf() + "| scenectrvals : " + pa.sceneCtrVals[pa.sceneIDX].toStrBrf() +"| current look-at vector from mouse point : " + curMseLookVec.toStrBrf());
		pa.pushMatrix();pa.pushStyle();
		pa.noLights();
		setLights();
		drawSpheres(animTimeMod);
		pa.noLights();
		pa.lights();
		pa.popStyle();pa.popMatrix();
		if(pa.flags[pa.playMusic]){playAllNotes();}				//need to build better mechanism to be draw-driven instead of 1-shot for play
	}
	
	public void playAllNotes(){
		 SortedMap<Integer, myNote> res = new TreeMap<Integer, myNote> (), tmp;
		// pa.glblOut.pauseNotes();
		 for(mySphereCntl cntl : sphereCntls.values()){
			 if(cntl.notes.size() != 0){cntl.sendNotesToPlayAndStop();}			 
		 }	 	
//		 for(mySphereCntl cntl : sphereCntls.values()){
//			 if(cntl.notes.size() != 0){cntl.sendNotesToStop();}			 
//		 }	 	
	
	//	 pa.glblOut.resumeNotes();
	}
	
	private void drawSpheres(float animTimeMod){	
		//pa.outStr2Scr("anim time mod:"  + animTimeMod);//varies between .01 and .02, mostly .01-.015 - driven by proc time of other functionality, to keep animations smooth
		if(privFlags[sphereSelIDX]){	//a sphere has been fixed for input
			for(mySphereCntl cntl : sphereCntls.values()){
				boolean isCurSelSphere = curSelSphere.equals(cntl.name);
				cntl.drawMe(animTimeMod,isCurSelSphere ? focusZoom : 0, isCurSelSphere);
			}		
		} else {			//no sphere is selected or a selected sphere has been deselected
			myPoint rayPt = pa.P(curMseLoc3D);
			for(mySphereCntl cntl : sphereCntls.values()){					//minimize collision checks
				if(curSelSphere == "") {									//no focus sphere, check if this is one
					double hit = cntl.hitMe(rayPt, curMseLookVec);
					if(hit >=0 ){
						cntl.drawMe(animTimeMod, 0, true);
						curSelSphere = cntl.name;
						curTrajAraIDX = getTrajAraIDXVal(curSelSphere);
					} else {
						cntl.drawMe(animTimeMod, 0, false);
					}
				} else if (curSelSphere.equals(cntl.name)){					//this one is focus sphere, check if still focus
					//check if still focus sphere
					double hit = cntl.hitMe(rayPt, curMseLookVec);
					//pa.outStr2Scr("hit for cntl : " + cntl.ID + " == " + hit);
					if(hit >=0 ){//still focus sphere
						cntl.drawMe(animTimeMod, 0, true);
					} else {//no longer hit, not focus sphere anymore
						cntl.drawMe(animTimeMod, 0, false);
						curSelSphere = "";
						curTrajAraIDX = -1;
					}
				} else {													//there is a focus sphere and this isn't it, draw normal
					cntl.drawMe(animTimeMod, 0, false);	
				}
				
			}
		}		
	}
	//- music is told to start
	@Override
	protected void playMe() {	}//only called 1 time
	//when music stops
	@Override
	protected void stopMe() {
		for(mySphereCntl cntl : sphereCntls.values()){			cntl.stopAllNotes();		}
	}	
	@Override
	//gets current notes for simulation visualization - from last poll time to now - each sphere governs time using radar sweep
	protected SortedMap<Integer, myNote> getNotesNow() {
		 SortedMap<Integer, myNote> res = new TreeMap<Integer, myNote> (), tmp;
		 for(mySphereCntl cntl : sphereCntls.values()){
//			 tmp = cntl.getNotesNow();
//			 res.putAll(tmp);
			 
		 }	 		 
		 return res;
	}//getNotesNow
	
	//print out all trajectories for current sphere
	public void dbgShowTrajLocs(){
		if(null != tmpDrawnTraj){	tmpDrawnTraj.dbgPrintAllPoints();}
		TreeMap<String,ArrayList<myDrawnNoteTraj>> tmpTreeMap = drwnTrajMap.get(curDrnTrajScrIDX);
		if((tmpTreeMap != null) && (tmpTreeMap.size() != 0)) {
			pa.outStr2Scr("cur sel sphere: " + curSelSphere);
			ArrayList<myDrawnNoteTraj> tmpAra = tmpTreeMap.get(curSelSphere);			
			if(null!=tmpAra){	for(int j =0; j<tmpAra.size();++j){tmpAra.get(j).dbgPrintAllPoints();}	}	
		}		
	}
	
	public void dbgNoteDispVals(){		
		for(Map.Entry<Integer,myNote> noteVal : sphereCntls.get(curSelSphere).notes.entrySet()){
			pa.outStr2Scr(noteVal.getValue().toString());
		}
	}
	
	@Override
	public void clickDebug(int btnNum){
		pa.outStr2Scr("click debug in "+name+" : btn : " + btnNum);
		switch(btnNum){
			case 0 : {//note vals
				dbgShowTrajLocs();
				break;
			}
			case 1 : {//measure vals
				dbgNoteDispVals();				
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
	protected void processTrajIndiv(myDrawnNoteTraj drawnNoteTraj){
		pa.outStr2Scr("Process traj in sphere ui");
		sphereCntls.get(curSelSphere).processTrajIndiv(drawnNoteTraj);
		//traj processing
	}
	@Override
	protected myPoint getMouseLoc3D(int mouseX, int mouseY) {
		if((privFlags[sphereSelIDX]) && (curSelSphere!="")){
			return sphereCntls.get(curSelSphere).getMouseLoc3D(mouseX, mouseY);			
		} else {
			return pa.P(-100000,10000,100000);			//dummy point - remove this crap eventually when handle trajs correctly
		}
	}//getMouseLoc3D
	@Override
	protected boolean hndlMouseMoveIndiv(int mouseX, int mouseY, myPoint mseClckInWorld){
		return false;
	}
	//alt key pressed handles trajectory
	//cntl key pressed handles unfocus of spherey
	@Override
	protected boolean hndlMouseClickIndiv(int mouseX, int mouseY, myPoint mseClckInWorld) {
		boolean res = checkUIButtons(mouseX, mouseY);
		if(res) {return res;}
		//pa.outStr2Scr("sphere ui click in world : " + mseClckInWorld.toStrBrf());
		if((!privFlags[sphereSelIDX]) && (curSelSphere!="")){			//set flags to fix sphere
			res = true;
			setPrivFlags(sphereSelIDX,true);			
		} else if((privFlags[sphereSelIDX]) && (curSelSphere!="")){
			if(pa.flags[pa.cntlKeyPressed]){			//cntl+click to deselect a sphere		
				setPrivFlags(sphereSelIDX,false);
				curSelSphere = ""; 
				res = true;
			} else {									//pass click through to selected sphere
				res = sphereCntls.get(curSelSphere).hndlMouseClickIndiv(mouseX, mouseY, mseClckInWorld,curMseLookVec);				
			}
		}
		return res;
	}//hndlMouseClickIndiv

	@Override
	protected boolean hndlMouseDragIndiv(int mouseX, int mouseY, int pmouseX, int pmouseY, myPoint mouseClickIn3D, myVector mseDragInWorld) {
		boolean res = false;
		//pa.outStr2Scr("hndlMouseDragIndiv sphere ui drag in world mouseClickIn3D : " + mouseClickIn3D.toStrBrf() + " mseDragInWorld : " + mseDragInWorld.toStrBrf());
		if((privFlags[sphereSelIDX]) && (curSelSphere!="")){//pass drag through to selected sphere
			//pa.outStr2Scr("sphere ui drag in world mouseClickIn3D : " + mouseClickIn3D.toStrBrf() + " mseDragInWorld : " + mseDragInWorld.toStrBrf());
			res = sphereCntls.get(curSelSphere).hndlMouseDragIndiv(mouseX, mouseY, pmouseX, pmouseY, mouseClickIn3D,curMseLookVec, mseDragInWorld);
		}
		return res;
	}

	@Override
	//set gobal key signature - for score, set it at nearest measure boundary
	protected void setGlobalKeySigValIndiv(int idx, float time){	}//setCurrentKeySigVal
	@Override
	//set time signature at time passed - for score, set it at nearest measure boundary
	protected void setGlobalTimeSigValIndiv(int tsnum, int tsdenom, durType _d, float time){	}//setCurrentTimeSigVal
	@Override
	//set time signature at time passed - for score, set it at nearest measure boundary
	protected void setGlobalTempoValIndiv(float tempo, float time){	}//setCurrentTimeSigVal
	@Override
	//set local at-time key signature, at time passed - for score, set it at nearest measure boundary
	protected void setLocalKeySigValIndiv(myKeySig lclKeySig, ArrayList<nValType> lclKeyNotesAra, float time){}
	@Override
	//set time signature at time passed - for score, set it at nearest measure boundary
	protected void setLocalTimeSigValIndiv(int tsnum, int tsdenom, durType _beatNoteType, float time){}
	@Override
	//set time signature at time passed - for score, set it at nearest measure boundary
	protected void setLocalTempoValIndiv(float tempo, float time){}
	
	@Override
	protected void snapMouseLocs(int oldMouseX, int oldMouseY, int[] newMouseLoc) {}	

	@Override
	protected void hndlMouseRelIndiv() {}
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
	//resize drawn all trajectories
	@Override
	protected void resizeMe(float scale) {}
	@Override
	protected void closeMe() {}
	@Override
	protected void showMe() {}
}

//object representing the spherical UI control
class mySphereCntl {
	public CAProject5 pa;
	public static int sphCntl = 0;
	public int ID;
	
	public AudioOutput noteOut;			//here or instrument?
	
	public mySphereUI win;	//the UI window drawing this sphere cntl	
	public myInstr instr;	//the instrument this control handles
	public String name;
	
	public int[] sphFlags;				//state flags
	public static final int
		debugIDX 		= 0,
		hadFocusIDX 	= 1,			//was in focus but no longer - apparently not used in pre-flag ara days
		isDrumKitIDX	= 2,
		inFocusIDX		= 3;
	
	public static final int numSphFlags = 4;
	
	//graphical components for sphere UI
	//needs texture to map on sphere, and color settings for sphere and display

	public int numNoteRings = 36;	//# of note rings to draw -> changing this will force a change to the drum machine
	public float radius, invRadius,
		ballSpeedMult;					//multiplier for playback time for this instrument, to speed up or slow down ball rolling
	public final float ringRad = 4;			//for ray cast calc, ringrad==radius difference between drawn note rings
	public myPoint ctr, drawCtr;
	public int[] specClr, ambClr, emissiveClr;
	public float shininess = 40.0f,
				 fcsDecay = .99f;				//fcs decay is fraction of displacement for ball when it loses focus
	
	public float orbitRad,						//distance from "sun"
				initThet;						//initial rotation around sun
	public PShape mySphere;
	
	public int UIRingIDX;						//for planetary rotation - which "orbit" does this sphere belong to

	public myMiniSphrCntl[] cntls;				//mini control "moons"
	public myVector panAxis,					//rotation axis to show pan
					rotAxis,					//rotation axis to show rolling
					cntlRotAxis,				//axis to rotate controls around
					hasFocusVec,				//displacement direction toward eye when this sphere has focus
					radarBarEndPt,				//end point in circle space of radar bar (rotated to get motion)
					ntRngDispVec;				//translation vector to draw note ring
	
	public static int volIDX = 0,
					  speedIDX = 1,
					  panIDX = 2;

	public myPoint mseClkDisp;			//displacement to click location on concentric note ring
	public static final myPoint ctrOfNotes = new myPoint(911.7756, 475.4429, -200.0001);			//ctr point in click space of notes - measure distance from this point /ringRad to get rel note value from start note
	public float curFcsZoom, 
				rollAmt,				//for rolling animation
				rotAmt,					//rotation around center				
				curSwpRotAmt,			//for sweeping arc that "plays" notes - 0->2pi
				//curSwpTime,			//for sweeping arc that plays notes - this is st time/duration (curSwpRotAmt * # tics in whole note)
				
				bncAmt;				//for beat bounce
	
	public myVector noteAlphaSt = new myVector(0,-1,0);			//measure angles from straight up from center of sphere
	public static final float noteAlphaWidth = 0.1745329251994329f;//10 degrees//0.0872664625997165f;//5 degrees //0.0174532925199433f; //default here is 1 degree//0.0872664625997165f;	
	
	public TreeMap<Integer, myNote> notes; 						//the notes of this control, keyed by integer subdivisions of 2pi (corresponding to ticks? degrees?)	
	public TreeMap<Integer,ArrayList<myNote>> noteEndLocs;			//list of notes of this control that end at a particular time
	public int[] noteClr;
	
	private int clickOnCntl;
	
	private float lastPlayAlpha;
	private static final float radarBarLen = 72;
	
	//private boolean resendNotes;
	
	public mySphereCntl(CAProject5 _pa, mySphereUI _win, myInstr _instr, String _name, float _rad, /**myPoint _ctr,**/ float _orbitRad, float _initAngle, int _UIRingIDX, PImage _txtr, int[][] _clrs){
		pa = _pa;
		ID = sphCntl++;
		win = _win;
		instr = _instr;
		UIRingIDX = _UIRingIDX;
		name = _name;
		radius = _rad;
		initThet = _initAngle;
		orbitRad = _orbitRad;
		initFlags();
		panAxis = pa.c.getUScrUpInWorld();
		rotAxis = pa.c.getUScrRightInWorld();
		cntlRotAxis = myVector._cross(panAxis, rotAxis)._normalize();
		radarBarEndPt = new myVector();
		invRadius = 1.0f/radius;
		ctr = new myPoint();
		setCtr(initThet, 0);
		//ctr=pa.P(_ctr);
		drawCtr = pa.P(ctr);
		specClr = new int[_clrs[0].length];
		ambClr = new int[_clrs[1].length];
		emissiveClr = new int[_clrs[2].length];
		noteClr = new int[]{0,0,0,255};
		System.arraycopy(_clrs[0],0,specClr,0,_clrs[0].length);
		System.arraycopy(_clrs[1],0,ambClr,0,_clrs[1].length);
		System.arraycopy(_clrs[2],0,emissiveClr,0,_clrs[2].length);
		ballSpeedMult = 20;	//default
		curFcsZoom = 0;
		lastPlayAlpha = -.01f;
		
		cntls = new myMiniSphrCntl[3];
//		horizBIDX = 0,				//whether this control's travel is predominantly horizontal
//		cWiseBIDX = 1,				//whether this control increases in the cwise dir
//		expValBIDX = 2;				//whether this control's value is exponential or linear
//
		
		cntls[0] = new myMiniSphrCntl(pa,this, myInstr.volCntlIDX, new myVector[]{panAxis,rotAxis,cntlRotAxis,new myVector(0,-2.0f*radius,0)},
				"Volume",pa.moonImgs[0], shininess, new float[]{100.0f,0,100.0f,PConstants.QUARTER_PI,PConstants.QUARTER_PI,2.0f*PConstants.THIRD_PI}, 
				new boolean[]{false,false,false}, specClr,  ambClr,  emissiveClr, new int[]{255,255,255,255}, radius*.2f);
		
		cntls[1] = new myMiniSphrCntl(pa,this, -1, new myVector[]{panAxis,rotAxis,cntlRotAxis,new myVector(0,-2.0f*radius,0)},
				"Speed",pa.moonImgs[1], shininess,new float[]{1.0f, 0.001f,2.0f, 3.0f*PConstants.HALF_PI, 4.0f*PConstants.THIRD_PI,7.0f*PConstants.QUARTER_PI}, 
				new boolean[]{false,true,true},specClr,  ambClr,  emissiveClr, new int[]{255,255,255,255}, radius*.2f);
		
		cntls[2] = new myMiniSphrCntl(pa,this,myInstr.panCntlIDX, new myVector[]{panAxis,rotAxis,cntlRotAxis,new myVector(0,-2.0f*radius,0)},
				"Pan",pa.moonImgs[2], shininess,new float[]{0.0f, -1.0f, 1.0f, PConstants.PI, 3.0f*PConstants.QUARTER_PI,5.0f*PConstants.QUARTER_PI}, 
				new boolean[]{true,false,false},specClr,  ambClr,  emissiveClr, new int[]{255,255,255,255}, radius*.2f);
		
				
		mySphere = pa.createShape(PConstants.SPHERE, radius); 
		mySphere.setTexture(_txtr);	
		mySphere.beginShape(PConstants.SPHERE);
		mySphere.noStroke();
		mySphere.ambient(ambClr[0],ambClr[1],ambClr[2]);		
		mySphere.specular(specClr[0],specClr[1],specClr[2]);
		mySphere.emissive(emissiveClr[0],emissiveClr[1],emissiveClr[2]);
		mySphere.shininess(shininess);
		mySphere.endShape(PConstants.CLOSE);
		//set up note structure
		clearNotes();		
		clickOnCntl = -1;
//		resendNotes = true;
		setFlag(isDrumKitIDX, instr.instFlags[myInstr.isDrumTrackIDX]);
		//isDrumKit = instr.instFlags[instr.isDrumTrackIDX];//		drumkit has special layout
	//	if(isDrumKit){pa.outStr2Scr("Made a drum kit sphere : " + ID);}
	}
	//allow rotAngle to vary around some initial amount
	public void setCtr(float rotAngle, float thumpAmt){	
		float orbMult = orbitRad + thumpAmt;
		ctr.set(win.ctrVec.x+(orbMult * PApplet.sin(rotAngle)), win.ctrVec.y+(orbMult * PApplet.cos(rotAngle)),win.ctrVec.z);
	}
	
	//clear all notes for this control
	public void clearNotes(){
		notes = new TreeMap<Integer,myNote>();	
		noteEndLocs = new TreeMap<Integer,ArrayList<myNote>>();		
	}
	
	private void initFlags(){sphFlags = new int[1 + numSphFlags/32]; for(int i = 0; i<numSphFlags; ++i){setFlag(i,false);}}
	public void setFlag(int idx, boolean val){
		int flIDX = idx/32, mask = 1<<(idx%32);
		sphFlags[flIDX] = (val ?  sphFlags[flIDX] | mask : sphFlags[flIDX] & ~mask);
		switch (idx) {//special actions for each flag
			case debugIDX 		: {break;}			//debug mode
			case hadFocusIDX 	: {break;}			//had, but no longer has, focus - shrinking
			case isDrumKitIDX	: {break;}			//this is a drumkit control
			case inFocusIDX		: {break;}			//this control is currently in focus
		}
	}//setFlag	
	public boolean getFlag(int idx){int bitLoc = 1<<(idx%32);return (sphFlags[idx/32] & bitLoc) == bitLoc;}	
	//toggle value of passed flag with given probability
	public void toggleFlag(int idx, double prob){
		//walking = (Math.random() < .950) ? walking : !walking;
		if(Math.random() <= prob){setFlag(idx,!getFlag(idx));}		
	}
  	

	public boolean hndlMouseClickIndiv(int mouseX, int mouseY, myPoint mseClckInWorld, myVector mseClickRayDir){
		boolean mod = false;
		//pa.outStr2Scr("Click in sphere : " + ID + " mseClckInWorld : " + mseClckInWorld.toStrBrf());
		myPoint clickLocInRings = getMouseLoc3D(mouseX, mouseY);
		myVector clickVec =  getClickVec(mouseX, mouseY);
		if(clickLocInRings != null){				
//			pa.outStr2Scr("hndlMouseClickIndiv sphere ID : " + ID + " sphere ui getMouseLoc3D : " + mouseX + ","+mouseY   
////					+  " \tPick Res (clickLoc) : " + clickLoc.toStrBrf() 
////			+ "\n\tClick from ctr : " + clickFromCtr.toStrBrf()+ "  drawCtr : "+ drawCtr.toStrBrf() 
//			+ " Clicked ring :\t" + clickRing 
//			+ "\n\tVec from ring ctr to click :\t" +clickVec.toStrBrf() + " | mseClkDisp :\t"+ mseClkDisp.toStrBrf());// " disp to ctr of sphere " + myPoint._add(ctr,myPoint._add(pa.sceneCtrVals[pa.sceneIDX],pa.focusTar)).toStrBrf() );//" hasFocusVec : " + hasFocusVec.toStrBrf());// + " radar bar end pt : " +radarBarEndPt.toStrBrf()) ; 
		} else { //outside of sweep circle, check if on controlls
//			myPoint clickLoc = getClickLoc(mouseX, mouseY);
//			myPoint	clickFromCtr = myPoint._sub(clickLoc,mseClkDisp);
//			double clickDist = clickVec._mag();
			int clickRing = (int)(clickVec._mag()/(ringRad*.5f));				
			if(clickRing <= 0){//clear out trajectories
				win.clearAllTrajectories();
				clearNotes();
//				pa.outStr2Scr("hndlMouseClickIndiv clear traj sphere ID : " + ID + " sphere ui getMouseLoc3D : " + mouseX + ","+mouseY  + " curTrajAraIDX :" + win.curTrajAraIDX + " instr ID : " + instr.ID
////						+  " \tPick Res (clickLoc) : " + clickLoc.toStrBrf() 
////				+ "\n\tClick from ctr : " + clickFromCtr.toStrBrf()+ "  drawCtr : "+ drawCtr.toStrBrf() 
//				+ " Clicked ring :\t" + clickRing 
//				+ "\n\tVec from ring ctr to click :\t" +clickVec.toStrBrf() + " | mseClkDisp :\t"+ mseClkDisp.toStrBrf());// " disp to ctr of sphere " + myPoint._add(ctr,myPoint._add(pa.sceneCtrVals[pa.sceneIDX],pa.focusTar)).toStrBrf() );//" hasFocusVec : " + hasFocusVec.toStrBrf());// + " radar bar end pt : " +radarBarEndPt.toStrBrf()) ; 
			}
			
			//pa.outStr2Scr("hndlMouseClickIndiv outside note circles sphere ID : " + ID + " sphere ui getMouseLoc3D : " + mouseX + ","+mouseY   +  " \tclickVec : " + clickVec.toStrBrf() );
//			+ "\n\tClick from ctr : " + clickFromCtr.toStrBrf()+ "  drawCtr : "+ drawCtr.toStrBrf() + " Clicked ring :\t" + clickRing 
//			+ "\n\tVec from ring ctr to click :\t" +clickVec.toStrBrf() + " | mseClkDisp :\t"+ mseClkDisp.toStrBrf());// " disp to ctr of sphere " + myPoint._add(ctr,myPoint._add(pa.sceneCtrVals[pa.sceneIDX],pa.focusTar)).toStrBrf() );//" hasFocusVec : " + hasFocusVec.toStrBrf());// + " radar bar end pt : " +radarBarEndPt.toStrBrf()) ; 
			//myVector curMseLookVec = pa.c.getMse2DtoMse3DinWorld(pa.sceneCtrVals[pa.sceneIDX]);
//			myVector noteCtr = new myVector(ctrOfNotes);
			for(int i=0;i<3;++i){		//get rid of these once we have values being set via UI input
				boolean hit = cntls[i].hitMeMini(new myVector(clickVec));
				if(hit){clickOnCntl = i; return true; }
			}
			clickOnCntl = -1;
			//check control, set clickOnCntl  to be idx of control clicked on
		}
		return mod;		
	}

	
	public boolean hndlMouseDragIndiv(int mouseX, int mouseY, int pmouseX, int pmouseY, myPoint mouseClickIn3D, myVector mseClickRayDir, myVector mseDragInWorld) {
		boolean mod = false;
		if(-1 != clickOnCntl){//modify mini control with drag either + or 
			myVector // clickVec =  getClickVec(mouseX, mouseY), oldVec = getClickVec(pmouseX, pmouseY), 
					dispVec = new myVector(mouseX-pmouseX, mouseY-pmouseY, 0);
			//pa.outStr2Scr("Mod obj : " + clickOnCntl + " in sphere: "+ID + " by dispVec : " + dispVec.toStrBrf());// + " mseClickRayDir : " + mseDragInWorld.toStrBrf());
			//cntls[clickOnCntl].modValAmt((clickOnCntl<2?-.5f:1)* modAmt, .75f*PConstants.QUARTER_PI + (clickOnCntl/2)*.25f*PConstants.QUARTER_PI);
			cntls[clickOnCntl].modValAmt((float)dispVec.x *.005f, (float)dispVec.y * -.005f);
			mod = true;
		}			
		return mod;
	}
	
	public void hndlMouseRelIndiv() {
		pa.outStr2Scr("release in sphere : " + ID);
		clickOnCntl = -1;
	}
	protected myPoint getClickLoc(int mouseX, int mouseY){
		float curDepth = 0.5779974f;// need to force this here so that swapping windows with sequencer doesn't screw up pick depth.  
		return pa.c.pick(mouseX, mouseY, curDepth);
	}
	protected myVector getClickVec(int mouseX, int mouseY){
		//float curDepth = 0.5779974f;// need to force this here so that swapping windows with sequencer doesn't screw up pick depth.  
		myPoint clickLoc = getClickLoc(mouseX, mouseY),
				clickFromCtr = myPoint._sub(clickLoc,mseClkDisp);
//		myVector clickVec = new myVector(ctrOfNotes,clickFromCtr);
//		int clickRing = (int)(clickVec._mag()/(ringRad*.5f));						//ring clicked in - corresponds to note
//		pa.outStr2Scr("sphere ID : " + ID + " sphere ui getMouseLoc3D : " + mouseX + ","+mouseY  +  " \tPick Res (clickLoc) : " + clickLoc.toStrBrf()// + " cur depth : " + curDepth
//		+ "\n\tRing disp vec (ntRngDispVec) : " + ntRngDispVec.toStrBrf() + " Click ring : " + clickRing
//		+ "\n\tClick from ctr : " + clickFromCtr.toStrBrf()+ "  drawCtr : "+ mySphereUI.fcsCtr.toStrBrf() 
//		+ "\n\tVec from ring ctr to click :\t" +clickVec.toStrBrf() + " | mseClkDisp :\t"+ mseClkDisp.toStrBrf()		
//		+"\n");// " disp to ctr of sphere " + myPoint._add(ctr,myPoint._add(pa.sceneCtrVals[pa.sceneIDX],pa.focusTar)).toStrBrf() );//" hasFocusVec : " + hasFocusVec.toStrBrf());// + " radar bar end pt : " +radarBarEndPt.toStrBrf()) ; 
		return new myVector(ctrOfNotes,clickFromCtr);
	}
	
	//called by traj drawing routine
	public myPoint getMouseLoc3D(int mouseX, int mouseY) {
		myVector clickVec =  getClickVec(mouseX, mouseY);
		int clickRing = (int)(clickVec._mag()/(ringRad*.5f));		
		if((clickRing >=  numNoteRings) || (clickRing <= 0)) {	return null;}
		return clickVec;	
	}

	//single entrypoint to notes and staff.notesGlblLoc structs
	public myNote putNoteInAra(int stTime, myNote note){
		myNote tmp = notes.put(stTime, note);				//if collision at this time
		int totClicks = getTickFromAlpha(PConstants.TWO_PI);	//total ticks in a circle
		int endTime = (stTime + note.sphereDur)%totClicks;
		ArrayList<myNote> tmpAra = noteEndLocs.get(endTime);
		if(tmpAra==null){			
			tmpAra = new ArrayList<myNote>();
		}
		tmpAra.add(note);
		noteEndLocs.put(endTime, tmpAra);
		//staff.notesGlblLoc.put(note.n.stTime, note);			
		return tmp;
	}	
	//add note at note's alpha
	public void addSphereNote(myNote note, int noteAddTime){
		//p.outStr2Scr("Add sphere Note : " +note.ID+":\t" +note.n.nameOct + "\ttime/dur : "+ note.n.stTime +"/"+note.n.dur +" add Time : " + noteAddTime + "|Meas : seqNum : "+ m.seqNum + " stTime : "+ m.stTime + " endTime : " + m.endTime );
		if(note.flags[myNote.isChord]) { addSphereChord((myChord)note, noteAddTime);}//note : do not attempt to add a chord of rests
			//if same time as another note, make a chord
		myNote tmp = putNoteInAra(noteAddTime, note); 
		if(tmp != null){
			//p.outStr2Scr(" Add Note tmp != null : "+tmp.toString());
			if(!tmp.flags[myNote.isChord]){						//no chord here, make new chord, put in treemap here
				//p.outStr2Scr("Add Note tmp ! chord: Tmp : " +tmp.toString()+":\tNote :" +note.toString());
				if(!tmp.equals(note)){				//if tmp!=note then make tmp a chord, and add note to temp
					//CAProject5 _p, float _alphaSt, float _alphaEnd, int _ring, mySphereCntl _sphrOwn
					myChord tmpChord = new myChord(tmp.p, tmp.sphereDims[0], tmp.sphereDims[1], tmp.sphereRing, tmp.sphrOwn);					
					tmpChord.addNote(note);
					putNoteInAra(noteAddTime, tmpChord);
				}
			} else {											//add note to existing chord if different 
				//p.outStr2Scr("Add Note tmp is chord: Tmp : " +tmp.toString()+":\tNote :" +note.toString());
				((myChord)tmp).addNote(note);
				putNoteInAra(noteAddTime, tmp);					//put chord back in notes map
			}				 
		}
	}//addNote
	
	private void addSphereChord(myChord note, int noteAddTime){
		//p.outStr2Scr("****Add Chord : " +note.ID+":\t" +note.n.nameOct + "\ttime/dur : "+ note.n.stTime +"/"+note.n.dur +" add Time : " + noteAddTime + "|Meas : seqNum : "+ m.seqNum + " stTime : "+ m.stTime + " endTime : " + m.endTime );
		//if same time as another note or chord, add note's notes to make a (bigger) chord
		myNote tmp = putNoteInAra(noteAddTime, note);
		if(tmp != null){
			note.addNote(tmp);
			if(tmp.flags[myNote.isChord]){										//tmp is a chord, add tmps notes to note's chord
				tmp.flags[myNote.isChord] = false;
				myChord tmpC = ((myChord)tmp);
				for(int i = 1; i<tmpC.cnotes.size(); ++i){note.addNote(tmpC.cnotes.get(i));}
			}
		}		
	}//addChord

	public void addDrawnNotesToStruct(boolean clearNotes, ArrayList<myNote> drawnNotes){
		if(clearNotes){clearNotes();}
		for(int i =0; i<drawnNotes.size();++i){	myNote n = drawnNotes.get(i); addSphereNote(n, getTickFromAlpha(n.sphereAlpha));}
	}	
	
	
	//return integer key for notes for map - degrees? 
	public int getTickFromAlpha(float alpha){ return (int)(alpha * (PConstants.RAD_TO_DEG/360.0f) * 4 * durType.Whole.getVal());}		//convert from alpha to ticks - one whole circle is 4 whole notes
	//return the note corresponding to the passed point
	public myNote getNoteFromSphereLoc(myPoint pt){
		myVector ptDirCtrVec = new myVector(pt);
		ptDirCtrVec._normalize();
		double clickDist = pa.P()._dist(pt);
		int clickRing = (int)(clickDist/(ringRad*.5f));						//ring clicked in - corresponds to note
		if(clickDist > .5*(ringRad * (numNoteRings+1))){
			pa.outStr2Scr("Bad Note value - outside ring bounds : " + clickRing + " loc : " + pt.toStrBrf());
			return null;
		}	
		float alphaSt = (float)pa.angle(noteAlphaSt, ptDirCtrVec); 	//measure angles from straight up from center of sphere
		//pa.outStr2Scr("getNoteFromSphereLoc sphere ID : " + ID + " pt's ring loc :\t" + clickRing + "\tPoint in Traj :\t" +pt.toStrBrf() +"\n");// " disp to ctr of sphere " + myPoint._add(ctr,myPoint._add(pa.sceneCtrVals[pa.sceneIDX],pa.focusTar)).toStrBrf() ); 
		myNote res = new myNote(pa, alphaSt, (alphaSt + noteAlphaWidth), clickRing, this);		
		//pa.outStr2Scr("New sphere note : " + res.toString());
		return res;
	}
	
	//return the drum sample "note" corresponding to the passed point
	public myNote getDrumNoteFromSphereLoc(myPoint pt){
		myVector ptDirCtrVec = new myVector(pt);
		ptDirCtrVec._normalize();
		double clickDist = pa.P()._dist(pt); 
		int noteSphereRing = (int)(clickDist/(ringRad*.5f));
		int clickRing = 36 -(4*((36-noteSphereRing)/4));						//ring clicked in - corresponds to note
		//pa.outStr2Scr("click ring in getDrum note : " + clickRing + "  original noteSphrRing : " + noteSphereRing );
		if(clickDist > .5*(ringRad * (numNoteRings+1))){
			pa.outStr2Scr("Bad Note value - outside ring bounds : " + clickRing + " loc : " + pt.toStrBrf());
			return null;
		}	
		float alphaSt = (float)pa.angle(noteAlphaSt, ptDirCtrVec); 	//measure angles from straight up from center of sphere
		//pa.outStr2Scr("getNoteFromSphereLoc sphere ID : " + ID + " pt's ring loc :\t" + clickRing + "\tPoint in Traj :\t" +pt.toStrBrf() +"\n");// " disp to ctr of sphere " + myPoint._add(ctr,myPoint._add(pa.sceneCtrVals[pa.sceneIDX],pa.focusTar)).toStrBrf() ); 
		myNote res = new myNote(pa, alphaSt, (alphaSt + noteAlphaWidth), clickRing, this);		
		//pa.outStr2Scr("New sphere note : " + res.toString());
		return res;
	}
			
	private void convTrajToDrumBeats(myDrawnNoteTraj drawnNoteTraj){
		myPoint[] pts = ((myVariStroke)drawnNoteTraj.drawnTraj).getDrawnPtAra(false);
		ArrayList<myNote> tmpDrawnSphereNotes = new ArrayList<myNote>();										//new trajectory of notes to play
		myNote newDrumNote,lastDrumNote = null;
		boolean checkedFirstNote = false;		
		for(int i=0; i< pts.length;++i){
			newDrumNote = getDrumNoteFromSphereLoc(pts[i]);
			if (newDrumNote == null){continue;}
			if(!checkedFirstNote){					//first note of trajectory
				checkedFirstNote = true;
				tmpDrawnSphereNotes.add(newDrumNote);
				lastDrumNote = newDrumNote;
			} else {		//check to make note of longer duration - if same note and new note extends past old note, then add duration to old note
				if((newDrumNote.sphereRing == lastDrumNote.sphereRing) && (newDrumNote.sphereDims[1] > lastDrumNote.sphereDims[1])){	//new note lasts longer than old note, add difference
					lastDrumNote.addDurationSphere(newDrumNote.sphereDims[1] - lastDrumNote.sphereDims[1]);						
				} else if (newDrumNote.sphereRing != lastDrumNote.sphereRing){
					lastDrumNote.setSphereDims(lastDrumNote.sphereDims[0], newDrumNote.sphereAlpha, lastDrumNote.sphereRing);
					tmpDrawnSphereNotes.add(newDrumNote);					
					lastDrumNote = newDrumNote;
				} else {//equal - just ignore
				}
			}			
		}//for each point
		addDrawnNotesToStruct(pa.flags[pa.clearStaffNewTraj],tmpDrawnSphereNotes);
	}
	private void convTrajToNotes(myDrawnNoteTraj drawnNoteTraj){
		myPoint[] pts = ((myVariStroke)drawnNoteTraj.drawnTraj).getDrawnPtAra(false);
		//TreeMap<Integer,myNote> tmpdrawnStaffNotes = new TreeMap<Integer,myNote>();		
		ArrayList<myNote> tmpDrawnSphereNotes = new ArrayList<myNote>();										//new trajectory of notes to play
		myNote newClickNote,lastNewNote = null;
		boolean checkedFirstNote = false;		
		
		for(int i=0; i< pts.length;++i){
			newClickNote = getNoteFromSphereLoc(pts[i]);
			if (newClickNote == null){continue;}
			if(!checkedFirstNote){					//first note of trajectory
				checkedFirstNote = true;
				tmpDrawnSphereNotes.add(newClickNote);
				lastNewNote = newClickNote;
			} else {		//check to make note of longer duration - if same note and new note extends past old note, then add duration to old note
				if((newClickNote.sphereRing == lastNewNote.sphereRing) && (newClickNote.sphereDims[1] > lastNewNote.sphereDims[1])){	//new note lasts longer than old note, add difference
					lastNewNote.addDurationSphere(newClickNote.sphereDims[1] - lastNewNote.sphereDims[1]);						
				} else if (newClickNote.sphereRing != lastNewNote.sphereRing){
					lastNewNote.setSphereDims(lastNewNote.sphereDims[0], newClickNote.sphereAlpha, lastNewNote.sphereRing);
					tmpDrawnSphereNotes.add(newClickNote);					
					lastNewNote = newClickNote;
				} else {//equal - just ignore
				}
			}			
		}//for each point
		addDrawnNotesToStruct(pa.flags[pa.clearStaffNewTraj],tmpDrawnSphereNotes);
	}//convTrajToNotes	
	
	//convert points in drawnNoteTraj to notes : convert traj notes to actual notes on sphere 
	public void processTrajIndiv(myDrawnNoteTraj drawnNoteTraj){	
		if(getFlag(isDrumKitIDX)){	convTrajToDrumBeats(drawnNoteTraj);	} 
		else {				convTrajToNotes(drawnNoteTraj);}
	}
	
	//tick is in seconds
	private float totTick = 0.0f;
	public void calcDispVals(float tick, float focusZoom){
		totTick += tick;
		//pa.outStr2Scr("tot tick : " + totTick);
		//setCtr(initThet + (pa.sin(totTick*1000.0f/orbitRad) * 100.0f/orbitRad));//oscillates 
		setCtr(initThet + (totTick * 100.0f/orbitRad), .1f * orbitRad * PApplet.sin(2.0f * cntls[speedIDX].vals[myMiniSphrCntl.valIDX]*totTick + orbitRad ));			//modify to use tempo in place of totTickMult
		
		lastPlayAlpha = curSwpRotAmt;
		curSwpRotAmt += tick * cntls[speedIDX].vals[myMiniSphrCntl.valIDX];		//value of speed of sweep
		curSwpRotAmt = curSwpRotAmt % PConstants.TWO_PI;
		//pa.outStr2Scr("Cur swp rot amt : " + curSwpRotAmt);
		if(lastPlayAlpha > curSwpRotAmt){
			lastPlayAlpha -= PConstants.TWO_PI;
		}
		
		hasFocusVec = pa.V(ctr,mySphereUI.fcsCtr);							//	 unit vector toward eye from non-displaced ctr	
		ntRngDispVec = myVector._mult(cntlRotAxis, 1.1f*radius );
		rollAmt += tick * ballSpeedMult;		
		if(focusZoom == 0){//not zooming any more - either decay or never had focus
			radarBarEndPt.set(0,-50,0);			//sweeping radar bar showing notes that are playing
			curFcsZoom *= fcsDecay;	
			if(curFcsZoom < .000001f){
				curFcsZoom = 0;
				myVector tmp = pa.U(pa.U(hasFocusVec),.5f,cntlRotAxis);					//vector 1/2 way between these two unit vectors
				ntRngDispVec = myVector._mult(tmp, 1.1f*radius );		
				drawCtr.set(ctr);
			} else {
				drawCtr = myPoint._add(ctr, hasFocusVec._mult(curFcsZoom));	
			}
		} 
		else {
			curFcsZoom = focusZoom;
			radarBarEndPt.set(0,-(50+(curFcsZoom * (radarBarLen-50))),0);			//sweeping radar bar showing notes that are playing
			drawCtr = myPoint._add(ctr, hasFocusVec._mult(curFcsZoom));				
		}
//		radarBarEndPt = new myVector(0,-(50+(focusZoom * (radarBarLen-50))),0);			//sweeping radar bar showing notes that are playing
		//drawCtr = myPoint._add(ctr, hasFocusVec._mult(curFcsZoom));	
		//mseClkDisp = myPoint._add(myPoint._add(ntRngDispVec,drawCtr), pa.sceneCtrVals[pa.sceneIDX]);
		mseClkDisp = myPoint._add(myPoint._add(ntRngDispVec,drawCtr), myPoint._add(pa.sceneCtrVals[pa.sceneIDX],pa.focusTar));
		//mseClkDisp = myPoint._add(ntRngDispVec,myPoint._add(pa.sceneCtrVals[pa.sceneIDX],pa.focusTar));
	}

	public void sendNotesToPlayAndStop(){	
		float durMod = noteAlphaWidth/ (3.0f*cntls[speedIDX].vals[myMiniSphrCntl.valIDX]);//durMod = getTickFromAlpha(noteAlphaWidth) / 1.1f*cntls[speedIDX].value;
		//get all notes from last play alpha
		int fromKey = getTickFromAlpha(lastPlayAlpha), toKey = getTickFromAlpha(curSwpRotAmt);
		SortedMap<Integer,myNote> subMapPlay = notes.subMap(fromKey, toKey);
		SortedMap<Integer,ArrayList<myNote>> subMapStop = noteEndLocs.subMap(fromKey, toKey);
		//if((subMapPlay == null) || (subMapPlay.size() <= 0)){return;}
		int stTime = 0;
		if(!((subMapPlay == null) || (subMapPlay.size() <= 0))){
			//pa.outStr2Scr("ID : " + ID + " play Submap has notes : Cur swp rot amt : " + curSwpRotAmt + " last play alpha : " + lastPlayAlpha + " from key, to key : " + fromKey + ","+toKey);
			win.addSphereNoteToPlayNow(instr,subMapPlay, durMod,stTime);
		}
		if(!((subMapStop == null) || (subMapStop.size() <= 0))){
//			/pa.outStr2Scr("ID : " + ID + " stop Submap has notes : Cur swp rot amt : " + curSwpRotAmt + " last play alpha : " + lastPlayAlpha + " from key, to key : " + fromKey + ","+toKey);
			win.addSphereNoteToStopNow(instr,subMapStop);
		}
	}
//	//stop all  notes that end in this period
//	public void sendNotesToStop(){
//		int fromKey = getTickFromAlpha(lastPlayAlpha), toKey = getTickFromAlpha(curSwpRotAmt);		
//		SortedMap<Integer,ArrayList<myNote>> subMap = noteEndLocs.subMap(fromKey, toKey);
//		if((subMap == null) || (subMap.size() <= 0)){return;}
//		pa.outStr2Scr("ID : " + ID + " stop Submap has notes : Cur swp rot amt : " + curSwpRotAmt + " last play alpha : " + lastPlayAlpha + " from key, to key : " + fromKey + ","+toKey);
//		win.addSphereNoteToStopNow(instr,subMap);
//	}
	

	//animate and draw this instrument in sphere UI
//	public void drawMe(float tick, float focusZoom){
//		calcDispVals(tick,focusZoom);
//		pa.pushMatrix();pa.pushStyle();
//			pa.translate(drawCtr);		//either ctr or displaced position
//			drawMainSphere();
//			pa.pushMatrix();pa.pushStyle();
//				pa.translate(0,(isFocus ? 1.5f : 0)*radius,(isFocus ? 0 : 1.5f)*radius);
//				drawName();
//			pa.popStyle();pa.popMatrix();	
//			if(isFocus){drawNoteCircle();}
//			drawAllNotes();
//			drawNotePlayBar();
//			for(int i=0;i<3;++i){
//				cntls[i].drawMini(isFocus);
//			}
//
//		pa.popStyle();pa.popMatrix();	
//	}
	public void drawMe(float tick, float focusZoom, boolean hasFocus){
		calcDispVals(tick,focusZoom);
		pa.pushMatrix();pa.pushStyle();
			pa.translate(drawCtr);		//either ctr or displaced position
			drawMainSphere();
			if(hasFocus){drawMeFocus(tick,focusZoom);} else {drawMeNoFocus(tick,focusZoom);}
		pa.popStyle();pa.popMatrix();	
	}
	//draw for focus
	public void drawMeFocus (float tick, float focusZoom){
		pa.pushMatrix();pa.pushStyle();
			pa.translate(0,1.5f *radius,0);
			drawName();
		pa.popStyle();pa.popMatrix();	
		drawNoteCircle();
		drawAllNotes();
		drawNotePlayBar();
		for(int i=0;i<3;++i){				cntls[i].drawMiniLocked();			}
	}//
	
	public void drawMeNoFocus(float tick, float focusZoom){
		pa.pushMatrix();pa.pushStyle();
			pa.translate(0,0, radius);
			drawName();
		pa.popStyle();pa.popMatrix();	
		drawAllNotes();
		drawNotePlayBar();
		for(int i=0;i<3;++i){				cntls[i].drawMini();			}
	}//drawMeNoFocus
	public void stopAllNotes(){
		win.addSphereNoteToStopNow(instr,noteEndLocs);
	}
	
	public void drawAllNotes(){
		//draw all notes
		pa.pushMatrix();pa.pushStyle();
			pa.translate(ntRngDispVec.x,ntRngDispVec.y,ntRngDispVec.z+.01f);			
			for(Map.Entry<Integer,myNote> noteVal : notes.entrySet()){
				myNote note = noteVal.getValue();
				note.drawMeSphere();
			}
		pa.popStyle();pa.popMatrix();	
	}
	
	public void drawTrajPts(myDrawnNoteTraj traj, float animTimeMod){
		pa.pushMatrix();pa.pushStyle();
			pa.translate(drawCtr);		//either ctr or displaced position		
			pa.translate(ntRngDispVec.x,ntRngDispVec.y,ntRngDispVec.z-1.0f);
			traj.drawMe( animTimeMod);
		pa.popStyle();pa.popMatrix();	
	}
	
	//draw concentric rings of notes "staff"
	public void drawNoteCircle(){
		pa.pushMatrix();pa.pushStyle();
		pa.fill(255,255,255,5);
		pa.stroke(0,0,0,255);
		pa.strokeWeight(1);
		pa.translate(ntRngDispVec);
		//note rings - fewer & wider for drums
		int ringStep = getFlag(isDrumKitIDX) ? 4 : 1;
		for(int i =0; i<numNoteRings; i+=ringStep){	pa.circle(myPoint.ZEROPT, (i+1)*ringRad);	}		
		pa.popStyle();pa.popMatrix();			
	}//drawNoteCircle()
	
	public void drawNotePlayBar(){
		pa.pushMatrix();pa.pushStyle();
		pa.fill(255,255,255,5);
		pa.translate(ntRngDispVec.x,ntRngDispVec.y,ntRngDispVec.z-.1f);
		pa.rotate(lastPlayAlpha, cntlRotAxis);
		//pa.rotate(curSwpRotAmt, cntlRotAxis);
		pa.stroke(0,0,0,255);
		pa.strokeWeight(3);
		pa.line(myPoint.ZEROPT, radarBarEndPt);
		pa.stroke(255,255,0,255);
		pa.strokeWeight(1.5f);
		pa.line(myPoint.ZEROPT, radarBarEndPt);
		pa.stroke(255,0,0,255);
		pa.strokeWeight(1);
		pa.line(myPoint.ZEROPT, radarBarEndPt);
		pa.translate(pa.P(radarBarEndPt)._mult(1.1));
		pa.rotate(-lastPlayAlpha, cntlRotAxis);
		//pa.rotate(-curSwpRotAmt, cntlRotAxis);
		pa.setColorValFill(CAProject5.gui_Black);
		
		//pa.text(""+curSwpRotAmt, 0, -0);
		pa.popStyle();pa.popMatrix();			
	}	
		
	private void drawName(){
		pa.setColorValFill(CAProject5.gui_DarkGray);
		pa.translate(-2*name.length(),0,0);
		pa.rect(-10,-11,name.length()*8 + 10,15);
		pa.setColorValFill(CAProject5.gui_White);
		pa.text(name, 0, 0);	
	}
	
	private void drawMainSphere(){
		pa.pushMatrix();pa.pushStyle();
		//pa.stroke(255,0,0,255);
		//pa.line(myPoint.ZEROPT, pa.P(panAxis)._mult(100));		//axis of rot lines
		pa.rotate(cntls[panIDX].vals[myMiniSphrCntl.rotAmtIDX],panAxis);
		//pa.stroke(111,0,110,255);
		//pa.line(myPoint.ZEROPT, pa.P(rotAxis)._mult(100));		//axis of rot lines
		pa.rotate(-rollAmt, rotAxis);
		pa.shape(mySphere);
		pa.popStyle();pa.popMatrix();			
	}
//	//return distance along pt + unit ray where this ray hits target note disc
	public double hitNoteDisc(myPoint pt, myVector ray){
		return (pa.V(pt,ctrOfNotes)._dot(cntlRotAxis))/(ray._dot(cntlRotAxis));	
	}
	
	public double hitMe(myPoint pt, myVector ray){return hitMe(pt,ray, ctr, invRadius);	}	 
	//returns value >= 0  if the passed point in the direction of the ray hits this sphere
	public double hitMe(myPoint pt, myVector ray, myPoint tarCtr, float invRad){
		double tVal = -1;
	    myVector pC = new myVector(tarCtr, pt)._mult(invRad), scRay = myVector._mult(ray, invRad);
	    double a = (scRay._SqMag()),
	    		b = (2*scRay._dot(pC)), 
	    		c = (pC._SqMag() - 1),
	    		discr = ((b*b) - (4*a*c));
	    if (!(discr < 0)){
	    	//find values of t - want those that give largest value of z, to indicate the closest intersection to the eye
	    	//quadratic equation
	    	double discr1 = Math.pow(discr,.5), t1 = (-b + discr1)/(2*a), t2 = (-b - discr1)/(2*a);
	    	tVal = Math.min(t1,t2);
	        if (tVal < pa.feps){//if min less than 0 then that means it intersects behind the viewer.  pick other t
	        	tVal = Math.max(t1,t2);
	        	if (tVal < pa.feps){tVal = -1;}//if both t's are less than 0 then don't paint anything
	        }//if the min t val is less than 0
	    }		
		return tVal;		
	}//hitme
	
	public String toString(){
		String res = "ID:"+ID+" Instr : " + instr.instrName + " ctr : " + ctr.toStrBrf();
		return res;		
	}
	
}//class mySphereCntl

class myMiniSphrCntl{
	public CAProject5 pa;
	public static int sphMiniCntl = 0;
	public int ID;	
	public mySphereCntl own;
	
	public String name;
	public PShape sh;
	public int[] txtFillClr;
	
	public int instrCntlIDX;			//the instrument control this control controls. controllingly.
	
	//public myPoint ctr, drawCtr;
		
	public myVector panAxis,			//rotation axis to show pan
					rotAxis,			//rotation axis to show rolling
					distFromCtr,
					cntlRotAxis,		//axis to rotate controls around
					rayCastDotVec;		//axis to dot against for modifying control - should be horizontal for side-to-side controls, and vertical for up and down controls
	
	public float[] vals;
	public static final int 
					valIDX 		= 0,	//amount represented by mini sphere
					minAmtIDX 	= 1,	//min value this cntl can take
					maxAmtIDX 	= 2,	//max value this cntl can take
					rotAmtIDX 	= 3,	//rotation for display			
					minRAmtIDX 	= 4,	//min rot value this cntl can take
					maxRAmtIDX 	= 5;	//max rot value this cntl can take
	public static final int numVals = 6;
	
	
	public float radius, invRadius;
	public boolean[] mSphrFlags;
	public static final int 
				horizBIDX = 0,				//whether this control's travel is predominantly horizontal
				cWiseBIDX = 1,				//whether this control increases in the cwise dir
				expValBIDX = 2;				//whether this control's value is exponential or linear
	public static final int numBFlags = 3;
	
	
	public myMiniSphrCntl(CAProject5 _pa, mySphereCntl _o, int _instCntlIDX, myVector[] _cntlAxis,
			String _name,PImage _txtr, float _shn, float[] _vals, boolean[] _flags,
			int[] specClr, int[] ambClr, int[] emissiveClr, int[] _txtFl, float _radius){
		pa = _pa;
		own = _o;
		ID = sphMiniCntl++;
		panAxis = new myVector();
		rotAxis = new myVector();
		cntlRotAxis = new myVector();
		distFromCtr = new myVector();
		panAxis.set(_cntlAxis[0]);				//axis that globe rotates around to reflect pan
		rotAxis.set(_cntlAxis[1]);				//axis that globe "rolls" around
		cntlRotAxis.set(_cntlAxis[2]);			//axis that controls rotate around
		distFromCtr.set(_cntlAxis[3]);
		initBFlags(_flags);
		instrCntlIDX = _instCntlIDX;
		rayCastDotVec = new myVector();
		//rayCastDotVec.set((ID == 2 ? pa.P(1,0,0) : pa.P(0,1,0)));
		rayCastDotVec.set((mSphrFlags[horizBIDX] ? rotAxis : panAxis));		
		if(!mSphrFlags[horizBIDX]){rayCastDotVec._mult(-1);}
		rayCastDotVec._normalize();
		txtFillClr = _txtFl;
		radius = _radius;
		invRadius = 1.0f/radius;
		name = _name;
		sh = pa.createShape(PConstants.SPHERE, radius); 
		sh.setTexture(_txtr);	
		sh.beginShape(PConstants.SPHERE);
		sh.noStroke();
		sh.ambient(ambClr[0],ambClr[1],ambClr[2]);		
		sh.specular(specClr[0],specClr[1],specClr[2]);
		sh.emissive(emissiveClr[0]*2,emissiveClr[1]*2,emissiveClr[2]*2);
		sh.shininess(_shn);
		sh.endShape(PConstants.CLOSE);
		initVals(_vals);
	}
	
	protected void initBFlags(boolean[] _bflags){mSphrFlags = new boolean[numBFlags];for(int i=0;i<numBFlags;++i){mSphrFlags[i] = _bflags[i];}}
	//set up value array
	protected void initVals(float [] _vals){
		vals = new float[numVals];
		for(int i=0; i<numVals; ++i){vals[i]=_vals[i];}
	}
	
	public myVector getDispVec(){
		double mag = distFromCtr._mag();
		myVector disp = myVector._rotAroundAxis(pa.U(distFromCtr), cntlRotAxis, vals[rotAmtIDX]);
		disp._mult(mag);
		//disp.z = 0;
		return disp;
	}
	
	public boolean hitMeMini(myVector clickVec){
		myVector disp= getDispVec();
		myPoint tmp = pa.P(clickVec);
		tmp.z = 0;
		double dist = tmp._dist(disp);
		//pa.outStr2Scr("Checking Hit in mini sphere ID : " + ID + " disp val (target ctr) : " + disp.toStrBrf() + " click vec : " + clickVec.toStrBrf() + " Dist from sphere : " + dist);
		return (dist < 30);
	}
	
	public void drawMiniLocked(){
		pa.pushMatrix();pa.pushStyle();	
		pa.rotate(vals[rotAmtIDX], cntlRotAxis);	
		pa.translate(distFromCtr);
		pa.pushMatrix();pa.pushStyle();	
			pa.rotate(-vals[rotAmtIDX], distFromCtr);	
			pa.shape(sh);									//main sphere
		pa.popStyle();pa.popMatrix();
		pa.setFill(txtFillClr);
		pa.rotate(-vals[rotAmtIDX], cntlRotAxis);	
		pa.pushMatrix();pa.pushStyle();	
			pa.translate(-4,1,10);	
			pa.scale(.6f,.6f,.6f);
			pa.text(String.format("%.2f",vals[valIDX]), 0,0);			//text of value
		pa.popStyle();pa.popMatrix();
		pa.fill(255,255,255,255);
		pa.translate(myVector._mult(distFromCtr,.25));
		pa.translate(-.5f*5*name.length(),-8,0);			//text of name of sphere
		pa.text(name, 0,0);
		pa.popStyle();pa.popMatrix();
	}

	public void drawMini(){
		pa.pushMatrix();pa.pushStyle();	
		pa.rotate(vals[rotAmtIDX], cntlRotAxis);	
		pa.translate(distFromCtr);
		pa.pushMatrix();pa.pushStyle();	
			pa.rotate(-vals[rotAmtIDX], distFromCtr);	
			pa.shape(sh);									//main sphere
		pa.popStyle();pa.popMatrix();
		pa.popStyle();pa.popMatrix();
	}
	//take in an incrementing value, return a rotated value
	//protected float getRotAmt(float val, float tick, float mult){return (PApplet.cos(val + tick) * mult);}
	protected float getLinInterp(float val){return  (val - vals[minAmtIDX])/(vals[maxAmtIDX] - vals[minAmtIDX]);}
	protected float getRotInterp(float val){return  (val - vals[minRAmtIDX])/(vals[maxRAmtIDX] - vals[minRAmtIDX]);}
	protected float getLinVal(float interp){float newVal = vals[minAmtIDX] + interp *(vals[maxAmtIDX] - vals[minAmtIDX]);  return bndLinVal(newVal);}
	protected float getRotVal(float interp){float newVal = vals[minRAmtIDX] + interp *(vals[maxRAmtIDX] - vals[minRAmtIDX]); return bndRotVal(newVal);}
	protected float bndLinVal(float val){return PApplet.min(vals[maxAmtIDX], PApplet.max(vals[minAmtIDX],val));}
	protected float bndRotVal(float val){return PApplet.min(vals[maxRAmtIDX], PApplet.max(vals[minRAmtIDX],val));}
	//return the actual value, not the stored value (if exp then act value is stored value sqred)
	public float getValue(){
		return (mSphrFlags[expValBIDX] ? vals[valIDX] * vals[valIDX] : vals[valIDX]);		
	}
	//sets value to be either passed val or its sqrt - using this to be able to represent exponential values with linear faders
	public void setValue(float _val){
		vals[valIDX] = bndLinVal(mSphrFlags[expValBIDX] ? (float)Math.sqrt(_val) : _val);
		recalcRotAmt();
	}
	
	//take existing value, map between min and max, and 
	public void recalcRotAmt(){
		float lItrp = getLinInterp(vals[valIDX]);
		vals[rotAmtIDX] = getRotVal((mSphrFlags[cWiseBIDX] ? lItrp : 1.0f-lItrp));
	}
	
	public boolean modValAmt(float tickX, float tickY){
		float tickToUse = (mSphrFlags[horizBIDX] ? tickX : tickY);
		if(PApplet.abs(tickToUse) < pa.feps){return false;}
		//pa.outStr2Scr("In mini sphere :  valIncr : " + valIncr + " tick : " + tick);
		float lItrp = getLinInterp(vals[valIDX]);
		vals[valIDX] = getLinVal(lItrp + tickToUse);		
		recalcRotAmt();
		//if((own.ID == 0) &&(ID ==1)){pa.outStr2Scr("Speed value : " + value);}	
		if(instrCntlIDX != -1){	own.instr.setInstCntlVals(instrCntlIDX, vals[valIDX], null);}
		return true;
	}
	
}//myMiniSphrCntl
