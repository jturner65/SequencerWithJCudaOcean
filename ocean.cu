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
float2 interp2F2(float2 a, float2 b, float d)
{
	return make_float2(a.x + d*(b.x-a.x), a.y + d*(b.y-a.y)); 
}
__device__
float2 complex_mult(float2 ab, float2 cd)
{
    return make_float2(ab.x * cd.x - ab.y * cd.y, ab.x * cd.y + ab.y * cd.x);
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
										float thresh,
										float t)				//1 if notes, 0 if audio
{
    unsigned int x = blockIdx.x*blockDim.x + threadIdx.x;
    unsigned int y = blockIdx.y*blockDim.y + threadIdx.y;
    
    unsigned int in_index = y*in_width+x;
    unsigned int out_index = y*out_width+x;
    
    unsigned int inx = (x % (in_width-1))+1;
	unsigned int iny = (y % (in_width-1))+1;
//  unsigned int inx = in_width- (x % (in_width-1));
//	unsigned int iny = in_width- (y % (in_width-1));

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
//			float freqR = logf(1 +(freq_rList[inx] < thresh ? thresh : freq_rList[inx]))-1;
//			float freqC = logf(1 +(freq_cList[iny] < thresh ? thresh : freq_cList[iny]))-1;
			float freqR = (freq_rList[inx] < thresh ? thresh : freq_rList[inx]);
			float freqC = (freq_cList[iny] < thresh ? thresh : freq_cList[iny]);
			
			freqR = freqR / powf(2,llrintf(log2f(freqR+1))-1);
			freqC = freqC / powf(2,llrintf(log2f(freqC+1))-1);
			
//			freq_out[out_index] = make_float2(sinf(u*freq + t) * cosf(v*freq + t) * scFct, sinf(v*freq + t) * cosf(u*freq + t) * scFct);
			freq_out[out_index] = make_float2(sinf(u*freqR + t) * cosf(v*freqR + t) * scFct, sinf(v*freqC + t) * cosf(u*freqC + t) * scFct);
	    	//freq_out[out_index] = make_float2(freqR *scFct, freqC *scFct);
	    	//freq_out[newIdx] = make_float2(freqR * scFct, freqC * scFct);
	    	//freq_out[newIdx] = make_float2(sinf(u*freqR + t) * cosf(v*freqC + t) * scFct, sinf(v*freqR + t) * cosf(u*freqC + t) * scFct);
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
// generate wave heightfield at time t based on initial heightfield and dispersion relationship
extern "C"
__global__ void generateSpectrumKernel(float2* h0, float2* ht,float2* freq, unsigned int in_width, unsigned int out_width, unsigned int out_height,
                                       float t,float mix,float patchSize)
{
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

	if ((x < out_width) && (y < out_height)) {
		float2 h0_k = h0[in_index];
		float2 h0_mk = h0[in_mindex];
		float2 tmpRes1 = complex_add( complex_mult(h0_k, complex_exp(w * t)), complex_mult(conjugate(h0_mk), complex_exp(-w * t)) );
		//float2 tmpRes2 = make_float2 (freq[out_index].x + tmpRes1.x,freq[out_index].y + tmpRes1.y);
		float2 tmpRes2 = freq[out_index];
		
        // output frequency-space complex values
		//ht[out_index] = complex_add( complex_mult(h0_k, complex_exp(w * t)), complex_mult(conjugate(h0_mk), complex_exp(-w * t)) );
		ht[out_index] = interp2F2(tmpRes1,tmpRes2,mix);
	}
}

// update height map values based on output of FFT
extern "C"
__global__ void updateHeightmapKernel(float*  heightMap, float2* ht, unsigned int width)
{
    unsigned int x = blockIdx.x*blockDim.x + threadIdx.x;
    unsigned int y = blockIdx.y*blockDim.y + threadIdx.y;
    unsigned int i = y*width+x;
    
    float sign_correction = ((x + y) & 0x01) ? -1.0f : 1.0f;
	heightMap[i] = ht[i].x * sign_correction;
}

// generate slope by partial differences in spatial domain
extern "C"
__global__ void calculateSlopeKernel(float* h, float2 *slopeOut, unsigned int width, unsigned int height)
{
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
