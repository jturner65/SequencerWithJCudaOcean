package SphrSeqFFTVisPKG;

import java.util.ArrayList;
import java.util.Arrays;

import processing.core.PImage;

public class myKeySig {
	public SeqVisFFTOcean p;
	public static int sigCnt = 0;
	public int ID;
	//there are 12 key signatures, each with a specific mapping of "allowed" notes
	private static final nValType[][] keyNotes = new nValType[][]{//circle of 5ths
		{nValType.C,nValType.D, nValType.E, nValType.F, nValType.G, nValType.A, nValType.B},		//CMaj(0) 0 sharps
		{nValType.G, nValType.A, nValType.B,nValType.C,nValType.D, nValType.E, nValType.Fs},		//GMaj(1) 1 sharp : Fs
		{nValType.D, nValType.E, nValType.Fs, nValType.G, nValType.A, nValType.B,nValType.Cs},		//DMaj(2) 2 sharps : Fs, Cs
		{nValType.A, nValType.B,nValType.Cs,nValType.D, nValType.E, nValType.Fs, nValType.Gs},		//Amaj(3) 3 sharps : Fs, Cs, Gs
		{nValType.E, nValType.Fs, nValType.Gs,nValType.A, nValType.B,nValType.Cs,nValType.Ds},		//EMaj(4) 4 sharps : Fs, Cs, Gs, Ds
		{nValType.B,nValType.Cs,nValType.Ds,nValType.E, nValType.Fs, nValType.Gs,nValType.As},		//BMaj(5) 5 sharps : Fs, Cs, Gs, Ds As
		{nValType.Fs, nValType.Gs,nValType.As,nValType.B,nValType.Cs,nValType.Ds,nValType.F},		//FsMaj(6) 6 sharps : Fs, Cs, Gs, Ds As Es
		{nValType.Cs,nValType.Ds,nValType.F,nValType.Fs, nValType.Gs,nValType.As,nValType.C},		//CsMaj(7) 5 flats :  Db, Eb, Gb, Ab, Bb
		{nValType.Gs,nValType.As,nValType.C,nValType.Cs,nValType.Ds,nValType.F,nValType.G},			//GsMaj(8) 4 flats :  Eb, Gb, Ab, Bb
		{nValType.Ds,nValType.F,nValType.G,nValType.Gs,nValType.As,nValType.C,nValType.D},			//DsMaj(9) 3 flats :  Gb, Ab, Bb
		{nValType.As,nValType.C,nValType.D,nValType.Ds,nValType.F,nValType.G,nValType.A},			//AsMaj(10) 2 flats : Ab, Bb
		{nValType.F,nValType.G,nValType.A,nValType.As,nValType.C,nValType.D,nValType.E},			//Fmaj(11) 1 flat   : Bb
	};
	private static final String[][] occsDisp = new String[][]{
		{},{"#"},{"#","#"},{"#","#","#"},{"#","#","#","#"},{"#","#","#","#","#"},{"#","#","#","#","#","#"},
		{"b","b","b","b","b"},{"b","b","b","b"},{"b","b","b"},{"b","b"},{"b"}};

	private static final float occsDim = 10;
	
	private static final float[][][] occsDimAra = new float[][][]{
		{{0,0}},
		{{0,8}},
		{{0,8},{occsDim,15}},
		{{0,8},{occsDim,15},{occsDim,-20}},
		{{0,8},{occsDim,15},{occsDim,-20},{occsDim,15}},
		{{0,8},{occsDim,15},{occsDim,-20},{occsDim,15},{occsDim,15}},
		{{0,8},{occsDim,15},{occsDim,-20},{occsDim,15},{occsDim,15},{occsDim,-20}},		//here and above is sharps, below is flats
		{{0,25},{occsDim,-15},{occsDim,20},{occsDim,-15},{occsDim,20}},
		{{0,25},{occsDim,-15},{occsDim,20},{occsDim,-15}},
		{{0,25},{occsDim,-15},{occsDim,20}},
		{{0,25},{occsDim,-15}},
		{{0,25}}
	};
	public keySigVals key;
	public float[] drawDim;
	public int keyIdx;
	
	public myKeySig(SeqVisFFTOcean _p, keySigVals _key) {
		p=_p;
		ID = sigCnt++;
		key = _key;		
		keyIdx = key.getVal();
		drawDim = new float[4];
		drawDim[0]=-10;
		drawDim[1]=0;		
		drawDim[2]= occsDisp[keyIdx].length * occsDim;
		drawDim[3]=50;
	}
	
	public myKeySig(myKeySig _ks){this(_ks.p, _ks.key);}
	//get root of key
	public nValType getRoot(){return keyNotes[key.getVal()][0];}
	public static nValType[] getKeyNotes(keySigVals _psdKey){return  keyNotes[_psdKey.getVal()];}
	public static ArrayList<nValType> getKeyNotesAsList(keySigVals _psdKey){return  new ArrayList<nValType>(Arrays.asList(keyNotes[_psdKey.getVal()]));}
	//return array of alloweable note types for this key signature
	public nValType[] getKeyVals(){	return keyNotes[key.getVal()];}	
	public ArrayList<nValType> getKeyNotesAsList(){return  new ArrayList<nValType>(Arrays.asList(keyNotes[keyIdx]));}

