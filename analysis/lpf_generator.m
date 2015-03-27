% FIR Filter
% Sampling rate: 25Hz
% Cutoff: 12Hz (Nyquist)

n = 10;
Fs = 25;
Fc = 12 / Fs;
transBand = 4 / Fs;

f = [0 Fc Fc+transBand 1];
a = [1 1 0 0];

% Parks-McClellan, maybe not the best, high passband ripple
b = firpm(n, f, a, [1 1]);

% Only specify Fc
% b = fir1(n, Fc);

% Similar to firpm. Monotonic downward
% b = fir2(n, f, a);

[h,w] = freqz(b, 1);
plot(f, a, w/pi, abs(h));

%%
[b, a] = butter(5, Fc);
[h,w] = freqz(b,a);
plot(w/pi, abs(h));

fprintf('private static final float[] bVal = {%ff, %ff, %ff, %ff, %ff, %ff};\n', b(1), b(2), b(3), b(4), b(5), b(6));
fprintf('private static final float[] aVal = {%ff, %ff, %ff, %ff, %ff, %ff};\n', a(1), a(2), a(3), a(4), a(5), a(6));