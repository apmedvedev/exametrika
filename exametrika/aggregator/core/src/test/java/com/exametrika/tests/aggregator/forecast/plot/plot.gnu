# set terminal pngcairo  transparent enhanced font "arial,10" fontscale 1.0 size 660, 320 
# set output 'finance.2.png'
set term qt
#set term wxt
set grid nopolar
set grid xtics nomxtics ytics nomytics noztics nomztics \
 nox2tics nomx2tics noy2tics nomy2tics nocbtics nomcbtics
set grid layerdefault   linetype 0 linewidth 1.000,  linetype 3 linewidth 3.000
set ytics  norangelimit
#set ytics   (80.0000, 85.0000, 90.0000, 95.0000, 100.000, 105.000)
#set title "Turn on grid" 
#set xrange [ 30.0000 : 40.000 ] noreverse nowriteback
#set xrange [ 800.0000 : 1000.000 ] noreverse nowriteback


#set yrange [ 75.0000 : 105.000 ] noreverse nowriteback
#set lmargin  9
#set rmargin  2
#set xtics (66, 87, 109, 130, 151, 174, 193, 215, 235)
#set xtics
#set autoscale y
#set multiplot
#set size 1, 0.7
#set origin 0, 0.3
#set bmargin 0
#plot 'plot.txt' using 3 notitle with lines lt 1, 'plot.txt' using 4 notitle with lines lt 2#, 'plot.txt' using 1:4 notitle with lines lt 3, 'plot.txt' using 1:5 notitle with lines lt 4
set bmargin
#set format x
set size 1.0, 0.3
set origin 0.0, 0.0
set tmargin 0
set autoscale y
#set format y "%1.0f"
#set ytics 500
plot 'plot.txt' using 5 notitle with lines lt 3, 'plot.txt' using 6 notitle with lines lt 4
