package SphrSeqFFTVisPKG.instrument;

import SphrSeqFFTVisPKG.ui.base.myMusicSimWindow;
import ddf.minim.ugens.Oscil;
import ddf.minim.ugens.Wavetable;
import processing.core.PApplet;

/**
 * oscilitory component of an instrument - 1 per harmonic series component - sum together via summer
 * @author 7strb
 *
 */
public class myInstrCmp {
	public myMusicSimWindow win;
	public static int iCmpCnt = 0;
	public int ID;
	//public myInstr own;				//owning instrument
	public myNoteChannel own;			//owning note channel
	public Oscil osc;				//oscilator for this component
	public float harmFract=1.0f, volExp, vol, freq, baseFreq, origVol, origFreq;				//multiplier for this oscillator, exponent to describe volume inv multiplicative (mult) decrease for each harmonic
	public Wavetable wTbl;
	
	public myInstrCmp(myMusicSimWindow _win, myNoteChannel _own, float _harmFract, Wavetable _wTbl){
		win = _win;
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