	//assumes starting at upper left of measure bound - offY is for offset from clef, to align with correct notes
	public void drawMe(float offX, float offY){
		p.pushMatrix();p.pushStyle();
		p.translate(drawDim[0] + offX,drawDim[1] + offY);		
		for(int i=0;i<occsDisp[keyIdx].length;++i){
			p.translate(occsDimAra[keyIdx][i][0], occsDimAra[keyIdx][i][1]);
			p.pushMatrix();p.pushStyle();
			p.scale(1, 1.6f);
			p.text(occsDisp[keyIdx][i], 0, 0);
			p.popStyle();p.popMatrix();	
		}
		p.popStyle();p.popMatrix();	
	}
	
	public boolean equals(myKeySig _ot){return (_ot.key.getVal() == this.key.getVal());}
	
	public String toString(){
		String res = "Key ID : "+ID+" Key Sig : "+ key;
		return res;
	}

}//class myKeySig


class myTimeSig{
	public SeqVisFFTOcean p;
	public static int tsigCnt = 0;
	public final int ID;
	public final int beatPerMeas, beatNote;
	public final durType noteType;
	public float[] drawDim;
	
	public myTimeSig(SeqVisFFTOcean _p, int _bPerMeas, int _beatNote, durType _noteType){
		p=_p;
		ID = tsigCnt++;
		beatPerMeas =_bPerMeas; 
		beatNote = _beatNote;
		noteType = _noteType;
		drawDim = new float[4];
		drawDim[0]=-5;
		drawDim[1]=20;
		drawDim[2]=20;
		drawDim[3]=50;
	}	
	
	public myTimeSig(myTimeSig _cp){
		this(_cp.p,_cp.beatPerMeas, _cp.beatNote, _cp.noteType);
		//p.outStr2Scr("----time sig cpy ctor : " + _cp.toString());
	}
	
	//assumes starting at upper left of drawing rectangle - yOff is offset from drawing-control parent
	public void drawMe(float offset){
		p.pushMatrix();p.pushStyle();
		p.translate(drawDim[0] + offset,drawDim[1]);
			p.pushMatrix();p.pushStyle();
			p.scale(2.0f);
			p.text(beatPerMeas, 0, 0);
			p.popStyle();p.popMatrix();	
		p.translate(0,drawDim[1]);
			p.pushMatrix();p.pushStyle();
			p.scale(2.0f);
			p.text(beatNote, 0, 0);
			p.popStyle();p.popMatrix();	
		p.popStyle();p.popMatrix();		}
	public int getTicksPerBeat(){return noteType.getVal();}	
	
	public float tSigMult(){
		float res = (beatPerMeas/(1.0f * beatNote));
		return res;}
	
	
	public boolean equals(myTimeSig _ot){return ((_ot.beatPerMeas == this.beatPerMeas) && (_ot.beatNote == this.beatNote)) ;}

	public String toString(){
		String res = "Timesig :"+beatPerMeas+" beats per measure, "+beatNote+" note gets beat";
		return res;
	}
}

abstract class myClefBase{
	public SeqVisFFTOcean p;
	public static int clfCnt = 0;
	public int ID;
	protected clefVal clef;		//clef for this instrument - for staff representation
	protected NoteData midNote, sphereMidNote,
					lowNote,
					highNote;
	
	protected float c4LocMultForClef;

	public String name;	
	public boolean isGrandStaff, 
				hasLoadedImg;
	public PImage clefImage;	
	public float[] drawDim;
	
	public float occsOffset;
	
	public myClefBase(SeqVisFFTOcean _p, String _name, clefVal _clef, NoteData _mdNote,PImage _img, float[] _drawDim, float _ocf){
		p = _p;
		name = _name;
		clef = _clef;	
		midNote = _mdNote;
		sphereMidNote = new NoteData(midNote);
		sphereMidNote.editNoteVal(nValType.C, sphereMidNote.octave);
		hasLoadedImg = !(null == _img);
		clefImage = _img;
		drawDim = new float[4];
		drawDim[0]=_drawDim[0];
		drawDim[1]=_drawDim[1];
		drawDim[2]=_drawDim[2];
		drawDim[3]=_drawDim[3];
		isGrandStaff = false;
		occsOffset = _ocf;					//y offset for note based on clef => 0 is for treble cleff, -10 is for bass clef
	}	

	public abstract void setImage(PImage[] _clf);
	
	public float getC4Mult(){return c4LocMultForClef;}
	
	//checks if passed note is above the middle note in this clef- if so then stem should be down
	public abstract boolean isAboveMiddle(NoteData _note);
	//whether or not a note is on the staff or above or below it : -1 is below, 0 is on, 1 is above
	public abstract int isOnStaff(NoteData _note);
	//draw clef at appropriate position
	//assumes starting at upper left of drawing rectangle - offset is x and y offset from drawing-control parent
	public abstract void drawMe(float offset);	
	
