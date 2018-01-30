package SphrSeqFFTVisPKG;

import static jcuda.driver.CUgraphicsMapResourceFlags.CU_GRAPHICS_MAP_RESOURCE_FLAGS_WRITE_DISCARD;
import static jcuda.driver.JCudaDriver.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;

import javax.swing.*;

import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.Animator;

import jcuda.*;
import jcuda.driver.*;
import jcuda.jcufft.*;
import processing.opengl.PJOGL;
//import processing.opengl.PJOGL;
//import processing.opengl.PShader;
import ddf.minim.analysis.*;
import ddf.minim.*;

public class mySimWindow extends myDispWindow {
	
	myMP3SongHandler[] songs;
	FFT fftSeqLog;
	public final int fftMinBandwidth = 22, fftBandsPerOctave = 24;
	public final int songBufSize = 1024;
	float[] blankRes1 = new float[songBufSize];
	float[] blankRes2 = new float[songBufSize];
	//per zone avg frequencies - avg pwr across all frequencies within each 1/numZones fraction of spectrum
	//float[] blankBands = new float[numZones];
	
	String[] songTitles = new String[]{"sati.mp3","PurpleHaze.mp3","karelia.mp3","choir.mp3"};
	String[] songList = new String[]{"Sati","PurpleHaze","Karelia","Choir"};
	
	public int songIDX;
	AudioOutput notesIn;					//notes currently playing in system - just get output from current myDispWindow
	
	//current index of windowing function, from ui
	int curWindowIDX = 0;
	WindowFunction[] windowList = new WindowFunction[]{FFT.NONE, FFT.BARTLETT, FFT.BARTLETTHANN, FFT.BLACKMAN, FFT.COSINE, FFT.GAUSS, FFT.HAMMING, FFT.HANN, FFT.LANCZOS, FFT.TRIANGULAR};
	String[] windowNames = new String[]{"NONE","BARTLETT","BARTLETTHANN","BLACKMAN","COSINE","GAUSS","HAMMING","HANN","LANCZOS","TRIANGULAR"};

	int[] perSongBuildKFuncs = new int[] {myOcean.bldFreqIDX,myOcean.bldFreq2IDX,myOcean.bldFreq2IDX,myOcean.bldFreq2IDX};	
	
	public myOcean fftOcean;
	
	//shader code as string
	public String vertexShaderSource ="#version 130\n" + 
			"varying vec3 eyeSpacePos;\n" +
			"varying vec3 worldSpaceNormal;\n" +
			"varying vec3 eyeSpaceNormal;\n" +
			"uniform float heightScale; // = 0.5;\n" +
			"uniform float chopiness;   // = 1.0;\n" +
			"uniform vec2  size;        // = vec2(256.0, 256.0);\n" +
			"void main(){\n" +
			"    float height     = gl_MultiTexCoord0.x;\n" +
			"    vec2  slope      = gl_MultiTexCoord1.xy;\n" +
			"	vec3 normal      = normalize(cross( vec3(0.0, slope.y*heightScale, 2.0 / size.x), vec3(2.0 / size.y, slope.x*heightScale, 0.0)));\n" +
			"    worldSpaceNormal = normal;\n" +
			"    vec4 pos         = vec4(gl_Vertex.x, height * heightScale, gl_Vertex.z, 1.0);\n" +
			"    gl_Position      = gl_ModelViewProjectionMatrix * pos;\n" +
			"eyeSpacePos      = (gl_ModelViewMatrix * pos).xyz;\n" +
			"eyeSpaceNormal   = (gl_NormalMatrix * normal).xyz;\n" +
			"}\n";

	public String fragmentShaderSource = "#version 130\n" + 
			"varying vec3 eyeSpacePos;\n" +
			"varying vec3 worldSpaceNormal;\n" +
			"varying vec3 eyeSpaceNormal;\n" +
			"uniform vec4 deepColor;\n" +
			"uniform vec4 shallowColor;\n" +
			"uniform vec4 skyColor;\n" +
			"uniform vec3 lightDir;\n" +
			"void main(){\n"  +
			"    vec3 eyeVector              = normalize(eyeSpacePos);\n" +
			"    vec3 eyeSpaceNormalVector   = normalize(eyeSpaceNormal);\n" +
			"    vec3 worldSpaceNormalVector = normalize(worldSpaceNormal);\n" +
			"    float facing    = max(0.0, dot(eyeSpaceNormalVector, -eyeVector));\n" +
			"    float fresnel   = pow(1.0 - facing, 5.0); // Fresnel approximation\n" +
			"    float diffuse   = max(0.0, dot(worldSpaceNormalVector, lightDir));  \n" +
			"    vec4 waterColor = mix(shallowColor, deepColor, facing);\n" +
			"    gl_FragColor = waterColor*diffuse + skyColor*fresnel;\n" +
			"}\n";
		
	
	//public boolean[] privFlags;
	public static final int 
			oceanMadeIDX 			= 0,
			useAudioForOceanIDX 	= 1,					//true if use mp3 for vis, false if use song notes
			forceCudaRecompIDX		= 2,					//force the recompile of the cuda .ptx even if one exists
			fftLogLoadedIDX			= 3,
			audioLoadedIDX 			= 4,
			seqLoadedIDX 			= 5,
			playVisIDX				= 6,					//play audio for visualization
			showFreqDomainIDX		= 7;
	public static final int numPrivFlags = 8;

//	//GUI Objects	
	//idx's of objects in gui objs array - relate to modifications of oceanFFT sim code
	public final static int 
		songSelIDX			= 0,
		winSelIDX			= 1,
		patchSizeIDX 		= 2,             //			patchSize 	: Phillips eq : 
		windSpeedIDX 	 	= 3,             //			windSpeed 	: Phillips eq : 
		windDirIDX  		= 4,             //			windDir   	: Phillips eq : 
		dirDependIDX  		= 5,             //			dirDepend 	: Phillips eq : 
		heightScaleIDX  	= 6,             //			heightScale	:
		freqMixIDX  		= 7,			 //			freqMix		: Mixture amount of pure phillips wave noise to song frequencies - 100 is all song, 0 is all phillips
		chopinessIDX  		= 8,			 //			chopiness	: 	
		
		songTransIDX		= 9;			//extended object - transport interaction
		//threshIDX			= 9,			//noise threshold - if less power in signal than this then force to 0
	public final int numGUIObjs = 10;												//# of gui objects for ui
	
	public mySimWindow(SeqVisFFTOcean _p, String _n, int _flagIdx, int[] fc, int[] sc, float[] rd, float[] rdClosed,
			String _winTxt, boolean _canDrawTraj) {
		super(_p, _n, _flagIdx, fc, sc, rd, rdClosed, _winTxt, _canDrawTraj);
		float stY = rectDim[1]+rectDim[3]-4*yOff,stYFlags = stY + 2*yOff;	
		//build fftocean window only when window actually shown
		//launchOceanWin();
		super.initThisWin(_canDrawTraj, false);
		setSongsAndFFT();
	}	
	
