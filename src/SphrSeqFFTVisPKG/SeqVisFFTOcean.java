package SphrSeqFFTVisPKG;

import java.awt.event.KeyEvent;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.*;

import com.jogamp.newt.opengl.GLWindow;

import SphrSeqFFTVisPKG.clef.myClef;
import SphrSeqFFTVisPKG.clef.myGrandClef;
import SphrSeqFFTVisPKG.clef.base.myClefBase;
import SphrSeqFFTVisPKG.clef.enums.clefType;
import SphrSeqFFTVisPKG.instrument.myInstrument;
import SphrSeqFFTVisPKG.note.NoteData;
import SphrSeqFFTVisPKG.note.myNote;
import SphrSeqFFTVisPKG.note.enums.chordType;
import SphrSeqFFTVisPKG.note.enums.durType;
import SphrSeqFFTVisPKG.note.enums.noteValType;
import SphrSeqFFTVisPKG.score.myScore;
import SphrSeqFFTVisPKG.ui.myInstEditWindow;
import SphrSeqFFTVisPKG.ui.mySequencerWindow;
import SphrSeqFFTVisPKG.ui.mySimWindow;
import SphrSeqFFTVisPKG.ui.mySphereWindow;
import SphrSeqFFTVisPKG.ui.base.myMusicSimWindow;
import base_JavaProjTools_IRender.base_Render_Interface.IRenderInterface;
import base_Math_Objects.MyMathUtils;
import base_Math_Objects.vectorObjs.doubles.myPoint;
import base_Math_Objects.vectorObjs.doubles.myVector;
import base_Math_Objects.vectorObjs.floats.myPointf;
import base_Math_Objects.vectorObjs.floats.myVectorf;
import base_UI_Objects.windowUI.base.myDispWindow;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PImage;
import processing.core.PMatrix3D;
import processing.event.MouseEvent;
import processing.opengl.PGL;
import processing.opengl.PGraphics3D;
import processing.opengl.PGraphicsOpenGL;

import ddf.minim.*;
import ddf.minim.ugens.*;

