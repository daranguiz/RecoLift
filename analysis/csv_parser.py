import numpy as np
import matplotlib.pyplot as plt
import csv
from scipy import interpolate
from scipy.constants import pi

thesis_dir = "C:/Users/Dario/Dropbox/SchoolWork/SeniorThesis/"
data_dir = thesis_dir + "data/SyncedFromPhoneDCIM/"

filename = [ 
            "sensor_csv_1421204195054squat20",
            "sensor_csv_1421542046004curl20"
           ]

curls_raw_data = []
with open(data_dir + filename[0] + ".csv", 'rb') as csvfile:
    curls_csv = csv.reader(csvfile, delimiter=",")
    for row in curls_csv:
        curls_raw_data.append(row)

dict_sensor_type = {
    "TYPE_ACCELEROMETER": 1,
    "TYPE_MAGNETIC_FIELD": 2,
    "TYPE_ORIENTATION": 3,
    "TYPE_GYROSCOPE": 4,
    "TYPE_GRAVITY": 9,
    "TYPE_LINEAR_ACCELERATION": 10,
    "TYPE_ROTATION_VECTOR":11
}

# Parse values (strings) to proper form
set_sensor_type = set()
timestamp_offset = long(curls_raw_data[0][1])
for row in curls_raw_data:
    # Sensor type
    row[0] = int(row[0])
    set_sensor_type.update([row[0]])  # for debug, should match dict_sensor_type

    # Timestamp in nanoseconds
    row[1] = long(row[1]) - timestamp_offset

    # Values, floats
    for val in row[2:]:
        val = float(val)

# Sort the raw sensor data by timestamp
curls_raw_data = sorted(curls_raw_data, key=lambda x: x[1])

# Arrange values into timeseries data by sensor type
# [(timestamp, [vals]), (timestamp, [vals]), ...]
dict_curls_data = {}
for row in curls_raw_data:
    sensor_type = row[0]
    timestamp = row[1]
    vals = row[2:]
    new_dict_entry = (timestamp, vals)

    # If key exists (where key = sensor_type), append new entry, else just append
    dict_curls_data.setdefault(sensor_type, []).append(new_dict_entry)

# Not the most efficient way to do this
# Switch to numpy array to fix
dict_timestamp_data = {}
dict_val_data = {}
for key, value in dict_curls_data.iteritems():
    for row in value:
        dict_timestamp_data.setdefault(key, []).append(row[0])
        if key not in dict_val_data:
            dict_val_data[key] = [[] for i in xrange(len(row[1]))]
        for i in xrange(len(row[1])):
            dict_val_data[key][i].append(row[1][i])

# Plot! "You've come so far, the end is near. Now, you are here."
str_cur_sensor = 'TYPE_ACCELEROMETER'
cur_sensor = dict_sensor_type[str_cur_sensor]
num_vals = len(dict_val_data[cur_sensor])


if 0:
    for i in xrange(num_vals):
        plt.subplot(num_vals,1,i+1)
        plt.plot(dict_timestamp_data[cur_sensor], dict_val_data[cur_sensor][i], 'bo-')
        plt.ylabel('Value')
        plt.title(str_cur_sensor + ' ' + str(i))
        plt.grid(True)

    plt.xlabel('time (ns)')
# plt.show()

#=======================================================================#
# Sample rate

# Sensor sampling rate is not guaranteed. What is the average sampling rate of accel?
# Mostly out of curiosity. Timestamp is in nanoseconds
# Accelerometer is 40.5ms appx, 24.67Hz
ns_to_ms = np.power(10,6)
time_diff = [(dict_timestamp_data[cur_sensor][i+1] - dict_timestamp_data[cur_sensor][i]) \
            for i in xrange(len(dict_timestamp_data[cur_sensor])-1)]
avg_sensor_rate_in_ms = np.mean(time_diff) / ns_to_ms
# print "Accelerometer Avg. Sensor Rate (in ms) = " + str(avg_sensor_rate_in_ms)
# print "Rate in Hz = " + str(1000/avg_sensor_rate_in_ms)

# Gyro is 40.0ms, 24.99Hz
str_cur_sensor = 'TYPE_GYROSCOPE'
cur_sensor = dict_sensor_type[str_cur_sensor]
ns_to_ms = np.power(10,6)
time_diff = [(dict_timestamp_data[cur_sensor][i+1] - dict_timestamp_data[cur_sensor][i]) \
            for i in xrange(len(dict_timestamp_data[cur_sensor])-1)]
avg_sensor_rate_in_ms = np.mean(time_diff) / ns_to_ms
# print "Gyroscope Avg. Sensor Rate (in ms) = " + str(avg_sensor_rate_in_ms)
# print "Rate in Hz = " + str(1000/avg_sensor_rate_in_ms)

# Resample accelerometer to 20Hz, or 50ms, or 50*10^6ns
str_cur_sensor = 'TYPE_ACCELEROMETER'
cur_sensor = dict_sensor_type[str_cur_sensor]
num_vals = len(dict_val_data[cur_sensor])

f_acc = {}
vals_20hz = {}
timestamp_20hz = np.arange(50*ns_to_ms, dict_timestamp_data[cur_sensor][-1], step=50*ns_to_ms)
# print timestamp_20hz
for i in xrange(num_vals):
    f_acc[i] = interpolate.interp1d(dict_timestamp_data[cur_sensor], dict_val_data[cur_sensor][i], kind='cubic', fill_value=0, bounds_error=False)
    vals_20hz[i] = f_acc[i](timestamp_20hz)