	@Override
	//initialize all private-flag based UI buttons here - called by base class
	public void initAllPrivBtns(){
		truePrivFlagNames = new String[]{								//needs to be in order of flags
				//"Playing audio", 
				"Vis : MP3->Seq","Turn Off PTX Comp ", "Showing Freq Domain"};
		falsePrivFlagNames = new String[]{			//needs to be in order of flags
			//	"Stopped audio",
				"Vis : Seq->MP3","Turn On PTX Comp ", "Showing Time Domain"};
		privModFlgIdxs = new int[]{//playVisIDX, 
				useAudioForOceanIDX, forceCudaRecompIDX,showFreqDomainIDX};
		numClickBools = privModFlgIdxs.length;
		//maybe have call for 		initPrivBtnRects(0);	
		initPrivBtnRects(0,numClickBools);
	}
	
	//init any extra ui objs
	@Override
	protected void initXtraUIObjsIndiv() {}


	@Override
	protected void initMe() {
//		dispFlags[uiObjsAreVert] = true;
		//init specific sim flags
		dispFlags[plays] = true;						//this window responds to transport
		initPrivFlags(numPrivFlags);
		setPrivFlags(useAudioForOceanIDX, true);				//for testing, need to start with sequence data shown
		songs = new myMP3SongHandler[songTitles.length];
	}
	
	protected void setSongsAndFFT() {
		loadSongsAndFFT();
		setFFTVals();
	}
	
	protected void setFFTVals() {
		if (privFlags[fftLogLoadedIDX]){
			for(int i=0;i<songTitles.length;++i){songs[i].setFFTVals(windowList[curWindowIDX], fftMinBandwidth, fftBandsPerOctave);	}	
		}
	}//setFFTVals
	
	protected void loadSongsAndFFT() {
		for(int i=0;i<songTitles.length;++i){
			songs[i] = new myMP3SongHandler(pa, songTitles[i], songList[i], songBufSize);
		}		
		setPrivFlags(audioLoadedIDX,true);
		setPrivFlags(fftLogLoadedIDX, true);
	}//loadSongList() 


	//set flag values and execute special functionality for this sequencer
	@Override
	public void setPrivFlags(int idx, boolean val){
		privFlags[idx] = val;
		switch(idx){
		case oceanMadeIDX 			: {break;}			//any specific code for when ocean is made or cleared	
		case playVisIDX				: { //turn play on/off
			if(val){launchOceanVis(privFlags[useAudioForOceanIDX]);	}
			else {	killOceanVis(privFlags[useAudioForOceanIDX]);	}
			break;
		}
		case showFreqDomainIDX 		: {
			fftOcean.cudaFlags[fftOcean.performInvFFT] = !val;
			break;}
		case useAudioForOceanIDX 	: {
			if(privFlags[playVisIDX]){
				killOceanVis(!val);launchOceanVis(val);	//stop old vis, start new vis if playing
			}
			break;}			//any specific code for when we change from audio to mp3 and back				
		case forceCudaRecompIDX 	: {
			if(val){//recompile and remake visualization
				closeMe();//closes ocean - called when this window is closed, but won't close this window
				//change window to spheres
				pa.handleShowWin(1, 1);
			}			
			break;}			//any specific code for when the audio file is actually loaded					
		case fftLogLoadedIDX		: {break;}			//any specific code for when the fft logger is loaded
		case audioLoadedIDX 		: {break;}			//any specific code for when the audio file is actually loaded					
		case seqLoadedIDX 			: {break;}			//any specific code for when the seq data is actually retrieved					
		}			
	}//setPRLFlags
	
	protected void launchOceanVis(boolean isMP3){	if(isMP3){launchAudioOcean();} else {launchSeqOcean();}}	
	protected void killOceanVis(boolean isMP3){if(isMP3){killAudioOcean();} else {killSeqOcean();}}
	
	protected void launchAudioOcean(){
		if(!privFlags[audioLoadedIDX]){//load songs if not loaded already
			setSongsAndFFT();
		}
		//pa.outStr2Scr("Song in buffer : " + songTitles[songIDX] + " size: " +  songs[songIDX].bufferSize() + " Sample rate : "+ songs[songIDX].sampleRate());
		songs[songIDX].play();
		if((!privFlags[oceanMadeIDX]) || (null == fftOcean) || (!fftOcean.cudaFlags[fftOcean.doneInit])){return;}
		fftOcean.setFreqVals(blankRes1, blankRes2);		
	}
	
	protected void killAudioOcean(){
		if(privFlags[audioLoadedIDX]){
			songs[songIDX].pause();
		}		
	}
	protected void launchSeqOcean(){
		if(!privFlags[seqLoadedIDX]){
			setPrivFlags(seqLoadedIDX,true);			
			notesIn = pa.glblOut;
			pa.outStr2Scr("Notes in buf size : " + notesIn.bufferSize() + " sample rate : " + notesIn.sampleRate());
			//sum all outputs before using
			fftSeqLog = new FFT(notesIn.bufferSize(), notesIn.sampleRate()); 
			fftSeqLog.logAverages(fftMinBandwidth, fftBandsPerOctave);
			if((!privFlags[oceanMadeIDX]) || (null == fftOcean) || (!fftOcean.cudaFlags[fftOcean.doneInit])){return;}
			//fftOcean.setFreqVals(new float[1024], new float[1024]);
			fftOcean.setFreqVals(blankRes1, blankRes2);
		}
	}
	protected void killSeqOcean(){
		if(privFlags[seqLoadedIDX]){
			notesIn.close();
			notesIn = null;
			setPrivFlags(seqLoadedIDX,false);
		}		
	}	
	
	public void launchOceanWin(){
		final GLCapabilities capabilities = new GLCapabilities(GLProfile.get(GLProfile.GL2));
		final mySimWindow thisWin = this;
		SwingUtilities.invokeLater(new Runnable() {	public void run() {fftOcean = new myOcean(pa, thisWin, capabilities);}});//launch in another thread	
		//test
		//PShader myShader = new PShader(pa, new String[] {vertexShaderSource}, new String[] {fragmentShaderSource});

		setPrivFlags(oceanMadeIDX, true);
		setPrivFlags(playVisIDX, true);
	}

	public int getFreqKFunc() {		return perSongBuildKFuncs[songIDX];	}
	
	public void setOceanFreqVals(){
		if((!privFlags[oceanMadeIDX]) || (null == fftOcean) || (!fftOcean.cudaFlags[fftOcean.doneInit])){return;}
		if(privFlags[useAudioForOceanIDX]){	setOceanAudio();	} else {setOceanNotes();	}
	}

	//send notes from sequencer to ocean - let ocean sample notes
	public void setOceanNotes(){
//		SortedMap<Integer, myNote> notes = pa.dispWinFrames[pa.curFocusWin].getNotesNow();
//		float[] res1 = new float[notes.size()],res2 = new float[notes.size()];
//		int idx = 0;
//		myNote note;
//		for(Map.Entry<Integer,myNote> noteEntry : notes.entrySet()) {
//			note = noteEntry.getValue();
//			res1[idx] = note.n.freq;			//for now - cuda will do what is necessary with this.
//			res2[idx] = note.n.freq;
//			idx++;
//		}
//		fftOcean.isNoteData = 1;
//		fftOcean.freqsInLen = res1.length;
//		fftOcean.setFreqVals(res1, res2);
		if((notesIn != null) && (notesIn.mix != null)){
			fftSeqLog.forward( notesIn.mix );
			float[] res1 = fftSeqLog.getSpectrumReal();
			float[] res2 = fftSeqLog.getSpectrumImaginary();
			fftOcean.isNoteData = 1;
			fftOcean.freqsInLen = res1.length;
			//pa.outStr2Scr("minim fft spectrum extremals : res1 len : "+res1.length+" | res2 len : "+res2.length);
			fftOcean.setFreqVals(res1, res2);
		}
	}
		
