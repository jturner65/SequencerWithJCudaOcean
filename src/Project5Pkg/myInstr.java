package Project5Pkg;
import ddf.minim.ugens.*;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PImage;
import processing.core.PShape;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import ddf.minim.AudioOutput;
import ddf.minim.AudioRecorder;
import ddf.minim.Minim;


//a class to hold a kind of instrument, determined by the harmonic series it reproduces
//represented on the screen as a sphere. 
//the pitch the instrument plays is determined by the distance from the center a note arc is placed
//the duration is based on how long the arc is around the sphere
//the speed/tempo is determined by the speed setting, which governs how long a single revolution around the circular profile of the sphere takes.
public class myInstr {
	public CAProject5 p;
	public static int instCnt = 0;
	public int ID;
	public String instrName;
	
	public myStaff staff;		//the staff for this instrument
	
	public myClefBase clef;		//clef for this instrument - for staff representation
	public String clefString;
	
	public ArrayDeque<myNoteChannel> chansAvail;					//use deque to always get last channel that was playing
	public TreeMap<Integer, myNoteChannel> chansPlaying;			//use treemap to get playing channel by note id it is playing, to release it
	public int numNotesPlaying;
	public myNoteChannel[] noteChans;
	public static final int numNoteChans = 36;	//16 voice polyphony
	public float[] oscilFracts;					//fractions for each component harmonic
	
	public Summer sumMstr;						//output that is patched to from all note channels and in return patches to out
//	ADSR(float maxAmp, float attTime, float decTime, float susLvl, float relTime)
//	adsr = new ADSR( 0.5, 0.01, 0.05, 0.5, 0.5 );	
//	public float[] adsrMults;		//5 component multipliers for adsr : max amplitude, aTime, dTime, suslevel, rTime

	public boolean[] instFlags;
	public static final int 
		outSetIDX = 0,
		mstOutPtchIDX = 1,
		isDrumTrackIDX = 2,				//whether this instrment is a drum kit
		isInvDrumTrackIDX = 3;			//whether this instrument is a drum kit with an inverted mapping (to give more detail to cymbals)
	public static final int numInstFlags = 3;
	
	
	public static final int
		volCntlIDX = 0,
		panCntlIDX = 1,
		waveCntlIDX = 2;
	public static final int numCntls = 3;
	
	public float panAmt;					//-1 to +1 l to r
	public float volAmt;
	
//	public AudioOutput out;
	public Wavetable wTbl;					//waveform for this instrument
	//TODO build instruments using minim
	public myInstr(CAProject5 _p, String _nm, myClefBase _clef, float[] _waveAmpMult, Wavetable _wTbl, boolean isDrums) {
		p = _p;
		ID = instCnt++;
		instrName = _nm;
		wTbl = _wTbl;
		initFlags();
		instFlags[isDrumTrackIDX] = isDrums;
		if(_clef.isGrandStaff){	clef = new myGrandClef(_clef);	}
		else {			clef = new myClef(_clef);		}
//		out = p.minim.getLineOut(p.OutTyp,p.glbBufrSize);	
//		out.setTempo(120);
		sumMstr = new Summer();
		sumMstr.setSampleRate(44100.0f);
		noteChans = new myNoteChannel[numNoteChans];
		oscilFracts = new float[_waveAmpMult.length];
		for(int i =0; i<_waveAmpMult.length; ++i){oscilFracts[i]=_waveAmpMult[i];}		//same oscil mults for all notes in this instrument
		for(int i =0; i<noteChans.length; ++i){	noteChans[i] = new myNoteChannel(p,this,oscilFracts, wTbl);	}
		initChanDeques();
//		adsrMults = new float[]{1.0f, 0.01f, 0.05f, 0.5f, 0.5f};//5 component multipliers for adsr : max amplitude, aTime, dTime, suslevel, rTime
//		adsr = new ADSR( adsrMults[0], adsrMults[1], adsrMults[2], adsrMults[3], adsrMults[4]);
//		adsr.setSampleRate(88200.0f);
		if(instFlags[isDrumTrackIDX]){
			for(int i =0; i<p.drumSounds.length;++i){	p.drumSounds[i].patch(sumMstr);	}
//		/	out.setTempo(60.0f);
		}
		
		if(instFlags[isDrumTrackIDX]){
//			sumMstr.patch(adsr);			//never unpatch sumMstr from adsr
		}
		clefString = this.clef.toString();
		panAmt = 0;
		volAmt = 100;	
		patchMstrOut();

	}	
	