if 0:
    plt.figure(0)
    for i in xrange(num_vals):
        plt.subplot(num_vals,1,i+1)
        plt.plot(vals_20hz[i])
        plt.ylabel('Value')
        plt.title(str_cur_sensor + ' ' + str(i))
        plt.grid(True)

    plt.xlabel('time (ns)')
# plt.show()

#=======================================================================#
# Preprocessing/filtering
#
# It's kind of frustrating that the sensor rate is only 25Hz. This should be
# enough, but we only get fidelity up to 10Hz if we're downsampling to 20Hz.
# Probably should only downsample to 22Hz or something.

# Thinking aloud here. Is it worth doing any sort of filtering on the data?
# I'm only going to get up to 11 or 12Hz tops. Maybe it's still worth doing, 
# although I'm concerned about throwing away information. 

Fs = 20

# Compute FFT just for area of interest, roughly samples 200-1200
if 1:
    plt.figure(1)
    for i in xrange(num_vals):
        cur_fft = np.absolute(np.fft.fft(vals_20hz[i]))
        cur_fft = cur_fft[:len(cur_fft)/2]
        for j in xrange(2):
            cur_fft[j] = 0
        f = Fs/2 * np.linspace(0, 1, len(cur_fft))
        plt.subplot(num_vals,1,i+1)
        plt.plot(f, cur_fft)
        plt.ylabel('FFT Value')
        plt.title(str_cur_sensor + ' ' + str(i))
        plt.grid(True)

    plt.xlabel('n')
# plt.show()

# Create a LPF to only keep <5Hz
# http://docs.scipy.org/doc/scipy-0.14.0/reference/signal.html
# http://www.ee.iitm.ac.in/~nitin/teaching/ee5480/firdesign.html
plt.figure(2)
from scipy import signal
from scipy.signal import remez
from scipy.signal import freqz
from scipy.signal import lfilter
end_passband = 4.0 / Fs 
start_stopband = 6.0 / Fs
end_stopband = 10.0 / Fs
lpf = remez(20, [0, end_passband, start_stopband, end_stopband], [1.0, 0.0])
# w, h = freqz(lpf)
# plt.plot(w/(2*pi), 20*np.log10(abs(h)))
# plt.show()

vals_filtered = []
for i in xrange(num_vals):
    vals_filtered.append(lfilter(lpf, 1, vals_20hz[i]))
    plt.subplot(num_vals,1,i+1)
    plt.plot(vals_filtered[i])
    plt.ylabel('Sensor Value')
    plt.title(str_cur_sensor + ' ' + str(i))
    plt.grid(True)

# plt.show()

#=======================================================================#
# Autocorrelation
# http://stackoverflow.com/questions/643699/how-can-i-use-numpy-correlate-to-do-autocorrelation

def autocorr(x):
    result = np.correlate(x, x, mode='full')
    return result[result.size/2:]

start_autoc = 400
end_autoc = 700
vals_autoc = []
for i in xrange(num_vals):
    vals_autoc.append(autocorr(vals_filtered[i][start_autoc:end_autoc]))

t = np.linspace(0, 1, 500, endpoint=False)
# plt.figure()
# tmp_wave = signal.square(2 * np.pi * 5 * t)
# plt.plot(autocorr(tmp_wave))
# plt.show()

plt.figure(3)
for i in xrange(num_vals):
    plt.subplot(num_vals,1,i+1)
    plt.plot(vals_autoc[i])
    plt.ylabel('Autocorrelation Value')
    plt.title(str_cur_sensor + ' ' + str(i))
    plt.grid(True)
    plt.xlabel('time (ns)')

#=======================================================================#
# Magnitude Autoc + slope correction

vals_magnitude = [vals_filtered[0][i]**2 + vals_filtered[1][i]**2 + \
                    vals_filtered[2][i]**2 for i in xrange(50, len(vals_filtered[0]))]

magnitude_autoc = autocorr(vals_magnitude[start_autoc:end_autoc])

# Correct for autocorrelation slope, if any
autoc_x_vals = np.arange(len(magnitude_autoc))
regression_coefs = np.polyfit(autoc_x_vals, magnitude_autoc, 1)
magnitude_autoc_corrected = [magnitude_autoc[i] - (regression_coefs[0] * autoc_x_vals[i] \
                                + regression_coefs[1]) for i in xrange(len(magnitude_autoc))]

if 1:
    plt.figure(4)
    plt.subplot(3,1,1)
    plt.plot(vals_magnitude)
    plt.ylabel('Magnitude')
    plt.title('x^2 + y^2 + z^2 Magnitude of ' + str_cur_sensor)
    plt.subplot(3,1,2)
    plt.plot(magnitude_autoc)
    plt.ylabel('Autocorrelation Value')
    plt.title('Autocorrelation from ' + str(start_autoc) + ':' + str(end_autoc))
    plt.subplot(3,1,3)
    plt.plot(magnitude_autoc_corrected)
    plt.ylabel('Autocorrelation Value')
    plt.title('Slope-Corrected Autocorrelation')
    plt.grid(True)
    plt.xlabel('time (ns)')

plt.show()
