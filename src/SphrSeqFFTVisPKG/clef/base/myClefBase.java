package SphrSeqFFTVisPKG.clef.base;

import SphrSeqFFTVisPKG.clef.enums.clefType;
import SphrSeqFFTVisPKG.note.NoteData;
import SphrSeqFFTVisPKG.note.enums.noteValType;
import SphrSeqFFTVisPKG.ui.base.myMusicSimWindow;
import base_JavaProjTools_IRender.base_Render_Interface.IRenderInterface;
import processing.core.PImage;

public abstract class myClefBase{
	public myMusicSimWindow win;
	protected clefType clefName;		//clef for this instrument - for staff representation
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
	
	public myClefBase(myMusicSimWindow _win, String _name, clefType _clef, NoteData _mdNote,PImage _img, float[] _drawDim, float _ocf){
		win = _win;
		name = _name;
		clefName = _clef;	
		midNote = _mdNote;
		setSphereMidNote(new NoteData(midNote));
		getSphereMidNote().editNoteVal(noteValType.C, getSphereMidNote().octave);
		hasLoadedImg = !(null == _img);
		clefImage = _img;
		drawDim = new float[4];
		drawDim[0]=_drawDim[0];
		drawDim[1]=_drawDim[1];
		drawDim[2]=_drawDim[2];
		drawDim[3]=_drawDim[3];
		isGrandStaff = (clefName == clefType.Piano);
		occsOffset = _ocf;					//y offset for note based on clef => 0 is for treble cleff, -10 is for bass clef
		initClef();
	}	
	public myClefBase(myClefBase _otr) {
		win = _otr.win;
		name = _otr.name;
		clefName = _otr.clefName;
		midNote = _otr.midNote;		
		setSphereMidNote(new NoteData(midNote));
		getSphereMidNote().editNoteVal(noteValType.C, getSphereMidNote().octave);
		hasLoadedImg = _otr.hasLoadedImg;
		clefImage = _otr.clefImage;
		
		drawDim = new float[4];
		System.arraycopy(_otr.drawDim, 0, drawDim, 0, _otr.drawDim.length);

		isGrandStaff = _otr.isGrandStaff;
		occsOffset = _otr.occsOffset;					//y offset for note based on clef => 0 is for treble cleff, -10 is for bass clef
		initClef();
	}//copy ctor
	
	protected void initClef() {
		lowNote = new NoteData(midNote);
		int[] dispAmts = win.getNoteDisp(lowNote, -7);
		lowNote.editNoteVal(noteValType.getVal(dispAmts[0]), dispAmts[1]);
		highNote = new NoteData(midNote);
		dispAmts = win.getNoteDisp(highNote, 7);
		highNote.editNoteVal(noteValType.getVal(dispAmts[0]), dispAmts[1]);
		c4LocMultForClef = clefType.getC4LocMultClef(clefName);
	}

	public abstract void setImage(PImage[] _clf);
	
	public float getC4Mult(){return c4LocMultForClef;}
	
	/**
	 * checks if passed note is above the middle note in this clef- if so then stem should be down
	 * @param _note
	 * @return
	 */
	public abstract boolean isAboveMiddle(NoteData _note);
	/**
	 * whether or not a note is on the staff or above or below it : -1 is below, 0 is on, 1 is above
	 * @param _note
	 * @return
	 */
	public abstract int isOnStaff(NoteData _note);
	/**
	 * draw clef at appropriate position assumes starting at upper left of drawing rectangle - offset is x and y offset from drawing-control parent
	 * @param p
	 * @param offset
	 */
	public abstract void drawMe(IRenderInterface p, float offset);	
	
	public clefType getClef(){return clefName;}
	public NoteData getMidNote() {return midNote;}
	
	public boolean equals(myClefBase _ot){return (this.clefName.getVal() == _ot.clefName.getVal());}
	
	public String toString(){
		//String res = "Clef ID : " + ID + " Name : " + name + " : Clef val : "+clef;		
		String res = "Clef Name : " + name + " | val : "+clefName;		
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