	public void patchMstrOut(){
		//p.outStr2Scr("instr ID : " + ID + " Attempt to patch to output : flag : " + instFlags[mstOutPtchIDX] );		
		if(instFlags[mstOutPtchIDX]){return;}//already patched
		//out.pauseNotes();
		if(instFlags[isDrumTrackIDX]){
//			if(out!=null){out.close();}
//			out = p.minim.getLineOut(p.OutTyp,p.glbBufrSize);	
//			out.setTempo(120);
//			adsr.noteOn();
//			adsr.patch(p.glblSum);
			sumMstr.patch(p.glblSum);
		}
		else {
			sumMstr.patch(p.glblSum);
		}
		//adsr.patch(p.glblSum);
		//out.resumeNotes();
		instFlags[mstOutPtchIDX] = true;
	}
	public void unPatchMstrOut(){
		//p.outStr2Scr("instr ID : " + ID + " Attempt to unpatch output : flag : " + instFlags[mstOutPtchIDX]);
		if(!instFlags[mstOutPtchIDX]){return;}//already unpatched
		if(instFlags[isDrumTrackIDX]){
//			adsr.noteOff();
//			adsr.unpatchAfterRelease(p.glblSum);
			sumMstr.unpatch(p.glblSum);
//			p.glblOut.close();
//			p.resetAudioOut();
			//adsr.unpatchAfterRelease(p.glblSum);
		} else {
			sumMstr.unpatch(p.glblSum);
		}
		instFlags[mstOutPtchIDX] = false;	
	}

	protected void initChanDeques(){
		chansAvail = new ArrayDeque<myNoteChannel>();
		chansPlaying = new TreeMap<Integer,myNoteChannel>();
		for(int i =0; i<noteChans.length; ++i){	chansAvail.push(noteChans[i]);}		
		numNotesPlaying = 0;
	}
		
	public void initFlags(){instFlags = new boolean[numInstFlags]; for(int i=0;i<numInstFlags;++i){instFlags[i]=false;}	}	
	
//	//set note channel control values from UI
	public void setInstCntlVals(int idx, float val, Wavetable wf){
		switch (idx){
			case volCntlIDX 	: {setVolume(val/100.0f);return;}
			case panCntlIDX 	: {setPanAmt(val);return;}
			case waveCntlIDX	: {if(wf==null){p.outStr2Scr("setInstCntlVals : Null wf sent to instrument: " +ID +":"+this.instrName); return;}setWaveForm(wf);return;}
			default 			: {return;}
		}		
	}//setInstCntlVals
	
	protected void setVolume(float _volAmt){volAmt = _volAmt;for(int i = 0; i< noteChans.length; ++i){noteChans[i].setVolume(volAmt);}}
	protected void setPanAmt(float _panAmt){panAmt = _panAmt;for(int i =0; i<noteChans.length; ++i){noteChans[i].setPan(panAmt);}}	
	protected void setWaveForm(Wavetable wf){wTbl = wf;	for(int i =0; i<noteChans.length; ++i){noteChans[i].setWaveForm(wTbl);}}
	
	public myNoteChannel getAvailNtChan(int nID){
		if(chansAvail.size() == 0){return null;}
		myNoteChannel nc = chansAvail.removeFirst(); 
		if(null == nc){//p.outStr2Scr("instr ID : " + ID + " ERROR : No more channels available to play note!");		
			return null;}
		//p.outStr2Scr("instr ID : " + ID + " getAvailNtChan NT Channel retreived :\n" + nc.toString() + "\nfor note ID : " + nID); 
		chansPlaying.put(nID,nc);
		return nc;}
	
	public myNoteChannel relPlayingNtChan(int nID){
		if(chansPlaying.size() == 0){return null;}
		myNoteChannel nc = chansPlaying.get(nID); 
		if(null == nc){//p.outStr2Scr("instr ID : " + ID + " ERROR : Specified note id " + nID + " not playing!");	
			return null;} 
		//p.outStr2Scr("instr ID : " + ID + " relPlaying NT Channel release :\n" + nc.toString()); 
		chansAvail.addLast(nc); 
		return nc;}

