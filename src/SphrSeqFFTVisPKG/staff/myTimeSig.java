package SphrSeqFFTVisPKG.staff;
import SphrSeqFFTVisPKG.SeqVisFFTOcean;
import SphrSeqFFTVisPKG.note.enums.noteDurType;
import SphrSeqFFTVisPKG.staff.myTimeSig;

public class myTimeSig{
    public SeqVisFFTOcean p;
    public static int tsigCnt = 0;
    public final int ID;
    public final int beatPerMeas, beatNote;
    public final noteDurType noteType;
    public float[] drawDim;
    
    public myTimeSig(SeqVisFFTOcean _p, int _bPerMeas, int _beatNote, noteDurType _noteType){
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
        p.popStyle();p.popMatrix();        }
    public int getTicksPerBeat(){return noteType.getVal();}    
    
    public float tSigMult(){
        float res = (beatPerMeas/(1.0f * beatNote));
        return res;}
    
    
    public boolean equals(myTimeSig _ot){return ((_ot.beatPerMeas == this.beatPerMeas) && (_ot.beatNote == this.beatNote)) ;}

    public String toString(){
        String res = "Timesig :"+beatPerMeas+" beats per measure, "+beatNote+" note gets beat";
        return res;
    }
}//class myTimeSig