/**
 * Project 5 Music visualization - full-fledged sequencer integrated with fft fluid visualization
 * 
 * John Turner
 * 
 */
 public class SeqVisFFTOcean extends PApplet implements IRenderInterface{
	//project-specific variables
	public String prjNmLong = "Interactive Sequencer/Visualization with FFT Ocean", prjNmShrt = "SeqVisFFTOcean";
	PImage jtFace; 
	
	public final int drawnTrajEditWidth = 10; //TODO make ui component			//width in cntl points of the amount of the drawn trajectory deformed by dragging
	public final float
				InstEditWinYMult = .75f,							//percentage of screen that InstEdit window is displayed when open
				wScale = frameRate/5.0f,					//velocity drag scaling	
				trajDragScaleAmt = 100.0f;					//amt of displacement when dragging drawn trajectory to edit
			
	public String msClkStr = "";
	
	public int glblStartPlayTime, glblLastPlayTime;
	
	/**
	 * Precalculated cosine and sine values
	 */
	private double[] cylCosVals, cylSinVals;
	//constant values defined for cylinder wall angles
	private final float deltaThet = MyMathUtils.TWO_PI_F/36.0f, 
		finalThet = MyMathUtils.TWO_PI_F+deltaThet;

		
	//CODE STARTS
	public static void main(String[] passedArgs) {		
		String[] appletArgs = new String[] { "SphrSeqFFTVisPKG.SeqVisFFTOcean" };
			if (passedArgs != null) {
				PApplet.main(PApplet.concat(appletArgs, passedArgs));
			} else {
				PApplet.main(appletArgs);
			}
	}//main
	
	private final float maxWinRatio =  1.77777778f;
	public void settings(){	
		float displayRatio = displayWidth/(1.0f*displayHeight);
		float newWidth = (displayRatio > maxWinRatio) ?  displayWidth * maxWinRatio/displayRatio : displayWidth;
		size((int)(newWidth*.95f), (int)(displayHeight*.92f), P3D);
	}
	public void setup(){
		//precalc cylinder cosine and sine vals
		cylCosVals = new double[38];
		cylSinVals = new double[38];
		int i=0;
		for(float a=0; a<=finalThet; a+=deltaThet) {
			cylCosVals[i] = Math.cos(a);
			cylSinVals[i++] = Math.sin(a);
		}

		noSmooth();
		initOnce();
		background(bground[0],bground[1],bground[2],bground[3]);		
	}//setup	
	
	//called once at start of program
	public void initOnce(){
		initVisOnce();						//always first
		
		hSrsMult = new float[numHarms];
		for(int i=0;i<numHarms;++i){hSrsMult[i] = 1.0f/(i+1); }
		sceneIDX = 1;//(flags[show3D] ? 1 : 0);
		glblStartPlayTime = millis() ;
		glblLastPlayTime =  millis();
		initAudio();		
		//load drum sounds
		String drumName;
		drumSounds = new Sampler[numDrumSnds];	
		for(int i =0; i<numDrumSnds; ++i){
			drumName = sketchPath() + fileDelim + "bin" + fileDelim + "drums"+ fileDelim + "drums"+i+".wav";// + 
			drumSounds[i] = new Sampler(drumName, 16, minim);
		}
//			drums0 : bd
//			drums1 : hh1
//			drums2 : hh2
//			drums3 : snare1
//			drums4 : snare2 (clap snare)
//			drums5 : ride
//			drums6 : crash
//			drums7 : talk drum			
		focusTar = new myVector(sceneFcsVals[sceneIDX]);
		loadAndSetImgs();					//load all images used : rest, clef, textures for spheres
		clefs = new myClefBase[]{
				new myClef(this, "Treble", clefType.Treble, new NoteData(this, noteValType.B, 4),clefImgs[0], new float[]{-5,0,40,50},0),   //Treble(0), Bass(1), Alto(2), Tenor(3), Piano(4), Drum(5); 
				new myClef(this, "Bass", clefType.Bass, new NoteData(this, noteValType.D, 3),clefImgs[1], new float[]{0,-1,40,38},10),
				new myClef(this, "Alto", clefType.Alto, new NoteData(this, noteValType.C, 4),clefImgs[2], new float[]{-10,-3,40,46},5),
				new myClef(this, "Tenor", clefType.Tenor, new NoteData(this, noteValType.A, 3),clefImgs[3],new float[]{-10,-13,40,46},-5),
				null,//replaced below (grand cleff)
				new myClef(this, "Drum", clefType.Drum, new NoteData(this, noteValType.B, 4),clefImgs[5], new float[]{0,0,40,40},0)				
		};
		clefs[clefType.Piano.getVal()] = new myGrandClef(this, "Piano", clefType.Piano, new NoteData(this, noteValType.B, 4),clefImgs[4], new float[]{0,0,40,40},0); 
		InstrList = new myInstrument[]{//TODO set harmonic series
				new myInstrument(this, "Guitar1", clefs[clefType.Treble.getVal()], hSrsMult,Waves.SAW, false),
				new myInstrument(this, "Guitar2", clefs[clefType.Treble.getVal()], hSrsMult,Waves.QUARTERPULSE, false),
				new myInstrument(this, "Bass", clefs[clefType.Bass.getVal()], hSrsMult, Waves.TRIANGLE, false),
				new myInstrument(this, "Vox1", clefs[clefType.Treble.getVal()], hSrsMult,Waves.SINE, false),
				new myInstrument(this, "Vox2", clefs[clefType.Treble.getVal()], hSrsMult,Waves.SINE, false),
				new myInstrument(this, "Synth1", clefs[clefType.Treble.getVal()], hSrsMult,Waves.SQUARE, false),
				new myInstrument(this, "Synth2", clefs[clefType.Treble.getVal()], hSrsMult,Waves.SQUARE, false),
				new myInstrument(this, "Synth3", clefs[clefType.Treble.getVal()], hSrsMult,Waves.QUARTERPULSE, false),
				new myInstrument(this, "Synth4", clefs[clefType.Alto.getVal()], hSrsMult,Waves.QUARTERPULSE, false),
				new myInstrument(this, "Synth5", clefs[clefType.Tenor.getVal()], hSrsMult,Waves.SAW, false),
				new myInstrument(this, "Drums", clefs[clefType.Drum.getVal()], hSrsMult,Waves.SINE, true),				//name of drum kits needs to be "Drums"
				new myInstrument(this, "Drums2", clefs[clefType.Drum.getVal()], hSrsMult,Waves.SINE, true)				//name of drum kits needs to be "Drums"
		};
	
		jtFace = loadImage("data/picJT.jpg"); 
		initDispWins();

		//setFlags(showSequence, true);				//show input sequencer	
		setFlags(setGlobalVals, true);				//show input sequencer	
		setFlags(showSphereUI, true);				//show input sequencer	
		setFlags(showUIMenu, true);					//show input UI menu	
		setFlags(moveKeyNoteUp, true);				//default is move notes up when forcing to key
		setCamView(); 
		initProgram();
	}//initOnce
	
	//called multiple times, whenever re-initing
	public void initProgram(){
		initVisProg();				//always first
		drawCount = 0;
	}//initProgram

	public void draw(){	
		animCntr = (animCntr + (baseAnimSpd )*animModMult) % maxAnimCntr;						//set animcntr - used only to animate visuals		
		//cyclModCmp = (drawCount % ((mySideBarMenu)dispWinFrames[dispMenuIDX]).guiObjs[((mySideBarMenu)dispWinFrames[dispMenuIDX]).gIDX_cycModDraw].valAsInt() == 0);
		pushMatrix();pushStyle();
		drawSetup();																//initialize camera, lights and scene orientation and set up eye movement
		//if ((!cyclModCmp) || (flags[playMusic])) {drawCount++;}						//needed to stop draw update so that pausing sim retains animation positions			
		if ((flags[playMusic])) {drawCount++;}											//needed to stop draw update so that pausing sim retains animation positions			
		glblStartPlayTime = millis();
		float modAmtSec = (glblStartPlayTime - glblLastPlayTime)/1000.0f;
		glblLastPlayTime = millis();
		if(flags[playMusic] ){movePBEReticle(modAmtSec);}		//play in current window
		translate(focusTar.x,focusTar.y,focusTar.z);								//focus location center of screen					
		if((curFocusWin == -1) || (dispWinIs3D[curFocusWin])){		draw3D_solve3D();} 
		//else {														draw3D_solve2D();}
		buildCanvas();		
		popStyle();popMatrix(); 
		drawUI(modAmtSec);																	//draw UI overlay on top of rendered results			
		if (flags[saveAnim]) {	savePic();}
		consoleStrings.clear();
		surface.setTitle(prjNmLong + " : " + (int)(frameRate) + " fps|cyc ");
	}//draw
	//move reticle if currently playing
	public void movePBEReticle(float modAmtSec){
		for(int i =1; i<numDispWins; ++i){if((isShowingWindow(i)) && (dispWinFrames[i].dispFlags[myMusicSimWindow.plays])){dispWinFrames[i].movePBEReticle(modAmtSec); return;}} //TODO Cntl if multiple windows need to handle simultaneous play here
	}
	
	//call 1 time if play is turned to true
	public void playMusic(){
		for(int i =1; i<numDispWins; ++i){if((isShowingWindow(i)) && (dispWinFrames[i].dispFlags[myMusicSimWindow.plays])){dispWinFrames[i].play(); return;}} //TODO Cntl if multiple windows need to handle simultaneous play here
	}
	//call 1 time if play is turned to false or stop is called
	public void stopMusic(){
		for(int i =1; i<numDispWins; ++i){if((isShowingWindow(i)) && (dispWinFrames[i].dispFlags[myMusicSimWindow.plays])){dispWinFrames[i].stopPlaying(); return;}} //TODO Cntl if multiple windows need to handle simultaneous play here
	}
	
	public void draw3D_solve3D(){
	//	if (cyclModCmp) {															//if drawing this frame, draw results of calculations								
			background(bground[0],bground[1],bground[2],bground[3]);				//if refreshing screen, this clears screen, sets background
			pushMatrix();pushStyle();
			translateSceneCtr();				//move to center of 3d volume to start drawing	
			for(int i =1; i<numDispWins; ++i){if((isShowingWindow(i)) && (dispWinFrames[i].dispFlags[myMusicSimWindow.is3DWin])){dispWinFrames[i].draw(myPoint._add(sceneCtrVals[sceneIDX],focusTar));}}
			popStyle();popMatrix();
			drawAxes(100,3, new myPoint(-canvas.viewDimW/2.0f+40,0.0f,0.0f), 200, false); 		//for visualisation purposes and to show movement and location in otherwise empty scene
	//	}
		if(canShow3DBox[this.curFocusWin]) {drawBoxBnds();}
	}
	
//		public void draw3D_solve2D(){
//			if (cyclModCmp) {															//if drawing this frame, draw results of calculations								
//				background(bground[0],bground[1],bground[2],bground[3]);				//if refreshing screen, this clears screen, sets background
//				pushMatrix();pushStyle();
//				translateSceneCtr();				//move to center of 3d volume to start drawing	
//				popStyle();popMatrix();
//				drawAxes(100,3, new myPoint(-c.viewDimW/2.0f+40,0.0f,0.0f), 200, false); 		//for visualisation purposes and to show movement and location in otherwise empty scene
//			}
//		}		

	public void buildCanvas(){
		canvas.buildCanvas();
		canvas.drawMseEdge();
	}
	
	//if should show problem # i
	public boolean isShowingWindow(int i){return flags[(i+this.showUIMenu)];}//showUIMenu is first flag of window showing flags
	public void drawUI(float modAmtMillis){					
		for(int i =1; i<numDispWins; ++i){if ((isShowingWindow(i)) && !(dispWinFrames[i].dispFlags[myMusicSimWindow.is3DWin])){dispWinFrames[i].draw(sceneCtrVals[sceneIDX]);}}
		//dispWinFrames[0].draw(sceneCtrVals[sceneIDX]);
		for(int i =1; i<numDispWins; ++i){dispWinFrames[i].drawHeader(modAmtMillis);}
		//menu
		dispWinFrames[0].draw2D(modAmtMillis);
		dispWinFrames[0].drawHeader(modAmtMillis);
		drawOnScreenData();				//debug and on-screen data
		hint(PConstants.DISABLE_DEPTH_TEST);
		noLights();
		displayHeader();				//my pic and name
		lights();
		hint(PConstants.ENABLE_DEPTH_TEST);
	}//drawUI	
	public void translateSceneCtr(){translate(sceneCtrVals[sceneIDX].x,sceneCtrVals[sceneIDX].y,sceneCtrVals[sceneIDX].z);}
	
	public void setFocus(){
		focusTar.set(sceneFcsVals[(sceneIDX+sceneFcsVals.length)%sceneFcsVals.length]);
		switch (sceneIDX){//special handling for each view
		case 0 : {initProgram();break;} //refocus camera on center
		case 1 : {initProgram();break;}  
		}
	}
	
	public void setCamView(){//also sets idx in scene focus and center arrays
		sceneIDX = (curFocusWin == -1 || SeqVisFFTOcean.dispWinIs3D[curFocusWin]) ? 1 : 0;
		rx = (float)cameraInitLocs[sceneIDX].x;
		ry = (float)cameraInitLocs[sceneIDX].y;
		dz = (float)cameraInitLocs[sceneIDX].z;
		setFocus();
	}
	public void loadAndSetImgs(){
		numRestImges = durType.getNumVals() - 2;
		numClefImgs = clefType.getNumVals();			
		numSphereImgs = 18;
		
		restImgs = new PImage[numRestImges];			
		String imgName;
		//load images of rests - whole and half rests are drawn
		for(int i =0; i< numRestImges; ++i){
			imgName = sketchPath() + fileDelim + "bin" + fileDelim + "data" + fileDelim + "rest"+i+".png";// + 
			restImgs[i] = loadImage(imgName);
		}	
		clefImgs = new PImage[numClefImgs];			
		//load images of clefs - grand staff is built from treble and bass clef so has no image of its own
		for(int i =0; i< numClefImgs; ++i){
			imgName = sketchPath() + fileDelim + "bin" + fileDelim + "data" + fileDelim + "clef"+i+".png";// + 
			clefImgs[i] = loadImage(imgName);
		}
		clefImgs[0].loadPixels();
//		int[] tmp = clefImgs[0].pixels;
//		//set images, for grand staff set images as treble + bass clef
//		for(int i =0; i<clefs.length;++i){if(i == clefVal.Piano.getVal()){	clefs[i].setImage(new PImage[]{clefImgs[0],clefImgs[1]});	}	else {clefs[i].setImage(new PImage[]{clefImgs[i]});}}
		
		sphereImgs = new PImage[numSphereImgs];
		for(int i =0; i< numSphereImgs; ++i){
			imgName = sketchPath() + fileDelim + "bin" + fileDelim + "data" + fileDelim + "SphereTextues" + fileDelim + "sphereTex"+i+".jpg";// + 
			sphereImgs[i] = loadImage(imgName);
		}	
		moonImgs = new PImage[3];		//3 "moon" controls
		for (int i =0; i<moonImgs.length;++i){
			imgName = sketchPath() + fileDelim + "bin" + fileDelim + "data" + fileDelim + "SphereTextues" + fileDelim + "sphereMoonTex"+i+".jpg";
			moonImgs[i] = loadImage(imgName);
		}
		//set images, for grand staff set images as treble + bass clef
//			/for(int i =0; i<numSphereImgs;++i){}
	}		
	
			//where is middle c for this staff's instrument's cleff
		
	//////////////////////////////////////////////////////
	/// user interaction
	//////////////////////////////////////////////////////	
	
	public void keyPressed(){
		switch (key){
			case '1' : {break;}
			case '2' : {break;}
			case '3' : {break;}
			case '4' : {break;}
			case '5' : {break;}							
			case '6' : {break;}
			case '7' : {break;}
			case '8' : {break;}
			case '9' : {break;}
			case '0' : {setFlags(showUIMenu,true); break;}							//to force show UI menu
			case ' ' : {setFlags(playMusic,!flags[playMusic]); break;}							//run sim
			case 'f' : {setCamView();break;}//reset camera
			case 'a' :
			case 'A' : {setFlags(saveAnim,!flags[saveAnim]);break;}						//start/stop saving every frame for making into animation
			case 's' :
			case 'S' : {save(sketchPath() + "\\"+prjNmLong+dateStr+"\\"+prjNmShrt+"_img"+timeStr + ".jpg");break;}//save picture of current image			
//				case ';' :
//				case ':' : {((mySideBarMenu)dispWinFrames[dispMenuIDX]).guiObjs[((mySideBarMenu)dispWinFrames[dispMenuIDX]).gIDX_cycModDraw].modVal(-1); break;}//decrease the number of cycles between each draw, to some lower bound
//				case '\'' :
//				case '"' : {((mySideBarMenu)dispWinFrames[dispMenuIDX]).guiObjs[((mySideBarMenu)dispWinFrames[dispMenuIDX]).gIDX_cycModDraw].modVal(1); break;}//increase the number of cycles between each draw to some upper bound		
			default : {	}
		}//switch	
		
		if((!flags[shiftKeyPressed])&&(key==CODED)){setFlags(shiftKeyPressed,(keyCode  == KeyEvent.VK_SHIFT));}
		if((!flags[altKeyPressed])&&(key==CODED)){setFlags(altKeyPressed,(keyCode  == KeyEvent.VK_ALT));}
		if((!flags[cntlKeyPressed])&&(key==CODED)){setFlags(cntlKeyPressed,(keyCode  == KeyEvent.VK_CONTROL));}
	}
	public void keyReleased(){
		if((flags[shiftKeyPressed])&&(key==CODED)){ if(keyCode == KeyEvent.VK_SHIFT){endShiftKey();}}
		if((flags[altKeyPressed])&&(key==CODED)){ if(keyCode == KeyEvent.VK_ALT){endAltKey();}}
		if((flags[cntlKeyPressed])&&(key==CODED)){ if(keyCode == KeyEvent.VK_CONTROL){endCntlKey();}}
	}		
	public void endShiftKey(){
		clearFlags(new int []{shiftKeyPressed, modView});
		for(int i =0; i<SeqVisFFTOcean.numDispWins; ++i){dispWinFrames[i].endShiftKey();}
	}
	public void endAltKey(){
		clearFlags(new int []{altKeyPressed});
		for(int i =0; i<SeqVisFFTOcean.numDispWins; ++i){dispWinFrames[i].endAltKey();}			
	}
	public void endCntlKey(){
		clearFlags(new int []{cntlKeyPressed});
		for(int i =0; i<SeqVisFFTOcean.numDispWins; ++i){dispWinFrames[i].endCntlKey();}			
	}

	//2d range checking of point
	public boolean ptInRange(double x, double y, double minX, double minY, double maxX, double maxY){return ((x > minX)&&(x <= maxX)&&(y > minY)&&(y <= maxY));}	
	//gives multiplier based on whether shift, alt or cntl (or any combo) is pressed
	public double clickValModMult(){
		return ((flags[altKeyPressed] ? .1 : 1.0) * (flags[cntlKeyPressed] ? 10.0 : 1.0));			
	}
	
	public void mouseMoved(){for(int i =0; i<numDispWins; ++i){if (dispWinFrames[i].handleMouseMove(mouseX, mouseY)){return;}}}
	public void mousePressed() {
		//verify left button if(mouseButton == LEFT)
		setFlags(mouseClicked, true);
		if(mouseButton == LEFT){			mouseClicked(0);} 
		else if (mouseButton == RIGHT) {	mouseClicked(1);}
		//for(int i =0; i<numDispWins; ++i){	if (dispWinFrames[i].handleMouseClick(mouseX, mouseY,c.getMseLoc(sceneCtrVals[sceneIDX]))){	return;}}
	}// mousepressed	

	private void mouseClicked(int mseBtn){ for(int i =0; i<numDispWins; ++i){if (dispWinFrames[i].handleMouseClick(mouseX, mouseY,mseBtn)){return;}}}		
	
//	private void mouseLeftClicked(){for(int i =0; i<numDispWins; ++i){if (dispWinFrames[i].handleMouseClick(mouseX, mouseY,c.getMseLoc(sceneCtrVals[sceneIDX]),0)){return;}}}		
//	private void mouseRightClicked(){for(int i =0; i<numDispWins; ++i){if (dispWinFrames[i].handleMouseClick(mouseX, mouseY,c.getMseLoc(sceneCtrVals[sceneIDX]),1)){return;}}}		
	public void mouseDragged(){//pmouseX is previous mouse x
		if((flags[shiftKeyPressed]) && (canMoveView[curFocusWin])){		//modifying view - always bypass HUD windows if doing this
			flags[modView]=true;
			if(mouseButton == LEFT){			rx-=PI*(mouseY-pmouseY)/height; ry+=PI*(mouseX-pmouseX)/width;} 
			else if (mouseButton == RIGHT) {	dz-=(float)(mouseY-pmouseY);}
		} else {
			if(mouseButton == LEFT){			mouseLeftDragged();} 
			else if (mouseButton == RIGHT) {	mouseRightDragged();}
			//for(int i =0; i<numDispWins; ++i){if (dispWinFrames[i].handleMouseDrag(mouseX, mouseY, pmouseX, pmouseY,new myVector(c.getOldMseLoc(),c.getMseLoc()))) {return;}}
		}
	}//mouseDragged()
	
	private void mouseLeftDragged(){
		myVector mseDiff = new myVector(canvas.getOldMseLoc(),canvas.getMseLoc());
		for(int i =0; i<numDispWins; ++i){if (dispWinFrames[i].handleMouseDrag(mouseX, mouseY, pmouseX, pmouseY,mseDiff,0)) {return;}}		
	}
	
	private void mouseRightDragged(){
		myVector mseDiff = new myVector(canvas.getOldMseLoc(),canvas.getMseLoc());
		for(int i =0; i<numDispWins; ++i){if (dispWinFrames[i].handleMouseDrag(mouseX, mouseY, pmouseX, pmouseY,mseDiff,1)) {return;}}		
	}
	
	
	public void mouseReleased(){
		clearFlags(new int[]{mouseClicked, modView});
		msClkStr = "";
		for(int i =0; i<numDispWins; ++i){dispWinFrames[i].handleMouseRelease();}
		flags[drawing] = false;
		//c.clearMsDepth();
	}//mouseReleased

	//these tie using the UI buttons to modify the window in with using the boolean tags - PITA but currently necessary
	public void handleShowWin(int btn, int val){handleShowWin(btn, val, true);}					//display specific windows - multi-select/ always on if sel
	public void handleShowWin(int btn, int val, boolean callFlags){//{"Score","Curve","InstEdit"},					//display specific windows - multi-select/ always on if sel
		//System.out.println((callFlags ? "|":"|!callFlags ") + " : btn : " + btn + " bVal : " + val);		
		if(!callFlags){//called from setflags - only sets button state in UI
			((mySideBarMenu)dispWinFrames[dispMenuIDX]).guiBtnSt[mySideBarMenu.btnShowWinIdx][btn] = val;
		} else {//called from clicking on buttons in UI
			boolean bVal = (val == 1?  false : true);//boolean version of button state
			switch(btn){
				case 0 : {setFlags(showSequence, bVal);break;}
				case 1 : {setFlags(showSphereUI, bVal);break;}
				case 2 : {setFlags(showSimWin, bVal);break;}
				case 3 : {setFlags(showInstEdit, bVal);break;}
			}
		}
	}//handleShowWin


//		//process request to add a  new component
//		public void handleAddNewCmp(int btn, int val){handleAddNewCmp(btn, val, true);}					//display specific windows - multi-select/ always on if sel
//		public void handleAddNewCmp(int btn, int val, boolean callFlags){//{"Score","Staff","Measure","Note"},			//add - momentary
//			if(!callFlags){
//				((mySideBarMenu)dispWinFrames[dispMenuIDX]).guiBtnSt[mySideBarMenu.btnAddNewCmpIdx][btn] = val;
//			} else {
//				switch(btn){
//					case 0 : {break;}//new song
//					case 1 : {break;}//
//					case 2 : {break;}
//					case 3 : {break;}
//				}
//			}
//		}//handleAddNewCmp
	
	//process to delete an existing component
	public void handleDBGSelCmp(int btn, int val){handleDBGSelCmp(btn, val, true);}					//display specific windows - multi-select/ always on if sel
	public void handleDBGSelCmp(int btn, int val, boolean callFlags){//{"Score","Staff","Measure","Note"},			//del - momentary
		if(!callFlags){
			((mySideBarMenu)dispWinFrames[dispMenuIDX]).guiBtnSt[mySideBarMenu.btnDBGSelCmpIdx][btn] = val;
		} else {
			switch(btn){
				case 0 : {dispWinFrames[curFocusWin].clickDebug(btn) ;break;}
				case 1 : {dispWinFrames[curFocusWin].clickDebug(btn) ;break;}
				case 2 : {dispWinFrames[curFocusWin].clickDebug(btn) ;break;}
				case 3 : {dispWinFrames[curFocusWin].clickDebug(btn) ;break;}
			}
		}
	}//handleAddDelSelCmp	
	
//		//process to edit an instrument : TODO
//		public void handleInstEdit(int btn, int val){handleInstEdit(btn, val, true);}					//display specific windows - multi-select/ always on if sel
//		public void handleInstEdit(int btn, int val, boolean callFlags){//{"New","Edit","Delete"},					//Instrument edit/modify - momentary
//			if(!callFlags){
//				((mySideBarMenu)dispWinFrames[dispMenuIDX]).guiBtnSt[mySideBarMenu.btnInstEditIdx][btn] = val;
//			} else {
//				switch(btn){
//					case 0 : {break;}
//					case 1 : {break;}
//					case 2 : {break;}
//				}		
//			}
//		}//handleInstEdit		
//		
	//process to handle file io		
	public void handleFileCmd(int btn, int val){handleFileCmd(btn, val, true);}					//display specific windows - multi-select/ always on if sel
	public void handleFileCmd(int btn, int val, boolean callFlags){//{"Load","Save"},							//load an existing score, save an existing score - momentary	
		if(!callFlags){
			((mySideBarMenu)dispWinFrames[dispMenuIDX]).guiBtnSt[mySideBarMenu.btnFileCmdIdx][btn] = val;
		} else {
			switch(btn){
				case 0 : {break;}
				case 1 : {break;}
			}		
		}
	}//handleFileCmd
	
	//process to handle play/stop transport control
	public void handleTrnsprt(int btn, int val){handleTrnsprt(btn, val, true);}					//display specific windows - multi-select/ always on if sel
	public void handleTrnsprt(int btn, int val, boolean callFlags){//{"Rewind","Stop","Play","FastFwd"},			//transport controls - mixed
		if(!callFlags){
			((mySideBarMenu)dispWinFrames[dispMenuIDX]).guiBtnSt[mySideBarMenu.btnTrnsprtIdx][btn] = val;			//turns on appropriate button in menu
			if(btn==1){	((mySideBarMenu)dispWinFrames[dispMenuIDX]).guiBtnSt[mySideBarMenu.btnTrnsprtIdx][2] = 0;}	//if stop, turn off play button
		//	if(flags[playMusic]) {playMusic();} else {stopMusic();} 
		} else {
			switch(btn){
				case 0 : {modCurPBETimeAllWins(-pbeModAmt);break;}//rewind
				case 1 : {setFlags(playMusic, false); stopMusic();break;}//stop - always turn play button off
				case 2 : {setFlags(playMusic, !flags[playMusic]); if(flags[playMusic]) {playMusic();} else {stopMusic();} break;}//play / pause
				case 3 : {modCurPBETimeAllWins(pbeModAmt);break;}//fastfwd
			}			
		}
	}//handleTrnsprt
	public void modCurPBETimeAllWins(float modAmt){
		if(flags[playMusic]) {stopMusic();}
		for(int i =0; i < numDispWins; ++i){((myMusicSimWindow) dispWinFrames[i]).modCurrentPBETime(modAmt);}	
		if(flags[playMusic]) {playMusic();}
	}

	//set current values to be global value entered in UI
	public void handleGlobalVals(int btn, int val){handleGlobalVals(btn, val, true);}
	public void handleGlobalVals(int btn, int val, boolean callFlags){
		if(!callFlags){
			((mySideBarMenu)dispWinFrames[dispMenuIDX]).guiBtnSt[mySideBarMenu.btnTrnsprtIdx][btn] = val;
		} else {
			switch(btn){
				case 0 : {break;}									//Key Signature
				case 1 : {break;}									//Time Signature
				case 2 : {break;}									//Tempo
			}			
		}		
	}//handleGlobalVals

	

	public void initDispWins(){
		float InstEditWinHeight = InstEditWinYMult * height;		//how high is the InstEdit window when shown
		//instanced window dimensions when open and closed - only showing 1 open at a time
		winRectDimOpen[dispPianoRollIDX] =  new float[]{menuWidth, 0,width-menuWidth,height-hideWinHeight};			
		winRectDimOpen[dispSphereUIIDX] =  new float[]{menuWidth+hideWinWidth, 0,width-menuWidth-hideWinWidth,height-hideWinHeight};			
		winRectDimOpen[dispSimIDX] =  new float[]{menuWidth+hideWinWidth, 0,width-menuWidth-hideWinWidth,height-hideWinHeight};			
		winRectDimOpen[dispInstEditIDX]  =  new float[]{menuWidth, InstEditWinHeight, width-menuWidth, height-InstEditWinHeight};
		//hidden
		winRectDimClose[dispPianoRollIDX] =  new float[]{menuWidth, 0, hideWinWidth, height};				
		winRectDimClose[dispSphereUIIDX] =  new float[]{menuWidth, 0, hideWinWidth, height};				
		winRectDimClose[dispSimIDX] =  new float[]{menuWidth, 0, hideWinWidth, height};				
		winRectDimClose[dispInstEditIDX]  =  new float[]{menuWidth, height-hideWinHeight, width-menuWidth, hideWinHeight};
		
		winTrajFillClrs = new int []{gui_Black,gui_LightGray,gui_LightGray,gui_LightGray,gui_LightGreen};		//set to color constants for each window
		winTrajStrkClrs = new int []{gui_Black,gui_DarkGray,gui_DarkGray,gui_DarkGray,gui_White};		//set to color constants for each window			
		
		String[] winTitles = new String[]{"","Piano Roll/Score","Sphere UI","Sim Ocean Visualisation", "Instrument Edit"},
				winDescr = new String[] {"","Piano/Score Editor Window - Draw In Here to enter or edit notes, and to see the resultant score","Control the various instruments using the spheres","Simulation Responds to Music", "Instrument Frequency Response Edit Window"};
//			//display window initialization	
		int wIdx = dispPianoRollIDX, fIdx = showSequence;
		dispWinFrames[wIdx] = new mySequencerWindow(this, winTitles[wIdx], fIdx, winFillClrs[wIdx], winStrkClrs[wIdx], winRectDimOpen[wIdx],winRectDimClose[wIdx],winDescr[wIdx],canDrawInWin[wIdx]);			
		wIdx = dispSphereUIIDX;fIdx = showSphereUI;
		dispWinFrames[wIdx] = new mySphereWindow(this, winTitles[wIdx], fIdx,winFillClrs[wIdx], winStrkClrs[wIdx], winRectDimOpen[wIdx], winRectDimClose[wIdx], winDescr[wIdx],canDrawInWin[wIdx]);
		wIdx = dispSimIDX;fIdx = showSimWin;
		dispWinFrames[wIdx] = new mySimWindow(this, winTitles[wIdx], fIdx,winFillClrs[wIdx], winStrkClrs[wIdx], winRectDimOpen[wIdx], winRectDimClose[wIdx], winDescr[0],canDrawInWin[wIdx]);
		wIdx = dispInstEditIDX;fIdx=showInstEdit;
		dispWinFrames[wIdx] = new myInstEditWindow(this, winTitles[wIdx], fIdx,winFillClrs[wIdx], winStrkClrs[wIdx], winRectDimOpen[wIdx], winRectDimClose[wIdx], winDescr[wIdx],canDrawInWin[wIdx]);
		//setup default score and finalize init of windows
		initNewScore();				
		for(int i =0; i < numDispWins; ++i){
			myMusicSimWindow win = ((myMusicSimWindow) dispWinFrames[i]);
			win.setGlobalTempoVal(120);		//set for right now
			win.setGlobalKeySigVal(0);		//set for right now
			win.setGlobalTimeSigVal(4,4,durType.getDurTypeForNote(4));		//set for right now
			win.dispFlags[myMusicSimWindow.is3DWin] = dispWinIs3D[i];
			//win.setTrajColors(winTrajFillClrs[i], winTrajStrkClrs[i]);
		}	
				
	}//initDispWins
	//needs to happen here, and then be propagated out to all windows
	public void initNewScore(){//uses default values originally called in mySequencer constructor
		//outStr2Scr("Build score with default instrument list");
		initNewScore("TempSong", InstrList, winRectDimOpen[dispPianoRollIDX][0] + winRectDimOpen[dispPianoRollIDX][2],winRectDimOpen[dispPianoRollIDX][1]+winRectDimOpen[dispPianoRollIDX][3]-4*myMusicSimWindow.yOff);
	}
	public void initNewScore(String scrName, myInstrument[] _instrs, float scoreWidth, float scoreHeight){
		float [] scoreRect = new float[]{0, myMusicSimWindow.topOffY, scoreWidth, scoreHeight};
		score = new myScore(this,dispWinFrames[dispPianoRollIDX],"TempSong",scoreRect);	
		//debug stuff - buildScore only
		String[] scoreStaffNames = new String[_instrs.length];
		for(int i =0; i<scoreStaffNames.length;++i){
			scoreStaffNames[i] = _instrs[i].instrName + " " + i;
			//score.addStaff(scoreStaffNames[i], new myInstr(_instrs[i]));
			score.addStaff(scoreStaffNames[i], _instrs[i]);
		}		
		for(int i =0; i < numDispWins; ++i){dispWinFrames[i].setScoreInstrVals(score.getInstrumentList(), scoreStaffNames);}
	}
	// this sends refs to the score and all instrments in the current score and their names to all windows 
//		public void sendInstrValsToWins(TreeMap<String,myInstr> _instrs, String[] _scoreStaffNames, int CallerID){
//			for(int i =0; i < numDispWins; ++i){if(dispWinFrames[i].ID ==CallerID){continue;}dispWinFrames[i].setScoreInstrVals(_instrs, _scoreStaffNames,CallerID);}
//		}
	
	//get the ui rect values of the "master" ui region (another window) -> this is so ui objects of one window can be made, clicked, and shown displaced from those of the parent windwo
	public double[] getUIRectVals(int idx){
		switch(idx){
		case dispMenuIDX 		: {return new double[0];}			//idx 0 is parent menu sidebar
		case dispPianoRollIDX 	: {return dispWinFrames[dispMenuIDX].uiClkCoords;}
		case dispSphereUIIDX 	: {return dispWinFrames[dispMenuIDX].uiClkCoords;}
		case dispSimIDX 		: {
			//if displaying visualization with sequence source video
			//double[] res = new double[]{dispWinFrames[dispMenuIDX].uiClkCoords[0], dispWinFrames[dispMenuIDX].uiClkCoords[1]-dispWinFrames[dispMenuIDX].uiClkCoords[3],dispWinFrames[dispMenuIDX].uiClkCoords[2],Math.max(dispWinFrames[dispSphereUIIDX].uiClkCoords[3],dispWinFrames[dispPianoRollIDX].uiClkCoords[3])};
			//return res;
			return dispWinFrames[dispMenuIDX].uiClkCoords;}//(dispWinFrames[dispPianoRollIDX].uiClkCoords[3] > dispWinFrames[dispSphereUIIDX].uiClkCoords[3]   ? dispWinFrames[dispPianoRollIDX].uiClkCoords : dispWinFrames[dispSphereUIIDX].uiClkCoords);}
		case dispInstEditIDX 	: {				
			return dispWinFrames[dispSimIDX].uiClkCoords;}
		default :  return dispWinFrames[dispMenuIDX].uiClkCoords;
		}
	}

//////////////////////////////////////////
/// graphics and base functionality utilities and variables
//////////////////////////////////////////
	
	//size of printed text (default is 12)
	public static final int txtSz = 12;
	//constant path strings for different file types
	public static final String fileDelim = "\\";	
	//display-related size variables
	public final int grid2D_X=800, grid2D_Y=800;	
	public final int gridDimX = 800, gridDimY = 800, gridDimZ = 800;				//dimensions of 3d region
	
	public int scrWidth, scrHeight;			//set to be applet.width and applet.height unless otherwise specified below
	public final int scrWidthMod = 200, 
			scrHeightMod = 0;
	public final float frate = 120;			//frame rate - # of playback updates per second
	
	public int sceneIDX;			//which idx in the 2d arrays of focus vals and glbl center vals to use, based on state
	public myVector[] sceneFcsVals = new myVector[]{						//set these values to be different targets of focus
			new myVector(-grid2D_X/2,-grid2D_Y/1.75f,0),
			new myVector(0,0,0)
	};
	
	public myPoint[] sceneCtrVals = new myPoint[]{						//set these values to be different display center translations -
			new myPoint(0,0,0),										// to be used to calculate mouse offset in world for pick
			new myPoint(-gridDimX/2.0,-gridDimY/2.0,-gridDimZ/2.0)
	};

	public final float camInitialDist = -200,		//initial distance camera is from scene - needs to be negative
				camInitRy = 0,
				camInitRx = -PI/2.0f;
	private float dz=0, rx=-0.06f*TWO_PI, ry=-0.04f*TWO_PI;		// distance to camera. Manipulated with wheel or when,view angles manipulated when space pressed but not mouse	
	
	public myVector[] cameraInitLocs = new myVector[]{						//set these values to be different initial camera locations based on 2d or 3d
			new myVector(camInitRx,camInitRy,camInitialDist),
			new myVector(camInitRx,camInitRy,camInitialDist),
			new myVector(-0.47f,-0.61f,-gridDimZ*.25f)			
		};
	
	//static variables - put obj constructor counters here
	public static int GUIObjID = 0;										//counter variable for gui objs
	
	//visualization variables
	// boolean flags used to control various elements of the program 
	public boolean[] flags;
	//dev/debug flags
	public final int debugMode 			= 0;			//whether we are in debug mode or not	
	public final int saveAnim 			= 1;			//whether we are saving or not
	//interface flags	
	public final int shiftKeyPressed 	= 2;			//shift pressed
	public final int altKeyPressed  	= 3;			//alt pressed
	public final int cntlKeyPressed  	= 4;			//cntrl pressed
	public final int mouseClicked 		= 5;			//mouse left button is held down	
	public final int drawing			= 6; 			//currently drawing
	public final int modView	 		= 7;			//shift+mouse click+mouse move being used to modify the view		

	public final int playMusic			= 8;			//run simulation (if off localization progresses on single pose
	//public final int useDrawnVels		= 9;			//whether or not to use the velocity of drawing in renderig the a drawn stroke to build the stroke
	
	public final int showUIMenu 		= 10;
	
	public final int showSequence  		= 11;			//whether or not to show the pianoroll/score editor
	public final int showSphereUI		= 12;
	public final int showSimWin			= 13;
	public final int showInstEdit		= 14;			//whether to show instrument editor
	
	public final int flipDrawnTraj  	= 15;			//whether or not to flip the direction of the drawn melody trajectory		

	public final int forceInKey			= 16;			//force all input notes to be in key unless manually modified
	public final int moveKeyNoteUp		= 17;			//move forced notes up or down when forcing in key
	public final int joinNotesPianoRoll	= 18;			//tie notes when entering them on piano roll
	public final int clearStaffNewTraj	= 19;			//clear out staff notes when new piano roll trajectory drawn
	public final int setGlobalVals		= 20;			//whether to set global or at-time local vals when modifying tempo, key, time sig
	
	public final int numFlags = 21;

	//flags to actually display in menu as clickable text labels - order does matter
	public List<Integer> flagsToShow = Arrays.asList( 
			debugMode, 			
			saveAnim,			
			forceInKey,			
			moveKeyNoteUp,		
			joinNotesPianoRoll,	
			clearStaffNewTraj,
			setGlobalVals
			);
	
	public final int numFlagsToShow = flagsToShow.size();
	
	public List<Integer> stateFlagsToShow = Arrays.asList( 
			 shiftKeyPressed,			//shift pressed
			 altKeyPressed  ,			//alt pressed
			 cntlKeyPressed ,			//cntrl pressed
			 mouseClicked 	,			//mouse left button is held down	
			 drawing		, 			//currently drawing
			 modView	 				//shift+mouse click+mouse move being used to modify the view					
			);
	public final int numStFlagsToShow = stateFlagsToShow.size();
//		//flags that can be modified by clicking on screen - order doesn't matter
//		public List<Integer> clkyFlgs = Arrays.asList( 
//				debugMode, saveAnim, 
//				playMusic,   //useDrawnVels,//showUIMenu,
//				showSequence,showInstEdit,showSphereUI, showSimWin,
//				//flipDrawnTraj,
//				forceInKey, moveKeyNoteUp,joinNotesPianoRoll,clearStaffNewTraj
//				//,show3D
//				);			

	
	//individual display/HUD windows for gui/user interaction
	public myDispWindow[] dispWinFrames;
	//idx's in dispWinFrames for each window
	public static final int dispMenuIDX = 0,
							dispPianoRollIDX = 1,
							dispSphereUIIDX = 2,
							dispSimIDX = 3,
							dispInstEditIDX = 4;
	
	public static final int numDispWins = 5;	
			
	public int curFocusWin;				//which myDispWindow currently has focus 
	
	//whether or not the display windows will accept a drawn trajectory -- eventually set InstEdit window to be drawable
	public boolean[] canDrawInWin = new boolean[]{	false,true,true,false,true};		
	public boolean[] canShow3DBox = new boolean[]{	false,false,false,true,false};		
	public boolean[] canMoveView = new boolean[]{	false,false,false,true,false};		
	public static final boolean[] dispWinIs3D = new boolean[]{false,false,true,true,false};
	
	public static final int[][] winFillClrs = new int[][]{          
		new int[]{255,255,255,255},                                 	// dispMenuIDX = 0,
		new int[]{220,220,220,255},                                 	// dispPianoRollIDX = 1,
		new int[]{0,0,0,255},                                        	// dispSphereUIIDX = 2;
		new int[]{0,0,0,255},                                        	// dispSimIDX =3
		new int[]{0,0,0,255}                                        	// dispInstEditIDX = 4
	};
	public static final int[][] winStrkClrs = new int[][]{
		new int[]{0,0,0,255},                                    		//dispMenuIDX = 0,
		new int[]{0,0,0,255},                                    		//dispPianoRollIDX = 1,
		new int[]{255,255,255,255},                               		//dispSphereUIIDX = 2;
		new int[]{255,255,255,255},                               		//dispSimIDX = 3
		new int[]{255,255,255,255}                               		//dispInstEditIDX = 4
	};
	public static int[] winTrajFillClrs = new int []{0,0,0,0,0};		//set to color constants for each window
	public static int[] winTrajStrkClrs = new int []{0,0,0,0,0};		//set to color constants for each window

	
	//unblocked window dimensions - location and dim of window if window is one\
	public float[][] winRectDimOpen;// = new float[][]{new float[]{0,0,0,0},new float[]{0,0,0,0},new float[]{0,0,0,0},new float[]{0,0,0,0}};
	//window dimensions if closed -location and dim of all windows if this window is closed
	public float[][] winRectDimClose;// = new float[][]{new float[]{0,0,0,0},new float[]{0,0,0,0},new float[]{0,0,0,0},new float[]{0,0,0,0}};
	
	public boolean showInfo;										//whether or not to show start up instructions for code		
	public myVector focusTar;										//target of focus - used in translate to set where the camera is looking - 
																	//set array of vector values (sceneFcsVals) based on application
	//private boolean cyclModCmp;										//comparison every draw of cycleModDraw			
	public final int[] bground = new int[]{244,244,244,255};		//bground color
			
	public myPoint mseCurLoc2D;
	//how many frames to wait to actually refresh/draw
	//public int cycleModDraw = 1;
	public final int maxCycModDraw = 20;	//max val for cyc mod draw		

	// path and filename to save pictures for animation
	public String animPath, animFileName;
	public int animCounter;	
	public final int scrMsgTime = 50;									//5 seconds to delay a message 60 fps (used against draw count)
	public ArrayDeque<String> consoleStrings;							//data being printed to console - show on screen
	
	public int drawCount,simCycles;												// counter for draw cycles		
	public float menuWidth,menuWidthMult = .15f, hideWinWidth, hideWinWidthMult = .03f, hideWinHeight, hideWinHeightMult = .05f;			//side menu is 15% of screen grid2D_X, 

	public ArrayList<String> DebugInfoAra;										//enable drawing dbug info onto screen
	public String debugInfoString;
	
	//animation control variables	
	public float animCntr = 0, animModMult = 1.0f;
	public final float maxAnimCntr = PI*1000.0f, baseAnimSpd = 1.0f;
	

	my3DCanvas canvas;												//3d interaction stuff and mouse tracking
	
	public float[] camVals;		
	public String dateStr, timeStr;								//used to build directory and file names for screencaps
	
	public PGraphicsOpenGL pg; 
	public PGL pgl;
	//public GL2 gl;
	
	public double eps = .000000001, msClkEps = 40;				//calc epsilon, distance within which to check if clicked from a point
	public float feps = .000001f;
	public float SQRT2 = sqrt(2.0f);

	public int[] rgbClrs = new int[]{gui_Red,gui_Green,gui_Blue};
	//3dbox stuff
	public myVector[] boxNorms = new myVector[] {new myVector(1,0,0),new myVector(-1,0,0),new myVector(0,1,0),new myVector(0,-1,0),new myVector(0,0,1),new myVector(0,0,-1)};//normals to 3 d bounding boxes
	private final float hGDimX = gridDimX/2.0f, hGDimY = gridDimY/2.0f, hGDimZ = gridDimZ/2.0f;
	private final float tGDimX = gridDimX*10, tGDimY = gridDimY*10, tGDimZ = gridDimZ*20;
	public myPoint[][] boxWallPts = new myPoint[][] {//pts to check if intersection with 3D bounding box happens
			new myPoint[] {new myPoint(hGDimX,tGDimY,tGDimZ), new myPoint(hGDimX,-tGDimY,tGDimZ), new myPoint(hGDimX,tGDimY,-tGDimZ)  },
			new myPoint[] {new myPoint(-hGDimX,tGDimY,tGDimZ), new myPoint(-hGDimX,-tGDimY,tGDimZ), new myPoint(-hGDimX,tGDimY,-tGDimZ) },
			new myPoint[] {new myPoint(tGDimX,hGDimY,tGDimZ), new myPoint(-tGDimX,hGDimY,tGDimZ), new myPoint(tGDimX,hGDimY,-tGDimZ) },
			new myPoint[] {new myPoint(tGDimX,-hGDimY,tGDimZ),new myPoint(-tGDimX,-hGDimY,tGDimZ),new myPoint(tGDimX,-hGDimY,-tGDimZ) },
			new myPoint[] {new myPoint(tGDimX,tGDimY,hGDimZ), new myPoint(-tGDimX,tGDimY,hGDimZ), new myPoint(tGDimX,-tGDimY,hGDimZ)  },
			new myPoint[] {new myPoint(tGDimX,tGDimY,-hGDimZ),new myPoint(-tGDimX,tGDimY,-hGDimZ),new myPoint(tGDimX,-tGDimY,-hGDimZ)  }
	};
	
///////////////////////////////////
/// generic graphics functions and classes
///////////////////////////////////
		//1 time initialization of things that won't change
	public void initVisOnce(){	
		dateStr = "_"+day() + "-"+ month()+ "-"+year();
		timeStr = "_"+hour()+"-"+minute()+"-"+second();
		
		scrWidth = width + scrWidthMod;
		scrHeight = height + scrHeightMod;		//set to be applet.width and applet.height unless otherwise specified below

		
		consoleStrings = new ArrayDeque<String>();				//data being printed to console		
		menuWidth = width * menuWidthMult;						//grid2D_X of menu region	
		hideWinWidth = width * hideWinWidthMult;				//dims for hidden windows
		hideWinHeight = height * hideWinHeightMult;
		canvas = new my3DCanvas(this);			
		winRectDimOpen = new float[numDispWins][];
		winRectDimClose = new float[numDispWins][];
		winRectDimOpen[0] =  new float[]{0,0, menuWidth, height};
		winRectDimClose[0] =  new float[]{0,0, hideWinWidth, height};
		
		strokeCap(SQUARE);//makes the ends of stroke lines squared off
		
		//display window initialization
		dispWinFrames = new myMusicSimWindow[numDispWins];		
		//menu bar init
		dispWinFrames[dispMenuIDX] = new mySideBarMenu(this, "UI Window", showUIMenu,  winFillClrs[dispMenuIDX], winStrkClrs[dispMenuIDX], winRectDimOpen[dispMenuIDX],winRectDimClose[dispMenuIDX], "User Controls",canDrawInWin[dispMenuIDX]);			
		
		colorMode(RGB, 255, 255, 255, 255);
		mseCurLoc2D = new myPoint(0,0,0);	
		frameRate(frate);
		sphereDetail(4);
		initBoolFlags();
		camVals = new float[]{width/2.0f, height/2.0f, (height/2.0f) / tan(PI/6.0f), width/2.0f, height/2.0f, 0, 0, 1, 0};
		showInfo = true;
		textSize(txtSz);
		outStr2Scr("Current sketchPath " + sketchPath());
		textureMode(NORMAL);			
		rectMode(CORNER);	
		
		initCamView();
		simCycles = 0;
		animPath = sketchPath() + "\\"+prjNmLong+"_" + (int) random(1000);
		animFileName = "\\" + prjNmLong;
	}				
		//init boolean state machine flags for program
	public void initBoolFlags(){
		flags = new boolean[numFlags];
		for (int i = 0; i < numFlags; ++i) { flags[i] = false;}	
		((mySideBarMenu)dispWinFrames[dispMenuIDX]).initPFlagColors();			//init sidebar window flags
	}		
	
	//address all flag-setting here, so that if any special cases need to be addressed they can be
	public void setFlags(int idx, boolean val ){
		flags[idx] = val;
		switch (idx){
			case debugMode 			: { break;}//anything special for debugMode 			
			case saveAnim 			: { break;}//anything special for saveAnim 			
			case altKeyPressed 		: { break;}//anything special for altKeyPressed 	
			case shiftKeyPressed 	: { break;}//anything special for shiftKeyPressed 	
			case mouseClicked 		: { break;}//anything special for mouseClicked 		
			case modView	 		: { break;}//anything special for modView	 	
			case drawing			: { break;}
			case playMusic			: { handleTrnsprt((val ? 2 : 1) ,(val ? 1 : 0),false); break;}		//anything special for playMusic	
			//case flipDrawnTraj		: { dispWinFrames[dispPianoRollIDX].rebuildDrawnTraj();break;}						//whether or not to flip the drawn melody trajectory, width-wise
			case flipDrawnTraj		: { for(int i =1; i<dispWinFrames.length;++i){dispWinFrames[i].rebuildAllDrawnTrajs();}break;}						//whether or not to flip the drawn melody trajectory, width-wise
			case showUIMenu 	    : { dispWinFrames[dispMenuIDX].setShow(val);    break;}											//whether or not to show the main ui window (sidebar)
			
			case showSequence 		: {setWinFlagsXOR(dispPianoRollIDX, val); break;}
			case showSphereUI		: {setWinFlagsXOR(dispSphereUIIDX, val); break;}
			case showSimWin			: {setWinFlagsXOR(dispSimIDX, val); break;}			
			//case showSimWin			: {dispWinFrames[dispSimIDX].setShow(val);handleShowWin(dispSimIDX-1 ,(val ? 1 : 0),false);break;}
			case showInstEdit 		: {dispWinFrames[dispInstEditIDX].setShow(val);handleShowWin(dispInstEditIDX-1 ,(val ? 1 : 0),false); setWinsHeight(); break;}	//show InstEdit window

			//case useDrawnVels 		: {for(int i =1; i<dispWinFrames.length;++i){dispWinFrames[i].rebuildAllDrawnTrajs();}break;}
			case forceInKey			: {if(val){((mySequencerWindow)dispWinFrames[dispPianoRollIDX]).forceAllNotesInKey();}break;}
			case moveKeyNoteUp		: {break;}
			case setGlobalVals		: {break;}
			default : {break;}
		}
	}//setFlags  
	
	//set the height of each window that is above the InstEdit window, to move up or down when it changes size
	public void setWinsHeight(){
		for(int i =0;i<winDispIdxXOR.length;++i){//skip first window - ui menu - and last window - InstEdit window
			dispWinFrames[winDispIdxXOR[i]].setRectDimsY( dispWinFrames[dispInstEditIDX].getRectDim(1));
		}						
	}		
	
	//set only one of the following windows based on selection from menu
	public int[] winFlagsXOR = new int[]{showSequence,showSphereUI, showSimWin};
	public int[] winDispIdxXOR = new int[]{dispPianoRollIDX,dispSphereUIIDX, dispSimIDX};
	public void setWinFlagsXOR(int idx, boolean val){
		for(int i =0;i<winDispIdxXOR.length;++i){//skip first window - ui menu - and last window - InstEdit window
			if(winDispIdxXOR[i]!= idx){
				dispWinFrames[winDispIdxXOR[i]].setShow(false);
				handleShowWin(i ,0,false); 
				flags[winFlagsXOR[i]] = false;
			} else {
				dispWinFrames[idx].setShow(true);
				handleShowWin(i ,1,false); 
				flags[winFlagsXOR[i]] = true;
				curFocusWin = winDispIdxXOR[i];
				setCamView();
			}
		}
	}//setWinFlagsXOR
	
//	//handles toggling between windows.  
//	public int[] winFlagsXOR = new int[]{showSequence,showSphereUI};
//	public int[] winDispIdxXOR = new int[]{dispPianoRollIDX,dispSphereUIIDX};
//	public void setWinFlagsXOR(int idx, boolean val){
//		//outStr2Scr("SetWinFlagsXOR : idx " + idx + " val : " + val);
//		if(val){//turning one on
//			//turn off not shown, turn on shown				
//			for(int i =0;i<winDispIdxXOR.length;++i){//skip first window - ui menu - and last window - InstEdit window
//				if(winDispIdxXOR[i]!= idx){dispWinFrames[winDispIdxXOR[i]].setShow(false);handleShowWin(i ,0,false); flags[winFlagsXOR[i]] = false;}
//				else {
//					dispWinFrames[idx].setShow(true);
//					handleShowWin(i ,1,false); 
//					flags[winFlagsXOR[i]] = true;
//					curFocusWin = winDispIdxXOR[i];
//					setCamView();
//				}
//			}
//		} else {				//if turning off a window - need a default uncloseable window - for now just turn on next window : idx-1 is idx of allowable winwdows (idx 0 is sidebar menu)
//			setWinFlagsXOR((((idx-1) + 1) % winFlagsXOR.length)+1, true);
//		}			
//	}//setWinFlagsXOR
	
	//set flags appropriately when only 1 can be true 
	public void setFlagsXOR(int tIdx, int[] fIdx){for(int i =0;i<fIdx.length;++i){if(tIdx != fIdx[i]){flags[fIdx[i]] =false;}}}				
	public void clearFlags(int[] idxs){		for(int idx : idxs){flags[idx]=false;}	}			
		//called every time re-initialized
	public void initVisProg(){	drawCount = 0;		debugInfoString = "";		reInitInfoStr();}	
	public void initCamView(){	dz=camInitialDist;	ry=camInitRy;	rx=camInitRx - ry;	}
	public void reInitInfoStr(){		DebugInfoAra = new ArrayList<String>();		DebugInfoAra.add("");	}	
	public int addInfoStr(String str){return addInfoStr(DebugInfoAra.size(), str);}
	public int addInfoStr(int idx, String str){	
		int lstIdx = DebugInfoAra.size();
		if(idx >= lstIdx){		for(int i = lstIdx; i <= idx; ++i){	DebugInfoAra.add(i,"");	}}
		setInfoStr(idx,str);	return idx;
	}
	public void setInfoStr(int idx, String str){DebugInfoAra.set(idx,str);	}
	public void drawInfoStr(float sc){//draw text on main part of screen
		pushMatrix();		pushStyle();
		fill(0,0,0,100);
		translate((menuWidth),0);
		scale(sc,sc);
		for(int i = 0; i < DebugInfoAra.size(); ++i){		text((flags[debugMode]?(i<10?"0":"")+i+":     " : "") +"     "+DebugInfoAra.get(i)+"\n\n",0,(10+(12*i)));	}
		popStyle();	popMatrix();
	}		
	//vector and point functions to be compatible with earlier code from jarek's class or previous projects	
	//draw bounding box for 3d
	public void drawBoxBnds(){
		pushMatrix();
		pushStyle();
		strokeWeight(3f);
		noFill();
		setColorValStroke(gui_TransGray,255);
		
		box(gridDimX,gridDimY,gridDimZ);
		popStyle();		
		popMatrix();
	}		
	//drawsInitial setup for each draw
	public void drawSetup(){			
		camera(camVals[0],camVals[1],camVals[2],camVals[3],camVals[4],camVals[5],camVals[6],camVals[7],camVals[8]);       // sets a standard perspective
		//outStr2Scr("rx :  " + rx + " ry : " + ry + " dz : " + dz);
		translate((float)width/2.0f,(float)height/2.0f,(float)dz); // puts origin of model at screen center and moves forward/away by dz
		setCamOrient();
		turnOnLights();
	}//drawSetup	
	//turn on lights for this sketch
	public void turnOnLights(){
		lights(); 
		//shininess(.1f);
//		    ambientLight(55, 55, 55);
//		    lightSpecular(111, 111, 111);
//		    //directionalLight(255, 255, 255, -1,1,-1);
//		    directionalLight(111, 111, 111, -1,1,-1);
		//specular(111, 111, 111);			
	}
	public void setCamOrient(){rotateX(rx);rotateY(ry); rotateX(PI/(2.0f));		}//sets the rx, ry, pi/2 orientation of the camera eye	
	public void unSetCamOrient(){rotateX(-PI/(2.0f)); rotateY(-ry);   rotateX(-rx); }//reverses the rx,ry,pi/2 orientation of the camera eye - paints on screen and is unaffected by camera movement
	public void drawAxes(double len, float stW, myPoint ctr, int alpha, boolean centered){//axes using current global orientation
		pushMatrix();pushStyle();
			strokeWeight(stW);
			stroke(255,0,0,alpha);
			if(centered){line(ctr.x-len*.5f,ctr.y,ctr.z,ctr.x+len*.5f,ctr.y,ctr.z);stroke(0,255,0,alpha);line(ctr.x,ctr.y-len*.5f,ctr.z,ctr.x,ctr.y+len*.5f,ctr.z);stroke(0,0,255,alpha);line(ctr.x,ctr.y,ctr.z-len*.5f,ctr.x,ctr.y,ctr.z+len*.5f);} 
			else {		line(ctr.x,ctr.y,ctr.z,ctr.x+len,ctr.y,ctr.z);stroke(0,255,0,alpha);line(ctr.x,ctr.y,ctr.z,ctr.x,ctr.y+len,ctr.z);stroke(0,0,255,alpha);line(ctr.x,ctr.y,ctr.z,ctr.x,ctr.y,ctr.z+len);}
		popStyle();	popMatrix();	
	}//	drawAxes
	public void drawAxes(double len, float stW, myPoint ctr, myVector[] _axis, int alpha, boolean drawVerts){//RGB -> XYZ axes
		pushMatrix();pushStyle();
		if(drawVerts){
			show(ctr,3,gui_Black,gui_Black, false);
			for(int i=0;i<_axis.length;++i){show(myPoint._add(ctr, myVector._mult(_axis[i],len)),3,rgbClrs[i],rgbClrs[i], false);}
		}
		strokeWeight(stW);
		for(int i =0; i<3;++i){	setColorValStroke(rgbClrs[i],255);	showVec(ctr,len, _axis[i]);	}
		popStyle();	popMatrix();	
	}//	drawAxes
	public void drawAxes(double len, float stW, myPoint ctr, myVector[] _axis, int[] clr, boolean drawVerts){//all axes same color
		pushMatrix();pushStyle();
			if(drawVerts){
				show(ctr,2,gui_Black,gui_Black, false);
				for(int i=0;i<_axis.length;++i){show(myPoint._add(ctr, myVector._mult(_axis[i],len)),2,rgbClrs[i],rgbClrs[i], false);}
			}
			strokeWeight(stW);stroke(clr[0],clr[1],clr[2],clr[3]);
			for(int i =0; i<3;++i){	showVec(ctr,len, _axis[i]);	}
		popStyle();	popMatrix();	
	}//	drawAxes

	public void drawText(String str, double x, double y, double z, int clr){
		int[] c = getClr(clr,255);
		pushMatrix();	pushStyle();
			fill(c[0],c[1],c[2],c[3]);
			unSetCamOrient();
			translate((float)x,(float)y,(float)z);
			text(str,0,0,0);		
		popStyle();	popMatrix();	
	}//drawText	
	public void savePic(){		save(animPath + animFileName + ((animCounter < 10) ? "000" : ((animCounter < 100) ? "00" : ((animCounter < 1000) ? "0" : ""))) + animCounter + ".jpg");		animCounter++;		}
	public void line(double x1, double y1, double z1, double x2, double y2, double z2){line((float)x1,(float)y1,(float)z1,(float)x2,(float)y2,(float)z2 );}
	public void line(myPoint p1, myPoint p2){line((float)p1.x,(float)p1.y,(float)p1.z,(float)p2.x,(float)p2.y,(float)p2.z);}
	public void drawOnScreenData(){
		if(flags[debugMode]){
			pushMatrix();pushStyle();			
			reInitInfoStr();
			addInfoStr(0,"mse loc on screen : " + new myPoint(mouseX, mouseY,0) + " mse loc in world :"+canvas.mseLoc +"  Eye loc in world :"+ canvas.eyeInWorld); 
			String[] res = ((mySideBarMenu)dispWinFrames[dispMenuIDX]).getDebugData();		//get debug data for each UI object
			//for(int s=0;s<res.length;++s) {	addInfoStr(res[s]);}				//add info to string to be displayed for debug
			int numToPrint = min(res.length,80);
			for(int s=0;s<numToPrint;++s) {	addInfoStr(res[s]);}				//add info to string to be displayed for debug
			drawInfoStr(1.0f); 	
			popStyle();	popMatrix();		
		}
		else if(showInfo){
			pushMatrix();pushStyle();			
			reInitInfoStr();	
			if(showInfo){
//			      addInfoStr(0,"Click the light green box to the left to toggle showing this message.");
//			      addInfoStr(1,"--Shift-Click-Drag to change view.  Shift-RClick-Drag to zoom.");
			 // addInfoStr(3,"Values at Mouse Location : "+ values at mouse location);
			}
			String[] res = consoleStrings.toArray(new String[0]);
			int dispNum = min(res.length, 80);
			for(int i=0;i<dispNum;++i){addInfoStr(res[i]);}
			drawInfoStr(1.1f); 
			popStyle();	popMatrix();	
		}
	}
	//print out multiple-line text to screen
	public void ml_text(String str, float x, float y){
		String[] res = str.split("\\r?\\n");
		float disp = 0;
		for(int i =0; i<res.length; ++i){
			text(res[i],x, y+disp);		//add console string output to screen display- decays over time
			disp += 12;
		}
	}
	
	public void outStr2Scr(String str){outStr2Scr(str,true);}
	//print informational string data to console, and to screen
	public void outStr2Scr(String str, boolean showDraw){
		if(trim(str) != ""){	System.out.println(str);}
		String[] res = str.split("\\r?\\n");
		if(showDraw){
			for(int i =0; i<res.length; ++i){
				consoleStrings.add(res[i]);		//add console string output to screen display- decays over time
			}
		}
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	// canvas functions
	
	public myVector getUScrUpInWorld(){		return canvas.getUScrUpInWorld();}	
	public myVector getUScrRightInWorld(){		return canvas.getUScrRightInWorld();}
	
	public myPoint getMseLoc(){			return canvas.getMseLoc();}
	public myPoint getMseLoc(myPoint glbTrans){			return canvas.getMseLoc(glbTrans);}
	public myPoint getEyeLoc(){			return canvas.getEyeLoc();	}
	public myPoint getOldMseLoc(){		return canvas.getOldMseLoc();	}	
	public myVector getMseDragVec(){	return canvas.getMseDragVec();}

	public myVector getMse2DtoMse3DinWorld(myPoint glbTrans) { return canvas.getMse2DtoMse3DinWorld(glbTrans);}
	
	public void scribeHeaderRight(String s) {scribeHeaderRight(s, 20);} // writes black on screen top, right-aligned
	public void scribeHeaderRight(String s, float y) {fill(0); text(s,width-6*s.length(),y); noFill();} // writes black on screen top, right-aligned
	public void displayHeader() { // Displays title and authors face on screen
		float stVal = 17;
		int idx = 1;	
		translate(0,10,0);
		fill(0); text("Shift-Click-Drag to change view.",width-190, stVal*idx++); noFill(); 
		fill(0); text("Shift-RClick-Drag to zoom.",width-160, stVal*idx++); noFill();
		fill(0); text("John Turner",width-75, stVal*idx++); noFill();			
		image(jtFace,  width-111,stVal*idx,100,100);
		}
	
	//project passed point onto box surface based on location - to help visualize the location in 3d
	public void drawProjOnBox(myPoint p){
		//myPoint[]  projOnPlanes = new myPoint[6];
		myPoint prjOnPlane;
		//public myPoint intersectPl(myPoint E, myVector T, myPoint A, myPoint B, myPoint C) { // if ray from E along T intersects triangle (A,B,C), return true and set proposal to the intersection point
		pushMatrix();
		translate(-p.x,-p.y,-p.z);
		for(int i  = 0; i< 6; ++i){				
			prjOnPlane = bndChkInCntrdBox3D(intersectPl(p, boxNorms[i], boxWallPts[i][0],boxWallPts[i][1],boxWallPts[i][2]));				
			show(prjOnPlane,5,rgbClrs[i/2],rgbClrs[i/2], false);				
		}
		popMatrix();
	}//drawProjOnBox
	
	public myPoint bndChkInBox2D(myPoint p){p.set(Math.max(0,Math.min(p.x,grid2D_X)),Math.max(0,Math.min(p.y,grid2D_Y)),0);return p;}
	public myPoint bndChkInBox3D(myPoint p){p.set(Math.max(0,Math.min(p.x,gridDimX)), Math.max(0,Math.min(p.y,gridDimY)),Math.max(0,Math.min(p.z,gridDimZ)));return p;}	
	public myPoint bndChkInCntrdBox3D(myPoint p){
		p.set(Math.max(-hGDimX,Math.min(p.x,hGDimX)), 
				Math.max(-hGDimY,Math.min(p.y,hGDimY)),
				Math.max(-hGDimZ,Math.min(p.z,hGDimZ)));return p;}	
	 
	public void translate(myPoint p){translate((float)p.x,(float)p.y,(float)p.z);}
	public void translate(myVector p){translate((float)p.x,(float)p.y,(float)p.z);}
	public void translate(double x, double y, double z){translate((float)x,(float)y,(float)z);}
	public void translate(double x, double y){translate((float)x,(float)y);}
	public void rotate(float thet, myPoint axis){rotate(thet, (float)axis.x,(float)axis.y,(float)axis.z);}
	public void rotate(float thet, double x, double y, double z){rotate(thet, (float)x,(float)y,(float)z);}
	//************************************************************************
	//**** SPIRAL
	//************************************************************************
	//3d rotation - rotate P by angle a around point G and axis normal to plane IJ
	public myPoint R(myPoint P, double a, myVector I, myVector J, myPoint G) {
		double x= myVector._dot(new myVector(G,P),U(I)), y=myVector._dot(new myVector(G,P),U(J)); 
		double c=Math.cos(a), s=Math.sin(a); 
		double iXVal = x*c-x-y*s, jYVal= x*s+y*c-y;			
		return myPoint._add(P,iXVal,I,jYVal,J); }; 
		
	public cntlPt R(cntlPt P, double a, myVector I, myVector J, myPoint G) {
		double x= myVector._dot(new myVector(G,P),U(I)), y=myVector._dot(new myVector(G,P),U(J)); 
		double c=Math.cos(a), s=Math.sin(a); 
		double iXVal = x*c-x-y*s, jYVal= x*s+y*c-y;		
		return new cntlPt(this, P(P,iXVal,I,jYVal,J), P.r, P.w); }; 

		
	public myPoint PtOnSpiral(myPoint A, myPoint B, myPoint C, double t) {
		//center is coplanar to A and B, and coplanar to B and C, but not necessarily coplanar to A, B and C
		//so center will be coplanar to mp(A,B) and mp(B,C) - use mpCA midpoint to determine plane mpAB-mpBC plane?
		myPoint mAB = new myPoint(A,.5f, B);
		myPoint mBC = new myPoint(B,.5f, C);
		myPoint mCA = new myPoint(C,.5f, A);
		myVector mI = U(mCA,mAB);
		myVector mTmp = myVector._cross(mI,U(mCA,mBC));
		myVector mJ = U(mTmp._cross(mI));	//I and J are orthonormal
		double a =spiralAngle(A,B,B,C); 
		double s =spiralScale(A,B,B,C);
		
		//myPoint G = spiralCenter(a, s, A, B, mI, mJ); 
		myPoint G = spiralCenter(A, mAB, B, mBC); 
		return new myPoint(G, Math.pow(s,t), R(A,t*a,mI,mJ,G));
	  }
	

	public double spiralAngle(myPoint A, myPoint B, myPoint C, myPoint D) {return myVector._angleBetween(new myVector(A,B),new myVector(C,D));}
	public double spiralScale(myPoint A, myPoint B, myPoint C, myPoint D) {return myPoint._dist(C,D)/ myPoint._dist(A,B);}
	
	public myPoint R(myPoint Q, myPoint C, myPoint P, myPoint R) { // returns rotated version of Q by angle(CP,CR) parallel to plane (C,P,R)
		myVector I0=U(C,P), I1=U(C,R), V=new myVector(C,Q); 
		double c=myPoint._dist(I0,I1), s=Math.sqrt(1.-(c*c)); 
		if(Math.abs(s)<0.00001) return Q;
		myVector J0=V(1./s,I1,-c/s,I0);  
		myVector J1=V(-s,I0,c,J0);  
		double x=V._dot(I0), y=V._dot(J0);  
		return P(Q,x,M(I1,I0),y,M(J1,J0)); 
	} 	
	// spiral given 4 points, AB and CD are edges corresponding through rotation
	public myPoint spiralCenter(myPoint A, myPoint B, myPoint C, myPoint D) {         // new spiral center
		myVector AB=V(A,B), CD=V(C,D), AC=V(A,C);
		double m=CD.magn/AB.magn, n=CD.magn*AB.magn;		
		myVector rotAxis = U(AB._cross(CD));		//expect ab and ac to be coplanar - this is the axis to rotate around to find f
		
		myVector rAB = myVector._rotAroundAxis(AB, rotAxis, MyMathUtils.HALF_PI_F);
		double c=AB._dot(CD)/n, 
				s=rAB._dot(CD)/n;
		double AB2 = AB._dot(AB), a=AB._dot(AC)/AB2, b=rAB._dot(AC)/AB2;
		double x=(a-m*( a*c+b*s)), y=(b-m*(-a*s+b*c));
		double d=1+m*(m-2*c);  if((c!=1)&&(m!=1)) { x/=d; y/=d; };
		return P(P(A,x,AB),y,rAB);
	  }
	
	
	public void cylinder(myPoint A, myPoint B, float r, int c1, int c2) {
		myPoint P = A;
		myVector V = V(A,B);
		myVector I = canvas.drawSNorm;//U(Normal(V));
		myVector J = U(N(I,V));
		float da = TWO_PI/36;
		beginShape(QUAD_STRIP);
			for(float a=0; a<=TWO_PI+da; a+=da) {fill(c1); gl_vertex(P(P,r*cos(a),I,r*sin(a),J,0,V)); fill(c2); gl_vertex(P(P,r*cos(a),I,r*sin(a),J,1,V));}
		endShape();
	}
	
	//point functions
	public myPoint P() {return new myPoint(); };                                                                          // point (x,y,z)
	public myPoint P(double x, double y, double z) {return new myPoint(x,y,z); };                                            // point (x,y,z)
	public myPoint P(myPoint A) {return new myPoint(A.x,A.y,A.z); };                                                           // copy of point P
	public myPoint P(myPoint A, double s, myPoint B) {return new myPoint(A.x+s*(B.x-A.x),A.y+s*(B.y-A.y),A.z+s*(B.z-A.z)); };        // A+sAB
	public myPoint L(myPoint A, double s, myPoint B) {return new myPoint(A.x+s*(B.x-A.x),A.y+s*(B.y-A.y),A.z+s*(B.z-A.z)); };        // A+sAB
	public myPoint P(myPoint A, myPoint B) {return P((A.x+B.x)/2.0,(A.y+B.y)/2.0,(A.z+B.z)/2.0); }                             // (A+B)/2
	public myPoint P(myPoint A, myPoint B, myPoint C) {return new myPoint((A.x+B.x+C.x)/3.0,(A.y+B.y+C.y)/3.0,(A.z+B.z+C.z)/3.0); };     // (A+B+C)/3
	public myPoint P(myPoint A, myPoint B, myPoint C, myPoint D) {return P(P(A,B),P(C,D)); };                                            // (A+B+C+D)/4
	public myPoint P(double s, myPoint A) {return new myPoint(s*A.x,s*A.y,s*A.z); };                                            // sA
	public myPoint A(myPoint A, myPoint B) {return new myPoint(A.x+B.x,A.y+B.y,A.z+B.z); };                                         // A+B
	public myPoint P(double a, myPoint A, double b, myPoint B) {return A(P(a,A),P(b,B));}                                        // aA+bB 
	public myPoint P(double a, myPoint A, double b, myPoint B, double c, myPoint C) {return A(P(a,A),P(b,B,c,C));}                     // aA+bB+cC 
	public myPoint P(double a, myPoint A, double b, myPoint B, double c, myPoint C, double d, myPoint D){return A(P(a,A,b,B),P(c,C,d,D));}   // aA+bB+cC+dD
	public myPoint P(myPoint P, myVector V) {return new myPoint(P.x + V.x, P.y + V.y, P.z + V.z); }                                 // P+V
	public myPoint P(myPoint P, double s, myVector V) {return new myPoint(P.x+s*V.x,P.y+s*V.y,P.z+s*V.z);}                           // P+sV
	public myPoint P(myPoint O, double x, myVector I, double y, myVector J) {return P(O.x+x*I.x+y*J.x,O.y+x*I.y+y*J.y,O.z+x*I.z+y*J.z);}  // O+xI+yJ
	public myPoint P(myPoint O, double x, myVector I, double y, myVector J, double z, myVector K) {return P(O.x+x*I.x+y*J.x+z*K.x,O.y+x*I.y+y*J.y+z*K.y,O.z+x*I.z+y*J.z+z*K.z);}  // O+xI+yJ+kZ
	void makePts(myPoint[] C) {for(int i=0; i<C.length; i++) C[i]=P();}

	//draw a circle - JT
	public void circle(myPoint P, float r, myVector I, myVector J, int n) {myPoint[] pts = new myPoint[n];pts[0] = P(P,r,U(I));float a = (2*PI)/(1.0f*n);for(int i=1;i<n;++i){pts[i] = R(pts[i-1],a,J,I,P);}pushMatrix(); pushStyle();noFill(); show(pts);popStyle();popMatrix();}; // render sphere of radius r and center P
	
	public void circle(myPoint p, float r){ellipse((float)p.x, (float)p.y, r, r);}
	void circle(float x, float y, float r1, float r2){ellipse(x,y, r1, r2);}
	
	public void noteArc(float[] dims, int[] noteClr){
		noFill();
		setStroke(noteClr);
		strokeWeight(1.5f*dims[3]);
		arc(0,0, dims[2], dims[2], dims[0] - MyMathUtils.HALF_PI_F, dims[1] - MyMathUtils.HALF_PI_F);
	}
	//draw a ring segment from alphaSt in radians to alphaEnd in radians
	void noteArc(myPoint ctr, float alphaSt, float alphaEnd, float rad, float thickness, int[] noteClr){
		noFill();
		setStroke(noteClr);
		strokeWeight(thickness);
		arc((float)ctr.x, (float)ctr.y, rad, rad, alphaSt - MyMathUtils.HALF_PI_F, alphaEnd- MyMathUtils.HALF_PI_F);
	}
	

	public void bezier(myPoint [] C) {bezier(C[0],C[1],C[2],C[3]);} // draws a cubic Bezier curve with control points A, B, C, D
	
	public myPoint Mouse() {return new myPoint(mouseX, mouseY,0);}                                          			// current mouse location
	public myVector MouseDrag() {return new myVector(mouseX-pmouseX,mouseY-pmouseY,0);};                     			// vector representing recent mouse displacement
	
	//public int color(myPoint p){return color((int)p.x,(int)p.z,(int)p.y);}	//needs to be x,z,y for some reason - to match orientation of color frames in z-up 3d geometry
	public int color(myPoint p){return color((int)p.x,(int)p.y,(int)p.z);}	
	
	// =====  vector functions
	public myVector V() {return new myVector(); };                                                                          // make vector (x,y,z)
	public myVector V(double x, double y, double z) {return new myVector(x,y,z); };                                            // make vector (x,y,z)
	public myVector V(myVector V) {return new myVector(V.x,V.y,V.z); };                                                          // make copy of vector V
	public myVector A(myVector A, myVector B) {return new myVector(A.x+B.x,A.y+B.y,A.z+B.z); };                                       // A+B
	public myVector A(myVector U, float s, myVector V) {return V(U.x+s*V.x,U.y+s*V.y,U.z+s*V.z);};                               // U+sV
	public myVector M(myVector U, myVector V) {return V(U.x-V.x,U.y-V.y,U.z-V.z);};                                              // U-V
	public myVector M(myVector V) {return V(-V.x,-V.y,-V.z);};                                                              // -V
	public myVector V(myVector A, myVector B) {return new myVector((A.x+B.x)/2.0,(A.y+B.y)/2.0,(A.z+B.z)/2.0); }                      // (A+B)/2
	public myVector V(myVector A, float s, myVector B) {return new myVector(A.x+s*(B.x-A.x),A.y+s*(B.y-A.y),A.z+s*(B.z-A.z)); };      // (1-s)A+sB
	public myVector V(myVector A, myVector B, myVector C) {return new myVector((A.x+B.x+C.x)/3.0,(A.y+B.y+C.y)/3.0,(A.z+B.z+C.z)/3.0); };  // (A+B+C)/3
	public myVector V(myVector A, myVector B, myVector C, myVector D) {return V(V(A,B),V(C,D)); };                                         // (A+B+C+D)/4
	public myVector V(double s, myVector A) {return new myVector(s*A.x,s*A.y,s*A.z); };                                           // sA
	public myVector V(double a, myVector A, double b, myVector B) {return A(V(a,A),V(b,B));}                                       // aA+bB 
	public myVector V(double a, myVector A, double b, myVector B, double c, myVector C) {return A(V(a,A,b,B),V(c,C));}                   // aA+bB+cC
	public myVector V(myPoint P, myPoint Q) {return new myVector(P,Q);};                                          // PQ
	public myVector N(myVector U, myVector V) {return V( U.y*V.z-U.z*V.y, U.z*V.x-U.x*V.z, U.x*V.y-U.y*V.x); };                  // UxV cross product (normal to both)
	public myVector N(myPoint A, myPoint B, myPoint C) {return N(V(A,B),V(A,C)); };                                                   // normal to triangle (A,B,C), not normalized (proportional to area)
	public myVector B(myVector U, myVector V) {return U(N(N(U,V),U)); }        


	public double d(myVector U, myVector V) {return U.x*V.x+U.y*V.y+U.z*V.z; };                                            //U*V dot product
	public double dot(myVector U, myVector V) {return U.x*V.x+U.y*V.y+U.z*V.z; };                                            //U*V dot product
	public double det2(myVector U, myVector V) {return -U.y*V.x+U.x*V.y; };                                       		// U|V det product
	public double det3(myVector U, myVector V) {double dist = d(U,V); return Math.sqrt(d(U,U)*d(V,V) - (dist*dist)); };                                // U|V det product
	public double m(myVector U, myVector V, myVector W) {return d(U,N(V,W)); };                                                 // (UxV)*W  mixed product, determinant - measures 6x the volume of the parallelapiped formed by myVectortors
	public double m(myPoint E, myPoint A, myPoint B, myPoint C) {return m(V(E,A),V(E,B),V(E,C));}                                    // det (EA EB EC) is >0 when E sees (A,B,C) clockwise
	public double n2(myVector V) {return (V.x*V.x)+(V.y*V.y)+(V.z*V.z);};                                                   // V*V    norm squared
	public double n(myVector V) {return  Math.sqrt(n2(V));};                                                                // ||V||  norm
	public double d(myPoint P, myPoint Q) {return  myPoint._dist(P, Q); };                            // ||AB|| distance
	public double area(myPoint A, myPoint B, myPoint C) {return n(N(A,B,C))/2; };                                               // area of triangle 
	public double volume(myPoint A, myPoint B, myPoint C, myPoint D) {return m(V(A,B),V(A,C),V(A,D))/6; };                           // volume of tet 
	public boolean parallel (myVector U, myVector V) {return n(N(U,V))<n(U)*n(V)*0.00001; }                              // true if U and V are almost parallel
	public double angle(myPoint A, myPoint B, myPoint C){return angle(V(A,B),V(A,C));}												//angle between AB and AC
	public double angle(myPoint A, myPoint B, myPoint C, myPoint D){return angle(U(A,B),U(C,D));}							//angle between AB and CD
	public double angle(myVector U, myVector V){double angle = Math.atan2(n(N(U,V)),d(U,V)),sign = m(U,V,V(0,0,1));if(sign<0){    angle=-angle;}	return angle;}
	public boolean cw(myVector U, myVector V, myVector W) {return m(U,V,W)>0; };                                               // (UxV)*W>0  U,V,W are clockwise
	public boolean cw(myPoint A, myPoint B, myPoint C, myPoint D) {return volume(A,B,C,D)>0; };                                     // tet is oriented so that A sees B, C, D clockwise 
	public boolean projectsBetween(myPoint P, myPoint A, myPoint B) {return dot(V(A,P),V(A,B))>0 && dot(V(B,P),V(B,A))>0 ; };
	public double distToLine(myPoint P, myPoint A, myPoint B) {double res = det3(U(A,B),V(A,P)); return Double.isNaN(res) ? 0 : res; };		//MAY RETURN NAN IF point P is on line
	public myPoint projectionOnLine(myPoint P, myPoint A, myPoint B) {return P(A,dot(V(A,B),V(A,P))/dot(V(A,B),V(A,B)),V(A,B));}
	public boolean isSame(myPoint A, myPoint B) {return (A.x==B.x)&&(A.y==B.y)&&(A.z==B.z) ;}                                         // A==B
	public boolean isSame(myPoint A, myPoint B, double e) {return ((Math.abs(A.x-B.x)<e)&&(Math.abs(A.y-B.y)<e)&&(Math.abs(A.z-B.z)<e));}                   // ||A-B||<e
	
	public myVector W(double s,myVector V) {return V(s*V.x,s*V.y,s*V.z);}                                                      // sV

	public myVector U(myVector v){myVector u = new myVector(v); return u._normalize(); }
	public myVector U(myVector v, float d, myVector u){myVector r = new myVector(v,d,u); return r._normalize(); }
	public myVector Upt(myPoint v){myVector u = new myVector(v); return u._normalize(); }
	public myVector U(myPoint a, myPoint b){myVector u = new myVector(a,b); return u._normalize(); }
	public myVector U(double x, double y, double z) {myVector u = new myVector(x,y,z); return u._normalize();}
	
	public myVector normToPlane(myPoint A, myPoint B, myPoint C) {return myVector._cross(new myVector(A,B),new myVector(A,C)); };   // normal to triangle (A,B,C), not normalized (proportional to area)

	public void gl_normal(myVector V) {normal((float)V.x,(float)V.y,(float)V.z);}                                          // changes normal for smooth shading
	public void gl_vertex(myPoint P) {vertex((float)P.x,(float)P.y,(float)P.z);}                                           // vertex for shading or drawing
	public void show(myPoint P, double r,int fclr, int sclr, boolean flat) {//TODO make flat circles for points if flat
		pushMatrix(); pushStyle(); 
		if((fclr!= -1) && (sclr!= -1)){setColorValFill(fclr, 255); setColorValStroke(sclr, 255);}
		if(!flat){
			translate((float)P.x,(float)P.y,(float)P.z); 
			sphereDetail(5);
			sphere((float)r);
		} else {
			translate((float)P.x,(float)P.y,0); 
			this.circle(0,0,(float)r,(float)r);				
		}
		popStyle(); popMatrix();} // render sphere of radius r and center P)
	public void show(myPoint P, double r){show(P,r, gui_Black, gui_Black, false);}
	public void show(myPoint P, String s) {text(s, (float)P.x, (float)P.y, (float)P.z); } // prints string s in 3D at P


	public void show(myPoint P, double r, String s, myVector D){show(P,r, gui_Black, gui_Black, false);pushStyle();setColorValFill(gui_Black, 255);show(P,s,D);popStyle();}
	public void show(myPoint P, double r, String s, myVector D, int clr, boolean flat){show(P,r, clr, clr, flat);pushStyle();setColorValFill(clr, 255);show(P,s,D);popStyle();}
	public void show(myPoint P, String s, myVector D) {text(s, (float)(P.x+D.x), (float)(P.y+D.y), (float)(P.z+D.z));  } // prints string s in 3D at P+D
	public void curveVertex(myPoint P) {curveVertex((float)P.x,(float)P.y);};                                           // curveVertex for shading or drawing
	public void curve(myPoint[] ara) {if(ara.length == 0){return;}beginShape(); curveVertex(ara[0]);for(int i=0;i<ara.length;++i){curveVertex(ara[i]);} curveVertex(ara[ara.length-1]);endShape();};                      // volume of tet 
	
	
	public boolean intersectPl(myPoint E, myVector T, myPoint A, myPoint B, myPoint C, myPoint X) { // if ray from E along T intersects triangle (A,B,C), return true and set proposal to the intersection point
		myVector EA=new myVector(E,A), AB=new myVector(A,B), AC=new myVector(A,C); 		double t = (float)(myVector._mixProd(EA,AC,AB) / myVector._mixProd(T,AC,AB));		X.set(myPoint._add(E,t,T));		return true;
	}	
	public myPoint intersectPl(myPoint E, myVector T, myPoint A, myPoint B, myPoint C) { // if ray from E along T intersects triangle (A,B,C), return true and set proposal to the intersection point
		myVector EA=new myVector(E,A), AB=new myVector(A,B), AC=new myVector(A,C); 		
		double t = (float)(myVector._mixProd(EA,AC,AB) / myVector._mixProd(T,AC,AB));		
		return (myPoint._add(E,t,T));		
	}	
	// if ray from E along V intersects sphere at C with radius r, return t when intersection occurs
	public double intersectPt(myPoint E, myVector V, myPoint C, double r) { 
		myVector Vce = V(C,E);
		double CEdCE = Vce._dot(Vce), VdV = V._dot(V), VdVce = V._dot(Vce), b = 2 * VdVce, c = CEdCE - (r*r),
				radical = (b*b) - 4 *(VdV) * c;
		if(radical < 0) return -1;
		double t1 = (b + Math.sqrt(radical))/(2*VdV), t2 = (b - Math.sqrt(radical))/(2*VdV);			
		return ((t1 > 0) && (t2 > 0) ? Math.min(t1, t2) : ((t1 < 0 ) ? ((t2 < 0 ) ? -1 : t2) : t1) );
		
	}	
	
	public void rect(float[] a){rect(a[0],a[1],a[2],a[3]);}				//rectangle from array of floats : x, y, w, h
	
	

/////////////////////		
///color utils
/////////////////////
	public final int  // set more colors using Menu >  Tools > Color Selector
	  black=0xff000000, 
	  white=0xffFFFFFF,
	  red=0xffFF0000, 
	  green=0xff00FF00, 
	  blue=0xff0000FF, 
	  yellow=0xffFFFF00, 
	  cyan=0xff00FFFF, 
	  magenta=0xffFF00FF,
	  grey=0xff818181, 
	  orange=0xffFFA600, 
	  brown=0xffB46005, 
	  metal=0xffB5CCDE, 
	  dgreen=0xff157901;
	//set color based on passed point r= x, g = z, b=y
	public void fillAndShowLineByRBGPt(myPoint p, float x,  float y, float w, float h){
		fill((int)p.x,(int)p.y,(int)p.z);
		stroke((int)p.x,(int)p.y,(int)p.z);
		rect(x,y,w,h);
		//show(p,r,-1);
	}
	
	public myPoint WrldToScreen(myPoint wPt){return new myPoint(screenX((float)wPt.x,(float)wPt.y,(float)wPt.z),screenY((float)wPt.x,(float)wPt.y,(float)wPt.z),screenZ((float)wPt.x,(float)wPt.y,(float)wPt.z));}

	public int[][] triColors = new int[][] {
		{gui_DarkMagenta,gui_DarkBlue,gui_DarkGreen,gui_DarkCyan}, 
		{gui_LightMagenta,gui_LightBlue,gui_LightGreen,gui_TransCyan}};
		
	public void setFill(int[] clr){setFill(clr,clr[3]);}
	public void setStroke(int[] clr){setStroke(clr,clr[3]);}		
	public void setFill(int[] clr, int alpha){fill(clr[0],clr[1],clr[2], alpha);}
	public void setStroke(int[] clr, int alpha){stroke(clr[0],clr[1],clr[2], alpha);}

	
	public int getRndClrInt(){return (int)random(0,23);}		//return a random color flag value from below
	public int[] getRndClr(){return getRndClr(255);	}		
	public Integer[] getClrMorph(int a, int b, double t){return getClrMorph(getClr(a, 255), getClr(b, 255), t);}    


	
	
	
	
	
	
	@Override
	public void setRenderBackground(int r, int g, int b, int alpha) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void loadBkgndSphere(String filename) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void setBkgndSphere() {
		// TODO Auto-generated method stub
		
	}
	
	
	@Override
	public void initRenderInterface() {
		//initialize constants
		
	}
	@Override
	public GLWindow getGLWindow() {return (GLWindow)getSurface().getNative();}
	///////////////////////////////////////////
	// draw routines
	protected static int pushPopAllDepth = 0, pushPopJustStyleDepth = 0;
	/**
	 * push matrix, and style (if available) - must be paired with pop matrix/style calls
	 */
	@Override
	public final int pushMatState() {	super.pushMatrix();super.pushStyle();return ++pushPopAllDepth;}
	/**
	 * pop style (if supported) and matrix - must be called after equivalent pushes
	 */
	@Override
	public final int popMatState() {	super.popStyle();super.popMatrix();	return --pushPopAllDepth;}
	
	/**
	 * push current style/color params onto "style stack" (save current settings)
	 */	
	@Override
	public int pushJustStyleState() {	super.pushStyle();return ++pushPopJustStyleDepth;}
	/**
	 * pop current style/color params from "style stack" (restore/overwrite with last saved settings)
	 */
	@Override
	public int popJustStyleState(){		super.popStyle();return --pushPopJustStyleDepth;}
	
//
//	/**
//	 * main draw loop - override if handling draw differently
//	 */
//	@Override
//	public void draw(){
//		//returns whether actually drawn or not
//		if(!AppMgr.mainSimAndDrawLoop()) {return;}
//		//TODO find better mechanism for saving screenshot
//		if (AppMgr.doSaveAnim()) {	savePic();}
//	}//draw	
//	
	/**
	 * Builds and sets window title
	 */
	@Override
	public void setWindowTitle(String applicationTitle, String windowName) {
		//build window title
		surface.setTitle(applicationTitle + " : " + (int)(frameRate) + " fps|cyc curFocusWin : " + windowName);		
	}		
	
	/**
	 * draw a translucent representation of a canvas plane ortho to eye-to-mouse vector
	 * @param eyeToMse vector 
	 * @param canvas3D
	 */
	@Override
	public void drawCanvas(myVector eyeToMse, myPointf[] canvas3D){
		disableLights();
		pushMatState();
		gl_beginShape(GL_PrimStyle.GL_LINE_LOOP);
		setFill(255,255,255,80);
		setNoStroke();
		gl_normal(eyeToMse);
        for(int i =canvas3D.length-1;i>=0;--i){		//build invisible canvas to draw upon
     		//p.line(canvas3D[i], canvas3D[(i+1)%canvas3D.length]);
     		gl_vertex(canvas3D[i]);
     	}
     	gl_endShape(true);
     	popMatState();
     	enableLights();
	}//drawCanvas


	/**
	 * set perspective matrix based on frustum for camera
	 * @param left left coordinate of the clipping plane
	 * @param right right coordinate of the clipping plane
	 * @param bottom bottom coordinate of the clipping plane
	 * @param top top coordinate of the clipping plane
	 * @param near near component of the clipping plane (> 0)
	 * @param far far component of the clipping plane (> near)
	 */
	@Override
	public void setFrustum(float left, float right, float bottom, float top, float near, float far) {
		super.frustum(left, right, bottom, top, near, far);
	}
	
	/**
	 * set perspective projection matrix for camera
	 * @param fovy Vertical FOV
	 * @param ar Aspect Ratio 
	 * @param zNear Z position of near clipping plane
	 * @param zFar Z position of far clipping plane 
	 */
	@Override
	public void setPerspective(float fovy, float ar, float zNear, float zFar) {
		super.perspective(fovy, ar, zNear, zFar);
	}
	
	/**
	 * set orthographic projection matrix for camera (2d or 3d)
	 * @param left left plane of clipping volume
	 * @param right right plane of the clipping volume
	 * @param bottom bottom plane of the clipping volume
	 * @param top top plane of the clipping volume
	 * @param near maximum distance from the origin to the viewer
	 * @param far maximum distance from the origin away from the viewer
	 */
	@Override
	public void setOrtho(float left, float right, float bottom, float top) {
		super.ortho(left, right, bottom, top);
	}
	@Override
	public void setOrtho(float left, float right, float bottom, float top, float near, float far) {
		super.ortho(left, right, bottom, top, near, far);
	}
	
	
	@Override
	public void gl_normal(float x, float y, float z) {super.normal(x,y,z);}                                          // changes normal for smooth shading
	@Override
	public void gl_vertex(float x, float y, float z) {super.vertex(x,y,z);}                                             // vertex for shading or drawing

	/**
	 * set fill color by value during shape building
	 * @param clr 1st 3 values denot integer color vals
	 * @param alpha 
	 */
	@Override
	public void gl_SetFill(int r, int g, int b, int alpha) {super.fill(r,g,b,alpha);}

	/**
	 * set stroke color by value during shape building
	 * @param clr rgba
	 * @param alpha 
	 */
	@Override
	public void gl_SetStroke(int r, int g, int b, int alpha) {super.stroke(r,g,b,alpha);}	
	
	
	/**
	 * type needs to be -1 for blank, otherwise should be specified in PConstants
	 * 
	 * (from PConstants) - these are allowed elements in glBegin function
  static final int POINTS          = 3;   // vertices
  static final int LINES           = 5;   // beginShape(), createShape()
  static final int LINE_STRIP      = 50;  // beginShape()
  static final int LINE_LOOP       = 51;
  static final int TRIANGLES       = 9;   // vertices
  static final int TRIANGLE_STRIP  = 10;  // vertices
  static final int TRIANGLE_FAN    = 11;  // vertices
  
  DONOT SUPPORT QUAD PRIMS - have been deprecated/Removed from opengl
  static final int QUADS           = 17;  // vertices
  static final int QUAD_STRIP      = 18;  // vertices
  
  static final int POLYGON         = 20;  // 
	 * 
	 * 
	 */
	@Override
	public void gl_beginShape(GL_PrimStyle primType) {
		switch (primType) {
			case GL_POINTS : {
				beginShape(POINTS);
				return;
			}
			case GL_LINES : {
				beginShape(LINES);
				return;
			}
			case GL_LINE_LOOP : {
				//Processing does not support line loop, so treat as polygon
				beginShape(POLYGON);
				return;
			}
			case GL_LINE_STRIP : {
				//Processing does not support line_strip, treat as lines
				beginShape(LINES);
				break;
			}
			case GL_TRIANGLES : {
				beginShape(TRIANGLES);
				break;
			}
			case GL_TRIANGLE_STRIP : {
				beginShape(TRIANGLE_STRIP);
				break;
			}
			case GL_TRIANGLE_FAN : {
				beginShape(TRIANGLE_FAN);
				break;
			}
			default : {
				beginShape(POLYGON);	
				return;
			}		
		};
	}//gl_beginShape
	/**
	 * type needs to be -1 for blank, otherwise will be CLOSE, regardless of passed value
	 */
	@Override
	public void gl_endShape(boolean isClosed) {		
		if(isClosed) {			endShape(CLOSE);		}
		else {				endShape();		}
	}
	
	@Override
	public void drawSphere(float rad) {sphere(rad);}
	private int sphereDtl = 4;

	@Override
	public void setSphereDetail(int det) {sphereDtl=det;sphereDetail(det);}

	@Override
	public int getSphereDetail() {return sphereDtl;}
	
	/**
	 * draw a 2 d ellipse 
	 * @param x,y,x rad, y rad
	 */
	@Override
	public void drawEllipse2D(float x, float y, float xr, float yr) {ellipse(x,y,xr,yr);}

	
	@Override
	public void drawLine(float x1, float y1, float z1, float x2, float y2, float z2){line(x1,y1,z1,x2,y2,z2 );}
	@Override
	public void drawLine(myPointf a, myPointf b, int stClr, int endClr){
		gl_beginShape();
		this.setStrokeWt(1.0f);
		this.setColorValStroke(stClr, 255);
		this.gl_vertex(a);
		this.setColorValStroke(endClr,255);
		this.gl_vertex(b);
		gl_endShape();
	}
	@Override
	public void drawLine(myPointf a, myPointf b, int[] stClr, int[] endClr){
		gl_beginShape();
		this.setStrokeWt(1.0f);
		this.setStroke(stClr, 255);
		this.gl_vertex(a);
		this.setStroke(endClr,255);
		this.gl_vertex(b);
		gl_endShape();
	}
	
	/**
	 * draw a cloud of points with passed color values as an integrated shape
	 * @param numPts number of points to draw
	 * @param ptIncr incrementer between points, to draw only every 2nd, 3rd or more'th point
	 * @param h_part_clr_int 2d array of per point 3-color stroke values
	 * @param h_part_pos_x per point x value
	 * @param h_part_pos_y per point y value
	 * @param h_part_pos_z per point z value
	 */
	@Override
	public void drawPointCloudWithColors(int numPts, int ptIncr, int[][] h_part_clr_int, float[] h_part_pos_x, float[] h_part_pos_y, float[] h_part_pos_z) {
		gl_beginShape(GL_PrimStyle.GL_POINTS);
		for(int i=0;i<=numPts-ptIncr;i+=ptIncr) {	
			this.setStroke(h_part_clr_int[i][0], h_part_clr_int[i][1], h_part_clr_int[i][2], 255);
			this.gl_vertex(h_part_pos_x[i], h_part_pos_y[i], h_part_pos_z[i]);
		}
		gl_endShape();
	}//drawPointCloudWithColors	
	
	/**
	 * draw a cloud of points with all points having same color value as an integrated shape
	 * @param numPts number of points to draw
	 * @param ptIncr incrementer between points, to draw only every 2nd, 3rd or more'th point
	 * @param h_part_clr_int array of 3-color stroke values for all points
	 * @param h_part_pos_x per point x value
	 * @param h_part_pos_y per point y value
	 * @param h_part_pos_z per point z value
	 */
	@Override
	public void drawPointCloudWithColor(int numPts, int ptIncr, int[] h_part_clr_int, float[] h_part_pos_x, float[] h_part_pos_y, float[] h_part_pos_z) {
		gl_beginShape(GL_PrimStyle.GL_POINTS);
		this.setStroke(h_part_clr_int[0], h_part_clr_int[1], h_part_clr_int[2], 255);
		for(int i=0;i<=numPts-ptIncr;i+=ptIncr) {	
			this.gl_vertex(h_part_pos_x[i], h_part_pos_y[i], h_part_pos_z[i]);
		}
		gl_endShape();
	}//drawPointCloudWithColors	
	
	/**
	 * draw a box centered at origin with passed dimensions, in 3D
	 */
	@Override
	public void drawBox3D(int x, int y, int z) {box(x,y,z);};
	/**
	 * draw a rectangle in 2D using the passed values as x,y,w,h
	 * @param a 4 element array : x,y,w,h
	 */
	@Override
	public void drawRect(float a, float b, float c, float d){rect(a,b,c,d);}				//rectangle from array of floats : x, y, w, h
	/**
	 * Build a set of n points inscribed on a circle centered at p in plane I,J
	 * @param p center point
	 * @param r circle radius
	 * @param I, J axes of plane
	 * @param n # of points
	 * @return array of n equal-arc-length points centered around p
	 */
	public synchronized myPoint[] buildCircleInscribedPoints(myPoint p, float r, myVector I, myVector J,int n) {
		myPoint[] pts = new myPoint[n];
		pts[0] = new myPoint(p,r,myVector._unit(I));
		float a = (MyMathUtils.TWO_PI_F)/(1.0f*n); 
		for(int i=1;i<n;++i){pts[i] = pts[i-1].rotMeAroundPt(a,J,I,p);}
		return pts;
	}
	/**
	 * Build a set of n points inscribed on a circle centered at p in plane I,J
	 * @param p center point
	 * @param r circle radius
	 * @param I, J axes of plane
	 * @param n # of points
	 * @return array of n equal-arc-length points centered around p
	 */
	public synchronized myPointf[] buildCircleInscribedPoints(myPointf p, float r, myVectorf I, myVectorf J,int n) {
		myPointf[] pts = new myPointf[n];
		pts[0] = new myPointf(p,r,myVectorf._unit(I));
		float a = (MyMathUtils.TWO_PI_F)/(1.0f*n);
		for(int i=1;i<n;++i){pts[i] = pts[i-1].rotMeAroundPt(a,J,I,p);}
		return pts;
	}
	/**
	 * draw a circle centered at P with specified radius r in plane proscribed by passed axes using n number of points
	 * @param P center
	 * @param r radius
	 * @param I x axis
	 * @param J y axis
	 * @param n # of points to use
	 */
	@Override
	public void drawCircle3D(myPoint P, float r, myVector I, myVector J, int n) {
		myPoint[] pts = buildCircleInscribedPoints(P,r,I,J,n);
		pushMatState();noFill(); show(pts);popMatState();
	}
	@Override
	public void drawCircle3D(myPointf P, float r, myVectorf I, myVectorf J, int n) {
		myPointf[] pts = buildCircleInscribedPoints(P,r,I,J,n);
		pushMatState();noFill(); show(pts);popMatState();
	} 
	
	/**
	 * draw a 6 pointed star centered at p inscribed in circle radius r
	 */
	@Override
	public void drawStar2D(myPointf p, float r) {
		myPointf[] pts = buildCircleInscribedPoints(p,r,myVectorf.FORWARD,myVectorf.RIGHT,6);
		drawTriangle2D(pts[0], pts[2],pts[4]);
		drawTriangle2D(pts[1], pts[3],pts[5]);
	}
	/**
	 * draw a triangle at 3 locations in 2D (only uses x,y)
	 * @param a
	 * @param b
	 * @param c
	 */
	@Override
	public void drawTriangle2D(myPointf a, myPointf b, myPointf c) {triangle(a.x,a.y, b.x, b.y, c.x, c.y);}
	/**
	 * draw a triangle at 3 locations in 2D (only uses x,y)
	 * @param a
	 * @param b
	 * @param c
	 */
	@Override
	public void drawTriangle2D(myPoint a, myPoint b, myPoint c) {triangle((float)a.x,(float)a.y,(float) b.x, (float)b.y,(float) c.x,(float) c.y);}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	// calculations
	
	/**
	 * build a frame based on world orientation given two passed points
	 * @param A
	 * @param B
	 * @return vec array of {AB, ScreenNorm, ScreenTan}
	 */
	public myVector[] buildViewBasedFrame(myPoint A, myPoint B) {
		myVector V = new myVector(A,B);
		myVector I = canvas.getDrawSNorm();//U(Normal(V));
		myVector J = I._cross(V)._normalize(); 
		return new myVector[] {V,I,J};		
	}
	
	/**
	 * build a frame based on world orientation given two passed points
	 * @param A
	 * @param B
	 * @return float vec array of {AB, ScreenNorm, ScreenTan}
	 */
	public myVectorf[] buildViewBasedFrame_f(myPointf A, myPointf B) {
		myVectorf V = new myVectorf(A,B);
		myVectorf I = canvas.getDrawSNorm_f();//U(Normal(V));
		myVectorf J = I._cross(V)._normalize(); 
		return new myVectorf[] {V,I,J};		
	}
	
	
	/**
	 * Derive the points of a cylinder of radius r around axis through A and B
	 * @param A center point of endcap
	 * @param B center point of endcap
	 * @param r desired radius of cylinder
	 * @return array of points for cylinder
	 */
	public myPoint[] buildCylVerts(myPoint A, myPoint B, float r) {
		myVector[] frame = buildViewBasedFrame(A, B);
		myPoint[] resList = new myPoint[2 * cylCosVals.length];
		double rca, rsa;
		int idx = 0;
		for(int i = 0; i<cylCosVals.length; ++i) {
			rca = r*cylCosVals[i];rsa=r*cylSinVals[i];
			resList[idx++] = myPoint._add(A,rca,frame[1],rsa,frame[2]); 
			resList[idx++] = myPoint._add(A,rca,frame[1],rsa,frame[2],1,frame[0]);				
		}
		return resList;
	}//build list of all cylinder vertices 
	
	/**
	 * Derive the points of a cylinder of radius r around axis through A and B
	 * @param A center point of endcap
	 * @param B center point of endcap
	 * @param r desired radius of cylinder
	 * @return array of points for cylinder
	 */
	public myPointf[] buildCylVerts(myPointf A, myPointf B, float r) {
		myVectorf[] frame = buildViewBasedFrame_f(A, B);
		myPointf[] resList = new myPointf[2 * cylCosVals.length];
		float rca, rsa;
		int idx = 0;
		for(int i = 0; i<cylCosVals.length; ++i) {
			rca = (float) (r*cylCosVals[i]);rsa=(float) (r*cylSinVals[i]);
			resList[idx++] = myPointf._add(A,rca,frame[1],rsa,frame[2]); 
			resList[idx++] = myPointf._add(A,rca,frame[1],rsa,frame[2],1,frame[0]);				
		}	
		return resList;
	}//build list of all cylinder vertices 
	
	
	@Override
	public void drawCylinder_NoFill(myPoint A, myPoint B, float r, int clr1, int clr2) {
		myPoint[] vertList = buildCylVerts(A, B, r);
		int[] c1 = getClr(clr1, 255);
		int[] c2 = getClr(clr2, 255);
		noFill();
		beginShape(QUAD_STRIP);
			for(int i=0; i<vertList.length; i+=2) {
				gl_SetStroke(c1[0],c1[1],c1[2],255);
				gl_vertex(vertList[i]);
				gl_SetStroke(c2[0],c2[1],c2[2],255);
				gl_vertex(vertList[i+1]);}
		gl_endShape();
	}
	@Override
	public void drawCylinder_NoFill(myPointf A, myPointf B, float r, int clr1, int clr2) {
		myPointf[] vertList = buildCylVerts(A, B, r);
		int[] c1 = getClr(clr1, 255);
		int[] c2 = getClr(clr2, 255);
		noFill();
		beginShape(QUAD_STRIP);
			for(int i=0; i<vertList.length; i+=2) {
				gl_SetStroke(c1[0],c1[1],c1[2],255);
				gl_vertex(vertList[i]); 
				gl_SetStroke(c2[0],c2[1],c2[2],255);
				gl_vertex(vertList[i+1]);}
		gl_endShape();
	}

	@Override
	public void drawCylinder(myPoint A, myPoint B, float r, int clr1, int clr2) {
		myPoint[] vertList = buildCylVerts(A, B, r);
		int[] c1 = getClr(clr1, 255);
		int[] c2 = getClr(clr2, 255);
		beginShape(QUAD_STRIP);
			for(int i=0; i<vertList.length; i+=2) {
				gl_SetFill(c1[0],c1[1],c1[2],255);		
				gl_vertex(vertList[i]); 
				gl_SetFill(c2[0],c2[1],c2[2],255);	
				gl_vertex(vertList[i+1]);}
		gl_endShape();
	}
	
	@Override
	public void drawCylinder(myPointf A, myPointf B, float r, int clr1, int clr2) {
		myPointf[] vertList = buildCylVerts(A, B, r);
		int[] c1 = getClr(clr1, 255);
		int[] c2 = getClr(clr2, 255);
		beginShape(QUAD_STRIP);
		for(int i=0; i<vertList.length; i+=2) {
			gl_SetFill(c1[0],c1[1],c1[2],255);		
			gl_vertex(vertList[i]); 
			gl_SetFill(c2[0],c2[1],c2[2],255);		
			gl_vertex(vertList[i+1]);}
	gl_endShape();
	}
	
	//////////////////////////////////
	// end draw routines
	
////////////////////////
// transformations
	
	@Override
	public void translate(float x, float y){super.translate(x,y);}
	@Override
	public void translate(float x, float y, float z){super.translate(x,y,z);}
	
	/**
	 * this will translate the passed box dimensions to keep them on the screen
	 * using p as start point and rectDims[2] and rectDims[3] as width and height
	 * @param P starting point
	 * @param rectDims box dimensions 
	 */
	@Override
	public void transToStayOnScreen(myPointf P, float[] rectDims) {
		float xLocSt = P.x + rectDims[0], xLocEnd = xLocSt + rectDims[2];
		float yLocSt = P.y + rectDims[1], yLocEnd = yLocSt + rectDims[3];
		float transX = 0.0f, transY = 0.0f;
		if (xLocSt < 0) {	transX = -1.0f * xLocSt;	} else if (xLocEnd > width) {transX = width - xLocEnd - 20;}
		if (yLocSt < 0) {	transY = -1.0f * yLocSt;	} else if (yLocEnd > height) {transY = height - yLocEnd - 20;}
		super.translate(transX,transY);		
	}

	@Override
	public void rotate(float thet, float x, float y, float z) {super.rotate(thet, x, y, z);}

	@Override
	public void scale(float x) {super.scale(x);}
	@Override
	public void scale(float x,float y) {super.scale(x, y);}
	@Override
	public void scale(float x,float y,float z) {super.scale(x,y,z);}

	

	////////////////////////
	// end transformations
	
//////////////////////////////////////////////////////
/// user interaction
//////////////////////////////////////////////////////	
	/**
	 * called by papplet super
	 */
	@Override
	public void mouseWheel(MouseEvent event) {
		//ticks is how much the wheel has moved one way or the other
		int ticks = event.getCount();		
		//AppMgr.mouseWheel(ticks);	
	}
		
	///////////////////////
	// display directives
	/**
	 * opengl hint directive to not check for depth - use this to display text on screen
	 */
	@Override
	public void setBeginNoDepthTest() {hint(PConstants.DISABLE_DEPTH_TEST);}
	/**
	 * opengl hint directive to start checking depth again
	 */
	@Override
	public void setEndNoDepthTest() {	hint(PConstants.ENABLE_DEPTH_TEST);}

	/**
	 * disable lights in scene
	 */
	@Override
	public void disableLights() { noLights();}
	/**
	 * enable lights in scene
	 */
	@Override
	public void enableLights(){ lights();}	

	@Override
	public void bezier(myPoint A, myPoint B, myPoint C, myPoint D) {bezier((float)A.x,(float)A.y,(float)A.z,(float)B.x,(float)B.y,(float)B.z,(float)C.x,(float)C.y,(float)C.z,(float)D.x,(float)D.y,(float)D.z);} // draws a cubic Bezier curve with control points A, B, C, D
	@Override
	public final myPoint bezierPoint(myPoint[] C, float t) {return new myPoint(bezierPoint((float)C[0].x,(float)C[1].x,(float)C[2].x,(float)C[3].x,(float)t),bezierPoint((float)C[0].y,(float)C[1].y,(float)C[2].y,(float)C[3].y,(float)t),bezierPoint((float)C[0].z,(float)C[1].z,(float)C[2].z,(float)C[3].z,(float)t)); }
	@Override
	public final myVector bezierTangent(myPoint[] C, float t) {return new myVector(bezierTangent((float)C[0].x,(float)C[1].x,(float)C[2].x,(float)C[3].x,(float)t),bezierTangent((float)C[0].y,(float)C[1].y,(float)C[2].y,(float)C[3].y,(float)t),bezierTangent((float)C[0].z,(float)C[1].z,(float)C[2].z,(float)C[3].z,(float)t)); }
	
	/**
	 * vertex with texture coordinates
	 * @param P vertex location
	 * @param u,v txtr coords
	 */
	public void vTextured(myPointf P, float u, float v) {vertex(P.x,P.y,P.z,u,v);}; 
	public void vTextured(myPoint P, double u, double v) {vertex((float)P.x,(float)P.y,(float)P.z,(float)u,(float)v);};                         
	
	/////////////
	// show functions 
	
	/////////////
	// text
	@Override
	public void showText(String txt, float x, float y) {				text(txt,x,y);}
	@Override
	public void showText(String txt, float x, float y, float z ) {	text(txt,x,y,z);}
	
	/**
	 * return the size, in pixels, of the passed text string, accounting for the currently set font dimensions
	 * @param txt the text string to be measured
	 * @return the size in pixels
	 */
	@Override
	public float textWidth(String txt) {		return super.textWidth(txt);	}
	
	@Override
	public void textSize(float fontSize) {super.textSize(fontSize);}

	///////////
	// end text	
	
	@Override
	public void setNoFill() {noFill();}
	
	@Override
	public void setNoStroke(){noStroke();}
	
	private void checkClrInts(int fclr, int sclr) {
		if(fclr > -1){setColorValFill(fclr,255); } else if(fclr <= -2) {noFill();}		
		if(sclr > -1){setColorValStroke(sclr,255);} else if(sclr <= -2) {noStroke();}
	}
	
	private void checkClrIntArrays(int[] fclr, int[] sclr) {
		if(fclr!= null){setFill(fclr,255);}
		if(sclr!= null){setStroke(sclr,255);}
	}	
	

	
	///////////
	// points
	
	@Override
	public void drawSphere(myPoint P, double rad, int det) {
		pushMatState(); 
		sphereDetail(det);
		translate(P.x,P.y,P.z); 
		sphere((float) rad); 
		popMatState();
	}
	
	////////////////////
	// showing double points as spheres or circles

	
	/**
	 * show a point as a sphere, using double point as center
	 * @param P point for center
	 * @param r radius
	 * @param det sphere detail
	 * @param fclr fill color index
	 * @param sclr scale color index
	 */
	@Override
	public void showPtAsSphere(myPoint P, double r, int det, int fclr, int sclr) {
		pushMatState();
		checkClrInts(fclr, sclr);
		drawSphere(P, r, det);
		popMatState();
	}
	/**
	 * show a point as a sphere, using double point as center
	 * @param P point for center
	 * @param r radius
	 * @param det sphere detail
	 * @param fclr fill color array
	 * @param sclr scale color array
	 */
	@Override
	public void showPtAsSphere(myPoint P, double r, int det, int[] fclr, int[] sclr) {
		pushMatState(); 
		checkClrIntArrays(fclr, sclr);
		drawSphere(P, r, det);
		popMatState();
	};
	
	/**
	 * show a point as a flat circle, using double point as center
	 * @param P point for center
	 * @param r radius
	 * @param fclr fill color index
	 * @param sclr scale color index
	 */
	@Override
	public void showPtAsCircle(myPoint P, double r, int fclr, int sclr) {
		pushMatState(); 
		checkClrInts(fclr, sclr);
		drawEllipse2D(P,(float)r);				
		popMatState();
	}
	/**
	 * show a point as a flat circle, using double point as center
	 * @param P point for center
	 * @param r radius
	 * @param det sphere detail
	 * @param fclr fill color array
	 * @param sclr scale color array
	 */
	@Override
	public void showPtAsCircle(myPoint P, double r, int[] fclr, int[] sclr) {
		pushMatState(); 
		checkClrIntArrays(fclr, sclr);
		drawEllipse2D(P,(float)r);						
		popMatState();
	}
	/**
	 * show a point either as a sphere or as a circle, with text
	 * @param P
	 * @param r
	 * @param s
	 * @param D
	 * @param clr
	 * @param flat
	 */
	@Override
	public void showPtWithText(myPoint P, double r, String s, myVector D, int clr, boolean flat){
		if(flat) {			showPtAsCircle(P,r, clr, clr);} 
		else {			showPtAsSphere(P,r,5, gui_Black, gui_Black);		}
		pushStyle();setColorValFill(clr,255);showTextAtPt(P,s,D);popStyle();
	}

	@Override
	public void showVec( myPoint ctr, double len, myVector v){drawLine(ctr.x,ctr.y,ctr.z,ctr.x+(v.x)*len,ctr.y+(v.y)*len,ctr.z+(v.z)*len);}
	
	@Override
	public void showTextAtPt(myPoint P, String s) {text(s, (float)P.x, (float)P.y, (float)P.z); } // prints string s in 3D at P
	
	@Override
	public void showTextAtPt(myPoint P, String s, myVector D) {text(s, (float)(P.x+D.x), (float)(P.y+D.y), (float)(P.z+D.z));  } // prints string s in 3D at P+D
	
	public void show(myPoint P, double rad, int fclr, int sclr, int tclr, String txt) {
		pushMatState(); 
		checkClrInts(fclr, sclr);
		sphereDetail(5);
		translate((float)P.x,(float)P.y,(float)P.z); 
		setColorValFill(tclr,255);setColorValStroke(tclr,255);
		showOffsetText(1.2f * (float)rad,tclr, txt);
		popMatState();} // render sphere of radius r and center P)
	
	public void show(myPoint P, double r, int fclr, int sclr) {
		pushMatState(); 
		checkClrInts(fclr, sclr);
		sphereDetail(5);
		translate((float)P.x,(float)P.y,(float)P.z); 
		sphere((float)r); 
		popMatState();} // render sphere of radius r and center P)
	
	public void show(myPoint[] ara) {gl_beginShape(); for(int i=0;i<ara.length;++i){gl_vertex(ara[i]);} gl_endShape(true);};                     
	public void show(myPoint[] ara, myVector norm) {gl_beginShape();gl_normal(norm); for(int i=0;i<ara.length;++i){gl_vertex(ara[i]);} gl_endShape(true);};   
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	// showing functions
	
	/**
	 * this will properly format and display a string of text, and will translate the width, so multiple strings can be displayed on the same line with different colors
	 * @param tclr
	 * @param txt
	 */
	public final void showOffsetText_RightSideMenu(int[] tclr, float mult,  String txt) {
		setFill(tclr,tclr[3]);setStroke(tclr,tclr[3]);
		showText(txt,0.0f,0.0f,0.0f);
		translate(txt.length()*mult, 0.0f,0.0f);		
	}
	
	public final void showOffsetText(float d, int tclr, String txt){
		setColorValFill(tclr, 255);setColorValStroke(tclr, 255);
		showText(txt, d, d,d); 
	}	
	public final void showOffsetText(myPointf loc, int tclr, String txt){
		setColorValFill(tclr, 255);setColorValStroke(tclr, 255);
		showText(txt, loc.x, loc.y, loc.z); 
	}	
	public final void showOffsetText2D(float d, int tclr, String txt){
		setColorValFill(tclr, 255);setColorValStroke(tclr, 255);
		showText(txt, d, d,0); 
	}
		
	public final void showBox_ClrAra(myPointf P, float rad, int det, int[] fclr, int[] strkclr, int tclr, String txt) {
		pushMatState();  
		translate(P.x,P.y,P.z);
		setColorValFill(IRenderInterface.gui_White,150);
		setColorValStroke(IRenderInterface.gui_Black,255);
		drawRect(new float[] {0,6.0f,txt.length()*7.8f,-15});
		tclr = IRenderInterface.gui_Black;		
		setFill(fclr,255); setStroke(strkclr,255);			
		drawSphere(myPointf.ZEROPT, rad, det);
		showOffsetText(1.2f * rad,tclr, txt);
		popMatState();
	} // render sphere of radius r and center P)
	
	//translate to point, draw point and text
	public final void showNoBox_ClrAra(myPointf P, float rad, int det, int[] fclr, int[] strkclr, int tclr, String txt) {
		pushMatState();  
		setFill(fclr,255); 
		setStroke(strkclr,255);		
		translate(P.x,P.y,P.z); 
		drawSphere(myPointf.ZEROPT, rad, det);
		showOffsetText(1.2f * rad,tclr, txt);
		popMatState();
	} // render sphere of radius r and center P)
	
	//textP is location of text relative to point
	public final void showNoBox_ClrAra(myPointf P, float rad, int det, int[] fclr, int[] strkclr, int tclr, myPointf txtP, String txt) {
		pushMatState();  
		translate(P.x,P.y,P.z); 
		setFill(fclr,255); 
		setStroke(strkclr,255);			
		drawSphere(myPointf.ZEROPT, rad, det);
		showOffsetText(txtP,tclr, txt);
		popMatState();
	} // render sphere of radius r and center P)
	
	//textP is location of text relative to point
	public final void showCrclNoBox_ClrAra(myPointf P, float rad, int[] fclr, int[] strkclr, int tclr, myPointf txtP, String txt) {
		pushMatState();  
		translate(P.x,P.y,P.z); 
		if((fclr!= null) && (strkclr!= null)){setFill(fclr,255); setStroke(strkclr,255);}		
		drawEllipse2D(0,0,rad,rad); 
		drawEllipse2D(0,0,2,2);
		showOffsetText(txtP,tclr, txt);
		popMatState();
	} // render sphere of radius r and center P)
	
	//show sphere of certain radius
	public final void show_ClrAra(myPointf P, float rad, int det, int[] fclr, int[] strkclr) {
		pushMatState();   
		if((fclr!= null) && (strkclr!= null)){setFill(fclr,255); setStroke(strkclr,255);}
		drawSphere(P, rad, det);
		popMatState();
	} // render sphere of radius r and center P)
	
	///////////
	// end double points
	///////////
	// float points (pointf)
	
	@Override
	public void drawSphere(myPointf P, float rad, int det) {
		pushMatState(); 
		sphereDetail(det);
		translate(P.x,P.y,P.z); 
		sphere(rad); 
		popMatState();
	}	
	////////////////////
	// showing float points as spheres or circles	
	/**
	 * show a point as a sphere, using float point as center
	 * @param P point for center
	 * @param r radius
	 * @param det sphere detail
	 * @param fclr fill color index
	 * @param sclr scale color index
	 */
	@Override
	public void showPtAsSphere(myPointf P, float r,int det, int fclr, int sclr) {
		pushMatState(); 
		checkClrInts(fclr, sclr);
		drawSphere(P,(float)r, det);
		popMatState();		
	}	
	/**
	 * show a point as a sphere, using float point as center
	 * @param P point for center
	 * @param r radius
	 * @param det sphere detail
	 * @param fclr fill color array
	 * @param sclr scale color array
	 */
	@Override
	public void showPtAsSphere(myPointf P, float r, int det, int[] fclr, int[] sclr){
		pushMatState(); 
		checkClrIntArrays(fclr, sclr);
		drawSphere(P,(float)r, det);
		popMatState();
	} // render sphere of radius r and center P)
	/**
	 * show a point as a flat circle, using float point as center
	 * @param P point for center
	 * @param r radius
	 * @param fclr fill color index
	 * @param sclr scale color index
	 */
	@Override
	public void showPtAsCircle(myPointf P, float r, int fclr, int sclr) {		
		pushMatState(); 
		checkClrInts(fclr, sclr);
		drawEllipse2D(P,(float)r);		
		popMatState();		
	}
	/**
	 * show a point as a flat circle, using float point as center
	 * @param P point for center
	 * @param r radius
	 * @param det sphere detail
	 * @param fclr fill color array
	 * @param sclr scale color array
	 */
	@Override
	public void showPtAsCircle(myPointf P, float r, int[] fclr, int[] sclr) {		
		pushMatState(); 
		checkClrIntArrays(fclr, sclr);
		drawEllipse2D(P,(float)r);					
		popMatState();
	} // render sphere of radius r and center P)

	@Override
	public void showPtWithText(myPointf P, float r, String s, myVectorf D, int clr, boolean flat){
		if(flat) {			showPtAsCircle(P,r, clr, clr);} 
		else {			showPtAsSphere(P,r,5, gui_Black, gui_Black);		}
		pushStyle();setColorValFill(clr,255);showTextAtPt(P,s,D);popStyle();
	}
	
	@Override
	public void showVec( myPointf ctr, float len, myVectorf v){line(ctr.x,ctr.y,ctr.z,ctr.x+(v.x)*len,ctr.y+(v.y)*len,ctr.z+(v.z)*len);}
	
	@Override
	public void showTextAtPt(myPointf P, String s) {text(s, P.x, P.y, P.z); } // prints string s in 3D at P
	
	@Override
	public void showTextAtPt(myPointf P, String s, myVectorf D) {text(s, (P.x+D.x), (P.y+D.y),(P.z+D.z));  } // prints string s in 3D at P+D
	
	/////////////
	// show functions using color idxs 
	/**
	 * display an array of text at a location on screen
	 * @param d initial y location
	 * @param tclr text color
 	 * @param txtAra string array to display
	 */
	@Override
	public void showOffsetTextAra(float d, int tclr, String[] txtAra){
		setColorValFill(tclr, 255);setColorValStroke(tclr, 255);
		float y = d;
		for (String txt : txtAra) {
			showText(txt, d, y, d);
			y+=10;
		}
	}	

	/**
	 * show array displayed at specific point on screens
	 * @param P
	 * @param rad
	 * @param det
	 * @param clrs
	 * @param txtAra
	 */
	@Override
	public void showTxtAra(myPointf P, float rad, int det, int[] clrs, String[] txtAra) {//only call with set fclr and sclr - idx0 == fill, idx 1 == strk, idx2 == txtClr
		pushMatState(); 
		setColorValFill(clrs[0],255); 
		setColorValStroke(clrs[1],255);
		drawSphere(P, rad, det);
		translate(P.x,P.y,P.z); 
		showOffsetTextAra(1.2f * rad, clrs[2], txtAra);
		popMatState();
	} // render sphere of radius r and center P)
	
	/**
	 * draw a box at a point containing an array of text
	 * @param P
	 * @param rad
	 * @param det
	 * @param clrs
	 * @param txtAra
	 * @param rectDims
	 */
	@Override
	public void showBoxTxtAra(myPointf P, float rad, int det, int[] clrs, String[] txtAra, float[] rectDims) {
		pushMatState();  		
			setColorValFill(clrs[0],255); 
			setColorValStroke(clrs[1],255);
			translate(P.x,P.y,P.z);
			drawSphere(myPointf.ZEROPT, rad, det);			
			
			pushMatState();  
			//make sure box doesn't extend off screen
				transToStayOnScreen(P,rectDims);
				setColorValFill(IRenderInterface.gui_White,150);
				setColorValStroke(IRenderInterface.gui_Black,255);
				setStrokeWt(2.5f);
				drawRect(rectDims);
				translate(rectDims[0],0,0);
				showOffsetTextAra(1.2f * rad, clrs[2], txtAra);
			 popMatState();
		 popMatState();
	} // render sphere of radius r and center P)
	
	public void show(myPointf[] ara) {gl_beginShape(); for(int i=0;i<ara.length;++i){gl_vertex(ara[i]);} gl_endShape(true);};                     
	public void show(myPointf[] ara, myVectorf norm) {gl_beginShape();gl_normal(norm); for(int i=0;i<ara.length;++i){gl_vertex(ara[i]);} gl_endShape(true);};                     
	
	public void showNoClose(myPoint[] ara) {gl_beginShape(); for(int i=0;i<ara.length;++i){gl_vertex(ara[i]);} gl_endShape();};                     
	public void showNoClose(myPointf[] ara) {gl_beginShape(); for(int i=0;i<ara.length;++i){gl_vertex(ara[i]);} gl_endShape();};   
	
	///end show functions
	
	
	////////////////////////
	// splines
	/**
	 * implementation of catumull rom - array needs to be at least 4 points, if not, then reuses first and last points as extra cntl points  
	 * @param pts
	 */
	@Override
	public void catmullRom2D(myPointf[] ara) {
		if(ara.length < 4){
			if(ara.length == 0){return;}
			gl_beginShape(); curveVertex2D(ara[0]);for(int i=0;i<ara.length;++i){curveVertex2D(ara[i]);} curveVertex2D(ara[ara.length-1]);gl_endShape();
			return;}		
		gl_beginShape(); for(int i=0;i<ara.length;++i){curveVertex2D(ara[i]);} gl_endShape();
	}
	/**
	 * implementation of catumull rom - array needs to be at least 4 points, if not, then reuses first and last points as extra cntl points  
	 * @param pts
	 */
	@Override
	public void catmullRom2D(myPoint[] ara) {
		if(ara.length < 4){
			if(ara.length == 0){return;}
			gl_beginShape(); curveVertex2D(ara[0]);for(int i=0;i<ara.length;++i){curveVertex2D(ara[i]);} curveVertex2D(ara[ara.length-1]);gl_endShape();
			return;}		
		gl_beginShape(); for(int i=0;i<ara.length;++i){curveVertex2D(ara[i]);} gl_endShape();		
	}
	protected final void curveVertex2D(myPoint P) {curveVertex((float)P.x,(float)P.y);};                                           // curveVertex for shading or drawing
	protected final void curveVertex2D(myPointf P) {curveVertex(P.x,P.y);};                                           // curveVertex for shading or drawing
	
	/**
	 * implementation of catumull rom - array needs to be at least 4 points, if not, then reuses first and last points as extra cntl points  
	 * @param pts
	 */
	@Override
	public void catmullRom3D(myPointf[] ara) {
		if(ara.length < 4){
			if(ara.length == 0){return;}
			gl_beginShape(); curveVertex3D(ara[0]);for(int i=0;i<ara.length;++i){curveVertex3D(ara[i]);} curveVertex3D(ara[ara.length-1]);gl_endShape();
			return;}		
		gl_beginShape(); for(int i=0;i<ara.length;++i){curveVertex3D(ara[i]);} gl_endShape();
	}
	/**
	 * implementation of catumull rom - array needs to be at least 4 points, if not, then reuses first and last points as extra cntl points  
	 * @param pts
	 */
	@Override
	public void catmullRom3D(myPoint[] ara) {
		if(ara.length < 4){
			if(ara.length == 0){return;}
			gl_beginShape(); curveVertex3D(ara[0]);for(int i=0;i<ara.length;++i){curveVertex3D(ara[i]);} curveVertex3D(ara[ara.length-1]);gl_endShape();
			return;}		
		gl_beginShape(); for(int i=0;i<ara.length;++i){curveVertex3D(ara[i]);} gl_endShape();		
	}
	
	protected final void curveVertex3D(myPoint P) {curveVertex((float)P.x,(float)P.y,(float)P.z);};                                           // curveVertex for shading or drawing
	protected final void curveVertex3D(myPointf P) {curveVertex(P.x,P.y,P.z);};                                           // curveVertex for shading or drawing


	///////////////////////////////////
	// getters/setters
	//////////////////////////////////
	
	/**
	 * returns application window width in pxls
	 * @return
	 */
	@Override
	public final int getWidth() {return width;}
	/**
	 * returns application window height in pxls
	 * @return
	 */
	@Override
	public final int getHeight() {return height;}	
	
	/**
	 * set smoothing level based on renderer
	 * @param smthLvl 0 == no smoothing,  	int: either 2, 3, 4, or 8 depending on the renderer
	 */
	@Override
	public void setSmoothing(int smthLvl) {
		if (smthLvl == 0) {	noSmooth();	}
		else {			smooth(smthLvl);}
	}
	/**
	 * set camera to passed 9-element values - should be called from window!
	 * @param camVals
	 */
	@Override
	public void setCameraWinVals(float[] camVals) {		camera(camVals[0],camVals[1],camVals[2],camVals[3],camVals[4],camVals[5],camVals[6],camVals[7],camVals[8]);}
	/**
	 * used to handle camera location/motion
	 */
	@Override
	public void setCamOrient(float rx, float ry){rotateX(rx);rotateY(ry); rotateX(MyMathUtils.HALF_PI_F);		}//sets the rx, ry, pi/2 orientation of the camera eye	
	/**
	 * used to draw text on screen without changing mode - reverses camera orientation setting
	 */
	@Override
	public void unSetCamOrient(float rx, float ry){rotateX(-MyMathUtils.HALF_PI_F); rotateY(-ry);   rotateX(-rx); }//reverses the rx,ry,pi/2 orientation of the camera eye - paints on screen and is unaffected by camera movement

	/**
	 * return x screen value for 3d point
	 * @param x
	 * @param y
	 * @param z
	 */
	@Override
	public float getSceenX(float x, float y, float z) {		return screenX(x,y,z);	};
	/**
	 * return y screen value for 3d point
	 * @param x
	 * @param y
	 * @param z
	 */
	@Override
	public float getSceenY(float x, float y, float z) {		return screenY(x,y,z);	};
	/**
	 * return screen value of z (Z-buffer) for 3d point
	 * @param x
	 * @param y
	 * @param z
	 */
	@Override
	public float getSceenZ(float x, float y, float z) {		return screenZ(x,y,z);	};
	
	/**
	 * return target frame rate
	 * @return
	 */
	@Override
	public final float getFrameRate() {return frameRate;}
	@Override
	public final myPoint getMouse_Raw() {return new myPoint(mouseX, mouseY,0);}                                          			// current mouse location
	@Override
	public final myVector getMouseDrag() {return new myVector(mouseX-pmouseX,mouseY-pmouseY,0);};                     			// vector representing recent mouse displacement
	
	@Override
	public final myPointf getMouse_Raw_f() {return new myPointf(mouseX, mouseY,0);}                                          			// current mouse location
	@Override
	public final myVectorf getMouseDrag_f() {return new myVectorf(mouseX-pmouseX,mouseY-pmouseY,0);};                     			// vector representing recent mouse displacement

	@Override
	public final int[] getMouse_Raw_Int() {return new int[] {mouseX, mouseY};}                                          			// current mouse location
	@Override
	public final int[] getMouseDrag_Int() {return new int[] {mouseX-pmouseX,mouseY-pmouseY};};              			// vector representing recent mouse displacement

	/**
	 * get depth at specified screen dim location
	 * @param x
	 * @param y
	 * @return
	 */
	@Override
	public float getDepth(int x, int y) {
		PGL pgl = beginPGL();
		FloatBuffer depthBuffer = ByteBuffer.allocateDirect(1 << 2).order(ByteOrder.nativeOrder()).asFloatBuffer();
		int newY = height - y;		pgl.readPixels(x, newY - 1, 1, 1, PGL.DEPTH_COMPONENT, PGL.FLOAT, depthBuffer);
		float depthValue = depthBuffer.get(0);
		endPGL();
		return depthValue;
	}
	
	/**
	 * determine world location as myPoint based on mouse click and passed depth
	 * @param x
	 * @param y
	 * @param depth
	 * @return
	 */
	@Override
	public myPoint getWorldLoc(int x, int y, float depth){
		int newY = height - y;
		float depthValue = depth;
		if(depth == -1){depthValue = getDepth( x,  y); }	
		//get 3d matrices
		PGraphics3D p3d = (PGraphics3D)g;
		PMatrix3D proj = p3d.projection.get(), modelView = p3d.modelview.get(), modelViewProjInv = proj; modelViewProjInv.apply( modelView ); modelViewProjInv.invert();	  
		float[] viewport = {0, 0, width, height},
				normalized = new float[] {
						((x - viewport[0]) / viewport[2]) * 2.0f - 1.0f, 
						((newY - viewport[1]) / viewport[3]) * 2.0f - 1.0f, 
						depthValue * 2.0f - 1.0f, 
						1.0f};	  
		float[] unprojected = new float[4];	  
		modelViewProjInv.mult( normalized, unprojected );
		myPoint pickLoc = new myPoint( unprojected[0]/unprojected[3], unprojected[1]/unprojected[3], unprojected[2]/unprojected[3] );
		return pickLoc;
	}		
	/**
	 * determine world location as myPointf based on mouse click and passed depth
	 * @param x
	 * @param y
	 * @param depth
	 * @return
	 */
	@Override
	public myPointf getWorldLoc_f(int x, int y, float depth){
		int newY = height - y;
		float depthValue = depth;
		
		if(depth == -1){depthValue = getDepth( x,  y); }	
		//get 3d matrices
		PGraphics3D p3d = (PGraphics3D)g;
		PMatrix3D proj = p3d.projection.get(), modelView = p3d.modelview.get(), modelViewProjInv = proj; modelViewProjInv.apply( modelView ); modelViewProjInv.invert();	  
		float[] viewport = {0, 0, width, height},
				normalized = new float[] {
						((x - viewport[0]) / viewport[2]) * 2.0f - 1.0f, 
						((newY - viewport[1]) / viewport[3]) * 2.0f - 1.0f, 
						depthValue * 2.0f - 1.0f, 
						1.0f};	  
		float[] unprojected = new float[4];	  
		modelViewProjInv.mult( normalized, unprojected );
		myPointf pickLoc = new myPointf( unprojected[0]/unprojected[3], unprojected[1]/unprojected[3], unprojected[2]/unprojected[3] );
		return pickLoc;
	}		

	@Override
	public final myPoint getScrLocOf3dWrldPt(myPoint pt){	return new myPoint(screenX((float)pt.x,(float)pt.y,(float)pt.z),screenY((float)pt.x,(float)pt.y,(float)pt.z),screenZ((float)pt.x,(float)pt.y,(float)pt.z));}
	
	/////////////////////		
	///color utils
	/////////////////////

	@Override
	public void setFill(int r, int g, int b, int alpha){fill(r,g,b,alpha);}
	@Override
	public void setStroke(int r, int g, int b, int alpha){stroke(r,g,b,alpha);}
	/**
	 * set stroke weight
	 */
	@Override
	public void setStrokeWt(float stW) {	strokeWeight(stW);}
	@Override
	public void setColorValFill(int colorVal, int alpha){
		if(colorVal == gui_TransBlack) {
			fill(0x00010100);//	have to use hex so that alpha val is not lost    TODO not taking care of alpha here
		} else {
			setFill(getClr(colorVal, alpha), alpha);
		}	
	}//setcolorValFill
	@Override
	public void setColorValStroke(int colorVal, int alpha){
		setStroke(getClr(colorVal, alpha), alpha);		
	}//setcolorValStroke	
	@Override
	public void setColorValFillAmb(int colorVal, int alpha){
		if(colorVal == gui_TransBlack) {
			fill(0x00010100);//	have to use hex so that alpha val is not lost    
			ambient(0,0,0);
		} else {
			int[] fillClr = getClr(colorVal, alpha);
			setFill(fillClr, alpha);
			ambient(fillClr[0],fillClr[1],fillClr[2]);
		}		
	}//setcolorValFill

	/**
	 * any instancing-class-specific colors - colorVal set to be higher than IRenderInterface.gui_OffWhite
	 * @param colorVal
	 * @param alpha
	 * @return
	 */
	@Override
	public int[] getClr_Custom(int colorVal, int alpha) {return getRndClr(alpha); }		
	@Override
	public final int[] getRndClr(int alpha){return new int[]{(int)random(0,255),(int)random(0,255),(int)random(0,255),alpha};	}
	@Override
	public final int[] getRndClrBright(int alpha){return new int[]{(int)random(50,255),(int)random(25,200),(int)random(80,255),alpha};	}
	
	@Override
	public final int getRndClrIndex(){return (int)random(0,IRenderInterface.gui_nextColorIDX);}		//return a random color flag value from IRenderInterface
	 
	
	@Override
	public final Integer[] getClrMorph(int[] a, int[] b, double t){
		if(t==0){return new Integer[]{a[0],a[1],a[2],a[3]};} else if(t==1){return new Integer[]{b[0],b[1],b[2],b[3]};}
		return new Integer[]{(int)(((1.0f-t)*a[0])+t*b[0]),(int)(((1.0f-t)*a[1])+t*b[1]),(int)(((1.0f-t)*a[2])+t*b[2]),(int)(((1.0f-t)*a[3])+t*b[3])};
	}

}
