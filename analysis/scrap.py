from pylab import *
from scipy import signal
from scipy.signal import remez
from scipy.signal import freqz
from scipy.signal import lfilter
Fs = 20

stop_lowstopband = 0.1 / Fs
start_passband = 0.15 / Fs
end_passband = 6.0 / Fs
start_stopband = 8.0 / Fs

b,a = signal.iirdesign(wp = [start_passband, end_passband], ws= [stop_lowstopband, start_stopband], gstop= 60, gpass=1, ftype='ellip')
w, h = freqz(b,a)
plot(w/(2*pi), 20*np.log10(abs(h)))
show()
