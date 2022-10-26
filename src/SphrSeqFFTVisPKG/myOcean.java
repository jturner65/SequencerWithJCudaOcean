package SphrSeqFFTVisPKG;

import static jcuda.driver.CUgraphicsMapResourceFlags.CU_GRAPHICS_MAP_RESOURCE_FLAGS_WRITE_DISCARD;
import static jcuda.driver.JCudaDriver.*;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Random;
import java.util.concurrent.ConcurrentSkipListMap;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.Animator;

import SphrSeqFFTVisPKG.ui.mySimWindow;
import base_JavaProjTools_IRender.base_Render_Interface.IRenderInterface;
import base_Math_Objects.MyMathUtils;
import jcuda.Pointer;
import jcuda.Sizeof;
import jcuda.driver.*;
import jcuda.jcufft.*;

//class implementing external window for fft ocean surface, based on cuda example.
public class myOcean implements GLEventListener{
	public IRenderInterface pa;
	public mySimWindow win;
	public JFrame frame;
	public Animator animator;
	public GL2 gl;	
	
	public int meshSize = 1024, 
			meshSzSq = meshSize * meshSize, 
			spectrumW = meshSize + 4, spectrumH = meshSize + 1, 
			vBufNumInts = ((meshSize*2)+2)*(meshSize-1),
			freqsInLen, 
			isNoteData;		//1 if note data, 0 if audio data
	
	// simulation parameters
	public float g = 9.81f, // gravitational constant
			wScl = 1e-7f, // wave scale factor
			translateX = 0.0f,
			translateY = 0.0f,
			translateZ = -1.0f,
			rotateX = 20f,
			rotateY = 0.0f,
			animTime = 0,						//timer for every frame of animation
			//UI mod variables
			patchSize = 75.0f,
			windSpeed = 50.0f,
			windDir = MyMathUtils.THIRD_PI_F,
			dirDepend = 0.07f,
			heightScale = 0.5f,
			freqMix = 0,						//amount of frequency data to mix into simulation
			//thresh = 0.0f,						//noise threshold in processing of freq data
			chopiness = 1.0f
			;
	
	public ConcurrentSkipListMap<String, Float> tmpSimVals;
	//OpenGL variables
	public int shaderProgramID;

	//OpenGL & Cuda variables
	public int posVertexBuffer, heightVertexBuffer, slopeVertexBuffer, indexBuffer;	
	public CUgraphicsResource cuda_heightVB_resource, cuda_slopeVB_resource;
	
	private String oceanKernel = "oceanMusic";//extensions added in loading function
	
	// FFT data
	public cufftHandle fftPlan;
	public CUdeviceptr d_h0Ptr, d_htPtr, d_slopePtr, d_freqInRPtr,d_freqInCPtr, d_freqPtr;
	public float[] h_h0, audFreqRealRes, audFreqImagRes;	
	
	//JCUDA
	public CUdevice device;
	public CUcontext glContext;
	public static final int bldFreqIDX = 0, bldFreq2IDX = 1, genSpecIDX = 2, genSpec2IDX = 3, updHMapIDX = 4, calcSlopeIDX = 5;
	public String[] kFuncNames = new String[]{"buildFrequencyDataKernel","buildFrequencyDataKernel2","generateSpectrumKernel","generateSpectrumKernel2","updateHeightmapKernel","calculateSlopeKernel"};
	public CUfunction[] kFuncs;// generateSpectrumKernel, updateHeightmapKernel, calculateSlopeKernel;
	public CUmodule module;
		
	protected int[] cudaFlags;
	public static final int 
			doneInit 		= 0, 
			newFreqVals		= 1, 
			newSimVals		= 2, 
			freqValsSet		= 3,
			forceRecomp		= 4,
			performInvFFT	= 5;
	public static final int numCudaFlags = 6;
	//colors
	public float[] deepColor, shallowColor, skyColor, lightDir;
	
	private final int gl2BufArg = GL2.GL_ARRAY_BUFFER;
		