	//ret 1 if success, -1 if fail, 0 if no notes
	public int playSingleSphNote(myNote note){
		if(null==note){		//p.outStr2Scr("instr ID : " + ID + " play single note : null");
			return -1;}
		//p.outStr2Scr("instr ID : " + ID + " play single note : "+ note.toString());
		if(this.instFlags[myInstr.isDrumTrackIDX]){return startSphDrumNote(note);}
		if(note.n.name == nValType.rest){return 0;}		//don't add or remove rests
		if(!instFlags[mstOutPtchIDX]){patchMstrOut();}
		myNoteChannel _nc = getAvailNtChan(note.ID);		
		if(_nc == null){return -1;}
		_nc.setCurNoteAndPlay(note);
		//p.outStr2Scr("Playing Note instID : " + ID + " note channel : " + _nc.ID);
		numNotesPlaying++;
		return 1;		
	}
	//ret 1 if success, -1 if fail, 0 if no notes
	public int stopSingleSphNote(myNote note){
		if(null==note){	//p.outStr2Scr("instr ID : " + ID + " stop single note : null");
			return -1;}
		if(this.instFlags[myInstr.isDrumTrackIDX]){return stopSphDrumNote(note);}
		//p.outStr2Scr("instr ID : " + ID + " stop single note : "+ note.toString());
		if(note.n.name == nValType.rest){return 0;}		//don't add or remove rests
		myNoteChannel _nc = relPlayingNtChan(note.ID);
		if(_nc == null){return -1;}
		_nc.clearCurNoteAndStop(note);
		numNotesPlaying--;
		//for every note in stop notes map	
		if(numNotesPlaying <= 0){this.unPatchMstrOut(); numNotesPlaying =0;}	//may clip off note ends		
		return 2;
	}	
	
	public int startSphDrumNote(myNote note){//note ring / 4 is drum sound to play (approx)
		int idx = (36 - note.sphereRing-1)/4;			//TODO
		numNotesPlaying++;
		//int idx = (note.sphereRing/4)-1;			
		//p.outStr2Scr("Play Drum Note : ring :  (36 - 1 -" + note.sphereRing + ")/4 idx :  " + idx);
		p.drumSounds[(idx % p.drumSounds.length)].trigger();
		//p.outStr2Scr("Play Drum Note : ring :  " + note.sphereRing + "/4 idx :  " + idx);
		
		return 1;
	}
	
	public int startDrumNote(myNote note){//note ring / 4 is drum sound to play (approx)
		int idx = note.n.octave;			//just use note octave for drum
		numNotesPlaying++;
		//int idx = (note.sphereRing/4)-1;			//TODO
		//p.outStr2Scr("Play Drum Note : ring :  (36 - 1 -" + note.sphereRing + ")/4 idx :  " + idx);
		p.drumSounds[(idx % p.drumSounds.length)].trigger();
		//p.outStr2Scr("Play Drum Note : ring :  " + note.sphereRing + "/4 idx :  " + idx);
		
		return 1;
	}	
	public int stopSphDrumNote(myNote note){
		numNotesPlaying--;
		return 1;
	}
	public int stopDrumNote(myNote note){
		numNotesPlaying--;
		return 1;
	}
	
