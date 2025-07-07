package SphrSeqFFTVisPKG.ui;

import SphrSeqFFTVisPKG.SeqVisFFTOcean;
import SphrSeqFFTVisPKG.Base_DispWindow;
import SphrSeqFFTVisPKG.myGUIObj;
import base_Math_Objects.vectorObjs.doubles.myVector;

public class myGUIProgressBar extends myGUIObj{
    public final float[] barDims;

    public myGUIProgressBar(SeqVisFFTOcean _p, Base_DispWindow _win, int _winID, String _name, myVector _start, myVector _end, double[] _minMaxMod, double _initVal, boolean[] _flags, double[] _off) {
        super(_p, _win, _winID, _name, _start, _end,  _minMaxMod, _initVal, _flags, _off);
        barDims = new float[] {.8f * p.menuWidth, (float)(.5*yOff)};        
    }
    
    @Override
    public void draw(){
//        p.pushMatrix();p.pushStyle();
//        p.fill(255,0,0,255);
//        p.rect((float)start.x, (float)start.y, (float)(end.x-start.x), (float)(end.y - start.y));
//        p.popStyle();p.popMatrix();
        p.pushMatrix();p.pushStyle();
            p.translate(initDrawTrans[0],initDrawTrans[1]);
            p.setColorValFill(_cVal, 255);
            p.setColorValStroke(_cVal, 255);
            p.strokeWeight(1.0f);
            p.stroke(0,0,0,255);
            p.text(dispText, 0,0);
            p.translate(0,10.0f);
            p.fill(255,255,255,255);
            p.rect(0,0,barDims[0],barDims[1]);
            //show progress
            p.strokeWeight(1.0f);
            p.stroke(0,0,0,0);
            p.fill(255,0,0,255);
            float ratio = (float) (val/(maxVal)), ratLoc = ratio*barDims[0];
            p.rect(0,0,ratLoc, barDims[1]);
            p.translate(ratLoc,20.0f);
            p.text(String.format("%4.2f", ratio), -10, 0);
        p.popStyle();p.popMatrix();
    }


}//class myGUIProgressBar