import numpy as np
from PIL import Image, ImageFilter
N=720
y,x=np.mgrid[0:N,0:N]
X=(x-(N-1)/2)/(N/2); Y=-(y-(N-1)/2)/(N/2)
R2=X*X+Y*Y; inside=R2<=1
Z=np.sqrt(np.clip(1-R2,0,1))
def vnoise(scale,seed):
    r=np.random.default_rng(seed); g=r.random((scale+3,scale+3))
    return np.asarray(Image.fromarray((g*255).astype(np.uint8)).resize((N,N),Image.BICUBIC),dtype=np.float32)/255
def fbm(oct,base,seed,p=0.55):
    t=np.zeros((N,N),np.float32);a=1;s=0
    for i in range(oct): t+=a*vnoise(base*2**i,seed+i);s+=a;a*=p
    return t/s
wx=(fbm(5,4,200)-0.5)*0.10; wy=(fbm(5,4,300)-0.5)*0.10
U=X+wx; V=Y+wy
blobs=[(-0.30,0.45,0.25,0.24),(0.15,0.42,0.15,0.14),(0.31,0.13,0.17,0.15),(0.38,0.04,0.10,0.08),(0.71,0.27,0.09,0.085),
(0.55,-0.10,0.09,0.15),(0.38,-0.27,0.075,0.075),(0.02,0.23,0.075,0.06),(0.0,0.03,0.05,0.05),(-0.15,-0.33,0.17,0.12),
(-0.25,-0.17,0.08,0.08),(-0.50,-0.40,0.085,0.085),(-0.62,0.18,0.22,0.27),(-0.53,-0.06,0.18,0.18),(-0.70,0.42,0.13,0.15),
(-0.44,0.03,0.13,0.12),(-0.76,-0.12,0.10,0.14),(-0.38,0.08,0.10,0.09),(-0.85,0.15,0.08,0.2),(0.92,0.05,0.05,0.2)]
for k in range(22): blobs.append((-0.52+k*0.042,0.765+0.012*np.sin(k*0.7)-0.0006*(k-11)**2,0.06,0.03))
f=np.zeros((N,N),np.float32)
for cx,cy,rx,ry in blobs:
    f+=np.exp(-(((U-cx)/rx)**2+((V-cy)/ry)**2)*1.3)
m=np.clip((f-0.30)/0.42,0,1); m=m*m*(3-2*m)
m*= 0.80+0.45*(fbm(5,5,77)-0.5)
m=np.clip(m,0,1)
fine=fbm(7,10,50,0.62); mid=fbm(6,5,70)
big=fbm(4,2,90)
high=0.80+0.10*(big-0.5)+0.08*(mid-0.5)+0.10*(fine-0.5)
mare_alb=0.44+0.12*(big-0.5)+0.09*(mid-0.5)+0.05*(fine-0.5)
alb=high*(1-m)+mare_alb*m
rng=np.random.default_rng(5)
for i in range(1600):
    r=np.sqrt(rng.uniform(0,0.97)); t=rng.uniform(0,2*np.pi); cx,cy=r*np.cos(t),r*np.sin(t)
    s=(0.002+min(rng.pareto(2.2)*0.003,0.02))
    zz=np.sqrt(max(1-r*r,0.05))
    i0=int((cx+1)*N/2); j0=int((1-cy)*N/2); w=int(s*N*2)+3
    sl=(slice(max(0,j0-w),min(N,j0+w)),slice(max(0,i0-w),min(N,i0+w)))
    # foreshortened along radial direction
    dx=X[sl]-cx; dy=Y[sl]-cy
    if r>1e-3:
        ur=(dx*cx+dy*cy)/r; ut=(-dx*cy+dy*cx)/r
    else: ur,ut=dx,dy
    d2=(ur/(s*zz))**2+(ut/s)**2
    amp=rng.choice([0.06,0.04,-0.04,0.10],p=[0.35,0.32,0.23,0.1])
    alb[sl]+=amp*np.exp(-d2*1.6)
def rays(cx,cy,n,length,amp,width,seed):
    global alb
    dx=X-cx; dy=Y-cy; dist=np.sqrt(dx*dx+dy*dy)+1e-6; ang=np.arctan2(dy,dx)
    r=np.random.default_rng(seed)
    for k in range(n):
        a=r.uniform(-np.pi,np.pi); Lr=length*r.uniform(0.3,1.0)
        da=np.angle(np.exp(1j*(ang-a)))
        alb+=amp*r.uniform(0.4,1.0)*np.exp(-(da*dist/width)**2)*np.exp(-dist/Lr)*(dist>0.03)
def crater(cx,cy,r,amp):
    global alb
    alb+=amp*np.exp(-(((X-cx)**2+(Y-cy)**2)/(r*r))*1.5)
rays(-0.13,-0.70,60,0.6,0.028,0.012,1); crater(-0.13,-0.70,0.05,0.10); crater(-0.13,-0.70,0.018,0.30)
rays(-0.32,0.17,40,0.25,0.025,0.010,2); crater(-0.32,0.17,0.045,0.08); crater(-0.32,0.17,0.026,0.22)
rays(-0.58,0.14,16,0.12,0.035,0.006,3); crater(-0.58,0.14,0.016,0.24)
rays(-0.72,0.38,10,0.08,0.03,0.005,4); crater(-0.72,0.38,0.014,0.5)
crater(0.62,-0.42,0.015,0.12); crater(0.82,-0.05,0.016,0.12); crater(-0.02,-0.10,0.012,0.08); crater(0.43,-0.18,0.012,0.08)
alb=np.clip(alb,0.05,1.3)
b=alb*(0.86+0.14*Z**0.4)
b=b/np.percentile(b[inside],99.7); b=np.clip(b,0,1)
rgb=np.stack([b*1.0,b*0.972,b*0.925],-1); rgb[~inside]=0
img=Image.fromarray((rgb*255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(0.5)).resize((512,512),Image.LANCZOS)
img.save('moon_tex.jpg',quality=90)
def phase(img,phi,earth=0.07):
    a=np.asarray(img,dtype=np.float32)/255; M=a.shape[0]
    yy,xx=np.mgrid[0:M,0:M]; X=(xx-(M-1)/2)/(M/2);Y=-(yy-(M-1)/2)/(M/2)
    R2=X*X+Y*Y; Z=np.sqrt(np.clip(1-R2,0,1))
    nd=X*np.sin(phi)+Z*(-np.cos(phi))
    I=0.35*np.clip(nd,0,1)+0.65*np.clip((nd+0.03)/0.21,0,1)**1.5
    o=a*(earth+(1-earth)*I)[...,None]; o[R2>1]=0
    return Image.fromarray((o*255).astype(np.uint8))
row=Image.new('RGB',(512*5,512))
for i,ph in enumerate([0.7,1.57,2.4,3.14,4.4]): row.paste(phase(img,ph),(512*i,0))
row.resize((1280,256)).save('preview.png')
