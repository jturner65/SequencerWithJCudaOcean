package SphrSeqFFTVisPKG;

import java.awt.event.KeyEvent;
import java.util.*;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PImage;
import processing.opengl.PGL;
import processing.opengl.PGraphicsOpenGL;

import ddf.minim.*;
import ddf.minim.spi.AudioOut;
import ddf.minim.ugens.*;

//enums used for note and key data
//note and key value
enum nValType {
	C(0),Cs(1),D(2),Ds(3),E(4),F(5),Fs(6),G(7),Gs(8),A(9),As(10),B(11),rest(12); 
	private int value; 
	private static Map<Integer, nValType> map = new HashMap<Integer, nValType>(); 
    static { for (nValType enumV : nValType.values()) { map.put(enumV.value, enumV);}}
	private nValType(int _val){value = _val;} 
	public int getVal(){return value;}
	public static nValType getVal(int idx){return map.get(idx);}
	public static int getNumVals(){return map.size();}						//get # of values in enum
};	

//chord type
enum chordType {
	Major(0),			//1,3,5
	Minor(1),			//1,b3,5
	Augmented(2),		//1,3,#5
	MajFlt5(3),			//1,3,b5
	Diminished(4),		//1,b3,b5
	Sus2(5),			//1,2,5
	Sus4(6),			//1,4,5
	Maj6(7),			//1,3,5,6
	Min6(8),			//1,b3,5,6
	Maj7(9),			//1,3,5,7
	Dom7(10),			//1,3,5,b7
	Min7(11),			//1,b3,5,b7
	Dim7(12),			//1,b3,b5,bb7==6
	None(13);			//not a predifined chord type
	private int value; 
	private static Map<Integer, chordType> map = new HashMap<Integer, chordType>(); 
	static { for (chordType enumV : chordType.values()) { map.put(enumV.value, enumV);}}
	private chordType(int _val){value = _val;} 
	public int getVal(){return value;}
	public static chordType getVal(int idx){return map.get(idx);}
	public static int getNumVals(){return map.size();}						//get # of values in enum
};	
//note duration types - dotted get mult by 1.5, tuples, get multiplied by (2/tuple size) -> triplets are 2/3 duration (3 in the space of 2)
enum durType {
	Whole(1024),Half(512),Quarter(256),Eighth(128),Sixteenth(64),Thirtisecond(32); 
	private int value; 
	private static Map<Integer, durType> map = new HashMap<Integer, durType>(); 
    static { for (durType enumV : durType.values()) { map.put(enumV.value, enumV);}}
	private durType(int _val){value = _val;} 
	public int getVal(){return value;}
	public static durType getVal(int idx){return map.get(idx);}
	public static int getNumVals(){return map.size();}						//get # of values in enum
};	
//key signatures
enum keySigVals {
	CMaj(0),GMaj(1),DMaj(2),Amaj(3),EMaj(4),BMaj(5),FsMaj(6),CsMaj(7),GsMaj(8),DsMaj(9),AsMaj(10),Fmaj(11); 
	private int value; 
	private static Map<Integer, keySigVals> map = new HashMap<Integer, keySigVals>(); 
    static { for (keySigVals enumV : keySigVals.values()) { map.put(enumV.value, enumV);}}
	private keySigVals(int _val){value = _val;} 
	public int getVal(){return value;} 	
	public static keySigVals getVal(int idx){return map.get(idx);}
	public static int getNumVals(){return map.size();}						//get # of values in enum
};	
enum clefVal{
	Treble(0), Bass(1), Alto(2), Tenor(3), Piano(4), Drum(5); 
	private int value; 
	private static Map<Integer, clefVal> map = new HashMap<Integer, clefVal>(); 
    static { for (clefVal enumV : clefVal.values()) { map.put(enumV.value, enumV);}}
	private clefVal(int _val){value = _val;} 
	public int getVal(){return value;} 	
	public static clefVal getVal(int idx){return map.get(idx);}
	public static int getNumVals(){return map.size();}						//get # of values in enum
};	