	//int stTime : start time when notes are received - play notes at note time - stTime (for offsets between polling of note collection)
	public int addSphNotesToPlay(SortedMap<Integer, myNote> tmpNotes, int stTime){
		//for every note this instrument wants to play, get next available noteChannel, give it the note, turn it on		
		if((null == tmpNotes) || (tmpNotes.size() == 0)){return 0;}				//no notes passed
		myNote _n;
		myNoteChannel _nc;
		if(!instFlags[mstOutPtchIDX]){patchMstrOut();}
		for(SortedMap.Entry<Integer, myNote> note : tmpNotes.entrySet()) {
			int retCode = playSingleSphNote(note.getValue());
			if(retCode == 0){continue;}
			if(retCode == -1){p.outStr2Scr("instr ID : " + ID + " bad note channel");return -1;}
			if(retCode == 2){}//drum note
//			if(note.getValue().n.name == nValType.rest){continue;}			//don't add or remove rests
//			_n = note.getValue();							//start time doesn't matter - sequencer handles it
//			_nc = getAvailNtChan(_n.ID);		
//			if(_nc == null){return -1;}
//			_nc.setCurNoteAndPlay(_n);
//			numNotesPlaying++;
		}
		return 1;
	}//addTrajNoteToPlay
	//returns -1 if any failed
	public int addSphNotesToStop(SortedMap<Integer,ArrayList<myNote>> tmpNotes){
		//for every note passed in, find the note channel that is playing it, stop it playing, release the note channel from the playing deque, and set that channel's note to null
		if((null == tmpNotes) || (tmpNotes.size() == 0)){return 0;}				//no notes passed
		int retCodeSt = 1, tmpRetCode;
		myNote _n;
		myNoteChannel _nc;
		for(SortedMap.Entry<Integer, ArrayList<myNote>> noteAra : tmpNotes.entrySet()) { 
			if(noteAra==null){continue;}
			ArrayList<myNote> nAra = noteAra.getValue();
			if((nAra==null) || nAra.size() == 0){continue;}
			for(int i =0;i<nAra.size();++i){
				myNote note = nAra.get(i);
				int retCode = stopSingleSphNote(note);
				if(retCode == 0){continue;}
				if(retCode == -1){p.outStr2Scr("instr ID : " + ID + " bad note channel");return -1;}
				if(retCode == 2){}//drum note
				
//				if(note.n.name == nValType.rest){continue;}		//don't add or remove rests				
//				_nc = relPlayingNtChan(note.ID);
//				if(_nc == null){retCode = -1; continue;}
//				_nc.clearCurNoteAndStop(note);
//				numNotesPlaying--;
			}
		}//for every note in stop notes map	
		if(numNotesPlaying <= 0){this.unPatchMstrOut();numNotesPlaying =0;}
		return retCodeSt;
	}//addNoteToStop
	
	
	//ret 1 if success, -1 if fail, 0 if no notes
	public int playSingleNote(myNote note){
		if(null==note){		//p.outStr2Scr("instr ID : " + ID + " play single note : null");
			return -1;}
		//p.outStr2Scr("instr ID : " + ID + " play single note : "+ note.toString());
		if(this.instFlags[myInstr.isDrumTrackIDX]){return startDrumNote(note);}
		if(note.n.name == nValType.rest){return 0;}		//don't add or remove rests
		if(!instFlags[mstOutPtchIDX]){patchMstrOut();}
		myNoteChannel _nc = getAvailNtChan(note.ID);		
		if(_nc == null){return -1;}
		_nc.setCurNoteAndPlay(note);
		p.outStr2Scr("Playing Note instID : " + ID + " note channel : " + _nc.ID);
		numNotesPlaying++;
		return 1;		
	}
	//ret 1 if success, -1 if fail, 0 if no notes
	public int stopSingleNote(myNote note){
		if(null==note){	//p.outStr2Scr("instr ID : " + ID + " stop single note : null");
			return -1;}
		if(this.instFlags[myInstr.isDrumTrackIDX]){return stopDrumNote(note);}
		//p.outStr2Scr("instr ID : " + ID + " stop single note : "+ note.toString());
		if(note.n.name == nValType.rest){return 0;}		//don't add or remove rests
		myNoteChannel _nc = relPlayingNtChan(note.ID);
		if(_nc == null){return -1;}
		_nc.clearCurNoteAndStop(note);
		numNotesPlaying--;
		//for every note in stop notes map	
		if(numNotesPlaying <= 0){this.unPatchMstrOut(); numNotesPlaying =0;}	//may clip off note ends		
		return 2;
	}	

	//set fractions for harmonics for this instrument
//	public void setHarmFract(int idx, float val){
//		float valToSet = p.min(p.max(val,0),1);
//		for(int i=0;i<noteChans.length;++i){noteChans[i].setHarmFract(idx,val);}		
//	}

	
	public String toString(){
		String res = "Instrument : "+ instrName +" | "+ clefString;
		return res;		
	}

}//class myInstr


////a set of oscil components, to represent 1 note channel at a time - # of these represents size of polyphony for instrument
class myNoteChannel {
	public CAProject5 p;
	public static int iNCCnt = 0;
	public int ID;
		
