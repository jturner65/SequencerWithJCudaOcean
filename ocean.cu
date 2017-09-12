///////////////////////////////////////////////////////////////////////////////
#include <cufft.h>
#include <math_constants.h>

//Round a / b to nearest higher integer value
int cuda_iDivUp(int a, int b)
{
    return (a + (b - 1)) / b;
}

// complex math functions
__device__
float2 conjugate(float2 arg)
{
    return make_float2(arg.x, -arg.y);
}

__device__
float2 complex_exp(float arg)
{
    return make_float2(cosf(arg), sinf(arg));
}

__device__
float2 complex_add(float2 a, float2 b)
{
    return make_float2(a.x + b.x, a.y + b.y);
}
__device__
float2 scalarMult(float2 a, float b)
{
    return make_float2(a.x * b, a.y *b);
}

__device__
float2 interp2F2(float2 a, float2 b, float d)
{
	return make_float2(a.x + d*(b.x-a.x), a.y + d*(b.y-a.y)); 
}
__device__
float2 complex_mult(float2 ab, float2 cd)
{
    return make_float2(ab.x * cd.x - ab.y * cd.y, ab.x * cd.y + ab.y * cd.x);
}

__device__
float absSqrt(float a, float thresh){
	if(a > thresh){	return sqrtf(a);	}
	else if (a < -thresh) {return -sqrtf(-a);}
}

