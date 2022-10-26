package SphrSeqFFTVisPKG.note;

import SphrSeqFFTVisPKG.SeqVisFFTOcean;
import SphrSeqFFTVisPKG.note.enums.durType;
import SphrSeqFFTVisPKG.note.enums.noteValType;
import base_JavaProjTools_IRender.base_Render_Interface.IRenderInterface;

/**
 * convenience class to hold the important values for a note
 * @author 7strb
 *
 */
public class NoteData implements Comparable<NoteData> {//only compares start time
	public static final float Cn1 = 16.352f;		//baseline for note frequency
	//note value
	public noteValType name;
	public int octave;		//what octave this note lives in - 0 is lowest octave 
	
	public String nameOct;
	//use durType for base duration, add capability of handling dotted/tied notes (?)
	public float freq;			//frequency of this note. c0 is lowest note allowable - 16.352. 
	public float amplitude;		//how loud is the note.  default to 1.0f;
	
	//note duration and placement
	public int dur;			//duration, where 256 is a whole note (4 beats)	
	public durType typ;			//type of note
	
	public int typIdx;			//idx used when displaying - single calculation
	public int stTime;			//when this note starts from beginning of sequence in # of ticks
	
	public boolean isSharp;		//this note is a black key (always a sharp)
		
	public NoteData(noteValType _name, int _octave){
		setNoteVals(_name,_octave, 1.0f);
		setDur(0);
		if(_name == noteValType.rest){setRest();}
	}	
	
	public NoteData(NoteData _n){//copy ctor
		setNoteVals(_n.name,_n.octave, _n.amplitude);
		setDur(_n.dur);
		setDurType();
		stTime = _n.stTime;
	}
	
	public NoteData(int d, int s, noteValType _name, int _octave){
		this(_name, _octave);
		setDur(d);
		stTime = s;
		if(_name == noteValType.rest){setRest();}
	}
	//use to set all values if this note is a rest
	private void setRest(){
		octave = 0;
		freq = 0;
	}
	
	public void moveHalfStep(boolean up){
		int noteVal = name.getVal(), newVal, mod;
		if(up){
			mod = (noteVal == 11 ? 1 : 0);			//octave goes up if going up at b	
			newVal = (noteVal + 1)%12;
		} else {//down
			mod = (noteVal == 0 ? -1 : 0);			//octave goes down if going down at c
			newVal = ((noteVal + 12) - 1)%12;
		}
		setNoteVals(noteValType.getVal(newVal),octave+mod, amplitude);		//mod 12 because we want 0-11 (avoid rest==12)			
	}//
	
	//set volume of note TODO
	public void setAmplitude(float _amp){
		setNoteVals(name,octave,_amp);
	}

	//calculate appropriate start time in beats for minim. derp
	public float getStartPlayback(){
		float res = stTime/(1.0f*durType.Quarter.getVal());
		return res;
	}
	//this returns the duration in terms of fractional beats, where a beat is a quarter note
	public float getDurPlayback(){		
		float res = dur/(1.0f*durType.Quarter.getVal());
		return res;
	}
	//this sets duration from the piano scroll, where the duration is in terms of beats, and needs to be multiplied by qtr note value
	public void setDurScroll(float _scrDur, int defaultVal){
		dur = (int)(_scrDur * defaultVal);
		setDurType();		
	}
	//this adds duration from the piano scroll, where the duration is in terms of beats, and needs to be multiplied by qtr note value
	public void addDurScroll(float _scrDur){
		dur += (int)(_scrDur * durType.Quarter.getVal());
		setDurType();		
	}
	//set duration and durType - all duration-modification calcs need to be called before this
	public void setDur(int _dur){
		dur = _dur;
		setDurType();		
	}
	//durType {Whole(256),Half(128),Quarter(64),Eighth(32),Sixteenth(16),Thirtisecond(8);
	//should be analytic
	private void setDurType(){
		if(dur >= durType.Half.getVal() * 1.75f){typ = durType.Whole; typIdx = -2; return;}
		if(dur >= durType.Quarter.getVal() * 1.75f){typ = durType.Half;typIdx = -1; return;}
		if(dur >= durType.Eighth.getVal() * 1.75f){typ = durType.Quarter; typIdx = 0;return;}
		if(dur >= durType.Sixteenth.getVal() * 1.75f){typ = durType.Eighth;typIdx = 1; return;}
		if(dur >= durType.Thirtisecond.getVal() * 1.75f){typ = durType.Sixteenth; typIdx = 2;return;}		
		typ = durType.Thirtisecond;
		typIdx = 3;
	}
	
	public void quantMe(){
		if(dur >= durType.Half.getVal() * 1.75f){setDur(durType.Whole.getVal()); return;}
		if(dur >= durType.Quarter.getVal() * 1.75f){setDur(durType.Half.getVal());  return;}
		if(dur >= durType.Eighth.getVal() * 1.75f){setDur(durType.Quarter.getVal()); return;}
		if(dur >= durType.Sixteenth.getVal() * 1.75f){setDur(durType.Eighth.getVal());  return;}
		if(dur >= durType.Thirtisecond.getVal() * 1.75f){setDur(durType.Sixteenth.getVal()); return;}		
		setDur(durType.Thirtisecond.getVal()); 
	}
	
	//edit this notedata with new values
	public void editNoteVal(noteValType _name, int _octave){
		setNoteVals(_name,_octave, amplitude);
	}
	
	//sets note name, octave and frequency, returns frequency
	private float setNoteVals(noteValType _name, int _octave, float amp){
		name = _name;
		octave = _octave;
		nameOct = "" + name+octave;
		isSharp = !name.isNaturalNote();
		amplitude = amp;
		float octaveMult = (octave*12 + name.getVal());
		freq = (float) (Cn1 * Math.pow(2.0f,(1.0f*octaveMult)/12.0f));
		return freq;
	}
	
//	//for two notes to be the same their name, octave, duration and st time need to be the same
//	public boolean equals(NoteData _ot){return ((_ot.stTime == stTime) && (_ot.dur==dur) && (_ot.nameOct.equals(nameOct)));}
	//for two notes to be the same their name, octave, need to be same
	public boolean equals(NoteData _ot){return (_ot.nameOct.equals(nameOct));}
	
	//sorted list sort on start time for note data NOTE!
	@Override
	public int compareTo(NoteData other) {   return Float.compare(this.stTime, other.stTime);}
	//returns whether this note is lower than the passed note
	public boolean isLowerThan(NoteData _ckNote){	return ((this.octave < _ckNote.octave) || ((this.octave == _ckNote.octave) && (this.name.getVal() < _ckNote.name.getVal())));	}
	
	public String getNameOct(){	return "" + this.name + this.octave;}
	
	public String toString(){
		String res = "|Note: " +nameOct+"|Frq: "+String.format("%.2f",freq) + "|St Time (ticks): " + stTime + "|Dur : "+dur+ " Type : "+ typ+" Type idx : "+ typIdx;
		return res;		
	}

}//NoteData class

