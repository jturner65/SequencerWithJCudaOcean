package SphrSeqFFTVisPKG;

import java.util.ArrayList;

import base_Math_Objects.vectorObjs.doubles.myVector;

//object on menu that can be modified via mouse input
public class myGUIObj {
    public int ID;
    public SeqVisFFTOcean p;
    public Base_DispWindow  win;            //mySideBarMenu owning window
    public int winID;                    //id in owning window
    public myVector start, end;                //x,y coords of start corner, end corner (z==0) for clickable region
    public String name, dispText;

    protected double val, minVal, maxVal;
    //public boolean treatAsInt;
    
    public int[] uiFlags;
    public static final int 
            debugIDX         = 0,
            showIDX            = 1,                //show this component
            //config flags
            treatAsIntIDX    = 2,
            hasListValsIDX    = 3,
            usedByWinsIDX    = 4;
    public static final int numFlags = 5;            
    
    public int _cVal;
    public double modMult,                        //multiplier for mod value
                    xOff,yOff;                        //Offset value
    public float[] initDrawTrans, boxDrawTrans;
    public int[] bxclr;
    
    public myGUIObj(SeqVisFFTOcean _p, Base_DispWindow _win, int _winID, String _name, myVector _start, myVector _end, double[] _minMaxMod, double _initVal, boolean[] _flags, double[] _off) {
        p=_p;
        win = _win;
        winID = _winID;
        ID = p.GUIObjID++;
        xOff = _off[0];
        yOff = _off[1];
        setName(_name);
        start = new myVector(_start); end = new myVector(_end);
        minVal=_minMaxMod[0]; maxVal = _minMaxMod[1]; val = _initVal;modMult = _minMaxMod[2];
        initFlags();
        for(int i =0; i<_flags.length;++i){     setFlags(i+2,_flags[i]);    }
        _cVal = p.gui_Black;
        bxclr = p.getRndClr();
        
        initDrawTrans= new float[]{(float)(start.x + xOff), (float)(start.y + yOff)};
        boxDrawTrans = new float[]{(float)(-xOff * .5f), (float)(-yOff*.25f)};        
    }    
    public myGUIObj(SeqVisFFTOcean _p, Base_DispWindow _win, int _winID, String _name,double _xst, double _yst, double _xend, double _yend, double[] _minMaxMod, double _initVal, boolean[] _flags, double[] _Off) {this(_p,_win, _winID,_name,new myVector(_xst,_yst,0), new myVector(_xend,_yend,0), _minMaxMod, _initVal, _flags, _Off);    }
    public void initFlags(){            uiFlags = new int[1 + numFlags/32]; for(int i = 0; i<numFlags; ++i){setFlags(i,false);}    }
    public boolean getFlags(int idx){    int bitLoc = 1<<(idx%32);return (uiFlags[idx/32] & bitLoc) == bitLoc;}    
    public void setFlags(int idx, boolean val){
        int flIDX = idx/32, mask = 1<<(idx%32);
        uiFlags[flIDX] = (val ?  uiFlags[flIDX] | mask : uiFlags[flIDX] & ~mask);
        switch (idx) {//special actions for each flag
        case debugIDX         :{break;}
        case showIDX        :{break;}    //show this component
        case treatAsIntIDX    :{break;}
        case hasListValsIDX    :{break;}
        case usedByWinsIDX    :{break;}
        }
    }//setFlag    
    
    public double getVal(){return val;}    
    public void setNewMax(double _newval){    maxVal = _newval;val = ((val >= minVal)&&(val<=maxVal)) ? val : (val < minVal) ? minVal : maxVal;        }
    public void setNewMin(double _newval){    minVal = _newval;val = ((val >= minVal)&&(val<=maxVal)) ? val : (val < minVal) ? minVal : maxVal;        }
    
    public double setVal(double _newVal){
        val = ((_newVal >= minVal)&&(_newVal<=maxVal)) ? _newVal : (_newVal < minVal) ? minVal : maxVal;        
        return val;
    }    
    
    public void setName(String _name) {
        name = _name;
        dispText = new String(""+name + " : ");
    }
    
    public double modVal(double mod){
        val += (mod*modMult);
        if(getFlags(treatAsIntIDX)){val = Math.round(val);}
        if(val<minVal){val = minVal;}
        else if(val>maxVal){val = maxVal;}
        //System.out.println("Mod : " + mod + " name : "+ name+ " Val : " + val);

        return val;        
    }
    public int getValueAsInt(){return (int)(val) ;}
    public float getValueAsFloat(){return (float)( val);}
    
    public boolean checkIn(float _clkx, float _clky){return (_clkx > start.x)&&(_clkx < end.x)&&(_clky > start.y)&&(_clky < end.y);}
    public void draw(){
        p.pushMatrix();p.pushStyle();
            p.translate(initDrawTrans[0],initDrawTrans[1]);
            p.setColorValFill(_cVal, 255);
            p.setColorValStroke(_cVal, 255);
            p.pushMatrix();p.pushStyle();
                p.noStroke();
                p.fill(bxclr[0],bxclr[1],bxclr[2],bxclr[3]);
                p.translate(boxDrawTrans[0],boxDrawTrans[1]);
                p.box(5);
            p.popStyle();p.popMatrix();
            if(!getFlags(treatAsIntIDX)){        p.text(dispText + String.format("%.5f",val), 0,0);}
            else{
                String resStr = String.format("%.0f",val);
                if(getFlags(hasListValsIDX)){                    
                    resStr = win.getUIListValStr(winID, (int)val);
                }
                p.text(dispText + resStr, 0,0);
            }
        p.popStyle();p.popMatrix();
    }
        
    public String[] getStrData(){
        ArrayList<String> tmpRes = new ArrayList<String>();
        tmpRes.add("ID : "+ ID+" Win ID : " + winID  + " Name : "+ name + " distText : " + dispText);
        tmpRes.add("Start loc : "+ start + " End loc : "+ end + " Treat as Int  : " + getFlags(treatAsIntIDX));
        tmpRes.add("Value : "+ val +" Max Val : "+ maxVal + " Min Val : " + minVal+ " Mod multiplier : " + modMult);
        return tmpRes.toArray(new String[0]);
    }
}//myGUIObj