	//send notes from audio to ocean - let ocean govern audio - called from draw
	public void setOceanAudio(){
		songs[songIDX].fftFwdOnAudio();
		float[][] res = songs[songIDX].fftSpectrumFromAudio();
		float[] bandRes = songs[songIDX].fftFwdBandsFromAudio();
		//pa.outStr2Scr("size of band res : " + bandRes.length);
		setSongTransInfo();

		fftOcean.isNoteData = 0;
		fftOcean.freqsInLen = res[0].length;
//		pa.outStr2Scr("minim fft spectrum extremals : 1 "+minr1+" | "+maxr1+" |2 "+minr2+" | "+maxr2);
		fftOcean.setFreqVals(res[0], res[1]);
	}
	
	public void delOcean(){
		setPrivFlags(oceanMadeIDX, false);
		setPrivFlags(playVisIDX, false);
		//killOceanVis(this.privFlags[this.useAudioForOceanIDX]);
	}

	//put rendering in here
	@Override
	protected void drawMe(float animTimeMod) {
		//pa.outStr2Scr("calling draw in sim window");
		setOceanFreqVals();								//set frequency values by either querying the fft of the audio file, or else by getting the notes of the currently playing sequence
	}

	
	@Override
	protected void setupGUIObjsAras(){
		//pa.outStr2Scr("setupGUIObjsAras in :"+ name);
		guiMinMaxModVals = new double [][]{  
			{0.0, songTitles.length-1, 1.0},	//song selected
			{0.0, windowNames.length-1, 1.0},	//window function selected
			{25.0, 125.0, 0.1},			//patchSizeIDX 	
			{0.0, 100.0, 0.1},			//windSpeedIDX 
			{0.0, pa.TWO_PI, 0.01},		//windDirIDX  	
			{0.01, 0.25, 0.0001},		//dirDependIDX  
			{0.01, 10.0, 0.01},       	//heightScaleIDX
			{0.0, 1.0,0.01},       	//freqMixIDX  	
			{0.0, 2.0, 0.01},       		//chopinessIDX 
			//{0.0, 10.0, 0.01},//threshIDX
		};					
		guiStVals = new double[]{
				0.0,0.0,
				75.0, 50.0, (Math.PI / 3.0f),0.07, 
				0.5, 
				0.45, 
				1.0, 
			//	0.0,//threshIDX
				};								//starting value
		guiObjNames = new String[]{
				"MP3 Song", "Window Function",
				"Patch Size","Wind Speed","Wind Direction","Wind Dir Strictness (fltr)",
				"Height Map Scale","Mix of Noise and Audio","Chopiness",
				//"Power Threshold",
				};	//name/label of component	
					
		//idx 0 is treat as int, idx 1 is obj has list vals, idx 2 is object gets sent to windows
		guiBoolVals = new boolean [][]{
			{true, true, true},			//song selected
			{true, true, true},			//window function selected
			{false, false, true},      //patchSizeIDX 	
			{false, false, true},      //windSpeedIDX 
			{false, false, true},      //windDirIDX  	
			{false, false, true},      //dirDependIDX  
			{false, false, true},      //heightScaleIDX
			{false, false, true},      //freqMixIDX  	
			{false, false, true},      //chopinessIDX  	
			//{false, false, true},      //threshIDX  	
		};						//per-object  list of boolean flags
			
		guiObjs = new myGUIObj[numGUIObjs];			//list of modifiable gui objects
		if(numGUIObjs > 0){
			buildGUIObjs(guiObjNames,guiStVals,guiMinMaxModVals,guiBoolVals,new double[]{xOff,yOff});			//builds a horizontal list of UI comps
		}
		setupGUI_XtraObjs();
	}//setupGUIObjsAras
	//setup UI object for song slider
	private void setupGUI_XtraObjs() {
		double stClkY = uiClkCoords[3], sizeClkY = 3*yOff;
		guiObjs[songTransIDX] = new myGUIBar(pa, this, songTransIDX, "MP3 Transport for ", 
				new myVector(0, stClkY,0), new myVector(uiClkCoords[2], stClkY+sizeClkY,0),
				new double[] {0.0, 1.0,0.1}, 0.0, new boolean[]{false, false, true}, new double[]{xOff,yOff});	
		
		//setup space for ui interaction with song bar
		stClkY += sizeClkY;				
		uiClkCoords[3] = stClkY;
	}
	
	private void setSongTransInfo() {
		guiObjs[songTransIDX].setName("MP3 Transport for " + songs[songIDX].dispName);
		guiObjs[songTransIDX].setVal(songs[songIDX].getPlayPosRatio());		
	}
	
	public void sendUIValsToOcean(){for(int i=0;i<numGUIObjs; ++i){setUIWinVals(i);}}//send ui vals

