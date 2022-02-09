# set terminal pngcairo  transparent enhanced font "arial,10" fontscale 1.0 size 660, 320 
# set output 'finance.2.png'
set term qt
set datafile separator ","
set grid nopolar
set grid xtics nomxtics ytics nomytics noztics nomztics \
 nox2tics nomx2tics noy2tics nomy2tics nocbtics nomcbtics
set grid layerdefault   linetype 0 linewidth 1.000,  linetype 3 linewidth 3.000
#set ytics  norangelimit
set xrange [ 0.0000 : 200.000 ] noreverse nowriteback
#set yrange [ 0.0000 : 100.000 ] noreverse nowriteback
set autoscale y
plot 'anomaly.csv' using 2 notitle with lines lt 2#, 'plot.csv' using 5 notitle with lines lt 1, 'plot.csv' using 9 notitle with lines lt 3
