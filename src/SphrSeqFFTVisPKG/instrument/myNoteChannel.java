package SphrSeqFFTVisPKG.instrument;

import SphrSeqFFTVisPKG.note.myNote;
import SphrSeqFFTVisPKG.ui.base.myMusicSimWindow;
import ddf.minim.ugens.Summer;
import ddf.minim.ugens.Wavetable;

/**
 * a set of oscil components, to represent 1 note channel at a time - # of these represents size of polyphony for instrument
 * @author 7strb
 *
 */
public class myNoteChannel {
	public myMusicSimWindow win;
	public static int iNCCnt = 0;
	public int ID;
		
	public myInstrument own;							//owning instrument
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
	public myNoteChannel(myMusicSimWindow _win, myInstrument _own, float[] _waveAmpMult, Wavetable _wTbl){
		win = _win;
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
			cmps[i] = new myInstrCmp(win,this, cmpFracts[i], wTbl); 
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
	public void setWaveForm(Wavetable wf){wTbl = wf;for(int i = 0; i<win.numHarms; ++i){cmps[i].setWave(wTbl);}}
	public void setVolume(float _volAmt){volAmt = _volAmt;for(int i = 0; i<win.numHarms; ++i){cmps[i].setVol(volAmt);}}
	public void setPan(float _pan){panAmt = _pan;}
	public String toString(){
		String res = "ID : " + ID + " Vol : "+ volAmt+ " pan : " +panAmt + " # Comps :  "+cmps.length+" : \n";
		for(int i =0;i<cmps.length;++i){
			res += "\tcmps["+i+"] = "+cmps[i].toString() + "\n";
		}
		
		return res;	
	}
}//myNoteChannel class