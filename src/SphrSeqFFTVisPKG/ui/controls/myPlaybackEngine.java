package SphrSeqFFTVisPKG.ui.controls;

import SphrSeqFFTVisPKG.note.enums.durType;
import SphrSeqFFTVisPKG.ui.base.myMusicSimWindow;
import base_JavaProjTools_IRender.base_Render_Interface.IRenderInterface;
import base_Math_Objects.MyMathUtils;

/**
 * handles all transport control - accessible by all classes who need it. 
 * provides a measure of current playback location from start of song.  window needs 
 * to manage this - this should provide time in ticks from beginning of song, window 
 * should translate to pixels per update displacement
 * @author 7strb
 *
 */
public class myPlaybackEngine {
	public IRenderInterface pa;
	public static int pbCnt = 0;
	public int ID;
	
	public myMusicSimWindow win;					//owning display window
	
	public int[] fillClr, strkClr;
	//current dims of reticle
	public float[] dims;						//y start and end
	public static float sWt = 1.0f;
	public final float pbNorm = .1f;
	
	//universal for all play back engines
	private float tempo;							//set the tempo
	private int ticksPerBeat;					// # of ticks to transition in 1 beat
	private float curTime;						//current playback time from beginning of sequence (never less than 0) - in ticks
	private float stTime;						//most recent start time - where playing will start (in ticks)
	private float timeMod; 						//how many ticks to advance playback in 1 update
	private float stLoopTime, endLoopTime;		//user programmable loop for playing	
	
	//each window/screen's play back engine has its own of the following, for display of reticle
	private int pxlsPerBeat; 					// # of pixels to transition in 1 beat (qtr note)
	private float curPxls;						//current playback location of reticle, from edge of screen (for drawing)
	private float stPixels;						//most recent start pixel location - where playing will start	
	private float pxlMod;						//how many pixels to move the reticle in 1 update	
	private float stLoopPxls, endLoopPxls;		//user programmable loop for playing - in pixels from start of sequence 
	
	public boolean[] pbeFlags;					//state machine flags for this obj
	public static final int 
		isPlayingIDX = 0,						//whether or not this is playing
		loopSetIDX = 1,							//whether or not this obj has had loop endpoints set
		isLoopingIDX = 2,						//whether or not this object is looping
		playMtrnmIDX = 3;						//whether or not to play metronome click
	
	public static final int numPFlags = 4;

	public myPlaybackEngine(IRenderInterface _p, myMusicSimWindow _win, int[] fc, int[] sc, float[] _dims){
		pa = _p;
		win = _win;
		ID = pbCnt++;
		fillClr = new int[4];strkClr = new int[4];
		dims = new float[_dims.length];
		for(int i=0;i<4;++i){fillClr[i]=fc[i];strkClr[i]=sc[i];}
		for(int i=0;i<_dims.length;++i){ dims[i]=_dims[i];}
		curTime = 0; stTime = 0; 
		stLoopTime = -1; endLoopTime = -1;
		updateTimSigTempo(120);				//some default values
		ticksPerBeat = durType.Quarter.getVal();
		pxlsPerBeat = 13;
		curPxls = 0; stPixels = 0; 
		stLoopPxls =-1; endLoopPxls = -1;
		initPBEFlags();
	}
	
	public void initPBEFlags(){	pbeFlags = new boolean[numPFlags];for(int i=0;i<pbeFlags.length;++i){pbeFlags[i]=false;}}
	
	//tempo is bpm, frate is updates/second, need to manage ticks per update - need to know beat species (quarter, eighth, etc) - call whenever time sig or tempo is changed
	public void updateTimSigTempo(float _newTempo){
		tempo = _newTempo;
		updatePBESpeed();
	}
	public void setPixelsPerBeat(int _pxlsPerBeat){	//change only when resolution of qtr note changes due to zoom TODO
		pxlsPerBeat = _pxlsPerBeat;	
	}
	
	private void updatePBESpeed(){
		//float updPerMin = pa.frameRate * 60;								//frames per second * 60 = frames per minute 
		timeMod = (tempo * ticksPerBeat)/60.0f;///updPerMin ;						//beats per minute * ticks per beat == ticks per minute / 60 == ticks per second.  use millis calls to make sure appropriate advance since last call				
		pxlMod = (tempo * pxlsPerBeat)/60.0f;///updPerMin ;						//beats per minute * ticks per beat == ticks per minute / 60 ==> pxls to advance second			
		//timeMod is how fast the playback advances every draw - calculate this so that the appropriate bpm is achieved 
		//pa.outStr2Scr("updatePBESpeed : ID:" + ID+" Setting PBEs tempo to : " + tempo + " Ticks per second = " + ticksPerBeat+ " Calced timeMod (ticks per second) = "+String.format("%.4f", timeMod)+ " Calced timeMod (pxlsPerBeat) = "+ pxlsPerBeat+ " Calced pxlMod (pxls per second) = "+String.format("%.4f", pxlMod),true);				
	}
	
