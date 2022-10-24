package SphrSeqFFTVisPKG.clef.base;

import SphrSeqFFTVisPKG.SeqVisFFTOcean;
import SphrSeqFFTVisPKG.clef.enums.clefVal;
import SphrSeqFFTVisPKG.note.NoteData;
import SphrSeqFFTVisPKG.note.enums.nValType;
import processing.core.PImage;

public abstract class myClefBase{
	public SeqVisFFTOcean p;
	public static int clfCnt = 0;
	public int ID;
	protected clefVal clef;		//clef for this instrument - for staff representation
	protected NoteData midNote;
	private NoteData sphereMidNote;
	protected NoteData lowNote;
	protected NoteData highNote;
	
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
		setSphereMidNote(new NoteData(midNote));
		getSphereMidNote().editNoteVal(nValType.C, getSphereMidNote().octave);
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
	public NoteData getMidNote() {return midNote;}
	
	public boolean equals(myClefBase _ot){return (this.clef.getVal() == _ot.clef.getVal());}
	
	public String toString(){
		//String res = "Clef ID : " + ID + " Name : " + name + " : Clef val : "+clef;		
		String res = "Clef Name : " + name + " | val : "+clef;		
		return res;
	}

	/**
	 * @return the sphereMidNote
	 */
	public NoteData getSphereMidNote() {		return sphereMidNote;	}

	/**
	 * @param sphereMidNote the sphereMidNote to set
	 */
	public void setSphereMidNote(NoteData _sphereMidNote) {		sphereMidNote = _sphereMidNote;	}
	
}//myClefBase