	public clefVal getClef(){return clef;}
	
	public boolean equals(myClefBase _ot){return (this.clef.getVal() == _ot.clef.getVal());}
	
	public String toString(){
		//String res = "Clef ID : " + ID + " Name : " + name + " : Clef val : "+clef;		
		String res = "Clef Name : " + name + " | val : "+clef;		
		return res;
	}
	
}//myClefBase

class myClef extends myClefBase{

	public myClef(SeqVisFFTOcean _p, String _name, clefVal _clef, NoteData _mdNote, PImage _img, float[] _drawDim, float _ocf) {
		super(_p, _name, _clef, _mdNote, _img, _drawDim, _ocf);
		lowNote = new NoteData(midNote);
		int[] dispAmts = p.getNoteDisp(lowNote, -7);
		lowNote.editNoteVal(nValType.getVal(dispAmts[0]), dispAmts[1]);
		highNote = new NoteData(midNote);
		dispAmts = p.getNoteDisp(highNote, 7);
		highNote.editNoteVal(nValType.getVal(dispAmts[0]), dispAmts[1]);
		c4LocMultForClef = p.getC4LocMultClef(clef, false);
	}
	public myClef(myClefBase _c){this(_c.p,_c.name,_c.clef,new NoteData(_c.midNote), _c.clefImage, _c.drawDim, _c.occsOffset);}

	@Override
	public boolean isAboveMiddle(NoteData _note) 	{
		return midNote.isLowerThan(_note);
	}
	@Override
	public void setImage(PImage[] _clf) {//only 1 image, passed by reference
		this.clefImage = _clf[0];	
	}
	//whether or not a note is on the staff or above or below it : -1 is below, 0 is on, 1 is above
	@Override
	public int isOnStaff(NoteData _note) {
		return (highNote.isLowerThan(_note) ? 1 : (_note.isLowerThan(lowNote)) ? -1: 0);
	}
	
	@Override
	public void drawMe(float offset) {
		p.pushMatrix();p.pushStyle();//draw image	
		p.image(clefImage, drawDim[0] + offset,drawDim[1], drawDim[2],drawDim[3]);		
		p.popStyle();p.popMatrix();	
	}
	
	@Override
	public String toString(){
		String res = super.toString();
		res +=" Clef middle note : " + midNote.name+midNote.octave+ " High Note : "+ highNote.name+highNote.octave+ " Low Note : " + lowNote.name+lowNote.octave;
		return res;
	}

}//myClef

class myGrandClef extends myClefBase{	
	public myClef[] clefs;

	public myGrandClef(SeqVisFFTOcean _p, String _name, clefVal _clef, NoteData _mdNote, PImage _img, float[] _drawDim, float _ocf) {
		super(_p, _name, _clef, _mdNote, _img,_drawDim, _ocf);		
		isGrandStaff = true;
		clefs = new myClef[2];
		clefs[0]=new myClef(p.clefs[0]);		//first clef is treble clef
		clefs[1]=new myClef(p.clefs[1]);		//2nd clef is bass clef
		//ignore this staff's values	
		clef = null;
		midNote = null;
		clefImage = null;
		c4LocMultForClef = p.getC4LocMultClef(clef, true);
	}
	public myGrandClef(myClefBase _c){this(_c.p,_c.name,_c.clef,null,null, _c.drawDim, _c.occsOffset);}

	@Override
	public boolean isAboveMiddle(NoteData _note) 	{
		//should be stem down if lower than middle c but higher than bass clef middle note or higher than treble clef mid note
		return (((_note.isLowerThan(p.C4)) && (clefs[1].midNote.isLowerThan(_note))) || (clefs[0].midNote.isLowerThan(_note)));
	}
	@Override
	public void setImage(PImage[] _clf) {
		//set both images for both clefs in this clef
		for(int i =0; i<_clf.length;++i){clefs[i].setImage(new PImage[]{_clf[i]});}
	}
	//whether or not a note is on the staff or above or below it : -1 is below, 0 is on, 1 is above
	@Override
	public int isOnStaff(NoteData _note) {
		int clf0Val = clefs[0].isOnStaff(_note),
			clf1Val = clefs[0].isOnStaff(_note);		
		return clf0Val * clf1Val * (clf1Val < 0 ? -1 : 1);		//will be -1 if below clf1 or above clf1 but below clf 0, if in either staff will be 0, will be 1 if above both
	}

	@Override
	public void drawMe(float offset) {
		p.pushMatrix();p.pushStyle();
		clefs[0].drawMe(offset);
		p.translate(0, 5*offset);			//TODO figure correct offset amount
		clefs[1].drawMe(offset);	
		p.popStyle();p.popMatrix();	
	}
	
	@Override
	public String toString(){
		String res = "Grand Staff : ID: "+ID+":\n";
		res +="\t"+clefs[0].toString()+"\n";
		res +="\t"+clefs[1].toString()+"\n";
		return res;
	}

}//myGrandClef