	//set dimensions - passed values are start and end of window showing this playback cursor
	public void setDims(float[] winDims){System.arraycopy(winDims, 0, dims, 0, dims.length);}	
	
	//any time handling required before passing time (in ticks) to windows
	public float getCurrentTime(){return curTime;}
	//modify current time and current pxls by passed mod (in beats)
	public void modCurTime(float modBeat){
		curTime += modBeat*ticksPerBeat;
		curPxls += modBeat*pxlsPerBeat;
	}
	
	//this playback engine's current pxl displacement
	public float getCurrentPxlTime(){return curPxls;}
	//move the display reticle by a certain amount without modifying the time - to be used with playback on score, to jump measure boundaries
	public void modCurPxlTime(int pxlOffset){curPxls += pxlOffset;}
	//set display reticle location without modifying time
	public void setCurPxlTime(int _pxlVal){curPxls = _pxlVal;}
	
	
	//stop playing and move back to the st loc
	public void stop(){
		curTime = (pbeFlags[isLoopingIDX] && pbeFlags[loopSetIDX] ? MyMathUtils.max(stTime,stLoopTime) : stTime);
		curPxls = (pbeFlags[isLoopingIDX] && pbeFlags[loopSetIDX] ? MyMathUtils.max(stPixels,stLoopPxls) : stPixels);
		pbeFlags[isPlayingIDX] = false; pbeFlags[isLoopingIDX] = false;		
	}
	
	//set start time in ticks
	public void setStartTime(float _st){
		//determine start time in pixels by converting start time in ticks to pxls
		stLoopPxls = convTicksToPxls(_st);
		stTime = _st;
		stop();
		curTime = _st;	
		curPxls = stLoopPxls;
	}
	
	//convert some span of pixels to an equivalent span of ticks
	public float convPxlsToTicks(float _pxls){return (ticksPerBeat/(1.0f*pxlsPerBeat)) * _pxls;}
	public float convTicksToPxls(float _ticks){return (pxlsPerBeat/(1.0f*ticksPerBeat)) * _ticks;}
	
	public void setLooping(float _lstTime, float _lendTime, int _lpStPxls, int _lpEndPxls){
		stLoopTime = _lstTime; endLoopTime = _lendTime;
		stLoopPxls =_lpStPxls; endLoopPxls = _lpEndPxls;
		pbeFlags[loopSetIDX] = (stLoopTime != -1)&&(endLoopTime != -1);		
	}
	
	public void clearLooping(){
		stLoopTime = -1; endLoopTime = -1;
		stLoopPxls =-1; endLoopPxls = -1;
		pbeFlags[loopSetIDX] = false;
	}
	
	//moves play cursor by 1 step, based on current tempo
	public void play(float modAmtSec){ //# of milliseconds since last update
		curTime += timeMod * modAmtSec;
		curPxls += pxlMod * modAmtSec;
		if((pbeFlags[isLoopingIDX]) && (curTime > endLoopTime)){curTime = stLoopTime; curPxls = stLoopPxls;}
	}
	
	//use this to transform time values for display
	private String transTime(float timeVal){return String.format("$.2f", timeVal);}	
	public String getPBEVals(){
		String res = "Current Time : " + curTime  + " Start Time : " +  stTime + " timeMod : " + timeMod + " pxlMod : " + pxlMod;
		if(pbeFlags[loopSetIDX]){res += " | Start Loop Time : " + transTime(stLoopTime)+" | End Loop Time : "+transTime(endLoopTime);}
		return res;
	}
	
	//draw reticle line at current time - make sure to translate this to start of playback region (measure boundary or piano roll) before starting
	//window should control this - needs to be passing over currently playing note always, in piano roll or in staff
	//offset is how much to slide over based on if window has discontinuity (for measure with time sig change, for example) 
	public void drawMe(){
		pa.pushMatState();
			pa.translate(curPxls, 0);
			pa.setStrokeWt(sWt);
			pa.setColorValStroke(IRenderInterface.gui_Black, 255);
			pa.drawLine(-1, dims[0], 0, -1, dims[1], 0);
			pa.drawLine(1, dims[0], 0, 1, dims[1], 0);
			pa.setStroke(strkClr,255);
			pa.drawLine(0, dims[0], 0, 0, dims[1], 0);	
			pa.setColorValFill(IRenderInterface.gui_Black, 255);
			pa.showText(getPBEVals(), 10, 30);
		pa.popMatState();
	}

}//myPlaybackEngine
