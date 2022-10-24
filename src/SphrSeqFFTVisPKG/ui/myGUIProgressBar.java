package SphrSeqFFTVisPKG.ui;

import SphrSeqFFTVisPKG.SeqVisFFTOcean;
import SphrSeqFFTVisPKG.myDispWindow;
import SphrSeqFFTVisPKG.myGUIObj;
import base_Math_Objects.vectorObjs.doubles.myVector;

public class myGUIProgressBar extends myGUIObj{
	public final float[] barDims;// = new float[] {.8f * pa.menuWidth, 10.0f};

	public myGUIProgressBar(SeqVisFFTOcean _p, myDispWindow _win, int _winID, String _name, myVector _start, myVector _end, double[] _minMaxMod, double _initVal, boolean[] _flags, double[] _off) {
		super(_p, _win, _winID, _name, _start, _end,  _minMaxMod, _initVal, _flags, _off);
		barDims = new float[] {.8f * p.menuWidth, (float) (.5*yOff)};		
	}
	
	public myGUIProgressBar(SeqVisFFTOcean _p, myDispWindow _win, int _winID, String _name, double _xst, double _yst, double _xend, double _yend, double[] _minMaxMod, double _initVal, boolean[] _flags, double[] _Off) {this(_p,_win, _winID,_name,new myVector(_xst,_yst,0), new myVector(_xend,_yend,0), _minMaxMod, _initVal, _flags, _Off);	}

	@Override
	public void draw(){
//		p.pushMatrix();p.pushStyle();
//		p.fill(255,0,0,255);
//		p.rect((float)start.x, (float)start.y, (float)(end.x-start.x), (float)(end.y - start.y));
//		p.popStyle();p.popMatrix();
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


}