thesis_dir = 'C:\Users\Dario\Dropbox\SchoolWork\SeniorThesis';
data_dir = strcat(thesis_dir, '\data\2014_11_1\');

curls_csv = csvread(strcat(data_dir,'Barbell Curls.csv'));
bench_csv = csvread(strcat(data_dir,'Barbell bench.csv'));
deadlift_csv = csvread(strcat(data_dir,'Barbell deadlift.csv'));
row_csv = csvread(strcat(data_dir,'Barbell row.csv'));
squat_csv = csvread(strcat(data_dir,'Barbell squat.csv'));
forward_fly_csv = csvread(strcat(data_dir,'Forward fly.csv'));
lateral_fly_csv = csvread(strcat(data_dir,'Lateral fly.csv'));
ohp_csv = csvread(strcat(data_dir,'OHP.csv'));

curls_csv(:,2) = curls_csv(:,2) - curls_csv(1,2);
curls_csv(1,:)

for i=1:10
    for j=1:7
        fprintf('%d: %d\n', j, curls_csv(i,j));
    end
    fprintf('\n');
end