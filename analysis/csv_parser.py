import numpy as np
import matplotlib.pyplot as plt
import csv
from scipy import interpolate

thesis_dir = "C:/Users/Dario/Dropbox/SchoolWork/SeniorThesis/"
data_dir = thesis_dir + "data/SyncedFromPhoneDCIM/"

curls_raw_data = []
with open(data_dir + "sensor_csv_1421204195054squat20.csv", 'rb') as csvfile:
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

# for i in xrange(num_vals):
#     plt.subplot(num_vals,1,i+1)
#     plt.plot(dict_timestamp_data[cur_sensor], dict_val_data[cur_sensor][i], 'bo-')
#     plt.ylabel('Value')
#     plt.title(str_cur_sensor + ' ' + str(i))
#     plt.grid(True)

# plt.xlabel('time (ns)')
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
acc_20hz_vals = {}
acc_20hz_timestamp = np.arange(dict_timestamp_data[cur_sensor][-1], step=50*ns_to_ms)
# print acc_20hz_timestamp
for i in xrange(num_vals):
    f_acc[i] = interpolate.interp1d(dict_timestamp_data[cur_sensor], dict_val_data[cur_sensor][i], kind='cubic')
    acc_20hz_vals[i] = f_acc[i](acc_20hz_timestamp)

plt.figure(0)
for i in xrange(num_vals):
    plt.subplot(num_vals,1,i+1)
    # plt.plot(acc_20hz_timestamp, acc_20hz_vals[i], 'bo-')
    plt.plot(acc_20hz_vals[i], 'bo-')
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

Fs = 20

# Compute FFT just for area of interest, roughly samples 200-1200
plt.figure(1)
for i in xrange(num_vals):
    cur_fft = np.absolute(np.fft.fft(acc_20hz_vals[i][200:1200]))
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
plt.show()