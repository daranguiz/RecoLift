import numpy as np
import matplotlib.pyplot as plt
import csv

thesis_dir = "C:/Users/Dario/Dropbox/SchoolWork/SeniorThesis/"
data_dir = thesis_dir + "data/2014_11_1/"

curls_raw_data = []
with open(data_dir + "barbell curls.csv", 'rb') as csvfile:
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
# Takes 5s vs sub-1s
timestamp_data = []
val_data = [[] for i in xrange(3)]
for row in dict_curls_data[1]:
    timestamp_data.append(row[0])
    for i in xrange(3):
        val_data[i].append(row[1][i])

# Plot! "You've come so far, the end is near. Now, you are here."
plt.subplot(3,1,1)
plt.plot(timestamp_data, val_data[0])
plt.xlabel('time (ns)')
plt.ylabel('Acceleration in m/s^2')
plt.title('Accelerometer X')
plt.grid(True)

plt.subplot(3,1,2)
plt.plot(timestamp_data, val_data[1])
plt.xlabel('time (ns)')
plt.ylabel('Acceleration in m/s^2')
plt.title('Accelerometer Y')
plt.grid(True)

plt.subplot(3,1,3)
plt.plot(timestamp_data, val_data[2])
plt.xlabel('time (ns)')
plt.ylabel('Acceleration in m/s^2')
plt.title('Accelerometer Z')
plt.grid(True)
plt.show()