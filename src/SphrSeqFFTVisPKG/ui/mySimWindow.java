package SphrSeqFFTVisPKG.ui;

import java.util.*;

import javax.swing.*;

import SphrSeqFFTVisPKG.SeqVisFFTOcean;
import SphrSeqFFTVisPKG.myOcean;
import SphrSeqFFTVisPKG.musicPlayer.myMP3SongHandler;
import SphrSeqFFTVisPKG.note.myNote;
import SphrSeqFFTVisPKG.note.enums.durType;
import SphrSeqFFTVisPKG.staff.myKeySig;
import SphrSeqFFTVisPKG.ui.base.myMusicSimWindow;
import base_Math_Objects.vectorObjs.doubles.myPoint;
import base_Math_Objects.vectorObjs.doubles.myVector;
import ddf.minim.analysis.*;
import processing.core.PConstants;
import ddf.minim.*;

public class mySimWindow extends myMusicSimWindow {
	
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
		//float stY = rectDim[1]+rectDim[3]-4*yOff,stYFlags = stY + 2*yOff;	
		//build fftocean window only when window actually shown
		//launchOceanWin();
		super.initThisWin(_canDrawTraj, false);
		setSongsAndFFT();
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
	
	//init any extra ui objs
	@Override
	protected void initXtraUIObjsIndiv() {}


	@Override
	protected void initMe_Indiv() {
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
			if(val){launchOceanVis(getPrivFlags(useAudioForOceanIDX));	}
			else {	killOceanVis(getPrivFlags(useAudioForOceanIDX));	}
			break;
		}
		case showFreqDomainIDX 		: {
			fftOcean.setFlags(myOcean.performInvFFT, !val);
			break;}
		case useAudioForOceanIDX 	: {
			if(getPrivFlags(playVisIDX)){
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
		if((!privFlags[oceanMadeIDX]) || (null == fftOcean) || (!fftOcean.getFlags(myOcean.doneInit))){return;}
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
			if((!privFlags[oceanMadeIDX]) || (null == fftOcean) || (!fftOcean.getFlags(myOcean.doneInit))){return;}
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
		//final GLCapabilities capabilities = new GLCapabilities(GLProfile.get(GLProfile.GL2));
		final mySimWindow thisWin = this;
		//SwingUtilities.invokeLater(new Runnable() {	public void run() {fftOcean = new myOcean(pa, thisWin, capabilities);}});//launch in another thread	
		SwingUtilities.invokeLater(new Runnable() {	public void run() {fftOcean = new myOcean(pa, thisWin);}});//launch in another thread	
		//test
		//PShader myShader = new PShader(pa, new String[] {vertexShaderSource}, new String[] {fragmentShaderSource});

		setPrivFlags(oceanMadeIDX, true);
		setPrivFlags(playVisIDX, true);
	}

	public int getFreqKFunc() {		return perSongBuildKFuncs[songIDX];	}
	
	public void setOceanFreqVals(){
		if((!privFlags[oceanMadeIDX]) || (null == fftOcean) || (!fftOcean.getFlags(myOcean.doneInit))){return;}
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
		//float[] bandRes = songs[songIDX].fftFwdBandsFromAudio();
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
	protected void setupGUIObjsAras(TreeMap<Integer, Object[]> tmpUIObjArray, TreeMap<Integer, String[]> tmpListObjVals){
		//pa.outStr2Scr("setupGUIObjsAras in :"+ name);
		guiMinMaxModVals = new double [][]{  
			{0.0, songTitles.length-1, 1.0},	//song selected
			{0.0, windowNames.length-1, 1.0},	//window function selected
			{25.0, 125.0, 0.1},			//patchSizeIDX 	
			{0.0, 100.0, 0.1},			//windSpeedIDX 
			{0.0, PConstants.TWO_PI, 0.01},		//windDirIDX  	
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
		guiObjs[songTransIDX] = new myGUIProgressBar(pa, this, songTransIDX, "MP3 Transport for ", 
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
	public String getUIListValStr(int UIidx, int validx) {
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
		if((!privFlags[oceanMadeIDX]) || (null == fftOcean) || (!fftOcean.getFlags(myOcean.doneInit))){return;}
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

	/**
	 * Whether or not to force the cuda kernel to be recompiled
	 * @return
	 */
	public boolean forceCUDARecomp() {return 
	
	@Override
	protected void setScoreInstrValsIndiv(){}
	@Override
	protected void snapMouseLocs(int oldMouseX, int oldMouseY, int[] newMouseLoc) {}
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
			//fftOcean.frame.dispatchEvent(new WindowEvent(fftOcean.frame, WindowEvent.WINDOW_CLOSING));	
			fftOcean.closeMe();
			//delOcean() called in closed event handler for frame
			fftOcean = null;
		}
	}//closeMe

}//class mySimWIndow

