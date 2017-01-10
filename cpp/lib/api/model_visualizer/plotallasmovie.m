
fmt = {'horizontalalignment', 'center', 'verticalalignment', 'middle'};
xax1=-1;
xax2=25;
ipid='172.18.51.67';
skipframesatstart=0;
statefactor=6; #we have input data at 10mins but get mlstate every 60mins. So take 6 samples of input when plotting histogram.

ts_length=100;

set(0, 'defaultfigurevisible', 'off');
filename = ['./resultdir/',ipid];
load(filename); # loads to variable 'A'
i_filename = ['./inputdir/i_',ipid];
source(i_filename); # loads to variable 'inIP'
#set (1, 'defaultlinecolor', 'red');
changepoint=1;


iii=skipframesatstart; # was 0

xlim1 = get (gca (), 'xlim');
xlimr = xlim1(2) - xlim1(1);
perm_xlim = [xlim1(1)-0.1*xlimr, xlim1(2)+0.1*xlimr];
ylim1 = get (gca (), 'ylim');
ylimr = ylim1(2) - ylim1(1);
perm_ylim = [ylim1(1)-0.1*ylimr, ylim1(2)+0.1*ylimr];

for x=[(144-skipframesatstart):-1:1]    
  iii+=1;
  subplot(4,1,2:4) #use bottom 3/4s of screen.
  #axis([xlim1(1)-40 xlim1(2)+60 ylim1(1) ylim1(2)])
  #axis([0 25 ylim1(1) ylim1(2)])
  hold off;
  total_i = iii*statefactor;
  total_i_minus_one = (iii-1)*statefactor;
  INDEXES = 1:(total_i);
  
  yo = inIP( INDEXES );
  yo_hi = max(10,max(yo));
  yo_lo = min(-2,min(yo));
  interval_range = 1; #max(1,(yo_hi-yo_lo)/20);
  hist(inIP( INDEXES ),(yo_lo-1):interval_range:(yo_hi+1),1/interval_range,1, 'facecolor', 'r')
  hold on;
  
  #set (gca(), 'ylim', ylim1);
  #set (gca(), 'xlim', xlim1);
  
  plot(A{1,x},A{2,x}, 'linewidth',5)
  axis([xax1 xax2 0 1])
  title(['IP=',ipid,'; t= ',num2str(iii),'hours; samples=',num2str(total_i)]);
  xlabel(['count of (hostIP=',ipid,') in 10minute bucket']);
  ylabel('P(count)');
  axes("position",[0.7 0.7 0.2 0.2])
  #plot( INDEXES, inIP( INDEXES ) , "color", "red", "linewidth", 5)
  
  subplot(4,1,1) #use top 1/4 of screen.
  
  #create scrolling time series.
  if length(INDEXES) > ts_length
    last100INDEXES = (total_i-ts_length+1):1:total_i;
    ts_vec = inIP( last100INDEXES )';#'
    #plot(1:100, ts_vec)
  else
    sz=zeros(1,ts_length-total_i);
    ts_vec = [sz inIP( INDEXES )'];
  endif
  plot(1:100, ts_vec, "color", "red")
  ylim([0 xax2])
  ylabel('count')
  #xlabel('t (hours ago)')
  set (gca, 'xtick', [4 28 52 76 100])
  set (gca, 'xticklabel', {'16h ago', '12h ago', '8h ago', '4h ago', 'Now'}) 


  hold on;
  
  #fill in the area under the last 6 values
  #mask = [zeros(1:94), ts_vec(95:100)]
  #vec1=zeros(1,(ts_length-statefactor))
  #vec2=ts_vec((ts_length-statefactor+1):1:ts_length)
  mask = [zeros(1,(ts_length-statefactor)), ts_vec((ts_length-statefactor+1):1:ts_length)];
  area ( 1:100, mask, "FaceColor", "red");
  
  #old 
  #mask=[zeros(1,total_i_minus_one),inIP((total_i_minus_one+1):1:total_i)']; #'
  #[zeros(),(total_i_minus_one:1:total_i]
  
  hold off;
  
  filename=sprintf('movie3/%03d.png',iii);
  
  print(filename);
  #input(['bucket: ', num2str(x)]);
endfor