//convert passed list of frequencies to appropriate array of float2
extern "C"
__global__ void buildFrequencyDataKernel(float2* freq_out,
										float* freq_rList,						//single dimension array of 1024 elements
										float* freq_cList,
                                       	unsigned int in_width,
                                       	unsigned int out_width,
                                       	unsigned int out_height,
										unsigned int is_NoteFreqs, 
									//	float thresh,
										float t){
    unsigned int x = blockIdx.x*blockDim.x + threadIdx.x;
    unsigned int y = blockIdx.y*blockDim.y + threadIdx.y;
    
    unsigned int out_index = y*out_width+x;
    unsigned int halfWidth = in_width/2;
    unsigned int inx = ((x+halfWidth) % (in_width))+1;
	unsigned int iny = in_width -(((y+halfWidth) % (in_width))+1); //mirrored

    float u = x / (float) out_width;
    float v = y / (float) out_height;
    u = u*2.0f - 1.0f;
    v = v*2.0f - 1.0f;
    
	float scFct = .1f;
	t = t+scFct;
//	unsigned int totalOut = out_width * out_height;
//	unsigned int colOff = out_width/2;
//	unsigned int rowOff = (out_width * colOff);
//	unsigned int newIdx = (rowOff + (out_width*(out_index+colOff)/out_width) + 			
//				((colOff + (out_index%out_width)) % out_height))%totalOut;
	//if note frequencies, get complex version of note data, otherwise use freq_rList and freq_cList
	//e^j2pifot = cos(2pifot)<---freq_rList from audio + j(sin(2pifot) <---freq_cList from audio)    
	
//	if(is_NoteFreqs == 0){
		if ((x < out_width) && (y < out_height)) { 	//in_width == out_width
			float freqR = freq_rList[inx];
			float freqC = freq_cList[iny];
			//this squaring these maximizes the value at the center of the frequency map - this is where the philips noise value is
			float val =  (freqR  * freqC * 100) ;
	    	freq_out[out_index] = make_float2(val,val);
		}
	
//	} else {
//		if ((x < out_width) && (y < out_height)) { 	//need to send in FFT!
//			float freqR = (freq_rList[inx] < thresh ? thresh : freq_rList[inx]);
//			float freqC = (freq_cList[iny] < thresh ? thresh : freq_cList[iny]);
//			freqR = freqR / powf(2,llrintf(log2f(freqR+1))-1);
//			freqC = freqC / powf(2,llrintf(log2f(freqC+1))-1);
//			freq_out[out_index] = make_float2(sinf(u*freqR + t) * cosf(v*freqR + t) * scFct, sinf(v*freqC + t) * cosf(u*freqC + t) * scFct);
//		}
//	}
//	//freq_out[out_index] 

}
//convert passed list of frequencies to appropriate array of float2 - use sqrt
extern "C"
__global__ void buildFrequencyDataKernel2(float2* freq_out,
										float* freq_rList,						//single dimension array of 1024 elements
										float* freq_cList,
                                       	unsigned int in_width,
                                       	unsigned int out_width,
                                       	unsigned int out_height,
										unsigned int is_NoteFreqs,
									//	float thresh,
										float t){
    unsigned int x = blockIdx.x*blockDim.x + threadIdx.x;
    unsigned int y = blockIdx.y*blockDim.y + threadIdx.y;

    unsigned int out_index = y*out_width+x;
    unsigned int halfWidth = in_width/2;
    unsigned int inx = ((x+halfWidth) % (in_width))+1;
	unsigned int iny = in_width -(((y+halfWidth) % (in_width))+1); //mirrored

    float u = x / (float) out_width;
    float v = y / (float) out_height;
    u = u*2.0f - 1.0f;
    v = v*2.0f - 1.0f;

	float scFct = .1f;
	t = t+scFct;
//	unsigned int totalOut = out_width * out_height;
//	unsigned int colOff = out_width/2;
//	unsigned int rowOff = (out_width * colOff);
//	unsigned int newIdx = (rowOff + (out_width*(out_index+colOff)/out_width) +
//				((colOff + (out_index%out_width)) % out_height))%totalOut;
	//if note frequencies, get complex version of note data, otherwise use freq_rList and freq_cList
	//e^j2pifot = cos(2pifot)<---freq_rList from audio + j(sin(2pifot) <---freq_cList from audio)

//	if(is_NoteFreqs == 0){
		if ((x < out_width) && (y < out_height)) { 	//in_width == out_width
			float freqR = freq_rList[inx];
			float freqC = freq_cList[iny];
			//this maximizes the value at the center of the frequency map - this is where the philips noise value is
			float val =  (freqR  * freqC * 10) ;
			val = absSqrt(val, 1);
	    	freq_out[out_index] = make_float2(val,val);
		}

//	} else {
//		if ((x < out_width) && (y < out_height)) { 	//need to send in FFT!
//			float freqR = (freq_rList[inx] < thresh ? thresh : freq_rList[inx]);
//			float freqC = (freq_cList[iny] < thresh ? thresh : freq_cList[iny]);
//			freqR = freqR / powf(2,llrintf(log2f(freqR+1))-1);
//			freqC = freqC / powf(2,llrintf(log2f(freqC+1))-1);
//			freq_out[out_index] = make_float2(sinf(u*freqR + t) * cosf(v*freqR + t) * scFct, sinf(v*freqC + t) * cosf(u*freqC + t) * scFct);
//		}
//	}
//	//freq_out[out_index]

}
// generate wave heightfield at time t based on initial heightfield and dispersion relationship : interp between noise and music
extern "C"
__global__ void generateSpectrumKernel(float2* h0, float2* ht,float2* freq, unsigned int in_width, unsigned int out_width, unsigned int out_height,
                                       float t,float mix,float patchSize){
    unsigned int x = blockIdx.x*blockDim.x + threadIdx.x;
    unsigned int y = blockIdx.y*blockDim.y + threadIdx.y;
    unsigned int in_index = y*in_width+x;
    unsigned int in_mindex = (out_height - y)*in_width + (out_width - x); // mirrored
    unsigned int out_index = y*out_width+x;
    
    // calculate wave vector
    float2 k;
    float twoPiInvPtch = (2.0f * CUDART_PI_F / patchSize);
    k.x = (-(int)out_width / 2.0f + x) * twoPiInvPtch;
    k.y = (-(int)out_height / 2.0f + y) * twoPiInvPtch;

    // calculate dispersion w(k)
    float k_len = sqrtf(k.x*k.x + k.y*k.y);
    float w = sqrtf(9.81f * k_len);
    float2 cmplxExp = complex_exp(w * t);
    float2 cmplxNExp = complex_exp(-w * t);

	if ((x < out_width) && (y < out_height)) {
		float2 h0_k = h0[in_index];
		float2 h0_mk = h0[in_mindex];
		float2 f0_k = freq[in_index];
		float2 f0_mk = freq[in_mindex];
		float2 tmpRes1 = complex_add( complex_mult(h0_k, cmplxExp), complex_mult(conjugate(h0_mk), cmplxNExp) );
		float2 tmpRes2 = complex_mult( tmpRes1, complex_add( complex_mult(f0_k, cmplxExp), complex_mult(conjugate(f0_mk), cmplxNExp) ));
		 // output frequency-space complex values
		ht[out_index] = interp2F2(tmpRes1,tmpRes2,mix);
	}
}
// generate wave heightfield at time t based on initial heightfield and dispersion relationship : interpolate between noise and music convolved with noise
extern "C"
__global__ void generateSpectrumKernel2(float2* h0, float2* ht,float2* freq, unsigned int in_width, unsigned int out_width, unsigned int out_height,
                                       float t,float mix,float patchSize){
    unsigned int x = blockIdx.x*blockDim.x + threadIdx.x;
    unsigned int y = blockIdx.y*blockDim.y + threadIdx.y;
    unsigned int in_index = y*in_width+x;
    unsigned int in_mindex = (out_height - y)*in_width + (out_width - x); // mirrored
    unsigned int out_index = y*out_width+x;

    // calculate wave vector
    float2 k;
    float twoPiInvPtch = (2.0f * CUDART_PI_F / patchSize);
    k.x = (-(int)out_width / 2.0f + x) * twoPiInvPtch;
    k.y = (-(int)out_height / 2.0f + y) * twoPiInvPtch;

    // calculate dispersion w(k)
    float k_len = sqrtf(k.x*k.x + k.y*k.y);
    float w = sqrtf(9.81f * k_len);
    float2 cmplxExp = complex_exp(w * t);
    float2 cmplxNExp = complex_exp(-w * t);

	if ((x < out_width) && (y < out_height)) {
		float2 h0_k = h0[in_index];
		float2 h0_mk = h0[in_mindex];
		float2 f0_k = freq[in_index];
		float2 f0_mk = freq[in_mindex];
		float2 tmpRes1 = complex_add( complex_mult(h0_k, cmplxExp), complex_mult(conjugate(h0_mk), cmplxNExp) );
		//set "wet" mix to be convolved noise with audio frequencies
		float2 tmpRes2 = scalarMult(complex_add( complex_mult(f0_k, cmplxExp), complex_mult(conjugate(f0_mk), cmplxNExp) ), .1f);
        // output frequency-space complex values
		ht[out_index] = interp2F2(tmpRes1,tmpRes2,mix);
	}
}

// update height map values based on output of FFT
extern "C"
__global__ void updateHeightmapKernel(float*  heightMap, float2* ht, unsigned int width){
    unsigned int x = blockIdx.x*blockDim.x + threadIdx.x;
    unsigned int y = blockIdx.y*blockDim.y + threadIdx.y;
    unsigned int i = y*width+x;
    
    float sign_correction = ((x + y) & 0x01) ? -1.0f : 1.0f;
	heightMap[i] = ht[i].x * sign_correction;
}

// generate slope by partial differences in spatial domain
extern "C"
__global__ void calculateSlopeKernel(float* h, float2 *slopeOut, unsigned int width, unsigned int height){
    unsigned int x = blockIdx.x*blockDim.x + threadIdx.x;
    unsigned int y = blockIdx.y*blockDim.y + threadIdx.y;
    unsigned int i = y*width+x;

    float2 slope = make_float2(0.0f, 0.0f);
    if ((x > 0) && (y > 0) && (x < width-1) && (y < height-1)) {
        slope.x = h[i+1] - h[i-1];
        slope.y = h[i+width] - h[i-width];
    }
    slopeOut[i] = slope;
}