/**
 * Project 5 Music visualization - full-fledged sequencer integrated with fft fluid visualization
 * 
 * John Turner
 * 
 */
 public class SeqVisFFTOcean extends PApplet {
	//project-specific variables
	public String prjNmLong = "Interactive Sequencer/Visualization with FFT Ocean", prjNmShrt = "SeqVisFFTOcean";
	PImage jtFace; 
	
	public final int drawnTrajEditWidth = 10; //TODO make ui component			//width in cntl points of the amount of the drawn trajectory deformed by dragging
	public final float
				InstEditWinYMult = .75f,							//percentage of screen that InstEdit window is displayed when open
				wScale = frameRate/5.0f,					//velocity drag scaling	
				trajDragScaleAmt = 100.0f;					//amt of displacement when dragging drawn trajectory to edit
			
	public String msClkStr = "";
	
	public PImage[] restImgs, clefImgs,sphereImgs, moonImgs; 			//array of images for rests, clefs and textures for spheres
	public int numRestImges, numClefImgs, numSphereImgs;

	//handles all transport controls
	public final int pbeModAmt = 1;		//how many beats ffwd and rewind move crsr for playback engine
	
	//display note width multiplier
	public final float ntWdthMult = 2.5f;
	
	// tools for playing music
	public Minim minim;		
	public AudioOutput glblOut;		
	public Summer glblSum;	
	public AudioRecorder recorder;	
	public int OutTyp = Minim.STEREO;
	
	//global values for minim/audio stuff
	public final int glbBufrSize = 1024;// * 16;
//		/public float glbSampleRate;
	
	public float playPosition;		//
	
	public int glblStartPlayTime, glblLastPlayTime;
	
	public float[] hSrsMult;
	public final int numHarms = 10;
	
	public myScore score;					//score being worked on - all windows reference same score

	//descending scale from C, to build piano roll piano
	public final nValType[] wKeyVals = new nValType[] {nValType.C, nValType.B, nValType.A, nValType.G, nValType.F,nValType.E,nValType.D},
								   bKeyVals = new nValType[] {nValType.As, nValType.Gs, nValType.Fs, nValType.Ds, nValType.Cs};
	
	//list of clefs  myClef(CAProject5 _p, String _name, clefVal _clef, NoteData _mdNote,PImage _img)
				//Notedata : nValType _name, int _octave
	public NoteData C4;
	public myClefBase[] clefs;
	//list of instruments
	public myInstr[] InstrList;
//		//idx's of instruments available
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
	
	public Sampler[] drumSounds;		
	public final int numDrumSnds = 8;
//		drums0 : bd
//		drums1 : hh1
//		drums2 : hh2
//		drums3 : snare1
//		drums4 : snare2 (clap snare)
//		drums5 : ride
//		drums6 : crash
//		drums7 : talk drum
	
	//sphere UI constants
	public final float sphereRad = 50.0f;
		
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
		initOnce();
		background(bground[0],bground[1],bground[2],bground[3]);		
	}//setup	
	
	//called once at start of program
	public void initOnce(){
		initVisOnce();						//always first
		C4 = new NoteData(this, nValType.C, 4);
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
				new myClef(this, "Treble", clefVal.Treble, new NoteData(this, nValType.B, 4),clefImgs[0], new float[]{-5,0,40,50},0),   //Treble(0), Bass(1), Alto(2), Tenor(3), Piano(4), Drum(5); 
				new myClef(this, "Bass", clefVal.Bass, new NoteData(this, nValType.D, 3),clefImgs[1], new float[]{0,-1,40,38},10),
				new myClef(this, "Alto", clefVal.Alto, new NoteData(this, nValType.C, 4),clefImgs[2], new float[]{-10,-3,40,46},5),
				new myClef(this, "Tenor", clefVal.Tenor, new NoteData(this, nValType.A, 3),clefImgs[3],new float[]{-10,-13,40,46},-5),
				null,//replaced below
				new myClef(this, "Drum", clefVal.Drum, new NoteData(this, nValType.B, 4),clefImgs[5], new float[]{0,0,40,40},0)				
		};
		clefs[4] = new myGrandClef(this, "Piano", clefVal.Piano, new NoteData(this, nValType.B, 4),clefImgs[4], new float[]{0,0,40,40},0); 
		InstrList = new myInstr[]{//TODO set harmonic series
				new myInstr(this, "Guitar1", clefs[clefVal.Treble.getVal()], hSrsMult,Waves.SAW, false),
				new myInstr(this, "Guitar2", clefs[clefVal.Treble.getVal()], hSrsMult,Waves.QUARTERPULSE, false),
				new myInstr(this, "Bass", clefs[clefVal.Bass.getVal()], hSrsMult, Waves.TRIANGLE, false),
				new myInstr(this, "Vox1", clefs[clefVal.Treble.getVal()], hSrsMult,Waves.SINE, false),
				new myInstr(this, "Vox2", clefs[clefVal.Treble.getVal()], hSrsMult,Waves.SINE, false),
				new myInstr(this, "Synth1", clefs[clefVal.Treble.getVal()], hSrsMult,Waves.SQUARE, false),
				new myInstr(this, "Synth2", clefs[clefVal.Treble.getVal()], hSrsMult,Waves.SQUARE, false),
				new myInstr(this, "Synth3", clefs[clefVal.Treble.getVal()], hSrsMult,Waves.QUARTERPULSE, false),
				new myInstr(this, "Synth4", clefs[clefVal.Alto.getVal()], hSrsMult,Waves.QUARTERPULSE, false),
				new myInstr(this, "Synth5", clefs[clefVal.Tenor.getVal()], hSrsMult,Waves.SAW, false),
				new myInstr(this, "Drums", clefs[clefVal.Drum.getVal()], hSrsMult,Waves.SINE, true),				//name of drum kits needs to be "Drums"
				new myInstr(this, "Drums2", clefs[clefVal.Drum.getVal()], hSrsMult,Waves.SINE, true)				//name of drum kits needs to be "Drums"
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

	//build audio specific constructs
	public void initAudio(){		
		minim = new Minim(this); 		// Declares minim which we use for sounds
		glblSum = new Summer();
		glblSum.setSampleRate(88200.0f);
		resetAudioOut();
		//glblSum.patch(glblOut);
	}

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
		drawUI();																	//draw UI overlay on top of rendered results			
		if (flags[saveAnim]) {	savePic();}
		consoleStrings.clear();
		surface.setTitle(prjNmLong + " : " + (int)(frameRate) + " fps|cyc ");
	}//draw
	//move reticle if currently playing
	public void movePBEReticle(float modAmtSec){
		for(int i =1; i<numDispWins; ++i){if((isShowingWindow(i)) && (dispWinFrames[i].dispFlags[myDispWindow.plays])){dispWinFrames[i].movePBEReticle(modAmtSec); return;}} //TODO Cntl if multiple windows need to handle simultaneous play here
	}
	
	//call 1 time if play is turned to true
	public void playMusic(){
		for(int i =1; i<numDispWins; ++i){if((isShowingWindow(i)) && (dispWinFrames[i].dispFlags[myDispWindow.plays])){dispWinFrames[i].play(); return;}} //TODO Cntl if multiple windows need to handle simultaneous play here
	}
	//call 1 time if play is turned to false or stop is called
	public void stopMusic(){
		for(int i =1; i<numDispWins; ++i){if((isShowingWindow(i)) && (dispWinFrames[i].dispFlags[myDispWindow.plays])){dispWinFrames[i].stopPlaying(); return;}} //TODO Cntl if multiple windows need to handle simultaneous play here
	}
	
	public void draw3D_solve3D(){
	//	if (cyclModCmp) {															//if drawing this frame, draw results of calculations								
			background(bground[0],bground[1],bground[2],bground[3]);				//if refreshing screen, this clears screen, sets background
			pushMatrix();pushStyle();
			translateSceneCtr();				//move to center of 3d volume to start drawing	
			for(int i =1; i<numDispWins; ++i){if((isShowingWindow(i)) && (dispWinFrames[i].dispFlags[myDispWindow.is3DWin])){dispWinFrames[i].draw(myPoint._add(sceneCtrVals[sceneIDX],focusTar));}}
			popStyle();popMatrix();
			drawAxes(100,3, new myPoint(-c.viewDimW/2.0f+40,0.0f,0.0f), 200, false); 		//for visualisation purposes and to show movement and location in otherwise empty scene
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
		c.buildCanvas();
		c.drawMseEdge();
	}
	
	//if should show problem # i
	public boolean isShowingWindow(int i){return flags[(i+this.showUIMenu)];}//showUIMenu is first flag of window showing flags
	public void drawUI(){					
		for(int i =1; i<numDispWins; ++i){if ((isShowingWindow(i)) && !(dispWinFrames[i].dispFlags[myDispWindow.is3DWin])){dispWinFrames[i].draw(sceneCtrVals[sceneIDX]);}}
		//dispWinFrames[0].draw(sceneCtrVals[sceneIDX]);
		for(int i =1; i<numDispWins; ++i){dispWinFrames[i].drawHeader();}
		//menu
		dispWinFrames[0].draw(sceneCtrVals[sceneIDX]);
		dispWinFrames[0].drawHeader();
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
		numClefImgs = clefVal.getNumVals();			
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
		int[] tmp = clefImgs[0].pixels;
		//set images, for grand staff set images as treble + bass clef
		//for(int i =0; i<clefs.length;++i){if(i == clefVal.Piano.getVal()){	clefs[i].setImage(new PImage[]{clefImgs[0],clefImgs[1]});	}	else {clefs[i].setImage(new PImage[]{clefImgs[i]});}}
		
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
	
	public void mouseMoved(){for(int i =0; i<numDispWins; ++i){if (dispWinFrames[i].handleMouseMove(mouseX, mouseY,c.getMseLoc(sceneCtrVals[sceneIDX]))){return;}}}
	public void mousePressed() {
		//verify left button if(mouseButton == LEFT)
		setFlags(mouseClicked, true);
		if(mouseButton == LEFT){			mouseClicked(0);} 
		else if (mouseButton == RIGHT) {	mouseClicked(1);}
		//for(int i =0; i<numDispWins; ++i){	if (dispWinFrames[i].handleMouseClick(mouseX, mouseY,c.getMseLoc(sceneCtrVals[sceneIDX]))){	return;}}
	}// mousepressed	

	private void mouseClicked(int mseBtn){ for(int i =0; i<numDispWins; ++i){if (dispWinFrames[i].handleMouseClick(mouseX, mouseY,c.getMseLoc(sceneCtrVals[sceneIDX]),mseBtn)){return;}}}		
	
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
		myPoint msePt = c.getMseLoc(sceneCtrVals[sceneIDX]);
		myVector mseDiff = new myVector(c.getOldMseLoc(),c.getMseLoc());
		for(int i =0; i<numDispWins; ++i){if (dispWinFrames[i].handleMouseDrag(mouseX, mouseY, pmouseX, pmouseY,msePt,mseDiff,0)) {return;}}		
	}
	
	private void mouseRightDragged(){
		myPoint msePt = c.getMseLoc(sceneCtrVals[sceneIDX]);
		myVector mseDiff = new myVector(c.getOldMseLoc(),c.getMseLoc());
		for(int i =0; i<numDispWins; ++i){if (dispWinFrames[i].handleMouseDrag(mouseX, mouseY, pmouseX, pmouseY,msePt,mseDiff,1)) {return;}}		
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
		for(int i =0; i < numDispWins; ++i){dispWinFrames[i].modCurrentPBETime(modAmt);}	
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
//		
//		//handle distributing modified UI values to windows
//		public void setEnumValAllWins(int uiIdx, int[] newVals){
//			switch(uiIdx){
//				case mySideBarMenu.gIDX_GlblKeySig : {break;}
//				case mySideBarMenu.gIDX_GlblTimeSigNum : 
//				case mySideBarMenu.gIDX_GlblTimeSigDenom : {break;}
//				case mySideBarMenu.gIDX_GlblTempo : {break;}
//			}	
//		}//setEnumValAllWins
	
	public durType getDurTypeForNote(int _noteType){
		switch (_noteType){
		case 1 : {return durType.Whole;}
		case 2 : {return durType.Half;}
		case 4 : {return durType.Quarter;}
		case 8 : {return durType.Eighth;}
		case 16 : {return durType.Sixteenth;}
		case 32 : {return durType.Thirtisecond;}
		}
		return durType.Quarter;
	}
	
	//instance an audio out
	public void resetAudioOut(){
		glblOut = minim.getLineOut(OutTyp,glbBufrSize, 44100.0f);	
		//glblOut.setVolume(.5f);
		float tmpTempo = 1;
		if(myDispWindow.glblTempo <1){		tmpTempo = 120.0f;}			
		else{			tmpTempo = myDispWindow.glblTempo;		}
		glblOut.setTempo(tmpTempo);			
		glblSum.patch(glblOut);
		outStr2Scr("Out tempo set to   " + tmpTempo);
	}
	
	public void initDispWins(){
		float InstEditWinHeight = InstEditWinYMult * height;		//how high is the InstEdit window when shown
		//instanced window dimensions when open and closed - only showing 1 open at a time
		winRectDimOpen[dispPianoRollIDX] =  new float[]{menuWidth, 0,width-menuWidth,height-hidWinHeight};			
		winRectDimOpen[dispSphereUIIDX] =  new float[]{menuWidth+hideWinWidth, 0,width-menuWidth-hideWinWidth,height-hidWinHeight};			
		winRectDimOpen[dispSimIDX] =  new float[]{menuWidth+hideWinWidth, 0,width-menuWidth-hideWinWidth,height-hidWinHeight};			
		winRectDimOpen[dispInstEditIDX]  =  new float[]{menuWidth, InstEditWinHeight, width-menuWidth, height-InstEditWinHeight};
		//hidden
		winRectDimClose[dispPianoRollIDX] =  new float[]{menuWidth, 0, hideWinWidth, height};				
		winRectDimClose[dispSphereUIIDX] =  new float[]{menuWidth, 0, hideWinWidth, height};				
		winRectDimClose[dispSimIDX] =  new float[]{menuWidth, 0, hideWinWidth, height};				
		winRectDimClose[dispInstEditIDX]  =  new float[]{menuWidth, height-hidWinHeight, width-menuWidth, hidWinHeight};
		
		winTrajFillClrs = new int []{gui_Black,gui_LightGray,gui_LightGray,gui_LightGray,gui_LightGreen};		//set to color constants for each window
		winTrajStrkClrs = new int []{gui_Black,gui_DarkGray,gui_DarkGray,gui_DarkGray,gui_White};		//set to color constants for each window			
		
		String[] winTitles = new String[]{"","Piano Roll/Score","Sphere UI","Sim Ocean Visualisation", "Instrument Edit"},
				winDescr = new String[] {"","Piano/Score Editor Window - Draw In Here to enter or edit notes, and to see the resultant score","Control the various instruments using the spheres","Simulation Responds to Music", "Instrument Frequency Response Edit Window"};
//			//display window initialization	
		int wIdx = dispPianoRollIDX, fIdx = showSequence;
		dispWinFrames[wIdx] = new mySequencer(this, winTitles[wIdx], fIdx, winFillClrs[wIdx], winStrkClrs[wIdx], winRectDimOpen[wIdx],winRectDimClose[wIdx],winDescr[wIdx],canDrawInWin[wIdx]);			
		wIdx = dispSphereUIIDX;fIdx = showSphereUI;
		dispWinFrames[wIdx] = new mySphereUI(this, winTitles[wIdx], fIdx,winFillClrs[wIdx], winStrkClrs[wIdx], winRectDimOpen[wIdx], winRectDimClose[wIdx], winDescr[wIdx],canDrawInWin[wIdx]);
		wIdx = dispSimIDX;fIdx = showSimWin;
		dispWinFrames[wIdx] = new mySimWindow(this, winTitles[wIdx], fIdx,winFillClrs[wIdx], winStrkClrs[wIdx], winRectDimOpen[wIdx], winRectDimClose[wIdx], winDescr[0],canDrawInWin[wIdx]);
		wIdx = dispInstEditIDX;fIdx=showInstEdit;
		dispWinFrames[wIdx] = new myInstEditWindow(this, winTitles[wIdx], fIdx,winFillClrs[wIdx], winStrkClrs[wIdx], winRectDimOpen[wIdx], winRectDimClose[wIdx], winDescr[wIdx],canDrawInWin[wIdx]);
		//setup default score and finalize init of windows
		initNewScore();				
		for(int i =0; i < numDispWins; ++i){
			dispWinFrames[i].setGlobalTempoVal(120);		//set for right now
			dispWinFrames[i].setGlobalKeySigVal(0);		//set for right now
			dispWinFrames[i].setGlobalTimeSigVal(4,4,getDurTypeForNote(4));		//set for right now
			dispWinFrames[i].dispFlags[myDispWindow.is3DWin] = dispWinIs3D[i];
			dispWinFrames[i].setTrajColors(winTrajFillClrs[i], winTrajStrkClrs[i]);
		}	
				
	}//initDispWins
	//needs to happen here, and then be propagated out to all windows
	public void initNewScore(){//uses default values originally called in mySequencer constructor
		//outStr2Scr("Build score with default instrument list");
		initNewScore("TempSong", InstrList, winRectDimOpen[dispPianoRollIDX][0] + winRectDimOpen[dispPianoRollIDX][2],winRectDimOpen[dispPianoRollIDX][1]+winRectDimOpen[dispPianoRollIDX][3]-4*myDispWindow.yOff);
	}
	public void initNewScore(String scrName, myInstr[] _instrs, float scoreWidth, float scoreHeight){
		float [] scoreRect = new float[]{0, myDispWindow.topOffY, scoreWidth, scoreHeight};
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
	public float menuWidth,menuWidthMult = .15f, hideWinWidth, hideWinWidthMult = .03f, hidWinHeight, hideWinHeightMult = .05f;			//side menu is 15% of screen grid2D_X, 

	public ArrayList<String> DebugInfoAra;										//enable drawing dbug info onto screen
	public String debugInfoString;
	
	//animation control variables	
	public float animCntr = 0, animModMult = 1.0f;
	public final float maxAnimCntr = PI*1000.0f, baseAnimSpd = 1.0f;
	

	my3DCanvas c;												//3d interaction stuff and mouse tracking
	
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
		hidWinHeight = height * hideWinHeightMult;
		c = new my3DCanvas(this);			
		winRectDimOpen = new float[numDispWins][];
		winRectDimClose = new float[numDispWins][];
		winRectDimOpen[0] =  new float[]{0,0, menuWidth, height};
		winRectDimClose[0] =  new float[]{0,0, hideWinWidth, height};
		
		strokeCap(SQUARE);//makes the ends of stroke lines squared off
		
		//display window initialization
		dispWinFrames = new myDispWindow[numDispWins];		
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
			case forceInKey			: {if(val){((mySequencer)dispWinFrames[dispPianoRollIDX]).forceAllNotesInKey();}break;}
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
		setColorValStroke(gui_TransGray);
		
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
		for(int i =0; i<3;++i){	setColorValStroke(rgbClrs[i]);	showVec(ctr,len, _axis[i]);	}
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
		int[] c = getClr(clr);
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
			addInfoStr(0,"mse loc on screen : " + new myPoint(mouseX, mouseY,0) + " mse loc in world :"+c.mseLoc +"  Eye loc in world :"+ c.eyeInWorld); 
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
	
	public myPoint getScrLocOf3dWrldPt(myPoint pt){	return new myPoint(screenX((float)pt.x,(float)pt.y,(float)pt.z),screenY((float)pt.x,(float)pt.y,(float)pt.z),screenZ((float)pt.x,(float)pt.y,(float)pt.z));}
	
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
		
		myVector rAB = myVector._rotAroundAxis(AB, rotAxis, PConstants.HALF_PI);
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
		myVector I = c.drawSNorm;//U(Normal(V));
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
	void circle(myPoint P, float r, myVector I, myVector J, int n) {myPoint[] pts = new myPoint[n];pts[0] = P(P,r,U(I));float a = (2*PI)/(1.0f*n);for(int i=1;i<n;++i){pts[i] = R(pts[i-1],a,J,I,P);}pushMatrix(); pushStyle();noFill(); show(pts);popStyle();popMatrix();}; // render sphere of radius r and center P
	
	void circle(myPoint p, float r){ellipse((float)p.x, (float)p.y, r, r);}
	void circle(float x, float y, float r1, float r2){ellipse(x,y, r1, r2);}
	
	void noteArc(float[] dims, int[] noteClr){
		noFill();
		setStroke(noteClr);
		strokeWeight(1.5f*dims[3]);
		arc(0,0, dims[2], dims[2], dims[0] - PConstants.HALF_PI, dims[1] - PConstants.HALF_PI);
	}
	//draw a ring segment from alphaSt in radians to alphaEnd in radians
	void noteArc(myPoint ctr, float alphaSt, float alphaEnd, float rad, float thickness, int[] noteClr){
		noFill();
		setStroke(noteClr);
		strokeWeight(thickness);
		arc((float)ctr.x, (float)ctr.y, rad, rad, alphaSt - PConstants.HALF_PI, alphaEnd- PConstants.HALF_PI);
	}
	
	
	void bezier(myPoint A, myPoint B, myPoint C, myPoint D) {bezier((float)A.x,(float)A.y,(float)A.z,(float)B.x,(float)B.y,(float)B.z,(float)C.x,(float)C.y,(float)C.z,(float)D.x,(float)D.y,(float)D.z);} // draws a cubic Bezier curve with control points A, B, C, D
	void bezier(myPoint [] C) {bezier(C[0],C[1],C[2],C[3]);} // draws a cubic Bezier curve with control points A, B, C, D
	myPoint bezierPoint(myPoint[] C, float t) {return P(bezierPoint((float)C[0].x,(float)C[1].x,(float)C[2].x,(float)C[3].x,(float)t),bezierPoint((float)C[0].y,(float)C[1].y,(float)C[2].y,(float)C[3].y,(float)t),bezierPoint((float)C[0].z,(float)C[1].z,(float)C[2].z,(float)C[3].z,(float)t)); }
	myVector bezierTangent(myPoint[] C, float t) {return V(bezierTangent((float)C[0].x,(float)C[1].x,(float)C[2].x,(float)C[3].x,(float)t),bezierTangent((float)C[0].y,(float)C[1].y,(float)C[2].y,(float)C[3].y,(float)t),bezierTangent((float)C[0].z,(float)C[1].z,(float)C[2].z,(float)C[3].z,(float)t)); }

	
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
	public void showVec( myPoint ctr, double len, myVector v){line(ctr.x,ctr.y,ctr.z,ctr.x+(v.x)*len,ctr.y+(v.y)*len,ctr.z+(v.z)*len);}
	public void show(myPoint P, double r,int fclr, int sclr, boolean flat) {//TODO make flat circles for points if flat
		pushMatrix(); pushStyle(); 
		if((fclr!= -1) && (sclr!= -1)){setColorValFill(fclr); setColorValStroke(sclr);}
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
	public void show(myPoint P, double r, int fclr, int sclr, int tclr, String txt) {
		pushMatrix(); pushStyle(); 
		if((fclr!= -1) && (sclr!= -1)){setColorValFill(fclr); setColorValStroke(sclr);}
		sphereDetail(5);
		translate((float)P.x,(float)P.y,(float)P.z); 
		sphere((float)r); 
		setColorValFill(tclr);setColorValStroke(tclr);
		double d = 1.1 * r;
		show(myPoint.ZEROPT, txt, new myVector(d,d,d));
		popStyle(); popMatrix();} // render sphere of radius r and center P)

	public void show(myPoint P, double r, String s, myVector D){show(P,r, gui_Black, gui_Black, false);pushStyle();setColorValFill(gui_Black);show(P,s,D);popStyle();}
	public void show(myPoint P, double r, String s, myVector D, int clr, boolean flat){show(P,r, clr, clr, flat);pushStyle();setColorValFill(clr);show(P,s,D);popStyle();}
	public void show(myPoint P, String s, myVector D) {text(s, (float)(P.x+D.x), (float)(P.y+D.y), (float)(P.z+D.z));  } // prints string s in 3D at P+D
	public void show(myPoint[] ara) {beginShape(); for(int i=0;i<ara.length;++i){gl_vertex(ara[i]);} endShape(CLOSE);};                     
	public void showNoClose(myPoint[] ara) {beginShape(); for(int i=0;i<ara.length;++i){gl_vertex(ara[i]);} endShape();};                     
	public void show(myPoint[] ara, myVector norm) {beginShape();gl_normal(norm); for(int i=0;i<ara.length;++i){gl_vertex(ara[i]);} endShape(CLOSE);};                     
	public void curveVertex(myPoint P) {curveVertex((float)P.x,(float)P.y);};                                           // curveVertex for shading or drawing
	public void curve(myPoint[] ara) {if(ara.length == 0){return;}beginShape(); curveVertex(ara[0]);for(int i=0;i<ara.length;++i){curveVertex(ara[i]);} curveVertex(ara[ara.length-1]);endShape();};                      // volume of tet 
	//note is tilted ellipse with stem (if  not whole note), and filled (if not whole or half note) and with flags (if 8th or smaller)
	//type  == type of note (0 is whole, 1 is half, 2 is qtr, 3 is eighth, etc)
	//nextNoteLoc is location of next note yikes.
	//flags : 0 : isDotted, 1 : isTuple, 2 : isRest, 3 : isChord, 4 : drawStemUp,   5 : isConnected, 6 :showDisplacement msg (8va, etc), 7 : isInStaff, 8 : isFlipped(part of chord and close to prev note, put note on other side of stem),
	//grpPos : 0 first in group of stem-tied notes, 1 : last in group of stemTied notes, otherwise neither 
	public void drawNote(float noteW, myVector nextNoteLoc, int noteTypIdx, int grpPos, boolean[] flags, float numLedgerLines){
		pushMatrix(); pushStyle(); 
		//draw body
		//noteIdx : -2,-1, 0, 1, 2, 3
		rotate(QUARTER_PI,0,0,1);
		strokeWeight(1);
		setColorValFill(gui_Black);
		setColorValStroke(gui_Black);
		if(flags[myNote.isChord] && flags[myNote.isFlipped]){translate(-noteW,0,0);}		//only flip if close to note
		//line(-noteW,0,0,noteW,0,0);//ledger lines, to help align the note
		if(noteTypIdx <= -1){	strokeWeight(2);	noFill();	}
		ellipse(0,0,noteW, .75f*noteW);
		rotate(-QUARTER_PI,0,0,1);
		if(flags[myNote.isDotted]){ellipse(1.5f*noteW,0,3,3);	}//is dotted
		if(noteTypIdx > -2){//has stem and is not last in stemmed group
			if(flags[myNote.drawStemUp]){	translate(-.5*noteW,0,0); line(0,0,0,0,-4*noteW,0);}//draw up
			else {							translate(.5*noteW,0,0);line(0,0,0,0,4*noteW,0);}//drawDown
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
				if(flags[myNote.drawStemUp]){	 moveDir = noteW; translate(0,-4*noteW,0);}//draw up
				else {			 moveDir = -noteW;translate(0,4*noteW,0);}//drawDown
				float yVal = 0,flagH2 = - .5f*moveDir;;
				for(int i =0; i<noteTypIdx;++i){ // noteIdx is # of flags to draw too
					quad(0,yVal,flagW,yVal+flagH1,flagW,yVal+flagH1+flagH2,0,yVal + flagH2);
					yVal += moveDir;
				}
			}			
		}
		if(numLedgerLines != 0.0f){ //draw ledger lines outside staff, either above or below (if negative # then below note, above staff)
			translate(-noteW*.5f,0);
			int mult;
			if(numLedgerLines < 0 ){mult=1;} else {mult=-1;}
			if(flags[myNote.isOnLdgrLine]){//put ledger line through middle of note
				line(-noteW,0,noteW,0);					
			} else {
				line(-noteW,.5f*noteW,noteW,.5f*noteW);			
			}
			pushMatrix(); pushStyle(); 
			if(abs(numLedgerLines) - (int)(abs(numLedgerLines)) != 0){translate(0,.5f*noteW);}
			for(int i =0;i<abs(numLedgerLines);++i){
				translate(0,mult*noteW);
				line(-noteW,0,noteW,0);
			}
			popStyle(); popMatrix();
			
		}
		popStyle(); popMatrix();
	}//draw a note head

	//flags : 0 : isDotted, 1 : drawUp, 2 : isFlipped(part of chord)
	//durType vals : Whole(256),Half(128),Quarter(64),Eighth(32),Sixteenth(16),Thirtisecond(8); 
	public void drawRest(float restW, int restIdx, boolean isDotted){
		pushMatrix(); pushStyle(); 
		//draw rest
		//restIdx : -2,-1, 0, 1, 2, 3
		if(restIdx > -1){//draw image
			translate(restDisp[restIdx][0], restDisp[restIdx][1],0);		//center image of rest - move up 2 ledger lines
			scale(1,1.2f,1);
			image(restImgs[restIdx], 0,0);				
		} else {//draw box
			if(restIdx == -2){	translate(0,-.5f * restW,0);}//whole rest is above half rest
			rect(-.5f * restW, 0, restW,.5f * restW);				
		}
		popStyle(); popMatrix();
	}
	
	
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
///Misc utils
/////////////////////
	//array of naturals drecreased by a sharp in y on grid
	public nValType[] hasSharps = new nValType[]{nValType.C, nValType.D, nValType.F,nValType.G,nValType.A};
	//array of naturals drecreased by a flat in y on grid
	public nValType[] hasFlats = new nValType[]{nValType.B, nValType.D, nValType.E,nValType.G,nValType.A};	
	//array of all natural notes
	public nValType[] isNaturalNotes = new nValType[]{nValType.A,nValType.B, nValType.C,nValType.D, nValType.E,nValType.F,nValType.G};	
	public boolean chkHasSharps(nValType n){for(int i =0; i<hasSharps.length;++i){	if(hasSharps[i].getVal() == n.getVal()){return true;}}return false;}
	public boolean chkHasFlats(nValType n){for(int i =0; i<hasFlats.length;++i){	if(hasFlats[i].getVal() == n.getVal()){return true;}}return false;}
	public boolean isNaturalNote(nValType n){for(int i =0; i<isNaturalNotes.length;++i){	if(isNaturalNotes[i].getVal() == n.getVal()){return true;}}return false;}
	public String getKeyNames(ArrayList<nValType> keyAra){String res = "";for(int i=0;i<keyAra.size();++i){res += "|i:"+i+" : val="+keyAra.get(i); }return res;}	

	
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
	public void setColorValFill(int colorVal){ setColorValFill(colorVal,255);}
	public void setColorValFill(int colorVal, int alpha){
		switch (colorVal){
			case gui_rnd				: { fill(random(255),random(255),random(255),alpha);break;}
			case gui_White  			: { fill(255,255,255,alpha);break; }
			case gui_Gray   			: { fill(120,120,120,alpha); break;}
			case gui_Yellow 			: { fill(255,255,0,alpha);break; }
			case gui_Cyan   			: { fill(0,255,255,alpha);  break; }
			case gui_Magenta			: { fill(255,0,255,alpha);break; }
			case gui_Red    			: { fill(255,0,0,alpha); break; }
			case gui_Blue				: { fill(0,0,255,alpha); break; }
			case gui_Green				: { fill(0,255,0,alpha);  break; } 
			case gui_DarkGray   		: { fill(80,80,80,alpha); break;}
			case gui_DarkRed    		: { fill(120,0,0,alpha);break;}
			case gui_DarkBlue   		: { fill(0,0,120,alpha); break;}
			case gui_DarkGreen  		: { fill(0,120,0,alpha); break;}
			case gui_DarkYellow 		: { fill(120,120,0,alpha); break;}
			case gui_DarkMagenta		: { fill(120,0,120,alpha); break;}
			case gui_DarkCyan   		: { fill(0,120,120,alpha); break;}	   
			case gui_LightGray   		: { fill(200,200,200,alpha); break;}
			case gui_LightRed    		: { fill(255,110,110,alpha); break;}
			case gui_LightBlue   		: { fill(110,110,255,alpha); break;}
			case gui_LightGreen  		: { fill(110,255,110,alpha); break;}
			case gui_LightYellow 		: { fill(255,255,110,alpha); break;}
			case gui_LightMagenta		: { fill(255,110,255,alpha); break;}
			case gui_LightCyan   		: { fill(110,255,255,alpha); break;}    	
			case gui_Black			 	: { fill(0,0,0,alpha);break;}//
			case gui_TransBlack  	 	: { fill(0x00010100);  break;}//	have to use hex so that alpha val is not lost    	
			case gui_FaintGray 		 	: { fill(77,77,77,alpha/3); break;}
			case gui_FaintRed 	 	 	: { fill(110,0,0,alpha/2);  break;}
			case gui_FaintBlue 	 	 	: { fill(0,0,110,alpha/2);  break;}
			case gui_FaintGreen 	 	: { fill(0,110,0,alpha/2);  break;}
			case gui_FaintYellow 	 	: { fill(110,110,0,alpha/2); break;}
			case gui_FaintCyan  	 	: { fill(0,110,110,alpha/2); break;}
			case gui_FaintMagenta  	 	: { fill(110,0,110,alpha/2); break;}
			case gui_TransGray 	 	 	: { fill(120,120,120,alpha/8); break;}//
			case gui_TransRed 	 	 	: { fill(255,0,0,alpha/2);  break;}
			case gui_TransBlue 	 	 	: { fill(0,0,255,alpha/2);  break;}
			case gui_TransGreen 	 	: { fill(0,255,0,alpha/2);  break;}
			case gui_TransYellow 	 	: { fill(255,255,0,alpha/2);break;}
			case gui_TransCyan  	 	: { fill(0,255,255,alpha/2);break;}
			case gui_TransMagenta  	 	: { fill(255,0,255,alpha/2);break;}
			case gui_OffWhite			: { fill(248,248,255,alpha);break; }
			default         			: { fill(255,255,255,alpha);break;}  	    	
		}//switch	
	}//setcolorValFill
	public void setColorValStroke(int colorVal){ setColorValStroke(colorVal, 255);}
	public void setColorValStroke(int colorVal, int alpha){
		switch (colorVal){
			case gui_White  	 	    : { stroke(255,255,255,alpha); break; }
			case gui_Gray   	 	    : { stroke(120,120,120,alpha); break;}
			case gui_Yellow      	    : { stroke(255,255,0,alpha); break; }
			case gui_Cyan   	 	    : { stroke(0,255,255,alpha); break; }
			case gui_Magenta	 	    : { stroke(255,0,255,alpha);  break; }
			case gui_Red    	 	    : { stroke(255,120,120,alpha); break; }
			case gui_Blue		 	    : { stroke(120,120,255,alpha); break; }
			case gui_Green		 	    : { stroke(120,255,120,alpha); break; }
			case gui_DarkGray    	    : { stroke(80,80,80,alpha); break; }
			case gui_DarkRed     	    : { stroke(120,0,0,alpha); break; }
			case gui_DarkBlue    	    : { stroke(0,0,120,alpha); break; }
			case gui_DarkGreen   	    : { stroke(0,120,0,alpha); break; }
			case gui_DarkYellow  	    : { stroke(120,120,0,alpha); break; }
			case gui_DarkMagenta 	    : { stroke(120,0,120,alpha); break; }
			case gui_DarkCyan    	    : { stroke(0,120,120,alpha); break; }	   
			case gui_LightGray   	    : { stroke(200,200,200,alpha); break;}
			case gui_LightRed    	    : { stroke(255,110,110,alpha); break;}
			case gui_LightBlue   	    : { stroke(110,110,255,alpha); break;}
			case gui_LightGreen  	    : { stroke(110,255,110,alpha); break;}
			case gui_LightYellow 	    : { stroke(255,255,110,alpha); break;}
			case gui_LightMagenta	    : { stroke(255,110,255,alpha); break;}
			case gui_LightCyan   		: { stroke(110,255,255,alpha); break;}		   
			case gui_Black				: { stroke(0,0,0,alpha); break;}
			case gui_TransBlack  		: { stroke(1,1,1,1); break;}	    	
			case gui_FaintGray 			: { stroke(120,120,120,250); break;}
			case gui_FaintRed 	 		: { stroke(110,0,0,alpha); break;}
			case gui_FaintBlue 	 		: { stroke(0,0,110,alpha); break;}
			case gui_FaintGreen 		: { stroke(0,110,0,alpha); break;}
			case gui_FaintYellow 		: { stroke(110,110,0,alpha); break;}
			case gui_FaintCyan  		: { stroke(0,110,110,alpha); break;}
			case gui_FaintMagenta  		: { stroke(110,0,110,alpha); break;}
			case gui_TransGray 	 		: { stroke(150,150,150,alpha/4); break;}
			case gui_TransRed 	 		: { stroke(255,0,0,alpha/2); break;}
			case gui_TransBlue 	 		: { stroke(0,0,255,alpha/2); break;}
			case gui_TransGreen 		: { stroke(0,255,0,alpha/2); break;}
			case gui_TransYellow 		: { stroke(255,255,0,alpha/2); break;}
			case gui_TransCyan  		: { stroke(0,255,255,alpha/2); break;}
			case gui_TransMagenta  		: { stroke(255,0,255,alpha/2); break;}
			case gui_OffWhite			: { stroke(248,248,255,alpha);break; }
			default         			: { stroke(55,55,255,alpha); break; }
		}//switch	
	}//setcolorValStroke	
	
	public void setColorValFillAmb(int colorVal){ setColorValFillAmb(colorVal,255);}
	public void setColorValFillAmb(int colorVal, int alpha){
		switch (colorVal){
			case gui_rnd				: { fill(random(255),random(255),random(255),alpha); ambient(120,120,120);break;}
			case gui_White  			: { fill(255,255,255,alpha); ambient(255,255,255); break; }
			case gui_Gray   			: { fill(120,120,120,alpha); ambient(120,120,120); break;}
			case gui_Yellow 			: { fill(255,255,0,alpha); ambient(255,255,0); break; }
			case gui_Cyan   			: { fill(0,255,255,alpha); ambient(0,255,alpha); break; }
			case gui_Magenta			: { fill(255,0,255,alpha); ambient(255,0,alpha); break; }
			case gui_Red    			: { fill(255,0,0,alpha); ambient(255,0,0); break; }
			case gui_Blue				: { fill(0,0,255,alpha); ambient(0,0,alpha); break; }
			case gui_Green				: { fill(0,255,0,alpha); ambient(0,255,0); break; } 
			case gui_DarkGray   		: { fill(80,80,80,alpha); ambient(80,80,80); break;}
			case gui_DarkRed    		: { fill(120,0,0,alpha); ambient(120,0,0); break;}
			case gui_DarkBlue   		: { fill(0,0,120,alpha); ambient(0,0,120); break;}
			case gui_DarkGreen  		: { fill(0,120,0,alpha); ambient(0,120,0); break;}
			case gui_DarkYellow 		: { fill(120,120,0,alpha); ambient(120,120,0); break;}
			case gui_DarkMagenta		: { fill(120,0,120,alpha); ambient(120,0,120); break;}
			case gui_DarkCyan   		: { fill(0,120,120,alpha); ambient(0,120,120); break;}		   
			case gui_LightGray   		: { fill(200,200,200,alpha); ambient(200,200,200); break;}
			case gui_LightRed    		: { fill(255,110,110,alpha); ambient(255,110,110); break;}
			case gui_LightBlue   		: { fill(110,110,255,alpha); ambient(110,110,alpha); break;}
			case gui_LightGreen  		: { fill(110,255,110,alpha); ambient(110,255,110); break;}
			case gui_LightYellow 		: { fill(255,255,110,alpha); ambient(255,255,110); break;}
			case gui_LightMagenta		: { fill(255,110,255,alpha); ambient(255,110,alpha); break;}
			case gui_LightCyan   		: { fill(110,255,255,alpha); ambient(110,255,alpha); break;}	    	
			case gui_Black			 	: { fill(0,0,0,alpha); ambient(0,0,0); break;}//
			case gui_TransBlack  	 	: { fill(0x00010100); ambient(0,0,0); break;}//	have to use hex so that alpha val is not lost    	
			case gui_FaintGray 		 	: { fill(77,77,77,alpha/3); ambient(77,77,77); break;}//
			case gui_FaintRed 	 	 	: { fill(110,0,0,alpha/2); ambient(110,0,0); break;}//
			case gui_FaintBlue 	 	 	: { fill(0,0,110,alpha/2); ambient(0,0,110); break;}//
			case gui_FaintGreen 	 	: { fill(0,110,0,alpha/2); ambient(0,110,0); break;}//
			case gui_FaintYellow 	 	: { fill(110,110,0,alpha/2); ambient(110,110,0); break;}//
			case gui_FaintCyan  	 	: { fill(0,110,110,alpha/2); ambient(0,110,110); break;}//
			case gui_FaintMagenta  	 	: { fill(110,0,110,alpha/2); ambient(110,0,110); break;}//
			case gui_TransGray 	 	 	: { fill(120,120,120,alpha/8); ambient(120,120,120); break;}//
			case gui_TransRed 	 	 	: { fill(255,0,0,alpha/2); ambient(255,0,0); break;}//
			case gui_TransBlue 	 	 	: { fill(0,0,255,alpha/2); ambient(0,0,alpha); break;}//
			case gui_TransGreen 	 	: { fill(0,255,0,alpha/2); ambient(0,255,0); break;}//
			case gui_TransYellow 	 	: { fill(255,255,0,alpha/2); ambient(255,255,0); break;}//
			case gui_TransCyan  	 	: { fill(0,255,255,alpha/2); ambient(0,255,alpha); break;}//
			case gui_TransMagenta  	 	: { fill(255,0,255,alpha/2); ambient(255,0,alpha); break;}//   	
			case gui_OffWhite			: { fill(248,248,255,alpha);ambient(248,248,255); break; }
			default         			: { fill(255,255,255,alpha); ambient(255,255,alpha); break; }	    
			
		}//switch	
	}//setcolorValFill
	
	//returns one of 30 predefined colors as an array (to support alpha)
	public int[] getClr(int colorVal){		return getClr(colorVal, 255);	}//getClr
	public int[] getClr(int colorVal, int alpha){
		switch (colorVal){
		case gui_Gray   		         : { return new int[] {120,120,120,alpha}; }
		case gui_White  		         : { return new int[] {255,255,255,alpha}; }
		case gui_Yellow 		         : { return new int[] {255,255,0,alpha}; }
		case gui_Cyan   		         : { return new int[] {0,255,255,alpha};} 
		case gui_Magenta		         : { return new int[] {255,0,255,alpha};}  
		case gui_Red    		         : { return new int[] {255,0,0,alpha};} 
		case gui_Blue			         : { return new int[] {0,0,255,alpha};}
		case gui_Green			         : { return new int[] {0,255,0,alpha};}  
		case gui_DarkGray   	         : { return new int[] {80,80,80,alpha};}
		case gui_DarkRed    	         : { return new int[] {120,0,0,alpha};}
		case gui_DarkBlue  	 	         : { return new int[] {0,0,120,alpha};}
		case gui_DarkGreen  	         : { return new int[] {0,120,0,alpha};}
		case gui_DarkYellow 	         : { return new int[] {120,120,0,alpha};}
		case gui_DarkMagenta	         : { return new int[] {120,0,120,alpha};}
		case gui_DarkCyan   	         : { return new int[] {0,120,120,alpha};}	   
		case gui_LightGray   	         : { return new int[] {200,200,200,alpha};}
		case gui_LightRed    	         : { return new int[] {255,110,110,alpha};}
		case gui_LightBlue   	         : { return new int[] {110,110,255,alpha};}
		case gui_LightGreen  	         : { return new int[] {110,255,110,alpha};}
		case gui_LightYellow 	         : { return new int[] {255,255,110,alpha};}
		case gui_LightMagenta	         : { return new int[] {255,110,255,alpha};}
		case gui_LightCyan   	         : { return new int[] {110,255,255,alpha};}
		case gui_Black			         : { return new int[] {0,0,0,alpha};}
		case gui_FaintGray 		         : { return new int[] {110,110,110,alpha};}
		case gui_FaintRed 	 	         : { return new int[] {110,0,0,alpha};}
		case gui_FaintBlue 	 	         : { return new int[] {0,0,110,alpha};}
		case gui_FaintGreen 	         : { return new int[] {0,110,0,alpha};}
		case gui_FaintYellow 	         : { return new int[] {110,110,0,alpha};}
		case gui_FaintCyan  	         : { return new int[] {0,110,110,alpha};}
		case gui_FaintMagenta  	         : { return new int[] {110,0,110,alpha};}    	
		case gui_TransBlack  	         : { return new int[] {1,1,1,alpha/2};}  	
		case gui_TransGray  	         : { return new int[] {110,110,110,alpha/2};}
		case gui_TransLtGray  	         : { return new int[] {180,180,180,alpha/2};}
		case gui_TransRed  	         	 : { return new int[] {110,0,0,alpha/2};}
		case gui_TransBlue  	         : { return new int[] {0,0,110,alpha/2};}
		case gui_TransGreen  	         : { return new int[] {0,110,0,alpha/2};}
		case gui_TransYellow  	         : { return new int[] {110,110,0,alpha/2};}
		case gui_TransCyan  	         : { return new int[] {0,110,110,alpha/2};}
		case gui_TransMagenta  	         : { return new int[] {110,0,110,alpha/2};}	
		case gui_TransWhite  	         : { return new int[] {220,220,220,alpha/2};}	
		case gui_OffWhite				 : { return new int[] {255,255,235,alpha};}
		default         		         : { return new int[] {255,255,255,alpha};}    
		}//switch
	}//getClr
	
	public int getRndClrInt(){return (int)random(0,23);}		//return a random color flag value from below
	public int[] getRndClr(int alpha){return new int[]{(int)random(0,255),(int)random(0,255),(int)random(0,255),alpha};	}
	public int[] getRndClr(){return getRndClr(255);	}		
	public Integer[] getClrMorph(int a, int b, double t){return getClrMorph(getClr(a), getClr(b), t);}    
	public Integer[] getClrMorph(int[] a, int[] b, double t){
		if(t==0){return new Integer[]{a[0],a[1],a[2],a[3]};} else if(t==1){return new Integer[]{b[0],b[1],b[2],b[3]};}
		return new Integer[]{(int)(((1.0f-t)*a[0])+t*b[0]),(int)(((1.0f-t)*a[1])+t*b[1]),(int)(((1.0f-t)*a[2])+t*b[2]),(int)(((1.0f-t)*a[3])+t*b[3])};
	}

	//used to generate random color
	public static final int gui_rnd = -1;
	//color indexes
	public static final int gui_Black 	= 0;
	public static final int gui_White 	= 1;	
	public static final int gui_Gray 	= 2;
	
	public static final int gui_Red 	= 3;
	public static final int gui_Blue 	= 4;
	public static final int gui_Green 	= 5;
	public static final int gui_Yellow 	= 6;
	public static final int gui_Cyan 	= 7;
	public static final int gui_Magenta = 8;
	
	public static final int gui_LightRed = 9;
	public static final int gui_LightBlue = 10;
	public static final int gui_LightGreen = 11;
	public static final int gui_LightYellow = 12;
	public static final int gui_LightCyan = 13;
	public static final int gui_LightMagenta = 14;
	public static final int gui_LightGray = 15;

	public static final int gui_DarkCyan = 16;
	public static final int gui_DarkYellow = 17;
	public static final int gui_DarkGreen = 18;
	public static final int gui_DarkBlue = 19;
	public static final int gui_DarkRed = 20;
	public static final int gui_DarkGray = 21;
	public static final int gui_DarkMagenta = 22;
	
	public static final int gui_FaintGray = 23;
	public static final int gui_FaintRed = 24;
	public static final int gui_FaintBlue = 25;
	public static final int gui_FaintGreen = 26;
	public static final int gui_FaintYellow = 27;
	public static final int gui_FaintCyan = 28;
	public static final int gui_FaintMagenta = 29;
	
	public static final int gui_TransBlack = 30;
	public static final int gui_TransGray = 31;
	public static final int gui_TransMagenta = 32;	
	public static final int gui_TransLtGray = 33;
	public static final int gui_TransRed = 34;
	public static final int gui_TransBlue = 35;
	public static final int gui_TransGreen = 36;
	public static final int gui_TransYellow = 37;
	public static final int gui_TransCyan = 38;	
	public static final int gui_TransWhite = 39;	
	public static final int gui_OffWhite = 40;

}
