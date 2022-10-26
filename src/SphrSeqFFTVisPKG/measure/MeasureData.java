package SphrSeqFFTVisPKG.measure;

import SphrSeqFFTVisPKG.clef.base.myClefBase;
import SphrSeqFFTVisPKG.staff.myKeySig;
import SphrSeqFFTVisPKG.staff.myTimeSig;
/**
 * convenience class to hold the important info for a measure (time sig, key sig, clef, c4 disp location, etc.
 * @author 7strb
 *
 */
public class MeasureData implements Comparable<MeasureData>{//comparison by measure seq #
	public int seqNum,
				stTime, 
				endTime;		//start time of measure from beginning of sequence in millis; end time in millis - notes after this need to go in new measure	

	public myMeasure meas;
	public myTimeSig ts;
	public myKeySig ks;
	public myClefBase clef;
	public float c4DispLoc;	
	
	public MeasureData(int _seqNum, int _stTime, myMeasure _meas, myTimeSig _ts, myKeySig _ks, myClefBase _clef, float _c4DispLoc){
		seqNum = _seqNum;		
		c4DispLoc = _c4DispLoc;			//may be overridden based on notes in this measure
		//key = _key;
		ks = _ks;
		ts = _ts;
		stTime = _stTime;
		clef = _clef;		
	}
	public MeasureData(MeasureData _m){	this(_m.seqNum,  _m.stTime,  _m.meas, _m.ts, _m.ks, _m.clef,_m.c4DispLoc);	}//copy ctor
	
	public void setAllVals(myTimeSig _timeSig,myKeySig _ks,myClefBase _clf,float _c4DispLoc){
		setKeySig(_ks);
		setTimeSig(_timeSig);
		setC4DispLoc(_c4DispLoc);
		setClef(_clf);	
	}
	
	//set all values dependent on key signature for this measure
	public void setKeySig(myKeySig _ks){ks = _ks;}
	public void setTimeSig(myTimeSig _timeSig){		ts = _timeSig;	}
	public void setC4DispLoc(float _c4DispLoc){	c4DispLoc = _c4DispLoc;}
	public void setClef(myClefBase _clf){clef = _clf;}

	public myKeySig getKeySig(){return ks;};	
	public myTimeSig getTimeSig(){return ts;};
	public float getC4DispLoc(){return c4DispLoc;}
	
	@Override
	public int compareTo(MeasureData arg0) {return Integer.compare(seqNum, arg0.seqNum);}//compare via seq number
	
	public String toString(){
		String res = "|seq # :" + seqNum+" |Time Sig : "+ ts.toString() + " |Key Sig : " + ks.toString() + "\n|Clef : "+ clef+ " |St time : " + stTime + " End Time : "+endTime; 
		return res;
	}
	
}//class MeasureData