import numpy as np
import matplotlib.pyplot as plt
import csv

thesis_dir = "C:/Users/Dario/Dropbox/SchoolWork/SeniorThesis/"
data_dir = thesis_dir + "data/SyncedFromPhoneDCIM/"

curls_raw_data = []
with open(data_dir + "sensor_csv_1416381476734.csv", 'rb') as csvfile:
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

for i in xrange(num_vals):
    plt.subplot(num_vals,1,i+1)
    plt.plot(dict_timestamp_data[cur_sensor], dict_val_data[cur_sensor][i], 'bo-')
    plt.ylabel('Value')
    plt.title(str_cur_sensor + ' ' + str(i))
    plt.grid(True)

plt.xlabel('time (ns)')
plt.show()

