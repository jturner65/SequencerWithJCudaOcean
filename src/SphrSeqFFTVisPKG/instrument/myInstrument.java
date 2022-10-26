package SphrSeqFFTVisPKG.instrument;
import ddf.minim.ugens.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

import SphrSeqFFTVisPKG.clef.myClef;
import SphrSeqFFTVisPKG.clef.myGrandClef;
import SphrSeqFFTVisPKG.clef.base.myClefBase;
import SphrSeqFFTVisPKG.note.myNote;
import SphrSeqFFTVisPKG.note.enums.noteValType;
import SphrSeqFFTVisPKG.staff.myStaff;
import SphrSeqFFTVisPKG.ui.base.myMusicSimWindow;


/**
 * A class to hold a kind of instrument, determined by the harmonic series it reproduces
 * @author john
 */
public class myInstrument {
	public myMusicSimWindow win;
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
	public myInstrument(myMusicSimWindow _win, String _nm, myClefBase _clef, float[] _waveAmpMult, Wavetable _wTbl, boolean isDrums) {
		win = _win;
		ID = instCnt++;
		instrName = _nm;
		wTbl = _wTbl;
		initFlags();
		instFlags[isDrumTrackIDX] = isDrums;
		if(_clef.isGrandStaff){	clef = new myGrandClef((myGrandClef) _clef);	}
		else {			clef = new myClef((myClef) _clef);		}
//		out = p.minim.getLineOut(p.OutTyp,p.glbBufrSize);	
//		out.setTempo(120);
		sumMstr = new Summer();
		sumMstr.setSampleRate(44100.0f);
		noteChans = new myNoteChannel[numNoteChans];
		oscilFracts = new float[_waveAmpMult.length];
		for(int i =0; i<_waveAmpMult.length; ++i){oscilFracts[i]=_waveAmpMult[i];}		//same oscil mults for all notes in this instrument
		for(int i =0; i<noteChans.length; ++i){	noteChans[i] = new myNoteChannel(win,this,oscilFracts, wTbl);	}
		initChanDeques();
//		adsrMults = new float[]{1.0f, 0.01f, 0.05f, 0.5f, 0.5f};//5 component multipliers for adsr : max amplitude, aTime, dTime, suslevel, rTime
//		adsr = new ADSR( adsrMults[0], adsrMults[1], adsrMults[2], adsrMults[3], adsrMults[4]);
//		adsr.setSampleRate(88200.0f);
		if(instFlags[isDrumTrackIDX]){
			for(int i =0; i<win.drumSounds.length;++i){	win.drumSounds[i].patch(sumMstr);	}
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
		//win.getMsgObj().dispInfoMessage("myInstrument","xxx","instr ID : " + ID + " Attempt to patch to output : flag : " + instFlags[mstOutPtchIDX] );		
		if(instFlags[mstOutPtchIDX]){return;}//already patched
		//out.pauseNotes();
		if(instFlags[isDrumTrackIDX]){
//			if(out!=null){out.close();}
//			out = p.minim.getLineOut(p.OutTyp,p.glbBufrSize);	
//			out.setTempo(120);
//			adsr.noteOn();
//			adsr.patch(p.glblSum);
			sumMstr.patch(win.glblSum);
		}
		else {
			sumMstr.patch(win.glblSum);
		}
		//adsr.patch(p.glblSum);
		//out.resumeNotes();
		instFlags[mstOutPtchIDX] = true;
	}
	public void unPatchMstrOut(){
		//win.getMsgObj().dispInfoMessage("myInstrument","xxx","instr ID : " + ID + " Attempt to unpatch output : flag : " + instFlags[mstOutPtchIDX]);
		if(!instFlags[mstOutPtchIDX]){return;}//already unpatched
		if(instFlags[isDrumTrackIDX]){
//			adsr.noteOff();
//			adsr.unpatchAfterRelease(p.glblSum);
			sumMstr.unpatch(win.glblSum);
//			p.glblOut.close();
//			p.resetAudioOut();
			//adsr.unpatchAfterRelease(p.glblSum);
		} else {
			sumMstr.unpatch(win.glblSum);
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
			case waveCntlIDX	: {if(wf==null){
				win.getMsgObj().dispInfoMessage("myInstrument","xxx","setInstCntlVals : Null wf sent to instrument: " +ID +":"+this.instrName); return;}setWaveForm(wf);return;}
			default 			: {return;}
		}		
	}//setInstCntlVals
	
	protected void setVolume(float _volAmt){volAmt = _volAmt;for(int i = 0; i< noteChans.length; ++i){noteChans[i].setVolume(volAmt);}}
	protected void setPanAmt(float _panAmt){panAmt = _panAmt;for(int i =0; i<noteChans.length; ++i){noteChans[i].setPan(panAmt);}}	
	protected void setWaveForm(Wavetable wf){wTbl = wf;	for(int i =0; i<noteChans.length; ++i){noteChans[i].setWaveForm(wTbl);}}
	
	public myNoteChannel getAvailNtChan(int nID){
		if(chansAvail.size() == 0){return null;}
		myNoteChannel nc = chansAvail.removeFirst(); 
		if(null == nc){//win.getMsgObj().dispInfoMessage("myInstrument","xxx","instr ID : " + ID + " ERROR : No more channels available to play note!");		
			return null;}
		//win.getMsgObj().dispInfoMessage("myInstrument","xxx","instr ID : " + ID + " getAvailNtChan NT Channel retreived :\n" + nc.toString() + "\nfor note ID : " + nID); 
		chansPlaying.put(nID,nc);
		return nc;}
	
	public myNoteChannel relPlayingNtChan(int nID){
		if(chansPlaying.size() == 0){return null;}
		myNoteChannel nc = chansPlaying.get(nID); 
		if(null == nc){//win.getMsgObj().dispInfoMessage("myInstrument","xxx","instr ID : " + ID + " ERROR : Specified note id " + nID + " not playing!");	
			return null;} 
		//win.getMsgObj().dispInfoMessage("myInstrument","xxx","instr ID : " + ID + " relPlaying NT Channel release :\n" + nc.toString()); 
		chansAvail.addLast(nc); 
		return nc;}

	//ret 1 if success, -1 if fail, 0 if no notes
	public int playSingleSphNote(myNote note){
		if(null==note){		//win.getMsgObj().dispInfoMessage("myInstrument","xxx","instr ID : " + ID + " play single note : null");
			return -1;}
		//win.getMsgObj().dispInfoMessage("myInstrument","xxx","instr ID : " + ID + " play single note : "+ note.toString());
		if(this.instFlags[myInstrument.isDrumTrackIDX]){return startSphDrumNote(note);}
		if(note.n.name == noteValType.rest){return 0;}		//don't add or remove rests
		if(!instFlags[mstOutPtchIDX]){patchMstrOut();}
		myNoteChannel _nc = getAvailNtChan(note.ID);		
		if(_nc == null){return -1;}
		_nc.setCurNoteAndPlay(note);
		//win.getMsgObj().dispInfoMessage("myInstrument","xxx","Playing Note instID : " + ID + " note channel : " + _nc.ID);
		numNotesPlaying++;
		return 1;		
	}
	//ret 1 if success, -1 if fail, 0 if no notes
	public int stopSingleSphNote(myNote note){
		if(null==note){	//win.getMsgObj().dispInfoMessage("myInstrument","xxx","instr ID : " + ID + " stop single note : null");
			return -1;}
		if(this.instFlags[myInstrument.isDrumTrackIDX]){return stopSphDrumNote(note);}
		//win.getMsgObj().dispInfoMessage("myInstrument","xxx","instr ID : " + ID + " stop single note : "+ note.toString());
		if(note.n.name == noteValType.rest){return 0;}		//don't add or remove rests
		myNoteChannel _nc = relPlayingNtChan(note.ID);
		if(_nc == null){return -1;}
		_nc.clearCurNoteAndStop(note);
		numNotesPlaying--;
		//for every note in stop notes map	
		if(numNotesPlaying <= 0){this.unPatchMstrOut(); numNotesPlaying =0;}	//may clip off note ends		
		return 2;
	}	
	
	private void triggerDrumSounds(int idx) {
		win.drumSounds[(idx % win.drumSounds.length)].trigger();
	}
	
	public int startSphDrumNote(myNote note){//note ring / 4 is drum sound to play (approx)
		int idx = (36 - note.sphereRing-1)/4;			//TODO
		numNotesPlaying++;
		//int idx = (note.sphereRing/4)-1;			
		//win.getMsgObj().dispInfoMessage("myInstrument","startSphDrumNote","Play Drum Note : ring :  (36 - 1 -" + note.sphereRing + ")/4 idx :  " + idx);
		triggerDrumSounds(idx);
		//win.getMsgObj().dispInfoMessage("myInstrument","startSphDrumNote","Play Drum Note : ring :  " + note.sphereRing + "/4 idx :  " + idx);
		
		return 1;
	}
	
	public int startDrumNote(myNote note){//note ring / 4 is drum sound to play (approx)
		int idx = note.n.octave;			//just use note octave for drum
		numNotesPlaying++;
		//int idx = (note.sphereRing/4)-1;			//TODO
		//win.getMsgObj().dispInfoMessage("myInstrument","xxx","Play Drum Note : ring :  (36 - 1 -" + note.sphereRing + ")/4 idx :  " + idx);
		triggerDrumSounds(idx);
		//win.getMsgObj().dispInfoMessage("myInstrument","xxx","Play Drum Note : ring :  " + note.sphereRing + "/4 idx :  " + idx);
		
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
			if(retCode == -1){win.getMsgObj().dispInfoMessage("myInstrument","xxx","instr ID : " + ID + " bad note channel");return -1;}
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
				if(retCode == -1){win.getMsgObj().dispInfoMessage("myInstrument","xxx","instr ID : " + ID + " bad note channel");return -1;}
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
		if(null==note){		//win.getMsgObj().dispInfoMessage("myInstrument","xxx","instr ID : " + ID + " play single note : null");
			return -1;}
		//win.getMsgObj().dispInfoMessage("myInstrument","xxx","instr ID : " + ID + " play single note : "+ note.toString());
		if(this.instFlags[myInstrument.isDrumTrackIDX]){return startDrumNote(note);}
		if(note.n.name == noteValType.rest){return 0;}		//don't add or remove rests
		if(!instFlags[mstOutPtchIDX]){patchMstrOut();}
		myNoteChannel _nc = getAvailNtChan(note.ID);		
		if(_nc == null){return -1;}
		_nc.setCurNoteAndPlay(note);
		win.getMsgObj().dispInfoMessage("myInstrument","xxx","Playing Note instID : " + ID + " note channel : " + _nc.ID);
		numNotesPlaying++;
		return 1;		
	}
	//ret 1 if success, -1 if fail, 0 if no notes
	public int stopSingleNote(myNote note){
		if(null==note){	//win.getMsgObj().dispInfoMessage("myInstrument","xxx","instr ID : " + ID + " stop single note : null");
			return -1;}
		if(this.instFlags[myInstrument.isDrumTrackIDX]){return stopDrumNote(note);}
		//win.getMsgObj().dispInfoMessage("myInstrument","stopSingleNote","instr ID : " + ID + " stop single note : "+ note.toString());
		if(note.n.name == noteValType.rest){return 0;}		//don't add or remove rests
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