	public myOcean(SeqVisFFTOcean _p,mySimWindow _win) {//, GLCapabilities capabilities) {
		pa=_p;
		win = _win;
//		PJOGL pgl = (PJOGL) pa.beginPGL();  
//		GL2 tmpGL = pgl.gl.getGL2();
//		pa.endPGL();
		
		GLCapabilities capabilities = new GLCapabilities(GLProfile.get(GLProfile.GL2)); 
		GLCanvas glComponent = new GLCanvas(capabilities);
		glComponent.setFocusable(true);
		glComponent.addGLEventListener(this);
		initShaderUnis();
		freqsInLen = 0;
		MouseControl mouseControl = new MouseControl();
		glComponent.addMouseMotionListener(mouseControl);
		glComponent.addMouseWheelListener(mouseControl);
		
		tmpSimVals = new ConcurrentSkipListMap<String,Float>();
		setTmpSimVals();
		initFlags();
		setFlags(forceRecomp,win.getPrivFlags(mySimWindow.forceCudaRecompIDX));			//whether or not 
		setFlags(performInvFFT, !win.getPrivFlags(mySimWindow.showFreqDomainIDX));
		setFlags(newSimVals, false);
		
		
		frame = new JFrame("WaterSurface");
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {	animator.stop();	}
			@Override
			public void windowClosed(WindowEvent e) {animator.stop();	delMe();	}			
		});
		frame.setLayout(new BorderLayout());
		glComponent.setPreferredSize(new Dimension(800, 800));
		//glComponent.setLocation(null);
		frame.add(glComponent, BorderLayout.CENTER);
		frame.pack();
		frame.setVisible(true);
		frame.setAlwaysOnTop(true);
		bringToFront();
		glComponent.requestFocus();
		
		// Create and start the animator
		animator = new Animator(glComponent);
		animator.start();
	}
	//private void initFlags(){cudaFlags = new boolean[numCudaFlags]; for(int i =0; i<numCudaFlags;++i){cudaFlags[i]=false;}}
	
	//boolean flag handling
	protected void initFlags(){cudaFlags = new int[1 + numCudaFlags/32]; for(int i = 0; i<numCudaFlags; ++i){setFlags(i,false);}}
	public boolean getFlags(int idx){int bitLoc = 1<<(idx%32);return (cudaFlags[idx/32] & bitLoc) == bitLoc;}	
	public void setFlags(int idx, boolean val) {
		boolean curVal = getFlags(idx);
		if(val == curVal) {return;}
		int flIDX = idx/32, mask = 1<<(idx%32);
		cudaFlags[flIDX] = (val ?  cudaFlags[flIDX] | mask : cudaFlags[flIDX] & ~mask);
		switch(idx){
			case doneInit 		: {break;}	
			case newFreqVals	: {break;}			
			case newSimVals		: {break;}
			case freqValsSet	: {break;}
			case forceRecomp	: {break;}
			case performInvFFT	: {break;}
			default :{}			
		}			
	}//setExecFlags
	
	
	
	public void closeMe() {
		frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));	
	}
	
	public void bringToFront(){
		SwingUtilities.invokeLater(new Runnable() {
		    @Override
		    public void run() {        frame.toFront();    }
		});
	}

	public void setFreqVals(float[] res1, float[] res2){
		audFreqRealRes = res1;
		audFreqImagRes = res2;	
		setFlags(newFreqVals, true);
	}
	
	//if parent UI changes any sim values
	public void setNewSimVal(String key, float val){
		tmpSimVals.put(key, val);
		//tmpSimVals[idx] = val;
		setFlags(newSimVals, true);
		//need to refresh window?
	}
	
	@Override
	public void init(GLAutoDrawable drawable) {
		gl = drawable.getGL().getGL2();
		gl.setSwapInterval(0);
		gl.glClearColor(skyColor[0],skyColor[1],skyColor[2],skyColor[3]);
		gl.glEnable(GL2.GL_DEPTH_TEST);

		initShaders(gl);
		initJCuda();
		initVBO(gl);
		setFlags(doneInit, true);
		win.sendUIValsToOcean();
	}
	
	public void initShaderUnis(){
		deepColor = new float[]{0.0f, 0.1f, 0.4f, 1.0f};
		shallowColor = new float[]{0.1f, 0.3f, 0.3f, 1.0f};
		skyColor = new float[]{.8f, .9f, 1.0f, 1.0f};
		lightDir = new float[]{ 0.0f, 1.0f, 0.0f};
	}
	
	public void delMe(){
		try{
			win.getMsgObj().dispInfoMessage("myOcean","delMe","Attempting to Release Ocean variables");
			cuMemFree(d_h0Ptr);
			cuMemFree(d_htPtr);
			cuMemFree(d_slopePtr);
			cuMemFree(d_freqPtr);
			cuMemFree(d_freqInRPtr);
			cuMemFree(d_freqInCPtr);
			JCufft.cufftDestroy(fftPlan);
			gl.glDeleteBuffers(1, IntBuffer.wrap(new int[] {posVertexBuffer}));
			gl.glDeleteBuffers(1, IntBuffer.wrap(new int[] {heightVertexBuffer}));
			gl.glDeleteBuffers(1, IntBuffer.wrap(new int[] {slopeVertexBuffer}));
			
		} catch (Exception e1){
			win.getMsgObj().dispInfoMessage("myOcean","delMe","Error when closing frame : ");
			e1.printStackTrace();
		}
		//System.exit(0);
		win.delOcean();	
	}
	private void initJCuda(){
		setExceptionsEnabled(true);
		cuInit(0);
		device = new CUdevice();
		cuDeviceGet(device, 0);
		glContext = new CUcontext();
		
		//cuGLCtxCreate(glContext, CUctx_flags.CU_CTX_BLOCKING_SYNC, device);	//deprecated - is this bad?
		cuCtxCreate(glContext, CUctx_flags.CU_CTX_BLOCKING_SYNC, device);
		//compile kernel file
		String ptxFileName = oceanKernel+".ptx";
		File f = new File(ptxFileName);
		if(!(f.exists() && !f.isDirectory()) || (getFlags(forceRecomp))) { //try to compile if doesn't exist			
			try {	compilePtxFile(oceanKernel+".cu",ptxFileName);} 
			catch (IOException e) {
				System.err.println("Could not create PTX file : "+ e.getMessage());
				throw new RuntimeException("Could not create PTX file", e);
			}
		} else {
			//debug 
		}
		module = new CUmodule();
		cuModuleLoad(module, ptxFileName);
		kFuncs = new CUfunction[kFuncNames.length];
		for(int i =0; i<kFuncNames.length;++i){
			kFuncs[i] = new CUfunction();
			cuModuleGetFunction(kFuncs[i], module, kFuncNames[i]);
		}
		fftPlan = new cufftHandle();
		JCufft.cufftPlan2d(fftPlan, meshSize, meshSize, cufftType.CUFFT_C2C);
		
		int spectrumSize = spectrumW * spectrumH * 2;
		h_h0 = new float[spectrumSize];
		int spectrumFloatSize = spectrumSize*Sizeof.FLOAT;
		h_h0 = generate_h0(h_h0);
		d_h0Ptr = new CUdeviceptr();
		cuMemAlloc(d_h0Ptr, spectrumFloatSize);
		cuMemcpyHtoD(d_h0Ptr, Pointer.to(h_h0), spectrumFloatSize);
		
		int outputSize = meshSzSq*Sizeof.FLOAT*2;
		d_htPtr = new CUdeviceptr();
		d_slopePtr = new CUdeviceptr();
		
		cuMemAlloc(d_htPtr, outputSize);
		cuMemAlloc(d_slopePtr, outputSize);

		d_freqPtr = new CUdeviceptr();		//2x as bg as either d_freqInRPtr or d_freqInCPtr
		//cuMemAlloc(d_freqPtr, outputSize);
		cuMemAlloc(d_freqPtr, spectrumFloatSize);
		//support up to 1024 simultaneous frequencies
		//make these not device ptrs? only 1 d //TODO
		int meshFloatSz = meshSize*Sizeof.FLOAT;
		d_freqInRPtr = new CUdeviceptr();
		cuMemAlloc(d_freqInRPtr,meshFloatSz);
		d_freqInCPtr = new CUdeviceptr();
		cuMemAlloc(d_freqInCPtr,meshFloatSz);
		setFlags(newFreqVals, false);
		setFlags(freqValsSet, false);	//legit values have been set
		setFlags(newSimVals, false);
	}//initJCuda
	
	//initialize vertex buffer object
	private void initVBO(GL2 gl) {		
		int[] buffer = new int[1];
		int size = meshSzSq*Sizeof.FLOAT;
		gl.glGenBuffers(1, IntBuffer.wrap(buffer));
		heightVertexBuffer = buffer[0];
		gl.glBindBuffer(gl2BufArg, heightVertexBuffer);
		gl.glBufferData(gl2BufArg, size, (Buffer) null, GL2.GL_DYNAMIC_DRAW);
		gl.glBindBuffer(gl2BufArg, 0);
		cuda_heightVB_resource = new CUgraphicsResource();
		cuGraphicsGLRegisterBuffer(cuda_heightVB_resource, heightVertexBuffer, CU_GRAPHICS_MAP_RESOURCE_FLAGS_WRITE_DISCARD);
		
		buffer = new int[1];
		size = meshSzSq*Sizeof.FLOAT*2;
		gl.glGenBuffers(1, IntBuffer.wrap(buffer));
		slopeVertexBuffer = buffer[0];
		gl.glBindBuffer(gl2BufArg, slopeVertexBuffer);
		gl.glBufferData(gl2BufArg, size, (Buffer) null, GL2.GL_DYNAMIC_DRAW);
		gl.glBindBuffer(gl2BufArg, 0);
		cuda_slopeVB_resource = new CUgraphicsResource();
		cuGraphicsGLRegisterBuffer(cuda_slopeVB_resource, slopeVertexBuffer, CU_GRAPHICS_MAP_RESOURCE_FLAGS_WRITE_DISCARD);

		buffer = new int[1];
		size = meshSzSq*Sizeof.FLOAT*4;
		gl.glGenBuffers(1, IntBuffer.wrap(buffer));
		posVertexBuffer = buffer[0];
		gl.glBindBuffer(gl2BufArg, posVertexBuffer);
		gl.glBufferData(gl2BufArg, size, (Buffer) null, GL2.GL_DYNAMIC_DRAW);
		gl.glBindBuffer(gl2BufArg, 0);
		gl.glBindBuffer(gl2BufArg, posVertexBuffer);
		ByteBuffer byteBuffer = gl.glMapBuffer(gl2BufArg, GL2.GL_WRITE_ONLY);
		if (byteBuffer != null){
			FloatBuffer put = byteBuffer.asFloatBuffer();
			int index = 0;
			float denom = (float) (meshSize - 1);
			for (int y = 0; y < meshSize; y++) {
				float v = y / denom;
				for (int x = 0; x < meshSize; x++) {
					float u = x / denom;
					put.put(index++, u * 2.0f - 1.0f);
					put.put(index++, 0.0f);
					put.put(index++, v * 2.0f - 1.0f);
					put.put(index++, 1.0f);
				}
			}
		}
		gl.glUnmapBuffer(gl2BufArg);
		gl.glBindBuffer(gl2BufArg, 0);

		size = vBufNumInts*Sizeof.INT;
		buffer = new int[1];
		gl.glGenBuffers(1, IntBuffer.wrap(buffer));
		indexBuffer = buffer[0];
		gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, indexBuffer);
		gl.glBufferData(GL2.GL_ELEMENT_ARRAY_BUFFER, size, (Buffer) null, GL2.GL_STATIC_DRAW);
		
		byteBuffer = gl.glMapBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, GL2.GL_WRITE_ONLY);
		if (byteBuffer != null){
			IntBuffer indices = byteBuffer.asIntBuffer();
			int index = 0;
			for(int y=0; y<meshSize-1; y++) {
		        for(int x=0; x<meshSize; x++) {
		        	indices.put(index, y*meshSize+x);
		        	indices.put(index+1, (y+1)*meshSize+x);
		        	index +=2;
		        }
		        indices.put(index, (y+1)*meshSize+(meshSize-1));
		        indices.put(index+1, (y+1)*meshSize);
		        index += 2;
		    }
		}
		gl.glUnmapBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER);
		gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, 0);
	}
	/**
	 * set uniform variables in shader
	 */
	private void setUnisInShdr(){
		int uniHeightScale = gl.glGetUniformLocation(shaderProgramID, "heightScale");
		gl.glUniform1f(uniHeightScale, heightScale * (getFlags(performInvFFT) ? 1.0f : 20.0f));

		int uniChopiness = gl.glGetUniformLocation(shaderProgramID, "chopiness");
		gl.glUniform1f(uniChopiness, chopiness);

		int uniSize = gl.glGetUniformLocation(shaderProgramID, "size");
		gl.glUniform2f(uniSize, (float) meshSize, (float) meshSize);

		int uniDeepColor = gl.glGetUniformLocation(shaderProgramID, "deepColor");
		gl.glUniform4f(uniDeepColor, deepColor[0], deepColor[1], deepColor[2], deepColor[3]);

		int uniShallowColor = gl.glGetUniformLocation(shaderProgramID, "shallowColor");
		gl.glUniform4f(uniShallowColor, shallowColor[0], shallowColor[1], shallowColor[2], shallowColor[3]);

		int uniSkyColor = gl.glGetUniformLocation(shaderProgramID, "skyColor");
		gl.glUniform4f(uniSkyColor, skyColor[0], skyColor[1], skyColor[2], skyColor[3]);

		int uniLightDir = gl.glGetUniformLocation(shaderProgramID, "lightDir");
		gl.glUniform3f(uniLightDir, lightDir[0], lightDir[1], lightDir[2]);
	}
	
	@Override
	public void display(GLAutoDrawable drawable) {
		float delta = System.nanoTime();
		if(getFlags(newSimVals)){updateSimVals();}
		gl = drawable.getGL().getGL2();

		runCuda();

		gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
		
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glLoadIdentity();
		gl.glTranslatef(translateX, translateY, translateZ);
		gl.glRotatef(rotateX, 1.0f, 0.0f, 0.0f);
		gl.glRotatef(rotateY, 0.0f, 1.0f, 0.0f);

		gl.glBindBuffer(gl2BufArg, posVertexBuffer);
		gl.glVertexPointer(4, GL2.GL_FLOAT, 0, 0);
		gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);

		gl.glBindBuffer(gl2BufArg, heightVertexBuffer);
		gl.glClientActiveTexture(GL2.GL_TEXTURE0);
		gl.glTexCoordPointer(1, GL2.GL_FLOAT, 0, 0);
		gl.glEnableClientState(GL2.GL_TEXTURE_COORD_ARRAY);

		gl.glBindBuffer(gl2BufArg, slopeVertexBuffer);
		gl.glClientActiveTexture(GL2.GL_TEXTURE1);
		gl.glTexCoordPointer(2, GL2.GL_FLOAT, 0, 0);
		gl.glEnableClientState(GL2.GL_TEXTURE_COORD_ARRAY);

		gl.glUseProgram(shaderProgramID);
		setUnisInShdr();
		
		gl.glColor3f(1.0f, 1.0f, 1.0f);
		gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, indexBuffer);
		gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_FILL);
		gl.glDrawElements(GL2.GL_TRIANGLE_STRIP, vBufNumInts, GL2.GL_UNSIGNED_INT, 0);
		gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_FILL);
		gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, 0);
		
		gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
		gl.glClientActiveTexture(GL2.GL_TEXTURE0);
		gl.glDisableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
		gl.glClientActiveTexture(GL2.GL_TEXTURE1);
		gl.glDisableClientState(GL2.GL_TEXTURE_COORD_ARRAY);

		gl.glUseProgram(0);
		
		delta = System.nanoTime() - delta;
		animTime +=  delta/1000000000;
	}

	@Override
	public void dispose(GLAutoDrawable drawable) { }
	
	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
		gl = drawable.getGL().getGL2();
		GLU glu = GLU.createGLU(gl);
		gl.glViewport(0, 0, width, height);

		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();
		glu.gluPerspective(60.0, (double) width / (double) height, 0.1, 10.0);
	}

	//move changed values to local variables
	private void updateSimVals(){
		patchSize 		= tmpSimVals.get("patchSizeIDX");
		windSpeed	 	= tmpSimVals.get("windSpeedIDX");
		windDir 		= tmpSimVals.get("windDirIDX");
		dirDepend 		= tmpSimVals.get("dirDependIDX");
		heightScale 	= tmpSimVals.get("heightScaleIDX");
		freqMix 		= tmpSimVals.get("freqMixIDX");
		chopiness 		= tmpSimVals.get("chopinessIDX");
		//thresh	 		= tmpSimVals.get("threshIDX");
		setFlags(newSimVals, false);
	}
	
	private void setTmpSimVals(){
		tmpSimVals.put("patchSizeIDX",patchSize );
		tmpSimVals.put("windSpeedIDX",windSpeed);
		tmpSimVals.put("windDirIDX",windDir);
		tmpSimVals.put("dirDependIDX",dirDepend);
		tmpSimVals.put("heightScaleIDX",heightScale);
		tmpSimVals.put("freqMixIDX",freqMix );
		tmpSimVals.put("chopinessIDX",chopiness );
		//tmpSimVals.put("threshIDX",thresh );
	}
	
	private void runCuda() {
		Pointer kernelParameters = null;
		//pa.outStr2Scr("run cuda : freqres1 : " + freqRes1.length);
		if(getFlags(newFreqVals)){
			cuMemcpyHtoD(d_freqInRPtr, Pointer.to(audFreqRealRes), audFreqRealRes.length*Sizeof.FLOAT);
			cuMemcpyHtoD(d_freqInCPtr, Pointer.to(audFreqImagRes), audFreqImagRes.length*Sizeof.FLOAT);
			setFlags(newFreqVals,false);
			setFlags(freqValsSet,true);
		}
		
		if(getFlags(freqValsSet)){
			int tblockX = 8;
			int tblockY = 8;
			int tgridX = meshSize/tblockX;
			int tgridY = tgridX;
	
			//build frequency data - meshSize ^2
			kernelParameters = Pointer.to(
					Pointer.to(d_freqPtr),
					Pointer.to(d_freqInRPtr),
					Pointer.to(d_freqInCPtr),
					Pointer.to(new int[] { freqsInLen }),
					Pointer.to(new int[] { spectrumW }),//meshSize }), 
					Pointer.to(new int[] { spectrumH }),//meshSize }),
					Pointer.to(new int[] { isNoteData }),
					//Pointer.to(new float[] { thresh }),
					Pointer.to(new float[] { animTime })					
					);
		
			cuLaunchKernel(kFuncs[win.getFreqKFunc()],//kFuncs[bldFreqIDX], 				//recalc phillips spectrum value for each time step
					tgridX, tgridY, 1, // Grid dimension
					tblockX, tblockY, 1, // Block dimension
					0, null, // Shared memory size and stream
					kernelParameters, null // Kernel- and extra parameters
			);
			cuCtxSynchronize();
	
			setFlags(freqValsSet, false);
		}
		
		int blockX = 8;
		int blockY = 8;
		int gridX = meshSize/blockX;
		int gridY = gridX;
		
		//build phillips data and mix with frequency data
		kernelParameters = Pointer.to(
				Pointer.to(d_h0Ptr),
				Pointer.to(d_htPtr),
				Pointer.to(d_freqPtr),
				Pointer.to(new int[] { spectrumW }),
				Pointer.to(new int[] { meshSize }),
				Pointer.to(new int[] { meshSize }),
				Pointer.to(new float[] { animTime }),  
				Pointer.to(new float[] { freqMix }),   
				Pointer.to(new float[] { patchSize }));
		
		//convert back from frequency domain to spatial domain
		if(getFlags(performInvFFT)) {
			cuLaunchKernel(kFuncs[genSpecIDX], 				//recalc phillips spectrum value for each time step
					gridX, gridY, 1, // Grid dimension
					blockX, blockY, 1, // Block dimension
					0, null, // Shared memory size and stream
					kernelParameters, null // Kernel- and extra parameters
			);
			cuCtxSynchronize();
			JCufft.cufftExecC2C(fftPlan, d_htPtr, d_htPtr, JCufft.CUFFT_INVERSE);
		} else {
			cuLaunchKernel(kFuncs[genSpec2IDX], 				//recalc phillips spectrum value for each time step
					gridX, gridY, 1, // Grid dimension
					blockX, blockY, 1, // Block dimension
					0, null, // Shared memory size and stream
					kernelParameters, null // Kernel- and extra parameters
			);
			cuCtxSynchronize();
		}
		
		CUdeviceptr g_hptr = new CUdeviceptr();
		cuGraphicsMapResources(1, new CUgraphicsResource[]{cuda_heightVB_resource}, null);
		cuGraphicsResourceGetMappedPointer(g_hptr, new long[1], cuda_heightVB_resource);
		kernelParameters = Pointer.to(
				Pointer.to(g_hptr),
				Pointer.to(d_htPtr),
				Pointer.to(new int[] { meshSize }));

		cuLaunchKernel(kFuncs[updHMapIDX],  
				gridX, gridY, 1, // Grid dimension
				blockX, blockY, 1, // Block dimension
				0, null, // Shared memory size and stream
				kernelParameters, null // Kernel- and extra parameters
		);
		cuCtxSynchronize();
		cuGraphicsUnmapResources(1, new CUgraphicsResource[]{cuda_heightVB_resource}, null);
		
		CUdeviceptr g_sptr = new CUdeviceptr();
		cuGraphicsMapResources(1, new CUgraphicsResource[]{cuda_slopeVB_resource}, null);
		cuGraphicsResourceGetMappedPointer(g_sptr, new long[1], cuda_slopeVB_resource);
		kernelParameters = Pointer.to(
				Pointer.to(g_hptr),
				Pointer.to(g_sptr),
				Pointer.to(new int[] { meshSize }),
				Pointer.to(new int[] { meshSize }));

		cuLaunchKernel(kFuncs[calcSlopeIDX],  
				gridX, gridY, 1, // Grid dimension
				blockX, blockY, 1, // Block dimension
				0, null, // Shared memory size and stream
				kernelParameters, null // Kernel- and extra parameters
		);
		cuCtxSynchronize();
		cuGraphicsUnmapResources(1, new CUgraphicsResource[]{ cuda_slopeVB_resource}, null);
		//pa.outStr2Scr("done cuda");
	}
	
	private void initShaders(GL2 gl) {
		shaderProgramID = gl.glCreateProgram();
		attachShader(gl, GL2.GL_VERTEX_SHADER, win.vertexShaderSource);
		attachShader(gl, GL2.GL_FRAGMENT_SHADER, win.fragmentShaderSource);
		gl.glLinkProgram(shaderProgramID);
		
		int[] buffer = new int[1];
		gl.glGetProgramiv(shaderProgramID, GL2.GL_LINK_STATUS, IntBuffer.wrap(buffer));
		gl.glValidateProgram(shaderProgramID);
	}
	
	private int attachShader(GL2 gl, int type, String shaderSource){
		int shader = 0; 
		shader = gl.glCreateShader(type);
		gl.glShaderSource(shader, 1, new String[] { shaderSource }, null);
		gl.glCompileShader(shader);
		int[] buffer = new int[1];
		gl.glGetShaderiv(shader, GL2.GL_COMPILE_STATUS, IntBuffer.wrap(buffer));
		gl.glAttachShader(shaderProgramID, shader);
		gl.glDeleteShader(shader);
		return shader;
	}
	
	// Phillips noise spectrum
	// (Kx, Ky) - normalized wave vector
	// Vdir - wind angle in radians
	// V - wind speed
	// A - constant
	public double phillips(float Kx, float Ky, float Vdir, float V, float A, float dir_depend, float Lsq) {
		double k_squared = Kx*Kx + Ky*Ky;
		
		if (k_squared == 0.0f){	return 0.0f;}
		double kSqrt = Math.sqrt(k_squared),
		//normalized dot prod
		k_x = (Kx / kSqrt),
		k_y = (Ky / kSqrt),
		w_dot_k = (k_x*Math.cos(Vdir) + k_y*Math.sin(Vdir)),
		//L = V*V/g,		
		res = (A*Math.exp(-1/(k_squared*Lsq))/(k_squared * k_squared) * w_dot_k*w_dot_k);
		
		// filter out waves moving opposite to wind
		if (w_dot_k < 0.0f){	res *= dir_depend;	}
		return res;
	}
	
	//initial setup of ocean
	public float[] generate_h0(float[] h0) {
		float kMult = (MyMathUtils.TWO_PI_F / patchSize);
		int nMshHalf = -meshSize/2; 
		float kx,ky,P,Er,Ei,h0_re,h0_im, L = (windSpeed*windSpeed/g), lsq = (L*L);
		//Min kx : -42.89321Max kx : 42.89321Min ky : -42.89321Max ky : 42.89321
		Random rnd = new Random();
		for (int y = 0; y <= meshSize; y++) {
			ky = (nMshHalf + y) * kMult;
			for (int x = 0; x <= meshSize; x++) {
				kx = (nMshHalf + x) * kMult;
				if (kx == 0.0f && ky == 0.0f){	P = 0.0f;}
				else {P = (float) (Math.sqrt(phillips(kx, ky, windDir, windSpeed, wScl, dirDepend,lsq)));	}
				Er = (float) rnd.nextGaussian();
				Ei = (float) rnd.nextGaussian();
				h0_re = (Er*P * MyMathUtils.SQRT_2_F);
				h0_im = (Ei*P * MyMathUtils.SQRT_2_F);

				int i2 = 2* (y * spectrumW + x);
				h0[i2] = h0_re;
				h0[i2+1] = h0_im;
			}
		}
		return h0;
	}
	
	class MouseControl implements MouseMotionListener, MouseWheelListener {
		public Point previousMousePosition = new Point();

		@Override
		public void mouseDragged(MouseEvent e) {
			int dx = e.getX() - previousMousePosition.x;
			int dy = e.getY() - previousMousePosition.y;

			if ((e.getModifiersEx() & MouseEvent.BUTTON3_DOWN_MASK) == MouseEvent.BUTTON3_DOWN_MASK) {
				translateX += dx / 100.0f;
				translateY -= dy / 100.0f;
			} else if ((e.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) == MouseEvent.BUTTON1_DOWN_MASK) {
				rotateX += dy;
				rotateY += dx;
			}
			previousMousePosition = e.getPoint();
		}

		@Override
		public void mouseMoved(MouseEvent e) {
			previousMousePosition = e.getPoint();
		}

		@Override
		public void mouseWheelMoved(MouseWheelEvent e) {
			translateZ -= e.getWheelRotation() * 0.25f;
			previousMousePosition = e.getPoint();
		}
	}
	//compiles Ptx file from file in passed file name -> cuFileName needs to have format "xxxxx.cu"
	public void compilePtxFile(String krnFileName, String ptxFileName) throws IOException {
	//NOTE : using new version of CUDA (as of 8/7/18) w/vs2015 compiles this incorrectly/makes it hang. TODO need to investigate this
		File cuFile = new File(krnFileName);
		if (!cuFile.exists()) {
			throw new IOException("Kernel file not found: " + krnFileName);
		}
		String modelString = "-m" + System.getProperty("sun.arch.data.model");
		//build compilation command
		String command = "nvcc " + modelString + " -ptx " + cuFile.getPath() + " -o " + ptxFileName;
		//execute compilation
		win.getMsgObj().dispInfoMessage("myOcean","compilePtxFile","Executing\n" + command);
		Process process = Runtime.getRuntime().exec(command);

		String errorMessage = new String(toByteArray(process.getErrorStream())), outputMessage = new String(toByteArray(process.getInputStream()));
		int exitValue = 0;
		try {exitValue = process.waitFor();} 
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException("Interrupted while waiting for nvcc output", e);
		}

		if (exitValue != 0) {
			win.getMsgObj().dispInfoMessage("myOcean","compilePtxFile","nvcc process error : exitValue : " + exitValue);
			win.getMsgObj().dispInfoMessage("myOcean","compilePtxFile","errorMessage :\n" + errorMessage);
			win.getMsgObj().dispInfoMessage("myOcean","compilePtxFile","outputMessage :\n" + outputMessage);
			throw new IOException("Could not create .ptx file: " + errorMessage);
		}
		win.getMsgObj().dispInfoMessage("myOcean","compilePtxFile","Finished compiling PTX file : "+ ptxFileName);
	}

	public byte[] toByteArray(InputStream inputStream) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte buffer[] = new byte[8192];
		while (true) {
			int read = inputStream.read(buffer);
			if (read == -1) {break;	}
			baos.write(buffer, 0, read);
		}
		return baos.toByteArray();
	}
	
}//class myOcean