	public myInstr own;							//owning instrument
//	ADSR envelope with maximum amplitude, attack Time, decay time, sustain level,
//	 and release time.  Amplitude before and after the envelope is set to 0.
//	ADSR(float maxAmp, float attTime, float decTime, float susLvl, float relTime)
//	adsr = new ADSR( 0.5, 0.01, 0.05, 0.5, 0.5 );	
//	public ADSR  adsr;				//adsr envelope for this instrument
//	public float[] adsrMults;		//5 component multipliers for adsr : max amplitude, aTime, dTime, suslevel, rTime
	
	public myInstrCmp[] cmps;					//component wave forms of this instrument
	public float[] cmpFracts;					//multipliers for each component harmonic - 1 per intsrcmp/oscil
	
	public float panAmt;					//-1 to +1 l to r
	public float volAmt;
	public Summer sum;
	public  Wavetable wTbl;
	public myNoteChannel(CAProject5 _p, myInstr _own, float[] _waveAmpMult, Wavetable _wTbl){
		p=_p;
		ID = iNCCnt++;
		own=_own;
		sum = new Summer();
		sum.setSampleRate(44100.0f);
		panAmt = 0;
		wTbl = _wTbl;
//		adsrMults = new float[]{1.0f, 0.01f, 0.05f, 0.5f, 0.1f};//5 component multipliers for adsr : max amplitude, aTime, dTime, suslevel, rTime
//		adsr = new ADSR( adsrMults[0], adsrMults[1], adsrMults[2], adsrMults[3], adsrMults[4]);
		cmps = new myInstrCmp[_waveAmpMult.length];
		cmpFracts = new float[_waveAmpMult.length];
		for(int i = 0; i< _waveAmpMult.length; ++i){
			cmpFracts[i]=_waveAmpMult[i];
			cmps[i] = new myInstrCmp(p,this, cmpFracts[i], wTbl); 
			cmps[i].osc.patch(sum);
//			/p.outStr2Scr("in myNoteChannelctor : " + ID + " cmp : " + i + " cmp : " + cmps[i].toString());
		}//build oscills and patch into sum
//		sum.patch(adsr);
//		adsr.patch(own.sumMstr);
		volAmt = 0.5f;
		panAmt = 0.0f;
	}
	//resetAllVals()
	public void setVals(float[][] vals){for(int i = 0; i< cmps.length; ++i){cmps[i].setVals(vals[i]);}}
	public float[][] getVals(){
		float [][] res = new float[cmps.length][];
		for(int i = 0; i< cmps.length; ++i){res[i] = cmps[i].getVals();}
		return res;
	}
	
	//dur is in seconds
	public void noteOn(float dur) {
		//p.outStr2Scr("NoteCh : " + ID + " Note on : " + curNote.ID);
		//adsr.noteOn();		
		//adsr.patch(own.sumMstr);		
		sum.patch(own.sumMstr);		
	}
	public void noteOff() {
		//p.outStr2Scr("NoteCh : " + ID + " Note off : " + curNote.ID);
		//adsr.unpatchAfterRelease( own.sumMstr );
//		adsr.noteOff();
		sum.unpatch(own.sumMstr);
	}	
	
	public void setCurNoteAndPlay(myNote _n){
		//curNote = _n;
		for(int i = 0; i<cmps.length; ++i){cmps[i].setFreq(_n.n.freq);}
		noteOn(0);
	}
	public void clearCurNoteAndStop(myNote _n){
		noteOff();		
		//p.outStr2Scr("In NC : " + ID + " reset all chans");
		for(int i = 0; i<cmps.length; ++i){cmps[i].resetAllVals();}
//		curNote = null;
	}

	//public float[] getADSRVals(){return new float[]{adsrMults[0], adsrMults[1], adsrMults[2], adsrMults[3], adsrMults[4]};}
	//public void setADSRVals(float[] vals){for(int i=0;i<5;++i){adsrMults[i] = vals[i];}adsr.setParameters(adsrMults[0], adsrMults[1], adsrMults[2], adsrMults[3], adsrMults[4], 0, 0);}
	//set new ADSR values :  0:max amplitude, 1:attTime, 2:dTime, 3:suslevel, 4:relTime
//	public void setADSR(int idx, float val){
//		if((idx < 0) || (idx>=adsrMults.length)){p.outStr2Scr("Error setting ADSR for :"+ID+" idx out of range : " + idx);return;}
//		adsrMults[idx]=val;
//		adsr.setParameters(adsrMults[0], adsrMults[1], adsrMults[2], adsrMults[3], adsrMults[4], 0, 0);
//	}
	
