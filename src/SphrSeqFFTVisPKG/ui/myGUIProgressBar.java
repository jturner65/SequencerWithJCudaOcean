package SphrSeqFFTVisPKG.ui;

import SphrSeqFFTVisPKG.ui.base.myMusicSimWindow;
import base_JavaProjTools_IRender.base_Render_Interface.IRenderInterface;
import base_Math_Objects.vectorObjs.doubles.myVector;
import base_UI_Objects.windowUI.uiObjs.myGUIObj;

public class myGUIProgressBar extends myGUIObj{
	public final float[] barDims;// = new float[] {.8f * pa.menuWidth, 10.0f};

	public myGUIProgressBar(IRenderInterface _p, int _winID, String _name, myVector _start, myVector _end, double[] _minMaxMod, double _initVal, boolean[] _flags, double[] _off) {
		super(_p,  _winID, _name, _start, _end,  _minMaxMod, _initVal, _flags, _off);
		//TODO should be .8 * menuwidth
		barDims = new float[] {.12f * p.getWidth(), (float) (.5*yOff)};		
	}
	
	public myGUIProgressBar(IRenderInterface _p, int _winID, String _name, double _xst, double _yst, double _xend, double _yend, double[] _minMaxMod, double _initVal, boolean[] _flags, double[] _Off) {
		this(_p, _winID,_name,new myVector(_xst,_yst,0), new myVector(_xend,_yend,0), _minMaxMod, _initVal, _flags, _Off);	
	}

	@Override
	public void draw(){
		p.pushMatState();
			p.translate(initDrawTrans[0],initDrawTrans[1]);
			p.setColorValFill(_cVal, 255);
			p.setColorValStroke(_cVal, 255);
			p.setStrokeWt(1.0f);
			p.setStroke(0,0,0,255);
			p.showText(dispText, 0,0);
			p.translate(0,10.0f);
			p.setFill(255,255,255,255);
			p.drawRect(0,0,barDims[0],barDims[1]);
			//show progress
			p.setStrokeWt(1.0f);
			p.setStroke(0,0,0,0);
			p.setFill(255,0,0,255);
			float ratio = (float) (val/(maxVal)), ratLoc = ratio*barDims[0];
			p.drawRect(0,0,ratLoc, barDims[1]);
			p.translate(ratLoc,20.0f);
			p.showText(String.format("%4.2f", ratio), -10, 0);
		p.popMatState();
	}//draw


}