package SphrSeqFFTVisPKG.clef;

import SphrSeqFFTVisPKG.SeqVisFFTOcean;
import SphrSeqFFTVisPKG.clef.base.myClefBase;
import SphrSeqFFTVisPKG.clef.enums.clefVal;
import SphrSeqFFTVisPKG.note.NoteData;
import SphrSeqFFTVisPKG.note.enums.nValType;
import processing.core.PImage;

public class myClef extends myClefBase{

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
	public myClef(myClefBase _c){this(_c.p,_c.name,_c.getClef(),new NoteData(_c.getMidNote()), _c.clefImage, _c.drawDim, _c.occsOffset);}

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