	//public void setHarmFract(int idx, float val){cmpFracts[idx]=val;	cmps[idx].setHarmFract(val);}

	//set oscil values if they change
	public void setWaveForm(Wavetable wf){wTbl = wf;for(int i = 0; i<p.numHarms; ++i){cmps[i].setWave(wTbl);}}
	public void setVolume(float _volAmt){volAmt = _volAmt;for(int i = 0; i<p.numHarms; ++i){cmps[i].setVol(volAmt);}}
	public void setPan(float _pan){panAmt = _pan;}
	public String toString(){
		String res = "ID : " + ID + " Vol : "+ volAmt+ " pan : " +panAmt + " # Comps :  "+cmps.length+" : \n";
		for(int i =0;i<cmps.length;++i){
			res += "\tcmps["+i+"] = "+cmps[i].toString() + "\n";
		}
		
		return res;	
	}
}//myNoteChannel class

//oscilitory component of an instrument - 1 per harmonic series component - sum together via summer
class myInstrCmp {
	public CAProject5 p;
	public static int iCmpCnt = 0;
	public int ID;
	//public myInstr own;				//owning instrument
	public myNoteChannel own;			//owning note channel
	public Oscil osc;				//oscilator for this component
	public float harmFract=1.0f, volExp, vol, freq, baseFreq, origVol, origFreq;				//multiplier for this oscillator, exponent to describe volume inv multiplicative (mult) decrease for each harmonic
	public Wavetable wTbl;
	
	public myInstrCmp(CAProject5 _p, myNoteChannel _own, float _harmFract, Wavetable _wTbl){
		p=_p;
		ID = iCmpCnt++;
		harmFract=_harmFract; own = _own; 
		volExp = 1.0f;
		vol = 0.5f*harmFract;origVol = vol;
		freq = 440.0f/harmFract;baseFreq = 440.0f; origFreq = 440.0f;
		wTbl = _wTbl;
		osc = new Oscil(freq, vol,wTbl); 
		osc.setSampleRate(44100.0f);
	}	
//	public void setVolExp(float _vExp){this.volExp = _vExp; setVol(vol);}
	
	public void resetAllVals(){	this.volExp =1.0f; setVolDir(origVol); setFreq(origFreq);}
	
	//pass in base vol/freq to component
	//public void setVol(float _vol){vol = _vol*(p.pow(harmFract, volExp));osc.setAmplitude(vol);	}
	public void setVol(float _vol){
		//p.outStr2Scr("Mod Vol for oscil " + ID + " to " + _vol + " | " + harmFract );
		vol = _vol*harmFract;osc.setAmplitude(vol);	}
	public void setVolDir(float _vol){
		//p.outStr2Scr("Mod Vol Dir for oscil " + ID + " to " + _vol + " | " + harmFract );
		vol = _vol;osc.setAmplitude(vol);	}
	public void setFreq (float _freq){baseFreq = _freq; freq = baseFreq/harmFract; freq = PApplet.min(freq,18000); osc.setFrequency(freq);	}	
	
	public void setFreqDir (float _freq){baseFreq = _freq * harmFract; freq = baseFreq/harmFract; freq = PApplet.min(freq,18000); osc.setFrequency(freq);	}	
	
	public void setWave(Wavetable wf){ wTbl = wf; osc.setWaveform(wf);}	
	
	public void setVals(float[] vals){setVolDir(vals[1]); setFreqDir(vals[2]); }
	public float[] getVals(){return new float[]{harmFract, vol, freq};}
	
	//shouldn't be called often - set harmonic multiplier, and reset indiv vol and freq
	//public void setHarmFract(float _harmFract){harmFract = _harmFract; setVol(vol);setFreq(freq); }
	public String toString(){
		String res = "Inst Cmp ID : "+ID+" sample rate : " + osc.sampleRate() + " Osc fraction : "+ String.format("%.2f", harmFract)+" Freq : "+ String.format("%.2f", freq)+" Vol : "+ String.format("%.2f", vol) + " Vol Exponent :" + String.format("%.2f", volExp);
		return res;
	}
}//myInstrCmp