	@Override
	public void clickDebug(int btnNum){
		pa.outStr2Scr("click debug in "+name+" : btn : " + btnNum);
		switch(btnNum){
			case 0 : {//note vals
				break;
			}
			case 1 : {//measure vals
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
	
	//if any ui values have a string behind them for display
	@Override
	protected String getUIListValStr(int UIidx, int validx) {
		switch(UIidx){
		case songSelIDX : {return songList[(validx % songList.length)]; }
		case winSelIDX  : {return windowNames[(validx % windowNames.length)]; }
		default : {break;}
		}
		return "";
	}
	
	public void changeCurrentSong(int newSongIDX){
		songs[songIDX].pause();
		//songs[songIDX].rewind();
		songIDX = newSongIDX;
		if((privFlags[useAudioForOceanIDX]) && (privFlags[playVisIDX])){
			songs[songIDX].play();
		}
		setSongTransInfo();
	}
	
	public void changeCurrentWindowfunc(int newWinFuncIDX) {
		curWindowIDX = newWinFuncIDX;
		if(privFlags[fftLogLoadedIDX]) {
			setFFTVals();
		}		
	}
	
	protected void setOceanFFTVal(String UIidx, float value, float oceanVal){if((oceanVal != value) && (fftOcean.tmpSimVals.get(UIidx) != value)){	fftOcean.setNewSimVal(UIidx, value);}}//setOceanFFTVal		
	//set appropriate values from UI
	@Override
	protected void setUIWinVals(int UIidx) {
		if((!privFlags[oceanMadeIDX]) || (null == fftOcean) || (!fftOcean.cudaFlags[fftOcean.doneInit])){return;}
		float val =  (float)(guiObjs[UIidx].getVal());
		switch(UIidx){
			case patchSizeIDX 	: {setOceanFFTVal("patchSizeIDX", val, fftOcean.patchSize);break;}//			patchSize 	: Phillips eq : 
			case windSpeedIDX 	: {setOceanFFTVal("windSpeedIDX", val, fftOcean.windSpeed);break;}//			windSpeed 	: Phillips eq : 
			case windDirIDX  	: {setOceanFFTVal("windDirIDX", val, fftOcean.windDir);break;}	//			windDir   	: Phillips eq : 
			case dirDependIDX  	: {setOceanFFTVal("dirDependIDX", val, fftOcean.dirDepend);break;}//			dirDepend 	: Phillips eq : 
			case heightScaleIDX : {setOceanFFTVal("heightScaleIDX", val, fftOcean.heightScale);break;}//			heightScale	:
			case freqMixIDX  	: {setOceanFFTVal("freqMixIDX", (float)(guiObjs[UIidx].getVal()/100.0), fftOcean.freqMix);break;}	 	//			freqMix		: Mixture amount of pure phillips wave noise to song frequencies - 100 is all song, 0 is all phillips
			case chopinessIDX  	: {setOceanFFTVal("chopinessIDX", val, fftOcean.chopiness);break;}//			chopiness	: 	
			//case threshIDX		: {setOceanFFTVal("threshIDX", (float)(guiObjs[UIidx].getVal()), fftOcean.thresh);break;}//			threshold	: 	
			case songSelIDX 	: {changeCurrentSong((int)(guiObjs[UIidx].getVal()));break;}
			case winSelIDX		: {changeCurrentWindowfunc((int)(guiObjs[UIidx].getVal()));	break;}
			case songTransIDX	: {
				//System.out.println("val : " + val);
				songs[songIDX].modPlayLoc(val);    break;}
		default : {break;}
		}
	}
	//put entry point into simulation here - music is told to start
	@Override
	protected void playMe() {
		setPrivFlags(playVisIDX, true);
		System.out.println("play all songs");
		changeCurrentSong(songIDX);
	}
	//when music stops
	@Override
	protected void stopMe() {
		setPrivFlags(playVisIDX, false);
		System.out.println("Stop all songs");
		songs[songIDX].pause();
	}
	
	//move current play position when playing mp3/sample (i.e. something not controlled by pbe reticle
	@Override
	protected void modMySongLoc(float modAmt) {
		songs[songIDX].modPlayLoc(modAmt);
		setSongTransInfo();
	};

	
	@Override
	protected void setScoreInstrValsIndiv(){}
	@Override
	protected void snapMouseLocs(int oldMouseX, int oldMouseY, int[] newMouseLoc) {}	
	@Override
	protected void processTrajIndiv(myDrawnNoteTraj drawnNoteTraj){}
	@Override
	protected boolean hndlMouseMoveIndiv(int mouseX, int mouseY, myPoint mseClckInWorld){	return false;}
	@Override
	protected boolean hndlMouseClickIndiv(int mouseX, int mouseY, myPoint mseClckInWorld, int mseBtn) {		return checkUIButtons(mouseX, mouseY);}//hndlMouseClickIndiv
	@Override
	protected boolean hndlMouseDragIndiv(int mouseX, int mouseY, int pmouseX, int pmouseY, myPoint mouseClickIn3D, myVector mseDragInWorld, int mseBtn) {return false;}
	@Override
	protected SortedMap<Integer, myNote> getNotesNow() {return null;}
	@Override
	//set current key signature, at time passed - for score, set it at nearest measure boundary
	protected void setGlobalKeySigValIndiv(int idx, float time){	}//setCurrentKeySigVal
	@Override
	//set time signature at time passed - for score, set it at nearest measure boundary
	protected void setGlobalTimeSigValIndiv(int tsnum, int tsdenom, durType _d, float time){	}//setCurrentTimeSigVal
	@Override
	//set time signature at time passed - for score, set it at nearest measure boundary
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
	@Override
	protected myPoint getMouseLoc3D(int mouseX, int mouseY){
		//TODO need to fix this for sim
		return pa.P(mouseX,mouseY,0);
	}
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
	protected void showMe(){if(null == fftOcean){	launchOceanWin();	}}	
	@Override
	protected void closeMe() {
		if(null!= fftOcean){		
			fftOcean.frame.dispatchEvent(new WindowEvent(fftOcean.frame, WindowEvent.WINDOW_CLOSING));	
			//delOcean() called in closed event handler for frame
			fftOcean = null;
		}
	}//closeMe

}//class mySimWIndow

//class implementing external window for fft ocean surface, based on cuda example.
class myOcean implements GLEventListener{
	public SeqVisFFTOcean pa;
	public mySimWindow win;
	public JFrame frame;
	public Animator animator;
	public GL2 gl;	

//	//shader code as string
//	public String vertexShaderSource ="#version 130\n" + 
//			"varying vec3 eyeSpacePos;\n" +
//			"varying vec3 worldSpaceNormal;\n" +
//			"varying vec3 eyeSpaceNormal;\n" +
//			"uniform float heightScale; // = 0.5;\n" +
//			"uniform float chopiness;   // = 1.0;\n" +
//			"uniform vec2  size;        // = vec2(256.0, 256.0);\n" +
//			"void main(){\n" +
//			"    float height     = gl_MultiTexCoord0.x;\n" +
//			"    vec2  slope      = gl_MultiTexCoord1.xy;\n" +
//			"	vec3 normal      = normalize(cross( vec3(0.0, slope.y*heightScale, 2.0 / size.x), vec3(2.0 / size.y, slope.x*heightScale, 0.0)));\n" +
//			"    worldSpaceNormal = normal;\n" +
//			"    vec4 pos         = vec4(gl_Vertex.x, height * heightScale, gl_Vertex.z, 1.0);\n" +
//			"    gl_Position      = gl_ModelViewProjectionMatrix * pos;\n" +
//			"eyeSpacePos      = (gl_ModelViewMatrix * pos).xyz;\n" +
//			"eyeSpaceNormal   = (gl_NormalMatrix * normal).xyz;\n" +
//			"}\n";
//
//	public String fragmentShaderSource = "#version 130\n" + 
//			"varying vec3 eyeSpacePos;\n" +
//			"varying vec3 worldSpaceNormal;\n" +
//			"varying vec3 eyeSpaceNormal;\n" +
//			"uniform vec4 deepColor;\n" +
//			"uniform vec4 shallowColor;\n" +
//			"uniform vec4 skyColor;\n" +
//			"uniform vec3 lightDir;\n" +
//			"void main(){\n"  +
//			"    vec3 eyeVector              = normalize(eyeSpacePos);\n" +
//			"    vec3 eyeSpaceNormalVector   = normalize(eyeSpaceNormal);\n" +
//			"    vec3 worldSpaceNormalVector = normalize(worldSpaceNormal);\n" +
//			"    float facing    = max(0.0, dot(eyeSpaceNormalVector, -eyeVector));\n" +
//			"    float fresnel   = pow(1.0 - facing, 5.0); // Fresnel approximation\n" +
//			"    float diffuse   = max(0.0, dot(worldSpaceNormalVector, lightDir));  \n" +
//			"    vec4 waterColor = mix(shallowColor, deepColor, facing);\n" +
//			"    gl_FragColor = waterColor*diffuse + skyColor*fresnel;\n" +
//			"}\n";
	
	public int meshSize = 1024, 
			meshSzSq = meshSize * meshSize, 
			spectrumW = meshSize + 4, spectrumH = meshSize + 1, 
			vBufNumInts = ((meshSize*2)+2)*(meshSize-1),
			freqsInLen, 
			isNoteData;		//1 if note data, 0 if audio data
	
	// simulation parameters
	public float g = 9.81f, // gravitational constant
			wScl = 1e-7f, // wave scale factor
			translateX = 0.0f,
			translateY = 0.0f,
			translateZ = -1.0f,
			rotateX = 20f,
			rotateY = 0.0f,
			animTime = 0,						//timer for every frame of animation
			//UI mod variables
			patchSize = 75.0f,
			windSpeed = 50.0f,
			windDir = (float) (Math.PI / 3.0f),
			dirDepend = 0.07f,
			heightScale = 0.5f,
			freqMix = 0,						//amount of frequency data to mix into simulation
			//thresh = 0.0f,						//noise threshold in processing of freq data
			chopiness = 1.0f
			;
	
	public ConcurrentSkipListMap<String, Float> tmpSimVals;
	//OpenGL variables
	public int shaderProgramID;

	//OpenGL & Cuda variables
	public int posVertexBuffer, heightVertexBuffer, slopeVertexBuffer, indexBuffer;	
	public CUgraphicsResource cuda_heightVB_resource, cuda_slopeVB_resource;
	
	// FFT data
	public cufftHandle fftPlan;
	public CUdeviceptr d_h0Ptr, d_htPtr, d_slopePtr, d_freqInRPtr,d_freqInCPtr, d_freqPtr;
	public float[] h_h0, audFreqRealRes, audFreqImagRes;	
	
	//JCUDA
	public CUdevice device;
	public CUcontext glContext;
	public static final int bldFreqIDX = 0, bldFreq2IDX = 1, genSpecIDX = 2, genSpec2IDX = 3, updHMapIDX = 4, calcSlopeIDX = 5;
	public String[] kFuncNames = new String[]{"buildFrequencyDataKernel","buildFrequencyDataKernel2","generateSpectrumKernel","generateSpectrumKernel2","updateHeightmapKernel","calculateSlopeKernel"};
	public CUfunction[] kFuncs;// generateSpectrumKernel, updateHeightmapKernel, calculateSlopeKernel;
	public CUmodule module;
		
	public boolean[] cudaFlags;
	public static final int 
			doneInit 		= 0, 
			newFreqVals		= 1, 
			newSimVals		= 2, 
			freqValsSet		= 3,
			forceRecomp		= 4,
			performInvFFT	= 5;
	public static final int numCudaFlags = 6;
	//colors
	public float[] deepColor, shallowColor, skyColor, lightDir;
	
	private final int gl2BufArg = GL2.GL_ARRAY_BUFFER;
		
	public myOcean(SeqVisFFTOcean _p,mySimWindow _win, GLCapabilities capabilities) {
		pa=_p;
		win = _win;
//		PJOGL pgl = (PJOGL) pa.beginPGL();  
//		GL2 tmpGL = pgl.gl.getGL2();
//		pa.endPGL();
		
	
		GLCanvas glComponent = new GLCanvas(capabilities);
		glComponent.setFocusable(true);
		glComponent.addGLEventListener(this);
		initShaderUnis();
		freqsInLen = 0;
		MouseControl mouseControl = new MouseControl();
		glComponent.addMouseMotionListener(mouseControl);
		glComponent.addMouseWheelListener(mouseControl);
		
		tmpSimVals = new ConcurrentSkipListMap<String,Float>();
		setTmpSimVals();
		initFlags();
		cudaFlags[forceRecomp] = win.privFlags[win.forceCudaRecompIDX];			//whether or not 
		cudaFlags[performInvFFT] = !win.privFlags[win.showFreqDomainIDX];
		cudaFlags[newSimVals] = false;
		frame = new JFrame("WaterSurface");
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {	animator.stop();	}
			@Override
			public void windowClosed(WindowEvent e) {animator.stop();	delMe();	}			
		});
		frame.setLayout(new BorderLayout());
		glComponent.setPreferredSize(new Dimension(800, 800));
		frame.add(glComponent, BorderLayout.CENTER);
		frame.pack();
		frame.setVisible(true);
		bringToFront();
		glComponent.requestFocus();
		
		// Create and start the animator
		animator = new Animator(glComponent);
		animator.start();
	}
	private void initFlags(){cudaFlags = new boolean[numCudaFlags]; for(int i =0; i<numCudaFlags;++i){cudaFlags[i]=false;}}
	
	public void bringToFront(){
		SwingUtilities.invokeLater(new Runnable() {
		    @Override
		    public void run() {        frame.toFront();    }
		});
	}

	public void setFreqVals(float[] res1, float[] res2){
		audFreqRealRes = res1;
		audFreqImagRes = res2;	
		cudaFlags[newFreqVals] = true;
	}
	
	//if parent UI changes any sim values
	public void setNewSimVal(String key, float val){
		tmpSimVals.put(key, val);
		//tmpSimVals[idx] = val;
		cudaFlags[newSimVals] = true;
		//need to refresh window?
	}
	
	@Override
	public void init(GLAutoDrawable drawable) {
		gl = drawable.getGL().getGL2();
		gl.setSwapInterval(0);
		gl.glClearColor(skyColor[0],skyColor[1],skyColor[2],skyColor[3]);
		gl.glEnable(GL2.GL_DEPTH_TEST);

		initShaders(gl);
		initJCuda();
		initVBO(gl);
		cudaFlags[doneInit] = true;
		win.sendUIValsToOcean();
	}
	
	public void initShaderUnis(){
		deepColor = new float[]{0.0f, 0.1f, 0.4f, 1.0f};
		shallowColor = new float[]{0.1f, 0.3f, 0.3f, 1.0f};
		skyColor = new float[]{.8f, .9f, 1.0f, 1.0f};
		lightDir = new float[]{ 0.0f, 1.0f, 0.0f};
	}
	
	public void delMe(){
		try{
			pa.outStr2Scr("Attempting to Release Ocean variables");
			cuMemFree(d_h0Ptr);
			cuMemFree(d_htPtr);
			cuMemFree(d_slopePtr);
			cuMemFree(d_freqPtr);
			cuMemFree(d_freqInRPtr);
			cuMemFree(d_freqInCPtr);
			JCufft.cufftDestroy(fftPlan);
			gl.glDeleteBuffers(1, IntBuffer.wrap(new int[] {posVertexBuffer}));
			gl.glDeleteBuffers(1, IntBuffer.wrap(new int[] {heightVertexBuffer}));
			gl.glDeleteBuffers(1, IntBuffer.wrap(new int[] {slopeVertexBuffer}));
			
		} catch (Exception e1){
			pa.outStr2Scr("error when closing frame");
			e1.printStackTrace();
		}
		//System.exit(0);
		win.delOcean();	
	}
	private void initJCuda(){
		setExceptionsEnabled(true);
		cuInit(0);
		device = new CUdevice();
		cuDeviceGet(device, 0);
		glContext = new CUcontext();
		cuGLCtxCreate(glContext, CUctx_flags.CU_CTX_BLOCKING_SYNC, device);
		//compile kernel file
		String ptxFileName = "ocean.ptx";
		File f = new File(ptxFileName);
		if(!(f.exists() && !f.isDirectory()) || (cudaFlags[forceRecomp])) { //try to compile if doesn't exist			
			try {	compilePtxFile("ocean.cu",ptxFileName);} 
			catch (IOException e) {
				System.err.println("Could not create PTX file");
				throw new RuntimeException("Could not create PTX file", e);
			}
		} else {
			//debug 
		}
		module = new CUmodule();
		cuModuleLoad(module, ptxFileName);
		kFuncs = new CUfunction[kFuncNames.length];
		for(int i =0; i<kFuncNames.length;++i){
			kFuncs[i] = new CUfunction();
			cuModuleGetFunction(kFuncs[i], module, kFuncNames[i]);
		}
		fftPlan = new cufftHandle();
		JCufft.cufftPlan2d(fftPlan, meshSize, meshSize, cufftType.CUFFT_C2C);
		
		int spectrumSize = spectrumW * spectrumH * 2;
		h_h0 = new float[spectrumSize];
		h_h0 = generate_h0(h_h0);
		d_h0Ptr = new CUdeviceptr();
		cuMemAlloc(d_h0Ptr, spectrumSize*Sizeof.FLOAT);
		cuMemcpyHtoD(d_h0Ptr, Pointer.to(h_h0), spectrumSize*Sizeof.FLOAT);
		
		int outputSize = meshSzSq*Sizeof.FLOAT*2;
		d_htPtr = new CUdeviceptr();
		d_slopePtr = new CUdeviceptr();
		
		cuMemAlloc(d_htPtr, outputSize);
		cuMemAlloc(d_slopePtr, outputSize);

		d_freqPtr = new CUdeviceptr();		//2x as bg as either d_freqInRPtr or d_freqInCPtr
		//cuMemAlloc(d_freqPtr, outputSize);
		cuMemAlloc(d_freqPtr, spectrumSize*Sizeof.FLOAT);
		//support up to 1024 simultaneous frequencies
		//make these not device ptrs? only 1 d //TODO
		d_freqInRPtr = new CUdeviceptr();
		cuMemAlloc(d_freqInRPtr, meshSize*Sizeof.FLOAT);
		d_freqInCPtr = new CUdeviceptr();
		cuMemAlloc(d_freqInCPtr, meshSize*Sizeof.FLOAT);
		cudaFlags[newFreqVals] = false;
		cudaFlags[freqValsSet] = false;	//legit values have been set
		cudaFlags[newSimVals] = false;
	}//initJCuda
	//initialize vertex buffer object
	private void initVBO(GL2 gl) {		
		int[] buffer = new int[1];
		int size = meshSzSq*Sizeof.FLOAT;
		gl.glGenBuffers(1, IntBuffer.wrap(buffer));
		heightVertexBuffer = buffer[0];
		gl.glBindBuffer(gl2BufArg, heightVertexBuffer);
		gl.glBufferData(gl2BufArg, size, (Buffer) null, GL2.GL_DYNAMIC_DRAW);
		gl.glBindBuffer(gl2BufArg, 0);
		cuda_heightVB_resource = new CUgraphicsResource();
		cuGraphicsGLRegisterBuffer(cuda_heightVB_resource, heightVertexBuffer, CU_GRAPHICS_MAP_RESOURCE_FLAGS_WRITE_DISCARD);
		
		buffer = new int[1];
		size = meshSzSq*Sizeof.FLOAT*2;
		gl.glGenBuffers(1, IntBuffer.wrap(buffer));
		slopeVertexBuffer = buffer[0];
		gl.glBindBuffer(gl2BufArg, slopeVertexBuffer);
		gl.glBufferData(gl2BufArg, size, (Buffer) null, GL2.GL_DYNAMIC_DRAW);
		gl.glBindBuffer(gl2BufArg, 0);
		cuda_slopeVB_resource = new CUgraphicsResource();
		cuGraphicsGLRegisterBuffer(cuda_slopeVB_resource, slopeVertexBuffer, CU_GRAPHICS_MAP_RESOURCE_FLAGS_WRITE_DISCARD);

		buffer = new int[1];
		size = meshSzSq*Sizeof.FLOAT*4;
		gl.glGenBuffers(1, IntBuffer.wrap(buffer));
		posVertexBuffer = buffer[0];
		gl.glBindBuffer(gl2BufArg, posVertexBuffer);
		gl.glBufferData(gl2BufArg, size, (Buffer) null, GL2.GL_DYNAMIC_DRAW);
		gl.glBindBuffer(gl2BufArg, 0);
		gl.glBindBuffer(gl2BufArg, posVertexBuffer);
		ByteBuffer byteBuffer = gl.glMapBuffer(gl2BufArg, GL2.GL_WRITE_ONLY);
		if (byteBuffer != null){
			FloatBuffer put = byteBuffer.asFloatBuffer();
			int index = 0;
			float denom = (float) (meshSize - 1);
			for (int y = 0; y < meshSize; y++) {
				float v = y / denom;
				for (int x = 0; x < meshSize; x++) {
					float u = x / denom;
					put.put(index++, u * 2.0f - 1.0f);
					put.put(index++, 0.0f);
					put.put(index++, v * 2.0f - 1.0f);
					put.put(index++, 1.0f);
				}
			}
		}
		gl.glUnmapBuffer(gl2BufArg);
		gl.glBindBuffer(gl2BufArg, 0);

		size = vBufNumInts*Sizeof.INT;
		buffer = new int[1];
		gl.glGenBuffers(1, IntBuffer.wrap(buffer));
		indexBuffer = buffer[0];
		gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, indexBuffer);
		gl.glBufferData(GL2.GL_ELEMENT_ARRAY_BUFFER, size, (Buffer) null, GL2.GL_STATIC_DRAW);
		
		byteBuffer = gl.glMapBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, GL2.GL_WRITE_ONLY);
		if (byteBuffer != null){
			IntBuffer indices = byteBuffer.asIntBuffer();
			int index = 0;
			for(int y=0; y<meshSize-1; y++) {
		        for(int x=0; x<meshSize; x++) {
		        	indices.put(index, y*meshSize+x);
		        	indices.put(index+1, (y+1)*meshSize+x);
		        	index +=2;
		        }
		        indices.put(index, (y+1)*meshSize+(meshSize-1));
		        indices.put(index+1, (y+1)*meshSize);
		        index += 2;
		    }
		}
		gl.glUnmapBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER);
		gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, 0);
	}

	private void setUnisInShdr(){
		int uniHeightScale = gl.glGetUniformLocation(shaderProgramID, "heightScale");
		gl.glUniform1f(uniHeightScale, heightScale * (cudaFlags[performInvFFT] ? 1.0f : 20.0f));

		int uniChopiness = gl.glGetUniformLocation(shaderProgramID, "chopiness");
		gl.glUniform1f(uniChopiness, chopiness);

		int uniSize = gl.glGetUniformLocation(shaderProgramID, "size");
		gl.glUniform2f(uniSize, (float) meshSize, (float) meshSize);

		int uniDeepColor = gl.glGetUniformLocation(shaderProgramID, "deepColor");
		gl.glUniform4f(uniDeepColor, deepColor[0], deepColor[1], deepColor[2], deepColor[3]);

		int uniShallowColor = gl.glGetUniformLocation(shaderProgramID, "shallowColor");
		gl.glUniform4f(uniShallowColor, shallowColor[0], shallowColor[1], shallowColor[2], shallowColor[3]);

		int uniSkyColor = gl.glGetUniformLocation(shaderProgramID, "skyColor");
		gl.glUniform4f(uniSkyColor, skyColor[0], skyColor[1], skyColor[2], skyColor[3]);

		int uniLightDir = gl.glGetUniformLocation(shaderProgramID, "lightDir");
		gl.glUniform3f(uniLightDir, lightDir[0], lightDir[1], lightDir[2]);
	}
	
	@Override
	public void display(GLAutoDrawable drawable) {
		float delta = System.nanoTime();
		if(cudaFlags[newSimVals]){updateSimVals();}
		gl = drawable.getGL().getGL2();

		runCuda();

		gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
		
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glLoadIdentity();
		gl.glTranslatef(translateX, translateY, translateZ);
		gl.glRotatef(rotateX, 1.0f, 0.0f, 0.0f);
		gl.glRotatef(rotateY, 0.0f, 1.0f, 0.0f);

		gl.glBindBuffer(gl2BufArg, posVertexBuffer);
		gl.glVertexPointer(4, GL2.GL_FLOAT, 0, 0);
		gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);

		gl.glBindBuffer(gl2BufArg, heightVertexBuffer);
		gl.glClientActiveTexture(GL2.GL_TEXTURE0);
		gl.glTexCoordPointer(1, GL2.GL_FLOAT, 0, 0);
		gl.glEnableClientState(GL2.GL_TEXTURE_COORD_ARRAY);

		gl.glBindBuffer(gl2BufArg, slopeVertexBuffer);
		gl.glClientActiveTexture(GL2.GL_TEXTURE1);
		gl.glTexCoordPointer(2, GL2.GL_FLOAT, 0, 0);
		gl.glEnableClientState(GL2.GL_TEXTURE_COORD_ARRAY);

		gl.glUseProgram(shaderProgramID);
		setUnisInShdr();
		
		gl.glColor3f(1.0f, 1.0f, 1.0f);
		gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, indexBuffer);
		gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_FILL);
		gl.glDrawElements(GL2.GL_TRIANGLE_STRIP, vBufNumInts, GL2.GL_UNSIGNED_INT, 0);
		gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_FILL);
		gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, 0);
		
		gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
		gl.glClientActiveTexture(GL2.GL_TEXTURE0);
		gl.glDisableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
		gl.glClientActiveTexture(GL2.GL_TEXTURE1);
		gl.glDisableClientState(GL2.GL_TEXTURE_COORD_ARRAY);

		gl.glUseProgram(0);
		
		delta = System.nanoTime() - delta;
		animTime +=  delta/1000000000;
	}

	@Override
	public void dispose(GLAutoDrawable drawable) { }
	
	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
		gl = drawable.getGL().getGL2();
		GLU glu = GLU.createGLU(gl);
		gl.glViewport(0, 0, width, height);

		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();
		glu.gluPerspective(60.0, (double) width / (double) height, 0.1, 10.0);
	}

	//move changed values to local variables
	private void updateSimVals(){
		patchSize 		= tmpSimVals.get("patchSizeIDX");
		windSpeed	 	= tmpSimVals.get("windSpeedIDX");
		windDir 		= tmpSimVals.get("windDirIDX");
		dirDepend 		= tmpSimVals.get("dirDependIDX");
		heightScale 	= tmpSimVals.get("heightScaleIDX");
		freqMix 		= tmpSimVals.get("freqMixIDX");
		chopiness 		= tmpSimVals.get("chopinessIDX");
		//thresh	 		= tmpSimVals.get("threshIDX");
		cudaFlags[newSimVals] = false;
	}
	
	private void setTmpSimVals(){
		tmpSimVals.put("patchSizeIDX",patchSize );
		tmpSimVals.put("windSpeedIDX",windSpeed);
		tmpSimVals.put("windDirIDX",windDir);
		tmpSimVals.put("dirDependIDX",dirDepend);
		tmpSimVals.put("heightScaleIDX",heightScale);
		tmpSimVals.put("freqMixIDX",freqMix );
		tmpSimVals.put("chopinessIDX",chopiness );
		//tmpSimVals.put("threshIDX",thresh );
	}
	
	private void runCuda() {
		Pointer kernelParameters = null;
		//pa.outStr2Scr("run cuda : freqres1 : " + freqRes1.length);
		if(cudaFlags[newFreqVals]){
			cuMemcpyHtoD(d_freqInRPtr, Pointer.to(audFreqRealRes), audFreqRealRes.length*Sizeof.FLOAT);
			cuMemcpyHtoD(d_freqInCPtr, Pointer.to(audFreqImagRes), audFreqImagRes.length*Sizeof.FLOAT);
			cudaFlags[newFreqVals] = false;
			cudaFlags[freqValsSet] = true;
		}
		
		if(cudaFlags[freqValsSet]){
			int tblockX = 8;
			int tblockY = 8;
			int tgridX = meshSize/tblockX;
			int tgridY = tgridX;
	
			//build frequency data - meshSize ^2
			kernelParameters = Pointer.to(
					Pointer.to(d_freqPtr),
					Pointer.to(d_freqInRPtr),
					Pointer.to(d_freqInCPtr),
					Pointer.to(new int[] { freqsInLen }),
					Pointer.to(new int[] { spectrumW }),//meshSize }), 
					Pointer.to(new int[] { spectrumH }),//meshSize }),
					Pointer.to(new int[] { isNoteData }),
					//Pointer.to(new float[] { thresh }),
					Pointer.to(new float[] { animTime })					
					);
		
			cuLaunchKernel(kFuncs[win.getFreqKFunc()],//kFuncs[bldFreqIDX], 				//recalc phillips spectrum value for each time step
					tgridX, tgridY, 1, // Grid dimension
					tblockX, tblockY, 1, // Block dimension
					0, null, // Shared memory size and stream
					kernelParameters, null // Kernel- and extra parameters
			);
			cuCtxSynchronize();
	
			cudaFlags[freqValsSet] = false;
		}
		
		int blockX = 8;
		int blockY = 8;
		int gridX = meshSize/blockX;
		int gridY = gridX;
		
		//build phillips data and mix with frequency data
		kernelParameters = Pointer.to(
				Pointer.to(d_h0Ptr),
				Pointer.to(d_htPtr),
				Pointer.to(d_freqPtr),
				Pointer.to(new int[] { spectrumW }),
				Pointer.to(new int[] { meshSize }),
				Pointer.to(new int[] { meshSize }),
				Pointer.to(new float[] { animTime }),  
				Pointer.to(new float[] { freqMix }),   
				Pointer.to(new float[] { patchSize }));
		
		//convert back from frequency domain to spatial domain
		if(cudaFlags[performInvFFT]) {
			cuLaunchKernel(kFuncs[genSpecIDX], 				//recalc phillips spectrum value for each time step
					gridX, gridY, 1, // Grid dimension
					blockX, blockY, 1, // Block dimension
					0, null, // Shared memory size and stream
					kernelParameters, null // Kernel- and extra parameters
			);
			cuCtxSynchronize();
			JCufft.cufftExecC2C(fftPlan, d_htPtr, d_htPtr, JCufft.CUFFT_INVERSE);
		} else {
			cuLaunchKernel(kFuncs[genSpec2IDX], 				//recalc phillips spectrum value for each time step
					gridX, gridY, 1, // Grid dimension
					blockX, blockY, 1, // Block dimension
					0, null, // Shared memory size and stream
					kernelParameters, null // Kernel- and extra parameters
			);
			cuCtxSynchronize();
		}
		
		CUdeviceptr g_hptr = new CUdeviceptr();
		cuGraphicsMapResources(1, new CUgraphicsResource[]{cuda_heightVB_resource}, null);
		cuGraphicsResourceGetMappedPointer(g_hptr, new long[1], cuda_heightVB_resource);
		kernelParameters = Pointer.to(
				Pointer.to(g_hptr),
				Pointer.to(d_htPtr),
				Pointer.to(new int[] { meshSize }));

		cuLaunchKernel(kFuncs[updHMapIDX],  
				gridX, gridY, 1, // Grid dimension
				blockX, blockY, 1, // Block dimension
				0, null, // Shared memory size and stream
				kernelParameters, null // Kernel- and extra parameters
		);
		cuCtxSynchronize();
		cuGraphicsUnmapResources(1, new CUgraphicsResource[]{cuda_heightVB_resource}, null);
		
		CUdeviceptr g_sptr = new CUdeviceptr();
		cuGraphicsMapResources(1, new CUgraphicsResource[]{cuda_slopeVB_resource}, null);
		cuGraphicsResourceGetMappedPointer(g_sptr, new long[1], cuda_slopeVB_resource);
		kernelParameters = Pointer.to(
				Pointer.to(g_hptr),
				Pointer.to(g_sptr),
				Pointer.to(new int[] { meshSize }),
				Pointer.to(new int[] { meshSize }));

		cuLaunchKernel(kFuncs[calcSlopeIDX],  
				gridX, gridY, 1, // Grid dimension
				blockX, blockY, 1, // Block dimension
				0, null, // Shared memory size and stream
				kernelParameters, null // Kernel- and extra parameters
		);
		cuCtxSynchronize();
		cuGraphicsUnmapResources(1, new CUgraphicsResource[]{ cuda_slopeVB_resource}, null);
		//pa.outStr2Scr("done cuda");
	}
	
	private void initShaders(GL2 gl) {
		shaderProgramID = gl.glCreateProgram();
		attachShader(gl, GL2.GL_VERTEX_SHADER, win.vertexShaderSource);
		attachShader(gl, GL2.GL_FRAGMENT_SHADER, win.fragmentShaderSource);
		gl.glLinkProgram(shaderProgramID);
		
		int[] buffer = new int[1];
		gl.glGetProgramiv(shaderProgramID, GL2.GL_LINK_STATUS, IntBuffer.wrap(buffer));
		gl.glValidateProgram(shaderProgramID);
	}
	
	private int attachShader(GL2 gl, int type, String shaderSource){
		int shader = 0; 
		shader = gl.glCreateShader(type);
		gl.glShaderSource(shader, 1, new String[] { shaderSource }, null);
		gl.glCompileShader(shader);
		int[] buffer = new int[1];
		gl.glGetShaderiv(shader, GL2.GL_COMPILE_STATUS, IntBuffer.wrap(buffer));
		gl.glAttachShader(shaderProgramID, shader);
		gl.glDeleteShader(shader);
		return shader;
	}
	
	// Phillips spectrum
	// (Kx, Ky) - normalized wave vector
	// Vdir - wind angle in radians
	// V - wind speed
	// A - constant
	public double phillips(float Kx, float Ky, float Vdir, float V, float A, float dir_depend, float Lsq) {
		double k_squared = Kx*Kx + Ky*Ky;
		if (k_squared == 0.0f){	return 0.0f;}
		double kSqrt = Math.sqrt(k_squared),
		//normalized dot prod
		k_x = (Kx / kSqrt),
		k_y = (Ky / kSqrt),
		w_dot_k = (k_x*Math.cos(Vdir) + k_y*Math.sin(Vdir)),
		//L = V*V/g,		
		res = (A*Math.exp(-1/(k_squared*Lsq))/(k_squared * k_squared) * w_dot_k*w_dot_k);
		
		// filter out waves moving opposite to wind
		if (w_dot_k < 0.0f){	res *= dir_depend;	}
		return res;
	}
	
	//initial setup of ocean
	public float[] generate_h0(float[] h0) {
		float kMult = (pa.TWO_PI / patchSize);
		int nMshHalf = -meshSize/2; 
		float kx,ky,P,Er,Ei,h0_re,h0_im, L = (windSpeed*windSpeed/g), lsq = (L*L);
		//Min kx : -42.89321Max kx : 42.89321Min ky : -42.89321Max ky : 42.89321
		Random rnd = new Random();
		for (int y = 0; y <= meshSize; y++) {
			ky = (nMshHalf + y) * kMult;
			for (int x = 0; x <= meshSize; x++) {
				kx = (nMshHalf + x) * kMult;
				if (kx == 0.0f && ky == 0.0f){	P = 0.0f;}
				else {P = (float) (Math.sqrt(phillips(kx, ky, windDir, windSpeed, wScl, dirDepend,lsq)));	}
				Er = (float) rnd.nextGaussian();
				Ei = (float) rnd.nextGaussian();
				h0_re = (Er*P * pa.SQRT2);
				h0_im = (Ei*P * pa.SQRT2);

				int i2 = 2* (y * spectrumW + x);
				h0[i2] = h0_re;
				h0[i2+1] = h0_im;
			}
		}
		return h0;
	}
	
	class MouseControl implements MouseMotionListener, MouseWheelListener {
		public Point previousMousePosition = new Point();

		@Override
		public void mouseDragged(MouseEvent e) {
			int dx = e.getX() - previousMousePosition.x;
			int dy = e.getY() - previousMousePosition.y;

			if ((e.getModifiersEx() & MouseEvent.BUTTON3_DOWN_MASK) == MouseEvent.BUTTON3_DOWN_MASK) {
				translateX += dx / 100.0f;
				translateY -= dy / 100.0f;
			} else if ((e.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) == MouseEvent.BUTTON1_DOWN_MASK) {
				rotateX += dy;
				rotateY += dx;
			}
			previousMousePosition = e.getPoint();
		}

		@Override
		public void mouseMoved(MouseEvent e) {
			previousMousePosition = e.getPoint();
		}

		@Override
		public void mouseWheelMoved(MouseWheelEvent e) {
			translateZ -= e.getWheelRotation() * 0.25f;
			previousMousePosition = e.getPoint();
		}
	}
	//compiles Ptx file from file in passed file name -> cuFileName needs to have format "xxxxx.cu"
	public void compilePtxFile(String krnFileName, String ptxFileName) throws IOException {
		File cuFile = new File(krnFileName);
		if (!cuFile.exists()) {
			throw new IOException("Kernel file not found: " + krnFileName);
		}
		String modelString = "-m" + System.getProperty("sun.arch.data.model");
		//build compilation command
		String command = "nvcc " + modelString + " -ptx " + cuFile.getPath() + " -o " + ptxFileName;
		//execute compilation
		pa.outStr2Scr("Executing\n" + command);
		Process process = Runtime.getRuntime().exec(command);

		String errorMessage = new String(toByteArray(process.getErrorStream())), outputMessage = new String(toByteArray(process.getInputStream()));
		int exitValue = 0;
		try {exitValue = process.waitFor();} 
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException("Interrupted while waiting for nvcc output", e);
		}

		if (exitValue != 0) {
			pa.outStr2Scr("nvcc process error : exitValue : " + exitValue);
			pa.outStr2Scr("errorMessage :\n" + errorMessage);
			pa.outStr2Scr("outputMessage :\n" + outputMessage);
			throw new IOException("Could not create .ptx file: " + errorMessage);
		}
		pa.outStr2Scr("Finished compiling PTX file : "+ ptxFileName);
	}

	public byte[] toByteArray(InputStream inputStream) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte buffer[] = new byte[8192];
		while (true) {
			int read = inputStream.read(buffer);
			if (read == -1) {break;	}
			baos.write(buffer, 0, read);
		}
		return baos.toByteArray();
	}
	
}//class myOcean
